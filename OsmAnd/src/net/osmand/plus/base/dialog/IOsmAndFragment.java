package net.osmand.plus.base.dialog;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.*;
import androidx.fragment.app.FragmentActivity;

import net.osmand.OnResultCallback;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.OsmandActionBarActivity;
import net.osmand.plus.base.AppModeDependentComponent;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.enums.ThemeUsageContext;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;

import java.util.Objects;

/**
 * Interface that defines common functionality for OsmAnd fragments
 * which are aware of ApplicationMode, theme usage, and UI utilities.
 *
 * <p>Provides access to:
 * <ul>
 *     <li>MapActivity and its safe references</li>
 *     <li>Night mode resolution and themed inflater</li>
 *     <li>Application resources and utilities</li>
 *     <li>Icon and drawable accessors</li>
 *     <li>Layout inflation</li>
 * </ul>
 *
 * <p>Should be implemented by all fragments that need to integrate
 * consistently into the OsmAnd theming and application mode system.
 */
public interface IOsmAndFragment extends AppModeDependentComponent {

	// === Required implementations ===

	@NonNull
	OsmandApplication getApp();

	@Nullable
	FragmentActivity getActivity();

	@NonNull
	default Context getThemedContext() {
		return getThemedInflater().getContext();
	}

	@NonNull
	LayoutInflater getThemedInflater();

	@NonNull
	ThemeUsageContext getThemeUsageContext();

	@NonNull
	UiUtilities getIconsCache();

	// === Activity access ===

	default void callActivity(@NonNull OnResultCallback<FragmentActivity> callback) {
		callActivity(FragmentActivity.class, callback);
	}

	default void callMapActivity(@NonNull OnResultCallback<MapActivity> callback) {
		callActivity(MapActivity.class, callback);
	}

	default <T extends FragmentActivity> void callActivity(@NonNull Class<T> activityClass, @NonNull OnResultCallback<T> callback) {
		FragmentActivity activity = getActivity();
		if (AndroidUtils.isActivityNotDestroyed(activity)) {
			if (activityClass.isInstance(activity)) {
				callback.onResult(activityClass.cast(activity));
			}
		}
	}

	@Nullable
	default MapActivity getMapActivity() {
		FragmentActivity activity = getActivity();
		if (activity instanceof MapActivity mapActivity) {
			return mapActivity;
		}
		return null;
	}

	/**
	 * Returns the MapActivity or throws IllegalStateException
	 * if the current activity is not attached or of the wrong type.
	 */
	@NonNull
	default MapActivity requireMapActivity() {
		FragmentActivity activity = getActivity();
		if (activity instanceof MapActivity mapActivity) {
			return mapActivity;
		}
		throw new IllegalStateException("Fragment " + this + " not attached to MapActivity.");
	}

	@Nullable
	default OsmandActionBarActivity getActionBarActivity() {
		if (getActivity() instanceof OsmandActionBarActivity activity) {
			return activity;
		}
		return null;
	}

	@NonNull
	default OsmandActionBarActivity requireActionBarActivity() {
		if (getActivity() instanceof OsmandActionBarActivity activity) {
			return activity;
		}
		throw new IllegalStateException("Fragment " + this + " not attached to OsmandActionBarActivity.");
	}

	// === Theme and night mode resolution ===

	/**
	 * Resolves the night mode status for the current application mode and usage context.
	 */
	default boolean resolveNightMode() {
		ApplicationMode appMode = getAppMode();
		ThemeUsageContext usageContext = getThemeUsageContext();
		return getApp().getDaynightHelper().isNightMode(appMode, usageContext);
	}

	// === Layout inflation ===

	@NonNull
	default View inflate(@LayoutRes int layoutResId) {
		return inflate(layoutResId, null);
	}

	@NonNull
	default View inflate(@LayoutRes int layoutResId, @Nullable ViewGroup parent) {
		return getThemedInflater().inflate(layoutResId, parent);
	}

	@NonNull
	default View inflate(@LayoutRes int layoutResId, @Nullable ViewGroup parent, boolean attachToRoot) {
		return getThemedInflater().inflate(layoutResId, parent, attachToRoot);
	}

	// === Resource access: dimensions, dp, colors ===

	default int dpToPx(float dp) {
		return AndroidUtils.dpToPx(getApp(), dp);
	}

	@Dimension
	default float getDimension(@DimenRes int resId) {
		return getApp().getResources().getDimension(resId);
	}

	@Dimension
	default int getDimensionPixelSize(@DimenRes int resId) {
		return getApp().getResources().getDimensionPixelSize(resId);
	}

	@ColorInt
	default int getColor(@ColorRes int colorId) {
		return ColorUtilities.getColor(getApp(), colorId);
	}

	// === Icon access and customization ===

	@NonNull
	default Drawable requireIcon(@DrawableRes int id) {
		return Objects.requireNonNull(getIcon(id));
	}

	@Nullable
	default Drawable getIcon(@DrawableRes int id) {
		return getIconsCache().getIcon(id);
	}

	@NonNull
	default Drawable requireIcon(@DrawableRes int id, @ColorRes int colorId) {
		return Objects.requireNonNull(getIcon(id, colorId));
	}

	@Nullable
	default Drawable getIcon(@DrawableRes int id, @ColorRes int colorId) {
		return getIconsCache().getIcon(id, colorId);
	}

	@Nullable
	default Drawable getContentIcon(@DrawableRes int id) {
		return getIconsCache().getThemedIcon(id);
	}

	@Nullable
	default Drawable getPaintedIcon(@DrawableRes int id, @ColorInt int color) {
		return getIconsCache().getPaintedIcon(id, color);
	}
}