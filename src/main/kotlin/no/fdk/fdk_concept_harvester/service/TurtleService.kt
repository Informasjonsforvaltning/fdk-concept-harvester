package no.fdk.fdk_concept_harvester.service

import no.fdk.fdk_concept_harvester.model.CollectionTurtle
import no.fdk.fdk_concept_harvester.model.ConceptTurtle
import no.fdk.fdk_concept_harvester.model.FDKCollectionTurtle
import no.fdk.fdk_concept_harvester.model.FDKConceptTurtle
import no.fdk.fdk_concept_harvester.model.HarvestSourceTurtle
import no.fdk.fdk_concept_harvester.model.TurtleDBO
import no.fdk.fdk_concept_harvester.rdf.createRDFResponse
import no.fdk.fdk_concept_harvester.repository.CollectionTurtleRepository
import no.fdk.fdk_concept_harvester.repository.ConceptTurtleRepository
import no.fdk.fdk_concept_harvester.repository.FDKCollectionTurtleRepository
import no.fdk.fdk_concept_harvester.repository.FDKConceptTurtleRepository
import no.fdk.fdk_concept_harvester.repository.HarvestSourceTurtleRepository
import org.apache.jena.rdf.model.Model
import org.apache.jena.riot.Lang
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.io.ByteArrayOutputStream
import java.util.*
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import kotlin.text.Charsets.UTF_8

@Service
class TurtleService(
    private val collectionTurtleRepository: CollectionTurtleRepository,
    private val conceptTurtleRepository: ConceptTurtleRepository,
    private val fdkCollectionTurtleRepository: FDKCollectionTurtleRepository,
    private val fdkConceptTurtleRepository: FDKConceptTurtleRepository,
    private val harvestSourceTurtleRepository: HarvestSourceTurtleRepository
) {

    fun saveAsCollection(model: Model, fdkId: String, withRecords: Boolean): TurtleDBO =
        if (withRecords) fdkCollectionTurtleRepository.save(model.createFDKCollectionTurtleDBO(fdkId))
        else collectionTurtleRepository.save(model.createCollectionTurtleDBO(fdkId))

    fun getCollection(fdkId: String, withRecords: Boolean): String? =
        if (withRecords) fdkCollectionTurtleRepository.findByIdOrNull(fdkId)
            ?.turtle
            ?.let { ungzip(it) }
        else collectionTurtleRepository.findByIdOrNull(fdkId)
            ?.turtle
            ?.let { ungzip(it) }

    fun saveAsConcept(model: Model, fdkId: String, withRecords: Boolean): TurtleDBO =
        if (withRecords) fdkConceptTurtleRepository.save(model.createFDKConceptTurtleDBO(fdkId))
        else conceptTurtleRepository.save(model.createConceptTurtleDBO(fdkId))

    fun getConcept(fdkId: String, withRecords: Boolean): String? =
        if (withRecords) fdkConceptTurtleRepository.findByIdOrNull(fdkId)
            ?.turtle
            ?.let { ungzip(it) }
        else conceptTurtleRepository.findByIdOrNull(fdkId)
            ?.turtle
            ?.let { ungzip(it) }

    fun saveAsHarvestSource(model: Model, uri: String): TurtleDBO =
        harvestSourceTurtleRepository.save(model.createHarvestSourceTurtleDBO(uri))

    fun getHarvestSource(uri: String): String? =
        harvestSourceTurtleRepository.findByIdOrNull(uri)
            ?.turtle
            ?.let { ungzip(it) }

    fun deleteTurtleFiles(fdkId: String) {
        conceptTurtleRepository.deleteById(fdkId)
        fdkConceptTurtleRepository.deleteById(fdkId)
    }

}

private fun Model.createHarvestSourceTurtleDBO(uri: String): HarvestSourceTurtle =
    HarvestSourceTurtle(
        id = uri,
        turtle = gzip(createRDFResponse(Lang.TURTLE))
    )

private fun Model.createCollectionTurtleDBO(fdkId: String): CollectionTurtle =
    CollectionTurtle(
        id = fdkId,
        turtle = gzip(createRDFResponse(Lang.TURTLE))
    )

private fun Model.createConceptTurtleDBO(fdkId: String): ConceptTurtle =
    ConceptTurtle(
        id = fdkId,
        turtle = gzip(createRDFResponse(Lang.TURTLE))
    )

private fun Model.createFDKCollectionTurtleDBO(fdkId: String): FDKCollectionTurtle =
    FDKCollectionTurtle(
        id = fdkId,
        turtle = gzip(createRDFResponse(Lang.TURTLE))
    )

private fun Model.createFDKConceptTurtleDBO(fdkId: String): FDKConceptTurtle =
    FDKConceptTurtle(
        id = fdkId,
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
