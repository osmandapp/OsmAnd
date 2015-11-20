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

package com.twofortyfouram.spackle.bundle;

import android.content.Intent;
import android.os.Bundle;
import android.test.InstrumentationTestCase;
import android.test.suitebuilder.annotation.SmallTest;

import com.twofortyfouram.assertion.BundleAssertions;
import com.twofortyfouram.test.assertion.MoarAsserts;

/**
 * Tests {@link BundleScrubber}.
 */
public final class BundleScrubberTest extends InstrumentationTestCase {

    @SmallTest
    public static void testNonInstantiable() {
        MoarAsserts.assertNoninstantiable(BundleScrubber.class);
    }

    @SmallTest
    public static void testScrub_intent_null() {
        assertFalse(BundleScrubber.scrub((Intent) null));
    }

    @SmallTest
    public static void testScrub_intent_empty() {
        final Intent intent = new Intent();
        assertNull(intent.getExtras());

        assertFalse(BundleScrubber.scrub(intent));

        assertNull(intent.getExtras());
    }

    @SmallTest
    public static void testScrub_intent_non_empty() {
        final Intent intent = new Intent()
                .putExtra("test_key", "test_value"); //$NON-NLS-1$ //$NON-NLS-2$

        assertFalse(BundleScrubber.scrub(intent));

        BundleAssertions.assertKeyCount(intent.getExtras(), 1);
        BundleAssertions.assertHasString(intent.getExtras(), "test_key",
                "test_value"); //$NON-NLS-1$ //$NON-NLS-2$
    }

    @SmallTest
    public static void testScrub_bundle_null() {
        assertFalse(BundleScrubber.scrub((Bundle) null));
    }

    @SmallTest
    public static void testScrub_bundle_empty() {
        final Bundle bundle = new Bundle();

        assertFalse(BundleScrubber.scrub(bundle));

        BundleAssertions.assertKeyCount(bundle, 0);
    }

    @SmallTest
    public static void testScrub_bundle_non_empty() {
        final Bundle bundle = new Bundle();
        bundle.putString("test_key", "test_value"); //$NON-NLS-1$ //$NON-NLS-2$

        assertFalse(BundleScrubber.scrub(bundle));

        BundleAssertions.assertKeyCount(bundle, 1);
        BundleAssertions
                .assertHasString(bundle, "test_key", "test_value"); //$NON-NLS-1$ //$NON-NLS-2$
    }
}
