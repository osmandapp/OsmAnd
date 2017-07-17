package net.osmand.plus.quickaction.actions;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.routing.RoutingHelper;

public class NavResumePauseAction extends QuickAction {

	public static final int TYPE = 26;

	public NavResumePauseAction() {
		super(TYPE);
	}

	public NavResumePauseAction(QuickAction quickAction) {
		super(quickAction);
	}

	@Override
	public void execute(MapActivity activity) {
		RoutingHelper routingHelper = activity.getRoutingHelper();
		if (routingHelper.isRoutePlanningMode()) {
			routingHelper.setRoutePlanningMode(false);
			routingHelper.setFollowingMode(true);
		} else {
			routingHelper.setRoutePlanningMode(true);
			routingHelper.setFollowingMode(false);
			routingHelper.setPauseNavigation(true);
		}
		activity.getMapViewTrackingUtilities().switchToRoutePlanningMode();
		activity.refreshMap();
	}

	@Override
	public void drawUI(ViewGroup parent, MapActivity activity) {
		View view = LayoutInflater.from(parent.getContext())
				.inflate(R.layout.quick_action_with_text, parent, false);

		((TextView) view.findViewById(R.id.text)).setText(R.string.quick_action_resume_pause_navigation_descr);

		parent.addView(view);
	}

	@Override
	public String getActionText(OsmandApplication application) {
		RoutingHelper helper = application.getRoutingHelper();
		if (!helper.isRouteCalculated() || helper.isRoutePlanningMode()) {
			return application.getString(R.string.continue_navigation);
		}
		return application.getString(R.string.pause_navigation);
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