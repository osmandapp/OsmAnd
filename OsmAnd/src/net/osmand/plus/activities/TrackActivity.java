package net.osmand.plus.activities;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;

import net.osmand.AndroidUtils;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.QuadRect;
import net.osmand.plus.GPXDatabase.GpxDataItem;
import net.osmand.plus.GPXUtilities;
import net.osmand.plus.GPXUtilities.GPXFile;
import net.osmand.plus.GPXUtilities.TrkSegment;
import net.osmand.plus.GPXUtilities.WptPt;
import net.osmand.plus.GpxSelectionHelper;
import net.osmand.plus.GpxSelectionHelper.GpxDisplayGroup;
import net.osmand.plus.GpxSelectionHelper.SelectedGpxFile;
import net.osmand.plus.OsmAndAppCustomization;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.measurementtool.NewGpxData;
import net.osmand.plus.myplaces.FavoritesActivity;
import net.osmand.plus.myplaces.SplitSegmentDialogFragment;
import net.osmand.plus.myplaces.TrackPointFragment;
import net.osmand.plus.myplaces.TrackSegmentFragment;
import net.osmand.plus.views.AddGpxPointBottomSheetHelper.NewGpxPoint;
import net.osmand.plus.views.controls.PagerSlidingTabStrip;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class TrackActivity extends TabActivity {

	public static final String TRACK_FILE_NAME = "TRACK_FILE_NAME";
	public static final String OPEN_POINTS_TAB = "OPEN_POINTS_TAB";
	public static final String OPEN_TRACKS_LIST = "OPEN_TRACKS_LIST";
	public static final String CURRENT_RECORDING = "CURRENT_RECORDING";
	protected List<WeakReference<Fragment>> fragList = new ArrayList<>();
	private OsmandApplication app;
	protected PagerSlidingTabStrip slidingTabLayout;
	private File file = null;
	private GPXFile gpxFile;
	private GpxDataItem gpxDataItem;
	ViewPager mViewPager;
	private long modifiedTime = -1;
	private List<GpxDisplayGroup> displayGroups;
	private List<GpxDisplayGroup> originalGroups = new ArrayList<>();
	private boolean stopped = false;
	private boolean openPointsTab = false;
	private boolean openTracksList = false;

	@Override
	public void onCreate(Bundle icicle) {
		this.app = getMyApplication();
		app.applyTheme(this);
		super.onCreate(icicle);
		Intent intent = getIntent();
		if (intent == null || (!intent.hasExtra(TRACK_FILE_NAME) &&
				!intent.hasExtra(CURRENT_RECORDING))) {
			Log.e("TrackActivity", "Required extra '" + TRACK_FILE_NAME + "' is missing");
			finish();
			return;
		}
		file = null;
		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			if (intent.hasExtra(TRACK_FILE_NAME)) {
				file = new File(intent.getStringExtra(TRACK_FILE_NAME));
				String fn = file.getName().replace(".gpx", "").replace("/", " ").replace("_", " ");
				actionBar.setTitle(fn);
			} else {
				actionBar.setTitle(getString(R.string.shared_string_currently_recording_track));
			}
			actionBar.setElevation(0);
		}
		if (intent.hasExtra(OPEN_POINTS_TAB)) {
			openPointsTab = true;
		}
		if (intent.hasExtra(OPEN_TRACKS_LIST)) {
			openTracksList = true;
		}
		setContentView(R.layout.tab_content);
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
		double left = 0, right = 0;
		double top = 0, bottom = 0;
		if (getGpx() != null) {
			for (GPXUtilities.Track track : getGpx().tracks) {
				for (GPXUtilities.TrkSegment segment : track.segments) {
					for (WptPt p : segment.points) {
						if (left == 0 && right == 0) {
							left = p.getLongitude();
							right = p.getLongitude();
							top = p.getLatitude();
							bottom = p.getLatitude();
						} else {
							left = Math.min(left, p.getLongitude());
							right = Math.max(right, p.getLongitude());
							top = Math.max(top, p.getLatitude());
							bottom = Math.min(bottom, p.getLatitude());
						}
					}
				}
			}
			for (WptPt p : getGpx().getPoints()) {
				if (left == 0 && right == 0) {
					left = p.getLongitude();
					right = p.getLongitude();
					top = p.getLatitude();
					bottom = p.getLatitude();
				} else {
					left = Math.min(left, p.getLongitude());
					right = Math.max(right, p.getLongitude());
					top = Math.max(top, p.getLatitude());
					bottom = Math.min(bottom, p.getLatitude());
				}
			}
			for (GPXUtilities.Route route : getGpx().routes) {
				for (WptPt p : route.points) {
					if (left == 0 && right == 0) {
						left = p.getLongitude();
						right = p.getLongitude();
						top = p.getLatitude();
						bottom = p.getLatitude();
					} else {
						left = Math.min(left, p.getLongitude());
						right = Math.max(right, p.getLongitude());
						top = Math.max(top, p.getLatitude());
						bottom = Math.min(bottom, p.getLatitude());
					}
				}
			}
		}
		return new QuadRect(left, top, right, bottom);
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
	}

	@Override
	protected void onResume() {
		super.onResume();
		stopped = false;
		slidingTabLayout = (PagerSlidingTabStrip) findViewById(R.id.sliding_tabs);
		if (slidingTabLayout != null) {
			slidingTabLayout.setShouldExpand(true);

			mViewPager = (ViewPager) findViewById(R.id.pager);

			setViewPagerAdapter(mViewPager, new ArrayList<TabActivity.TabItem>());
			slidingTabLayout.setViewPager(mViewPager);
			new AsyncTask<Void, Void, GPXFile>() {

				protected void onPreExecute() {
					setSupportProgressBarIndeterminateVisibility(true);
				}

				@Override
				protected GPXFile doInBackground(Void... params) {
					long startTime = System.currentTimeMillis();
					GPXFile result;
					if (file == null) {
						result = getMyApplication().getSavingTrackHelper().getCurrentGpx();
					} else {
						SelectedGpxFile selectedGpxFile = getMyApplication().getSelectedGpxHelper().getSelectedFileByPath(file.getAbsolutePath());
						if (selectedGpxFile != null && selectedGpxFile.getGpxFile() != null) {
							result = selectedGpxFile.getGpxFile();
						} else {
							result = GPXUtilities.loadGPXFile(TrackActivity.this, file);
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

				protected void onPostExecute(GPXFile result) {
					setSupportProgressBarIndeterminateVisibility(false);

					if (!stopped) {
						setGpx(result);
						setGpxDataItem(file != null ? getMyApplication().getGpxDatabase().getItem(file) : null);

						for (WeakReference<Fragment> f : fragList) {
							Fragment frag = f.get();
							if (frag instanceof TrackSegmentFragment) {
								((TrackSegmentFragment) frag).updateContent();
							} else if (frag instanceof SplitSegmentDialogFragment) {
								((SplitSegmentDialogFragment) frag).updateContent();
							} else if (frag instanceof TrackPointFragment) {
								((TrackPointFragment) frag).setContent();
							}
						}
						((OsmandFragmentPagerAdapter) mViewPager.getAdapter()).addTab(
								getTabIndicator(R.string.gpx_track, TrackSegmentFragment.class));
						if (isHavingWayPoints() || isHavingRoutePoints()) {
							((OsmandFragmentPagerAdapter) mViewPager.getAdapter()).addTab(
									getTabIndicator(R.string.points, TrackPointFragment.class));
							if (openPointsTab) {
								mViewPager.setCurrentItem(1, false);
							}
						} else {
							slidingTabLayout.setVisibility(View.GONE);
							getSupportActionBar().setElevation(AndroidUtils.dpToPx(getMyApplication(), 4f));
						}
					}
				}
			}.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void) null);
		}
	}

	public OsmandApplication getMyApplication() {
		return (OsmandApplication) getApplication();
	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	@Override
	protected void onStop() {
		super.onStop();
		stopped = true;
	}

	public Toolbar getClearToolbar(boolean visible) {
		final Toolbar tb = (Toolbar) findViewById(R.id.bottomControls);
		if (tb != null) {
			tb.setTitle(null);
			tb.getMenu().clear();
			tb.setVisibility(visible ? View.VISIBLE : View.GONE);
		}
		return tb;
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

	boolean isHavingWayPoints() {
		return getGpx() != null && getGpx().hasWptPt();
	}

	boolean isHavingRoutePoints() {
		return getGpx() != null && getGpx().hasRtePt();
	}

	public GPXFile getGpx() {
		return gpxFile;
	}

	public GpxDataItem getGpxDataItem() {
		return gpxDataItem;
	}
}
