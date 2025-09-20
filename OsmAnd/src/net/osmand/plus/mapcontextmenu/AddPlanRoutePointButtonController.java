package net.osmand.plus.mapcontextmenu;

import androidx.annotation.NonNull;

import net.osmand.data.LatLon;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.measurementtool.MeasurementToolFragment;

public class AddPlanRoutePointButtonController extends BottomButtonController {

	public AddPlanRoutePointButtonController(@NonNull MenuController controller) {
		super(controller, R.drawable.ic_action_plus, R.string.coord_input_add_point);
	}

	@Override
	public void buttonPressed() {
		MapActivity mapActivity = controller.getMapActivity();
		if (mapActivity != null) {
			LatLon latLon = controller.getLatLon();
			MeasurementToolFragment fragment = mapActivity.getFragmentsHelper().getMeasurementToolFragment();
			if (fragment != null && latLon != null) {
				fragment.addPoint(latLon);
			}
			mapActivity.getContextMenu().hide();
		}
	}
}
