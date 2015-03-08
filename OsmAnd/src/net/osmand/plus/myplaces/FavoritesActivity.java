/**
 *
 */
package net.osmand.plus.myplaces;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import net.osmand.plus.OsmAndLocationProvider;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.FavoritesTreeFragment;
import net.osmand.plus.activities.TabActivity;
import net.osmand.plus.myplaces.AvailableGPXFragment;
import net.osmand.plus.views.controls.PagerSlidingTabStrip;
import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ImageSpan;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;

/**
 *
 */
public class FavoritesActivity extends TabActivity {

//	private static final String FAVOURITES_INFO = "FAVOURITES_INFO";
	private static final String TRACKS = "TRACKS";
//	private static final String SELECTED_TRACK = "SELECTED_TRACK";
	public static int FAVORITES_TAB = 0;
	public static int GPX_TAB = 1;
	public static int SELECTED_GPX_TAB = 2;
	public static int NOTES_TAB = 3;
	public static int OSM_EDITS_TAB = 4;
	public static String TAB_PARAM = "TAB_PARAM";
	protected List<WeakReference<Fragment>> fragList = new ArrayList<WeakReference<Fragment>>();

	@Override
	public void onCreate(Bundle icicle) {
		((OsmandApplication) getApplication()).applyTheme(this);
		super.onCreate(icicle);
		getSupportActionBar().setTitle(R.string.shared_string_my_places);
		getSupportActionBar().setElevation(0);

		File[] lf = ((OsmandApplication) getApplication()).getAppPath(TRACKS).listFiles();
		boolean hasGpx = false;
		if (lf != null) {
			for (File t : lf) {
				if (t.isDirectory() || (t.getName().toLowerCase().endsWith(".gpx"))) {
					hasGpx = true;
					break;
				}
			}
		}

		setContentView(R.layout.tab_content);

		PagerSlidingTabStrip mSlidingTabLayout = (PagerSlidingTabStrip) findViewById(R.id.sliding_tabs);
		OsmandSettings settings = ((OsmandApplication) getApplication()).getSettings();
		
		ViewPager mViewPager = (ViewPager) findViewById(R.id.pager);

		List<TabItem> mTabs = new ArrayList<TabItem>();
//		mTabs.add(new TabItem("\t\t\t", FavoritesTreeFragment.class));
		mTabs.add(getTabIndicator(R.string.my_favorites, FavoritesTreeFragment.class));
		if (hasGpx) {
			mTabs.add(getTabIndicator(R.string.my_tracks, AvailableGPXFragment.class));
		}
		OsmandPlugin.addMyPlacesTabPlugins(this, mTabs, getIntent());
		
		Integer tab = settings.FAVORITES_TAB.get();
		if (tab == NOTES_TAB) {
			tab = mTabs.size() -1;
		} else if (tab == OSM_EDITS_TAB) {
			//TODO implement method of counting position for other plugins
		}
		
		setViewPagerAdapter(mViewPager, mTabs);
		mSlidingTabLayout.setViewPager(mViewPager);

		if (tab > mTabs.size() - 1){
			tab = 0;
		}
		mViewPager.setCurrentItem(tab );
		// setupHomeButton();
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

	private OsmAndLocationProvider getLocationProvider() {
		return getMyApplication().getLocationProvider();
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

