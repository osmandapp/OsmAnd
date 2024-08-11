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
import java.util.Arrays;
import java.util.List;

public enum LocationIcon {

	STATIC_DEFAULT(R.drawable.map_location_default, R.drawable.map_location_default_view_angle, "model_map_default_location"),
	STATIC_CAR(R.drawable.map_location_car, R.drawable.map_location_car_view_angle, "model_map_car_location"),
	STATIC_BICYCLE(R.drawable.map_location_bicycle, R.drawable.map_location_bicycle_view_angle, "model_map_bicycle_location"),
	MOVEMENT_DEFAULT(R.drawable.map_navigation_default, "model_map_car_bearing"),
	MOVEMENT_NAUTICAL(R.drawable.map_navigation_nautical, "model_map_navigation_nautical"),
	MOVEMENT_CAR(R.drawable.map_navigation_car, "model_map_navigation_car"),
	MODEL();

	@DrawableRes
	private final int iconId;
	@DrawableRes
	private final int headingIconId;
	private String represented3DModelKey;

	LocationIcon(@DrawableRes int iconId, @DrawableRes int headingIconId, @NonNull String represented3DModelKey) {
		this.iconId = iconId;
		this.headingIconId = headingIconId;
		this.represented3DModelKey = represented3DModelKey;
	}

	LocationIcon(@DrawableRes int iconId, @NonNull String represented3DModelKey) {
		this.iconId = iconId;
		this.headingIconId = R.drawable.map_location_default_view_angle;
		this.represented3DModelKey = represented3DModelKey;
	}

	LocationIcon() {
		this.iconId = R.drawable.map_location_default;
		this.headingIconId = R.drawable.map_location_default_view_angle;
	}

	@DrawableRes
	public int getIconId() {
		return iconId;
	}

	public String getRepresented3DModelKey() {
		return represented3DModelKey;
	}

	@DrawableRes
	public int getHeadingIconId() {
		return headingIconId;
	}

	public boolean isOriginallyUsedForNavigation() {
		return CollectionUtils.equalsToAny(this, MOVEMENT_DEFAULT, MOVEMENT_NAUTICAL, MOVEMENT_CAR);
	}

	public static boolean isDefaultModel(@NonNull String name) {
		return getIconForDefaultModel(name) != null;
	}

	@Nullable
	public static String getIconForDefaultModel(@NonNull String modelName){
		if (!isModel(modelName)) {
			return null;
		}
		for (LocationIcon icon : getDefaultIcons()) {
			if (modelName.equals(icon.represented3DModelKey)) {
				return icon.name();
			}
		}
		return null;
	}

	public static boolean isModel(@NonNull String name) {
		return name.startsWith(IndexConstants.MODEL_NAME_PREFIX);
	}

	public static boolean isModelRepresented(@NonNull String name) {
		for (LocationIcon icon : getDefaultIcons()) {
			if (icon.name().equals(name)) {
				return true;
			}
		}
		return false;
	}

	private static List<LocationIcon> getDefaultIcons(){
		return Arrays.asList(STATIC_DEFAULT, STATIC_CAR, STATIC_BICYCLE, MOVEMENT_DEFAULT, MOVEMENT_CAR, MOVEMENT_NAUTICAL);
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
		boolean navLoc = staticLocation != null && !staticLocation;
		try {
			for (LocationIcon l : LocationIcon.values()) {
				if (navLoc && l.name().equalsIgnoreCase("MOVEMENT_" + name)) {
					return l;
				} else if (!navLoc && l.name().equalsIgnoreCase("STATIC_" + name)) {
					return l;
				}
			}
			return valueOf(name);
		} catch (IllegalArgumentException e) {
			return !navLoc ? STATIC_DEFAULT : MOVEMENT_DEFAULT;
		}
	}

	@NonNull
	public static String getActualIconName(@NonNull String name, boolean nav) {
		for (LocationIcon l : LocationIcon.values()) {
			if (nav && l.name().equalsIgnoreCase("MOVEMENT_" + name)) {
				return l.name();
			} else if (!nav && l.name().equalsIgnoreCase("STATIC_" + name)) {
				return l.name();
			}
		}
		return name;
	}


}