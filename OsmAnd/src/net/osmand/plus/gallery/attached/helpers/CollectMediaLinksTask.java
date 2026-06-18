package net.osmand.plus.gallery.attached.helpers;

import static android.content.ContentResolver.SCHEME_CONTENT;
import static net.osmand.shared.media.MediaFileNameFormat.IMG_EXTENSION;
import static net.osmand.shared.media.MediaFileNameFormat.MPEG4_EXTENSION;

import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.OpenableColumns;
import android.webkit.MimeTypeMap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.CallbackWithObject;
import net.osmand.PlatformUtil;
import net.osmand.data.LatLon;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.settings.mediastorage.MediaDirType;
import net.osmand.plus.settings.mediastorage.MediaStorageHelper;
import net.osmand.plus.settings.mediastorage.MediaStorageLocation;
import net.osmand.plus.settings.mediastorage.MediaStorageUtils;
import net.osmand.plus.settings.mediastorage.MediaTarget;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.shared.gpx.primitives.Link;
import net.osmand.shared.media.MediaFileNameFormat;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

class CollectMediaLinksTask extends AsyncTask<Void, Void, List<Link>> {

	private static final Log LOG = PlatformUtil.getLog(CollectMediaLinksTask.class);
	private static final String DEFAULT_AUDIO_EXTENSION = "mp3";

	private final OsmandApplication app;
	private final ContentResolver contentResolver;
	private final MediaStorageHelper mediaStorageHelper;

	private final LatLon latLon;
	private final List<Uri> uris;
	private final MediaStorageLocation storageLocation;
	private final CallbackWithObject<List<Link>> callback;
	private final boolean autoCopyMedia;
	private final int persistableUriFlags;

	CollectMediaLinksTask(@NonNull OsmandApplication app, @NonNull LatLon latLon, @NonNull List<Uri> uris,
			int persistableUriFlags, @NonNull CallbackWithObject<List<Link>> callback) {
		this.app = app;
		this.contentResolver = app.getContentResolver();
		this.mediaStorageHelper = new MediaStorageHelper(app);
		this.latLon = latLon;
		this.autoCopyMedia = app.getSettings().AUTO_COPY_MEDIA_TO_OSMAND_STORAGE.get();
		this.storageLocation = MediaStorageLocation.fromSettings(app);
		this.uris = new ArrayList<>(uris);
		this.persistableUriFlags = persistableUriFlags;
		this.callback = callback;
	}

	@Override
	protected List<Link> doInBackground(Void... voids) {
		logDebug("Collect picked media links started: count=" + uris.size() + ", autoCopy=" + autoCopyMedia + ", storage=" + storageLocation.getStorageType());
		List<Link> links = new ArrayList<>();
		for (Uri uri : uris) {
			Link link = createMediaLink(uri);
			if (link != null) {
				links.add(link);
			}
		}
		logDebug("Collect picked media links finished: created=" + links.size() + ", requested=" + uris.size());
		return links;
	}

	@Override
	protected void onPostExecute(List<Link> links) {
		if (links.isEmpty()) {
			LOG.warn("No media links were created for picked media");
		}
		callback.processResult(links);
	}

	@Nullable
	private Link createMediaLink(@NonNull Uri uri) {
		try {
			PickedMedia media = readPickedMedia(uri);
			Link link = autoCopyMedia ? createAutoCopyLink(media) : createOriginalOrCopyLink(media);
			logDebug("Picked media link result: uri=" + uri + ", href=" + (link == null ? null : link.getHref()));
			return link;
		} catch (RuntimeException e) {
			LOG.warn("Failed to create media link: " + uri, e);
			return null;
		}
	}

	@Nullable
	private Link createAutoCopyLink(@NonNull PickedMedia media) {
		if (!isSupportedMedia(media)) {
			LOG.warn("Picked media has unsupported type, storing original URI if permission is persistent: " + media);
			return createOriginalLinkIfPersistent(media);
		}
		return copyMediaToOsmAndStorage(media);
	}

	@Nullable
	private Link createOriginalOrCopyLink(@NonNull PickedMedia media) {
		Link link = createOriginalLinkIfPersistent(media);
		if (link != null) {
			return link;
		}
		if (isSupportedMedia(media)) {
			LOG.warn("Picked media URI permission is not persistent, copying to OsmAnd storage: " + media.uri());
			return copyMediaToOsmAndStorage(media);
		}
		LOG.warn("Picked media URI permission is not persistent and media type is unsupported, skipping: " + media);
		return null;
	}

	@Nullable
	private Link createOriginalLinkIfPersistent(@NonNull PickedMedia media) {
		if (!canStoreOriginalUri(media.uri())) {
			return null;
		}
		return createOriginalLink(media);
	}

	@NonNull
	private Link createOriginalLink(@NonNull PickedMedia media) {
		logDebug("Picked media stored as original URI: uri=" + media.uri() + ", mimeType=" + media.mimeType());
		return new Link(media.uri().toString(), media.name(), media.mimeType());
	}

	@Nullable
	private Link copyMediaToOsmAndStorage(@NonNull PickedMedia media) {
		String extension = getMediaExtension(media);
		MediaDirType dirType = getMediaDirType(media, extension);
		String fileName = MediaFileNameFormat.createUniqueMediaFileName(latLon.getLatitude(), latLon.getLongitude(),
				extension, name -> mediaStorageHelper.mediaFileExists(storageLocation, dirType, name));
		String mimeType = MediaStorageUtils.getMimeType(media.mimeType(), fileName, dirType);
		MediaTarget target = mediaStorageHelper.createTarget(storageLocation, dirType, fileName, mimeType);
		if (target == null) {
			LOG.warn("Failed to create media storage target for: " + media);
			return null;
		}
		logDebug("Picked media auto-copy started: uri=" + media.uri() + ", target=" + target.getFileName()
				+ ", dirType=" + dirType + ", mimeType=" + mimeType);
		try {
			InputStream input = contentResolver.openInputStream(media.uri());
			if (input == null) {
				LOG.warn("Failed to open media input stream: " + media.uri());
				return null;
			}
			String error = MediaStorageUtils.copyToTarget(input, target, null);
			if (error != null) {
				LOG.warn("Failed to copy media to OsmAnd storage: " + media.uri() + ", " + error);
				return null;
			}
			logDebug("Picked media auto-copy finished: uri=" + media.uri() + ", href=" + target.getHref());
			return createOsmAndStorageLink(target, media, mimeType);
		} catch (Exception e) {
			LOG.warn("Failed to copy media to OsmAnd storage: " + media.uri(), e);
			return null;
		}
	}

	@NonNull
	private Link createOsmAndStorageLink(@NonNull MediaTarget target, @NonNull PickedMedia media, @NonNull String mimeType) {
		String href = target.getHref();
		return new Link(href, media.name(), mimeType);
	}

	@NonNull
	private PickedMedia readPickedMedia(@NonNull Uri uri) {
		String name = getMediaName(uri);
		String extension = getExtensionByName(name);
		String mimeType = getMediaMimeType(uri, extension);
		return new PickedMedia(uri, name, mimeType, extension);
	}

	private boolean isSupportedMedia(@NonNull PickedMedia media) {
		return isSupportedMediaMimeType(media.mimeType()) || MediaDirType.isSupportedExtension(media.extension());
	}

	@NonNull
	private MediaDirType getMediaDirType(@NonNull PickedMedia media, @NonNull String extension) {
		return MediaDirType.fromMimeTypeOrExtension(media.mimeType(), extension);
	}

	@NonNull
	private String getMediaExtension(@NonNull PickedMedia media) {
		String extension = media.extension();
		if (MediaDirType.isSupportedExtension(extension)) {
			return extension;
		}
		if (!Algorithms.isEmpty(media.mimeType())) {
			String extensionByMimeType = MimeTypeMap.getSingleton().getExtensionFromMimeType(media.mimeType());
			if (!Algorithms.isEmpty(extensionByMimeType)) {
				return extensionByMimeType.toLowerCase(Locale.US);
			}
		}
		return getDefaultMediaExtension(media.mimeType());
	}

	@Nullable
	private String getMediaMimeType(@NonNull Uri uri, @Nullable String extension) {
		String mimeType = contentResolver.getType(uri);
		if (isSupportedMediaMimeType(mimeType)) {
			return mimeType;
		}
		String mimeTypeByExtension = Algorithms.isEmpty(extension) ? null : MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
		if (isSupportedMediaMimeType(mimeTypeByExtension)) {
			return mimeTypeByExtension;
		}
		return MediaDirType.isSupportedExtension(extension) ? null : mimeType;
	}

	private boolean isSupportedMediaMimeType(@Nullable String mimeType) {
		if (Algorithms.isEmpty(mimeType)) {
			return false;
		}
		String normalized = mimeType.trim().toLowerCase(Locale.US);
		return normalized.startsWith("image/") || normalized.startsWith("video/") || normalized.startsWith("audio/");
	}

	@NonNull
	private String getDefaultMediaExtension(@Nullable String mimeType) {
		if (!Algorithms.isEmpty(mimeType)) {
			String normalized = mimeType.trim().toLowerCase(Locale.US);
			if (normalized.startsWith("video/")) {
				return MPEG4_EXTENSION;
			} else if (normalized.startsWith("audio/")) {
				return DEFAULT_AUDIO_EXTENSION;
			}
		}
		return IMG_EXTENSION;
	}

	@Nullable
	private String getMediaName(@NonNull Uri uri) {
		if (SCHEME_CONTENT.equalsIgnoreCase(uri.getScheme())) {
			try (Cursor cursor = contentResolver.query(uri, new String[] {OpenableColumns.DISPLAY_NAME}, null, null, null)) {
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

	@Nullable
	private String getExtensionByName(@Nullable String name) {
		if (Algorithms.isEmpty(name)) {
			return null;
		}
		String extension = Algorithms.getFileNameExtension(name);
		if (extension.length() == name.length() || extension.isEmpty()) {
			return null;
		}
		return extension.toLowerCase(Locale.US);
	}

	private boolean canStoreOriginalUri(@NonNull Uri uri) {
		if (!SCHEME_CONTENT.equalsIgnoreCase(uri.getScheme())) {
			return true;
		}
		boolean persisted = persistableUriFlags == 0
				? AndroidUtils.takePersistableUriPermission(app, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
				: AndroidUtils.takePersistableUriPermission(app, uri, persistableUriFlags, Intent.FLAG_GRANT_READ_URI_PERMISSION);
		if (persisted) {
			logDebug("Persisted picked media URI permission: " + uri);
		} else {
			logDebug("Picked media URI permission is not persistable: " + uri);
		}
		return persisted;
	}

	private static void logDebug(@NonNull String message) {
		if (PluginsHelper.isDevelopment()) {
			LOG.debug(message);
		}
	}

	private record PickedMedia(@NonNull Uri uri, @Nullable String name, @Nullable String mimeType, @Nullable String extension) {}
}
