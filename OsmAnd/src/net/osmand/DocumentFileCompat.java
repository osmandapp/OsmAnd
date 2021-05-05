package net.osmand;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.documentfile.provider.DocumentFile;

import net.osmand.util.Algorithms;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DocumentFileCompat {

	public static final String PRIMARY = "primary";
	public static final String MIME_TYPE_UNKNOWN = "*/*";
	public static final String MIME_TYPE_BINARY_FILE = "application/octet-stream";
	public static final String EXTERNAL_STORAGE_AUTHORITY = "com.android.externalstorage.documents";
	public static final String DOWNLOADS_FOLDER_AUTHORITY = "com.android.providers.downloads.documents";
	public static final String MEDIA_FOLDER_AUTHORITY = "com.android.providers.media.documents";

	/**
	 * Only available on API 26 to 29.
	 */
	public static final String DOWNLOADS_TREE_URI = "content://" + DOWNLOADS_FOLDER_AUTHORITY + "/tree/downloads";

	private final DocumentFile doc;

	public enum DocumentFileType {
		ANY,
		FILE,
		FOLDER
	}

	@RequiresApi(api = Build.VERSION_CODES.KITKAT)
	public enum PublicDirectory {
		DOWNLOADS(Environment.DIRECTORY_DOWNLOADS),
		MUSIC(Environment.DIRECTORY_MUSIC),
		PODCASTS(Environment.DIRECTORY_PODCASTS),
		RINGTONES(Environment.DIRECTORY_RINGTONES),
		ALARMS(Environment.DIRECTORY_ALARMS),
		NOTIFICATIONS(Environment.DIRECTORY_NOTIFICATIONS),
		PICTURES(Environment.DIRECTORY_PICTURES),
		MOVIES(Environment.DIRECTORY_MOVIES),
		DCIM(Environment.DIRECTORY_DCIM),
		DOCUMENTS(Environment.DIRECTORY_DOCUMENTS);

		private final String folderName;

		PublicDirectory(String folderName) {
			this.folderName = folderName;
		}

		public String getFolderName() {
			return folderName;
		}
	}

	public DocumentFileCompat(@NonNull DocumentFile doc) {
		this.doc = doc;
	}

	public DocumentFileCompat(@NonNull Context ctx, @NonNull String path) throws IllegalArgumentException {
		DocumentFile doc = DocumentFile.fromTreeUri(ctx, Uri.parse(path));
		if (doc == null) {
			throw new IllegalArgumentException("DocumentFile for '" + path + "' is null");
		}
		this.doc = doc;
	}

	@NonNull
	public Uri getUri() {
		return doc.getUri();
	}

	@Nullable
	public String getName() {
		return doc.getName();
	}

	@Nullable
	public DocumentFile getParentFile() {
		return doc.getParentFile();
	}

	public static String getExternalStoragePath() {
		return Environment.getExternalStorageDirectory().getAbsolutePath();
	}

	@Nullable
	public static File toRawFile(@NonNull DocumentFile doc) {
		if (isRawFile(doc)) {
			String path = doc.getUri().getPath();
			return path != null ? new File(path) : null;
		} else if (inPrimaryStorage(doc)) {
			return new File(getExternalStoragePath() + "/" + getBasePath(doc));
		} else if (!Algorithms.isEmpty(getStorageId(doc))) {
			return new File("/storage/" + getStorageId(doc) + "/" + getBasePath(doc));
		} else {
			return null;
		}
	}

	public static boolean isDocumentWritable(@NonNull DocumentFile doc) {
		return isRawFile(doc) ? isFileWritable(toRawFile(doc)) : doc.canWrite();
	}

	public static boolean isFileWritable(@NonNull File file) {
		return file.canWrite()
				&& (file.isFile() || (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || Environment.isExternalStorageManager(file)));
	}

	public static String getFileStorageId(@NonNull File file) {
		return getFileStorageId(file.getPath());
	}

	public static String getFileStorageId(@NonNull String path) {
		if (path.startsWith(getExternalStoragePath())) {
			return DocumentFileCompat.PRIMARY;
		} else {
			int i1 = path.indexOf("/storage/");
			int i2 = path.indexOf("/storage/", 9);
			if (i1 != -1 && i2 != -1) {
				return path.substring(9, i2);
			}
		}
		return null;
	}

	public String getStorageId() {
		return getStorageId(doc);
	}

	public static String getStorageId(@NonNull DocumentFile doc) {
		String path = Algorithms.emptyIfNull(doc.getUri().getPath());
		if (isRawFile(doc)) {
			return getFileStorageId(path);
		} else if (isExternalStorageDocument(doc)) {
			int i1 = path.indexOf(":");
			if (i1 != -1) {
				String s1 = path.substring(0, i1);
				int i2 = s1.lastIndexOf('/');
				if (s1.length() > i2 + 1) {
					return s1.substring(i2 + 1);
				}
			}
		} else if (isDownloadsDocument(doc)) {
			return DocumentFileCompat.PRIMARY;
		}
		return "";
	}

	public boolean isRawFile() {
		return isRawFile(doc);
	}

	public boolean isMediaFile() {
		return isMediaFile(doc);
	}

	public boolean isTreeDocumentFile() {
		return isTreeDocumentFile(doc);
	}

	public boolean isExternalStorageDocument() {
		return isExternalStorageDocument(doc);
	}

	public boolean isDownloadsDocument() {
		return isDownloadsDocument(doc);
	}

	public boolean isMediaDocument() {
		return isMediaDocument(doc);
	}

	public boolean inPrimaryStorage() {
		return inPrimaryStorage(doc);
	}

	public static boolean isRawFile(@NonNull DocumentFile doc) {
		return Algorithms.objectEquals(doc.getUri().getScheme(), ContentResolver.SCHEME_FILE);
	}

	public static boolean isMediaFile(@NonNull DocumentFile doc) {
		return Algorithms.objectEquals(doc.getUri().getAuthority(), MediaStore.AUTHORITY);
	}

	public static boolean isTreeDocumentFile(@NonNull DocumentFile doc) {
		String path = doc.getUri().getPath();
		return path != null && path.startsWith("/tree/");
	}

	public static boolean isExternalStorageDocument(@NonNull DocumentFile doc) {
		return Algorithms.objectEquals(doc.getUri().getAuthority(), EXTERNAL_STORAGE_AUTHORITY);
	}

	public static boolean isDownloadsDocument(@NonNull DocumentFile doc) {
		return Algorithms.objectEquals(doc.getUri().getAuthority(), DOWNLOADS_FOLDER_AUTHORITY);
	}

	public static boolean isMediaDocument(@NonNull DocumentFile doc) {
		return Algorithms.objectEquals(doc.getUri().getAuthority(), MEDIA_FOLDER_AUTHORITY);
	}

	public static boolean inPrimaryStorage(@NonNull DocumentFile doc) {
		return isTreeDocumentFile(doc) && Algorithms.objectEquals(getStorageId(doc), PRIMARY)
				|| isRawFile(doc) && Algorithms.emptyIfNull(doc.getUri().getPath()).startsWith(getExternalStoragePath());
	}

	@Nullable
	public static File getRootRawFile(String storageId, boolean requiresWriteAccess) {
		File rootFile = Algorithms.objectEquals(storageId, PRIMARY)
				? Environment.getExternalStorageDirectory() : new File("/storage/" + storageId);
		return rootFile.canRead() && (!requiresWriteAccess || isFileWritable(rootFile)) ? rootFile : null;
	}

	@NonNull
	public static Uri createDocumentUri(String storageId, String basePath) {
		return Uri.parse("content://" + EXTERNAL_STORAGE_AUTHORITY + "/tree/" + Uri.encode(storageId + ":" + basePath));
	}

	@Nullable
	public static DocumentFile getRootDocumentFile(@NonNull Context context, @NonNull String storageId,
												   boolean requiresWriteAccess, boolean considerRawFile) {
		if (considerRawFile) {
			File file = getRootRawFile(storageId, requiresWriteAccess);
			if (file != null) {
				return DocumentFile.fromFile(file);
			} else {
				return DocumentFile.fromTreeUri(context, createDocumentUri(storageId, ""));
			}
		} else {
			return DocumentFile.fromTreeUri(context, createDocumentUri(storageId, ""));
		}
	}

	public static String getFileBasePath(@NonNull File file) {
		String externalStoragePath = getExternalStoragePath();
		String sdCardStoragePath = "/storage/" + getFileStorageId(file);
		String path = file.getPath();
		if (path.startsWith(externalStoragePath)) {
			return Algorithms.trim(path.substring(externalStoragePath.length()), '/');
		} else if (path.startsWith(sdCardStoragePath)) {
			return Algorithms.trim(path.substring(sdCardStoragePath.length()), '/');
		} else {
			return "";
		}
	}

	/**
	 * File path without storage ID, otherwise return empty String if this is the root path
	 */
	public String getBasePath() {
		return getBasePath(doc);
	}

	public static String getBasePath(@NonNull DocumentFile doc) {
		String path = Algorithms.emptyIfNull(doc.getUri().getPath());
		if (isRawFile(doc)) {
			if (!Algorithms.isEmpty(path)) {
				return getFileBasePath(new File(path));
			}
		} else if (isExternalStorageDocument(doc)) {
			if (!Algorithms.isEmpty(path)) {
				String docPath = "/document/" + getStorageId(doc) + ":";
				int i = path.lastIndexOf(docPath);
				if (i != -1) {
					return Algorithms.trim(path.substring(i + docPath.length()), '/');
				}
			}
		} else if (isDownloadsDocument(doc) && isTreeDocumentFile(doc)) {
			// content://com.android.providers.downloads.documents/tree/raw:/storage/emulated/0/Download/Denai/document/raw:/storage/emulated/0/Download/Denai
			// content://com.android.providers.downloads.documents/tree/downloads/document/raw:/storage/emulated/0/Download/Denai
			if (!path.contains("/document/raw:") && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
				List<String> parentTree = new ArrayList<>();
				parentTree.add(Algorithms.emptyIfNull(doc.getName()));
				DocumentFile parent = doc;
				while (parent.getParentFile() != null) {
					parentTree.add(Algorithms.emptyIfNull(parent.getParentFile().getName()));
					parent = parent.getParentFile();
				}
				Collections.reverse(parentTree);
				return Algorithms.trim(TextUtils.join("/", parentTree), '/');
			} else {
				String externalStoragePath = getExternalStoragePath();
				int i = path.lastIndexOf(externalStoragePath);
				if (i != -1) {
					return Algorithms.trim(path.substring(i + externalStoragePath.length()), '/');
				}
			}
		}
		return "";
	}


	/*
	 * SD Card => `/storage/6882-2349/osmand`
	 * External storage => `/storage/emulated/0/osmand`
	 */
	@Nullable
	public File getFile() {
		Uri uri = getUri();
		String scheme = uri.getScheme();
		String path = uri.getPath();
		if (path == null || !path.startsWith("/tree/")) {
			return null;
		} else if (Algorithms.objectEquals(scheme, ContentResolver.SCHEME_FILE)) {
			return new File(path);
		} else if (uri.toString().equals(DOWNLOADS_TREE_URI + "/document/downloads")) {
			return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
		} else if (isDownloadsDocument()) {
			if (!path.contains("/document/raw:") && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
				List<String> parentTree = new ArrayList<>();
				parentTree.add(Algorithms.emptyIfNull(getName()));
				DocumentFile parent = doc;
				while (parent.getParentFile() != null) {
					parentTree.add(Algorithms.emptyIfNull(parent.getParentFile().getName()));
					parent = parent.getParentFile();
				}
				Collections.reverse(parentTree);
				return new File(Algorithms.trimEnd(getExternalStoragePath() + "/" + TextUtils.join("/", parentTree), '/'));
			} else {
				String docRaw = "/document/raw:";
				int i = path.lastIndexOf(docRaw);
				if (i != -1) {
					return new File(Algorithms.trimEnd(path.substring(i + docRaw.length()), '/'));
				}
			}
		} else if (inPrimaryStorage()) {
			return new File(Algorithms.trimEnd(getExternalStoragePath() + "/" + getBasePath(), '/'));
		} else {
			return new File(Algorithms.trimEnd("/storage/" + getStorageId() + "/" + getBasePath(), '/'));
		}
		return null;
	}

	@RequiresApi(api = Build.VERSION_CODES.KITKAT)
	@Nullable
	public static DocumentFile fromFullPath(@NonNull Context context, @NonNull String fullPath) {
		return fromFullPath(context, fullPath, DocumentFileType.ANY, true);
	}

	@RequiresApi(api = Build.VERSION_CODES.KITKAT)
	@Nullable
	public static DocumentFile fromFullPath(@NonNull Context context, @NonNull String fullPath,
											@NonNull DocumentFileType documentType,
											boolean considerRawFile) {
		if (fullPath.startsWith("/")) {
			// absolute path
			return fromFile(context, new File(fullPath), documentType, considerRawFile);
		} else {
			// simple path
			return fromSimplePath(context, Algorithms.substringBefore(fullPath, ":"),
					Algorithms.substringAfter(fullPath, ":"), documentType, considerRawFile);
		}
	}

	@RequiresApi(api = Build.VERSION_CODES.KITKAT)
	@Nullable
	public static DocumentFile fromPublicFolder(@NonNull Context context, @NonNull PublicDirectory type,
												@NonNull String subFile, boolean requiresWriteAccess,
												boolean considerRawFile) {
		File rawFile = Environment.getExternalStoragePublicDirectory(type.folderName);
		if (!Algorithms.isEmpty(subFile)) {
			rawFile = new File(rawFile, subFile);
		}
		if (considerRawFile && rawFile.canRead() && (!requiresWriteAccess || isFileWritable(rawFile))) {
			return DocumentFile.fromFile(rawFile);
		}

		DocumentFile folder;
		if (type == PublicDirectory.DOWNLOADS) {
            /*
            Root path will be                   => content://com.android.providers.downloads.documents/tree/downloads/document/downloads
            Get file/listFiles() will be        => content://com.android.providers.downloads.documents/tree/downloads/document/msf%3A268
            When creating files with makeFile() => content://com.android.providers.downloads.documents/tree/downloads/document/147
            When creating directory  "IKO5"     => content://com.android.providers.downloads.documents/tree/downloads/document/raw%3A%2Fstorage%2Femulated%2F0%2FDownload%2FIKO5

            Seems that com.android.providers.downloads.documents no longer available on SAF's folder selector on API 30+.

            You can create directory with authority com.android.providers.downloads.documents on API 29,
            but unfortunately cannot create file in the directory. So creating directory with this authority is useless.
            Hence, convert it to writable URI with DocumentFile.toWritableDownloadsDocumentFile()
            */
			DocumentFile downloadFolder = DocumentFile.fromTreeUri(context, Uri.parse(DOWNLOADS_TREE_URI));
			if (downloadFolder != null && downloadFolder.canRead()) {
				for (String s : getDirectorySequence(subFile)) {
					DocumentFile directory = downloadFolder.findFile(s);
					if (directory != null && directory.canRead()) {
						downloadFolder = directory;
					}
				}
				folder = downloadFolder;
			} else {
				folder = fromFullPath(context, rawFile.getAbsolutePath(), DocumentFileType.ANY, false);
			}
		} else {
			folder = fromFullPath(context, rawFile.getAbsolutePath(), DocumentFileType.ANY, false);
		}
		return folder != null && folder.canRead() && (!requiresWriteAccess || isDocumentWritable(folder)) ? folder : null;
	}

	@RequiresApi(api = Build.VERSION_CODES.KITKAT)
	@Nullable
	public static DocumentFile fromFile(@NonNull Context context, @NonNull File file,
										@NonNull DocumentFileType documentType,
										boolean considerRawFile) {
		if (considerRawFile && file.canRead()) {
			return documentType == DocumentFileType.FILE && !file.isFile()
					|| documentType == DocumentFileType.FOLDER && !file.isDirectory()
					? null : DocumentFile.fromFile(file);
		} else {
			String basePath = Algorithms.trim(removeForbiddenCharsFromFilename(getFileBasePath(file)), '/');
			DocumentFile doc = exploreFile(context, getFileStorageId(file), basePath, documentType, considerRawFile);
			return doc != null ? doc : fromSimplePath(context, getFileStorageId(file), basePath, documentType, considerRawFile);
		}
	}

	@RequiresApi(api = Build.VERSION_CODES.KITKAT)
	@Nullable
	public static DocumentFile fromSimplePath(@NonNull Context context, @NonNull String storageId,
											  @NonNull String basePath, @NonNull DocumentFileType documentType,
											  boolean considerRawFile) {
		if (basePath.isEmpty()) {
			return getRootDocumentFile(context, storageId, false, considerRawFile);
		} else {
			DocumentFile file = exploreFile(context, storageId, basePath, documentType, considerRawFile);
			if (file == null && basePath.startsWith(Environment.DIRECTORY_DOWNLOADS) && Algorithms.objectEquals(storageId, PRIMARY)) {
				DocumentFile doc = fromPublicFolder(context, PublicDirectory.DOWNLOADS, Algorithms.substringAfter(basePath, "/", ""), false, considerRawFile);
				return doc != null &&
						documentType == DocumentFileType.ANY
						|| documentType == DocumentFileType.FILE && doc.isFile()
						|| documentType == DocumentFileType.FOLDER && doc.isDirectory()
						? doc : null;
			} else {
				return file;
			}
		}
	}

	private static String buildAbsolutePath(String storageId, String basePath) {
		String cleanBasePath = removeForbiddenCharsFromFilename(basePath);
		return Algorithms.objectEquals(storageId, PRIMARY)
				? Algorithms.trimEnd(getExternalStoragePath() + "/" + cleanBasePath, '/')
				: Algorithms.trimEnd("/storage/" + storageId + "/" + cleanBasePath, '/');
	}

	@Nullable
	private static DocumentFile exploreFile(@NonNull Context context, @NonNull String storageId,
											@NonNull String basePath, @NonNull DocumentFileType documentType, boolean considerRawFile) {
		File rawFile = new File(buildAbsolutePath(storageId, basePath));
		if (considerRawFile && rawFile.canRead()) {
			return (documentType == DocumentFileType.ANY || documentType == DocumentFileType.FILE && rawFile.isFile()
					|| documentType == DocumentFileType.FOLDER && rawFile.isDirectory())
					? DocumentFile.fromFile(rawFile) : null;
		}
		DocumentFile file;
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
			DocumentFile current = getRootDocumentFile(context, storageId, false, considerRawFile);
			if (current == null) {
				return null;
			}
			for (String s : getDirectorySequence(basePath)) {
				current = current.findFile(s);
			}
			return current;
		} else {
			List<String> directorySequence = getDirectorySequence(basePath);
			List<String> parentTree = new ArrayList<>(directorySequence.size());
			DocumentFile grantedFile = null;
			// Find granted file tree.
			// For example, /storage/emulated/0/Music may not granted, but /storage/emulated/0/Music/Pop is granted by user.
			while (!Algorithms.isEmpty(directorySequence)) {
				parentTree.add(directorySequence.remove(0));
				String folderTree = TextUtils.join("/", parentTree);
				try {
					grantedFile = DocumentFile.fromTreeUri(context, createDocumentUri(storageId, folderTree));
					if (grantedFile != null && grantedFile.canRead()) {
						break;
					}
				} catch (SecurityException e) {
					// ignore
				}
			}
			if (grantedFile == null || directorySequence.isEmpty()) {
				file = grantedFile;
			} else {
				String fileTree = "/" + TextUtils.join("/", directorySequence);
				file = DocumentFile.fromTreeUri(context, Uri.parse(grantedFile.getUri().toString() + Uri.encode(fileTree)));
			}
		}
		return file != null && file.canRead()
				&& (documentType == DocumentFileType.ANY
				|| documentType == DocumentFileType.FILE && file.isFile()
				|| documentType == DocumentFileType.FOLDER && file.isDirectory())
				? file : null;
	}

	private static List<String> getDirectorySequence(@NonNull String path) {
		List<String> res = new ArrayList<>();
		String[] arr = path.split("/");
		for (String s : arr) {
			if (!Algorithms.isEmpty(s)) {
				res.add(s);
			}
		}
		return res;
	}

	private static String removeForbiddenCharsFromFilename(@NonNull String path) {
		path = path.replaceAll(":", "_");
		return replaceCompletely(path, "//", "/");
	}

	private static String replaceCompletely(@NonNull String path, @NonNull String match, @NonNull String replaceWith) {
		do {
			path = path.replaceAll(match, replaceWith);
		} while (!Algorithms.isEmpty(path) && path.contains(match));
		return path;
	}
}
