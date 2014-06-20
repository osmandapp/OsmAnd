package net.osmand.plus.routepointsnavigation;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import net.osmand.data.LatLon;
import net.osmand.plus.ApplicationMode;
import net.osmand.plus.GPXUtilities;
import net.osmand.plus.GPXUtilities.GPXFile;
import net.osmand.plus.GPXUtilities.Route;
import net.osmand.plus.GPXUtilities.WptPt;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.TargetPointsHelper;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.views.MapInfoLayer;
import net.osmand.plus.views.OsmandMapLayer;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.mapwidgets.TextInfoWidget;
import net.osmand.util.MapUtils;
import android.content.Intent;
import android.graphics.Paint;
import android.text.format.DateFormat;
import android.view.View;

/**
 * Created by Barsik on 10.06.2014.
 */
public class RoutePointsPlugin extends OsmandPlugin {

	public static final String ID = "osmand.route.stepsPlugin";

	private static final String VISITED_KEY = "VISITED_KEY";

	private OsmandApplication app;
	private TextInfoWidget routeStepsControl;
	private SelectedRouteGpxFile currentRoute;

	private RoutePointsLayer routePointsLayer;

	public RoutePointsPlugin(OsmandApplication app) {
		ApplicationMode.regWidget("route_steps", ApplicationMode.CAR, ApplicationMode.DEFAULT);
		this.app = app;
	}

	public SelectedRouteGpxFile getCurrentRoute() {
		return currentRoute;
	}


	public void setCurrentRoute(GPXFile gpx) {
		if (gpx == null) {
			currentRoute = null;
		} else {
			currentRoute = new SelectedRouteGpxFile(gpx);
		}
	}


	@Override
	public boolean destinationReached() {
		if (currentRoute != null) {
			boolean naviateToNextPoint = currentRoute.naviateToNextPoint();
			if (naviateToNextPoint) {
				return false;
			}
		}
		return true;
	}

	@Override
	public String getId() {
		return ID;
	}

	@Override
	public String getDescription() {
		return app.getString(R.string.route_plugin_descr);
	}

	@Override
	public String getName() {
		return app.getString(R.string.route_plugin_name);
	}

	@Override
	public boolean init(OsmandApplication app) {
		return true;
	}

	private void registerWidget(MapActivity activity) {
		MapInfoLayer mapInfoLayer = activity.getMapLayers().getMapInfoLayer();
		if (mapInfoLayer != null) {
			routeStepsControl = createRouteStepsInfoControl(activity, mapInfoLayer.getPaintSubText(), mapInfoLayer.getPaintSubText());
			mapInfoLayer.getMapInfoControls().registerSideWidget(routeStepsControl,
					R.drawable.ic_signpost, R.string.map_widget_route_points, "route_steps", false, 8);
			mapInfoLayer.recreateControls();
		}
	}

	@Override
	public void registerLayers(MapActivity activity) {
		super.registerLayers(activity);

		if (routePointsLayer != null) {
			activity.getMapView().removeLayer(routePointsLayer);
		}

		routePointsLayer = new RoutePointsLayer(activity, this);
		activity.getMapView().addLayer(routePointsLayer, 5.5f);
		registerWidget(activity);
	}

	@Override
	public void updateLayers(OsmandMapTileView mapView, MapActivity activity) {
		if (routePointsLayer == null){
			registerLayers(activity);
		}

		if (routeStepsControl == null) {
			registerWidget(activity);
		}
	}

	public String getVisitedAllString() {
		if (currentRoute != null) {
			return String.valueOf(currentRoute.getVisitedCount()) + "/" + String.valueOf(currentRoute.getCount());
		} else {
			return app.getString(R.string.route_points_no_gpx);

		}
	}

	private TextInfoWidget createRouteStepsInfoControl(final MapActivity map, Paint paintText, Paint paintSubText) {
		TextInfoWidget routeStepsControl = new TextInfoWidget(map, 0, paintText, paintSubText) {

			@Override()
			public boolean updateInfo(OsmandMapLayer.DrawSettings drawSettings) {
				setText(getVisitedAllString(), "");
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
		routeStepsControl.setImageDrawable(map.getResources().getDrawable(R.drawable.ic_signpost));
		return routeStepsControl;
	}


	public class RoutePoint {
		boolean isNextNavigate;
		int gpxOrder;
		long visitedTime; // 0 not visited
		WptPt wpt;

		public String getName() {
			return wpt.name;
		}

		public WptPt getWpt() {
			return wpt;
		}

		public boolean isNextNavigate() {
			return isNextNavigate;
		}

		public boolean isVisited() {
			return visitedTime != 0;
		}

		public int getGpxOrder() {
			return gpxOrder;
		}

		public String getDistance(RoutePoint rp) {
			double d = MapUtils.getDistance(rp.getPoint(), getPoint());
			String distance = OsmAndFormatter.getFormattedDistance((float) d, app);
			return distance;
		}

		public String getTime() {
			if (visitedTime == 0) {
				return "";
			}
			String dateString;
			Date date = new Date(visitedTime);
			if (DateFormat.is24HourFormat(app)) {
				dateString = DateFormat.format("MM/dd k:mm", date).toString();
			} else {
				dateString = DateFormat.format("MM/dd h:mm", date).toString() + DateFormat.format("aa", date).toString();
			}
			return dateString;
		}

		public LatLon getPoint() {
			return new LatLon(wpt.lat, wpt.lon);
		}

		public void setVisitedTime(long currentTimeMillis) {
			visitedTime = currentTimeMillis;
			wpt.getExtensionsToWrite().put(VISITED_KEY, visitedTime + "");
		}

	}

	public class SelectedRouteGpxFile {
		private GPXUtilities.GPXFile gpx;
		private List<RoutePoint> currentPoints = new ArrayList<RoutePointsPlugin.RoutePoint>();


		public SelectedRouteGpxFile(GPXUtilities.GPXFile gpx) {
			this.gpx = gpx;
			parseGPXFile(gpx);
		}

		public List<RoutePoint> getCurrentPoints() {
			return currentPoints;
		}

		public int getVisitedCount() {
			int k = 0;
			for (RoutePoint rp : currentPoints) {
				if (rp.isVisited()) {
					k++;
				}
			}
			return k;
		}

		public int getCount() {
			return currentPoints.size();
		}

		public GPXUtilities.Route getRoute() {
			if (gpx.routes.isEmpty()) {
				return null;
			}
			return gpx.routes.get(0);
		}

		public String saveFile() {
			return GPXUtilities.writeGpxFile(new File(gpx.path), gpx, app);
		}

		public void markPoint(RoutePoint point, boolean visited) {
			if (point.isNextNavigate() && visited) {
				naviateToNextPoint();
				return;
			}
			if (visited) {
				point.setVisitedTime(System.currentTimeMillis());
			} else {
				point.setVisitedTime(0);
			}
			sortPoints();
		}

		public boolean naviateToNextPoint() {
			if (!currentPoints.isEmpty()) {
				RoutePoint rp = currentPoints.get(0);
				if (rp.isNextNavigate) {
					rp.setVisitedTime(System.currentTimeMillis());
					sortPoints();
				}
				RoutePoint first = currentPoints.get(0);
				if (!first.isVisited()) {
					app.getTargetPointsHelper().navigateToPoint(first.getPoint(), true, -1, first.getName());
					first.isNextNavigate = true;
					return true;
				} else {
					app.getTargetPointsHelper().clearPointToNavigate(true);
				}
			}
			return false;
		}

		private void sortPoints() {
			Collections.sort(currentPoints, new Comparator<RoutePoint>() {

				@Override
				public int compare(RoutePoint lhs, RoutePoint rhs) {
					if (lhs.isNextNavigate || rhs.isNextNavigate) {
						return lhs.isNextNavigate ? -1 : 1;
					}
					if (!lhs.isVisited() || !rhs.isVisited()) {
						if (lhs.isVisited()) {
							return 1;
						}
						if (rhs.isVisited()) {
							return -1;
						}
						return lcompare(lhs.gpxOrder, rhs.gpxOrder);
					}
					return -lcompare(lhs.visitedTime, rhs.visitedTime);
				}

				public int lcompare(long lhs, long rhs) {
					return lhs < rhs ? -1 : (lhs == rhs ? 0 : 1);
				}
			});
		}


		private void parseGPXFile(GPXFile gpx) {
			this.gpx = gpx;
			Route rt = getRoute();
			currentPoints.clear();
			if (rt != null) {
				TargetPointsHelper targetPointsHelper = app.getTargetPointsHelper();
				String locName = targetPointsHelper.getPointNavigateDescription();
				for (int i = 0; i < rt.points.size(); i++) {
					WptPt wptPt = rt.points.get(i);
					RoutePoint rtp = new RoutePoint();
					rtp.gpxOrder = i;
					rtp.wpt = wptPt;
					String time = wptPt.getExtensionsToRead().get(VISITED_KEY);
					try {
						rtp.visitedTime = Long.parseLong(time);
					} catch (NumberFormatException e) {
					}
					rtp.isNextNavigate = rtp.visitedTime == 0 && locName != null && locName.equals(wptPt.name);
					if (rtp.isNextNavigate) {
						locName = null;
					}
					currentPoints.add(rtp);
				}
				sortPoints();
			}
		}

		public String getName() {
			return gpx.path.substring(gpx.path.lastIndexOf("/") + 1, gpx.path.lastIndexOf("."));
		}

		public void navigateToPoint(RoutePoint rp) {
			if (!currentPoints.isEmpty()) {
				if (currentPoints.get(0).isNextNavigate()) {
					currentPoints.get(0).isNextNavigate = false;
				}
			}
			rp.isNextNavigate = true;
			sortPoints();
			app.getTargetPointsHelper().navigateToPoint(rp.getPoint(), true, -1, rp.getName());
		}

		public void updateCurrentTargetPoint() {
			TargetPointsHelper targetPointsHelper = app.getTargetPointsHelper();
			String locName = targetPointsHelper.getPointNavigateDescription();
			for (int i = 0; i < currentPoints.size(); i++) {
				RoutePoint rtp = currentPoints.get(i);
				rtp.isNextNavigate = rtp.visitedTime == 0 && locName != null && locName.equals(rtp.getName());
				if (rtp.isNextNavigate) {
					locName = null;
				}

			}
			sortPoints();
		}

		public boolean getPointStatus(WptPt p) {
			RoutePoint point = getRoutePointFromWpt(p);
			return point != null && (point.isVisited());
		}

		public void markPoint(WptPt point, boolean visited) {
			RoutePoint routePoint = getRoutePointFromWpt(point);
			if (routePoint != null) {
				markPoint(routePoint, visited);
			}
		}

		public void navigateToPoint(WptPt point) {
			RoutePoint routePoint = getRoutePointFromWpt(point);
			if (routePoint != null) {
				navigateToPoint(routePoint);
			}
		}

		public RoutePoint getRoutePointFromWpt(WptPt point) {
			if (currentPoints != null) {
				for (RoutePoint find : currentPoints) {
					WptPt itemToFind = find.getWpt();
					if (itemToFind.equals(point)) {
						return find;
					}
				}
			}
			return null;
		}
	}
}
