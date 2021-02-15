package no.fdk.fdk_concept_harvester.model

import no.fdk.fdk_concept_harvester.rdf.parseRDFResponse
import no.fdk.fdk_concept_harvester.service.ungzip
import org.apache.jena.riot.Lang
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = "conceptMeta")
data class ConceptMeta(
    @Id
    val uri: String,

    @Indexed(unique = true)
    val fdkId: String,

    val isPartOf: String,
    val issued: Long,
    val modified: Long
)

@Document(collection = "collectionMeta")
data class CollectionMeta(
    @Id
    val uri: String,

    @Indexed(unique = true)
    val fdkId: String,

    val issued: Long,
    val modified: Long
)

@Document(collection = "turtle")
data class TurtleDBO(
    @Id
    val id: String,
    val turtle: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TurtleDBO

        return when {
            id != other.id -> false
            else -> zippedModelsAreIsomorphic(turtle, other.turtle)
        }
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + turtle.hashCode()
        return result
    }
}

private fun zippedModelsAreIsomorphic(zip0: String, zip1: String): Boolean {
    val model0 = parseRDFResponse(ungzip(zip0), Lang.TURTLE, null)
    val model1 = parseRDFResponse(ungzip(zip1), Lang.TURTLE, null)

    return when {
        model0 != null && model1 != null -> model0.isIsomorphicWith(model1)
        model0 == null && model1 == null -> true
        else -> false
    }
}
