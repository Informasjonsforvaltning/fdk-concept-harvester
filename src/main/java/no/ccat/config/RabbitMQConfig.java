package no.ccat.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import no.ccat.service.ConceptHarvester;
import no.ccat.service.HarvestAdminClient;
import no.ccat.service.RabbitMQListener;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class RabbitMQConfig {

    private final HarvestAdminClient harvestAdminClient;
    private final ConceptHarvester conceptHarvester;
    private final ObjectMapper objectMapper;

    @Bean
    public RabbitMQListener receiver() {
        return new RabbitMQListener(harvestAdminClient, conceptHarvester, objectMapper);
    }

    @Bean
    public Queue queue() {
        return new AnonymousQueue();
    }

    @Bean
    public Queue sendQueue() {
        return new AnonymousQueue();
    }

    @Bean
    public Jackson2JsonMessageConverter converter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public TopicExchange topicExchange() {
        return new TopicExchange("harvests", false, false);
    }

    @Bean
    public Binding binding(TopicExchange topicExchange, Queue queue) {
        return BindingBuilder.bind(queue).to(topicExchange).with("concept.*.HarvestTrigger");
    }

    @Bean
    public Binding sendBinding(TopicExchange topicExchange, Queue sendQueue) {
        return BindingBuilder.bind(sendQueue).to(topicExchange).with("concepts.harvester.UpdateSearchTrigger");
    }

    @Bean
    public AmqpTemplate jsonRabbitTemplate(ConnectionFactory connectionFactory) {
        final RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(converter());
        return rabbitTemplate;
    }
}
