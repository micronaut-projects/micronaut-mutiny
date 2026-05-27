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
import io.micronaut.http.client.HttpClientConfiguration;
import io.micronaut.http.client.sse.SseClient;
import io.micronaut.http.sse.Event;
import io.micronaut.mutiny.http.client.sse.BridgedMutinySseClient;
import io.smallrye.mutiny.Multi;
import org.jspecify.annotations.Nullable;

import java.net.URL;

/**
 * Mutiny variation of the {@link SseClient} interface.
 *
 * @author Sergio del Amo
 * @since 1.0.0
 */
public interface MutinySseClient extends AutoCloseable {

    /**
     * Stream server-sent events.
     *
     * @param request The request
     * @param <I> The request body type
     * @return The event stream
     */
    <I> Multi<Event<ByteBuffer<?>>> eventStream(HttpRequest<I> request);

    /**
     * Stream server-sent events.
     *
     * @param request The request
     * @param eventType The event type
     * @param <I> The request body type
     * @param <B> The event body type
     * @return The event stream
     */
    <I, B> Multi<Event<B>> eventStream(HttpRequest<I> request, Argument<B> eventType);

    /**
     * Stream server-sent events.
     *
     * @param request The request
     * @param eventType The event type
     * @param errorType The error type
     * @param <I> The request body type
     * @param <B> The event body type
     * @return The event stream
     */
    <I, B> Multi<Event<B>> eventStream(HttpRequest<I> request, Argument<B> eventType, Argument<?> errorType);

    /**
     * Stream server-sent events.
     *
     * @param request The request
     * @param eventType The event type
     * @param <I> The request body type
     * @param <B> The event body type
     * @return The event stream
     */
    default <I, B> Multi<Event<B>> eventStream(HttpRequest<I> request, Class<B> eventType) {
        return eventStream(request, Argument.of(eventType));
    }

    /**
     * Stream server-sent events.
     *
     * @param uri The URI
     * @param eventType The event type
     * @param <B> The event body type
     * @return The event stream
     */
    default <B> Multi<Event<B>> eventStream(String uri, Class<B> eventType) {
        return eventStream(HttpRequest.GET(uri), Argument.of(eventType));
    }

    /**
     * Stream server-sent events.
     *
     * @param uri The URI
     * @param eventType The event type
     * @param <B> The event body type
     * @return The event stream
     */
    default <B> Multi<Event<B>> eventStream(String uri, Argument<B> eventType) {
        return eventStream(HttpRequest.GET(uri), eventType);
    }

    /**
     * Create a new {@link MutinySseClient}.
     *
     * @param url The base URL
     * @return The client
     */
    static MutinySseClient create(@Nullable URL url) {
        return new BridgedMutinySseClient(SseClient.create(url));
    }

    /**
     * Create a new {@link MutinySseClient} with the specified configuration.
     *
     * @param url The base URL
     * @param configuration The client configuration
     * @return The client
     */
    static MutinySseClient create(@Nullable URL url, HttpClientConfiguration configuration) {
        return new BridgedMutinySseClient(SseClient.create(url, configuration));
    }
}
