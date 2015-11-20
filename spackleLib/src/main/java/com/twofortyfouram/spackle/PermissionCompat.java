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

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.annotation.Size;

import com.twofortyfouram.log.Lumberjack;

import net.jcip.annotations.ThreadSafe;

@ThreadSafe
public final class PermissionCompat {

    /*
     * Since test contexts are usually intended to be available in unobfuscated
     * debug builds, this relies on the class name.  If the debug release is obfuscated, this
     * will fail.  Relying on a direct reference to the class itself is not possible, because
     * then spackleLib would need an explicit dependency on the test library.
     */
    @NonNull
    private static final String FEATURE_CONTEXT_WRAPPER_CLASS_NAME
            = "com.twofortyfouram.test.context.FeatureContextWrapper"; //$NON-NLS-1$

    /**
     * Dynamically checks app permissions at runtime, with forward and backward
     * compatibility for pre-Marshmallow and post-Mashmallow permission behavior.
     *
     * The behavior is different from {@code android.support.v4.content.PermissionChecker}.  This
     * method lets SDK and app developers handle scenarios when permissions may intentionally
     * be omitted from the manifest.
     *
     * Note that on Marshmallow, this class also correctly handles checking for {@link
     * android.Manifest.permission#WRITE_SETTINGS} and
     * {@link android.Manifest.permission#REQUEST_IGNORE_BATTERY_OPTIMIZATIONS}.
     *
     * @param context        Application context.
     * @param permissionName Name of the permission to check.
     * @return The status indicating whether {@code context} is able to use {@code
     * permissionName}.  Note that {@link PermissionStatus#NOT_GRANTED_BY_MANIFEST} is higher
     * priority than {@link com.twofortyfouram.spackle.PermissionCompat.PermissionStatus#NOT_GRANTED_BY_USER},
     * so {@link PermissionStatus#NOT_GRANTED_BY_MANIFEST} will be returned when both statuses
     * are true.
     */
    @NonNull
    public static PermissionStatus getPermissionStatus(@NonNull final Context context,
            @NonNull @Size(min = 1) final String permissionName) {
        return getPermissionStatusLegacy(context, permissionName);
    }

    @NonNull
    private static PermissionStatus getPermissionStatusLegacy(@NonNull final Context context,
            @NonNull @Size(min = 1) final String permissionName) {
        if (PackageManager.PERMISSION_DENIED == context.getPackageManager().checkPermission(
                permissionName, context.getPackageName())) {
            Lumberjack.i("Permission %s is not granted via the AndroidManifest",
                    permissionName); //$NON-NLS-1$
            return PermissionStatus.NOT_GRANTED_BY_MANIFEST;
        }

        /*
         * Some custom ROMs allowed permissions to be disabled dynamically before the advent of
         * Marshallow. It is not possible to detect when that happens.
         *
         * http://review.cyanogenmod.com/#/c/4055/
         */

        return PermissionStatus.GRANTED;
    }

    /**
     * @param context        Application context.
     * @param permissionName Name of the permission to check.
     * @return True if the permission is explicitly declared in the manifest.
     */
    private static boolean isPermissionGrantedByManifest(@NonNull final Context context,
            @NonNull @Size(min = 1) final String permissionName) {
        final PackageInfo myPackageInfo = AppBuildInfo.getMyPackageInfo(context,
                PackageManager.GET_PERMISSIONS);

        final String[] requestedPermissions = myPackageInfo.requestedPermissions;
        final int length = requestedPermissions.length;
        for (int i = 0; i < length; i++) {
            if (permissionName.equals(requestedPermissions[i])) {
                return true;
            }
        }

        return false;
    }

    /*
     * Rather than use an IntDef, let ProGuard take care of optimizing this for us.
     */
    @ThreadSafe
    public enum PermissionStatus {
        /**
         * The permission is missing from the Android Manifest, so the permission cannot be used
         * and
         * the permission cannot be requested at runtime.  This state may not necessarily be due to
         * developer error; some applications and some SDKs may scale up
         * and down based on the permissions available.  For example, an application may have
         * different build flavors with different permissions enabled via the manifest or the user
         * may revoke permissions at runtime.
         */
        NOT_GRANTED_BY_MANIFEST,

        /**
         * The permission is not granted by the user, per the new permission APIs in Android
         * Marshmallow.  The app needs to ask the user to grant the permission.  This status will
         * only be returned on Marshmallow.
         */
        NOT_GRANTED_BY_USER,

        /**
         * The permission is granted and the app can use the permission.
         */
        GRANTED;
    }

    /**
     * Private constructor prevents instantiation.
     *
     * @throws UnsupportedOperationException because this class cannot be instantiated.
     */
    private PermissionCompat() {
        throw new UnsupportedOperationException("This class is non-instantiable"); //$NON-NLS-1$
    }
}
