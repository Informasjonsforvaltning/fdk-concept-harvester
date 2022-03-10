package no.fdk.fdk_concept_harvester.adapter

import no.fdk.fdk_concept_harvester.model.Organization

interface OrganizationsAdapter {
    fun getOrganization(id: String): Organization?
}