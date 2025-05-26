package no.fdk.fdk_concept_harvester.model

import no.fdk.fdk_concept_harvester.rdf.safeParseRDF
import no.fdk.fdk_concept_harvester.service.ungzip
import org.apache.jena.riot.Lang
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = "conceptMeta")
data class ConceptMeta(
    @Id
    val uri: String,

    val fdkId: String,

    val isPartOf: String? = null,
    val removed: Boolean = false,
    val issued: Long,
    val modified: Long
)

@Document(collection = "collectionMeta")
data class CollectionMeta(
    @Id
    val uri: String,

    val fdkId: String,

    val concepts: Set<String>,
    val issued: Long,
    val modified: Long
)

@Document(collection = "harvestSourceTurtle")
data class HarvestSourceTurtle(
    @Id override val id: String,
    override val turtle: String
) : TurtleDBO()

@Document(collection = "collectionTurtle")
data class CollectionTurtle(
    @Id override val id: String,
    override val turtle: String
) : TurtleDBO()

@Document(collection = "fdkCollectionTurtle")
data class FDKCollectionTurtle(
    @Id override val id: String,
    override val turtle: String
) : TurtleDBO()

@Document(collection = "conceptTurtle")
data class ConceptTurtle(
    @Id override val id: String,
    override val turtle: String
) : TurtleDBO()

@Document(collection = "fdkConceptTurtle")
data class FDKConceptTurtle(
    @Id override val id: String,
    override val turtle: String
) : TurtleDBO()

abstract class TurtleDBO {
    abstract val id: String
    abstract val turtle: String

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
    val model0 = safeParseRDF(ungzip(zip0), Lang.TURTLE)
    val model1 = safeParseRDF(ungzip(zip1), Lang.TURTLE)

    return model0.isIsomorphicWith(model1)
}
