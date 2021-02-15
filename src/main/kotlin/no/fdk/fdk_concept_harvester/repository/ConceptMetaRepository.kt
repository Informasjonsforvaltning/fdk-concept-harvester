package no.fdk.fdk_concept_harvester.repository

import no.fdk.fdk_concept_harvester.model.ConceptMeta
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface ConceptMetaRepository : MongoRepository<ConceptMeta, String> {
    fun findAllByIsPartOf(isPartOf: String): List<ConceptMeta>
}
