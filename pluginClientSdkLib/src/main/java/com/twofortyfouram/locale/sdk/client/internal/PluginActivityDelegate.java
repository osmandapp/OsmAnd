package com.twofortyfouram.locale.sdk.client.internal;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.twofortyfouram.locale.sdk.client.ui.activity.IPluginActivity;
import com.twofortyfouram.log.Lumberjack;
import com.twofortyfouram.spackle.bundle.BundleComparer;
import com.twofortyfouram.spackle.bundle.BundleScrubber;

import net.jcip.annotations.Immutable;

/**
 * Activities that implement the {@link IPluginActivity} interface can delegate much of their
 * responsibility to this class.
 *
 * @param <T> Plug-in activity.
 */
/*
 * This class is intended to make the implementation of various plug-in Activities DRY.
 *
 * This class has no state, so therefore is immutable.
 */
@Immutable
public final class PluginActivityDelegate<T extends Activity & IPluginActivity> {
    /**
     * @param intent Intent to check.
     * @return True if intent is a Locale plug-in edit Intent.
     */
    public static boolean isLocalePluginIntent(@NonNull final Intent intent) {
        final String action = intent.getAction();

        return com.twofortyfouram.locale.api.Intent.ACTION_EDIT_CONDITION.equals(action)
                || com.twofortyfouram.locale.api.Intent.ACTION_EDIT_SETTING.equals(action);
    }

    public void onCreate(@NonNull final T activity, @Nullable final Bundle savedInstanceState) {
        final Intent intent = activity.getIntent();

        if (isLocalePluginIntent(intent)) {
            if (BundleScrubber.scrub(intent)) {
                return;
            }

            final Bundle previousBundle = activity.getPreviousBundle();
            if (BundleScrubber.scrub(previousBundle)) {
                return;
            }

            Lumberjack
                    .v("Creating Activity with Intent=%s, savedInstanceState=%s, EXTRA_BUNDLE=%s",
                            intent, savedInstanceState, previousBundle); //$NON-NLS-1$
        }
    }

    public void onPostCreate(@NonNull final T activity, @Nullable final Bundle savedInstanceState) {
        if (PluginActivityDelegate.isLocalePluginIntent(activity.getIntent())) {
            if (null == savedInstanceState) {
                final Bundle previousBundle = activity.getPreviousBundle();
                final String previousBlurb = activity.getPreviousBlurb();
                if (null != previousBundle && null != previousBlurb) {
                    activity.onPostCreateWithPreviousResult(previousBundle, previousBlurb);
                }
            }
        }
    }

    public void finish(@NonNull final T activity, final boolean isCancelled) {
        if (PluginActivityDelegate.isLocalePluginIntent(activity.getIntent())) {
            if (!isCancelled) {
                final Bundle resultBundle = activity.getResultBundle();

                if (null != resultBundle) {
                    final String blurb = activity.getResultBlurb(resultBundle);
                    if (!BundleComparer.areBundlesEqual(resultBundle, activity.getPreviousBundle())
                            || !blurb.equals(activity.getPreviousBlurb())) {
                        final Intent resultIntent = new Intent();
                        resultIntent.putExtra(com.twofortyfouram.locale.api.Intent.EXTRA_BUNDLE,
                                resultBundle);
                        resultIntent.putExtra(
                                com.twofortyfouram.locale.api.Intent.EXTRA_STRING_BLURB,
                                blurb);

                        activity.setResult(Activity.RESULT_OK, resultIntent);
                    }
                }
            }
        }
    }

    @Nullable
    public final String getPreviousBlurb(@NonNull final T activity) {
        final String blurb = activity.getIntent().getStringExtra(
                com.twofortyfouram.locale.api.Intent.EXTRA_STRING_BLURB);

        return blurb;
    }

    @Nullable
    public Bundle getPreviousBundle(@NonNull final T activity) {
        final Bundle bundle = activity.getIntent().getBundleExtra(
                com.twofortyfouram.locale.api.Intent.EXTRA_BUNDLE);

        if (null != bundle) {
            if (activity.isBundleValid(bundle)) {
                return bundle;
            }
        }

        return null;
    }
}
