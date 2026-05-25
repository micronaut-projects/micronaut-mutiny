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

import io.micronaut.http.HttpRequest;
import io.micronaut.http.MutableHttpRequest;
import io.micronaut.http.client.HttpClientConfiguration;
import io.micronaut.websocket.WebSocketClient;
import io.smallrye.mutiny.Uni;
import org.jspecify.annotations.Nullable;

import java.net.URI;
import java.util.Map;

/**
 * Mutiny variation of the {@link WebSocketClient} interface.
 *
 * @author Sergio del Amo
 * @since 1.0.0
 */
public interface MutinyWebSocketClient extends AutoCloseable {

    /**
     * Connect the given client endpoint type to the URI over WebSocket.
     *
     * @param clientEndpointType The endpoint type
     * @param request The original request to establish the connection
     * @param <T> The endpoint type
     * @return The connected endpoint
     */
    <T extends AutoCloseable> Uni<T> connect(Class<T> clientEndpointType, MutableHttpRequest<?> request);

    /**
     * Connect the given client endpoint type to the URI over WebSocket.
     *
     * @param clientEndpointType The endpoint type
     * @param parameters The URI parameters
     * @param <T> The endpoint type
     * @return The connected endpoint
     */
    <T extends AutoCloseable> Uni<T> connect(Class<T> clientEndpointType, Map<String, Object> parameters);

    /**
     * Connect the given client endpoint type to the URI over WebSocket.
     *
     * @param clientEndpointType The endpoint type
     * @param uri The URI
     * @param <T> The endpoint type
     * @return The connected endpoint
     */
    default <T extends AutoCloseable> Uni<T> connect(Class<T> clientEndpointType, URI uri) {
        return connect(clientEndpointType, HttpRequest.GET(uri));
    }

    /**
     * Connect the given client endpoint type to the URI over WebSocket.
     *
     * @param clientEndpointType The endpoint type
     * @param uri The URI
     * @param <T> The endpoint type
     * @return The connected endpoint
     */
    default <T extends AutoCloseable> Uni<T> connect(Class<T> clientEndpointType, String uri) {
        return connect(clientEndpointType, URI.create(uri));
    }

    @Override
    void close();

    /**
     * Create a new {@link MutinyWebSocketClient}.
     *
     * @param uri The base URI
     * @return The client
     */
    static MutinyWebSocketClient create(@Nullable URI uri) {
        return new BridgedMutinyWebSocketClient(WebSocketClient.create(uri));
    }

    /**
     * Create a new {@link MutinyWebSocketClient} with the specified configuration.
     *
     * @param uri The base URI
     * @param configuration The client configuration
     * @return The client
     */
    static MutinyWebSocketClient create(@Nullable URI uri, HttpClientConfiguration configuration) {
        return new BridgedMutinyWebSocketClient(WebSocketClient.create(uri, configuration));
    }
}
