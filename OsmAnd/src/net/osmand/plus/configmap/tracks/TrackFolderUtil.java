package net.osmand.plus.configmap.tracks;

import androidx.annotation.NonNull;

import net.osmand.IndexConstants;
import net.osmand.shared.gpx.data.SmartFolder;
import net.osmand.shared.io.KFile;
import net.osmand.util.CollectionUtils;

import java.io.File;

public class TrackFolderUtil {

	@NonNull
	public static String getTrackFolderId(@NonNull KFile file) {
		return getTrackFolderId(file.absolutePath());
	}

	@NonNull
	public static String getTrackFolderId(@NonNull File file) {
		return getTrackFolderId(file.getAbsolutePath());
	}

	@NonNull
	public static String getTrackFolderId(@NonNull String absolutePath) {
		int startIndex = absolutePath.indexOf(IndexConstants.GPX_INDEX_DIR);
		return absolutePath.substring(startIndex);
	}

	@NonNull
	public static String getOutdatedStandardFolderId(@NonNull String id) {
		int index = id.lastIndexOf(File.separator);
		return index > 0 ? id.substring(index + 1) : id;
	}

	public static boolean isOutdatedStandardFolderId(@NonNull String id) {
		return !isPredefinedTabId(id) && !isSmartFolderId(id) && !isStandardFolderId(id);
	}

	public static boolean isPredefinedTabId(@NonNull String id) {
		return CollectionUtils.equalsToAny(id, TrackTabType.ON_MAP.name(), TrackTabType.ALL.name());
	}

	public static boolean isSmartFolderId(@NonNull String id) {
		return id.startsWith(SmartFolder.ID_PREFIX);
	}

	public static boolean isStandardFolderId(@NonNull String id) {
		return id.startsWith(IndexConstants.GPX_INDEX_DIR);
	}
}
