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
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import mutiny.zero.flow.adapters.AdaptersToFlow;
import org.reactivestreams.Publisher;

/**
 * Shared Mutiny HTTP client conversion helpers.
 *
 * @author Sergio del Amo
 * @since 1.0.0
 */
@Internal
public final class MutinyHttpClientUtils {

    private MutinyHttpClientUtils() {
    }

    /**
     * Convert a Reactive Streams publisher to a Mutiny {@link Uni}.
     *
     * @param publisher The publisher
     * @param <T> The emitted type
     * @return The Uni
     */
    public static <T> Uni<T> toUni(Publisher<T> publisher) {
        return Uni.createFrom().publisher(AdaptersToFlow.publisher(publisher));
    }

    /**
     * Convert a Reactive Streams publisher to a Mutiny {@link Multi}.
     *
     * @param publisher The publisher
     * @param <T> The emitted type
     * @return The Multi
     */
    public static <T> Multi<T> toMulti(Publisher<T> publisher) {
        return Multi.createFrom().publisher(AdaptersToFlow.publisher(publisher));
    }
}
