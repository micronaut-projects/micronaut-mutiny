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

import io.micronaut.context.LifeCycle;
import io.micronaut.core.io.buffer.ByteBuffer;
import io.micronaut.core.type.Argument;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.client.BlockingHttpClient;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.HttpClientConfiguration;
import io.micronaut.http.hateoas.JsonError;
import io.smallrye.mutiny.Uni;
import org.jspecify.annotations.Nullable;

import java.io.Closeable;
import java.net.URL;

/**
 * Mutiny variation of the {@link HttpClient} interface.
 *
 * @author Sergio del Amo
 * @since 1.0.0
 */
public interface MutinyHttpClient extends Closeable, LifeCycle<MutinyHttpClient> {

    /**
     * The default error type.
     */
    Argument<JsonError> DEFAULT_ERROR_TYPE = HttpClient.DEFAULT_ERROR_TYPE;

    /**
     * Returns a blocking HTTP client suitable for testing and non-production scenarios.
     *
     * @return The blocking HTTP client
     */
    BlockingHttpClient toBlocking();

    /**
     * Perform an HTTP request and emit the full HTTP response.
     *
     * @param request The request
     * @param bodyType The body type
     * @param errorType The error type
     * @param <I> The request body type
     * @param <O> The response body type
     * @param <E> The error body type
     * @return The response
     */
    <I, O, E> Uni<HttpResponse<O>> exchange(HttpRequest<I> request, @Nullable Argument<O> bodyType, Argument<E> errorType);

    /**
     * Perform an HTTP request and emit the full HTTP response.
     *
     * @param request The request
     * @param bodyType The body type
     * @param <I> The request body type
     * @param <O> The response body type
     * @return The response
     */
    default <I, O> Uni<HttpResponse<O>> exchange(HttpRequest<I> request, Argument<O> bodyType) {
        return exchange(request, bodyType, DEFAULT_ERROR_TYPE);
    }

    /**
     * Perform an HTTP request and emit the full HTTP response.
     *
     * @param request The request
     * @param <I> The request body type
     * @return The response
     */
    default <I> Uni<HttpResponse<ByteBuffer<?>>> exchange(HttpRequest<I> request) {
        return exchange(request, byteBufferBodyType());
    }

    /**
     * Perform an HTTP GET request and emit the full HTTP response.
     *
     * @param uri The URI
     * @return The response
     */
    default Uni<HttpResponse<ByteBuffer<?>>> exchange(String uri) {
        return exchange(HttpRequest.GET(uri), byteBufferBodyType());
    }

    /**
     * Perform an HTTP GET request and emit the full HTTP response.
     *
     * @param uri The URI
     * @param bodyType The body type
     * @param <O> The response body type
     * @return The response
     */
    default <O> Uni<HttpResponse<O>> exchange(String uri, Class<O> bodyType) {
        return exchange(HttpRequest.GET(uri), Argument.of(bodyType));
    }

    /**
     * Perform an HTTP request and emit the full HTTP response.
     *
     * @param request The request
     * @param bodyType The body type
     * @param <I> The request body type
     * @param <O> The response body type
     * @return The response
     */
    default <I, O> Uni<HttpResponse<O>> exchange(HttpRequest<I> request, Class<O> bodyType) {
        return exchange(request, Argument.of(bodyType));
    }

    /**
     * Perform an HTTP request and emit the decoded body.
     *
     * @param request The request
     * @param bodyType The body type
     * @param errorType The error type
     * @param <I> The request body type
     * @param <O> The response body type
     * @param <E> The error body type
     * @return The decoded body
     */
    <I, O, E> Uni<O> retrieve(HttpRequest<I> request, Argument<O> bodyType, Argument<E> errorType);

    /**
     * Perform an HTTP request and emit the decoded body.
     *
     * @param request The request
     * @param bodyType The body type
     * @param <I> The request body type
     * @param <O> The response body type
     * @return The decoded body
     */
    default <I, O> Uni<O> retrieve(HttpRequest<I> request, Argument<O> bodyType) {
        return retrieve(request, bodyType, DEFAULT_ERROR_TYPE);
    }

    /**
     * Perform an HTTP request and emit the decoded body.
     *
     * @param request The request
     * @param bodyType The body type
     * @param <I> The request body type
     * @param <O> The response body type
     * @return The decoded body
     */
    default <I, O> Uni<O> retrieve(HttpRequest<I> request, Class<O> bodyType) {
        return retrieve(request, Argument.of(bodyType), DEFAULT_ERROR_TYPE);
    }

    /**
     * Perform an HTTP request and emit the decoded body as a string.
     *
     * @param request The request
     * @param <I> The request body type
     * @return The decoded body
     */
    default <I> Uni<String> retrieve(HttpRequest<I> request) {
        return retrieve(request, String.class);
    }

    /**
     * Perform an HTTP GET request and emit the decoded body as a string.
     *
     * @param uri The URI
     * @return The decoded body
     */
    default Uni<String> retrieve(String uri) {
        return retrieve(HttpRequest.GET(uri), String.class);
    }

    /**
     * Create a new {@link MutinyHttpClient}.
     *
     * @param url The base URL
     * @return The client
     */
    static MutinyHttpClient create(@Nullable URL url) {
        return new BridgedMutinyHttpClient(HttpClient.create(url));
    }

    /**
     * Create a new {@link MutinyHttpClient} with the specified configuration.
     *
     * @param url The base URL
     * @param configuration The client configuration
     * @return The client
     */
    static MutinyHttpClient create(@Nullable URL url, HttpClientConfiguration configuration) {
        return new BridgedMutinyHttpClient(HttpClient.create(url, configuration));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Argument<ByteBuffer<?>> byteBufferBodyType() {
        return (Argument) Argument.of(ByteBuffer.class, Argument.OBJECT_ARGUMENT);
    }
}
