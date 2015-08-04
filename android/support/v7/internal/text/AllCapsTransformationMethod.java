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

package android.support.v7.internal.text;

import android.content.Context;
import android.graphics.Rect;
import android.text.method.TransformationMethod;
import android.view.View;

import java.util.Locale;

/**
 * @hide
 */
public class AllCapsTransformationMethod implements TransformationMethod {
    private Locale mLocale;

    public AllCapsTransformationMethod(Context context) {
        mLocale = context.getResources().getConfiguration().locale;
    }

    @Override
    public CharSequence getTransformation(CharSequence source, View view) {
        return source != null ? source.toString().toUpperCase(mLocale) : null;
    }

    @Override
    public void onFocusChanged(View view, CharSequence sourceText, boolean focused,
            int direction, Rect previouslyFocusedRect) {
    }
}