package no.ccat.service;

import lombok.RequiredArgsConstructor;
import no.ccat.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RabbitMQListener {
    private static final Logger logger = LoggerFactory.getLogger(RabbitMQListener.class);
    private final HarvestAdminClient harvestAdminClient;
    private final ConceptHarvester conceptHarvester;

    private void harvest(HarvestTriggerMessage harvestTriggerMessage) {
        this.harvestAdminClient.getDataSources(harvestTriggerMessage.queryParams())
                .forEach(conceptHarvester::harvestFromSingleURLSource);
    }

    @RabbitListener(queues = "#{conceptPublisher.name}")
    public void receiveConceptPublisher(ConceptPublisherMessage conceptPublisherMessage) {
        logger.info("concept publisher received");
        harvest(conceptPublisherMessage);
    }

    @RabbitListener(queues = "#{conceptAll.name}")
    public void receiveConceptAll(ConceptAllMessage conceptAllMessage) {
        logger.info("concept all received");
        harvest(conceptAllMessage);
    }

    @RabbitListener(queues = "#{concept.name}")
    public void receiveConcept(Concept concept) {
        logger.info("concept received");
        harvest(concept);
    }

    @RabbitListener(queues = "#{conceptCatalogue.name}")
    public void receiveConceptCatalogue(ConceptCatalogue conceptCatalogue) {
        logger.info("concept catalogue received");
        harvest(conceptCatalogue);
    }
}
