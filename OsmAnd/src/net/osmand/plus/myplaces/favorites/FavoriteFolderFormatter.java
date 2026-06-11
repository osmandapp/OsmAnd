package net.osmand.plus.myplaces.favorites;

import android.content.Context;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.view.ViewTreeObserver;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.utils.ColorUtilities;
import net.osmand.util.Algorithms;

import java.util.List;

public final class FavoriteFolderFormatter {

	private static final String DISPLAY_DELIMITER = " / ";
	private static final String PATH_ELLIPSIS = "...";

	private FavoriteFolderFormatter() {
	}

	@NonNull
	public static String getDisplayName(@NonNull Context ctx, @Nullable String fullPath) {
		if (isRootOrPersonal(fullPath)) {
			return FavoriteGroup.getDisplayName(ctx, fullPath != null ? fullPath : "");
		}
		return FavoriteFolderPath.lastSegment(fullPath);
	}

	@NonNull
	public static String getBreadcrumb(@NonNull Context ctx, @Nullable String fullPath) {
		if (isRootOrPersonal(fullPath)) {
			return FavoriteGroup.getDisplayName(ctx, fullPath != null ? fullPath : "");
		}
		return getBreadcrumb(fullPath);
	}

	@NonNull
	public static CharSequence getStyledBreadcrumb(@NonNull Context ctx, @Nullable String fullPath,
			boolean nightMode) {
		return styleBreadcrumbDelimiters(ctx, getBreadcrumb(ctx, fullPath), nightMode);
	}

	@NonNull
	public static CharSequence getStyledBreadcrumb(@NonNull Context ctx, @Nullable String fullPath,
			boolean nightMode, @Nullable TextPaint textPaint, int availableWidth) {
		String breadcrumb = getMiddleTruncatedBreadcrumb(ctx, fullPath, textPaint, availableWidth);
		return styleBreadcrumbDelimiters(ctx, breadcrumb, nightMode);
	}

	public static void setupStyledBreadcrumb(@NonNull TextView textView, @Nullable String fullPath,
			boolean nightMode) {
		Context context = textView.getContext();
		textView.setSingleLine(true);
		textView.setMaxLines(1);
		textView.setEllipsize(TextUtils.TruncateAt.MIDDLE);

		Object token = new Object();
		textView.setTag(token);
		int availableWidth = getTextAvailableWidth(textView);
		if (availableWidth > 0) {
			textView.setText(getStyledBreadcrumb(context, fullPath, nightMode, textView.getPaint(), availableWidth));
		} else {
			textView.setText(getStyledBreadcrumb(context, fullPath, nightMode));
			textView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
				@Override
				public boolean onPreDraw() {
					textView.getViewTreeObserver().removeOnPreDrawListener(this);
					if (textView.getTag() == token) {
						textView.setText(getStyledBreadcrumb(context, fullPath, nightMode,
								textView.getPaint(), getTextAvailableWidth(textView)));
					}
					return true;
				}
			});
		}
	}

	@NonNull
	public static String getMiddleTruncatedBreadcrumb(@NonNull Context ctx, @Nullable String fullPath,
			@Nullable TextPaint textPaint, int availableWidth) {
		String fullBreadcrumb = getBreadcrumb(ctx, fullPath);
		if (textPaint == null || availableWidth <= 0 || fits(fullBreadcrumb, textPaint, availableWidth)) {
			return fullBreadcrumb;
		}

		List<String> segments = FavoriteFolderPath.split(fullPath);
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
	private static String getBreadcrumb(@Nullable String fullPath) {
		if (Algorithms.isEmpty(fullPath)) {
			return "";
		}
		if (isNormalizedPath(fullPath)) {
			return fullPath.replace(FavoriteFolderPath.DELIMITER, DISPLAY_DELIMITER);
		}
		StringBuilder builder = new StringBuilder();
		for (String segment : FavoriteFolderPath.split(fullPath)) {
			appendBreadcrumbSegment(builder, segment);
		}
		return builder.toString();
	}

	@NonNull
	private static String buildMiddleTruncatedBreadcrumb(@NonNull List<String> segments, int leadingCount,
			int trailingCount) {
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

	private static int getTextAvailableWidth(@NonNull TextView textView) {
		int width = textView.getWidth() > 0 ? textView.getWidth() : textView.getMeasuredWidth();
		if (width > 0) {
			width -= textView.getCompoundPaddingLeft() + textView.getCompoundPaddingRight();
		}
		return Math.max(0, width);
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

	private static boolean isRootOrPersonal(@Nullable String fullPath) {
		return Algorithms.isEmpty(fullPath) || FavoriteGroup.PERSONAL_CATEGORY.equals(fullPath);
	}

	private static boolean isNormalizedPath(@NonNull String fullPath) {
		return !fullPath.startsWith(FavoriteFolderPath.DELIMITER)
				&& !fullPath.endsWith(FavoriteFolderPath.DELIMITER)
				&& !fullPath.contains(FavoriteFolderPath.DELIMITER + FavoriteFolderPath.DELIMITER);
	}
}
