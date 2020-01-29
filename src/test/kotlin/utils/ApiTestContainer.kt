package utils

import org.slf4j.LoggerFactory
import org.testcontainers.Testcontainers
import org.testcontainers.containers.BindMode
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.Network
import org.testcontainers.containers.wait.strategy.Wait
import utils.*
import java.io.IOException

abstract class ApiTestContainer {
    companion object {

        private val logger = LoggerFactory.getLogger(ApiTestContainer::class.java)
        var TEST_API: KGenericContainer

        init {

            startMockServer()

            Testcontainers.exposeHostPorts(LOCAL_SERVER_PORT)
          //  val apiNetwork = Network.newNetwork()

            populateDbContract()

            TEST_API = KGenericContainer("eu.gcr.io/fdk-infra/organization-catalogue:latest")
                    .withExposedPorts(API_PORT)
                 //   .withEnv(API_ENV_VALUES)
                    .waitingFor(Wait.forHttp("/ready").forStatusCode(200))
          //          .withNetwork(apiNetwork)

            TEST_API.start()


            try {
                val result = TEST_API.execInContainer("wget", "-O", "-", "$WIREMOCK_TEST_HOST/auth/realms/fdk/protocol/openid-connect/certs")
                if (!result.stderr.contains("200")) {
                    logger.debug("Ping to AuthMock server failed")
                }
            } catch (e: IOException) {
                e.printStackTrace()
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }

        }

        fun stopGracefully() {
            logger.debug("Shutting down container gracefully")
            TEST_API.dockerClient
                    .stopContainerCmd(ApiTestContainer.TEST_API.containerId)
                    .withTimeout(100)
                    .exec()
        }
    }

}

// Hack needed because testcontainers use of generics confuses Kotlin
class KGenericContainer(imageName: String) : GenericContainer<KGenericContainer>(imageName)