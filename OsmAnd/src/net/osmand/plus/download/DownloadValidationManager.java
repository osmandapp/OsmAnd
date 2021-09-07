package net.osmand.plus.download;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import net.osmand.AndroidUtils;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.download.DownloadIndexesThread.DownloadEvents;
import net.osmand.plus.settings.backend.OsmandSettings;

import java.io.File;
import java.text.MessageFormat;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;

public class DownloadValidationManager {

	public static final int MAXIMUM_AVAILABLE_FREE_DOWNLOADS = 7;

	private final OsmandApplication app;
	private final OsmandSettings settings;
	private final DownloadIndexesThread downloadThread;

	public DownloadValidationManager(OsmandApplication app) {
		this.app = app;
		this.settings = app.getSettings();
		this.downloadThread = app.getDownloadThread();
	}

	public boolean isSpaceEnoughForDownload(final FragmentActivity context, final boolean showAlert,
	                                        final IndexItem... items) {
		long szChangeLong = 0;
		long szMaxTempLong = 0;
		int i = 0;
		for (IndexItem es : downloadThread.getCurrentDownloadingItems()) {
			final long szExistingLong = getExistingFileSize(es.getTargetFile(app));
			long change = es.contentSize - szExistingLong;
			szChangeLong += change;
			if (szExistingLong > szMaxTempLong) szMaxTempLong = szExistingLong;
			i++;
		}
		for (IndexItem es : items) {
			if (es != null) {
				final long szExistingLong = getExistingFileSize(es.getTargetFile(app));
				long change = es.contentSize - szExistingLong;
				szChangeLong += change;
				if (szExistingLong > szMaxTempLong) szMaxTempLong = szExistingLong;
				i++;
			}
		}
		double szChange = ((double) szChangeLong) / (1 << 20);
		double szMaxTemp = szChange + ((double) szMaxTempLong) / (1 << 20);

		// get availabile space
		double asz = downloadThread.getAvailableSpace();
		if (asz != -1 && asz > 0 && (szMaxTemp > asz)) {
			if (showAlert) {
				AlertDialog.Builder builder = new AlertDialog.Builder(context);
				String message = app.getString(R.string.download_files_error_not_enough_space);
				builder.setMessage(MessageFormat.format(message, i, szChange, asz, szMaxTemp));
				builder.setNegativeButton(R.string.shared_string_ok, null);
				builder.show();
			}
			return false;
		} else {
			return true;
		}
	}

	private long getExistingFileSize(File file) {
		if (file != null) {
			if (file.canRead()) {
				return file.length();
			}
		}
		return 0;
	}

	public void startDownload(@NonNull FragmentActivity context, IndexItem... items) {
		if (downloadFilesCheck_1_FreeVersion(context)) {
			downloadFilesCheck_2_Internet(context, items);
		}
	}

	public void copyAssetsWithoutInternet(@NonNull FragmentActivity activity, IndexItem... items) {
		if (downloadFilesCheck_1_FreeVersion(activity)) {
			downloadFilesCheck_3_ValidateSpace(activity, items);
		}
	}

	private boolean downloadFilesCheck_1_FreeVersion(@NonNull FragmentActivity context) {
		if (!Version.isPaidVersion(app)) {
			int total = settings.NUMBER_OF_FREE_DOWNLOADS.get();
			if (total > MAXIMUM_AVAILABLE_FREE_DOWNLOADS) {
				new InstallPaidVersionDialogFragment()
						.show(context.getSupportFragmentManager(), InstallPaidVersionDialogFragment.TAG);
				return false;
			}
		}
		return true;
	}

	private void downloadFilesCheck_2_Internet(@NonNull final FragmentActivity context, final IndexItem[] items) {
		if (!settings.isWifiConnected()) {
			if (settings.isInternetConnectionAvailable()) {
				AlertDialog.Builder builder = new AlertDialog.Builder(context);
				builder.setMessage(context.getString(R.string.download_using_mobile_internet));
				builder.setPositiveButton(R.string.shared_string_yes, (dialog, which) -> downloadFilesCheck_3_ValidateSpace(context, items));
				builder.setNegativeButton(R.string.shared_string_no, null);
				builder.show();
			} else {
				Toast.makeText(context, R.string.no_index_file_to_download, Toast.LENGTH_LONG).show();
			}
		} else {
			downloadFilesCheck_3_ValidateSpace(context, items);
		}
	}

	private void downloadFilesCheck_3_ValidateSpace(@NonNull final FragmentActivity context, final IndexItem... items) {
		long szChangeLong = 0;
		long szMaxTempLong = 0;
		int i = 0;
		for (IndexItem es : downloadThread.getCurrentDownloadingItems()) {
			final long szExistingLong = getExistingFileSize(es.getTargetFile(app));
			long change = es.contentSize - szExistingLong;
			szChangeLong += change;
			if (szExistingLong > szMaxTempLong) szMaxTempLong = szExistingLong;
			i++;
		}
		for (IndexItem es : items) {
			final long szExistingLong = getExistingFileSize(es.getTargetFile(app));
			long change = es.contentSize - szExistingLong;
			szChangeLong += change;
			if (szExistingLong > szMaxTempLong) szMaxTempLong = szExistingLong;
			i++;
		}
		double szChange = ((double) szChangeLong) / (1 << 20);
		double szMaxTemp = szChange + ((double) szMaxTempLong) / (1 << 20);

		// get available space
		double asz = downloadThread.getAvailableSpace();
		if (asz != -1 && asz > 0 && (szMaxTemp > asz)) {
			AlertDialog.Builder builder = new AlertDialog.Builder(context);
			String message = app.getString(R.string.download_files_error_not_enough_space);
			builder.setMessage(MessageFormat.format(message, i, szChange, asz, szMaxTemp));
			builder.setNegativeButton(R.string.shared_string_ok, null);
			builder.show();
		} else if (asz != -1 && asz > 0 && ((szChange + 10 > asz) || (szMaxTemp + 10 > asz))) {
			AlertDialog.Builder builder = new AlertDialog.Builder(context);
			if (szChange != szMaxTemp) {
				String message = app.getString(R.string.download_files_question_space_with_temp);
				builder.setMessage(MessageFormat.format(message, i, szChange, asz, szMaxTemp));
			} else {
				String message = app.getString(R.string.download_files_question_space);
				builder.setMessage(MessageFormat.format(message, i, szChange, asz));
			}
			builder.setPositiveButton(R.string.shared_string_yes, (dialog, which) ->
					downloadFileCheck_Final_Run(context, items));
			builder.setNegativeButton(R.string.shared_string_no, null);
			builder.show();
		} else {
			downloadFileCheck_Final_Run(context, items);
		}
	}

	private void downloadFileCheck_Final_Run(@NonNull FragmentActivity context, IndexItem[] items) {
		downloadThread.runDownloadFiles(items);
		if (context instanceof DownloadEvents) {
			((DownloadEvents) context).downloadInProgress();
		}
	}

	public void makeSureUserCancelDownload(FragmentActivity ctx, final DownloadItem item) {
		AlertDialog.Builder bld = new AlertDialog.Builder(ctx);
		bld.setTitle(ctx.getString(R.string.shared_string_cancel));
		bld.setMessage(R.string.confirm_interrupt_download);
		bld.setPositiveButton(R.string.shared_string_yes, (dialog, which) -> {
			dialog.dismiss();
			downloadThread.cancelDownload(item);
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
			AlertDialog.Builder msg = new AlertDialog.Builder(requireActivity());
			msg.setTitle(R.string.free_version_title);
			msg.setMessage(msgTx);
			if (Version.isMarketEnabled()) {
				msg.setPositiveButton(R.string.install_paid, (dialog, which) -> {
					Activity activity = getActivity();
					if (activity != null) {
						Uri uri = Uri.parse(Version.getUrlWithUtmRef(getMyApplication(), "net.osmand.plus"));
						Intent intent = new Intent(Intent.ACTION_VIEW, uri);
						AndroidUtils.startActivityIfSafe(activity, intent);
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