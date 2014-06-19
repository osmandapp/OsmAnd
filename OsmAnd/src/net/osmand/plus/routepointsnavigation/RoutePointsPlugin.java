package net.osmand.plus.routepointsnavigation;

import android.content.Intent;
import android.graphics.Paint;
import android.view.View;
import net.osmand.data.LatLon;
import net.osmand.plus.*;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.views.MapInfoLayer;
import net.osmand.plus.views.OsmandMapLayer;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.mapwidgets.TextInfoWidget;

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
	private List<GPXUtilities.WptPt> pointsList;
	private TextInfoWidget routeStepsControl;

	private int visitedCount;
	private int currentPointIndex = -1;


	public RoutePointsPlugin(OsmandApplication app) {
		ApplicationMode.regWidget("route_steps", (ApplicationMode[]) null);
		this.app = app;
	}

	public void setCurrentPoint(GPXUtilities.WptPt point) {
		TargetPointsHelper targetPointsHelper = app.getTargetPointsHelper();
		LatLon latLon = new LatLon(point.lat, point.lon);
		targetPointsHelper.navigateToPoint(latLon, true, -1,":" + point.name);
		getCurrentPoint();
	}

	public void setCurrentPoint(int number) {
		GPXUtilities.WptPt point = pointsList.get(number);
		TargetPointsHelper targetPointsHelper = app.getTargetPointsHelper();
		LatLon latLon = new LatLon(point.lat, point.lon);
		targetPointsHelper.navigateToPoint(latLon, true, -1, point.name);
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

	public GPXUtilities.WptPt getCurrentPoint() {
		TargetPointsHelper targetPointsHelper = app.getTargetPointsHelper();
		String locName = targetPointsHelper.getPointNavigateDescription();
		for (int i = 0; i < pointsList.size(); i++) {
			String pointName = ":" + pointsList.get(i).name;
			if (pointName.equals(locName)) {
				currentPoint = pointsList.get(i);
				currentPointIndex = i;
				break;
			}
		}

		return currentPoint;
	}

	public GPXUtilities.GPXFile getGpx() {
		return gpx;
	}

	public void setGpx(GPXUtilities.GPXFile gpx) {
		this.gpx = gpx;
		currentRoute = gpx.routes.get(0);
		pointsList = currentRoute.points;
		refreshPointsStatus();
		getCurrentPoint();
	}

	private void registerWidget(MapActivity activity) {
		MapInfoLayer mapInfoLayer = activity.getMapLayers().getMapInfoLayer();
		if (mapInfoLayer != null) {
			routeStepsControl = createRouteStepsInfoControl(activity, mapInfoLayer.getPaintSubText(), mapInfoLayer.getPaintSubText());
			mapInfoLayer.getMapInfoControls().registerSideWidget(routeStepsControl,
					R.drawable.widget_target, R.string.map_widget_route_points, "route_steps", false, 8);
			mapInfoLayer.recreateControls();
		}
	}

	public GPXUtilities.WptPt getNextPoint() {
		if (pointsList.size() > currentPointIndex + 1) {
			return pointsList.get(currentPointIndex + 1);
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

		if (routeStepsControl == null) {
			registerWidget(activity);
		}
	}

	public String getVisitedAllString() {
		return String.valueOf(visitedCount) + "/" + String.valueOf(pointsList.size());
	}

	private TextInfoWidget createRouteStepsInfoControl(final MapActivity map, Paint paintText, Paint paintSubText) {
		TextInfoWidget routeStepsControl = new TextInfoWidget(map, 0, paintText, paintSubText) {

			@Override()
			public boolean updateInfo(OsmandMapLayer.DrawSettings drawSettings) {
				if (gpx != null) {
					setText(String.valueOf(visitedCount) + "/", String.valueOf(pointsList.size()));
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

	public void refreshPointsStatus() {
		visitedCount = 0;
		for (int i = 0; i < pointsList.size(); i++) {
			if (getPointStatus(i) != 0) {
				visitedCount++;
			}
		}
	}

	public long getPointStatus(GPXUtilities.WptPt point) {
		return getPointStatus(findPointPosition(point));
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
	public void setPointStatus(GPXUtilities.WptPt point, boolean status) {
		Map<String, String> map = currentRoute.getExtensionsToWrite();
		int pos = findPointPosition(point);
		String mapKey = POINT_KEY + pos + VISITED_KEY;
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

	public Integer getCurrentPointIndex() {
		return currentPointIndex;
	}
}
