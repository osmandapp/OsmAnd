package net.osmand.plus.download;

import static net.osmand.plus.download.DownloadActivityType.NORMAL_FILE;
import static net.osmand.plus.download.DownloadActivityType.WIKIPEDIA_FILE;
import static net.osmand.plus.download.ui.SearchDialogFragment.SHOW_WIKI_KEY;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.appcompat.app.ActionBar;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager.widget.ViewPager;

import net.osmand.IProgress;
import net.osmand.IndexConstants;
import net.osmand.map.WorldRegion;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.activities.RestartActivity;
import net.osmand.plus.activities.TabActivity;
import net.osmand.plus.activities.TabActivity.OsmandFragmentPagerAdapter;
import net.osmand.plus.activities.TabActivity.TabItem;
import net.osmand.plus.download.DownloadIndexesThread.DownloadEvents;
import net.osmand.plus.download.local.dialogs.LocalCategoriesFragment;
import net.osmand.plus.download.ui.AskMapDownloadFragment;
import net.osmand.plus.download.ui.BannerAndDownloadFreeVersion;
import net.osmand.plus.download.ui.DownloadResourceGroupFragment;
import net.osmand.plus.download.ui.GoToMapFragment;
import net.osmand.plus.download.ui.SearchDialogFragment;
import net.osmand.plus.download.ui.UpdatesIndexFragment;
import net.osmand.plus.helpers.FileNameTranslationHelper;
import net.osmand.plus.inapp.InAppPurchaseHelper.InAppPurchaseTaskType;
import net.osmand.plus.inapp.InAppPurchaseUtils;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.accessibility.AccessibilityAssistant;
import net.osmand.plus.plugins.openseamaps.NauticalMapsPlugin;
import net.osmand.plus.plugins.srtm.SRTMPlugin;
import net.osmand.plus.resources.ResourceManager.ReloadIndexesListener;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.views.controls.PagerSlidingTabStrip;
import net.osmand.util.Algorithms;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DownloadActivity extends AbstractDownloadActivity implements DownloadEvents {


	public static final int UPDATES_TAB_NUMBER = 2;
	public static final int LOCAL_TAB_NUMBER = 1;
	public static final int DOWNLOAD_TAB_NUMBER = 0;
	public static final int PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 0;

	public static final String FILTER_KEY = "filter";
	public static final String FILTER_CAT = "filter_cat";
	public static final String FILTER_GROUP = "filter_group";

	public static final String TAB_TO_OPEN = "Tab_to_open";
	public static final String LOCAL_TAB = "local";
	public static final String DOWNLOAD_TAB = "download";
	public static final String UPDATES_TAB = "updates";
	public static final String REGION_TO_SEARCH = "search_region";


	private static final boolean SUGGEST_TO_DOWNLOAD_BASEMAP = false;
	private static boolean SUGGESTED_TO_DOWNLOAD_BASEMAP;

	private OsmandApplication app;
	private DownloadIndexesThread downloadThread;

	private BannerAndDownloadFreeVersion visibleBanner;
	private ViewPager viewPager;
	private AccessibilityAssistant accessibilityAssistant;
	private String filter;
	private String filterCat;
	private String filterGroup;
	private final List<TabItem> mTabs = new ArrayList<>();
	private Set<WeakReference<Fragment>> fragSet = new HashSet<>();
	private WorldRegion downloadItem;
	private String downloadTargetFileName;

	private boolean srtmDisabled;
	private boolean srtmNeedsInstallation;
	private boolean nauticalPluginDisabled;
	private boolean freeVersion;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		app = getMyApplication();
		downloadThread = app.getDownloadThread();
		app.applyTheme(this);
		super.onCreate(savedInstanceState);

		DownloadResources indexes = downloadThread.getIndexes();
		if (!indexes.isDownloadedFromInternet) {
			downloadThread.runReloadIndexFiles();
		}
		accessibilityAssistant = new AccessibilityAssistant(this);

		setContentView(R.layout.download_activity);
		updateToolbar();

		View downloadProgressLayout = findViewById(R.id.downloadProgressLayout);
		downloadProgressLayout.setVisibility(View.VISIBLE);
		BannerAndDownloadFreeVersion.updateDescriptionTextWithSize(app, downloadProgressLayout);

		viewPager = findViewById(R.id.pager);
		PagerSlidingTabStrip pagerSlidingTabs = findViewById(R.id.sliding_tabs);

		mTabs.add(new TabItem(R.string.downloads, getString(R.string.downloads), DownloadResourceGroupFragment.class));
		mTabs.add(new TabItem(R.string.download_tab_local, getString(R.string.download_tab_local), LocalCategoriesFragment.class));
		mTabs.add(new TabItem(R.string.download_tab_updates, getString(R.string.download_tab_updates), UpdatesIndexFragment.class));

		viewPager.setAdapter(new OsmandFragmentPagerAdapter(getSupportFragmentManager(), mTabs));
		pagerSlidingTabs.setViewPager(viewPager);
		pagerSlidingTabs.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {

			@Override
			public void onPageSelected(int position) {
				accessibilityAssistant.onPageSelected(position);
				visibleBanner.updateBannerInProgress();
			}

			@Override
			public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
				accessibilityAssistant.onPageScrolled(position, positionOffset, positionOffsetPixels);
			}

			@Override
			public void onPageScrollStateChanged(int state) {
				accessibilityAssistant.onPageScrollStateChanged(state);
			}
		});
		visibleBanner = new BannerAndDownloadFreeVersion(findViewById(R.id.mainLayout), this, true);
		if (shouldShowFreeVersionBanner(app)) {
			visibleBanner.updateFreeVersionBanner();
		}
		viewPager.setCurrentItem(loadCurrentTab());

		Intent intent = getIntent();
		if (intent != null && intent.getExtras() != null) {
			String region = getIntent().getStringExtra(REGION_TO_SEARCH);
			if (region != null && !region.isEmpty()) {
				if (getIntent().getBooleanExtra(SHOW_WIKI_KEY, false)) {
					showDialog(this, SearchDialogFragment.createInstance(region, true, NORMAL_FILE, WIKIPEDIA_FILE));
				} else {
					showDialog(this, SearchDialogFragment.createInstance(region, true, NORMAL_FILE));
				}
			}
			filter = intent.getExtras().getString(FILTER_KEY);
			filterCat = intent.getExtras().getString(FILTER_CAT);
			filterGroup = intent.getExtras().getString(FILTER_GROUP);
		}
	}

	public void updateToolbar() {
		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			actionBar.setTitle(R.string.maps_and_resources);
			actionBar.setElevation(0);
		}
	}

	private int loadCurrentTab() {
		Intent intent = getIntent();
		if (intent != null && intent.hasExtra(TAB_TO_OPEN)) {
			String tab = intent.getExtras().getString(TAB_TO_OPEN);
			if (tab != null) {
				switch (tab) {
					case DOWNLOAD_TAB:
						return DOWNLOAD_TAB_NUMBER;
					case LOCAL_TAB:
						return LOCAL_TAB_NUMBER;
					case UPDATES_TAB:
						return UPDATES_TAB_NUMBER;
				}
			}
		}
		return DOWNLOAD_TAB_NUMBER;
	}

	public boolean isInAppPurchaseAllowed() {
		return true;
	}

	@Override
	public void onInAppPurchaseError(InAppPurchaseTaskType taskType, String error) {
		visibleBanner.updateFreeVersionBanner();
	}

	@Override
	public void onInAppPurchaseGetItems() {
		visibleBanner.updateFreeVersionBanner();
		initAppStatusVariables();
	}

	@Override
	public void onInAppPurchaseItemPurchased(String sku) {
		visibleBanner.updateFreeVersionBanner();
		initAppStatusVariables();
	}

	public DownloadIndexesThread getDownloadThread() {
		return downloadThread;
	}

	public AccessibilityAssistant getAccessibilityAssistant() {
		return accessibilityAssistant;
	}

	@Override
	public void onAttachFragment(@NonNull Fragment fragment) {
		fragSet.add(new WeakReference<>(fragment));
	}

	@Override
	protected void onResume() {
		super.onResume();
		initAppStatusVariables();
		downloadThread.setUiActivity(this);
		app.getImportHelper().setUiActivity(this);
		downloadInProgress();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int itemId = item.getItemId();
		switch (itemId) {
			case android.R.id.home:
				onBackPressed();
				return true;

		}
		return false;
	}

	@Override
	public void onPause() {
		super.onPause();
		downloadThread.resetUiActivity(this);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		app.getImportHelper().resetUIActivity(this);
	}

	@Override
	@UiThread
	public void downloadHasFinished() {
		visibleBanner.updateBannerInProgress();
		if (downloadItem != null && downloadItem != app.getRegions().getWorldRegion()
				&& !WorldRegion.WORLD_BASEMAP.equals(downloadItem.getRegionDownloadNameLC())) {

			if (!Algorithms.isEmpty(downloadTargetFileName)) {
				File f = new File(downloadTargetFileName);
				if (f.exists() && f.lastModified() > System.currentTimeMillis() - 10000) {
					downloadThread.initSettingsFirstMap(downloadItem);
					showGoToMap(downloadItem);
				}
			}
			downloadItem = null;
			downloadTargetFileName = null;
		}
		if (!Algorithms.isEmpty(downloadTargetFileName)) {
			File f = new File(downloadTargetFileName);
			if (f.exists()) {
				String fileName = f.getName();
				if (fileName.endsWith(IndexConstants.FONT_INDEX_EXT)) {
					RestartActivity.doRestart(this);
				} else if (fileName.startsWith(FileNameTranslationHelper.SEA_DEPTH)) {
					app.getSettings().getCustomRenderBooleanProperty("depthContours").set(true);
				}
			}
			downloadItem = null;
			downloadTargetFileName = null;
		}
		for (WeakReference<Fragment> ref : fragSet) {
			Fragment f = ref.get();
			if (f instanceof DownloadEvents && f.isAdded()) {
				((DownloadEvents) f).downloadHasFinished();
			}
		}
	}

	@Override
	@UiThread
	public void downloadInProgress() {
		if (accessibilityAssistant.isUiUpdateDiscouraged()) return;
		accessibilityAssistant.lockEvents();
		visibleBanner.updateBannerInProgress();
		showDownloadWorldMapIfNeeded();
		for (WeakReference<Fragment> ref : fragSet) {
			Fragment f = ref.get();
			if (f instanceof DownloadEvents && f.isAdded()) {
				((DownloadEvents) f).downloadInProgress();
			}
		}
		accessibilityAssistant.unlockEvents();
	}


	@Override
	@UiThread
	public void onUpdatedIndexesList() {
		visibleBanner.updateBannerInProgress();
		for (WeakReference<Fragment> ref : fragSet) {
			Fragment f = ref.get();
			if (f instanceof DownloadEvents && f.isAdded()) {
				((DownloadEvents) f).onUpdatedIndexesList();
			}
		}
		downloadHasFinished();
	}

	public void updateBanner() {
		visibleBanner.updateBannerInProgress();
	}

	public int getCurrentTab() {
		return viewPager.getCurrentItem();
	}

	public void showDialog(FragmentActivity activity, DialogFragment fragment) {
		fragment.show(activity.getSupportFragmentManager(), "dialog");
	}

	public static boolean isDownloadingPermitted(@NonNull OsmandSettings settings) {
		Integer mapsDownloaded = settings.NUMBER_OF_FREE_DOWNLOADS.get();
		int downloadsLeft = DownloadValidationManager.MAXIMUM_AVAILABLE_FREE_DOWNLOADS - mapsDownloaded;
		return Math.max(downloadsLeft, 0) > 0;
	}

	public static boolean shouldShowFreeVersionBanner(OsmandApplication application) {
		return !Version.isPaidVersion(application) || application.getSettings().SHOULD_SHOW_FREE_VERSION_BANNER.get();
	}

	public void reloadLocalIndexes() {
		app.getResourceManager().reloadIndexesAsync(IProgress.EMPTY_PROGRESS, new ReloadIndexesListener() {
			@Override
			public void reloadIndexesStarted() {
				setSupportProgressBarIndeterminateVisibility(true);
			}

			@Override
			public void reloadIndexesFinished(@NonNull List<String> warnings) {
				setSupportProgressBarIndeterminateVisibility(false);
				if (!Algorithms.isEmpty(warnings)) {
					app.showToastMessage(AndroidUtils.formatWarnings(warnings).toString());
				}
				onUpdatedIndexesList();
			}
		});
	}

	public void setDownloadItem(WorldRegion region, String targetFileName) {
		if (downloadItem == null) {
			downloadItem = region;
			downloadTargetFileName = targetFileName;
		} else if (region == null) {
			downloadItem = null;
			downloadTargetFileName = null;
		}
	}

	private void showGoToMap(@NonNull WorldRegion region) {
		GoToMapFragment fragment = new GoToMapFragment();
		fragment.setRegionCenter(region.getRegionCenter());
		fragment.setRegionName(region.getLocaleName());
		fragment.show(getSupportFragmentManager(), GoToMapFragment.TAG);
	}

	private void showDownloadWorldMapIfNeeded() {
		if (downloadThread.getCurrentDownloadingItem() == null) {
			return;
		}
		IndexItem item = downloadThread.getIndexes().getWorldBaseMapItem();
		if (SUGGEST_TO_DOWNLOAD_BASEMAP && !SUGGESTED_TO_DOWNLOAD_BASEMAP && item != null
				&& item.isDownloaded() && item.isOutdated() && !downloadThread.isDownloading(item)) {
			SUGGESTED_TO_DOWNLOAD_BASEMAP = true;

			AskMapDownloadFragment fragment = new AskMapDownloadFragment();
			fragment.setIndexItem(item);
			fragment.show(getSupportFragmentManager(), AskMapDownloadFragment.TAG);
		}
	}

	public String getFilterAndClear() {
		String res = filter;
		filter = null;
		return res;
	}

	public String getFilterCatAndClear() {
		String res = filterCat;
		filterCat = null;
		return res;
	}

	public String getFilterGroupAndClear() {
		String res = filterGroup;
		filterGroup = null;
		return res;
	}

	public boolean isSrtmDisabled() {
		return srtmDisabled;
	}

	public boolean isSrtmNeedsInstallation() {
		return srtmNeedsInstallation;
	}

	public boolean isNauticalPluginDisabled() {
		return nauticalPluginDisabled;
	}

	public boolean isFreeVersion() {
		return freeVersion;
	}

	public void initAppStatusVariables() {
		srtmDisabled = !PluginsHelper.isActive(SRTMPlugin.class) && !InAppPurchaseUtils.isContourLinesAvailable(app);
		nauticalPluginDisabled = !PluginsHelper.isActive(NauticalMapsPlugin.class);
		freeVersion = Version.isFreeVersion(app);
		SRTMPlugin srtmPlugin = PluginsHelper.getPlugin(SRTMPlugin.class);
		srtmNeedsInstallation = srtmPlugin == null || srtmPlugin.needsInstallation();
	}

	@Override
	public List<Fragment> getActiveTalkbackFragments() {
		List<Fragment> fragmentsWithoutTabs = new ArrayList<>();
		for (Fragment fragment : getSupportFragmentManager().getFragments()) {
			boolean isTabFragment = false;
			for (TabActivity.TabItem tabItem : mTabs) {
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
		View pagerContent = findViewById(R.id.pager);
		View slidingTabs = findViewById(R.id.sliding_tabs);
		int accessibility = getActiveTalkbackFragments().isEmpty() ? View.IMPORTANT_FOR_ACCESSIBILITY_YES : View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS;
		pagerContent.setImportantForAccessibility(accessibility);
		slidingTabs.setImportantForAccessibility(accessibility);
	}

	@NonNull
	public View getLayout() {
		return getWindow().getDecorView().findViewById(android.R.id.content);
	}

	@Nullable
	public <T> T getFragment(String fragmentTag) {
		Fragment fragment = getSupportFragmentManager().findFragmentByTag(fragmentTag);
		return fragment != null && !fragment.isDetached() && !fragment.isRemoving() ? (T) fragment : null;
	}
}
