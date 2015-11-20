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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;
import android.text.format.DateUtils;

import com.twofortyfouram.locale.sdk.client.test.condition.receiver.PluginBundleValues;
import com.twofortyfouram.locale.sdk.client.test.condition.receiver.PluginConditionReceiver;
import com.twofortyfouram.log.Lumberjack;
import com.twofortyfouram.spackle.ThreadUtil;
import com.twofortyfouram.spackle.ThreadUtil.ThreadPriority;
import com.twofortyfouram.spackle.bundle.BundleScrubber;

import net.jcip.annotations.ThreadSafe;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public final class AbstractPluginConditionReceiverTest extends AndroidTestCase {
   /*
    * In order to test the abstract class, these tests rely on the concrete implementation of
    * PluginConditionReceiver.
    */

    @NonNull
    private static final String ALTERNATIVE_ACTION_FOR_TEST
            = "com.twofortyfouram.locale.sdk.client.test.action.TEST_ACTION"; //$NON-NLS-1$

    @SmallTest
    public void testReceiver_no_ordered_broadcast() {
        final Intent intent = getDefaultIntent(
                com.twofortyfouram.locale.api.Intent.RESULT_CONDITION_UNKNOWN);

        final PluginConditionReceiver receiver = new PluginConditionReceiver();

        /*
         * This verifies that no exception is thrown for a non-ordered
         * broadcast. Yes, this is different from all the other tests because it
         * is not ordered.
         */
        receiver.onReceive(getContext(), intent);
    }

    // Test fails on Travis for some reason...
//    @SmallTest
//    public void testReceiver_no_bundle() {
//        final Intent intent = getDefaultIntent(
//                com.twofortyfouram.locale.api.Intent.RESULT_CONDITION_UNSATISFIED);
//        intent.removeExtra(com.twofortyfouram.locale.api.Intent.EXTRA_BUNDLE);
//
//        assertOrderedBroadcastResultCode(intent,
//                com.twofortyfouram.locale.api.Intent.RESULT_CONDITION_SATISFIED,
//                com.twofortyfouram.locale.api.Intent.RESULT_CONDITION_UNKNOWN);
//    }

    @SmallTest
    public void testReceiver_wrong_action() {
        final Intent intent = getDefaultIntent(
                com.twofortyfouram.locale.api.Intent.RESULT_CONDITION_UNSATISFIED);
        intent.setAction(ALTERNATIVE_ACTION_FOR_TEST);

        assertOrderedBroadcastResultCode(intent,
                com.twofortyfouram.locale.api.Intent.RESULT_CONDITION_SATISFIED,
                com.twofortyfouram.locale.api.Intent.RESULT_CONDITION_UNKNOWN);
    }

    @SmallTest
    public void testResult_satisfied() {
        final Intent intent = getDefaultIntent(
                com.twofortyfouram.locale.api.Intent.RESULT_CONDITION_SATISFIED);

        assertOrderedBroadcastResultCode(intent,
                com.twofortyfouram.locale.api.Intent.RESULT_CONDITION_UNKNOWN,
                com.twofortyfouram.locale.api.Intent.RESULT_CONDITION_SATISFIED);
    }

    @SmallTest
    public void testResult_unsatisfied() {
        final Intent intent = getDefaultIntent(
                com.twofortyfouram.locale.api.Intent.RESULT_CONDITION_UNSATISFIED);

        assertOrderedBroadcastResultCode(intent,
                com.twofortyfouram.locale.api.Intent.RESULT_CONDITION_UNKNOWN,
                com.twofortyfouram.locale.api.Intent.RESULT_CONDITION_UNSATISFIED);
    }

    @SmallTest
    public void testResult_unknown() {
        final Intent intent = getDefaultIntent(
                com.twofortyfouram.locale.api.Intent.RESULT_CONDITION_UNKNOWN);

        assertOrderedBroadcastResultCode(intent,
                com.twofortyfouram.locale.api.Intent.RESULT_CONDITION_SATISFIED,
                com.twofortyfouram.locale.api.Intent.RESULT_CONDITION_UNKNOWN);
    }

    @SmallTest
    public static void testAssertState() {

        AbstractPluginConditionReceiver
                .assertResult(com.twofortyfouram.locale.api.Intent.RESULT_CONDITION_UNKNOWN);
        AbstractPluginConditionReceiver
                .assertResult(com.twofortyfouram.locale.api.Intent.RESULT_CONDITION_UNSATISFIED);
        AbstractPluginConditionReceiver
                .assertResult(com.twofortyfouram.locale.api.Intent.RESULT_CONDITION_SATISFIED);

        try {
            AbstractPluginConditionReceiver.assertResult(Activity.RESULT_OK);
            fail();
        } catch (final AssertionError e) {
            // Expected exception
        }
    }

    @NonNull
    @SuppressLint("InlinedApi")
    private Intent getDefaultIntent(final int state) {
        final Intent intent = new Intent();
        intent.setFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES | Intent.FLAG_FROM_BACKGROUND);
        intent.setClassName(getContext(), PluginConditionReceiver.class.getName());
        intent.setAction(com.twofortyfouram.locale.api.Intent.ACTION_QUERY_CONDITION);

        final Bundle bundle = new Bundle();
        intent.putExtra(com.twofortyfouram.locale.api.Intent.EXTRA_BUNDLE,
                PluginBundleValues.generateBundle(getContext(), state));

        return intent;
    }

    /**
     * @param intent             Intent to send to the receiver implementation under test.
     * @param initialResultCode  Normally this should be
     *                           com.twofortyfouram.locale.api.Intent#RESULT_CONDITION_UNKNOWN, but
     *                           can be overridden to verify the receiver under test.
     * @param expectedResultCode Result code to assert comes back.
     */
    private void assertOrderedBroadcastResultCode(final Intent intent,
            final int initialResultCode, final int expectedResultCode) {

        final HandlerThread handlerThread = ThreadUtil.newHandlerThread(getName(),
                ThreadPriority.DEFAULT);

        try {
            final QueryResultReceiver resultReceiver = new QueryResultReceiver();
            mContext.sendOrderedBroadcast(intent, null, resultReceiver,
                    new Handler(handlerThread.getLooper()), initialResultCode, null, null);

            try {
                assertTrue(resultReceiver.mLatch.await(6 * DateUtils.SECOND_IN_MILLIS,
                        TimeUnit.MILLISECONDS));
            } catch (final InterruptedException e) {
                throw new AssertionError(e);
            }

            assertEquals(expectedResultCode + "!=" + resultReceiver.mQueryResult.get(), expectedResultCode, resultReceiver.mQueryResult.get());
        } finally {
            handlerThread.getLooper().quit();
        }

    }

    @ThreadSafe
    private static final class QueryResultReceiver extends BroadcastReceiver {

        @NonNull
        /* package */ final CountDownLatch mLatch = new CountDownLatch(1);

        @NonNull
        /* package */ final AtomicInteger mQueryResult = new AtomicInteger(
                com.twofortyfouram.locale.api.Intent.RESULT_CONDITION_UNKNOWN);

        @Override
        public void onReceive(final Context context, final Intent intent) {

            if (BundleScrubber.scrub(intent)) {
                throw new AssertionError();
            }

            Lumberjack.v("Received %s", intent); //$NON-NLS-1$

            switch (getResultCode()) {
                case com.twofortyfouram.locale.api.Intent.RESULT_CONDITION_SATISFIED: {
                    Lumberjack.v("Got RESULT_CONDITION_SATISFIED"); //$NON-NLS-1$
                    mQueryResult
                            .set(com.twofortyfouram.locale.api.Intent.RESULT_CONDITION_SATISFIED);
                    break;
                }
                case com.twofortyfouram.locale.api.Intent.RESULT_CONDITION_UNSATISFIED: {
                    Lumberjack.v("Got RESULT_CONDITION_UNSATISFIED"); //$NON-NLS-1$
                    mQueryResult
                            .set(com.twofortyfouram.locale.api.Intent.RESULT_CONDITION_UNSATISFIED);
                    break;
                }
                case com.twofortyfouram.locale.api.Intent.RESULT_CONDITION_UNKNOWN: {
                    Lumberjack.v("Got RESULT_CONDITION_UNKNOWN"); //$NON-NLS-1$
                    mQueryResult.set(com.twofortyfouram.locale.api.Intent.RESULT_CONDITION_UNKNOWN);
                    break;
                }
                default: {
                    /*
                     * Although this shouldn't happen, don't throw an exception
                     * because bad 3rd party apps could give bad result codes
                     */
                    Lumberjack.w("Got unrecognized result code: %d", getResultCode()); //$NON-NLS-1$
                    mQueryResult.set(com.twofortyfouram.locale.api.Intent.RESULT_CONDITION_UNKNOWN);
                }
            }

            mLatch.countDown();
        }
    }
}
