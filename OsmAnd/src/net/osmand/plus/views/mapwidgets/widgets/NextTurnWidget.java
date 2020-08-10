package net.osmand.plus.views.mapwidgets.widgets;

import android.app.Activity;

import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.routing.RouteCalculationResult;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.views.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.mapwidgets.RouteInfoWidgetsFactory;
import net.osmand.plus.views.mapwidgets.TurnDrawable;
import net.osmand.router.TurnType;

public class NextTurnWidget extends TextInfoWidget {

	protected boolean horizontalMini;

	protected int deviatedPath = 0;
	protected int nextTurnDistance = 0;

	private TurnDrawable turnDrawable;
	private OsmandApplication app;

	public NextTurnWidget(Activity activity, OsmandApplication app, boolean horizontalMini) {
		super(activity);
		this.app = app;
		this.horizontalMini = horizontalMini;
		turnDrawable = new TurnDrawable(activity, horizontalMini);
		if (horizontalMini) {
			setImageDrawable(turnDrawable, false);
			setTopImageDrawable(null, null);
		} else {
			setImageDrawable(null, true);
			setTopImageDrawable(turnDrawable, "");
		}
	}

	public TurnType getTurnType() {
		return turnDrawable.getTurnType();
	}

	public void setTurnType(TurnType turnType) {
		boolean vis = updateVisibility(turnType != null);
		if (turnDrawable.setTurnType(turnType) || vis) {
			turnDrawable.setTextPaint(topTextView.getPaint());
			if (horizontalMini) {
				setImageDrawable(turnDrawable, false);
			} else {
				setTopImageDrawable(turnDrawable, "");
//				setTopImageDrawable(turnDrawable, turnType == null || turnType.getExitOut() == 0 ? "" : 
//					turnType.getExitOut() + "");
			}
		}
	}

	public void setTurnImminent(int turnImminent, boolean deviatedFromRoute) {
		if (turnDrawable.getTurnImminent() != turnImminent || turnDrawable.isDeviatedFromRoute() != deviatedFromRoute) {
			turnDrawable.setTurnImminent(turnImminent, deviatedFromRoute);
		}
	}

	public void setDeviatePath(int deviatePath) {
		if (RouteInfoWidgetsFactory.distChanged(deviatePath, this.deviatedPath)) {
			this.deviatedPath = deviatePath;
			updateDistance();
		}
	}

	public void setTurnDistance(int nextTurnDistance) {
		if (RouteInfoWidgetsFactory.distChanged(nextTurnDistance, this.nextTurnDistance)) {
			this.nextTurnDistance = nextTurnDistance;
			updateDistance();
		}
	}

	private void updateDistance() {
		int deviatePath = turnDrawable.isDeviatedFromRoute() ? deviatedPath : nextTurnDistance;
		String ds = OsmAndFormatter.getFormattedDistance(deviatePath, app);

		TurnType turnType = getTurnType();
		RoutingHelper routingHelper = app.getRoutingHelper();
		if ((turnType != null) && (routingHelper != null)) {
			setContentDescription(ds + " " + RouteCalculationResult.toString(turnType, app, false));
		} else {
			setContentDescription(ds);
		}

		int ls = ds.lastIndexOf(' ');
		if (ls == -1) {
			setTextNoUpdateVisibility(ds, null);
		} else {
			setTextNoUpdateVisibility(ds.substring(0, ls), ds.substring(ls + 1));
		}
	}

	@Override
	public boolean updateInfo(DrawSettings drawSettings) {
		return false;
	}
}