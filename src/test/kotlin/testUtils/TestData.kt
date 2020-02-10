package testUtils

const val API_PORT = 8080
const val LOCAL_SERVER_PORT = 5000

const val WIREMOCK_TEST_HOST = "http://host.testcontainers.internal:$LOCAL_SERVER_PORT"
const val COMPLEX_SEARCH_STRING = "søke noe"
const val WORD_MATCH_SCRIPT = "\"(doc['prefLabel.nn'][0].contains('dokument') && (doc['prefLabel.nn'][0].length() < 10)) && (doc[ 'prefLabel.nn'].size() == 1) || (doc['prefLabel.nb'][0].contains('dokument') && (doc['prefLabel.nb'][0].length() < 10)) && (doc[ 'prefLabel.nb'].size() == 1)|| (doc['prefLabel.no'][0].contains('dokument')) && (doc['prefLabel.nn'][0].length() < 10) || (doc['prefLabel.en'][0].contains('dokument')) &&(doc['prefLabel.nn'][0].length() < 10)\""
const val PARTIAL_MATCH_SCRIPT = "\"(doc['prefLabel.nn'][0].contains('dok') && (doc['prefLabel.nn'][0].length() < 10)) && (doc[ 'prefLabel.nn'].size() == 1) || (doc['prefLabel.nb'][0].contains('dokument') && (doc['prefLabel.nb'][0].length() < 10)) && (doc[ 'prefLabel.nb'].size() == 1)|| (doc['prefLabel.no'][0].contains('dok')) && (doc['prefLabel.nn'][0].length() < 10) || (doc['prefLabel.en'][0].contains('dok')) &&(doc['prefLabel.nn'][0].length() < 10)\""

const val mockDataArkiv = "/Users/bbreg/Documents/concept-catalouge/fdk-concept-harvester/src/main/resources/dev.data/arkivverket_fdk.turtle"
fun populateDbContract(){}

data class Properties(val type : String, val fielddata : Boolean = false, val analyzer: String)
