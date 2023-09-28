package net.osmand.plus.quickaction.actions;

import static net.osmand.plus.quickaction.QuickActionIds.NAV_RESUME_PAUSE_ACTION_ID;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.QuickActionType;
import net.osmand.plus.routing.RoutingHelper;

public class NavResumePauseAction extends QuickAction {

	public static final QuickActionType TYPE = new QuickActionType(NAV_RESUME_PAUSE_ACTION_ID,
			"nav.resumepause", NavResumePauseAction .class).
			nameRes(R.string.quick_action_resume_pause_navigation).iconRes(R.drawable.ic_play_dark).nonEditable().
			category(QuickActionType.NAVIGATION);


	public NavResumePauseAction() {
		super(TYPE);
	}

	public NavResumePauseAction(QuickAction quickAction) {
		super(quickAction);
	}

	@Override
	public void execute(@NonNull MapActivity mapActivity) {
		RoutingHelper routingHelper = mapActivity.getRoutingHelper();
		if (routingHelper.isRoutePlanningMode()) {
			routingHelper.setRoutePlanningMode(false);
			routingHelper.setFollowingMode(true);
			routingHelper.setCurrentLocation(mapActivity.getMyApplication().getLocationProvider().getLastKnownLocation(), false);
		} else {
			routingHelper.setRoutePlanningMode(true);
			routingHelper.setFollowingMode(false);
			routingHelper.setPauseNavigation(true);
		}
		mapActivity.getMapViewTrackingUtilities().switchRoutePlanningMode();
		mapActivity.refreshMap();
	}

	@Override
	public void drawUI(@NonNull ViewGroup parent, @NonNull MapActivity mapActivity) {
		View view = LayoutInflater.from(parent.getContext())
				.inflate(R.layout.quick_action_with_text, parent, false);

		((TextView) view.findViewById(R.id.text)).setText(R.string.quick_action_resume_pause_navigation_descr);

		parent.addView(view);
	}

	@Override
	public String getActionText(OsmandApplication app) {
		RoutingHelper helper = app.getRoutingHelper();
		if (!helper.isRouteCalculated() || helper.isRoutePlanningMode()) {
			return app.getString(R.string.continue_navigation);
		}
		return app.getString(R.string.pause_navigation);
	}

	@Override
	public int getIconRes(Context context) {
		if (context instanceof MapActivity) {
			RoutingHelper helper = ((MapActivity) context).getRoutingHelper();
			if (!helper.isRouteCalculated() || helper.isRoutePlanningMode()) {
				return R.drawable.ic_play_dark;
			}
			return R.drawable.ic_pause;
		}
		return super.getIconRes(context);
	}

	@Override
	public boolean isActionEnable(OsmandApplication app) {
		return app.getRoutingHelper().isRouteCalculated();
	}
}