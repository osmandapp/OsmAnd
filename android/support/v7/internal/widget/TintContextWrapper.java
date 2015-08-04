/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.support.v7.internal.widget;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.Resources;

/**
 * A {@link android.content.ContextWrapper} which returns a tint-aware
 * {@link android.content.res.Resources} instance from {@link #getResources()}.
 *
 * @hide
 */
class TintContextWrapper extends ContextWrapper {

    private final TintManager mTintManager;

    public static Context wrap(Context context) {
        if (!(context instanceof TintContextWrapper)) {
            context = new TintContextWrapper(context);
        }
        return context;
    }

    TintContextWrapper(Context base) {
        super(base);
        mTintManager = new TintManager(base);
    }

    @Override
    public Resources getResources() {
        return mTintManager.getResources();
    }

    final TintManager getTintManager() {
        return mTintManager;
    }
}