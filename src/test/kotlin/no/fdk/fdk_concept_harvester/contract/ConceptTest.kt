package no.fdk.fdk_concept_harvester.contract

import no.fdk.fdk_concept_harvester.utils.*
import no.fdk.fdk_concept_harvester.utils.jwk.Access
import no.fdk.fdk_concept_harvester.utils.jwk.JwtToken
import org.apache.jena.riot.Lang
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
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
    fun findSpecificWithRecords() {
        val response = apiGet(port, "/concepts/$CONCEPT_0_ID?catalogrecords=true", "application/rdf+json")
        Assumptions.assumeTrue(HttpStatus.OK.value() == response["status"])

        val expected = responseReader.parseFile("concept_0.ttl", "TURTLE")
        val responseModel = responseReader.parseResponse(response["body"] as String, "RDF/JSON")

        assertTrue(expected.isIsomorphicWith(responseModel))
    }

    @Test
    fun findSpecificNoRecords() {
        val response = apiGet(port, "/concepts/$CONCEPT_0_ID", "application/n-quads")
        Assumptions.assumeTrue(HttpStatus.OK.value() == response["status"])

        val expected = responseReader.parseFile("no_meta_concept_0.ttl", "TURTLE")
        val responseModel = responseReader.parseResponse(response["body"] as String, Lang.NQUADS.name)

        assertTrue(expected.isIsomorphicWith(responseModel))
    }

    @Test
    fun idDoesNotExist() {
        val response = apiGet(port, "/concepts/123", "text/turtle")
        Assertions.assertEquals(HttpStatus.NOT_FOUND.value(), response["status"])
    }

    @Test
    fun unionDoesNotExist() {
        val response = apiGet(port, "/concepts", "text/turtle")
        Assertions.assertEquals(HttpStatus.NOT_FOUND.value(), response["status"])
    }

    @Nested
    internal inner class RemoveConceptById {

        @Test
        fun unauthorizedForNoToken() {
            val response = authorizedRequest(port, "/concepts/$CONCEPT_0_ID", null, "DELETE")
            assertEquals(HttpStatus.UNAUTHORIZED.value(), response["status"])
        }

        @Test
        fun forbiddenWithNonSysAdminRole() {
            val response = authorizedRequest(
                port,
                "/concepts/$CONCEPT_0_ID",
                JwtToken(Access.ORG_WRITE).toString(),
                "DELETE"
            )
            assertEquals(HttpStatus.FORBIDDEN.value(), response["status"])
        }

        @Test
        fun notFoundWhenIdNotInDB() {
            val response =
                authorizedRequest(port, "/concepts/123", JwtToken(Access.ROOT).toString(), "DELETE")
            assertEquals(HttpStatus.NOT_FOUND.value(), response["status"])
        }

        @Test
        fun okWithSysAdminRole() {
            val response = authorizedRequest(
                port,
                "/concepts/$CONCEPT_0_ID",
                JwtToken(Access.ROOT).toString(),
                "DELETE"
            )
            assertEquals(HttpStatus.NO_CONTENT.value(), response["status"])
        }
    }

}
