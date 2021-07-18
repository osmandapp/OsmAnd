package net.osmand;

import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Build;
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

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.Version;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.inapp.InAppPurchaseHelper;

public class SecondSplashScreenFragment extends BaseOsmAndFragment {
	private final static int LOGO_ID = 1001;
	private final static int TEXT_ID = 1002;
	private final static int OSM_TEXT_ID = 1003;
	
	public static final String TAG = "SecondSplashScreenFragment";
	public static final int MIN_SCREEN_WIDTH_TABLET_DP = 600;
	public static boolean SHOW = true;
	public static boolean VISIBLE = false;

	private boolean systemDefaultNightMode;

	public MapActivity getMapActivity() {
		return (MapActivity) getActivity();
	}

	private int getStatusBarHeight() {
		int statusBarHeight = 0;
		int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
		if (resourceId > 0) {
			statusBarHeight = getResources().getDimensionPixelSize(resourceId);
		}
		return statusBarHeight;
	}

	private int getNavigationBarHeight() {
		if (!AndroidUtils.hasNavBar(getContext()) && !AndroidUtils.isNavBarVisible(getMapActivity()))
			return 0;
		int orientation = getResources().getConfiguration().orientation;
		if (isSmartphone() && Configuration.ORIENTATION_LANDSCAPE == orientation)
			return 0;
		int id = getResources().getIdentifier(orientation == Configuration.ORIENTATION_PORTRAIT ? "navigation_bar_height" : "navigation_bar_height_landscape", "dimen", "android");
		if (id > 0)
			return getResources().getDimensionPixelSize(id);
		return 0;
	}

	private int getNavigationBarWidth() {
		if (!AndroidUtils.hasNavBar(getContext()) && !AndroidUtils.isNavBarVisible(getMapActivity()))
			return 0;
		int orientation = getResources().getConfiguration().orientation;
		if (orientation == Configuration.ORIENTATION_LANDSCAPE && isSmartphone()) {
			int id = getResources().getIdentifier("navigation_bar_width", "dimen", "android");
			if (id > 0)
				return getResources().getDimensionPixelSize(id);
		}
		return 0;
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		OsmandApplication app = requireMyApplication();
		FragmentActivity activity = requireActivity();
		UiUtilities iconsCache = app.getUIUtilities();
		systemDefaultNightMode = app.getSettings().isSupportSystemDefaultTheme() &&
				!app.getSettings().isLightSystemDefaultTheme();

		RelativeLayout view = new RelativeLayout(activity);
		view.setOnClickListener(null);

		int backgroundColorId = systemDefaultNightMode ?
				R.color.list_background_color_dark :
				R.color.map_background_color_light;
		view.setBackgroundColor(getResources().getColor(backgroundColorId));

		ImageView logo = new ImageView(getContext());
		logo.setId(LOGO_ID);
		if (Version.isFreeVersion(app)) {
			logo.setImageDrawable(AppCompatResources.getDrawable(activity, R.drawable.ic_logo_splash_osmand));
		} else if (Version.isPaidVersion(app) || Version.isDeveloperVersion(app)) {
			logo.setImageDrawable(AppCompatResources.getDrawable(activity, R.drawable.ic_logo_splash_osmand_plus));
		}
		RelativeLayout.LayoutParams logoLayoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
		if (isSmartphone()) {
			logoLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
			logoLayoutParams.addRule(RelativeLayout.CENTER_IN_PARENT);
		} else {
			logoLayoutParams.addRule(RelativeLayout.CENTER_IN_PARENT);
		}

		ImageView text = new ImageView(activity);
		text.setId(TEXT_ID);
		int textColorId = systemDefaultNightMode ?
				R.color.text_color_tertiary_dark :
				R.color.text_color_tertiary_light;
		if (Version.isFreeVersion(app)) {
			if (InAppPurchaseHelper.isSubscribedToOsmAndPro(app)) {
				text.setImageDrawable(iconsCache.getIcon(R.drawable.image_text_osmand_pro, textColorId));
			} else if (InAppPurchaseHelper.isSubscribedToMaps(app)) {
				text.setImageDrawable(iconsCache.getIcon(R.drawable.image_text_osmand_maps_plus, textColorId));
			} else if (InAppPurchaseHelper.isSubscribedToLiveUpdates(app)) {
				text.setImageDrawable(iconsCache.getIcon(R.drawable.image_text_osmand_osmlive, textColorId));
			} else if (InAppPurchaseHelper.isFullVersionPurchased(app)) {
				text.setImageDrawable(iconsCache.getIcon(R.drawable.image_text_osmand_inapp, textColorId));
			} else {
				text.setImageDrawable(iconsCache.getIcon(R.drawable.image_text_osmand, textColorId));
			}
		} else if (Version.isPaidVersion(app) || Version.isDeveloperVersion(app)) {
			if (InAppPurchaseHelper.isSubscribedToOsmAndPro(app)) {
				text.setImageDrawable(iconsCache.getIcon(R.drawable.image_text_osmand_plus_pro, textColorId));
			} if (InAppPurchaseHelper.isSubscribedToLiveUpdates(app)) {
				text.setImageDrawable(iconsCache.getIcon(R.drawable.image_text_osmand_plus_osmlive, textColorId));
			} else {
				text.setImageDrawable(iconsCache.getIcon(R.drawable.image_text_osmand_plus, textColorId));
			}
		}
		RelativeLayout.LayoutParams textLayoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
		textLayoutParams.addRule(RelativeLayout.ABOVE, OSM_TEXT_ID);
		textLayoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
		
		ImageView osmText = new ImageView(activity);
		osmText.setId(OSM_TEXT_ID);
		osmText.setImageDrawable(iconsCache.getIcon(R.drawable.image_text_openstreetmap, textColorId));
		RelativeLayout.LayoutParams osmTextLayoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
		osmTextLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
		osmTextLayoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL);

		int defaultLogoMarginTop = getResources().getDimensionPixelSize(R.dimen.splash_screen_logo_top);
		int logoMarginTop = defaultLogoMarginTop - (Build.VERSION.SDK_INT >= 21 ? 0 : getStatusBarHeight());
		int textMarginBottom = getResources().getDimensionPixelSize(R.dimen.splash_screen_text_bottom);
		int osmTextMarginBottom = getResources().getDimensionPixelSize(R.dimen.splash_screen_osm_text_bottom);
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
		AndroidUtils.setMargins(logoLayoutParams, 0, logoMarginTop, 0, 0);
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

	private boolean isSmartphone() {
		return getResources().getConfiguration().smallestScreenWidthDp < MIN_SCREEN_WIDTH_TABLET_DP;
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
		return systemDefaultNightMode ?
				R.color.status_bar_color_dark :
				R.color.status_bar_transparent_light;
	}
}
