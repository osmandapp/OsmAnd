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

import android.content.Context;
import android.support.v4.internal.view.SupportMenuItem;
import android.support.v4.internal.view.SupportSubMenu;
import android.support.v4.util.ArrayMap;
import android.view.MenuItem;
import android.view.SubMenu;

import java.util.Iterator;
import java.util.Map;

abstract class BaseMenuWrapper<T> extends BaseWrapper<T> {

    final Context mContext;

    private Map<SupportMenuItem, MenuItem> mMenuItems;
    private Map<SupportSubMenu, SubMenu> mSubMenus;

    BaseMenuWrapper(Context context, T object) {
        super(object);
        mContext = context;
    }

    final MenuItem getMenuItemWrapper(final MenuItem menuItem) {
        if (menuItem instanceof SupportMenuItem) {
            final SupportMenuItem supportMenuItem = (SupportMenuItem) menuItem;

            // Instantiate Map if null
            if (mMenuItems == null) {
                mMenuItems = new ArrayMap<>();
            }

            // First check if we already have a wrapper for this item
            MenuItem wrappedItem = mMenuItems.get(menuItem);

            if (null == wrappedItem) {
                // ... if not, create one and add it to our map
                wrappedItem = MenuWrapperFactory.wrapSupportMenuItem(mContext, supportMenuItem);
                mMenuItems.put(supportMenuItem, wrappedItem);
            }

            return wrappedItem;
        }
        return menuItem;
    }

    final SubMenu getSubMenuWrapper(final SubMenu subMenu) {
        if (subMenu instanceof SupportSubMenu) {
            final SupportSubMenu supportSubMenu = (SupportSubMenu) subMenu;

            // Instantiate Map if null
            if (mSubMenus == null) {
                mSubMenus = new ArrayMap<>();
            }

            SubMenu wrappedMenu = mSubMenus.get(supportSubMenu);

            if (null == wrappedMenu) {
                wrappedMenu = MenuWrapperFactory.wrapSupportSubMenu(mContext, supportSubMenu);
                mSubMenus.put(supportSubMenu, wrappedMenu);
            }
            return wrappedMenu;
        }
        return subMenu;
    }


    final void internalClear() {
        if (mMenuItems != null) {
            mMenuItems.clear();
        }
        if (mSubMenus != null) {
            mSubMenus.clear();
        }
    }

    final void internalRemoveGroup(final int groupId) {
        if (mMenuItems == null) {
            return;
        }

        Iterator<SupportMenuItem> iterator = mMenuItems.keySet().iterator();
        android.view.MenuItem menuItem;

        while (iterator.hasNext()) {
            menuItem = iterator.next();
            if (groupId == menuItem.getGroupId()) {
                iterator.remove();
            }
        }
    }

    final void internalRemoveItem(final int id) {
        if (mMenuItems == null) {
            return;
        }

        Iterator<SupportMenuItem> iterator = mMenuItems.keySet().iterator();
        android.view.MenuItem menuItem;

        while (iterator.hasNext()) {
            menuItem = iterator.next();
            if (id == menuItem.getItemId()) {
                iterator.remove();
                break;
            }
        }
    }
}
