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

package com.twofortyfouram.locale.sdk.client.test.condition.receiver;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.twofortyfouram.assertion.Assertions;
import com.twofortyfouram.assertion.BundleAssertions;
import com.twofortyfouram.locale.api.Intent;
import com.twofortyfouram.log.Lumberjack;
import com.twofortyfouram.spackle.AppBuildInfo;

import net.jcip.annotations.ThreadSafe;

/**
 * Class for managing the {@link com.twofortyfouram.locale.api.Intent#EXTRA_BUNDLE} for this
 * plug-in.
 */
@ThreadSafe
public final class PluginBundleValues {

    /**
     * TYPE: {@code int}
     * <p>
     * An extra that contains the result code that the test plug-in condition should return when
     * queried.
     *
     * @see com.twofortyfouram.locale.api.Intent#RESULT_CONDITION_SATISFIED
     * @see com.twofortyfouram.locale.api.Intent#RESULT_CONDITION_UNSATISFIED
     * @see com.twofortyfouram.locale.api.Intent#RESULT_CONDITION_UNKNOWN
     */
    public static final String BUNDLE_EXTRA_INT_RESULT_CODE
            = "com.twofortyfouram.locale.sdk.client.test.condition.extra.INT_CODE"; //$NON-NLS-1$

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
            = "com.twofortyfouram.locale.sdk.client.test.condition.extra.INT_VERSION_CODE"; //$NON-NLS-1$

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
            BundleAssertions.assertHasInt(bundle, BUNDLE_EXTRA_INT_RESULT_CODE,
                    Intent.RESULT_CONDITION_SATISFIED,
                    Intent.RESULT_CONDITION_UNKNOWN);
            BundleAssertions.assertHasInt(bundle, BUNDLE_EXTRA_INT_VERSION_CODE);
            BundleAssertions.assertKeyCount(bundle, 2);
        } catch (final AssertionError e) {
            Lumberjack.e("Bundle failed verification%s", e); //$NON-NLS-1$
            return false;
        }

        return true;
    }

    /**
     * @param context    Application context.
     * @param resultCode The result code the plug-in should respond with when queried.
     * @return A plug-in bundle.
     */
    @NonNull
    public static Bundle generateBundle(@NonNull final Context context,
            @NonNull final int resultCode) {
        Assertions.assertNotNull(context, "context"); //$NON-NLS-1$
        Assertions.assertInRangeInclusive(resultCode,
                Intent.RESULT_CONDITION_SATISFIED,
                Intent.RESULT_CONDITION_UNKNOWN,
                "resultCode"); //$NON-NLS-1$

        final Bundle result = new Bundle();
        result.putInt(BUNDLE_EXTRA_INT_VERSION_CODE, AppBuildInfo.getVersionCode(context));
        result.putInt(BUNDLE_EXTRA_INT_RESULT_CODE, resultCode);

        return result;
    }

    /**
     * @param bundle A valid plug-in bundle.
     * @return The result code inside the plug-in bundle.  Will return 0 if the result code does not
     * exist in {@code bundle}.
     */
    @NonNull
    public static int getResultCode(@NonNull final Bundle bundle) {
        return bundle.getInt(BUNDLE_EXTRA_INT_RESULT_CODE);
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
