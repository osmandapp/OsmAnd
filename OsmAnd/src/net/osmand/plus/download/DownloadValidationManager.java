package net.osmand.plus.download;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;

import net.osmand.map.WorldRegion;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.download.DownloadIndexesThread.DownloadEvents;
import net.osmand.plus.download.ui.FreeRegionDialogs;
import net.osmand.plus.settings.backend.OsmandSettings;

import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class DownloadValidationManager {

	public static final int MAXIMUM_AVAILABLE_FREE_DOWNLOADS = 7;
	private static boolean DOWNLOAD_MOBILE_INTERNET_CONFIRMED;

	private final OsmandApplication app;
	private final OsmandSettings settings;
	private final DownloadIndexesThread downloadThread;

	private interface DownloadValidatedCallback {
		void onDownloadValidated(@NonNull IndexItem[] items);
	}

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
			downloadFilesWithAllChecks(context, items, downloadThread::runDownloadFiles);
		}
	}

	public void startReplacementDownload(@NonNull FragmentActivity context, @NonNull IndexItem item,
	                                     @NonNull File fileToDelete) {
		downloadFilesWithAllChecks(context, new IndexItem[] {item}, validatedItems ->
				downloadThread.runDownloadFileWithPendingCleanup(
						validatedItems[0], Collections.singletonList(fileToDelete)));
	}

	private void copyVoiceAssetsWithoutInternet(@NonNull FragmentActivity activity, IndexItem... items) {
		if (downloadFilesCheck_1_FreeVersion(activity, items)) {
			downloadFilesCheck_3_ValidateSpace(activity, items, downloadThread::runDownloadFiles);
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

	private void downloadFilesWithAllChecks(@NonNull FragmentActivity activity, @NonNull IndexItem[] items,
	                                        @NonNull DownloadValidatedCallback callback) {
		IndexItem[] freeItems = downloadFilesCheck_1_FreeRegion(activity, items, callback);
		if (freeItems != null) {
			downloadFilesCheck_2_Internet(activity, freeItems, callback);
		}
	}

	private boolean downloadFilesCheck_1_FreeVersion(@NonNull FragmentActivity context, IndexItem... items) {
		return downloadFilesCheck_1_FreeRegion(context, items, null) != null;
	}

	/**
	 * Free version: new maps of one region only. The first map chooses the region without a dialog
	 * (it is stored when the download starts), updates of maps on the device always go through.
	 *
	 * @return the items to download now, or null when a dialog takes over (replace the free region,
	 * get Maps+) - it restarts the download itself.
	 */
	@Nullable
	private IndexItem[] downloadFilesCheck_1_FreeRegion(@NonNull FragmentActivity activity, @NonNull IndexItem[] items,
	                                                    @Nullable DownloadValidatedCallback callback) {
		FreeRegionHelper helper = new FreeRegionHelper(app);
		if (!helper.isFreeVersion()) {
			return items;
		}
		WorldRegion freeRegion = helper.getFreeRegion();
		List<IndexItem> free = new ArrayList<>();
		Set<WorldRegion> otherRegions = new LinkedHashSet<>();
		for (IndexItem item : items) {
			WorldRegion region = helper.getRegion(item);
			if (freeRegion == null && region != null) {
				freeRegion = region;
			}
			if (helper.isFree(item, freeRegion)) {
				free.add(item);
			} else {
				otherRegions.add(region);
			}
		}
		if (otherRegions.isEmpty()) {
			return items;
		}
		if (!free.isEmpty() || callback == null) {
			List<String> names = new ArrayList<>();
			for (WorldRegion region : otherRegions) {
				names.add(region.getLocaleName());
			}
			app.showToastMessage(R.string.free_region_other_regions_need_maps_plus, TextUtils.join(", ", names));
			return free.isEmpty() ? null : free.toArray(new IndexItem[0]);
		}
		FreeRegionDialogs.showOtherRegion(activity, freeRegion, otherRegions.iterator().next(),
				() -> downloadFilesWithAllChecks(activity, items, callback));
		return null;
	}

	private void downloadFilesCheck_2_Internet(@NonNull FragmentActivity context, @NonNull IndexItem[] items,
	                                           @NonNull DownloadValidatedCallback callback) {
		if (settings.isWifiConnected()) {
			downloadFilesCheck_3_ValidateSpace(context, items, callback);
		} else if (settings.isInternetConnectionAvailable()) {
			if (DOWNLOAD_MOBILE_INTERNET_CONFIRMED) {
				downloadFilesCheck_3_ValidateSpace(context, items, callback);
			} else {
				AlertDialog.Builder builder = new AlertDialog.Builder(context);
				builder.setMessage(context.getString(R.string.download_using_mobile_internet));
				builder.setPositiveButton(R.string.shared_string_yes, (dialog, which) -> {
					DOWNLOAD_MOBILE_INTERNET_CONFIRMED = true;
					downloadFilesCheck_3_ValidateSpace(context, items, callback);
				});
				builder.setNegativeButton(R.string.shared_string_no, null);
				builder.show();
			}
		} else {
			app.showToastMessage(R.string.no_index_file_to_download);
		}
	}

	private void downloadFilesCheck_3_ValidateSpace(@NonNull FragmentActivity context, @NonNull IndexItem[] items,
	                                                @NonNull DownloadValidatedCallback callback) {
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
					downloadFileCheckFinalRun(context, items, callback));
			builder.setNegativeButton(R.string.shared_string_no, null);
			builder.show();
		} else {
			downloadFileCheckFinalRun(context, items, callback);
		}
	}

	private void downloadFileCheckFinalRun(@NonNull FragmentActivity context, @NonNull IndexItem[] items,
	                                       @NonNull DownloadValidatedCallback callback) {
		callback.onDownloadValidated(items);
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
