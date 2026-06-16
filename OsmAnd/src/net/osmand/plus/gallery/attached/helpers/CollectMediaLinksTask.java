package net.osmand.plus.gallery.attached.helpers;

import static net.osmand.plus.plugins.audionotes.RecordingsFileHelper.IMG_EXTENSION;
import static net.osmand.plus.plugins.audionotes.RecordingsFileHelper.MPEG4_EXTENSION;

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
import net.osmand.plus.plugins.audionotes.RecordingsFileHelper;
import net.osmand.shared.gpx.primitives.Link;
import net.osmand.shared.media.LinkMediaFactory;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

class CollectMediaLinksTask extends AsyncTask<Void, Void, List<Link>> {

	private static final Log LOG = PlatformUtil.getLog(CollectMediaLinksTask.class);
	private static final String DEFAULT_AUDIO_EXTENSION = "mp3";

	private final ContentResolver contentResolver;

	private final LatLon latLon;
	private final List<Uri> uris;
	private final String mediaStorageDir;
	private final File mediaStorageFolder;
	private final CallbackWithObject<List<Link>> callback;
	private final boolean autoCopyMedia;

	CollectMediaLinksTask(@NonNull OsmandApplication app, @NonNull LatLon latLon,
			@NonNull List<Uri> uris, @NonNull CallbackWithObject<List<Link>> callback) {
		this.contentResolver = app.getContentResolver();
		this.mediaStorageFolder = AttachedMediaUiHelper.getMediaStorageFolder(app);
		this.mediaStorageDir = AttachedMediaUiHelper.getMediaStorageDir();
		this.latLon = latLon;
		this.autoCopyMedia = app.getSettings().AUTO_COPY_MEDIA_TO_OSMAND_STORAGE.get();
		this.uris = new ArrayList<>(uris);
		this.callback = callback;
	}

	@Override
	protected List<Link> doInBackground(Void... voids) {
		List<Link> links = new ArrayList<>();
		for (Uri uri : uris) {
			Link link = createMediaLink(uri);
			if (link != null) {
				links.add(link);
			}
		}
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
			return autoCopyMedia ? createAutoCopyLink(media) : createOriginalLink(media);
		} catch (SecurityException e) {
			LOG.warn("Failed to create media link: " + uri, e);
			return null;
		}
	}

	@Nullable
	private Link createAutoCopyLink(@NonNull PickedMedia media) {
		if (!isSupportedMedia(media)) {
			LOG.warn("Picked media has unsupported type, storing original URI: " + media);
			return createOriginalLink(media);
		}
		File file = copyMediaToOsmAndStorage(media);
		return file != null ? createOsmAndStorageLink(file, media) : null;
	}

	@NonNull
	private Link createOriginalLink(@NonNull PickedMedia media) {
		persistOriginalUriPermission(media.uri());
		return new Link(media.uri().toString(), media.name(), media.mimeType());
	}

	@Nullable
	private File copyMediaToOsmAndStorage(@NonNull PickedMedia media) {
		File destFile = RecordingsFileHelper.getBaseFileName(latLon.getLatitude(), latLon.getLongitude(), mediaStorageFolder, getMediaExtension(media));
		InputStream input = null;
		OutputStream output = null;
		try {
			input = contentResolver.openInputStream(media.uri());
			if (input == null) {
				LOG.warn("Failed to open media input stream: " + media.uri());
				return null;
			}
			output = new FileOutputStream(destFile);
			Algorithms.streamCopy(input, output);
			return destFile;
		} catch (IOException | SecurityException e) {
			LOG.warn("Failed to copy media to OsmAnd storage: " + media.uri(), e);
			if (destFile.exists() && !destFile.delete()) {
				LOG.warn("Failed to delete incomplete media copy: " + destFile);
			}
			return null;
		} finally {
			Algorithms.closeStream(output);
			Algorithms.closeStream(input);
		}
	}

	@NonNull
	private Link createOsmAndStorageLink(@NonNull File file, @NonNull PickedMedia media) {
		String href = LinkMediaFactory.createInternalUri(mediaStorageDir + file.getName());
		return new Link(href, media.name(), media.mimeType());
	}

	@NonNull
	private PickedMedia readPickedMedia(@NonNull Uri uri) {
		String name = getMediaName(uri);
		String extension = getExtensionByName(name);
		String mimeType = getMediaMimeType(uri, extension);
		return new PickedMedia(uri, name, mimeType, extension);
	}

	private boolean isSupportedMedia(@NonNull PickedMedia media) {
		return isSupportedMediaMimeType(media.mimeType()) || isSupportedMediaExtension(media.extension());
	}

	@NonNull
	private String getMediaExtension(@NonNull PickedMedia media) {
		String extension = media.extension();
		if (extension != null && isSupportedMediaExtension(extension)) {
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
		return isSupportedMediaExtension(extension) ? null : mimeType;
	}

	private boolean isSupportedMediaMimeType(@Nullable String mimeType) {
		if (Algorithms.isEmpty(mimeType)) {
			return false;
		}
		String normalized = mimeType.trim().toLowerCase(Locale.US);
		return normalized.startsWith("image/") || normalized.startsWith("video/") || normalized.startsWith("audio/");
	}

	private boolean isSupportedMediaExtension(@Nullable String extension) {
		if (Algorithms.isEmpty(extension)) {
			return false;
		}
		return switch (extension) {
			case "jpg", "jpeg", "png", "gif", "webp", "bmp", "heic", "heif", "svg", "mp4", "m4v",
			     "mov", "avi", "mkv", "webm", "3gp", "3gpp", "mp3", "m4a", "aac", "wav", "ogg",
			     "oga", "opus", "amr" -> true;
			default -> false;
		};
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
		if ("content".equalsIgnoreCase(uri.getScheme())) {
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
		int index = name.lastIndexOf('.');
		if (index < 0 || index == name.length() - 1) {
			return null;
		}
		return name.substring(index + 1).toLowerCase(Locale.US);
	}

	private void persistOriginalUriPermission(@NonNull Uri uri) {
		try {
			contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
		} catch (SecurityException e) {
			LOG.warn("Failed to persist media URI permission: " + uri, e);
		}
	}

	private record PickedMedia(@NonNull Uri uri, @Nullable String name, @Nullable String mimeType, @Nullable String extension) {}
}
