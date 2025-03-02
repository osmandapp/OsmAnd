package net.osmand.plus.quickaction.actions;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.OnResultCallback;
import net.osmand.data.LatLon;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.card.base.multistate.MultiStateCard;
import net.osmand.plus.dialogs.SelectLocationController;
import net.osmand.plus.quickaction.PointLocationCardController;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.QuickActionType;

public abstract class SelectMapLocationAction extends QuickAction {

	private static final String KEY_SELECT_LOCATION_MANUALLY = "select_location_manually";

	private boolean useManualSelection;

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
		if (isManualLocationSelection()) {
			SelectLocationController.showDialog(mapActivity, () -> getLocationIcon(mapActivity), callback);
		} else {
			OsmandApplication app = mapActivity.getMyApplication();
			callback.onResult(SelectLocationController.getMapCenterCoordinates(app));
		}
	}

	protected abstract void onLocationSelected(@NonNull MapActivity mapActivity, @NonNull LatLon latLon);

	@Nullable
	protected abstract Object getLocationIcon(@NonNull MapActivity mapActivity);

	public boolean isManualLocationSelection() {
		return Boolean.parseBoolean(getParameter(KEY_SELECT_LOCATION_MANUALLY, "true"));
	}

	public void setUseManualSelection(boolean useManualSelection) {
		this.useManualSelection = useManualSelection;
	}

	@Override
	public boolean fillParams(@NonNull View root, @NonNull MapActivity mapActivity) {
		setParameter(KEY_SELECT_LOCATION_MANUALLY, Boolean.toString(useManualSelection));
		return true;
	}

	@NonNull
	protected CharSequence getQuickActionDescription(@NonNull Context context) {
		return "";
	}

	@Override
	public void drawUI(@NonNull ViewGroup parent, @NonNull MapActivity mapActivity) {
		View view = LayoutInflater.from(parent.getContext())
				.inflate(R.layout.quick_action_select_map_location, parent, false);
		setupPointLocationView(view.findViewById(R.id.point_location_container), mapActivity);

		((TextView) view.findViewById(R.id.text)).setText(getQuickActionDescription(mapActivity));
		parent.addView(view);
	}

	protected void setupPointLocationView(@NonNull ViewGroup container, @NonNull MapActivity mapActivity) {
		OsmandApplication app = mapActivity.getMyApplication();
		PointLocationCardController controller = new PointLocationCardController(app, this);
		MultiStateCard card = new MultiStateCard(mapActivity, controller);
		container.addView(card.build());
	}
}
