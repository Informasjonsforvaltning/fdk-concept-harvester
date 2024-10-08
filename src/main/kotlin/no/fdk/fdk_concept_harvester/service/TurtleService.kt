package no.fdk.fdk_concept_harvester.service

import no.fdk.fdk_concept_harvester.model.TurtleDBO
import no.fdk.fdk_concept_harvester.rdf.createRDFResponse
import no.fdk.fdk_concept_harvester.repository.TurtleRepository
import org.apache.jena.rdf.model.Model
import org.apache.jena.riot.Lang
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.io.ByteArrayOutputStream
import java.util.*
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import kotlin.text.Charsets.UTF_8

private const val NO_RECORDS_ID_PREFIX = "no-records-"
const val UNION_ID = "union-graph"
private const val COLLECTION_ID_PREFIX = "collection-"
private const val CONCEPT_ID_PREFIX = "concept-"

@Service
class TurtleService(private val turtleRepository: TurtleRepository) {

    fun saveAsCollectionUnion(model: Model, withRecords: Boolean): TurtleDBO =
        turtleRepository.save(model.createCollectionTurtleDBO(UNION_ID, withRecords))

    fun getCollectionUnion(withRecords: Boolean): String? =
        turtleRepository.findByIdOrNull(collectionTurtleID(UNION_ID, withRecords))
            ?.turtle
            ?.let { ungzip(it) }

    fun saveAsCollection(model: Model, fdkId: String, withRecords: Boolean): TurtleDBO =
        turtleRepository.save(model.createCollectionTurtleDBO(fdkId, withRecords))

    fun getCollection(fdkId: String, withRecords: Boolean): String? =
        turtleRepository.findByIdOrNull(collectionTurtleID(fdkId, withRecords))
            ?.turtle
            ?.let { ungzip(it) }

    fun saveAsConcept(model: Model, fdkId: String, withRecords: Boolean): TurtleDBO =
        turtleRepository.save(model.createConceptTurtleDBO(fdkId, withRecords))

    fun getConcept(fdkId: String, withRecords: Boolean): String? =
        turtleRepository.findByIdOrNull(conceptTurtleID(fdkId, withRecords))
            ?.turtle
            ?.let { ungzip(it) }

    fun saveAsHarvestSource(model: Model, uri: String): TurtleDBO =
        turtleRepository.save(model.createHarvestSourceTurtleDBO(uri))

    fun getHarvestSource(uri: String): String? =
        turtleRepository.findByIdOrNull(uri)
            ?.turtle
            ?.let { ungzip(it) }

    fun deleteTurtleFiles(fdkId: String) {
        turtleRepository.findAllById(
            listOf(
                conceptTurtleID(fdkId, true),
                conceptTurtleID(fdkId, false)
            )
        ).run { turtleRepository.deleteAll(this) }
    }

}

private fun collectionTurtleID(fdkId: String, withFDKRecords: Boolean): String =
    "$COLLECTION_ID_PREFIX${if (withFDKRecords) "" else NO_RECORDS_ID_PREFIX}$fdkId"

private fun conceptTurtleID(fdkId: String, withFDKRecords: Boolean): String =
    "$CONCEPT_ID_PREFIX${if (withFDKRecords) "" else NO_RECORDS_ID_PREFIX}$fdkId"

private fun Model.createHarvestSourceTurtleDBO(uri: String): TurtleDBO =
    TurtleDBO(
        id = uri,
        turtle = gzip(createRDFResponse(Lang.TURTLE))
    )

private fun Model.createCollectionTurtleDBO(fdkId: String, withRecords: Boolean): TurtleDBO =
    TurtleDBO(
        id = collectionTurtleID(fdkId, withRecords),
        turtle = gzip(createRDFResponse(Lang.TURTLE))
    )

private fun Model.createConceptTurtleDBO(fdkId: String, withRecords: Boolean): TurtleDBO =
    TurtleDBO(
        id = conceptTurtleID(fdkId, withRecords),
        turtle = gzip(createRDFResponse(Lang.TURTLE))
    )

fun gzip(content: String): String {
    val bos = ByteArrayOutputStream()
    GZIPOutputStream(bos).bufferedWriter(UTF_8).use { it.write(content) }
    return Base64.getEncoder().encodeToString(bos.toByteArray())
}

fun ungzip(base64Content: String): String {
    val content = Base64.getDecoder().decode(base64Content)
    return GZIPInputStream(content.inputStream())
        .bufferedReader(UTF_8)
        .use { it.readText() }
}
