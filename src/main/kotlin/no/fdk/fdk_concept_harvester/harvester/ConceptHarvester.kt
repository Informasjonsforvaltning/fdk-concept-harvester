package no.fdk.fdk_concept_harvester.harvester

import no.fdk.fdk_concept_harvester.adapter.ConceptsAdapter
import no.fdk.fdk_concept_harvester.adapter.DefaultOrganizationsAdapter
import no.fdk.fdk_concept_harvester.configuration.ApplicationProperties
import no.fdk.fdk_concept_harvester.model.CollectionMeta
import no.fdk.fdk_concept_harvester.model.ConceptMeta
import no.fdk.fdk_concept_harvester.model.ContentType
import no.fdk.fdk_concept_harvester.model.HarvestDataSource
import no.fdk.fdk_concept_harvester.rdf.createIdFromUri
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
import java.util.*

private val LOGGER = LoggerFactory.getLogger(ConceptHarvester::class.java)

@Service
class ConceptHarvester(
    private val adapter: ConceptsAdapter,
    private val orgAdapter: DefaultOrganizationsAdapter,
    private val collectionMetaRepository: CollectionMetaRepository,
    private val conceptMetaRepository: ConceptMetaRepository,
    private val turtleService: TurtleService,
    private val applicationProperties: ApplicationProperties
) {

    fun harvestConceptCollection(source: HarvestDataSource, harvestDate: Calendar) =
        if (source.url != null) {
            LOGGER.debug("Starting harvest of ${source.url}")
            val contentType = ContentType.fromValue(source.acceptHeaderValue)
            if (contentType != null) {
                if(contentType.isRDF()) {
                    harvestConceptCollectionFromRDF(source, harvestDate)
                } else if(contentType.isTBX()) {
                    harvestConceptCollectionFromTBX(source, harvestDate)
                } else {
                    LOGGER.error("Harvest source contains an unsupported content-type", HarvestException("undefined"))
                }

            } else {
                LOGGER.error("Harvest source contains an unsupported accept header", HarvestException("undefined"))
            }

        } else LOGGER.error("Harvest source is not defined", HarvestException("undefined"))

    private fun harvestConceptCollectionFromRDF(source: HarvestDataSource, harvestDate: Calendar) {
        val jenaWriterType = jenaTypeFromAcceptHeader(source.acceptHeaderValue)

        val harvested = when (jenaWriterType) {
            null -> null
            Lang.RDFNULL -> null
            else -> adapter.getConcepts(source)?.let {
                parseRDFResponse(it, jenaWriterType, source.url) }
        }

        when {
            jenaWriterType == null -> LOGGER.error("Not able to harvest from ${source.url}, no accept header supplied", HarvestException(source.url!!))
            jenaWriterType == Lang.RDFNULL -> LOGGER.error("Not able to harvest from ${source.url}, header ${source.acceptHeaderValue} is not acceptable", HarvestException(source.url!!))
            harvested == null -> LOGGER.info("Not able to harvest ${source.url}")
            else -> saveIfHarvestedContainsChanges(harvested, source.url!!, harvestDate, source.publisherId)
        }
    }

    private fun harvestConceptCollectionFromTBX(source: HarvestDataSource, harvestDate: Calendar) {
        val harvested = adapter.getConcepts(source)?.let { parseTBXResponse(it, source.url, orgAdapter) }

        when {
            harvested == null -> LOGGER.info("Not able to harvest ${source.url}")
            else -> saveIfHarvestedContainsChanges(harvested, source.url!!, harvestDate, source.publisherId)
        }
    }

    private fun saveIfHarvestedContainsChanges(
        harvested: Model,
        sourceURL: String,
        harvestDate: Calendar,
        publisherId: String?
    ) {
        val dbData = turtleService.getHarvestSource(sourceURL)
            ?.let { parseRDFResponse(it, Lang.TURTLE, null) }

        if (dbData != null && harvested.isIsomorphicWith(dbData)) {
            LOGGER.info("No changes from last harvest of $sourceURL")
        } else {
            LOGGER.info("Changes detected, saving data from $sourceURL and updating FDK meta data")
            turtleService.saveAsHarvestSource(harvested, sourceURL)

            val concepts = splitConceptsFromRDF(harvested, sourceURL)

            if (concepts.isEmpty()) LOGGER.error("No collection with concepts found in data harvested from $sourceURL", HarvestException(sourceURL))
            else {
                updateConcepts(concepts, harvestDate)

                val organization = if (publisherId != null && concepts.containsFreeConcepts()) {
                    orgAdapter.getOrganization(publisherId)
                } else null

                val collections = splitCollectionsFromRDF(harvested, concepts, sourceURL, organization)
                updateCollections(collections, harvestDate)
            }
        }
    }

    private fun updateConcepts(concepts: List<ConceptRDFModel>, harvestDate: Calendar) {
        concepts.forEach { concept ->
            val dbMeta = conceptMetaRepository.findByIdOrNull(concept.resourceURI)
            if (concept.conceptHasChanges(dbMeta?.fdkId)) {
                val modelMeta = concept.mapToDBOMeta(harvestDate, dbMeta)
                conceptMetaRepository.save(modelMeta)

                turtleService.saveAsConcept(
                    model = concept.harvested,
                    fdkId = modelMeta.fdkId,
                    withRecords = false
                )
            }
        }
    }

    private fun updateCollections(collections: List<CollectionRDFModel>, harvestDate: Calendar) {
        collections
            .map { Pair(it, collectionMetaRepository.findByIdOrNull(it.resourceURI)) }
            .filter { it.first.collectionHasChanges(it.second?.fdkId) }
            .forEach {
                val updatedCollectionMeta = it.first.mapToCollectionMeta(harvestDate, it.second)
                collectionMetaRepository.save(updatedCollectionMeta)

                turtleService.saveAsCollection(
                    model = it.first.harvested,
                    fdkId = updatedCollectionMeta.fdkId,
                    withRecords = false
                )

                val fdkUri = "${applicationProperties.collectionsUri}/${updatedCollectionMeta.fdkId}"

                it.first.concepts.forEach { conceptURI ->
                    addIsPartOfToConcept(conceptURI, fdkUri)
                }
            }
    }

    private fun CollectionRDFModel.mapToCollectionMeta(
        harvestDate: Calendar,
        dbMeta: CollectionMeta?
    ): CollectionMeta {
        val collectionURI = resourceURI
        val fdkId = dbMeta?.fdkId ?: createIdFromUri(collectionURI)
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
        val fdkId = dbMeta?.fdkId ?: createIdFromUri(resourceURI)
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
}
