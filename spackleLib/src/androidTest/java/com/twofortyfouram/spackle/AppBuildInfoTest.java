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

import android.content.pm.PackageManager.NameNotFoundException;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;

import com.twofortyfouram.test.assertion.MoarAsserts;

/**
 * Tests {@link AppBuildInfo}.
 */
public final class AppBuildInfoTest extends AndroidTestCase {

    @SmallTest
    public static void testNonInstantiable() {
        MoarAsserts.assertNoninstantiable(AppBuildInfo.class);
    }

    @SmallTest
    public void testGetVersionName() throws NameNotFoundException {
        final String expected = ""; //$NON-NLS-1$
        final String actual = AppBuildInfo.getVersionName(getContext());

        assertEquals(expected, actual);
    }

    @SmallTest
    public void testVersionCode() throws NameNotFoundException {
        final int expected = 0;
        final int actual = AppBuildInfo.getVersionCode(getContext());
        assertEquals(expected, actual);
    }

    @SmallTest
    public void testGetApplicationName() {
        final String expected = "com.twofortyfouram.spackle.test"; //$NON-NLS-1$
        final String actual = AppBuildInfo.getApplicationName(getContext());

        assertEquals(expected, actual);
    }
}
