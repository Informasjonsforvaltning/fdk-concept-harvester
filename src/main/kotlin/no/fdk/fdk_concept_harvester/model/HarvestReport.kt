package no.fdk.fdk_concept_harvester.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown=true)
data class HarvestReport(
    val runId: String? = null,
    val dataSourceId: String? = null,
    val dataSourceUrl: String? = null,
    val dataType: String = "concept",
    val harvestError: Boolean,
    val startTime: String,
    val endTime: String,
    val errorMessage: String? = null,
    val changedCatalogs: List<FdkIdAndUri> = emptyList(),
    val changedResources: List<FdkIdAndUri> = emptyList(),
    val removedResources: List<FdkIdAndUri> = emptyList()
)

data class FdkIdAndUri(
    val fdkId: String,
    val uri: String
)