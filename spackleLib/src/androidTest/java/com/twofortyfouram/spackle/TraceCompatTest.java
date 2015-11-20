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

package com.twofortyfouram.spackle;

import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;

import com.twofortyfouram.test.assertion.MoarAsserts;

/**
 * Tests {@link TraceCompat}.
 */
public final class TraceCompatTest extends AndroidTestCase {

    @SmallTest
    public static void testNonInstantiable() {
        MoarAsserts.assertNoninstantiable(TraceCompat.class);
    }

    @SmallTest
    public static void testBeginEndSection() {
        // This doesn't do much but ensure that no exceptions or failures occur when the code is called.

        TraceCompat.beginSection("tag");
        TraceCompat.endSection();
    }
}
