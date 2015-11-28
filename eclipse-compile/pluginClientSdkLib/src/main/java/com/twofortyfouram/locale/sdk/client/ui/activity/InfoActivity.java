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
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.twofortyfouram.locale.sdk.client.R;
import com.twofortyfouram.log.Lumberjack;

import net.jcip.annotations.NotThreadSafe;

/**
 * If the user tries to launch the plug-in via the app store,
 * this will redirect the user to a compatible host app on the device. If a
 * compatible host does not exist, this directs the user to download the host
 * from the app store.
 */
@NotThreadSafe
public final class InfoActivity extends Activity {

    @Override
    public void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Intent i = getLaunchIntent(getApplicationContext(), getPackageManager(),
                getPackageName());

        try {
            startActivity(i);
        } catch (final Exception e) {
            Lumberjack.e("Error starting Activity%s", e); //$NON-NLS-1$
            /*
             * Catch generic exception to handle all sorts of cases that could
             * cause crashes: ActivityNotFoundException, SecurityException, and
             * who knows what else.
             */
        }

        finish();
    }

    @NonNull
    /* package */ static Intent getLaunchIntent(@NonNull final Context context,
                                                @NonNull final PackageManager manager,
                                                @NonNull final String myPackageName) {
        final String compatiblePackage
                = com.twofortyfouram.locale.sdk.client.internal.HostPackageUtil
                .getCompatiblePackage(manager, null);
        if (null != compatiblePackage) {
            final Intent i = manager.getLaunchIntentForPackage(compatiblePackage);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            return i;
        } else {
            return new Intent(Intent.ACTION_VIEW, Uri.parse(context.getString(R.string.com_twofortyfouram_locale_sdk_client_app_store_deep_link_format,
                    "com.twofortyfouram.locale", myPackageName
            ))).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); //$NON-NLS-1$
        }
    }
}
