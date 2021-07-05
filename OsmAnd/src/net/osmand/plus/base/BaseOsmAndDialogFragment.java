package net.osmand.plus.base;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.ImageView;

import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;

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

	protected Drawable getIcon(@DrawableRes int id) {
		return getIconsCache().getIcon(id);
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
