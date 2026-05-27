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

import io.micronaut.core.annotation.Internal;
import io.micronaut.core.type.Argument;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.client.BlockingHttpClient;
import io.micronaut.http.client.HttpClient;
import io.smallrye.mutiny.Uni;
import org.jspecify.annotations.Nullable;

/**
 * Internal bridge for the HTTP client.
 *
 * @author Sergio del Amo
 * @since 1.0.0
 */
@Internal
class BridgedMutinyHttpClient implements MutinyHttpClient {

    private final HttpClient httpClient;

    /**
     * Default constructor.
     *
     * @param httpClient HTTP Client
     */
    BridgedMutinyHttpClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    public BlockingHttpClient toBlocking() {
        return httpClient.toBlocking();
    }

    @Override
    public <I, O, E> Uni<HttpResponse<O>> exchange(HttpRequest<I> request, @Nullable Argument<O> bodyType, Argument<E> errorType) {
        return MutinyHttpClientUtils.toUni(httpClient.exchange(request, bodyType, errorType));
    }

    @Override
    public <I, O, E> Uni<O> retrieve(HttpRequest<I> request, Argument<O> bodyType, Argument<E> errorType) {
        return MutinyHttpClientUtils.toUni(httpClient.retrieve(request, bodyType, errorType));
    }

    @Override
    public boolean isRunning() {
        return httpClient.isRunning();
    }

    @Override
    public MutinyHttpClient start() {
        httpClient.start();
        return this;
    }

    @Override
    public MutinyHttpClient stop() {
        httpClient.stop();
        return this;
    }

    @Override
    public void close() {
        httpClient.close();
    }
}
