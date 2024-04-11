package no.fdk.fdk_concept_harvester.utils

import no.fdk.fdk_concept_harvester.model.CollectionMeta
import no.fdk.fdk_concept_harvester.model.HarvestDataSource
import no.fdk.fdk_concept_harvester.model.Organization
import no.fdk.fdk_concept_harvester.model.PrefLabel
import org.testcontainers.shaded.com.google.common.collect.ImmutableMap
import java.util.*

const val LOCAL_SERVER_PORT = 5050

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
    id = "concept-harvest-source-0",
    url = "http://localhost:5050/concept-harvest-source-0",
    acceptHeaderValue = "text/turtle",
    dataType = "concept",
    dataSourceType = "SKOS-AP-NO",
    publisherId = "123456789"
)

val TEST_HARVEST_SOURCE_1 = HarvestDataSource(
    id = "concept-harvest-source-1",
    url = "http://localhost:5050/concept-harvest-source-1",
    acceptHeaderValue = "text/turtle",
    dataType = "concept",
    dataSourceType = "SKOS-AP-NO",
    publisherId = "987654321"
)

const val COLLECTION_0_ID = "9b8f1c42-1161-33b1-9d43-a733ee94ddfc"

const val CONCEPT_0_ID = "db1b701c-b4b9-3c20-bc23-236a91236754"
const val CONCEPT_1_ID = "7dbac738-4944-323a-a777-ad2f83bf75f8"

const val GENERATED_COLLECTION_ID = "03a39fae-1d78-337c-b573-e523d5e7097f"

val GENERATED_COLLECTION = CollectionMeta(
    uri = "http://localhost:5050/concept-harvest-source-0#GeneratedCollection", fdkId = GENERATED_COLLECTION_ID,
    issued = 1609852539831, modified = 1609852539831,
    concepts = setOf("https://example.com/begrep/0")
)

val ORGANIZATION_0 = Organization(
    organizationId = "123456789",
    uri = "http://localhost:5050/organizations/123456789",
    name = "TESTDIREKTORATET",
    prefLabel = PrefLabel(nb = "Testdirektoratet")
)
