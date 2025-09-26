package net.osmand.plus.mapcontextmenu;

import androidx.annotation.NonNull;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;

public class DetailsButtonController extends BottomButtonController {

	public DetailsButtonController(@NonNull MenuController controller) {
		super(controller, 0, R.string.rendering_category_details);
	}

	@Override
	public void buttonPressed() {
		MapActivity mapActivity = controller.getMapActivity();
		if (mapActivity != null) {
			mapActivity.getContextMenu().openMenuHalfScreen();
		}
	}
}
