package no.fdk.fdk_concept_harvester.service

import no.fdk.fdk_concept_harvester.configuration.ApplicationProperties
import no.fdk.fdk_concept_harvester.harvester.calendarFromTimestamp
import no.fdk.fdk_concept_harvester.harvester.extractCollectionModel
import no.fdk.fdk_concept_harvester.model.CollectionMeta
import no.fdk.fdk_concept_harvester.model.ConceptMeta
import no.fdk.fdk_concept_harvester.rdf.containsTriple
import no.fdk.fdk_concept_harvester.rdf.safeParseRDF
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
        val collectionUnion = ModelFactory.createDefaultModel()
        val collectionUnionNoRecords = ModelFactory.createDefaultModel()

        collectionMetaRepository.findAll()
            .filter { it.concepts.isNotEmpty() }
            .forEach {
                turtleService.getCollection(it.fdkId, withRecords = true)
                    ?.let { turtle -> safeParseRDF(turtle, Lang.TURTLE) }
                    ?.run { collectionUnion.add(this) }

                turtleService.getCollection(it.fdkId, withRecords = false)
                    ?.let { turtle -> safeParseRDF(turtle, Lang.TURTLE) }
                    ?.run { collectionUnionNoRecords.add(this) }
            }

        turtleService.saveAsCollectionUnion(collectionUnion, true)
        turtleService.saveAsCollectionUnion(collectionUnionNoRecords, false)
    }

    fun updateMetaData() {
        collectionMetaRepository.findAll()
            .forEach { collection ->
                val collectionNoRecords = turtleService.getCollection(collection.fdkId, withRecords = false)
                    ?.let { safeParseRDF(it, Lang.TURTLE) }

                if (collectionNoRecords != null) {
                    val collectionURI = "${applicationProperties.collectionsUri}/${collection.fdkId}"
                    val collectionMeta = collection.createMetaModel()
                    val completeMetaModel = ModelFactory.createDefaultModel()
                    completeMetaModel.add(collectionMeta)

                    val collectionTriples = collectionNoRecords.getResource(collection.uri)
                        .extractCollectionModel()
                    collectionTriples.add(collectionMeta)

                    conceptMetaRepository.findAllByIsPartOf(collectionURI)
                        .filter { it.modelContainsConcept(collectionNoRecords) }
                        .forEach { concept ->
                            val conceptMeta = concept.createMetaModel()
                            completeMetaModel.add(conceptMeta)

                            turtleService.getConcept(concept.fdkId, withRecords = false)
                                ?.let { conceptNoRecords -> safeParseRDF(conceptNoRecords, Lang.TURTLE) }
                                ?.let { conceptModelNoRecords -> conceptMeta.union(conceptModelNoRecords) }
                                ?.let { conceptModel -> collectionTriples.union(conceptModel) }
                                ?.run { turtleService.saveAsConcept(this, fdkId = concept.fdkId, withRecords = true) }
                        }

                    turtleService.saveAsCollection(
                        completeMetaModel.union(collectionNoRecords),
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
