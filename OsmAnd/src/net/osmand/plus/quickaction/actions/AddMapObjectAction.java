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
import net.osmand.plus.views.PointImageDrawable;

public abstract class AddMapObjectAction extends QuickAction {

	public AddMapObjectAction(@NonNull QuickActionType type) {
		super(type);
	}

	public AddMapObjectAction(@NonNull QuickAction quickAction) {
		super(quickAction);
	}

	@Override
	public void execute(@NonNull MapActivity mapActivity) {
		requestLocation(mapActivity, latLon -> addMapObject(mapActivity, latLon));
	}

	private void requestLocation(@NonNull MapActivity mapActivity,
	                             @NonNull OnResultCallback<LatLon> callback) {
		if (shouldSelectLocationManually()) {
			SelectLocationController.showDialog(mapActivity, this::getMapObjectDrawable, callback);
		} else {
			callback.onResult(getMapLocation(mapActivity));
		}
	}

	protected abstract void addMapObject(@NonNull MapActivity mapActivity, @NonNull LatLon latLon);

	@Nullable
	protected abstract PointImageDrawable getMapObjectDrawable();

	protected boolean shouldSelectLocationManually() {
		//TODO: check preference instead of using hardcoded value
		return true;
	}

	@Override
	public void drawUI(@NonNull ViewGroup parent, @NonNull MapActivity mapActivity) {
		//TODO: extract UI for "select point" preference here
//		View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.quick_action_add_object, parent, false);
//		parent.addView(view);
	}
}
