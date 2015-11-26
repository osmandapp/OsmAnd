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

package com.twofortyfouram.spackle.power;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.os.PowerManager;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresPermission;

import com.twofortyfouram.log.Lumberjack;

import net.jcip.annotations.ThreadSafe;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Manages partial {@code WakeLock}s.
 * <p>
 * Users of this class must have the permission
 * {@link android.Manifest.permission#WAKE_LOCK}.
 */
/*
 * Android Lint warnings for this class are useless.
 */
@SuppressLint("Wakelock")
@ThreadSafe
public final class PartialWakeLock {

    /**
     * Map of WakeLock name to approximate cumulative duration in milliseconds
     * that lock has been held.
     */
    @NonNull
    private static final ConcurrentHashMap<String, AtomicLong> sWakeLockCumulativeUsage
            = new ConcurrentHashMap<String, AtomicLong>();

    /**
     * The {@code WakeLock} tag.
     */
    @NonNull
    private final String mLockName;

    /**
     * Flag indicating whether {@link #mWakeLock} is reference counted.
     */
    private final boolean mIsReferenceCounted;

    /**
     * {@code WakeLock} encapsulated by this class.
     */
    @NonNull
    private final PowerManager.WakeLock mWakeLock;

    /**
     * Reference count for the number of times {@link #mWakeLock} has been
     * obtained.
     */
    private int mReferenceCount = 0;

    /**
     * Realtime when {@link #mWakeLock} was acquired.
     */
    private long mAcquiredRealtimeMillis = 0;

    /**
     * Dumps cumulative WakeLock usage from this class and {@link com.twofortyfouram.spackle.power.PartialWakeLockForService}.
     * This is useful to debug WakeLock usage.
     *
     * @return A map of WakeLock name and cumulative duration in milliseconds that the lock was
     * held.
     */
    @NonNull
    public static Map<String, Long> dumpWakeLockUsage() {
        final Map<String, Long> wakeLockCumulativeUsageToReturn = new HashMap<String, Long>();

        /*
         * Note that the iterator does not lock the map.  The read is thread safe, but it is not
         * atomic.  In other words, between starting the loop and ending the loop, some cumulative
         * usages could be incremented. The results returned by this method are therefore
         * approximate.
         */
        for (final Entry<String, AtomicLong> entry : sWakeLockCumulativeUsage.entrySet()) {
            wakeLockCumulativeUsageToReturn.put(entry.getKey(), entry.getValue().get());
        }

        return wakeLockCumulativeUsageToReturn;
    }

    /**
     * Constructs a new {@code PartialWakeLock}.
     *
     * <p>It is recommended that
     * applications use a finite number of values for {@code lockName}, as an unbounded
     * number of names would create a memory leak.  For example, consider
     * a background service.  Using the name "my_service_lock" every time the service is started
     * would be better than "my_service_lock_%d" where %d is incremented
     * every time the service starts.  Internally this class maintains a historical count of lock
     * durations to enable {@link #dumpWakeLockUsage()}, so creating an unbounded number of tags
     * would grow linearly in memory usage.
     *
     * @param context            Application context.
     * @param lockName           a tag for identifying the lock.
     * @param isReferenceCounted true if the lock is reference counted. False if
     *                           the lock is not reference counted.
     * @return A new {@link PartialWakeLock}.
     */
    @NonNull
    public static PartialWakeLock newInstance(@NonNull final Context context,
            @NonNull final String lockName, final boolean isReferenceCounted) {
        sWakeLockCumulativeUsage.putIfAbsent(lockName, new AtomicLong(0));

        return new PartialWakeLock(context, lockName, isReferenceCounted);
    }

    private PartialWakeLock(@NonNull final Context context, @NonNull final String lockName,
            final boolean isReferenceCounted) {
        mLockName = lockName;
        mIsReferenceCounted = isReferenceCounted;

        final PowerManager mgr = (PowerManager) context.getSystemService(Context.POWER_SERVICE);

        mWakeLock = mgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, mLockName);
        mWakeLock.setReferenceCounted(isReferenceCounted);
    }

    /**
     * Acquire a partial {@code WakeLock}.
     * <p>
     * This method may be called multiple times. If the lock is reference
     * counted, then each call to this method needs to be balanced by
     * a call to {@link #releaseLock()}. Otherwise if the lock is not reference
     * counted, then multiple calls have no effect.
     */
    @RequiresPermission(Manifest.permission.WAKE_LOCK)
    public void acquireLock() {
        synchronized (mWakeLock) {
            final boolean isHeld = isHeld();

            if (!isHeld) {
                mAcquiredRealtimeMillis = SystemClock.elapsedRealtime();
            }

            if (mIsReferenceCounted || !isHeld) {
                mReferenceCount++;
            }

            mWakeLock.acquire();

            Lumberjack.v("%s", this); //$NON-NLS-1$
        }
    }

    /**
     * Similar to {@link #acquireLock()}, but only acquires a lock if
     * {@link #isHeld()} returns false.
     */
    @RequiresPermission(Manifest.permission.WAKE_LOCK)
    public void acquireLockIfNotHeld() {
        synchronized (mWakeLock) {
            if (!mWakeLock.isHeld()) {
                acquireLock();
            }
        }
    }

    /**
     * Release a {@code WakeLock} previously acquired. This method should
     * balance a call to {@link #acquireLock()}.
     *
     * @throws IllegalStateException if the {@code WakeLockManager} is
     *                               underlocked.
     */
    public void releaseLock() {
        synchronized (mWakeLock) {
            if (isHeld()) {
                mReferenceCount--;
                mWakeLock.release();

                Lumberjack.v("%s", this); //$NON-NLS-1$

                if (!isHeld()) {
                    sWakeLockCumulativeUsage.get(mLockName).addAndGet(getHeldDurationMillis());
                    mAcquiredRealtimeMillis = 0;
                }
            } else {
                throw new IllegalStateException(Lumberjack.formatMessage(
                        "Lock \"%s\" was not held", mLockName)); //$NON-NLS-1$
            }
        }
    }

    /**
     * Like {@link #releaseLock()} but only releases if {@link #isHeld()}
     * returns true. This method will not throw exceptions for being
     * underlocked.
     */
    public void releaseLockIfHeld() {
        synchronized (mWakeLock) {
            if (isHeld()) {
                releaseLock();
            }
        }
    }

    /**
     * Determine whether a {@code WakeLock} is held.
     *
     * @return {@code true} if a lock is held. Otherwise returns {@code false}.
     */
    public boolean isHeld() {
        synchronized (mWakeLock) {
            return mWakeLock.isHeld();
        }
    }

    /**
     * @return The number of references held for this lock. If the lock is not
     * reference counted, then the maximum value this method will return
     * is 1.
     */
    /* package */int getReferenceCount() {
        synchronized (mWakeLock) {
            return mReferenceCount;
        }
    }

    /**
     * @return The duration the lock has been held, or 0 if the lock is not
     * held.
     */
    private long getHeldDurationMillis() {
        synchronized (mWakeLock) {
            final long acquiredRealtimeMillis = mAcquiredRealtimeMillis;

            final long durationMillis;
            if (0 == acquiredRealtimeMillis) {
                durationMillis = 0;
            } else {
                durationMillis = SystemClock.elapsedRealtime() - acquiredRealtimeMillis;
            }

            return durationMillis;
        }
    }

    @Override
    public String toString() {
        return String
                .format(Locale.US,
                        "PartialWakeLock [mLockName=%s, mIsReferenceCounted=%s, mReferenceCount=%s, durationHeldMillis=%d, mWakeLock=%s]",
                        //$NON-NLS-1$
                        mLockName, mIsReferenceCounted, mReferenceCount, getHeldDurationMillis(),
                        mWakeLock);
    }
}
