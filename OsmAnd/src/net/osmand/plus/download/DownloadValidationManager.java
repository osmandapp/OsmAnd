package net.osmand.plus.download;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.chooseplan.ChoosePlanFragment;
import net.osmand.plus.chooseplan.OsmAndFeature;
import net.osmand.plus.download.DownloadIndexesThread.DownloadEvents;
import net.osmand.plus.settings.backend.OsmandSettings;

import java.text.MessageFormat;

public class DownloadValidationManager {

	public static final int MAXIMUM_AVAILABLE_FREE_DOWNLOADS = 7;
	private static boolean DOWNLOAD_MOBILE_INTERNET_CONFIRMED;

	private final OsmandApplication app;
	private final OsmandSettings settings;
	private final DownloadIndexesThread downloadThread;

	public DownloadValidationManager(@NonNull OsmandApplication app) {
		this.app = app;
		this.settings = app.getSettings();
		this.downloadThread = app.getDownloadThread();
	}

	public boolean isSpaceEnoughForDownload(@NonNull FragmentActivity context, boolean showAlert,
	                                        IndexItem... items) {
		long szChangeLong = 0;
		long szMaxTempLong = 0;
		int i = 0;
		for (IndexItem es : downloadThread.getCurrentDownloadingItems()) {
			long szExistingLong = es.getExistingFileSize(app);
			long change = es.contentSize - szExistingLong;
			szChangeLong += change;
			if (szExistingLong > szMaxTempLong) szMaxTempLong = szExistingLong;
			i++;
		}
		for (IndexItem es : items) {
			if (es != null) {
				long szExistingLong = es.getExistingFileSize(app);
				long change = es.contentSize - szExistingLong;
				szChangeLong += change;
				if (szExistingLong > szMaxTempLong) szMaxTempLong = szExistingLong;
				i++;
			}
		}
		double szChange = ((double) szChangeLong) / (1 << 20);
		double szMaxTemp = szChange + ((double) szMaxTempLong) / (1 << 20);

		double availableSpace = downloadThread.getAvailableSpace();
		if (availableSpace > 0 && (szMaxTemp > availableSpace)) {
			if (showAlert) {
				String message = app.getString(R.string.download_files_error_not_enough_space);
				AlertDialog.Builder builder = new AlertDialog.Builder(context);
				builder.setMessage(MessageFormat.format(message, i, szChange, availableSpace, szMaxTemp));
				builder.setNegativeButton(R.string.shared_string_ok, null);
				builder.show();
			}
			return false;
		}
		return true;
	}

	public void startDownload(@NonNull FragmentActivity context, @NonNull IndexItem... items) {
		if (isAllAssets(items)) {
			copyVoiceAssetsWithoutInternet(context, items);
		} else {
			downloadFilesWithAllChecks(context, items);
		}
	}

	private void copyVoiceAssetsWithoutInternet(@NonNull FragmentActivity activity, IndexItem... items) {
		if (downloadFilesCheck_1_FreeVersion(activity, items)) {
			downloadFilesCheck_3_ValidateSpace(activity, items);
		}
	}

	private boolean isAllAssets(@NonNull IndexItem... items) {
		for (IndexItem index : items) {
			if (!DownloadActivityType.isVoiceTTS(index)) {
				return false;
			}
		}
		return true;
	}

	private void downloadFilesWithAllChecks(@NonNull FragmentActivity activity, IndexItem... items) {
		if (downloadFilesCheck_1_FreeVersion(activity, items)) {
			downloadFilesCheck_2_Internet(activity, items);
		}
	}

	private boolean downloadFilesCheck_1_FreeVersion(@NonNull FragmentActivity context, IndexItem... items) {
		if (!Version.isPaidVersion(app) && shouldShowChoosePlan(items)) {
			ChoosePlanFragment.showInstance(context, OsmAndFeature.UNLIMITED_MAP_DOWNLOADS);
			return false;
		}
		return true;
	}

	private boolean shouldShowChoosePlan(IndexItem... items) {
		boolean isAnyItemCountedInDownload = false;
		for (IndexItem indexItem : items) {
			if (DownloadActivityType.isCountedInDownloads(indexItem)) {
				isAnyItemCountedInDownload = true;
				break;
			}
		}
		int total = settings.NUMBER_OF_FREE_DOWNLOADS.get();
		return total >= MAXIMUM_AVAILABLE_FREE_DOWNLOADS && isAnyItemCountedInDownload;
	}

	private void downloadFilesCheck_2_Internet(@NonNull FragmentActivity context, IndexItem[] items) {
		if (settings.isWifiConnected()) {
			downloadFilesCheck_3_ValidateSpace(context, items);
		} else if (settings.isInternetConnectionAvailable()) {
			if (DOWNLOAD_MOBILE_INTERNET_CONFIRMED) {
				downloadFilesCheck_3_ValidateSpace(context, items);
			} else {
				AlertDialog.Builder builder = new AlertDialog.Builder(context);
				builder.setMessage(context.getString(R.string.download_using_mobile_internet));
				builder.setPositiveButton(R.string.shared_string_yes, (dialog, which) -> {
					DOWNLOAD_MOBILE_INTERNET_CONFIRMED = true;
					downloadFilesCheck_3_ValidateSpace(context, items);
				});
				builder.setNegativeButton(R.string.shared_string_no, null);
				builder.show();
			}
		} else {
			app.showToastMessage(R.string.no_index_file_to_download);
		}
	}

	private void downloadFilesCheck_3_ValidateSpace(@NonNull FragmentActivity context, IndexItem... items) {
		long szChangeLong = 0;
		long szMaxTempLong = 0;
		int i = 0;
		for (IndexItem es : downloadThread.getCurrentDownloadingItems()) {
			long szExistingLong = es.getExistingFileSize(app);
			long change = es.contentSize - szExistingLong;
			szChangeLong += change;
			if (szExistingLong > szMaxTempLong) szMaxTempLong = szExistingLong;
			i++;
		}
		for (IndexItem es : items) {
			long szExistingLong = es.getExistingFileSize(app);
			long change = es.contentSize - szExistingLong;
			szChangeLong += change;
			if (szExistingLong > szMaxTempLong) szMaxTempLong = szExistingLong;
			i++;
		}
		double szChange = ((double) szChangeLong) / (1 << 20);
		double szMaxTemp = szChange + ((double) szMaxTempLong) / (1 << 20);

		// get available space
		double availableSpace = downloadThread.getAvailableSpace();
		if (availableSpace > 0 && (szMaxTemp > availableSpace)) {
			AlertDialog.Builder builder = new AlertDialog.Builder(context);
			String message = app.getString(R.string.download_files_error_not_enough_space);
			builder.setMessage(MessageFormat.format(message, i, szChange, availableSpace, szMaxTemp));
			builder.setNegativeButton(R.string.shared_string_ok, null);
			builder.show();
		} else if (availableSpace > 0 && ((szChange + 10 > availableSpace) || (szMaxTemp + 10 > availableSpace))) {
			AlertDialog.Builder builder = new AlertDialog.Builder(context);
			if (szChange != szMaxTemp) {
				String message = app.getString(R.string.download_files_question_space_with_temp);
				builder.setMessage(MessageFormat.format(message, i, szChange, availableSpace, szMaxTemp));
			} else {
				String message = app.getString(R.string.download_files_question_space);
				builder.setMessage(MessageFormat.format(message, i, szChange, availableSpace));
			}
			builder.setPositiveButton(R.string.shared_string_yes, (dialog, which) ->
					downloadFileCheckFinalRun(context, items));
			builder.setNegativeButton(R.string.shared_string_no, null);
			builder.show();
		} else {
			downloadFileCheckFinalRun(context, items);
		}
	}

	private void downloadFileCheckFinalRun(@NonNull FragmentActivity context, IndexItem[] items) {
		downloadThread.runDownloadFiles(items);
		if (context instanceof DownloadEvents) {
			((DownloadEvents) context).downloadInProgress();
		}
	}

	public void makeSureUserCancelDownload(@NonNull FragmentActivity ctx, DownloadItem item) {
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

	@NonNull
	public static String getFreeVersionMessage(@NonNull Context context) {
		return context.getString(R.string.free_version_message, String.valueOf(MAXIMUM_AVAILABLE_FREE_DOWNLOADS));
	}
}