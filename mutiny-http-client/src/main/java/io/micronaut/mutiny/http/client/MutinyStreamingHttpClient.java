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

import io.micronaut.core.io.buffer.ByteBuffer;
import io.micronaut.core.type.Argument;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.client.HttpClientConfiguration;
import io.micronaut.http.client.StreamingHttpClient;
import io.smallrye.mutiny.Multi;
import org.jspecify.annotations.Nullable;

import java.net.URL;
import java.util.Map;

/**
 * Mutiny variation of the {@link StreamingHttpClient} interface.
 *
 * @author Sergio del Amo
 * @since 1.0.0
 */
public interface MutinyStreamingHttpClient extends MutinyHttpClient {

    /**
     * Request a stream of data where each emitted item is a {@link ByteBuffer} instance.
     *
     * @param request The request
     * @param <I> The request body type
     * @return The response stream
     */
    <I> Multi<ByteBuffer<?>> dataStream(HttpRequest<I> request);

    /**
     * Request a stream of data where each emitted item is a {@link ByteBuffer} instance.
     *
     * @param request The request
     * @param errorType The error type
     * @param <I> The request body type
     * @return The response stream
     */
    <I> Multi<ByteBuffer<?>> dataStream(HttpRequest<I> request, Argument<?> errorType);

    /**
     * Request a stream of data where each emitted item is wrapped in an {@link HttpResponse}.
     *
     * @param request The request
     * @param <I> The request body type
     * @return The response stream
     */
    <I> Multi<HttpResponse<ByteBuffer<?>>> exchangeStream(HttpRequest<I> request);

    /**
     * Request a stream of data where each emitted item is wrapped in an {@link HttpResponse}.
     *
     * @param request The request
     * @param errorType The error type
     * @param <I> The request body type
     * @return The response stream
     */
    <I> Multi<HttpResponse<ByteBuffer<?>>> exchangeStream(HttpRequest<I> request, Argument<?> errorType);

    /**
     * Request a stream of JSON objects.
     *
     * @param request The request
     * @param <I> The request body type
     * @return The JSON stream
     */
    <I> Multi<Map<String, Object>> jsonStream(HttpRequest<I> request);

    /**
     * Request a stream of decoded JSON objects.
     *
     * @param request The request
     * @param type The body type
     * @param <I> The request body type
     * @param <O> The response body type
     * @return The JSON stream
     */
    <I, O> Multi<O> jsonStream(HttpRequest<I> request, Argument<O> type);

    /**
     * Request a stream of decoded JSON objects.
     *
     * @param request The request
     * @param type The body type
     * @param errorType The error type
     * @param <I> The request body type
     * @param <O> The response body type
     * @return The JSON stream
     */
    <I, O> Multi<O> jsonStream(HttpRequest<I> request, Argument<O> type, Argument<?> errorType);

    /**
     * Request a stream of decoded JSON objects.
     *
     * @param request The request
     * @param type The body type
     * @param <I> The request body type
     * @param <O> The response body type
     * @return The JSON stream
     */
    default <I, O> Multi<O> jsonStream(HttpRequest<I> request, Class<O> type) {
        return jsonStream(request, Argument.of(type));
    }

    /**
     * Create a new {@link MutinyStreamingHttpClient}.
     *
     * @param url The base URL
     * @return The client
     */
    static MutinyStreamingHttpClient create(@Nullable URL url) {
        return new BridgedMutinyStreamingHttpClient(StreamingHttpClient.create(url));
    }

    /**
     * Create a new {@link MutinyStreamingHttpClient} with the specified configuration.
     *
     * @param url The base URL
     * @param configuration The client configuration
     * @return The client
     */
    static MutinyStreamingHttpClient create(@Nullable URL url, HttpClientConfiguration configuration) {
        return new BridgedMutinyStreamingHttpClient(StreamingHttpClient.create(url, configuration));
    }
}
