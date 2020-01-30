package net.osmand.plus.wikivoyage;

import android.app.Dialog;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.Toolbar;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import net.osmand.plus.R;
import net.osmand.plus.base.BaseOsmAndDialogFragment;

public class WikiBaseDialogFragment extends BaseOsmAndDialogFragment {

	protected boolean nightMode;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		nightMode = isNightMode(false);
	}

	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		int themeId = nightMode ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme_LightStatusBar;
		Dialog dialog = new Dialog(getContext(), themeId);
		Window window = dialog.getWindow();
		if (window != null) {
			if (!getSettings().DO_NOT_USE_ANIMATIONS.get()) {
				window.getAttributes().windowAnimations = R.style.Animations_Alpha;
			}
			if (Build.VERSION.SDK_INT >= 21) {
				window.setStatusBarColor(getResolvedColor(getStatusBarColor()));
			}
		}
		return dialog;
	}

	@Override
	public void show(FragmentManager manager, String tag) {
		FragmentTransaction ft = manager.beginTransaction();
		ft.addToBackStack(tag);
		show(ft, tag);
	}

	@Override
	protected Drawable getContentIcon(int id) {
		return getIcon(id, R.color.icon_color_default_light);
	}

	protected Drawable getActiveIcon(@DrawableRes int iconId) {
		return getIcon(iconId, nightMode ? R.color.wikivoyage_active_dark : R.color.wikivoyage_active_light);
	}

	@ColorRes
	protected int getStatusBarColor() {
		return nightMode ? R.color.status_bar_wikivoyage_dark : R.color.status_bar_wikivoyage_light;
	}

	@ColorInt
	protected int getResolvedColor(@ColorRes int colorId) {
		return ContextCompat.getColor(getContext(), colorId);
	}

	protected View inflate(@LayoutRes int layoutId, @Nullable ViewGroup container) {
		int themeRes = nightMode ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme;
		return LayoutInflater.from(new ContextThemeWrapper(getContext(), themeRes))
				.inflate(layoutId, container, false);
	}

	protected void setupToolbar(Toolbar toolbar) {
		toolbar.setNavigationIcon(getContentIcon(R.drawable.ic_arrow_back));
		toolbar.setNavigationContentDescription(R.string.access_shared_string_navigate_up);
		toolbar.setNavigationOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				closeFragment();
			}
		});
	}
	
	protected void closeFragment() {
		dismiss();
	}
}
