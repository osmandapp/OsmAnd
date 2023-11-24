package net.osmand.data;

import android.content.Context;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.myplaces.favorites.FavoriteGroup;
import net.osmand.plus.plugins.parking.ParkingPositionPlugin;
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

	public String getName() {
		return typeName;
	}

	public String getCategory() {
		return FavoriteGroup.PERSONAL_CATEGORY;
	}

	public int getIconId(@NonNull Context ctx) {
		if (this == PARKING) {
			OsmandApplication app = (OsmandApplication) ctx.getApplicationContext();
			OsmandPreference<?> parkingType = app.getSettings().getPreference(ParkingPositionPlugin.PARKING_TYPE);
			if (parkingType instanceof BooleanPreference && ((BooleanPreference) parkingType).get()) {
				return R.drawable.mx_special_parking_time_limited;
			}
			return iconId;
		}
		return iconId;
	}

	public String getHumanString(@NonNull Context ctx) {
		return ctx.getString(resId);
	}
}
