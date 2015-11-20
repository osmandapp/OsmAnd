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
import android.support.annotation.RequiresPermission;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;

/**
 * Tests {@link PartialWakeLockForService}.
 */
public final class PartialWakeLockForServiceTest extends AndroidTestCase {

    @SmallTest
    public void testConstructor_good() {
        final PartialWakeLockForService helper = new PartialWakeLockForService(getName());

        assertFalse(helper.getWakeLock(getContext()).isHeld());
    }

    @SmallTest
    public void testGetWakeLock() {
        final PartialWakeLockForService helper = new PartialWakeLockForService(getName());

        final PartialWakeLock wakeLock = helper.getWakeLock(getContext());
        assertNotNull(wakeLock);

        assertSame("Should return same object", wakeLock,
                helper.getWakeLock(getContext())); //$NON-NLS-1$
    }

    @SmallTest
    @RequiresPermission(Manifest.permission.WAKE_LOCK)
    public void testBeforeStartingService() {
        final PartialWakeLockForService helper = new PartialWakeLockForService(getName());

        helper.beforeStartingService(getContext());
        assertTrue(helper.getWakeLock(getContext()).isHeld());
        assertEquals(1, helper.getWakeLock(getContext()).getReferenceCount());

        helper.beforeStartingService(getContext());
        assertEquals(2, helper.getWakeLock(getContext()).getReferenceCount());
        assertTrue(helper.getWakeLock(getContext()).isHeld());
    }

    @SmallTest
    @RequiresPermission(Manifest.permission.WAKE_LOCK)
    public void testOnStartCommand() {
        final PartialWakeLockForService helper = new PartialWakeLockForService(getName());

        helper.beforeDoingWork(getContext());
        assertTrue(helper.getWakeLock(getContext()).isHeld());
        assertEquals(1, helper.getWakeLock(getContext()).getReferenceCount());

        helper.beforeDoingWork(getContext());
        assertEquals(1, helper.getWakeLock(getContext()).getReferenceCount());
        assertTrue(helper.getWakeLock(getContext()).isHeld());
    }

    @SmallTest
    @RequiresPermission(Manifest.permission.WAKE_LOCK)
    public void testAfterDoingWork_no_start() {
        final PartialWakeLockForService helper = new PartialWakeLockForService(getName());

        helper.afterDoingWork(getContext());
        assertFalse(helper.getWakeLock(getContext()).isHeld());
    }

    @SmallTest
    @RequiresPermission(Manifest.permission.WAKE_LOCK)
    public void testAfterDoingWork_with_onStartCommand() {
        final PartialWakeLockForService helper = new PartialWakeLockForService(getName());

        helper.beforeDoingWork(getContext());
        assertTrue(helper.getWakeLock(getContext()).isHeld());

        helper.afterDoingWork(getContext());
    }

    @SmallTest
    @RequiresPermission(Manifest.permission.WAKE_LOCK)
    public void testAfterDoingWork_full() {
        final PartialWakeLockForService helper = new PartialWakeLockForService(getName());

        assertFalse(helper.getWakeLock(getContext()).isHeld());
        assertEquals(0, helper.getWakeLock(getContext()).getReferenceCount());

        helper.beforeStartingService(getContext());
        assertTrue(helper.getWakeLock(getContext()).isHeld());
        assertEquals(1, helper.getWakeLock(getContext()).getReferenceCount());

        helper.beforeDoingWork(getContext());
        assertTrue(helper.getWakeLock(getContext()).isHeld());
        assertEquals(1, helper.getWakeLock(getContext()).getReferenceCount());

        helper.afterDoingWork(getContext());
        assertFalse(helper.getWakeLock(getContext()).isHeld());
        assertEquals(0, helper.getWakeLock(getContext()).getReferenceCount());
    }
}
