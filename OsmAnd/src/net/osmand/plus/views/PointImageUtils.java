package net.osmand.plus.views;

import static net.osmand.data.BackgroundType.CIRCLE;
import static net.osmand.data.FavouritePoint.DEFAULT_BACKGROUND_TYPE;
import static net.osmand.data.FavouritePoint.DEFAULT_UI_ICON_ID;

import android.content.Context;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.data.BackgroundType;
import net.osmand.data.FavouritePoint;
import net.osmand.gpx.GPXUtilities.WptPt;

import java.util.TreeMap;

public class PointImageUtils {

	private static final TreeMap<String, PointImageDrawable> DRAWABLE_CACHE = new TreeMap<>();

	@NonNull
	private static PointImageDrawable getOrCreate(@NonNull Context context, @NonNull PointImageInfo info) {
		String uniqueId = context.getResources().getResourceEntryName(info.iconId);
		uniqueId += info.type.name();
		int color = info.color | 0xff000000;
		int hash = (color << 4) + ((info.withShadow ? 1 : 0) << 2) + ((info.synced ? 3 : 0) << 2);
		uniqueId = hash + uniqueId;
		PointImageDrawable drawable = DRAWABLE_CACHE.get(uniqueId);
		if (drawable == null) {
			drawable = new PointImageDrawable(context, info);
			drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
			DRAWABLE_CACHE.put(uniqueId, drawable);
		}
		return drawable;
	}

	@NonNull
	public static PointImageDrawable getOrCreateSyncedIcon(@NonNull Context context, @ColorInt int color, @Nullable FavouritePoint point) {
		return getFromPoint(context, color, true, true, point);
	}

	@NonNull
	public static PointImageDrawable getOrCreateSyncedIcon(@NonNull Context context, @ColorInt int color, @Nullable WptPt point) {
		return getFromPoint(context, color, true, true, point);
	}

	@NonNull
	public static PointImageDrawable getFromPoint(@NonNull Context context, @ColorInt int color,
	                                              boolean withShadow, @Nullable WptPt point) {
		return getFromPoint(context, color, withShadow, false, point);
	}

	@NonNull
	public static PointImageDrawable getFromPoint(@NonNull Context context, @ColorInt int color,
	                                              boolean withShadow, boolean synced, @Nullable WptPt point) {
		if (point != null) {
			int overlayIconId = context.getResources().getIdentifier("mx_" + point.getIconNameOrDefault(),
					"drawable", context.getPackageName());
			return getOrCreate(context, color, withShadow, synced, overlayIconId,
					BackgroundType.getByTypeName(point.getBackgroundType(), DEFAULT_BACKGROUND_TYPE));
		} else {
			return getOrCreate(context, color, withShadow);
		}
	}

	@NonNull
	public static PointImageDrawable getFromPoint(@NonNull Context context, @ColorInt int color,
	                                              boolean withShadow, @Nullable FavouritePoint point) {
		return getFromPoint(context, color, withShadow, false, point);
	}

	@NonNull
	public static PointImageDrawable getFromPoint(@NonNull Context context, @ColorInt int color,
	                                              boolean withShadow, boolean synced,
	                                              @Nullable FavouritePoint point) {
		if (point != null) {
			return getOrCreate(context, color, withShadow, synced, point.getOverlayIconId(context),
					point.getBackgroundType());
		} else {
			return getOrCreate(context, color, withShadow);
		}
	}

	@NonNull
	public static PointImageDrawable getOrCreate(@NonNull Context context, @ColorInt int color, boolean withShadow) {
		return getOrCreate(context, color, withShadow, DEFAULT_UI_ICON_ID);
	}

	@NonNull
	public static PointImageDrawable getOrCreate(@NonNull Context context, @ColorInt int color,
	                                             boolean withShadow, @DrawableRes int iconId) {
		return getOrCreate(context, color, withShadow, false, iconId, CIRCLE);
	}

	@NonNull
	public static PointImageDrawable getOrCreate(@NonNull Context context, @ColorInt int color,
	                                             boolean withShadow, boolean synced,
	                                             @DrawableRes int iconId, @NonNull BackgroundType type) {
		iconId = iconId == 0 ? DEFAULT_UI_ICON_ID : iconId;
		return getOrCreate(context, new PointImageInfo(type, color, iconId, synced, withShadow));
	}

	public static class PointImageInfo {

		@ColorInt
		protected final int color;
		@DrawableRes
		protected final int iconId;
		protected final boolean synced;
		protected final boolean withShadow;
		protected final BackgroundType type;

		private PointImageInfo(@NonNull BackgroundType type, @ColorInt int color,
		                       @DrawableRes int iconId, boolean synced, boolean withShadow) {
			this.iconId = iconId;
			this.color = color;
			this.synced = synced;
			this.withShadow = withShadow;
			this.type = type;
		}
	}
}