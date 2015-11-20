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

package com.twofortyfouram.locale.sdk.client.ui.activity;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;

import com.twofortyfouram.locale.sdk.client.internal.HostPackageUtilTest.HostPackageManager;

public final class InfoActivityTest extends AndroidTestCase {

    @SmallTest
    public void testGetLaunchIntent_host() {
        final Intent i = InfoActivity.getLaunchIntent(getContext(), new ExtendedHostPackageManager(
                "com.twofortyfouram.locale"), getContext().getPackageName()); //$NON-NLS-1$

        assertEquals("com.twofortyfouram.locale", i.getPackage()); //$NON-NLS-1$
    }

    @SmallTest
    public void testGetLaunchIntent_google_play() {
        final Intent i = InfoActivity.getLaunchIntent(getContext(), new HostPackageManager(), getContext()
                .getPackageName());

        assertEquals(Intent.ACTION_VIEW, i.getAction());
        assertEquals("market", i.getData().getScheme()); //$NON-NLS-1$
    }

    private static final class ExtendedHostPackageManager extends HostPackageManager {

        public ExtendedHostPackageManager(@NonNull final String... packages) {
            super(packages);
        }

        @Override
        public Intent getLaunchIntentForPackage(String packageName) {
            return new Intent().setPackage(packageName);
        }
    }
}
