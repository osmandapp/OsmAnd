package net.osmand;

import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.base.BaseFullScreenFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.inapp.InAppPurchaseUtils;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;

public class SecondSplashScreenFragment extends BaseFullScreenFragment {

	private static final int LOGO_ID = 1001;
	private static final int TEXT_ID = 1002;
	private static final int OSM_TEXT_ID = 1003;

	public static final String TAG = "SecondSplashScreenFragment";
	public static boolean SHOW = true;
	public static boolean VISIBLE;

	private int getNavigationBarWidth() {
		if (!AndroidUtils.hasNavBar(getContext()) && !AndroidUtils.isNavBarVisible(getMapActivity()))
			return 0;
		int orientation = getResources().getConfiguration().orientation;
		if (orientation == Configuration.ORIENTATION_LANDSCAPE && !AndroidUiHelper.isTablet(getContext())) {
			int id = getResources().getIdentifier("navigation_bar_width", "dimen", "android");
			if (id > 0)
				return getDimensionPixelSize(id);
		}
		return 0;
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		FragmentActivity activity = requireActivity();
		nightMode = settings.isSupportSystemTheme() && !settings.isLightSystemTheme();

		RelativeLayout view = new RelativeLayout(activity);
		view.setId(R.id.bottom_buttons_container);
		view.setClickable(true);
		view.setFocusable(true);
		view.setOnClickListener(null);

		int backgroundColorId = nightMode ?
				R.color.list_background_color_dark :
				R.color.map_background_color_light;
		view.setBackgroundColor(getColor(backgroundColorId));

		ImageView logo = new ImageView(getContext());
		logo.setId(LOGO_ID);
		if (Version.isFreeVersion(app)) {
			logo.setImageDrawable(AppCompatResources.getDrawable(activity, R.drawable.ic_logo_splash_osmand));
		} else if (Version.isPaidVersion(app) || Version.isDeveloperVersion(app)) {
			logo.setImageDrawable(AppCompatResources.getDrawable(activity, R.drawable.ic_logo_splash_osmand_plus));
		}
		RelativeLayout.LayoutParams logoLayoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
		if (!AndroidUiHelper.isTablet(activity)) {
			logoLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
			logoLayoutParams.addRule(RelativeLayout.CENTER_IN_PARENT);
		} else {
			logoLayoutParams.addRule(RelativeLayout.CENTER_IN_PARENT);
		}

		ImageView text = new ImageView(activity);
		text.setId(TEXT_ID);
		int textColorId = ColorUtilities.getTertiaryTextColorId(nightMode);
		if (Version.isFreeVersion(app)) {
			if (InAppPurchaseUtils.isOsmAndProAvailable(app)) {
				text.setImageDrawable(uiUtilities.getIcon(R.drawable.image_text_osmand_pro, textColorId));
			} else if (InAppPurchaseUtils.isMapsPlusAvailable(app)) {
				text.setImageDrawable(uiUtilities.getIcon(R.drawable.image_text_osmand_maps_plus, textColorId));
			} else if (InAppPurchaseUtils.isLiveUpdatesAvailable(app)) {
				text.setImageDrawable(uiUtilities.getIcon(R.drawable.image_text_osmand_osmlive, textColorId));
			} else if (InAppPurchaseUtils.isFullVersionAvailable(app)) {
				text.setImageDrawable(uiUtilities.getIcon(R.drawable.image_text_osmand_inapp, textColorId));
			} else {
				text.setImageDrawable(uiUtilities.getIcon(R.drawable.image_text_osmand, textColorId));
			}
		} else if (Version.isPaidVersion(app) || Version.isDeveloperVersion(app)) {
			if (InAppPurchaseUtils.isOsmAndProAvailable(app)) {
				text.setImageDrawable(uiUtilities.getIcon(R.drawable.image_text_osmand_plus_pro, textColorId));
			} else if (InAppPurchaseUtils.isLiveUpdatesAvailable(app)) {
				text.setImageDrawable(uiUtilities.getIcon(R.drawable.image_text_osmand_plus_osmlive, textColorId));
			} else {
				text.setImageDrawable(uiUtilities.getIcon(R.drawable.image_text_osmand_plus, textColorId));
			}
		}
		RelativeLayout.LayoutParams textLayoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
		textLayoutParams.addRule(RelativeLayout.ABOVE, OSM_TEXT_ID);
		textLayoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL);

		ImageView osmText = new ImageView(activity);
		osmText.setId(OSM_TEXT_ID);
		osmText.setImageDrawable(uiUtilities.getIcon(R.drawable.image_text_openstreetmap, textColorId));
		RelativeLayout.LayoutParams osmTextLayoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
		osmTextLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
		osmTextLayoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL);

		int defaultLogoMarginTop = getDimensionPixelSize(R.dimen.splash_screen_logo_top);
		int textMarginBottom = getDimensionPixelSize(R.dimen.splash_screen_text_bottom);
		int osmTextMarginBottom = getDimensionPixelSize(R.dimen.splash_screen_osm_text_bottom);
		int elementsPaddingLeft = 0;
		int elementsPaddingRight = 0;
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N_MR1) {
			int screenOrientation = AndroidUiHelper.getScreenOrientation(activity);
			if (screenOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
				elementsPaddingLeft = getNavigationBarWidth();
			} else if (screenOrientation == ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE) {
				elementsPaddingRight = getNavigationBarWidth();
			}
		} else {
			elementsPaddingLeft = getNavigationBarWidth();
		}
		AndroidUtils.setMargins(logoLayoutParams, 0, defaultLogoMarginTop, 0, 0);
		logo.setPadding(elementsPaddingLeft, 0, elementsPaddingRight, 0);
		logo.setLayoutParams(logoLayoutParams);
		view.addView(logo);
		textLayoutParams.setMargins(0, 0, 0, textMarginBottom);
		text.setPadding(elementsPaddingLeft, 0, elementsPaddingRight, 0);
		text.setLayoutParams(textLayoutParams);
		view.addView(text);
		osmTextLayoutParams.setMargins(0, 0, 0, osmTextMarginBottom);
		osmText.setPadding(elementsPaddingLeft, 0, elementsPaddingRight, 0);
		osmText.setLayoutParams(osmTextLayoutParams);
		view.addView(osmText);

		return view;
	}

	@Override
	public void onResume() {
		super.onResume();
		getMapActivity().disableDrawer();
	}

	@Override
	public void onPause() {
		super.onPause();
		getMapActivity().enableDrawer();
	}

	@Override
	public int getStatusBarColorId() {
		return nightMode ? R.color.status_bar_main_dark : R.color.status_bar_transparent_light;
	}

	public static boolean showInstance(@NonNull FragmentManager manager) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			manager.beginTransaction()
					.add(R.id.fragmentContainer, new SecondSplashScreenFragment(), TAG)
					.commitAllowingStateLoss();
			return true;
		}
		return false;
	}
}