/*
 * Copyright (c) 2026. Terrence Daniels
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.axonframework.messaging.commandhandling.annotation;

import org.axonframework.common.AxonConfigurationException;
import org.axonframework.messaging.commandhandling.CommandMessage;
import org.axonframework.messaging.commandhandling.GenericCommandMessage;
import org.axonframework.messaging.core.MessageType;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Test class validating the {@link AnnotationRoutingStrategy}.
 */
class AnnotationRoutingStrategyTest {

    private final AnnotationRoutingStrategy strategy = new AnnotationRoutingStrategy();

    private static CommandMessage command(Object payload) {
        return new GenericCommandMessage(new MessageType(payload.getClass()), payload);
    }

    @Command(routingKey = "id")
    record FieldRouted(String id, String other) {

    }

    @Command(routingKey = "identifier")
    static class MethodRouted {

        String getIdentifier() {
            return "from-method";
        }
    }

    @Command(routingKey = "number")
    record NumberRouted(int number) {

    }

    @Command(routingKey = "id")
    record NullRouted(String id) {

    }

    @Command(routingKey = "missing")
    record UnknownMember(String id) {

    }

    @Command
    record NoRoutingKey(String id) {

    }

    record NotAnnotated(String id) {

    }

    @Command(routingKey = "id")
    static class Failing {

        String getId() {
            throw new IllegalStateException("boom");
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface Routing {

        String routingKey() default "";
    }

    @Routing(routingKey = "id")
    record CustomAnnotated(String id) {

    }

    @Test
    void routesOnTheAnnotatedField() {
        assertEquals("abc", strategy.getRoutingKey(command(new FieldRouted("abc", "x"))));
    }

    @Test
    void routesOnTheAnnotatedMethod() {
        assertEquals("from-method", strategy.getRoutingKey(command(new MethodRouted())));
    }

    @Test
    void routingKeyIsTheStringRepresentationOfTheIdentifier() {
        assertEquals("42", strategy.getRoutingKey(command(new NumberRouted(42))));
    }

    @Test
    void anIdentifierThatIsNullYieldsANullRoutingKey() {
        assertNull(strategy.getRoutingKey(command(new NullRouted(null))));
    }

    @Test
    void aRoutingKeyNamingAnUnknownMemberYieldsNull() {
        assertNull(strategy.getRoutingKey(command(new UnknownMember("a"))));
    }

    @Test
    void anAnnotationWithoutRoutingKeyYieldsNull() {
        assertNull(strategy.getRoutingKey(command(new NoRoutingKey("a"))));
    }

    @Test
    void aPayloadWithoutTheAnnotationYieldsNull() {
        assertNull(strategy.getRoutingKey(command(new NotAnnotated("a"))));
    }

    @Test
    void resolvesRepeatedlyForTheSamePayloadType() {
        assertEquals("a", strategy.getRoutingKey(command(new FieldRouted("a", "x"))));
        assertEquals("b", strategy.getRoutingKey(command(new FieldRouted("b", "x"))));
    }

    @Test
    void supportsACustomAnnotationType() {
        AnnotationRoutingStrategy custom = new AnnotationRoutingStrategy(Routing.class);

        assertEquals("a", custom.getRoutingKey(command(new CustomAnnotated("a"))));
        assertNull(custom.getRoutingKey(command(new FieldRouted("a", "x"))));
    }

    @Test
    void rejectsANullAnnotationType() {
        assertThrows(NullPointerException.class, () -> new AnnotationRoutingStrategy(null));
    }

    @Test
    void failuresWhileExtractingTheKeyAreReportedAsConfigurationExceptions() {
        assertThrows(AxonConfigurationException.class, () -> strategy.getRoutingKey(command(new Failing())));
    }
}
