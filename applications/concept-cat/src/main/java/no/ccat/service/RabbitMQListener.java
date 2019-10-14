package no.ccat.service;

import lombok.RequiredArgsConstructor;
import no.ccat.dto.ConceptAllMessage;
import no.ccat.dto.ConceptPublisherMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;

@Service
@RequiredArgsConstructor
public class RabbitMQListener {
    private static final Logger logger = LoggerFactory.getLogger(RabbitMQListener.class);
    private final HarvestAdminClient harvestAdminClient;
    private final ConceptHarvester conceptHarvester;

    @RabbitListener(queues = "#{conceptPublisher.name}")
    public void receiveConceptPublisher(ConceptPublisherMessage conceptPublisherMessage) {

        logger.info("harvest message received");

        MultiValueMap<String, String> p = conceptPublisherMessage.queryParams();
        this.harvestAdminClient.getDataSources(p)
                .forEach(conceptHarvester::harvestFromSingleURLSource);
    }

    @RabbitListener(queues = "#{conceptAll.name}")
    public void receiveConceptAll(ConceptAllMessage conceptAllMessage) {

        logger.info("conceptAll received");

        MultiValueMap<String, String> p = conceptAllMessage.queryParams();
        this.harvestAdminClient.getDataSources(p)
                .forEach(conceptHarvester::harvestFromSingleURLSource);
    }
}
