/*
 * Copyright (C) 2013 The Android Open Source Project
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

package android.support.v7.internal.view.menu;

import android.content.Context;
import android.support.v7.internal.view.menu.MenuBuilder;
import android.support.v7.internal.view.menu.MenuBuilder.ItemInvoker;
import android.support.v7.internal.view.menu.MenuView;
import android.support.v7.internal.widget.TintTypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

/**
 * The expanded menu view is a list-like menu with all of the available menu items.  It is opened
 * by the user clicking no the 'More' button on the icon menu view.
 *
 * @hide
 */
public final class ExpandedMenuView extends ListView
        implements ItemInvoker, MenuView, OnItemClickListener {

    private static final int[] TINT_ATTRS = {
            android.R.attr.background,
            android.R.attr.divider
    };

    private MenuBuilder mMenu;

    /** Default animations for this menu */
    private int mAnimations;

    public ExpandedMenuView(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.listViewStyle);
    }

    public ExpandedMenuView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs);
        setOnItemClickListener(this);

        TintTypedArray a = TintTypedArray.obtainStyledAttributes(context, attrs, TINT_ATTRS,
                defStyleAttr, 0);
        if (a.hasValue(0)) {
            setBackgroundDrawable(a.getDrawable(0));
        }
        if (a.hasValue(1)) {
            setDivider(a.getDrawable(1));
        }
        a.recycle();
    }

    @Override
    public void initialize(MenuBuilder menu) {
        mMenu = menu;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        // Clear the cached bitmaps of children
        setChildrenDrawingCacheEnabled(false);
    }

    @Override
    public boolean invokeItem(MenuItemImpl item) {
        return mMenu.performItemAction(item, 0);
    }

    @Override
    @SuppressWarnings("rawtypes")
    public void onItemClick(AdapterView parent, View v, int position, long id) {
        invokeItem((MenuItemImpl) getAdapter().getItem(position));
    }

    @Override
    public int getWindowAnimations() {
        return mAnimations;
    }

}
