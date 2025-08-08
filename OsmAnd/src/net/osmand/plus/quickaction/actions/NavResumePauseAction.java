package net.osmand.plus.quickaction.actions;

import static net.osmand.plus.quickaction.QuickActionIds.NAV_RESUME_PAUSE_ACTION_ID;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.QuickActionType;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.UiUtilities;

public class NavResumePauseAction extends QuickAction {

	public static final QuickActionType TYPE = new QuickActionType(NAV_RESUME_PAUSE_ACTION_ID,
			"nav.resumepause", NavResumePauseAction.class)
			.nameRes(R.string.shared_string_navigation).iconRes(R.drawable.ic_play_dark).nonEditable()
			.category(QuickActionType.NAVIGATION).nameActionRes(R.string.quick_action_verb_pause_resume);


	public NavResumePauseAction() {
		super(TYPE);
	}

	public NavResumePauseAction(QuickAction quickAction) {
		super(quickAction);
	}

	@Override
	public void execute(@NonNull MapActivity mapActivity, @Nullable Bundle params) {
		RoutingHelper routingHelper = mapActivity.getRoutingHelper();
		if (routingHelper.isRoutePlanningMode()) {
			routingHelper.resumeNavigation();
		} else {
			routingHelper.pauseNavigation();
		}
		AndroidUtils.requestNotificationPermissionIfNeeded(mapActivity);
		mapActivity.getMapViewTrackingUtilities().switchRoutePlanningMode();
		mapActivity.refreshMap();
	}

	@Override
	public void drawUI(@NonNull ViewGroup parent, @NonNull MapActivity mapActivity, boolean nightMode) {
		View view = UiUtilities.inflate(parent.getContext(), nightMode, R.layout.quick_action_with_text, parent, false);

		((TextView) view.findViewById(R.id.text)).setText(R.string.quick_action_resume_pause_navigation_descr);

		parent.addView(view);
	}

	@Override
	public String getActionText(@NonNull OsmandApplication app) {
		RoutingHelper helper = app.getRoutingHelper();
		if (!helper.isRouteCalculated() || helper.isRoutePlanningMode()) {
			return app.getString(R.string.continue_navigation);
		}
		return app.getString(R.string.pause_navigation);
	}

	@Override
	public int getIconRes(Context context) {
		OsmandApplication app = (OsmandApplication) context.getApplicationContext();
		RoutingHelper helper = app.getRoutingHelper();
		if (!helper.isRouteCalculated() || helper.isRoutePlanningMode()) {
			return R.drawable.ic_play_dark;
		}
		return R.drawable.ic_pause;
	}

	@Override
	public boolean isActionEnable(OsmandApplication app) {
		return app.getRoutingHelper().isRouteCalculated();
	}
}