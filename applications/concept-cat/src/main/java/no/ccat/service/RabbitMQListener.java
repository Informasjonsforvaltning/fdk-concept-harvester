package no.ccat.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class RabbitMQListener {

    private static final Logger logger = LoggerFactory.getLogger(RabbitMQListener.class);
    private final HarvestAdminClient harvestAdminClient;
    private final ConceptHarvester conceptHarvester;
    private final ObjectMapper objectMapper;

    private MultiValueMap<String, String> createQueryParams(JsonNode body) {
        // convert from map to multivaluemap for UriComponentBuilder
        Map<String, String> queryParams = objectMapper.convertValue(
                body,
                new TypeReference<Map<String, String>>() {
                });

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        queryParams.forEach(params::add);
        return params;
    }

    private void harvest(JsonNode body) {
        this.harvestAdminClient.getDataSources(createQueryParams(body))
                .forEach(conceptHarvester::harvestFromSingleURLSource);
    }

    @RabbitListener(queues = "#{queue.name}")
    public void receiveConceptPublisher(@Payload JsonNode body, Message message) {
        String routingKey = message.getMessageProperties().getReceivedRoutingKey().split("\\.")[0];
        logger.info(String.format("Received message from key: %s", routingKey));
        harvest(body);
    }
}
