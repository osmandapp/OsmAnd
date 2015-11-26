package com.twofortyfouram.locale.sdk.client.ui.activity;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * Common interface for plug-in Activities.
 */
public interface IPluginActivity {
    /**
     * @return The {@link com.twofortyfouram.locale.api.Intent#EXTRA_BUNDLE EXTRA_BUNDLE} that was
     * previously saved to the host and subsequently passed back to this Activity for further
     * editing.  Internally, this method relies on {@link #isBundleValid(android.os.Bundle)}.  If
     * the bundle exists but is not valid, this method will return null.
     */
    @Nullable
    Bundle getPreviousBundle();

    /**
     * @return The {@link com.twofortyfouram.locale.api.Intent#EXTRA_STRING_BLURB
     * EXTRA_STRING_BLURB} that was
     * previously saved to the host and subsequently passed back to this Activity for further
     * editing.
     */
    @Nullable
    String getPreviousBlurb();

    /**
     * <p>Validates the Bundle, to ensure that a malicious application isn't attempting to pass
     * an invalid Bundle.</p>
     *
     * @param bundle The plug-in's Bundle previously returned by the edit
     *               Activity.  {@code bundle} should not be mutated by this method.
     * @return true if {@code bundle} is valid for the plug-in.
     */
    boolean isBundleValid(@NonNull final Bundle bundle);

    /**
     * Plug-in Activity lifecycle callback to allow the Activity to restore
     * state for editing a previously saved plug-in instance. This callback will
     * occur during the onPostCreate() phase of the Activity lifecycle.
     * <p>{@code bundle} will have been
     * validated by {@link #isBundleValid(android.os.Bundle)} prior to this
     * method being called.  If {@link #isBundleValid(android.os.Bundle)} returned false, then this
     * method will not be called.  This helps ensure that plug-in Activity subclasses only have to
     * worry about bundle validation once, in the {@link #isBundleValid(android.os.Bundle)}
     * method.</p>
     * <p>Note this callback only occurs the first time the Activity is created, so it will not be
     * called
     * when the Activity is recreated (e.g. {@code savedInstanceState != null}) such as after a
     * configuration change like a screen rotation.</p>
     *
     * @param previousBundle Previous bundle that the Activity saved.
     * @param previousBlurb  Previous blurb that the Activity saved
     */
    void onPostCreateWithPreviousResult(
            @NonNull final Bundle previousBundle, @NonNull final String previousBlurb);

    /**
     * @return Bundle for the plug-in or {@code null} if a valid Bundle cannot
     * be generated.
     */
    @Nullable
    Bundle getResultBundle();

    /**
     * @param bundle Valid bundle for the component.
     * @return Blurb for {@code bundle}.
     */
    @NonNull
    String getResultBlurb(@NonNull final Bundle bundle);

}
