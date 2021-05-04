package no.fdk.fdk_concept_harvester.harvester

import no.fdk.fdk_concept_harvester.model.HarvestDataSource
import no.fdk.fdk_concept_harvester.model.Organization
import no.fdk.fdk_concept_harvester.rdf.*
import org.apache.jena.query.QueryExecutionFactory
import org.apache.jena.query.QueryFactory
import org.apache.jena.rdf.model.*
import org.apache.jena.riot.Lang
import org.apache.jena.vocabulary.*
import java.util.*


fun CollectionRDFModel.harvestDiff(dboNoRecords: String?): Boolean =
    if (dboNoRecords == null) true
    else !harvested.isIsomorphicWith(parseRDFResponse(dboNoRecords, Lang.TURTLE, null))

fun ConceptRDFModel.harvestDiff(dboNoRecords: String?): Boolean =
    if (dboNoRecords == null) true
    else !harvested.isIsomorphicWith(parseRDFResponse(dboNoRecords, Lang.TURTLE, null))

fun splitCollectionsFromRDF(
    harvested: Model,
    allConcepts: List<ConceptRDFModel>,
    sourceURL: String,
    organization: Organization?
): List<CollectionRDFModel> {
    val harvestedCollections = harvested.listResourcesWithProperty(RDF.type, SKOS.Collection)
        .toList()
        .filter { it.hasProperty(SKOS.member) }
        .map { collectionResource ->
            val collectionConcepts: Set<String> = collectionResource.listProperties(SKOS.member)
                .toList()
                .map { dataset -> dataset.resource.uri }
                .toSet()

            val collectionModelWithoutConcepts = collectionResource.extractCollectionModel()

            var collectionModel = collectionModelWithoutConcepts
            allConcepts.filter { collectionConcepts.contains(it.resourceURI) }
                .forEach { collectionModel = collectionModel.union(it.harvested) }

            CollectionRDFModel(
                resourceURI = collectionResource.uri,
                harvestedWithoutConcepts = collectionModelWithoutConcepts,
                harvested = collectionModel,
                concepts = collectionConcepts
            )
        }

    val conceptsNotMemberOfCollection = allConcepts.filterNot { it.isMemberOfAnyCollection }

    return if (conceptsNotMemberOfCollection.isEmpty()) harvestedCollections
    else harvestedCollections.plus(generatedCollection(conceptsNotMemberOfCollection, sourceURL, organization))
}

fun splitConceptsFromRDF(harvested: Model): List<ConceptRDFModel> =
    harvested.listResourcesWithProperty(RDF.type, SKOS.Concept)
        .toList()
        .map { conceptResource -> conceptResource.extractConcept() }

fun Resource.extractCollectionModel(): Model {
    var collectionModelWithoutConcepts = listProperties().toModel()
    collectionModelWithoutConcepts.setNsPrefixes(model.nsPrefixMap)

    listProperties().toList()
        .filter { it.isResourceProperty() }
        .forEach {
            if (it.predicate != SKOS.member) {
                collectionModelWithoutConcepts =
                    collectionModelWithoutConcepts.recursiveAddNonConceptResource(it.resource, 5)
            }
        }

    return collectionModelWithoutConcepts
}

fun Resource.extractConcept(): ConceptRDFModel {
    var conceptModel = listProperties().toModel()
    conceptModel = conceptModel.setNsPrefixes(model.nsPrefixMap)

    listProperties().toList()
        .filter { it.isResourceProperty() }
        .forEach { conceptModel = conceptModel.recursiveAddNonConceptResource(it.resource, 10) }

    return ConceptRDFModel(
        resourceURI = uri,
        harvested = conceptModel,
        isMemberOfAnyCollection = isMemberOfAnyCollection()
    )
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

private fun generatedCollection(
    concepts: List<ConceptRDFModel>,
    sourceURL: String,
    organization: Organization?
): CollectionRDFModel {
    val conceptURIs = concepts.map { it.resourceURI }.toSet()
    val generatedCollectionURI = "$sourceURL#GeneratedCollection"
    val collectionModelWithoutConcepts = createModelForHarvestSourceCollection(generatedCollectionURI, conceptURIs, organization)

    var collectionModel = collectionModelWithoutConcepts
    concepts.forEach { collectionModel = collectionModel.union(it.harvested) }

    return CollectionRDFModel(
        resourceURI = generatedCollectionURI,
        harvestedWithoutConcepts = collectionModelWithoutConcepts,
        harvested = collectionModel,
        concepts = conceptURIs
    )
}

private fun createModelForHarvestSourceCollection(
    collectionURI: String,
    concepts: Set<String>,
    organization: Organization?
): Model {
    val collectionModel = ModelFactory.createDefaultModel()
    collectionModel.createResource(collectionURI)
        .addProperty(RDF.type, SKOS.Collection)
        .addPublisherForGeneratedCollection(organization?.uri)
        .addLabelForGeneratedCollection(organization)
        .addMembersForGeneratedCollection(concepts)

    return collectionModel
}

private fun Resource.addPublisherForGeneratedCollection(publisherURI: String?): Resource {
    if (publisherURI != null) {
        addProperty(
            DCTerms.publisher,
            ResourceFactory.createResource(publisherURI)
        )
    }

    return this
}

private fun Resource.addLabelForGeneratedCollection(organization: Organization?): Resource {
    val nb: String? = organization?.prefLabel?.nb ?: organization?.name
    if (!nb.isNullOrBlank()) {
        val label = model.createLiteral("$nb - Begrepssamling", "nb")
        addProperty(RDFS.label, label)
    }

    val nn: String? = organization?.prefLabel?.nn ?: organization?.name
    if (!nb.isNullOrBlank()) {
        val label = model.createLiteral("$nn - Begrepssamling", "nn")
        addProperty(RDFS.label, label)
    }

    val en: String? = organization?.prefLabel?.en ?: organization?.name
    if (!en.isNullOrBlank()) {
        val label = model.createLiteral("$en - Concept collection", "en")
        addProperty(RDFS.label, label)
    }

    return this
}

private fun Resource.addMembersForGeneratedCollection(concepts: Set<String>): Resource {
    concepts.forEach { addProperty(SKOS.member, model.createResource(it)) }
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

data class CollectionRDFModel(
    val resourceURI: String,
    val harvested: Model,
    val harvestedWithoutConcepts: Model,
    val concepts: Set<String>,
)

data class ConceptRDFModel(
    val resourceURI: String,
    val harvested: Model,
    val isMemberOfAnyCollection: Boolean
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

private fun Resource.isMemberOfAnyCollection(): Boolean {
    val askQuery = """ASK {
        ?collection a <${SKOS.Collection.uri}> .
        ?collection <${SKOS.member.uri}> <$uri> .
    }""".trimMargin()

    val query = QueryFactory.create(askQuery)
    return QueryExecutionFactory.create(query, model).execAsk()
}

fun List<ConceptRDFModel>.containsFreeConcepts(): Boolean =
    firstOrNull { !it.isMemberOfAnyCollection } != null

