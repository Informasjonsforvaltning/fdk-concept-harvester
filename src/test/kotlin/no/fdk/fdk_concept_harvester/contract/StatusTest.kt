package no.fdk.fdk_concept_harvester.contract

import no.fdk.fdk_concept_harvester.utils.ApiTestContext
import no.fdk.fdk_concept_harvester.utils.apiGet
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.test.context.ContextConfiguration

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(
    properties = ["spring.profiles.active=contract-test"],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(initializers = [ApiTestContext.Initializer::class])
@Tag("contract")
class StatusTest: ApiTestContext() {

    @Test
    fun ping() {
        val response = apiGet(port, "/ping", null)

        assertEquals(HttpStatus.OK.value(), response["status"])
    }

    @Test
    @Disabled("TODO: Make sure rabbit is initalized and ready")
    fun ready() {
        val response = apiGet(port, "/ready", null)

        assertEquals(HttpStatus.OK.value(), response["status"])
    }

}