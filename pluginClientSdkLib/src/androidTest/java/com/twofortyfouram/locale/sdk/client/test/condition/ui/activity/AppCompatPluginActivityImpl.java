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

package com.twofortyfouram.locale.sdk.client.test.condition.ui.activity;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.twofortyfouram.locale.sdk.client.ui.activity.AbstractAppCompatPluginActivity;
import com.twofortyfouram.spackle.bundle.BundleComparer;

import java.util.concurrent.atomic.AtomicInteger;

import static com.twofortyfouram.assertion.Assertions.assertNotNull;

/**
 * A concrete implementation of {@link AbstractAppCompatPluginActivity} in order to test the abstract class.
 */
public final class AppCompatPluginActivityImpl extends AbstractAppCompatPluginActivity {

    @NonNull
    public final AtomicInteger mIsBundleValidCount = new AtomicInteger(0);

    @NonNull
    public final AtomicInteger mOnPostCreateWithPreviousBundleCount = new AtomicInteger(0);

    @NonNull
    public final AtomicInteger mGetBlurbCount = new AtomicInteger(0);

    @NonNull
    public final AtomicInteger mGetResultBundleCount = new AtomicInteger(0);

    @Nullable
    public volatile String mBlurb = null;

    @Nullable
    public volatile Bundle mBundle = null;

    @Override
    public boolean isBundleValid(@NonNull final Bundle bundle) {
        mIsBundleValidCount.incrementAndGet();
        return PluginBundleValues.isBundleValid(bundle);
    }

    @Override
    public void onPostCreateWithPreviousResult(@NonNull final Bundle previousBundle,
                                               @NonNull final String previousBlurb) {
        mOnPostCreateWithPreviousBundleCount.incrementAndGet();

        assertNotNull(previousBundle, "previousBundle"); //$NON-NLS-1$
    }

    @Override
    public Bundle getResultBundle() {
        mGetResultBundleCount.incrementAndGet();

        return mBundle;
    }

    @Override
    public String getResultBlurb(@NonNull final Bundle bundle) {
        mGetBlurbCount.incrementAndGet();

        assertNotNull(bundle, "bundle"); //$NON-NLS-1$

        if (!BundleComparer.areBundlesEqual(bundle, mBundle)) {
            throw new AssertionError();
        }

        return mBlurb;
    }

}
