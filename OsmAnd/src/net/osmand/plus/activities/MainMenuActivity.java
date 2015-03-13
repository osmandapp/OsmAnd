package net.osmand.plus.activities;


import java.lang.ref.WeakReference;
import java.util.List;

import net.osmand.Location;
import net.osmand.plus.AppInitializer;
import net.osmand.plus.OsmAndAppCustomization;
import net.osmand.plus.OsmAndLocationProvider;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.base.BasicProgressAsyncTask;
import net.osmand.plus.dashboard.DashDownloadMapsFragment;
import net.osmand.plus.dashboard.DashErrorFragment;
import net.osmand.plus.dashboard.DashUpdatesFragment;
import net.osmand.plus.dashboard.NotifyingScrollView;
import net.osmand.plus.download.BaseDownloadActivity;
import net.osmand.plus.download.IndexItem;
import net.osmand.plus.sherpafy.TourViewActivity;
import android.app.Dialog;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;


/**
 */
public class MainMenuActivity extends BaseDownloadActivity implements OsmAndLocationProvider.OsmAndCompassListener, OsmAndLocationProvider.OsmAndLocationListener {

	private static final int HELP_ID = 0;
	private static final int SETTINGS_ID = 1;
	private static final int EXIT_ID = 2;

	private static final int START_ALPHA = 60;

	int defaultMargin;

	private Drawable actionBarBackground;

	private NotifyingScrollView.OnScrollChangedListener onScrollChangedListener = new NotifyingScrollView.OnScrollChangedListener() {
		public void onScrollChanged(ScrollView who, int l, int t, int oldl, int oldt) {
			//making background of actionbar transparent with scroll
			final int imageHeight = findViewById(R.id.map_image).getMeasuredHeight();
			final int headerHeight = imageHeight - getSupportActionBar().getHeight();
			final float ratio = (float) Math.min(Math.max(t, 0), headerHeight) / headerHeight;
			final int newAlpha = (int) (ratio * 255);
			int margintop = -(int)(ratio * 60);
			Resources r = getResources();
			int px = (int) TypedValue.applyDimension(
					TypedValue.COMPLEX_UNIT_DIP,
					margintop,
					r.getDisplayMetrics());
			int margin = px + defaultMargin;
			if (headerHeight >= t - margin){
				//makes other cards to move on top of the map card to make it look like android animations
				View fragments = findViewById(R.id.fragments);
				LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

				params.setMargins(0, margin, 0, 0);
				fragments.setLayoutParams(params);
			}
			if (newAlpha > START_ALPHA) {
				actionBarBackground.setAlpha(newAlpha);
			}

		}
	};

	private Drawable.Callback mDrawableCallback = new Drawable.Callback() {
		@Override
		public void invalidateDrawable(Drawable who) {
			getSupportActionBar().setBackgroundDrawable(who);
		}

		@Override
		public void scheduleDrawable(Drawable who, Runnable what, long when) {
		}

		@Override
		public void unscheduleDrawable(Drawable who, Runnable what) {
		}
	};

	@Override
	public void onPause() {
		super.onPause();
		getLocationProvider().pauseAllUpdates();
		getLocationProvider().removeLocationListener(this);
		getLocationProvider().removeCompassListener(this);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		getMyApplication().applyTheme(this);
		super.onCreate(savedInstanceState);
		haveHomeButton = false;
		if (getIntent() != null) {
			Intent intent = getIntent();
			if (intent.getExtras() != null && intent.getExtras().containsKey(AppInitializer.APP_EXIT_KEY)) {
				getMyApplication().closeApplication(this);
				return;
			}
		}
		if (Version.isSherpafy(getMyApplication())) {
			final Intent mapIntent = new Intent(this, TourViewActivity.class);
			startActivity(mapIntent);
			finish();
			return;
		}
		setContentView(R.layout.dashboard);

		String textVersion = Version.getFullVersion(getMyApplication());
		if (textVersion.contains("#")) {
			textVersion = textVersion.substring(0, textVersion.indexOf("#") + 1);
		}
		ActionBar actionBar = getSupportActionBar();
		actionBar.setTitle(textVersion);
		actionBar.setIcon(android.R.color.transparent);
		actionBarBackground = new ColorDrawable(Color.argb(180, 0, 0, 0));
		actionBarBackground.setAlpha(START_ALPHA);
		actionBar.setBackgroundDrawable(actionBarBackground);
		((NotifyingScrollView)findViewById(R.id.main_scroll)).setOnScrollChangedListener(onScrollChangedListener);
		//setting up callback for drawable on actionbar for old android
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
			actionBarBackground.setCallback(mDrawableCallback);
		}

		if (getMyApplication().getSettings().FOLLOW_THE_ROUTE.get() && !getMyApplication().getRoutingHelper().isRouteCalculated()) {
			startMapActivity();
		}

		getLocationProvider().addCompassListener(this);
		getLocationProvider().registerOrUnregisterCompassListener(true);

		defaultMargin = (int) TypedValue.applyDimension(
				TypedValue.COMPLEX_UNIT_DIP,
				-40,
				getResources().getDisplayMetrics());
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (getMyApplication().getFavorites().getFavouritePoints().size() > 0) {
		//	getLocationProvider().addLocationListener(this);
			getLocationProvider().addCompassListener(this);
			getLocationProvider().registerOrUnregisterCompassListener(true);
		//	getLocationProvider().resumeAllUpdates();
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == AppInitializer.APP_EXIT_CODE) {
			getMyApplication().closeApplication(this);
		}
	}

	private void addErrorFragment() {
		android.support.v4.app.FragmentManager manager = getSupportFragmentManager();
		android.support.v4.app.FragmentTransaction fragmentTransaction = manager.beginTransaction();
		if (manager.findFragmentByTag(DashErrorFragment.TAG) == null) {
			DashErrorFragment errorFragment = new DashErrorFragment();
			fragmentTransaction.add(R.id.content, errorFragment, DashErrorFragment.TAG).commit();
		}
	}


	private void startMapActivity() {
		final Intent mapIndent = new Intent(this, getMyApplication().getAppCustomization().getMapActivity());
		startActivityForResult(mapIndent, 0);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuItem menuItem = menu.add(0, HELP_ID, 0, R.string.shared_string_help).setIcon(R.drawable.ic_ac_help);
		MenuItemCompat.setShowAsAction(menuItem, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
		menuItem = menu.add(0, SETTINGS_ID, 0, R.string.shared_string_settings).setIcon(R.drawable.ic_ac_settings);
		MenuItemCompat.setShowAsAction(menuItem, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
		menuItem = menu.add(0, EXIT_ID, 0, R.string.shared_string_exit).setIcon(R.drawable.ic_ac_close);
		MenuItemCompat.setShowAsAction(menuItem, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
		return true;
	}


	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		OsmAndAppCustomization appCustomization = getMyApplication().getAppCustomization();
		if (item.getItemId() == HELP_ID) {
			if (AppInitializer.TIPS_AND_TRICKS) {
				TipsAndTricksActivity activity = new TipsAndTricksActivity(this);
				Dialog dlg = activity.getDialogToShowTips(false, true);
				dlg.show();
			} else {
				final Intent helpIntent = new Intent(this, HelpActivity.class);
				startActivity(helpIntent);
			}
		} else if (item.getItemId() == SETTINGS_ID) {
			final Intent settings = new Intent(this, appCustomization.getSettingsActivity());
			startActivity(settings);
		} else if (item.getItemId() == EXIT_ID) {
			getMyApplication().closeApplication(this);
		}
		return true;
	}

	@Override
	public void updateProgress(boolean updateOnlyProgress) {
		BasicProgressAsyncTask<?, ?, ?> basicProgressAsyncTask = BaseDownloadActivity.downloadListIndexThread.getCurrentRunningTask();
		for (WeakReference<Fragment> ref : fragList) {
			Fragment f = ref.get();
			if (f instanceof DashUpdatesFragment && !f.isDetached()) {
				((DashUpdatesFragment) f).updateProgress(basicProgressAsyncTask, updateOnlyProgress);
			}
		}
	}

	@Override
	public void updateDownloadList(List<IndexItem> list) {
		for (WeakReference<Fragment> ref : fragList) {
			Fragment f = ref.get();
			if (f instanceof DashUpdatesFragment && !f.isDetached()) {
				if (downloadQueue.size() > 0) {
					startDownload(downloadQueue.get(0));
					downloadQueue.remove(0);
				}
				((DashUpdatesFragment) f).updatedDownloadsList(list);

			}
			if (f instanceof DashDownloadMapsFragment && !f.isDetached()) {
				((DashDownloadMapsFragment) f).refreshData();
			}
		}
	}

	@Override
	public void updateCompassValue(float value) {
		// 
//		throw new UnsupportedOperationException();
	}

	@Override
	public void updateLocation(Location location) {
//		for (WeakReference<Fragment> ref : fragList) {
//			Fragment f = ref.get();
//			if (f instanceof DashLocationFragment && !f.isDetached()) {
//				((DashLocationFragment) f).updateLocation(location);
//			}
//		}
//		throw new UnsupportedOperationException();
	}

	private OsmAndLocationProvider getLocationProvider() {
		return getMyApplication().getLocationProvider();
	}
}
