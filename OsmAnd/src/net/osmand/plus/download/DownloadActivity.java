package net.osmand.plus.download;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.ListFragment;
import android.support.v4.view.ViewPager;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.BaseExpandableListAdapter;
import android.widget.Button;
import android.widget.ExpandableListAdapter;
import android.widget.ListAdapter;
import android.widget.ProgressBar;
import android.widget.TextView;

import net.osmand.IndexConstants;
import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.activities.LocalIndexInfo;
import net.osmand.plus.activities.OsmAndListFragment;
import net.osmand.plus.activities.OsmandBaseExpandableListAdapter;
import net.osmand.plus.activities.OsmandExpandableListFragment;
import net.osmand.plus.activities.TabActivity;
import net.osmand.plus.base.BasicProgressAsyncTask;
import net.osmand.plus.download.items.RegionDialogFragment;
import net.osmand.plus.download.items.WorldItemsFragment;
import net.osmand.plus.srtmplugin.SRTMPlugin;
import net.osmand.plus.views.controls.PagerSlidingTabStrip;

import org.apache.commons.logging.Log;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class DownloadActivity extends BaseDownloadActivity implements RegionDialogFragment.DialogDismissListener {
	private List<LocalIndexInfo> localIndexInfos = new ArrayList<LocalIndexInfo>();

	private String initialFilter = "";
	private boolean singleTab;

	List<TabActivity.TabItem> mTabs = new ArrayList<TabActivity.TabItem>();

	public static final String FILTER_KEY = "filter";
	public static final String FILTER_CAT = "filter_cat";

	public static final String TAB_TO_OPEN = "Tab_to_open";
	public static final String LOCAL_TAB = "local";
	public static final String DOWNLOAD_TAB = "download";
	public static final String UPDATES_TAB = "updates";
	public static final String SINGLE_TAB = "SINGLE_TAB";
	private List<DownloadActivityType> downloadTypes = new ArrayList<DownloadActivityType>();
	private BannerAndDownloadFreeVersion visibleBanner;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		getMyApplication().applyTheme(this);
		super.onCreate(savedInstanceState);
		updateDownloads();

		setContentView(R.layout.download);
		singleTab = getIntent() != null && getIntent().getBooleanExtra(SINGLE_TAB, false);
		int currentTab = 0;
		String tab = getIntent() == null || getIntent().getExtras() == null ? null : getIntent().getExtras().getString(TAB_TO_OPEN);
		if (tab != null) {
			if (tab.equals(DOWNLOAD_TAB)) {
				currentTab = 1;
			} else if (tab.equals(UPDATES_TAB)) {
				currentTab = 2;
			}
		}
//		if (singleTab) {
//			ViewPager viewPager = (ViewPager) findViewById(R.id.pager);
//			viewPager.setVisibility(View.GONE);
//			Fragment f = currentTab == 0 ? new LocalIndexesFragment() :
//				(currentTab == 1? new DownloadIndexFragment() : new UpdatesIndexFragment());
//			String tag = currentTab == 0 ? LOCAL_TAB :
//					(currentTab == 1 ? DOWNLOAD_TAB : UPDATES_TAB);
//			findViewById(R.id.layout).setVisibility(View.VISIBLE);
//			android.support.v4.app.FragmentManager manager = getSupportFragmentManager();
//			if (manager.findFragmentByTag(tag) == null){
//				getSupportFragmentManager().beginTransaction().add(R.id.layout, f, tag).commit();
//			}
//		} else {
		ViewPager viewPager = (ViewPager) findViewById(R.id.pager);
		PagerSlidingTabStrip mSlidingTabLayout = (PagerSlidingTabStrip) findViewById(R.id.sliding_tabs);

		mTabs.add(new TabActivity.TabItem(R.string.download_tab_local,
				getString(R.string.download_tab_local), LocalIndexesFragment.class));
		mTabs.add(new TabActivity.TabItem(R.string.download_tab_downloads,
				getString(R.string.download_tab_downloads), DownloadIndexFragment.class));
		mTabs.add(new TabActivity.TabItem(R.string.download_tab_updates,
				getString(R.string.download_tab_updates), UpdatesIndexFragment.class));

//		mTabs.add(new TabActivity.TabItem(R.string.download_tab_local,
//				getString(R.string.download_tab_local), NewLocalIndexesFragment.class));
		mTabs.add(new TabActivity.TabItem(R.string.download_tab_downloads,
				getString(R.string.download_tab_downloads), WorldItemsFragment.class));

		viewPager.setAdapter(new TabActivity.OsmandFragmentPagerAdapter(getSupportFragmentManager(), mTabs));
		mSlidingTabLayout.setViewPager(viewPager);

		viewPager.setCurrentItem(currentTab);
//		}

		settings = ((OsmandApplication) getApplication()).getSettings();

		findViewById(R.id.downloadButton).setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				downloadFilesCheckFreeVersion();
			}

		});
		findViewById(R.id.wikiButton).setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				downloadWikiFiles();
			}
		});

		findViewById(R.id.CancelAll).setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				getEntriesToDownload().clear();
				updateDownloadButton();
				for (WeakReference<Fragment> ref : fragSet) {
					Fragment f = ref.get();
					if (f != null) {
						if (f instanceof OsmAndListFragment) {
							if (f.isAdded() && ((OsmAndListFragment) f).getListAdapter() instanceof ArrayAdapter) {
								((ArrayAdapter) ((OsmAndListFragment) f).getListAdapter()).notifyDataSetChanged();
							}
						} else if (f.isAdded() && f instanceof OsmandExpandableListFragment &&
								((OsmandExpandableListFragment) f).getAdapter() instanceof BaseExpandableListAdapter) {
							((BaseExpandableListAdapter) ((OsmandExpandableListFragment) f).getAdapter()).notifyDataSetChanged();
						}
					}
				}
			}
		});

		downloadTypes = createDownloadTypes();
		final Intent intent = getIntent();
		if (intent != null && intent.getExtras() != null) {
			final String filter = intent.getExtras().getString(FILTER_KEY);
			if (filter != null) {
				initialFilter = filter;
			}

			final String filterCat = intent.getExtras().getString(FILTER_CAT);
			if (filterCat != null) {
				DownloadActivityType type = DownloadActivityType.getIndexType(filterCat.toLowerCase());
				if (type != null) {
					downloadTypes.remove(type);
					downloadTypes.add(0, type);
				}
			}
		}
		changeType(downloadTypes.get(0));
	}


	public Map<String, String> getIndexActivatedFileNames() {
		return downloadListIndexThread != null ? downloadListIndexThread.getIndexActivatedFileNames() : null;
	}

	public String getInitialFilter() {
		return initialFilter;
	}

	@Override
	protected void onResume() {
		super.onResume();
		getMyApplication().getAppCustomization().resumeActivity(DownloadActivity.class, this);
		initFreeVersionBanner(findViewById(R.id.mainLayout));
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

	public void changeType(final DownloadActivityType tp) {
		//invalidateOptionsMenu();
		if (downloadListIndexThread != null && type != tp) {
			type = tp;
			downloadListIndexThread.runCategorization(tp);
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		getMyApplication().getAppCustomization().pauseActivity(DownloadActivity.class);
	}

	@Override
	public void updateProgress(boolean updateOnlyProgress) {
		BasicProgressAsyncTask<?, ?, ?> basicProgressAsyncTask =
				DownloadActivity.downloadListIndexThread.getCurrentRunningTask();
		if (visibleBanner != null) {
			final int countedDownloads = DownloadActivity.downloadListIndexThread.getCountedDownloads();
			visibleBanner.updateProgress(countedDownloads, basicProgressAsyncTask);
		}
		if (!updateOnlyProgress) {
			updateDownloadButton();
		}
	}

	@Override
	public void updateDownloadList(List<IndexItem> list) {
		for (WeakReference<Fragment> ref : fragSet) {
			Fragment f = ref.get();
			if (f instanceof UpdatesIndexFragment) {
				if (f.isAdded()) {
					((UpdatesIndexFragment) f).updateItemsList(list);
				}
			}
		}
	}

	@Override
	public void categorizationFinished(List<IndexItem> filtered, List<IndexItemCategory> cats) {
		for (WeakReference<Fragment> ref : fragSet) {
			Fragment f = ref.get();
			if (f instanceof DownloadIndexFragment) {
				if (f.isAdded()) {
					((DownloadIndexFragment) f).categorizationFinished(filtered, cats);
				}
			}
		}
	}

	@Override
	public void onCategorizationFinished() {
		for (WeakReference<Fragment> ref : fragSet) {
			Fragment f = ref.get();
			if (f instanceof WorldItemsFragment) {
				if (f.isAdded()) {
					((WorldItemsFragment) f).onCategorizationFinished();
				}
			}
		}
	}

	public void downloadListUpdated() {
		for (WeakReference<Fragment> ref : fragSet) {
			Fragment f = ref.get();
			if (f instanceof DownloadIndexFragment) {
				if (f.isAdded()) {
					((DownloadIndexAdapter) ((DownloadIndexFragment) f).getExpandableListAdapter())
							.notifyDataSetInvalidated();
				}
			}
		}
	}

	@Override
	public boolean startDownload(IndexItem item) {
		final boolean b = super.startDownload(item);
		visibleBanner.initFreeVersionBanner();
		return b;
	}

	@Override
	public void downloadedIndexes() {
		for (WeakReference<Fragment> ref : fragSet) {
			Fragment f = ref.get();
			if (f instanceof LocalIndexesFragment) {
				if (f.isAdded()) {
					((LocalIndexesFragment) f).reloadData();
				}
			} else if (f instanceof DownloadIndexFragment) {
				if (f.isAdded()) {
					DownloadIndexAdapter adapter = ((DownloadIndexAdapter)
							((DownloadIndexFragment) f).getExpandableListAdapter());
					if (adapter != null) {
						adapter.setLoadedFiles(getIndexActivatedFileNames(), getIndexFileNames());

					}
				}
			}
		}

	}

	protected void downloadWikiFiles() {
		if (Version.isFreeVersion(getMyApplication())) {
			new InstallPaidVersionDialogFragment().show(getSupportFragmentManager(),
					InstallPaidVersionDialogFragment.TAG);
		} else {
			Builder bld = new AlertDialog.Builder(this);
			final List<IndexItem> wi = getWikipediaItems();
			long size = 0;
			for (IndexItem i : wi) {
				size += i.getSize();
			}
			bld.setMessage(getString(R.string.download_wikipedia_files, (size >> 20)));
			bld.setPositiveButton(R.string.shared_string_ok, new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					for (IndexItem i : wi) {
						addToDownload(i);
					}
					updateDownloadButton();
					checkOldWikiFiles();
				}
			});
			bld.setNegativeButton(R.string.shared_string_cancel, null);
			if (wi.size() > 0) {
				bld.show();
			}
		}
	}

	protected void checkOldWikiFiles() {
		Map<String, String> fileNames = getMyApplication().getResourceManager().getIndexFileNames();
		final Set<String> wiki = new HashSet<String>();
		for (String s : fileNames.keySet()) {
			if (s.contains("_wiki")) {
				wiki.add(s);
			}
		}
		if (wiki.size() > 0) {
			Builder bld = new AlertDialog.Builder(this);
			bld.setMessage(R.string.archive_wikipedia_data);
			bld.setNegativeButton(R.string.shared_string_cancel, null);
			bld.setPositiveButton(R.string.shared_string_yes, new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					for (String w : wiki) {
						File fl = getMyApplication().getAppPath(w);
						File nf = new File(fl.getParentFile(), IndexConstants.BACKUP_INDEX_DIR + "/" + fl.getName());
						boolean res = fl.renameTo(nf);
						if (!res) {
							System.err.println("Renaming from " + fl.getAbsolutePath() + " to " + nf.getAbsolutePath() + " failed");
						}
					}
				}
			});
			bld.show();
		}
	}


	@Override
	public void updateDownloadButton() {
//		View view = getView();
//		if (view == null || getExpandableListView() == null){
//			return;
//		}
//		int x = getExpandableListView().getScrollX();
//		int y = getExpandableListView().getScrollY();
		if (getEntriesToDownload().isEmpty()) {
			findViewById(R.id.DownloadLayout).setVisibility(View.GONE);
		} else {
			BasicProgressAsyncTask<?, ?, ?> task = DownloadActivity.downloadListIndexThread.getCurrentRunningTask();
			boolean running = task instanceof DownloadIndexesThread.DownloadIndexesAsyncTask;
			((Button) findViewById(R.id.downloadButton)).setEnabled(!running);
			String text;
			int downloads = DownloadActivity.downloadListIndexThread.getDownloads();
			if (!running) {
				text = getString(R.string.shared_string_download) + "  (" + downloads + ")"; //$NON-NLS-1$
			} else {
				text = getString(R.string.shared_string_downloading) + "  (" + downloads + ")"; //$NON-NLS-1$
			}
			findViewById(R.id.DownloadLayout).setVisibility(View.VISIBLE);
			if (Version.isFreeVersion(getMyApplication())) {
				int left = DownloadActivity.MAXIMUM_AVAILABLE_FREE_DOWNLOADS - settings.NUMBER_OF_FREE_DOWNLOADS.get() - downloads;
				boolean excessLimit = left < 0;
				if (left < 0)
					left = 0;
				if (getDownloadType() == DownloadActivityType.NORMAL_FILE || getDownloadType() == DownloadActivityType.ROADS_FILE) {
					text += " (" + (excessLimit ? "! " : "") + getString(R.string.files_limit, left).toLowerCase() + ")";
				}
			}
			((Button) findViewById(R.id.downloadButton)).setText(text);
			List<IndexItem> wikipediaItems = getWikipediaItems();
			findViewById(R.id.wikiButton).setVisibility(wikipediaItems.size() == 0 ? View.GONE : View.VISIBLE);
		}

		for (WeakReference<Fragment> ref : fragSet) {
			Fragment f = ref.get();
			if (f != null)
				if (f.isAdded()) {
					if (f instanceof OsmandExpandableListFragment) {
						ExpandableListAdapter ad = ((OsmandExpandableListFragment) f).getExpandableListView()
								.getExpandableListAdapter();
						if (ad instanceof OsmandBaseExpandableListAdapter) {
							((OsmandBaseExpandableListAdapter) ad).notifyDataSetChanged();
						}
					} else if (f instanceof ListFragment) {
						ListAdapter la = ((ListFragment) f).getListAdapter();
						if (la instanceof BaseAdapter) {
							((BaseAdapter) la).notifyDataSetChanged();
						}
					}
				}
		}
//		if (scroll) {
//			getExpandableListView().scrollTo(x, y);
//		}
	}


	private List<IndexItem> getWikipediaItems() {
		Set<String> wikipediaItems = new HashSet<String>();
		Map<String, String> indexed = getMyApplication().getResourceManager().getIndexFileNames();
		for (IndexItem i : getEntriesToDownload().keySet()) {
			if (i.getType() == DownloadActivityType.NORMAL_FILE) {
				boolean fit = true;
				fit = fit && i.getFileName().contains("obf");
				fit = fit && !i.getFileName().contains("world");
				String fname = i.getBasename();
				if (fit && !indexed.containsKey(fname + ".wiki.obf")) {
					wikipediaItems.add(fname);
				}
			}
		}
		for (IndexItem i : getEntriesToDownload().keySet()) {
			if (i.getType() == DownloadActivityType.WIKIPEDIA_FILE) {
				wikipediaItems.remove(i.getBasename());
			}
		}
		List<IndexItem> res = new ArrayList<IndexItem>();
		IndexFileList list = downloadListIndexThread.getIndexFiles();
		if (list != null) {
			List<IndexItem> indexFiles = list.getIndexFiles();
			for (IndexItem i : indexFiles) {
				if (i.getType() == DownloadActivityType.WIKIPEDIA_FILE &&
						wikipediaItems.contains(i.getBasename())) {
					res.add(i);
				}
			}
		}
		return res;
	}


	public List<DownloadActivityType> getDownloadTypes() {
		return downloadTypes;
	}

	public List<DownloadActivityType> createDownloadTypes() {
		List<DownloadActivityType> items = new ArrayList<DownloadActivityType>();
		items.add(DownloadActivityType.NORMAL_FILE);
		if (!Version.isFreeVersion(getMyApplication())) {
			items.add(DownloadActivityType.WIKIPEDIA_FILE);
		}
		items.add(DownloadActivityType.VOICE_FILE);
		items.add(DownloadActivityType.ROADS_FILE);
		if (OsmandPlugin.getEnabledPlugin(SRTMPlugin.class) != null) {
			items.add(DownloadActivityType.HILLSHADE_FILE);
			items.add(DownloadActivityType.SRTM_COUNTRY_FILE);
		}

		getMyApplication().getAppCustomization().getDownloadTypes(items);
		return items;
	}

	public boolean isLightActionBar() {
		return ((OsmandApplication) getApplication()).getSettings().isLightActionBar();
	}


	public Map<String, String> getIndexFileNames() {
		return downloadListIndexThread != null ? downloadListIndexThread.getIndexFileNames() : null;
	}

	public List<IndexItem> getIndexFiles() {
		return downloadListIndexThread != null ? downloadListIndexThread.getCachedIndexFiles() : null;
	}

	public void showDialogToDownloadMaps(Collection<String> maps) {
		int count = 0;
		int sz = 0;
		String s = "";
		for (IndexItem i : DownloadActivity.downloadListIndexThread.getCachedIndexFiles()) {
			for (String map : maps) {
				if ((i.getFileName().equals(map + ".obf.zip") || i.getFileName().equals(map + "_" + IndexConstants.BINARY_MAP_VERSION + ".obf.zip"))
						&& i.getType() == DownloadActivityType.NORMAL_FILE) {
					final List<DownloadEntry> de = i.createDownloadEntry(getMyApplication(), i.getType(), new ArrayList<DownloadEntry>(1));
					for (DownloadEntry d : de) {
						count++;
						sz += d.sizeMB;
					}
					if (s.length() > 0) {
						s += ", ";
					}
					s += i.getVisibleName(getMyApplication(), getMyApplication().getResourceManager().getOsmandRegions());
					getEntriesToDownload().put(i, de);
				}
			}
		}
		if (count > 0) {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage(getString(R.string.download_additional_maps, s, sz));
			builder.setPositiveButton(R.string.shared_string_yes, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					downloadFilesCheckInternet();
				}
			});
			builder.setNegativeButton(R.string.shared_string_no, new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					getEntriesToDownload().clear();
				}
			});
			builder.setOnCancelListener(new DialogInterface.OnCancelListener() {

				@Override
				public void onCancel(DialogInterface dialog) {
					getEntriesToDownload().clear();
				}
			});
			builder.show();

		}
	}


	public void initFreeVersionBanner(View header) {
		visibleBanner = new BannerAndDownloadFreeVersion(header, this);
		updateProgress(true);
	}


	public void showDialog(FragmentActivity activity, DialogFragment fragment) {
		fragment.show(activity.getSupportFragmentManager(), "dialog");
	}

	@Override
	public void onDialogDismissed() {
		initFreeVersionBanner(findViewById(R.id.mainLayout));
	}

	private static class ToggleCollapseFreeVersionBanner implements View.OnClickListener {
		private final View freeVersionDescriptionTextView;
		private final View buttonsLinearLayout;

		private ToggleCollapseFreeVersionBanner(View freeVersionDescriptionTextView,
												View buttonsLinearLayout) {
			this.freeVersionDescriptionTextView = freeVersionDescriptionTextView;
			this.buttonsLinearLayout = buttonsLinearLayout;
		}

		@Override
		public void onClick(View v) {
			if (freeVersionDescriptionTextView.getVisibility() == View.VISIBLE) {
				freeVersionDescriptionTextView.setVisibility(View.GONE);
				buttonsLinearLayout.setVisibility(View.GONE);
			} else {
				freeVersionDescriptionTextView.setVisibility(View.VISIBLE);
				buttonsLinearLayout.setVisibility(View.VISIBLE);
			}
		}
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

		private final Context ctx;
		private final OsmandApplication application;
		private final boolean shouldShowFreeVersionBanner;
		private final View freeVersionBannerTitle;

		public BannerAndDownloadFreeVersion(View view, Context ctx) {
			this.ctx = ctx;
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
			updateFreeVersionBanner();
		}

		public void updateProgress(int countedDownloads,
								   BasicProgressAsyncTask<?, ?, ?> basicProgressAsyncTask) {
			final boolean isFinished = basicProgressAsyncTask == null
					|| basicProgressAsyncTask.getStatus() == AsyncTask.Status.FINISHED;
			if (isFinished) {
				downloadProgressLayout.setVisibility(View.GONE);
				updateFreeVersionBanner();
			} else {
				boolean indeterminate = basicProgressAsyncTask.isIndeterminate();
				String message = basicProgressAsyncTask.getDescription();
				int percent = basicProgressAsyncTask.getProgressPercentage();

				setMinimizedFreeVersionBanner(true);
				updateAvailableDownloads(countedDownloads);
				downloadProgressLayout.setVisibility(View.VISIBLE);
				progressBar.setIndeterminate(indeterminate);
				if (indeterminate) {
					leftTextView.setText(message);
				} else {
					// TODO if only 1 map, show map name
					progressBar.setProgress(percent);
//					final String format = ctx.getString(R.string.downloading_number_of_files);
					leftTextView.setText(message);
					rightTextView.setText(percent + "%");
				}
			}

		}

		private void initFreeVersionBanner() {
			if (!shouldShowFreeVersionBanner) {
				freeVersionBanner.setVisibility(View.GONE);
				return;
			}
			freeVersionBanner.setVisibility(View.VISIBLE);
			downloadsLeftProgressBar.setMax(BaseDownloadActivity.MAXIMUM_AVAILABLE_FREE_DOWNLOADS);
			freeVersionDescriptionTextView.setText(ctx.getString(R.string.free_version_message,
					BaseDownloadActivity.MAXIMUM_AVAILABLE_FREE_DOWNLOADS));
			freeVersionBanner.findViewById(R.id.getFullVersionButton).setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					BaseDownloadActivity context = (BaseDownloadActivity) v.getContext();
					Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(Version.marketPrefix(context
							.getMyApplication()) + "net.osmand.plus"));
					try {
						context.startActivity(intent);
					} catch (ActivityNotFoundException e) {
					}
				}
			});
			laterButton.setOnClickListener(
					new ToggleCollapseFreeVersionBanner(freeVersionDescriptionTextView, buttonsLinearLayout));
		}

		private void updateFreeVersionBanner() {
			if (!shouldShowFreeVersionBanner) return;
			setMinimizedFreeVersionBanner(false);
			OsmandSettings settings = application.getSettings();
			final Integer mapsDownloaded = settings.NUMBER_OF_FREE_DOWNLOADS.get();
			downloadsLeftProgressBar.setProgress(mapsDownloaded);
			int downloadsLeft = BaseDownloadActivity.MAXIMUM_AVAILABLE_FREE_DOWNLOADS
					- mapsDownloaded;
			downloadsLeft = Math.max(downloadsLeft, 0);
			if (downloadsLeft <= 0) {
				laterButton.setVisibility(View.GONE);
			}
			downloadsLeftTextView.setText(ctx.getString(R.string.downloads_left_template, downloadsLeft));
			// TODO review logic
			freeVersionBanner.setOnClickListener(new ToggleCollapseFreeVersionBanner(freeVersionDescriptionTextView,
					buttonsLinearLayout));
		}

		private void updateAvailableDownloads(int activeTasks) {
			OsmandSettings settings = application.getSettings();
			final Integer mapsDownloaded = settings.NUMBER_OF_FREE_DOWNLOADS.get() + activeTasks;
			downloadsLeftProgressBar.setProgress(mapsDownloaded);
		}

		private void setMinimizedFreeVersionBanner(boolean minimize) {
			if (minimize) {
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

}
