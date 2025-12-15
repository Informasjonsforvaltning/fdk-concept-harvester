package no.fdk.fdk_concept_harvester.rabbit

import no.fdk.fdk_concept_harvester.harvester.HarvesterActivity
import no.fdk.fdk_concept_harvester.model.HarvestTrigger
import org.slf4j.LoggerFactory
import org.springframework.amqp.core.Message
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.stereotype.Service

private val logger = LoggerFactory.getLogger(RabbitMQListener::class.java)
private val ALLOWED_FIELDS = listOf("publisherId", "dataType")

@Service
class RabbitMQListener(
    private val harvesterActivity: HarvesterActivity
) {

    @RabbitListener(queues = ["#{receiverQueue.name}"])
    fun receiveDatasetHarvestTrigger(body: HarvestTrigger, message: Message) {
        logger.info("Received message with key ${message.messageProperties.receivedRoutingKey}")

        harvesterActivity.initiateHarvest(body)
    }

}
