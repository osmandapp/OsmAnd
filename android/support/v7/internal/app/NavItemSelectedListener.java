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


package android.support.v7.internal.app;

import android.support.v7.app.ActionBar;
import android.support.v7.internal.widget.AdapterViewCompat;
import android.view.View;

/**
 * Wrapper to adapt the ActionBar.OnNavigationListener in an AdapterView.OnItemSelectedListener
 * for use in Spinner widgets. Used by action bar implementations.
 *
 * @hide
 */
class NavItemSelectedListener implements AdapterViewCompat.OnItemSelectedListener {
    private final ActionBar.OnNavigationListener mListener;

    public NavItemSelectedListener(ActionBar.OnNavigationListener listener) {
        mListener = listener;
    }

    @Override
    public void onItemSelected(AdapterViewCompat<?> parent, View view, int position, long id) {
        if (mListener != null) {
            mListener.onNavigationItemSelected(position, id);
        }
    }

    @Override
    public void onNothingSelected(AdapterViewCompat<?> parent) {
        // Do nothing
    }
}