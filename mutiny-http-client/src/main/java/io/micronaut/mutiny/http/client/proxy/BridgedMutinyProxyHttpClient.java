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

import io.micronaut.core.annotation.Internal;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.client.ProxyHttpClient;
import io.micronaut.http.client.ProxyRequestOptions;
import io.micronaut.mutiny.http.client.MutinyHttpClientUtils;
import io.smallrye.mutiny.Uni;

/**
 * Internal bridge for the proxy HTTP client.
 *
 * @author Sergio del Amo
 * @since 1.0.0
 */
@Internal
class BridgedMutinyProxyHttpClient implements MutinyProxyHttpClient {

    private final ProxyHttpClient proxyHttpClient;

    BridgedMutinyProxyHttpClient(ProxyHttpClient proxyHttpClient) {
        this.proxyHttpClient = proxyHttpClient;
    }

    @Override
    public Uni<MutableHttpResponse<?>> proxy(HttpRequest<?> request) {
        return MutinyHttpClientUtils.toUni(proxyHttpClient.proxy(request));
    }

    @Override
    public Uni<MutableHttpResponse<?>> proxy(HttpRequest<?> request, ProxyRequestOptions options) {
        return MutinyHttpClientUtils.toUni(proxyHttpClient.proxy(request, options));
    }
}
