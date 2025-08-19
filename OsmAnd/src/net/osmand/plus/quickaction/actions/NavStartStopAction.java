package net.osmand.plus.quickaction.actions;

import static net.osmand.plus.quickaction.QuickActionIds.NAV_START_STOP_ACTION_ID;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.other.DestinationReachedFragment;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.QuickActionType;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.utils.UiUtilities;

public class NavStartStopAction extends QuickAction {

	private static final String KEY_DIALOG = "dialog";
	public static final QuickActionType TYPE = new QuickActionType(NAV_START_STOP_ACTION_ID,
			"nav.startstop", NavStartStopAction.class)
			.nameRes(R.string.shared_string_navigation).iconRes(R.drawable.ic_action_start_navigation).nonEditable()
			.category(QuickActionType.NAVIGATION)
			.nameActionRes(R.string.quick_action_verb_start_stop);

	public NavStartStopAction() {
		super(TYPE);
	}

	public NavStartStopAction(QuickAction quickAction) {
		super(quickAction);
	}

	@Override
	public void execute(@NonNull MapActivity mapActivity, @Nullable Bundle params) {
		RoutingHelper helper = mapActivity.getRoutingHelper();
		if (helper.isPauseNavigation() || helper.isFollowingMode()) {
			if (Boolean.parseBoolean(getParams().get(KEY_DIALOG))) {
				DestinationReachedFragment.show(mapActivity);
			} else {
				mapActivity.getMapActions().stopNavigation();
			}
		} else {
			mapActivity.getMapActions().doRoute();
		}
	}

	@Override
	public void drawUI(@NonNull ViewGroup parent, @NonNull MapActivity mapActivity, boolean nightMode) {
		View view = UiUtilities.inflate(parent.getContext(), nightMode, R.layout.quick_action_start_stop_navigation, parent, false);

		SwitchCompat showDialogSwitch = view.findViewById(R.id.show_dialog_switch);

		if (!getParams().isEmpty()) {
			showDialogSwitch.setChecked(Boolean.valueOf(getParams().get(KEY_DIALOG)));
		}

		view.findViewById(R.id.show_dialog_row).setOnClickListener(v ->
				showDialogSwitch.setChecked(!showDialogSwitch.isChecked()));

		parent.addView(view);
	}

	@Override
	public boolean fillParams(@NonNull View root, @NonNull MapActivity mapActivity) {
		getParams().put(KEY_DIALOG, Boolean
				.toString(((SwitchCompat) root.findViewById(R.id.show_dialog_switch)).isChecked()));
		return true;
	}

	@Override
	public String getActionText(@NonNull OsmandApplication app) {
		RoutingHelper helper = app.getRoutingHelper();
		if (helper.isPauseNavigation() || helper.isFollowingMode()) {
			return app.getString(R.string.cancel_navigation);
		}
		return app.getString(R.string.follow);
	}

	@Override
	public int getIconRes(Context context) {
		OsmandApplication app = (OsmandApplication) context.getApplicationContext();
		RoutingHelper helper = app.getRoutingHelper();

		if (!helper.isRoutePlanningMode() && !helper.isFollowingMode() && context instanceof MapActivity activity) {
			return activity.getMapActions().getRouteMode().getIconRes();
		} else if (helper.isPauseNavigation() || helper.isFollowingMode() || helper.isRoutePlanningMode()) {
			return helper.getAppMode().getIconRes();
		} else {
			return app.getSettings().getApplicationMode().getIconRes();
		}
	}

	@Override
	public boolean isActionWithSlash(@NonNull OsmandApplication app) {
		RoutingHelper helper = app.getRoutingHelper();
		return helper.isPauseNavigation() || helper.isFollowingMode();
	}
}