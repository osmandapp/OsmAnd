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
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.TypedValue;

/**
 * A class that wraps a {@link android.content.res.TypedArray} and provides the same public API
 * surface. The purpose of this class is so that we can intercept the {@link #getDrawable(int)}
 * call and tint the result.
 *
 * @hide
 */
public class TintTypedArray {

    private final Context mContext;
    private final TypedArray mWrapped;

    private TintManager mTintManager;

    public static TintTypedArray obtainStyledAttributes(Context context, AttributeSet set,
            int[] attrs) {
        TypedArray array = context.obtainStyledAttributes(set, attrs);
        return new TintTypedArray(context, array);
    }

    public static TintTypedArray obtainStyledAttributes(Context context, AttributeSet set,
            int[] attrs, int defStyleAttr, int defStyleRes) {
        TypedArray array = context.obtainStyledAttributes(set, attrs, defStyleAttr, defStyleRes);
        return new TintTypedArray(context, array);
    }

    private TintTypedArray(Context context, TypedArray array) {
        mContext = context;
        mWrapped = array;
    }

    public Drawable getDrawable(int index) {
        if (mWrapped.hasValue(index)) {
            final int resourceId = mWrapped.getResourceId(index, 0);
            if (resourceId != 0) {
                return getTintManager().getDrawable(resourceId);
            }
        }
        return mWrapped.getDrawable(index);
    }

    public int length() {
        return mWrapped.length();
    }

    public int getIndexCount() {
        return mWrapped.getIndexCount();
    }

    public int getIndex(int at) {
        return mWrapped.getIndex(at);
    }

    public Resources getResources() {
        return mWrapped.getResources();
    }

    public CharSequence getText(int index) {
        return mWrapped.getText(index);
    }

    public String getString(int index) {
        return mWrapped.getString(index);
    }

    public String getNonResourceString(int index) {
        return mWrapped.getNonResourceString(index);
    }

    public boolean getBoolean(int index, boolean defValue) {
        return mWrapped.getBoolean(index, defValue);
    }

    public int getInt(int index, int defValue) {
        return mWrapped.getInt(index, defValue);
    }

    public float getFloat(int index, float defValue) {
        return mWrapped.getFloat(index, defValue);
    }

    public int getColor(int index, int defValue) {
        return mWrapped.getColor(index, defValue);
    }

    public ColorStateList getColorStateList(int index) {
        return mWrapped.getColorStateList(index);
    }

    public int getInteger(int index, int defValue) {
        return mWrapped.getInteger(index, defValue);
    }

    public float getDimension(int index, float defValue) {
        return mWrapped.getDimension(index, defValue);
    }

    public int getDimensionPixelOffset(int index, int defValue) {
        return mWrapped.getDimensionPixelOffset(index, defValue);
    }

    public int getDimensionPixelSize(int index, int defValue) {
        return mWrapped.getDimensionPixelSize(index, defValue);
    }

    public int getLayoutDimension(int index, String name) {
        return mWrapped.getLayoutDimension(index, name);
    }

    public int getLayoutDimension(int index, int defValue) {
        return mWrapped.getLayoutDimension(index, defValue);
    }

    public float getFraction(int index, int base, int pbase, float defValue) {
        return mWrapped.getFraction(index, base, pbase, defValue);
    }

    public int getResourceId(int index, int defValue) {
        return mWrapped.getResourceId(index, defValue);
    }

    public CharSequence[] getTextArray(int index) {
        return mWrapped.getTextArray(index);
    }

    public boolean getValue(int index, TypedValue outValue) {
        return mWrapped.getValue(index, outValue);
    }

    public int getType(int index) {
        return mWrapped.getType(index);
    }

    public boolean hasValue(int index) {
        return mWrapped.hasValue(index);
    }

    public TypedValue peekValue(int index) {
        return mWrapped.peekValue(index);
    }

    public String getPositionDescription() {
        return mWrapped.getPositionDescription();
    }

    public void recycle() {
        mWrapped.recycle();
    }

    public int getChangingConfigurations() {
        return mWrapped.getChangingConfigurations();
    }

    public TintManager getTintManager() {
        if (mTintManager == null) {
            mTintManager = (mContext instanceof TintContextWrapper)
                    ? ((TintContextWrapper) mContext).getTintManager()
                    : new TintManager(mContext);
        }
        return mTintManager;
    }

}
