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

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.twofortyfouram.assertion.BundleAssertions;
import com.twofortyfouram.log.Lumberjack;
import com.twofortyfouram.spackle.AppBuildInfo;

import net.jcip.annotations.ThreadSafe;

import static com.twofortyfouram.assertion.Assertions.assertNotEmpty;
import static com.twofortyfouram.assertion.Assertions.assertNotNull;

/**
 * Class for managing the {@link com.twofortyfouram.locale.api.Intent#EXTRA_BUNDLE} for this
 * plug-in.
 */
@ThreadSafe
public final class PluginBundleValues {

    /**
     * TYPE: {@code String}
     * <p>
     * An extra that maps to an arbitrary String value.
     */
    @NonNull
    public static final String BUNDLE_EXTRA_STRING_KEY
            = "com.twofortyfouram.locale.sdk.client.test.condition.extra.STRING_VALUE";
    //$NON-NLS-1$

    /**
     * Type: {@code int}.
     * <p>
     * versionCode of the plug-in that saved the Bundle.
     */
    /*
     * This extra is not strictly required, however it makes backward and forward compatibility
     * significantly easier. For example, suppose a bug is found in how some version of the plug-in
     * stored its Bundle. By having the version, the plug-in can better detect when such bugs occur.
     */
    @NonNull
    public static final String BUNDLE_EXTRA_INT_VERSION_CODE
            = "com.twofortyfouram.locale.sdk.client.test.condition.extra.INT_VERSION_CODE";
    //$NON-NLS-1$

    /**
     * Method to verify the content of the bundle are correct.
     * <p>
     * This method will not mutate {@code bundle}.
     *
     * @param bundle bundle to verify. May be null, which will always return false.
     * @return true if the Bundle is valid, false if the bundle is invalid.
     */
    public static boolean isBundleValid(@Nullable final Bundle bundle) {
        if (null == bundle) {
            return false;
        }

        try {
            BundleAssertions.assertHasString(bundle, BUNDLE_EXTRA_STRING_KEY, false, false);
            BundleAssertions.assertHasInt(bundle, BUNDLE_EXTRA_INT_VERSION_CODE);
            BundleAssertions.assertKeyCount(bundle, 2);
        } catch (final AssertionError e) {
            Lumberjack.e("Bundle failed verification%s", e); //$NON-NLS-1$
            return false;
        }

        return true;
    }

    /**
     * @param context Application context.
     * @param value   The value stored in the plug-in bundle.
     * @return A plug-in bundle.
     */
    @NonNull
    public static Bundle generateBundle(@NonNull final Context context,
            @NonNull final String value) {
        assertNotNull(context, "context"); //$NON-NLS-1$
        assertNotEmpty(value, "value"); //$NON-NLS-1$

        final Bundle result = new Bundle();
        result.putInt(BUNDLE_EXTRA_INT_VERSION_CODE, AppBuildInfo.getVersionCode(context));
        result.putString(BUNDLE_EXTRA_STRING_KEY, value);

        return result;
    }

    /**
     * @param bundle A valid plug-in bundle.
     * @return The value inside the plug-in bundle.
     */
    @NonNull
    public static String getValue(@NonNull final Bundle bundle) {
        return bundle.getString(BUNDLE_EXTRA_STRING_KEY);
    }

    /**
     * Private constructor prevents instantiation
     *
     * @throws UnsupportedOperationException because this class cannot be instantiated.
     */
    private PluginBundleValues() {
        throw new UnsupportedOperationException("This class is non-instantiable"); //$NON-NLS-1$
    }
}
