package net.osmand.plus.base;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.helpers.RequestThemeParams;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;

public abstract class BaseOsmAndDialogFragment extends DialogFragment {

	private static final String APP_MODE_KEY = "app_mode_key";

	protected OsmandApplication app;
	protected OsmandSettings settings;
	protected ApplicationMode appMode;
	protected UiUtilities iconsCache;
	protected LayoutInflater themedInflater;
	protected boolean nightMode;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = (OsmandApplication) requireActivity().getApplication();
		settings = app.getSettings();
		iconsCache = app.getUIUtilities();
		restoreAppMode(savedInstanceState);

		updateNightMode();
		setStyle(STYLE_NO_FRAME, nightMode ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme);
		getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
	}

	private void restoreAppMode(@Nullable Bundle savedInstanceState) {
		if (savedInstanceState != null) {
			String modeKey = savedInstanceState.getString(APP_MODE_KEY);
			appMode = ApplicationMode.valueOfStringKey(modeKey, null);
		}
		Bundle args = getArguments();
		if (appMode == null && args != null) {
			String modeKey = args.getString(APP_MODE_KEY);
			appMode = ApplicationMode.valueOfStringKey(modeKey, null);
		}
		if (appMode == null) {
			appMode = settings.getApplicationMode();
		}
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		if (appMode != null) {
			outState.putString(APP_MODE_KEY, appMode.getStringKey());
		}
	}

	protected void updateNightMode() {
		nightMode = isNightMode(isUsedOnMap());
		themedInflater = UiUtilities.getInflater(requireContext(), nightMode);
	}

	protected boolean isUsedOnMap() {
		return false;
	}

	public void setAppMode(@NonNull ApplicationMode appMode) {
		this.appMode = appMode;
	}

	@NonNull
	public ApplicationMode getAppMode() {
		return appMode;
	}

	@NonNull
	protected View inflate(@LayoutRes int layoutRedId) {
		return inflate(layoutRedId, null);
	}

	@NonNull
	protected View inflate(@LayoutRes int layoutResId, @Nullable ViewGroup root) {
		return inflate(layoutResId, root, false);
	}

	@NonNull
	protected View inflate(@LayoutRes int layoutResId, @Nullable ViewGroup root, boolean attachToRoot) {
		return themedInflater.inflate(layoutResId, root, attachToRoot);
	}

	protected int getDimension(int id) {
		return app.getResources().getDimensionPixelSize(id);
	}

	@ColorInt
	protected int getColor(@ColorRes int colorId) {
		return ColorUtilities.getColor(app, colorId);
	}

	protected Drawable getPaintedContentIcon(@DrawableRes int id, @ColorInt int color) {
		return iconsCache.getPaintedIcon(id, color);
	}

	protected Drawable getIcon(@DrawableRes int id) {
		return iconsCache.getIcon(id);
	}

	protected Drawable getIcon(@DrawableRes int id, @ColorRes int colorId) {
		return iconsCache.getIcon(id, colorId);
	}

	protected Drawable getContentIcon(@DrawableRes int id) {
		return iconsCache.getThemedIcon(id);
	}

	protected boolean isNightMode(boolean usedOnMap) {
		RequestThemeParams params = new RequestThemeParams(appMode, true);
		return app.getDaynightHelper().isNightMode(usedOnMap, params);
	}
}
