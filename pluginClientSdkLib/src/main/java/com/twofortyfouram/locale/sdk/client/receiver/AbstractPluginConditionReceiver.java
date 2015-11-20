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

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;

import com.twofortyfouram.log.Lumberjack;
import com.twofortyfouram.spackle.AndroidSdkVersion;
import com.twofortyfouram.spackle.bundle.BundleScrubber;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * <p>Abstract superclass for a plug-in condition BroadcastReceiver implementation.</p>
 * <p>The plug-in receiver lifecycle is as follows:</p>
 * <ol>
 * <li>{@link #onReceive(android.content.Context, android.content.Intent)} is called by the Android
 * frameworks.
 * onReceive() will verify that the Intent is valid.  If the Intent is invalid, then the receiver
 * sets the result to {@link com.twofortyfouram.locale.api.Intent#RESULT_CONDITION_UNKNOWN
 * RESULT_CONDITION_UNKNOWN} and
 * returns.  If the Intent appears to be valid, then the lifecycle continues.</li>
 * <li>{@link #isBundleValid(android.os.Bundle)} is called to determine whether {@link
 * com.twofortyfouram.locale.api.Intent#EXTRA_BUNDLE EXTRA_BUNDLE} is valid. If the Bundle is
 * invalid, then the
 * receiver
 * sets the result to {@link com.twofortyfouram.locale.api.Intent#RESULT_CONDITION_UNKNOWN
 * RESULT_CONDITION_UNKNOWN} and
 * returns.  If the bundle is valid, then the lifecycle continues.</li>
 * <li>{@link #isAsync()} is called to determine whether the remaining work should be performed on
 * a
 * background thread.</li>
 * <li>{@link #getPluginConditionResult(android.content.Context, android.os.Bundle)} is called to
 * determine the plug-in's status, and the return value of this method is set as the receiver's
 * result code.</li>
 * </ol>
 * <p>
 * Implementations of this BroadcastReceiver must be registered in the Android
 * Manifest with an Intent filter for
 * {@link com.twofortyfouram.locale.api.Intent#ACTION_QUERY_CONDITION ACTION_QUERY_CONDITION}. The
 * BroadcastReceiver must be exported, enabled, and cannot have permissions
 * enforced on it.
 * </p>
 */
public abstract class AbstractPluginConditionReceiver extends AbstractAsyncReceiver {

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

        if (!isOrderedBroadcast()) {
            Lumberjack.e("Broadcast is not ordered"); //$NON-NLS-1$
            return;
        }

        if (!com.twofortyfouram.locale.api.Intent.ACTION_QUERY_CONDITION.equals(intent
                .getAction())) {
            Lumberjack
                    .e("Intent action is not %s",
                            com.twofortyfouram.locale.api.Intent.ACTION_QUERY_CONDITION); //$NON-NLS-1$
            setResultCode(com.twofortyfouram.locale.api.Intent.RESULT_CONDITION_UNKNOWN);
            return;
        }
        /*
         * Ignore implicit intents, because they are not valid. It would be
         * meaningless if ALL plug-in condition BroadcastReceivers installed
         * were asked to handle queries not intended for them.
         */
        if (!new ComponentName(context, this.getClass().getName()).equals(intent
                .getComponent())) {
            Lumberjack.e("Intent is not explicit"); //$NON-NLS-1$
            setResultCode(com.twofortyfouram.locale.api.Intent.RESULT_CONDITION_UNKNOWN);
            abortBroadcast();
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
            setResultCode(com.twofortyfouram.locale.api.Intent.RESULT_CONDITION_UNKNOWN);
            return;
        }

        if (!isBundleValid(bundle)) {
            Lumberjack.e("%s is invalid",
                    com.twofortyfouram.locale.api.Intent.EXTRA_BUNDLE); //$NON-NLS-1$
            setResultCode(com.twofortyfouram.locale.api.Intent.RESULT_CONDITION_UNKNOWN);
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
                    final int pluginResult = getPluginConditionResult(mContext, mBundle);
                    assertResult(pluginResult);
                    return pluginResult;
                }

            };

            goAsyncWithCallback(callback);
        } else {
            final int pluginState = getPluginConditionResult(context, bundle);
            assertResult(pluginState);
            setResultCode(pluginState);
        }
    }

    /**
     * @param result One of the acceptable plug-in result codes.
     * @throws AssertionError if {@code result} is not one of the three
     *                        acceptable values.
     */
    /* package */ static void assertResult(final int result) {
        if (com.twofortyfouram.locale.api.Intent.RESULT_CONDITION_SATISFIED != result
                && com.twofortyfouram.locale.api.Intent.RESULT_CONDITION_UNSATISFIED != result
                && com.twofortyfouram.locale.api.Intent.RESULT_CONDITION_UNKNOWN != result) {
            throw new AssertionError(
                    Lumberjack
                            .formatMessage(
                                    "result=%d is not one of [%d, %d, %d]", result, //$NON-NLS-1$
                                    com.twofortyfouram.locale.api.Intent.RESULT_CONDITION_SATISFIED,
                                    com.twofortyfouram.locale.api.Intent.RESULT_CONDITION_UNSATISFIED,
                                    com.twofortyfouram.locale.api.Intent.RESULT_CONDITION_UNKNOWN)
            );
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
     * {@link #getPluginConditionResult(Context, Bundle)} method performs any
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
     * Determines the state of the plug-in.
     *
     * If {@link #isAsync()} returns true, this method will be called on a
     * background thread. If {@link #isAsync()} returns false, this method will
     * be called on the main thread. Regardless of which thread this method is
     * called on, this method MUST return within 10 seconds per the requirements
     * for BroadcastReceivers.
     *
     * @param context BroadcastReceiver context.
     * @param bundle The plug-in's Bundle previously returned by the edit
     *               Activity.
     * @return One of the Locale plug-in query results:
     * {@link com.twofortyfouram.locale.api.Intent#RESULT_CONDITION_SATISFIED
     * RESULT_CONDITION_SATISFIED}
     * ,
     * {@link com.twofortyfouram.locale.api.Intent#RESULT_CONDITION_UNSATISFIED
     * RESULT_CONDITION_UNSATISFIED}
     * , or
     * {@link com.twofortyfouram.locale.api.Intent#RESULT_CONDITION_UNKNOWN
     * RESULT_CONDITION_UNKNOWN}
     * . If {@code bundle} is invalid, implementations must return
     * {@link com.twofortyfouram.locale.api.Intent#RESULT_CONDITION_UNKNOWN
     * RESULT_CONDITION_UNKNOWN}
     */
    @ConditionResult
    protected abstract int getPluginConditionResult(@NonNull final Context context,
                                                    @NonNull final Bundle bundle);

    @IntDef({com.twofortyfouram.locale.api.Intent.RESULT_CONDITION_SATISFIED, com.twofortyfouram
            .locale.api.Intent.RESULT_CONDITION_UNKNOWN, com.twofortyfouram.locale.api.Intent
            .RESULT_CONDITION_UNSATISFIED})
    @Retention(RetentionPolicy.SOURCE)
    public @interface ConditionResult {

    }

}
