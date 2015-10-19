package net.osmand.plus.download;

import java.lang.ref.WeakReference;
import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Set;

import net.osmand.access.AccessibleToast;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.activities.ActionBarProgressActivity;
import net.osmand.plus.download.DownloadIndexesThread.DownloadEvents;
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

public class BaseDownloadActivity extends ActionBarProgressActivity implements DownloadEvents {
	protected OsmandSettings settings;
	private static DownloadIndexesThread downloadListIndexThread;
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

	
	public DownloadIndexesThread getDownloadThread() {
		return downloadListIndexThread;
	}
	
	public void startDownload(IndexItem... items) {
		downloadFilesWithAllChecks(items);
	}


	@UiThread
	public void downloadInProgress() {
	}

	@UiThread
	public void downloadHasFinished() {
	}
	
	@UiThread
	public void newDownloadIndexes() {
	}
	

	public OsmandApplication getMyApplication() {
		return (OsmandApplication) getApplication();
	}

	public void downloadFilesCheck_3_ValidateSpace(final IndexItem... items) {
		long szLong = 0;
		int i = 0;
		for (IndexItem es : downloadListIndexThread.getCurrentDownloadingItems()) {
			szLong += es.contentSize;
			i++;
		}
		for (IndexItem es : items) {
			szLong += es.contentSize;
			i++;
		}
		double sz = ((double) szLong) / (1 << 20);
		// get availabile space
		double asz = downloadListIndexThread.getAvailableSpace();
		if (asz != -1 && asz > 0 && sz / asz > 0.4) {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage(MessageFormat.format(getString(R.string.download_files_question_space), i, sz, asz));
			builder.setPositiveButton(R.string.shared_string_yes, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					downloadFileCheck_Final_Run(items);
				}
			});
			builder.setNegativeButton(R.string.shared_string_no, null);
			builder.show();
		} else {
			downloadFileCheck_Final_Run(items);
		}
	}
	
	private void downloadFileCheck_Final_Run(IndexItem[] items) {
		downloadListIndexThread.runDownloadFiles(items);
		downloadInProgress();
	}
	
	
	
	protected void downloadFilesWithAllChecks(IndexItem[] items) {
		downloadFilesCheck_1_FreeVersion(items);
	}

	protected void downloadFilesCheck_1_FreeVersion(IndexItem[] items) {
		if (Version.isFreeVersion(getMyApplication())) {
			int total = settings.NUMBER_OF_FREE_DOWNLOADS.get();
			if (total > MAXIMUM_AVAILABLE_FREE_DOWNLOADS) {
				new InstallPaidVersionDialogFragment()
						.show(getSupportFragmentManager(), InstallPaidVersionDialogFragment.TAG);
			} else {
				downloadFilesCheck_2_Internet(items);
			}
		} else {
			downloadFilesCheck_2_Internet(items);
		}
	}

	protected void downloadFilesCheck_2_Internet(IndexItem[] items) {
		if (!getMyApplication().getSettings().isWifiConnected()) {
			if (getMyApplication().getSettings().isInternetConnectionAvailable()) {
				new ConfirmDownloadDialogFragment().show(getSupportFragmentManager(),
						ConfirmDownloadDialogFragment.TAG);
			} else {
				AccessibleToast.makeText(this, R.string.no_index_file_to_download, Toast.LENGTH_LONG).show();
			}
		} else {
			downloadFilesCheck_3_ValidateSpace(items);
		}
	}

	@Override
	public void onAttachFragment(Fragment fragment) {
		fragSet.add(new WeakReference<Fragment>(fragment));
	}

	public void makeSureUserCancelDownload(final IndexItem item) {
		AlertDialog.Builder bld = new AlertDialog.Builder(this);
		bld.setTitle(getString(R.string.shared_string_cancel));
		bld.setMessage(R.string.confirm_interrupt_download);
		bld.setPositiveButton(R.string.shared_string_yes, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
				getDownloadThread().cancelDownload(item);
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
					((BaseDownloadActivity) getActivity()).downloadFilesCheck_3_ValidateSpace();
				}
			});
			builder.setNegativeButton(R.string.shared_string_no, null);
			return builder.create();
		}
	}
}

