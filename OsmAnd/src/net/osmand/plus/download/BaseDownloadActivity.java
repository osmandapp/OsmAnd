package net.osmand.plus.download;

import java.lang.ref.WeakReference;
import java.text.MessageFormat;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import net.osmand.access.AccessibleToast;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.WorldRegion;
import net.osmand.plus.activities.ActionBarProgressActivity;
import net.osmand.plus.download.items.ItemsListBuilder;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.UiThread;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.widget.Toast;

public class BaseDownloadActivity extends ActionBarProgressActivity {
	protected OsmandSettings settings;
	public static DownloadIndexesThread downloadListIndexThread;
	protected Set<WeakReference<Fragment>> fragSet = new HashSet<>();
	public static final int MAXIMUM_AVAILABLE_FREE_DOWNLOADS = 10;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		settings = ((OsmandApplication) getApplication()).getSettings();
		if (downloadListIndexThread == null) {
			downloadListIndexThread = new DownloadIndexesThread(this);
		}
		super.onCreate(savedInstanceState);
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

	
	public void cancelDownload(IndexItem item) {
		// TODO Auto-generated method stub
		FIXME;
	}


	// FIXME
	public void onCategorizationFinished() {
	}

	@UiThread
	public void updateDownloadList() {
	}

	@UiThread
	public void updateProgress(boolean updateOnlyProgress, Object tag) {
	}

	public void downloadedIndexes() {
	}

	public void updateFragments() {
	}

	public void downloadListUpdated() {
	}
	/////// FIXME	


	public OsmandApplication getMyApplication() {
		return (OsmandApplication) getApplication();
	}

	public ItemsListBuilder getItemsBuilder() {
		return getItemsBuilder("", false);
	}

	public ItemsListBuilder getVoicePromptsBuilder() {
		return getItemsBuilder("", true);
	}

	public ItemsListBuilder getItemsBuilder(String regionId, boolean voicePromptsOnly) {
		if (downloadListIndexThread.getResourcesByRegions().size() > 0) {
			ItemsListBuilder builder = new ItemsListBuilder(getMyApplication(), regionId, downloadListIndexThread.getResourcesByRegions(),
					downloadListIndexThread.getVoiceRecItems(), downloadListIndexThread.getVoiceTTSItems());
			if (!voicePromptsOnly) {
				return builder.build();
			} else {
				return builder;
			}
		} else {
			return null;
		}
	}

	public List<IndexItem> getIndexItemsByRegion(WorldRegion region) {
		if (downloadListIndexThread.getResourcesByRegions().size() > 0) {
			return new LinkedList<>(downloadListIndexThread.getResourcesByRegions().get(region).values());
		} else {
			return new LinkedList<>();
		}
	}

	public boolean startDownload(IndexItem... items) {
		for(IndexItem i : items) {
			downloadListIndexThread.addToDownload(i);
		}
		// FIXME ??? commented line
//		if (downloadListIndexThread.getCurrentRunningTask() != null && getEntriesToDownload().get(item) == null) {
//			return false;
//		}
		downloadFilesWithAllChecks();
		updateFragments();
		return true;
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
	
	
	
	protected void downloadFilesWithAllChecks() {
		downloadFilesCheckFreeVersion();
	}

	protected void downloadFilesCheckFreeVersion() {
		if (Version.isFreeVersion(getMyApplication())) {
			int total = settings.NUMBER_OF_FREE_DOWNLOADS.get();
			if (total > MAXIMUM_AVAILABLE_FREE_DOWNLOADS) {
				new InstallPaidVersionDialogFragment()
						.show(getSupportFragmentManager(), InstallPaidVersionDialogFragment.TAG);
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
				new ConfirmDownloadDialogFragment().show(getSupportFragmentManager(),
						ConfirmDownloadDialogFragment.TAG);
			} else {
				AccessibleToast.makeText(this, R.string.no_index_file_to_download, Toast.LENGTH_LONG).show();
			}
		} else {
			downloadFilesPreCheckSpace();
		}
	}

	@Override
	public void onAttachFragment(Fragment fragment) {
		fragSet.add(new WeakReference<Fragment>(fragment));
	}

	public void makeSureUserCancelDownload(IndexItem item) {
		TODO;
		AlertDialog.Builder bld = new AlertDialog.Builder(this);
		bld.setTitle(getString(R.string.shared_string_cancel));
		bld.setMessage(R.string.confirm_interrupt_download);
		bld.setPositiveButton(R.string.shared_string_yes, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
				cancelDownload.run();
			}
		});
		bld.setNegativeButton(R.string.shared_string_no, null);
		bld.show();
	}


	public static class InstallPaidVersionDialogFragment extends DialogFragment {
		public static final String TAG = "InstallPaidVersionDialogFragment";
		@NonNull
		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			String msgTx = getString(R.string.free_version_message, MAXIMUM_AVAILABLE_FREE_DOWNLOADS + "");
			AlertDialog.Builder msg = new AlertDialog.Builder(getActivity());
			msg.setTitle(R.string.free_version_title);
			msg.setMessage(msgTx);
			if (Version.isMarketEnabled(getMyApplication())) {
				msg.setPositiveButton(R.string.install_paid, new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						Intent intent = new Intent(Intent.ACTION_VIEW,
								Uri.parse(Version.marketPrefix(getMyApplication())
										+ "net.osmand.plus"));
						try {
							startActivity(intent);
						} catch (ActivityNotFoundException e) {
						}
					}
				});
				msg.setNegativeButton(R.string.shared_string_cancel, null);
			} else {
				msg.setNeutralButton(R.string.shared_string_ok, null);
			}
			return msg.create();
		}

		private OsmandApplication getMyApplication() {
			return (OsmandApplication) getActivity().getApplication();
		}
	}

	public static class ConfirmDownloadDialogFragment extends DialogFragment {
		public static final String TAG = "ConfirmDownloadDialogFragment";
		@NonNull
		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			builder.setMessage(getString(R.string.download_using_mobile_internet));
			builder.setPositiveButton(R.string.shared_string_yes, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					((BaseDownloadActivity) getActivity()).downloadFilesPreCheckSpace();
				}
			});
			builder.setNegativeButton(R.string.shared_string_no, null);
			return builder.create();
		}
	}
}

