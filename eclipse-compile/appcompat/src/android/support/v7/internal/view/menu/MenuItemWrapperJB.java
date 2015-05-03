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

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.support.v4.internal.view.SupportMenuItem;
import android.support.v4.view.ActionProvider;
import android.view.MenuItem;
import android.view.View;

/**
 * Wraps a support {@link SupportMenuItem} as a framework {@link android.view.MenuItem}
 * @hide
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
class MenuItemWrapperJB extends MenuItemWrapperICS {

    MenuItemWrapperJB(Context context, SupportMenuItem object) {
        super(context, object);
    }

    @Override
    ActionProviderWrapper createActionProviderWrapper(android.view.ActionProvider provider) {
        return new ActionProviderWrapperJB(mContext, provider);
    }

    class ActionProviderWrapperJB extends ActionProviderWrapper
            implements android.view.ActionProvider.VisibilityListener {
        ActionProvider.VisibilityListener mListener;

        public ActionProviderWrapperJB(Context context, android.view.ActionProvider inner) {
            super(context, inner);
        }

        @Override
        public View onCreateActionView(MenuItem forItem) {
            return mInner.onCreateActionView(forItem);
        }

        @Override
        public boolean overridesItemVisibility() {
            return mInner.overridesItemVisibility();
        }

        @Override
        public boolean isVisible() {
            return mInner.isVisible();
        }

        @Override
        public void refreshVisibility() {
            mInner.refreshVisibility();
        }

        @Override
        public void setVisibilityListener(ActionProvider.VisibilityListener listener) {
            mListener = listener;
            mInner.setVisibilityListener(listener != null ? this : null);
        }

        @Override
        public void onActionProviderVisibilityChanged(boolean isVisible) {
            if (mListener != null) {
                mListener.onActionProviderVisibilityChanged(isVisible);
            }
        }
    }
}
