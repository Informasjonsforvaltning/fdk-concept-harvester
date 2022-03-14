package no.fdk.fdk_concept_harvester.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class AuthHeader(
    val name: String,
    val value: String
)
