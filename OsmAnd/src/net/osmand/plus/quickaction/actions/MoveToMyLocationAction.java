package net.osmand.plus.quickaction.actions;

import static net.osmand.plus.quickaction.QuickActionIds.MOVE_TO_MY_LOCATION_ACTION;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.QuickActionType;
import net.osmand.plus.utils.UiUtilities;

public class MoveToMyLocationAction extends QuickAction {

	public static final QuickActionType TYPE = new QuickActionType(MOVE_TO_MY_LOCATION_ACTION,
			"map.move.my_location", MoveToMyLocationAction.class)
			.nameRes(R.string.quick_action_to_my_location)
			.iconRes(R.drawable.ic_action_my_location).nonEditable()
			.category(QuickActionType.MAP_INTERACTIONS)
			.nameActionRes(R.string.shared_string_move);

	public MoveToMyLocationAction() {
		super(TYPE);
	}

	public MoveToMyLocationAction(QuickAction quickAction) {
		super(quickAction);
	}

	@Override
	public void execute(@NonNull MapActivity mapActivity, @Nullable Bundle params) {
		mapActivity.getMapViewTrackingUtilities().backToLocationImpl();
	}

	@Override
	public void drawUI(@NonNull ViewGroup parent, @NonNull MapActivity mapActivity, boolean nightMode) {
		View view = UiUtilities.inflate(parent.getContext(), nightMode, R.layout.quick_action_with_text, parent, false);
		((TextView) view.findViewById(R.id.text)).setText(mapActivity.getString(R.string.key_event_action_move_to_my_location));
		parent.addView(view);
	}
}
