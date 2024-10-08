package no.fdk.fdk_concept_harvester.utils

import no.fdk.fdk_concept_harvester.model.CollectionMeta
import no.fdk.fdk_concept_harvester.model.ConceptMeta
import no.fdk.fdk_concept_harvester.model.TurtleDBO
import no.fdk.fdk_concept_harvester.service.UNION_ID
import no.fdk.fdk_concept_harvester.service.gzip
import org.bson.Document

private val responseReader = TestResponseReader()

val COLLECTION_0 = CollectionMeta(
    uri = "https://www.example.com/begrepskatalog/0", fdkId = "9b8f1c42-1161-33b1-9d43-a733ee94ddfc",
    issued = 1609852539831, modified = 1609852539831,
    concepts = setOf("https://example.com/begrep/0", "https://example.com/begrep/1")
)

val CONCEPT_0 = ConceptMeta(
    uri = "https://example.com/begrep/0", fdkId = "db1b701c-b4b9-3c20-bc23-236a91236754",
    isPartOf = "http://localhost:5050/collections/9b8f1c42-1161-33b1-9d43-a733ee94ddfc",
    issued = 1609852539831, modified = 1609852539831
)

val CONCEPT_1 = ConceptMeta(
    uri = "https://example.com/begrep/1", fdkId = "7dbac738-4944-323a-a777-ad2f83bf75f8",
    isPartOf = "http://localhost:5050/collections/9b8f1c42-1161-33b1-9d43-a733ee94ddfc",
    issued = 1609852539831, modified = 1609852539831
)

val REMOVED_CONCEPT = ConceptMeta(
    uri = "https://example.com/begrep/removed", fdkId = "removed",
    isPartOf = "http://localhost:5050/collections/9b8f1c42-1161-33b1-9d43-a733ee94ddfc",
    removed = true, issued = 1609852539831, modified = 1609852539831
)

val CONCEPT_2 = ConceptMeta(
    uri = "https://example.com/begrep/2", fdkId = "no-collection",
    isPartOf = null,
    issued = 1609852539831, modified = 1609852539831
)

val CONCEPT_TURTLE_0_META = TurtleDBO(
    id = "concept-$CONCEPT_0_ID",
    turtle = gzip(responseReader.readFile("concept_0.ttl"))
)

val CONCEPT_TURTLE_0_NO_META = TurtleDBO(
    id = "concept-no-records-$CONCEPT_0_ID",
    turtle = gzip(responseReader.readFile("no_meta_concept_0.ttl"))
)

val CONCEPT_TURTLE_1_META = TurtleDBO(
    id = "concept-$CONCEPT_1_ID",
    turtle = gzip(responseReader.readFile("concept_1.ttl"))
)

val CONCEPT_TURTLE_1_NO_META = TurtleDBO(
    id = "concept-no-records-$CONCEPT_1_ID",
    turtle = gzip(responseReader.readFile("no_meta_concept_1.ttl"))
)

val REMOVED_CONCEPT_TURTLE_META = TurtleDBO(
    id = "concept-removed",
    turtle = gzip(responseReader.readFile("concept_1.ttl"))
)

val REMOVED_CONCEPT_TURTLE_NO_META = TurtleDBO(
    id = "concept-no-records-removed",
    turtle = gzip(responseReader.readFile("no_meta_concept_1.ttl"))
)

val CONCEPT_TURTLE_2_META = TurtleDBO(
    id = "concept-no-collection",
    turtle = gzip(responseReader.readFile("concept_2.ttl"))
)

val CONCEPT_TURTLE_2_NO_META = TurtleDBO(
    id = "concept-no-records-no-collection",
    turtle = gzip(responseReader.readFile("no_meta_concept_2.ttl"))
)

val COLLECTION_TURTLE_META = TurtleDBO(
    id = "collection-$COLLECTION_0_ID",
    turtle = gzip(responseReader.readFile("collection_0.ttl"))
)

val COLLECTION_TURTLE_NO_META = TurtleDBO(
    id = "collection-no-records-$COLLECTION_0_ID",
    turtle = gzip(responseReader.readFile("no_meta_collection_0.ttl"))
)

val COLLECTION_UNION_TURTLE = TurtleDBO(
    id = "collection-$UNION_ID",
    turtle = gzip(responseReader.readFile("collection_0.ttl"))
)

val COLLECTION_UNION_TURTLE_NO_RECORDS = TurtleDBO(
    id = "collection-no-records-$UNION_ID",
    turtle = gzip(responseReader.readFile("no_meta_collection_0.ttl"))
)

val HARVESTED_TURTLE = TurtleDBO(
    id = TEST_HARVEST_SOURCE_0.url!!,
    turtle = gzip(responseReader.readFile("harvest_response_0.ttl"))
)

fun turleDBPopulation(): List<Document> =
    listOf(
        COLLECTION_UNION_TURTLE, HARVESTED_TURTLE, CONCEPT_TURTLE_0_META,
        CONCEPT_TURTLE_0_NO_META, CONCEPT_TURTLE_1_META, CONCEPT_TURTLE_1_NO_META, COLLECTION_TURTLE_META,
        COLLECTION_TURTLE_NO_META, CONCEPT_TURTLE_2_META, CONCEPT_TURTLE_2_NO_META,
        COLLECTION_UNION_TURTLE_NO_RECORDS, REMOVED_CONCEPT_TURTLE_META, REMOVED_CONCEPT_TURTLE_NO_META
    )
        .map { it.mapDBO() }

fun conceptDBPopulation(): List<Document> =
    listOf(CONCEPT_0, CONCEPT_1, CONCEPT_2, REMOVED_CONCEPT)
        .map { it.mapDBO() }

fun collectionDBPopulation(): List<Document> =
    listOf(COLLECTION_0)
        .map { it.mapDBO() }

private fun CollectionMeta.mapDBO(): Document =
    Document()
        .append("_id", uri)
        .append("fdkId", fdkId)
        .append("issued", issued)
        .append("modified", modified)
        .append("concepts", concepts)

private fun ConceptMeta.mapDBO(): Document =
    Document()
        .append("_id", uri)
        .append("fdkId", fdkId)
        .append("isPartOf", isPartOf)
        .append("removed", removed)
        .append("issued", issued)
        .append("modified", modified)

private fun TurtleDBO.mapDBO(): Document =
    Document()
        .append("_id", id)
        .append("turtle", turtle)
