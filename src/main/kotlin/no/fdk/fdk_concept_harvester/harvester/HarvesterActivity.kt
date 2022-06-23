package no.fdk.fdk_concept_harvester.harvester

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import no.fdk.fdk_concept_harvester.adapter.HarvestAdminAdapter
import no.fdk.fdk_concept_harvester.model.HarvestAdminParameters
import no.fdk.fdk_concept_harvester.rabbit.RabbitMQPublisher
import no.fdk.fdk_concept_harvester.service.UpdateService
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import java.util.*

private val LOGGER = LoggerFactory.getLogger(HarvesterActivity::class.java)

@Service
class HarvesterActivity(
    private val harvestAdminAdapter: HarvestAdminAdapter,
    private val harvester: ConceptHarvester,
    private val publisher: RabbitMQPublisher,
    private val updateService: UpdateService
) : CoroutineScope by CoroutineScope(Dispatchers.Default) {

    private val activitySemaphore = Semaphore(1)

    @EventListener
    fun fullHarvestOnStartup(event: ApplicationReadyEvent) = initiateHarvest(HarvestAdminParameters(null, null, null), false)

    fun initiateHarvest(params: HarvestAdminParameters, forceUpdate: Boolean) {
        if (params.harvestAllConcepts()) LOGGER.debug("starting harvest of all concept collections, force update: $forceUpdate")
        else LOGGER.debug("starting harvest with parameters $params, force update: $forceUpdate")

        launch {
            activitySemaphore.withPermit {
                harvestAdminAdapter.getDataSources(params)
                    .filter { it.dataType == "concept" }
                    .filter { it.url != null }
                    .map { async { harvester.harvestConceptCollection(it, Calendar.getInstance(), forceUpdate) } }
                    .awaitAll()
                    .filterNotNull()
                    .also { updateService.updateMetaData() }
                    .also {
                        if (params.harvestAllConcepts()) LOGGER.debug("completed harvest with parameters $params, force update: $forceUpdate")
                        else LOGGER.debug("completed harvest of all collections, force update: $forceUpdate") }
                    .run { publisher.send(this) }
            }
        }
    }
}
