package net.osmand.plus.quickaction.actions;

import static net.osmand.plus.quickaction.QuickActionIds.NEW_ACTION_ID;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.quickaction.AddQuickActionDialog;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.QuickActionType;
import net.osmand.plus.views.MapLayers;
import net.osmand.plus.views.controls.maphudbuttons.QuickActionButton;

public class NewAction extends QuickAction {

	public static final QuickActionType TYPE = new QuickActionType(NEW_ACTION_ID, "new", NewAction.class)
			.iconRes(R.drawable.ic_action_plus).nameRes(R.string.quick_action_new_action);

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
			FragmentManager manager = mapActivity.getSupportFragmentManager();
			AddQuickActionDialog.showInstance(manager, selectedButton.getButtonState(), true);
		}
	}
}
