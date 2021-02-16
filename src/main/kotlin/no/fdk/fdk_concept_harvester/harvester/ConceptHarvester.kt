package no.fdk.fdk_concept_harvester.harvester

import no.fdk.fdk_concept_harvester.adapter.ConceptsAdapter
import no.fdk.fdk_concept_harvester.configuration.ApplicationProperties
import no.fdk.fdk_concept_harvester.model.*
import no.fdk.fdk_concept_harvester.rdf.*
import no.fdk.fdk_concept_harvester.repository.*
import no.fdk.fdk_concept_harvester.service.TurtleService
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.riot.Lang
import org.apache.jena.sparql.vocabulary.FOAF
import org.apache.jena.vocabulary.DCAT
import org.apache.jena.vocabulary.DCTerms
import org.apache.jena.vocabulary.RDF
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.util.*

private val LOGGER = LoggerFactory.getLogger(ConceptHarvester::class.java)

@Service
class ConceptHarvester(
    private val adapter: ConceptsAdapter,
    private val collectionMetaRepository: CollectionMetaRepository,
    private val conceptMetaRepository: ConceptMetaRepository,
    private val turtleService: TurtleService,
    private val applicationProperties: ApplicationProperties
) {

    fun harvestConceptCollection(source: HarvestDataSource, harvestDate: Calendar) =
        if (source.url != null) {
            LOGGER.debug("Starting harvest of ${source.url}")
            val jenaWriterType = jenaTypeFromAcceptHeader(source.acceptHeaderValue)

            val harvested = when (jenaWriterType) {
                null -> null
                Lang.RDFNULL -> null
                else -> adapter.getConcepts(source)?.let { parseRDFResponse(it, jenaWriterType, source.url) }
            }

            when {
                jenaWriterType == null -> LOGGER.error("Not able to harvest from ${source.url}, no accept header supplied")
                jenaWriterType == Lang.RDFNULL -> LOGGER.error("Not able to harvest from ${source.url}, header ${source.acceptHeaderValue} is not acceptable ")
                harvested == null -> LOGGER.info("Not able to harvest ${source.url}")
                else -> saveIfHarvestedContainsChanges(harvested, source.url, harvestDate)
            }
        } else LOGGER.error("Harvest source is not defined")

    private fun saveIfHarvestedContainsChanges(harvested: Model, sourceURL: String, harvestDate: Calendar) {
        val dbData = turtleService.getHarvestSource(sourceURL)
            ?.let { parseRDFResponse(it, Lang.TURTLE, null) }

        if (dbData != null && harvested.isIsomorphicWith(dbData)) {
            LOGGER.info("No changes from last harvest of $sourceURL")
        } else {
            LOGGER.info("Changes detected, saving data from $sourceURL and updating FDK meta data")
            turtleService.saveAsHarvestSource(harvested, sourceURL)

            val collections = splitCollectionsFromRDF(harvested)

            if (collections.isEmpty()) LOGGER.error("No collection with conceptss found in data harvested from $sourceURL")
            else updateDB(collections, harvestDate)
        }
    }

    private fun updateDB(collections: List<CollectionsAndConcepts>, harvestDate: Calendar) {
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

                it.first.concepts.forEach { infoModel ->
                    infoModel.updateDBOs(harvestDate, fdkUri)
                }

                var collectionModel = it.first.harvestedWithoutConcepts
                collectionModel.createResource(fdkUri)
                    .addProperty(RDF.type, DCAT.CatalogRecord)
                    .addProperty(DCTerms.identifier, updatedCollectionMeta.fdkId)
                    .addProperty(FOAF.primaryTopic, collectionModel.createResource(updatedCollectionMeta.uri))
                    .addProperty(DCTerms.issued, collectionModel.createTypedLiteral(calendarFromTimestamp(updatedCollectionMeta.issued)))
                    .addProperty(DCTerms.modified, collectionModel.createTypedLiteral(harvestDate))

                conceptMetaRepository.findAllByIsPartOf(fdkUri)
                    .mapNotNull { infoMeta -> turtleService.getConcept(infoMeta.fdkId, withRecords = true) }
                    .map { infoModelTurtle -> parseRDFResponse(infoModelTurtle, Lang.TURTLE, null) }
                    .forEach { infoModel -> collectionModel = collectionModel.union(infoModel) }

                turtleService.saveAsCollection(
                    model = collectionModel,
                    fdkId = updatedCollectionMeta.fdkId,
                    withRecords = true
                )
            }
    }

    private fun ConceptRDFModel.updateDBOs(
        harvestDate: Calendar,
        fdkCatalogURI: String
    ) {
        val dbMeta = conceptMetaRepository.findByIdOrNull(resourceURI)
        if (conceptHasChanges(dbMeta?.fdkId)) {
            val modelMeta = mapToDBOMeta(harvestDate, fdkCatalogURI, dbMeta)
            conceptMetaRepository.save(modelMeta)

            turtleService.saveAsConcept(
                model = harvested,
                fdkId = modelMeta.fdkId,
                withRecords = false
            )

            val fdkUri = "${applicationProperties.conceptsUri}/${modelMeta.fdkId}"
            val metaModel = ModelFactory.createDefaultModel()

            metaModel.createResource(fdkUri)
                .addProperty(RDF.type, DCAT.CatalogRecord)
                .addProperty(DCTerms.identifier, modelMeta.fdkId)
                .addProperty(FOAF.primaryTopic, metaModel.createResource(resourceURI))
                .addProperty(DCTerms.isPartOf, metaModel.createResource(modelMeta.isPartOf))
                .addProperty(DCTerms.issued, metaModel.createTypedLiteral(calendarFromTimestamp(modelMeta.issued)))
                .addProperty(DCTerms.modified, metaModel.createTypedLiteral(harvestDate))

            turtleService.saveAsConcept(
                model = metaModel.union(harvested),
                fdkId = modelMeta.fdkId,
                withRecords = true
            )
        }
    }

    private fun CollectionsAndConcepts.mapToCollectionMeta(
        harvestDate: Calendar,
        dbMeta: CollectionMeta?
    ): CollectionMeta {
        val catalogURI = resourceURI
        val fdkId = dbMeta?.fdkId ?: createIdFromUri(catalogURI)
        val issued = dbMeta?.issued
            ?.let { timestamp -> calendarFromTimestamp(timestamp) }
            ?: harvestDate

        return CollectionMeta(
            uri = catalogURI,
            fdkId = fdkId,
            issued = issued.timeInMillis,
            modified = harvestDate.timeInMillis
        )
    }

    private fun ConceptRDFModel.mapToDBOMeta(
        harvestDate: Calendar,
        fdkCatalogURI: String,
        dbMeta: ConceptMeta?
    ): ConceptMeta {
        val fdkId = dbMeta?.fdkId ?: createIdFromUri(resourceURI)
        val issued: Calendar = dbMeta?.issued
            ?.let { timestamp -> calendarFromTimestamp(timestamp) }
            ?: harvestDate

        return ConceptMeta(
            uri = resourceURI,
            fdkId = fdkId,
            isPartOf = fdkCatalogURI,
            issued = issued.timeInMillis,
            modified = harvestDate.timeInMillis
        )
    }

    private fun CollectionsAndConcepts.collectionHasChanges(fdkId: String?): Boolean =
        if (fdkId == null) true
        else harvestDiff(turtleService.getCollection(fdkId, withRecords = false))

    private fun ConceptRDFModel.conceptHasChanges(fdkId: String?): Boolean =
        if (fdkId == null) true
        else harvestDiff(turtleService.getConcept(fdkId, withRecords = false))
}
