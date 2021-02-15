package no.fdk.fdk_concept_harvester.harvester

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import no.fdk.fdk_concept_harvester.adapter.HarvestAdminAdapter
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import java.util.*

private val LOGGER = LoggerFactory.getLogger(HarvesterActivity::class.java)
private const val HARVEST_ALL_ID = "all"

@Service
class HarvesterActivity(
    private val harvestAdminAdapter: HarvestAdminAdapter,
) : CoroutineScope by CoroutineScope(Dispatchers.Default) {

    @EventListener
    fun fullHarvestOnStartup(event: ApplicationReadyEvent) = initiateHarvest(null)

    fun initiateHarvest(params: Map<String, String>?) {
        if (params == null || params.isEmpty()) LOGGER.debug("starting harvest of all concept collections")
        else LOGGER.debug("starting harvest with parameters $params")

        val harvest = launch {
            harvestAdminAdapter.getDataSources(params)
                .filter { it.dataType == "concept" }
                .filter { it.url != null }
                .forEach {
                    try {
                        LOGGER.info("Harvest ${it.url}") // harvester.harvestConceptCollection(it, Calendar.getInstance())
                    } catch (exception: Exception) {
                        LOGGER.error("Harvest of ${it.url} failed", exception)
                    }
                }
        }

        val onHarvestCompletion = launch {
            harvest.join()
            LOGGER.debug("Updating union model")
            // updateService.updateUnionModel()

            if (params.isNullOrEmpty()) LOGGER.debug("completed harvest of all collections")
            else LOGGER.debug("completed harvest with parameters $params")

            harvest.cancelChildren()
            harvest.cancel()
        }

        onHarvestCompletion.invokeOnCompletion { onHarvestCompletion.cancel() }
    }
}
