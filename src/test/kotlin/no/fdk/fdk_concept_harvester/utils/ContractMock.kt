package no.fdk.fdk_concept_harvester.utils

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.*
import no.fdk.fdk_concept_harvester.utils.jwk.JwkStore
import java.io.File

private val mockserver = WireMockServer(LOCAL_SERVER_PORT)

fun startMockServer() {
    if(!mockserver.isRunning) {
        mockserver.stubFor(get(urlEqualTo("/ping"))
                .willReturn(aResponse()
                        .withStatus(200))
        )
        mockserver.stubFor(get(urlEqualTo("/internal/datasources?dataType=concept"))
            .willReturn(okJson(jacksonObjectMapper().writeValueAsString(
                listOf(TEST_HARVEST_SOURCE_0, TEST_HARVEST_SOURCE_1))))
        )
        mockserver.stubFor(get(urlMatching("/concept-harvest-source-0"))
            .willReturn(ok(File("src/test/resources/harvest_response_0.ttl").readText())))
        mockserver.stubFor(get(urlMatching("/concept-harvest-source-1"))
            .willReturn(ok(File("src/test/resources/harvest_error_response.ttl").readText())))

        mockserver.stubFor(put(urlEqualTo("/fuseki/harvested?graph=https://data.norge.no/concepts"))
            .willReturn(aResponse().withStatus(200))
        )

        mockserver.stubFor(get(urlEqualTo("/auth/realms/fdk/protocol/openid-connect/certs"))
            .willReturn(okJson(JwkStore.get())))

        mockserver.start()
    }
}

fun stopMockServer() {

    if (mockserver.isRunning) mockserver.stop()

}