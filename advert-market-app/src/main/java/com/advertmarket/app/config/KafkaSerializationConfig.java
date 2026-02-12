package com.advertmarket.app.config;

import com.advertmarket.shared.event.EventEnvelopeDeserializer;
import com.advertmarket.shared.event.EventEnvelopeSerializer;
import com.advertmarket.shared.event.EventTypeRegistry;
import com.advertmarket.shared.json.JsonFacade;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Provides JSON facade and event envelope serialization beans.
 */
@Configuration
public class KafkaSerializationConfig {

    /** JSON facade wrapping Spring's ObjectMapper. */
    @Bean
    public JsonFacade jsonFacade(ObjectMapper objectMapper) {
        return new JsonFacade(objectMapper);
    }

    /** Event envelope serializer. */
    @Bean
    public EventEnvelopeSerializer eventEnvelopeSerializer(
            JsonFacade jsonFacade) {
        return new EventEnvelopeSerializer(jsonFacade);
    }

    /** Event envelope deserializer with type registry. */
    @Bean
    public EventEnvelopeDeserializer eventEnvelopeDeserializer(
            JsonFacade jsonFacade,
            EventTypeRegistry eventTypeRegistry) {
        return new EventEnvelopeDeserializer(
                jsonFacade, eventTypeRegistry);
    }
}
