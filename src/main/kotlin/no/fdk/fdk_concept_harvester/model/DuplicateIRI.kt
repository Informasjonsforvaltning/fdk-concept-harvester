package no.fdk.fdk_concept_harvester.model

data class DuplicateIRI(
    val iriToRetain: String,
    val iriToRemove: String,
    val keepRemovedFdkId: Boolean = true
)
