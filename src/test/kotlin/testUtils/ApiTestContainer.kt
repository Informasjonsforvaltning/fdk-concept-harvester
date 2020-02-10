package testUtils


import org.slf4j.LoggerFactory
import org.testcontainers.Testcontainers
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.Network
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy
import java.io.IOException
import java.time.Duration


abstract class ApiTestContainer {
    companion object {

        private val logger = LoggerFactory.getLogger(ApiTestContainer::class.java)
        var elasticContainer: KGenericContainer
        var TEST_API: KGenericContainer
        var rabbitContainer: KGenericContainer

        init {

            startMockServer()

            Testcontainers.exposeHostPorts(LOCAL_SERVER_PORT)
            val apiNetwork = Network.newNetwork()

            elasticContainer = KGenericContainer("docker.elastic.co/elasticsearch/elasticsearch:5.6.9")
                    .withEnv(ELASTIC_ENV_VALUES)
                    .withExposedPorts(ELASTIC_PORT, ELASTIC_TCP_PORT)
                    .waitingFor(HttpWaitStrategy()
                            .forPort(ELASTIC_PORT)
                            .forPath("/_cluster/health?pretty=true")
                            .forStatusCode(200)
                            .withStartupTimeout(Duration.ofMinutes(1)))
                    .withNetwork(apiNetwork)
                    .withNetworkAliases(ELASTIC_NETWORK_NAME)

            rabbitContainer = KGenericContainer("rabbitmq:3.8.2-management")
                    .withEnv(RABBIT_MQ_ENV_VALUES)
                    .withExposedPorts(RABBIT_MQ_PORT, RABBIT_MQ_PORT_2)
                    .withNetwork(apiNetwork)
                    .withNetworkAliases(RABBIT_NETWORK_NAME)

            TEST_API = KGenericContainer(API_IMAGE)
                    .withExposedPorts(API_PORT)
                    .dependsOn(elasticContainer, rabbitContainer)
                    .waitingFor(HttpWaitStrategy()
                            .forPort(API_PORT)
                            .forPath("/ready")
                            .forStatusCode(200)
                            .withStartupTimeout(Duration.ofMinutes(2)))
                    .withNetwork(apiNetwork)
                    .withEnv(API_ENV_VALUES)

            elasticContainer.start()
            TEST_API.start()


            try {
                val result = TEST_API.execInContainer("wget", "-O", "-", "$WIREMOCK_TEST_HOST/ping")
                if (!result.stderr.contains("200")) {
                    logger.debug("Ping to mock server failed")
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
                    .stopContainerCmd(TEST_API.containerId)
                    .withTimeout(100)
                    .exec()
        }
    }

}

// Hack needed because testcontainers use of generics confuses Kotlin
class KGenericContainer(imageName: String) : GenericContainer<KGenericContainer>(imageName)