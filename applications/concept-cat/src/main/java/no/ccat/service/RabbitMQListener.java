package no.ccat.service;

import lombok.RequiredArgsConstructor;
import no.ccat.dto.Concept;
import no.ccat.dto.ConceptAllMessage;
import no.ccat.dto.ConceptCatalogue;
import no.ccat.dto.ConceptPublisherMessage;
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

    @RabbitListener(queues = "#{conceptPublisher.name}")
    public void receiveConceptPublisher(ConceptPublisherMessage conceptPublisherMessage) {
        logger.info("concept publisher received");

        this.harvestAdminClient.getDataSources(conceptPublisherMessage.queryParams())
                .forEach(conceptHarvester::harvestFromSingleURLSource);
    }

    @RabbitListener(queues = "#{conceptAll.name}")
    public void receiveConceptAll(ConceptAllMessage conceptAllMessage) {
        logger.info("concept all received");

        this.harvestAdminClient.getDataSources(conceptAllMessage.queryParams())
                .forEach(conceptHarvester::harvestFromSingleURLSource);
    }

    @RabbitListener(queues = "#{concept.name}")
    public void receiveConcept(Concept concept) {
        logger.info("concept received");

        this.harvestAdminClient.getDataSources(concept.queryParams())
                .forEach(conceptHarvester::harvestFromSingleURLSource);
    }

    @RabbitListener(queues = "#{conceptCatalogue.name}")
    public void receiveConceptCatalogue(ConceptCatalogue conceptCatalogue) {
        logger.info("concept catalogue received");

        this.harvestAdminClient.getDataSources(conceptCatalogue.queryParams())
                .forEach(conceptHarvester::harvestFromSingleURLSource);
    }
}
