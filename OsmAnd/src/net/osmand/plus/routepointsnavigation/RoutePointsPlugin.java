package net.osmand.plus.routepointsnavigation;

import android.content.Intent;
import android.graphics.Paint;
import android.view.View;
import net.osmand.plus.*;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.views.MapInfoLayer;
import net.osmand.plus.views.OsmandMapLayer;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.mapwidgets.TextInfoWidget;

import java.io.File;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

/**
 * Created by Barsik on 10.06.2014.
 */
public class RoutePointsPlugin extends OsmandPlugin {

	public static final String ID = "osmand.route.stepsPlugin";

	private static final String VISITED_KEY = "IsVisited";
	private static final String POINT_KEY = "Point";

	private OsmandApplication app;
	private GPXUtilities.GPXFile gpx;
	private GPXUtilities.Route currentRoute;
	private GPXUtilities.WptPt currentPoint;
	private int currentPointPos;
	private RoutePointsLayer routeStepsLayer;
	private List<GPXUtilities.WptPt> pointsList;
	private TextInfoWidget routeStepsControl;

	private int visitedCount;


	public RoutePointsPlugin(OsmandApplication app) {
		ApplicationMode. regWidget("route_steps", (ApplicationMode[]) null);
		this.app = app;
	}

	public void setCurrentPoint(GPXUtilities.WptPt point) {
		currentPoint = point;
		int number = findPointPosition(point);
		currentPointPos = number;
	}

	public void setCurrentPoint(int number) {
		currentPoint = pointsList.get(number);
		currentPointPos = number;
	}

	public List<GPXUtilities.WptPt> getPoints() {
		return currentRoute.points;
	}

	@Override
	public String getId() {
		return ID;
	}

	@Override
	public String getDescription() {
		return "This plugin allows you to view key positions of your route...";
	}

	@Override
	public String getName() {
		return "Tour Point Plugin";
	}

	@Override
	public boolean init(OsmandApplication app) {
		return true;
	}

	public GPXUtilities.WptPt getCurrentPoint(){return currentPoint;}

	public GPXUtilities.GPXFile getGpx(){ return gpx;}

	public void setGpx(GPXUtilities.GPXFile gpx) {
		this.gpx = gpx;
		currentRoute = gpx.routes.get(0);
		pointsList = currentRoute.points;
		refreshPointsStatus();
	}

	public void registerLayers(MapActivity activity) {
		// remove old if existing after turn
		if (routeStepsLayer != null) {
			activity.getMapView().removeLayer(routeStepsLayer);
		}
		routeStepsLayer = new RoutePointsLayer(activity, this);
		activity.getMapView().addLayer(routeStepsLayer, 5.5f);
		registerWidget(activity);
	}

	private void registerWidget(MapActivity activity) {
		MapInfoLayer mapInfoLayer = activity.getMapLayers().getMapInfoLayer();
		if (mapInfoLayer != null) {
			routeStepsControl = createRouteStepsInfoControl(activity, mapInfoLayer.getPaintText(), mapInfoLayer.getPaintText());
			mapInfoLayer.getMapInfoControls().registerSideWidget(routeStepsControl,
					R.drawable.widget_parking, R.string.map_widget_route_steps, "route_steps", false, 8);
			mapInfoLayer.recreateControls();
		}
	}

	public GPXUtilities.WptPt getNextPoint() {
		if (pointsList.size() > currentPointPos + 1) {
			return pointsList.get(currentPointPos + 1);
		} else {
			return null;
		}
	}

	public int findPointPosition(GPXUtilities.WptPt point) {
		int i = 0;
		for (GPXUtilities.WptPt item : pointsList) {
			if (item.equals(point)) {
				return i;
			}
			i++;
		}
		return -1;
	}

	@Override
	public void updateLayers(OsmandMapTileView mapView, MapActivity activity) {

		if (routeStepsLayer == null){
			registerLayers(activity);
		}

		if (routeStepsControl == null) {
			registerWidget(activity);
		}
	}

	private TextInfoWidget createRouteStepsInfoControl(final MapActivity map, Paint paintText, Paint paintSubText) {
		TextInfoWidget routeStepsControl = new TextInfoWidget(map, 0, paintText, paintSubText) {

			@Override()
			public boolean updateInfo(OsmandMapLayer.DrawSettings drawSettings) {
				if (gpx != null) {
					setText(String.valueOf(visitedCount) + "/",String.valueOf(pointsList.size()));
				} else {
					setText(app.getString(R.string.route_points_no_gpx), "");

				}
				return true;
			}

		};
		routeStepsControl.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(app, RoutePointsActivity.class);
				intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				app.startActivity(intent);
			}
		});
		routeStepsControl.setText(null, null);
		routeStepsControl.setImageDrawable(map.getResources().getDrawable(R.drawable.widget_parking));
		return routeStepsControl;
	}

	public void refreshPointsStatus(){
		visitedCount = 0;
		for (int i=0; i< pointsList.size();i++){
			if (getPointStatus(i) != 0){ visitedCount++;}
		}
	}

	public long getPointStatus(int numberOfPoint) {
		Map<String, String> map = currentRoute.getExtensionsToRead();

		String mapKey = POINT_KEY + numberOfPoint + VISITED_KEY;
		if (map.containsKey(mapKey)) {
			String value = map.get(mapKey);
			return (Long.valueOf(value));
		}

		return 0L;
	}

	//saves point status value to gpx extention file
	public void setPointStatus(int numberOfPoint, boolean status) {
		Map<String, String> map = currentRoute.getExtensionsToWrite();

		String mapKey = POINT_KEY + numberOfPoint + VISITED_KEY;
		if (status) {
			//value is current time
			Calendar c = Calendar.getInstance();
			long number = c.getTimeInMillis();
			map.put(mapKey, String.valueOf(number));
		} else if (map.containsKey(mapKey)) {
			map.remove(mapKey);
		}

		refreshPointsStatus();
	}
}
