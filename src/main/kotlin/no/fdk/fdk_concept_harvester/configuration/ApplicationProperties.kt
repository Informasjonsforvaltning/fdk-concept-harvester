package no.fdk.fdk_concept_harvester.configuration

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("application")
data class ApplicationProperties(
    val conceptsUri: String,
    val collectionsUri: String,
    val organizationsUri: String,
    val harvestAdminRootUrl: String
)
