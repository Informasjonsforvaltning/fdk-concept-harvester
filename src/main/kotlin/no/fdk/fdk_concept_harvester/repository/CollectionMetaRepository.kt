package no.fdk.fdk_concept_harvester.repository

import no.fdk.fdk_concept_harvester.model.CollectionMeta
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface CollectionMetaRepository : MongoRepository<CollectionMeta, String>
