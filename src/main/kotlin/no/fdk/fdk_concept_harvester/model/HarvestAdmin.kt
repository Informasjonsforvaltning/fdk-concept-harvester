package no.fdk.fdk_concept_harvester.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class HarvestDataSource(
    val dataSourceType: String? = null,
    val dataType: String? = null,
    val url: String? = null,
    val acceptHeaderValue: String? = null,
    val publisherId: String? = null,
    val authHeader: AuthHeader? = null
)

@JsonIgnoreProperties(ignoreUnknown=true)
data class HarvestAdminParameters(
    val dataSourceId: String? = null,
    val publisherId: String? = null,
    val dataSourceType: String? = null,
    val dataType: String? = "concept"
)
