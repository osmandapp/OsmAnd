/*
 * android-bootstrap-core-lib https://github.com/twofortyfouram/android-bootstrap-core
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

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.SystemClock;
import android.test.suitebuilder.annotation.MediumTest;
import android.test.suitebuilder.annotation.SmallTest;
import android.text.format.DateUtils;

import com.twofortyfouram.spackle.ThreadUtil.ThreadPriority;
import com.twofortyfouram.test.assertion.MoarAsserts;

import junit.framework.TestCase;

import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Tests {@link ThreadUtil}.
 */
public final class ThreadUtilTest extends TestCase {

    @SmallTest
    public static void testNonInstantiable() {
        MoarAsserts.assertNoninstantiable(ThreadUtil.class);
    }

    @SmallTest
    public void testIsMainThread_not_on_main() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);

        HandlerThread thread = null;
        try {
            thread = ThreadUtil.newHandlerThread(getName(), ThreadPriority.DEFAULT);
            new Handler(thread.getLooper()).post(new Runnable() {

                @Override
                public void run() {
                    assertFalse(ThreadUtil.isMainThread());

                    latch.countDown();
                }

            });

            assertTrue(latch.await(1 * DateUtils.SECOND_IN_MILLIS, TimeUnit.MILLISECONDS));
        } finally {
            if (null != thread) {
                thread.getLooper().quit();
            }
        }
    }

    @SmallTest
    public static void testIsMainThread_on_main() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);

        new Handler(Looper.getMainLooper()).post(new Runnable() {

            @Override
            public void run() {
                assertTrue(ThreadUtil.isMainThread());

                latch.countDown();
            }

        });

        assertTrue(latch.await(1 * DateUtils.SECOND_IN_MILLIS, TimeUnit.MILLISECONDS));
    }

    @MediumTest
    public void testGetHandlerThread() {

        for (final ThreadPriority priority : ThreadPriority.values()) {
            final String threadName = String
                    .format(Locale.US, "%s-%s", getName(), priority); //$NON-NLS-1$

            HandlerThread thread = null;
            try {
                thread = ThreadUtil.newHandlerThread(threadName, priority);

                assertNotNull(thread);
                assertEquals(threadName, thread.getName());
                assertTrue(thread.isAlive());
                assertNotNull(thread.getLooper());
                assertNotSame(
                        "Should not be main thread", Looper.getMainLooper(),
                        thread.getLooper()); //$NON-NLS-1$
                assertNotSame("Should not be current thread", Looper.myLooper(),
                        thread.getLooper()); //$NON-NLS-1$

                /*
                 * There appears to be a race condition before thread priorities are set, so a
                 * slight delay is needed before thread priority can be read.
                 */
                SystemClock.sleep(100);

                assertEquals(priority.getPriority(),
                        android.os.Process.getThreadPriority(thread.getThreadId()));
            } finally {
                if (null != thread) {
                    thread.getLooper().quit();
                }
            }
        }
    }

    @SmallTest
    public void testGetHandlerThread_new_instance() {
        final String threadName = getName();

        HandlerThread thread1 = null;
        HandlerThread thread2 = null;
        try {
            thread1 = ThreadUtil.newHandlerThread(threadName, ThreadPriority.DEFAULT);
            thread2 = ThreadUtil.newHandlerThread(threadName, ThreadPriority.DEFAULT);

            assertNotSame(thread1, thread2);
        } finally {
            if (null != thread1) {
                thread1.getLooper().quit();
            }

            if (null != thread2) {
                thread2.getLooper().quit();
            }
        }
    }

    @SmallTest
    public static void testGetHandlerThread_bad_parameter_empty_name() {
        try {
            ThreadUtil.newHandlerThread("", ThreadPriority.DEFAULT); //$NON-NLS-1$
            fail();
        } catch (final AssertionError e) {
            // expected exception
        }
    }

    @SmallTest
    public static void testGetHandlerThread_bad_parameter_null_name() {
        try {
            ThreadUtil.newHandlerThread(null, ThreadPriority.DEFAULT);
            fail();
        } catch (final AssertionError e) {
            // expected exception
        }
    }
}
