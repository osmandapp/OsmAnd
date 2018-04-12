package net.osmand.plus.quickaction.actions;

import android.view.ViewGroup;

import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.quickaction.AddQuickActionDialog;
import net.osmand.plus.quickaction.QuickAction;

public class NewAction extends QuickAction {

	public static final int TYPE = 1;

	public NewAction() {
		super(TYPE);
	}

	public NewAction(QuickAction quickAction) {
		super(quickAction);
	}

	@Override
	public void execute(MapActivity activity) {

		AddQuickActionDialog dialog = new AddQuickActionDialog();
		dialog.show(activity.getSupportFragmentManager(), AddQuickActionDialog.TAG);
	}

	@Override
	public void drawUI(ViewGroup parent, MapActivity activity) {

	}
}
