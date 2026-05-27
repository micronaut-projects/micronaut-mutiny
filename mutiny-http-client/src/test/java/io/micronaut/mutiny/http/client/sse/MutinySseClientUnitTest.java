/*
 * Copyright 2017-2026 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.mutiny.http.client.sse;

import io.micronaut.context.BeanContext;
import io.micronaut.core.io.buffer.ByteBuffer;
import io.micronaut.core.type.Argument;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.client.DefaultHttpClientConfiguration;
import io.micronaut.http.client.LoadBalancer;
import io.micronaut.http.client.sse.SseClient;
import io.micronaut.http.client.sse.SseClientRegistry;
import io.micronaut.http.sse.Event;
import io.micronaut.inject.InjectionPoint;
import io.micronaut.mutiny.http.client.MutinySseClient;
import io.smallrye.mutiny.Multi;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MutinySseClientUnitTest {

    @Test
    void bridgedSseClientDelegatesAllEventStreamVariantsAndClosesAutoCloseableDelegate() throws Exception {
        HttpRequest<Object> request = HttpRequest.GET("/events");
        Event<String> typedEvent = Event.of("typed");
        Event<ByteBuffer<?>> rawEvent = Event.of(byteBuffer("raw"));
        AtomicReference<Argument<?>> eventType = new AtomicReference<>();
        AtomicReference<Argument<?>> errorType = new AtomicReference<>();
        AtomicBoolean closed = new AtomicBoolean();

        SseClient delegate = proxy(SseClient.class, new Class<?>[]{AutoCloseable.class}, (method, args) -> switch (method.getName()) {
            case "eventStream" -> {
                if (args.length >= 2) {
                    eventType.set((Argument<?>) args[1]);
                }
                if (args.length == 3) {
                    errorType.set((Argument<?>) args[2]);
                }
                yield args.length == 1 ? publisherOf(rawEvent) : publisherOf(typedEvent);
            }
            case "close" -> {
                closed.set(true);
                yield null;
            }
            default -> throw new UnsupportedOperationException(method.getName());
        });

        BridgedMutinySseClient client = new BridgedMutinySseClient(delegate);

        assertEquals("raw", new String((byte[]) client.eventStream(request).collect().first().await().indefinitely().getData().asNativeBuffer(), StandardCharsets.UTF_8));
        assertEquals("typed", client.eventStream(request, Argument.STRING).collect().first().await().indefinitely().getData());
        assertEquals("typed", client.eventStream(request, Argument.STRING, Argument.INT).collect().first().await().indefinitely().getData());
        assertSame(Argument.STRING, eventType.get());
        assertSame(Argument.INT, errorType.get());
        client.close();
        assertTrue(closed.get());
    }

    @Test
    void sseDefaultMethodsDelegateToArgumentVariants() {
        AtomicReference<HttpRequest<?>> requestRef = new AtomicReference<>();
        AtomicReference<Argument<?>> typeRef = new AtomicReference<>();
        MutinySseClient client = new MutinySseClient() {
            @Override
            public <I> Multi<Event<ByteBuffer<?>>> eventStream(HttpRequest<I> request) {
                return Multi.createFrom().empty();
            }

            @Override
            public <I, B> Multi<Event<B>> eventStream(HttpRequest<I> request, Argument<B> eventType) {
                requestRef.set(request);
                typeRef.set(eventType);
                return Multi.createFrom().item((Event<B>) Event.of("typed"));
            }

            @Override
            public <I, B> Multi<Event<B>> eventStream(HttpRequest<I> request, Argument<B> eventType, Argument<?> errorType) {
                return eventStream(request, eventType);
            }

            @Override
            public void close() {
                // No-op: this test double only exercises the default SSE methods.
            }
        };

        assertEquals("typed", client.eventStream(HttpRequest.GET("/typed"), String.class).collect().first().await().indefinitely().getData());
        assertEquals("/typed", requestRef.get().getUri().toString());
        assertSame(String.class, typeRef.get().getType());
        assertEquals("typed", client.eventStream("/typed2", String.class).collect().first().await().indefinitely().getData());
        assertEquals("/typed2", requestRef.get().getUri().toString());
        assertEquals("typed", client.eventStream("/typed3", Argument.STRING).collect().first().await().indefinitely().getData());
        assertEquals("/typed3", requestRef.get().getUri().toString());
    }

    @Test
    void sseFactoryWrapsResolvedClient() {
        Event<String> event = Event.of("factory");
        SseClient delegate = proxy(SseClient.class, new Class<?>[0], (method, args) -> publisherOf(event));
        AtomicReference<Object[]> argsRef = new AtomicReference<>();
        SseClientRegistry<?> registry = proxy(SseClientRegistry.class, new Class<?>[0], (method, args) -> {
            if (method.getName().equals("resolveSseClient")) {
                argsRef.set(args);
                return delegate;
            }
            throw new UnsupportedOperationException(method.getName());
        });

        MutinySseClientFactory factory = new MutinySseClientFactory(registry);
        DefaultHttpClientConfiguration configuration = new DefaultHttpClientConfiguration();
        MutinySseClient client = factory.sseClient(null, null, configuration, null);

        assertInstanceOf(BridgedMutinySseClient.class, client);
        assertEquals("factory", client.eventStream(HttpRequest.GET("/factory"), String.class).collect().first().await().indefinitely().getData());
        assertSame(configuration, argsRef.get()[2]);
    }

    @Test
    void bridgedSseClientCloseIsNoOpForNonAutoCloseableDelegate() throws Exception {
        AtomicBoolean interacted = new AtomicBoolean();
        SseClient delegate = proxy(SseClient.class, new Class<?>[0], (method, args) -> {
            interacted.set(true);
            return publisherOf(Event.of("noop"));
        });
        new BridgedMutinySseClient(delegate).close();
        assertFalse(interacted.get());
    }

    @Test
    void staticCreateMethodsProduceSseClients() throws Exception {
        java.net.URL url = new java.net.URL("http://localhost");
        DefaultHttpClientConfiguration configuration = new DefaultHttpClientConfiguration();

        MutinySseClient client = MutinySseClient.create(url);
        MutinySseClient configuredClient = MutinySseClient.create(url, configuration);

        assertInstanceOf(BridgedMutinySseClient.class, client);
        assertInstanceOf(BridgedMutinySseClient.class, configuredClient);
        client.close();
        configuredClient.close();
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> primaryType, Class<?>[] additionalTypes, ThrowingInvocationHandler handler) {
        Class<?>[] types = new Class<?>[additionalTypes.length + 1];
        types[0] = primaryType;
        System.arraycopy(additionalTypes, 0, types, 1, additionalTypes.length);
        InvocationHandler invocationHandler = (proxy, method, args) -> {
            if (method.getDeclaringClass() == Object.class) {
                return switch (method.getName()) {
                    case "toString" -> "Proxy";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> null;
                };
            }
            return handler.invoke(method, args == null ? new Object[0] : args);
        };
        return primaryType.cast(Proxy.newProxyInstance(primaryType.getClassLoader(), types, invocationHandler));
    }

    private static <T> Publisher<T> publisherOf(T value) {
        return subscriber -> subscriber.onSubscribe(new org.reactivestreams.Subscription() {
            private boolean done;

            @Override
            public void request(long n) {
                if (!done && n > 0) {
                    done = true;
                    subscriber.onNext(value);
                    subscriber.onComplete();
                }
            }

            @Override
            public void cancel() {
                done = true;
            }
        });
    }

    @FunctionalInterface
    private interface ThrowingInvocationHandler {
        Object invoke(Method method, Object[] args) throws Throwable;
    }

    @SuppressWarnings("unchecked")
    private static ByteBuffer<?> byteBuffer(String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        return proxy(ByteBuffer.class, new Class<?>[0], (method, args) -> switch (method.getName()) {
            case "asNativeBuffer", "toByteArray" -> bytes;
            case "readableBytes", "writerIndex", "maxCapacity", "writableBytes", "readerIndex", "indexOf" -> bytes.length;
            case "asNioBuffer" -> java.nio.ByteBuffer.wrap(bytes);
            case "readCharSequence" -> value;
            case "getByte" -> bytes[(int) args[0]];
            case "toInputStream" -> new java.io.ByteArrayInputStream(bytes);
            case "toOutputStream" -> new java.io.ByteArrayOutputStream();
            default -> null;
        });
    }
}
