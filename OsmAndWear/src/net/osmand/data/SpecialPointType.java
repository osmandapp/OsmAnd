package net.osmand.data;

import static net.osmand.plus.myplaces.favorites.FavoriteGroup.PERSONAL_CATEGORY;
import static net.osmand.plus.plugins.parking.ParkingPositionPlugin.PARKING_TYPE;

import android.content.Context;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.settings.backend.preferences.BooleanPreference;
import net.osmand.plus.settings.backend.preferences.OsmandPreference;

public enum SpecialPointType {

	HOME("home", R.string.home_button, R.drawable.mx_special_house),
	WORK("work", R.string.work_button, R.drawable.mx_special_building),
	PARKING("parking", R.string.osmand_parking_position_name, R.drawable.mx_parking);

	private final String typeName;
	@StringRes
	private final int resId;
	@DrawableRes
	private final int iconId;

	SpecialPointType(@NonNull String typeName, @StringRes int resId, @DrawableRes int iconId) {
		this.typeName = typeName;
		this.resId = resId;
		this.iconId = iconId;
	}

	@NonNull
	public String getName() {
		return typeName;
	}

	@NonNull
	public String getCategory() {
		return PERSONAL_CATEGORY;
	}

	@DrawableRes
	public int getIconId(@NonNull Context ctx) {
		if (this == PARKING) {
			OsmandApplication app = (OsmandApplication) ctx.getApplicationContext();
			OsmandPreference<?> preference = app.getSettings().getPreference(PARKING_TYPE);
			if (preference instanceof BooleanPreference && ((BooleanPreference) preference).get()) {
				return R.drawable.mx_special_parking_time_limited;
			}
			return iconId;
		}
		return iconId;
	}

	@NonNull
	public String getHumanString(@NonNull Context ctx) {
		return ctx.getString(resId);
	}

	@Nullable
	public static SpecialPointType getByName(@NonNull String name) {
		for (SpecialPointType type : values()) {
			if (type.getName().equalsIgnoreCase(name)) {
				return type;
			}
		}
		return null;
	}
}
