package org.axonframework.integrationtests;

import tools.jackson.databind.ObjectMapper;
import org.axonframework.conversion.jackson.JsonNodeToByteArrayConverter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class ConversionSmokeTest {
    @Test
    void convertersLoad() {
        assertNotNull(new JsonNodeToByteArrayConverter(new ObjectMapper()));
    }
}
