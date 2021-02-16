package no.fdk.fdk_concept_harvester.harvester

import com.nhaarman.mockitokotlin2.*
import no.fdk.fdk_concept_harvester.adapter.ConceptsAdapter
import no.fdk.fdk_concept_harvester.configuration.ApplicationProperties
import no.fdk.fdk_concept_harvester.model.CollectionMeta
import no.fdk.fdk_concept_harvester.model.ConceptMeta
import no.fdk.fdk_concept_harvester.repository.CollectionMetaRepository
import no.fdk.fdk_concept_harvester.repository.ConceptMetaRepository
import no.fdk.fdk_concept_harvester.service.TurtleService
import no.fdk.fdk_concept_harvester.utils.*
import org.apache.jena.rdf.model.Model
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.util.*
import kotlin.test.assertEquals

@Tag("unit")
class HarvesterTest {
    private val collectionRepository: CollectionMetaRepository = mock()
    private val conceptRepository: ConceptMetaRepository = mock()
    private val turtleService: TurtleService = mock()
    private val valuesMock: ApplicationProperties = mock()
    private val adapter: ConceptsAdapter = mock()

    private val harvester = ConceptHarvester(adapter, collectionRepository, conceptRepository, turtleService, valuesMock)
    private val responseReader = TestResponseReader()

    @Test
    fun harvestDataSourceSavedWhenDBIsEmpty() {
        whenever(adapter.getConcepts(TEST_HARVEST_SOURCE_0))
            .thenReturn(responseReader.readFile("harvest_response_0.ttl"))
        whenever(conceptRepository.findAllByIsPartOf("http://localhost:5000/collections/$COLLECTION_0_ID"))
            .thenReturn(listOf(CONCEPT_0, CONCEPT_1))

        whenever(turtleService.getConcept(CONCEPT_0_ID, true))
            .thenReturn(responseReader.readFile("concept_0.ttl"))
        whenever(turtleService.getConcept(CONCEPT_1_ID, true))
            .thenReturn(responseReader.readFile("concept_1.ttl"))

        whenever(valuesMock.collectionsUri)
            .thenReturn("http://localhost:5000/collections")
        whenever(valuesMock.conceptsUri)
            .thenReturn("http://localhost:5000/concepts")

        harvester.harvestConceptCollection(TEST_HARVEST_SOURCE_0, TEST_HARVEST_DATE)

        argumentCaptor<Model, String>().apply {
            verify(turtleService, times(1)).saveAsHarvestSource(first.capture(), second.capture())
            assertTrue(first.firstValue.isIsomorphicWith(responseReader.parseFile("harvest_response_0.ttl", "TURTLE")))
            Assertions.assertEquals(TEST_HARVEST_SOURCE_0.url, second.firstValue)
        }

        argumentCaptor<Model, String, Boolean>().apply {
            verify(turtleService, times(2)).saveAsCollection(first.capture(), second.capture(), third.capture())
            assertTrue(checkIfIsomorphicAndPrintDiff(first.allValues[0], responseReader.parseFile("harvest_response_0.ttl", "TURTLE"), "harvestDataSourceSavedWhenDBIsEmpty-norecords-collection0"))
            assertTrue(checkIfIsomorphicAndPrintDiff(first.allValues[1], responseReader.parseFile("collection_0.ttl", "TURTLE"), "harvestDataSourceSavedWhenDBIsEmpty-collection0"))
            assertEquals(listOf(COLLECTION_0_ID, COLLECTION_0_ID), second.allValues)
            Assertions.assertEquals(listOf(false, true), third.allValues)
        }

        argumentCaptor<CollectionMeta>().apply {
            verify(collectionRepository, times(1)).save(capture())
            assertEquals(COLLECTION_0, firstValue)
        }

        argumentCaptor<Model, String, Boolean>().apply {
            verify(turtleService, times(4)).saveAsConcept(first.capture(), second.capture(), third.capture())
            assertTrue(checkIfIsomorphicAndPrintDiff(first.allValues[0], responseReader.parseFile("no_meta_concept_1.ttl", "TURTLE"), "harvestDataSourceSavedWhenDBIsEmpty-norecords-concept1"))
            assertTrue(checkIfIsomorphicAndPrintDiff(first.allValues[1], responseReader.parseFile("concept_1.ttl", "TURTLE"), "harvestDataSourceSavedWhenDBIsEmpty-concept1"))
            assertTrue(checkIfIsomorphicAndPrintDiff(first.allValues[2], responseReader.parseFile("no_meta_concept_0.ttl", "TURTLE"), "harvestDataSourceSavedWhenDBIsEmpty-norecords-concept0"))
            assertTrue(checkIfIsomorphicAndPrintDiff(first.allValues[3], responseReader.parseFile("concept_0.ttl", "TURTLE"), "harvestDataSourceSavedWhenDBIsEmpty-concept0"))
            assertEquals(listOf(CONCEPT_1_ID, CONCEPT_1_ID, CONCEPT_0_ID, CONCEPT_0_ID), second.allValues)
            Assertions.assertEquals(listOf(false, true, false, true), third.allValues)
        }

        argumentCaptor<ConceptMeta>().apply {
            verify(conceptRepository, times(2)).save(capture())
            assertEquals(listOf(CONCEPT_0, CONCEPT_1), allValues.sortedBy { it.uri })
        }
    }

    @Test
    fun harvestDataSourceNotPersistedWhenNoChangesFromDB() {
        val harvested = responseReader.readFile("harvest_response_0.ttl")
        whenever(adapter.getConcepts(TEST_HARVEST_SOURCE_0))
            .thenReturn(harvested)
        whenever(turtleService.getHarvestSource(TEST_HARVEST_SOURCE_0.url!!))
            .thenReturn(harvested)

        harvester.harvestConceptCollection(TEST_HARVEST_SOURCE_0, TEST_HARVEST_DATE)

        argumentCaptor<Model, String>().apply {
            verify(turtleService, times(0)).saveAsHarvestSource(first.capture(), second.capture())
        }
        argumentCaptor<Model, String, Boolean>().apply {
            verify(turtleService, times(0)).saveAsCollection(first.capture(), second.capture(), third.capture())
        }
        argumentCaptor<Model, String, Boolean>().apply {
            verify(turtleService, times(0)).saveAsConcept(first.capture(), second.capture(), third.capture())
        }

        argumentCaptor<CollectionMeta>().apply {
            verify(collectionRepository, times(0)).save(capture())
        }
        argumentCaptor<ConceptMeta>().apply {
            verify(conceptRepository, times(0)).save(capture())
        }
    }

    @Test
    fun onlyRelevantUpdatedWhenHarvestedFromDB() {
        whenever(adapter.getConcepts(TEST_HARVEST_SOURCE_0))
            .thenReturn(responseReader.readFile("harvest_response_0.ttl"))
        whenever(turtleService.getHarvestSource(TEST_HARVEST_SOURCE_0.url!!))
            .thenReturn(responseReader.readFile("harvest_response_0_diff.ttl"))
        whenever(conceptRepository.findAllByIsPartOf("http://localhost:5000/collections/$COLLECTION_0_ID"))
            .thenReturn(listOf(CONCEPT_0.copy(modified = NEW_TEST_HARVEST_DATE.timeInMillis), CONCEPT_1))

        whenever(valuesMock.collectionsUri)
            .thenReturn("http://localhost:5000/collections")
        whenever(valuesMock.conceptsUri)
            .thenReturn("http://localhost:5000/concepts")

        whenever(collectionRepository.findById(COLLECTION_0.uri))
            .thenReturn(Optional.of(COLLECTION_0))
        whenever(conceptRepository.findById(CONCEPT_0.uri))
            .thenReturn(Optional.of(CONCEPT_0))
        whenever(conceptRepository.findById(CONCEPT_1.uri))
            .thenReturn(Optional.of(CONCEPT_1))

        whenever(turtleService.getConcept(CONCEPT_0_ID, true))
            .thenReturn(responseReader.readFile("concept_0_updated.ttl"))
        whenever(turtleService.getConcept(CONCEPT_1_ID, true))
            .thenReturn(responseReader.readFile("concept_1.ttl"))

        whenever(turtleService.getCollection(COLLECTION_0_ID, false))
            .thenReturn(responseReader.readFile("harvest_response_0_diff.ttl"))
        whenever(turtleService.getConcept(CONCEPT_0_ID, false))
            .thenReturn(responseReader.readFile("no_meta_concept_0_diff.ttl"))
        whenever(turtleService.getConcept(CONCEPT_1_ID, false))
            .thenReturn(responseReader.readFile("no_meta_concept_1.ttl"))

        harvester.harvestConceptCollection(TEST_HARVEST_SOURCE_0, NEW_TEST_HARVEST_DATE)

        argumentCaptor<Model, String>().apply {
            verify(turtleService, times(1)).saveAsHarvestSource(first.capture(), second.capture())
            assertTrue(first.firstValue.isIsomorphicWith(responseReader.parseFile("harvest_response_0.ttl", "TURTLE")))
            Assertions.assertEquals(TEST_HARVEST_SOURCE_0.url, second.firstValue)
        }

        argumentCaptor<CollectionMeta>().apply {
            verify(collectionRepository, times(1)).save(capture())
            assertEquals(COLLECTION_0.copy(modified = NEW_TEST_HARVEST_DATE.timeInMillis), firstValue)
        }

        argumentCaptor<ConceptMeta>().apply {
            verify(conceptRepository, times(1)).save(capture())
            assertEquals(CONCEPT_0.copy(modified = NEW_TEST_HARVEST_DATE.timeInMillis), firstValue)
        }

        argumentCaptor<Model, String, Boolean>().apply {
            verify(turtleService, times(2)).saveAsCollection(first.capture(), second.capture(), third.capture())
            assertTrue(checkIfIsomorphicAndPrintDiff(first.allValues[0], responseReader.parseFile("harvest_response_0.ttl", "TURTLE"), "onlyRelevantUpdatedWhenHarvestedFromDB-norecords-collection0"))
            assertTrue(checkIfIsomorphicAndPrintDiff(first.allValues[1], responseReader.parseFile("collection_0_updated.ttl", "TURTLE"), "onlyRelevantUpdatedWhenHarvestedFromDB-collection0"))
            assertEquals(listOf(COLLECTION_0_ID, COLLECTION_0_ID), second.allValues)
            Assertions.assertEquals(listOf(false, true), third.allValues)
        }

        argumentCaptor<Model, String, Boolean>().apply {
            verify(turtleService, times(2)).saveAsConcept(first.capture(), second.capture(), third.capture())
            assertTrue(checkIfIsomorphicAndPrintDiff(first.allValues[0], responseReader.parseFile("no_meta_concept_0.ttl", "TURTLE"), "onlyRelevantUpdatedWhenHarvestedFromDB-norecords-concept0"))
            assertTrue(checkIfIsomorphicAndPrintDiff(first.allValues[1], responseReader.parseFile("concept_0_updated.ttl", "TURTLE"), "onlyRelevantUpdatedWhenHarvestedFromDB-concept0"))
            assertEquals(listOf(CONCEPT_0_ID, CONCEPT_0_ID), second.allValues)
            Assertions.assertEquals(listOf(false, true), third.allValues)
        }

    }

    @Test
    fun harvestWithErrorsIsNotPersisted() {
        whenever(adapter.getConcepts(TEST_HARVEST_SOURCE_0))
            .thenReturn(responseReader.readFile("harvest_error_response.ttl"))

        harvester.harvestConceptCollection(TEST_HARVEST_SOURCE_0, TEST_HARVEST_DATE)

        argumentCaptor<Model, String>().apply {
            verify(turtleService, times(0)).saveAsHarvestSource(first.capture(), second.capture())
        }
        argumentCaptor<Model, String, Boolean>().apply {
            verify(turtleService, times(0)).saveAsCollection(first.capture(), second.capture(), third.capture())
        }
        argumentCaptor<Model, String, Boolean>().apply {
            verify(turtleService, times(0)).saveAsConcept(first.capture(), second.capture(), third.capture())
        }

        argumentCaptor<CollectionMeta>().apply {
            verify(collectionRepository, times(0)).save(capture())
        }
        argumentCaptor<ConceptMeta>().apply {
            verify(conceptRepository, times(0)).save(capture())
        }
    }

}