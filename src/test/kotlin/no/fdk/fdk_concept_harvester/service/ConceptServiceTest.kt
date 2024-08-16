package no.fdk.fdk_concept_harvester.service

import no.fdk.fdk_concept_harvester.model.ConceptMeta
import no.fdk.fdk_concept_harvester.model.DuplicateIRI
import no.fdk.fdk_concept_harvester.model.FdkIdAndUri
import no.fdk.fdk_concept_harvester.model.HarvestReport
import no.fdk.fdk_concept_harvester.rabbit.RabbitMQPublisher
import no.fdk.fdk_concept_harvester.repository.ConceptMetaRepository
import no.fdk.fdk_concept_harvester.utils.*
import org.apache.jena.riot.Lang
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.web.server.ResponseStatusException
import java.util.*

@Tag("unit")
class ConceptServiceTest {
    private val repository: ConceptMetaRepository = mock()
    private val publisher: RabbitMQPublisher = mock()
    private val turtleService: TurtleService = mock()
    private val conceptService = ConceptService(repository, publisher, turtleService)

    private val responseReader = TestResponseReader()

    @Nested
    internal inner class AllCollections {

        @Test
        fun responseIsomorphicWithEmptyModelForEmptyDB() {
            whenever(turtleService.getCollectionUnion(true))
                .thenReturn(null)

            val expected = responseReader.parseResponse("", "TURTLE")

            val response = conceptService.getAllCollections(Lang.TURTLE, true)

            assertTrue(expected.isIsomorphicWith(responseReader.parseResponse(response, "TURTLE")))
        }

        @Test
        fun responseIsIsomorphicWithExpectedModel() {
            whenever(turtleService.getCollectionUnion(true))
                .thenReturn(javaClass.classLoader.getResource("collection_0.ttl")!!.readText())
            whenever(turtleService.getCollectionUnion(false))
                .thenReturn(javaClass.classLoader.getResource("harvest_response_0.ttl")!!.readText())

            val expected = responseReader.parseFile("collection_0.ttl", "TURTLE")
            val expectedNoRecords = responseReader.parseFile("harvest_response_0.ttl", "TURTLE")

            val responseTurtle = conceptService.getAllCollections(Lang.TURTLE, true)
            val responseN3 = conceptService.getAllCollections(Lang.N3, true)
            val responseNTriples = conceptService.getAllCollections(Lang.NTRIPLES, false)

            assertTrue(expected.isIsomorphicWith(responseReader.parseResponse(responseTurtle, "TURTLE")))
            assertTrue(expected.isIsomorphicWith(responseReader.parseResponse(responseN3, "N3")))
            assertTrue(expectedNoRecords.isIsomorphicWith(responseReader.parseResponse(responseNTriples, "N-TRIPLES")))
        }

    }

    @Nested
    internal inner class CollectionById {

        @Test
        fun responseIsNullWhenNoCatalogIsFound() {
            whenever(turtleService.getCollection("123", false))
                .thenReturn(null)

            val response = conceptService.getCollectionById("123", Lang.TURTLE, false)

            assertNull(response)
        }

        @Test
        fun responseIsIsomorphicWithExpectedModel() {
            whenever(turtleService.getCollection(COLLECTION_0_ID, withRecords = true))
                .thenReturn(javaClass.classLoader.getResource("collection_0.ttl")!!.readText())
            whenever(turtleService.getCollection(COLLECTION_0_ID, withRecords = false))
                .thenReturn(javaClass.classLoader.getResource("harvest_response_0.ttl")!!.readText())

            val responseTurtle = conceptService.getCollectionById(COLLECTION_0_ID, Lang.TURTLE, false)
            val responseJsonRDF = conceptService.getCollectionById(COLLECTION_0_ID, Lang.RDFJSON, true)

            val expected = responseReader.parseFile("collection_0.ttl", "TURTLE")
            val expectedNoRecords = responseReader.parseFile("harvest_response_0.ttl", "TURTLE")

            assertTrue(expectedNoRecords.isIsomorphicWith(responseReader.parseResponse(responseTurtle!!, "TURTLE")))
            assertTrue(expected.isIsomorphicWith(responseReader.parseResponse(responseJsonRDF!!, "RDF/JSON")))
        }

    }

    @Nested
    internal inner class ConceptById {

        @Test
        fun responseIsNullWhenNoModelIsFound() {
            whenever(turtleService.getConcept("123", true))
                .thenReturn(null)

            val response = conceptService.getConceptById("123", Lang.TURTLE, true)

            assertNull(response)
        }

        @Test
        fun responseIsIsomorphicWithExpectedModel() {
            whenever(turtleService.getConcept(CONCEPT_0_ID, true))
                .thenReturn(javaClass.classLoader.getResource("concept_0.ttl")!!.readText())
            whenever(turtleService.getConcept(CONCEPT_0_ID, false))
                .thenReturn(javaClass.classLoader.getResource("no_meta_concept_0.ttl")!!.readText())

            val responseTurtle = conceptService.getConceptById(CONCEPT_0_ID, Lang.TURTLE, true)
            val responseRDFXML = conceptService.getConceptById(CONCEPT_0_ID, Lang.RDFXML, false)

            val expected = responseReader.parseFile("concept_0.ttl", "TURTLE")
            val expectedNoRecords = responseReader.parseFile("no_meta_concept_0.ttl", "TURTLE")

            assertTrue(expected.isIsomorphicWith(responseReader.parseResponse(responseTurtle!!, "TURTLE")))
            assertTrue(expectedNoRecords.isIsomorphicWith(responseReader.parseResponse(responseRDFXML!!, "RDF/XML")))
        }

    }

    @Nested
    internal inner class RemoveConceptById {

        @Test
        fun throwsResponseStatusExceptionWhenNoMetaFoundInDB() {
            whenever(repository.findAllByFdkId("123"))
                .thenReturn(emptyList())

            assertThrows<ResponseStatusException> { conceptService.removeConcept("123") }
        }

        @Test
        fun throwsExceptionWhenNoNonRemovedMetaFoundInDB() {
            whenever(repository.findAllByFdkId(CONCEPT_0_ID))
                .thenReturn(listOf(CONCEPT_0.copy(removed = true)))

            assertThrows<ResponseStatusException> { conceptService.removeConcept(CONCEPT_0_ID) }
        }

        @Test
        fun updatesMetaAndSendsRabbitReportWhenMetaIsFound() {
            whenever(repository.findAllByFdkId(CONCEPT_0_ID))
                .thenReturn(listOf(CONCEPT_0))

            conceptService.removeConcept(CONCEPT_0_ID)

            argumentCaptor<List<ConceptMeta>>().apply {
                verify(repository, times(1)).saveAll(capture())
                assertEquals(listOf(CONCEPT_0.copy(removed = true)), firstValue)
            }

            val expectedReport = HarvestReport(
                id = "manual-delete-$CONCEPT_0_ID",
                url = CONCEPT_0.uri,
                harvestError = false,
                startTime = "startTime",
                endTime = "endTime",
                removedResources = listOf(FdkIdAndUri(CONCEPT_0.fdkId, CONCEPT_0.uri))
            )
            argumentCaptor<List<HarvestReport>>().apply {
                verify(publisher, times(1)).send(capture())

                assertEquals(
                    listOf(expectedReport.copy(
                        startTime = firstValue.first().startTime,
                        endTime = firstValue.first().endTime
                    )),
                    firstValue
                )
            }
        }

    }

    @Nested
    internal inner class RemoveDuplicates {

        @Test
        fun throwsExceptionWhenRemoveIRINotFoundInDB() {
            whenever(repository.findById("https://123.no"))
                .thenReturn(Optional.empty())
            whenever(repository.findById(CONCEPT_1.uri))
                .thenReturn(Optional.of(CONCEPT_1))

            val duplicateIRI = DuplicateIRI(
                iriToRemove = "https://123.no",
                iriToRetain = CONCEPT_1.uri
            )
            assertThrows<ResponseStatusException> { conceptService.removeDuplicates(listOf(duplicateIRI)) }
        }

        @Test
        fun createsNewMetaWhenRetainIRINotFoundInDB() {
            whenever(repository.findById(CONCEPT_0.uri))
                .thenReturn(Optional.of(CONCEPT_0))
            whenever(repository.findById(CONCEPT_1.uri))
                .thenReturn(Optional.empty())

            val duplicateIRI = DuplicateIRI(
                iriToRemove = CONCEPT_0.uri,
                iriToRetain = CONCEPT_1.uri
            )
            conceptService.removeDuplicates(listOf(duplicateIRI))

            argumentCaptor<List<ConceptMeta>>().apply {
                verify(repository, times(1)).saveAll(capture())
                assertEquals(listOf(CONCEPT_0.copy(removed = true), CONCEPT_0.copy(uri = CONCEPT_1.uri)), firstValue)
            }

            verify(publisher, times(0)).send(any())
        }

        @Test
        fun sendsRabbitReportWithRetainFdkIdWhenKeepingRemoveFdkId() {
            whenever(repository.findById(CONCEPT_0.uri))
                .thenReturn(Optional.of(CONCEPT_0))
            whenever(repository.findById(CONCEPT_1.uri))
                .thenReturn(Optional.of(CONCEPT_1))

            val duplicateIRI = DuplicateIRI(
                iriToRemove = CONCEPT_0.uri,
                iriToRetain = CONCEPT_1.uri
            )
            conceptService.removeDuplicates(listOf(duplicateIRI))

            argumentCaptor<List<ConceptMeta>>().apply {
                verify(repository, times(1)).saveAll(capture())
                assertEquals(listOf(
                    CONCEPT_0.copy(removed = true),
                    CONCEPT_0.copy(uri = CONCEPT_1.uri, isPartOf = CONCEPT_1.isPartOf)
                ), firstValue)
            }

            val expectedReport = HarvestReport(
                id = "duplicate-delete",
                url = "https://fellesdatakatalog.digdir.no/duplicates",
                harvestError = false,
                startTime = "startTime",
                endTime = "endTime",
                removedResources = listOf(FdkIdAndUri(CONCEPT_1.fdkId, CONCEPT_1.uri))
            )
            argumentCaptor<List<HarvestReport>>().apply {
                verify(publisher, times(1)).send(capture())

                assertEquals(
                    listOf(expectedReport.copy(
                        startTime = firstValue.first().startTime,
                        endTime = firstValue.first().endTime
                    )),
                    firstValue
                )
            }
        }

        @Test
        fun sendsRabbitReportWithRemoveFdkIdWhenNotKeepingRemoveFdkId() {
            whenever(repository.findById(CONCEPT_0.uri))
                .thenReturn(Optional.of(CONCEPT_0))
            whenever(repository.findById(CONCEPT_1.uri))
                .thenReturn(Optional.of(CONCEPT_1))

            val duplicateIRI = DuplicateIRI(
                iriToRemove = CONCEPT_1.uri,
                iriToRetain = CONCEPT_0.uri,
                keepRemovedFdkId = false
            )
            conceptService.removeDuplicates(listOf(duplicateIRI))

            argumentCaptor<List<ConceptMeta>>().apply {
                verify(repository, times(1)).saveAll(capture())
                assertEquals(listOf(
                    CONCEPT_1.copy(removed = true),
                    CONCEPT_0
                ), firstValue)
            }

            val expectedReport = HarvestReport(
                id = "duplicate-delete",
                url = "https://fellesdatakatalog.digdir.no/duplicates",
                harvestError = false,
                startTime = "startTime",
                endTime = "endTime",
                removedResources = listOf(FdkIdAndUri(CONCEPT_1.fdkId, CONCEPT_1.uri))
            )
            argumentCaptor<List<HarvestReport>>().apply {
                verify(publisher, times(1)).send(capture())

                assertEquals(
                    listOf(expectedReport.copy(
                        startTime = firstValue.first().startTime,
                        endTime = firstValue.first().endTime
                    )),
                    firstValue
                )
            }
        }

        @Test
        fun throwsExceptionWhenTryingToReportAlreadyRemovedAsRemoved() {
            whenever(repository.findById(CONCEPT_0.uri))
                .thenReturn(Optional.of(CONCEPT_0.copy(removed = true)))
            whenever(repository.findById(CONCEPT_1.uri))
                .thenReturn(Optional.of(CONCEPT_1))

            val duplicateIRI = DuplicateIRI(
                iriToRemove = CONCEPT_0.uri,
                iriToRetain = CONCEPT_1.uri,
                keepRemovedFdkId = false
            )

            assertThrows<ResponseStatusException> { conceptService.removeDuplicates(listOf(duplicateIRI)) }

            whenever(repository.findById(CONCEPT_0.uri))
                .thenReturn(Optional.of(CONCEPT_0))
            whenever(repository.findById(CONCEPT_1.uri))
                .thenReturn(Optional.of(CONCEPT_1.copy(removed = true)))

            assertThrows<ResponseStatusException> { conceptService.removeDuplicates(listOf(duplicateIRI.copy(keepRemovedFdkId = true))) }
        }

    }

}
