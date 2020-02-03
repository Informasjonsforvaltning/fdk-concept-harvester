package testUtils

const val API_PORT = 8080
const val LOCAL_SERVER_PORT = 5000

const val MONGO_SERVICE_NAME = "mongodb"
const val MONGO_USER = "testuser"
const val MONGO_PASSWORD = "testpassword"
const val MONGO_AUTH = "?authSource=admin&authMechanism=SCRAM-SHA-1"
const val MONGO_PORT = 27017

const val WIREMOCK_TEST_HOST = "http://host.testcontainers.internal:$LOCAL_SERVER_PORT"
const val ES_SERVICE = "http://localhost:9200"

const val mockDataArkiv = "/Users/bbreg/Documents/concept-catalouge/fdk-concept-harvester/src/main/resources/dev.data/arkivverket_fdk.turtle"

fun populateDbContract(){}

data class Properties(val type : String, val fielddata : Boolean = false, val analyzer: String)
data class PrefLabelProperties(val nn: Properties, val nb: Properties)