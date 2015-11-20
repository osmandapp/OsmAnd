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

import android.test.suitebuilder.annotation.SmallTest;

import com.twofortyfouram.test.assertion.MoarAsserts;

import junit.framework.TestCase;

/**
 * Tests {@link Reflector}.
 */
public final class ReflectorTest extends TestCase {

    @SmallTest
    public static void testNonInstantiable() {
        MoarAsserts.assertNoninstantiable(Reflector.class);
    }

    @SmallTest
    @SuppressWarnings("rawtypes")
    public static void testInvokeStatic_class_object() {
        final String result1 = Reflector.tryInvokeStatic(Boolean.class,
                "toString", new Class[]{Boolean.TYPE}, new Object[]{Boolean.TRUE}); //$NON-NLS-1$
        assertEquals(Boolean.toString(Boolean.TRUE), result1);

        final String result2 = Reflector.tryInvokeStatic(Boolean.class,
                "toString", new Class[]{Boolean.TYPE}, new Object[]{Boolean.FALSE}); //$NON-NLS-1$

        assertEquals(Boolean.toString(Boolean.FALSE), result2);
    }

    @SmallTest
    @SuppressWarnings("rawtypes")
    public static void testInvokeStatic_class_name() {
        final String result1 = Reflector.tryInvokeStatic(Boolean.class.getName(),
                "toString", new Class[]{Boolean.TYPE}, new Object[]{Boolean.TRUE}); //$NON-NLS-1$
        assertEquals(Boolean.toString(Boolean.TRUE), result1);

        final String result2 = Reflector.tryInvokeStatic(Boolean.class.getName(),
                "toString", new Class[]{Boolean.TYPE}, new Object[]{Boolean.FALSE}); //$NON-NLS-1$

        assertEquals(Boolean.toString(Boolean.FALSE), result2);
    }

    @SmallTest
    @SuppressWarnings("rawtypes")
    public static void testInvokeInstance() {
        final String result = Reflector
                .tryInvokeInstance(Boolean.TRUE, "toString", null, null); //$NON-NLS-1$

        assertEquals(Boolean.TRUE.toString(), result);
    }
}
