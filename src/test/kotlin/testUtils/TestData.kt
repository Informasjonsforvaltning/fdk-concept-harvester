package testUtils

import com.google.common.collect.ImmutableMap
import testUtils.ApiTestContainer.Companion.TEST_API

const val API_PORT = 8080
const val LOCAL_SERVER_PORT = 5000

const val WIREMOCK_TEST_HOST = "http://host.testcontainers.internal:$LOCAL_SERVER_PORT"
const val COMPLEX_SEARCH_STRING = "søke noe"

const val ELASTIC_PORT = 9200
const val ELASTIC_TCP_PORT = 9300
const val ELASTIC_NETWORK_NAME = "elasticsearch5"
const val ELASTIC_CLUSERNAME = "elasticsearch"
const val ELASTIC_CLUSTERNODES = "$ELASTIC_NETWORK_NAME:$ELASTIC_TCP_PORT"
const val RABBIT_NETWORK_NAME = "rabbitmq"
const val RABBIT_MQ_PORT = 5672
const val RABBIT_MQ_PORT_2 = 15672

const val API_IMAGE = "eu.gcr.io/fdk-infra/fdk-concept-harvester:latest"

val API_ENV_VALUES : Map<String,String> = mutableMapOf(
        "SPRING_PROFILES_ACTIVE" to  "test",
        "WIREMOCK_TEST_HOST" to WIREMOCK_TEST_HOST,
        "FDK_ES_CLUSTERNODES" to ELASTIC_CLUSTERNODES,
        "FDK_ES_CLUSTERNAME" to ELASTIC_CLUSERNAME,
        "HARVEST_ADMIN_ROOT_URL" to  "${WIREMOCK_TEST_HOST}/api",
        "RABBIT_USERNAME" to "admin",
        "RABBIT_PASSWORD" to "admin"
)

val ELASTIC_ENV_VALUES : Map<String,String> = ImmutableMap.of(
        "cluster.name" , ELASTIC_CLUSERNAME,
        "xpack.security.enabled", "false",
        "xpack.monitoring.enabled", "false"
)


val RABBIT_MQ_ENV_VALUES : Map <String, String> = ImmutableMap.of(
        "RABBITMQ_DEFAULT_USER" , "admin",
        "RABBITMQ_DEFAULT_PASS","admin"
)

/*
  rabbitmq:
    image: rabbitmq:3.8.2-management
    ports:
      - "5672:5672"
      - "15672:15672"
    environment:
      - RABBITMQ_DEFAULT_USER=admin
      - RABBITMQ_DEFAULT_PASS=admin
*/




fun getApiAddress( endpoint: String ): String{
    return "http://${TEST_API.getContainerIpAddress()}:${TEST_API.getMappedPort(API_PORT)}$endpoint"
}
const val mockDataArkiv = "/Users/bbreg/Documents/concept-catalouge/fdk-concept-harvester/src/main/resources/dev.data/arkivverket_fdk.turtle"

data class SortResponse(val sortWord: String, val primaryLanguagePath: String = "nb") {
        var exactMatchesId = mutableListOf<String>()
        var lastExactMatch = 0
        var lastPath =  ""

        fun isLessRelevant(currentPath: String, currentValue: String, positionInList: Int, conceptId: String): Boolean {
            var correctlySorted = false

            if(currentValue == sortWord) {
                //if current hit is an exact match, last match should be an exact match
                if (positionInList == lastExactMatch + 1) {
                    lastExactMatch += 1
                    //if the current hit is in primaryLanguage, last hit should also be in primaryLanguage
                    if(currentPath == primaryLanguagePath){
                        if(lastPath == primaryLanguagePath) {
                            correctlySorted = true
                        }

                    } else {
                        //If the concept has already been listed in primary language, it should not be listed on secondary languages
                        if(!exactMatchesId.contains(conceptId)){
                            correctlySorted = true
                        }
                    }
                }
            } else {
                //TODO: better search specification
                correctlySorted = true
            }

            return correctlySorted
        }

}
