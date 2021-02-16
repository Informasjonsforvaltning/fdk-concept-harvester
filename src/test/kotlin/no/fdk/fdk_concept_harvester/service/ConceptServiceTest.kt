package no.fdk.fdk_concept_harvester.service

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import no.fdk.fdk_concept_harvester.utils.*
import org.apache.jena.riot.Lang
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

@Tag("unit")
class ConceptServiceTest {
    private val turtleService: TurtleService = mock()
    private val conceptService = ConceptService(turtleService)

    private val responseReader = TestResponseReader()

    @Nested
    internal inner class AllCollections {

        @Test
        fun responseIsomorphicWithEmptyModelForEmptyDB() {
            whenever(turtleService.getUnion())
                .thenReturn(null)

            val expected = responseReader.parseResponse("", "TURTLE")

            val response = conceptService.getAllCollections(Lang.TURTLE)

            assertTrue(expected.isIsomorphicWith(responseReader.parseResponse(response, "TURTLE")))
        }

        @Test
        fun responseIsIsomorphicWithExpectedModel() {
            whenever(turtleService.getUnion())
                .thenReturn(javaClass.classLoader.getResource("collection_0.ttl")!!.readText())

            val expected = responseReader.parseFile("collection_0.ttl", "TURTLE")

            val responseTurtle = conceptService.getAllCollections(Lang.TURTLE)
            val responseN3 = conceptService.getAllCollections(Lang.N3)
            val responseNTriples = conceptService.getAllCollections(Lang.NTRIPLES)

            assertTrue(expected.isIsomorphicWith(responseReader.parseResponse(responseTurtle, "TURTLE")))
            assertTrue(expected.isIsomorphicWith(responseReader.parseResponse(responseN3, "N3")))
            assertTrue(expected.isIsomorphicWith(responseReader.parseResponse(responseNTriples, "N-TRIPLES")))
        }

    }

    @Nested
    internal inner class CollectionById {

        @Test
        fun responseIsNullWhenNoCatalogIsFound() {
            whenever(turtleService.getCollection("123", true))
                .thenReturn(null)

            val response = conceptService.getCollectionById("123", Lang.TURTLE)

            assertNull(response)
        }

        @Test
        fun responseIsIsomorphicWithExpectedModel() {
            whenever(turtleService.getCollection(COLLECTION_0_ID, withRecords = true))
                .thenReturn(javaClass.classLoader.getResource("collection_0.ttl")!!.readText())

            val responseTurtle = conceptService.getCollectionById(COLLECTION_0_ID, Lang.TURTLE)
            val responseJsonRDF = conceptService.getCollectionById(COLLECTION_0_ID, Lang.RDFJSON)

            val expected = responseReader.parseFile("collection_0.ttl", "TURTLE")

            assertTrue(expected.isIsomorphicWith(responseReader.parseResponse(responseTurtle!!, "TURTLE")))
            assertTrue(expected.isIsomorphicWith(responseReader.parseResponse(responseJsonRDF!!, "RDF/JSON")))
        }

    }

    @Nested
    internal inner class InformationModelById {

        @Test
        fun responseIsNullWhenNoModelIsFound() {
            whenever(turtleService.getConcept("123", true))
                .thenReturn(null)

            val response = conceptService.getConceptById("123", Lang.TURTLE)

            assertNull(response)
        }

        @Test
        fun responseIsIsomorphicWithExpectedModel() {
            whenever(turtleService.getConcept(CONCEPT_0_ID, true))
                .thenReturn(javaClass.classLoader.getResource("concept_0.ttl")!!.readText())

            val responseTurtle = conceptService.getConceptById(CONCEPT_0_ID, Lang.TURTLE)
            val responseRDFXML = conceptService.getConceptById(CONCEPT_0_ID, Lang.RDFXML)

            val expected = responseReader.parseFile("concept_0.ttl", "TURTLE")

            assertTrue(expected.isIsomorphicWith(responseReader.parseResponse(responseTurtle!!, "TURTLE")))
            assertTrue(expected.isIsomorphicWith(responseReader.parseResponse(responseRDFXML!!, "RDF/XML")))
        }

    }
}