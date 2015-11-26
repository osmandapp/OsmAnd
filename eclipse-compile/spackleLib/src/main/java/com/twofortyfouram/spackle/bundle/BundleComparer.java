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

package com.twofortyfouram.spackle.bundle;

import android.os.Bundle;
import android.support.annotation.Nullable;

import com.twofortyfouram.log.Lumberjack;
import com.twofortyfouram.spackle.internal.Reflector;

import net.jcip.annotations.ThreadSafe;

import java.util.Arrays;
import java.util.Set;

/**
 * Utility class to compare two {@code Bundle} instances.
 */
@ThreadSafe
public final class BundleComparer {

    /**
     * Performs an key-value comparison of two {@code Bundle} instances.
     *
     * @param bundle1 {@code Bundle} for comparison.
     * @param bundle2 {@code Bundle} for comparison.
     * @return true if the {@code Bundle} objects are equal. False if the
     * {@code Bundle} objects are unequal.
     */
    public static boolean areBundlesEqual(@Nullable final Bundle bundle1,
            @Nullable final Bundle bundle2) {
        // Special case for null parameters
        if (null == bundle1 || null == bundle2) {
            if (null != bundle1) {
                return false;
            }
            if (null != bundle2) {
                return false;
            }

            return true;
        }

        /*
         * For the Bundles to be equal, the keysets must be equal
         */
        final Set<String> keyset1 = bundle1.keySet();
        final Set<String> keyset2 = bundle2.keySet();

        if (!keyset1.equals(keyset2)) {
            Lumberjack.v("Key sets are unequal: %s != %s", keyset1, keyset2); //$NON-NLS-1$
            return false;
        }

        /*
         * Once the keysets are determined to be equal, then it is possible to
         * iterate through the keys and compare their values
         */
        for (final String key1 : keyset1) {
            final Object value1 = bundle1.get(key1);
            final Object value2 = bundle2.get(key1);

            /*
             * Special case for null values. It is possible for the keysets to
             * match but the values may or may not be null.
             */
            if (null == value1 && null != value2 || null != value1 && null == value2) {
                Lumberjack.v("Values for key %s have null mismatch", key1); //$NON-NLS-1$
                return false;
            }

            if (null == value1 && null == value2) {
                Lumberjack.v("Values for key %s are both null", key1); //$NON-NLS-1$
                continue;
            }

            // special case for comparing Bundles
            if (value1 instanceof Bundle && value2 instanceof Bundle) {
                Lumberjack.v("Values for key %s are Bundles; going recursive!", key1); //$NON-NLS-1$

                // recursive!
                if (!areBundlesEqual((Bundle) value1, (Bundle) value2)) {
                    Lumberjack.v("Key %s recursive Bundles are unequal", key1); //$NON-NLS-1$

                    return false;
                }
                Lumberjack.v("Key %s recursive Bundles are equal", key1); //$NON-NLS-1$
                continue;
            }

            // special case for comparing arrays
            final Class<?> class1 = value1.getClass();
            final Class<?> class2 = value2.getClass();
            if (class1.isArray() && class2.isArray()) {
                Lumberjack.v("Key %s are both arrays", key1); //$NON-NLS-1$

                final Class<?> componentType1 = class1.getComponentType();
                final Class<?> componentType2 = class2.getComponentType();
                if (componentType1.equals(componentType2)) {
                    Lumberjack.v("Key %s are both arrays with the same component type",
                            key1); //$NON-NLS-1$

                    /*
                     * Two cases: primitive and object arrays
                     */
                    if (componentType1.isPrimitive()) {
                        Lumberjack
                                .v("Key %s are arrays of %s[]", key1, componentType1); //$NON-NLS-1$
                        final Class<?> arrayType = class1;

                        if ((Boolean) Reflector
                                .tryInvokeStatic(
                                        Arrays.class,
                                        "equals", new Class<?>[]{arrayType, arrayType},
                                        new Object[]{value1, value2})) //$NON-NLS-1$
                        {
                            Lumberjack
                                    .v("Key %s arrays are equal: %s == %s", key1, Reflector
                                            .tryInvokeStatic(Arrays.class, "toString",
                                                    new Class<?>[]{arrayType},
                                                    new Object[]{value1}), Reflector
                                            .tryInvokeStatic(Arrays.class, "toString",
                                                    new Class<?>[]{arrayType}, new Object[]{
                                                    value1})); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
                            continue;
                        }
                        Lumberjack
                                .v("Key %s arrays are not equal: %s != %s", key1, Reflector
                                        .tryInvokeStatic(Arrays.class, "toString",
                                                new Class<?>[]{arrayType}, new Object[]{value1}),
                                        Reflector.tryInvokeStatic(Arrays.class, "toString",
                                                new Class<?>[]{arrayType}, new Object[]{
                                                value1})); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
                        return false;
                    }

                    Lumberjack.v("Key %s are arrays of %s[]", key1, componentType1); //$NON-NLS-1$
                    final Class<?> arrayType = Object[].class;

                    if ((Boolean) Reflector
                            .tryInvokeStatic(
                                    Arrays.class,
                                    "deepEquals", new Class<?>[]{arrayType, arrayType},
                                    new Object[]{value1, value2})) //$NON-NLS-1$
                    {
                        Lumberjack
                                .v("Key %s arrays are equal: %s == %s", key1, Reflector
                                        .tryInvokeStatic(Arrays.class, "toString",
                                                new Class<?>[]{arrayType}, new Object[]{value1}),
                                        Reflector.tryInvokeStatic(Arrays.class, "toString",
                                                new Class<?>[]{arrayType}, new Object[]{
                                                value1})); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
                        continue;
                    }
                    Lumberjack
                            .v("Key %s arrays are not equal: %s != %s", key1, Reflector
                                    .tryInvokeStatic(Arrays.class, "toString",
                                            new Class<?>[]{arrayType}, new Object[]{value1}),
                                    Reflector.tryInvokeStatic(Arrays.class, "toString",
                                            new Class<?>[]{arrayType}, new Object[]{
                                            value1})); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
                    return false;

                }
                Lumberjack
                        .v("Key %s arrays aren't of the same type: %s[] != %s[]", key1,
                                componentType1, componentType2); //$NON-NLS-1$

                return false;
            }

            /*
             * All other object types are compared using normal equals
             * comparison
             */
            if (value1.equals(value2) && value2.equals(value1)) {
                Lumberjack.v("Values for key %s are equal: %s == %s", key1, value1,
                        value2); //$NON-NLS-1$
                continue;
            }
            Lumberjack.v("Values for key %s are unequal: %s != %s", key1, value1,
                    value2); //$NON-NLS-1$

            return false;
        }

        Lumberjack.v("Bundles are equal!"); //$NON-NLS-1$
        return true;
    }

    /**
     * Private constructor prevents instantiation.
     *
     * @throws UnsupportedOperationException because this class cannot be
     *                                       instantiated.
     */
    private BundleComparer() {
        throw new UnsupportedOperationException("This class is non-instantiable"); //$NON-NLS-1$
    }
}
