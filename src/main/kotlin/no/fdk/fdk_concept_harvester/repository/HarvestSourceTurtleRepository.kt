package no.fdk.fdk_concept_harvester.repository

import no.fdk.fdk_concept_harvester.model.HarvestSourceTurtle
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface HarvestSourceTurtleRepository : MongoRepository<HarvestSourceTurtle, String>
