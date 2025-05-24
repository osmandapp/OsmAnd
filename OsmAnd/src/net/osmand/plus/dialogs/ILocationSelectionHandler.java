package net.osmand.plus.dialogs;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.activities.MapActivity;

public interface ILocationSelectionHandler {
	@Nullable
	Object getCenterPointIcon(@NonNull MapActivity mapActivity);

	void onApplySelection(@NonNull MapActivity mapActivity);

	void onScreenClosed(@NonNull MapActivity mapActivity);

	@NonNull
	String getDialogTitle(@NonNull MapActivity mapActivity);
}
