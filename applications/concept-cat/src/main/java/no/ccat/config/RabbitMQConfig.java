package no.ccat.config;

import lombok.RequiredArgsConstructor;
import no.ccat.service.ConceptHarvester;
import no.ccat.service.HarvestAdminClient;
import no.ccat.service.RabbitMQListener;
import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class RabbitMQConfig {

    private final HarvestAdminClient harvestAdminClient;
    private final ConceptHarvester conceptHarvester;

    @Bean
    public RabbitMQListener receiver() {
        return new RabbitMQListener(harvestAdminClient, conceptHarvester);
    }

    @Bean
    public Queue conceptPublisher() {
        return new AnonymousQueue();
    }

    @Bean
    public Queue conceptAll() {
        return new AnonymousQueue();
    }

    @Bean
    public Queue concept() {
        return new AnonymousQueue();
    }

    @Bean
    public Queue conceptCatalogue() {
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
    public Binding declareBindingConceptPublisher(TopicExchange topicExchange, Queue conceptPublisher) {
        return BindingBuilder.bind(conceptPublisher).to(topicExchange).with("conceptPublisher.HarvestTrigger");
    }

    @Bean
    public Binding declareBindingConceptAll(TopicExchange topicExchange, Queue conceptAll) {
        return BindingBuilder.bind(conceptAll).to(topicExchange).with("conceptAll.HarvestTrigger");
    }

    @Bean
    public Binding declareBindingConcept(TopicExchange topicExchange, Queue concept) {
        return BindingBuilder.bind(concept).to(topicExchange).with("concept.HarvestTrigger");
    }

    @Bean
    public Binding declareBindingConceptCatalogue(TopicExchange topicExchange, Queue conceptCatalogue) {
        return BindingBuilder.bind(conceptCatalogue).to(topicExchange).with("conceptCatalogue.HarvestTrigger");
    }
}
