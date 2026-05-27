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
import io.micronaut.core.type.Argument;
import io.micronaut.mutiny.converters.MutinyConverterRegistrar;
import io.micronaut.mutiny.type.MutinyTypeInformationProvider;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import mutiny.zero.flow.adapters.AdaptersToFlow;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import java.util.concurrent.Flow.Subscription;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MutinyConverterRegistrarTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    private final ConversionService conversionService = buildConversionService();

    @Test
    void convertsObjectSourceTypeToUni() {
        Uni<String> uni = Uni.createFrom().item("uni");
        Multi<String> multi = Multi.createFrom().item("multi");
        CompletableFuture<String> stage = CompletableFuture.completedFuture("stage");
        Flow.Publisher<String> flowPublisher = flowPublisherOf("flow");

        Uni<?> sameUni = convertObject(uni, Uni.class);
        Uni<?> multiAsUni = convertObject(multi, Uni.class);
        Uni<?> stageAsUni = convertObject(stage, Uni.class);
        Uni<?> publisherAsUni = convertObject(Publishers.just("publisher"), Uni.class);
        Uni<?> flowPublisherAsUni = convertObject(flowPublisher, Uni.class);
        Uni<?> valueAsUni = convertObject("value", Uni.class);

        assertSame(uni, sameUni);
        assertEquals("multi", multiAsUni.await().atMost(TIMEOUT));
        assertEquals("stage", stageAsUni.await().atMost(TIMEOUT));
        assertEquals("publisher", publisherAsUni.await().atMost(TIMEOUT));
        assertEquals("flow", flowPublisherAsUni.await().atMost(TIMEOUT));
        assertEquals("value", valueAsUni.await().atMost(TIMEOUT));
    }

    @Test
    void convertsObjectSourceTypeToMulti() {
        Multi<String> multi = Multi.createFrom().items("multi-1", "multi-2");
        Uni<String> uni = Uni.createFrom().item("uni");
        Flow.Publisher<String> flowPublisher = flowPublisherOf("flow");

        Multi<?> sameMulti = convertObject(multi, Multi.class);
        Multi<?> uniAsMulti = convertObject(uni, Multi.class);
        Multi<?> publisherAsMulti = convertObject(Publishers.just("publisher"), Multi.class);
        Multi<?> flowPublisherAsMulti = convertObject(flowPublisher, Multi.class);
        Multi<?> iterableAsMulti = convertObject(List.of("one", "two"), Multi.class);
        Multi<?> valueAsMulti = convertObject("value", Multi.class);

        assertSame(multi, sameMulti);
        assertEquals(List.of("multi-1", "multi-2"), sameMulti.collect().asList().await().atMost(TIMEOUT));
        assertEquals(List.of("uni"), uniAsMulti.collect().asList().await().atMost(TIMEOUT));
        assertEquals(List.of("publisher"), publisherAsMulti.collect().asList().await().atMost(TIMEOUT));
        assertEquals(List.of("flow"), flowPublisherAsMulti.collect().asList().await().atMost(TIMEOUT));
        assertEquals(List.of("one", "two"), iterableAsMulti.collect().asList().await().atMost(TIMEOUT));
        assertEquals(List.of("value"), valueAsMulti.collect().asList().await().atMost(TIMEOUT));
    }

    @Test
    void convertsThroughDirectMutinyConverters() {
        Uni<?> stageAsUni = conversionService.convert(CompletableFuture.completedFuture("stage"), Uni.class).orElseThrow();
        Flow.Publisher<?> uniAsFlowPublisher = conversionService.convert(Uni.createFrom().item("uni-flow"), Flow.Publisher.class).orElseThrow();
        Flow.Publisher<?> multiAsFlowPublisher = conversionService.convert(Multi.createFrom().items("one", "two"), Flow.Publisher.class).orElseThrow();
        Publisher<?> multiAsPublisher = conversionService.convert(Multi.createFrom().item("publisher"), Publisher.class).orElseThrow();

        assertEquals("stage", stageAsUni.await().atMost(TIMEOUT));
        assertEquals("uni-flow", Uni.createFrom().publisher(uniAsFlowPublisher).await().atMost(TIMEOUT));
        assertEquals(List.of("one", "two"), Multi.createFrom().publisher(multiAsFlowPublisher).collect().asList().await().atMost(TIMEOUT));
        assertEquals(List.of("publisher"), Multi.createFrom().publisher(AdaptersToFlow.publisher(multiAsPublisher)).collect().asList().await().atMost(TIMEOUT));
    }

    @Test
    void typeInformationProviderRecognizesMutinyTypes() {
        MutinyTypeInformationProvider provider = new MutinyTypeInformationProvider();

        assertTrue(provider.isSingle(Uni.class));
        assertFalse(provider.isSingle(Multi.class));
        assertTrue(provider.isReactive(Uni.class));
        assertTrue(provider.isReactive(Multi.class));
        assertFalse(provider.isReactive(String.class));
    }

    @SuppressWarnings("unchecked")
    private <T> T convertObject(Object value, Class<T> targetType) {
        return (T) conversionService.convert(value, Object.class, Argument.of(targetType)).orElseThrow();
    }

    private static Flow.Publisher<String> flowPublisherOf(String value) {
        return subscriber -> subscriber.onSubscribe(new Subscription() {
            private boolean done;

            @Override
            public void request(long n) {
                if (!done && n > 0) {
                    done = true;
                    subscriber.onNext(value);
                    subscriber.onComplete();
                }
            }

            @Override
            public void cancel() {
                done = true;
            }
        });
    }

    private static ConversionService buildConversionService() {
        DefaultMutableConversionService conversionService = new DefaultMutableConversionService();
        new MutinyConverterRegistrar().register(conversionService);
        return conversionService;
    }
}
