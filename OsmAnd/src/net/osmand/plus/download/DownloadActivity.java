package net.osmand.plus.download;

import java.io.File;
import java.lang.ref.WeakReference;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import net.osmand.IProgress;
import net.osmand.PlatformUtil;
import net.osmand.access.AccessibleToast;
import net.osmand.map.WorldRegion;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.OsmandSettings.DrivingRegion;
import net.osmand.plus.OsmandSettings.MetricsConstants;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.activities.ActionBarProgressActivity;
import net.osmand.plus.activities.LocalIndexInfo;
import net.osmand.plus.activities.TabActivity;
import net.osmand.plus.base.BasicProgressAsyncTask;
import net.osmand.plus.download.DownloadIndexesThread.DownloadEvents;
import net.osmand.plus.download.DownloadResourceGroup.DownloadResourceGroupType;
import net.osmand.plus.download.ui.ActiveDownloadsDialogFragment;
import net.osmand.plus.download.ui.DataStoragePlaceDialogFragment;
import net.osmand.plus.download.ui.DownloadResourceGroupFragment;
import net.osmand.plus.download.ui.LocalIndexesFragment;
import net.osmand.plus.download.ui.UpdatesIndexFragment;
import net.osmand.plus.views.controls.PagerSlidingTabStrip;

import org.apache.commons.logging.Log;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.StatFs;
import android.support.annotation.UiThread;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.text.method.LinkMovementMethod;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class DownloadActivity extends ActionBarProgressActivity implements DownloadEvents {
	private static final Log LOG = PlatformUtil.getLog(DownloadActivity.class);

	public static final int UPDATES_TAB_NUMBER = 2;
	public static final int LOCAL_TAB_NUMBER = 1;
	public static final int DOWNLOAD_TAB_NUMBER = 0;


	private List<LocalIndexInfo> localIndexInfos = new ArrayList<>();

	List<TabActivity.TabItem> mTabs = new ArrayList<TabActivity.TabItem>();
	public static final String FILTER_KEY = "filter";
	public static final String FILTER_CAT = "filter_cat";

	public static final String TAB_TO_OPEN = "Tab_to_open";
	public static final String LOCAL_TAB = "local";
	public static final String DOWNLOAD_TAB = "download";
	public static final String UPDATES_TAB = "updates";
	public static final MessageFormat formatGb = new MessageFormat("{0, number,#.##} GB", Locale.US);
	public static final MessageFormat formatMb = new MessageFormat("{0, number,##.#} MB", Locale.US);
	private static boolean SUGGESTED_TO_DOWNLOAD_BASEMAP = false;

	private BannerAndDownloadFreeVersion visibleBanner;
	private ViewPager viewPager;
	private String filter;
	private String filterCat;
	protected Set<WeakReference<Fragment>> fragSet = new HashSet<>();
	private DownloadIndexesThread downloadThread;
	private DownloadValidationManager downloadValidationManager;
	protected WorldRegion downloadItem;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		getMyApplication().applyTheme(this);
		super.onCreate(savedInstanceState);
		downloadValidationManager = new DownloadValidationManager(getMyApplication());
		downloadThread = getMyApplication().getDownloadThread();
		DownloadResources indexes = getDownloadThread().getIndexes();
		if (!indexes.isDownloadedFromInternet) {
			getDownloadThread().runReloadIndexFiles();
		}

		setContentView(R.layout.download);
		final View downloadProgressLayout = findViewById(R.id.downloadProgressLayout);
		downloadProgressLayout.setVisibility(View.VISIBLE);
		updateDescriptionTextWithSize(this, downloadProgressLayout);
		int currentTab = DOWNLOAD_TAB_NUMBER;
		String tab = getIntent() == null || getIntent().getExtras() == null ? null : getIntent().getExtras().getString(TAB_TO_OPEN);
		if (tab != null) {
			if (tab.equals(DOWNLOAD_TAB)) {
				currentTab = DOWNLOAD_TAB_NUMBER;
			} else if (tab.equals(LOCAL_TAB)) {
				currentTab = LOCAL_TAB_NUMBER;
			} else if (tab.equals(UPDATES_TAB)) {
				currentTab = UPDATES_TAB_NUMBER;
			}
		}

		viewPager = (ViewPager) findViewById(R.id.pager);
		PagerSlidingTabStrip mSlidingTabLayout = (PagerSlidingTabStrip) findViewById(R.id.sliding_tabs);


		mTabs.add(new TabActivity.TabItem(R.string.download_tab_downloads,
				getString(R.string.download_tab_downloads), DownloadResourceGroupFragment.class));
		mTabs.add(new TabActivity.TabItem(R.string.download_tab_local,
				getString(R.string.download_tab_local), LocalIndexesFragment.class));
		mTabs.add(new TabActivity.TabItem(R.string.download_tab_updates,
				getString(R.string.download_tab_updates), UpdatesIndexFragment.class));

		viewPager.setAdapter(new TabActivity.OsmandFragmentPagerAdapter(getSupportFragmentManager(), mTabs));
		mSlidingTabLayout.setViewPager(viewPager);

		viewPager.setCurrentItem(currentTab);
		viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
			@Override
			public void onPageScrolled(int i, float v, int i1) {

			}

			@Override
			public void onPageSelected(int i) {
				visibleBanner.updateBannerInProgress();
			}

			@Override
			public void onPageScrollStateChanged(int i) {

			}
		});
		visibleBanner = new BannerAndDownloadFreeVersion(findViewById(R.id.mainLayout), this, true);

		final Intent intent = getIntent();
		if (intent != null && intent.getExtras() != null) {
			filter = intent.getExtras().getString(FILTER_KEY);
			filterCat = intent.getExtras().getString(FILTER_CAT);
		}
		showFirstTimeExternalStorage();
	}

	public DownloadIndexesThread getDownloadThread() {
		return downloadThread;
	}
	
	public void startDownload(IndexItem... indexItem) {
		downloadValidationManager.startDownload(this, indexItem);
	}
	
	public void makeSureUserCancelDownload(IndexItem item) {
		downloadValidationManager.makeSureUserCancelDownload(this, item);
	}
	
	@Override
	public void onAttachFragment(Fragment fragment) {
		fragSet.add(new WeakReference<Fragment>(fragment));
	}

	@Override
	protected void onResume() {
		super.onResume();
		getMyApplication().getAppCustomization().resumeActivity(DownloadActivity.class, this);
		downloadThread.setUiActivity(this);
		downloadInProgress();
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

	public void setLocalIndexInfos(List<LocalIndexInfo> list) {
		this.localIndexInfos = list;
	}

	public List<LocalIndexInfo> getLocalIndexInfos() {
		return localIndexInfos;
	}
	
	public OsmandApplication getMyApplication() {
		return (OsmandApplication) getApplication();
	}

	@Override
	public void onPause() {
		super.onPause();
		getMyApplication().getAppCustomization().pauseActivity(DownloadActivity.class);
		downloadThread.setUiActivity(null);
	}

	@Override
	@UiThread
	public void downloadHasFinished() {
		visibleBanner.updateBannerInProgress();
		if(downloadItem != null && !WorldRegion.WORLD_BASEMAP.equals(downloadItem.getRegionDownloadNameLC())) {
			boolean firstMap = !getMyApplication().getSettings().FIRST_MAP_IS_DOWNLOADED.get();
			if(firstMap) {
				initSettingsFirstMap(downloadItem);
			}
			showGoToMap(downloadItem);
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
		visibleBanner.updateBannerInProgress();
		showDownloadWorldMapIfNeeded();
		for (WeakReference<Fragment> ref : fragSet) {
			Fragment f = ref.get();
			if (f instanceof DownloadEvents && f.isAdded()) {
				((DownloadEvents) f).downloadInProgress();
			}
		}
	}

	

	@Override
	@UiThread
	public void newDownloadIndexes() {
		visibleBanner.updateBannerInProgress();
		for (WeakReference<Fragment> ref : fragSet) {
			Fragment f = ref.get();
			if (f instanceof DownloadEvents && f.isAdded()) {
				((DownloadEvents) f).newDownloadIndexes();
			}
		}
		downloadHasFinished();
	}

	private int getCurrentTab() {
		return viewPager.getCurrentItem();
	}

	public boolean isLightActionBar() {
		return ((OsmandApplication) getApplication()).getSettings().isLightActionBar();
	}

	public void showDialog(FragmentActivity activity, DialogFragment fragment) {
		fragment.show(activity.getSupportFragmentManager(), "dialog");
	}

	private static class ToggleCollapseFreeVersionBanner implements View.OnClickListener {
		private final View freeVersionDescriptionTextView;
		private final View buttonsLinearLayout;
		private final View freeVersionTitle;
		private final OsmandSettings settings;

		private ToggleCollapseFreeVersionBanner(View freeVersionDescriptionTextView,
												View buttonsLinearLayout, View freeVersionTitle,
												OsmandSettings settings) {
			this.freeVersionDescriptionTextView = freeVersionDescriptionTextView;
			this.buttonsLinearLayout = buttonsLinearLayout;
			this.freeVersionTitle = freeVersionTitle;
			this.settings = settings;
		}

		@Override
		public void onClick(View v) {

			if (freeVersionDescriptionTextView.getVisibility() == View.VISIBLE
					&& isDownlodingPermitted(settings)) {
				freeVersionDescriptionTextView.setVisibility(View.GONE);
				buttonsLinearLayout.setVisibility(View.GONE);
			} else {
				freeVersionDescriptionTextView.setVisibility(View.VISIBLE);
				buttonsLinearLayout.setVisibility(View.VISIBLE);
				freeVersionTitle.setVisibility(View.VISIBLE);
			}
		}
	}

	public static boolean isDownlodingPermitted(OsmandSettings settings) {
		final Integer mapsDownloaded = settings.NUMBER_OF_FREE_DOWNLOADS.get();
		int downloadsLeft = DownloadValidationManager.MAXIMUM_AVAILABLE_FREE_DOWNLOADS - mapsDownloaded;
		return Math.max(downloadsLeft, 0) > 0;
	}

	public static class BannerAndDownloadFreeVersion {
		private final View freeVersionBanner;
		private final View downloadProgressLayout;
		private final ProgressBar progressBar;
		private final TextView leftTextView;
		private final TextView rightTextView;
		private final ProgressBar downloadsLeftProgressBar;
		private final View buttonsLinearLayout;
		private final TextView freeVersionDescriptionTextView;
		private final TextView downloadsLeftTextView;
		private final View laterButton;

		private final DownloadActivity ctx;
		private final OsmandApplication application;
		private final boolean shouldShowFreeVersionBanner;
		private final View freeVersionBannerTitle;
		private boolean showSpace;

		public BannerAndDownloadFreeVersion(View view, final DownloadActivity ctx, boolean showSpace) {
			this.ctx = ctx;
			this.showSpace = showSpace;
			application = (OsmandApplication) ctx.getApplicationContext();
			freeVersionBanner = view.findViewById(R.id.freeVersionBanner);
			downloadProgressLayout = view.findViewById(R.id.downloadProgressLayout);
			progressBar = (ProgressBar) view.findViewById(R.id.progressBar);
			leftTextView = (TextView) view.findViewById(R.id.leftTextView);
			rightTextView = (TextView) view.findViewById(R.id.rightTextView);
			downloadsLeftTextView = (TextView) freeVersionBanner.findViewById(R.id.downloadsLeftTextView);
			downloadsLeftProgressBar = (ProgressBar) freeVersionBanner.findViewById(R.id.downloadsLeftProgressBar);
			buttonsLinearLayout = freeVersionBanner.findViewById(R.id.buttonsLinearLayout);
			freeVersionDescriptionTextView = (TextView) freeVersionBanner
					.findViewById(R.id.freeVersionDescriptionTextView);
			laterButton = freeVersionBanner.findViewById(R.id.laterButton);
			freeVersionBannerTitle = freeVersionBanner.findViewById(R.id.freeVersionBannerTitle);

			shouldShowFreeVersionBanner = Version.isFreeVersion(application)
					|| application.getSettings().SHOULD_SHOW_FREE_VERSION_BANNER.get();

			initFreeVersionBanner();
			updateBannerInProgress();
		}
		
		public void updateBannerInProgress() {
			BasicProgressAsyncTask<?, ?, ?, ?> basicProgressAsyncTask = ctx.getDownloadThread().getCurrentRunningTask();
			final boolean isFinished = basicProgressAsyncTask == null
					|| basicProgressAsyncTask.getStatus() == AsyncTask.Status.FINISHED;
			if (isFinished) {
				downloadProgressLayout.setOnClickListener(null);
				updateDescriptionTextWithSize(ctx, downloadProgressLayout);
				if (ctx.getCurrentTab() == UPDATES_TAB_NUMBER || !showSpace) {
					downloadProgressLayout.setVisibility(View.GONE);
				} else {
					downloadProgressLayout.setVisibility(View.VISIBLE);
				}
				updateFreeVersionBanner();
			} else {
				boolean indeterminate = basicProgressAsyncTask.isIndeterminate();
				String message = basicProgressAsyncTask.getDescription();
				int percent = basicProgressAsyncTask.getProgressPercentage();
				setMinimizedFreeVersionBanner(true);

				updateAvailableDownloads();
				downloadProgressLayout.setVisibility(View.VISIBLE);
				downloadProgressLayout.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						new ActiveDownloadsDialogFragment().show(ctx.getSupportFragmentManager(), "dialog");
					}
				});
				progressBar.setIndeterminate(indeterminate);
				if (indeterminate) {
					leftTextView.setText(message);
					rightTextView.setText(null);
				} else {
					progressBar.setProgress(percent);
//					final String format = ctx.getString(R.string.downloading_number_of_files);
					leftTextView.setText(message);
					rightTextView.setText(percent + "%");
				}
			}
		}

		public void hideDownloadProgressLayout() {
			downloadProgressLayout.setVisibility(View.GONE);
		}

		public void showDownloadProgressLayout() {
			downloadProgressLayout.setVisibility(View.VISIBLE);
		}

		private void initFreeVersionBanner() {
			if (!shouldShowFreeVersionBanner) {
				freeVersionBanner.setVisibility(View.GONE);
				return;
			}
			freeVersionBanner.setVisibility(View.VISIBLE);
			downloadsLeftProgressBar.setMax(DownloadValidationManager.MAXIMUM_AVAILABLE_FREE_DOWNLOADS);
			freeVersionDescriptionTextView.setText(ctx.getString(R.string.free_version_message,
					DownloadValidationManager.MAXIMUM_AVAILABLE_FREE_DOWNLOADS));
			freeVersionBanner.findViewById(R.id.getFullVersionButton).setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(Version.marketPrefix(
							ctx.getMyApplication()) + "net.osmand.plus"));
					try {
						ctx.startActivity(intent);
					} catch (ActivityNotFoundException e) {
						LOG.error("ActivityNotFoundException", e);
					}
				}
			});
			laterButton.setOnClickListener(new ToggleCollapseFreeVersionBanner(freeVersionDescriptionTextView,
					buttonsLinearLayout, freeVersionBannerTitle, application.getSettings()));
			updateFreeVersionBanner();
		}

		private void updateFreeVersionBanner() {
			if (!shouldShowFreeVersionBanner) {
				return;
			}
			setMinimizedFreeVersionBanner(false);
			OsmandSettings settings = application.getSettings();
			final Integer mapsDownloaded = settings.NUMBER_OF_FREE_DOWNLOADS.get();
			downloadsLeftProgressBar.setProgress(mapsDownloaded);
			int downloadsLeft = DownloadValidationManager.MAXIMUM_AVAILABLE_FREE_DOWNLOADS - mapsDownloaded;
			downloadsLeft = Math.max(downloadsLeft, 0);
			if (downloadsLeft <= 0) {
				laterButton.setVisibility(View.GONE);
			}
			downloadsLeftTextView.setText(ctx.getString(R.string.downloads_left_template, downloadsLeft));
			freeVersionBanner.setOnClickListener(new ToggleCollapseFreeVersionBanner(freeVersionDescriptionTextView,
					buttonsLinearLayout, freeVersionBannerTitle, settings));
		}

		private void updateAvailableDownloads() {
			int activeTasks = ctx.getDownloadThread().getCountedDownloads();
			OsmandSettings settings = application.getSettings();
			final Integer mapsDownloaded = settings.NUMBER_OF_FREE_DOWNLOADS.get() + activeTasks;
			downloadsLeftProgressBar.setProgress(mapsDownloaded);
		}

		private void setMinimizedFreeVersionBanner(boolean minimize) {
			if (minimize && isDownlodingPermitted(application.getSettings())) {
				freeVersionDescriptionTextView.setVisibility(View.GONE);
				buttonsLinearLayout.setVisibility(View.GONE);
				freeVersionBannerTitle.setVisibility(View.GONE);
			} else {
				freeVersionDescriptionTextView.setVisibility(View.VISIBLE);
				buttonsLinearLayout.setVisibility(View.VISIBLE);
				freeVersionBannerTitle.setVisibility(View.VISIBLE);
			}
		}
	}

	public void reloadLocalIndexes() {
		AsyncTask<Void, String, List<String>> task = new AsyncTask<Void, String, List<String>>() {
			@Override
			protected void onPreExecute() {
				super.onPreExecute();
				setSupportProgressBarIndeterminateVisibility(true);
			}

			@Override
			protected List<String> doInBackground(Void... params) {
				return getMyApplication().getResourceManager().reloadIndexes(IProgress.EMPTY_PROGRESS,
						new ArrayList<String>()
				);
			}

			@Override
			protected void onPostExecute(List<String> warnings) {
				setSupportProgressBarIndeterminateVisibility(false);
				if (!warnings.isEmpty()) {
					final StringBuilder b = new StringBuilder();
					boolean f = true;
					for (String w : warnings) {
						if (f) {
							f = false;
						} else {
							b.append('\n');
						}
						b.append(w);
					}
					AccessibleToast.makeText(DownloadActivity.this, b.toString(), Toast.LENGTH_LONG).show();
				}
				newDownloadIndexes();
			}
		};
		task.execute();
	}
	
	private void initSettingsFirstMap(WorldRegion reg) {
		// TODO test set correctly (4 tests): when you download first Australia, Japan, Luxembourgh, US  
		getMyApplication().getSettings().FIRST_MAP_IS_DOWNLOADED.set(true);
		DrivingRegion drg = null;
		boolean americanSigns = "american".equals(reg.getRegionRoadSigns());
		boolean leftHand = "yes".equals(reg.getRegionLeftHandDriving());
		MetricsConstants mc  = "miles".equals(reg.getRegionMetric()) ?
				MetricsConstants.MILES_AND_FOOTS : MetricsConstants.KILOMETERS_AND_METERS;
		for (DrivingRegion r : DrivingRegion.values()) {
			if(r.americanSigns == americanSigns && r.leftHandDriving == leftHand && 
					r.defMetrics == mc) {
				drg = r;
				break;
			}
		}
		if (drg != null) {
			getMyApplication().getSettings().DRIVING_REGION.set(drg);
		}
		String lng = reg.getRegionLang();
		if (lng != null) {
			String setTts = null;
			for (String s : OsmandSettings.TTS_AVAILABLE_VOICES) {
				if (lng.startsWith(s)) {
					setTts = s + "-tts";
					break;
				} else if (lng.contains("," + s)) {
					setTts = s + "-tts";
				}
			}
			if (setTts != null) {
				getMyApplication().getSettings().VOICE_PROVIDER.set(setTts);
			}
		}
	}
	
	public void setDownloadItem(WorldRegion region) {
		downloadItem = region;
	}
	
	private void showGoToMap(WorldRegion worldRegion) {
		// TODO Show dialog go to map (coordinates to open take from WorldRegion.getCenter)
		
	}
	
	private void showDownloadWorldMapIfNeeded() {
		if(getDownloadThread().getCurrentDownloadingItem() == null) {
			return;
		}
		DownloadResourceGroup worldMaps = getDownloadThread().getIndexes().
				getSubGroupById(DownloadResourceGroupType.WORLD_MAPS.getDefaultId());
		IndexItem worldMap = null;
		List<IndexItem> list = worldMaps.getIndividualResources();
		if(list != null) {
			for(IndexItem ii  : list) {
				if(ii.getBasename().equalsIgnoreCase(WorldRegion.WORLD_BASEMAP)) {
					worldMap = ii;
					break;
				}
			}
		}
		
		if(!SUGGESTED_TO_DOWNLOAD_BASEMAP && worldMap != null && (!worldMap.isDownloaded() || worldMap.isOutdated()) && 
				!getDownloadThread().isDownloading(worldMap)) {
			SUGGESTED_TO_DOWNLOAD_BASEMAP = true;
			// TODO Show dialog Download world map with 2 buttons to download it or no			
		}
		
	}
	
	private void showFirstTimeExternalStorage() {
		// TODO finish + test & hide dialog if the download has started
		final boolean firstTime = getMyApplication().getAppInitializer().isFirstTime(this);
		final boolean externalExists =
				DataStoragePlaceDialogFragment.getExternalStorageDirectory() != null;
		if (firstTime && externalExists) {
			new DataStoragePlaceDialogFragment().show(getFragmentManager(), null);
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

	@SuppressWarnings("deprecation")
	public static void updateDescriptionTextWithSize(DownloadActivity activity, View view) {
		TextView descriptionText = (TextView) view.findViewById(R.id.rightTextView);
		TextView messageTextView = (TextView) view.findViewById(R.id.leftTextView);
		ProgressBar sizeProgress = (ProgressBar) view.findViewById(R.id.progressBar);

		File dir = activity.getMyApplication().getAppPath("").getParentFile();
		String size = formatGb.format(new Object[]{0});
		int percent = 0;
		if (dir.canRead()) {
			StatFs fs = new StatFs(dir.getAbsolutePath());
			size = formatGb.format(new Object[]{(float) (fs.getAvailableBlocks()) * fs.getBlockSize() / (1 << 30)});
			percent = 100 - fs.getAvailableBlocks() * 100 / fs.getBlockCount();
		}
		sizeProgress.setIndeterminate(false);
		sizeProgress.setProgress(percent);
		String text = activity.getString(R.string.free, size);
		descriptionText.setText(text);
		descriptionText.setMovementMethod(LinkMovementMethod.getInstance());

		messageTextView.setText(R.string.device_memory);
	}
}
