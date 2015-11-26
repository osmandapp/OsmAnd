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
import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.PowerManager;
import android.os.SystemClock;
import android.support.annotation.IntDef;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresPermission;
import android.text.format.DateUtils;

import com.twofortyfouram.log.Lumberjack;
import com.twofortyfouram.spackle.power.PartialWakeLock;

import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.Immutable;
import net.jcip.annotations.ThreadSafe;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Provides backwards and forwards compatibility with the AlarmManager, specifically with changes
 * to the alarm manager APIs.
 */
@ThreadSafe
public final class AlarmManagerCompat {

    /**
     * Alarms scheduled further out than this number of milliseconds will use the alarm manager.
     */
    /*
     * The actual OS cutoff is 5 seconds, but 6 seconds was chosen to allow for slight processing
     * delays and to mitigate against TOCTOU errors that might occur when deciding which scheduling
     * type to use.
     */
    private static final long LOLLIPOP_ALARM_CUTOFF_MILLIS = 5 * DateUtils.SECOND_IN_MILLIS;

    /**
     * Lock to synchronize initialization of {@link #sInstance}.
     */
    @NonNull
    private static final Object INITIALIZATION_INTRINSIC_LOCK = new Object();

    @Nullable
    @GuardedBy("INITIALIZATION_INTRINSIC_LOCK")
    private static volatile AlarmManagerCompat sInstance;

    @NonNull
    private final Context mContext;

    @NonNull
    private final AlarmManager mAlarmManager;

    @NonNull
    private final PowerManager mPowerManager;

    @NonNull
    private final Handler mHandler;

    /**
     * @param applicationContext Application context.
     * @return Instance of the alarm manager compatibility class.
     */
    @NonNull
    public static AlarmManagerCompat getInstance(@NonNull final Context applicationContext) {
        /*
         * Note: this method may be called from any thread.
         */

        final Context ctx = ContextUtil.cleanContext(applicationContext);

        /*
         * Double-checked idiom for lazy initialization, Effective Java 2nd
         * edition page 283.
         */
        AlarmManagerCompat alarmManagerCompat = sInstance;
        if (null == alarmManagerCompat) {
            synchronized (INITIALIZATION_INTRINSIC_LOCK) {
                alarmManagerCompat = sInstance;
                if (null == alarmManagerCompat) {
                    sInstance = alarmManagerCompat = new AlarmManagerCompat(ctx);
                }
            }
        }

        return alarmManagerCompat;
    }

    /**
     * Private constructor prevents instantiation.
     *
     * @throws UnsupportedOperationException because this class cannot be instantiated.
     */
    private AlarmManagerCompat(@NonNull final Context applicationContext) {
        mContext = ContextUtil.cleanContext(applicationContext);

        mAlarmManager = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
        mPowerManager = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);

        /*
         * Default priority was chosen to improve the exactness of when the handler processes the message
         * if the device is under heavy load.
         */
        final HandlerThread thread = ThreadUtil
                .newHandlerThread(AlarmManagerCompat.class.getName() + ".handler", //$NON-NLS-1$
                        ThreadUtil.ThreadPriority.DEFAULT);
        mHandler = new Handler(thread.getLooper());
    }

    /**
     * This method schedules exact alarms with support for the new exact alarm behavior KitKat,
     * works around the minimum alarm interval of 5 seconds for Lollipop, and provides best-effort
     * workarounds for the idling behavior of Marshmallow.
     *
     * Handling of short duration alarms on Lollipop has a few consequences.  This documentation
     * reveals implementation details, which is necessary to communicate the behavior seen, however
     * implementation details are subject to change.
     *
     * On Lollipop, short duration alarms (less than 5 seconds), will be reliably
     * triggered by this method which will hold a WakeLock and fire the Intent directly rather than
     * go through the AlarmManager.  Note that a WakeLock is also held for non-WAKEUP alarms,
     * because this class prioritizes the exactness of the scheduling. Due to the usage of
     * WakeLocks, clients should not schedule short duration alarms frequently.  The expected use
     * case is an application that needs to check things at exact times, and infrequently has exact
     * checks in the near future. To make scheduling behavior explicit to clients, this method
     * makes
     * decisions based on the
     * {@code currentWalltimeMillis} and {@code currentElapsedRealtimeMillis} passed in.  For a
     * short duration RTC alarm, there is a risk of TOCTOU errors if the wall time changes after
     * this method is called.  To mitigate this, clients should be able to handle receiving a short
     * duration RTC alarm slightly sooner or later than expected.  This risk does not apply to
     * longer-duration alarms.
     *
     * @param context         Application context.
     * @param type            One of the alarm types: {@link AlarmManager#RTC}, {@link
     *                        AlarmManager#RTC_WAKEUP}
     *                        , {@link AlarmManager#ELAPSED_REALTIME}, or
     *                        {@link AlarmManager#ELAPSED_REALTIME_WAKEUP}.
     * @param triggerAtMillis Time in milliseconds when the alarm should fire. This will be either
     *                        wall time or elapsed realtime, depending on {@code type}.
     * @param pendingIntent   Operation to execute when the alarm fires.  This object should not
     *                        be mutated after being passed to this method.
     */
    @NonNull
    @RequiresPermission(Manifest.permission.WAKE_LOCK)
    public AlarmToken setExact(@NonNull final Context context, @AlarmType final int type,
            @IntRange(from = 0) final long triggerAtMillis,
            @NonNull final PendingIntent pendingIntent,
            final long currentWalltimeMillis, final long currentElapsedRealtimeMillis) {

        if (AndroidSdkVersion.isAtLeastSdk(Build.VERSION_CODES.LOLLIPOP)) {
            // Not entirely sure if this impacts Lollipop or Lollipop MR1 but that shouldn't matter
            // if we include both.
            return setExactLollipop(type, triggerAtMillis, pendingIntent, currentWalltimeMillis,
                    currentElapsedRealtimeMillis);
        } else if (AndroidSdkVersion.isAtLeastSdk(Build.VERSION_CODES.KITKAT)) {
            return setExactKitKat(type, triggerAtMillis, pendingIntent);
        } else {
            mAlarmManager.set(type, triggerAtMillis, pendingIntent);
            return new AlarmToken(pendingIntent);
        }
    }

    /**
     * Cancels the alarm, on a best-effort basis.
     */
    public void cancel(@NonNull final AlarmToken alarmToken) {
        if (null != alarmToken.mPendingIntent) {
            mAlarmManager.cancel(alarmToken.mPendingIntent);
        } else {
            final PartialWakeLock partialWakeLock = alarmToken.mPartialWakeLock;
            mHandler.removeCallbacksAndMessages(alarmToken.mPartialWakeLock);

            /*
             * There is a chance the message is being processed concurrently.  This ensures
             * that the WakeLock is guaranteed to be released.
             */
            partialWakeLock.releaseLockIfHeld();
        }
    }

    @NonNull
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @RequiresPermission(Manifest.permission.WAKE_LOCK)
    private AlarmToken setExactLollipop(@AlarmType final int type,
            final long triggerAtMillis, @NonNull final PendingIntent pendingIntent,
            final long currentWalltimeMillis, final long currentElapsedRealtimeMillis) {
        switch (type) {
            case AlarmManager.ELAPSED_REALTIME:
            case AlarmManager.ELAPSED_REALTIME_WAKEUP: {
                if (triggerAtMillis - currentElapsedRealtimeMillis
                        > LOLLIPOP_ALARM_CUTOFF_MILLIS) {
                    return setExactKitKat(type, triggerAtMillis, pendingIntent);
                } else {
                    return scheduleElapsedWakeupOnHandler(
                            SystemClock.uptimeMillis() + (triggerAtMillis
                                    - currentElapsedRealtimeMillis), pendingIntent);
                }
            }
            case AlarmManager.RTC:
            case AlarmManager.RTC_WAKEUP: {
                if (triggerAtMillis - currentWalltimeMillis
                        > LOLLIPOP_ALARM_CUTOFF_MILLIS) {
                    return setExactKitKat(type, triggerAtMillis, pendingIntent);
                } else {
                    return scheduleElapsedWakeupOnHandler(
                            SystemClock.uptimeMillis() + (triggerAtMillis - currentWalltimeMillis),
                            pendingIntent);
                }
            }
            default: {
                throw new AssertionError();
            }
        }
    }

    @NonNull
    @TargetApi(Build.VERSION_CODES.KITKAT)
    private AlarmToken setExactKitKat(@AlarmType final int type,
            final long triggerAtMillis, @NonNull final PendingIntent pendingIntent) {
        mAlarmManager.setExact(type, triggerAtMillis, pendingIntent);
        return new AlarmToken(pendingIntent);
    }

    @RequiresPermission(Manifest.permission.WAKE_LOCK)
    private AlarmToken scheduleElapsedWakeupOnHandler(final long triggerAtUptimeMillis,
            @NonNull final PendingIntent pendingIntent) {

        // Note: because the AlarmManagerCompat class name is used, blame for the wakelock may not
        // correctly attributed.  In practice, very few short duration WakeLocks should be held so
        // practically this limitation probably doesn't matter.  If it ever does, consider
        // passing a WakeLock "tag" into the setExact method.
        final PartialWakeLock partialWakeLock = PartialWakeLock
                .newInstance(mContext, AlarmManagerCompat.class.getName(), false);
        partialWakeLock.acquireLock();
        final AlarmToken alarmToken = new AlarmToken(partialWakeLock);

        // handler users uptime internally, but if the lock is held then uptime and elapsed realtime
        // end up being effectively the same.
        if (!mHandler.postAtTime(new AlarmRunnable(pendingIntent, partialWakeLock), alarmToken,
                triggerAtUptimeMillis)) {
            partialWakeLock.releaseLock();
        }

        return alarmToken;
    }

    @ThreadSafe
    private static final class AlarmRunnable implements Runnable {

        @NonNull
        private final PendingIntent mPendingIntent;

        @NonNull
        private final PartialWakeLock mPartialWakeLock;

        public AlarmRunnable(@NonNull final PendingIntent pendingIntent,
                @NonNull final PartialWakeLock partialWakeLock) {
            mPendingIntent = pendingIntent;
            mPartialWakeLock = partialWakeLock;
        }

        private static void handleTriggerAlarm(@NonNull final PendingIntent pendingIntent) {
            try {
                pendingIntent.send();
            } catch (final PendingIntent.CanceledException e) {
                Lumberjack.v("pendingIntent was canceled", e);
            }
        }

        @Override
        public void run() {
            try {
                handleTriggerAlarm(mPendingIntent);
            } finally {
                mPartialWakeLock.releaseLockIfHeld();
            }
        }
    }

    /**
     * Effectively immutable class representing a token used to set an alarm.
     */
    @Immutable
    public static final class AlarmToken {

        @Nullable
        private final PartialWakeLock mPartialWakeLock;

        @Nullable
        private final PendingIntent mPendingIntent;

        private AlarmToken(@NonNull final PendingIntent pendingIntent) {
            mPendingIntent = pendingIntent;
            mPartialWakeLock = null;
        }

        private AlarmToken(@NonNull final PartialWakeLock partialWakeLock) {
            mPartialWakeLock = partialWakeLock;
            mPendingIntent = null;
        }
    }

    /**
     * Defines the possible values for the alarm manager type.
     */
    @IntDef({AlarmManager.RTC, AlarmManager.RTC_WAKEUP, AlarmManager.ELAPSED_REALTIME,
            AlarmManager.ELAPSED_REALTIME_WAKEUP})
    @Retention(RetentionPolicy.SOURCE)
    public @interface AlarmType {

    }
}
