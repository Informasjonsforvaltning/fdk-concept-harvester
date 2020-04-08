package testUtils

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.*
import java.io.File


private val mockserver = WireMockServer(LOCAL_SERVER_PORT)

fun startMockServer() {
    if(!mockserver.isRunning) {
        mockserver.stubFor(get(urlEqualTo("/ping"))
                .willReturn(aResponse()
                        .withStatus(200))
        )
        mockserver.stubFor(get(urlEqualTo("/api/v1/schemas"))
                .willReturn(okJson(File("src/test/resources/contract/test-schemas.json").readText())))

        mockserver.stubFor(get(urlEqualTo("/api/registration/apis"))
                .willReturn(okJson(File("src/test/resources/contract/api-reg.json").readText())))

        mockserver.stubFor(get(urlEqualTo("/api/publishers/12345678"))
                .willReturn(okJson(File("src/test/resources/contract/org-0.json").readText())))

        mockserver.stubFor(get(urlEqualTo("/api/publishers/87654321"))
                .willReturn(okJson(File("src/test/resources/contract/org-1.json").readText())))
        mockserver.stubFor(get(urlEqualTo("/api/publishers/961181399"))
                .willReturn(okJson(File("src/test/resources/contract/org-2.json").readText())))

        mockserver.stubFor(get(urlEqualTo("/api/publishers/974761076"))
                .willReturn(okJson(File("src/test/resources/contract/org-0.json").readText())))
        mockserver.stubFor(get(urlEqualTo("/api/publishers/961189899"))
                .willReturn(okJson(File("src/test/resources/contract/org-0.json").readText())))
        mockserver.stubFor(get(urlEqualTo("/api/publishers/974760673"))
                .willReturn(okJson(File("src/test/resources/contract/org-1.json").readText())))
        mockserver.stubFor(get(urlEqualTo("/api/datasources?dataType=concept"))
                .willReturn(okJson(File("src/test/resources/contract/datasource.json").readText())))
        mockserver.stubFor(get(urlEqualTo("/mockconcepts"))
                .willReturn(aResponse().withBody(File("src/test/resources/contract/conceptsmock.turtle").readText()))
        )

        mockserver.start()
    }
}

fun stopMockServer() {

    if (mockserver.isRunning) mockserver.stop()

}