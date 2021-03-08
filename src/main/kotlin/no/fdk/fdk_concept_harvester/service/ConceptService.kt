package no.fdk.fdk_concept_harvester.service

import no.fdk.fdk_concept_harvester.rdf.createRDFResponse
import no.fdk.fdk_concept_harvester.rdf.parseRDFResponse
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.riot.Lang
import org.springframework.stereotype.Service

@Service
class ConceptService(private val turtleService: TurtleService) {

    fun getAllCollections(returnType: Lang, withRecords: Boolean): String =
        turtleService.getCollectionUnion(withRecords)
            ?.let {
                if (returnType == Lang.TURTLE) it
                else parseRDFResponse(it, Lang.TURTLE, null)?.createRDFResponse(returnType)
            }
            ?: ModelFactory.createDefaultModel().createRDFResponse(returnType)

    fun getAllConcepts(returnType: Lang, withRecords: Boolean): String =
        turtleService.getConceptUnion(withRecords)
            ?.let {
                if (returnType == Lang.TURTLE) it
                else parseRDFResponse(it, Lang.TURTLE, null)?.createRDFResponse(returnType)
            }
            ?: ModelFactory.createDefaultModel().createRDFResponse(returnType)

    fun getCollectionById(id: String, returnType: Lang, withRecords: Boolean): String? =
        turtleService.getCollection(id, withRecords)
            ?.let {
                if (returnType == Lang.TURTLE) it
                else parseRDFResponse(it, Lang.TURTLE, null)?.createRDFResponse(returnType)
            }

    fun getConceptById(id: String, returnType: Lang, withRecords: Boolean): String? =
        turtleService.getConcept(id, withRecords)
            ?.let {
                if (returnType == Lang.TURTLE) it
                else parseRDFResponse(it, Lang.TURTLE, null)?.createRDFResponse(returnType)
            }

}
