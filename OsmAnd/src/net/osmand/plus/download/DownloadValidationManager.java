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
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.UiThread;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.widget.Toast;

public class DownloadValidationManager {
	public static final int MAXIMUM_AVAILABLE_FREE_DOWNLOADS = 5;
	protected OsmandSettings settings;
	private OsmandApplication app;
	private DownloadIndexesThread downloadThread;

	public DownloadValidationManager(OsmandApplication app) {
		this.app = app;
		settings = app.getSettings();
		downloadThread = app.getDownloadThread();
	}

	
	public DownloadIndexesThread getDownloadThread() {
		return downloadThread;
	}
	
	public void startDownload(FragmentActivity activity, IndexItem... items) {
		downloadFilesWithAllChecks(activity, items);
	}

	public OsmandApplication getMyApplication() {
		return app;
	}

	public void downloadFilesCheck_3_ValidateSpace(final FragmentActivity activity, final IndexItem... items) {
		long szLong = 0;
		int i = 0;
		for (IndexItem es : downloadThread.getCurrentDownloadingItems()) {
			szLong += es.contentSize;
			i++;
		}
		for (IndexItem es : items) {
			szLong += es.contentSize;
			i++;
		}
		double sz = ((double) szLong) / (1 << 20);
		// get availabile space
		double asz = downloadThread.getAvailableSpace();
		if (asz != -1 && asz > 0 && sz / asz > 0.4) {
			AlertDialog.Builder builder = new AlertDialog.Builder(activity);
			builder.setMessage(MessageFormat.format(activity.getString(R.string.download_files_question_space), i, sz, asz));
			builder.setPositiveButton(R.string.shared_string_yes, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					downloadFileCheck_Final_Run(activity, items);
				}
			});
			builder.setNegativeButton(R.string.shared_string_no, null);
			builder.show();
		} else {
			downloadFileCheck_Final_Run(activity, items);
		}
	}
	
	private void downloadFileCheck_Final_Run(FragmentActivity activity, IndexItem[] items) {
		downloadThread.runDownloadFiles(items);
		if(activity instanceof DownloadEvents) {
			((DownloadEvents) activity).downloadInProgress();
		}
	}
	
	
	
	protected void downloadFilesWithAllChecks(FragmentActivity activity, IndexItem[] items) {
		downloadFilesCheck_1_FreeVersion(activity, items);
	}

	protected void downloadFilesCheck_1_FreeVersion(FragmentActivity activity, IndexItem[] items) {
		if (Version.isFreeVersion(getMyApplication())) {
			int total = settings.NUMBER_OF_FREE_DOWNLOADS.get();
			if (total > MAXIMUM_AVAILABLE_FREE_DOWNLOADS) {
				new InstallPaidVersionDialogFragment()
						.show(activity.getSupportFragmentManager(), InstallPaidVersionDialogFragment.TAG);
			} else {
				downloadFilesCheck_2_Internet(activity, items);
			}
		} else {
			downloadFilesCheck_2_Internet(activity, items);
		}
	}

	protected void downloadFilesCheck_2_Internet(final FragmentActivity activity, final IndexItem[] items) {
		if (!getMyApplication().getSettings().isWifiConnected()) {
			if (getMyApplication().getSettings().isInternetConnectionAvailable()) {
				AlertDialog.Builder builder = new AlertDialog.Builder(activity);
				builder.setMessage(activity.getString(R.string.download_using_mobile_internet));
				builder.setPositiveButton(R.string.shared_string_yes, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						downloadFilesCheck_3_ValidateSpace(activity, items);
					}
				});
				builder.setNegativeButton(R.string.shared_string_no, null);
				builder.show();
			} else {
				AccessibleToast.makeText(activity, R.string.no_index_file_to_download, Toast.LENGTH_LONG).show();
			}
		} else {
			downloadFilesCheck_3_ValidateSpace(activity, items);
		}
	}

	

	public void makeSureUserCancelDownload(Context ctx, final IndexItem item) {
		AlertDialog.Builder bld = new AlertDialog.Builder(ctx);
		bld.setTitle(ctx.getString(R.string.shared_string_cancel));
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

}

