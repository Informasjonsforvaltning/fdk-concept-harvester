package no.fdk.fdk_concept_harvester.contract

import no.fdk.fdk_concept_harvester.utils.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertTrue
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.test.context.ContextConfiguration

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(
    properties = ["spring.profiles.active=contract-test"],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(initializers = [ApiTestContext.Initializer::class])
@Tag("contract")
class CollectionsTest: ApiTestContext() {
    private val responseReader = TestResponseReader()

    @Test
    fun findSpecificWithRecords() {
        val response = apiGet(port, "/collections/$COLLECTION_0_ID?catalogrecords=true", "application/rdf+xml")
        Assumptions.assumeTrue(HttpStatus.OK.value() == response["status"])

        val expected = responseReader.parseFile("collection_0.ttl", "TURTLE")
        val responseModel = responseReader.parseResponse(response["body"] as String, "RDFXML")

        assertTrue(expected.isIsomorphicWith(responseModel))
    }

    @Test
    fun findSpecificNoRecords() {
        val response = apiGet(port, "/collections/$COLLECTION_0_ID", "application/rdf+xml")
        Assumptions.assumeTrue(HttpStatus.OK.value() == response["status"])

        val expected = responseReader.parseFile("harvest_response_0.ttl", "TURTLE")
        val responseModel = responseReader.parseResponse(response["body"] as String, "RDFXML")

        assertTrue(expected.isIsomorphicWith(responseModel))
    }

    @Test
    fun idDoesNotExist() {
        val response = apiGet(port, "/collections/123", "text/turtle")
        Assertions.assertEquals(HttpStatus.NOT_FOUND.value(), response["status"])
    }

    @Test
    fun findAllWithRecords() {
        val response = apiGet(port, "/collections?catalogrecords=true", "text/turtle")
        Assumptions.assumeTrue(HttpStatus.OK.value() == response["status"])

        val expected = responseReader.parseFile("collection_0.ttl", "TURTLE")
        val responseModel = responseReader.parseResponse(response["body"] as String, "TURTLE")
        assertTrue(expected.isIsomorphicWith(responseModel))
    }

    @Test
    fun findAllNoRecords() {
        val response = apiGet(port, "/collections", "text/turtle")
        Assumptions.assumeTrue(HttpStatus.OK.value() == response["status"])

        val expected = responseReader.parseFile("harvest_response_0.ttl", "TURTLE")
        val responseModel = responseReader.parseResponse(response["body"] as String, "TURTLE")
        assertTrue(expected.isIsomorphicWith(responseModel))
    }

}