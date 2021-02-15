package no.fdk.fdk_concept_harvester.configuration

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties("application")
data class ApplicationProperties(
    val conceptsUri: String,
    val collectionsUri: String,
    val harvestAdminRootUrl: String
)
