package net.osmand.plus.myplaces.favorites;

import android.content.Context;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.utils.ColorUtilities;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;

public final class FavoriteFolderPath {

	public static final String DELIMITER = "/";
	public static final String DISPLAY_DELIMITER = " / ";
	private static final String PATH_ELLIPSIS = "...";

	private FavoriteFolderPath() {
	}

	@NonNull
	public static List<String> split(@Nullable String fullPath) {
		List<String> segments = new ArrayList<>();
		if (!Algorithms.isEmpty(fullPath)) {
			for (String segment : fullPath.split(DELIMITER)) {
				if (!Algorithms.isEmpty(segment)) {
					segments.add(segment);
				}
			}
		}
		return segments;
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
		List<String> segments = split(fullPath);
		if (segments.size() <= 1) {
			return "";
		}
		return join(segments.subList(0, segments.size() - 1));
	}

	@NonNull
	public static String lastSegment(@Nullable String fullPath) {
		List<String> segments = split(fullPath);
		return segments.isEmpty() ? "" : segments.get(segments.size() - 1);
	}

	@NonNull
	public static String lastSegment(@NonNull Context ctx, @Nullable String fullPath) {
		if (Algorithms.isEmpty(fullPath) || FavoriteGroup.PERSONAL_CATEGORY.equals(fullPath)) {
			return FavoriteGroup.getDisplayName(ctx, fullPath != null ? fullPath : "");
		}
		return lastSegment(fullPath);
	}

	@NonNull
	public static String toBreadcrumb(@Nullable String fullPath) {
		List<String> segments = split(fullPath);
		StringBuilder builder = new StringBuilder();
		for (String segment : segments) {
			if (builder.length() > 0) {
				builder.append(DISPLAY_DELIMITER);
			}
			builder.append(segment);
		}
		return builder.toString();
	}

	@NonNull
	public static String toBreadcrumb(@NonNull Context ctx, @Nullable String fullPath) {
		if (Algorithms.isEmpty(fullPath) || FavoriteGroup.PERSONAL_CATEGORY.equals(fullPath)) {
			return FavoriteGroup.getDisplayName(ctx, fullPath != null ? fullPath : "");
		}
		return toBreadcrumb(fullPath);
	}

	@NonNull
	public static CharSequence toStyledBreadcrumb(@NonNull Context ctx, @Nullable String fullPath, boolean nightMode) {
		return styleBreadcrumbDelimiters(ctx, toBreadcrumb(ctx, fullPath), nightMode);
	}

	@NonNull
	public static CharSequence toStyledBreadcrumb(@NonNull Context ctx, @Nullable String fullPath,
			boolean nightMode, @Nullable TextPaint textPaint, int availableWidth) {
		String breadcrumb = getMiddleTruncatedBreadcrumb(ctx, fullPath, textPaint, availableWidth);
		return styleBreadcrumbDelimiters(ctx, breadcrumb, nightMode);
	}

	@NonNull
	public static String getMiddleTruncatedBreadcrumb(@NonNull Context ctx, @Nullable String fullPath,
			@Nullable TextPaint textPaint, int availableWidth) {
		String fullBreadcrumb = toBreadcrumb(ctx, fullPath);
		if (textPaint == null || availableWidth <= 0 || fits(fullBreadcrumb, textPaint, availableWidth)) {
			return fullBreadcrumb;
		}

		List<String> segments = split(fullPath);
		if (segments.size() <= 2) {
			return TextUtils.ellipsize(fullBreadcrumb, textPaint, availableWidth, TextUtils.TruncateAt.MIDDLE).toString();
		}

		String best = buildMiddleTruncatedBreadcrumb(segments, 1, 1);
		for (int visibleSegments = 3; visibleSegments < segments.size(); visibleSegments++) {
			int leadingCount = (visibleSegments + 1) / 2;
			int trailingCount = visibleSegments - leadingCount;
			String candidate = buildMiddleTruncatedBreadcrumb(segments, leadingCount, trailingCount);
			if (fits(candidate, textPaint, availableWidth)) {
				best = candidate;
			} else {
				break;
			}
		}
		return fits(best, textPaint, availableWidth)
				? best
				: TextUtils.ellipsize(best, textPaint, availableWidth, TextUtils.TruncateAt.MIDDLE).toString();
	}

	@NonNull
	private static String buildMiddleTruncatedBreadcrumb(@NonNull List<String> segments, int leadingCount, int trailingCount) {
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < leadingCount; i++) {
			appendBreadcrumbSegment(builder, segments.get(i));
		}
		appendBreadcrumbSegment(builder, PATH_ELLIPSIS);
		for (int i = segments.size() - trailingCount; i < segments.size(); i++) {
			appendBreadcrumbSegment(builder, segments.get(i));
		}
		return builder.toString();
	}

	private static void appendBreadcrumbSegment(@NonNull StringBuilder builder, @NonNull String segment) {
		if (builder.length() > 0) {
			builder.append(DISPLAY_DELIMITER);
		}
		builder.append(segment);
	}

	private static boolean fits(@NonNull String text, @NonNull TextPaint textPaint, int availableWidth) {
		return textPaint.measureText(text) <= availableWidth;
	}

	@NonNull
	private static SpannableStringBuilder styleBreadcrumbDelimiters(@NonNull Context ctx,
			@NonNull String breadcrumb, boolean nightMode) {
		SpannableStringBuilder builder = new SpannableStringBuilder(breadcrumb);
		int color = ColorUtilities.getTertiaryTextColor(ctx, nightMode);
		int start = breadcrumb.indexOf(DISPLAY_DELIMITER);
		while (start >= 0) {
			builder.setSpan(new ForegroundColorSpan(color), start, start + DISPLAY_DELIMITER.length(),
					Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			start = breadcrumb.indexOf(DISPLAY_DELIMITER, start + DISPLAY_DELIMITER.length());
		}
		return builder;
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
		for (String segment : fullPath.split(DELIMITER, -1)) {
			if (!isValidSegment(segment)) {
				return false;
			}
		}
		return true;
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
