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

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.v4.view.TintableBackgroundView;
import android.util.AttributeSet;
import android.widget.ListPopupWindow;
import android.widget.Spinner;

import java.lang.reflect.Field;

/**
 * An tint aware {@link android.widget.Spinner}.
 * <p>
 * This will automatically be used when you use {@link android.widget.Spinner} in your
 * layouts. You should only need to manually use this class when writing custom views.
 */
public class TintSpinner extends Spinner implements TintableBackgroundView {

    private static final int[] TINT_ATTRS = {
            android.R.attr.background,
            android.R.attr.popupBackground
    };

    private TintInfo mBackgroundTint;

    public TintSpinner(Context context) {
        this(context, null);
    }

    public TintSpinner(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.spinnerStyle);
    }

    public TintSpinner(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        if (TintManager.SHOULD_BE_USED) {
            TintTypedArray a = TintTypedArray.obtainStyledAttributes(getContext(), attrs,
                    TINT_ATTRS, defStyleAttr, 0);
            if (a.hasValue(0)) {
                setSupportBackgroundTintList(
                        a.getTintManager().getColorStateList(a.getResourceId(0, -1)));
            }
            if (a.hasValue(1)) {
                final Drawable popupBackground = a.getDrawable(1);
                if (Build.VERSION.SDK_INT >= 16) {
                    setPopupBackgroundDrawable(popupBackground);
                } else if (Build.VERSION.SDK_INT >= 11) {
                    setPopupBackgroundDrawableV11(this, popupBackground);
                }
            }
            a.recycle();
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private static void setPopupBackgroundDrawableV11(Spinner view, Drawable background) {
        try {
            Field popupField = Spinner.class.getDeclaredField("mPopup");
            popupField.setAccessible(true);

            Object popup = popupField.get(view);

            if (popup instanceof ListPopupWindow) {
                ((ListPopupWindow) popup).setBackgroundDrawable(background);
            }
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * This should be accessed via
     * {@link android.support.v4.view.ViewCompat#setBackgroundTintList(android.view.View,
     * android.content.res.ColorStateList)}
     *
     * @hide
     */
    @Override
    public void setSupportBackgroundTintList(@Nullable ColorStateList tint) {
        if (mBackgroundTint == null && tint != null) {
            mBackgroundTint = new TintInfo();
        }
        mBackgroundTint.mTintList = tint;
        applySupportBackgroundTint();
    }

    /**
     * This should be accessed via
     * {@link android.support.v4.view.ViewCompat#getBackgroundTintList(android.view.View)}
     *
     * @hide
     */
    @Override
    @Nullable
    public ColorStateList getSupportBackgroundTintList() {
        return mBackgroundTint != null ? mBackgroundTint.mTintList : null;
    }

    /**
     * This should be accessed via
     * {@link android.support.v4.view.ViewCompat#setBackgroundTintMode(android.view.View, android.graphics.PorterDuff.Mode)}
     *
     * @hide
     */
    @Override
    public void setSupportBackgroundTintMode(@Nullable PorterDuff.Mode tintMode) {
        if (mBackgroundTint == null) {
            mBackgroundTint = new TintInfo();
        }
        mBackgroundTint.mTintMode = tintMode;
        applySupportBackgroundTint();
    }

    /**
     * This should be accessed via
     * {@link android.support.v4.view.ViewCompat#getBackgroundTintMode(android.view.View)}
     *
     * @hide
     */
    @Override
    @Nullable
    public PorterDuff.Mode getSupportBackgroundTintMode() {
        return mBackgroundTint != null ? mBackgroundTint.mTintMode : null;
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        applySupportBackgroundTint();
    }

    private void applySupportBackgroundTint() {
        if (getBackground() != null && mBackgroundTint != null) {
            TintManager.tintViewBackground(this, mBackgroundTint);
        }
    }

}
