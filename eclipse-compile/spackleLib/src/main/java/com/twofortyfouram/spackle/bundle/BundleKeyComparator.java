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


import android.support.annotation.Nullable;

import net.jcip.annotations.ThreadSafe;

import java.io.Serializable;
import java.util.Comparator;

/**
 * A comparator that can sort the keys of a {@code Bundle}.
 * <p>
 * This class is intended for sorting keys for consistent program ordering,
 * rather than sorting items for display in a user interface.
 */
@ThreadSafe
public final class BundleKeyComparator implements Comparator<String>, Serializable {

    /**
     * Implements the {@link java.io.Serializable} interface.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Performs {@code String} comparison, with handling of {@code null} values.
     *
     * @param object1 first string to compare.
     * @param object2 second string to compare.
     * @return standard {@link String#compareTo(String)} value, except that null
     * is always less than a non-null value and that two null values are
     * equal.
     */
    @Override
    public int compare(@Nullable final String object1, @Nullable final String object2) {
        if (null == object1 && null == object2) {
            return 0;
        }

        if (null == object1) {
            return -1;
        }

        if (null == object2) {
            return 1;
        }

        return object1.compareToIgnoreCase(object2);
    }
}
