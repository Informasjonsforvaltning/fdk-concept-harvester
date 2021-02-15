package no.fdk.fdk_concept_harvester.repository

import no.fdk.fdk_concept_harvester.model.TurtleDBO
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface TurtleRepository : MongoRepository<TurtleDBO, String>
