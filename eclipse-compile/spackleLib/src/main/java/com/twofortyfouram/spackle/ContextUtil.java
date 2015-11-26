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

import android.app.Activity;
import android.app.Service;
import android.app.backup.BackupAgent;
import android.content.Context;
import android.support.annotation.NonNull;

import com.twofortyfouram.log.Lumberjack;

import net.jcip.annotations.ThreadSafe;

/**
 * Utilities for interaction with {@link Context}.
 */
@ThreadSafe
public final class ContextUtil {

    /*
     * Note: this implementation relies on an explicit whitelist of test context classes, as
     * opposed to a whitelist based on a partial package name (e.g. excluding all android.test.*).
     * This implementation reduces the likelihood of a malicious actor attempting to introduce
     * context memory leaks.
     *
     * A leak could still occur if a malicious actor passes in a context that overrides
      * getApplicationContext().
     */

    /**
     * Class name of isolated context.
     */
    @NonNull
    private static final String ISOLATED_CONTEXT_CLASS_NAME = "android.test.IsolatedContext";
    //$NON-NLS-1$

    /**
     * Class name of isolated context.
     */
    @NonNull
    private static final String RENAMING_DELEGATING_CONTEXT_CLASS_NAME
            = "android.test.RenamingDelegatingContext";
    //$NON-NLS-1$

    /**
     * Gets the Application Context of {@code context} to prevent memory leaks
     * from {@code Activity}, {@code Service}, or similar contexts.
     * <p>
     * This is typically used to check a Context parameter when entering a
     * method that expects an Application context, because this will log whether
     * the Context is correctly cleaned or not.
     *
     * This method is useful, because it tries to avoid breaking out of context during automated
     * tests.
     *
     * @param context {@code Context} to clean. If this is a test context that
     *                doesn't support {@link Context#getApplicationContext()}, then
     *                {@code context} will be returned.
     * @return Cleaned instance of {@code context}.
     */
    public static Context cleanContext(@NonNull final Context context) {
        /*
         * These warnings are important, because they allow Context memory leaks
         * to be fixed.
         */
        if (context instanceof Activity) {
            Lumberjack.w("context was an instance of Activity%s", new Exception()); //$NON-NLS-1$
        } else if (context instanceof Service) {
            Lumberjack.w("context was an instance of Service%s", new Exception()); //$NON-NLS-1$
        } else if (context instanceof BackupAgent) {
            Lumberjack.w("context was an instance of BackupAgent%s", new Exception()); //$NON-NLS-1$
        }

        final String className = context.getClass().getName();

        if (className.equals(ISOLATED_CONTEXT_CLASS_NAME) || className
                .equals(RENAMING_DELEGATING_CONTEXT_CLASS_NAME)) {
            return context;
        } else {
            try {
                final Context returnContext = context.getApplicationContext();

            /*
             * Check for null is required because during unit tests the
             * Application context might not have been initialized yet.
             */
                if (null == returnContext) {
                    return context;
                }
                return returnContext;
            } catch (final UnsupportedOperationException e) {
            /*
             * This is required for when the app's JUnit test suite is run.
             * Calling getApplicationContext() on a test context will fail.
             */
                Lumberjack
                        .w("Couldn't clean context; probably running in test mode%s",
                                e); //$NON-NLS-1$

                return context;
            }
        }
    }

    /**
     * Private constructor prevents instantiation.
     *
     * @throws UnsupportedOperationException because this class cannot be
     *                                       instantiated.
     */
    private ContextUtil() {
        throw new UnsupportedOperationException("This class is non-instantiable"); //$NON-NLS-1$
    }
}
