package no.fdk.fdk_concept_harvester.harvester

import no.fdk.fdk_concept_harvester.adapter.ConceptsAdapter
import no.fdk.fdk_concept_harvester.adapter.DefaultOrganizationsAdapter
import no.fdk.fdk_concept_harvester.configuration.ApplicationProperties
import no.fdk.fdk_concept_harvester.model.CollectionMeta
import no.fdk.fdk_concept_harvester.model.ConceptMeta
import no.fdk.fdk_concept_harvester.model.FdkIdAndUri
import no.fdk.fdk_concept_harvester.model.HarvestReport
import no.fdk.fdk_concept_harvester.repository.CollectionMetaRepository
import no.fdk.fdk_concept_harvester.repository.ConceptMetaRepository
import no.fdk.fdk_concept_harvester.service.TurtleService
import no.fdk.fdk_concept_harvester.utils.*
import org.apache.jena.rdf.model.Model
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.*
import kotlin.test.assertEquals

@Tag("unit")
class HarvesterTest {
    private val collectionRepository: CollectionMetaRepository = mock()
    private val conceptRepository: ConceptMetaRepository = mock()
    private val turtleService: TurtleService = mock()
    private val valuesMock: ApplicationProperties = mock()
    private val adapter: ConceptsAdapter = mock()
    private val orgAdapter: DefaultOrganizationsAdapter = mock()

    private val harvester = ConceptHarvester(adapter, orgAdapter, collectionRepository, conceptRepository, turtleService, valuesMock)
    private val responseReader = TestResponseReader()

    @Test
    fun harvestDataSourceSavedWhenDBIsEmpty() {
        whenever(adapter.getConcepts(TEST_HARVEST_SOURCE_0))
            .thenReturn(responseReader.readFile("harvest_response_0.ttl"))
        whenever(conceptRepository.findAllByIsPartOf("http://localhost:5000/collections/$COLLECTION_0_ID"))
            .thenReturn(listOf(CONCEPT_0, CONCEPT_1))
        whenever(conceptRepository.findById(CONCEPT_0.uri))
            .thenReturn(Optional.of(CONCEPT_0.copy(isPartOf = null)))
        whenever(conceptRepository.findById(CONCEPT_1.uri))
            .thenReturn(Optional.of(CONCEPT_1.copy(isPartOf = null)))

        whenever(turtleService.getConcept(CONCEPT_0_ID, true))
            .thenReturn(responseReader.readFile("concept_0.ttl"))
        whenever(turtleService.getConcept(CONCEPT_1_ID, true))
            .thenReturn(responseReader.readFile("concept_1.ttl"))

        whenever(valuesMock.collectionsUri)
            .thenReturn("http://localhost:5000/collections")
        whenever(valuesMock.conceptsUri)
            .thenReturn("http://localhost:5000/concepts")

        val report = harvester.harvestConceptCollection(TEST_HARVEST_SOURCE_0, TEST_HARVEST_DATE, false)

        val expectedReport = HarvestReport(
            id = "concept-harvest-source-0",
            url = "http://localhost:5000/concept-harvest-source-0",
            harvestError = false,
            startTime = "2021-01-05 14:15:39 +0100",
            endTime = report!!.endTime,
            changedResources = listOf(
                FdkIdAndUri(fdkId="db1b701c-b4b9-3c20-bc23-236a91236754", uri="https://example.com/begrep/0"),
                FdkIdAndUri(fdkId="7dbac738-4944-323a-a777-ad2f83bf75f8", uri="https://example.com/begrep/1")),
            changedCatalogs = listOf(
                FdkIdAndUri(fdkId="9b8f1c42-1161-33b1-9d43-a733ee94ddfc", uri="https://www.example.com/begrepskatalog/0"),
                FdkIdAndUri(fdkId= GENERATED_COLLECTION_ID, uri= GENERATED_COLLECTION.uri))
        )
        assertEquals(expectedReport, report)

        argumentCaptor<Model, String>().apply {
            verify(turtleService, times(1)).saveAsHarvestSource(first.capture(), second.capture())
            assertTrue(first.firstValue.isIsomorphicWith(responseReader.parseFile("harvest_response_0.ttl", "TURTLE")))
            Assertions.assertEquals(TEST_HARVEST_SOURCE_0.url, second.firstValue)
        }

        argumentCaptor<Model, String, Boolean>().apply {
            verify(turtleService, times(2)).saveAsCollection(first.capture(), second.capture(), third.capture())
            assertTrue(checkIfIsomorphicAndPrintDiff(first.firstValue, responseReader.parseFile("no_meta_collection_0.ttl", "TURTLE"), "harvestDataSourceSavedWhenDBIsEmpty-norecords-collection0"))
            assertEquals(listOf(COLLECTION_0_ID, GENERATED_COLLECTION_ID), second.allValues)
            Assertions.assertEquals(listOf(false, false), third.allValues)
        }

        argumentCaptor<CollectionMeta>().apply {
            verify(collectionRepository, times(2)).save(capture())
            assertEquals(COLLECTION_0, firstValue)
            assertEquals(GENERATED_COLLECTION.copy(concepts = emptySet()), secondValue)
        }

        argumentCaptor<Model, String, Boolean>().apply {
            verify(turtleService, times(2)).saveAsConcept(first.capture(), second.capture(), third.capture())
            assertTrue(checkIfIsomorphicAndPrintDiff(first.allValues[0], responseReader.parseFile("no_meta_concept_0.ttl", "TURTLE"), "harvestDataSourceSavedWhenDBIsEmpty-norecords-concept0"))
            assertTrue(checkIfIsomorphicAndPrintDiff(first.allValues[1], responseReader.parseFile("no_meta_concept_1.ttl", "TURTLE"), "harvestDataSourceSavedWhenDBIsEmpty-norecords-concept1"))
            assertEquals(listOf(CONCEPT_0_ID, CONCEPT_1_ID), second.allValues)
            Assertions.assertEquals(listOf(false, false), third.allValues)
        }

        argumentCaptor<ConceptMeta>().apply {
            verify(conceptRepository, times(4)).save(capture())
            assertEquals(listOf(CONCEPT_0.copy(isPartOf = null), CONCEPT_1.copy(isPartOf = null), CONCEPT_1, CONCEPT_0), allValues)
        }
    }

    @Test
    fun harvestDataSourceNotPersistedWhenNoChangesFromDB() {
        val harvested = responseReader.readFile("harvest_response_0.ttl")
        whenever(adapter.getConcepts(TEST_HARVEST_SOURCE_0))
            .thenReturn(harvested)
        whenever(turtleService.getHarvestSource(TEST_HARVEST_SOURCE_0.url!!))
            .thenReturn(harvested)

        val report = harvester.harvestConceptCollection(TEST_HARVEST_SOURCE_0, TEST_HARVEST_DATE, false)

        val expectedReport = HarvestReport(
            id = "concept-harvest-source-0",
            url = "http://localhost:5000/concept-harvest-source-0",
            harvestError = false,
            startTime = "2021-01-05 14:15:39 +0100",
            endTime = report!!.endTime
        )
        assertEquals(expectedReport, report)

        verify(turtleService, times(0)).saveAsHarvestSource(any(), any())
        verify(turtleService, times(0)).saveAsCollection(any(), any(), any())
        verify(turtleService, times(0)).saveAsConcept(any(), any(), any())
        verify(collectionRepository, times(0)).save(any())
        verify(conceptRepository, times(0)).save(any())
    }

    @Test
    fun noChangesIgnoredWhenForceUpdateIsTrue() {
        val harvested = responseReader.readFile("harvest_response_0.ttl")
        whenever(adapter.getConcepts(TEST_HARVEST_SOURCE_0))
            .thenReturn(harvested)
        whenever(turtleService.getHarvestSource(TEST_HARVEST_SOURCE_0.url!!))
            .thenReturn(harvested)
        whenever(conceptRepository.findById(CONCEPT_0.uri))
            .thenReturn(Optional.of(CONCEPT_0))
        whenever(conceptRepository.findById(CONCEPT_1.uri))
            .thenReturn(Optional.of(CONCEPT_1))
        whenever(collectionRepository.findById(COLLECTION_0.uri))
            .thenReturn(Optional.of(COLLECTION_0))

        whenever(turtleService.getConcept(CONCEPT_0_ID, false))
            .thenReturn(responseReader.readFile("no_meta_concept_0.ttl"))
        whenever(turtleService.getConcept(CONCEPT_1_ID, false))
            .thenReturn(responseReader.readFile("no_meta_concept_1.ttl"))
        whenever(turtleService.getCollection(COLLECTION_0_ID, false))
            .thenReturn(responseReader.readFile("no_meta_collection_0.ttl"))

        whenever(valuesMock.collectionsUri)
            .thenReturn("http://localhost:5000/collections")
        whenever(valuesMock.conceptsUri)
            .thenReturn("http://localhost:5000/concepts")

        val report = harvester.harvestConceptCollection(TEST_HARVEST_SOURCE_0, TEST_HARVEST_DATE, true)

        val expectedReport = HarvestReport(
            id = "concept-harvest-source-0",
            url = "http://localhost:5000/concept-harvest-source-0",
            harvestError = false,
            startTime = "2021-01-05 14:15:39 +0100",
            endTime = report!!.endTime,
            changedResources = listOf(
                FdkIdAndUri(fdkId="db1b701c-b4b9-3c20-bc23-236a91236754", uri="https://example.com/begrep/0"),
                FdkIdAndUri(fdkId="7dbac738-4944-323a-a777-ad2f83bf75f8", uri="https://example.com/begrep/1")),
            changedCatalogs = listOf(
                FdkIdAndUri(fdkId="9b8f1c42-1161-33b1-9d43-a733ee94ddfc", uri="https://www.example.com/begrepskatalog/0"),
                FdkIdAndUri(fdkId= GENERATED_COLLECTION_ID, uri= GENERATED_COLLECTION.uri))
        )
        assertEquals(expectedReport, report)

        verify(turtleService, times(1)).saveAsHarvestSource(any(), any())
        verify(turtleService, times(2)).saveAsCollection(any(), any(), any())
        verify(turtleService, times(2)).saveAsConcept(any(), any(), any())
        verify(collectionRepository, times(1)).save(any())
        verify(conceptRepository, times(0)).save(any())
    }

    @Test
    fun onlyRelevantUpdatedWhenHarvestedFromDB() {
        whenever(adapter.getConcepts(TEST_HARVEST_SOURCE_0))
            .thenReturn(responseReader.readFile("harvest_response_0.ttl"))
        whenever(turtleService.getHarvestSource(TEST_HARVEST_SOURCE_0.url!!))
            .thenReturn(responseReader.readFile("harvest_response_0_diff.ttl"))

        whenever(valuesMock.collectionsUri)
            .thenReturn("http://localhost:5000/collections")
        whenever(valuesMock.conceptsUri)
            .thenReturn("http://localhost:5000/concepts")

        whenever(collectionRepository.findById(COLLECTION_0.uri))
            .thenReturn(Optional.of(COLLECTION_0))
        whenever(collectionRepository.findById(GENERATED_COLLECTION.uri))
            .thenReturn(Optional.of(GENERATED_COLLECTION.copy(concepts = emptySet())))
        whenever(conceptRepository.findById(CONCEPT_0.uri))
            .thenReturn(Optional.of(CONCEPT_0.copy(modified = NEW_TEST_HARVEST_DATE.timeInMillis, isPartOf = null)))
        whenever(conceptRepository.findById(CONCEPT_1.uri))
            .thenReturn(Optional.of(CONCEPT_1))

        whenever(turtleService.getConcept(CONCEPT_0_ID, true))
            .thenReturn(responseReader.readFile("concept_0.ttl"))
        whenever(turtleService.getConcept(CONCEPT_1_ID, true))
            .thenReturn(responseReader.readFile("concept_1.ttl"))

        whenever(turtleService.getCollection(COLLECTION_0_ID, false))
            .thenReturn(responseReader.readFile("harvest_response_0_diff.ttl"))
        whenever(turtleService.getCollection(GENERATED_COLLECTION_ID, false))
            .thenReturn(responseReader.readFile("empty_generated_collection.ttl"))
        whenever(turtleService.getConcept(CONCEPT_0_ID, false))
            .thenReturn(responseReader.readFile("no_meta_concept_0_diff.ttl"))
        whenever(turtleService.getConcept(CONCEPT_1_ID, false))
            .thenReturn(responseReader.readFile("no_meta_concept_1.ttl"))

        val report = harvester.harvestConceptCollection(TEST_HARVEST_SOURCE_0, NEW_TEST_HARVEST_DATE, false)

        val expectedReport = HarvestReport(
            id = "concept-harvest-source-0",
            url = "http://localhost:5000/concept-harvest-source-0",
            harvestError = false,
            startTime = "2021-02-15 12:52:16 +0100",
            endTime = report!!.endTime,
            changedCatalogs = listOf(FdkIdAndUri(fdkId="9b8f1c42-1161-33b1-9d43-a733ee94ddfc", uri="https://www.example.com/begrepskatalog/0")),
            changedResources = listOf(FdkIdAndUri(fdkId="db1b701c-b4b9-3c20-bc23-236a91236754", uri="https://example.com/begrep/0"))
        )
        assertEquals(expectedReport, report)

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
            verify(conceptRepository, times(2)).save(capture())
            assertEquals(CONCEPT_0.copy(modified = NEW_TEST_HARVEST_DATE.timeInMillis, isPartOf = null), firstValue)
            assertEquals(CONCEPT_0.copy(modified = NEW_TEST_HARVEST_DATE.timeInMillis), secondValue)
        }

        argumentCaptor<Model, String, Boolean>().apply {
            verify(turtleService, times(1)).saveAsCollection(first.capture(), second.capture(), third.capture())
            assertTrue(checkIfIsomorphicAndPrintDiff(first.allValues[0], responseReader.parseFile("no_meta_collection_0.ttl", "TURTLE"), "onlyRelevantUpdatedWhenHarvestedFromDB-norecords-collection0"))
            assertEquals(listOf(COLLECTION_0_ID), second.allValues)
            Assertions.assertEquals(listOf(false), third.allValues)
        }

        argumentCaptor<Model, String, Boolean>().apply {
            verify(turtleService, times(1)).saveAsConcept(first.capture(), second.capture(), third.capture())
            assertTrue(checkIfIsomorphicAndPrintDiff(first.allValues[0], responseReader.parseFile("no_meta_concept_0.ttl", "TURTLE"), "onlyRelevantUpdatedWhenHarvestedFromDB-norecords-concept0"))
            assertEquals(listOf(CONCEPT_0_ID), second.allValues)
            Assertions.assertEquals(listOf(false), third.allValues)
        }

    }

    @Test
    fun harvestWithErrorsIsNotPersisted() {
        whenever(adapter.getConcepts(TEST_HARVEST_SOURCE_0))
            .thenReturn(responseReader.readFile("harvest_error_response.ttl"))

        val report = harvester.harvestConceptCollection(TEST_HARVEST_SOURCE_0, TEST_HARVEST_DATE, false)

        val expectedReport = HarvestReport(
            id = "concept-harvest-source-0",
            url = "http://localhost:5000/concept-harvest-source-0",
            harvestError = true,
            errorMessage = "[line: 11, col: 5 ] Undefined prefix: rdfs",
            startTime = "2021-01-05 14:15:39 +0100",
            endTime = report!!.endTime
        )
        assertEquals(expectedReport, report)

        verify(turtleService, times(0)).saveAsHarvestSource(any(), any())
        verify(turtleService, times(0)).saveAsCollection(any(), any(), any())
        verify(turtleService, times(0)).saveAsConcept(any(), any(), any())
        verify(collectionRepository, times(0)).save(any())
        verify(conceptRepository, times(0)).save(any())
    }

    @Test
    fun conceptsWithNoCollectionIsAddedToHarvestSourceCollection() {
        whenever(adapter.getConcepts(TEST_HARVEST_SOURCE_0))
            .thenReturn(responseReader.readFile("no_meta_concept_0.ttl"))
        whenever(conceptRepository.findById(CONCEPT_0.uri))
            .thenReturn(Optional.of(CONCEPT_0.copy(isPartOf = null)))

        whenever(turtleService.getConcept(CONCEPT_0_ID, true))
            .thenReturn(responseReader.readFile("concept_0.ttl"))

        whenever(orgAdapter.getOrganization("123456789")).thenReturn(ORGANIZATION_0)

        whenever(valuesMock.collectionsUri)
            .thenReturn("http://localhost:5000/collections")
        whenever(valuesMock.conceptsUri)
            .thenReturn("http://localhost:5000/concepts")

        val report = harvester.harvestConceptCollection(TEST_HARVEST_SOURCE_0, TEST_HARVEST_DATE, false)

        val expectedReport = HarvestReport(
            id = "concept-harvest-source-0",
            url = "http://localhost:5000/concept-harvest-source-0",
            harvestError = false,
            startTime = "2021-01-05 14:15:39 +0100",
            endTime = report!!.endTime,
            changedCatalogs=listOf(FdkIdAndUri(fdkId="24a90ee1-bd80-390b-8cfc-983960909392", uri="http://localhost:5000/concept-harvest-source-0#GeneratedCollection")),
            changedResources = listOf(FdkIdAndUri(fdkId="db1b701c-b4b9-3c20-bc23-236a91236754", uri="https://example.com/begrep/0"))
        )
        assertEquals(expectedReport, report)

        argumentCaptor<Model, String>().apply {
            verify(turtleService, times(1)).saveAsHarvestSource(first.capture(), second.capture())
            assertTrue(first.firstValue.isIsomorphicWith(responseReader.parseFile("no_meta_concept_0.ttl", "TURTLE")))
            Assertions.assertEquals(TEST_HARVEST_SOURCE_0.url, second.firstValue)
        }

        argumentCaptor<Model, String, Boolean>().apply {
            verify(turtleService, times(1)).saveAsCollection(first.capture(), second.capture(), third.capture())
            assertTrue(checkIfIsomorphicAndPrintDiff(first.firstValue, responseReader.parseFile("no_meta_generated_collection.ttl", "TURTLE"), "conceptsWithNoCollectionIsAddedToHarvestSourceCollection-collection"))
            assertEquals(listOf(GENERATED_COLLECTION_ID), second.allValues)
            Assertions.assertEquals(listOf(false), third.allValues)
        }

        argumentCaptor<CollectionMeta>().apply {
            verify(collectionRepository, times(1)).save(capture())
            assertEquals(GENERATED_COLLECTION, firstValue)
        }

        argumentCaptor<Model, String, Boolean>().apply {
            verify(turtleService, times(1)).saveAsConcept(first.capture(), second.capture(), third.capture())
            assertTrue(checkIfIsomorphicAndPrintDiff(first.allValues[0], responseReader.parseFile("no_meta_concept_0.ttl", "TURTLE"), "conceptsWithNoCollectionIsAddedToHarvestSourceCollection-concept"))
            assertEquals(listOf(CONCEPT_0_ID), second.allValues)
            Assertions.assertEquals(listOf(false), third.allValues)
        }

        argumentCaptor<ConceptMeta>().apply {
            verify(conceptRepository, times(2)).save(capture())
            assertEquals(listOf(CONCEPT_0.copy(isPartOf = null), CONCEPT_0.copy(isPartOf = "http://localhost:5000/collections/$GENERATED_COLLECTION_ID")), allValues)
        }
    }

    @Test
    fun harvestReportContainsRemovedConcepts() {
        val prev = responseReader.readFile("harvest_response_0.ttl")
        val harvested = responseReader.readFile("harvest_response_0_missing_concept1.ttl")
        whenever(adapter.getConcepts(TEST_HARVEST_SOURCE_0))
            .thenReturn(harvested)
        whenever(turtleService.getHarvestSource(TEST_HARVEST_SOURCE_0.url!!))
            .thenReturn(prev)
        whenever(collectionRepository.findById(GENERATED_COLLECTION.uri))
            .thenReturn(Optional.of(GENERATED_COLLECTION.copy(concepts = emptySet())))
        whenever(turtleService.getCollection(GENERATED_COLLECTION_ID, false))
            .thenReturn(responseReader.readFile("empty_generated_collection.ttl"))
        whenever(conceptRepository.findAllByIsPartOf("http://localhost:5000/collections/$COLLECTION_0_ID"))
            .thenReturn(listOf(CONCEPT_0, CONCEPT_1))
        whenever(conceptRepository.findById(CONCEPT_0.uri))
            .thenReturn(Optional.of(CONCEPT_0))
        whenever(turtleService.getConcept(CONCEPT_0_ID, false))
            .thenReturn(responseReader.readFile("no_meta_concept_0.ttl"))

        whenever(valuesMock.collectionsUri)
            .thenReturn("http://localhost:5000/collections")
        whenever(valuesMock.conceptsUri)
            .thenReturn("http://localhost:5000/concepts")

        val report = harvester.harvestConceptCollection(TEST_HARVEST_SOURCE_0, TEST_HARVEST_DATE, false)

        val expectedReport = HarvestReport(
            id = "concept-harvest-source-0",
            url = TEST_HARVEST_SOURCE_0.url!!,
            harvestError = false,
            startTime = "2021-01-05 14:15:39 +0100",
            endTime = report!!.endTime,
            changedCatalogs=listOf(FdkIdAndUri(fdkId=COLLECTION_0_ID, uri="https://www.example.com/begrepskatalog/0")),
            removedResources = listOf(FdkIdAndUri(fdkId=CONCEPT_1.fdkId, uri=CONCEPT_1.uri))
        )
        assertEquals(expectedReport, report)

        verify(turtleService, times(1)).saveAsHarvestSource(any(), any())
        verify(turtleService, times(1)).saveAsCollection(any(), any(), any())
        verify(turtleService, times(0)).saveAsConcept(any(), any(), any())
        verify(collectionRepository, times(1)).save(any())
        verify(conceptRepository, times(0)).save(any())
    }

}