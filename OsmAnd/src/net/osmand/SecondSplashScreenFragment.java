package net.osmand;

import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;

public class SecondSplashScreenFragment extends Fragment {
	public static final String TAG = "SecondSplashScreenFragment";
	public static boolean SHOW = true;
	public static boolean VISIBLE = false;

	public OsmandApplication getMyApplication() {
		return ((OsmandApplication) getActivity().getApplication());
	}

	public MapActivity getMapActivity() {
		return (MapActivity) getActivity();
	}

	private boolean hasNavBar() {
		int id = getResources().getIdentifier("config_showNavigationBar", "bool", "android");
		if (id > 0) {
			return getResources().getBoolean(id);
		} else {
			return false;
		}
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
		if (!hasNavBar())
			return 0;
		int orientation = getResources().getConfiguration().orientation;
		boolean isSmartphone = getResources().getConfiguration().smallestScreenWidthDp < 600;
		if (isSmartphone && Configuration.ORIENTATION_LANDSCAPE == orientation)
			return 0;
		int id = getResources().getIdentifier(orientation == Configuration.ORIENTATION_PORTRAIT ? "navigation_bar_height" : "navigation_bar_height_landscape", "dimen", "android");
		if (id > 0)
			return getResources().getDimensionPixelSize(id);
		return 0;
	}

	private int getNavigationBarWidth() {
		if (!hasNavBar())
			return 0;
		int orientation = getResources().getConfiguration().orientation;
		boolean isSmartphone = getResources().getConfiguration().smallestScreenWidthDp < 600;
		if (orientation == Configuration.ORIENTATION_LANDSCAPE && isSmartphone) {
			int id = getResources().getIdentifier("navigation_bar_width", "dimen", "android");
			if (id > 0)
				return getResources().getDimensionPixelSize(id);
		}
		return 0;
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		OsmandApplication app = getMyApplication();
		OsmandSettings settings = app.getSettings();
		FragmentActivity activity = getActivity();

		RelativeLayout view = new RelativeLayout(activity);
		view.setOnClickListener(null);
		view.setBackgroundColor(getResources().getColor(R.color.map_background_color_light));

		ImageView logo = new ImageView(getContext());
		if (Version.isFreeVersion(app)) {
			logo.setImageDrawable(getResources().getDrawable(R.drawable.ic_logo_splash_osmand));
		} else if (Version.isPaidVersion(app) || Version.isDeveloperVersion(app)) {
			logo.setImageDrawable(getResources().getDrawable(R.drawable.ic_logo_splash_osmand_plus));
		}
		RelativeLayout.LayoutParams logoLayoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
		logoLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
		logoLayoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
		ImageView text = new ImageView(activity);
		if (Version.isFreeVersion(app)) {
			if (settings.LIVE_UPDATES_PURCHASED.get()) {
				text.setImageDrawable(getResources().getDrawable(R.drawable.image_text_osmand_osmlive));
			} else if (settings.FULL_VERSION_PURCHASED.get()) {
				text.setImageDrawable(getResources().getDrawable(R.drawable.image_text_osmand_inapp));
			} else {
				text.setImageDrawable(getResources().getDrawable(R.drawable.image_text_osmand));
			}
		} else if (Version.isPaidVersion(app) || Version.isDeveloperVersion(app)) {
			if (settings.LIVE_UPDATES_PURCHASED.get()) {
				text.setImageDrawable(getResources().getDrawable(R.drawable.image_text_osmand_plus_osmlive));
			} else {
				text.setImageDrawable(getResources().getDrawable(R.drawable.image_text_osmand_plus));
			}
		}
		RelativeLayout.LayoutParams textLayoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
		textLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
		textLayoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
		int defaultLogoMarginTop = (int) getResources().getDimension(R.dimen.splash_screen_logo_top);
		int logoMarginTop = defaultLogoMarginTop - getStatusBarHeight();
		int logoPaddingLeft = 0;
		int logoPaddingRight = 0;
		int defaultTextMarginBottom = (int) getResources().getDimension(R.dimen.splash_screen_text_bottom);
		int textMarginBottom = defaultTextMarginBottom - getNavigationBarHeight();
		int textPaddingLeft = 0;
		int textPaddingRight = 0;
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N_MR1) {
			int screenOrientation = AndroidUiHelper.getScreenOrientation(activity);
			if (screenOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
				logoPaddingLeft = getNavigationBarWidth();
				textPaddingLeft = getNavigationBarWidth();
			} else if (screenOrientation == ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE) {
				logoPaddingRight = getNavigationBarWidth();
				textPaddingRight = getNavigationBarWidth();
			}
		} else {
			logoPaddingLeft = getNavigationBarWidth();
			textPaddingLeft = getNavigationBarWidth();
		}
		logoLayoutParams.setMargins(0, logoMarginTop, 0, 0);
		logo.setPadding(logoPaddingLeft, 0, logoPaddingRight, 0);
		logo.setLayoutParams(logoLayoutParams);
		view.addView(logo);
		textLayoutParams.setMargins(0, 0, 0, textMarginBottom);
		text.setPadding(textPaddingLeft, 0, textPaddingRight, 0);
		text.setLayoutParams(textLayoutParams);
		view.addView(text);

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
}
