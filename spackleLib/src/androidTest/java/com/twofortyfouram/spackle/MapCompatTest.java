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

package com.twofortyfouram.spackle;

import android.os.Build;
import android.support.test.filters.SdkSuppress;
import android.test.suitebuilder.annotation.SmallTest;
import android.util.ArrayMap;

import com.twofortyfouram.test.assertion.MoarAsserts;

import junit.framework.TestCase;

import java.util.HashMap;
import java.util.Map;

/**
 * Tests {@link MapCompat}.
 */
public final class MapCompatTest extends TestCase {

    @SmallTest
    public static void testNonInstantiable() {
        MoarAsserts.assertNoninstantiable(MapCompat.class);
    }

    @SmallTest
    public static void testNewMap_new_instance() {
        assertNotSame(MapCompat.newMap(0), MapCompat.newMap(0));
    }

    @SmallTest
    public static void testNewMap_zero() {
        final Map<String, Integer> map = MapCompat.newMap(0);

        assertNotNull(map);
        assertEquals(0, map.size());
    }

    @SmallTest
    public static void testNewMap_one() {
        final Map<String, Integer> map = MapCompat.newMap(1);

        assertNotNull(map);
        assertEquals(0, map.size());
    }

    @SmallTest
    public static void testNewMap_mutable() {
        final Map<String, Integer> map = MapCompat.newMap(1);

        map.put("test_key", 1);
        // the map doesn't throw UnsupportedOperationException!

        assertEquals(1, map.size());
        assertEquals(1, (int) map.get("test_key"));
    }

    @SmallTest
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.KITKAT)
    public static void testNewMap_ArrayMap() {
        assertEquals(ArrayMap.class.getName(), MapCompat.newMap(0).getClass().getName());
    }

    @SmallTest
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.KITKAT)
    public static void testNewMap_size_limit() {
        assertEquals(HashMap.class.getName(), MapCompat.newMap(MapCompat.ARRAY_MAP_MAX_SIZE_CUTOFF_INCLUSIVE+1).getClass().getName());
    }
}
