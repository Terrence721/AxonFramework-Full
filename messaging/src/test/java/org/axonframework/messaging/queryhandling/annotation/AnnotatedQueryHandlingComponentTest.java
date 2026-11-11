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
package org.axonframework.messaging.queryhandling.annotation;

import org.axonframework.common.infra.ComponentDescriptor;
import org.axonframework.conversion.Converter;
import org.axonframework.messaging.core.ClassBasedMessageTypeResolver;
import org.axonframework.messaging.core.EmptyApplicationContext;
import org.axonframework.messaging.core.GenericMessage;
import org.axonframework.messaging.core.Message;
import org.axonframework.messaging.core.MessageStream;
import org.axonframework.messaging.core.MessageType;
import org.axonframework.messaging.core.MessageTypeResolver;
import org.axonframework.messaging.core.QualifiedName;
import org.axonframework.messaging.core.annotation.AnnotatedHandlerInspector;
import org.axonframework.messaging.core.annotation.AnnotatedMessageHandlingMemberDefinition;
import org.axonframework.messaging.core.annotation.DefaultParameterResolverFactory;
import org.axonframework.messaging.core.annotation.MessageHandlingMember;
import org.axonframework.messaging.core.annotation.MultiHandlerDefinition;
import org.axonframework.messaging.core.annotation.UnsupportedHandlerException;
import org.axonframework.messaging.core.conversion.DelegatingMessageConverter;
import org.axonframework.messaging.core.conversion.MessageConverter;
import org.axonframework.messaging.core.unitofwork.SimpleUnitOfWorkFactory;
import org.axonframework.messaging.core.unitofwork.UnitOfWorkFactory;
import org.axonframework.messaging.queryhandling.GenericQueryMessage;
import org.axonframework.messaging.queryhandling.QueryExecutionException;
import org.axonframework.messaging.queryhandling.QueryMessage;
import org.axonframework.messaging.queryhandling.QueryResponseMessage;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class validating the {@link AnnotatedQueryHandlingComponent} and the {@link MethodQueryHandlerDefinition}.
 */
class AnnotatedQueryHandlingComponentTest {

    private static final DefaultParameterResolverFactory PARAMETERS = new DefaultParameterResolverFactory();
    private static final MultiHandlerDefinition HANDLERS = MultiHandlerDefinition.ordered(
            new MethodQueryHandlerDefinition(), new AnnotatedMessageHandlingMemberDefinition()
    );

    private static final Converter IDENTITY_CONVERTER = new Converter() {
        @SuppressWarnings("unchecked")
        @Override
        public <T> @Nullable T convert(@Nullable Object input, Type targetType) {
            return (T) input;
        }

        @Override
        public void describeTo(ComponentDescriptor descriptor) {
        }
    };

    private final MessageTypeResolver typeResolver = new ClassBasedMessageTypeResolver();
    private final MessageConverter converter = new DelegatingMessageConverter(IDENTITY_CONVERTER);
    private final UnitOfWorkFactory unitOfWorkFactory = new SimpleUnitOfWorkFactory(EmptyApplicationContext.INSTANCE);

    static class Handlers {

        final AtomicReference<String> lastGreeting = new AtomicReference<>();

        @QueryHandler(queryName = "greet")
        String greet(String name) {
            lastGreeting.set(name);
            return "Hello " + name;
        }

        @QueryHandler(queryName = "boom")
        String boom(String ignored) {
            throw new IllegalStateException("kaboom");
        }

        @QueryHandler(queryName = "executionFailure")
        String executionFailure(String ignored) {
            throw new QueryExecutionException("already wrapped", null);
        }

        @QueryHandler
        Integer lengthOf(Long payload) {
            return payload.intValue();
        }
    }

    private AnnotatedQueryHandlingComponent<Handlers> component(Handlers handlers) {
        return new AnnotatedQueryHandlingComponent<>(
                handlers,
                PARAMETERS,
                HANDLERS,
                typeResolver,
                converter
        );
    }

    private MessageStream<QueryResponseMessage> handle(AnnotatedQueryHandlingComponent<Handlers> component,
                                                       String name,
                                                       Object payload) {
        QueryMessage query = new GenericQueryMessage(new MessageType(name), payload);
        return unitOfWorkFactory.create()
                                .executeWithResult(context -> CompletableFuture.completedFuture(
                                        component.handle(query, context)
                                ))
                                .join();
    }

    // --- registration ---

    @Test
    void supportsQueriesByExplicitNameAndByResolvedPayloadType() {
        Set<QualifiedName> supported = component(new Handlers()).supportedQueries();

        assertEquals(
                Set.of(new QualifiedName("greet"),
                       new QualifiedName("boom"),
                       new QualifiedName("executionFailure"),
                       typeResolver.resolveOrThrow(Long.class).qualifiedName()),
                supported
        );
    }

    @Test
    void supportedQueriesAreACopy() {
        AnnotatedQueryHandlingComponent<Handlers> component = component(new Handlers());

        assertThrows(UnsupportedOperationException.class, () -> component.supportedQueries().clear());
    }

    @Test
    void constructorRejectsNullArguments() {
        assertThrows(NullPointerException.class, () -> new AnnotatedQueryHandlingComponent<>(
                null,
                PARAMETERS,
                HANDLERS,
                typeResolver,
                converter
        ));
        assertThrows(NullPointerException.class, () -> new AnnotatedQueryHandlingComponent<>(
                new Handlers(),
                PARAMETERS,
                HANDLERS,
                typeResolver,
                null
        ));
    }

    // --- handling ---

    @Test
    void handlesAQueryAndReturnsTheResultAsAQueryResponse() {
        Handlers handlers = new Handlers();

        MessageStream<QueryResponseMessage> result = handle(component(handlers), "greet", "Bob");

        QueryResponseMessage response = result.first().asCompletableFuture().join().message();
        assertEquals("Hello Bob", response.payload());
        assertEquals("Bob", handlers.lastGreeting.get());
    }

    @Test
    void handlesAQueryRegisteredByPayloadTypeUsingTheResolvedName() {
        String name = typeResolver.resolveOrThrow(Long.class).qualifiedName().fullName();

        MessageStream<QueryResponseMessage> result = handle(component(new Handlers()), name, 42L);

        assertEquals(42, result.first().asCompletableFuture().join().message().payload());
    }

    @Test
    void queryWithoutHandlerFails() {
        MessageStream<QueryResponseMessage> result = handle(component(new Handlers()), "unknown", "x");

        assertTrue(result.error().isPresent() || result.first().asCompletableFuture().isCompletedExceptionally());
    }

    @Test
    void handlerExceptionIsWrappedInAQueryExecutionException() {
        MessageStream<QueryResponseMessage> result = handle(component(new Handlers()), "boom", "x");

        QueryExecutionException wrapped = assertInstanceOf(QueryExecutionException.class,
                                                           result.error().orElseThrow());
        assertInstanceOf(IllegalStateException.class, wrapped.getCause());
    }

    @Test
    void queryExecutionExceptionIsNotWrappedAgain() {
        MessageStream<QueryResponseMessage> result = handle(component(new Handlers()), "executionFailure", "x");

        QueryExecutionException error = assertInstanceOf(QueryExecutionException.class, result.error().orElseThrow());
        assertEquals("already wrapped", error.getMessage());
    }

    // --- description ---

    @Test
    void describesItself() {
        Handlers handlers = new Handlers();
        Map<String, Object> properties = new HashMap<>();
        ComponentDescriptor descriptor = (ComponentDescriptor) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{ComponentDescriptor.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("describeProperty") && args.length == 2) {
                        properties.put((String) args[0], args[1]);
                    } else if (method.getName().equals("describeWrapperOf")) {
                        properties.put("wrapped", args[0]);
                    }
                    return null;
                }
        );

        component(handlers).describeTo(descriptor);

        assertSame(handlers, properties.get("target"));
        assertSame(typeResolver, properties.get("messageTypeResolver"));
        assertSame(converter, properties.get("converter"));
        assertTrue(properties.containsKey("wrapped"));
    }

    // --- MethodQueryHandlerDefinition ---

    @SuppressWarnings("java:S119")
    static class ResultTypes {

        @QueryHandler(queryName = "plain")
        String plain(String q) {
            return "";
        }

        @QueryHandler(queryName = "primitive")
        int primitive(String q) {
            return 1;
        }

        @QueryHandler(queryName = "optional")
        Optional<String> optional(String q) {
            return Optional.empty();
        }

        @QueryHandler(queryName = "list")
        List<Integer> list(String q) {
            return List.of();
        }

        @QueryHandler(queryName = "future")
        CompletableFuture<Long> future(String q) {
            return null;
        }

        @QueryHandler(queryName = "upper")
        List<? extends Number> upper(String q) {
            return List.of();
        }

        @QueryHandler(queryName = "unbounded")
        List<?> unbounded(String q) {
            return List.of();
        }
    }

    @SuppressWarnings("java:S119")
    static class VoidHandler {

        @QueryHandler(queryName = "void")
        void none(String q) {
        }
    }

    @SuppressWarnings("java:S119")
    static class BoxedVoidHandler {

        @QueryHandler(queryName = "boxedVoid")
        Void none(String q) {
            return null;
        }
    }

    private static <T> QueryHandlingMember<?> member(Class<T> type, String queryName) {
        AnnotatedHandlerInspector<T> inspector = AnnotatedHandlerInspector.inspectType(
                type,
                new ClassBasedMessageTypeResolver(),
                PARAMETERS,
                HANDLERS
        );
        for (MessageHandlingMember<? super T> handler : inspector.getUniqueHandlers(type, QueryMessage.class)) {
            QueryHandlingMember<?> queryMember = (QueryHandlingMember<?>) handler;
            if (queryName.equals(queryMember.queryName())) {
                return queryMember;
            }
        }
        throw new AssertionError("No query handler named " + queryName);
    }

    @Test
    void resultTypeIsTheMethodReturnTypeWhenNotWrapped() {
        assertEquals(String.class, member(ResultTypes.class, "plain").resultType());
    }

    @Test
    void primitiveResultTypesAreBoxed() {
        assertEquals(Integer.class, member(ResultTypes.class, "primitive").resultType());
    }

    @Test
    void wrappedResultTypesAreUnwrapped() {
        assertEquals(String.class, member(ResultTypes.class, "optional").resultType());
        assertEquals(Integer.class, member(ResultTypes.class, "list").resultType());
        assertEquals(Long.class, member(ResultTypes.class, "future").resultType());
    }

    @Test
    void wildcardResultTypesResolveToTheirUpperBound() {
        assertEquals(Number.class, member(ResultTypes.class, "upper").resultType());
        assertEquals(Object.class, member(ResultTypes.class, "unbounded").resultType());
    }

    @Test
    void queryNameIsTakenFromTheAnnotationWithoutFallback() {
        assertEquals("plain", member(ResultTypes.class, "plain").queryName());
    }

    @Test
    void membersWithoutAQueryNameAttributeAreNotWrapped() {
        MessageHandlingMember<Object> original = stubMember();

        assertSame(original, new MethodQueryHandlerDefinition().wrapHandler(original));
    }

    @Test
    void voidReturningHandlersAreRejected() {
        assertThrows(UnsupportedHandlerException.class, () -> member(VoidHandler.class, "void"));
    }

    @Test
    void boxedVoidReturningHandlersAreRejected() {
        assertThrows(UnsupportedHandlerException.class, () -> member(BoxedVoidHandler.class, "boxedVoid"));
    }

    @Test
    void wrappedMemberOnlyHandlesQueryMessages() {
        MessageHandlingMember<?> member = member(ResultTypes.class, "plain");
        Message notAQuery = new GenericMessage(new MessageType("plain"), "x");

        boolean canHandle = unitOfWorkFactory.create()
                                             .executeWithResult(context -> CompletableFuture.completedFuture(
                                                     member.canHandle(notAQuery, context)
                                             ))
                                             .join();

        assertFalse(canHandle);
    }

    @SuppressWarnings("unchecked")
    private static MessageHandlingMember<Object> stubMember() {
        return (MessageHandlingMember<Object>) Proxy.newProxyInstance(
                AnnotatedQueryHandlingComponentTest.class.getClassLoader(),
                new Class<?>[]{MessageHandlingMember.class},
                (proxy, method, args) -> method.getName().equals("attribute") ? Optional.empty() : null
        );
    }
}
