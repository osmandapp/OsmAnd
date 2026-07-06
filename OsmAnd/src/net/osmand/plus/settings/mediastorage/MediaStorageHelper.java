package net.osmand.plus.settings.mediastorage;

import static android.content.ContentResolver.SCHEME_CONTENT;
import static android.content.ContentResolver.SCHEME_FILE;
import static net.osmand.plus.settings.enums.MediaStorageType.MAIN_STORAGE;
import static net.osmand.plus.settings.enums.MediaStorageType.MANUALLY_SPECIFIED;

import android.Manifest;
import android.database.Cursor;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.OpenableColumns;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.settings.enums.MediaStorageType;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.FileUtils;
import net.osmand.shared.media.LinkMediaFactory;
import net.osmand.shared.media.MediaFileNameFormat;
import net.osmand.shared.media.MediaProvider;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Resolves and writes OsmAnd-owned attached media. Internal app media is file-backed;
 * public media is written through MediaStore; manual folders are written through SAF.
 */
public class MediaStorageHelper {

	private static final Log log = PlatformUtil.getLog(MediaStorageHelper.class);

	private static final String MEDIA_AUTHORITY = "media";

	@NonNull
	private final OsmandApplication app;

	public MediaStorageHelper(@NonNull OsmandApplication app) {
		this.app = app;
	}

	@NonNull
	OsmandApplication getApp() {
		return app;
	}

	@NonNull
	private File getInternalMediaDir() {
		return MediaProvider.getInternalMediaDir(app.getAppPath().getAbsolutePath());
	}

	@NonNull
	public String getStorageDisplayDirectory(@NonNull MediaStorageType storageType) {
		return getStorageDisplayDirectory(storageType, app.getSettings().MEDIA_STORAGE_MANUAL_URI.get());
	}

	@NonNull
	public String getStorageDisplayDirectory(@NonNull MediaStorageType storageType, @Nullable String manualTreeUri) {
		if (storageType == MANUALLY_SPECIFIED) {
			return getManualStorageDisplayDirectory(manualTreeUri);
		}
		File rawDir = MediaStorageUtils.resolveRawMediaDir(storageType, MediaDirType.PHOTO, getInternalMediaDir());
		return rawDir != null ? rawDir.getAbsolutePath() : "";
	}

	@NonNull
	private String getManualStorageDisplayDirectory(@Nullable String manualTreeUri) {
		if (Algorithms.isEmpty(manualTreeUri)) {
			return "";
		}
		try {
			Uri uri = Uri.parse(manualTreeUri);
			if (DocumentsContract.isTreeUri(uri)) {
				String documentId = DocumentsContract.getTreeDocumentId(uri);
				String name = MediaStorageUtils.getManualTreeName(documentId);
				return Algorithms.isEmpty(name) ? manualTreeUri : name;
			}
		} catch (IllegalArgumentException e) {
			return manualTreeUri;
		}
		return manualTreeUri;
	}

	public boolean isStorageWritable(@NonNull MediaStorageLocation location) {
		MediaStorageType storageType = location.getStorageType();
		if (storageType == MANUALLY_SPECIFIED) {
			DocumentFile folder = getManualRootDocument(location);
			return folder != null && folder.canWrite();
		}
		if (MediaStorageUtils.requiresRawPublicStoragePermission(storageType) && !hasRawPublicStoragePermission()) {
			return false;
		}
		if (MediaStorageUtils.usesMediaStore(storageType)) {
			return true;
		}
		for (MediaDirType dirType : MediaDirType.values()) {
			File dir = MediaStorageUtils.resolveRawMediaDir(storageType, dirType, getInternalMediaDir());
			if (dir == null || !FileUtils.isWritable(dir, true)) {
				return false;
			}
		}
		return true;
	}

	public boolean mediaFileExists(@NonNull MediaStorageLocation location, @NonNull MediaDirType dirType, @NonNull String fileName) {
		MediaStorageType storageType = location.getStorageType();
		if (storageType == MANUALLY_SPECIFIED) {
			DocumentFile root = getManualRootDocument(location);
			DocumentFile dir = root == null ? null : root.findFile(dirType.getDirName());
			DocumentFile file = dir != null && dir.isDirectory() ? dir.findFile(fileName) : null;
			return file != null && file.isFile();
		}
		if (MediaStorageUtils.usesMediaStore(storageType)) {
			return mediaStoreFileExists(MediaStorageUtils.getMediaStoreCollectionUri(dirType),
					MediaStorageUtils.getMediaStoreRelativePath(storageType, dirType), fileName);
		}
		File dir = MediaStorageUtils.resolveRawMediaDir(storageType, dirType, getInternalMediaDir());
		return dir != null && new File(dir, fileName).exists();
	}

	@Nullable
	public MediaTarget createTarget(@NonNull MediaStorageLocation location, @NonNull MediaDirType dirType,
	                                @NonNull String fileName, @Nullable String mimeType) {
		MediaStorageType storageType = location.getStorageType();
		if (storageType == MANUALLY_SPECIFIED) {
			return createDocumentTarget(location, dirType, fileName, mimeType);
		}
		if (MediaStorageUtils.usesMediaStore(storageType)) {
			return createMediaStoreTarget(storageType, dirType, fileName, mimeType);
		}
		if (MediaStorageUtils.requiresRawPublicStoragePermission(storageType) && !hasRawPublicStoragePermission()) {
			return null;
		}
		File dir = MediaStorageUtils.resolveRawMediaDir(storageType, dirType, getInternalMediaDir());
		return dir != null ? new FileMediaTarget(this, new File(dir, fileName)) : null;
	}

	private boolean hasRawPublicStoragePermission() {
		return AndroidUtils.hasPermission(app, Manifest.permission.WRITE_EXTERNAL_STORAGE);
	}

	@Nullable
	public MediaSource resolveManagedMediaSource(@NonNull MediaStorageLocation location, @Nullable String href) {
		return resolveMediaSource(location, href, false);
	}

	@Nullable
	public MediaSource resolveMediaSource(@NonNull MediaStorageLocation location, @Nullable String href, boolean includeNonManagedMedia) {
		if (Algorithms.isEmpty(href)) {
			return null;
		}
		String uri = href.trim();
		String internalPath = LinkMediaFactory.getInternalPath(uri);
		if (internalPath != null) {
			return resolveInternalMediaSource(uri, internalPath, includeNonManagedMedia);
		}

		Uri parsedUri = Uri.parse(uri);
		String scheme = parsedUri.getScheme();
		if (SCHEME_FILE.equalsIgnoreCase(scheme)) {
			String path = parsedUri.getPath();
			return path != null ? resolveFileMediaSource(location, uri, new File(path), includeNonManagedMedia) : null;
		} else if (SCHEME_CONTENT.equalsIgnoreCase(scheme)) {
			MediaSource source = null;
			if (location.getStorageType() == MANUALLY_SPECIFIED) {
				source = resolveDocumentMediaSource(location, uri, parsedUri);
			} else if (MediaStorageUtils.isPublicStorage(location.getStorageType())) {
				source = resolveMediaStoreSource(location, uri, parsedUri);
			}
			return source != null || !includeNonManagedMedia ? source : resolveContentMediaSource(uri, parsedUri);
		} else if (scheme == null) {
			File file = new File(uri);
			if (file.isAbsolute()) {
				return resolveFileMediaSource(location, uri, file, includeNonManagedMedia);
			}
		}
		return null;
	}

	@NonNull
	public String createMediaFileHref(@NonNull File file) {
		if (isInInternalMediaDir(file)) {
			return LinkMediaFactory.createInternalMediaUri(file.getName());
		}
		String relativePath = getRelativeAppPath(file);
		return relativePath != null ? LinkMediaFactory.createInternalUri(relativePath) : Uri.fromFile(file).toString();
	}

	private boolean isInInternalMediaDir(@NonNull File file) {
		return MediaStorageUtils.isInDirectory(getInternalMediaDir(), file);
	}

	/**
	 * Notifies MediaStore about a raw file creation/deletion. MediaStore and SAF targets already
	 * expose their content through content providers and do not need scanning here.
	 */
	public void scanMediaFile(@NonNull File file) {
		if (!isInAppStorage(file)) {
			MediaScannerConnection.scanFile(app, new String[] {file.getAbsolutePath()}, null, null);
		}
	}

	@Nullable
	private String getRelativeAppPath(@NonNull File file) {
		try {
			String appPath = app.getAppPath().getCanonicalPath();
			String filePath = file.getCanonicalPath();
			String prefix = appPath.endsWith(File.separator) ? appPath : appPath + File.separator;
			return filePath.startsWith(prefix) ? filePath.substring(prefix.length()).replace(File.separatorChar, '/') : null;
		} catch (IOException e) {
			log.warn("Failed to resolve app-relative media path: " + file, e);
			return null;
		}
	}

	@Nullable
	private MediaSource resolveInternalMediaSource(@NonNull String href, @NonNull String internalPath, boolean includeNonManagedMedia) {
		File file = MediaProvider.resolveInternalMediaFile(app.getAppPath().getAbsolutePath(), internalPath);
		if (includeNonManagedMedia) {
			return file.isFile() ? new FileMediaSource(this, href, file) : null;
		}
		if (file.exists() && MediaFileNameFormat.isManagedMediaFileName(file.getName())) {
			return new FileMediaSource(this, href, file);
		}
		return null;
	}

	@Nullable
	private MediaSource resolveFileMediaSource(@NonNull MediaStorageLocation location,
			@NonNull String href, @NonNull File file, boolean includeNonManagedMedia) {
		if (includeNonManagedMedia) {
			return file.isFile() ? new FileMediaSource(this, href, file) : null;
		}
		if (!file.exists() || !isManagedMediaFile(location, file)) {
			return null;
		}
		return new FileMediaSource(this, href, file);
	}

	@Nullable
	private MediaSource resolveMediaStoreSource(@NonNull MediaStorageLocation location, @NonNull String href, @NonNull Uri uri) {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || !MEDIA_AUTHORITY.equalsIgnoreCase(uri.getAuthority())) {
			return null;
		}
		MediaInfo info = readMediaInfo(uri, true);
		if (info == null || !isGeneratedMediaName(info.name())) {
			return null;
		}
		MediaDirType dirType = MediaDirType.fromExtension(Algorithms.getFileNameExtension(info.name()));
		String expectedPath = MediaStorageUtils.getMediaStoreRelativePath(location.getStorageType(), dirType);
		if (!Algorithms.stringsEqual(MediaStorageUtils.normalizeRelativePath(info.relativePath()), expectedPath)) {
			return null;
		}
		return new UriMediaSource(app, href, uri, info.name(), info.length(), info.mimeType(), dirType, null);
	}

	@Nullable
	private MediaSource resolveDocumentMediaSource(@NonNull MediaStorageLocation location, @NonNull String href, @NonNull Uri uri) {
		if (!MediaStorageUtils.isInManualTree(location, uri)) {
			return null;
		}
		MediaInfo info = readMediaInfo(uri, false);
		if (info == null || !isGeneratedMediaName(info.name())) {
			return null;
		}
		// Classify by the generated file extension (consistent with resolveMediaStoreSource).
		// The system mime for a .3gp file is video/3gpp, which would otherwise misfile audio notes.
		MediaDirType dirType = MediaDirType.fromExtension(Algorithms.getFileNameExtension(info.name()));
		return new UriMediaSource(app, href, uri, info.name(), info.length(), info.mimeType(), dirType,
				location.getManualUri());
	}

	@Nullable
	private MediaSource resolveContentMediaSource(@NonNull String href, @NonNull Uri uri) {
		MediaInfo info = readMediaInfo(uri, false);
		if (info == null || Algorithms.isEmpty(info.name())) {
			return null;
		}
		MediaDirType dirType = MediaDirType.fromMimeTypeOrExtension(info.mimeType(), Algorithms.getFileNameExtension(info.name()));
		return new UriMediaSource(app, href, uri, info.name(), info.length(), info.mimeType(), dirType, null);
	}

	private static boolean isGeneratedMediaName(@Nullable String name) {
		return !Algorithms.isEmpty(name) && MediaFileNameFormat.isManagedMediaFileName(name);
	}

	private boolean isManagedMediaFile(@NonNull MediaStorageLocation location, @NonNull File file) {
		if (!MediaFileNameFormat.isManagedMediaFileName(file.getName())) {
			return false;
		}
		if (location.getStorageType() == MAIN_STORAGE) {
			return MediaStorageUtils.isInDirectory(getInternalMediaDir(), file);
		}
		if (location.getStorageType() == MANUALLY_SPECIFIED) {
			return false;
		}
		for (MediaDirType dirType : MediaDirType.values()) {
			File dir = MediaStorageUtils.resolveRawMediaDir(location.getStorageType(), dirType, getInternalMediaDir());
			if (MediaStorageUtils.isInDirectory(dir, file)) {
				return true;
			}
		}
		return false;
	}

	@Nullable
	private MediaTarget createMediaStoreTarget(@NonNull MediaStorageType storageType, @NonNull MediaDirType dirType,
	                                           @NonNull String fileName, @Nullable String mimeType) {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
			return null;
		}
		Uri collectionUri = MediaStorageUtils.getMediaStoreCollectionUri(dirType);
		String relativePath = MediaStorageUtils.getMediaStoreRelativePath(storageType, dirType);
		String type = MediaStorageUtils.getMimeType(mimeType, fileName, dirType);

		if (dirType == MediaDirType.AUDIO && "audio/3gpp".equals(type) && fileName.endsWith(".3gp")) {
			fileName = Algorithms.getFileNameWithoutExtension(fileName) + ".3ga";
		}
		return new MediaStoreMediaTarget(this, collectionUri, relativePath, fileName, type);
	}

	@Nullable
	private MediaTarget createDocumentTarget(@NonNull MediaStorageLocation location, @NonNull MediaDirType dirType,
	                                         @NonNull String fileName, @Nullable String mimeType) {
		DocumentFile root = getManualRootDocument(location);
		if (root == null || !root.canWrite()) {
			return null;
		}
		DocumentFile dir = MediaStorageUtils.getOrCreateDirectory(root, dirType.getDirName());
		if (dir == null || !dir.canWrite()) {
			return null;
		}
		return new DocumentMediaTarget(app, dir, fileName, MediaStorageUtils.getMimeType(mimeType, fileName, dirType));
	}

	@Nullable
	private DocumentFile getManualRootDocument(@NonNull MediaStorageLocation location) {
		Uri treeUri = location.getManualUri();
		return treeUri == null ? null : DocumentFile.fromTreeUri(app, treeUri);
	}

	@Nullable
	private MediaInfo readMediaInfo(@NonNull Uri uri, boolean includeRelativePath) {
		List<String> columns = new ArrayList<>();
		columns.add(OpenableColumns.DISPLAY_NAME);
		columns.add(OpenableColumns.SIZE);
		if (includeRelativePath) {
			columns.add(MediaStore.MediaColumns.MIME_TYPE);
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
				columns.add(MediaStore.MediaColumns.RELATIVE_PATH);
			}
		}
		try (Cursor cursor = app.getContentResolver().query(uri, columns.toArray(new String[0]), null, null, null)) {
			if (cursor != null && cursor.moveToFirst()) {
				String name = MediaStorageUtils.getString(cursor, OpenableColumns.DISPLAY_NAME);
				long length = MediaStorageUtils.getLong(cursor, OpenableColumns.SIZE);
				String mimeType = includeRelativePath ? MediaStorageUtils.getString(cursor, MediaStore.MediaColumns.MIME_TYPE) : null;
				String relativePath = includeRelativePath && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
						? MediaStorageUtils.getString(cursor, MediaStore.MediaColumns.RELATIVE_PATH)
						: null;
				if (Algorithms.isEmpty(mimeType)) {
					mimeType = app.getContentResolver().getType(uri);
				}
				return new MediaInfo(name, Math.max(length, 0), mimeType, relativePath);
			}
		} catch (Exception e) {
			log.warn("Failed to read media info: " + uri, e);
		}
		return null;
	}

	boolean mediaStoreFileExists(@NonNull Uri collectionUri, @NonNull String relativePath, @NonNull String fileName) {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
			return false;
		}
		String expectedRelativePath = MediaStorageUtils.normalizeRelativePath(relativePath);
		String selection = MediaStore.MediaColumns.DISPLAY_NAME + "=?";
		String[] args = {fileName};
		try (Cursor cursor = app.getContentResolver().query(collectionUri,
				new String[] {MediaStore.MediaColumns._ID, MediaStore.MediaColumns.RELATIVE_PATH}, selection, args, null)) {
			while (cursor != null && cursor.moveToNext()) {
				if (Algorithms.stringsEqual(MediaStorageUtils.normalizeRelativePath(MediaStorageUtils.getString(cursor, MediaStore.MediaColumns.RELATIVE_PATH)),
						expectedRelativePath)) {
					return true;
				}
			}
		} catch (Exception e) {
			log.warn("Failed to query media store target: " + fileName, e);
		}
		return false;
	}

	private boolean isInAppStorage(@NonNull File file) {
		return getRelativeAppPath(file) != null;
	}

	private record MediaInfo(@Nullable String name, long length, @Nullable String mimeType, @Nullable String relativePath) {
	}
}