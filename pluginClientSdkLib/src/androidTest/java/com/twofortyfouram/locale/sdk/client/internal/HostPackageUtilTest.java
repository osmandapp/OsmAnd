/*
 * android-plugin-client-sdk-for-locale https://github.com/twofortyfouram/android-plugin-client-sdk-for-locale
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

package com.twofortyfouram.locale.sdk.client.internal;

import android.content.pm.PackageInfo;
import android.support.annotation.NonNull;
import android.test.AndroidTestCase;
import android.test.mock.MockPackageManager;
import android.test.suitebuilder.annotation.SmallTest;

import com.twofortyfouram.test.assertion.MoarAsserts;

import net.jcip.annotations.ThreadSafe;

import java.util.LinkedList;
import java.util.List;

public final class HostPackageUtilTest extends AndroidTestCase {

    @SmallTest
    public void testNonInstantiable() {
        MoarAsserts.assertNoninstantiable(HostPackageUtil.class);
    }

    @SmallTest
    public void testGetCompatiblePackage_hinted_positive() {
        assertEquals("com.twofortyfouram.locale",
                HostPackageUtil.getCompatiblePackage( //$NON-NLS-1$
                        new HostPackageManager("com.twofortyfouram.locale"),
                        "com.twofortyfouram.locale")); //$NON-NLS-1$//$NON-NLS-2$
    }

    @SmallTest
    public void testGetCompatiblePackage_hinted_negative() {
        assertEquals("com.twofortyfouram.locale",
                HostPackageUtil.getCompatiblePackage( //$NON-NLS-1$
                        new HostPackageManager("com.twofortyfouram.locale"),
                        "com.foo.bar")); //$NON-NLS-1$//$NON-NLS-2$
    }

    @SmallTest
    public void testGetCompatiblePackage_none() {
        assertNull(HostPackageUtil.getCompatiblePackage(new HostPackageManager(), null));
    }

    @ThreadSafe
    public static class HostPackageManager extends MockPackageManager {

        @NonNull
        private final List<PackageInfo> mInstalledPackages = new LinkedList<PackageInfo>();

        public HostPackageManager(@NonNull final String... packages) {
            for (String pkg : packages) {
                final PackageInfo pi = new PackageInfo();
                pi.packageName = pkg;
                mInstalledPackages.add(pi);
            }
        }

        @Override
        public List<PackageInfo> getInstalledPackages(int flags) {
            return mInstalledPackages;
        }
    }
}
