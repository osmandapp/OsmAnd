package net.osmand.plus.settings.mediastorage;

import static net.osmand.plus.settings.enums.MediaStorageType.CAMERA_FOLDER;
import static net.osmand.plus.settings.enums.MediaStorageType.SHARED_STORAGE;

import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.webkit.MimeTypeMap;

import androidx.annotation.ChecksSdkIntAtLeast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.documentfile.provider.DocumentFile;

import net.osmand.IProgress;
import net.osmand.PlatformUtil;
import net.osmand.plus.settings.enums.MediaStorageType;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Stateless helpers for {@link MediaStorageHelper}: storage-type predicates, directory and
 * relative-path math, MediaStore collection URIs, MIME resolution, stream copying and
 * low-level file/cursor utilities. All methods are pure (no app state) and unit-testable.
 */
public final class MediaStorageUtils {

	private static final Log log = PlatformUtil.getLog(MediaStorageUtils.class);
	static final String MEDIA_SUBFOLDER = "OsmAnd";

	// ---- Storage-type predicates ----

	static boolean isPublicStorage(@NonNull MediaStorageType storageType) {
		return storageType == SHARED_STORAGE || storageType == CAMERA_FOLDER;
	}

	/**
	 * Public storage written through MediaStore (Android Q+).
	 */
	@ChecksSdkIntAtLeast(api = VERSION_CODES.Q)
	static boolean usesMediaStore(@NonNull MediaStorageType storageType) {
		return Build.VERSION.SDK_INT >= VERSION_CODES.Q && isPublicStorage(storageType);
	}

	/**
	 * Public storage written through raw file access, which needs WRITE_EXTERNAL_STORAGE (pre-Q).
	 */
	static boolean requiresRawPublicStoragePermission(@NonNull MediaStorageType storageType) {
		return Build.VERSION.SDK_INT < VERSION_CODES.Q && isPublicStorage(storageType);
	}

	// ---- Directory resolution ----

	@Nullable
	static File resolveRawMediaDir(@NonNull MediaStorageType storageType, @NonNull MediaDirType dirType, @NonNull File internalDir) {
		return switch (storageType) {
			case SHARED_STORAGE -> publicSubfolder(dirType.getStandardDir());
			case CAMERA_FOLDER -> publicSubfolder(cameraDirectory(dirType));
			case MANUALLY_SPECIFIED -> null;
			default -> internalDir;
		};
	}

	@NonNull
	static String getMediaStoreRelativePath(@NonNull MediaStorageType storageType, @NonNull MediaDirType dirType) {
		String directory = storageType == CAMERA_FOLDER ? cameraDirectory(dirType) : dirType.getStandardDir();
		return normalizeRelativePath(directory + File.separator + MEDIA_SUBFOLDER);
	}

	@NonNull
	private static File publicSubfolder(@NonNull String publicDirectory) {
		return new File(Environment.getExternalStoragePublicDirectory(publicDirectory), MEDIA_SUBFOLDER);
	}

	@NonNull
	private static String cameraDirectory(@NonNull MediaDirType dirType) {
		return dirType == MediaDirType.AUDIO ? Environment.DIRECTORY_MUSIC : Environment.DIRECTORY_DCIM;
	}

	@Nullable
	static DocumentFile getOrCreateDirectory(@NonNull DocumentFile root, @NonNull String name) {
		DocumentFile child = root.findFile(name);
		if (child != null) {
			return child.isDirectory() ? child : null;
		}
		return root.createDirectory(name);
	}

	@RequiresApi(api = VERSION_CODES.Q)
	@NonNull
	static Uri getMediaStoreCollectionUri(@NonNull MediaDirType dirType) {
		String volumeName = MediaStore.VOLUME_EXTERNAL_PRIMARY;
		return switch (dirType) {
			case AUDIO -> MediaStore.Audio.Media.getContentUri(volumeName);
			case VIDEO -> MediaStore.Video.Media.getContentUri(volumeName);
			default -> MediaStore.Images.Media.getContentUri(volumeName);
		};
	}

	// ---- SAF / manual tree ----

	static boolean isInManualTree(@NonNull MediaStorageLocation location, @NonNull Uri uri) {
		Uri treeUri = location.getManualUri();
		if (treeUri == null || !DocumentsContract.isTreeUri(treeUri)
				|| !Algorithms.stringsEqual(treeUri.getAuthority(), uri.getAuthority())
				|| !uri.getPathSegments().contains("document")) {
			return false;
		}
		try {
			String treeDocumentId = DocumentsContract.getTreeDocumentId(treeUri);
			String documentId = DocumentsContract.getDocumentId(uri);
			return documentId.equals(treeDocumentId) || documentId.startsWith(treeDocumentId + "/");
		} catch (IllegalArgumentException e) {
			log.warn("Failed to compare manual media tree uri: " + uri, e);
			return false;
		}
	}

	@Nullable
	static String getManualTreeName(@Nullable String documentId) {
		if (Algorithms.isEmpty(documentId)) {
			return null;
		}
		int separator = documentId.indexOf(':');
		String name = separator >= 0 && separator + 1 < documentId.length()
				? documentId.substring(separator + 1) : documentId;
		return Uri.decode(Algorithms.isEmpty(name) ? documentId : name);
	}

	// ---- Path helpers ----

	static boolean isInDirectory(@Nullable File dir, @NonNull File file) {
		if (dir == null) {
			return false;
		}
		try {
			String dirPath = dir.getCanonicalPath();
			String prefix = dirPath.endsWith(File.separator) ? dirPath : dirPath + File.separator;
			return file.getCanonicalPath().startsWith(prefix);
		} catch (IOException e) {
			log.warn("Failed to compare media paths: " + file + ", " + dir, e);
			return false;
		}
	}

	@NonNull
	static String normalizeRelativePath(@Nullable String path) {
		if (path == null) {
			return "";
		}
		String normalized = path.replace('\\', '/');
		while (normalized.endsWith("/")) {
			normalized = normalized.substring(0, normalized.length() - 1);
		}
		return normalized;
	}

	// ---- MIME ----

	@NonNull
	public static String getMimeType(@Nullable String mimeType, @NonNull String fileName, @NonNull MediaDirType dirType) {
		String extension = Algorithms.getFileNameExtension(fileName);
		if (dirType == MediaDirType.AUDIO && ("3gp".equals(extension) || "3gpp".equals(extension) || "3ga".equals(extension))) {
			return "audio/3gpp";
		}
		if (!Algorithms.isEmpty(mimeType)) {
			return mimeType;
		}
		String type = Algorithms.isEmpty(extension) ? null : MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
		if (!Algorithms.isEmpty(type)) {
			return type;
		}
		return switch (dirType) {
			case AUDIO -> "audio/mpeg";
			case VIDEO -> "video/mp4";
			default -> "image/jpeg";
		};
	}

	// ---- Cursor ----

	@Nullable
	static String getString(@NonNull Cursor cursor, @NonNull String column) {
		int index = cursor.getColumnIndex(column);
		return index >= 0 ? cursor.getString(index) : null;
	}

	static long getLong(@NonNull Cursor cursor, @NonNull String column) {
		int index = cursor.getColumnIndex(column);
		return index >= 0 ? cursor.getLong(index) : 0;
	}

	// ---- Stream copy ----

	@Nullable
	public static String copyToTarget(@NonNull InputStream inputStream, @NonNull MediaTarget target, @Nullable IProgress progress) {
		// The output stream must be fully closed before finish(true), so it is closed
		// by try-with-resources before the finish step runs.
		try (OutputStream outputStream = target.openOutputStream()) {
			if (progress != null) {
				Algorithms.streamCopy(inputStream, outputStream, progress, 1024);
			} else {
				Algorithms.streamCopy(inputStream, outputStream);
			}
		} catch (Exception e) {
			return finishFailed(target, e);
		} finally {
			Algorithms.closeStream(inputStream);
		}
		try {
			target.finish(true);
			return null;
		} catch (Exception e) {
			return finishFailed(target, e);
		}
	}

	@NonNull
	private static String finishFailed(@NonNull MediaTarget target, @NonNull Exception cause) {
		try {
			target.finish(false);
		} catch (Exception e) {
			log.warn("Failed to clean up media target after copy error", e);
		}
		return getErrorMessage(cause);
	}

	@NonNull
	public static String getErrorMessage(@NonNull Exception e) {
		String message = e.getMessage();
		return Algorithms.isEmpty(message) ? e.getClass().getSimpleName() : message;
	}
}