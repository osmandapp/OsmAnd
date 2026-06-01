package net.osmand.plus.gallery.helpers;

import static android.app.Activity.RESULT_OK;
import static net.osmand.plus.gallery.model.GalleryMediaGroup.OTHER;

import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.SystemClock;
import android.provider.OpenableColumns;
import android.view.View;
import android.webkit.MimeTypeMap;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.OnResultCallback;
import net.osmand.PlatformUtil;
import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.gallery.controller.GalleryController;
import net.osmand.plus.gallery.controller.GalleryItemsHolder;
import net.osmand.plus.gallery.model.GalleryItem;
import net.osmand.plus.gallery.ui.GalleryGridFragment;
import net.osmand.plus.gallery.ui.GalleryPhotoPagerFragment;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.audionotes.AudioVideoNotesPlugin;
import net.osmand.plus.plugins.audionotes.AudioVideoNotesPlugin.AVActionType;
import net.osmand.plus.plugins.audionotes.Recording;
import net.osmand.plus.settings.enums.ThemeUsageContext;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.popup.PopUpMenu;
import net.osmand.plus.widgets.popup.PopUpMenuDisplayData;
import net.osmand.plus.widgets.popup.PopUpMenuItem;
import net.osmand.plus.widgets.popup.PopUpMenuWidthMode;
import net.osmand.shared.gpx.primitives.Link;
import net.osmand.shared.gpx.primitives.WptPt;
import net.osmand.shared.media.LinkMediaFactory;
import net.osmand.shared.media.MediaUriResolver;
import net.osmand.shared.media.domain.MediaItem;
import net.osmand.shared.media.domain.MediaType;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class AttachedMediaUiHelper {

	private static final Log LOG = PlatformUtil.getLog(AttachedMediaUiHelper.class);
	private static final String ADD_MEDIA_PICKER_KEY = "attached_media_picker_";
	private static final String[] MEDIA_MIME_TYPES = {"image/*", "video/*", "audio/*"};

	private final OsmandApplication app;
	private final MapActivity mapActivity;
	private final AttachedMediaDataHelper dataHelper;
	@Nullable
	private ActivityResultLauncher<Intent> mediaPickerLauncher;

	public AttachedMediaUiHelper(@NonNull MapActivity mapActivity) {
		this.mapActivity = mapActivity;
		this.app = mapActivity.getApp();
		this.dataHelper = new AttachedMediaDataHelper(app);
	}

	@NonNull
	public List<GalleryItem> getGalleryItems(@Nullable List<Link> links) {
		List<MediaItem> mediaItems = LinkMediaFactory.fromLinks(links);
		List<GalleryItem> items = new ArrayList<>();
		if (mediaItems.isEmpty()) {
			items.add(new GalleryItem.NoMedia(null, R.string.no_media, R.string.no_media_descr, R.drawable.ic_action_image_disabled));
		} else {
			for (int i = mediaItems.size() - 1; i >= 0; i--) {
				items.add(new GalleryItem.Media(mediaItems.get(i), false));
			}
		}
		return items;
	}

	public void showAddMenu(@NonNull View anchorView, @Nullable Object object,
			@Nullable LatLon latLon, @Nullable Runnable onMediaChanged) {
		if (latLon == null) {
			return;
		}
		UiUtilities uiUtilities = app.getUIUtilities();
		boolean nightMode = app.getDaynightHelper().isNightMode(ThemeUsageContext.OVER_MAP);
		int iconColor = ColorUtilities.getDefaultIconColor(app, nightMode);
		List<PopUpMenuItem> items = new ArrayList<>();
		items.add(createAddMenuItem(R.string.recording_context_menu_precord,
				R.drawable.ic_action_photo_dark, uiUtilities, iconColor,
				() -> takeNote(AVActionType.REC_PHOTO, latLon, object, onMediaChanged), false));
		items.add(createAddMenuItem(R.string.recording_context_menu_vrecord,
				R.drawable.ic_action_video_dark, uiUtilities, iconColor,
				() -> takeNote(AVActionType.REC_VIDEO, latLon, object, onMediaChanged), false));
		items.add(createAddMenuItem(R.string.recording_context_menu_arecord,
				R.drawable.ic_action_micro_dark, uiUtilities, iconColor,
				() -> takeNote(AVActionType.REC_AUDIO, latLon, object, onMediaChanged), false));
		items.add(createAddMenuItem(R.string.choose_from_gallery,
				R.drawable.ic_action_photo_album, uiUtilities, iconColor,
				() -> chooseMedia(object, onMediaChanged), true));
		items.add(createAddMenuItem(R.string.choose_from_files,
				R.drawable.ic_action_group_list, uiUtilities, iconColor,
				() -> chooseMedia(object, onMediaChanged), false));

		PopUpMenuDisplayData data = new PopUpMenuDisplayData();
		data.anchorView = anchorView;
		data.menuItems = items;
		data.nightMode = nightMode;
		data.widthMode = PopUpMenuWidthMode.STANDARD;
		PopUpMenu.show(data);
	}

	@NonNull
	private PopUpMenuItem createAddMenuItem(int titleId, int iconId, @NonNull UiUtilities uiUtilities,
			int iconColor, @NonNull Runnable action, boolean showTopDivider) {
		return new PopUpMenuItem.Builder(app)
				.setTitleId(titleId)
				.setIcon(uiUtilities.getPaintedIcon(iconId, iconColor))
				.setOnClickListener(item -> action.run())
				.showTopDivider(showTopDivider)
				.create();
	}

	private void takeNote(@NonNull AVActionType type, @NonNull LatLon latLon, @Nullable Object object, @Nullable Runnable onMediaChanged) {
		AudioVideoNotesPlugin plugin = PluginsHelper.getPlugin(AudioVideoNotesPlugin.class);
		if (plugin != null && !plugin.isActive()) {
			PluginsHelper.enablePluginIfNeeded(mapActivity, app, plugin, true);
		}
		if (plugin != null && plugin.isActive()) {
			if (plugin.isRecording()) {
				plugin.stopRecording(mapActivity, true, true);
			} else {
				OnResultCallback<Recording> callback = createRecordingSavedCallback(object, onMediaChanged);
				if (callback != null) {
					plugin.addRecordingCallback(callback);
				}
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

	@Nullable
	private OnResultCallback<Recording> createRecordingSavedCallback(@Nullable Object object,
			@Nullable Runnable onMediaChanged) {
		return object instanceof FavouritePoint || object instanceof WptPt
				? recording -> dataHelper.addRecordingLink(object, recording, onMediaChanged) : null;
	}

	private void chooseMedia(@Nullable Object object, @Nullable Runnable onMediaChanged) {
		startMediaPicker(createOpenMediaDocumentIntent(), object, onMediaChanged);
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

	private void startMediaPicker(@NonNull Intent intent, @Nullable Object object, @Nullable Runnable onMediaChanged) {
		unregisterMediaPickerLauncher();
		mediaPickerLauncher = mapActivity.getActivityResultRegistry().register(ADD_MEDIA_PICKER_KEY + SystemClock.elapsedRealtimeNanos(),
				new StartActivityForResult(), result -> {
					unregisterMediaPickerLauncher();
					onMediaPicked(object, onMediaChanged, result.getResultCode(), result.getData());
				});
		try {
			mediaPickerLauncher.launch(intent);
		} catch (ActivityNotFoundException e) {
			unregisterMediaPickerLauncher();
			app.showToastMessage(R.string.no_activity_for_intent);
		}
	}

	private void unregisterMediaPickerLauncher() {
		if (mediaPickerLauncher != null) {
			mediaPickerLauncher.unregister();
			mediaPickerLauncher = null;
		}
	}

	private void onMediaPicked(@Nullable Object object, @Nullable Runnable onMediaChanged,
			int resultCode, @Nullable Intent data) {
		if (data == null || resultCode != RESULT_OK) {
			return;
		}
		List<Link> links = getPickedMediaLinks(data);
		if (!links.isEmpty()) {
			dataHelper.addMediaLinks(object, links, onMediaChanged);
		}
	}

	@NonNull
	private List<Link> getPickedMediaLinks(@NonNull Intent data) {
		Set<Uri> uris = new LinkedHashSet<>();
		Uri dataUri = data.getData();
		if (dataUri != null) {
			uris.add(dataUri);
		}
		List<Link> links = new ArrayList<>();
		ClipData clipData = data.getClipData();
		if (clipData != null) {
			for (int i = 0; i < clipData.getItemCount(); i++) {
				Uri uri = clipData.getItemAt(i).getUri();
				if (uri != null) {
					uris.add(uri);
				}
			}
		}
		for (Uri uri : uris) {
			links.add(createMediaLink(uri, data));
		}
		return links;
	}

	@NonNull
	private Link createMediaLink(@NonNull Uri uri, @NonNull Intent data) {
		persistMediaPermission(uri, data);
		String name = getMediaName(uri);
		String mimeType = getMediaMimeType(uri, name);
		return new Link(uri.toString(), name, mimeType);
	}

	@Nullable
	private String getMediaMimeType(@NonNull Uri uri, @Nullable String name) {
		String mimeType = mapActivity.getContentResolver().getType(uri);
		if (isSupportedMediaMimeType(mimeType)) {
			return mimeType;
		}
		if (shouldFallbackToName(mimeType)) {
			String mimeTypeByName = getMediaMimeTypeByName(name);
			if (!Algorithms.isEmpty(mimeTypeByName)) {
				return mimeTypeByName;
			}
		}
		return mimeType;
	}

	private boolean isSupportedMediaMimeType(@Nullable String mimeType) {
		if (Algorithms.isEmpty(mimeType)) {
			return false;
		}
		String normalized = mimeType.trim().toLowerCase(Locale.US);
		return normalized.startsWith("image/")
				|| normalized.startsWith("video/")
				|| normalized.startsWith("audio/");
	}

	private boolean shouldFallbackToName(@Nullable String mimeType) {
		if (Algorithms.isEmpty(mimeType)) {
			return true;
		}
		String normalized = mimeType.trim().toLowerCase(Locale.US);
		return "*/*".equals(normalized)
				|| "application/octet-stream".equals(normalized)
				|| "binary/octet-stream".equals(normalized);
	}

	@Nullable
	private String getMediaMimeTypeByName(@Nullable String name) {
		if (Algorithms.isEmpty(name)) {
			return null;
		}
		int index = name.lastIndexOf('.');
		if (index < 0 || index == name.length() - 1) {
			return null;
		}
		String extension = name.substring(index + 1).toLowerCase(Locale.US);
		return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
	}

	private void persistMediaPermission(@NonNull Uri uri, @NonNull Intent data) {
		int flags = data.getFlags() & Intent.FLAG_GRANT_READ_URI_PERMISSION;
		if (flags != 0) {
			try {
				mapActivity.getContentResolver().takePersistableUriPermission(uri, flags);
			} catch (SecurityException e) {
				LOG.warn("Failed to persist media URI permission: " + uri, e);
			}
		}
	}

	@Nullable
	private String getMediaName(@NonNull Uri uri) {
		if ("content".equalsIgnoreCase(uri.getScheme())) {
			try (Cursor cursor = mapActivity.getContentResolver().query(uri,
					new String[] {OpenableColumns.DISPLAY_NAME}, null, null, null)) {
				if (cursor != null && cursor.moveToFirst()) {
					int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
					if (index >= 0) {
						return cursor.getString(index);
					}
				}
			}
		}
		String path = uri.getPath();
		return Algorithms.isEmpty(path) ? null : new File(path).getName();
	}

	public void showAllMedia(@NonNull GalleryController galleryController, @Nullable Object object,
			@Nullable LatLon latLon) {
		if (updateMediaGalleryHolder(galleryController, object, latLon)) {
			GalleryGridFragment.showInstance(mapActivity, app.getString(R.string.shared_string_media));
		}
	}

	public void onMediaItemClicked(@NonNull GalleryController galleryController,
			@NonNull MediaItem mediaItem, @Nullable Object object, @Nullable LatLon latLon,
			boolean nightMode) {
		if (mediaItem.getType() == MediaType.PHOTO && updateMediaGalleryHolder(galleryController, object, latLon)) {
			int position = galleryController.getPhotoItemIndexById(mediaItem.getId());
			GalleryPhotoPagerFragment.showInstance(mapActivity, position);
		} else {
			openMediaItem(mediaItem, nightMode);
		}
	}

	private boolean updateMediaGalleryHolder(@NonNull GalleryController galleryController,
			@Nullable Object object, @Nullable LatLon latLon) {
		if (latLon == null) {
			return false;
		}
		GalleryItemsHolder holder = new GalleryItemsHolder(latLon, Collections.emptyMap());
		List<GalleryItem> galleryItems = getGalleryItems(dataHelper.getMediaLinks(object));
		for (GalleryItem item : galleryItems) {
			if (item instanceof GalleryItem.Media media) {
				holder.addMediaItem(OTHER, media.getMediaItem());
			}
		}
		galleryController.setCurrentGalleryItemsHolder(holder);
		return !holder.getOrderedGalleryItems().isEmpty();
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
