package net.osmand.plus.download;

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

import net.osmand.IProgress;
import net.osmand.access.AccessibleToast;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.activities.LocalIndexInfo;
import net.osmand.plus.activities.TabActivity;
import net.osmand.plus.base.BasicProgressAsyncTask;
import net.osmand.plus.download.DownloadIndexesThread.DownloadEvents;
import net.osmand.plus.download.ui.ActiveDownloadsDialogFragment;
import net.osmand.plus.download.ui.DownloadResourceGroupFragment;
import net.osmand.plus.download.ui.LocalIndexesFragment;
import net.osmand.plus.download.ui.UpdatesIndexFragment;
import net.osmand.plus.views.controls.PagerSlidingTabStrip;

import java.io.File;
import java.lang.ref.WeakReference;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DownloadActivity extends BaseDownloadActivity {
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

	private BannerAndDownloadFreeVersion visibleBanner;
	private ViewPager viewPager;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		getMyApplication().applyTheme(this);
		super.onCreate(savedInstanceState);
		DownloadResources indexes = getDownloadThread().getIndexes();
		if (!indexes.isDownloadedFromInternet) {
			getDownloadThread().runReloadIndexFiles();
		}

		setContentView(R.layout.download);
		final View downloadProgressLayout = findViewById(R.id.downloadProgressLayout);
		downloadProgressLayout.setVisibility(View.VISIBLE);
		updateDescriptionTextWithSize(this, downloadProgressLayout);
		int currentTab = 0;
		String tab = getIntent() == null || getIntent().getExtras() == null ? null : getIntent().getExtras().getString(TAB_TO_OPEN);
		switch (tab) {
			case DOWNLOAD_TAB:
				currentTab = DOWNLOAD_TAB_NUMBER;
				break;
			case LOCAL_TAB:
				currentTab = LOCAL_TAB_NUMBER;
				break;
			case UPDATES_TAB:
				currentTab = UPDATES_TAB_NUMBER;
				break;
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
				if (i == UPDATES_TAB_NUMBER) {
					visibleBanner.hideDownloadProgressLayout();
				} else {
					visibleBanner.showDownloadProgressLayout();
				}
			}

			@Override
			public void onPageScrollStateChanged(int i) {

			}
		});
		visibleBanner = new BannerAndDownloadFreeVersion(findViewById(R.id.mainLayout), this);

		final Intent intent = getIntent();
		// FIXME INITIAL FILTER & INITIAL KEY
		if (intent != null && intent.getExtras() != null) {
			final String filter = intent.getExtras().getString(FILTER_KEY);
			final String filterCat = intent.getExtras().getString(FILTER_CAT);
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		getMyApplication().getAppCustomization().resumeActivity(DownloadActivity.class, this);
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
	}

	@Override
	@UiThread
	public void downloadHasFinished() {
		visibleBanner.updateBannerInProgress();
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
		int downloadsLeft = BaseDownloadActivity.MAXIMUM_AVAILABLE_FREE_DOWNLOADS - mapsDownloaded;
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

		public BannerAndDownloadFreeVersion(View view, final DownloadActivity ctx) {
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
			updateBannerInProgress();

			if (ctx.getCurrentTab() != UPDATES_TAB_NUMBER) {
				downloadProgressLayout.setVisibility(View.VISIBLE);
			}
		}

		public void updateBannerInProgress() {
			BasicProgressAsyncTask<?, ?, ?, ?> basicProgressAsyncTask = ctx.getDownloadThread().getCurrentRunningTask();
			final boolean isFinished = basicProgressAsyncTask == null
					|| basicProgressAsyncTask.getStatus() == AsyncTask.Status.FINISHED;
			if (isFinished) {
				downloadProgressLayout.setOnClickListener(null);
				updateDescriptionTextWithSize(ctx, downloadProgressLayout);
				if (ctx.getCurrentTab() == UPDATES_TAB_NUMBER) {
					downloadProgressLayout.setVisibility(View.GONE);
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
					buttonsLinearLayout, freeVersionBannerTitle, application.getSettings()));
		}

		private void updateFreeVersionBanner() {
			if (!shouldShowFreeVersionBanner) {
				return;
			}
			setMinimizedFreeVersionBanner(false);
			OsmandSettings settings = application.getSettings();
			final Integer mapsDownloaded = settings.NUMBER_OF_FREE_DOWNLOADS.get();
			downloadsLeftProgressBar.setProgress(mapsDownloaded);
			int downloadsLeft = BaseDownloadActivity.MAXIMUM_AVAILABLE_FREE_DOWNLOADS - mapsDownloaded;
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
			percent = 100 - (int) (fs.getAvailableBlocks() * 100 / fs.getBlockCount());
		}
		sizeProgress.setIndeterminate(false);
		sizeProgress.setProgress(percent);
		String text = activity.getString(R.string.free, size);
		descriptionText.setText(text);
		descriptionText.setMovementMethod(LinkMovementMethod.getInstance());

		messageTextView.setText(R.string.device_memory);
	}

}
