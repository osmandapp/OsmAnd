package net.osmand.plus.gallery.attached.helpers;

import static android.app.Activity.RESULT_OK;

import static net.osmand.IndexConstants.MEDIA_INDEX_DIR;

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

import net.osmand.PlatformUtil;
import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.plus.OsmAndTaskManager;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.IntentHelper;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.audionotes.AudioVideoNotesPlugin;
import net.osmand.plus.plugins.audionotes.AVActionType;
import net.osmand.plus.plugins.audionotes.Recording;
import net.osmand.plus.settings.enums.ThemeUsageContext;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.popup.PopUpMenu;
import net.osmand.plus.widgets.popup.PopUpMenuDisplayData;
import net.osmand.plus.widgets.popup.PopUpMenuItem;
import net.osmand.plus.widgets.popup.PopUpMenuWidthMode;
import net.osmand.shared.gpx.primitives.Linkable;
import net.osmand.shared.gpx.primitives.WptPt;
import net.osmand.shared.media.MediaUriResolver;
import net.osmand.shared.media.domain.MediaItem;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class AttachedMediaUiHelper {

	private static final Log LOG = PlatformUtil.getLog(AttachedMediaUiHelper.class);
	private static final String ADD_MEDIA_PICKER_KEY = "attached_media_picker_";
	private static final String[] MEDIA_MIME_TYPES = {"image/*", "video/*", "audio/*"};

	private final OsmandApplication app;
	private final MapActivity mapActivity;
	private final UiUtilities iconsCache;
	private final AttachedMediaDataHelper dataHelper;
	@Nullable
	private ActivityResultLauncher<?> mediaPickerLauncher;

	public AttachedMediaUiHelper(@NonNull MapActivity mapActivity) {
		this.mapActivity = mapActivity;
		this.app = mapActivity.getApp();
		this.iconsCache = app.getUIUtilities();
		this.dataHelper = new AttachedMediaDataHelper(app);
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
				plugin.addRecordingsListener(new AudioVideoNotesPlugin.RecordingsListener() {
					@Override
					public boolean onRecordingsAdded(@NonNull List<Recording> recordings) {
						plugin.removeRecordingsListener(this);
						for (Recording recording : recordings) {
							dataHelper.addRecordingLink(target, recording, onMediaChanged);
						}
						return true;
					}

					@Override
					public void onRecordingsCancelled() {
						plugin.removeRecordingsListener(this);
					}
				});
				switch (type) {
					case REC_PHOTO ->
							plugin.takePhoto(latLon.getLatitude(), latLon.getLongitude(), mapActivity, false, false);
					case REC_VIDEO ->
							plugin.recordVideo(latLon.getLatitude(), latLon.getLongitude(), mapActivity, false);
					case REC_AUDIO ->
							plugin.recordAudio(latLon.getLatitude(), latLon.getLongitude(), mapActivity);
				}
			}
		}
	}

	private void chooseFromGallery(@NonNull Linkable target, @NonNull LatLon latLon, @Nullable Runnable onMediaChanged) {
		PickVisualMediaRequest request = new PickVisualMediaRequest.Builder()
				.setMediaType(PickVisualMedia.ImageAndVideo.INSTANCE)
				.build();
		launchMediaPicker(new PickMultipleVisualMedia(), request,
				uris -> onMediaPicked(target, latLon, onMediaChanged, uris));
	}

	private void chooseFromFiles(@NonNull Linkable target, @NonNull LatLon latLon, @Nullable Runnable onMediaChanged) {
		launchMediaPicker(new StartActivityForResult(), createOpenMediaDocumentIntent(), result -> {
			Intent data = result.getData();
			if (data != null && result.getResultCode() == RESULT_OK) {
				onMediaPicked(target, latLon, onMediaChanged, IntentHelper.getIntentUris(data));
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
	                           @Nullable Runnable onMediaChanged, @NonNull List<Uri> uris) {
		if (uris.isEmpty()) {
			return;
		}
		OsmAndTaskManager.executeTask(new CollectMediaLinksTask(app, latLon, uris, links -> {
			dataHelper.addMediaLinks(target, links, onMediaChanged);
			return true;
		}));
	}

	@NonNull
	public static File getMediaStorageFolder(@NonNull OsmandApplication app) {
		return app.getAppPath(getMediaStorageDir());
	}

	@NonNull
	public static String getMediaStorageDir() {
		return MEDIA_INDEX_DIR;
	}

	public void openMediaItem(@NonNull MediaItem mediaItem, boolean nightMode) {
		String uri = MediaUriResolver.getDetailsLink(mediaItem);
		if (!Algorithms.isEmpty(uri)) {
			String scheme = Uri.parse(uri).getScheme();
			if ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)) {
				AndroidUtils.openUrl(mapActivity, uri, nightMode);
			} else {
				openLocalMediaItem(mediaItem, uri);
			}
		}
	}

	private void openLocalMediaItem(@NonNull MediaItem mediaItem, @NonNull String uri) {
		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.setDataAndType(getLocalMediaUri(uri), getMediaMimeType(mediaItem));
		intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
		AndroidUtils.startActivityIfSafe(mapActivity, intent);
	}

	@NonNull
	private Uri getLocalMediaUri(@NonNull String uri) {
		Uri parsedUri = Uri.parse(uri);
		String scheme = parsedUri.getScheme();
		if ("content".equalsIgnoreCase(scheme)) {
			return parsedUri;
		}
		if ("file".equalsIgnoreCase(scheme)) {
			String path = parsedUri.getPath();
			return Algorithms.isEmpty(path) ? parsedUri : AndroidUtils.getUriForFile(mapActivity, new File(path));
		}
		File file = new File(uri);
		if (!file.isAbsolute()) {
			file = app.getAppPath(uri);
		}
		return AndroidUtils.getUriForFile(mapActivity, file);
	}

	@NonNull
	private String getMediaMimeType(@NonNull MediaItem mediaItem) {
		return switch (mediaItem.getType()) {
			case PHOTO -> "image/*";
			case VIDEO -> "video/*";
			case AUDIO -> "audio/*";
			default -> "*/*";
		};
	}
}
