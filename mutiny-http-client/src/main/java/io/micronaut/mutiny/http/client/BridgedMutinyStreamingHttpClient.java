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
import io.micronaut.core.io.buffer.ByteBuffer;
import io.micronaut.core.type.Argument;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.client.StreamingHttpClient;
import io.smallrye.mutiny.Multi;

import java.util.Map;

/**
 * Internal bridge for the streaming HTTP client.
 *
 * @author Sergio del Amo
 * @since 1.0.0
 */
@Internal
class BridgedMutinyStreamingHttpClient extends BridgedMutinyHttpClient implements MutinyStreamingHttpClient {

    private final StreamingHttpClient streamingHttpClient;

    /**
     * Default constructor.
     *
     * @param streamingHttpClient Streaming HTTP Client
     */
    BridgedMutinyStreamingHttpClient(StreamingHttpClient streamingHttpClient) {
        super(streamingHttpClient);
        this.streamingHttpClient = streamingHttpClient;
    }

    @Override
    public <I> Multi<ByteBuffer<?>> dataStream(HttpRequest<I> request) {
        return MutinyHttpClientUtils.toMulti(streamingHttpClient.dataStream(request));
    }

    @Override
    public <I> Multi<ByteBuffer<?>> dataStream(HttpRequest<I> request, Argument<?> errorType) {
        return MutinyHttpClientUtils.toMulti(streamingHttpClient.dataStream(request, errorType));
    }

    @Override
    public <I> Multi<HttpResponse<ByteBuffer<?>>> exchangeStream(HttpRequest<I> request) {
        return MutinyHttpClientUtils.toMulti(streamingHttpClient.exchangeStream(request));
    }

    @Override
    public <I> Multi<HttpResponse<ByteBuffer<?>>> exchangeStream(HttpRequest<I> request, Argument<?> errorType) {
        return MutinyHttpClientUtils.toMulti(streamingHttpClient.exchangeStream(request, errorType));
    }

    @Override
    public <I> Multi<Map<String, Object>> jsonStream(HttpRequest<I> request) {
        return MutinyHttpClientUtils.toMulti(streamingHttpClient.jsonStream(request));
    }

    @Override
    public <I, O> Multi<O> jsonStream(HttpRequest<I> request, Argument<O> type) {
        return MutinyHttpClientUtils.toMulti(streamingHttpClient.jsonStream(request, type));
    }

    @Override
    public <I, O> Multi<O> jsonStream(HttpRequest<I> request, Argument<O> type, Argument<?> errorType) {
        return MutinyHttpClientUtils.toMulti(streamingHttpClient.jsonStream(request, type, errorType));
    }
}
