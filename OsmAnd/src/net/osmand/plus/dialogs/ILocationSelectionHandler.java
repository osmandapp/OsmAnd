package net.osmand.plus.dialogs;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.data.LatLon;
import net.osmand.plus.activities.MapActivity;

public interface ILocationSelectionHandler {
	@Nullable
	Object getCenterPointIcon(@NonNull MapActivity mapActivity);

	void onLocationSelected(@NonNull MapActivity mapActivity, @NonNull LatLon latLon);

	@NonNull
	String getDialogTitle(@NonNull MapActivity mapActivity);
}
