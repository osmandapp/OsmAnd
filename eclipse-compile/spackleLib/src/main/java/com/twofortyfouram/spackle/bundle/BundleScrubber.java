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

package com.twofortyfouram.spackle.bundle;


import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.CheckResult;
import android.support.annotation.Nullable;

import com.twofortyfouram.log.Lumberjack;

import net.jcip.annotations.ThreadSafe;

/**
 * Scrubs Intents and Bundles of invalid extras, preventing Intent fuzzing and denial-of-service
 * attacks from malicious applications.  To use this class, call {@link
 * #scrub(android.content.Intent)} BEFORE inspecting any of the Intent's extras.
 */
@ThreadSafe
public final class BundleScrubber {

    /**
     * Scrubs an Intent for bad extras in {@link Intent#getExtras()}.
     * <p>
     * Note: this method does not recursively scrub Bundles.
     *
     * @param intent Intent whose extras will be scrubbed. If {@code intent} has
     *               at least one bad extra, the extra Bundle will be cleared. Note
     *               that {@code intent} may be mutated by this method.
     * @return true if {@code intent}'s extras were cleared, false if
     * {@code intent} was not mutated.
     */
    @CheckResult
    public static boolean scrub(@Nullable final Intent intent) {
        boolean isBundleMutated = false;

        if (null != intent) {
            /*
             * Calling Intent.getExtras() returns a copy, so scrubbing here will
             * not mutate the Bundle in the Intent.
             */
            isBundleMutated = scrub(intent.getExtras());

            if (isBundleMutated) {
                intent.replaceExtras(new Bundle());
            }
        }

        return isBundleMutated;
    }

    /**
     * Scrubs a Bundle for bad extras.
     * <p>
     * Note: this method does not recursively scrub Bundles.
     *
     * @param bundle Bundle whose extras will be scrubbed. If {@code bundle} has
     *               at least one bad extra, the Bundle will be cleared. Note that
     *               {@code bundle} may be mutated by this method.
     * @return true if {@code bundle} was cleared, false if {@code bundle} was
     * not mutated.
     */
    @CheckResult
    public static boolean scrub(@Nullable final Bundle bundle) {
        boolean isBundleMutated = false;

        if (null != bundle) {
            /*
             * This is a workaround for an Android bug:
             * <http://code.google.com/p/android/issues/detail?id=16006>.
             *
             * If a private Serializable exists, attempting to retrieve anything
             * from the Bundle will throw an exception.
             */
            try {
                bundle.containsKey(null);
            } catch (final Exception e) {
                Lumberjack.e("Private serializable attack detected; deleting all extras%s",
                        e); //$NON-NLS-1$

                bundle.clear();
                isBundleMutated = true;
            }
        }

        return isBundleMutated;
    }

    /**
     * Private constructor prevents instantiation.
     *
     * @throws UnsupportedOperationException because this class cannot be
     *                                       instantiated.
     */
    private BundleScrubber() {
        throw new UnsupportedOperationException("This class is non-instantiable"); //$NON-NLS-1$
    }
}
