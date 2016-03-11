/**
 *
 */
package net.osmand.plus.myplaces;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.SearchView;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ImageSpan;
import android.view.MenuItem;
import android.widget.ImageView;

import net.osmand.IndexConstants;
import net.osmand.plus.OsmAndLocationProvider;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.FavoritesTreeFragment;
import net.osmand.plus.activities.TabActivity;
import net.osmand.plus.views.controls.PagerSlidingTabStrip;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class FavoritesActivity extends TabActivity {

	public static final int  GPX_TAB = R.string.shared_string_my_tracks;
	public static final int  FAV_TAB = R.string.shared_string_my_favorites;
	protected List<WeakReference<Fragment>> fragList = new ArrayList<>();
	private int tabSize;

	@Override
	public void onCreate(Bundle icicle) {
		((OsmandApplication) getApplication()).applyTheme(this);
		super.onCreate(icicle);
		//noinspection ConstantConditions
		getSupportActionBar().setTitle(R.string.shared_string_my_places);
		getSupportActionBar().setElevation(0);

		
		setContentView(R.layout.tab_content);
		List<TabItem> mTabs = getTabItems();
		setTabs(mTabs);
		// setupHomeButton();
	}
	
	

	private void setTabs(List<TabItem> mTabs) {
		PagerSlidingTabStrip mSlidingTabLayout = (PagerSlidingTabStrip) findViewById(R.id.sliding_tabs);
		OsmandSettings settings = ((OsmandApplication) getApplication()).getSettings();
		ViewPager mViewPager = (ViewPager) findViewById(R.id.pager);
		Integer tabId = settings.FAVORITES_TAB.get();
		int tab = 0;
		for(int i = 0; i < mTabs.size(); i++) {
			if(mTabs.get(i).resId == tabId) {
				tab = i;
			}
		}
		tabSize = mTabs.size();
		setViewPagerAdapter(mViewPager, mTabs);
		mSlidingTabLayout.setViewPager(mViewPager);
		mViewPager.setCurrentItem(tab);
	}

	private List<TabItem> getTabItems() {
		File[] lf = ((OsmandApplication) getApplication()).getAppPath(IndexConstants.GPX_INDEX_DIR).listFiles();
		boolean hasGpx = false;
		if (lf != null) {
			for (File t : lf) {
				if (t.isDirectory() || (t.getName().toLowerCase().endsWith(".gpx"))) {
					hasGpx = true;
					break;
				}
			}
		}

		List<TabItem> mTabs = new ArrayList<>();
		mTabs.add(getTabIndicator(R.string.shared_string_my_favorites, FavoritesTreeFragment.class));
		if (hasGpx) {
			mTabs.add(getTabIndicator(R.string.shared_string_my_tracks, AvailableGPXFragment.class));
		}
		OsmandPlugin.addMyPlacesTabPlugins(this, mTabs, getIntent());
		return mTabs;
	}

	@Override
	public void onAttachFragment(Fragment fragment) {
		fragList.add(new WeakReference<>(fragment));
	}

	@Override
	protected void onResume() {
		super.onResume();
		List<TabItem> mTabs = getTabItems();
		if(mTabs.size() != tabSize ) {
			setTabs(mTabs);
		}
	}

	public OsmandApplication getMyApplication() {
		return (OsmandApplication) getApplication();
	}

	private OsmAndLocationProvider getLocationProvider() {
		return getMyApplication().getLocationProvider();
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

	public static void updateSearchView(Activity activity, SearchView searchView) {
		//do not ever do like this
		OsmandApplication app = (OsmandApplication) activity.getApplication();
		if (!app.getSettings().isLightContent()) {
			return;
		}
		try {
			ImageView cancelIcon = (ImageView) searchView.findViewById(R.id.search_close_btn);
			cancelIcon.setImageResource(R.drawable.ic_action_gremove_dark);
			//styling search hint icon and text
			SearchView.SearchAutoComplete searchEdit = (SearchView.SearchAutoComplete) searchView.findViewById(R.id.search_src_text);
			searchEdit.setTextColor(activity.getResources().getColor(R.color.color_white));
			SpannableStringBuilder stopHint = new SpannableStringBuilder("   ");
			Float rawTextSize = searchEdit.getTextSize();
			int textSize = (int) (rawTextSize * 1.25);

			//setting icon as spannable
			Drawable searchIcon = activity.getResources().getDrawable(R.drawable.ic_action_search_dark);
			searchIcon.setBounds(0,0, textSize, textSize);
			stopHint.setSpan(new ImageSpan(searchIcon), 1, 2, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			searchEdit.setHint(stopHint);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}

