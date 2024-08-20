package no.fdk.fdk_concept_harvester.service

import no.fdk.fdk_concept_harvester.harvester.formatNowWithOsloTimeZone
import no.fdk.fdk_concept_harvester.model.DuplicateIRI
import no.fdk.fdk_concept_harvester.model.FdkIdAndUri
import no.fdk.fdk_concept_harvester.model.HarvestReport
import no.fdk.fdk_concept_harvester.rabbit.RabbitMQPublisher
import no.fdk.fdk_concept_harvester.rdf.createRDFResponse
import no.fdk.fdk_concept_harvester.rdf.parseRDFResponse
import no.fdk.fdk_concept_harvester.repository.ConceptMetaRepository
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.riot.Lang
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException

@Service
class ConceptService(
    private val conceptMetaRepository: ConceptMetaRepository,
    private val rabbitPublisher: RabbitMQPublisher,
    private val turtleService: TurtleService
) {

    fun getAllCollections(returnType: Lang, withRecords: Boolean): String =
        turtleService.getCollectionUnion(withRecords)
            ?.let {
                if (returnType == Lang.TURTLE) it
                else parseRDFResponse(it, Lang.TURTLE).createRDFResponse(returnType)
            }
            ?: ModelFactory.createDefaultModel().createRDFResponse(returnType)

    fun getCollectionById(id: String, returnType: Lang, withRecords: Boolean): String? =
        turtleService.getCollection(id, withRecords)
            ?.let {
                if (returnType == Lang.TURTLE) it
                else parseRDFResponse(it, Lang.TURTLE).createRDFResponse(returnType)
            }

    fun getConceptById(id: String, returnType: Lang, withRecords: Boolean): String? =
        turtleService.getConcept(id, withRecords)
            ?.let {
                if (returnType == Lang.TURTLE) it
                else parseRDFResponse(it, Lang.TURTLE).createRDFResponse(returnType)
            }

    fun removeConcept(id: String) {
        val start = formatNowWithOsloTimeZone()
        val meta = conceptMetaRepository.findAllByFdkId(id)
        if (meta.isEmpty()) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "No concept found with fdkID $id")
        } else if (meta.none { !it.removed }) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Concept with fdkID $id has already been removed")
        } else {
            conceptMetaRepository.saveAll(meta.map { it.copy(removed = true) })

            val uri = meta.first().uri
            rabbitPublisher.send(listOf(
                HarvestReport(
                    id = "manual-delete-$id",
                    url = uri,
                    harvestError = false,
                    startTime = start,
                    endTime = formatNowWithOsloTimeZone(),
                    removedResources = listOf(FdkIdAndUri(fdkId = id, uri = uri))
                )
            ))
        }
    }

    fun removeDuplicates(duplicates: List<DuplicateIRI>) {
        val start = formatNowWithOsloTimeZone()
        val reportAsRemoved: MutableList<FdkIdAndUri> = mutableListOf()

        duplicates.flatMap { duplicate ->
            val remove = conceptMetaRepository.findByIdOrNull(duplicate.iriToRemove)
                ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "No concept connected to IRI ${duplicate.iriToRemove}")

            val retain = conceptMetaRepository.findByIdOrNull(duplicate.iriToRetain)
                ?.let { if (it.issued > remove.issued) it.copy(issued = remove.issued) else it } // keep earliest issued
                ?.let { if (it.modified < remove.modified) it.copy(modified = remove.modified) else it } // keep latest modified
                ?.let {
                    if (duplicate.keepRemovedFdkId) {
                        if (it.removed) throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Concept with IRI ${it.uri} has already been removed")
                        reportAsRemoved.add(FdkIdAndUri(fdkId = it.fdkId, uri = it.uri))
                        it.copy(fdkId = remove.fdkId)
                    } else {
                        if (remove.removed) throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Concept with IRI ${remove.uri} has already been removed")
                        reportAsRemoved.add(FdkIdAndUri(fdkId = remove.fdkId, uri = remove.uri))
                        it
                    }
                }
                ?: remove.copy(uri = duplicate.iriToRetain)

            listOf(remove.copy(removed = true), retain.copy(removed = false))
        }.run { conceptMetaRepository.saveAll(this) }

        if (reportAsRemoved.isNotEmpty()) {
            rabbitPublisher.send(listOf(
                HarvestReport(
                    id = "duplicate-delete",
                    url = "https://fellesdatakatalog.digdir.no/duplicates",
                    harvestError = false,
                    startTime = start,
                    endTime = formatNowWithOsloTimeZone(),
                    removedResources = reportAsRemoved
                )
            ))
        }
    }

    // Purges everything associated with a removed fdkID
    fun purgeByFdkId(fdkId: String) {
        conceptMetaRepository.findAllByFdkId(fdkId)
            .also { concepts -> if (concepts.any { !it.removed }) throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Unable to purge files, concept with id $fdkId has not been removed") }
            .run { conceptMetaRepository.deleteAll(this) }

        turtleService.deleteTurtleFiles(fdkId)
    }

}
