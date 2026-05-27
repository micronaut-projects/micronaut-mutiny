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
package io.micronaut.mutiny;

import io.micronaut.core.annotation.Internal;
import io.micronaut.core.async.publisher.Publishers;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

/**
 * Registers Mutiny types with Micronaut's reactive type registry.
 *
 * @author Sergio del Amo
 * @since 1.0.0
 */
@Internal
public final class MutinyReactiveTypesRegistrar {

    private static volatile boolean registered;

    private MutinyReactiveTypesRegistrar() {
    }

    /**
     * Register Mutiny reactive types if they are not already registered.
     */
    public static void register() {
        if (!registered) {
            synchronized (MutinyReactiveTypesRegistrar.class) {
                if (!registered) {
                    if (!Publishers.getKnownSingleTypes().contains(Uni.class)) {
                        Publishers.registerReactiveSingle(Uni.class);
                    }
                    if (!Publishers.getKnownReactiveTypes().contains(Multi.class)) {
                        Publishers.registerReactiveType(Multi.class);
                    }
                    registered = true;
                }
            }
        }
    }
}
