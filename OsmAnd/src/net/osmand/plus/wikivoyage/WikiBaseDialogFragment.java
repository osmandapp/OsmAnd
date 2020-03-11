package net.osmand.plus.wikivoyage;

import android.app.Dialog;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import net.osmand.AndroidUtils;
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
		Drawable icBack = getContentIcon(AndroidUtils.getNavigationIconResId(getContext()));
		toolbar.setNavigationIcon(icBack);
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
