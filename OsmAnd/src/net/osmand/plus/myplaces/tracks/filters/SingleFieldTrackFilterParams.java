package net.osmand.plus.myplaces.tracks.filters;

import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;

public class SingleFieldTrackFilterParams {
	boolean hasSelectAllVariant() {
		return false;
	}

	String getItemText(OsmandApplication app, String itemName) {
		return itemName;
	}

	@Nullable
	Drawable getItemIcon(OsmandApplication app, String itemName) {
		return null;
	}

	@Nullable
	Drawable getAllItemsIcon(OsmandApplication app, boolean isChecked, boolean nightMode) {
		return null;
	}

	@NonNull
	String trackParamToString(@NonNull Object trackParam) {
		return trackParam.toString();
	}

	public boolean includeEmptyValues() {
		return false;
	}

	public boolean sortByName() {
		return false;
	}

	public boolean sortDescending() {
		return true;
	}
}