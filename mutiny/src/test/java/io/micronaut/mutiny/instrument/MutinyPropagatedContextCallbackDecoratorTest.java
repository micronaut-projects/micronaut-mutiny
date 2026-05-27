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
import io.micronaut.core.propagation.PropagatedContextElement;
import io.smallrye.mutiny.tuples.Functions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.LongConsumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MutinyPropagatedContextCallbackDecoratorTest {

    private static final String CONTEXT_VALUE = "context-value";

    private final MutinyPropagatedContextCallbackDecorator decorator = new MutinyPropagatedContextCallbackDecorator();

    @Test
    void returnsOriginalCallbacksWithoutContext() {
        Supplier<String> supplier = () -> "value";
        Consumer<String> consumer = value -> {
        };
        LongConsumer longConsumer = value -> {
        };
        BiConsumer<String, String> biConsumer = (left, right) -> {
        };
        Functions.Function3<Integer, Integer, Integer, Integer> function3 = (a, b, c) -> a + b + c;
        Functions.Function4<Integer, Integer, Integer, Integer, Integer> function4 = (a, b, c, d) -> a + b + c + d;
        Functions.Function5<Integer, Integer, Integer, Integer, Integer, Integer> function5 =
            (a, b, c, d, e) -> a + b + c + d + e;
        Functions.Function6<Integer, Integer, Integer, Integer, Integer, Integer, Integer> function6 =
            (a, b, c, d, e, f) -> a + b + c + d + e + f;
        Functions.Function7<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer> function7 =
            (a, b, c, d, e, f, g) -> a + b + c + d + e + f + g;
        Functions.Function8<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer> function8 =
            (a, b, c, d, e, f, g, h) -> a + b + c + d + e + f + g + h;
        Functions.Function9<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer> function9 =
            (a, b, c, d, e, f, g, h, i) -> a + b + c + d + e + f + g + h + i;
        Function<String, String> function = value -> value;
        BiFunction<String, String, String> biFunction = String::concat;
        BinaryOperator<String> binaryOperator = String::concat;
        Functions.TriConsumer<String, String, String> triConsumer = (a, b, c) -> {
        };
        BooleanSupplier booleanSupplier = () -> true;
        Predicate<String> predicate = value -> true;

        assertSame(supplier, decorator.decorate(supplier));
        assertSame(consumer, decorator.decorate(consumer));
        assertSame(longConsumer, decorator.decorate(longConsumer));
        assertSame(biConsumer, decorator.decorate(biConsumer));
        assertSame(function3, decorator.decorate(function3));
        assertSame(function4, decorator.decorate(function4));
        assertSame(function5, decorator.decorate(function5));
        assertSame(function6, decorator.decorate(function6));
        assertSame(function7, decorator.decorate(function7));
        assertSame(function8, decorator.decorate(function8));
        assertSame(function9, decorator.decorate(function9));
        assertSame(function, decorator.decorate(function));
        assertSame(biFunction, decorator.decorate(biFunction));
        assertSame(binaryOperator, decorator.decorate(binaryOperator));
        assertSame(triConsumer, decorator.decorate(triConsumer));
        assertSame(booleanSupplier, decorator.decorate(booleanSupplier));
        assertSame(predicate, decorator.decorate(predicate));
    }

    @Test
    void propagatesContextAcrossCallbackTypes() throws Exception {
        Supplier<String> decoratedSupplier = withContext(() -> decorator.decorate(
            (Supplier<String>) MutinyPropagatedContextCallbackDecoratorTest::currentValue
        ));
        assertEquals(CONTEXT_VALUE, decoratedSupplier.get());

        AtomicReference<String> consumerValue = new AtomicReference<>();
        Consumer<String> decoratedConsumer = withContext(() -> decorator.decorate(
            (Consumer<String>) value -> consumerValue.set(currentValue() + ":" + value)
        ));
        decoratedConsumer.accept("consumer");
        assertEquals("context-value:consumer", consumerValue.get());

        AtomicReference<String> longConsumerValue = new AtomicReference<>();
        LongConsumer decoratedLongConsumer = withContext(() -> decorator.decorate(
            (LongConsumer) value -> longConsumerValue.set(currentValue() + ":" + value)
        ));
        decoratedLongConsumer.accept(7L);
        assertEquals("context-value:7", longConsumerValue.get());

        AtomicReference<String> runnableValue = new AtomicReference<>();
        Runnable decoratedRunnable = withContext(() -> decorator.decorate((Runnable) () -> runnableValue.set(currentValue())));
        decoratedRunnable.run();
        assertEquals(CONTEXT_VALUE, runnableValue.get());

        Callable<String> decoratedCallable = withContext(() -> decorator.decorate(
            (Callable<String>) MutinyPropagatedContextCallbackDecoratorTest::currentValue
        ));
        assertEquals(CONTEXT_VALUE, decoratedCallable.call());

        AtomicReference<String> biConsumerValue = new AtomicReference<>();
        BiConsumer<String, String> decoratedBiConsumer = withContext(() -> decorator.decorate(
            (BiConsumer<String, String>) (left, right) -> biConsumerValue.set(currentValue() + ":" + left + right)
        ));
        decoratedBiConsumer.accept("left", "right");
        assertEquals("context-value:leftright", biConsumerValue.get());

        Functions.Function3<Integer, Integer, Integer, String> decoratedFunction3 = withContext(() -> decorator.decorate(
            (a, b, c) -> currentValue() + ":" + (a + b + c)
        ));
        assertEquals("context-value:6", decoratedFunction3.apply(1, 2, 3));

        Functions.Function4<Integer, Integer, Integer, Integer, String> decoratedFunction4 = withContext(() -> decorator.decorate(
            (a, b, c, d) -> currentValue() + ":" + (a + b + c + d)
        ));
        assertEquals("context-value:10", decoratedFunction4.apply(1, 2, 3, 4));

        Functions.Function5<Integer, Integer, Integer, Integer, Integer, String> decoratedFunction5 = withContext(() -> decorator.decorate(
            (a, b, c, d, e) -> currentValue() + ":" + (a + b + c + d + e)
        ));
        assertEquals("context-value:15", decoratedFunction5.apply(1, 2, 3, 4, 5));

        Functions.Function6<Integer, Integer, Integer, Integer, Integer, Integer, String> decoratedFunction6 = withContext(() -> decorator.decorate(
            (a, b, c, d, e, f) -> currentValue() + ":" + (a + b + c + d + e + f)
        ));
        assertEquals("context-value:21", decoratedFunction6.apply(1, 2, 3, 4, 5, 6));

        Functions.Function7<Integer, Integer, Integer, Integer, Integer, Integer, Integer, String> decoratedFunction7 =
            withContext(() -> decorator.decorate(
                (a, b, c, d, e, f, g) -> currentValue() + ":" + (a + b + c + d + e + f + g)
            ));
        assertEquals("context-value:28", decoratedFunction7.apply(1, 2, 3, 4, 5, 6, 7));

        Functions.Function8<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer, String> decoratedFunction8 =
            withContext(() -> decorator.decorate(
                (a, b, c, d, e, f, g, h) -> currentValue() + ":" + (a + b + c + d + e + f + g + h)
            ));
        assertEquals("context-value:36", decoratedFunction8.apply(1, 2, 3, 4, 5, 6, 7, 8));

        Functions.Function9<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer, String> decoratedFunction9 =
            withContext(() -> decorator.decorate(
                (a, b, c, d, e, f, g, h, i) -> currentValue() + ":" + (a + b + c + d + e + f + g + h + i)
            ));
        assertEquals("context-value:45", decoratedFunction9.apply(1, 2, 3, 4, 5, 6, 7, 8, 9));

        Function<String, String> decoratedFunction = withContext(() -> decorator.decorate(
            (Function<String, String>) value -> currentValue() + ":" + value
        ));
        assertEquals("context-value:function", decoratedFunction.apply("function"));

        BiFunction<String, String, String> decoratedBiFunction = withContext(() -> decorator.decorate(
            (left, right) -> currentValue() + ":" + left + right
        ));
        assertEquals("context-value:leftright", decoratedBiFunction.apply("left", "right"));

        BinaryOperator<String> decoratedBinaryOperator = withContext(() -> decorator.decorate(
            (left, right) -> currentValue() + ":" + left + right
        ));
        assertEquals("context-value:leftright", decoratedBinaryOperator.apply("left", "right"));

        AtomicReference<String> triConsumerValue = new AtomicReference<>();
        Functions.TriConsumer<String, String, String> decoratedTriConsumer = withContext(() -> decorator.decorate(
            (Functions.TriConsumer<String, String, String>) (a, b, c) -> triConsumerValue.set(currentValue() + ":" + a + b + c)
        ));
        decoratedTriConsumer.accept("a", "b", "c");
        assertEquals("context-value:abc", triConsumerValue.get());

        BooleanSupplier decoratedBooleanSupplier = withContext(() -> decorator.decorate(
            (BooleanSupplier) () -> currentValue().equals(CONTEXT_VALUE)
        ));
        assertTrue(decoratedBooleanSupplier.getAsBoolean());

        Predicate<String> decoratedPredicate = withContext(() -> decorator.decorate(
            (Predicate<String>) value -> (currentValue() + ":" + value).equals("context-value:predicate")
        ));
        assertTrue(decoratedPredicate.test("predicate"));
    }

    private static String currentValue() {
        return PropagatedContext.get().get(TestContextElement.class).value();
    }

    private static <T> T withContext(Supplier<T> supplier) {
        return PropagatedContext.empty()
            .plus(new TestContextElement(CONTEXT_VALUE))
            .propagate(supplier);
    }

    private record TestContextElement(String value) implements PropagatedContextElement {
    }
}
