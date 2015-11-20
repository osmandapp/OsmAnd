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
 * Tests the basics of the {@code PartialWakeLock}.
 */
public final class PartialWakeLockTest extends AndroidTestCase {

    @SmallTest
    public void testConstructor_not_reference_counted() {
        final PartialWakeLock manager = PartialWakeLock.newInstance(getContext(), getName(), false);

        assertEquals(0, manager.getReferenceCount());
        assertFalse(manager.isHeld());
    }

    @SmallTest
    public void testConstructor_reference_counted() {
        final PartialWakeLock manager = PartialWakeLock.newInstance(getContext(), getName(), true);

        assertFalse(manager.isHeld());
        assertEquals(0, manager.getReferenceCount());
    }

    @SmallTest
    @RequiresPermission(Manifest.permission.WAKE_LOCK)
    public void testAcquire_single_not_reference_counted() {
        final PartialWakeLock manager = PartialWakeLock.newInstance(getContext(), getName(), false);

        manager.acquireLock();
        assertEquals(1, manager.getReferenceCount());
        assertTrue(manager.isHeld());

        manager.releaseLock();
        assertEquals(0, manager.getReferenceCount());
        assertFalse(manager.isHeld());
    }

    @SmallTest
    @RequiresPermission(Manifest.permission.WAKE_LOCK)
    public void testAcquire_single_reference_counted() {
        final PartialWakeLock manager = PartialWakeLock.newInstance(getContext(), getName(), true);

        manager.acquireLock();
        assertEquals(1, manager.getReferenceCount());
        assertTrue(manager.isHeld());

        manager.releaseLock();
        assertEquals(0, manager.getReferenceCount());
        assertFalse(manager.isHeld());
    }

    @SmallTest
    @RequiresPermission(Manifest.permission.WAKE_LOCK)
    public void testAcquire_multiple_not_reference_counted() {
        final PartialWakeLock manager = PartialWakeLock.newInstance(getContext(), getName(), false);

        manager.acquireLock();
        assertEquals(1, manager.getReferenceCount());
        assertTrue(manager.isHeld());

        manager.acquireLock();
        assertEquals(1, manager.getReferenceCount());
        assertTrue(manager.isHeld());

        manager.releaseLock();
        assertEquals(0, manager.getReferenceCount());
        assertFalse(manager.isHeld());

        manager.acquireLock();
        assertEquals(1, manager.getReferenceCount());
        assertTrue(manager.isHeld());

        manager.acquireLock();
        assertEquals(1, manager.getReferenceCount());
        assertTrue(manager.isHeld());

        manager.releaseLock();
        assertEquals(0, manager.getReferenceCount());
        assertFalse(manager.isHeld());
    }

    @SmallTest
    @RequiresPermission(Manifest.permission.WAKE_LOCK)
    public void testAcquire_multiple_reference_counted() {
        final PartialWakeLock manager = PartialWakeLock.newInstance(getContext(), getName(), true);

        manager.acquireLock();
        assertEquals(1, manager.getReferenceCount());
        assertTrue(manager.isHeld());

        manager.acquireLock();
        assertEquals(2, manager.getReferenceCount());
        assertTrue(manager.isHeld());

        manager.releaseLock();
        assertEquals(1, manager.getReferenceCount());
        assertTrue(manager.isHeld());

        manager.acquireLock();
        assertEquals(2, manager.getReferenceCount());
        assertTrue(manager.isHeld());

        manager.acquireLock();
        assertEquals(3, manager.getReferenceCount());
        assertTrue(manager.isHeld());

        manager.releaseLock();
        assertEquals(2, manager.getReferenceCount());
        assertTrue(manager.isHeld());

        manager.releaseLock();
        assertEquals(1, manager.getReferenceCount());
        assertTrue(manager.isHeld());

        manager.releaseLock();
        assertEquals(0, manager.getReferenceCount());
        assertFalse(manager.isHeld());

        manager.acquireLock();
        assertEquals(1, manager.getReferenceCount());
        assertTrue(manager.isHeld());

        manager.releaseLock();
        assertEquals(0, manager.getReferenceCount());
        assertFalse(manager.isHeld());
    }

    @SmallTest
    public void testUnderlock_not_reference_counted() {
        final PartialWakeLock manager = PartialWakeLock.newInstance(getContext(), getName(), false);

        try {
            manager.releaseLock();
            fail();
        } catch (final IllegalStateException e) {
            // expected exception
        }

    }

    @SmallTest
    public void testUnderlock_reference_counted() {
        final PartialWakeLock manager = PartialWakeLock.newInstance(getContext(), getName(), true);

        try {
            manager.releaseLock();
            fail();
        } catch (final IllegalStateException e) {
            // expected exception
        }
    }

    @SmallTest
    @RequiresPermission(Manifest.permission.WAKE_LOCK)
    public void testReleaseIfHeld_not_reference_counted() {
        final PartialWakeLock manager = PartialWakeLock.newInstance(getContext(), getName(), false);

        manager.releaseLockIfHeld();

        assertFalse(manager.isHeld());

        manager.acquireLock();
        manager.releaseLockIfHeld();
        assertFalse(manager.isHeld());
    }


    @SmallTest
    @RequiresPermission(Manifest.permission.WAKE_LOCK)
    public void testReleaseIfHeld_reference_counted() {
        final PartialWakeLock manager = PartialWakeLock.newInstance(getContext(), getName(), true);

        manager.releaseLockIfHeld();

        assertFalse(manager.isHeld());

        manager.acquireLock();
        manager.acquireLock();
        assertEquals(2, manager.getReferenceCount());
        manager.releaseLockIfHeld();
        assertEquals(1, manager.getReferenceCount());
        manager.releaseLockIfHeld();
        assertEquals(0, manager.getReferenceCount());
        assertFalse(manager.isHeld());
    }

    @SmallTest
    @RequiresPermission(Manifest.permission.WAKE_LOCK)
    public void testAcquireIfNotHeld_not_reference_counted() {
        final PartialWakeLock manager = PartialWakeLock.newInstance(getContext(), getName(), false);

        manager.acquireLockIfNotHeld();
        assertEquals(1, manager.getReferenceCount());
        assertTrue(manager.isHeld());

        manager.releaseLock();
        assertFalse(manager.isHeld());

        manager.acquireLock();
        manager.acquireLockIfNotHeld();
        assertEquals(1, manager.getReferenceCount());

        manager.releaseLock();
        assertFalse(manager.isHeld());
    }

    @SmallTest
    @RequiresPermission(Manifest.permission.WAKE_LOCK)
    public void testAcquireIfNotHeld_reference_counted() {
        final PartialWakeLock manager = PartialWakeLock.newInstance(getContext(), getName(), true);

        manager.acquireLockIfNotHeld();
        assertEquals(1, manager.getReferenceCount());
        assertTrue(manager.isHeld());

        manager.releaseLock();
        assertFalse(manager.isHeld());

        manager.acquireLock();
        manager.acquireLockIfNotHeld();
        assertEquals(1, manager.getReferenceCount());

        manager.releaseLock();
        assertFalse(manager.isHeld());
    }

    @SmallTest
    public void testToString() {
        assertNotNull(PartialWakeLock.newInstance(getContext(), getName(), false).toString());
    }
}
