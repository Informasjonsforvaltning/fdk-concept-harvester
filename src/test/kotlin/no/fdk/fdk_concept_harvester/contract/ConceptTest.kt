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
class ConceptTest: ApiTestContext() {
    private val responseReader = TestResponseReader()

    @Test
    fun findSpecific() {
        val response = apiGet(port, "/concepts/$CONCEPT_0_ID", "application/rdf+json")
        Assumptions.assumeTrue(HttpStatus.OK.value() == response["status"])

        val expected = responseReader.parseFile("concept_0.ttl", "TURTLE")
        val responseModel = responseReader.parseResponse(response["body"] as String, "RDF/JSON")

        assertTrue(expected.isIsomorphicWith(responseModel))
    }

    @Test
    fun idDoesNotExist() {
        val response = apiGet(port, "/concepts/123", "text/turtle")
        Assertions.assertEquals(HttpStatus.NOT_FOUND.value(), response["status"])
    }

    @Test
    fun findAll() {
        val response = apiGet(port, "/concepts", "text/turtle")
        Assumptions.assumeTrue(HttpStatus.OK.value() == response["status"])

        val expected = responseReader.parseFile("all_concepts.ttl", "TURTLE")
        val responseModel = responseReader.parseResponse(response["body"] as String, "TURTLE")
        assertTrue(checkIfIsomorphicAndPrintDiff(responseModel, expected, "ConceptTest-findAll"))
    }

}