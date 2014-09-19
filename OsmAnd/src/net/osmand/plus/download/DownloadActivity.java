package net.osmand.plus.download;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.widget.*;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Window;
import net.osmand.IndexConstants;
import net.osmand.access.AccessibleAlertBuilder;
import net.osmand.plus.*;
import net.osmand.plus.activities.FavouritesActivity;
import net.osmand.plus.activities.SettingsGeneralActivity;
import net.osmand.plus.base.BasicProgressAsyncTask;
import net.osmand.plus.base.SuggestExternalDirectoryDialog;
import net.osmand.plus.srtmplugin.SRTMPlugin;

import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Denis on 08.09.2014.
 */
public class DownloadActivity extends SherlockFragmentActivity {

	private TabHost tabHost;
	private FavouritesActivity.TabsAdapter mTabsAdapter;
	public static DownloadIndexesThread downloadListIndexThread;
	private DownloadActivityType type = DownloadActivityType.NORMAL_FILE;
	public static final int MAXIMUM_AVAILABLE_FREE_DOWNLOADS = 10;

	private OsmandSettings settings;

	private View progressView;
	private ProgressBar indeterminateProgressBar;
	private ProgressBar determinateProgressBar;
	private TextView progressMessage;
	private TextView progressPercent;
	private ImageView cancel;
	private UpdatesIndexFragment updatesIndexFragment;


	public static final String FILTER_KEY = "filter";
	public static final String FILTER_CAT = "filter_cat";


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		getMyApplication().applyTheme(this);
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setProgressBarIndeterminateVisibility(false);

		setContentView(R.layout.tab_content);
		OsmandSettings settings = ((OsmandApplication) getApplication()).getSettings();
		tabHost = (TabHost) findViewById(android.R.id.tabhost);
		tabHost.setup();
		ViewPager viewPager = (ViewPager) findViewById(R.id.pager);
		mTabsAdapter = new FavouritesActivity.TabsAdapter(this, tabHost, viewPager, settings);
		mTabsAdapter.addTab(tabHost.newTabSpec("LOCAL_INDEX").setIndicator("Local"),
				LocalIndexesFragment.class, null);
		mTabsAdapter.addTab(tabHost.newTabSpec("DOWNLOADS").setIndicator("Downloads"),
				DownloadIndexFragment.class, null);
		mTabsAdapter.addTab(tabHost.newTabSpec("UPDATES").setIndicator("Updates"),
				UpdatesIndexFragment.class, null);
		tabHost.setCurrentTab(0);

		if(downloadListIndexThread == null) {
			downloadListIndexThread = new DownloadIndexesThread(this);
		}
		if (downloadListIndexThread.getCachedIndexFiles() != null && downloadListIndexThread.isDownloadedFromInternet()) {
			downloadListIndexThread.runCategorization(type);
		} else {
			downloadListIndexThread.runReloadIndexFiles();
		}

		settings = ((OsmandApplication)getApplication()).getSettings();

		indeterminateProgressBar = (ProgressBar) findViewById(R.id.IndeterminateProgressBar);
		determinateProgressBar = (ProgressBar) findViewById(R.id.DeterminateProgressBar);
		progressView = findViewById(R.id.ProgressView);
		progressMessage = (TextView) findViewById(R.id.ProgressMessage);
		progressPercent = (TextView) findViewById(R.id.ProgressPercent);
		cancel = (ImageView) findViewById(R.id.Cancel);
		int d = settings.isLightContent() ? R.drawable.a_1_navigation_cancel_small_light : R.drawable.a_1_navigation_cancel_small_dark;
		cancel.setImageDrawable(getResources().getDrawable(d));
		cancel.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				makeSureUserCancelDownload();
			}
		});

		findViewById(R.id.DownloadButton).setOnClickListener(new View.OnClickListener(){

			@Override
			public void onClick(View v) {
				downloadFilesCheckFreeVersion();
			}

		});

		final List<DownloadActivityType> downloadTypes = getDownloadTypes();
		final Intent intent = getIntent();
		setType(downloadTypes.get(0));
		if (intent != null && intent.getExtras() != null) {
			final String filter = intent.getExtras().getString(FILTER_KEY);
//			if (filter != null) {
//				filterText.setText(filter);
//			}
			final String filterCat = intent.getExtras().getString(FILTER_CAT);
			if (filterCat != null) {
				DownloadActivityType type = DownloadActivityType.getIndexType(filterCat.toLowerCase());
				if (type != null) {
					setType(type);
					downloadTypes.remove(type);
					downloadTypes.add(0, type);
				}
			}
		}

		if(getMyApplication().getResourceManager().getIndexFileNames().isEmpty()) {
			boolean showedDialog = false;
			if(Build.VERSION.SDK_INT < OsmandSettings.VERSION_DEFAULTLOCATION_CHANGED) {
				SuggestExternalDirectoryDialog.showDialog(this, null, null);
			}
			if(!showedDialog) {
				showDialogOfFreeDownloadsIfNeeded();
			}
		} else {
			showDialogOfFreeDownloadsIfNeeded();
		}


		if (Build.VERSION.SDK_INT >= OsmandSettings.VERSION_DEFAULTLOCATION_CHANGED) {
			final String currentStorage = settings.getExternalStorageDirectory().getAbsolutePath();
			String primaryStorage = settings.getDefaultExternalStorageLocation();
			if (!currentStorage.startsWith(primaryStorage)) {
				// secondary storage
				boolean currentDirectoryNotWritable = true;
				for (String writeableDirectory : settings.getWritableSecondaryStorageDirectorys()) {
					if (currentStorage.startsWith(writeableDirectory)) {
						currentDirectoryNotWritable = false;
						break;
					}
				}
				if (currentDirectoryNotWritable) {
					currentDirectoryNotWritable = !OsmandSettings.isWritable(settings.getExternalStorageDirectory());
				}
				if (currentDirectoryNotWritable) {
					final String newLoc = settings.getMatchingExternalFilesDir(currentStorage);
					if (newLoc != null && newLoc.length() != 0) {
						AccessibleAlertBuilder ab = new AccessibleAlertBuilder(this);
						ab.setMessage(getString(R.string.android_19_location_disabled,
								settings.getExternalStorageDirectory()));
						ab.setPositiveButton(R.string.default_buttons_yes, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								copyFilesForAndroid19(newLoc);
							}
						});
						ab.setNegativeButton(R.string.default_buttons_cancel, null);
						ab.show();
					}
				}
			}
		}

		getSupportActionBar().setHomeButtonEnabled(true);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
	}

	public void setUpdatesIndexFragment(UpdatesIndexFragment fragment){
		this.updatesIndexFragment = fragment;
	}

	@Override
	protected void onResume() {
		super.onResume();
		BasicProgressAsyncTask<?, ?, ?> t = downloadListIndexThread.getCurrentRunningTask();
	}


	@Override
	public boolean onOptionsItemSelected(com.actionbarsherlock.view.MenuItem item) {
		int itemId = item.getItemId();
		switch (itemId) {
			case android.R.id.home:
				finish();
				return true;

		}
		return false;
	}


	public DownloadActivityType getType() { return type;}

	public void setType(DownloadActivityType type) { this.type = type;}

	public void changeType(final DownloadActivityType tp) {
		//invalidateOptionsMenu();
		if (downloadListIndexThread != null && type != tp) {
			type = tp;
			downloadListIndexThread.runCategorization(tp);
		}
	}

	public void downloadFilesPreCheckSpace() {
		double sz = 0;
		List<DownloadEntry> list = downloadListIndexThread.flattenDownloadEntries();
		for (DownloadEntry es :  list) {
			sz += es.sizeMB;
		}
		// get availabile space
		double asz = downloadListIndexThread.getAvailableSpace();
		if (asz != -1 && asz > 0 && sz / asz > 0.4) {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage(MessageFormat.format(getString(R.string.download_files_question_space), list.size(), sz, asz));
			builder.setPositiveButton(R.string.default_buttons_yes, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					downloadListIndexThread.runDownloadFiles();
				}
			});
			builder.setNegativeButton(R.string.default_buttons_no, null);
			builder.show();
		} else {
			downloadListIndexThread.runDownloadFiles();
		}

	}

	public Map<IndexItem, List<DownloadEntry>> getEntriesToDownload() {
		if(downloadListIndexThread == null) {
			return new LinkedHashMap<IndexItem, List<DownloadEntry>>();
		}
		return downloadListIndexThread.getEntriesToDownload();
	}

	@Override
	public void onPause() {
		super.onPause();
		(getMyApplication()).setDownloadActivity(null);
	}

	protected void downloadFilesCheckFreeVersion() {
		if (Version.isFreeVersion(getMyApplication()) ) {
			int total = settings.NUMBER_OF_FREE_DOWNLOADS.get();
			boolean wiki = false;
			for (IndexItem es : DownloadActivity.downloadListIndexThread.getEntriesToDownload().keySet()) {
				if (es.getBasename() != null && es.getBasename().contains("_wiki")) {
					wiki = true;
					break;
				} else if (DownloadActivityType.isCountedInDownloads(es.getType())) {
					total++;
				}
			}
			if (total > MAXIMUM_AVAILABLE_FREE_DOWNLOADS || wiki) {
				String msgTx = getString(R.string.free_version_message, MAXIMUM_AVAILABLE_FREE_DOWNLOADS + "");
				AlertDialog.Builder msg = new AlertDialog.Builder(this);
				msg.setTitle(R.string.free_version_title);
				msg.setMessage(msgTx);
				msg.setPositiveButton(R.string.default_buttons_ok, null);
				msg.show();
			} else {
				downloadFilesCheckInternet();
			}
		} else {
			downloadFilesCheckInternet();
		}
	}

	protected void downloadFilesCheckInternet() {
		if(!getMyApplication().getSettings().isWifiConnected()) {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage(getString(R.string.download_using_mobile_internet));
			builder.setPositiveButton(R.string.default_buttons_yes, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					downloadFilesPreCheckSpace();
				}
			});
			builder.setNegativeButton(R.string.default_buttons_no, null);
			builder.show();
		} else {
			downloadFilesPreCheckSpace();
		}
	}

	public OsmandApplication getMyApplication(){ return (OsmandApplication)getApplication();}

	public void showDialogOfFreeDownloadsIfNeeded() {
		if (Version.isFreeVersion(getMyApplication())) {
			AlertDialog.Builder msg = new AlertDialog.Builder(this);
			msg.setTitle(R.string.free_version_title);
			String m = getString(R.string.free_version_message, MAXIMUM_AVAILABLE_FREE_DOWNLOADS + "", "") + "\n";
			m += getString(R.string.available_downloads_left, MAXIMUM_AVAILABLE_FREE_DOWNLOADS - settings.NUMBER_OF_FREE_DOWNLOADS.get());
			msg.setMessage(m);
			if (Version.isMarketEnabled(getMyApplication())) {
				msg.setNeutralButton(R.string.install_paid, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(Version.marketPrefix(getMyApplication()) + "net.osmand.plus"));
						try {
							startActivity(intent);
						} catch (ActivityNotFoundException e) {
						}
					}
				});
			}
			msg.setPositiveButton(R.string.default_buttons_ok, null);
			msg.show();
		}
	}

	public void updateProgress(boolean updateOnlyProgress) {
		BasicProgressAsyncTask<?, ?, ?> basicProgressAsyncTask = DownloadActivity.downloadListIndexThread.getCurrentRunningTask();
		//needed when rotation is performed and progress can be null
		if (progressView == null){
			return;
		}
		if(updateOnlyProgress) {
			if(!basicProgressAsyncTask.isIndeterminate()) {
				progressPercent.setText(basicProgressAsyncTask.getProgressPercentage() +"%");
				determinateProgressBar.setProgress(basicProgressAsyncTask.getProgressPercentage());
			}
		} else {
			boolean visible = basicProgressAsyncTask != null && basicProgressAsyncTask.getStatus() != AsyncTask.Status.FINISHED;
			progressView.setVisibility(visible ? View.VISIBLE : View.GONE);
			if (visible) {
				boolean indeterminate = basicProgressAsyncTask.isIndeterminate();
				indeterminateProgressBar.setVisibility(!indeterminate ? View.GONE : View.VISIBLE);
				determinateProgressBar.setVisibility(indeterminate ? View.GONE : View.VISIBLE);
				cancel.setVisibility(indeterminate ? View.GONE : View.VISIBLE);
				progressPercent.setVisibility(indeterminate ? View.GONE : View.VISIBLE);

				progressMessage.setText(basicProgressAsyncTask.getDescription());
				if (!indeterminate) {
					progressPercent.setText(basicProgressAsyncTask.getProgressPercentage() + "%");
					determinateProgressBar.setProgress(basicProgressAsyncTask.getProgressPercentage());
				}
			}
			updateDownloadButton(false);

		}
	}

	private void makeSureUserCancelDownload() {
		AlertDialog.Builder bld = new AlertDialog.Builder(this);
		bld.setTitle(getString(R.string.default_buttons_cancel));
		bld.setMessage(R.string.confirm_interrupt_download);
		bld.setPositiveButton(R.string.default_buttons_yes, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
				BasicProgressAsyncTask<?, ?, ?> t = DownloadActivity.downloadListIndexThread.getCurrentRunningTask();
				if(t != null) {
					t.setInterrupted(true);
				}
			}
		});
		bld.setNegativeButton(R.string.default_buttons_no, null);
		bld.show();
	}

	public void updateDownloadList(List<IndexItem> list){
		if(updatesIndexFragment == null){
			return;
		}
		updatesIndexFragment.updateItemsList(list);
	}

	public void updateDownloadButton(boolean scroll) {
//		View view = getView();
//		if (view == null || getExpandableListView() == null){
//			return;
//		}
//		int x = getExpandableListView().getScrollX();
//		int y = getExpandableListView().getScrollY();
		if (getEntriesToDownload().isEmpty()) {
			findViewById(R.id.DownloadButton).setVisibility(View.GONE);
		} else {
			BasicProgressAsyncTask<?, ?, ?> task = DownloadActivity.downloadListIndexThread.getCurrentRunningTask();
			boolean running = task instanceof DownloadIndexesThread.DownloadIndexesAsyncTask;
			((Button) findViewById(R.id.DownloadButton)).setEnabled(!running);
			String text;
			int downloads = DownloadActivity.downloadListIndexThread.getDownloads();
			if (!running) {
				text = getString(R.string.download_files) + "  (" + downloads + ")"; //$NON-NLS-1$
			} else {
				text = getString(R.string.downloading_file_new) + "  (" + downloads + ")"; //$NON-NLS-1$
			}
			findViewById(R.id.DownloadButton).setVisibility(View.VISIBLE);
			if (Version.isFreeVersion(getMyApplication())) {
				int countedDownloads = DownloadActivity.downloadListIndexThread.getDownloads();
				int left = DownloadActivity.MAXIMUM_AVAILABLE_FREE_DOWNLOADS - settings.NUMBER_OF_FREE_DOWNLOADS.get() - downloads;
				boolean excessLimit = left < 0;
				if (left < 0)
					left = 0;
				if (DownloadActivityType.isCountedInDownloads(getType())) {
					text += " (" + (excessLimit ? "! " : "") + getString(R.string.files_limit, left).toLowerCase() + ")";
				}
			}
			((Button) findViewById(R.id.DownloadButton)).setText(text);
		}
//		if (scroll) {
//			getExpandableListView().scrollTo(x, y);
//		}
	}

	public List<DownloadActivityType> getDownloadTypes() {
		List<DownloadActivityType> items = new ArrayList<DownloadActivityType>();
		items.add(DownloadActivityType.NORMAL_FILE);
		items.add(DownloadActivityType.VOICE_FILE);
		items.add(DownloadActivityType.ROADS_FILE);
		if(OsmandPlugin.getEnabledPlugin(SRTMPlugin.class) != null){
			items.add(DownloadActivityType.HILLSHADE_FILE);
			items.add(DownloadActivityType.SRTM_COUNTRY_FILE);
		}
		getMyApplication().getAppCustomization().getDownloadTypes(items);
		return items;
	}

	public boolean isLightActionBar() {
		return ((OsmandApplication) getApplication()).getSettings().isLightActionBar();
	}

	private void copyFilesForAndroid19(final String newLoc) {
		SettingsGeneralActivity.MoveFilesToDifferentDirectory task =
				new SettingsGeneralActivity.MoveFilesToDifferentDirectory(this,
						new File(settings.getExternalStorageDirectory(), IndexConstants.APP_DIR),
						new File(newLoc, IndexConstants.APP_DIR)) {
					protected Boolean doInBackground(Void[] params) {
						Boolean result = super.doInBackground(params);
						if(result) {
							settings.setExternalStorageDirectory(newLoc);
							getMyApplication().getResourceManager().resetStoreDirectory();
							getMyApplication().getResourceManager().reloadIndexes(progress)	;
						}
						return result;
					};
				};
		task.execute();
	}

}
