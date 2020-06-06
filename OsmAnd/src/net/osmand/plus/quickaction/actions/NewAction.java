package net.osmand.plus.quickaction.actions;

import android.view.ViewGroup;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.quickaction.AddQuickActionDialog;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.QuickActionType;

public class NewAction extends QuickAction {

	public static final QuickActionType TYPE = new QuickActionType(1, "new", NewAction.class)
			.iconRes(R.drawable.ic_action_plus).nameRes(R.string.quick_action_new_action);


	public NewAction() {
		super(TYPE);
	}

	public NewAction(QuickAction quickAction) {
		super(quickAction);
	}

	@Override
	public void execute(MapActivity activity) {
		AddQuickActionDialog.showInstance(activity.getSupportFragmentManager(), true);
	}

	@Override
	public void drawUI(ViewGroup parent, MapActivity activity) {

	}
}
