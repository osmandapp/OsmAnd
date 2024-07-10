package net.osmand.plus.profiles;

import android.graphics.drawable.Drawable;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;

import net.osmand.IndexConstants;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.util.CollectionUtils;

import java.io.File;

public enum LocationIcon {

	DEFAULT(R.drawable.map_location_default, R.drawable.map_location_default_view_angle),
	CAR(R.drawable.map_location_car, R.drawable.map_location_car_view_angle),
	BICYCLE(R.drawable.map_location_bicycle, R.drawable.map_location_bicycle_view_angle),
	MOVEMENT_DEFAULT(R.drawable.map_navigation_default),
	MOVEMENT_NAUTICAL(R.drawable.map_navigation_nautical),
	MOVEMENT_CAR(R.drawable.map_navigation_car),
	MODEL();

	@DrawableRes
	private final int iconId;
	@DrawableRes
	private final int headingIconId;

	LocationIcon(@DrawableRes int iconId, @DrawableRes int headingIconId) {
		this.iconId = iconId;
		this.headingIconId = headingIconId;
	}

	LocationIcon(@DrawableRes int iconId) {
		this.iconId = iconId;
		this.headingIconId = R.drawable.map_location_default_view_angle;
	}

	LocationIcon() {
		this.iconId = R.drawable.map_location_default;
		this.headingIconId = R.drawable.map_location_default_view_angle;
	}

	@DrawableRes
	public int getIconId() {
		return iconId;
	}

	@DrawableRes
	public int getHeadingIconId() {
		return headingIconId;
	}

	public boolean isOriginallyUsedForNavigation() {
		return CollectionUtils.equalsToAny(this, MOVEMENT_DEFAULT, MOVEMENT_NAUTICAL, MOVEMENT_CAR);
	}

	public static boolean isModel(@NonNull String name) {
		return name.startsWith(IndexConstants.MODEL_NAME_PREFIX);
	}

	public static Drawable getDrawable(OsmandApplication ctx, @NonNull String name) {
		return getDrawable(ctx, name, null);
	}

	public static Drawable getDrawable(OsmandApplication ctx, @NonNull String name, @Nullable Boolean staticLocation) {
		Drawable mp = getModelPreviewDrawable(ctx, name);
		if (mp != null) {
			return mp;
		}
		return AppCompatResources.getDrawable(ctx, fromName(name, staticLocation).getIconId());
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

	@NonNull
	public static LocationIcon fromName(@NonNull String name) {
		return fromName(name, null);
	}

	@NonNull
	public static LocationIcon fromName(@NonNull String name, @Nullable Boolean staticLocation) {
		if (isModel(name)) {
			return MODEL;
		}
		try {
			if (staticLocation != null && !staticLocation) {
				LocationIcon icon = findNavigationIconByPreviousName(name);
				if (icon != null) {
					return icon;
				}
			}
			return valueOf(name);
		} catch (IllegalArgumentException e) {
			return staticLocation == null || staticLocation ? DEFAULT : MOVEMENT_DEFAULT;
		}
	}

	@NonNull
	public static String getActualNavigationIconName(@NonNull String name) {
		LocationIcon newIcon = findNavigationIconByPreviousName(name);
		return newIcon != null ? newIcon.name() : name;
	}

	@Nullable
	public static LocationIcon findNavigationIconByPreviousName(@NonNull String name) {
		try {
			return valueOf("MOVEMENT_" + name);
		} catch (IllegalArgumentException e) {
			return null;
		}
	}
}