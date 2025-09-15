package net.osmand.plus.wikivoyage;

import static net.osmand.plus.utils.ColorUtilities.getStatusBarSecondaryColorId;

import android.graphics.drawable.Drawable;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import net.osmand.plus.R;
import net.osmand.plus.base.BaseFullScreenDialogFragment;
import net.osmand.plus.utils.AndroidUtils;

public class WikiBaseDialogFragment extends BaseFullScreenDialogFragment {

	@Override
	protected int getThemeId() {
		return nightMode ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme_LightStatusBar;
	}

	@Override
	public void show(@NonNull FragmentManager fragmentManager, @Nullable String tag) {
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, tag)) {
			FragmentTransaction transaction = fragmentManager.beginTransaction();
			transaction.addToBackStack(tag);
			show(transaction, tag);
		}
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
