package net.osmand.plus.base;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.WindowManager;

import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.utils.UiUtilities;

public class BaseOsmAndDialogFragment extends DialogFragment {

	protected OsmandApplication app;
	protected OsmandSettings settings;
	protected UiUtilities iconsCache;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = getMyApplication();
		settings = app.getSettings();
		iconsCache = app.getUIUtilities();

		int themeId = isNightMode(false) ? R.style.OsmandLightTheme : R.style.OsmandDarkTheme;
		setStyle(STYLE_NO_FRAME, themeId);
		getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
	}

	@NonNull
	protected OsmandApplication getMyApplication() {
		return (OsmandApplication) requireActivity().getApplication();
	}

	@NonNull
	protected OsmandSettings getSettings() {
		return getMyApplication().getSettings();
	}

	protected AppCompatActivity getMyActivity() {
		return (AppCompatActivity) getActivity();
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
		if (usedOnMap) {
			return getMyApplication().getDaynightHelper().isNightModeForMapControls();
		}
		return !getSettings().isLightContent();
	}
}
