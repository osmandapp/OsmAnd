package net.osmand.plus.base.dialog;

import androidx.annotation.DimenRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.AppModeDependentComponent;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.enums.ThemeUsageContext;
import net.osmand.plus.utils.AndroidUtils;

public interface IOsmAndFragment extends AppModeDependentComponent {

	default boolean resolveNightMode() {
		ApplicationMode appMode = getAppMode();
		ThemeUsageContext usageContext = getThemeUsageContext();
		return getApp().getDaynightHelper().isNightMode(appMode, usageContext);
	}

	@Nullable
	default MapActivity getNotDestroyedMapActivity() {
		MapActivity mapActivity = getMapActivity();
		if (AndroidUtils.isActivityNotDestroyed(mapActivity)) {
			return mapActivity;
		}
		return null;
	}

	@Nullable
	default MapActivity getMapActivity() {
		FragmentActivity activity = getActivity();
		if (activity instanceof MapActivity mapActivity) {
			return mapActivity;
		}
		return null;
	}

	@NonNull
	default MapActivity requireMapActivity() {
		FragmentActivity activity = getActivity();
		if (activity instanceof MapActivity mapActivity) {
			return mapActivity;
		}
		throw new IllegalStateException("Fragment " + this + " not attached to MapActivity.");
	}

	default int dpToPx(float dp) {
		return AndroidUtils.dpToPx(getApp(), dp);
	}

	default int getDimension(@DimenRes int dimensionResId) {
		return getApp().getResources().getDimensionPixelSize(dimensionResId);
	}

	@NonNull
	ThemeUsageContext getThemeUsageContext();

	@NonNull
	OsmandApplication getApp();

	@Nullable
	FragmentActivity getActivity();
}
