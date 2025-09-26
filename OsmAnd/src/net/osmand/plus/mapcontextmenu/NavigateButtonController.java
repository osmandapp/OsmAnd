package net.osmand.plus.mapcontextmenu;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.settings.backend.ApplicationMode;

public class NavigateButtonController extends BottomButtonController {

	public NavigateButtonController(@NonNull MenuController controller) {
		super(controller, getIconId(controller), R.string.shared_string_navigation);
	}

	@Override
	public void buttonPressed() {
		navigateButtonPressed();
	}

	private void navigateButtonPressed() {
		MapActivity mapActivity = controller.getMapActivity();
		if (mapActivity != null) {
			if (controller.navigateInPedestrianMode()) {
				mapActivity.getSettings().setApplicationMode(ApplicationMode.PEDESTRIAN, false);
			}
			mapActivity.getMapActions().navigateButton();
		}
	}

	@DrawableRes
	private static int getIconId(@NonNull MenuController controller) {
		return controller.navigateInPedestrianMode()
				? R.drawable.ic_action_pedestrian_dark
				: R.drawable.ic_action_gdirections_dark;
	}
}
