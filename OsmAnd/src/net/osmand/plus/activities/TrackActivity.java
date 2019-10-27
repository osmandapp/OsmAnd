package net.osmand.plus.activities;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;

import net.osmand.AndroidUtils;
import net.osmand.GPXUtilities;
import net.osmand.GPXUtilities.GPXFile;
import net.osmand.GPXUtilities.TrkSegment;
import net.osmand.GPXUtilities.WptPt;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.QuadRect;
import net.osmand.plus.GPXDatabase.GpxDataItem;
import net.osmand.plus.GpxSelectionHelper;
import net.osmand.plus.GpxSelectionHelper.GpxDisplayGroup;
import net.osmand.plus.GpxSelectionHelper.SelectedGpxFile;
import net.osmand.plus.LockableViewPager;
import net.osmand.plus.OsmAndAppCustomization;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.mapmarkers.CoordinateInputDialogFragment;
import net.osmand.plus.measurementtool.NewGpxData;
import net.osmand.plus.myplaces.FavoritesActivity;
import net.osmand.plus.myplaces.SplitSegmentDialogFragment;
import net.osmand.plus.myplaces.TrackBitmapDrawer;
import net.osmand.plus.myplaces.TrackBitmapDrawer.TrackBitmapDrawerListener;
import net.osmand.plus.myplaces.TrackPointFragment;
import net.osmand.plus.myplaces.TrackSegmentFragment;
import net.osmand.plus.views.AddGpxPointBottomSheetHelper.NewGpxPoint;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class TrackActivity extends TabActivity {

	public static final String TRACK_FILE_NAME = "TRACK_FILE_NAME";
	public static final String OPEN_POINTS_TAB = "OPEN_POINTS_TAB";
	public static final String OPEN_TRACKS_LIST = "OPEN_TRACKS_LIST";
	public static final String CURRENT_RECORDING = "CURRENT_RECORDING";
	public static final String SHOW_TEMPORARILY = "SHOW_TEMPORARILY";
	protected List<WeakReference<Fragment>> fragList = new ArrayList<>();
	private OsmandApplication app;
	private TrackBitmapDrawer trackBitmapDrawer;

	private File file = null;
	private GPXFile gpxFile;
	private GpxDataItem gpxDataItem;
	private LockableViewPager viewPager;
	private long modifiedTime = -1;

	private List<GpxDisplayGroup> displayGroups;
	private List<GpxDisplayGroup> originalGroups = new ArrayList<>();
	private boolean stopped = false;
	private boolean openPointsTab = false;
	private boolean openTracksList = false;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		this.app = getMyApplication();
		app.applyTheme(this);
		super.onCreate(savedInstanceState);
		Intent intent = getIntent();
		if (intent == null || (!intent.hasExtra(TRACK_FILE_NAME) &&
				!intent.hasExtra(CURRENT_RECORDING))) {
			Log.e("TrackActivity", "Required extra '" + TRACK_FILE_NAME + "' is missing");
			finish();
			return;
		}
		if (intent.hasExtra(TRACK_FILE_NAME)) {
			file = new File(intent.getStringExtra(TRACK_FILE_NAME));
		}

		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			if (file != null) {
				String fn = file.getName().replace(".gpx", "").replace("/", " ").replace("_", " ");
				actionBar.setTitle(fn);
			} else {
				actionBar.setTitle(getString(R.string.shared_string_currently_recording_track));
			}
			actionBar.setElevation(AndroidUtils.dpToPx(app, 4f));
		}
		if (intent.hasExtra(OPEN_POINTS_TAB)
				|| (savedInstanceState != null && savedInstanceState.getBoolean(OPEN_POINTS_TAB, false))) {
			openPointsTab = true;
		}
		if (intent.hasExtra(OPEN_TRACKS_LIST)) {
			openTracksList = true;
		}
		setContentView(R.layout.track_content);
	}

	@Nullable
	public TrackBitmapDrawer getTrackBitmapDrawer() {
		return trackBitmapDrawer;
	}

	public void addPoint(PointDescription pointDescription) {
		Intent currentIntent = getIntent();
		if (currentIntent != null) {
			currentIntent.putExtra(TrackActivity.OPEN_POINTS_TAB, true);
		}
		final OsmandSettings settings = app.getSettings();
		GPXFile gpx = getGpx();
		LatLon location = settings.getLastKnownMapLocation();
		QuadRect rect = getRect();
		NewGpxPoint newGpxPoint = new NewGpxPoint(gpx, pointDescription, rect);
		if (gpx != null && location != null) {
			settings.setMapLocationToShow(location.getLatitude(), location.getLongitude(),
					settings.getLastKnownMapZoom(),
					pointDescription,
					false,
					newGpxPoint);

			MapActivity.launchMapActivityMoveToTop(this);
		}
	}

	public void addNewGpxData(NewGpxData.ActionType actionType) {
		addNewGpxData(actionType, null);
	}

	public void addNewGpxData(NewGpxData.ActionType actionType, TrkSegment segment) {
		GPXFile gpxFile = getGpx();
		QuadRect rect = getRect();
		NewGpxData newGpxData = new NewGpxData(gpxFile, rect, actionType, segment);
		WptPt pointToShow = gpxFile != null ? gpxFile.findPointToShow() : null;
		if (pointToShow != null) {
			LatLon location = new LatLon(pointToShow.getLatitude(), pointToShow.getLongitude());
			final OsmandSettings settings = app.getSettings();
			settings.setMapLocationToShow(location.getLatitude(), location.getLongitude(),
					settings.getLastKnownMapZoom(),
					new PointDescription(PointDescription.POINT_TYPE_WPT, getString(R.string.add_line)),
					false,
					newGpxData
			);

			MapActivity.launchMapActivityMoveToTop(this);
		}
	}

	public QuadRect getRect() {
		if (getGpx() != null) {
			return getGpx().getRect();
		} else {
			return new QuadRect(0, 0, 0, 0);
		}
	}

	protected void setGpxDataItem(GpxDataItem gpxDataItem) {
		this.gpxDataItem = gpxDataItem;
	}

	protected void setGpx(GPXFile result) {
		this.gpxFile = result;
		if (file == null) {
			this.gpxFile = getMyApplication().getSavingTrackHelper().getCurrentGpx();
		}
	}

	public List<GpxDisplayGroup> getGpxFile(boolean useDisplayGroups) {
		if (gpxFile == null) {
			return new ArrayList<>();
		}
		if (gpxFile.modifiedTime != modifiedTime) {
			modifiedTime = gpxFile.modifiedTime;
			GpxSelectionHelper selectedGpxHelper = ((OsmandApplication) getApplication()).getSelectedGpxHelper();
			displayGroups = selectedGpxHelper.collectDisplayGroups(gpxFile);
			originalGroups.clear();
			for (GpxDisplayGroup g : displayGroups) {
				originalGroups.add(g.cloneInstance());
			}
			if (file != null) {
				SelectedGpxFile sf = selectedGpxHelper.getSelectedFileByPath(gpxFile.path);
				if (sf != null && file != null && sf.getDisplayGroups() != null) {
					displayGroups = sf.getDisplayGroups();
				}
			}
		}
		if (useDisplayGroups) {
			return displayGroups;
		} else {
			return originalGroups;
		}
	}

	@Override
	public void onAttachFragment(Fragment fragment) {
		fragList.add(new WeakReference<>(fragment));
		if (trackBitmapDrawer != null && fragment instanceof TrackBitmapDrawerListener) {
			trackBitmapDrawer.addListener((TrackBitmapDrawerListener) fragment);
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		stopped = false;

		viewPager = (LockableViewPager) findViewById(R.id.pager);
		viewPager.setSwipeLocked(true);
		setViewPagerAdapter(viewPager, new ArrayList<TabActivity.TabItem>());

		loadGpx();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if (viewPager.getCurrentItem() == 1) {
			outState.putBoolean(OPEN_POINTS_TAB, true);
		}
	}

	public void loadGpx() {
		new GPXFileLoaderTask(this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void) null);
	}

	public OsmandApplication getMyApplication() {
		return (OsmandApplication) getApplication();
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (viewPager.getCurrentItem() == 1) {
			openPointsTab = true;
		}
	}

	@Override
	protected void onStop() {
		super.onStop();
		stopped = true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int itemId = item.getItemId();
		switch (itemId) {
			case android.R.id.home:
				if (getIntent().hasExtra(MapActivity.INTENT_KEY_PARENT_MAP_ACTIVITY) || openTracksList) {
					OsmAndAppCustomization appCustomization = getMyApplication().getAppCustomization();
					final Intent favorites = new Intent(this, appCustomization.getFavoritesActivity());
					getMyApplication().getSettings().FAVORITES_TAB.set(FavoritesActivity.GPX_TAB);
					favorites.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
					startActivity(favorites);
				}
				finish();
				return true;

		}
		return false;
	}

	public void updateSplitView() {
		for (WeakReference<Fragment> f : fragList) {
			Fragment frag = f.get();
			if (frag instanceof TrackSegmentFragment) {
				((TrackSegmentFragment) frag).updateSplitView();
			}
		}
	}

	public void updateHeader(Fragment sender) {
		for (WeakReference<Fragment> f : fragList) {
			Fragment frag = f.get();
			if (frag != sender) {
				if (frag instanceof TrackSegmentFragment) {
					((TrackSegmentFragment) frag).updateHeader();
				} else if (frag instanceof TrackPointFragment) {
					((TrackPointFragment) frag).updateHeader();
				}
			}
		}
	}

	@Override
	public void onBackPressed() {
		if (openTracksList) {
			OsmAndAppCustomization appCustomization = getMyApplication().getAppCustomization();
			final Intent favorites = new Intent(this, appCustomization.getFavoritesActivity());
			getMyApplication().getSettings().FAVORITES_TAB.set(FavoritesActivity.GPX_TAB);
			favorites.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
			startActivity(favorites);
		}
		super.onBackPressed();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (trackBitmapDrawer != null) {
			trackBitmapDrawer.clearListeners();
			trackBitmapDrawer = null;
		}
	}

	boolean hasTrackPoints() {
		return getGpx() != null && getGpx().hasTrkPt();
	}

	boolean hasWayPoints() {
		return getGpx() != null && getGpx().hasWptPt();
	}

	boolean hasRoutePoints() {
		return getGpx() != null && getGpx().hasRtePt();
	}

	@Nullable
	public GPXFile getGpx() {
		return gpxFile;
	}

	@Nullable
	public GpxDataItem getGpxDataItem() {
		return gpxDataItem;
	}

	private void onGPXFileReady(@Nullable GPXFile gpxFile) {
		setGpx(gpxFile);
		setGpxDataItem(file != null ? app.getGpxDbHelper().getItem(file) : null);

		WindowManager mgr = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
		if (gpxFile != null && mgr != null) {
			DisplayMetrics dm = new DisplayMetrics();
			mgr.getDefaultDisplay().getMetrics(dm);
			trackBitmapDrawer = new TrackBitmapDrawer(app, gpxFile, getGpxDataItem(), getRect(), dm.density, dm.widthPixels, AndroidUtils.dpToPx(app, 152f));
		}

		for (WeakReference<Fragment> f : fragList) {
			Fragment frag = f.get();
			if (frag instanceof TrackBitmapDrawerListener) {
				if (trackBitmapDrawer != null) {
					trackBitmapDrawer.addListener((TrackBitmapDrawerListener) frag);
				}
			}
			if (frag instanceof TrackSegmentFragment) {
				TrackSegmentFragment trackSegmentFragment = (TrackSegmentFragment) frag;
				if (trackBitmapDrawer != null) {
					trackBitmapDrawer.setDrawEnabled(trackSegmentFragment.isUpdateEnable());
				}
				trackSegmentFragment.updateContent();
			} else if (frag instanceof SplitSegmentDialogFragment) {
				((SplitSegmentDialogFragment) frag).updateContent();
			} else if (frag instanceof TrackPointFragment) {
				TrackPointFragment trackPointFragment = (TrackPointFragment) frag;
				if (trackBitmapDrawer != null) {
					trackBitmapDrawer.setDrawEnabled(trackPointFragment.isUpdateEnable());
				}
				trackPointFragment.setContent();
			} else if (gpxFile != null && frag instanceof CoordinateInputDialogFragment) {
				((CoordinateInputDialogFragment) frag).setGpx(gpxFile);
			}
		}
		OsmandFragmentPagerAdapter pagerAdapter = (OsmandFragmentPagerAdapter) viewPager.getAdapter();
		if (pagerAdapter != null && pagerAdapter.getCount() == 0) {
			pagerAdapter.addTab(getTabIndicator(R.string.shared_string_gpx_tracks, TrackSegmentFragment.class));
			pagerAdapter.addTab(getTabIndicator(R.string.shared_string_gpx_points, TrackPointFragment.class));
			if (openPointsTab || !hasTrackPoints()) {
				viewPager.setCurrentItem(1, false);
			}

			if (pagerAdapter.getCount() > 1) {
				boolean nightMode = !app.getSettings().isLightContent();
				final ColorStateList navColorStateList = AndroidUtils.createBottomNavColorStateList(this, nightMode);
				final BottomNavigationView bottomNav = (BottomNavigationView) findViewById(R.id.bottom_navigation);
				bottomNav.setItemIconTintList(navColorStateList);
				bottomNav.setItemTextColor(navColorStateList);
				bottomNav.setVisibility(View.VISIBLE);
				if (viewPager.getCurrentItem() == 1) {
					bottomNav.setSelectedItemId(R.id.action_points);
				}
				bottomNav.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
					@Override
					public boolean onNavigationItemSelected(@NonNull MenuItem item) {
						int position = -1;
						int i = item.getItemId();
						if (i == R.id.action_track) {
							position = 0;
						} else if (i == R.id.action_points) {
							position = 1;
						}
						if (position != -1 && position != viewPager.getCurrentItem()) {
							viewPager.setCurrentItem(position);
							return true;
						}
						return false;
					}
				});
			}
		}
	}

	private static class GPXFileLoaderTask extends AsyncTask<Void, Void, GPXFile> {

		private OsmandApplication app;
		private WeakReference<TrackActivity> activityRef;
		private File file;
		private boolean showTemporarily;

		private TrackActivity getTrackActivity() {
			return activityRef.get();
		}

		GPXFileLoaderTask(@NonNull TrackActivity activity) {
			this.activityRef = new WeakReference<>(activity);
			app = activity.getMyApplication();
			file = activity.file;
		}

		protected void onPreExecute() {
			TrackActivity activity = getTrackActivity();
			if (activity != null) {
				activity.setSupportProgressBarIndeterminateVisibility(true);
				Intent intent = activity.getIntent();
				if (intent != null && intent.hasExtra(SHOW_TEMPORARILY)) {
					showTemporarily = true;
					intent.removeExtra(SHOW_TEMPORARILY);
				}
			}
		}

		@Override
		protected GPXFile doInBackground(Void... params) {
			long startTime = System.currentTimeMillis();
			GPXFile result;
			if (file == null) {
				result = app.getSavingTrackHelper().getCurrentGpx();
			} else {
				SelectedGpxFile selectedGpxFile = app.getSelectedGpxHelper().getSelectedFileByPath(file.getAbsolutePath());
				if (selectedGpxFile != null && selectedGpxFile.getGpxFile() != null && selectedGpxFile.getGpxFile().modifiedTime == file.lastModified()) {
					result = selectedGpxFile.getGpxFile();
				} else {
					result = GPXUtilities.loadGPXFile(file);
				}
			}
			if (result != null) {
				result.addGeneralTrack();
				long timeout = 200 - (System.currentTimeMillis() - startTime);
				if (timeout > 0) {
					try {
						Thread.sleep(timeout);
					} catch (InterruptedException e) {
						// ignore
					}
				}
			}
			return result;
		}

		@Override
		protected void onPostExecute(@Nullable GPXFile result) {
			TrackActivity activity = getTrackActivity();
			if (activity != null) {
				activity.setSupportProgressBarIndeterminateVisibility(false);
				if (result != null) {
					final GpxSelectionHelper helper = app.getSelectedGpxHelper();
					if (showTemporarily) {
						helper.selectGpxFile(result, false, false);
					} else {
						final SelectedGpxFile selectedGpx = helper.getSelectedFileByPath(result.path);
						if (selectedGpx != null && result.error == null) {
							selectedGpx.setGpxFile(result);
						}
					}
				}
				if (!activity.stopped) {
					activity.onGPXFileReady(result);
				}
			}
		}
	}
}
