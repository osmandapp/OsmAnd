package net.osmand.plus.quickaction.actions;

import static net.osmand.plus.quickaction.QuickActionIds.SHOW_HIDE_DRAWER_ACTION;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.QuickActionType;

public class ShowHideDrawerAction extends QuickAction {

	public static final QuickActionType TYPE = new QuickActionType(SHOW_HIDE_DRAWER_ACTION,
			"drawer.showhide", ShowHideDrawerAction.class)
			.nameActionRes(R.string.quick_action_verb_show_hide)
			.nameRes(R.string.quick_action_drawer_title)
			.iconRes(R.drawable.ic_action_drawer)
			.category(QuickActionType.INTERFACE)
			.nonEditable();

	public ShowHideDrawerAction() {
		super(TYPE);
	}

	public ShowHideDrawerAction(QuickAction quickAction) {
		super(quickAction);
	}

	@Override
	public void execute(@NonNull MapActivity mapActivity) {
		// event.getRepeatCount() repeat count 0 doesn't work for samsung, 1 doesn't work for lg
		mapActivity.toggleDrawer();
	}

	@Override
	public void drawUI(@NonNull ViewGroup parent, @NonNull MapActivity mapActivity) {
		View view = LayoutInflater.from(parent.getContext())
				.inflate(R.layout.quick_action_with_text, parent, false);
		((TextView) view.findViewById(R.id.text))
				.setText(R.string.quick_action_drawer_desc);
		parent.addView(view);
	}

}
