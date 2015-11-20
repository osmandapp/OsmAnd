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

import android.content.res.Configuration;
import android.location.Location;
import android.os.Bundle;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;

import com.twofortyfouram.test.assertion.MoarAsserts;

import java.util.ArrayList;
import java.util.LinkedList;

public final class BundleComparerTest extends AndroidTestCase {

    @SmallTest
    public static void testNonInstantiable() {
        MoarAsserts.assertNoninstantiable(BundleComparer.class);
    }

    /**
     * Tests comparing boolean types
     */
    @SmallTest
    public void testEmpty() {
        final Bundle bundle1 = new Bundle();
        final Bundle bundle2 = new Bundle();

        assertTrue(BundleComparer.areBundlesEqual(bundle1, bundle2));
    }

    /**
     * Tests comparing null types
     */
    @SmallTest
    public void testNull() {
        /*
         * Test null parameters
         */
        {
            assertTrue(BundleComparer.areBundlesEqual(null, null));
        }

        /*
         * Test one null parameter
         */
        {
            final Bundle bundle1 = new Bundle();
            bundle1.putString("com.test.extra", null); //$NON-NLS-1$
            final Bundle bundle2 = null;

            assertFalse(BundleComparer.areBundlesEqual(bundle1, bundle2));
            assertFalse(BundleComparer.areBundlesEqual(bundle2, bundle1));
        }

        /*
         * Test equal keys and values
         */
        {
            final Bundle bundle1 = new Bundle();
            bundle1.putString("com.test.extra", null); //$NON-NLS-1$
            final Bundle bundle2 = new Bundle();
            bundle2.putString("com.test.extra", null); //$NON-NLS-1$

            assertTrue(BundleComparer.areBundlesEqual(bundle1, bundle2));
            assertTrue(BundleComparer.areBundlesEqual(bundle2, bundle1));
        }

        /*
         * Test unequal values
         */
        {
            final Bundle bundle1 = new Bundle();
            bundle1.putString("com.test.extra", null); //$NON-NLS-1$
            final Bundle bundle2 = new Bundle();
            bundle2.putString("com.test.extra", "foo"); //$NON-NLS-1$ //$NON-NLS-2$

            assertFalse(BundleComparer.areBundlesEqual(bundle1, bundle2));
            assertFalse(BundleComparer.areBundlesEqual(bundle2, bundle1));
        }

        /*
         * Test unequal keys
         */
        {
            final Bundle bundle1 = new Bundle();
            bundle1.putString("com.test.extra1", null); //$NON-NLS-1$
            final Bundle bundle2 = new Bundle();
            bundle2.putString("com.test.extra2", null); //$NON-NLS-1$

            assertFalse(BundleComparer.areBundlesEqual(bundle1, bundle2));
            assertFalse(BundleComparer.areBundlesEqual(bundle2, bundle1));
        }

        /*
         * Test unequal keys and values
         */
        {
            final Bundle bundle1 = new Bundle();
            bundle1.putString("com.test.extra1", null); //$NON-NLS-1$
            final Bundle bundle2 = new Bundle();
            bundle2.putString("com.test.extra2", "foo"); //$NON-NLS-1$ //$NON-NLS-2$

            assertFalse(BundleComparer.areBundlesEqual(bundle1, bundle2));
            assertFalse(BundleComparer.areBundlesEqual(bundle2, bundle1));
        }
    }

    /**
     * Tests comparing boolean types
     */
    @SuppressWarnings("boxing")
    @SmallTest
    public void testBoolean() {
        /*
         * Test equal keys and values
         */
        {
            final Bundle bundle1 = new Bundle();
            bundle1.putBoolean("com.test.extra", false); //$NON-NLS-1$
            final Bundle bundle2 = new Bundle();
            bundle2.putBoolean("com.test.extra", false); //$NON-NLS-1$

            assertTrue(BundleComparer.areBundlesEqual(bundle1, bundle2));
            assertTrue(BundleComparer.areBundlesEqual(bundle2, bundle1));
        }

        /*
         * Test equal keys and values, autoboxing
         */
        {
            final Bundle bundle1 = new Bundle();
            bundle1.putBoolean("com.test.extra", false); //$NON-NLS-1$
            final Bundle bundle2 = new Bundle();
            bundle2.putBoolean("com.test.extra", Boolean.FALSE); //$NON-NLS-1$

            assertTrue(BundleComparer.areBundlesEqual(bundle1, bundle2));
            assertTrue(BundleComparer.areBundlesEqual(bundle2, bundle1));
        }

        /*
         * Test unequal values
         */
        {
            final Bundle bundle1 = new Bundle();
            bundle1.putBoolean("com.test.extra", false); //$NON-NLS-1$
            final Bundle bundle2 = new Bundle();
            bundle2.putBoolean("com.test.extra", true); //$NON-NLS-1$

            assertFalse(BundleComparer.areBundlesEqual(bundle1, bundle2));
            assertFalse(BundleComparer.areBundlesEqual(bundle2, bundle1));

        }

        /*
         * Test unequal keys and values
         */
        {
            final Bundle bundle1 = new Bundle();
            bundle1.putBoolean("com.test.extra1", false); //$NON-NLS-1$
            final Bundle bundle2 = new Bundle();
            bundle2.putBoolean("com.test.extra2", true); //$NON-NLS-1$

            assertFalse(BundleComparer.areBundlesEqual(bundle1, bundle2));
            assertFalse(BundleComparer.areBundlesEqual(bundle1, bundle2));

        }

        /*
         * Test unequal keys
         */
        {
            final Bundle bundle1 = new Bundle();
            bundle1.putBoolean("com.test.extra1", false); //$NON-NLS-1$
            final Bundle bundle2 = new Bundle();
            bundle2.putBoolean("com.test.extra2", false); //$NON-NLS-1$

            assertFalse(BundleComparer.areBundlesEqual(bundle1, bundle2));
            assertFalse(BundleComparer.areBundlesEqual(bundle1, bundle2));

        }
    }

    /**
     * Tests comparing boolean[] types
     */
    @SmallTest
    public void testBooleanArray() {
        /*
         * Test equal empty arrays
         */
        {
            final Bundle bundle1 = new Bundle();
            bundle1.putBooleanArray("com.test.extra_array", new boolean[]{}); //$NON-NLS-1$
            final Bundle bundle2 = new Bundle();
            bundle2.putBooleanArray("com.test.extra_array", new boolean[]{}); //$NON-NLS-1$

            assertTrue(BundleComparer.areBundlesEqual(bundle1, bundle2));
            assertTrue(BundleComparer.areBundlesEqual(bundle2, bundle1));
        }

        /*
         * Test equal non-empty arrays
         */
        {
            final Bundle bundle1 = new Bundle();
            bundle1.putBooleanArray("com.test.extra_array", new boolean[]{true}); //$NON-NLS-1$
            final Bundle bundle2 = new Bundle();
            bundle2.putBooleanArray("com.test.extra_array", new boolean[]{true}); //$NON-NLS-1$

            assertTrue(BundleComparer.areBundlesEqual(bundle1, bundle2));
            assertTrue(BundleComparer.areBundlesEqual(bundle2, bundle1));
        }

        /*
         * Test unequal keys
         */
        {
            final Bundle bundle1 = new Bundle();
            bundle1.putBooleanArray("com.test.extra_array1", new boolean[]{true}); //$NON-NLS-1$
            final Bundle bundle2 = new Bundle();
            bundle2.putBooleanArray("com.test.extra_array2", new boolean[]{true}); //$NON-NLS-1$

            assertFalse(BundleComparer.areBundlesEqual(bundle1, bundle2));
            assertFalse(BundleComparer.areBundlesEqual(bundle2, bundle1));
        }

        /*
         * Test unequal values
         */
        {
            final Bundle bundle1 = new Bundle();
            bundle1.putBooleanArray("com.test.extra_array", new boolean[]{true}); //$NON-NLS-1$
            final Bundle bundle2 = new Bundle();
            bundle2.putBooleanArray("com.test.extra_array", new boolean[]{false}); //$NON-NLS-1$

            assertFalse(BundleComparer.areBundlesEqual(bundle1, bundle2));
            assertFalse(BundleComparer.areBundlesEqual(bundle2, bundle1));

        }

        /*
         * Test mixing primitive and object
         */
        {
            final Bundle bundle1 = new Bundle();
            bundle1.putSerializable("com.test.extra_array",
                    new Boolean[]{Boolean.TRUE}); //$NON-NLS-1$
            final Bundle bundle2 = new Bundle();
            bundle2.putBooleanArray("com.test.extra_array", new boolean[]{true}); //$NON-NLS-1$

            assertFalse(BundleComparer.areBundlesEqual(bundle1, bundle2));
            assertFalse(BundleComparer.areBundlesEqual(bundle2, bundle1));

        }
    }

    /**
     * Tests comparing boolean types
     */
    @SmallTest
    public void testBundle() {
        /*
         * Test equal empty bundles
         */
        {
            final Bundle bundle1 = new Bundle();
            bundle1.putBundle("com.test.extra_bundle", new Bundle()); //$NON-NLS-1$
            final Bundle bundle2 = new Bundle();
            bundle2.putBundle("com.test.extra_bundle", new Bundle()); //$NON-NLS-1$

            assertTrue(BundleComparer.areBundlesEqual(bundle1, bundle2));
            assertTrue(BundleComparer.areBundlesEqual(bundle2, bundle1));
        }
    }

    /**
     * Tests comparing boolean types
     */
    @SmallTest
    public void testByte() {
        /*
         * Test equal keys and values
         */
        {
            final Bundle bundle1 = new Bundle();
            bundle1.putByte("com.test.extra", Byte.MAX_VALUE); //$NON-NLS-1$
            final Bundle bundle2 = new Bundle();
            bundle2.putByte("com.test.extra", Byte.MAX_VALUE); //$NON-NLS-1$

            assertTrue(BundleComparer.areBundlesEqual(bundle1, bundle2));
            assertTrue(BundleComparer.areBundlesEqual(bundle2, bundle1));
        }

        /*
         * Test unequal values
         */
        {
            final Bundle bundle1 = new Bundle();
            bundle1.putByte("com.test.extra", Byte.MIN_VALUE); //$NON-NLS-1$
            final Bundle bundle2 = new Bundle();
            bundle2.putByte("com.test.extra", Byte.MAX_VALUE); //$NON-NLS-1$

            assertFalse(BundleComparer.areBundlesEqual(bundle1, bundle2));
            assertFalse(BundleComparer.areBundlesEqual(bundle2, bundle1));

        }

        /*
         * Test unequal keys and values
         */
        {
            final Bundle bundle1 = new Bundle();
            bundle1.putByte("com.test.extra1", Byte.MAX_VALUE); //$NON-NLS-1$
            final Bundle bundle2 = new Bundle();
            bundle2.putByte("com.test.extra2", Byte.MIN_VALUE); //$NON-NLS-1$

            assertFalse(BundleComparer.areBundlesEqual(bundle1, bundle2));
            assertFalse(BundleComparer.areBundlesEqual(bundle1, bundle2));

        }

        /*
         * Test unequal keys
         */
        {
            final Bundle bundle1 = new Bundle();
            bundle1.putByte("com.test.extra1", Byte.MAX_VALUE); //$NON-NLS-1$
            final Bundle bundle2 = new Bundle();
            bundle2.putByte("com.test.extra2", Byte.MAX_VALUE); //$NON-NLS-1$

            assertFalse(BundleComparer.areBundlesEqual(bundle1, bundle2));
            assertFalse(BundleComparer.areBundlesEqual(bundle1, bundle2));

        }
    }

    /**
     * Tests comparing byte[] types
     */
    @SuppressWarnings("boxing")
    @SmallTest
    public void testByteArray() {
        /*
         * Test equal empty arrays
         */
        {
            final Bundle bundle1 = new Bundle();
            bundle1.putByteArray("com.test.extra_array", new byte[]{}); //$NON-NLS-1$
            final Bundle bundle2 = new Bundle();
            bundle2.putByteArray("com.test.extra_array", new byte[]{}); //$NON-NLS-1$

            assertTrue(BundleComparer.areBundlesEqual(bundle1, bundle2));
            assertTrue(BundleComparer.areBundlesEqual(bundle2, bundle1));
        }

        /*
         * Test equal non-empty arrays
         */
        {
            final Bundle bundle1 = new Bundle();
            bundle1.putByteArray("com.test.extra_array", new byte[]{Byte.MAX_VALUE}); //$NON-NLS-1$
            final Bundle bundle2 = new Bundle();
            bundle2.putByteArray("com.test.extra_array", new byte[]{Byte.MAX_VALUE}); //$NON-NLS-1$

            assertTrue(BundleComparer.areBundlesEqual(bundle1, bundle2));
            assertTrue(BundleComparer.areBundlesEqual(bundle2, bundle1));
        }

        /*
         * Test unequal keys
         */
        {
            final Bundle bundle1 = new Bundle();
            bundle1.putByteArray("com.test.extra_array1", new byte[]{Byte.MAX_VALUE}); //$NON-NLS-1$
            final Bundle bundle2 = new Bundle();
            bundle2.putByteArray("com.test.extra_array2", new byte[]{Byte.MAX_VALUE}); //$NON-NLS-1$

            assertFalse(BundleComparer.areBundlesEqual(bundle1, bundle2));
            assertFalse(BundleComparer.areBundlesEqual(bundle2, bundle1));
        }

        /*
         * Test unequal values
         */
        {
            final Bundle bundle1 = new Bundle();
            bundle1.putByteArray("com.test.extra_array", new byte[]{Byte.MIN_VALUE}); //$NON-NLS-1$
            final Bundle bundle2 = new Bundle();
            bundle2.putByteArray("com.test.extra_array", new byte[]{Byte.MAX_VALUE}); //$NON-NLS-1$

            assertFalse(BundleComparer.areBundlesEqual(bundle1, bundle2));
            assertFalse(BundleComparer.areBundlesEqual(bundle2, bundle1));

        }

        /*
         * Test mixing primitive and object
         */
        {
            final Bundle bundle1 = new Bundle();
            bundle1.putSerializable("com.test.extra_array",
                    new Byte[]{Byte.MAX_VALUE}); //$NON-NLS-1$
            final Bundle bundle2 = new Bundle();
            bundle2.putByteArray("com.test.extra_array", new byte[]{Byte.MAX_VALUE}); //$NON-NLS-1$

            assertFalse(BundleComparer.areBundlesEqual(bundle1, bundle2));
            assertFalse(BundleComparer.areBundlesEqual(bundle2, bundle1));

        }
    }

    /**
     * Tests comparing boolean types
     */
    @SmallTest
    public void testChar() {
        /*
         * Test equal keys and values
         */
        {
            final Bundle bundle1 = new Bundle();
            bundle1.putChar("com.test.extra", 'a'); //$NON-NLS-1$
            final Bundle bundle2 = new Bundle();
            bundle2.putChar("com.test.extra", 'a'); //$NON-NLS-1$

            assertTrue(BundleComparer.areBundlesEqual(bundle1, bundle2));
            assertTrue(BundleComparer.areBundlesEqual(bundle2, bundle1));

        }

        /*
         * Test unequal values
         */
        {
            final Bundle bundle1 = new Bundle();
            bundle1.putChar("com.test.extra", 'a'); //$NON-NLS-1$
            final Bundle bundle2 = new Bundle();
            bundle2.putChar("com.test.extra", 'b'); //$NON-NLS-1$

            assertFalse(BundleComparer.areBundlesEqual(bundle1, bundle2));
            assertFalse(BundleComparer.areBundlesEqual(bundle2, bundle1));

        }

        /*
         * Test unequal keys and values
         */
        {
            final Bundle bundle1 = new Bundle();
            bundle1.putChar("com.test.extra1", 'a'); //$NON-NLS-1$
            final Bundle bundle2 = new Bundle();
            bundle2.putChar("com.test.extra2", 'b'); //$NON-NLS-1$

            assertFalse(BundleComparer.areBundlesEqual(bundle1, bundle2));
            assertFalse(BundleComparer.areBundlesEqual(bundle1, bundle2));

        }

        /*
         * Test unequal keys
         */
        {
            final Bundle bundle1 = new Bundle();
            bundle1.putChar("com.test.extra1", 'a'); //$NON-NLS-1$
            final Bundle bundle2 = new Bundle();
            bundle2.putChar("com.test.extra2", 'a'); //$NON-NLS-1$

            assertFalse(BundleComparer.areBundlesEqual(bundle1, bundle2));
            assertFalse(BundleComparer.areBundlesEqual(bundle1, bundle2));

        }
    }

    /**
     * Tests comparing char[] types
     */
    @SmallTest
    public void testCharArray() {
        /*
         * Test equal empty arrays
         */
        {
            final Bundle bundle1 = new Bundle();
            bundle1.putCharArray("com.test.extra_array", new char[]{}); //$NON-NLS-1$
            final Bundle bundle2 = new Bundle();
            bundle2.putCharArray("com.test.extra_array", new char[]{}); //$NON-NLS-1$

            assertTrue(BundleComparer.areBundlesEqual(bundle1, bundle2));
            assertTrue(BundleComparer.areBundlesEqual(bundle2, bundle1));
        }

        /*
         * Test equal non-empty arrays
         */
        {
            final Bundle bundle1 = new Bundle();
            bundle1.putCharArray("com.test.extra_array", new char[]{'a'}); //$NON-NLS-1$
            final Bundle bundle2 = new Bundle();
            bundle2.putCharArray("com.test.extra_array", new char[]{'a'}); //$NON-NLS-1$

            assertTrue(BundleComparer.areBundlesEqual(bundle1, bundle2));
            assertTrue(BundleComparer.areBundlesEqual(bundle2, bundle1));
        }

        /*
         * Test unequal keys
         */
        {
            final Bundle bundle1 = new Bundle();
            bundle1.putCharArray("com.test.extra_array1", new char[]{'a'}); //$NON-NLS-1$
            final Bundle bundle2 = new Bundle();
            bundle2.putCharArray("com.test.extra_array2", new char[]{'a'}); //$NON-NLS-1$

            assertFalse(BundleComparer.areBundlesEqual(bundle1, bundle2));
            assertFalse(BundleComparer.areBundlesEqual(bundle2, bundle1));
        }

        /*
         * Test unequal values
         */
        {
            final Bundle bundle1 = new Bundle();
            bundle1.putCharArray("com.test.extra_array", new char[]{'a'}); //$NON-NLS-1$
            final Bundle bundle2 = new Bundle();
            bundle2.putCharArray("com.test.extra_array", new char[]{'b'}); //$NON-NLS-1$

            assertFalse(BundleComparer.areBundlesEqual(bundle1, bundle2));
            assertFalse(BundleComparer.areBundlesEqual(bundle2, bundle1));
        }

        /*
         * Test mixing primitive and object
         */
        {
            final Bundle bundle1 = new Bundle();
            bundle1.putSerializable(
                    "com.test.extra_array", new Character[]{Character.valueOf('a')}); //$NON-NLS-1$
            final Bundle bundle2 = new Bundle();
            bundle2.putCharArray("com.test.extra_array", new char[]{'a'}); //$NON-NLS-1$

            assertFalse(BundleComparer.areBundlesEqual(bundle1, bundle2));
            assertFalse(BundleComparer.areBundlesEqual(bundle2, bundle1));
        }
    }

    /**
     * Tests comparing Charsequence types
     */
    @SmallTest
    public void testCharsequence() {
        /*
         * Test equal keys and values
         */
        {
            final Bundle bundle1 = new Bundle();
            bundle1.putCharSequence("com.test.extra", "foo"); //$NON-NLS-1$ //$NON-NLS-2$
            final Bundle bundle2 = new Bundle();
            bundle2.putCharSequence("com.test.extra", "foo"); //$NON-NLS-1$ //$NON-NLS-2$

            assertTrue(BundleComparer.areBundlesEqual(bundle1, bundle2));
            assertTrue(BundleComparer.areBundlesEqual(bundle2, bundle1));
        }

        /*
         * Test unequal values
         */
        {
            final Bundle bundle1 = new Bundle();
            bundle1.putCharSequence("com.test.extra", "foo"); //$NON-NLS-1$ //$NON-NLS-2$
            final Bundle bundle2 = new Bundle();
            bundle2.putCharSequence("com.test.extra", "bar"); //$NON-NLS-1$ //$NON-NLS-2$

            assertFalse(BundleComparer.areBundlesEqual(bundle1, bundle2));
            assertFalse(BundleComparer.areBundlesEqual(bundle2, bundle1));
        }

        /*
         * Test unequal keys and values
         */
        {
            final Bundle bundle1 = new Bundle();
            bundle1.putCharSequence("com.test.extra1", "foo"); //$NON-NLS-1$ //$NON-NLS-2$
            final Bundle bundle2 = new Bundle();
            bundle2.putCharSequence("com.test.extra2", "bar"); //$NON-NLS-1$ //$NON-NLS-2$

            assertFalse(BundleComparer.areBundlesEqual(bundle1, bundle2));
            assertFalse(BundleComparer.areBundlesEqual(bundle1, bundle2));

        }

        /*
         * Test unequal keys
         */
        {
            final Bundle bundle1 = new Bundle();
            bundle1.putCharSequence("com.test.extra1", "foo"); //$NON-NLS-1$ //$NON-NLS-2$
            final Bundle bundle2 = new Bundle();
            bundle2.putCharSequence("com.test.extra2", "bar"); //$NON-NLS-1$ //$NON-NLS-2$

            assertFalse(BundleComparer.areBundlesEqual(bundle1, bundle2));
            assertFalse(BundleComparer.areBundlesEqual(bundle1, bundle2));
        }
    }

    /**
     * Tests comparing double types
     */
    @SmallTest
    public void testDouble() {
        /*
         * Test equal keys and values
         */
        {
            final Bundle bundle1 = new Bundle();
            bundle1.putDouble("com.test.extra", 1.2); //$NON-NLS-1$
            final Bundle bundle2 = new Bundle();
            bundle2.putDouble("com.test.extra", 1.2); //$NON-NLS-1$

            assertTrue(BundleComparer.areBundlesEqual(bundle1, bundle2));
            assertTrue(BundleComparer.areBundlesEqual(bundle2, bundle1));
        }

        /*
         * Test unequal values
         */
        {
            final Bundle bundle1 = new Bundle();
            bundle1.putDouble("com.test.extra", 1.2); //$NON-NLS-1$
            final Bundle bundle2 = new Bundle();
            bundle2.putDouble("com.test.extra", 1.3); //$NON-NLS-1$

            assertFalse(BundleComparer.areBundlesEqual(bundle1, bundle2));
            assertFalse(BundleComparer.areBundlesEqual(bundle2, bundle1));
        }

        /*
         * Test unequal keys and values
         */
        {
            final Bundle bundle1 = new Bundle();
            bundle1.putDouble("com.test.extra1", 1.2); //$NON-NLS-1$
            final Bundle bundle2 = new Bundle();
            bundle2.putDouble("com.test.extra2", 1.3); //$NON-NLS-1$

            assertFalse(BundleComparer.areBundlesEqual(bundle1, bundle2));
            assertFalse(BundleComparer.areBundlesEqual(bundle1, bundle2));
        }

        /*
         * Test unequal keys
         */
        {
            final Bundle bundle1 = new Bundle();
            bundle1.putDouble("com.test.extra1", 1.2); //$NON-NLS-1$
            final Bundle bundle2 = new Bundle();
            bundle2.putDouble("com.test.extra2", 1.2); //$NON-NLS-1$

            assertFalse(BundleComparer.areBundlesEqual(bundle1, bundle2));
            assertFalse(BundleComparer.areBundlesEqual(bundle1, bundle2));
        }
    }

    /**
     * Tests comparing double[] types
     */
    @SuppressWarnings("boxing")
    @SmallTest
    public void testDoubleArray() {
        /*
         * Test equal empty arrays
         */
        {
            final Bundle bundle1 = new Bundle();
            bundle1.putDoubleArray("com.test.extra_array", new double[]{}); //$NON-NLS-1$
            final Bundle bundle2 = new Bundle();
            bundle2.putDoubleArray("com.test.extra_array", new double[]{}); //$NON-NLS-1$

            assertTrue(BundleComparer.areBundlesEqual(bundle1, bundle2));
            assertTrue(BundleComparer.areBundlesEqual(bundle2, bundle1));
        }

        /*
         * Test equal non-empty arrays
         */
        {
            final Bundle bundle1 = new Bundle();
            bundle1.putDoubleArray("com.test.extra_array", new double[]{1.2}); //$NON-NLS-1$
            final Bundle bundle2 = new Bundle();
            bundle2.putDoubleArray("com.test.extra_array", new double[]{1.2}); //$NON-NLS-1$

            assertTrue(BundleComparer.areBundlesEqual(bundle1, bundle2));
            assertTrue(BundleComparer.areBundlesEqual(bundle2, bundle1));
        }

        /*
         * Test unequal keys
         */
        {
            final Bundle bundle1 = new Bundle();
            bundle1.putDoubleArray("com.test.extra_array1", new double[]{1.2}); //$NON-NLS-1$
            final Bundle bundle2 = new Bundle();
            bundle2.putDoubleArray("com.test.extra_array2", new double[]{1.2}); //$NON-NLS-1$

            assertFalse(BundleComparer.areBundlesEqual(bundle1, bundle2));
            assertFalse(BundleComparer.areBundlesEqual(bundle2, bundle1));
        }

        /*
         * Test unequal values
         */
        {
            final Bundle bundle1 = new Bundle();
            bundle1.putDoubleArray("com.test.extra_array", new double[]{1.2}); //$NON-NLS-1$
            final Bundle bundle2 = new Bundle();
            bundle2.putDoubleArray("com.test.extra_array", new double[]{1.3}); //$NON-NLS-1$

            assertFalse(BundleComparer.areBundlesEqual(bundle1, bundle2));
            assertFalse(BundleComparer.areBundlesEqual(bundle2, bundle1));

        }

        /*
         * Test mixing primitive and object
         */
        {
            final Bundle bundle1 = new Bundle();
            bundle1.putSerializable("com.test.extra_array", new Double[]{1.2}); //$NON-NLS-1$
            final Bundle bundle2 = new Bundle();
            bundle2.putDoubleArray("com.test.extra_array", new double[]{1.2}); //$NON-NLS-1$

            assertFalse(BundleComparer.areBundlesEqual(bundle1, bundle2));
            assertFalse(BundleComparer.areBundlesEqual(bundle2, bundle1));

        }
    }

    /**
     * Tests comparing float types
     */
    @SmallTest
    public void testFloat() {
        /*
         * Test equal keys and values
         */
        {
            final Bundle bundle1 = new Bundle();
            bundle1.putFloat("com.test.extra", 1.2f); //$NON-NLS-1$
            final Bundle bundle2 = new Bundle();
            bundle2.putFloat("com.test.extra", 1.2f); //$NON-NLS-1$

            assertTrue(BundleComparer.areBundlesEqual(bundle1, bundle2));
            assertTrue(BundleComparer.areBundlesEqual(bundle2, bundle1));

        }

        /*
         * Test unequal values
         */
        {
            final Bundle bundle1 = new Bundle();
            bundle1.putFloat("com.test.extra", 1.2f); //$NON-NLS-1$
            final Bundle bundle2 = new Bundle();
            bundle2.putFloat("com.test.extra", 1.3f); //$NON-NLS-1$

            assertFalse(BundleComparer.areBundlesEqual(bundle1, bundle2));
            assertFalse(BundleComparer.areBundlesEqual(bundle2, bundle1));

        }

        /*
         * Test unequal keys and values
         */
        {
            final Bundle bundle1 = new Bundle();
            bundle1.putFloat("com.test.extra1", 1.2f); //$NON-NLS-1$
            final Bundle bundle2 = new Bundle();
            bundle2.putFloat("com.test.extra2", 1.3f); //$NON-NLS-1$

            assertFalse(BundleComparer.areBundlesEqual(bundle1, bundle2));
            assertFalse(BundleComparer.areBundlesEqual(bundle1, bundle2));

        }

        /*
         * Test unequal keys
         */
        {
            final Bundle bundle1 = new Bundle();
            bundle1.putFloat("com.test.extra1", 1.2f); //$NON-NLS-1$
            final Bundle bundle2 = new Bundle();
            bundle2.putFloat("com.test.extra2", 1.2f); //$NON-NLS-1$

            assertFalse(BundleComparer.areBundlesEqual(bundle1, bundle2));
            assertFalse(BundleComparer.areBundlesEqual(bundle1, bundle2));

        }
    }

    /**
     * Tests comparing float[] types
     */
    @SuppressWarnings("boxing")
    @SmallTest
    public void testFloatArray() {
        /*
         * Test equal empty arrays
         */
        {
            final Bundle bundle1 = new Bundle();
            bundle1.putFloatArray("com.test.extra_array", new float[]{}); //$NON-NLS-1$
            final Bundle bundle2 = new Bundle();
            bundle2.putFloatArray("com.test.extra_array", new float[]{}); //$NON-NLS-1$

            assertTrue(BundleComparer.areBundlesEqual(bundle1, bundle2));
            assertTrue(BundleComparer.areBundlesEqual(bundle2, bundle1));
        }

        /*
         * Test equal non-empty arrays
         */
        {
            final Bundle bundle1 = new Bundle();
            bundle1.putFloatArray("com.test.extra_array", new float[]{1.2f}); //$NON-NLS-1$
            final Bundle bundle2 = new Bundle();
            bundle2.putFloatArray("com.test.extra_array", new float[]{1.2f}); //$NON-NLS-1$

            assertTrue(BundleComparer.areBundlesEqual(bundle1, bundle2));
            assertTrue(BundleComparer.areBundlesEqual(bundle2, bundle1));
        }

        /*
         * Test unequal keys
         */
        {
            final Bundle bundle1 = new Bundle();
            bundle1.putFloatArray("com.test.extra_array1", new float[]{1.2f}); //$NON-NLS-1$
            final Bundle bundle2 = new Bundle();
            bundle2.putFloatArray("com.test.extra_array2", new float[]{1.2f}); //$NON-NLS-1$

            assertFalse(BundleComparer.areBundlesEqual(bundle1, bundle2));
            assertFalse(BundleComparer.areBundlesEqual(bundle2, bundle1));
        }

        /*
         * Test unequal values
         */
        {
            final Bundle bundle1 = new Bundle();
            bundle1.putFloatArray("com.test.extra_array", new float[]{1.2f}); //$NON-NLS-1$
            final Bundle bundle2 = new Bundle();
            bundle2.putFloatArray("com.test.extra_array", new float[]{1.3f}); //$NON-NLS-1$

            assertFalse(BundleComparer.areBundlesEqual(bundle1, bundle2));
            assertFalse(BundleComparer.areBundlesEqual(bundle2, bundle1));

        }

        /*
         * Test mixing primitive and object
         */
        {
            final Bundle bundle1 = new Bundle();
            bundle1.putSerializable("com.test.extra_array", new Float[]{1.2f}); //$NON-NLS-1$
            final Bundle bundle2 = new Bundle();
            bundle2.putFloatArray("com.test.extra_array", new float[]{1.2f}); //$NON-NLS-1$

            assertFalse(BundleComparer.areBundlesEqual(bundle1, bundle2));
            assertFalse(BundleComparer.areBundlesEqual(bundle2, bundle1));

        }
    }

    /**
     * Tests comparing float types
     */
    @SmallTest
    public void testInt() {
        /*
         * Test equal keys and values
         */
        {
            final Bundle bundle1 = new Bundle();
            bundle1.putInt("com.test.extra", 1); //$NON-NLS-1$
            final Bundle bundle2 = new Bundle();
            bundle2.putInt("com.test.extra", 1); //$NON-NLS-1$

            assertTrue(BundleComparer.areBundlesEqual(bundle1, bundle2));
            assertTrue(BundleComparer.areBundlesEqual(bundle2, bundle1));

        }

        /*
         * Test unequal values
         */
        {
            final Bundle bundle1 = new Bundle();
            bundle1.putInt("com.test.extra", 1); //$NON-NLS-1$
            final Bundle bundle2 = new Bundle();
            bundle2.putInt("com.test.extra", 2); //$NON-NLS-1$

            assertFalse(BundleComparer.areBundlesEqual(bundle1, bundle2));
            assertFalse(BundleComparer.areBundlesEqual(bundle2, bundle1));

        }

        /*
         * Test unequal keys and values
         */
        {
            final Bundle bundle1 = new Bundle();
            bundle1.putInt("com.test.extra1", 1); //$NON-NLS-1$
            final Bundle bundle2 = new Bundle();
            bundle2.putInt("com.test.extra2", 2); //$NON-NLS-1$

            assertFalse(BundleComparer.areBundlesEqual(bundle1, bundle2));
            assertFalse(BundleComparer.areBundlesEqual(bundle1, bundle2));

        }

        /*
         * Test unequal keys
         */
        {
            final Bundle bundle1 = new Bundle();
            bundle1.putInt("com.test.extra1", 1); //$NON-NLS-1$
            final Bundle bundle2 = new Bundle();
            bundle2.putInt("com.test.extra2", 1); //$NON-NLS-1$

            assertFalse(BundleComparer.areBundlesEqual(bundle1, bundle2));
            assertFalse(BundleComparer.areBundlesEqual(bundle1, bundle2));
        }

        /*
         * Test type differences
         */
        {
            final Bundle bundle1 = new Bundle();
            bundle1.putLong("com.test.extra1", 1); //$NON-NLS-1$
            final Bundle bundle2 = new Bundle();
            bundle2.putInt("com.test.extra2", 1); //$NON-NLS-1$

            assertFalse(BundleComparer.areBundlesEqual(bundle1, bundle2));
            assertFalse(BundleComparer.areBundlesEqual(bundle1, bundle2));
        }
    }

    /**
     * Tests comparing int[] types
     */
    @SuppressWarnings("boxing")
    @SmallTest
    public void testIntArray() {
        /*
         * Test equal empty arrays
         */
        {
            final Bundle bundle1 = new Bundle();
            bundle1.putIntArray("com.test.extra_array", new int[]{}); //$NON-NLS-1$
            final Bundle bundle2 = new Bundle();
            bundle2.putIntArray("com.test.extra_array", new int[]{}); //$NON-NLS-1$

            assertTrue(BundleComparer.areBundlesEqual(bundle1, bundle2));
            assertTrue(BundleComparer.areBundlesEqual(bundle2, bundle1));
        }

        /*
         * Test equal non-empty arrays
         */
        {
            final Bundle bundle1 = new Bundle();
            bundle1.putIntArray("com.test.extra_array", new int[]{1}); //$NON-NLS-1$
            final Bundle bundle2 = new Bundle();
            bundle2.putIntArray("com.test.extra_array", new int[]{1}); //$NON-NLS-1$

            assertTrue(BundleComparer.areBundlesEqual(bundle1, bundle2));
            assertTrue(BundleComparer.areBundlesEqual(bundle2, bundle1));
        }

        /*
         * Test unequal keys
         */
        {
            final Bundle bundle1 = new Bundle();
            bundle1.putIntArray("com.test.extra_array1", new int[]{1}); //$NON-NLS-1$
            final Bundle bundle2 = new Bundle();
            bundle2.putIntArray("com.test.extra_array2", new int[]{1}); //$NON-NLS-1$

            assertFalse(BundleComparer.areBundlesEqual(bundle1, bundle2));
            assertFalse(BundleComparer.areBundlesEqual(bundle2, bundle1));
        }

        /*
         * Test unequal values
         */
        {
            final Bundle bundle1 = new Bundle();
            bundle1.putIntArray("com.test.extra_array", new int[]{1}); //$NON-NLS-1$
            final Bundle bundle2 = new Bundle();
            bundle2.putIntArray("com.test.extra_array", new int[]{2}); //$NON-NLS-1$

            assertFalse(BundleComparer.areBundlesEqual(bundle1, bundle2));
            assertFalse(BundleComparer.areBundlesEqual(bundle2, bundle1));

        }

        /*
         * Test mixing primitive and object
         */
        {
            final Bundle bundle1 = new Bundle();
            bundle1.putSerializable("com.test.extra_array", new Integer[]{1}); //$NON-NLS-1$
            final Bundle bundle2 = new Bundle();
            bundle2.putIntArray("com.test.extra_array", new int[]{1}); //$NON-NLS-1$

            assertFalse(BundleComparer.areBundlesEqual(bundle1, bundle2));
            assertFalse(BundleComparer.areBundlesEqual(bundle2, bundle1));

        }
    }

    /**
     * Tests comparing ArrayList<Integer> types
     */
    @SmallTest
    public void testIntegerArrayList() {
        /*
         * Test equal empty arrays
         */
        {
            final Bundle bundle1 = new Bundle();
            bundle1.putIntegerArrayList("com.test.extra_array",
                    new ArrayList<Integer>()); //$NON-NLS-1$
            final Bundle bundle2 = new Bundle();
            bundle2.putIntegerArrayList("com.test.extra_array",
                    new ArrayList<Integer>()); //$NON-NLS-1$

            assertTrue(BundleComparer.areBundlesEqual(bundle1, bundle2));
            assertTrue(BundleComparer.areBundlesEqual(bundle2, bundle1));
        }

        /*
         * Test equal non-empty arrays
         */
        {
            final Bundle bundle1 = new Bundle();
            final ArrayList<Integer> list1 = new ArrayList<Integer>();
            list1.add(Integer.valueOf(1));
            bundle1.putIntegerArrayList("com.test.extra_array", list1); //$NON-NLS-1$

            final Bundle bundle2 = new Bundle();
            final ArrayList<Integer> list2 = new ArrayList<Integer>();
            list2.add(Integer.valueOf(1));
            bundle2.putIntegerArrayList("com.test.extra_array", list2); //$NON-NLS-1$

            assertTrue(BundleComparer.areBundlesEqual(bundle1, bundle2));
            assertTrue(BundleComparer.areBundlesEqual(bundle2, bundle1));
        }

        /*
         * Test unequal keys
         */
        {
            final Bundle bundle1 = new Bundle();
            final ArrayList<Integer> list1 = new ArrayList<Integer>();
            list1.add(Integer.valueOf(1));
            bundle1.putIntegerArrayList("com.test.extra_array1", list1); //$NON-NLS-1$

            final Bundle bundle2 = new Bundle();
            final ArrayList<Integer> list2 = new ArrayList<Integer>();
            list2.add(Integer.valueOf(1));
            bundle2.putIntegerArrayList("com.test.extra_array2", list2); //$NON-NLS-1$

            assertFalse(BundleComparer.areBundlesEqual(bundle1, bundle2));
            assertFalse(BundleComparer.areBundlesEqual(bundle2, bundle1));
        }

        /*
         * Test unequal values
         */
        {
            final Bundle bundle1 = new Bundle();
            final ArrayList<Integer> list1 = new ArrayList<Integer>();
            list1.add(Integer.valueOf(1));
            bundle1.putIntegerArrayList("com.test.extra_array", list1); //$NON-NLS-1$

            final Bundle bundle2 = new Bundle();
            final ArrayList<Integer> list2 = new ArrayList<Integer>();
            list2.add(Integer.valueOf(2));
            bundle2.putIntegerArrayList("com.test.extra_array", list2); //$NON-NLS-1$

            assertFalse(BundleComparer.areBundlesEqual(bundle1, bundle2));
            assertFalse(BundleComparer.areBundlesEqual(bundle2, bundle1));
        }
    }

    /**
     * Tests comparing long types
     */
    @SmallTest
    public void testLong() {
        /*
         * Test equal keys and values
         */
        {
            final Bundle bundle1 = new Bundle();
            bundle1.putLong("com.test.extra", Long.MAX_VALUE); //$NON-NLS-1$
            final Bundle bundle2 = new Bundle();
            bundle2.putLong("com.test.extra", Long.MAX_VALUE); //$NON-NLS-1$

            assertTrue(BundleComparer.areBundlesEqual(bundle1, bundle2));
            assertTrue(BundleComparer.areBundlesEqual(bundle2, bundle1));

        }

        /*
         * Test unequal values
         */
        {
            final Bundle bundle1 = new Bundle();
            bundle1.putLong("com.test.extra", Long.MAX_VALUE); //$NON-NLS-1$
            final Bundle bundle2 = new Bundle();
            bundle2.putLong("com.test.extra", Long.MIN_VALUE); //$NON-NLS-1$

            assertFalse(BundleComparer.areBundlesEqual(bundle1, bundle2));
            assertFalse(BundleComparer.areBundlesEqual(bundle2, bundle1));

        }

        /*
         * Test unequal keys and values
         */
        {
            final Bundle bundle1 = new Bundle();
            bundle1.putLong("com.test.extra1", Long.MAX_VALUE); //$NON-NLS-1$
            final Bundle bundle2 = new Bundle();
            bundle2.putLong("com.test.extra2", Long.MIN_VALUE); //$NON-NLS-1$

            assertFalse(BundleComparer.areBundlesEqual(bundle1, bundle2));
            assertFalse(BundleComparer.areBundlesEqual(bundle1, bundle2));

        }

        /*
         * Test unequal keys
         */
        {
            final Bundle bundle1 = new Bundle();
            bundle1.putLong("com.test.extra1", Long.MAX_VALUE); //$NON-NLS-1$
            final Bundle bundle2 = new Bundle();
            bundle2.putLong("com.test.extra2", Long.MAX_VALUE); //$NON-NLS-1$

            assertFalse(BundleComparer.areBundlesEqual(bundle1, bundle2));
            assertFalse(BundleComparer.areBundlesEqual(bundle1, bundle2));

        }
    }

    /**
     * Tests comparing long[] types
     */
    @SuppressWarnings("boxing")
    @SmallTest
    public void testLongArray() {
        /*
         * Test equal empty arrays
         */
        {
            final Bundle bundle1 = new Bundle();
            bundle1.putLongArray("com.test.extra_array", new long[]{}); //$NON-NLS-1$
            final Bundle bundle2 = new Bundle();
            bundle2.putLongArray("com.test.extra_array", new long[]{}); //$NON-NLS-1$

            assertTrue(BundleComparer.areBundlesEqual(bundle1, bundle2));
            assertTrue(BundleComparer.areBundlesEqual(bundle2, bundle1));
        }

        /*
         * Test equal non-empty arrays
         */
        {
            final Bundle bundle1 = new Bundle();
            bundle1.putLongArray("com.test.extra_array", new long[]{Long.MAX_VALUE}); //$NON-NLS-1$
            final Bundle bundle2 = new Bundle();
            bundle2.putLongArray("com.test.extra_array", new long[]{Long.MAX_VALUE}); //$NON-NLS-1$

            assertTrue(BundleComparer.areBundlesEqual(bundle1, bundle2));
            assertTrue(BundleComparer.areBundlesEqual(bundle2, bundle1));
        }

        /*
         * Test unequal keys
         */
        {
            final Bundle bundle1 = new Bundle();
            bundle1.putLongArray("com.test.extra_array1", new long[]{Long.MAX_VALUE}); //$NON-NLS-1$
            final Bundle bundle2 = new Bundle();
            bundle2.putLongArray("com.test.extra_array2", new long[]{Long.MAX_VALUE}); //$NON-NLS-1$

            assertFalse(BundleComparer.areBundlesEqual(bundle1, bundle2));
            assertFalse(BundleComparer.areBundlesEqual(bundle2, bundle1));
        }

        /*
         * Test unequal values
         */
        {
            final Bundle bundle1 = new Bundle();
            bundle1.putLongArray("com.test.extra_array", new long[]{Long.MAX_VALUE}); //$NON-NLS-1$
            final Bundle bundle2 = new Bundle();
            bundle2.putLongArray("com.test.extra_array", new long[]{Long.MIN_VALUE}); //$NON-NLS-1$

            assertFalse(BundleComparer.areBundlesEqual(bundle1, bundle2));
            assertFalse(BundleComparer.areBundlesEqual(bundle2, bundle1));

        }

        /*
         * Test mixing primitive and object
         */
        {
            final Bundle bundle1 = new Bundle();
            bundle1.putSerializable("com.test.extra_array",
                    new Long[]{Long.MAX_VALUE}); //$NON-NLS-1$
            final Bundle bundle2 = new Bundle();
            bundle2.putLongArray("com.test.extra_array", new long[]{Long.MAX_VALUE}); //$NON-NLS-1$

            assertFalse(BundleComparer.areBundlesEqual(bundle1, bundle2));
            assertFalse(BundleComparer.areBundlesEqual(bundle2, bundle1));

        }
    }

    /**
     * Tests comparing long types
     */
    @SmallTest
    public void testParcelable() {
        /*
         * Test equal keys and values
         */
        {
            /*
             * Using configuration for this test, as many Parcelable types don't
             * implement equals()
             */
            final Bundle bundle1 = new Bundle();
            final Configuration one = new Configuration();
            one.setToDefaults();
            bundle1.putParcelable("com.test.extra", one); //$NON-NLS-1$
            final Bundle bundle2 = new Bundle();
            final Configuration two = new Configuration();
            two.setToDefaults();
            bundle2.putParcelable("com.test.extra", two); //$NON-NLS-1$

            assertTrue(BundleComparer.areBundlesEqual(bundle1, bundle2));
            assertTrue(BundleComparer.areBundlesEqual(bundle2, bundle1));
        }

        /*
         * Test unequal values
         */
        {
            /*
             * The Location class doesn't implement equals, so these will never
             * be equal even if the name was the same
             */

            final Bundle bundle1 = new Bundle();
            bundle1.putParcelable("com.test.extra",
                    new Location("Foo")); //$NON-NLS-1$ //$NON-NLS-2$
            final Bundle bundle2 = new Bundle();
            bundle2.putParcelable("com.test.extra",
                    new Location("Bar")); //$NON-NLS-1$ //$NON-NLS-2$

            assertFalse(BundleComparer.areBundlesEqual(bundle1, bundle2));
            assertFalse(BundleComparer.areBundlesEqual(bundle2, bundle1));

        }

        /*
         * Test unequal keys and values
         */
        {
            final Bundle bundle1 = new Bundle();
            bundle1.putParcelable("com.test.extra1",
                    new Location("Foo")); //$NON-NLS-1$ //$NON-NLS-2$
            final Bundle bundle2 = new Bundle();
            bundle2.putParcelable("com.test.extra2",
                    new Location("Bar")); //$NON-NLS-1$ //$NON-NLS-2$

            assertFalse(BundleComparer.areBundlesEqual(bundle1, bundle2));
            assertFalse(BundleComparer.areBundlesEqual(bundle1, bundle2));

        }

        /*
         * Test unequal keys
         */
        {
            final Bundle bundle1 = new Bundle();
            final Configuration one = new Configuration();
            one.setToDefaults();
            bundle1.putParcelable("com.test.extra1", one); //$NON-NLS-1$
            final Bundle bundle2 = new Bundle();
            final Configuration two = new Configuration();
            two.setToDefaults();
            bundle2.putParcelable("com.test.extra2", two); //$NON-NLS-1$

            assertFalse(BundleComparer.areBundlesEqual(bundle1, bundle2));
            assertFalse(BundleComparer.areBundlesEqual(bundle1, bundle2));
        }
    }

    /**
     * Tests comparing short types
     */
    @SmallTest
    public void testShort() {
        /*
         * Test equal keys and values
         */
        {
            final Bundle bundle1 = new Bundle();
            bundle1.putShort("com.test.extra", Short.MAX_VALUE); //$NON-NLS-1$
            final Bundle bundle2 = new Bundle();
            bundle2.putShort("com.test.extra", Short.MAX_VALUE); //$NON-NLS-1$

            assertTrue(BundleComparer.areBundlesEqual(bundle1, bundle2));
            assertTrue(BundleComparer.areBundlesEqual(bundle2, bundle1));

        }

        /*
         * Test unequal values
         */
        {
            final Bundle bundle1 = new Bundle();
            bundle1.putShort("com.test.extra", Short.MAX_VALUE); //$NON-NLS-1$
            final Bundle bundle2 = new Bundle();
            bundle2.putShort("com.test.extra", Short.MIN_VALUE); //$NON-NLS-1$

            assertFalse(BundleComparer.areBundlesEqual(bundle1, bundle2));
            assertFalse(BundleComparer.areBundlesEqual(bundle2, bundle1));

        }

        /*
         * Test unequal keys and values
         */
        {
            final Bundle bundle1 = new Bundle();
            bundle1.putShort("com.test.extra1", Short.MAX_VALUE); //$NON-NLS-1$
            final Bundle bundle2 = new Bundle();
            bundle2.putShort("com.test.extra2", Short.MIN_VALUE); //$NON-NLS-1$

            assertFalse(BundleComparer.areBundlesEqual(bundle1, bundle2));
            assertFalse(BundleComparer.areBundlesEqual(bundle1, bundle2));

        }

        /*
         * Test unequal keys
         */
        {
            final Bundle bundle1 = new Bundle();
            bundle1.putShort("com.test.extra1", Short.MAX_VALUE); //$NON-NLS-1$
            final Bundle bundle2 = new Bundle();
            bundle2.putShort("com.test.extra2", Short.MAX_VALUE); //$NON-NLS-1$

            assertFalse(BundleComparer.areBundlesEqual(bundle1, bundle2));
            assertFalse(BundleComparer.areBundlesEqual(bundle1, bundle2));

        }
    }

    /**
     * Tests comparing short[] types
     */
    @SuppressWarnings("boxing")
    @SmallTest
    public void testShortArray() {
        /*
         * Test equal empty arrays
         */
        {
            final Bundle bundle1 = new Bundle();
            bundle1.putShortArray("com.test.extra_array", new short[]{}); //$NON-NLS-1$
            final Bundle bundle2 = new Bundle();
            bundle2.putShortArray("com.test.extra_array", new short[]{}); //$NON-NLS-1$

            assertTrue(BundleComparer.areBundlesEqual(bundle1, bundle2));
            assertTrue(BundleComparer.areBundlesEqual(bundle2, bundle1));
        }

        /*
         * Test equal non-empty arrays
         */
        {
            final Bundle bundle1 = new Bundle();
            bundle1.putShortArray("com.test.extra_array",
                    new short[]{Short.MAX_VALUE}); //$NON-NLS-1$
            final Bundle bundle2 = new Bundle();
            bundle2.putShortArray("com.test.extra_array",
                    new short[]{Short.MAX_VALUE}); //$NON-NLS-1$

            assertTrue(BundleComparer.areBundlesEqual(bundle1, bundle2));
            assertTrue(BundleComparer.areBundlesEqual(bundle2, bundle1));
        }

        /*
         * Test unequal keys
         */
        {
            final Bundle bundle1 = new Bundle();
            bundle1.putShortArray("com.test.extra_array1",
                    new short[]{Short.MAX_VALUE}); //$NON-NLS-1$
            final Bundle bundle2 = new Bundle();
            bundle2.putShortArray("com.test.extra_array2",
                    new short[]{Short.MAX_VALUE}); //$NON-NLS-1$

            assertFalse(BundleComparer.areBundlesEqual(bundle1, bundle2));
            assertFalse(BundleComparer.areBundlesEqual(bundle2, bundle1));
        }

        /*
         * Test unequal values
         */
        {
            final Bundle bundle1 = new Bundle();
            bundle1.putShortArray("com.test.extra_array",
                    new short[]{Short.MAX_VALUE}); //$NON-NLS-1$
            final Bundle bundle2 = new Bundle();
            bundle2.putShortArray("com.test.extra_array",
                    new short[]{Short.MIN_VALUE}); //$NON-NLS-1$

            assertFalse(BundleComparer.areBundlesEqual(bundle1, bundle2));
            assertFalse(BundleComparer.areBundlesEqual(bundle2, bundle1));

        }

        /*
         * Test mixing primitive and object
         */
        {
            final Bundle bundle1 = new Bundle();
            bundle1.putSerializable("com.test.extra_array",
                    new Short[]{Short.MAX_VALUE}); //$NON-NLS-1$
            final Bundle bundle2 = new Bundle();
            bundle2.putShortArray("com.test.extra_array",
                    new short[]{Short.MAX_VALUE}); //$NON-NLS-1$

            assertFalse(BundleComparer.areBundlesEqual(bundle1, bundle2));
            assertFalse(BundleComparer.areBundlesEqual(bundle2, bundle1));

        }
    }

    /**
     * Tests comparing Serializable types
     */
    @SmallTest
    public void testSerializable() {
        /*
         * Test equal keys and values
         */
        {
            final Bundle bundle1 = new Bundle();
            bundle1.putSerializable("com.test.extra", new LinkedList<Integer>()); //$NON-NLS-1$
            final Bundle bundle2 = new Bundle();
            bundle2.putSerializable("com.test.extra", new LinkedList<Integer>()); //$NON-NLS-1$

            assertTrue(BundleComparer.areBundlesEqual(bundle1, bundle2));
            assertTrue(BundleComparer.areBundlesEqual(bundle2, bundle1));

        }

        /*
         * Test unequal values
         */
        {
            final Bundle bundle1 = new Bundle();
            final LinkedList<Integer> one = new LinkedList<Integer>();
            one.add(Integer.valueOf(1));
            bundle1.putSerializable("com.test.extra", one); //$NON-NLS-1$
            final Bundle bundle2 = new Bundle();
            final LinkedList<String> two = new LinkedList<String>();
            two.add("nard"); //$NON-NLS-1$
            bundle2.putSerializable("com.test.extra", two); //$NON-NLS-1$

            assertFalse(BundleComparer.areBundlesEqual(bundle1, bundle2));
            assertFalse(BundleComparer.areBundlesEqual(bundle2, bundle1));

        }

        /*
         * Test unequal keys and values
         */
        {
            final Bundle bundle1 = new Bundle();
            bundle1.putSerializable("com.test.extra1", new LinkedList<Integer>()); //$NON-NLS-1$
            final Bundle bundle2 = new Bundle();
            bundle2.putSerializable("com.test.extra2", new LinkedList<String>()); //$NON-NLS-1$

            assertFalse(BundleComparer.areBundlesEqual(bundle1, bundle2));
            assertFalse(BundleComparer.areBundlesEqual(bundle1, bundle2));
        }

        /*
         * Test unequal keys
         */
        {
            final Bundle bundle1 = new Bundle();
            bundle1.putSerializable("com.test.extra1", new LinkedList<Integer>()); //$NON-NLS-1$
            final Bundle bundle2 = new Bundle();
            bundle2.putSerializable("com.test.extra2", new LinkedList<Integer>()); //$NON-NLS-1$

            assertFalse(BundleComparer.areBundlesEqual(bundle1, bundle2));
            assertFalse(BundleComparer.areBundlesEqual(bundle1, bundle2));

        }
    }

    // /**
    // * Tests comparing SparseParcelable types
    // */
    // @SmallTest
    // public void testSparseParcelable()
    // {
    // /*
    // * Test equal keys and values
    // */
    // {
    // final Bundle bundle1 = new Bundle();
    //            bundle1.putSparseParcelableArray("com.test.extra", new SparseArray<Location>()); //$NON-NLS-1$
    // final Bundle bundle2 = new Bundle();
    //            bundle2.putSparseParcelableArray("com.test.extra", new SparseArray<Location>()); //$NON-NLS-1$
    //
    // assertTrue(BundleComparer.areBundlesEqual(bundle1, bundle2));
    // assertTrue(BundleComparer.areBundlesEqual(bundle2, bundle1));
    // }
    //
    // /*
    // * Test unequal values
    // */
    // {
    // final Bundle bundle1 = new Bundle();
    //            bundle1.putSparseParcelableArray("com.test.extra", new SparseArray<Location>()); //$NON-NLS-1$
    // final Bundle bundle2 = new Bundle();
    //            bundle2.putSparseParcelableArray("com.test.extra", new SparseArray<ActivityInfo>()); //$NON-NLS-1$
    //
    // assertFalse(BundleComparer.areBundlesEqual(bundle1, bundle2));
    // assertFalse(BundleComparer.areBundlesEqual(bundle2, bundle1));
    // }
    //
    // /*
    // * Test unequal keys and values
    // */
    // {
    // final Bundle bundle1 = new Bundle();
    //            bundle1.putSparseParcelableArray("com.test.extra1", new SparseArray<Location>()); //$NON-NLS-1$
    // final Bundle bundle2 = new Bundle();
    //            bundle2.putSparseParcelableArray("com.test.extra2", new SparseArray<ActivityInfo>()); //$NON-NLS-1$
    //
    // assertFalse(BundleComparer.areBundlesEqual(bundle1, bundle2));
    // assertFalse(BundleComparer.areBundlesEqual(bundle1, bundle2));
    // }
    //
    // /*
    // * Test unequal keys
    // */
    // {
    // final Bundle bundle1 = new Bundle();
    //            bundle1.putSparseParcelableArray("com.test.extra1", new SparseArray<Location>()); //$NON-NLS-1$
    // final Bundle bundle2 = new Bundle();
    //            bundle2.putSparseParcelableArray("com.test.extra2", new SparseArray<Location>()); //$NON-NLS-1$
    //
    // assertFalse(BundleComparer.areBundlesEqual(bundle1, bundle2));
    // assertFalse(BundleComparer.areBundlesEqual(bundle1, bundle2));
    // }
    // }

    /**
     * Tests comparing float types
     */
    @SmallTest
    public void testString() {
        /*
         * Test equal keys and values
         */
        {
            final Bundle bundle1 = new Bundle();
            bundle1.putString("com.test.extra", "foo"); //$NON-NLS-1$ //$NON-NLS-2$
            final Bundle bundle2 = new Bundle();
            bundle2.putString("com.test.extra", "foo"); //$NON-NLS-1$ //$NON-NLS-2$

            assertTrue(BundleComparer.areBundlesEqual(bundle1, bundle2));
            assertTrue(BundleComparer.areBundlesEqual(bundle2, bundle1));
        }

        /*
         * Test equal keys and values, using non-interned objects
         */
        {
            final Bundle bundle1 = new Bundle();
            bundle1.putString("com.test.extra", new String("foo")); //$NON-NLS-1$ //$NON-NLS-2$
            final Bundle bundle2 = new Bundle();
            bundle2.putString("com.test.extra", new String("foo")); //$NON-NLS-1$ //$NON-NLS-2$

            assertTrue(BundleComparer.areBundlesEqual(bundle1, bundle2));
            assertTrue(BundleComparer.areBundlesEqual(bundle2, bundle1));
        }

        /*
         * Test unequal values
         */
        {
            final Bundle bundle1 = new Bundle();
            bundle1.putString("com.test.extra", "foo"); //$NON-NLS-1$ //$NON-NLS-2$
            final Bundle bundle2 = new Bundle();
            bundle2.putString("com.test.extra", "bar"); //$NON-NLS-1$ //$NON-NLS-2$

            assertFalse(BundleComparer.areBundlesEqual(bundle1, bundle2));
            assertFalse(BundleComparer.areBundlesEqual(bundle2, bundle1));
        }

        /*
         * Test unequal keys and values
         */
        {
            final Bundle bundle1 = new Bundle();
            bundle1.putString("com.test.extra1", "foo"); //$NON-NLS-1$ //$NON-NLS-2$
            final Bundle bundle2 = new Bundle();
            bundle2.putString("com.test.extra2", "bar"); //$NON-NLS-1$ //$NON-NLS-2$

            assertFalse(BundleComparer.areBundlesEqual(bundle1, bundle2));
            assertFalse(BundleComparer.areBundlesEqual(bundle1, bundle2));
        }

        /*
         * Test unequal keys
         */
        {
            final Bundle bundle1 = new Bundle();
            bundle1.putString("com.test.extra1", "foo"); //$NON-NLS-1$ //$NON-NLS-2$
            final Bundle bundle2 = new Bundle();
            bundle2.putString("com.test.extra2", "foo"); //$NON-NLS-1$ //$NON-NLS-2$

            assertFalse(BundleComparer.areBundlesEqual(bundle1, bundle2));
            assertFalse(BundleComparer.areBundlesEqual(bundle1, bundle2));
        }
    }

    /**
     * Tests comparing String[] types
     */
    @SmallTest
    public void testStringArray() {
        /*
         * Test equal empty arrays
         */
        {
            final Bundle bundle1 = new Bundle();
            bundle1.putStringArray("com.test.extra_array", new String[]{}); //$NON-NLS-1$
            final Bundle bundle2 = new Bundle();
            bundle2.putStringArray("com.test.extra_array", new String[]{}); //$NON-NLS-1$

            assertTrue(BundleComparer.areBundlesEqual(bundle1, bundle2));
            assertTrue(BundleComparer.areBundlesEqual(bundle2, bundle1));
        }

        /*
         * Test equal non-empty arrays
         */
        {
            final Bundle bundle1 = new Bundle();
            bundle1.putStringArray("com.test.extra_array",
                    new String[]{"foo"}); //$NON-NLS-1$ //$NON-NLS-2$
            final Bundle bundle2 = new Bundle();
            bundle2.putStringArray("com.test.extra_array",
                    new String[]{"foo"}); //$NON-NLS-1$ //$NON-NLS-2$

            assertTrue(BundleComparer.areBundlesEqual(bundle1, bundle2));
            assertTrue(BundleComparer.areBundlesEqual(bundle2, bundle1));
        }

        /*
         * Test equal non-empty arrays, with non-interned strings
         */
        {
            final Bundle bundle1 = new Bundle();
            bundle1.putStringArray("com.test.extra_array",
                    new String[]{new String("foo")}); //$NON-NLS-1$ //$NON-NLS-2$
            final Bundle bundle2 = new Bundle();
            bundle2.putStringArray("com.test.extra_array",
                    new String[]{new String("foo")}); //$NON-NLS-1$ //$NON-NLS-2$

            assertTrue(BundleComparer.areBundlesEqual(bundle1, bundle2));
            assertTrue(BundleComparer.areBundlesEqual(bundle2, bundle1));
        }

        /*
         * Test unequal keys
         */
        {
            final Bundle bundle1 = new Bundle();
            bundle1.putStringArray("com.test.extra_array1",
                    new String[]{"foo"}); //$NON-NLS-1$ //$NON-NLS-2$
            final Bundle bundle2 = new Bundle();
            bundle2.putStringArray("com.test.extra_array2",
                    new String[]{"foo"}); //$NON-NLS-1$ //$NON-NLS-2$

            assertFalse(BundleComparer.areBundlesEqual(bundle1, bundle2));
            assertFalse(BundleComparer.areBundlesEqual(bundle2, bundle1));
        }

        /*
         * Test unequal values
         */
        {
            final Bundle bundle1 = new Bundle();
            bundle1.putStringArray("com.test.extra_array",
                    new String[]{"foo"}); //$NON-NLS-1$ //$NON-NLS-2$
            final Bundle bundle2 = new Bundle();
            bundle2.putStringArray("com.test.extra_array",
                    new String[]{"bar"}); //$NON-NLS-1$ //$NON-NLS-2$

            assertFalse(BundleComparer.areBundlesEqual(bundle1, bundle2));
            assertFalse(BundleComparer.areBundlesEqual(bundle2, bundle1));

        }
    }

    /**
     * Tests comparing ArrayList<Integer> types
     */
    @SmallTest
    public void testStringArrayList() {
        /*
         * Test equal empty arrays
         */
        {
            final Bundle bundle1 = new Bundle();
            bundle1.putStringArrayList("com.test.extra_array",
                    new ArrayList<String>()); //$NON-NLS-1$
            final Bundle bundle2 = new Bundle();
            bundle2.putStringArrayList("com.test.extra_array",
                    new ArrayList<String>()); //$NON-NLS-1$

            assertTrue(BundleComparer.areBundlesEqual(bundle1, bundle2));
            assertTrue(BundleComparer.areBundlesEqual(bundle2, bundle1));
        }

        /*
         * Test equal non-empty arrays
         */
        {
            final Bundle bundle1 = new Bundle();
            final ArrayList<String> list1 = new ArrayList<String>();
            list1.add("foo"); //$NON-NLS-1$
            bundle1.putStringArrayList("com.test.extra_array1", list1); //$NON-NLS-1$

            final Bundle bundle2 = new Bundle();
            final ArrayList<String> list2 = new ArrayList<String>();
            list2.add("foo"); //$NON-NLS-1$
            bundle2.putStringArrayList("com.test.extra_array1", list2); //$NON-NLS-1$

            assertTrue(BundleComparer.areBundlesEqual(bundle1, bundle2));
            assertTrue(BundleComparer.areBundlesEqual(bundle2, bundle1));
        }

        /*
         * Test equal non-empty arrays with non-interned strings
         */
        {
            final Bundle bundle1 = new Bundle();
            final ArrayList<String> list1 = new ArrayList<String>();
            list1.add(new String("foo")); //$NON-NLS-1$
            bundle1.putStringArrayList("com.test.extra_array1", list1); //$NON-NLS-1$

            final Bundle bundle2 = new Bundle();
            final ArrayList<String> list2 = new ArrayList<String>();
            list2.add(new String("foo")); //$NON-NLS-1$
            bundle2.putStringArrayList("com.test.extra_array1", list2); //$NON-NLS-1$

            assertTrue(BundleComparer.areBundlesEqual(bundle1, bundle2));
            assertTrue(BundleComparer.areBundlesEqual(bundle2, bundle1));
        }

        /*
         * Test unequal keys
         */
        {
            final Bundle bundle1 = new Bundle();
            final ArrayList<String> list1 = new ArrayList<String>();
            list1.add("foo"); //$NON-NLS-1$
            bundle1.putStringArrayList("com.test.extra_array1", list1); //$NON-NLS-1$

            final Bundle bundle2 = new Bundle();
            final ArrayList<String> list2 = new ArrayList<String>();
            list2.add("foo"); //$NON-NLS-1$
            bundle2.putStringArrayList("com.test.extra_array2", list2); //$NON-NLS-1$

            assertFalse(BundleComparer.areBundlesEqual(bundle1, bundle2));
            assertFalse(BundleComparer.areBundlesEqual(bundle2, bundle1));
        }

        /*
         * Test unequal values
         */
        {
            final Bundle bundle1 = new Bundle();
            final ArrayList<String> list1 = new ArrayList<String>();
            list1.add("foo"); //$NON-NLS-1$
            bundle1.putStringArrayList("com.test.extra_array", list1); //$NON-NLS-1$

            final Bundle bundle2 = new Bundle();
            final ArrayList<String> list2 = new ArrayList<String>();
            list2.add("bar"); //$NON-NLS-1$
            bundle2.putStringArrayList("com.test.extra_array", list2); //$NON-NLS-1$

            assertFalse(BundleComparer.areBundlesEqual(bundle1, bundle2));
            assertFalse(BundleComparer.areBundlesEqual(bundle2, bundle1));
        }
    }
}
