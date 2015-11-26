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
import android.content.res.TypedArray;
import android.support.annotation.AnyRes;
import android.support.annotation.ArrayRes;
import android.support.annotation.NonNull;

import net.jcip.annotations.ThreadSafe;

import java.util.NoSuchElementException;

/**
 * Utilities for interaction with Android application resources.
 */
@ThreadSafe
public final class ResourceUtil {

    /**
     * Gets the first position of an element in a typed array.
     *
     * @param context   Application context.
     * @param arrayId   resource ID of the array.
     * @param elementId resource ID of the element in the array to find.  If this id is repeated,
     *                  only
     *                  position of the first instance will be returned.
     * @return position of the {@code elementId} in the array.
     * @throws NoSuchElementException if {@code elementId} is not in the array.
     */
    public static int getPositionForIdInArray(@NonNull final Context context,
            @ArrayRes final int arrayId,
            @AnyRes final int elementId) {
        TypedArray array = null;
        try {
            array = context.getResources().obtainTypedArray(arrayId);
            for (int x = 0; x < array.length(); x++) {
                if (array.getResourceId(x, 0) == elementId) {
                    return x;
                }
            }
        } finally {
            if (null != array) {
                array.recycle();
                array = null;
            }
        }

        throw new NoSuchElementException();
    }

    /**
     * Gets the resource ID of an element in a typed array.
     *
     * @param context  Application context.
     * @param arrayId  resource ID of the array.
     * @param position position in the array to retrieve.
     * @return resource id of element in {@code position}.
     * @throws IndexOutOfBoundsException if {@code position} is not in the array.
     */
    public static int getResourceIdForPositionInArray(@NonNull final Context context,
            @ArrayRes final int arrayId, final int position) {
        TypedArray stateArray = null;
        try {
            stateArray = context.getResources().obtainTypedArray(arrayId);
            final int selectedResourceId = stateArray.getResourceId(position, 0);

            if (0 == selectedResourceId) {
                throw new IndexOutOfBoundsException();
            }

            return selectedResourceId;
        } finally {
            if (null != stateArray) {
                stateArray.recycle();
                stateArray = null;
            }
        }
    }

    /**
     * Returns a resource from a string name, rather than an integer ID.
     *
     * @param context      Application context.
     * @param resourceName Name of the resource to retrieve.
     * @return The value mapping to {@code resourceName}.
     */
    public static boolean getBoolean(@NonNull final Context context,
            @NonNull final String resourceName) {
        final int id = context.getResources().getIdentifier(resourceName,
                "bool", context.getPackageName()); //$NON-NLS-1$

        return context.getResources().getBoolean(id);
    }

    /**
     * Private constructor prevents instantiation.
     *
     * @throws UnsupportedOperationException because this class cannot be instantiated.
     */
    private ResourceUtil() {
        throw new UnsupportedOperationException("This class is non-instantiable"); //$NON-NLS-1$
    }
}
