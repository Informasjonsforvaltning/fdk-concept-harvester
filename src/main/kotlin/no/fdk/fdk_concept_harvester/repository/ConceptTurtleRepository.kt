package no.fdk.fdk_concept_harvester.repository

import no.fdk.fdk_concept_harvester.model.ConceptTurtle
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface ConceptTurtleRepository : MongoRepository<ConceptTurtle, String>
