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

package com.twofortyfouram.locale.sdk.client.ui.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.test.ActivityInstrumentationTestCase2;
import android.test.UiThreadTest;
import android.test.suitebuilder.annotation.MediumTest;

import com.twofortyfouram.assertion.Assertions;
import com.twofortyfouram.assertion.BundleAssertions;
import com.twofortyfouram.locale.sdk.client.test.condition.ui.activity.PluginActivityImpl;
import com.twofortyfouram.locale.sdk.client.test.condition.ui.activity.PluginBundleValues;
import com.twofortyfouram.spackle.bundle.BundleComparer;
import com.twofortyfouram.test.ui.activity.ActivityTestUtil;

/**
 * Superclass for Activity unit tests that provides facilities to make testing
 * easier.
 */
public final class PluginActivityImplTest extends
        ActivityInstrumentationTestCase2<PluginActivityImpl> {

    public PluginActivityImplTest() {
        super(PluginActivityImpl.class);
    }

    /**
     * Setup that executes before every test case
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();

        /*
         * Perform test case specific initialization. This is required to be set
         * up here because setActivityIntent() has no effect inside a method
         * annotated with @UiThreadTest
         */

        final Context context = getInstrumentation().getContext();

        if ("testNewCondition_cancel_because_null_bundle".equals(getName())) { //$NON-NLS-1$
            setActivityIntent(getDefaultStartIntent(PluginType.CONDITION));
        } else if ("testNewSetting_cancel_because_null_bundle".equals(getName())) { //$NON-NLS-1$
            setActivityIntent(getDefaultStartIntent(PluginType.SETTING));
        } else if ("testNewCondition_save".equals(getName())) { //$NON-NLS-1$
            setActivityIntent(getDefaultStartIntent(PluginType.CONDITION));
        } else if ("testNewSetting_save".equals(getName())) { //$NON-NLS-1$
            setActivityIntent(getDefaultStartIntent(PluginType.SETTING));
        } else if ("testOldCondition_save_bundle_and_blurb_changed".equals(getName())) { //$NON-NLS-1$
            final Bundle bundle = PluginBundleValues
                    .generateBundle(context, "some_old_test_value");  //$NON-NLS-1$

            final Intent i = getDefaultStartIntent(PluginType.CONDITION);
            i.putExtra(com.twofortyfouram.locale.api.Intent.EXTRA_BUNDLE, bundle);
            i.putExtra(com.twofortyfouram.locale.api.Intent.EXTRA_STRING_BLURB,
                    "Some old blurb"); //$NON-NLS-1$

            setActivityIntent(i);
        } else if ("testOldCondition_save_bundle_changed".equals(getName())) { //$NON-NLS-1$
            final Bundle bundle = PluginBundleValues
                    .generateBundle(context, "some_old_test_value");  //$NON-NLS-1$

            final Intent i = getDefaultStartIntent(PluginType.CONDITION);
            i.putExtra(com.twofortyfouram.locale.api.Intent.EXTRA_BUNDLE, bundle);
            i.putExtra(com.twofortyfouram.locale.api.Intent.EXTRA_STRING_BLURB,
                    "Some old blurb"); //$NON-NLS-1$

            setActivityIntent(i);
        } else if ("testOldCondition_save_blurb_changed".equals(getName())) { //$NON-NLS-1$
            final Bundle bundle = PluginBundleValues
                    .generateBundle(context, "some_old_test_value");  //$NON-NLS-1$

            final Intent i = getDefaultStartIntent(PluginType.CONDITION);
            i.putExtra(com.twofortyfouram.locale.api.Intent.EXTRA_BUNDLE, bundle);
            i.putExtra(com.twofortyfouram.locale.api.Intent.EXTRA_STRING_BLURB,
                    "Some old blurb"); //$NON-NLS-1$

            setActivityIntent(i);
        } else if ("testOldSetting_save_bundle_and_blurb_changed".equals(getName())) { //$NON-NLS-1$
            final Bundle bundle = PluginBundleValues
                    .generateBundle(context, "some_old_test_value");  //$NON-NLS-1$

            final Intent i = getDefaultStartIntent(PluginType.SETTING);
            i.putExtra(com.twofortyfouram.locale.api.Intent.EXTRA_BUNDLE, bundle);
            i.putExtra(com.twofortyfouram.locale.api.Intent.EXTRA_STRING_BLURB,
                    "Some old blurb"); //$NON-NLS-1$

            setActivityIntent(i);
        } else if ("testOldSetting_save_bundle_changed".equals(getName())) { //$NON-NLS-1$
            final Bundle bundle = PluginBundleValues
                    .generateBundle(context, "some_old_test_value");  //$NON-NLS-1$

            final Intent i = getDefaultStartIntent(PluginType.SETTING);
            i.putExtra(com.twofortyfouram.locale.api.Intent.EXTRA_BUNDLE, bundle);
            i.putExtra(com.twofortyfouram.locale.api.Intent.EXTRA_STRING_BLURB,
                    "Some old blurb"); //$NON-NLS-1$

            setActivityIntent(i);
        } else if ("testOldSetting_save_blurb_changed".equals(getName())) { //$NON-NLS-1$
            final Bundle bundle = PluginBundleValues
                    .generateBundle(context, "some_old_test_value");  //$NON-NLS-1$

            final Intent i = getDefaultStartIntent(PluginType.SETTING);
            i.putExtra(com.twofortyfouram.locale.api.Intent.EXTRA_BUNDLE, bundle);
            i.putExtra(com.twofortyfouram.locale.api.Intent.EXTRA_STRING_BLURB,
                    "Some old blurb"); //$NON-NLS-1$

            setActivityIntent(i);
        } else if ("testOldCondition_diffing_cancel".equals(getName())) { //$NON-NLS-1$
            final Bundle bundle = PluginBundleValues
                    .generateBundle(context, "some_old_test_value");  //$NON-NLS-1$

            final Intent i = getDefaultStartIntent(PluginType.CONDITION);
            i.putExtra(com.twofortyfouram.locale.api.Intent.EXTRA_BUNDLE, bundle);
            i.putExtra(com.twofortyfouram.locale.api.Intent.EXTRA_STRING_BLURB,
                    "Some old blurb"); //$NON-NLS-1$

            setActivityIntent(i);
        } else if ("testOldSetting_diffing_cancel".equals(getName())) { //$NON-NLS-1$
            final Bundle bundle = PluginBundleValues
                    .generateBundle(context, "some_old_test_value");  //$NON-NLS-1$

            final Intent i = getDefaultStartIntent(PluginType.SETTING);
            i.putExtra(com.twofortyfouram.locale.api.Intent.EXTRA_BUNDLE, bundle);
            i.putExtra(com.twofortyfouram.locale.api.Intent.EXTRA_STRING_BLURB,
                    "Some old blurb"); //$NON-NLS-1$

            setActivityIntent(i);
        } else if ("testOldCondition_bad_bundle".equals(getName())) { //$NON-NLS-1$
            final Bundle bundle = PluginBundleValues
                    .generateBundle(context, "some_old_test_value");  //$NON-NLS-1$
            bundle.putString("extra_key", "extra_value");

            final Intent i = getDefaultStartIntent(PluginType.SETTING);
            i.putExtra(com.twofortyfouram.locale.api.Intent.EXTRA_BUNDLE, bundle);
            i.putExtra(com.twofortyfouram.locale.api.Intent.EXTRA_STRING_BLURB,
                    "Some old blurb"); //$NON-NLS-1$

            setActivityIntent(i);
        } else if ("testOldSetting_bad_bundle".equals(getName())) { //$NON-NLS-1$
            final Bundle bundle = PluginBundleValues
                    .generateBundle(context, "some_old_test_value");  //$NON-NLS-1$
            bundle.putString("extra_key", "extra_value");

            final Intent i = getDefaultStartIntent(PluginType.SETTING);
            i.putExtra(com.twofortyfouram.locale.api.Intent.EXTRA_BUNDLE, bundle);
            i.putExtra(com.twofortyfouram.locale.api.Intent.EXTRA_STRING_BLURB,
                    "Some old blurb"); //$NON-NLS-1$

            setActivityIntent(i);
        }

        getActivity();
    }

    @MediumTest
    @UiThreadTest
    public void testNewCondition_cancel_because_null_bundle() {
        final PluginActivityImpl activity = getActivity();

        assertIsBundleValidCount(0);
        assertOnPostCreateWithPreviousBundleCount(0);
        assertNull(activity.getPreviousBundle());

        activity.finish();

        assertGetResultBundleCount(1);
        assertGetBlurbCount(0);

        assertActivityResult(Activity.RESULT_CANCELED, null, null);
    }

    @MediumTest
    @UiThreadTest
    public void testNewSetting_cancel_because_null_bundle() {
        final PluginActivityImpl activity = getActivity();

        assertIsBundleValidCount(0);
        assertOnPostCreateWithPreviousBundleCount(0);
        assertNull(activity.getPreviousBundle());

        activity.finish();

        assertGetResultBundleCount(1);
        assertGetBlurbCount(0);

        assertActivityResult(Activity.RESULT_CANCELED, null, null);
    }

    @MediumTest
    @UiThreadTest
    public void testNewCondition_save() {
        final PluginActivityImpl activity = getActivity();

        assertIsBundleValidCount(0);
        assertOnPostCreateWithPreviousBundleCount(0);
        assertNull(activity.getPreviousBundle());

        final Bundle bundle = PluginBundleValues.generateBundle(getInstrumentation().getContext(),
                "some_new_test_value");  //$NON-NLS-1$

        final String blurb = "Some new blurb"; //$NON-NLS-1$

        setActivityBundleAndBlurb(bundle, blurb);

        activity.finish();

        assertGetResultBundleCount(1);
        assertGetBlurbCount(1);

        assertActivityResult(Activity.RESULT_OK, bundle, blurb);
    }

    @MediumTest
    @UiThreadTest
    public void testNewSetting_save() {
        final PluginActivityImpl activity = getActivity();

        assertIsBundleValidCount(0);
        assertOnPostCreateWithPreviousBundleCount(0);
        assertNull(activity.getPreviousBundle());

        final Bundle bundle = PluginBundleValues.generateBundle(getInstrumentation().getContext(),
                "some_new_test_value");  //$NON-NLS-1$

        final String blurb = "Some new blurb"; //$NON-NLS-1$

        setActivityBundleAndBlurb(bundle, blurb);

        activity.finish();

        assertGetResultBundleCount(1);
        assertGetBlurbCount(1);

        assertActivityResult(Activity.RESULT_OK, bundle, blurb);
    }

    @MediumTest
    @UiThreadTest
    public void testOldCondition_save_bundle_and_blurb_changed() {
        final PluginActivityImpl activity = getActivity();

        assertIsBundleValidCount(2);
        assertOnPostCreateWithPreviousBundleCount(1);

        final Bundle oldBundle = PluginBundleValues
                .generateBundle(getInstrumentation().getContext(),
                        "some_old_test_value"); //$NON-NLS-1$
        assertTrue(BundleComparer.areBundlesEqual(oldBundle, activity.getPreviousBundle()));
        assertEquals("Some old blurb", activity.getPreviousBlurb()); //$NON-NLS-1$

        final Bundle newBundle = PluginBundleValues
                .generateBundle(getInstrumentation().getContext(), "some_new_test_value"); //$NON-NLS-1$

        final String newBlurb = "Some new blurb"; //$NON-NLS-1$

        setActivityBundleAndBlurb(newBundle, newBlurb);

        activity.finish();

        assertGetResultBundleCount(1);
        assertGetBlurbCount(1);

        assertActivityResult(Activity.RESULT_OK, newBundle, newBlurb);
    }

    @MediumTest
    @UiThreadTest
    public void testOldCondition_save_bundle_changed() {
        final PluginActivityImpl activity = getActivity();

        assertIsBundleValidCount(2);
        assertOnPostCreateWithPreviousBundleCount(1);

        final Bundle oldBundle = PluginBundleValues
                .generateBundle(getInstrumentation().getContext(),
                        "some_old_test_value"); //$NON-NLS-1$
        assertTrue(BundleComparer.areBundlesEqual(oldBundle, activity.getPreviousBundle()));

        final String oldBlurb = "Some old blurb"; //$NON-NLS-1$
        assertEquals(oldBlurb, activity.getPreviousBlurb()); //$NON-NLS-1$

        final Bundle newBundle = PluginBundleValues
                .generateBundle(getInstrumentation().getContext(), "some_new_test_value"); //$NON-NLS-1$

        setActivityBundleAndBlurb(newBundle, oldBlurb);

        activity.finish();

        assertGetResultBundleCount(1);
        assertGetBlurbCount(1);

        assertActivityResult(Activity.RESULT_OK, newBundle, oldBlurb);
    }

    @MediumTest
    @UiThreadTest
    public void testOldCondition_save_blurb_changed() {
        final PluginActivityImpl activity = getActivity();

        assertIsBundleValidCount(2);
        assertOnPostCreateWithPreviousBundleCount(1);

        final Bundle oldBundle = PluginBundleValues
                .generateBundle(getInstrumentation().getContext(),
                        "some_old_test_value"); //$NON-NLS-1$
        assertTrue(BundleComparer.areBundlesEqual(oldBundle, activity.getPreviousBundle()));
        assertEquals("Some old blurb", activity.getPreviousBlurb()); //$NON-NLS-1$

        final String newBlurb = "Some new blurb"; //$NON-NLS-1$

        setActivityBundleAndBlurb(oldBundle, newBlurb);

        activity.finish();

        assertGetResultBundleCount(1);
        assertGetBlurbCount(1);

        assertActivityResult(Activity.RESULT_OK, oldBundle, newBlurb);
    }

    @MediumTest
    @UiThreadTest
    public void testOldSetting_save_bundle_and_blurb_changed() {
        final PluginActivityImpl activity = getActivity();

        assertIsBundleValidCount(2);
        assertOnPostCreateWithPreviousBundleCount(1);

        final Bundle oldBundle = PluginBundleValues
                .generateBundle(getInstrumentation().getContext(),
                        "some_old_test_value"); //$NON-NLS-1$
        assertTrue(BundleComparer.areBundlesEqual(oldBundle, activity.getPreviousBundle()));
        assertEquals("Some old blurb", activity.getPreviousBlurb()); //$NON-NLS-1$

        final Bundle newBundle = PluginBundleValues
                .generateBundle(getInstrumentation().getContext(), "some_new_test_value"); //$NON-NLS-1$

        final String newBlurb = "Some new blurb"; //$NON-NLS-1$

        setActivityBundleAndBlurb(newBundle, newBlurb);

        activity.finish();

        assertGetResultBundleCount(1);
        assertGetBlurbCount(1);

        assertActivityResult(Activity.RESULT_OK, newBundle, newBlurb);
    }

    @MediumTest
    @UiThreadTest
    public void testOldSetting_save_bundle_changed() {
        final PluginActivityImpl activity = getActivity();

        assertIsBundleValidCount(2);
        assertOnPostCreateWithPreviousBundleCount(1);

        final Bundle oldBundle = PluginBundleValues
                .generateBundle(getInstrumentation().getContext(),
                        "some_old_test_value"); //$NON-NLS-1$
        assertTrue(BundleComparer.areBundlesEqual(oldBundle, activity.getPreviousBundle()));

        final String oldBlurb = "Some old blurb"; //$NON-NLS-1$
        assertEquals(oldBlurb, activity.getPreviousBlurb()); //$NON-NLS-1$

        final Bundle newBundle = PluginBundleValues
                .generateBundle(getInstrumentation().getContext(), "some_new_test_value"); //$NON-NLS-1$

        setActivityBundleAndBlurb(newBundle, oldBlurb);

        activity.finish();

        assertGetResultBundleCount(1);
        assertGetBlurbCount(1);

        assertActivityResult(Activity.RESULT_OK, newBundle, oldBlurb);
    }

    @MediumTest
    @UiThreadTest
    public void testOldSetting_save_blurb_changed() {
        final PluginActivityImpl activity = getActivity();

        assertIsBundleValidCount(2);
        assertOnPostCreateWithPreviousBundleCount(1);

        final Bundle oldBundle = PluginBundleValues
                .generateBundle(getInstrumentation().getContext(),
                        "some_old_test_value"); //$NON-NLS-1$
        assertTrue(BundleComparer.areBundlesEqual(oldBundle, activity.getPreviousBundle()));
        assertEquals("Some old blurb", activity.getPreviousBlurb()); //$NON-NLS-1$

        final String newBlurb = "Some new blurb"; //$NON-NLS-1$

        setActivityBundleAndBlurb(oldBundle, newBlurb);

        activity.finish();

        assertGetResultBundleCount(1);
        assertGetBlurbCount(1);

        assertActivityResult(Activity.RESULT_OK, oldBundle, newBlurb);
    }

    @MediumTest
    @UiThreadTest
    public void testOldCondition_diffing_cancel() {
        final PluginActivityImpl activity = getActivity();

        assertIsBundleValidCount(2);
        assertOnPostCreateWithPreviousBundleCount(1);

        final Bundle oldBundle = PluginBundleValues
                .generateBundle(getInstrumentation().getContext(),
                        "some_old_test_value"); //$NON-NLS-1$
        assertTrue(BundleComparer.areBundlesEqual(oldBundle, activity.getPreviousBundle()));
        assertEquals("Some old blurb", activity.getPreviousBlurb()); //$NON-NLS-1$

        setActivityBundleAndBlurb(oldBundle, "Some old blurb"); //$NON-NLS-1$

        activity.finish();

        assertGetResultBundleCount(1);
        assertGetBlurbCount(1);

        assertActivityResult(Activity.RESULT_CANCELED, null, null);
    }

    @MediumTest
    @UiThreadTest
    public void testOldSetting_diffing_cancel() {
        final PluginActivityImpl activity = getActivity();

        assertIsBundleValidCount(2);
        assertOnPostCreateWithPreviousBundleCount(1);

        final Bundle oldBundle = PluginBundleValues
                .generateBundle(getInstrumentation().getContext(),
                        "some_old_test_value"); //$NON-NLS-1$
        assertTrue(BundleComparer.areBundlesEqual(oldBundle, activity.getPreviousBundle()));
        assertEquals("Some old blurb", activity.getPreviousBlurb()); //$NON-NLS-1$

        setActivityBundleAndBlurb(oldBundle, "Some old blurb"); //$NON-NLS-1$

        activity.finish();

        assertGetResultBundleCount(1);
        assertGetBlurbCount(1);

        assertActivityResult(Activity.RESULT_CANCELED, null, null);
    }

    @MediumTest
    @UiThreadTest
    public void testOldCondition_bad_bundle() {
        final PluginActivityImpl activity = getActivity();

        assertIsBundleValidCount(2);
        assertOnPostCreateWithPreviousBundleCount(0); // This is key for this test!

        activity.finish();

        assertGetResultBundleCount(1);
        assertGetBlurbCount(0);

        assertActivityResult(Activity.RESULT_CANCELED, null, null);
    }

    @MediumTest
    @UiThreadTest
    public void testOldSetting_bad_bundle() {
        final PluginActivityImpl activity = getActivity();

        assertIsBundleValidCount(2);
        assertOnPostCreateWithPreviousBundleCount(0); // This is key for this test!

        activity.finish();

        assertGetResultBundleCount(1);
        assertGetBlurbCount(0);

        assertActivityResult(Activity.RESULT_CANCELED, null, null);
    }

    /**
     * Executes a runnable on the main thread. This method works even if the
     * current thread is already the main thread.
     *
     * @param runnable to execute.
     */
    protected final void autoSyncRunnable(final Runnable runnable) {
        if (Looper.getMainLooper() == Looper.myLooper()) {
            runnable.run();
        } else {
            getInstrumentation().runOnMainSync(runnable);
            getInstrumentation().waitForIdleSync();
        }
    }

    private void setActivityBundleAndBlurb(@Nullable final Bundle bundle,
                                           @Nullable final String blurb) {
        PluginActivityImpl activity = getActivity();
        activity.mBundle = bundle;
        activity.mBlurb = blurb;
    }

    /**
     * Asserts the Activity result is correct.
     * <p/>
     * {@link Activity#finish()} must be called prior to calling this method.
     *
     * @param bundle The bundle to verify exists. Null indicates that no bundle
     *               should be present (not that a null bundle should be present).
     * @param blurb  The blurb to verify exists. Null indicates that no blurb
     *               should be present (not that a null blurb should be present).
     */
    private void assertActivityResult(final int resultCode, @Nullable final Bundle bundle,
                                      @Nullable final String blurb) {
        final Activity activity = getActivity();

        assertEquals(resultCode, ActivityTestUtil.getActivityResultCodeSync(getInstrumentation(), activity));

        if (Activity.RESULT_OK == resultCode) {
            final Intent result = ActivityTestUtil.getActivityResultDataSync(getInstrumentation(), activity);
            assertNotNull(result);

            final Bundle extras = result.getExtras();
            BundleAssertions.assertKeyCount(extras, 2);

            BundleAssertions.assertHasString(extras,
                    com.twofortyfouram.locale.api.Intent.EXTRA_STRING_BLURB, blurb);

            final Bundle pluginBundle = extras
                    .getBundle(com.twofortyfouram.locale.api.Intent.EXTRA_BUNDLE);
            assertTrue(BundleComparer.areBundlesEqual(bundle, pluginBundle));
        } else if (Activity.RESULT_CANCELED == resultCode) {
            assertNull(ActivityTestUtil.getActivityResultDataSync(getInstrumentation(), activity));
        }
    }

    /**
     * @param expectedCount Expected number of calls to
     *                      {@link AbstractPluginActivity#getResultBlurb(android.os.Bundle)}}.
     */
    private void assertGetBlurbCount(final int expectedCount) {
        final PluginActivityImpl activity = getActivity();

        assertEquals(expectedCount, activity.mGetBlurbCount.get());
    }

    /**
     * @param expectedCount Expected number of calls to
     *                      {@link AbstractPluginActivity#getResultBundle()}.
     */
    private void assertGetResultBundleCount(final int expectedCount) {
        final PluginActivityImpl activity = getActivity();

        assertEquals(expectedCount, activity.mGetResultBundleCount.get());
    }

    /**
     * @param expectedCount Expected number of calls to
     *                      {@link AbstractPluginActivity#isBundleValid(android.os.Bundle)}}
     *                      .
     */
    private void assertIsBundleValidCount(final int expectedCount) {
        final PluginActivityImpl activity = getActivity();

        assertEquals(expectedCount, activity.mIsBundleValidCount.get());
    }

    /**
     * @param expectedCount Expected number of calls to
     *                      {@link AbstractPluginActivity#onPostCreateWithPreviousResult(android.os.Bundle, String)}}
     *                      .
     */
    private void assertOnPostCreateWithPreviousBundleCount(final int expectedCount) {
        final PluginActivityImpl activity = getActivity();

        assertEquals(expectedCount, activity.mOnPostCreateWithPreviousBundleCount.get());
    }

    /**
     * @param type Plug-in type.
     * @return The default Intent to start the plug-in Activity. The Intent will
     * contain
     * {@link com.twofortyfouram.locale.api.Intent#EXTRA_STRING_BREADCRUMB}
     * .
     */
    @NonNull
    private static Intent getDefaultStartIntent(@NonNull final PluginType type) {
        Assertions.assertNotNull(type, "type"); //$NON-NLS-1$
        final Intent i = new Intent(type.getActivityIntentAction());

        i.putExtra(com.twofortyfouram.locale.api.Intent.EXTRA_STRING_BREADCRUMB,
                "Edit Situation"); //$NON-NLS-1$

        return i;
    }

}
