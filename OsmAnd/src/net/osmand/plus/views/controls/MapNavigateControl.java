package net.osmand.plus.views.controls;

import net.osmand.data.RotatedTileBox;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.routing.RouteProvider.GPXRouteParams;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.views.OsmandMapLayer.DrawSettings;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;

public class MapNavigateControl extends MapControls {
	private Button navigateButton;
	private MapRouteInfoControl ri;
	
	
	public MapNavigateControl(MapRouteInfoControl ri,  MapActivity mapActivity, Handler showUIHandler, float scaleCoefficient) {
		super(mapActivity, showUIHandler, scaleCoefficient);
		this.ri = ri;
	}
	
	@Override
	public void showControls(final FrameLayout parent) {
		navigateButton = addButton(parent, R.string.get_directions, R.drawable.map_btn_navigate);
		navigateButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				OsmandApplication app = mapActivity.getMyApplication();
				RoutingHelper routingHelper = app.getRoutingHelper();
				if(routingHelper.isFollowingMode()) {
					routingHelper.setRoutePlanningMode(false);
					mapActivity.getMapViewTrackingUtilities().switchToRoutePlanningMode();
				} else {
					OsmandApplication ctx = mapActivity.getMyApplication();
					if(!ctx.getTargetPointsHelper().checkPointToNavigateShort()) {
						ri.showDialog();
					} else {
						mapActivity.getMapViewTrackingUtilities().backToLocationImpl();
						GPXRouteParams gpxRoute = null; // TODO gpx route
						if (gpxRoute == null) {
							app.getSettings().FOLLOW_THE_GPX_ROUTE.set(null);
						}
						app.getSettings().FOLLOW_THE_ROUTE.set(true);
						routingHelper.setFollowingMode(true);
						routingHelper.setRoutePlanningMode(false);
						mapActivity.getMapViewTrackingUtilities().switchToRoutePlanningMode();
						routingHelper.setCurrentLocation(app.getLocationProvider().getLastKnownLocation(), false);
						app.initVoiceCommandPlayer(mapActivity);
					}
				}
			}
		});
	}

	@Override
	public void hideControls(FrameLayout layout) {
		removeButton(layout, navigateButton);
	}

	@Override
	public void onDraw(Canvas canvas, RotatedTileBox tileBox, DrawSettings nightMode) {
	}
	
	public int getWidth() {
		if (width == 0) {
			Drawable buttonDrawable = mapActivity.getResources().getDrawable(R.drawable.map_btn_navigate);
			width = buttonDrawable.getMinimumWidth();
		}
		return width ;
	}
}
