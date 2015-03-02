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
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.views.controls.PagerSlidingTabStrip;
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
	public static String TAB_PARAM = "TAB_PARAM";
	protected List<WeakReference<Fragment>> fragList = new ArrayList<WeakReference<Fragment>>();
	private File file = null;
	private GPXFile result;

	@Override
	public void onCreate(Bundle icicle) {
		((OsmandApplication) getApplication()).applyTheme(this);
		super.onCreate(icicle);
		Intent intent = getIntent();
		if (intent == null || !intent.hasExtra(TRACK_FILE_NAME)) {
			Log.e("TrackActivity", "Required extra '" + TRACK_FILE_NAME + "' is missing");
			finish();
			return;
		}

		file = new File(intent.getStringExtra(TRACK_FILE_NAME));
		String fn = file.getName().replace(".gpx", "").replace("/", " ").replace("_", " ");
		getSupportActionBar().setTitle(fn);
		getSupportActionBar().setElevation(0);
		setContentView(R.layout.tab_content);

		PagerSlidingTabStrip mSlidingTabLayout = (PagerSlidingTabStrip) findViewById(R.id.sliding_tabs);
		
		final ViewPager mViewPager = (ViewPager) findViewById(R.id.pager);

		setViewPagerAdapter(mViewPager, new ArrayList<TabActivity.TabItem>());
		mSlidingTabLayout.setViewPager(mViewPager);
		
		new AsyncTask<Void, Void, GPXFile>() {

			protected void onPreExecute() {
				setSupportProgressBarIndeterminateVisibility(true);
				
			};
			@Override
			protected GPXFile doInBackground(Void... params) {
				return GPXUtilities.loadGPXFile(TrackActivity.this, file);
			}
			protected void onPostExecute(GPXFile result) {
				setSupportProgressBarIndeterminateVisibility(false);
//				List<TabItem> items = new ArrayList<TabActivity.TabItem>();
//				items.add(getTabIndicator(R.string.selected_track, SelectedGPXFragment.class));
				setResult(result);
				((OsmandFragmentPagerAdapter) mViewPager.getAdapter()).addTab(
						getTabIndicator(R.string.selected_track, SelectedGPXFragment.class));
//				setViewPagerAdapter(mViewPager, items );
			};
		}.execute((Void)null);
		
	}

	protected void setResult(GPXFile result) {
		this.result = result;
	}
	
	public GPXFile getResult() {
		return result;
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

	public void setToolbarVisibility(boolean visible){
		findViewById(R.id.bottomControls).setVisibility(visible? View.VISIBLE : View.GONE);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int itemId = item.getItemId();
		switch (itemId) {
		case android.R.id.home:
			finish();
			return true;

		}
		return false;
	}

}

