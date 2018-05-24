package net.osmand.plus.base;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatActivity;
import android.view.WindowManager;
import android.widget.ImageView;

import net.osmand.plus.UiUtilities;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;

public class BaseOsmAndDialogFragment extends DialogFragment {

	private UiUtilities iconsCache;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		int themeId = getSettings().isLightContent() ? R.style.OsmandLightTheme : R.style.OsmandDarkTheme;
		setStyle(STYLE_NO_FRAME, themeId);
		getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
	}

	protected OsmandApplication getMyApplication() {
		return (OsmandApplication) getActivity().getApplication();
	}

	protected AppCompatActivity getMyActivity() {
		return (AppCompatActivity) getActivity();
	}

	protected UiUtilities getIconsCache() {
		if (iconsCache == null) {
			iconsCache = getMyApplication().getUIUtilities();
		}
		return iconsCache;
	}

	protected Drawable getPaintedContentIcon(@DrawableRes int id, @ColorInt int color) {
		return getIconsCache().getPaintedIcon(id, color);
	}

	protected Drawable getIcon(@DrawableRes int id, @ColorRes int colorId) {
		return getIconsCache().getIcon(id, colorId);
	}

	protected Drawable getContentIcon(@DrawableRes int id) {
		return getIconsCache().getThemedIcon(id);
	}

	protected void setThemedDrawable(ImageView view, @DrawableRes int iconId) {
		view.setImageDrawable(getContentIcon(iconId));
	}

	protected OsmandSettings getSettings() {
		return getMyApplication().getSettings();
	}

	protected boolean isNightMode(boolean usedOnMap) {
		if (usedOnMap) {
			return getMyApplication().getDaynightHelper().isNightModeForMapControls();
		}
		return !getSettings().isLightContent();
	}
}
