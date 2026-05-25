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
package io.micronaut.mutiny.instrument;

import io.micronaut.core.propagation.PropagatedContext;
import io.smallrye.mutiny.infrastructure.CallbackDecorator;
import io.smallrye.mutiny.tuples.Functions;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.Callable;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.LongConsumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Propagates Micronaut context through Mutiny callbacks.
 *
 * @author Sergio del Amo
 * @since 1.0.0
 */
public final class MutinyPropagatedContextCallbackDecorator implements CallbackDecorator {

    /**
     * Creates a Mutiny callback decorator.
     */
    public MutinyPropagatedContextCallbackDecorator() {
    }

    @Override
    public <T> Supplier<T> decorate(Supplier<T> supplier) {
        PropagatedContext context = currentContext();
        if (context == null) {
            return supplier;
        }
        return () -> context.propagate(supplier);
    }

    @Override
    public <T> Consumer<T> decorate(Consumer<T> consumer) {
        PropagatedContext context = currentContext();
        if (context == null) {
            return consumer;
        }
        return value -> context.propagate(() -> consumer.accept(value));
    }

    @Override
    public LongConsumer decorate(LongConsumer consumer) {
        PropagatedContext context = currentContext();
        if (context == null) {
            return consumer;
        }
        return value -> context.propagate(() -> consumer.accept(value));
    }

    @Override
    public Runnable decorate(Runnable runnable) {
        return PropagatedContext.wrapCurrent(runnable);
    }

    @Override
    public <V> Callable<V> decorate(Callable<V> callable) {
        return PropagatedContext.wrapCurrent(callable);
    }

    @Override
    public <T1, T2> BiConsumer<T1, T2> decorate(BiConsumer<T1, T2> consumer) {
        PropagatedContext context = currentContext();
        if (context == null) {
            return consumer;
        }
        return (value1, value2) -> context.propagate(() -> consumer.accept(value1, value2));
    }

    @Override
    public <T1, T2, T3, O> Functions.Function3<T1, T2, T3, O> decorate(Functions.Function3<T1, T2, T3, O> function) {
        PropagatedContext context = currentContext();
        if (context == null) {
            return function;
        }
        return (value1, value2, value3) -> context.propagate(() -> function.apply(value1, value2, value3));
    }

    @Override
    public <T1, T2, T3, T4, O> Functions.Function4<T1, T2, T3, T4, O> decorate(
            Functions.Function4<T1, T2, T3, T4, O> function) {
        PropagatedContext context = currentContext();
        if (context == null) {
            return function;
        }
        return (value1, value2, value3, value4) -> context.propagate(
                () -> function.apply(value1, value2, value3, value4));
    }

    @Override
    public <T1, T2, T3, T4, T5, O> Functions.Function5<T1, T2, T3, T4, T5, O> decorate(
            Functions.Function5<T1, T2, T3, T4, T5, O> function) {
        PropagatedContext context = currentContext();
        if (context == null) {
            return function;
        }
        return (value1, value2, value3, value4, value5) -> context.propagate(
                () -> function.apply(value1, value2, value3, value4, value5));
    }

    @Override
    public <T1, T2, T3, T4, T5, T6, O> Functions.Function6<T1, T2, T3, T4, T5, T6, O> decorate(
            Functions.Function6<T1, T2, T3, T4, T5, T6, O> function) {
        PropagatedContext context = currentContext();
        if (context == null) {
            return function;
        }
        return (value1, value2, value3, value4, value5, value6) -> context.propagate(
                () -> function.apply(value1, value2, value3, value4, value5, value6));
    }

    @Override
    public <T1, T2, T3, T4, T5, T6, T7, O> Functions.Function7<T1, T2, T3, T4, T5, T6, T7, O> decorate(
            Functions.Function7<T1, T2, T3, T4, T5, T6, T7, O> function) {
        PropagatedContext context = currentContext();
        if (context == null) {
            return function;
        }
        return (value1, value2, value3, value4, value5, value6, value7) -> context.propagate(
                () -> function.apply(value1, value2, value3, value4, value5, value6, value7));
    }

    @Override
    public <T1, T2, T3, T4, T5, T6, T7, T8, O> Functions.Function8<T1, T2, T3, T4, T5, T6, T7, T8, O> decorate(
            Functions.Function8<T1, T2, T3, T4, T5, T6, T7, T8, O> function) {
        PropagatedContext context = currentContext();
        if (context == null) {
            return function;
        }
        return (value1, value2, value3, value4, value5, value6, value7, value8) -> context.propagate(
                () -> function.apply(value1, value2, value3, value4, value5, value6, value7, value8));
    }

    @Override
    public <T1, T2, T3, T4, T5, T6, T7, T8, T9, O> Functions.Function9<T1, T2, T3, T4, T5, T6, T7, T8, T9, O> decorate(
            Functions.Function9<T1, T2, T3, T4, T5, T6, T7, T8, T9, O> function) {
        PropagatedContext context = currentContext();
        if (context == null) {
            return function;
        }
        return (value1, value2, value3, value4, value5, value6, value7, value8, value9) -> context.propagate(
                () -> function.apply(value1, value2, value3, value4, value5, value6, value7, value8, value9));
    }

    @Override
    public <I, O> Function<I, O> decorate(Function<I, O> function) {
        PropagatedContext context = currentContext();
        if (context == null) {
            return function;
        }
        return value -> context.propagate(() -> function.apply(value));
    }

    @Override
    public <I1, I2, O> BiFunction<I1, I2, O> decorate(BiFunction<I1, I2, O> function) {
        PropagatedContext context = currentContext();
        if (context == null) {
            return function;
        }
        return (value1, value2) -> context.propagate(() -> function.apply(value1, value2));
    }

    @Override
    public <T> BinaryOperator<T> decorate(BinaryOperator<T> operator) {
        PropagatedContext context = currentContext();
        if (context == null) {
            return operator;
        }
        return (value1, value2) -> context.propagate(() -> operator.apply(value1, value2));
    }

    @Override
    public <T1, T2, T3> Functions.TriConsumer<T1, T2, T3> decorate(Functions.TriConsumer<T1, T2, T3> consumer) {
        PropagatedContext context = currentContext();
        if (context == null) {
            return consumer;
        }
        return (value1, value2, value3) -> context.propagate(() -> consumer.accept(value1, value2, value3));
    }

    @Override
    public BooleanSupplier decorate(BooleanSupplier supplier) {
        PropagatedContext context = currentContext();
        if (context == null) {
            return supplier;
        }
        return () -> context.propagate(supplier::getAsBoolean);
    }

    @Override
    public <T> Predicate<T> decorate(Predicate<T> predicate) {
        PropagatedContext context = currentContext();
        if (context == null) {
            return predicate;
        }
        return value -> context.propagate(() -> predicate.test(value));
    }

    private static @Nullable PropagatedContext currentContext() {
        return PropagatedContext.find().orElse(null);
    }
}
