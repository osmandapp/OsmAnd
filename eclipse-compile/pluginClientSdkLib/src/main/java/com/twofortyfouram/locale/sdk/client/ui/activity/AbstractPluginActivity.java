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
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.twofortyfouram.locale.sdk.client.internal.PluginActivityDelegate;

import net.jcip.annotations.NotThreadSafe;

/**
 * <p>
 * Implements the basic behaviors of a "Edit" activity for a
 * plug-in, handling the Intent protocol for storing and retrieving the plug-in's data.
 * Recall that a plug-in Activity more or less saves a Bundle and a String blurb via the Intent
 * extras {@link com.twofortyfouram.locale.api.Intent#EXTRA_BUNDLE EXTRA_BUNDLE} and {@link
 * com.twofortyfouram.locale.api.Intent#EXTRA_STRING_BLURB EXTRA_STRING_BLURB}.
 * Those extras represent the configured plug-in, so this Activity helps plug-ins store and
 * retrieve
 * those
 * extras while abstracting the actual Intent protocol.
 * </p>
 * <p>
 * The Activity can be started in one of two states:
 * <ul>
 * <li>New plug-in instance: The Activity's Intent will not contain
 * {@link com.twofortyfouram.locale.api.Intent#EXTRA_BUNDLE EXTRA_BUNDLE}.</li>
 * <li>Old plug-in instance: The Activity's Intent will contain
 * {@link com.twofortyfouram.locale.api.Intent#EXTRA_BUNDLE EXTRA_BUNDLE} and {@link
 * com.twofortyfouram.locale.api.Intent#EXTRA_STRING_BLURB EXTRA_STRING_BLURB} from a previously
 * saved plug-in instance that the user is editing. The previously saved Bundle
 * and blurb can be retrieved at any time via {@link #getPreviousBundle()} and
 * {@link #getPreviousBlurb()}. These will also be delivered via
 * {@link #onPostCreateWithPreviousResult(Bundle, String)} during the
 * Activity's {@link #onPostCreate(Bundle)} phase when the Activity is first
 * created.</li>
 * </ul>
 * <p>During
 * the Activity's {@link #finish()} lifecycle callback, this class will call {@link
 * #getResultBundle()} and {@link #getResultBlurb(android.os.Bundle)}, which should return the
 * Bundle and blurb data the Activity would like to save back to the host.
 * </p>
 * <p>
 * Note that all of these behaviors only apply if the Intent
 * starting the Activity is one of the plug-in "edit" Intent actions.
 * </p>
 *
 * @see com.twofortyfouram.locale.api.Intent#ACTION_EDIT_CONDITION ACTION_EDIT_CONDITION
 * @see com.twofortyfouram.locale.api.Intent#ACTION_EDIT_SETTING ACTION_EDIT_SETTING
 */
@NotThreadSafe
public abstract class AbstractPluginActivity extends Activity implements IPluginActivity {

    /**
     * Flag boolean that can be set prior to calling {@link #finish()} to control whether the
     * Activity
     * attempts to save a result back to the host.  Typically this is only set to true after an
     * explicit user interaction to abort editing the plug-in, such as tapping a "cancel" button.
     */
    /*
     * There is no need to save/restore this field's state.
     */
    protected boolean mIsCancelled = false;

    @NonNull
    private final PluginActivityDelegate<AbstractPluginActivity> mPluginActivityDelegate = new PluginActivityDelegate<>();

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mPluginActivityDelegate.onCreate(this, savedInstanceState);
    }

    @Override
    protected void onPostCreate(@Nullable final Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        mPluginActivityDelegate.onPostCreate(this, savedInstanceState);
    }

    @Override
    public void finish() {
        mPluginActivityDelegate.finish(this, mIsCancelled);

        /*
         * Super call must come after the Activity result is set. If it comes
         * first, then the Activity result will be lost.
         */
        super.finish();
    }

    /**
     * @return The {@link com.twofortyfouram.locale.api.Intent#EXTRA_BUNDLE EXTRA_BUNDLE} that was
     * previously saved to the host and subsequently passed back to this Activity for further
     * editing.  Internally, this method relies on {@link #isBundleValid(android.os.Bundle)}.  If
     * the bundle exists but is not valid, this method will return null.
     */
    @Nullable
    public final Bundle getPreviousBundle() {
        return mPluginActivityDelegate.getPreviousBundle(this);
    }

    /**
     * @return The {@link com.twofortyfouram.locale.api.Intent#EXTRA_STRING_BLURB
     * EXTRA_STRING_BLURB} that was
     * previously saved to the host and subsequently passed back to this Activity for further
     * editing.
     */
    @Nullable
    public final String getPreviousBlurb() {
        return mPluginActivityDelegate.getPreviousBlurb(this);
    }
}
