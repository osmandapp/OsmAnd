/*
 * android-spackle-lib https://github.com/twofortyfouram/android-spackle
 * Copyright 2014 two forty four a.m. LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.twofortyfouram.spackle.internal;


import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import net.jcip.annotations.ThreadSafe;

import java.lang.reflect.InvocationTargetException;

/**
 * Utilities for performing reflection against newer Android SDKs.
 * <p>
 * This is not a general-purpose reflection class but is rather specifically
 * designed for calling methods that must exist in newer versions of Android.
 */
@ThreadSafe
public final class Reflector {

    /**
     * Use reflection to invoke a static method for a class object and method
     * name.
     *
     * @param <T>         Type that the method should return
     * @param classObject Class on which to invoke {@code methodName}.
     * @param methodName  Name of the method to invoke.
     * @param types       explicit types for the objects. This is useful if the types
     *                    are primitives, rather than objects.
     * @param args        arguments for the method.
     * @return The result of invoking the named method on the given class for
     * the args.
     * @throws RuntimeException if the class or method doesn't exist.
     */
    public static <T> T tryInvokeStatic(@NonNull final Class<?> classObject,
            @NonNull final String methodName, @Nullable final Class<?>[] types,
            @Nullable final Object[] args) {
        return helper(null, classObject, null, methodName, types, args);
    }

    /**
     * Use reflection to invoke a static method for a class object and method
     * name.
     *
     * @param <T>        Type that the method should return.
     * @param className  Name of the class on which to invoke {@code methodName}.
     * @param methodName Name of the method to invoke.
     * @param types      explicit types for the objects. This is useful if the types
     *                   are primitives, rather than objects.
     * @param args       arguments for the method.
     * @return The result of invoking the named method on the given class for
     * the args
     * @throws RuntimeException if the class or method doesn't exist.
     */
    public static <T> T tryInvokeStatic(@NonNull final String className,
            @NonNull final String methodName, @Nullable final Class<?>[] types,
            @Nullable final Object[] args) {
        return helper(null, null, className, methodName, types, args);
    }

    /**
     * Use reflection to invoke an instance method for a class object and method
     * name.
     *
     * @param <T>        Type that the method should return.
     * @param target     Object instance on which to invoke {@code methodName}.
     * @param methodName Name of the method to invoke.
     * @param types      explicit types for the objects. This is useful if the types
     *                   are primitives, rather than objects.
     * @param args       arguments for the method.
     * @return The result of invoking the named method on the given class for
     * the args
     * @throws RuntimeException if the class or method doesn't exist.
     */
    public static <T> T tryInvokeInstance(@NonNull final Object target,
            @NonNull final String methodName, @Nullable final Class<?>[] types,
            @Nullable final Object[] args) {
        return helper(target, null, null, methodName, types, args);
    }

    @SuppressWarnings("unchecked")
    private static <T> T helper(final Object target, final Class<?> classObject,
            final String className, final String methodName, final Class<?>[] argTypes,
            final Object... args) {
        try {
            final Class<?> cls;
            if (null != classObject) {
                cls = classObject;
            } else if (null != target) {
                cls = target.getClass();
            } else {
                cls = Class.forName(className);
            }

            return (T) cls.getMethod(methodName, argTypes).invoke(target, args);
        } catch (final NoSuchMethodException e) {
            throw new RuntimeException(e);
        } catch (final IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (final InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (final ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Private constructor prevents instantiation.
     *
     * @throws UnsupportedOperationException because this class cannot be
     *                                       instantiated.
     */
    private Reflector() {
        throw new UnsupportedOperationException("This class is non-instantiable"); //$NON-NLS-1$
    }
}
