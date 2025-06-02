package net.osmand.plus.dialogs.selectlocation;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.activities.MapActivity;

public interface ILocationSelectionHandler<ResultType> {
	@Nullable
	Object getCenterPointIcon(@NonNull MapActivity mapActivity);

	@Nullable
	String getCenterPointLabel(@NonNull MapActivity mapActivity);

	void onLocationSelected(@NonNull MapActivity mapActivity, @NonNull ResultType location);

	void onScreenClosed(@NonNull MapActivity mapActivity, boolean selected);

	@NonNull
	String getDialogTitle(@NonNull MapActivity mapActivity);
}
