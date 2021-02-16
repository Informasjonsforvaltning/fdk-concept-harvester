package no.fdk.fdk_concept_harvester.service

import no.fdk.fdk_concept_harvester.adapter.FusekiAdapter
import no.fdk.fdk_concept_harvester.rdf.parseRDFResponse
import no.fdk.fdk_concept_harvester.repository.*
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.riot.Lang
import org.springframework.stereotype.Service

@Service
class UpdateService (
    private val fusekiAdapter: FusekiAdapter,
    private val collectionMetaRepository: CollectionMetaRepository,
    private val turtleService: TurtleService
) {

    fun updateUnionModel() {
        var unionModel = ModelFactory.createDefaultModel()

        collectionMetaRepository.findAll()
            .forEach {
                turtleService.getCollection(it.fdkId, withRecords = true)
                    ?.let { turtle -> parseRDFResponse(turtle, Lang.TURTLE, null) }
                    ?.run { unionModel = unionModel.union(this) }
            }

        fusekiAdapter.storeUnionModel(unionModel)
        turtleService.saveAsUnion(unionModel)
    }

}
