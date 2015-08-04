/*
 * Copyright (C) 2014 The Android Open Source Project
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

import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.support.v7.appcompat.R;

/**
 * This class allows us to intercept calls so that we can tint resources (if applicable).
 *
 * @hide
 */
class TintResources extends ResourcesWrapper {

    private final TintManager mTintManager;

    public TintResources(Resources resources, TintManager tintManager) {
        super(resources);
        mTintManager = tintManager;
    }

    /**
     * We intercept this call so that we tint the result (if applicable). This is needed for things
     * like {@link DrawableContainer}s which retrieve their children via this method.
     */
    @Override
    public Drawable getDrawable(int id) throws NotFoundException {
        Drawable d = super.getDrawable(id);
        if (d != null) {
            mTintManager.tintDrawable(id, d);
        }
        return d;
    }
}
