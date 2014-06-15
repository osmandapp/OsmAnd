package net.osmand.plus.routesteps;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Paint;
import android.view.View;
import net.osmand.data.LatLon;
import net.osmand.plus.*;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.views.AnimateDraggingMapThread;
import net.osmand.plus.views.MapInfoLayer;
import net.osmand.plus.views.OsmandMapLayer;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.mapwidgets.TextInfoWidget;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

/**
 * Created by Barsik on 10.06.2014.
 */
public class RouteStepsPlugin extends OsmandPlugin {

	public static final String ID = "osmand.route.stepsPlugin";

	private OsmandApplication app;
	private GPXUtilities.GPXFile gpx;
	private File file;
	private GPXUtilities.Route currentRoute;
	private GPXUtilities.WptPt currentPoint;
	private int currentPointPos;
	private RouteStepsLayer routeStepsLayer;
	private List<GPXUtilities.WptPt> pointsList;
	private TextInfoWidget routeStepsControl;


	public RouteStepsPlugin(OsmandApplication app) {
		ApplicationMode. regWidget("route_steps", (ApplicationMode[]) null);
		this.app = app;
//		this.file = new File("/storage/emulated/0/osmand/tracks/", "504.gpx");
//		gpx = GPXUtilities.loadGPXFile(app, file);
	}

	public void setGpxFile(GPXUtilities.GPXFile file) {
		this.gpx = file;
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

	public GPXUtilities.GPXFile getGpx(){ return gpx;}

	public void setGpx(GPXUtilities.GPXFile gpx) { this.gpx = gpx;}

	public void registerLayers(MapActivity activity) {
		// remove old if existing after turn
		if (routeStepsLayer != null) {
			activity.getMapView().removeLayer(routeStepsLayer);
		}
		routeStepsLayer = new RouteStepsLayer(activity, this);
		activity.getMapView().addLayer(routeStepsLayer, 5.5f);
		registerWidget(activity);
	}

	private void registerWidget(MapActivity activity) {
		MapInfoLayer mapInfoLayer = activity.getMapLayers().getMapInfoLayer();
		if (mapInfoLayer != null) {
			routeStepsControl = createRouteStepsInfoControl(activity, mapInfoLayer.getPaintText(), mapInfoLayer.getPaintSubText());
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

			@Override
			public boolean updateInfo(OsmandMapLayer.DrawSettings drawSettings) {
				if (gpx != null) {
					OsmandMapTileView view = map.getMapView();
					setText("test", "test");
				} else {
					setText("No gpx", "");

				}
				return true;
			}

		};
		routeStepsControl.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(app, RouteStepsActivity.class);
				intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				app.startActivity(intent);
			}
		});
		routeStepsControl.setText(null, null);
		routeStepsControl.setImageDrawable(map.getResources().getDrawable(R.drawable.widget_parking));
		return routeStepsControl;
	}


}
