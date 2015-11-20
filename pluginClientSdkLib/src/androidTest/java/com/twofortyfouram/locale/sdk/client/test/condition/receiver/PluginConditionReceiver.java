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

package com.twofortyfouram.locale.sdk.client.test.condition.receiver;


import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;

import com.twofortyfouram.locale.sdk.client.receiver.AbstractPluginConditionReceiver;

/**
 * A basic well-formed plug-in Condition receiver.
 */
public final class PluginConditionReceiver extends AbstractPluginConditionReceiver {

    @Override
    protected boolean isBundleValid(@NonNull Bundle bundle) {
        return PluginBundleValues.isBundleValid(bundle);
    }

    @Override
    protected boolean isAsync() {
        return false;
    }

    @Override
    protected int getPluginConditionResult(@NonNull final Context context,
                                           @NonNull final Bundle bundle) {
        return PluginBundleValues.getResultCode(bundle);
    }
}
