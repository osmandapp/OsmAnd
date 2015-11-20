/*
 * android-bootstrap-core-lib https://github.com/twofortyfouram/android-bootstrap-core
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
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;

import com.twofortyfouram.test.assertion.MoarAsserts;

/**
 * Tests {@link BundlePrinter}.
 */
public final class BundlePrinterTest extends AndroidTestCase {

    @SmallTest
    public static void testNonInstantiable() {
        MoarAsserts.assertNoninstantiable(BundlePrinter.class);
    }

    @SmallTest
    public static void testToString_null_bundle() {
        assertEquals("null", BundlePrinter.toString(null)); //$NON-NLS-1$
    }

    @SmallTest
    public static void testToString_empty_bundle() {
        assertEquals("empty", BundlePrinter.toString(new Bundle())); //$NON-NLS-1$
    }

    @SmallTest
    public static void testToString_null_key() {
        final Bundle bundle = new Bundle();
        bundle.putString(null, "test"); //$NON-NLS-1$
        BundlePrinter.toString(bundle);
        assertEquals("{null = test}", BundlePrinter.toString(bundle)); //$NON-NLS-1$
    }

    @SmallTest
    public static void testToString_null_value() {
        final Bundle bundle = new Bundle();
        bundle.putString("test", null); //$NON-NLS-1$
        assertEquals("{test = null}", BundlePrinter.toString(bundle)); //$NON-NLS-1$
    }

    @SmallTest
    public static void testToString_recursive() {
        final Bundle bundle1 = new Bundle();
        final Bundle bundle2 = new Bundle();
        bundle1.putBundle("bundle2", bundle2); //$NON-NLS-1$
        bundle2.putString("test", "test"); //$NON-NLS-1$//$NON-NLS-2$

        assertEquals("{bundle2 = {test = test}}", BundlePrinter.toString(bundle1)); //$NON-NLS-1$
    }

    @SmallTest
    public static void testToString_primitive_array() {
        final Bundle bundle1 = new Bundle();
        bundle1.putIntArray("test", new int[]{1, 2, 3}); //$NON-NLS-1$
        assertEquals("{test = [1, 2, 3]}", BundlePrinter.toString(bundle1)); //$NON-NLS-1$
    }

    @SmallTest
    public static void testToString_object_array() {
        final Bundle bundle1 = new Bundle();
        bundle1.putStringArray("test", new String[]{"a", "b",
                "c"}); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        assertEquals("{test = [a, b, c]}", BundlePrinter.toString(bundle1)); //$NON-NLS-1$
    }
}
