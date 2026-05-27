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

import io.micronaut.context.annotation.Requires;
import io.micronaut.core.annotation.Indexed;
import io.micronaut.core.annotation.Internal;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.ReactiveClientResultTransformer;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Singleton;

/**
 * Adds custom support for Mutiny types to handle NOT_FOUND results.
 *
 * @author Sergio del Amo
 * @since 1.0.0
 */
@Singleton
@Requires(classes = {Uni.class, Multi.class, ReactiveClientResultTransformer.class})
@Internal
@Indexed(ReactiveClientResultTransformer.class)
class MutinyReactiveClientResultTransformer implements ReactiveClientResultTransformer {

    @Override
    public Object transform(Object publisherResult) {
        if (publisherResult instanceof Uni<?> uni) {
            return transformUni(uni);
        }
        if (publisherResult instanceof Multi<?> multi) {
            return transformMulti(multi);
        }
        return publisherResult;
    }

    private static <T> Uni<T> transformUni(Uni<T> uni) {
        return uni.onFailure(HttpClientResponseException.class).recoverWithUni((HttpClientResponseException responseException) -> {
            if (responseException.getStatus() == HttpStatus.NOT_FOUND) {
                return Uni.createFrom().nullItem();
            }
            return Uni.createFrom().failure(responseException);
        });
    }

    private static <T> Multi<T> transformMulti(Multi<T> multi) {
        return multi.onFailure(throwable -> throwable instanceof HttpClientResponseException).recoverWithMulti(throwable -> {
            HttpClientResponseException responseException = (HttpClientResponseException) throwable;
            if (responseException.getStatus() == HttpStatus.NOT_FOUND) {
                return Multi.createFrom().empty();
            }
            return Multi.createFrom().failure(responseException);
        });
    }
}
