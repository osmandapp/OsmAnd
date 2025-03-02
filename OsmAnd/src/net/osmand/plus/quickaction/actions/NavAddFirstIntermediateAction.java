package net.osmand.plus.quickaction.actions;

import static net.osmand.plus.quickaction.QuickActionIds.NAV_ADD_FIRST_INTERMEDIATE_ACTION_ID;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.data.LatLon;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.QuickActionType;

public class NavAddFirstIntermediateAction extends SelectMapLocationAction {

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
	protected void onLocationSelected(@NonNull MapActivity mapActivity, @NonNull LatLon latLon) {
		mapActivity.getMapActions().addFirstIntermediate(latLon);
	}

	@Override
	@Nullable
	protected Object getLocationIcon(@NonNull MapActivity mapActivity) {
		return mapActivity.getMapLayers().getNavigationLayer().getIntermediatePointIcon();
	}

	@NonNull
	@Override
	protected CharSequence getQuickActionDescription(@NonNull Context context) {
		return context.getString(R.string.quick_action_add_first_intermediate_desc);
	}
}
