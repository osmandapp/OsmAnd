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

package com.twofortyfouram.locale.sdk.client.receiver;


import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;

import com.twofortyfouram.log.Lumberjack;
import com.twofortyfouram.spackle.AndroidSdkVersion;
import com.twofortyfouram.spackle.bundle.BundleScrubber;

/**
 * <p>Abstract superclass for a plug-in setting BroadcastReceiver implementation.</p>
 * <p>The plug-in receiver lifecycle is as follows:</p>
 * <ol>
 * <li>{@link #onReceive(android.content.Context, android.content.Intent)} is called by the Android
 * frameworks.
 * onReceive() will verify that the Intent is valid.  If the Intent is invalid, the receiver
 * returns
 * immediately.  If the Intent appears to be valid, then the lifecycle continues.</li>
 * <li>{@link #isBundleValid(android.os.Bundle)} is called to determine whether {@link
 * com.twofortyfouram.locale.api.Intent#EXTRA_BUNDLE EXTRA_BUNDLE} is valid. If the Bundle is
 * invalid, then the
 * receiver returns immediately.  If the bundle is valid, then the lifecycle continues.</li>
 * <li>{@link #isAsync()} is called to determine whether the remaining work should be performed on
 * a
 * background thread.</li>
 * <li>{@link #firePluginSetting(android.content.Context, android.os.Bundle)} is called to trigger
 * the plug-in setting's action.</li>
 * </ol>
 * <p>
 * Implementations of this BroadcastReceiver must be registered in the Android
 * Manifest with an Intent filter for
 * {@link com.twofortyfouram.locale.api.Intent#ACTION_FIRE_SETTING ACTION_FIRE_SETTING}. The
 * BroadcastReceiver must be exported, enabled, and cannot have permissions
 * enforced on it.
 * </p>
 */
public abstract class AbstractPluginSettingReceiver extends AbstractAsyncReceiver {

    /*
     * The multiple return statements in this method are a little gross, but the
     * alternative of nested if statements is even worse :/
     */
    @Override
    public final void onReceive(final Context context, final Intent intent) {
        if (BundleScrubber.scrub(intent)) {
            return;
        }
        Lumberjack.v("Received %s", intent); //$NON-NLS-1$

        /*
         * Note: It is OK if a host sends an ordered broadcast for plug-in
         * settings. Such a behavior would allow the host to optionally block until the
         * plug-in setting finishes.
         */

        if (!com.twofortyfouram.locale.api.Intent.ACTION_FIRE_SETTING.equals(intent.getAction())) {
            Lumberjack
                    .e("Intent action is not %s",
                            com.twofortyfouram.locale.api.Intent.ACTION_FIRE_SETTING); //$NON-NLS-1$
            return;
        }

        /*
         * Ignore implicit intents, because they are not valid. It would be
         * meaningless if ALL plug-in setting BroadcastReceivers installed were
         * asked to handle queries not intended for them. Ideally this
         * implementation here would also explicitly assert the class name as
         * well, but then the unit tests would have trouble. In the end,
         * asserting the package is probably good enough.
         */
        if (!context.getPackageName().equals(intent.getPackage())
                && !new ComponentName(context, this.getClass().getName()).equals(intent
                .getComponent())) {
            Lumberjack.e("Intent is not explicit"); //$NON-NLS-1$
            return;
        }

        final Bundle bundle = intent
                .getBundleExtra(com.twofortyfouram.locale.api.Intent.EXTRA_BUNDLE);
        if (BundleScrubber.scrub(intent)) {
            return;
        }

        if (null == bundle) {
            Lumberjack.e("%s is missing",
                    com.twofortyfouram.locale.api.Intent.EXTRA_BUNDLE); //$NON-NLS-1$
            return;
        }

        if (!isBundleValid(bundle)) {
            Lumberjack.e("%s is invalid",
                    com.twofortyfouram.locale.api.Intent.EXTRA_BUNDLE); //$NON-NLS-1$
            return;
        }

        if (isAsync() && AndroidSdkVersion.isAtLeastSdk(Build.VERSION_CODES.HONEYCOMB)) {
            final AsyncCallback callback = new AsyncCallback() {

                @NonNull
                private final Context mContext = context;

                @NonNull
                private final Bundle mBundle = bundle;

                @Override
                public int runAsync() {
                    firePluginSetting(mContext, mBundle);
                    return Activity.RESULT_OK;
                }

            };

            goAsyncWithCallback(callback);
        } else {
            firePluginSetting(context, bundle);
        }
    }

    /**
     * <p>Gives the plug-in receiver an opportunity to validate the Bundle, to
     * ensure that a malicious application isn't attempting to pass
     * an invalid Bundle.</p>
     * <p>
     * This method will be called on the BroadcastReceiver's Looper (normatively the main thread)
     * </p>
     *
     * @param bundle The plug-in's Bundle previously returned by the edit
     *               Activity.  {@code bundle} should not be mutated by this method.
     * @return true if {@code bundle} appears to be valid.  false if {@code bundle} appears to be
     * invalid.
     */
    protected abstract boolean isBundleValid(@NonNull final Bundle bundle);

    /**
     * Configures the receiver whether it should process the Intent in a
     * background thread. Plug-ins should return true if their
     * {@link #firePluginSetting(android.content.Context, android.os.Bundle)} method performs any
     * sort of disk IO (ContentProvider query, reading SharedPreferences, etc.).
     * or other work that may be slow.
     * <p>
     * Asynchronous BroadcastReceivers are not supported prior to Honeycomb, so
     * with older platforms broadcasts will always be processed on the BroadcastReceiver's Looper
     * (which for Manifest registered receivers will be the main thread).
     *
     * @return True if the receiver should process the Intent in a background
     * thread. False if the plug-in should process the Intent on the
     * BroadcastReceiver's Looper (normatively the main thread).
     */
    protected abstract boolean isAsync();

    /**
     * If {@link #isAsync()} returns true, this method will be called on a
     * background thread. If {@link #isAsync()} returns false, this method will
     * be called on the main thread. Regardless of which thread this method is
     * called on, this method MUST return within 10 seconds per the requirements
     * for BroadcastReceivers.
     *
     * @param context BroadcastReceiver context.
     * @param bundle  The plug-in's Bundle previously returned by the edit
     *                Activity.
     */
    protected abstract void firePluginSetting(@NonNull final Context context,
            @NonNull final Bundle bundle);
}
