package no.fdk.fdk_concept_harvester.rabbit

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import no.fdk.fdk_concept_harvester.harvester.HarvesterActivity
import no.fdk.fdk_concept_harvester.model.HarvestAdminParameters
import no.fdk.fdk_concept_harvester.model.RabbitHarvestTrigger
import org.slf4j.LoggerFactory
import org.springframework.amqp.core.Message
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Service

private val logger = LoggerFactory.getLogger(RabbitMQListener::class.java)
private val ALLOWED_FIELDS = listOf("publisherId", "dataType")

@Service
class RabbitMQListener(
    private val harvesterActivity: HarvesterActivity
) {

    @RabbitListener(queues = ["#{receiverQueue.name}"])
    fun receiveDatasetHarvestTrigger(body: RabbitHarvestTrigger, message: Message) {
        logger.info("Received message with key ${message.messageProperties.receivedRoutingKey}")

        val params = HarvestAdminParameters(
            dataSourceId = body.dataSourceId,
            publisherId = body.publisherId,
            dataSourceType = body.dataSourceType
        )

        harvesterActivity.initiateHarvest(params, body.forceUpdate, body.runId)
    }

}
