package net.osmand.data;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import net.osmand.plus.R;

public enum BackgroundType {

	CIRCLE("circle", R.string.shared_string_circle, R.drawable.bg_point_circle),
	OCTAGON("octagon", R.string.shared_string_octagon, R.drawable.bg_point_octagon),
	SQUARE("square", R.string.shared_string_square, R.drawable.bg_point_square),
	COMMENT("comment", R.string.poi_dialog_comment, R.drawable.bg_point_comment);

	private final String typeName;
	@StringRes
	private final int nameId;
	@DrawableRes
	private final int iconId;

	BackgroundType(@NonNull String typeName, @StringRes int nameId, @DrawableRes int iconId) {
		this.typeName = typeName;
		this.nameId = nameId;
		this.iconId = iconId;
	}

	@StringRes
	public int getNameId() {
		return nameId;
	}

	@DrawableRes
	public int getIconId() {
		return iconId;
	}

	@NonNull
	public String getTypeName() {
		return typeName;
	}

	public static BackgroundType getByTypeName(String typeName, BackgroundType defaultValue) {
		for (BackgroundType type : values()) {
			if (type.typeName.equals(typeName)) {
				return type;
			}
		}
		return defaultValue;
	}

	public boolean isSelected() {
		return this != COMMENT;
	}

	public int getOffsetY(@NonNull Context ctx, float textScale) {
		return this == COMMENT ? Math.round(ctx.getResources()
				.getDimensionPixelSize(R.dimen.point_background_comment_offset_y) * textScale) : 0;
	}

	public Bitmap getTouchBackground(@NonNull Context ctx, boolean isSmall) {
		return getMapBackgroundIconId(ctx, "center", isSmall);
	}

	public Bitmap getMapBackgroundIconId(@NonNull Context ctx, String layer, boolean isSmall) {
		Resources res = ctx.getResources();
		String iconName = res.getResourceEntryName(getIconId());
		String suffix = isSmall ? "_small" : "";
		return BitmapFactory.decodeResource(res, res.getIdentifier("ic_" + iconName + "_" + layer + suffix,
				"drawable", ctx.getPackageName()));
	}
}
