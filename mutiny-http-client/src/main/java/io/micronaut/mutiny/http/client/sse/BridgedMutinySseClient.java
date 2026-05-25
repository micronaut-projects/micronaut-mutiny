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
package io.micronaut.mutiny.http.client.sse;

import io.micronaut.core.annotation.Internal;
import io.micronaut.core.io.buffer.ByteBuffer;
import io.micronaut.core.type.Argument;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.client.sse.SseClient;
import io.micronaut.http.sse.Event;
import io.micronaut.mutiny.http.client.MutinyHttpClientUtils;
import io.micronaut.mutiny.http.client.MutinySseClient;
import io.smallrye.mutiny.Multi;

/**
 * Mutiny bridge for the server-sent events HTTP client.
 *
 * @author Sergio del Amo
 * @since 1.0.0
 */
@Internal
public class BridgedMutinySseClient implements MutinySseClient {

    private final SseClient sseClient;

    /**
     * Default constructor.
     *
     * @param sseClient Server Sent Events HTTP Client
     */
    public BridgedMutinySseClient(SseClient sseClient) {
        this.sseClient = sseClient;
    }

    @Override
    public <I> Multi<Event<ByteBuffer<?>>> eventStream(HttpRequest<I> request) {
        return MutinyHttpClientUtils.toMulti(sseClient.eventStream(request));
    }

    @Override
    public <I, B> Multi<Event<B>> eventStream(HttpRequest<I> request, Argument<B> eventType) {
        return MutinyHttpClientUtils.toMulti(sseClient.eventStream(request, eventType));
    }

    @Override
    public <I, B> Multi<Event<B>> eventStream(HttpRequest<I> request, Argument<B> eventType, Argument<?> errorType) {
        return MutinyHttpClientUtils.toMulti(sseClient.eventStream(request, eventType, errorType));
    }

    @Override
    public void close() throws Exception {
        if (sseClient instanceof AutoCloseable autoCloseable) {
            autoCloseable.close();
        }
    }
}
