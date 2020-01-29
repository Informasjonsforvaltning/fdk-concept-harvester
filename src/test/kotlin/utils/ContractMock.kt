package utils

import com.github.tomakehurst.wiremock.WireMockServer


private val mockserver = WireMockServer(LOCAL_SERVER_PORT)

fun startMockServer() {
    //TODO implement mock methods
}

fun stopMockServer() {

    if (mockserver.isRunning) mockserver.stop()

}