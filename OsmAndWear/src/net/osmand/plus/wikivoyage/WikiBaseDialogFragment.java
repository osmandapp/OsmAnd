package net.osmand.plus.wikivoyage;

import static net.osmand.plus.utils.ColorUtilities.getStatusBarSecondaryColorId;

import android.app.Dialog;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Window;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.R;
import net.osmand.plus.base.BaseOsmAndDialogFragment;

public class WikiBaseDialogFragment extends BaseOsmAndDialogFragment {

	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		int themeId = nightMode ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme_LightStatusBar;
		Dialog dialog = new Dialog(getContext(), themeId);
		Window window = dialog.getWindow();
		if (window != null) {
			if (!settings.DO_NOT_USE_ANIMATIONS.get()) {
				window.getAttributes().windowAnimations = R.style.Animations_Alpha;
			}
			window.setStatusBarColor(getColor(getStatusBarColor()));
		}
		return dialog;
	}

	@Override
	public void show(@NonNull FragmentManager fragmentManager, @Nullable String tag) {
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, tag)) {
			FragmentTransaction transaction = fragmentManager.beginTransaction();
			transaction.addToBackStack(tag);
			show(transaction, tag);
		}
	}

	@Override
	protected Drawable getContentIcon(int id) {
		return getIcon(id, R.color.icon_color_default_light);
	}

	protected Drawable getActiveIcon(@DrawableRes int iconId) {
		return getIcon(iconId, nightMode ? R.color.active_color_primary_dark : R.color.active_color_primary_light);
	}

	@ColorRes
	protected int getStatusBarColor() {
		return getStatusBarSecondaryColorId(nightMode);
	}

	protected void setupToolbar(Toolbar toolbar) {
		Drawable icBack = getContentIcon(AndroidUtils.getNavigationIconResId(getContext()));
		toolbar.setNavigationIcon(icBack);
		toolbar.setNavigationContentDescription(R.string.access_shared_string_navigate_up);
		toolbar.setNavigationOnClickListener(v -> closeFragment());
	}
	
	protected void closeFragment() {
		dismiss();
	}
}
