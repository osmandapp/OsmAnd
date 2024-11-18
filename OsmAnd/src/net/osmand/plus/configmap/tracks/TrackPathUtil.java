package net.osmand.plus.configmap.tracks;

import androidx.annotation.NonNull;

import net.osmand.IndexConstants;
import net.osmand.shared.io.KFile;

import java.io.File;

public class TrackPathUtil {

	@NonNull
	public static String getRelativePath(@NonNull KFile file) {
		return getRelativePath(file.absolutePath());
	}

	@NonNull
	public static String getRelativePath(@NonNull File file) {
		return getRelativePath(file.getAbsolutePath());
	}

	@NonNull
	public static String getRelativePath(@NonNull String absolutePath) {
		int startIndex = absolutePath.indexOf(IndexConstants.GPX_INDEX_DIR);
		return absolutePath.substring(startIndex);
	}

}
