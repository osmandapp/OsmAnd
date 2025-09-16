package net.osmand.plus.myplaces;

import static net.osmand.plus.backup.ui.BackupAuthorizationFragment.OPEN_BACKUP_AUTH;
import static net.osmand.plus.helpers.IntentHelper.REQUEST_CODE_CREATE_FILE;
import static net.osmand.plus.helpers.MapFragmentsHelper.CLOSE_ALL_FRAGMENTS;
import static net.osmand.plus.mapcontextmenu.other.ShareMenu.KEY_SAVE_FILE_NAME;
import static net.osmand.plus.myplaces.favorites.dialogs.FavoritesSearchFragment.FAV_SEARCH_QUERY_KEY;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;
import androidx.viewpager.widget.ViewPager.SimpleOnPageChangeListener;

import net.osmand.data.PointDescription;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.TabActivity;
import net.osmand.plus.importfiles.ImportHelper;
import net.osmand.plus.myplaces.favorites.dialogs.FavoritesSearchFragment;
import net.osmand.plus.myplaces.favorites.dialogs.FavoritesTreeFragment;
import net.osmand.plus.myplaces.favorites.dialogs.FragmentStateHolder;
import net.osmand.plus.myplaces.tracks.dialogs.AvailableTracksFragment;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.InsetsUtils;
import net.osmand.plus.views.controls.PagerSlidingTabStrip;
import net.osmand.util.Algorithms;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class MyPlacesActivity extends TabActivity {


	public static final String TAB_ID = "selected_tab_id";

	public static final int GPX_TAB = R.string.shared_string_tracks;
	public static final int FAV_TAB = R.string.shared_string_my_favorites;

	private ViewPager viewPager;
	private final List<WeakReference<FragmentStateHolder>> fragmentsStateList = new ArrayList<>();
	private int tabSize;

	private Bundle intentParams;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		app.applyTheme(this);
		super.onCreate(savedInstanceState);

		app.logEvent("myplaces_open");
		app.getImportHelper().setUiActivity(this);

		updateToolbar();
		setContentView(R.layout.my_places);
		viewPager = findViewById(R.id.pager);

		List<TabItem> tabItems = getTabItems();
		setTabs(tabItems);

		if (savedInstanceState == null) {
			Intent intent = getIntent();

			if (intent != null) {
				Bundle bundle = intent.getExtras();
				if (bundle != null && bundle.containsKey(FAV_SEARCH_QUERY_KEY)) {
					String searchQuery = bundle.getString(FAV_SEARCH_QUERY_KEY, "");
					FavoritesSearchFragment.showInstance(this, searchQuery);
				}

				if (intent.hasExtra(MapActivity.INTENT_PARAMS)) {
					intentParams = intent.getBundleExtra(MapActivity.INTENT_PARAMS);
					int tabId = intentParams.getInt(TAB_ID, FAV_TAB);
					int pagerItem = 0;
					for (int n = 0; n < tabItems.size(); n++) {
						if (tabItems.get(n).resId == tabId) {
							pagerItem = n;
							break;
						}
					}
					viewPager.setCurrentItem(pagerItem, false);
				}
			}
		}
	}

	@Nullable
	@Override
	public List<Integer> getBottomContainersIds() {
		List<Integer> ids = new ArrayList<>();
		ids.add(R.id.bottomControls);
		return ids;
	}

	public void updateToolbar() {
		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			actionBar.setTitle(R.string.shared_string_my_places);
			actionBar.setElevation(0);
		}
	}

	private void setTabs(@NonNull List<TabItem> tabItems) {
		PagerSlidingTabStrip mSlidingTabLayout = findViewById(R.id.sliding_tabs);
		Integer tabId = settings.FAVORITES_TAB.get();
		int tab = 0;
		for (int i = 0; i < tabItems.size(); i++) {
			if (tabItems.get(i).resId == tabId) {
				tab = i;
			}
		}
		tabSize = tabItems.size();
		setViewPagerAdapter(viewPager, tabItems);
		mSlidingTabLayout.setViewPager(viewPager);
		viewPager.setCurrentItem(tab);
	}

	@NonNull
	private List<TabItem> getTabItems() {
		List<TabItem> mTabs = new ArrayList<>();
		mTabs.add(getTabIndicator(FAV_TAB, FavoritesTreeFragment.class));
		mTabs.add(getTabIndicator(GPX_TAB, AvailableTracksFragment.class));
		PluginsHelper.addMyPlacesTabPlugins(this, mTabs, getIntent());
		return mTabs;
	}

	@Nullable
	public Bundle storeCurrentState() {
		int currentItem = viewPager.getCurrentItem();
		if (currentItem >= 0 && currentItem < fragmentsStateList.size()) {
			FragmentStateHolder stateHolder = fragmentsStateList.get(currentItem).get();
			if (stateHolder != null) {
				return stateHolder.storeState();
			}
		}
		return null;
	}

	@Override
	public void onAttachFragment(@NonNull Fragment fragment) {
		if (fragment instanceof FragmentStateHolder) {
			fragment.setArguments(intentParams);
			fragmentsStateList.add(new WeakReference<>((FragmentStateHolder) fragment));
		}
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		setIntent(intent);

		if (intent.hasExtra(KEY_SAVE_FILE_NAME)) {
			String filePath = intent.getStringExtra("file_path");
			if (Algorithms.isEmpty(filePath)) {
				return;
			}
			File fileToSave = new File(filePath);

			Intent createFileIntent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
			createFileIntent.setType("*/*");
			createFileIntent.putExtra(Intent.EXTRA_TITLE, fileToSave.getName());

			AndroidUtils.startActivityForResultIfSafe(this, createFileIntent, REQUEST_CODE_CREATE_FILE);
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		List<TabItem> tabItems = getTabItems();
		if (tabItems.size() != tabSize) {
			setTabs(tabItems);
		}
		app.getImportHelper().setUiActivity(this);
		viewPager.addOnPageChangeListener(new SimpleOnPageChangeListener() {
			@Override
			public void onPageSelected(int position) {
				settings.FAVORITES_TAB.set(tabItems.get(position).resId);
			}
		});
	}

	@Override
	protected void onPause() {
		super.onPause();
		viewPager.clearOnPageChangeListeners();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		ImportHelper importHelper = app.getImportHelper();
		importHelper.resetUIActivity(this);
		removeActivityResultListener(importHelper.getSaveFileResultListener());
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int itemId = item.getItemId();
		if (itemId == android.R.id.home) {
			onBackPressed();
			return true;
		}
		return false;
	}

	@Override
	public List<Fragment> getActiveTalkbackFragments() {
		List<Fragment> fragmentsWithoutTabs = new ArrayList<>();
		for (Fragment fragment : getSupportFragmentManager().getFragments()) {
			boolean isTabFragment = false;
			for (TabItem tabItem : getTabItems()) {
				if (fragment.getClass() == tabItem.fragment) {
					isTabFragment = true;
					break;
				}
			}
			if (!isTabFragment) {
				fragmentsWithoutTabs.add(fragment);
			}
		}
		return fragmentsWithoutTabs;
	}

	@Override
	public void setActivityAccessibility(boolean hideActivity) {
		View pagerContent = findViewById(R.id.pager_content);
		View slidingTabs = findViewById(R.id.sliding_tabs);
		int accessibility = getActiveTalkbackFragments().isEmpty() ? View.IMPORTANT_FOR_ACCESSIBILITY_YES : View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS;
		pagerContent.setImportantForAccessibility(accessibility);
		slidingTabs.setImportantForAccessibility(accessibility);
	}

	public void showOnMap(@Nullable FragmentStateHolder fragment, double latitude, double longitude,
	                      int zoom, PointDescription pointDescription, boolean addToHistory, Object toShow) {
		settings.setMapLocationToShow(latitude, longitude, zoom, pointDescription, addToHistory, toShow);

		Bundle args = new Bundle();
		args.putBoolean(CLOSE_ALL_FRAGMENTS, true);

		Bundle bundle = fragment != null ? fragment.storeState() : null;
		MapActivity.launchMapActivityMoveToTop(this, bundle, null, args);
	}

	public void showOsmAndCloud(@Nullable FragmentStateHolder fragment) {
		Bundle args = new Bundle();
		args.putBoolean(OPEN_BACKUP_AUTH, true);
		args.putBoolean(CLOSE_ALL_FRAGMENTS, true);

		Bundle bundle = fragment != null ? fragment.storeState() : null;
		MapActivity.launchMapActivityMoveToTop(this, bundle, null, args);
	}

	@Nullable
	public <T> T getFragment(String fragmentTag) {
		Fragment fragment = getSupportFragmentManager().findFragmentByTag(fragmentTag);
		return fragment != null && !fragment.isDetached() && !fragment.isRemoving() ? (T) fragment : null;
	}
}