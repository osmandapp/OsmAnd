/*
 * Copyright (C) 2012 The Android Open Source Project
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

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v4.internal.view.SupportMenuItem;
import android.support.v4.view.ActionProvider;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.view.CollapsibleActionView;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.widget.FrameLayout;

import java.lang.reflect.Method;

/**
 * Wraps a support {@link SupportMenuItem} as a framework {@link android.view.MenuItem}
 * @hide
 */
@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class MenuItemWrapperICS extends BaseMenuWrapper<SupportMenuItem> implements MenuItem {
    static final String LOG_TAG = "MenuItemWrapper";

    // Reflection Method to call setExclusiveCheckable
    private Method mSetExclusiveCheckableMethod;

    MenuItemWrapperICS(Context context, SupportMenuItem object) {
        super(context, object);
    }

    @Override
    public int getItemId() {
        return mWrappedObject.getItemId();
    }

    @Override
    public int getGroupId() {
        return mWrappedObject.getGroupId();
    }

    @Override
    public int getOrder() {
        return mWrappedObject.getOrder();
    }

    @Override
    public MenuItem setTitle(CharSequence title) {
        mWrappedObject.setTitle(title);
        return this;
    }

    @Override
    public MenuItem setTitle(int title) {
        mWrappedObject.setTitle(title);
        return this;
    }

    @Override
    public CharSequence getTitle() {
        return mWrappedObject.getTitle();
    }

    @Override
    public MenuItem setTitleCondensed(CharSequence title) {
        mWrappedObject.setTitleCondensed(title);
        return this;
    }

    @Override
    public CharSequence getTitleCondensed() {
        return mWrappedObject.getTitleCondensed();
    }

    @Override
    public MenuItem setIcon(Drawable icon) {
        mWrappedObject.setIcon(icon);
        return this;
    }

    @Override
    public MenuItem setIcon(int iconRes) {
        mWrappedObject.setIcon(iconRes);
        return this;
    }

    @Override
    public Drawable getIcon() {
        return mWrappedObject.getIcon();
    }

    @Override
    public MenuItem setIntent(Intent intent) {
        mWrappedObject.setIntent(intent);
        return this;
    }

    @Override
    public Intent getIntent() {
        return mWrappedObject.getIntent();
    }

    @Override
    public MenuItem setShortcut(char numericChar, char alphaChar) {
        mWrappedObject.setShortcut(numericChar, alphaChar);
        return this;
    }

    @Override
    public MenuItem setNumericShortcut(char numericChar) {
        mWrappedObject.setNumericShortcut(numericChar);
        return this;
    }

    @Override
    public char getNumericShortcut() {
        return mWrappedObject.getNumericShortcut();
    }

    @Override
    public MenuItem setAlphabeticShortcut(char alphaChar) {
        mWrappedObject.setAlphabeticShortcut(alphaChar);
        return this;
    }

    @Override
    public char getAlphabeticShortcut() {
        return mWrappedObject.getAlphabeticShortcut();
    }

    @Override
    public MenuItem setCheckable(boolean checkable) {
        mWrappedObject.setCheckable(checkable);
        return this;
    }

    @Override
    public boolean isCheckable() {
        return mWrappedObject.isCheckable();
    }

    @Override
    public MenuItem setChecked(boolean checked) {
        mWrappedObject.setChecked(checked);
        return this;
    }

    @Override
    public boolean isChecked() {
        return mWrappedObject.isChecked();
    }

    @Override
    public MenuItem setVisible(boolean visible) {
        return mWrappedObject.setVisible(visible);
    }

    @Override
    public boolean isVisible() {
        return mWrappedObject.isVisible();
    }

    @Override
    public MenuItem setEnabled(boolean enabled) {
        mWrappedObject.setEnabled(enabled);
        return this;
    }

    @Override
    public boolean isEnabled() {
        return mWrappedObject.isEnabled();
    }

    @Override
    public boolean hasSubMenu() {
        return mWrappedObject.hasSubMenu();
    }

    @Override
    public SubMenu getSubMenu() {
        return getSubMenuWrapper(mWrappedObject.getSubMenu());
    }

    @Override
    public MenuItem setOnMenuItemClickListener(OnMenuItemClickListener menuItemClickListener) {
        mWrappedObject.setOnMenuItemClickListener(menuItemClickListener != null ?
                new OnMenuItemClickListenerWrapper(menuItemClickListener) : null);
        return this;
    }

    @Override
    public ContextMenu.ContextMenuInfo getMenuInfo() {
        return mWrappedObject.getMenuInfo();
    }

    @Override
    public void setShowAsAction(int actionEnum) {
        mWrappedObject.setShowAsAction(actionEnum);
    }

    @Override
    public MenuItem setShowAsActionFlags(int actionEnum) {
        mWrappedObject.setShowAsActionFlags(actionEnum);
        return this;
    }

    @Override
    public MenuItem setActionView(View view) {
        if (view instanceof android.view.CollapsibleActionView) {
            view = new CollapsibleActionViewWrapper(view);
        }
        mWrappedObject.setActionView(view);
        return this;
    }

    @Override
    public MenuItem setActionView(int resId) {
        // Make framework menu item inflate the view
        mWrappedObject.setActionView(resId);

        View actionView = mWrappedObject.getActionView();
        if (actionView instanceof android.view.CollapsibleActionView) {
            // If the inflated Action View is support-collapsible, wrap it
            mWrappedObject.setActionView(new CollapsibleActionViewWrapper(actionView));
        }
        return this;
    }

    @Override
    public View getActionView() {
        View actionView = mWrappedObject.getActionView();
        if (actionView instanceof CollapsibleActionViewWrapper) {
            return ((CollapsibleActionViewWrapper) actionView).getWrappedView();
        }
        return actionView;
    }

    @Override
    public MenuItem setActionProvider(android.view.ActionProvider provider) {
        mWrappedObject.setSupportActionProvider(
                provider != null ? createActionProviderWrapper(provider) : null);
        return this;
    }

    @Override
    public android.view.ActionProvider getActionProvider() {
        ActionProvider provider = mWrappedObject.getSupportActionProvider();
        if (provider instanceof ActionProviderWrapper) {
            return ((ActionProviderWrapper) provider).mInner;
        }
        return null;
    }

    @Override
    public boolean expandActionView() {
        return mWrappedObject.expandActionView();
    }

    @Override
    public boolean collapseActionView() {
        return mWrappedObject.collapseActionView();
    }

    @Override
    public boolean isActionViewExpanded() {
        return mWrappedObject.isActionViewExpanded();
    }

    @Override
    public MenuItem setOnActionExpandListener(MenuItem.OnActionExpandListener listener) {
        mWrappedObject.setSupportOnActionExpandListener(listener != null ?
                new OnActionExpandListenerWrapper(listener) : null);
        return this;
    }

    public void setExclusiveCheckable(boolean checkable) {
        try {
            if (mSetExclusiveCheckableMethod == null) {
                mSetExclusiveCheckableMethod = mWrappedObject.getClass()
                        .getDeclaredMethod("setExclusiveCheckable", Boolean.TYPE);
            }
            mSetExclusiveCheckableMethod.invoke(mWrappedObject, checkable);
        } catch (Exception e) {
            Log.w(LOG_TAG, "Error while calling setExclusiveCheckable", e);
        }
    }

    ActionProviderWrapper createActionProviderWrapper(android.view.ActionProvider provider) {
        return new ActionProviderWrapper(mContext, provider);
    }

    private class OnMenuItemClickListenerWrapper extends BaseWrapper<OnMenuItemClickListener>
            implements android.view.MenuItem.OnMenuItemClickListener {

        OnMenuItemClickListenerWrapper(OnMenuItemClickListener object) {
            super(object);
        }

        @Override
        public boolean onMenuItemClick(android.view.MenuItem item) {
            return mWrappedObject.onMenuItemClick(getMenuItemWrapper(item));
        }
    }

    private class OnActionExpandListenerWrapper extends BaseWrapper<MenuItem.OnActionExpandListener>
            implements MenuItemCompat.OnActionExpandListener {

        OnActionExpandListenerWrapper(MenuItem.OnActionExpandListener object) {
            super(object);
        }

        @Override
        public boolean onMenuItemActionExpand(android.view.MenuItem item) {
            return mWrappedObject.onMenuItemActionExpand(getMenuItemWrapper(item));
        }

        @Override
        public boolean onMenuItemActionCollapse(android.view.MenuItem item) {
            return mWrappedObject.onMenuItemActionCollapse(getMenuItemWrapper(item));
        }
    }

    class ActionProviderWrapper extends android.support.v4.view.ActionProvider {
        final android.view.ActionProvider mInner;

        public ActionProviderWrapper(Context context, android.view.ActionProvider inner) {
            super(context);
            mInner = inner;
        }

        @Override
        public View onCreateActionView() {
            return mInner.onCreateActionView();
        }

        @Override
        public boolean onPerformDefaultAction() {
            return mInner.onPerformDefaultAction();
        }

        @Override
        public boolean hasSubMenu() {
            return mInner.hasSubMenu();
        }

        @Override
        public void onPrepareSubMenu(android.view.SubMenu subMenu) {
            mInner.onPrepareSubMenu(getSubMenuWrapper(subMenu));
        }
    }

    /**
     * Wrap a support {@link android.support.v7.view.CollapsibleActionView} into a framework
     * {@link android.view.CollapsibleActionView}.
     */
    static class CollapsibleActionViewWrapper extends FrameLayout
            implements CollapsibleActionView {

        final android.view.CollapsibleActionView mWrappedView;

        CollapsibleActionViewWrapper(View actionView) {
            super(actionView.getContext());
            mWrappedView = (android.view.CollapsibleActionView) actionView;
            addView(actionView);
        }

        @Override
        public void onActionViewExpanded() {
            mWrappedView.onActionViewExpanded();
        }

        @Override
        public void onActionViewCollapsed() {
            mWrappedView.onActionViewCollapsed();
        }

        View getWrappedView() {
            return (View) mWrappedView;
        }
    }
}
