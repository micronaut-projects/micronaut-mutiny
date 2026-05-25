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

import io.micronaut.core.annotation.Internal;
import io.micronaut.http.MutableHttpRequest;
import io.micronaut.mutiny.http.client.MutinyHttpClientUtils;
import io.micronaut.websocket.WebSocketClient;
import io.smallrye.mutiny.Uni;

import java.util.Map;

/**
 * Internal bridge for the WebSocket client.
 *
 * @author Sergio del Amo
 * @since 1.0.0
 */
@Internal
class BridgedMutinyWebSocketClient implements MutinyWebSocketClient {

    private final WebSocketClient webSocketClient;

    BridgedMutinyWebSocketClient(WebSocketClient webSocketClient) {
        this.webSocketClient = webSocketClient;
    }

    @Override
    public <T extends AutoCloseable> Uni<T> connect(Class<T> clientEndpointType, MutableHttpRequest<?> request) {
        return MutinyHttpClientUtils.toUni(webSocketClient.connect(clientEndpointType, request));
    }

    @Override
    public <T extends AutoCloseable> Uni<T> connect(Class<T> clientEndpointType, Map<String, Object> parameters) {
        return MutinyHttpClientUtils.toUni(webSocketClient.connect(clientEndpointType, parameters));
    }

    @Override
    public void close() {
        webSocketClient.close();
    }
}
