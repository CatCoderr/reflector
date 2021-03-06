/*
 *  Copyright 2018 Petr P.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package ru.progrm_jarvis.reflector.wrapper;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.*;
import org.jetbrains.annotations.Nullable;
import ru.progrm_jarvis.reflector.AccessHelper;

import java.lang.reflect.Field;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

/**
 * Wrapper for {@link Field} to be used with Reflector
 *
 * @param <T> type of class containing this field
 * @param <V> type of value contained in this field
 */
@Value
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class FieldWrapper<T, V> implements ReflectorWrapper {

    /**
     * Cache of field wrappers
     */
    private static final Cache<Field, FieldWrapper<?, ?>> CACHE = CacheBuilder.newBuilder().weakValues().build();

    /**
     * Actual field wrapped.
     */
    @NonNull private Field field;

    /**
     * Creates new field wrapper instance for the field given or gets it from cache if one already exists.
     *
     * @param field field to get wrapped
     * @param <T> type containing this field
     * @param <V> type of this field's value
     * @return field wrapper created or got from cache
     */
    @SuppressWarnings("unchecked")
    public static <T, V> FieldWrapper<T, V> of(@NonNull final Field field) {
        try {
            return ((FieldWrapper<T, V>) CACHE.get(field, () -> new FieldWrapper<>(field)));
        } catch (final ExecutionException e) {
            throw new RuntimeException("Could not obtain FieldWrapper<V> value from cache");
        }
    }

    /**
     * Gets value of this field ignoring any limitations if possible.
     *
     * @param instance instance of which field's value is get
     * @return value of this field (static)
     * @throws NullPointerException if {@code object} is {@code null} but this field is not static
     */
    @SneakyThrows
    @SuppressWarnings("unchecked")
    public V getValue(@Nullable final T instance) {
        return AccessHelper.accessAndGet(field, field -> (V) field.get(instance));
    }

    /**
     * Gets value of this field on no instance (which means that static value is to be got)
     * ignoring any limitations if possible.
     *
     * @return value of this {@code static} field
     * @throws NullPointerException if this field is not {@code static}
     */
    public V getValue() {
        return getValue(null);
    }

    /**
     * Sets value of this field ignoring any limitations if possible.
     *
     * @param instance instance of which field's value is set
     * @param value value to set to this field
     * @throws NullPointerException if {@code object} is {@code null} but this field is not static
     */
    @SneakyThrows
    public void setValue(@Nullable final T instance, @Nullable final V value) {
        AccessHelper.operate(field, field -> field.set(instance, value));
    }

    /**
     * Sets value of this field on no instance (which means that static value is to be set)
     * ignoring any limitations if possible.
     *
     * @param value value to set to this field
     * @throws NullPointerException if this field is not {@code static}
     */
    public void setValue(@Nullable final V value) {
        setValue(null, value);
    }

    /**
     * Updates value of this field ignoring any limitations if possible.
     *
     * @param instance instance of which field's value is set
     * @param value value to set to this field
     * @return previous value of this field
     * @throws NullPointerException if {@code object} is {@code null} but this field is not static
     */
    @SneakyThrows
    @SuppressWarnings("unchecked")
    public V updateValue(@Nullable final T instance, @Nullable final V value) {
        return AccessHelper.operateAndGet(field, field -> {
            val oldValue = (V) field.get(instance);

            field.set(instance, value);

            return oldValue;
        });
    }

    /**
     * Updates value of this field on no instance (which means that static value is to be updated)
     * ignoring any limitations if possible.
     *
     * @param value value to set to this field
     * @return previous value of this field
     * @throws NullPointerException if {@code object} is {@code null} but this field is not static
     */
    public V updateValue(@Nullable final V value) {
        return updateValue(null, value);
    }

    /**
     * Updates value of this field based on previous value using function given ignoring any limitations if possible.
     *
     * @param instance instance of which field's value is set
     * @param function function to create new value based on old
     * @return previous value of this field
     * @throws NullPointerException if {@code object} is {@code null} but this field is not static
     */
    @SneakyThrows
    @SuppressWarnings("unchecked")
    public V updateValue(@Nullable final T instance, @NonNull final Function<V, V> function) {
        return AccessHelper.operateAndGet(field, field -> {
            val oldValue = (V) field.get(instance);

            field.set(instance, function.apply(oldValue));

            return oldValue;
        });
    }

    /**
     * Updates value of this field based on previous value using function given
     * on no instance (which means that static value is to be updated) ignoring any limitations if possible.
     *
     * @param function function to create new value based on old
     * @return previous value of this field
     * @throws NullPointerException if {@code object} is {@code null} but this field is not static
     */
    public V updateValue(@NonNull final Function<V, V> function) {
        return updateValue(null, function);
    }
}
