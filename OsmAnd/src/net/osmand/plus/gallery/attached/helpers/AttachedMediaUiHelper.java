package net.osmand.plus.gallery.attached.helpers;

import static android.app.Activity.RESULT_OK;
import static net.osmand.shared.media.MediaFileNameFormat.IMG_EXTENSION;
import static net.osmand.shared.media.MediaFileNameFormat.MPEG4_EXTENSION;
import static net.osmand.shared.media.MediaFileNameFormat.THREEGP_EXTENSION;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.SystemClock;
import android.view.View;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.activity.result.contract.ActivityResultContracts.PickMultipleVisualMedia;
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia;
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult;
import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import net.osmand.CallbackWithObject;
import net.osmand.PlatformUtil;
import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.plus.OsmAndTaskManager;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.IntentHelper;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.audionotes.AVActionType;
import net.osmand.plus.plugins.audionotes.AudioVideoNotesPlugin;
import net.osmand.plus.plugins.audionotes.Recording;
import net.osmand.plus.settings.enums.ThemeUsageContext;
import net.osmand.plus.settings.mediastorage.MediaDirType;
import net.osmand.plus.settings.mediastorage.MediaStorageHelper;
import net.osmand.plus.settings.mediastorage.MediaStorageLocation;
import net.osmand.plus.settings.mediastorage.MediaStorageUtils;
import net.osmand.plus.settings.mediastorage.MediaTarget;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.popup.PopUpMenu;
import net.osmand.plus.widgets.popup.PopUpMenuDisplayData;
import net.osmand.plus.widgets.popup.PopUpMenuItem;
import net.osmand.plus.widgets.popup.PopUpMenuWidthMode;
import net.osmand.shared.gpx.primitives.Link;
import net.osmand.shared.gpx.primitives.Linkable;
import net.osmand.shared.gpx.primitives.WptPt;
import net.osmand.shared.media.MediaFileNameFormat;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AttachedMediaUiHelper {

	private static final Log LOG = PlatformUtil.getLog(AttachedMediaUiHelper.class);
	private static final String ADD_MEDIA_PICKER_KEY = "attached_media_picker_";
	private static final String[] MEDIA_MIME_TYPES = {"image/*", "video/*", "audio/*"};

	private final OsmandApplication app;
	private final MapActivity mapActivity;
	private final UiUtilities iconsCache;
	private final AttachedMediaDataHelper dataHelper;
	private final MediaStorageHelper mediaStorageHelper;
	@Nullable
	private ActivityResultLauncher<?> mediaPickerLauncher;

	public AttachedMediaUiHelper(@NonNull MapActivity mapActivity) {
		this.mapActivity = mapActivity;
		this.app = mapActivity.getApp();
		this.iconsCache = app.getUIUtilities();
		this.dataHelper = new AttachedMediaDataHelper(app);
		this.mediaStorageHelper = new MediaStorageHelper(app);
	}

	public void showAddMenu(@NonNull View anchorView, @NonNull Linkable target,
	                        @Nullable LatLon latLon, @Nullable Runnable onMediaChanged) {
		if (latLon == null || !canAttachMedia(target)) {
			return;
		}
		boolean nightMode = app.getDaynightHelper().isNightMode(ThemeUsageContext.OVER_MAP);
		int iconColor = ColorUtilities.getDefaultIconColor(app, nightMode);
		List<PopUpMenuItem> items = new ArrayList<>();
		items.add(createAddMenuItem(R.string.recording_context_menu_precord,
				R.drawable.ic_action_photo_dark, iconColor,
				() -> takeNote(AVActionType.REC_PHOTO, latLon, target, onMediaChanged), false));
		items.add(createAddMenuItem(R.string.recording_context_menu_vrecord,
				R.drawable.ic_action_video_dark, iconColor,
				() -> takeNote(AVActionType.REC_VIDEO, latLon, target, onMediaChanged), false));
		items.add(createAddMenuItem(R.string.recording_context_menu_arecord,
				R.drawable.ic_action_micro_dark, iconColor,
				() -> takeNote(AVActionType.REC_AUDIO, latLon, target, onMediaChanged), false));
		items.add(createAddMenuItem(R.string.choose_from_gallery,
				R.drawable.ic_action_photo_album, iconColor,
				() -> chooseFromGallery(target, latLon, onMediaChanged), true));
		items.add(createAddMenuItem(R.string.choose_from_files,
				R.drawable.ic_action_group_list, iconColor,
				() -> chooseFromFiles(target, latLon, onMediaChanged), false));

		PopUpMenuDisplayData data = new PopUpMenuDisplayData();
		data.anchorView = anchorView;
		data.menuItems = items;
		data.nightMode = nightMode;
		data.widthMode = PopUpMenuWidthMode.STANDARD;
		PopUpMenu.show(data);
	}

	@NonNull
	private PopUpMenuItem createAddMenuItem(@StringRes int titleId, @DrawableRes int iconId,
	                                        @ColorInt int iconColor, @NonNull Runnable action,
	                                        boolean showTopDivider) {
		return new PopUpMenuItem.Builder(app)
				.setTitleId(titleId)
				.setIcon(iconsCache.getPaintedIcon(iconId, iconColor))
				.setOnClickListener(item -> action.run())
				.showTopDivider(showTopDivider)
				.create();
	}

	private boolean canAttachMedia(@Nullable Linkable target) {
		return target instanceof FavouritePoint || target instanceof WptPt;
	}

	private void takeNote(@NonNull AVActionType type, @NonNull LatLon latLon,
	                      @NonNull Linkable target, @Nullable Runnable onMediaChanged) {
		AudioVideoNotesPlugin plugin = PluginsHelper.getPlugin(AudioVideoNotesPlugin.class);
		if (plugin != null && !plugin.isActive()) {
			PluginsHelper.enablePluginIfNeeded(mapActivity, app, plugin, true);
		}
		if (plugin != null && plugin.isActive()) {
			if (plugin.isRecording()) {
				plugin.stopRecording(mapActivity, true, true);
			} else {
				captureAttachedMedia(plugin, type, latLon, target, onMediaChanged);
			}
		}
	}

	private void captureAttachedMedia(@NonNull AudioVideoNotesPlugin plugin, @NonNull AVActionType type,
	                                  @NonNull LatLon latLon, @NonNull Linkable target, @Nullable Runnable onMediaChanged) {
		MediaDirType dirType = getMediaDirType(type);
		String extension = getMediaExtension(type);
		MediaStorageLocation storageLocation = MediaStorageLocation.fromSettings(app);
		String fileName = MediaFileNameFormat.createUniqueMediaFileName(latLon.getLatitude(), latLon.getLongitude(), extension,
				name -> mediaStorageHelper.mediaFileExists(storageLocation, dirType, name));
		String mimeType = MediaStorageUtils.getMimeType(null, fileName, dirType);
		MediaTarget mediaTarget = mediaStorageHelper.createTarget(storageLocation, dirType, fileName, mimeType);
		if (mediaTarget == null || mediaTarget.exists()) {
			logDebug("Attached media capture target unavailable: type=" + type + ", storage=" + storageLocation.getStorageType()
					+ ", fileName=" + fileName + ", target=" + mediaTarget);
			app.showToastMessage(R.string.media_storage_directory_not_writable);
			return;
		}
		File captureFile = getCaptureFile(mediaTarget);
		boolean deleteCaptureFile = mediaTarget.getFile() == null;
		logDebug("Attached media capture started: type=" + type + ", storage=" + storageLocation.getStorageType()
				+ ", fileName=" + mediaTarget.getFileName() + ", captureFile=" + captureFile + ", directTarget=" + !deleteCaptureFile);
		CallbackWithObject<File> callback = file -> {
			if (file != null) {
				logDebug("Attached media capture finished: type=" + type + ", file=" + file);
				saveCapturedMedia(file, mediaTarget, target, onMediaChanged, mimeType);
			} else {
				logDebug("Attached media capture cancelled: type=" + type + ", target=" + mediaTarget.getFileName());
				discardMediaTarget(mediaTarget);
				if (deleteCaptureFile) {
					Algorithms.removeAllFiles(captureFile);
				}
			}
			return true;
		};
		switch (type) {
			case REC_PHOTO ->
					plugin.takeAttachedPhoto(latLon.getLatitude(), latLon.getLongitude(), mapActivity, captureFile, callback);
			case REC_VIDEO ->
					plugin.recordAttachedVideo(latLon.getLatitude(), latLon.getLongitude(), mapActivity, captureFile, callback);
			case REC_AUDIO ->
					plugin.recordAttachedAudio(latLon.getLatitude(), latLon.getLongitude(), mapActivity, captureFile, callback);
		}
	}

	private void saveCapturedMedia(@NonNull File file, @NonNull MediaTarget mediaTarget, @NonNull Linkable target,
	                               @Nullable Runnable onMediaChanged, @NonNull String mimeType) {
		String name = new Recording(file).getName(app, false);
		if (isTargetFile(file, mediaTarget)) {
			logDebug("Attached media saved directly to target file: " + mediaTarget.getFileName());
			addCapturedMediaLink(file, mediaTarget, target, onMediaChanged, name, mimeType);
			return;
		}
		logDebug("Attached media save scheduled: source=" + file + ", target=" + mediaTarget.getFileName());
		OsmAndTaskManager.executeTask(new SaveCapturedMediaTask(app, file, mediaTarget, name, mimeType, link -> {
			logDebug("Attached media save completed: href=" + link.getHref());
			dataHelper.addMediaLinks(target, Collections.singletonList(link), onMediaChanged);
			return true;
		}));
	}

	private boolean isTargetFile(@NonNull File file, @NonNull MediaTarget target) {
		File targetFile = target.getFile();
		return targetFile != null && targetFile.equals(file);
	}

	private void addCapturedMediaLink(@NonNull File file, @NonNull MediaTarget mediaTarget,
	                                  @NonNull Linkable target, @Nullable Runnable onMediaChanged,
	                                  @Nullable String name, @NonNull String mimeType) {
		if (!file.exists() || file.length() == 0) {
			discardMediaTarget(mediaTarget);
			app.showToastMessage(app.getString(R.string.shared_string_io_error));
			return;
		}
		if (!finishMediaTarget(mediaTarget)) {
			return;
		}
		Link link = new Link(mediaTarget.getHref(), name, mimeType);
		logDebug("Attached media link created: href=" + link.getHref() + ", mimeType=" + mimeType);
		dataHelper.addMediaLinks(target, Collections.singletonList(link), onMediaChanged);
	}

	private boolean finishMediaTarget(@NonNull MediaTarget mediaTarget) {
		try {
			mediaTarget.finish(true);
			logDebug("Attached media target finished: " + mediaTarget.getFileName());
			return true;
		} catch (IOException e) {
			LOG.warn("Failed to finish attached media target: " + mediaTarget.getFileName(), e);
			app.showToastMessage(app.getString(R.string.shared_string_io_error));
			return false;
		}
	}

	private void discardMediaTarget(@NonNull MediaTarget mediaTarget) {
		try {
			mediaTarget.finish(false);
			logDebug("Attached media target discarded: " + mediaTarget.getFileName());
		} catch (IOException e) {
			LOG.warn("Failed to discard attached media target: " + mediaTarget.getFileName(), e);
		}
	}

	@NonNull
	private File getCaptureFile(@NonNull MediaTarget target) {
		File file = target.getFile();
		if (file != null) {
			return file;
		}
		File dir = new File(app.getCacheDir(), "attached_media");
		File captureFile = new File(dir, target.getFileName());
		Algorithms.removeAllFiles(captureFile);
		return captureFile;
	}

	@NonNull
	private MediaDirType getMediaDirType(@NonNull AVActionType type) {
		return switch (type) {
			case REC_AUDIO -> MediaDirType.AUDIO;
			case REC_VIDEO -> MediaDirType.VIDEO;
			case REC_PHOTO -> MediaDirType.PHOTO;
		};
	}

	@NonNull
	private String getMediaExtension(@NonNull AVActionType type) {
		return switch (type) {
			case REC_AUDIO -> THREEGP_EXTENSION;
			case REC_VIDEO -> MPEG4_EXTENSION;
			case REC_PHOTO -> IMG_EXTENSION;
		};
	}

	private void chooseFromGallery(@NonNull Linkable target, @NonNull LatLon latLon, @Nullable Runnable onMediaChanged) {
		PickVisualMediaRequest request = new PickVisualMediaRequest.Builder()
				.setMediaType(PickVisualMedia.ImageAndVideo.INSTANCE)
				.build();
		launchMediaPicker(new PickMultipleVisualMedia(), request, uris -> onMediaPicked(target, latLon, onMediaChanged, uris, Intent.FLAG_GRANT_READ_URI_PERMISSION));
	}

	private void chooseFromFiles(@NonNull Linkable target, @NonNull LatLon latLon, @Nullable Runnable onMediaChanged) {
		launchMediaPicker(new StartActivityForResult(), createOpenMediaDocumentIntent(), result -> {
			Intent data = result.getData();
			if (data != null && result.getResultCode() == RESULT_OK) {
				onMediaPicked(target, latLon, onMediaChanged, IntentHelper.getIntentUris(data), data.getFlags());
			}
		});
	}

	@NonNull
	private Intent createOpenMediaDocumentIntent() {
		Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
		intent.addCategory(Intent.CATEGORY_OPENABLE);
		intent.setType("*/*");
		intent.putExtra(Intent.EXTRA_MIME_TYPES, MEDIA_MIME_TYPES);
		intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
		intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
		return intent;
	}

	private <I, O> void launchMediaPicker(@NonNull ActivityResultContract<I, O> contract, @NonNull I input, @NonNull ActivityResultCallback<O> callback) {
		unregisterMediaPickerLauncher();
		ActivityResultLauncher<I> launcher = mapActivity.getActivityResultRegistry().register(
				ADD_MEDIA_PICKER_KEY + SystemClock.elapsedRealtimeNanos(), contract, result -> {
					unregisterMediaPickerLauncher();
					callback.onActivityResult(result);
				});
		mediaPickerLauncher = launcher;
		try {
			launcher.launch(input);
		} catch (ActivityNotFoundException e) {
			unregisterMediaPickerLauncher();
			LOG.warn("Failed to launch media picker", e);
			app.showToastMessage(R.string.no_activity_for_intent);
		}
	}

	private void unregisterMediaPickerLauncher() {
		if (mediaPickerLauncher != null) {
			mediaPickerLauncher.unregister();
			mediaPickerLauncher = null;
		}
	}

	private void onMediaPicked(@NonNull Linkable target, @NonNull LatLon latLon,
	                           @Nullable Runnable onMediaChanged, @NonNull List<Uri> uris, int persistableUriFlags) {
		if (uris.isEmpty()) {
			logDebug("Attached media picker returned no media");
			return;
		}
		logDebug("Attached media picker returned media: count=" + uris.size() + ", persistableFlags=" + persistableUriFlags);
		OsmAndTaskManager.executeTask(new CollectMediaLinksTask(app, latLon, uris, persistableUriFlags, links -> {
			logDebug("Attached media picker links collected: count=" + links.size());
			dataHelper.addMediaLinks(target, links, onMediaChanged);
			return true;
		}));
	}

	private static void logDebug(@NonNull String message) {
		if (PluginsHelper.isDevelopment()) {
			LOG.debug(message);
		}
	}
}
