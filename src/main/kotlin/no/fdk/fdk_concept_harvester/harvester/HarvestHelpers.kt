package no.fdk.fdk_concept_harvester.harvester

import no.fdk.fdk_concept_harvester.rdf.*
import org.apache.jena.query.QueryExecutionFactory
import org.apache.jena.query.QueryFactory
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.Resource
import org.apache.jena.rdf.model.ResourceRequiredException
import org.apache.jena.rdf.model.Statement
import org.apache.jena.riot.Lang
import org.apache.jena.vocabulary.RDF
import org.apache.jena.vocabulary.SKOS
import java.util.*


fun CollectionsAndConcepts.harvestDiff(dboNoRecords: String?): Boolean =
    if (dboNoRecords == null) true
    else !harvested.isIsomorphicWith(parseRDFResponse(dboNoRecords, Lang.TURTLE, null))

fun ConceptRDFModel.harvestDiff(dboNoRecords: String?): Boolean =
    if (dboNoRecords == null) true
    else !harvested.isIsomorphicWith(parseRDFResponse(dboNoRecords, Lang.TURTLE, null))

fun splitCollectionsFromRDF(harvested: Model): List<CollectionsAndConcepts> =
    harvested.listResourcesWithProperty(RDF.type, SKOS.Collection)
        .toList()
        .filter { it.hasProperty(SKOS.member) }
        .map { collectionResource ->
            val collectionConcepts: List<ConceptRDFModel> = collectionResource.listProperties(SKOS.member)
                .toList()
                .map { dataset -> dataset.resource.extractConcept() }

            var collectionModelWithoutConcepts = collectionResource.listProperties().toModel()
            collectionModelWithoutConcepts.setNsPrefixes(harvested.nsPrefixMap)

            collectionResource.listProperties().toList()
                .filter { it.isResourceProperty() }
                .forEach {
                    if (it.predicate != SKOS.member) {
                        collectionModelWithoutConcepts =
                            collectionModelWithoutConcepts.recursiveAddNonConceptResource(it.resource, 5)
                    }
                }

            var collectionModel = collectionModelWithoutConcepts
            collectionConcepts.forEach { collectionModel = collectionModel.union(it.harvested) }

            CollectionsAndConcepts(
                resourceURI = collectionResource.uri,
                harvested = collectionModel,
                harvestedWithoutConcepts = collectionModelWithoutConcepts,
                concepts = collectionConcepts
            )
        }

fun Resource.extractConcept(): ConceptRDFModel {
    var conceptModel = listProperties().toModel()
    conceptModel = conceptModel.setNsPrefixes(model.nsPrefixMap)

    listProperties().toList()
        .filter { it.isResourceProperty() }
        .forEach { conceptModel = conceptModel.recursiveAddNonConceptResource(it.resource, 10) }

    return ConceptRDFModel(resourceURI = uri, harvested = conceptModel)
}

private fun Model.recursiveAddNonConceptResource(resource: Resource, recursiveCount: Int): Model {
    val newCount = recursiveCount - 1

    if (resourceShouldBeAdded(resource)) {
        add(resource.listProperties())

        if (newCount > 0) {
            resource.listProperties().toList()
                .filter { it.isResourceProperty() }
                .forEach { recursiveAddNonConceptResource(it.resource, newCount) }
        }
    }

    return this
}

fun Statement.isResourceProperty(): Boolean =
    try {
        resource.isResource
    } catch (ex: ResourceRequiredException) {
        false
    }

fun calendarFromTimestamp(timestamp: Long): Calendar {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = timestamp
    return calendar
}

data class CollectionsAndConcepts(
    val resourceURI: String,
    val harvested: Model,
    val harvestedWithoutConcepts: Model,
    val concepts: List<ConceptRDFModel>,
)

data class ConceptRDFModel(
    val resourceURI: String,
    val harvested: Model
)

private fun Model.resourceShouldBeAdded(resource: Resource): Boolean {
    val types = resource.listProperties(RDF.type)
        .toList()
        .map { it.`object` }

    return when {
        types.contains(SKOS.Concept) -> false
        containsTriple("<${resource.uri}>", "a", "?o") -> false
        else -> true
    }
}

private fun Model.containsTriple(subj: String, pred: String, obj: String): Boolean {
    val askQuery = "ASK { $subj $pred $obj }"

    val query = QueryFactory.create(askQuery)
    return QueryExecutionFactory.create(query, this).execAsk()
}
