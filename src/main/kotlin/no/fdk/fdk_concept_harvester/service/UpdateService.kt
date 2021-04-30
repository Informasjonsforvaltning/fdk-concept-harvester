package no.fdk.fdk_concept_harvester.service

import no.fdk.fdk_concept_harvester.configuration.ApplicationProperties
import no.fdk.fdk_concept_harvester.harvester.calendarFromTimestamp
import no.fdk.fdk_concept_harvester.model.CollectionMeta
import no.fdk.fdk_concept_harvester.model.ConceptMeta
import no.fdk.fdk_concept_harvester.rdf.containsTriple
import no.fdk.fdk_concept_harvester.rdf.parseRDFResponse
import no.fdk.fdk_concept_harvester.rdf.safeAddProperty
import no.fdk.fdk_concept_harvester.repository.*
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.riot.Lang
import org.apache.jena.sparql.vocabulary.FOAF
import org.apache.jena.vocabulary.DCAT
import org.apache.jena.vocabulary.DCTerms
import org.apache.jena.vocabulary.RDF
import org.apache.jena.vocabulary.SKOS
import org.springframework.stereotype.Service

@Service
class UpdateService(
    private val applicationProperties: ApplicationProperties,
    private val collectionMetaRepository: CollectionMetaRepository,
    private val conceptMetaRepository: ConceptMetaRepository,
    private val turtleService: TurtleService
) {

    fun updateUnionModels() {
        var collectionUnion = ModelFactory.createDefaultModel()
        var collectionUnionNoRecords = ModelFactory.createDefaultModel()
        var conceptUnion = ModelFactory.createDefaultModel()
        var conceptUnionNoRecords = ModelFactory.createDefaultModel()

        collectionMetaRepository.findAll()
            .forEach {
                turtleService.getCollection(it.fdkId, withRecords = true)
                    ?.let { turtle -> parseRDFResponse(turtle, Lang.TURTLE, null) }
                    ?.run { collectionUnion = collectionUnion.union(this) }

                turtleService.getCollection(it.fdkId, withRecords = false)
                    ?.let { turtle -> parseRDFResponse(turtle, Lang.TURTLE, null) }
                    ?.run { collectionUnionNoRecords = collectionUnionNoRecords.union(this) }
            }

        conceptMetaRepository.findAll()
            .forEach {
                turtleService.getConcept(it.fdkId, withRecords = true)
                    ?.let { turtle -> parseRDFResponse(turtle, Lang.TURTLE, null) }
                    ?.run { conceptUnion = conceptUnion.union(this) }

                turtleService.getConcept(it.fdkId, withRecords = false)
                    ?.let { turtle -> parseRDFResponse(turtle, Lang.TURTLE, null) }
                    ?.run { conceptUnionNoRecords = conceptUnionNoRecords.union(this) }
            }

        turtleService.saveAsCollectionUnion(collectionUnion, true)
        turtleService.saveAsCollectionUnion(collectionUnionNoRecords, false)
        turtleService.saveAsConceptUnion(conceptUnion, true)
        turtleService.saveAsConceptUnion(conceptUnionNoRecords, false)
    }

    fun updateMetaData() {
        conceptMetaRepository.findAll()
            .forEach { concept ->
                val conceptMeta = concept.createMetaModel()

                turtleService.getConcept(concept.fdkId, withRecords = false)
                    ?.let { conceptNoRecords -> parseRDFResponse(conceptNoRecords, Lang.TURTLE, null) }
                    ?.let { conceptModelNoRecords -> conceptMeta.union(conceptModelNoRecords) }
                    ?.run { turtleService.saveAsConcept(this, fdkId = concept.fdkId, withRecords = true) }
            }

        collectionMetaRepository.findAll()
            .forEach { collection ->
                val collectionNoRecords = turtleService.getCollection(collection.fdkId, withRecords = false)
                    ?.let { parseRDFResponse(it, Lang.TURTLE, null) }

                if (collectionNoRecords != null) {
                    val collectionURI = "${applicationProperties.collectionsUri}/${collection.fdkId}"
                    var collectionMeta = collection.createMetaModel()

                    conceptMetaRepository.findAllByIsPartOf(collectionURI)
                        .filter { it.modelContainsConcept(collectionNoRecords) }
                        .forEach { concept ->
                            val conceptMeta = concept.createMetaModel()
                            collectionMeta = collectionMeta.union(conceptMeta)
                        }

                    turtleService.saveAsCollection(
                        collectionMeta.union(collectionNoRecords),
                        fdkId = collection.fdkId,
                        withRecords = true
                    )
                }
            }

        updateUnionModels()
    }

    private fun CollectionMeta.createMetaModel(): Model {
        val fdkUri = "${applicationProperties.collectionsUri}/$fdkId"

        val metaModel = ModelFactory.createDefaultModel()

        metaModel.createResource(fdkUri)
            .addProperty(RDF.type, DCAT.CatalogRecord)
            .addProperty(DCTerms.identifier, fdkId)
            .addProperty(FOAF.primaryTopic, metaModel.createResource(uri))
            .addProperty(DCTerms.issued, metaModel.createTypedLiteral(calendarFromTimestamp(issued)))
            .addProperty(DCTerms.modified, metaModel.createTypedLiteral(calendarFromTimestamp(modified)))

        return metaModel
    }

    private fun ConceptMeta.createMetaModel(): Model {
        val fdkUri = "${applicationProperties.conceptsUri}/$fdkId"

        val metaModel = ModelFactory.createDefaultModel()

        metaModel.createResource(fdkUri)
            .addProperty(RDF.type, DCAT.CatalogRecord)
            .addProperty(DCTerms.identifier, fdkId)
            .addProperty(FOAF.primaryTopic, metaModel.createResource(uri))
            .safeAddProperty(DCTerms.isPartOf, isPartOf)
            .addProperty(DCTerms.issued, metaModel.createTypedLiteral(calendarFromTimestamp(issued)))
            .addProperty(DCTerms.modified, metaModel.createTypedLiteral(calendarFromTimestamp(modified)))

        return metaModel
    }

    private fun ConceptMeta.modelContainsConcept(model: Model): Boolean =
        model.containsTriple("<${uri}>", "a", "<${SKOS.Concept.uri}>")

}
