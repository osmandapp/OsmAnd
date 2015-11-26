package com.twofortyfouram.spackle;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Trace;
import android.support.annotation.NonNull;

import net.jcip.annotations.ThreadSafe;

/**
 * Compatibility wrapper for {@link android.os.Trace}.  On older API levels, this class has no
 * effect.
 */
@ThreadSafe
public final class TraceCompat {

    /**
     * @param tag Tag for the trace section.
     * @see Trace#beginSection(String)
     */
    public static void beginSection(@NonNull final String tag) {
        if (AndroidSdkVersion.isAtLeastSdk(Build.VERSION_CODES.JELLY_BEAN_MR2)) {
            beginSectionJellybeanMr2(tag);
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private static void beginSectionJellybeanMr2(@NonNull final String tag) {
        Trace.beginSection(tag);
    }

    /**
     * @see Trace#endSection()
     */
    public static void endSection() {
        if (AndroidSdkVersion.isAtLeastSdk(Build.VERSION_CODES.JELLY_BEAN_MR2)) {
            endSectionJellybeanMr2();
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private static void endSectionJellybeanMr2() {
        Trace.endSection();
    }

    /**
     * Private constructor prevents instantiation.
     *
     * @throws UnsupportedOperationException because this class cannot be instantiated.
     */
    private TraceCompat() {
        throw new UnsupportedOperationException("This class is non-instantiable"); //$NON-NLS-1$
    }
}
