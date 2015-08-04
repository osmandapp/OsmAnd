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

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.widget.FrameLayout;

/**
 * @hide
 */
public class FitWindowsFrameLayout extends FrameLayout implements FitWindowsViewGroup {

    private OnFitSystemWindowsListener mListener;

    public FitWindowsFrameLayout(Context context) {
        super(context);
    }

    public FitWindowsFrameLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void setOnFitSystemWindowsListener(OnFitSystemWindowsListener listener) {
        mListener = listener;
    }

    @Override
    protected boolean fitSystemWindows(Rect insets) {
        if (mListener != null) {
            mListener.onFitSystemWindows(insets);
        }
        return super.fitSystemWindows(insets);
    }
}
