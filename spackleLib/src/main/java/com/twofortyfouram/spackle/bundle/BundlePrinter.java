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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.twofortyfouram.spackle.internal.Reflector;

import net.jcip.annotations.ThreadSafe;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

/**
 * Utility class to print a {@code Bundle} to a String.
 */
@ThreadSafe
public final class BundlePrinter {

    /**
     * Convert a Bundle into a string representation.  This string representation is only intended
     * for debugging purposes and may change in future versions.
     * <p>
     * Note: This method's behavior is undefined for cyclic Bundles.
     *
     * @param bundle {@link Bundle} to convert to a string.
     * @return Stringified representation of {@code bundle}.  If the bundle is null, returns the
     * string "null."  If the bundle needed to be scrubbed as per {@link
     * com.twofortyfouram.spackle.bundle.BundleScrubber}, then returns the string "bad."  If
     * the bundle is empty, then returns the string "empty."
     */
    @NonNull
    public static String toString(@Nullable final Bundle bundle) {
        if (BundleScrubber.scrub(bundle)) {
            return "bad"; //$NON-NLS-1$
        }

        if (null == bundle) {
            return "null"; //$NON-NLS-1$
        }

        if (bundle.keySet().isEmpty()) {
            return "empty"; //$NON-NLS-1$
        }

        /*
         * Converting to a TreeSet forces the keys to be sorted, which provides
         * consistent ordering of the output results. This is NOT a public
         * interface commitment that the result will be sorted by the keys,
         * although this sorting does make it easier to perform regression
         * tests.
         */
        final Set<String> keys = new TreeSet<String>(new BundleKeyComparator());
        keys.addAll(bundle.keySet());

        final StringBuilder result = new StringBuilder();
        for (final String key : keys) {
            if (0 < result.length()) {
                result.append(", "); //$NON-NLS-1$
            }

            final String keyString = null == key ? "null" : key; //$NON-NLS-1$
            String valueString = null;

            final Object value = bundle.get(key);
            if (null != value) {
                if (value instanceof Bundle) {
                    valueString = toString((Bundle) value);
                } else {
                    final Class<?> cls = value.getClass();
                    if (cls.isArray()) {
                        /*
                         * Two cases: primitive and object arrays.
                         */
                        if (cls.getComponentType().isPrimitive()) {
                            valueString = Reflector.tryInvokeStatic(Arrays.class,
                                    "toString", new Class<?>[]{cls},
                                    new Object[]{value}); //$NON-NLS-1$
                        } else {
                            valueString = Reflector
                                    .tryInvokeStatic(
                                            Arrays.class,
                                            "deepToString", new Class<?>[]{Object[].class},
                                            new Object[]{value}); //$NON-NLS-1$
                        }
                    } else {
                        valueString = value.toString();
                    }
                }

            }

            result.append(
                    String.format(Locale.US, "{%s = %s}", keyString, valueString)); //$NON-NLS-1$
        }

        return result.toString();
    }

    /**
     * Private constructor prevents instantiation.
     *
     * @throws UnsupportedOperationException because this class cannot be
     *                                       instantiated.
     */
    private BundlePrinter() {
        throw new UnsupportedOperationException("This class is non-instantiable"); //$NON-NLS-1$
    }
}
