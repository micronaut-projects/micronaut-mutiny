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
package io.micronaut.mutiny.converters;

import io.micronaut.core.annotation.Internal;
import io.micronaut.core.annotation.TypeHint;
import io.micronaut.core.convert.MutableConversionService;
import io.micronaut.core.convert.TypeConverterRegistrar;
import io.micronaut.mutiny.MutinyReactiveTypesRegistrar;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import mutiny.zero.flow.adapters.AdaptersToFlow;
import mutiny.zero.flow.adapters.AdaptersToReactiveStreams;
import org.reactivestreams.Publisher;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;

/**
 * Converters for Mutiny reactive types.
 *
 * @author Sergio del Amo
 * @since 1.0.0
 */
@Internal
@TypeHint({Uni.class, Multi.class})
@SuppressWarnings({"rawtypes", "unchecked"})
public final class MutinyConverterRegistrar implements TypeConverterRegistrar {

    @Override
    public void register(MutableConversionService conversionService) {
        MutinyReactiveTypesRegistrar.register();

        conversionService.addConverter(Publisher.class, Flow.Publisher.class, MutinyConverterRegistrar::toFlowPublisher);
        conversionService.addConverter(Flow.Publisher.class, Publisher.class, MutinyConverterRegistrar::toReactiveStreamsPublisher);

        conversionService.addConverter(Publisher.class, Uni.class, MutinyConverterRegistrar::publisherToUni);
        conversionService.addConverter(Publisher.class, Multi.class, MutinyConverterRegistrar::publisherToMulti);
        conversionService.addConverter(Flow.Publisher.class, Uni.class, MutinyConverterRegistrar::flowPublisherToUni);
        conversionService.addConverter(Flow.Publisher.class, Multi.class, MutinyConverterRegistrar::flowPublisherToMulti);

        conversionService.addConverter(Uni.class, Publisher.class, uni -> toReactiveStreamsPublisher(uni.convert().toPublisher()));
        conversionService.addConverter(Multi.class, Publisher.class, multi -> toReactiveStreamsPublisher(multi));
        conversionService.addConverter(Uni.class, Flow.Publisher.class, uni -> uni.convert().toPublisher());
        conversionService.addConverter(Multi.class, Flow.Publisher.class, multi -> multi);

        conversionService.addConverter(Uni.class, Multi.class, Uni::toMulti);
        conversionService.addConverter(Multi.class, Uni.class, Multi::toUni);

        conversionService.addConverter(CompletionStage.class, Uni.class, stage -> Uni.createFrom().completionStage(stage));
        conversionService.addConverter(Uni.class, CompletionStage.class, Uni::subscribeAsCompletionStage);
        registerIterableToMultiConverter(conversionService);

        conversionService.addConverter(Object.class, Uni.class, MutinyConverterRegistrar::objectToUni);
        conversionService.addConverter(Object.class, Multi.class, MutinyConverterRegistrar::objectToMulti);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void registerIterableToMultiConverter(MutableConversionService conversionService) {
        conversionService.addConverter(Iterable.class, Multi.class, MutinyConverterRegistrar::iterableToMulti);
    }

    private static Uni<?> objectToUni(Object object) {
        if (object instanceof Uni<?> uni) {
            return uni;
        }
        if (object instanceof Multi<?> multi) {
            return multi.toUni();
        }
        if (object instanceof CompletionStage<?> stage) {
            return Uni.createFrom().completionStage(stage);
        }
        if (object instanceof Publisher<?> publisher) {
            return publisherToUni(publisher);
        }
        if (object instanceof Flow.Publisher<?> publisher) {
            return flowPublisherToUni(publisher);
        }
        return Uni.createFrom().item(object);
    }

    private static Multi<?> objectToMulti(Object object) {
        if (object instanceof Multi<?> multi) {
            return multi;
        }
        if (object instanceof Uni<?> uni) {
            return uni.toMulti();
        }
        if (object instanceof Publisher<?> publisher) {
            return publisherToMulti(publisher);
        }
        if (object instanceof Flow.Publisher<?> publisher) {
            return flowPublisherToMulti(publisher);
        }
        if (object instanceof Iterable<?> iterable) {
            return Multi.createFrom().iterable(iterable);
        }
        return Multi.createFrom().item(object);
    }

    private static Multi<?> iterableToMulti(Iterable<?> iterable) {
        return Multi.createFrom().iterable(iterable);
    }

    private static Uni<?> publisherToUni(Publisher<?> publisher) {
        return flowPublisherToUni(toFlowPublisher(publisher));
    }

    private static Multi<?> publisherToMulti(Publisher<?> publisher) {
        return flowPublisherToMulti(toFlowPublisher(publisher));
    }

    private static Uni<?> flowPublisherToUni(Flow.Publisher<?> publisher) {
        return Uni.createFrom().publisher(publisher);
    }

    private static Multi<?> flowPublisherToMulti(Flow.Publisher<?> publisher) {
        return Multi.createFrom().publisher(publisher);
    }

    private static Flow.Publisher<?> toFlowPublisher(Publisher<?> publisher) {
        return AdaptersToFlow.publisher(publisher);
    }

    private static Publisher<?> toReactiveStreamsPublisher(Flow.Publisher<?> publisher) {
        return AdaptersToReactiveStreams.publisher(publisher);
    }
}
