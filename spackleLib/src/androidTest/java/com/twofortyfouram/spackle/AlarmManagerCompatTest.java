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

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.support.annotation.RequiresPermission;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.MediumTest;
import android.text.format.DateUtils;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public final class AlarmManagerCompatTest extends AndroidTestCase {

    /*
     * Allows for jitter in alarm delivery, especially when running tests on the emulator.
     */
    private static final long ALARM_SLOP_MILLIS = 5 * DateUtils.SECOND_IN_MILLIS;

    /*
     * Alarms less than 5 seconds away use the handler
     */
    private final long SHORT_ALARM_DELAY_MILLIS = Math.round(1 * DateUtils.SECOND_IN_MILLIS);

    /*
     * Alarms more than 5 seconds away use the alarm manager.
     */
    private final long LONG_ALARM_DELAY_MILLIS = Math.round(7 * DateUtils.SECOND_IN_MILLIS);

    @MediumTest
    @RequiresPermission(Manifest.permission.WAKE_LOCK)
    public void testSetExactElapsedWakeup_short() {
        if (!AndroidSdkVersion.isAtLeastSdk(Build.VERSION_CODES.M)) {
            assertAlarmFiresWithin(AlarmManager.ELAPSED_REALTIME_WAKEUP, SHORT_ALARM_DELAY_MILLIS);
        }
    }

    @MediumTest
    @RequiresPermission(Manifest.permission.WAKE_LOCK)
    public void testSetExactElapsedWakeup_long() {
        if (!AndroidSdkVersion.isAtLeastSdk(Build.VERSION_CODES.M)) {
            assertAlarmFiresWithin(AlarmManager.ELAPSED_REALTIME_WAKEUP, LONG_ALARM_DELAY_MILLIS);
        }
    }

    @MediumTest
    @RequiresPermission(Manifest.permission.WAKE_LOCK)
    public void testSetExactRtcWakeup_short() {
        if (!AndroidSdkVersion.isAtLeastSdk(Build.VERSION_CODES.M)) {
            assertAlarmFiresWithin(AlarmManager.RTC_WAKEUP, SHORT_ALARM_DELAY_MILLIS);
        }
    }

    @MediumTest
    @RequiresPermission(Manifest.permission.WAKE_LOCK)
    public void testSetExactRtcWakeup_long() {
        if (!AndroidSdkVersion.isAtLeastSdk(Build.VERSION_CODES.M)) {
            assertAlarmFiresWithin(AlarmManager.RTC_WAKEUP, LONG_ALARM_DELAY_MILLIS);
        }
    }

    @RequiresPermission(Manifest.permission.WAKE_LOCK)
    private void assertAlarmFiresWithin(
            @com.twofortyfouram.spackle.AlarmManagerCompat.AlarmType final int type,
            final long delayMillis) {
        final HandlerThread
                thread = ThreadUtil.newHandlerThread(getName(), ThreadUtil.ThreadPriority.DEFAULT);

        final long wallTimeMillis = System.currentTimeMillis();
        final long elapsedRealtimeMillis = SystemClock.elapsedRealtime();

        try {
            final String intentAction = "com.twofortyfouram.spackle.test."
                    + getName(); //$NON-NLS-1$
            final CountDownLatch latch = new CountDownLatch(1);
            final BroadcastReceiver receiver = new BroadcastReceiver() {

                @Override
                public void onReceive(Context context, Intent intent) {

                    latch.countDown();

                }
            };
            getContext().registerReceiver(receiver, new IntentFilter(intentAction));

            final long whenMillis;
            switch (type) {
                case AlarmManager.ELAPSED_REALTIME:
                case AlarmManager.ELAPSED_REALTIME_WAKEUP: {
                    whenMillis = elapsedRealtimeMillis + delayMillis;
                    break;
                }
                case AlarmManager.RTC:
                case AlarmManager.RTC_WAKEUP: {
                    whenMillis = wallTimeMillis + delayMillis;
                    break;
                }
                default: {
                    throw new AssertionError();
                }
            }

            com.twofortyfouram.spackle.AlarmManagerCompat
                    .getInstance(getContext()).setExact(getContext(), type,
                    whenMillis,
                    PendingIntent.getBroadcast(getContext(), 0, new Intent(intentAction), 0),
                    wallTimeMillis, elapsedRealtimeMillis);

            try {
                assertTrue(latch.await(delayMillis + ALARM_SLOP_MILLIS,
                        TimeUnit.MILLISECONDS));
            } catch (final InterruptedException e) {
                fail(e.getMessage());
            } finally {
                getContext().unregisterReceiver(receiver);
            }

        } finally {
            thread.getLooper().quit();
        }
    }

}
