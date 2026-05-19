package net.osmand.plus.myplaces.favorites;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;

public final class FavoriteFolderPath {

	public static final String DELIMITER = "/";
	private static final char DELIMITER_CHAR = '/';

	private FavoriteFolderPath() {
	}

	@NonNull
	public static List<String> split(@Nullable String fullPath) {
		List<String> segments = new ArrayList<>();
		if (!Algorithms.isEmpty(fullPath)) {
			int start = 0;
			int delimiterIndex;
			while ((delimiterIndex = fullPath.indexOf(DELIMITER_CHAR, start)) >= 0) {
				addSegment(segments, fullPath.substring(start, delimiterIndex));
				start = delimiterIndex + 1;
			}
			addSegment(segments, fullPath.substring(start));
		}
		return segments;
	}

	private static void addSegment(@NonNull List<String> segments, @NonNull String segment) {
		if (!Algorithms.isEmpty(segment)) {
			segments.add(segment);
		}
	}

	@NonNull
	public static String join(@NonNull List<String> segments) {
		StringBuilder builder = new StringBuilder();
		for (String segment : segments) {
			if (Algorithms.isEmpty(segment)) {
				continue;
			}
			if (builder.length() > 0) {
				builder.append(DELIMITER);
			}
			builder.append(segment);
		}
		return builder.toString();
	}

	@NonNull
	public static String parentPath(@Nullable String fullPath) {
		if (Algorithms.isEmpty(fullPath)) {
			return "";
		}
		int end = trimTrailingDelimiters(fullPath);
		if (end <= 0) {
			return "";
		}
		int delimiterIndex = fullPath.lastIndexOf(DELIMITER_CHAR, end - 1);
		if (delimiterIndex < 0) {
			return "";
		}
		String parentPath = fullPath.substring(0, delimiterIndex);
		return isNormalizedPath(parentPath) ? parentPath : join(split(parentPath));
	}

	@NonNull
	public static String lastSegment(@Nullable String fullPath) {
		if (Algorithms.isEmpty(fullPath)) {
			return "";
		}
		int end = trimTrailingDelimiters(fullPath);
		if (end <= 0) {
			return "";
		}
		int delimiterIndex = fullPath.lastIndexOf(DELIMITER_CHAR, end - 1);
		return fullPath.substring(delimiterIndex + 1, end);
	}

	public static boolean isDescendantOrSelf(@Nullable String path, @Nullable String ancestorPath) {
		String normalizedPath = path != null ? path : "";
		String normalizedAncestor = ancestorPath != null ? ancestorPath : "";
		return Algorithms.isEmpty(normalizedAncestor)
				|| Algorithms.stringsEqual(normalizedPath, normalizedAncestor)
				|| normalizedPath.startsWith(normalizedAncestor + DELIMITER);
	}

	@NonNull
	public static String replacePathPrefix(@NonNull String path, @NonNull String oldPrefix, @NonNull String newPrefix) {
		if (!isDescendantOrSelf(path, oldPrefix)) {
			return path;
		}
		if (Algorithms.stringsEqual(path, oldPrefix)) {
			return newPrefix;
		}
		String suffix = Algorithms.isEmpty(oldPrefix) ? path : path.substring(oldPrefix.length() + DELIMITER.length());
		if (Algorithms.isEmpty(newPrefix)) {
			return suffix;
		}
		return newPrefix + DELIMITER + suffix;
	}

	public static boolean isValidFullPath(@Nullable String fullPath) {
		if (fullPath == null) {
			return false;
		}
		if (fullPath.isEmpty()) {
			return true;
		}
		int start = 0;
		int delimiterIndex;
		while ((delimiterIndex = fullPath.indexOf(DELIMITER_CHAR, start)) >= 0) {
			if (!isValidSegment(fullPath.substring(start, delimiterIndex))) {
				return false;
			}
			start = delimiterIndex + 1;
		}
		return isValidSegment(fullPath.substring(start));
	}

	private static int trimTrailingDelimiters(@NonNull String fullPath) {
		int end = fullPath.length();
		while (end > 0 && fullPath.charAt(end - 1) == DELIMITER_CHAR) {
			end--;
		}
		return end;
	}

	private static boolean isNormalizedPath(@NonNull String fullPath) {
		return fullPath.isEmpty()
				|| (!fullPath.startsWith(DELIMITER)
				&& !fullPath.endsWith(DELIMITER)
				&& !fullPath.contains(DELIMITER + DELIMITER));
	}

	public static boolean isValidSegment(@Nullable String segment) {
		return !Algorithms.isEmpty(segment)
				&& !segment.contains(DELIMITER)
				&& !segment.contains(FavouritesFileHelper.SUBFOLDER_PLACEHOLDER);
	}

	public static void requireValidFullPath(@NonNull String fullPath) {
		if (!isValidFullPath(fullPath)) {
			throw new IllegalArgumentException("Invalid favorite folder path: " + fullPath);
		}
	}
}
