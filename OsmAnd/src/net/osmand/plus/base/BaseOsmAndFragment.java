package net.osmand.plus.base;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.annotation.DimenRes;
import androidx.annotation.Dimension;
import androidx.annotation.DrawableRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.activities.OsmandActionBarActivity;
import net.osmand.plus.base.dialog.IOsmAndFragment;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.enums.ThemeUsageContext;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;

/**
 * Base fragment class for all UI components in OsmAnd that directly extend Android's Fragment.
 *
 * This class provides centralized access to core components of the application,
 * such as the current Application Mode, UI theming (night mode, theme context),
 * and other shared utilities.
 *
 * All fragments in OsmAnd that would otherwise extend the standard Android Fragment
 * must instead extend this class (or one of its subclasses such as {@link BaseFullScreenFragment}
 * or {@link BaseNestedFragment}). This ensures proper theming behavior, access to application
 * services, and consistent lifecycle handling.
 *
 * Note: Fragments based on DialogFragment or BottomSheetFragment should NOT inherit from this class.
 */
public class BaseOsmAndFragment extends Fragment implements IOsmAndFragment {

	protected OsmandApplication app;
	protected ApplicationMode appMode;
	protected OsmandSettings settings;
	protected UiUtilities uiUtilities;
	protected LayoutInflater themedInflater;
	protected boolean nightMode;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = (OsmandApplication) requireActivity().getApplication();
		settings = app.getSettings();
		uiUtilities = app.getUIUtilities();
		appMode = restoreAppMode(app, appMode, savedInstanceState, getArguments());
		updateNightMode();
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		saveAppModeToBundle(appMode, outState);
	}

	protected void updateNightMode() {
		nightMode = resolveNightMode();
		themedInflater = UiUtilities.getInflater(requireContext(), nightMode);
	}

	@NonNull
	protected View inflate(@LayoutRes int layoutResId) {
		return inflate(layoutResId, null);
	}

	@NonNull
	protected View inflate(@LayoutRes int layoutResId, @Nullable ViewGroup parent) {
		return inflate(layoutResId, parent, false);
	}

	@NonNull
	protected View inflate(@LayoutRes int layoutResId, @Nullable ViewGroup parent, boolean attachToRoot) {
		return themedInflater.inflate(layoutResId, parent, attachToRoot);
	}

	@NonNull
	public ApplicationMode getAppMode() {
		return appMode;
	}

	public void setAppMode(@NonNull ApplicationMode appMode) {
		this.appMode = appMode;
	}

	public boolean isNightMode() {
		return nightMode;
	}

	protected boolean isUsedOnMap() {
		return false;
	}

	@Nullable
	protected OsmandActionBarActivity getMyActivity() {
		return (OsmandActionBarActivity) getActivity();
	}

	@NonNull
	protected OsmandActionBarActivity requireMyActivity() {
		return (OsmandActionBarActivity) requireActivity();
	}

	protected Drawable getPaintedContentIcon(@DrawableRes int id, @ColorInt int color) {
		return uiUtilities.getPaintedIcon(id, color);
	}

	protected Drawable getIcon(@DrawableRes int id) {
		return uiUtilities.getIcon(id);
	}

	protected Drawable getIcon(@DrawableRes int id, @ColorRes int colorId) {
		return uiUtilities.getIcon(id, colorId);
	}

	protected Drawable getContentIcon(@DrawableRes int id) {
		return uiUtilities.getThemedIcon(id);
	}

	@ColorInt
	protected int getColor(@ColorRes int resId) {
		return ColorUtilities.getColor(app, resId);
	}

	@Dimension
	protected int getDimensionPixelSize(@DimenRes int resId) {
		return getResources().getDimensionPixelSize(resId);
	}

	@NonNull
	@Override
	public ThemeUsageContext getThemeUsageContext() {
		return ThemeUsageContext.valueOf(isUsedOnMap());
	}

	@NonNull
	@Override
	public OsmandApplication getApp() {
		return app;
	}
}
