package net.osmand.plus.routesteps;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import net.osmand.plus.*;
import net.osmand.plus.activities.MapActivity;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by Barsik on 10.06.2014.
 */
public class RouteStepsPlugin extends OsmandPlugin {

	public static final String ID = "osmand.route.stepsPlugin";

	private static final String VISITED_KEY = "IsVisited";
	private static final String POINT_KEY = "Point";
	private static final String CURRENT_ROUTE_KEY = "CurrentRoute";
	private int routeKey;


	private OsmandApplication app;
	private GPXUtilities.GPXFile gpx;
	private File file;
	private GPXUtilities.Route currentRoute;
	private GPXUtilities.WptPt currentPoint;
	private int currentPointPos;
	private RouteStepsLayer routeStepsLayer;
	private List<GPXUtilities.WptPt> pointsList;
	private List<Boolean> pointsStatus;


	public RouteStepsPlugin(OsmandApplication app) {
		this.app = app;
		this.file = new File("/storage/emulated/0/osmand/tracks/", "504.gpx");
		gpx = GPXUtilities.loadGPXFile(app, file);
		loadCurrentRoute();
		pointsList = currentRoute.points;
		pointsStatus = new ArrayList<Boolean>(pointsList.size());
		getAllPointsStatus();
	}

	public void setGpxFile(GPXUtilities.GPXFile file) {
		this.gpx = file;
	}

	public void saveGPXFile() {
		GPXUtilities.writeGpxFile(file, gpx, app);
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

	@Override
	public void registerLayers(MapActivity activity) {
		// remove old if existing after turn
		if (routeStepsLayer != null) {
			activity.getMapView().removeLayer(routeStepsLayer);
		}
		routeStepsLayer = new RouteStepsLayer(activity, this);
		activity.getMapView().addLayer(routeStepsLayer, 5.5f);
		//registerWidget(activity);
	}

	public List<GPXUtilities.WptPt> getPoints() {
		return currentRoute.points;
	}

	public boolean getPointStatus(int numberOfPoint) {
		Map<String, String> map = gpx.getExtensionsToRead();

		String mapKey = routeKey + POINT_KEY + numberOfPoint + VISITED_KEY;
		if (map.containsKey(mapKey)) {
			String value = map.get(mapKey);
			return (value.equals("true"));
		}

		return false;
	}

	//saves point status value to gpx extention file
	public void setPointStatus(int numberOfPoint, boolean status) {
		Map<String, String> map = gpx.getExtensionsToWrite();

		String mapKey = routeKey + POINT_KEY + numberOfPoint + VISITED_KEY;
		if (status) {
			map.put(mapKey, "true");
		} else {
			map.put(mapKey, "false");
		}
	}

	public GPXUtilities.WptPt getNextPoint() {
		if (pointsList.size() > currentPointPos + 1) {
			return pointsList.get(currentPointPos + 1);
		} else {
			return null;
		}
	}

	private void loadCurrentRoute() {
		if (gpx.routes.size() < 1) {
			return;
		}

		Map<String, String> map = gpx.getExtensionsToRead();
		if (map.containsKey(CURRENT_ROUTE_KEY)) {
			String routeName = map.get(CURRENT_ROUTE_KEY);
			int i = 0;
			for (GPXUtilities.Route route : gpx.routes) {
				if (route.name.equals(routeName)) {
					currentRoute = route;
					routeKey = i;
					return;
				}
				i++;
			}
		}

		routeKey = 0;
		currentRoute = gpx.routes.get(0);
	}

	@Override
	public void registerMapContextMenuActions(final MapActivity mapActivity,
											  final double latitude, final double longitude,
											  ContextMenuAdapter adapter, Object selectedObj) {

		ContextMenuAdapter.OnContextMenuClick addListener = new ContextMenuAdapter.OnContextMenuClick() {
			@Override
			public void onContextMenuClick(int resId, int pos,
										   boolean isChecked, DialogInterface dialog) {
				if (resId == R.string.context_menu_item_show_route_points) {
					showStepsDialog(mapActivity);
				}
			}
		};
		adapter.item(R.string.context_menu_item_show_route_points)
				.icons(R.drawable.ic_action_parking_dark, R.drawable.ic_action_parking_light).listen(addListener).reg();

	}

	private void getAllPointsStatus() {
		for (int i = 0; i < pointsList.size(); i++) {
			pointsStatus.add(getPointStatus(i));
		}
	}

	private void showStepsDialog(MapActivity mapActivity) {

		List<String> pointNames = new ArrayList<String>();
		//this array need to collect user selection during dialogue
		final List<Boolean> pointsIntermediateState = new ArrayList<Boolean>(pointsStatus);
		for (GPXUtilities.WptPt point : pointsList) {
			pointNames.add(point.name);
		}

		AlertDialog.Builder builder = new AlertDialog.Builder(mapActivity);
		builder.setTitle("All available points");
		builder.setMultiChoiceItems(pointNames.toArray(new String[pointNames.size()]), toPrimitiveArray(pointsIntermediateState), new DialogInterface.OnMultiChoiceClickListener() {
			@Override
			public void onClick(DialogInterface dialogInterface, int i, boolean isChecked) {
				//saving user choice
				pointsIntermediateState.set(i, isChecked);
			}
		});
		builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialogInterface, int i) {
				for (int j = 0; j < pointsIntermediateState.size(); j++) {
					boolean newValue = pointsIntermediateState.get(j);
					//if values is the same - there's no need to save data
					if (newValue != pointsStatus.get(j)) {
						setPointStatus(j, newValue);
					}
				}
				pointsStatus = new ArrayList<Boolean>(pointsIntermediateState);
				saveGPXFile();
			}
		});
		builder.setNegativeButton("Cancel", null);

		builder.show();

	}

	private boolean[] toPrimitiveArray(final List<Boolean> booleanList) {
		final boolean[] primitives = new boolean[booleanList.size()];
		int index = 0;
		for (Boolean object : booleanList) {
			primitives[index++] = object;
		}
		return primitives;
	}


	private int findPointPosition(GPXUtilities.WptPt point) {
		int i = 0;
		for (GPXUtilities.WptPt item : pointsList) {
			if (item.equals(point)) {
				return i;
			}
			i++;
		}
		return -1;
	}

}
