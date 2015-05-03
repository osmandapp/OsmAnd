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

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.support.v4.internal.view.SupportMenu;
import android.support.v4.internal.view.SupportMenuItem;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;

/**
 * Wraps a support {@link SupportMenu} as a framework {@link android.view.Menu}
 * @hide
 */
class MenuWrapperICS extends BaseMenuWrapper<SupportMenu> implements Menu {

    MenuWrapperICS(Context context, SupportMenu object) {
        super(context, object);
    }

    @Override
    public MenuItem add(CharSequence title) {
        return getMenuItemWrapper(mWrappedObject.add(title));
    }

    @Override
    public MenuItem add(int titleRes) {
        return getMenuItemWrapper(mWrappedObject.add(titleRes));
    }

    @Override
    public MenuItem add(int groupId, int itemId, int order, CharSequence title) {
        return getMenuItemWrapper(mWrappedObject.add(groupId, itemId, order, title));
    }

    @Override
    public MenuItem add(int groupId, int itemId, int order, int titleRes) {
        return getMenuItemWrapper(mWrappedObject.add(groupId, itemId, order, titleRes));
    }

    @Override
    public SubMenu addSubMenu(CharSequence title) {
        return getSubMenuWrapper(mWrappedObject.addSubMenu(title));
    }

    @Override
    public SubMenu addSubMenu(int titleRes) {
        return getSubMenuWrapper(mWrappedObject.addSubMenu(titleRes));
    }

    @Override
    public SubMenu addSubMenu(int groupId, int itemId, int order, CharSequence title) {
        return getSubMenuWrapper(mWrappedObject.addSubMenu(groupId, itemId, order, title));
    }

    @Override
    public SubMenu addSubMenu(int groupId, int itemId, int order, int titleRes) {
        return getSubMenuWrapper(
                mWrappedObject.addSubMenu(groupId, itemId, order, titleRes));
    }

    @Override
    public int addIntentOptions(int groupId, int itemId, int order, ComponentName caller,
            Intent[] specifics, Intent intent, int flags, MenuItem[] outSpecificItems) {
        android.view.MenuItem[] items = null;
        if (outSpecificItems != null) {
            items = new android.view.MenuItem[outSpecificItems.length];
        }

        int result = mWrappedObject
                .addIntentOptions(groupId, itemId, order, caller, specifics, intent, flags, items);

        if (items != null) {
            for (int i = 0, z = items.length; i < z; i++) {
                outSpecificItems[i] = getMenuItemWrapper(items[i]);
            }
        }

        return result;
    }

    @Override
    public void removeItem(int id) {
        internalRemoveItem(id);
        mWrappedObject.removeItem(id);
    }

    @Override
    public void removeGroup(int groupId) {
        internalRemoveGroup(groupId);
        mWrappedObject.removeGroup(groupId);
    }

    @Override
    public void clear() {
        internalClear();
        mWrappedObject.clear();
    }

    @Override
    public void setGroupCheckable(int group, boolean checkable, boolean exclusive) {
        mWrappedObject.setGroupCheckable(group, checkable, exclusive);
    }

    @Override
    public void setGroupVisible(int group, boolean visible) {
        mWrappedObject.setGroupVisible(group, visible);
    }

    @Override
    public void setGroupEnabled(int group, boolean enabled) {
        mWrappedObject.setGroupEnabled(group, enabled);
    }

    @Override
    public boolean hasVisibleItems() {
        return mWrappedObject.hasVisibleItems();
    }

    @Override
    public MenuItem findItem(int id) {
        return getMenuItemWrapper(mWrappedObject.findItem(id));
    }

    @Override
    public int size() {
        return mWrappedObject.size();
    }

    @Override
    public MenuItem getItem(int index) {
        return getMenuItemWrapper(mWrappedObject.getItem(index));
    }

    @Override
    public void close() {
        mWrappedObject.close();
    }

    @Override
    public boolean performShortcut(int keyCode, KeyEvent event, int flags) {
        return mWrappedObject.performShortcut(keyCode, event, flags);
    }

    @Override
    public boolean isShortcutKey(int keyCode, KeyEvent event) {
        return mWrappedObject.isShortcutKey(keyCode, event);
    }

    @Override
    public boolean performIdentifierAction(int id, int flags) {
        return mWrappedObject.performIdentifierAction(id, flags);
    }

    @Override
    public void setQwertyMode(boolean isQwerty) {
        mWrappedObject.setQwertyMode(isQwerty);
    }
}
