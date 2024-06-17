package net.osmand.plus.profiles;

import android.content.Context;
import android.graphics.drawable.Drawable;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;

import net.osmand.IndexConstants;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;

import java.io.File;

public enum NavigationIcon {

	DEFAULT(R.drawable.map_navigation_default),
	NAUTICAL(R.drawable.map_navigation_nautical),
	CAR(R.drawable.map_navigation_car),
	MODEL(R.drawable.map_navigation_default);

	NavigationIcon(@DrawableRes int iconId) {
		this.iconId = iconId;
	}

	@DrawableRes
	private final int iconId;

	@DrawableRes
	public int getIconId() {
		return iconId;
	}

	public static boolean isModel(@NonNull String name) {
		return name.startsWith(IndexConstants.MODEL_NAME_PREFIX);
	}

	public static Drawable getModelPreviewDrawable(OsmandApplication ctx, @NonNull String name) {
		if (isModel(name)) {
			String iconName = name.substring(IndexConstants.MODEL_NAME_PREFIX.length());
			File iconFile = new File(ctx.getAppPath(IndexConstants.MODEL_3D_DIR), iconName + "/" + iconName + ".png");
			if (iconFile.exists()) {
				return Drawable.createFromPath(iconFile.getAbsolutePath());
			}
		}
		return null;
	}

	public static Drawable getDrawable(OsmandApplication ctx, @NonNull String name) {
		Drawable mp = getModelPreviewDrawable(ctx, name);
		if (mp != null) {
			return mp;
		}
		return AppCompatResources.getDrawable(ctx, fromName(name).getIconId());
	}

	@NonNull
	public static NavigationIcon fromName(@NonNull String name) {
		if (isModel(name)) {
			return MODEL;
		}
		try {
			return valueOf(name);
		} catch (IllegalArgumentException e) {
			return DEFAULT;
		}
	}
}