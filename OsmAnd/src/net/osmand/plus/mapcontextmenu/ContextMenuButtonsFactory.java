package net.osmand.plus.mapcontextmenu;

import androidx.annotation.NonNull;

import net.osmand.plus.activities.MapActivity;

public class ContextMenuButtonsFactory {

	private final MenuController controller;

	public ContextMenuButtonsFactory(@NonNull MenuController controller) {
		this.controller = controller;
	}

	@NonNull
	public BottomButtonController createDetailButtonController() {
		return new DetailsButtonController(controller);
	}

	@NonNull
	public BottomButtonController createMainButtonController(@NonNull MapActivity mapActivity) {
		return isInMeasurementMode(mapActivity)
				? new AddPlanRoutePointButtonController(controller)
				: new NavigateButtonController(controller);
	}

	private boolean isInMeasurementMode(@NonNull MapActivity mapActivity) {
		return mapActivity.getMapLayers().getMeasurementToolLayer().isInMeasurementMode();
	}
}
