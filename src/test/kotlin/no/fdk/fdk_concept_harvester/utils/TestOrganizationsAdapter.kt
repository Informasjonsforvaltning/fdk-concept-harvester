package no.fdk.fdk_concept_harvester.utils

import no.fdk.fdk_concept_harvester.adapter.OrganizationsAdapter
import no.fdk.fdk_concept_harvester.model.Organization


class TestOrganizationsAdapter() : OrganizationsAdapter {

    override fun getOrganization(id: String): Organization? {
        if (id.equals("NO_PUBLISHER")) {
            return null
        }
        return Organization(id, "http://organization/$id", "Test Organization", null)
    }

}
