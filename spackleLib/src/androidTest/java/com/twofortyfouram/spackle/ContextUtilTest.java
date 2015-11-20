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

import android.content.Context;
import android.content.ContextWrapper;
import android.test.AndroidTestCase;
import android.test.IsolatedContext;
import android.test.RenamingDelegatingContext;
import android.test.suitebuilder.annotation.SmallTest;

import com.twofortyfouram.test.assertion.MoarAsserts;


public final class ContextUtilTest extends AndroidTestCase {

    @SmallTest
    public static void testNonInstantiable() {
        MoarAsserts.assertNoninstantiable(ContextUtil.class);
    }

    @SmallTest
    public void testCleanContext() {
        final Context cleanableContext = new CleanableContext(getContext());

        assertNotSame(cleanableContext, ContextUtil.cleanContext(cleanableContext));
        assertSame(cleanableContext.getApplicationContext(),
                ContextUtil.cleanContext(cleanableContext));
    }

    @SmallTest
    public static void testCleanContext_null() {
        try {
            ContextUtil.cleanContext(null);
            fail();
        } catch (final AssertionError e) {
            // expected exception
        }
    }

    @SmallTest
    public static void testCleanContext_isolated_context() {
        final Context isolatedContext = new IsolatedContext(null, null);

        assertSame(isolatedContext, ContextUtil.cleanContext(isolatedContext));
    }

    @SmallTest
    public static void testCleanContext_renaming_delegating_context() {
        final Context renamingDelegatingContext = new RenamingDelegatingContext(null, null);

        assertSame(renamingDelegatingContext, ContextUtil.cleanContext(renamingDelegatingContext));
    }

    private static final class CleanableContext extends ContextWrapper {

        public CleanableContext(final Context base) {
            super(base);
        }

    }
}
