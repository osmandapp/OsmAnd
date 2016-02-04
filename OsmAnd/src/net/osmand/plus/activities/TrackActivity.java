/**
 *
 */
package net.osmand.plus.activities;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import net.osmand.plus.GPXUtilities;
import net.osmand.plus.GPXUtilities.GPXFile;
import net.osmand.plus.GpxSelectionHelper;
import net.osmand.plus.GpxSelectionHelper.GpxDisplayGroup;
import net.osmand.plus.GpxSelectionHelper.SelectedGpxFile;
import net.osmand.plus.OsmAndAppCustomization;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.myplaces.FavoritesActivity;
import net.osmand.plus.myplaces.SelectedGPXFragment;
import net.osmand.plus.myplaces.TrackPointFragment;
import net.osmand.plus.myplaces.TrackRoutePointFragment;
import net.osmand.plus.myplaces.TrackSegmentFragment;
import net.osmand.plus.views.controls.PagerSlidingTabStrip;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;

/**
 *
 */
public class TrackActivity extends TabActivity {

	public static final String TRACK_FILE_NAME = "TRACK_FILE_NAME";
	public static final String CURRENT_RECORDING = "CURRENT_RECORDING";
	public static String TAB_PARAM = "TAB_PARAM";
	protected List<WeakReference<Fragment>> fragList = new ArrayList<WeakReference<Fragment>>();
	private File file = null;
	private GPXFile result;
	ViewPager mViewPager;
	private long modifiedTime = -1;
	private List<GpxDisplayGroup> displayGroups;

	@Override
	public void onCreate(Bundle icicle) {
		((OsmandApplication) getApplication()).applyTheme(this);
		super.onCreate(icicle);
		Intent intent = getIntent();
		if (intent == null || (!intent.hasExtra(TRACK_FILE_NAME) &&
				!intent.hasExtra(CURRENT_RECORDING))) {
			Log.e("TrackActivity", "Required extra '" + TRACK_FILE_NAME + "' is missing");
			finish();
			return;
		}
		file = null;
		if (intent.hasExtra(TRACK_FILE_NAME)) {
			file = new File(intent.getStringExtra(TRACK_FILE_NAME));
			String fn = file.getName().replace(".gpx", "").replace("/", " ").replace("_", " ");
			getSupportActionBar().setTitle(fn);
		} else {
			getSupportActionBar().setTitle(getString(R.string.shared_string_currently_recording_track));
		}
		getSupportActionBar().setElevation(0);
		setContentView(R.layout.tab_content);

		PagerSlidingTabStrip mSlidingTabLayout = (PagerSlidingTabStrip) findViewById(R.id.sliding_tabs);

		mViewPager = (ViewPager) findViewById(R.id.pager);

		setViewPagerAdapter(mViewPager, new ArrayList<TabActivity.TabItem>());
		mSlidingTabLayout.setViewPager(mViewPager);
		new AsyncTask<Void, Void, GPXFile>() {

			protected void onPreExecute() {
				setSupportProgressBarIndeterminateVisibility(true);

			};
			@Override
			protected GPXFile doInBackground(Void... params) {
				if(file == null) {
					return getMyApplication().getSavingTrackHelper().getCurrentGpx();
				}
				return GPXUtilities.loadGPXFile(TrackActivity.this, file);
			}
			protected void onPostExecute(GPXFile result) {
				setSupportProgressBarIndeterminateVisibility(false);

				setGpx(result);
				for(WeakReference<Fragment> f : fragList) {
					Fragment frag = f.get();
					if(frag instanceof SelectedGPXFragment) {
						((SelectedGPXFragment) frag).setContent();
					}
				}
				((OsmandFragmentPagerAdapter) mViewPager.getAdapter()).addTab(
						getTabIndicator(R.string.track_segments, TrackSegmentFragment.class));
				if (isHavingWayPoints()){
					((OsmandFragmentPagerAdapter) mViewPager.getAdapter()).addTab(
							getTabIndicator(R.string.track_points, TrackPointFragment.class));
				}
				if (isHavingRoutePoints()){
					((OsmandFragmentPagerAdapter) mViewPager.getAdapter()).addTab(
							getTabIndicator(R.string.route_points, TrackRoutePointFragment.class));
				}

			};
		}.execute((Void)null);

	}

	protected void setGpx(GPXFile result) {
		this.result = result;
		if(file == null) {
			result = getMyApplication().getSavingTrackHelper().getCurrentGpx();
		}
	}

	public List<GpxSelectionHelper.GpxDisplayGroup> getResult() {
		if(result == null) {
			return new ArrayList<GpxSelectionHelper.GpxDisplayGroup>();
		}
		if (result.modifiedTime != modifiedTime) {
			modifiedTime = result.modifiedTime;
			GpxSelectionHelper selectedGpxHelper = ((OsmandApplication) getApplication()).getSelectedGpxHelper();
			displayGroups = selectedGpxHelper.collectDisplayGroups(result);
			if (file != null) {
				SelectedGpxFile sf = selectedGpxHelper.getSelectedFileByPath(result.path);
				if (sf != null && file != null && sf.getDisplayGroups() != null) {
					displayGroups = sf.getDisplayGroups();
				}
			}
		}
		return displayGroups;
	}

	@Override
	public void onAttachFragment(Fragment fragment) {
		fragList.add(new WeakReference<Fragment>(fragment));
	}

	@Override
	protected void onResume() {
		super.onResume();

	}

	public OsmandApplication getMyApplication() {
		return (OsmandApplication) getApplication();
	}

	@Override
	protected void onPause() {
		super.onPause();
	}
	
	

	public Toolbar getClearToolbar(boolean visible) {
		final Toolbar tb = (Toolbar) findViewById(R.id.bottomControls);
		tb.setTitle(null);
		tb.getMenu().clear();
		tb.setVisibility(visible? View.VISIBLE : View.GONE);
		return tb;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int itemId = item.getItemId();
		switch (itemId) {
		case android.R.id.home:
			if (getIntent().hasExtra(MapActivity.INTENT_KEY_PARENT_MAP_ACTIVITY)) {
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

	boolean isHavingWayPoints(){
		return getGpx() != null && getGpx().hasWptPt();
	}

	boolean isHavingRoutePoints(){
		return getGpx() != null && getGpx().hasRtePt();
	}

	public GPXFile getGpx() {
		return result;
	}
}

