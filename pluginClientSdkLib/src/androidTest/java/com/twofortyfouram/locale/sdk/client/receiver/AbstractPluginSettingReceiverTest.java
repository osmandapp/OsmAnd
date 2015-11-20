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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;
import android.text.format.DateUtils;

import com.twofortyfouram.log.Lumberjack;
import com.twofortyfouram.spackle.ThreadUtil;
import com.twofortyfouram.spackle.ThreadUtil.ThreadPriority;
import com.twofortyfouram.spackle.bundle.BundleComparer;
import com.twofortyfouram.spackle.bundle.BundleScrubber;

import net.jcip.annotations.ThreadSafe;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public final class AbstractPluginSettingReceiverTest extends AndroidTestCase {


    @NonNull
    private static final String ALTERNATIVE_ACTION_FOR_TEST
            = "com.twofortyfouram.locale.sdk.client.test.action.TEST_ACTION"; //$NON-NLS-1$

    @SmallTest
    public void testReceiver_no_bundle() {
        final Intent intent = getDefaultIntent();
        intent.removeExtra(com.twofortyfouram.locale.api.Intent.EXTRA_BUNDLE);

        final PluginSettingReceiverImpl receiver = new PluginSettingReceiverImpl(false);
        receiver.onReceive(getContext(), intent);

        assertNull(receiver.mFireBundle.get());
        assertEquals(0, receiver.mFireCount.get());
    }

    @SmallTest
    public void testReceiver_wrong_action() {
        final Intent intent = getDefaultIntent();
        intent.setAction(ALTERNATIVE_ACTION_FOR_TEST);

        final PluginSettingReceiverImpl receiver = new PluginSettingReceiverImpl(false);
        receiver.onReceive(getContext(), intent);

        assertNull(receiver.mFireBundle.get());
        assertEquals(0, receiver.mFireCount.get());
    }

    @SmallTest
    public void testReceiver_not_explicit() {
        final Intent intent = getDefaultIntent();
        intent.setPackage(null);

        final PluginSettingReceiverImpl receiver = new PluginSettingReceiverImpl(false);
        receiver.onReceive(getContext(), intent);

        assertNull(receiver.mFireBundle.get());
        assertEquals(0, receiver.mFireCount.get());
    }

    @SmallTest
    public void testReceiver_valid() {
        final Intent intent = getDefaultIntent();

        final PluginSettingReceiverImpl receiver = new PluginSettingReceiverImpl(false);
        receiver.onReceive(getContext(), intent);

        assertNotNull(receiver.mFireBundle.get());
        assertEquals(1, receiver.mFireCount.get());
    }

    @SmallTest
    public void testReceiver_valid_async() {
        final Intent intent = getDefaultIntent();

        assertOrderedBroadcast(intent, Activity.RESULT_CANCELED, Activity.RESULT_OK, true, 1,
                intent.getBundleExtra(com.twofortyfouram.locale.api.Intent.EXTRA_BUNDLE));
    }

    @NonNull
    private Intent getDefaultIntent() {
        final Intent intent = new Intent();
        intent.setPackage(getContext().getPackageName());
        intent.setAction(com.twofortyfouram.locale.api.Intent.ACTION_FIRE_SETTING);

        final Bundle bundle = new Bundle();
        intent.putExtra(com.twofortyfouram.locale.api.Intent.EXTRA_BUNDLE, bundle);

        return intent;
    }

    /**
     * @param intent             Intent to send to the receiver implementation under test.
     * @param initialResultCode  Normally this should be
     *                           com.twofortyfouram.locale.api.Intent#RESULT_CONDITION_UNKNOWN, but
     *                           can be overridden to verify the receiver under test.
     * @param expectedResultCode The expected result code.
     * @param isAsync            True if the plug-in receiver should handle its work in a
     *                           background thread.
     */
    private void assertOrderedBroadcast(final Intent intent, final int initialResultCode,
            final int expectedResultCode, final boolean isAsync, final int expectedFireCount,
            final Bundle expectedBundle) {

        final HandlerThread handlerThread = ThreadUtil.newHandlerThread(getName(),
                ThreadPriority.DEFAULT);

        final PluginSettingReceiverImpl receiverImpl = new PluginSettingReceiverImpl(isAsync);

        final IntentFilter filter = new IntentFilter(
                com.twofortyfouram.locale.api.Intent.ACTION_FIRE_SETTING);
        filter.addAction(ALTERNATIVE_ACTION_FOR_TEST);

        mContext.registerReceiver(receiverImpl, filter);
        try {

            final QueryResultReceiver resultReceiver = new QueryResultReceiver();
            mContext.sendOrderedBroadcast(intent, null, resultReceiver,
                    new Handler(handlerThread.getLooper()), initialResultCode, null, null);

            try {
                assertTrue(resultReceiver.mLatch.await(5 * DateUtils.SECOND_IN_MILLIS,
                        TimeUnit.MILLISECONDS));
            } catch (final InterruptedException e) {
                throw new AssertionError(e);
            }

            assertTrue(BundleComparer.areBundlesEqual(expectedBundle,
                    receiverImpl.mFireBundle.get()));
            assertEquals(expectedFireCount, receiverImpl.mFireCount.get());
        } finally {
            handlerThread.getLooper().quit();
            mContext.unregisterReceiver(receiverImpl);
        }

    }

    private static final class PluginSettingReceiverImpl extends AbstractPluginSettingReceiver {

        private final boolean mIsAsync;

        private final AtomicInteger mIsValidCount = new AtomicInteger(0);

        private final AtomicInteger mFireCount = new AtomicInteger(0);

        private final AtomicReference<Bundle> mFireBundle = new AtomicReference<Bundle>(null);

        public PluginSettingReceiverImpl(final boolean isAsync) {
            mIsAsync = isAsync;
        }


        @Override
        protected boolean isBundleValid(@NonNull Bundle bundle) {
            mIsValidCount.incrementAndGet();
            return true;
        }

        @Override
        protected boolean isAsync() {
            return mIsAsync;
        }

        @Override
        protected void firePluginSetting(final Context context, final Bundle bundle) {
            mFireCount.incrementAndGet();
            mFireBundle.set(bundle);
        }
    }

    @ThreadSafe
    private static final class QueryResultReceiver extends BroadcastReceiver {

        @NonNull
        /* package */ final CountDownLatch mLatch = new CountDownLatch(1);

        @NonNull
        /* package */ final AtomicInteger mQueryResult = new AtomicInteger(0);

        @Override
        public void onReceive(final Context context, final Intent intent) {

            if (BundleScrubber.scrub(intent)) {
                throw new AssertionError();
            }

            Lumberjack.v("Received %s", intent); //$NON-NLS-1$

            mQueryResult.set(getResultCode());

            mLatch.countDown();
        }
    }
}
