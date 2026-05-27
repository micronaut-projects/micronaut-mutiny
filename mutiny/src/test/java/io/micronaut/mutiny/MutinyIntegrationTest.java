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

import io.micronaut.core.async.publisher.Publishers;
import io.micronaut.core.convert.ConversionService;
import io.micronaut.core.convert.DefaultMutableConversionService;
import io.micronaut.core.propagation.PropagatedContext;
import io.micronaut.core.propagation.PropagatedContextElement;
import io.micronaut.core.type.Argument;
import io.micronaut.core.type.ReturnType;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Flow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MutinyIntegrationTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    @Test
    void mutinyTypesAreRegisteredAsReactiveTypes() {
        MutinyReactiveTypesRegistrar.register();

        ReturnType<?> uniReturnType = ReturnType.of(Uni.class, Argument.STRING);
        ReturnType<?> multiReturnType = ReturnType.of(Multi.class, Argument.STRING);

        assertTrue(uniReturnType.isReactive());
        assertTrue(uniReturnType.isSingleResult());
        assertTrue(multiReturnType.isReactive());
        assertFalse(multiReturnType.isSingleResult());
        assertTrue(Publishers.isConvertibleToPublisher(Uni.class));
        assertTrue(Publishers.isConvertibleToPublisher(Multi.class));
    }

    @Test
    void conversionServiceConvertsMutinyTypes() {
        ConversionService conversionService = new DefaultMutableConversionService();

        Uni<?> uni = conversionService.convert(Publishers.just("one"), Uni.class).orElseThrow();
        Multi<?> multi = conversionService.convert(List.of("one", "two"), Multi.class).orElseThrow();
        Publisher<?> publisher = conversionService.convert(Uni.createFrom().item("one"), Publisher.class).orElseThrow();
        Publisher<?> multiPublisher = conversionService.convert(Multi.createFrom().item("one"), Publisher.class).orElseThrow();
        Publisher<?> iterableMultiPublisher = conversionService.convert(Multi.createFrom().iterable(List.of("one")), Publisher.class).orElseThrow();
        Flow.Publisher<?> flowPublisher = conversionService.convert(Multi.createFrom().item("one"), Flow.Publisher.class).orElseThrow();

        assertEquals("one", uni.await().atMost(TIMEOUT));
        assertEquals(List.of("one", "two"), multi.collect().asList().await().atMost(TIMEOUT));
        assertInstanceOf(Publisher.class, publisher);
        assertInstanceOf(Publisher.class, multiPublisher);
        assertInstanceOf(Publisher.class, iterableMultiPublisher);
        assertInstanceOf(Flow.Publisher.class, flowPublisher);
    }

    @Test
    void propagatedContextIsAvailableInMutinyCallbacks() {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        try {
            PropagatedContext context = PropagatedContext.empty().plus(new TestContextElement("context-value"));
            Uni<String> uni = context.propagate(() -> Uni.createFrom()
                .item("item")
                .emitOn(executorService)
                .onItem()
                .transform(item -> PropagatedContext.get().get(TestContextElement.class).value() + ":" + item));

            assertEquals("context-value:item", uni.await().atMost(TIMEOUT));
        } finally {
            executorService.shutdownNow();
        }
    }

    private record TestContextElement(String value) implements PropagatedContextElement {
    }
}
