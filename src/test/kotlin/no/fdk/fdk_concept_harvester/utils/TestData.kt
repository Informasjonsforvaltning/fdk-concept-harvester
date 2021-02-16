package no.fdk.fdk_concept_harvester.utils

import no.fdk.fdk_concept_harvester.model.HarvestDataSource
import org.testcontainers.shaded.com.google.common.collect.ImmutableMap
import java.util.*

const val LOCAL_SERVER_PORT = 5000

const val MONGO_USER = "testuser"
const val MONGO_PASSWORD = "testpassword"
const val MONGO_PORT = 27017
const val MONGO_COLLECTION = "conceptHarvester"

val MONGO_ENV_VALUES: Map<String, String> = ImmutableMap.of(
    "MONGO_INITDB_ROOT_USERNAME", MONGO_USER,
    "MONGO_INITDB_ROOT_PASSWORD", MONGO_PASSWORD
)

val TEST_HARVEST_DATE: Calendar = Calendar.Builder().setTimeZone(TimeZone.getTimeZone("UTC")).setDate(2021, 0, 5).setTimeOfDay(13, 15, 39, 831).build()
val NEW_TEST_HARVEST_DATE: Calendar = Calendar.Builder().setTimeZone(TimeZone.getTimeZone("UTC")).setDate(2021, 1, 15).setTimeOfDay(11, 52, 16, 122).build()

val TEST_HARVEST_SOURCE_0 = HarvestDataSource(
    url = "http://localhost:5000/concept-harvest-source-0",
    acceptHeaderValue = "text/turtle",
    dataType = "concept",
    dataSourceType = "SKOS-AP-NO"
)

val TEST_HARVEST_SOURCE_1 = HarvestDataSource(
    url = "http://localhost:5000/concept-harvest-source-1",
    acceptHeaderValue = "text/turtle",
    dataType = "concept",
    dataSourceType = "SKOS-AP-NO"
)

const val COLLECTION_0_ID = "9b8f1c42-1161-33b1-9d43-a733ee94ddfc"

const val CONCEPT_0_ID = "db1b701c-b4b9-3c20-bc23-236a91236754"
const val CONCEPT_1_ID = "7dbac738-4944-323a-a777-ad2f83bf75f8"
