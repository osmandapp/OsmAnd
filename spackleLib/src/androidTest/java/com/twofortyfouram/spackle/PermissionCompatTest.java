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

import android.Manifest;
import android.content.Context;
import android.os.Build;
import android.support.test.filters.SdkSuppress;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;

import com.twofortyfouram.test.assertion.MoarAsserts;
import com.twofortyfouram.test.context.FeatureContextWrapper;

public final class PermissionCompatTest extends AndroidTestCase {

    @SmallTest
    public static void testNonInstantiable() {
        MoarAsserts.assertNoninstantiable(PermissionCompat.class);
    }

    @SmallTest
    public void testGetPermissionStatus_granted() {
        if (!AndroidSdkVersion.isAtLeastSdk(Build.VERSION_CODES.M)) {
            final String[] permissionArray = new String[]{
                    android.Manifest.permission.ACCESS_FINE_LOCATION
            };

            final Context testContext = new FeatureContextWrapper(getContext(), permissionArray,
                    permissionArray, null);

            assertEquals(PermissionCompat.PermissionStatus.GRANTED,
                    PermissionCompat.getPermissionStatus(testContext,
                            android.Manifest.permission.ACCESS_FINE_LOCATION));
        }
    }

    @SmallTest
    public void testGetPermissionStatus_not_granted_via_manifest() {
        if (!AndroidSdkVersion.isAtLeastSdk(Build.VERSION_CODES.M)) {
            final Context testContext = new FeatureContextWrapper(getContext(), null, null, null);

            assertEquals(PermissionCompat.PermissionStatus.NOT_GRANTED_BY_MANIFEST,
                    PermissionCompat.getPermissionStatus(testContext,
                            android.Manifest.permission.ACCESS_FINE_LOCATION));
        }
    }

    @SmallTest
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.M)
    public void testGetPermissionStatus_not_granted_by_user() {

        /*
         * Note: this test is dependent on the Manifest having the permission, but the user not
         * granting the permission to the test app.  These conditions make the test slightly brittle,
         * but it should be relatively reliable in a CI environment.
         */

        assertEquals(PermissionCompat.PermissionStatus.NOT_GRANTED_BY_USER,
                PermissionCompat.getPermissionStatus(getContext(),
                        Manifest.permission.CALL_PHONE));
    }

    /*
     * Handle the write settings special case with a FeatureContextWrapper.
     */
    @SmallTest
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.M)
    public void testGetPermissionStatus_write_settings_granted_with_feature_context_wrapper() {
        final String[] permissionArray = new String[]{
                Manifest.permission.WRITE_SETTINGS
        };

        final Context testContext = new FeatureContextWrapper(getContext(), permissionArray,
                permissionArray, null);

        assertEquals(PermissionCompat.PermissionStatus.GRANTED,
                PermissionCompat.getPermissionStatus(testContext,
                        Manifest.permission.WRITE_SETTINGS));
    }

    /*
     * Handle the ignore battery optimizations special case with a FeatureContextWrapper.
     */
    @SmallTest
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.M)
    public void testGetPermissionStatus_request_ignore_battery_optimizations_granted_with_feature_context_wrapper() {
        final String[] permissionArray = new String[]{
                Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
        };

        final Context testContext = new FeatureContextWrapper(getContext(), permissionArray,
                permissionArray, null);

        assertEquals(PermissionCompat.PermissionStatus.GRANTED,
                PermissionCompat.getPermissionStatus(testContext,
                        Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS));
    }

    /*
     * Handle the write settings special case.
     */
    @SmallTest
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.M)
    public void testGetPermissionStatus_write_settings_not_granted() {
        /*
         * Note: this test is dependent on the Manifest NOT having the permission.
         */

        assertEquals(PermissionCompat.PermissionStatus.NOT_GRANTED_BY_MANIFEST,
                PermissionCompat.getPermissionStatus(getContext(),
                        Manifest.permission.WRITE_SETTINGS));
    }

    /*
     * Handle the ignore battery optimizations special case.
     */
    @SmallTest
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.M)
    public void testGetPermissionStatus_request_ignore_battery_optimizations_not_granted() {
        /*
         * Note: this test is dependent on the Manifest NOT having the permission.
         */

        assertEquals(PermissionCompat.PermissionStatus.NOT_GRANTED_BY_MANIFEST,
                PermissionCompat.getPermissionStatus(getContext(),
                        Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS));
    }

    @SmallTest
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.M)
    public void testGetPermissionStatus_not_granted_by_manifest() {

        /*
         * Note: this test is dependent on the Manifest NOT having the permission.
         */

        assertEquals(PermissionCompat.PermissionStatus.NOT_GRANTED_BY_MANIFEST,
                PermissionCompat.getPermissionStatus(getContext(),
                        Manifest.permission.ACCESS_COARSE_LOCATION));
    }

}
