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
package io.micronaut.mutiny.http.client.websocket;

import io.micronaut.context.BeanContext;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.MutableHttpRequest;
import io.micronaut.http.client.DefaultHttpClientConfiguration;
import io.micronaut.http.client.LoadBalancer;
import io.micronaut.inject.InjectionPoint;
import io.micronaut.websocket.WebSocketClient;
import io.micronaut.websocket.WebSocketClientRegistry;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MutinyWebSocketClientUnitTest {

    @Test
    void bridgedWebSocketClientDelegatesConnectionsAndClose() {
        AutoCloseable endpoint = () -> {
        };
        AtomicReference<MutableHttpRequest<?>> requestRef = new AtomicReference<>();
        AtomicReference<Map<String, Object>> parametersRef = new AtomicReference<>();
        AtomicBoolean closed = new AtomicBoolean();
        WebSocketClient delegate = proxy(WebSocketClient.class, new Class<?>[0], (method, args) -> switch (method.getName()) {
            case "connect" -> {
                if (args[1] instanceof MutableHttpRequest<?> request) {
                    requestRef.set(request);
                } else {
                    parametersRef.set((Map<String, Object>) args[1]);
                }
                yield publisherOf(endpoint);
            }
            case "close" -> {
                closed.set(true);
                yield null;
            }
            default -> throw new UnsupportedOperationException(method.getName());
        });

        BridgedMutinyWebSocketClient client = new BridgedMutinyWebSocketClient(delegate);

        assertSame(endpoint, client.connect(AutoCloseable.class, HttpRequest.GET("/ws")).await().indefinitely());
        assertEquals("/ws", requestRef.get().getUri().toString());
        assertSame(endpoint, client.connect(AutoCloseable.class, Map.of("id", 1)).await().indefinitely());
        assertEquals(Map.of("id", 1), parametersRef.get());
        client.close();
        assertTrue(closed.get());
    }

    @Test
    void webSocketDefaultMethodsDelegateToRequestConnect() {
        AtomicReference<MutableHttpRequest<?>> requestRef = new AtomicReference<>();
        MutinyWebSocketClient client = new MutinyWebSocketClient() {
            @Override
            public <T extends AutoCloseable> io.smallrye.mutiny.Uni<T> connect(Class<T> clientEndpointType, MutableHttpRequest<?> request) {
                requestRef.set(request);
                return io.smallrye.mutiny.Uni.createFrom().nullItem();
            }

            @Override
            public <T extends AutoCloseable> io.smallrye.mutiny.Uni<T> connect(Class<T> clientEndpointType, Map<String, Object> parameters) {
                return io.smallrye.mutiny.Uni.createFrom().nullItem();
            }

            @Override
            public void close() {
                // No-op: this test double only verifies request-based default methods.
            }
        };

        client.connect(AutoCloseable.class, URI.create("/one")).await().indefinitely();
        assertEquals("/one", requestRef.get().getUri().toString());
        client.connect(AutoCloseable.class, "/two").await().indefinitely();
        assertEquals("/two", requestRef.get().getUri().toString());
    }

    @Test
    void webSocketFactoryWrapsResolvedClient() {
        AutoCloseable endpoint = () -> {
        };
        WebSocketClient delegate = proxy(WebSocketClient.class, new Class<?>[0], (method, args) -> switch (method.getName()) {
            case "connect" -> publisherOf(endpoint);
            case "close" -> null;
            default -> throw new UnsupportedOperationException(method.getName());
        });
        AtomicReference<Object[]> argsRef = new AtomicReference<>();
        WebSocketClientRegistry<?> registry = proxy(WebSocketClientRegistry.class, new Class<?>[0], (method, args) -> {
            if (method.getName().equals("resolveWebSocketClient")) {
                argsRef.set(args);
                return delegate;
            }
            throw new UnsupportedOperationException(method.getName());
        });

        MutinyWebSocketClientFactory factory = new MutinyWebSocketClientFactory(registry);
        DefaultHttpClientConfiguration configuration = new DefaultHttpClientConfiguration();
        MutinyWebSocketClient client = factory.webSocketClient(null, null, configuration, null);

        assertInstanceOf(BridgedMutinyWebSocketClient.class, client);
        assertSame(endpoint, client.connect(AutoCloseable.class, HttpRequest.GET("/factory")).await().indefinitely());
        assertSame(configuration, argsRef.get()[2]);
    }

    @Test
    void staticCreateMethodsProduceWebSocketClients() {
        URI uri = URI.create("ws://localhost");
        DefaultHttpClientConfiguration configuration = new DefaultHttpClientConfiguration();

        MutinyWebSocketClient client = MutinyWebSocketClient.create(uri);
        MutinyWebSocketClient configuredClient = MutinyWebSocketClient.create(uri, configuration);

        assertInstanceOf(BridgedMutinyWebSocketClient.class, client);
        assertInstanceOf(BridgedMutinyWebSocketClient.class, configuredClient);
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
}
