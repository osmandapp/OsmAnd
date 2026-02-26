package net.osmand.plus.quickaction.actions;

import static net.osmand.plus.quickaction.QuickActionIds.NEW_ACTION_ID;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.QuickActionType;
import net.osmand.plus.quickaction.controller.AddQuickActionController;
import net.osmand.plus.views.MapLayers;
import net.osmand.plus.views.controls.maphudbuttons.QuickActionButton;
import net.osmand.plus.views.mapwidgets.configure.buttons.QuickActionButtonState;

public class NewAction extends QuickAction {

	public static final QuickActionType TYPE = new QuickActionType(NEW_ACTION_ID, "new", NewAction.class)
			.iconRes(R.drawable.ic_action_plus).nameRes(R.string.shared_string_action).nameActionRes(R.string.shared_string_add);

	public NewAction() {
		super(TYPE);
	}

	public NewAction(QuickAction quickAction) {
		super(quickAction);
	}

	@Override
	public void execute(@NonNull MapActivity mapActivity) {
		MapLayers mapLayers = mapActivity.getMapLayers();
		QuickActionButton selectedButton = mapLayers.getMapQuickActionLayer().getSelectedButton();
		if (selectedButton != null) {
			OsmandApplication app = mapActivity.getMyApplication();
			FragmentManager manager = mapActivity.getSupportFragmentManager();
			QuickActionButtonState buttonState = selectedButton.getButtonState();
			AddQuickActionController.showAddQuickActionDialog(app, manager, buttonState);
		}
	}
}
