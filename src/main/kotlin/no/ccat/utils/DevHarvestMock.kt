package no.ccat.utils

import java.io.File
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.*


private val mockserver = WireMockServer(5000)
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
        mockserver.stubFor(get(urlEqualTo("/api/publishers/961181399"))
                .willReturn(okJson(File("src/test/resources/contract/org-1.json").readText())))
        mockserver.stubFor(get(urlEqualTo("/api/publishers/974761076"))
                .willReturn(okJson(File("src/test/resources/contract/org-1.json").readText())))
        mockserver.stubFor(get(urlEqualTo("/api/publishers/974760673"))
                .willReturn(okJson(File("src/test/resources/contract/org-1.json").readText())))
        mockserver.stubFor(get(urlEqualTo("/api/datasources?datatype/concept"))
                .willReturn(okJson(File("src/main/resources/dev_data/datasource.json").readText())))
        mockserver.stubFor(get(urlEqualTo("/skatteEtaten"))
                .willReturn(aResponse().withBody(File("./src/main/resources/dev_data/skatteEtaten.turtle").readText())))
        mockserver.stubFor(get(urlEqualTo("/mockconcepts"))
                .willReturn(aResponse().withBody(File("./src/main/resources/dev_data/arkivverket_fdk.turtle").readText())))


        mockserver.start()
    }
}
fun ready(): Boolean = mockserver.isRunning

fun stopMockServer() {

    if (mockserver.isRunning) mockserver.stop()

}