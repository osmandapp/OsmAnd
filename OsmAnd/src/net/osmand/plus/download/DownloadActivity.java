package net.osmand.plus.download;

import java.io.File;
import java.lang.ref.WeakReference;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
import net.osmand.plus.download.items.ActiveDownloadsDialogFragment;
import net.osmand.plus.download.items.DialogDismissListener;
import net.osmand.plus.download.items.RegionItemsFragment;
import net.osmand.plus.download.items.SearchDialogFragment;
import net.osmand.plus.download.items.WorldItemsFragment;
import net.osmand.plus.srtmplugin.SRTMPlugin;
import net.osmand.plus.views.controls.PagerSlidingTabStrip;

import org.apache.commons.logging.Log;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.StatFs;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.text.method.LinkMovementMethod;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListAdapter;
import android.widget.ProgressBar;
import android.widget.TextView;


public class DownloadActivity extends BaseDownloadActivity implements DialogDismissListener {
	private List<LocalIndexInfo> localIndexInfos = new ArrayList<>();

	List<TabActivity.TabItem> mTabs = new ArrayList<TabActivity.TabItem>();
	public static final String FILTER_KEY = "filter";
	public static final String FILTER_CAT = "filter_cat";

	public static final String TAB_TO_OPEN = "Tab_to_open";
	public static final String LOCAL_TAB = "local";
	public static final String DOWNLOAD_TAB = "download";
	public static final String UPDATES_TAB = "updates";
	public static final MessageFormat formatGb = new MessageFormat("{0, number,#.##} GB", Locale.US);

	private BannerAndDownloadFreeVersion visibleBanner;
	private ActiveDownloadsDialogFragment activeDownloads;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		getMyApplication().applyTheme(this);
		super.onCreate(savedInstanceState);
		if (downloadListIndexThread.getCachedIndexFiles() == null || !downloadListIndexThread.isDownloadedFromInternet()) {
			downloadListIndexThread.runReloadIndexFiles();
		}
		
		setContentView(R.layout.download);
		int currentTab = 0;
		String tab = getIntent() == null || getIntent().getExtras() == null ? null : getIntent().getExtras().getString(TAB_TO_OPEN);
		if (tab != null) {
			if (tab.equals(DOWNLOAD_TAB)) {
				currentTab = 1;
			} else if (tab.equals(UPDATES_TAB)) {
				currentTab = 2;
			}
		}
		ViewPager viewPager = (ViewPager) findViewById(R.id.pager);
		PagerSlidingTabStrip mSlidingTabLayout = (PagerSlidingTabStrip) findViewById(R.id.sliding_tabs);


		mTabs.add(new TabActivity.TabItem(R.string.download_tab_downloads,
				getString(R.string.download_tab_downloads), WorldItemsFragment.class));
		mTabs.add(new TabActivity.TabItem(R.string.download_tab_local,
				getString(R.string.download_tab_local), LocalIndexesFragment.class));
		mTabs.add(new TabActivity.TabItem(R.string.download_tab_updates,
				getString(R.string.download_tab_updates), UpdatesIndexFragment.class));

		viewPager.setAdapter(new TabActivity.OsmandFragmentPagerAdapter(getSupportFragmentManager(), mTabs));
		mSlidingTabLayout.setViewPager(viewPager);

		viewPager.setCurrentItem(currentTab);

		final Intent intent = getIntent();
		// FIXME INITIAL FILTER & INITIAL KEY
		if (intent != null && intent.getExtras() != null) {
			final String filter = intent.getExtras().getString(FILTER_KEY);
			final String filterCat = intent.getExtras().getString(FILTER_CAT);
		}
		registerFreeVersionBanner(findViewById(R.id.mainLayout));
	}


	public Map<String, String> getIndexActivatedFileNames() {
		return downloadListIndexThread != null ? downloadListIndexThread.getIndexActivatedFileNames() : null;
	}

	@Override
	protected void onResume() {
		super.onResume();
		getMyApplication().getAppCustomization().resumeActivity(DownloadActivity.class, this);
		updateProgress(false);
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
	}

	@Override
	public void updateProgress(boolean updateOnlyProgress) {
		BasicProgressAsyncTask<?, ?, ?, ?> basicProgressAsyncTask =
				downloadListIndexThread.getCurrentRunningTask();
		if (visibleBanner != null) {
			final int countedDownloads = downloadListIndexThread.getCountedDownloads();
			visibleBanner.updateProgress(countedDownloads, basicProgressAsyncTask);
		}
		if(activeDownloads != null) {
			if(updateOnlyProgress) {
				activeDownloads.notifyDataSetChanged();
			} else {
				activeDownloads.notifyDataSetInvalidated();
			}
		}
		// FIXME
		//((DownloadActivity) getActivity()).updateDescriptionTextWithSize(getView());
		for (WeakReference<Fragment> ref : fragSet) {
			Fragment f = ref.get();
			notifyUpdateDataSetChanged(f);
			if(f instanceof RegionItemsFragment) {
				Fragment innerFragment = ((RegionItemsFragment)f).getChildFragmentManager().findFragmentById(R.id.fragmentContainer);
				notifyUpdateDataSetChanged(innerFragment);
			}
		}
	}

	@Override
	public void updateDownloadList() {
		for (WeakReference<Fragment> ref : fragSet) {
			Fragment f = ref.get();
			if (f instanceof UpdatesIndexFragment) {
				if (f.isAdded()) {
					((UpdatesIndexFragment) f).updateItemsList();
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
			} else if (f instanceof SearchDialogFragment) {
				if (f.isAdded()) {
					((SearchDialogFragment) f).onCategorizationFinished();
				}
			}
		}
	}


	@Override
	public void downloadedIndexes() {
		for (WeakReference<Fragment> ref : fragSet) {
			Fragment f = ref.get();
			if (f instanceof LocalIndexesFragment) {
				if (f.isAdded()) {
					((LocalIndexesFragment) f).reloadData();
				}
			}
		}
	}


	
	public void setActiveDownloads(ActiveDownloadsDialogFragment activeDownloads) {
		this.activeDownloads = activeDownloads;
	}
	


	private void notifyUpdateDataSetChanged(Fragment f) {
		if (f != null && f.isAdded()) {
			if(f instanceof OsmandExpandableListFragment) {
				ExpandableListAdapter adapter = ((OsmandExpandableListFragment) f).getAdapter();
				if(adapter instanceof BaseExpandableListAdapter) {
					((BaseExpandableListAdapter)adapter).notifyDataSetChanged();
				}
			}
			if(f instanceof OsmAndListFragment) {
				 ArrayAdapter<?> adapter = ((OsmAndListFragment) f).getAdapter();
				if(adapter != null) {
					adapter.notifyDataSetChanged();
				}
			}
		}
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


	public void registerFreeVersionBanner(View view) {
		visibleBanner = new BannerAndDownloadFreeVersion(view, this);
		updateProgress(true);
	}


	public void showDialog(FragmentActivity activity, DialogFragment fragment) {
		fragment.show(activity.getSupportFragmentManager(), "dialog");
	}

	@Override
	public void onDialogDismissed() {
		registerFreeVersionBanner(findViewById(R.id.mainLayout));
	}

	private static class ToggleCollapseFreeVersionBanner implements View.OnClickListener {
		private final View freeVersionDescriptionTextView;
		private final View buttonsLinearLayout;
		private final View freeVersionTitle;

		private ToggleCollapseFreeVersionBanner(View freeVersionDescriptionTextView,
												View buttonsLinearLayout, View freeVersionTitle) {
			this.freeVersionDescriptionTextView = freeVersionDescriptionTextView;
			this.buttonsLinearLayout = buttonsLinearLayout;
			this.freeVersionTitle = freeVersionTitle;
		}

		@Override
		public void onClick(View v) {
			if (freeVersionDescriptionTextView.getVisibility() == View.VISIBLE) {
				freeVersionDescriptionTextView.setVisibility(View.GONE);
				buttonsLinearLayout.setVisibility(View.GONE);
			} else {
				freeVersionDescriptionTextView.setVisibility(View.VISIBLE);
				buttonsLinearLayout.setVisibility(View.VISIBLE);
				freeVersionTitle.setVisibility(View.VISIBLE);
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

		private final FragmentActivity ctx;
		private final OsmandApplication application;
		private final boolean shouldShowFreeVersionBanner;
		private final View freeVersionBannerTitle;

		public BannerAndDownloadFreeVersion(View view, final FragmentActivity ctx) {
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
			downloadProgressLayout.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					new ActiveDownloadsDialogFragment().show(ctx.getSupportFragmentManager(), "dialog");
				}
			});
		}

		public void updateProgress(int countedDownloads,
								   BasicProgressAsyncTask<?, ?, ?, ?> basicProgressAsyncTask) {
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
			laterButton.setOnClickListener(new ToggleCollapseFreeVersionBanner(freeVersionDescriptionTextView,
					buttonsLinearLayout, freeVersionBannerTitle));
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
			freeVersionBanner.setOnClickListener(new ToggleCollapseFreeVersionBanner(freeVersionDescriptionTextView,
					buttonsLinearLayout, freeVersionBannerTitle));
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


	@SuppressWarnings("deprecation")
	public void updateDescriptionTextWithSize(View view){
		TextView descriptionText = (TextView) view.findViewById(R.id.sizeFreeTextView);
		ProgressBar sizeProgress = (ProgressBar) view.findViewById(R.id.memoryLeftProgressBar);

		File dir = getMyApplication().getAppPath("").getParentFile();
		String size = formatGb.format(new Object[]{0});
		int percent = 0;
		if(dir.canRead()){
			StatFs fs = new StatFs(dir.getAbsolutePath());
			size = formatGb.format(new Object[]{(float) (fs.getAvailableBlocks()) * fs.getBlockSize() / (1 << 30) });
			percent = 100 - (int) (fs.getAvailableBlocks() * 100 / fs.getBlockCount());
		}
		sizeProgress.setProgress(percent);
		String text = getString(R.string.free, size);
		int l = text.indexOf('.');
		if(l == -1) {
			l = text.length();
		}
		descriptionText.setText(text);
		descriptionText.setMovementMethod(LinkMovementMethod.getInstance());
	}
	
}
