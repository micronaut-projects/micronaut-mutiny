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
package io.micronaut.mutiny.http.client;

import io.micronaut.context.BeanContext;
import io.micronaut.core.io.buffer.ByteBuffer;
import io.micronaut.core.type.Argument;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.client.BlockingHttpClient;
import io.micronaut.http.client.DefaultHttpClientConfiguration;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.LoadBalancer;
import io.micronaut.http.client.ReactiveClientResultTransformer;
import io.micronaut.http.client.StreamingHttpClient;
import io.micronaut.http.client.StreamingHttpClientRegistry;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.inject.InjectionPoint;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MutinyHttpClientUnitTest {

    @Test
    void bridgedHttpClientDelegatesToUnderlyingClient() {
        BlockingHttpClient blockingHttpClient = proxy(BlockingHttpClient.class, (method, args) -> null);
        HttpResponse<String> exchangeResponse = HttpResponse.ok("exchange");
        AtomicReference<HttpRequest<?>> exchangeRequest = new AtomicReference<>();
        AtomicReference<Argument<?>> exchangeBodyType = new AtomicReference<>();
        AtomicReference<Argument<?>> exchangeErrorType = new AtomicReference<>();
        AtomicReference<HttpRequest<?>> retrieveRequest = new AtomicReference<>();
        AtomicReference<Argument<?>> retrieveBodyType = new AtomicReference<>();
        AtomicReference<Argument<?>> retrieveErrorType = new AtomicReference<>();
        AtomicBoolean started = new AtomicBoolean();
        AtomicBoolean stopped = new AtomicBoolean();
        AtomicBoolean closed = new AtomicBoolean();

        HttpClient httpClient = proxy(HttpClient.class, (method, args) -> switch (method.getName()) {
            case "toBlocking" -> blockingHttpClient;
            case "exchange" -> {
                exchangeRequest.set((HttpRequest<?>) args[0]);
                exchangeBodyType.set((Argument<?>) args[1]);
                exchangeErrorType.set((Argument<?>) args[2]);
                yield publisherOf(exchangeResponse);
            }
            case "retrieve" -> {
                retrieveRequest.set((HttpRequest<?>) args[0]);
                retrieveBodyType.set((Argument<?>) args[1]);
                retrieveErrorType.set((Argument<?>) args[2]);
                yield publisherOf("retrieve");
            }
            case "isRunning" -> true;
            case "start" -> {
                started.set(true);
                yield httpClientProxyPlaceholder();
            }
            case "stop" -> {
                stopped.set(true);
                yield httpClientProxyPlaceholder();
            }
            case "close" -> {
                closed.set(true);
                yield null;
            }
            default -> throw new UnsupportedOperationException(method.getName());
        });

        BridgedMutinyHttpClient client = new BridgedMutinyHttpClient(httpClient);
        HttpRequest<Object> request = HttpRequest.GET(URI.create("/test"));

        assertSame(blockingHttpClient, client.toBlocking());
        assertSame(exchangeResponse, client.exchange(request, Argument.STRING, Argument.STRING).await().indefinitely());
        assertEquals("retrieve", client.retrieve(request, Argument.STRING, Argument.STRING).await().indefinitely());
        assertSame(request, exchangeRequest.get());
        assertSame(Argument.STRING, exchangeBodyType.get());
        assertSame(Argument.STRING, exchangeErrorType.get());
        assertSame(request, retrieveRequest.get());
        assertSame(Argument.STRING, retrieveBodyType.get());
        assertSame(Argument.STRING, retrieveErrorType.get());
        assertTrue(client.isRunning());
        assertSame(client, client.start());
        assertSame(client, client.stop());
        assertTrue(started.get());
        assertTrue(stopped.get());
        client.close();
        assertTrue(closed.get());
    }

    @Test
    void mutinyHttpClientDefaultMethodsDelegateToCoreMethods() {
        AtomicReference<HttpRequest<?>> exchangeRequest = new AtomicReference<>();
        AtomicReference<Argument<?>> exchangeBodyType = new AtomicReference<>();
        AtomicReference<Argument<?>> exchangeErrorType = new AtomicReference<>();
        AtomicReference<HttpRequest<?>> retrieveRequest = new AtomicReference<>();
        AtomicReference<Argument<?>> retrieveBodyType = new AtomicReference<>();
        AtomicReference<Argument<?>> retrieveErrorType = new AtomicReference<>();
        AtomicBoolean closed = new AtomicBoolean();

        MutinyHttpClient client = new MutinyHttpClient() {
            @Override
            public BlockingHttpClient toBlocking() {
                return null;
            }

            @Override
            public <I, O, E> Uni<HttpResponse<O>> exchange(HttpRequest<I> request, Argument<O> bodyType, Argument<E> errorType) {
                exchangeRequest.set(request);
                exchangeBodyType.set(bodyType);
                exchangeErrorType.set(errorType);
                Object body = bodyType.getType() == ByteBuffer.class ? buffer("ok") : "ok";
                return Uni.createFrom().item((HttpResponse<O>) HttpResponse.ok(body));
            }

            @Override
            public <I, O, E> Uni<O> retrieve(HttpRequest<I> request, Argument<O> bodyType, Argument<E> errorType) {
                retrieveRequest.set(request);
                retrieveBodyType.set(bodyType);
                retrieveErrorType.set(errorType);
                Object value = bodyType.getType() == String.class ? "body" : 42;
                return Uni.createFrom().item((O) value);
            }

            @Override
            public boolean isRunning() {
                return true;
            }

            @Override
            public MutinyHttpClient start() {
                return this;
            }

            @Override
            public MutinyHttpClient stop() {
                return this;
            }

            @Override
            public void close() {
                closed.set(true);
            }
        };

        assertEquals("ok", client.exchange(HttpRequest.GET("/a"), String.class).await().indefinitely().body());
        assertInstanceOf(ByteBuffer.class, client.exchange(HttpRequest.GET("/aa")).await().indefinitely().body());
        assertEquals("ok", client.exchange("/b", String.class).await().indefinitely().body());
        assertInstanceOf(ByteBuffer.class, client.exchange("/c").await().indefinitely().body());
        assertEquals("body", client.retrieve(HttpRequest.GET("/cc"), Argument.STRING).await().indefinitely());
        assertEquals("body", client.retrieve(HttpRequest.GET("/d")).await().indefinitely());
        assertEquals("body", client.retrieve("/e").await().indefinitely());
        assertEquals(42, client.retrieve(HttpRequest.GET("/f"), Integer.class).await().indefinitely());

        assertEquals("/c", exchangeRequest.get().getUri().toString());
        assertEquals(ByteBuffer.class, exchangeBodyType.get().getType());
        assertSame(MutinyHttpClient.DEFAULT_ERROR_TYPE, exchangeErrorType.get());
        assertEquals("/f", retrieveRequest.get().getUri().toString());
        assertSame(Integer.class, retrieveBodyType.get().getType());
        assertSame(MutinyHttpClient.DEFAULT_ERROR_TYPE, retrieveErrorType.get());
        client.close();
        assertTrue(closed.get());
    }

    @Test
    void staticCreateMethodsProduceCloseableClients() throws Exception {
        DefaultHttpClientConfiguration configuration = new DefaultHttpClientConfiguration();
        java.net.URL url = new java.net.URL("http://localhost");

        MutinyHttpClient httpClient = MutinyHttpClient.create(url);
        MutinyHttpClient configuredHttpClient = MutinyHttpClient.create(url, configuration);
        MutinyStreamingHttpClient streamingHttpClient = MutinyStreamingHttpClient.create(url);
        MutinyStreamingHttpClient configuredStreamingHttpClient = MutinyStreamingHttpClient.create(url, configuration);
        MutinySseClient sseClient = MutinySseClient.create(url);
        MutinySseClient configuredSseClient = MutinySseClient.create(url, configuration);

        assertInstanceOf(BridgedMutinyHttpClient.class, httpClient);
        assertInstanceOf(BridgedMutinyHttpClient.class, configuredHttpClient);
        assertInstanceOf(BridgedMutinyStreamingHttpClient.class, streamingHttpClient);
        assertInstanceOf(BridgedMutinyStreamingHttpClient.class, configuredStreamingHttpClient);
        assertInstanceOf(io.micronaut.mutiny.http.client.sse.BridgedMutinySseClient.class, sseClient);
        assertInstanceOf(io.micronaut.mutiny.http.client.sse.BridgedMutinySseClient.class, configuredSseClient);

        httpClient.close();
        configuredHttpClient.close();
        streamingHttpClient.close();
        configuredStreamingHttpClient.close();
        sseClient.close();
        configuredSseClient.close();
    }

    @Test
    void bridgedStreamingHttpClientDelegatesStreamMethods() {
        HttpRequest<Object> request = HttpRequest.GET("/stream");
        Publisher<ByteBuffer<?>> data = publisherOf(buffer("data"));
        Publisher<HttpResponse<ByteBuffer<?>>> responses = publisherOf(HttpResponse.ok(buffer("response")));
        Publisher<Map<String, Object>> json = publisherOf(Map.of("value", 1));
        Publisher<Integer> typed = publisherOf(1, 2);
        AtomicReference<Argument<?>> errorArg = new AtomicReference<>();
        AtomicReference<Argument<?>> typeArg = new AtomicReference<>();

        StreamingHttpClient streamingHttpClient = proxy(StreamingHttpClient.class, (method, args) -> switch (method.getName()) {
            case "toBlocking" -> null;
            case "exchange" -> publisherOf(HttpResponse.ok("ok"));
            case "retrieve" -> publisherOf("body");
            case "isRunning" -> true;
            case "start", "stop" -> httpClientProxyPlaceholder();
            case "close" -> null;
            case "dataStream" -> {
                if (args.length == 2) {
                    errorArg.set((Argument<?>) args[1]);
                }
                yield data;
            }
            case "exchangeStream" -> {
                if (args.length == 2) {
                    errorArg.set((Argument<?>) args[1]);
                }
                yield responses;
            }
            case "jsonStream" -> {
                if (args.length == 1) {
                    yield json;
                }
                typeArg.set((Argument<?>) args[1]);
                if (args.length == 3) {
                    errorArg.set((Argument<?>) args[2]);
                }
                yield typed;
            }
            default -> throw new UnsupportedOperationException(method.getName());
        });

        BridgedMutinyStreamingHttpClient client = new BridgedMutinyStreamingHttpClient(streamingHttpClient);

        assertEquals("data", asString(client.dataStream(request).collect().first().await().indefinitely()));
        assertEquals("data", asString(client.dataStream(request, Argument.STRING).collect().first().await().indefinitely()));
        assertEquals("response", asString(client.exchangeStream(request).collect().first().await().indefinitely().body()));
        assertEquals("response", asString(client.exchangeStream(request, Argument.STRING).collect().first().await().indefinitely().body()));
        assertEquals(Map.of("value", 1), client.jsonStream(request).collect().first().await().indefinitely());
        assertEquals(List.of(1, 2), client.jsonStream(request, Argument.INT).collect().asList().await().indefinitely());
        assertEquals(List.of(1, 2), client.jsonStream(request, Argument.INT, Argument.STRING).collect().asList().await().indefinitely());

        assertSame(Argument.INT, typeArg.get());
        assertSame(Argument.STRING, errorArg.get());
    }

    @Test
    void streamingHttpClientDefaultJsonStreamMethodUsesArgumentOfClass() {
        AtomicReference<Argument<?>> type = new AtomicReference<>();
        MutinyStreamingHttpClient client = new StubMutinyStreamingHttpClient(type);

        assertEquals(List.of("value"), client.jsonStream(HttpRequest.GET("/json"), String.class).collect().asList().await().indefinitely());
        assertSame(String.class, type.get().getType());
    }

    @Test
    void httpClientFactoryWrapsResolvedStreamingClient() {
        HttpRequest<Object> request = HttpRequest.GET("/factory");
        StreamingHttpClient delegate = proxy(StreamingHttpClient.class, (method, args) -> switch (method.getName()) {
            case "toBlocking" -> null;
            case "exchange" -> publisherOf(HttpResponse.ok("ok"));
            case "retrieve" -> publisherOf("body");
            case "isRunning" -> true;
            case "start", "stop", "close" -> null;
            case "dataStream" -> publisherOf(buffer("factory"));
            case "exchangeStream" -> publisherOf(HttpResponse.ok(buffer("factory")));
            case "jsonStream" -> publisherOf(Map.of("ok", true));
            default -> throw new UnsupportedOperationException(method.getName());
        });
        AtomicReference<Object[]> capturedArgs = new AtomicReference<>();
        StreamingHttpClientRegistry<?> registry = proxy(StreamingHttpClientRegistry.class, (method, args) -> {
            if (method.getName().equals("resolveStreamingHttpClient")) {
                capturedArgs.set(args);
                return delegate;
            }
            throw new UnsupportedOperationException(method.getName());
        });

        MutinyHttpClientFactory factory = new MutinyHttpClientFactory(registry);
        InjectionPoint<?> injectionPoint = null;
        LoadBalancer loadBalancer = null;
        DefaultHttpClientConfiguration configuration = new DefaultHttpClientConfiguration();
        BeanContext beanContext = null;

        MutinyStreamingHttpClient client = factory.streamingHttpClient(injectionPoint, loadBalancer, configuration, beanContext);

        assertInstanceOf(BridgedMutinyStreamingHttpClient.class, client);
        assertEquals("factory", asString(client.dataStream(request).collect().first().await().indefinitely()));
        assertSame(injectionPoint, capturedArgs.get()[0]);
        assertSame(loadBalancer, capturedArgs.get()[1]);
        assertSame(configuration, capturedArgs.get()[2]);
        assertSame(beanContext, capturedArgs.get()[3]);
    }

    @Test
    void utilityMethodsConvertReactiveStreamsPublishers() {
        assertEquals("uni", MutinyHttpClientUtils.toUni(publisherOf("uni")).await().indefinitely());
        assertEquals(List.of("one", "two"), MutinyHttpClientUtils.toMulti(publisherOf("one", "two")).collect().asList().await().indefinitely());
    }

    @Test
    void reactiveClientResultTransformerHandlesNotFoundAndPassThroughCases() {
        MutinyReactiveClientResultTransformer transformer = new MutinyReactiveClientResultTransformer();
        HttpClientResponseException notFound = new HttpClientResponseException("missing", HttpResponse.status(HttpStatus.NOT_FOUND));
        HttpClientResponseException badRequest = new HttpClientResponseException("bad", HttpResponse.status(HttpStatus.BAD_REQUEST));

        assertNull(((Uni<?>) transformer.transform(Uni.createFrom().failure(notFound))).await().indefinitely());
        assertEquals(List.of(), ((Multi<?>) transformer.transform(Multi.createFrom().failure(notFound))).collect().asList().await().indefinitely());
        HttpClientResponseException uniException = assertThrows(HttpClientResponseException.class,
            () -> ((Uni<?>) transformer.transform(Uni.createFrom().failure(badRequest))).await().indefinitely());
        HttpClientResponseException multiException = assertThrows(HttpClientResponseException.class,
            () -> ((Multi<?>) transformer.transform(Multi.createFrom().failure(badRequest))).collect().asList().await().indefinitely());
        assertSame(badRequest, uniException);
        assertSame(badRequest, multiException);
        assertSame("value", transformer.transform("value"));
    }

    @Test
    void reactiveClientResultTransformerLeavesSuccessfulMutinyTypesUntouched() {
        MutinyReactiveClientResultTransformer transformer = new MutinyReactiveClientResultTransformer();

        assertEquals("ok", ((Uni<String>) transformer.transform(Uni.createFrom().item("ok"))).await().indefinitely());
        assertEquals(List.of("a", "b"), ((Multi<String>) transformer.transform(Multi.createFrom().items("a", "b"))).collect().asList().await().indefinitely());
    }

    private static ByteBuffer<?> buffer(String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        return proxy(ByteBuffer.class, (method, args) -> switch (method.getName()) {
            case "asNativeBuffer", "toByteArray" -> bytes;
            case "readableBytes", "writerIndex", "maxCapacity", "writableBytes", "readerIndex", "indexOf" -> bytes.length;
            case "asNioBuffer" -> java.nio.ByteBuffer.wrap(bytes);
            case "capacity", "read", "write", "slice" -> method.getReturnType().isAssignableFrom(ByteBuffer.class)
                ? proxy(ByteBuffer.class, (m, a) -> null) : (byte) bytes[0];
            case "readCharSequence" -> value;
            case "getByte" -> bytes[(int) args[0]];
            case "toInputStream" -> new java.io.ByteArrayInputStream(bytes);
            case "toOutputStream" -> new java.io.ByteArrayOutputStream();
            default -> null;
        });
    }

    private static String asString(ByteBuffer<?> buffer) {
        return new String((byte[]) buffer.asNativeBuffer(), StandardCharsets.UTF_8);
    }

    private static <T> Publisher<T> publisherOf(T... values) {
        return subscriber -> subscriber.onSubscribe(new org.reactivestreams.Subscription() {
            private int index;
            private boolean cancelled;

            @Override
            public void request(long n) {
                if (cancelled) {
                    return;
                }
                while (index < values.length) {
                    subscriber.onNext(values[index++]);
                }
                subscriber.onComplete();
            }

            @Override
            public void cancel() {
                cancelled = true;
            }
        });
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, ThrowingInvocationHandler handler) {
        InvocationHandler invocationHandler = (proxy, method, args) -> {
            if (method.getDeclaringClass() == Object.class) {
                return switch (method.getName()) {
                    case "toString" -> type.getSimpleName() + "Proxy";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> null;
                };
            }
            return handler.invoke(method, args == null ? new Object[0] : args);
        };
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, invocationHandler);
    }

    private static HttpClient httpClientProxyPlaceholder() {
        return proxy(HttpClient.class, (method, args) -> null);
    }

    @FunctionalInterface
    private interface ThrowingInvocationHandler {
        Object invoke(Method method, Object[] args) throws Throwable;
    }

    private static final class StubMutinyStreamingHttpClient implements MutinyStreamingHttpClient {
        private final AtomicReference<Argument<?>> type;

        private StubMutinyStreamingHttpClient(AtomicReference<Argument<?>> type) {
            this.type = type;
        }

        @Override
        public BlockingHttpClient toBlocking() {
            return null;
        }

        @Override
        public <I, O, E> Uni<HttpResponse<O>> exchange(HttpRequest<I> request, Argument<O> bodyType, Argument<E> errorType) {
            return Uni.createFrom().item((HttpResponse<O>) HttpResponse.ok("ok"));
        }

        @Override
        public <I, O, E> Uni<O> retrieve(HttpRequest<I> request, Argument<O> bodyType, Argument<E> errorType) {
            return Uni.createFrom().item((O) "body");
        }

        @Override
        public boolean isRunning() {
            return false;
        }

        @Override
        public MutinyHttpClient start() {
            return this;
        }

        @Override
        public MutinyHttpClient stop() {
            return this;
        }

        @Override
        public void close() {
        }

        @Override
        public <I> Multi<ByteBuffer<?>> dataStream(HttpRequest<I> request) {
            return Multi.createFrom().empty();
        }

        @Override
        public <I> Multi<ByteBuffer<?>> dataStream(HttpRequest<I> request, Argument<?> errorType) {
            return Multi.createFrom().empty();
        }

        @Override
        public <I> Multi<HttpResponse<ByteBuffer<?>>> exchangeStream(HttpRequest<I> request) {
            return Multi.createFrom().empty();
        }

        @Override
        public <I> Multi<HttpResponse<ByteBuffer<?>>> exchangeStream(HttpRequest<I> request, Argument<?> errorType) {
            return Multi.createFrom().empty();
        }

        @Override
        public <I> Multi<Map<String, Object>> jsonStream(HttpRequest<I> request) {
            return Multi.createFrom().empty();
        }

        @Override
        public <I, O> Multi<O> jsonStream(HttpRequest<I> request, Argument<O> type) {
            this.type.set(type);
            return Multi.createFrom().items((O) "value");
        }

        @Override
        public <I, O> Multi<O> jsonStream(HttpRequest<I> request, Argument<O> type, Argument<?> errorType) {
            this.type.set(type);
            return Multi.createFrom().items((O) "value");
        }
    }
}
