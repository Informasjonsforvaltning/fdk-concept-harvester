package no.fdk.fdk_concept_harvester.service

import com.nhaarman.mockitokotlin2.*
import no.fdk.fdk_concept_harvester.configuration.ApplicationProperties
import no.fdk.fdk_concept_harvester.repository.CollectionMetaRepository
import no.fdk.fdk_concept_harvester.repository.ConceptMetaRepository
import no.fdk.fdk_concept_harvester.utils.*
import org.apache.jena.rdf.model.Model
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

@Tag("unit")
class UpdateServiceTest {
    private val collectionMetaRepository: CollectionMetaRepository = mock()
    private val conceptMetaRepository: ConceptMetaRepository = mock()
    private val valuesMock: ApplicationProperties = mock()
    private val turtleService: TurtleService = mock()
    private val updateService = UpdateService(
        valuesMock, collectionMetaRepository, conceptMetaRepository, turtleService)

    private val responseReader = TestResponseReader()

    @Nested
    internal inner class UpdateMetaData {

        @Test
        fun catalogRecordsIsRecreatedFromMetaDBO() {
            whenever(collectionMetaRepository.findAll())
                .thenReturn(listOf(COLLECTION_0))
            whenever(conceptMetaRepository.findAll())
                .thenReturn(listOf(CONCEPT_0, CONCEPT_1))
            whenever(conceptMetaRepository.findAllByIsPartOf("http://localhost:5000/collections/${COLLECTION_0_ID}"))
                .thenReturn(listOf(CONCEPT_0, CONCEPT_1))
            whenever(turtleService.getCollection(COLLECTION_0_ID, false))
                .thenReturn(responseReader.readFile("harvest_response_0.ttl"))
            whenever(turtleService.getConcept(CONCEPT_0_ID, false))
                .thenReturn(responseReader.readFile("no_meta_concept_0.ttl"))
            whenever(turtleService.getConcept(CONCEPT_1_ID, false))
                .thenReturn(responseReader.readFile("no_meta_concept_1.ttl"))

            whenever(valuesMock.collectionsUri)
                .thenReturn("http://localhost:5000/collections")
            whenever(valuesMock.conceptsUri)
                .thenReturn("http://localhost:5000/concepts")

            updateService.updateMetaData()

            val expectedCollection = responseReader.parseFile("collection_0.ttl", "TURTLE")
            val expectedConcept0 = responseReader.parseFile("concept_0.ttl", "TURTLE")
            val expectedConcept1 = responseReader.parseFile("concept_1.ttl", "TURTLE")

            argumentCaptor<Model, String, Boolean>().apply {
                verify(turtleService, times(1)).saveAsCollection(first.capture(), second.capture(), third.capture())
                assertTrue(checkIfIsomorphicAndPrintDiff(first.firstValue, expectedCollection, "catalogRecordsIsRecreatedFromMetaDBO-collection"))
                assertEquals(COLLECTION_0_ID, second.firstValue)
                assertEquals(listOf(true), third.allValues)
            }

            argumentCaptor<Model, String, Boolean>().apply {
                verify(turtleService, times(2)).saveAsConcept(first.capture(), second.capture(), third.capture())
                assertTrue(checkIfIsomorphicAndPrintDiff(first.firstValue, expectedConcept0, "catalogRecordsIsRecreatedFromMetaDBO-concept0"))
                assertTrue(checkIfIsomorphicAndPrintDiff(first.secondValue, expectedConcept1, "catalogRecordsIsRecreatedFromMetaDBO-concept1"))
                assertEquals(listOf(CONCEPT_0_ID, CONCEPT_1_ID), second.allValues)
                assertEquals(listOf(true, true), third.allValues)
            }
        }

        @Test
        fun conceptIsSkippedIfNotActuallyPresentInCollection() {
            whenever(collectionMetaRepository.findAll())
                .thenReturn(listOf(COLLECTION_0))
            whenever(conceptMetaRepository.findAllByIsPartOf("http://localhost:5000/collections/${COLLECTION_0_ID}"))
                .thenReturn(listOf(CONCEPT_0, CONCEPT_1))
            whenever(turtleService.getCollection(COLLECTION_0_ID, false))
                .thenReturn(responseReader.readFile("harvest_response_0_no_concepts.ttl"))
            whenever(turtleService.getConcept(CONCEPT_0_ID, false))
                .thenReturn(responseReader.readFile("no_meta_concept_0.ttl"))
            whenever(turtleService.getConcept(CONCEPT_1_ID, false))
                .thenReturn(responseReader.readFile("no_meta_concept_1.ttl"))

            whenever(valuesMock.collectionsUri)
                .thenReturn("http://localhost:5000/collections")
            whenever(valuesMock.conceptsUri)
                .thenReturn("http://localhost:5000/concepts")

            updateService.updateMetaData()

            val expectedCollection = responseReader.parseFile("collection_0_no_concepts.ttl", "TURTLE")

            argumentCaptor<Model, String, Boolean>().apply {
                verify(turtleService, times(1)).saveAsCollection(first.capture(), second.capture(), third.capture())
                assertTrue(checkIfIsomorphicAndPrintDiff(first.firstValue, expectedCollection, "conceptIsSkippedIfNotActuallyPresentInCollection"))
                assertEquals(COLLECTION_0_ID, second.firstValue)
                assertEquals(listOf(true), third.allValues)
            }

            argumentCaptor<Model, String, Boolean>().apply {
                verify(turtleService, times(0)).saveAsConcept(first.capture(), second.capture(), third.capture())
            }
        }

    }

    @Nested
    internal inner class UpdateUnionModel {

        @Test
        fun updateUnionModel() {
            whenever(collectionMetaRepository.findAll())
                .thenReturn(listOf(COLLECTION_0))

            whenever(turtleService.getCollection(COLLECTION_0_ID, true))
                .thenReturn(responseReader.readFile("collection_0.ttl"))
            whenever(turtleService.getCollection(COLLECTION_0_ID, false))
                .thenReturn(responseReader.readFile("harvest_response_0.ttl"))

            updateService.updateUnionModels()

            val collectionUnion = responseReader.parseFile("collection_0.ttl", "TURTLE")
            val collectionUnionNoRecords = responseReader.parseFile("harvest_response_0.ttl", "TURTLE")

            argumentCaptor<Model, Boolean>().apply {
                verify(turtleService, times(2)).saveAsCollectionUnion(first.capture(), second.capture())
                assertTrue(checkIfIsomorphicAndPrintDiff(first.firstValue, collectionUnion, "updateUnionModel-collection"))
                assertTrue(checkIfIsomorphicAndPrintDiff(first.secondValue, collectionUnionNoRecords, "updateUnionModel-collection-norecords"))
                assertEquals(listOf(true, false), second.allValues)
            }
        }
    }
}