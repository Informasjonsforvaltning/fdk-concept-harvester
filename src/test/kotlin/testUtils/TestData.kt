package testUtils

import com.google.common.collect.ImmutableMap
import com.jayway.jsonpath.DocumentContext
import net.minidev.json.JSONArray
import no.ccat.utils.QueryParams
import testUtils.ApiTestContainer.Companion.TEST_API
import testUtils.ApiTestContainer.Companion.elasticContainer

const val API_PORT = 8080
const val LOCAL_SERVER_PORT = 5000

const val COMPLEX_SEARCH_STRING = "søke noe"

const val ELASTIC_PORT = 9200
const val ELASTIC_TCP_PORT = 9300
const val ELASTIC_NETWORK_NAME = "elasticsearch5"
const val ELASTIC_CLUSERNAME = "elasticsearch"
const val ELASTIC_CLUSTERNODES = "$ELASTIC_NETWORK_NAME:$ELASTIC_TCP_PORT"
const val RABBIT_NETWORK_NAME = "rabbitmq"
const val RABBIT_MQ_PORT = 5672
const val RABBIT_MQ_PORT_2 = 15672

val MOCK_MULTIPLE_WORDS = mutableMapOf<String,String>(
        "TWO_CONCATENATED_WORD" to "overføringsfil",
        "TWO_SPLIT_WORD" to "overførings fil",
        "SEVERAL_SPLIT_WORDS" to "arbeidsprosess i offentlig sektor",
        "SEVERAL_SPLIT_VARIATION" to "arbeidsprosess i offentlig sektor",
        "SEVERAL_SPLIT_WORDS_WITH_DASH" to "arbeidsprosess- og styring",
        "WORDS_WITH_FORWARD_SLASH" to "arbeidsprosess/styring",
        "WORDS_WITH_PARENTHESIS" to "arbeid (fritid)",
        "WORDS_WITHOUT_PARENTHESIS" to "arbeid fritid"
)

const val WIREMOCK_TEST_HOST = "http://host.testcontainers.internal:$LOCAL_SERVER_PORT"
val API_IMAGE = System.getProperty("testImageName") ?: "eu.gcr.io/fdk-infra/fdk-concept-harvester:latest"

val API_ENV_VALUES : Map<String,String> = mutableMapOf(
        "SPRING_PROFILES_ACTIVE" to  "test",
        "WIREMOCK_TEST_HOST" to WIREMOCK_TEST_HOST,
        "FDK_ES_CLUSTERNODES" to ELASTIC_CLUSTERNODES,
        "FDK_ES_CLUSTERNAME" to ELASTIC_CLUSERNAME,
        "HARVEST_ADMIN_ROOT_URL" to  "${WIREMOCK_TEST_HOST}/api",
        "RABBIT_USERNAME" to "admin",
        "RABBIT_PASSWORD" to "admin",
        "RABBIT_HOST" to RABBIT_NETWORK_NAME,
        "RABBIT_PORT" to RABBIT_MQ_PORT.toString()
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

fun getApiAddress( endpoint: String ): String{
    return "http://${TEST_API.getContainerIpAddress()}:${TEST_API.getMappedPort(API_PORT)}$endpoint"
}

fun paramsWithQueryString() = QueryParams(queryString = "Some query")
fun paramsWithQueryStringAndPrefLabel() = QueryParams(queryString = "Some query", prefLabel = "something else")
fun paramsWithQueryStringAndOrgPath() = QueryParams(queryString = "Some query", orgPath="STAT/123456/65432")
fun paramsWithQueryStringAndIdentifiers() = QueryParams(
        queryString = "Some query",
        identifiers = setOf("http://dfg4-hdjk-22-sdjk","http://dfgkk-hdjk-22-sdjk"))
fun paramsWithQueryStringAndIdentifiersAndOrgPath() = QueryParams(
        queryString = "Some query",
        orgPath="STAT/123456/65432",
        identifiers = setOf("http://dfg4-hdjk-22-sdjk","http://dfgkk-hdjk-22-sdjk"))
fun paramsWithUriAndSort() = QueryParams(queryString = "some string",
        uris = setOf("One","two"), sortDirection = "asc", size="400")
fun paramsWithQueryStringIdentifiersAndSort() = QueryParams(
        queryString = "Some query",
        orgPath="STAT/123456/65432",
        identifiers = setOf("http://dfg4-hdjk-22-sdjk","http://dfgkk-hdjk-22-sdjk"),
        sortDirection = "asc",sortField = "prefLabel")
fun paramsWithPrefLabelIdentifiersAndSort() = QueryParams(
        prefLabel = "Some query",
        identifiers = setOf("http://dfg4-hdjk-22-sdjk","http://dfgkk-hdjk-22-sdjk"),
        sortDirection = "asc",sortField = "prefLabel")
fun paramsWithPrefLabelUrisAndSort() = QueryParams(
        prefLabel = "Some query",
        uris = setOf("http://dfg4-hdjk-22-sdjk","http://dfgkk-hdjk-22-sdjk"),
        sortDirection = "asc",sortField = "prefLabel")
fun paramsWithAllQueryValues() = QueryParams(
        queryString = "Some query",
        prefLabel = "something else",
        identifiers = setOf("http://dfg4-hdjk-22-sdjk","http://dfgkk-hdjk-22-sdjk"),
        uris = setOf("http://dfg4-hdjk-22-sdjk","http://dfgkk-hdjk-22-sdjk"))
fun paramsWithQueryStringAndUris() = QueryParams(
        queryString = "Some query",
        uris = setOf("http://dfg4-hdjk-22-sdjk","http://dfgkk-hdjk-22-sdjk"))
fun paramsWithPrefLabel() = QueryParams(
        prefLabel = "Some query")
fun paramsWithPrefLabelAndOrgPath() = QueryParams(
        prefLabel = "Some query",
        orgPath="STAT/123456/65432")
fun paramsWithPrefLabelAndOrgPathAndSort() = QueryParams(
        prefLabel = "Some query",
        orgPath="STAT/123456/65432",
        sortDirection = "asc",
        sortField = "prefLabel")
fun paramsWithPrefLabelAndUris() = QueryParams(
        prefLabel = "somequery",
        uris = setOf("http://dfg4-hdjk-22-sdjk","http://dfgkk-hdjk-22-sdjk"))
fun paramsWithPrefLabelUrisAndOrgPath() = QueryParams(
        prefLabel = "somequery",
        uris = setOf("http://dfg4-hdjk-22-sdjk","http://dfgkk-hdjk-22-sdjk"),
        orgPath="STAT/123456/65432")

fun paramsWithUri() = QueryParams(uris = setOf("some-uri","some-other-uri"))
fun paramsWithUriAndSize() = QueryParams(uris = setOf("some-uri","some-other-uri"), size = "10")
fun paramsWithIdentifiers() = QueryParams(identifiers = setOf("sine-idnetifier","some-other-identifier"))
fun paramsWithIdentifiersAndPage() = QueryParams(
        identifiers = setOf("sine-idnetifier","some-other-identifier"),
        startPage= "1")
fun paramsWithUriAndIdentifiers() = QueryParams(
        identifiers = setOf("http://dfg4-hdjk-22-sdjk","http://dfgkk-hdjk-22-sdjk"),
        uris = setOf("http://dfg4-hdjk-22-sdjk","http://dfgkk-hdjk-22-sdjk"))
fun paramsWithQueryStringUriAndOrgPath() = QueryParams(
        queryString = "Some query",
        orgPath = "STAS/123456/67890",
        uris = setOf("http://dfg4-hdjk-22-sdjk","http://dfgkk-hdjk-22-sdjk"))
fun paramsWithQueryStringPrefLabelAndOrgPath() = QueryParams(
        queryString = "Some query",
        orgPath = "STAS/123456/67890",
        uris = setOf("http://dfg4-hdjk-22-sdjk","http://dfgkk-hdjk-22-sdjk"))

