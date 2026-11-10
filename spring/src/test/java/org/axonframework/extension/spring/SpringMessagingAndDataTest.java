package org.axonframework.extension.spring;

import java.util.Map;
import tools.jackson.databind.ObjectMapper;

import org.axonframework.messaging.core.MessageType;
import org.axonframework.messaging.eventhandling.GenericEventMessage;
import org.axonframework.extension.spring.data.JacksonPageDeserializer;
import org.axonframework.extension.spring.messaging.DefaultEventMessageConverter;
import org.axonframework.extension.spring.messaging.EventMessageConverter;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class SpringMessagingAndDataTest {
    @Test
    void convertsAxonEventToSpringMessageAndBack() {
        EventMessageConverter converter = new DefaultEventMessageConverter();
        org.axonframework.messaging.eventhandling.EventMessage axon =
                new GenericEventMessage(new MessageType("example", "Event", MessageType.DEFAULT_VERSION), "payload", Map.of("key", "value"));

        org.springframework.messaging.Message<?> outbound = converter.convertToOutboundMessage(axon);

        assertNotNull(outbound);
        assertEquals(axon.payload(), outbound.getPayload());

        org.axonframework.messaging.eventhandling.EventMessage inbound = converter.convertFromInboundMessage(outbound);

        assertEquals("payload", inbound.payload());
        assertEquals(axon.identifier(), inbound.identifier());
    }

    @Test
    void deserializesSpringDataPage() throws Exception {
        JacksonPageDeserializer deserializer = new JacksonPageDeserializer();
        String json = "{\"content\":[\"a\",\"b\"],\"number\":2,\"size\":2,\"totalElements\":5}";

        tools.jackson.databind.module.SimpleModule module =
                new tools.jackson.databind.module.SimpleModule();
        module.addDeserializer(Page.class, deserializer);
        ObjectMapper mapper = tools.jackson.databind.json.JsonMapper.builder().addModule(module).build();
        Page<?> page = mapper.readValue(json, Page.class);

        assertNotNull(page);
        assertEquals(2, page.getContent().size());
        assertEquals(2, page.getNumber());
        assertEquals(6, page.getTotalElements());
    }
}
