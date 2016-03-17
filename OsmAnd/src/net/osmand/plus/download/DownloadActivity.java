package net.osmand.plus.download;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.StatFs;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.Space;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import net.osmand.IProgress;
import net.osmand.PlatformUtil;
import net.osmand.access.AccessibleToast;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.map.WorldRegion;
import net.osmand.map.WorldRegion.RegionParams;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.OsmandSettings.DrivingRegion;
import net.osmand.plus.OsmandSettings.MetricsConstants;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.activities.LocalIndexInfo;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.TabActivity;
import net.osmand.plus.base.BasicProgressAsyncTask;
import net.osmand.plus.base.BottomSheetDialogFragment;
import net.osmand.plus.download.DownloadIndexesThread.DownloadEvents;
import net.osmand.plus.download.ui.ActiveDownloadsDialogFragment;
import net.osmand.plus.download.ui.DataStoragePlaceDialogFragment;
import net.osmand.plus.download.ui.DownloadResourceGroupFragment;
import net.osmand.plus.download.ui.LocalIndexesFragment;
import net.osmand.plus.download.ui.UpdatesIndexFragment;
import net.osmand.plus.openseamapsplugin.NauticalMapsPlugin;
import net.osmand.plus.srtmplugin.SRTMPlugin;
import net.osmand.plus.views.controls.PagerSlidingTabStrip;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.io.File;
import java.lang.ref.WeakReference;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

public class DownloadActivity extends AbstractDownloadActivity implements DownloadEvents,
		ActivityCompat.OnRequestPermissionsResultCallback {
	private static final Log LOG = PlatformUtil.getLog(DownloadActivity.class);

	public static final int UPDATES_TAB_NUMBER = 2;
	public static final int LOCAL_TAB_NUMBER = 1;
	public static final int DOWNLOAD_TAB_NUMBER = 0;
	public static final int PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 0;


	private List<LocalIndexInfo> localIndexInfos = new ArrayList<>();

	List<TabActivity.TabItem> mTabs = new ArrayList<TabActivity.TabItem>();
	public static final String FILTER_KEY = "filter";
	public static final String FILTER_CAT = "filter_cat";
	public static final String FILTER_GROUP = "filter_group";

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
	private String filterGroup;
	protected Set<WeakReference<Fragment>> fragSet = new HashSet<>();
	private DownloadIndexesThread downloadThread;
	protected WorldRegion downloadItem;
	protected String downloadTargetFileName;

	private boolean srtmDisabled;
	private boolean srtmNeedsInstallation;
	private boolean nauticalPluginDisabled;
	private boolean freeVersion;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		getMyApplication().applyTheme(this);
		super.onCreate(savedInstanceState);
		downloadThread = getMyApplication().getDownloadThread();
		DownloadResources indexes = getDownloadThread().getIndexes();
		if (!indexes.isDownloadedFromInternet) {
			getDownloadThread().runReloadIndexFiles();
		}

		setContentView(R.layout.download);
		//noinspection ConstantConditions
		getSupportActionBar().setTitle(R.string.shared_string_map);
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
		visibleBanner = new BannerAndDownloadFreeVersion(findViewById(R.id.mainLayout), this, true);

		final Intent intent = getIntent();
		if (intent != null && intent.getExtras() != null) {
			filter = intent.getExtras().getString(FILTER_KEY);
			filterCat = intent.getExtras().getString(FILTER_CAT);
			filterGroup = intent.getExtras().getString(FILTER_GROUP);
		}
		showFirstTimeExternalStorage();
	}

	public DownloadIndexesThread getDownloadThread() {
		return downloadThread;
	}

	@Override
	public void onAttachFragment(Fragment fragment) {
		fragSet.add(new WeakReference<Fragment>(fragment));
	}

	@Override
	protected void onResume() {
		super.onResume();
		initAppStatusVariables();
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

	@Override
	public void onPause() {
		super.onPause();
		getMyApplication().getAppCustomization().pauseActivity(DownloadActivity.class);
		downloadThread.resetUiActivity(this);
	}

	@Override
	@UiThread
	public void downloadHasFinished() {
		visibleBanner.updateBannerInProgress();
		if (downloadItem != null && downloadItem != getMyApplication().getRegions().getWorldRegion()
				&& !WorldRegion.WORLD_BASEMAP.equals(downloadItem.getRegionDownloadNameLC())) {

			if (!Algorithms.isEmpty(downloadTargetFileName)) {
				File f = new File(downloadTargetFileName);
				if (f.exists() && f.lastModified() > System.currentTimeMillis() - 10000) {
					boolean firstMap = !getMyApplication().getSettings().FIRST_MAP_IS_DOWNLOADED.get();
					if (firstMap) {
						initSettingsFirstMap(downloadItem);
					}
					showGoToMap(downloadItem);
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

			shouldShowFreeVersionBanner =
					(Version.isFreeVersion(application) && !application.getSettings().LIVE_UPDATES_PURCHASED.get())
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
			ToggleCollapseFreeVersionBanner clickListener =
					new ToggleCollapseFreeVersionBanner(freeVersionDescriptionTextView,
							buttonsLinearLayout, freeVersionBannerTitle, application.getSettings());
			laterButton.setOnClickListener(clickListener);

			LinearLayout marksLinearLayout = (LinearLayout) freeVersionBanner.findViewById(R.id.marksLinearLayout);
			Space spaceView = new Space(ctx);
			LinearLayout.LayoutParams layoutParams =
					new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1);
			spaceView.setLayoutParams(layoutParams);
			marksLinearLayout.addView(spaceView);
			int markWidth = (int) (1 * ctx.getResources().getDisplayMetrics().density);
			int colorBlack = ctx.getResources().getColor(R.color.color_black);
			for (int i = 1; i < DownloadValidationManager.MAXIMUM_AVAILABLE_FREE_DOWNLOADS; i++) {
				View markView = new View(ctx);
				layoutParams = new LinearLayout.LayoutParams(markWidth, ViewGroup.LayoutParams.MATCH_PARENT);
				markView.setLayoutParams(layoutParams);
				markView.setBackgroundColor(colorBlack);
				marksLinearLayout.addView(markView);
				spaceView = new Space(ctx);
				layoutParams = new LinearLayout.LayoutParams(0,
						ViewGroup.LayoutParams.MATCH_PARENT, 1);
				spaceView.setLayoutParams(layoutParams);
				marksLinearLayout.addView(spaceView);
			}


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
		getMyApplication().getSettings().FIRST_MAP_IS_DOWNLOADED.set(true);
		DrivingRegion drg = null;
		RegionParams params = reg.getParams();
		boolean americanSigns = "american".equals(params.getRegionRoadSigns());
		boolean leftHand = "yes".equals(params.getRegionLeftHandDriving());
		MetricsConstants mc = "miles".equals(params.getRegionMetric()) ?
				MetricsConstants.MILES_AND_FOOTS : MetricsConstants.KILOMETERS_AND_METERS;
		for (DrivingRegion r : DrivingRegion.values()) {
			if (r.americanSigns == americanSigns && r.leftHandDriving == leftHand &&
					r.defMetrics == mc) {
				drg = r;
				break;
			}
		}
		if (drg != null) {
			getMyApplication().getSettings().DRIVING_REGION.set(drg);
		}
		String lang = params.getRegionLang();
		if (lang != null) {
			String lng = lang.split(",")[0];
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

	public void setDownloadItem(WorldRegion region, String targetFileName) {
		if (downloadItem == null) {
			downloadItem = region;
			downloadTargetFileName = targetFileName;
		} else if (region == null) {
			downloadItem = null;
			downloadTargetFileName = null;
		}
	}

	private void showGoToMap(WorldRegion region) {
		GoToMapFragment fragment = new GoToMapFragment();
		fragment.regionCenter = region.getRegionCenter();
		fragment.regionName = region.getLocaleName();
		fragment.show(getSupportFragmentManager(), GoToMapFragment.TAG);
	}

	private void showDownloadWorldMapIfNeeded() {
		if (getDownloadThread().getCurrentDownloadingItem() == null) {
			return;
		}
		IndexItem worldMap = getDownloadThread().getIndexes().getWorldBaseMapItem();
		if (!SUGGESTED_TO_DOWNLOAD_BASEMAP && worldMap != null && (!worldMap.isDownloaded() || worldMap.isOutdated()) &&
				!getDownloadThread().isDownloading(worldMap)) {
			SUGGESTED_TO_DOWNLOAD_BASEMAP = true;
			AskMapDownloadFragment fragment = new AskMapDownloadFragment();
			fragment.indexItem = worldMap;
			fragment.show(getSupportFragmentManager(), AskMapDownloadFragment.TAG);
		}
	}

	private void showFirstTimeExternalStorage() {
		final boolean firstTime = getMyApplication().getAppInitializer().isFirstTime();
		if (firstTime && DataStoragePlaceDialogFragment.isInterestedInFirstTime) {
			if (!hasPermissionToWriteExternalStorage(this)) {
				ActivityCompat.requestPermissions(this,
						new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
						PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);

			} else {
				chooseDataStorage();
			}
		}
	}

	private void chooseDataStorage() {
		DataStoragePlaceDialogFragment.showInstance(getSupportFragmentManager(), false);
	}

	public static boolean hasPermissionToWriteExternalStorage(Context ctx) {
		return ContextCompat.checkSelfPermission(ctx, Manifest.permission.WRITE_EXTERNAL_STORAGE)
				== PackageManager.PERMISSION_GRANTED;
	}

	@Override
	public void onRequestPermissionsResult(int requestCode,
										   String permissions[], int[] grantResults) {
		if (requestCode == PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE
				&& grantResults.length > 0
				&& grantResults[0] == PackageManager.PERMISSION_GRANTED) {
			new Timer().schedule(new TimerTask() {
				@Override
				public void run() {
					chooseDataStorage();
				}
			}, 1);
		} else {
			AccessibleToast.makeText(this,
					R.string.missing_write_external_storage_permission,
					Toast.LENGTH_LONG).show();
		}
		return;
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
		srtmDisabled = OsmandPlugin.getEnabledPlugin(SRTMPlugin.class) == null;
		nauticalPluginDisabled = OsmandPlugin.getEnabledPlugin(NauticalMapsPlugin.class) == null;
		freeVersion = Version.isFreeVersion(getMyApplication());
		OsmandPlugin srtmPlugin = OsmandPlugin.getPlugin(SRTMPlugin.class);
		srtmNeedsInstallation = srtmPlugin == null || srtmPlugin.needsInstallation();

	}

	public static class AskMapDownloadFragment extends BottomSheetDialogFragment {
		public static final String TAG = "AskMapDownloadFragment";

		private static final String KEY_ASK_MAP_DOWNLOAD_ITEM_FILENAME = "key_ask_map_download_item_filename";
		private IndexItem indexItem;

		@Nullable
		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			if (savedInstanceState != null) {
				String itemFileName = savedInstanceState.getString(KEY_ASK_MAP_DOWNLOAD_ITEM_FILENAME);
				if (itemFileName != null) {
					indexItem = getMyApplication().getDownloadThread().getIndexes().getIndexItem(itemFileName);
				}
			}
			View view = inflater.inflate(R.layout.ask_map_download_fragment, container, false);
			((ImageView) view.findViewById(R.id.titleIconImageView))
					.setImageDrawable(getIcon(R.drawable.ic_map, R.color.osmand_orange));

			Button actionButtonOk = (Button) view.findViewById(R.id.actionButtonOk);

			String titleText = null;
			String descriptionText = null;

			if (indexItem != null) {
				if (indexItem.getBasename().equalsIgnoreCase(WorldRegion.WORLD_BASEMAP)) {
					titleText = getString(R.string.index_item_world_basemap);
					descriptionText = getString(R.string.world_map_download_descr);
				}

				actionButtonOk.setText(getString(R.string.shared_string_download) + " (" + indexItem.getSizeDescription(getActivity()) + ")");
			}

			if (titleText != null) {
				((TextView) view.findViewById(R.id.titleTextView))
						.setText(titleText);
			}
			if (descriptionText != null) {
				((TextView) view.findViewById(R.id.descriptionTextView))
						.setText(descriptionText);
			}

			final ImageButton closeImageButton = (ImageButton) view.findViewById(R.id.closeImageButton);
			closeImageButton.setImageDrawable(getContentIcon(R.drawable.ic_action_remove_dark));
			closeImageButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					dismiss();
				}
			});

			actionButtonOk.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (indexItem != null) {
						((DownloadActivity) getActivity()).startDownload(indexItem);
						dismiss();
					}
				}
			});

			view.findViewById(R.id.actionButtonCancel)
					.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							dismiss();
						}
					});

			return view;
		}

		@Override
		public void onSaveInstanceState(@NonNull Bundle outState) {
			if (indexItem != null) {
				outState.putString(KEY_ASK_MAP_DOWNLOAD_ITEM_FILENAME, indexItem.getFileName());
			}
		}


	}

	public static class GoToMapFragment extends BottomSheetDialogFragment {
		public static final String TAG = "GoToMapFragment";

		private static final String KEY_GOTO_MAP_REGION_CENTER = "key_goto_map_region_center";
		private static final String KEY_GOTO_MAP_REGION_NAME = "key_goto_map_region_name";
		private LatLon regionCenter;
		private String regionName;

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			if (savedInstanceState != null) {
				regionName = savedInstanceState.getString(KEY_GOTO_MAP_REGION_NAME);
				regionName = regionName == null ? "" : regionName;
				Object rCenterObj = savedInstanceState.getSerializable(KEY_GOTO_MAP_REGION_CENTER);
				if (rCenterObj != null) {
					regionCenter = (LatLon) rCenterObj;
				} else {
					regionCenter = new LatLon(0, 0);
				}
			}

			View view = inflater.inflate(R.layout.go_to_map_fragment, container, false);
			((ImageView) view.findViewById(R.id.titleIconImageView))
					.setImageDrawable(getIcon(R.drawable.ic_map, R.color.osmand_orange));
			((TextView) view.findViewById(R.id.descriptionTextView))
					.setText(getActivity().getString(R.string.map_downloaded_descr, regionName));

			final ImageButton closeImageButton = (ImageButton) view.findViewById(R.id.closeImageButton);
			closeImageButton.setImageDrawable(getContentIcon(R.drawable.ic_action_remove_dark));
			closeImageButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (getActivity() instanceof DownloadActivity) {
						((DownloadActivity) getActivity()).setDownloadItem(null, null);
					}
					dismiss();
				}
			});

			view.findViewById(R.id.actionButton)
					.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							OsmandApplication app = (OsmandApplication) getActivity().getApplication();
							app.getSettings().setMapLocationToShow(
									regionCenter.getLatitude(),
									regionCenter.getLongitude(),
									5,
									new PointDescription(PointDescription.POINT_TYPE_WORLD_REGION_SHOW_ON_MAP, ""));

							dismiss();
							MapActivity.launchMapActivityMoveToTop(getActivity());
						}
					});

			return view;
		}

		@Override
		public void onSaveInstanceState(@NonNull Bundle outState) {
			outState.putString(KEY_GOTO_MAP_REGION_NAME, regionName);
			outState.putSerializable(KEY_GOTO_MAP_REGION_CENTER, regionCenter);
		}


	}

}
