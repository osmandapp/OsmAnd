package net.osmand.plus.quickaction.actions;

import static net.osmand.plus.quickaction.QuickActionIds.NAV_ADD_FIRST_INTERMEDIATE_ACTION_ID;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;

import net.osmand.data.LatLon;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.QuickActionType;

public class NavAddFirstIntermediateAction extends QuickAction {

	public static final QuickActionType TYPE = new QuickActionType(NAV_ADD_FIRST_INTERMEDIATE_ACTION_ID,
			"nav.intermediate.add", NavAddFirstIntermediateAction.class).
			nameRes(R.string.quick_action_first_intermediate).iconRes(R.drawable.ic_action_intermediate).nonEditable().
			category(QuickActionType.NAVIGATION).nameActionRes(R.string.shared_string_add);

	public NavAddFirstIntermediateAction() {
		super(TYPE);
	}

	public NavAddFirstIntermediateAction(QuickAction quickAction) {
		super(quickAction);
	}

	@Override
	public void execute(@NonNull MapActivity mapActivity) {
		LatLon latLon = getMapLocation(mapActivity);
		mapActivity.getMapLayers().getMapActionsHelper().addFirstIntermediate(latLon);
	}

	@Override
	public void drawUI(@NonNull ViewGroup parent, @NonNull MapActivity mapActivity) {

		View view = LayoutInflater.from(parent.getContext())
				.inflate(R.layout.quick_action_with_text, parent, false);

		((TextView) view.findViewById(R.id.text)).setText(
				R.string.quick_action_add_first_intermediate_desc);

		parent.addView(view);
	}
}
