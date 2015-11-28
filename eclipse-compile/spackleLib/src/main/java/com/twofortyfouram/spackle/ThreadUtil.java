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

import android.os.HandlerThread;
import android.os.Looper;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;

import net.jcip.annotations.Immutable;
import net.jcip.annotations.ThreadSafe;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Provides Android-specific thread management.
 */
@ThreadSafe
public final class ThreadUtil {

    /**
     * @return True if the current thread is the main application thread.
     */
    public static boolean isMainThread() {
        return Looper.myLooper() == Looper.getMainLooper();
    }

    /**
     * Obtains a new, started HandlerThread.
     *
     * @param threadName     Name of the thread.
     * @param threadPriority Priority of the thread. Must be one of the priority constants defined
     *                       in {@link android.os.Process}.
     * @return a HandlerThread with the given {@code threadName} and {@code threadPriority}, whose
     * {@link HandlerThread#start()} method has already been called.
     */
    @NonNull
    public static HandlerThread newHandlerThread(@NonNull final String threadName,
            @NonNull final ThreadPriority threadPriority) {
        final HandlerThread thread = new HandlerThread(threadName, threadPriority.getPriority());

        thread.start();

        return thread;
    }

    /**
     * Rich enum for the Android thread priority levels.
     */
    /*
     * Note that passing java.lang.Thread.MIN_PRIORITY is not the same thing as passing
     * android.os.Process.THREAD_PRIORITY_LOWEST to the constructor of HandlerThread. This is an
     * easy mistake to make, because Android won't complain. This enum's primary purpose is to help
     * avoid such errors. The Android team is overly aggressive with the use if int constants
     * instead of enums, and the long term cost of setting a thread to the wrong priority is likely
     * much greater than the memory cost of using an enum here.
     */
    @Immutable
    public enum ThreadPriority {
        /**
         * @see android.os.Process#THREAD_PRIORITY_AUDIO
         */
        @NonNull
        AUDIO(android.os.Process.THREAD_PRIORITY_AUDIO),

        /**
         * @see android.os.Process#THREAD_PRIORITY_BACKGROUND
         */
        @NonNull
        BACKGROUND(android.os.Process.THREAD_PRIORITY_BACKGROUND),

        /**
         * @see android.os.Process#THREAD_PRIORITY_DEFAULT
         */
        @NonNull
        DEFAULT(android.os.Process.THREAD_PRIORITY_DEFAULT),

        /**
         * @see android.os.Process#THREAD_PRIORITY_DISPLAY
         */
        @NonNull
        DISPLAY(android.os.Process.THREAD_PRIORITY_DISPLAY),

        /**
         * @see android.os.Process#THREAD_PRIORITY_FOREGROUND
         */
        @NonNull
        FOREGROUND(android.os.Process.THREAD_PRIORITY_FOREGROUND),

        /**
         * @see android.os.Process#THREAD_PRIORITY_LESS_FAVORABLE
         */
        @NonNull
        LESS_FAVORABLE(android.os.Process.THREAD_PRIORITY_LESS_FAVORABLE),

        /**
         * @see android.os.Process#THREAD_PRIORITY_LOWEST
         */
        @NonNull
        LOWEST(android.os.Process.THREAD_PRIORITY_LOWEST),

        /**
         * @see android.os.Process#THREAD_PRIORITY_MORE_FAVORABLE
         */
        @NonNull
        MORE_FAVORABLE(android.os.Process.THREAD_PRIORITY_MORE_FAVORABLE),

        /**
         * @see android.os.Process#THREAD_PRIORITY_URGENT_AUDIO
         */
        @NonNull
        URGENT_AUDIO(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO),

        /**
         * @see android.os.Process#THREAD_PRIORITY_URGENT_DISPLAY
         */
        @NonNull
        URGENT_DISPLAY(android.os.Process.THREAD_PRIORITY_URGENT_DISPLAY);

        @ThreadPriorityTypeDef
        private final int mPriority;

        private ThreadPriority(@ThreadPriorityTypeDef final int priority) {
            mPriority = priority;
        }

        /**
         * @return The Android thread priority level.
         */
        @ThreadPriorityTypeDef
        /* package */int getPriority() {
            return mPriority;
        }
    }

    /**
     * Defines the possible values for the alarm manager type.
     */
    @IntDef({android.os.Process.THREAD_PRIORITY_AUDIO,
            android.os.Process.THREAD_PRIORITY_BACKGROUND,
            android.os.Process.THREAD_PRIORITY_DEFAULT,
            android.os.Process.THREAD_PRIORITY_DISPLAY,
            android.os.Process.THREAD_PRIORITY_FOREGROUND,
            android.os.Process.THREAD_PRIORITY_LESS_FAVORABLE,
            android.os.Process.THREAD_PRIORITY_LOWEST,
            android.os.Process.THREAD_PRIORITY_MORE_FAVORABLE,
            android.os.Process.THREAD_PRIORITY_URGENT_AUDIO,
            android.os.Process.THREAD_PRIORITY_URGENT_DISPLAY})
    @Retention(RetentionPolicy.SOURCE)
    /*package*/ @interface ThreadPriorityTypeDef {

    }

    /**
     * Private constructor prevents instantiation.
     *
     * @throws UnsupportedOperationException because this class cannot be instantiated.
     */
    private ThreadUtil() {
        throw new UnsupportedOperationException("This class is non-instantiable"); //$NON-NLS-1$
    }
}
