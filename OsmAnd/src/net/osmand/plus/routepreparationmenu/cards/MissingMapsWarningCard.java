package net.osmand.plus.routepreparationmenu.cards;

import androidx.annotation.NonNull;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.routepreparationmenu.RequiredMapsController;
import net.osmand.plus.widgets.dialogbutton.DialogButton;

public class MissingMapsWarningCard extends MapBaseCard {

	public MissingMapsWarningCard(@NonNull MapActivity mapActivity) {
		super(mapActivity);
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.card_missing_maps_warning;
	}

	@Override
	protected void updateContent() {
		DialogButton dialogButton = view.findViewById(R.id.details_button);
		dialogButton.setOnClickListener(v -> showMissingMapsDialog());
	}

	private void showMissingMapsDialog() {
		// TODO TO THINK CALCULATE_MISSING_MAPS
		// 0 - ignore at all (testing, button pressed)
		// 1 - switch to A* and continue
		// 2 - return error
		app.getRoutingHelper().stopCalculationImmediately(); // TODO it should not restart calculation !!!
		RequiredMapsController.showDialog(getMapActivity());
	}
}
