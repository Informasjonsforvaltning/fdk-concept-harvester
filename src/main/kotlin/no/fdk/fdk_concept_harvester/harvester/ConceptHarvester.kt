package no.fdk.fdk_concept_harvester.harvester

import no.fdk.fdk_concept_harvester.adapter.ConceptsAdapter
import no.fdk.fdk_concept_harvester.adapter.DefaultOrganizationsAdapter
import no.fdk.fdk_concept_harvester.configuration.ApplicationProperties
import no.fdk.fdk_concept_harvester.model.*
import no.fdk.fdk_concept_harvester.rdf.createIdFromString
import no.fdk.fdk_concept_harvester.rdf.jenaTypeFromAcceptHeader
import no.fdk.fdk_concept_harvester.rdf.parseRDFResponse
import no.fdk.fdk_concept_harvester.repository.CollectionMetaRepository
import no.fdk.fdk_concept_harvester.repository.ConceptMetaRepository
import no.fdk.fdk_concept_harvester.service.TurtleService
import no.fdk.fdk_concept_harvester.tbx.parseTBXResponse
import org.apache.jena.rdf.model.Model
import org.apache.jena.riot.Lang
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*

private val LOGGER = LoggerFactory.getLogger(ConceptHarvester::class.java)
private const val dateFormat: String = "yyyy-MM-dd HH:mm:ss Z"

@Service
class ConceptHarvester(
    private val adapter: ConceptsAdapter,
    private val orgAdapter: DefaultOrganizationsAdapter,
    private val collectionMetaRepository: CollectionMetaRepository,
    private val conceptMetaRepository: ConceptMetaRepository,
    private val turtleService: TurtleService,
    private val applicationProperties: ApplicationProperties
) {

    fun harvestConceptCollection(source: HarvestDataSource, harvestDate: Calendar, forceUpdate: Boolean): HarvestReport? =
        if (source.id != null && source.url != null) {
            try {
                LOGGER.debug("Starting harvest of ${source.url}")
                val contentType = ContentType.fromValue(source.acceptHeaderValue)
                when {
                    contentType == null -> {
                        LOGGER.error("Unable to harvest ${source.url}, source contains an unsupported accept header", HarvestException("unsupported accept header"))
                        HarvestReport(
                            id = source.id,
                            url = source.url,
                            harvestError = true,
                            errorMessage = "Not able to harvest, unsupported accept header",
                            startTime = harvestDate.formatWithOsloTimeZone(),
                            endTime = formatNowWithOsloTimeZone()
                        )
                    }
                    contentType.isRDF() -> harvestConceptCollectionFromRDF(source, harvestDate, forceUpdate)
                    contentType.isTBX() -> harvestConceptCollectionFromTBX(source, harvestDate, forceUpdate)
                    else -> {
                        LOGGER.error("Harvest source contains an unsupported content-type", HarvestException("unsupported content-type"))
                        HarvestReport(
                            id = source.id,
                            url = source.url,
                            harvestError = true,
                            errorMessage = "Not able to harvest, unsupported accept header",
                            startTime = harvestDate.formatWithOsloTimeZone(),
                            endTime = formatNowWithOsloTimeZone()
                        )
                    }
                }
            } catch (ex: Exception) {
                LOGGER.error("Harvest of ${source.url} failed", ex)
                HarvestReport(
                    id = source.id,
                    url = source.url,
                    harvestError = true,
                    errorMessage = ex.message,
                    startTime = harvestDate.formatWithOsloTimeZone(),
                    endTime = formatNowWithOsloTimeZone()
                )
            }

        } else {
            LOGGER.error("Harvest source is not valid", HarvestException("undefined"))
            null
        }

    private fun harvestConceptCollectionFromRDF(source: HarvestDataSource, harvestDate: Calendar, forceUpdate: Boolean): HarvestReport {
        val jenaWriterType = jenaTypeFromAcceptHeader(source.acceptHeaderValue)

        return if (jenaWriterType == null || jenaWriterType == Lang.RDFNULL) {
            LOGGER.error("Unable to harvest ${source.url}, source contains an unsupported accept header", HarvestException("unsupported accept header"))
            HarvestReport(
                id = source.id ?: "unknown-id",
                url = source.url ?: "https://example.com",
                harvestError = true,
                errorMessage = "Not able to harvest, unsupported accept header",
                startTime = harvestDate.formatWithOsloTimeZone(),
                endTime = formatNowWithOsloTimeZone()
            )
        } else {
            saveIfHarvestedContainsChanges(
                parseRDFResponse(adapter.getConcepts(source), jenaWriterType, source.url),
                source.id!!,
                source.url!!,
                harvestDate,
                source.publisherId,
                forceUpdate
            )
        }
    }

    private fun harvestConceptCollectionFromTBX(source: HarvestDataSource, harvestDate: Calendar, forceUpdate: Boolean): HarvestReport {
        val harvested = parseTBXResponse(adapter.getConcepts(source), source.url, orgAdapter)
        return saveIfHarvestedContainsChanges(harvested, source.id!!, source.url!!, harvestDate, source.publisherId, forceUpdate)
    }

    private fun saveIfHarvestedContainsChanges(
        harvested: Model,
        sourceId: String,
        sourceURL: String,
        harvestDate: Calendar,
        publisherId: String?,
        forceUpdate: Boolean
    ): HarvestReport {
        val dbData = turtleService.getHarvestSource(sourceURL)
            ?.let { parseRDFResponse(it, Lang.TURTLE, null) }

        return if (!forceUpdate && dbData != null && harvested.isIsomorphicWith(dbData)) {
            LOGGER.info("No changes from last harvest of $sourceURL")
            HarvestReport(
                id = sourceId,
                url = sourceURL,
                harvestError = false,
                startTime = harvestDate.formatWithOsloTimeZone(),
                endTime = formatNowWithOsloTimeZone()
            )
        } else {
            LOGGER.info("Saving data from $sourceURL and updating FDK meta data")
            turtleService.saveAsHarvestSource(harvested, sourceURL)

            updateDB(harvested, sourceId, sourceURL, harvestDate, publisherId, forceUpdate)
        }
    }

    private fun updateDB(
        harvested: Model,
        sourceId: String,
        sourceURL: String,
        harvestDate: Calendar,
        publisherId: String?,
        forceUpdate: Boolean
    ): HarvestReport {
        val concepts = splitConceptsFromRDF(harvested, sourceURL)

        return if (concepts.isEmpty()) {
            LOGGER.error("No collection with concepts found in data harvested from $sourceURL", HarvestException(sourceURL))
            HarvestReport(
                id = sourceId,
                url = sourceURL,
                harvestError = true,
                errorMessage = "No collection with concepts found in data harvested from $sourceURL",
                startTime = harvestDate.formatWithOsloTimeZone(),
                endTime = formatNowWithOsloTimeZone()
            )
        } else {
            val updatedConcepts = updateConcepts(concepts, harvestDate, forceUpdate)

            val organization = if (publisherId != null && concepts.containsFreeConcepts()) {
                orgAdapter.getOrganization(publisherId)
            } else null

            val collections = splitCollectionsFromRDF(harvested, concepts, sourceURL, organization)
            val updatedCollections = updateCollections(collections, harvestDate, forceUpdate)

            val removedConcepts = getConceptsRemovedThisHarvest(
                updatedCollections.map { collectionFdkUri(it.fdkId) },
                concepts.map { it.resourceURI }
            )
            removedConcepts.map { it.copy(removed = true) }
                .run { conceptMetaRepository.saveAll(this) }

            HarvestReport(
                id = sourceId,
                url = sourceURL,
                harvestError = false,
                startTime = harvestDate.formatWithOsloTimeZone(),
                endTime = formatNowWithOsloTimeZone(),
                changedCatalogs = updatedCollections,
                changedResources = updatedConcepts,
                removedResources = removedConcepts.map { FdkIdAndUri(fdkId = it.fdkId, uri = it.uri) }
            )
        }
    }

    private fun updateConcepts(concepts: List<ConceptRDFModel>, harvestDate: Calendar, forceUpdate: Boolean): List<FdkIdAndUri> =
        concepts
            .map { Pair(it, conceptMetaRepository.findByIdOrNull(it.resourceURI)) }
            .filter { forceUpdate || it.first.conceptHasChanges(it.second?.fdkId) }
            .map {
                val modelMeta = it.first.mapToDBOMeta(harvestDate, it.second)
                conceptMetaRepository.save(modelMeta)

                turtleService.saveAsConcept(
                    model = it.first.harvested,
                    fdkId = modelMeta.fdkId,
                    withRecords = false
                )

                FdkIdAndUri(fdkId = modelMeta.fdkId, uri = it.first.resourceURI)
            }

    private fun updateCollections(collections: List<CollectionRDFModel>, harvestDate: Calendar, forceUpdate: Boolean): List<FdkIdAndUri> =
        collections
            .map { Pair(it, collectionMetaRepository.findByIdOrNull(it.resourceURI)) }
            .filter { forceUpdate || it.first.collectionHasChanges(it.second?.fdkId) }
            .map {
                val updatedCollectionMeta = it.first.mapToCollectionMeta(harvestDate, it.second)
                collectionMetaRepository.save(updatedCollectionMeta)

                turtleService.saveAsCollection(
                    model = it.first.harvested,
                    fdkId = updatedCollectionMeta.fdkId,
                    withRecords = false
                )

                val fdkUri = collectionFdkUri(updatedCollectionMeta.fdkId)

                it.first.concepts.forEach { conceptURI ->
                    addIsPartOfToConcept(conceptURI, fdkUri)
                }

                FdkIdAndUri(fdkId = updatedCollectionMeta.fdkId, uri = updatedCollectionMeta.uri)
            }

    private fun CollectionRDFModel.mapToCollectionMeta(
        harvestDate: Calendar,
        dbMeta: CollectionMeta?
    ): CollectionMeta {
        val collectionURI = resourceURI
        val fdkId = dbMeta?.fdkId ?: createIdFromString(collectionURI)
        val issued = dbMeta?.issued
            ?.let { timestamp -> calendarFromTimestamp(timestamp) }
            ?: harvestDate

        return CollectionMeta(
            uri = collectionURI,
            fdkId = fdkId,
            issued = issued.timeInMillis,
            modified = harvestDate.timeInMillis,
            concepts = concepts
        )
    }

    private fun ConceptRDFModel.mapToDBOMeta(
        harvestDate: Calendar,
        dbMeta: ConceptMeta?
    ): ConceptMeta {
        val fdkId = dbMeta?.fdkId ?: createIdFromString(resourceURI)
        val issued: Calendar = dbMeta?.issued
            ?.let { timestamp -> calendarFromTimestamp(timestamp) }
            ?: harvestDate

        return ConceptMeta(
            uri = resourceURI,
            fdkId = fdkId,
            issued = issued.timeInMillis,
            modified = harvestDate.timeInMillis
        )
    }

    private fun addIsPartOfToConcept(conceptURI: String, collectionURI: String) =
        conceptMetaRepository.findByIdOrNull(conceptURI)
            ?.run { conceptMetaRepository.save(copy(isPartOf = collectionURI)) }

    private fun CollectionRDFModel.collectionHasChanges(fdkId: String?): Boolean =
        if (fdkId == null) true
        else harvestDiff(turtleService.getCollection(fdkId, withRecords = false))

    private fun ConceptRDFModel.conceptHasChanges(fdkId: String?): Boolean =
        if (fdkId == null) true
        else harvestDiff(turtleService.getConcept(fdkId, withRecords = false))

    private fun formatNowWithOsloTimeZone(): String =
        ZonedDateTime.now(ZoneId.of("Europe/Oslo"))
            .format(DateTimeFormatter.ofPattern(dateFormat))

    private fun Calendar.formatWithOsloTimeZone(): String =
        ZonedDateTime.from(toInstant().atZone(ZoneId.of("Europe/Oslo")))
            .format(DateTimeFormatter.ofPattern(dateFormat))

    private fun collectionFdkUri(fdkId: String): String =
        "${applicationProperties.collectionsUri}/$fdkId"

    private fun getConceptsRemovedThisHarvest(collections: List<String>, concepts: List<String>): List<ConceptMeta> =
        collections.flatMap { conceptMetaRepository.findAllByIsPartOf(it) }
            .filter { !it.removed && !concepts.contains(it.uri) }
}
