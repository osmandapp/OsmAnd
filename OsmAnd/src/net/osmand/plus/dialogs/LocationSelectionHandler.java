package net.osmand.plus.dialogs;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.data.LatLon;

public interface LocationSelectionHandler {
	@Nullable
	Object getCenterPointIcon();

	void onLocationSelected(@NonNull LatLon latLon);

	@NonNull
	String getDialogTitle();
}
