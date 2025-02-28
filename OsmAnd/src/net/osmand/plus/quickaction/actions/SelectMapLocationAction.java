package net.osmand.plus.quickaction.actions;

import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.OnResultCallback;
import net.osmand.data.LatLon;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.dialogs.SelectLocationController;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.QuickActionType;

public abstract class SelectMapLocationAction extends QuickAction {

	public SelectMapLocationAction(@NonNull QuickActionType type) {
		super(type);
	}

	public SelectMapLocationAction(@NonNull QuickAction quickAction) {
		super(quickAction);
	}

	@Override
	public void execute(@NonNull MapActivity mapActivity) {
		requestLocation(mapActivity, latLon -> onLocationSelected(mapActivity, latLon));
	}

	private void requestLocation(@NonNull MapActivity mapActivity,
	                             @NonNull OnResultCallback<LatLon> callback) {
		if (shouldSelectLocationManually()) {
			SelectLocationController.showDialog(mapActivity, () -> getLocationIcon(mapActivity), callback);
		} else {
			callback.onResult(getMapLocation(mapActivity));
		}
	}

	protected abstract void onLocationSelected(@NonNull MapActivity mapActivity, @NonNull LatLon latLon);

	@Nullable
	protected abstract Object getLocationIcon(@NonNull MapActivity mapActivity);

	protected boolean shouldSelectLocationManually() {
		// TODO: Implement preference-based selection logic
		return true;
	}

	@Override
	public void drawUI(@NonNull ViewGroup parent, @NonNull MapActivity mapActivity) {
		// TODO: Implement UI for "select location manually" preference
	}
}
