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

import io.micronaut.http.HttpRequest;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.client.HttpClientConfiguration;
import io.micronaut.http.client.ProxyHttpClient;
import io.micronaut.http.client.ProxyRequestOptions;
import io.smallrye.mutiny.Uni;
import org.jspecify.annotations.Nullable;

import java.net.URL;

/**
 * Mutiny variation of the {@link ProxyHttpClient} interface.
 *
 * @author Sergio del Amo
 * @since 1.0.0
 */
public interface MutinyProxyHttpClient {

    /**
     * Proxy the given request.
     *
     * @param request The request
     * @return The response
     */
    Uni<MutableHttpResponse<?>> proxy(HttpRequest<?> request);

    /**
     * Proxy the given request.
     *
     * @param request The request
     * @param options Further options for the proxy request
     * @return The response
     */
    Uni<MutableHttpResponse<?>> proxy(HttpRequest<?> request, ProxyRequestOptions options);

    /**
     * Create a new {@link MutinyProxyHttpClient}.
     *
     * @param url The base URL
     * @return The client
     */
    static MutinyProxyHttpClient create(@Nullable URL url) {
        return new BridgedMutinyProxyHttpClient(ProxyHttpClient.create(url));
    }

    /**
     * Create a new {@link MutinyProxyHttpClient} with the specified configuration.
     *
     * @param url The base URL
     * @param configuration The client configuration
     * @return The client
     */
    static MutinyProxyHttpClient create(@Nullable URL url, HttpClientConfiguration configuration) {
        return new BridgedMutinyProxyHttpClient(ProxyHttpClient.create(url, configuration));
    }
}
