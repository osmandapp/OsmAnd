/*
 * android-spackle-lib https://github.com/twofortyfouram/android-spackle
 * Copyright 2014 two forty four a.m. LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.twofortyfouram.spackle;

import android.os.Build;

import net.jcip.annotations.ThreadSafe;

/**
 * Utility class to determine information about the environment the app is running in.
 */
@ThreadSafe
public final class AndroidSdkVersion {

    /*
     * Yes, it is silly to have a class just for this when doing a
     * simple >= check should be so easy. So why is this class here? Because we've seen those >=
     * end up being wrong. FML.
     */

    /**
     * @param sdkInt SDK version number to test against the current environment.
     * @return {@code true} if {@link android.os.Build.VERSION#SDK_INT} is greater than or equal to
     * {@code sdkInt}.
     */
    public static boolean isAtLeastSdk(final int sdkInt) {
        return Build.VERSION.SDK_INT >= sdkInt;
    }


    /**
     * Private constructor prevents instantiation.
     *
     * @throws UnsupportedOperationException because this class cannot be instantiated.
     */
    private AndroidSdkVersion() {
        throw new UnsupportedOperationException("This class is non-instantiable"); //$NON-NLS-1$
    }
}
