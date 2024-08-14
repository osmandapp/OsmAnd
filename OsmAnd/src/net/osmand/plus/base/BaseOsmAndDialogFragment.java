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
import net.osmand.plus.helpers.RequestMapThemeParams;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;

public abstract class BaseOsmAndDialogFragment extends DialogFragment {

	protected OsmandApplication app;
	protected OsmandSettings settings;
	protected UiUtilities iconsCache;
	protected LayoutInflater themedInflater;
	protected boolean nightMode;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = (OsmandApplication) requireActivity().getApplication();
		settings = app.getSettings();
		iconsCache = app.getUIUtilities();

		updateNightMode();
		setStyle(STYLE_NO_FRAME, nightMode ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme);
		getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
	}

	protected void updateNightMode() {
		nightMode = isNightMode(isUsedOnMap());
		themedInflater = UiUtilities.getInflater(getContext(), nightMode);
	}

	protected boolean isUsedOnMap() {
		return false;
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
		RequestMapThemeParams params = new RequestMapThemeParams().markIgnoreExternalProvider();
		return app.getDaynightHelper().isNightMode(usedOnMap, params);
	}
}
