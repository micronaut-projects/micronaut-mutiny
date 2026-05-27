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
package io.micronaut.mutiny.http.client.proxy;

import io.micronaut.context.BeanContext;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.client.DefaultHttpClientConfiguration;
import io.micronaut.http.client.LoadBalancer;
import io.micronaut.http.client.ProxyHttpClient;
import io.micronaut.http.client.ProxyHttpClientRegistry;
import io.micronaut.http.client.ProxyRequestOptions;
import io.micronaut.inject.InjectionPoint;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;

class MutinyProxyHttpClientUnitTest {

    @Test
    void bridgedProxyClientDelegatesCalls() {
        HttpRequest<?> request = HttpRequest.GET("/proxy");
        ProxyRequestOptions options = ProxyRequestOptions.getDefault();
        MutableHttpResponse<?> response = HttpResponse.ok("proxied");
        AtomicReference<HttpRequest<?>> capturedRequest = new AtomicReference<>();
        AtomicReference<ProxyRequestOptions> capturedOptions = new AtomicReference<>();

        ProxyHttpClient delegate = proxy(ProxyHttpClient.class, (method, args) -> switch (method.getName()) {
            case "proxy" -> {
                capturedRequest.set((HttpRequest<?>) args[0]);
                if (args.length == 2) {
                    capturedOptions.set((ProxyRequestOptions) args[1]);
                }
                yield publisherOf(response);
            }
            default -> throw new UnsupportedOperationException(method.getName());
        });

        BridgedMutinyProxyHttpClient client = new BridgedMutinyProxyHttpClient(delegate);

        assertSame(response, client.proxy(request).await().indefinitely());
        assertSame(request, capturedRequest.get());
        assertSame(response, client.proxy(request, options).await().indefinitely());
        assertSame(options, capturedOptions.get());
    }

    @Test
    void proxyFactoryWrapsRegistryClient() {
        MutableHttpResponse<?> response = HttpResponse.ok("wrapped");
        ProxyHttpClient delegate = proxy(ProxyHttpClient.class, (method, args) -> publisherOf(response));
        AtomicReference<Object[]> argsRef = new AtomicReference<>();
        ProxyHttpClientRegistry<?> registry = proxy(ProxyHttpClientRegistry.class, (method, args) -> {
            if (method.getName().equals("resolveProxyHttpClient")) {
                argsRef.set(args);
                return delegate;
            }
            throw new UnsupportedOperationException(method.getName());
        });

        MutinyProxyHttpClientFactory factory = new MutinyProxyHttpClientFactory(registry);
        DefaultHttpClientConfiguration configuration = new DefaultHttpClientConfiguration();
        MutinyProxyHttpClient client = factory.proxyHttpClient(null, null, configuration, null);

        assertInstanceOf(BridgedMutinyProxyHttpClient.class, client);
        assertEquals("wrapped", client.proxy(HttpRequest.GET("/factory")).await().indefinitely().body());
        assertSame(configuration, argsRef.get()[2]);
    }

    @Test
    void staticCreateMethodsProduceProxyClients() throws Exception {
        java.net.URL url = new java.net.URL("http://localhost");
        DefaultHttpClientConfiguration configuration = new DefaultHttpClientConfiguration();

        MutinyProxyHttpClient client = MutinyProxyHttpClient.create(url);
        MutinyProxyHttpClient configuredClient = MutinyProxyHttpClient.create(url, configuration);

        assertInstanceOf(BridgedMutinyProxyHttpClient.class, client);
        assertInstanceOf(BridgedMutinyProxyHttpClient.class, configuredClient);
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
