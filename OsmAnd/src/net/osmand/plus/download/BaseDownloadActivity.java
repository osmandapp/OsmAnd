package net.osmand.plus.download;

import java.lang.ref.WeakReference;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.osmand.access.AccessibleToast;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.activities.ActionBarProgressActivity;
import net.osmand.plus.base.BasicProgressAsyncTask;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.widget.Toast;

/**
 * Created by Denis
 * on 25.11.2014.
 */
public class BaseDownloadActivity extends ActionBarProgressActivity {
	protected DownloadActivityType type = DownloadActivityType.NORMAL_FILE;
	protected OsmandSettings settings;
	public static DownloadIndexesThread downloadListIndexThread;
	protected List<WeakReference<Fragment>> fragList = new ArrayList<WeakReference<Fragment>>();
	protected List<IndexItem> downloadQueue = new ArrayList<IndexItem>();

	public static final int MAXIMUM_AVAILABLE_FREE_DOWNLOADS = 10;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		settings = ((OsmandApplication) getApplication()).getSettings();
		if (downloadListIndexThread == null) {
			downloadListIndexThread = new DownloadIndexesThread(this);
		}
		super.onCreate(savedInstanceState);
		// Having the next line here causes bug AND-197: The storage folder dialogue popped up upon EVERY app startup, because the map list is not indexed yet.
		// Hence line moved to updateDownloads() now.
		// prepareDownloadDirectory();
	}

	public void updateDownloads() {
		if (downloadListIndexThread.getCachedIndexFiles() != null && downloadListIndexThread.isDownloadedFromInternet()) {
			downloadListIndexThread.runCategorization(DownloadActivityType.NORMAL_FILE);
		} else {
			downloadListIndexThread.runReloadIndexFiles();
		}
		prepareDownloadDirectory();
	}


	@Override
	protected void onResume() {
		super.onResume();
		downloadListIndexThread.setUiActivity(this);
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		downloadListIndexThread.setUiActivity(null);
	}


	public void updateDownloadList(List<IndexItem> list) {

	}

	public void updateProgress(boolean updateOnlyProgress) {

	}

	public DownloadActivityType getDownloadType() {
		return type;
	}

	public Map<IndexItem, List<DownloadEntry>> getEntriesToDownload() {
		if (downloadListIndexThread == null) {
			return new LinkedHashMap<IndexItem, List<DownloadEntry>>();
		}
		return downloadListIndexThread.getEntriesToDownload();
	}

	public void downloadedIndexes() {

	}

	public void updateDownloadButton() {

	}

	public void downloadListUpdated() {

	}

	public OsmandApplication getMyApplication() {
		return (OsmandApplication) getApplication();
	}

	public void categorizationFinished(List<IndexItem> filtered, List<IndexItemCategory> cats) {

	}

	public boolean startDownload(IndexItem item) {
		if (downloadListIndexThread.getCurrentRunningTask() != null && getEntriesToDownload().get(item) == null) {
			downloadQueue.add(item);
			return false;
		}
		addToDownload(item);
		downloadFilesCheckFreeVersion();
		return true;
	}

	private void addToDownload(IndexItem item) {
		List<DownloadEntry> download = item.createDownloadEntry(getMyApplication(), item.getType(), new ArrayList<DownloadEntry>());
		getEntriesToDownload().put(item, download);
	}

	public void downloadFilesPreCheckSpace() {
		double sz = 0;
		List<DownloadEntry> list = downloadListIndexThread.flattenDownloadEntries();
		for (DownloadEntry es : list) {
			sz += es.sizeMB;
		}
		// get availabile space
		double asz = downloadListIndexThread.getAvailableSpace();
		if (asz != -1 && asz > 0 && sz / asz > 0.4) {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage(MessageFormat.format(getString(R.string.download_files_question_space), list.size(), sz, asz));
			builder.setPositiveButton(R.string.shared_string_yes, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					downloadListIndexThread.runDownloadFiles();
				}
			});
			builder.setNegativeButton(R.string.shared_string_no, null);
			builder.show();
		} else {
			downloadListIndexThread.runDownloadFiles();
		}
	}

	protected void downloadFilesCheckFreeVersion() {
		if (Version.isFreeVersion(getMyApplication())) {
			int total = settings.NUMBER_OF_FREE_DOWNLOADS.get();
			boolean wiki = false;
			for (IndexItem es : DownloadActivity.downloadListIndexThread.getEntriesToDownload().keySet()) {
				if (es.getBasename() != null && es.getBasename().contains("_wiki")) {
					wiki = true;
					break;
				} else if (DownloadActivityType.isCountedInDownloads(es)) {
					total++;
				}
			}
			if (total > MAXIMUM_AVAILABLE_FREE_DOWNLOADS || wiki) {
				String msgTx = getString(R.string.free_version_message, MAXIMUM_AVAILABLE_FREE_DOWNLOADS + "");
				AlertDialog.Builder msg = new AlertDialog.Builder(this);
				msg.setTitle(R.string.free_version_title);
				msg.setMessage(msgTx);
				msg.setPositiveButton(R.string.shared_string_ok, null);
				msg.show();
			} else {
				downloadFilesCheckInternet();
			}
		} else {
			downloadFilesCheckInternet();
		}
	}

	protected void downloadFilesCheckInternet() {
		if (!getMyApplication().getSettings().isWifiConnected()) {
			if (getMyApplication().getSettings().isInternetConnectionAvailable()) {
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setMessage(getString(R.string.download_using_mobile_internet));
				builder.setPositiveButton(R.string.shared_string_yes, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						downloadFilesPreCheckSpace();
					}
				});
				builder.setNegativeButton(R.string.shared_string_no, null);
				builder.show();
			} else {
				AccessibleToast.makeText(this, R.string.no_index_file_to_download, Toast.LENGTH_LONG).show();
			}
		} else {
			downloadFilesPreCheckSpace();
		}
	}

	@Override
	public void onAttachFragment(Fragment fragment) {
		fragList.add(new WeakReference<Fragment>(fragment));
	}

	public void makeSureUserCancelDownload() {
		AlertDialog.Builder bld = new AlertDialog.Builder(this);
		bld.setTitle(getString(R.string.shared_string_cancel));
		bld.setMessage(R.string.confirm_interrupt_download);
		bld.setPositiveButton(R.string.shared_string_yes, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
				cancelDownload();
			}
		});
		bld.setNegativeButton(R.string.shared_string_no, null);
		bld.show();
	}

	public void cancelDownload() {
		BasicProgressAsyncTask<?, ?, ?> t = DownloadActivity.downloadListIndexThread.getCurrentRunningTask();
		if (t != null) {
			t.setInterrupted(true);
		}
		// list of items to download need to be cleared in case of dashboard activity
//		if (this instanceof MainMenuActivity) {
//			getEntriesToDownload().clear();
//		}
	}

	private void prepareDownloadDirectory() {
		if (!getMyApplication().getResourceManager().getIndexFileNames().isEmpty()) {
			showDialogOfFreeDownloadsIfNeeded();
		}
	}

	private void showDialogOfFreeDownloadsIfNeeded() {
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
			msg.setPositiveButton(R.string.shared_string_ok, null);
			msg.show();
		}
	}


	public boolean isInQueue(IndexItem item) {
		return downloadQueue.contains(item);
	}

	public void removeFromQueue(IndexItem item) {
		downloadQueue.remove(item);
	}
}

