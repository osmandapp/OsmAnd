/**
 *
 */
package net.osmand.plus.myplaces;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
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
import net.osmand.plus.helpers.GpxImportHelper;
import net.osmand.plus.views.controls.PagerSlidingTabStrip;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class FavoritesActivity extends TabActivity {

	private static final int OPEN_GPX_DOCUMENT_REQUEST = 1006;
	private static final int IMPORT_FAVOURITES_REQUEST = 1007;

	public static final String GROUP_NAME_TO_SHOW = "group_name_to_show";

	public static final String OPEN_FAVOURITES_TAB = "open_favourites_tab";
	public static final String OPEN_MY_PLACES_TAB = "open_my_places_tab";

	public static final int  GPX_TAB = R.string.shared_string_my_tracks;
	public static final int  FAV_TAB = R.string.shared_string_my_favorites;
	protected List<WeakReference<Fragment>> fragList = new ArrayList<>();
	private int tabSize;
	private GpxImportHelper gpxImportHelper;
	private String groupNameToShow;

	@Override
	public void onCreate(Bundle icicle) {
		OsmandApplication app = (OsmandApplication) getApplication();
		app.applyTheme(this);
		super.onCreate(icicle);

		app.logEvent(this, "myplaces_open");

		gpxImportHelper = new GpxImportHelper(this, getMyApplication(), null);

		//noinspection ConstantConditions
		getSupportActionBar().setTitle(R.string.shared_string_my_places);
		getSupportActionBar().setElevation(0);

		
		setContentView(R.layout.tab_content);
		List<TabItem> mTabs = getTabItems();
		setTabs(mTabs);
		// setupHomeButton();

		ViewPager mViewPager = (ViewPager) findViewById(R.id.pager);
		if (icicle == null) {
			Intent intent = getIntent();
			if (intent != null) {
				if (intent.hasExtra(OPEN_FAVOURITES_TAB) && intent.getBooleanExtra(OPEN_FAVOURITES_TAB, false)) {
					if (intent.hasExtra(GROUP_NAME_TO_SHOW)) {
						groupNameToShow = intent.getStringExtra(GROUP_NAME_TO_SHOW);
					}
					mViewPager.setCurrentItem(0, false);
				} else if (intent.hasExtra(OPEN_MY_PLACES_TAB) && intent.getBooleanExtra(OPEN_MY_PLACES_TAB, false)) {
					mViewPager.setCurrentItem(1, false);
				}
			}
		}
	}

	public String getGroupNameToShow() {
		return groupNameToShow;
	}

	public void addTrack() {
		Intent intent = getImportGpxIntent();
		startActivityForResult(intent, OPEN_GPX_DOCUMENT_REQUEST);
	}

	public void importFavourites() {
		Intent intent = getImportGpxIntent();
		startActivityForResult(intent, IMPORT_FAVOURITES_REQUEST);
	}

	private Intent getImportGpxIntent() {
		Intent intent = new Intent();
		String action;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
			action = Intent.ACTION_OPEN_DOCUMENT;
		} else {
			action = Intent.ACTION_GET_CONTENT;
		}
		intent.setAction(action);
		intent.setType("*/*");
		return intent;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == OPEN_GPX_DOCUMENT_REQUEST && resultCode == Activity.RESULT_OK) {
			if (data != null) {
				Uri uri = data.getData();
				AvailableGPXFragment gpxFragment = getGpxFragment();
				if (gpxFragment!= null) {
					gpxFragment.startImport();
				}
				gpxImportHelper.setGpxImportCompleteListener(new GpxImportHelper.OnGpxImportCompleteListener() {
					@Override
					public void onComplete(boolean success) {
						AvailableGPXFragment gpxFragment = getGpxFragment();
						if (gpxFragment!= null) {
							gpxFragment.finishImport(success);
						}
						gpxImportHelper.setGpxImportCompleteListener(null);
					}
				});
				if (!gpxImportHelper.handleGpxImport(uri, false)) {
					if (gpxFragment!= null) {
						gpxFragment.finishImport(false);
					}
				}
			}
		} else if (requestCode == IMPORT_FAVOURITES_REQUEST && resultCode == Activity.RESULT_OK) {
			if (data != null) {
				Uri uri = data.getData();
				gpxImportHelper.handleFavouritesImport(uri);
			}
		} else {
			super.onActivityResult(requestCode, resultCode, data);
		}
	}

	private AvailableGPXFragment getGpxFragment() {
		AvailableGPXFragment gpxFragment = null;
		for (WeakReference<Fragment> f : fragList) {
			Fragment frag = f.get();
			if (frag instanceof AvailableGPXFragment) {
				gpxFragment = (AvailableGPXFragment) frag;
			}
		}
		return gpxFragment;
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
		List<TabItem> mTabs = new ArrayList<>();
		mTabs.add(getTabIndicator(FAV_TAB, FavoritesTreeFragment.class));
		mTabs.add(getTabIndicator(GPX_TAB, AvailableGPXFragment.class));
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

