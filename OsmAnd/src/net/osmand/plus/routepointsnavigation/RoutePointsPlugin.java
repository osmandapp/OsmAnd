package net.osmand.plus.routepointsnavigation;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
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
import net.osmand.plus.TargetPointsHelper.TargetPoint;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.views.MapInfoLayer;
import net.osmand.plus.views.OsmandMapLayer;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.mapwidgets.TextInfoWidget;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.text.format.DateFormat;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

/**
 * Created by Barsik on 10.06.2014.
 */
public class RoutePointsPlugin extends OsmandPlugin {

	public static final String ID = "osmand.route.stepsPlugin";
	public static final String ROUTE_POINTS_PLUGIN_COMPONENT = "net.osmand.routePointsPlugin";
	private static final String VISITED_KEY = "VISITED_KEY";
	private static final String DELIVERED_KEY = "DELIVERED_KEY";

	private OsmandApplication app;
	private TextInfoWidget routeStepsControl;
	private SelectedRouteGpxFile currentRoute;

	private MapActivity mapActivity;
	private RoutePointsLayer routePointsLayer;

	public RoutePointsPlugin(OsmandApplication app) {
		ApplicationMode.regWidget("route_steps", ApplicationMode.CAR, ApplicationMode.DEFAULT);
		this.app = app;
	}

	public SelectedRouteGpxFile getCurrentRoute() {
		return currentRoute;
	}
	
	@Override
	public int getLogoResourceId() {
		// TODO
		return super.getLogoResourceId();
	}
	
	@Override
	public int getAssetResourceName() {
		return R.drawable.trip_recording;
	}

	public void setCurrentRoute(GPXFile gpx) {
		if (gpx == null) {
			currentRoute = null;
		} else {
			currentRoute = new SelectedRouteGpxFile(gpx);
		}
	}

	private View createDeliveredView(RoutePoint point) {
		final LayoutInflater vi = (LayoutInflater) app.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View deliveredView = vi.inflate(R.layout.package_delivered, null);

		TextView name = (TextView) deliveredView.findViewById(R.id.point_name);
		name.setText(point.getName());
		TextView id = (TextView) deliveredView.findViewById(R.id.point_id);
		id.setText(point.id.toString());

		Button btnY = (Button) deliveredView.findViewById(R.id.delivered_yes);
		btnY.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				setPointDelivered(view, true);
			}
		});

		Button btnN = (Button) deliveredView.findViewById(R.id.delivered_no);
		btnN.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				setPointDelivered(view, false);
			}
		});

		return deliveredView;
	}

	private void setPointDelivered(View child, boolean delivered) {
		if (child == null || child.getParent() == null) {
			return;
		}

		View parent = (View) child.getParent().getParent();
		if (parent == null) {
			return;
		}
		TextView id = (TextView) parent.findViewById(R.id.point_id);
		if (id != null) {
			RoutePoint point = getPointById(UUID.fromString(id.getText().toString()));
			if (point != null) {
				point.setDelivered(delivered);
			}
		}

		FrameLayout layout = (FrameLayout) mapActivity.getLayout();
		if (layout != null) {
			layout.removeView(parent);
		}
	}

	private RoutePoint getPointById(UUID id) {
		if (currentRoute == null) {
			return null;
		}

		for (RoutePoint p : currentRoute.currentPoints) {
			if (p.id.compareTo(id) == 0) {
				return p;
			}
		}
		return null;
	}


	@Override
	public boolean destinationReached() {
		if (currentRoute != null) {
			//Check EVERYTHING
			if (currentRoute.currentPoints != null &&
					currentRoute.currentPoints.size() > 0 &&
					currentRoute.currentPoints.get(0).isNextNavigate) {
				FrameLayout layout = (FrameLayout) mapActivity.getLayout();
				FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
				params.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
				View deliveredView = createDeliveredView(currentRoute.currentPoints.get(0));

				if (deliveredView != null) {
					layout.addView(deliveredView, params);
				}
			}

			//if it's possible to navigate to next point - navigation continues
			return !currentRoute.navigateToNextPoint();

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

	private void registerWidget(MapActivity activity) {
		MapInfoLayer mapInfoLayer = activity.getMapLayers().getMapInfoLayer();
		if (mapInfoLayer != null) {
			routeStepsControl = createRouteStepsInfoControl(activity);
			mapInfoLayer.registerSideWidget(routeStepsControl,
					R.drawable.ic_action_signpost_dark,  R.string.map_widget_route_points, "route_steps", false, 8);
			mapInfoLayer.recreateControls();
		}
	}

	@Override
	public void registerLayers(MapActivity activity) {
		super.registerLayers(activity);
		mapActivity = activity;
		if (routePointsLayer != null) {
			activity.getMapView().removeLayer(routePointsLayer);
		}

		routePointsLayer = new RoutePointsLayer(activity, this);
		activity.getMapView().addLayer(routePointsLayer, 5.5f);
		registerWidget(activity);
	}

	@Override
	public void updateLayers(OsmandMapTileView mapView, MapActivity activity) {
		if (routePointsLayer == null) {
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

	public void saveCurrentRoute() {
		if (currentRoute != null) {
			currentRoute.saveGPXAsync();
		}
	}

	private TextInfoWidget createRouteStepsInfoControl(final MapActivity map) {
		TextInfoWidget routeStepsControl = new TextInfoWidget(map) {

			@Override()
			public boolean updateInfo(OsmandMapLayer.DrawSettings drawSettings) {
				if (currentRoute != null) {
					setText(getVisitedAllString(), "");
				} else {
					setText("", app.getString(R.string.route_points_no_gpx));
				}
				return true;
			}

		};
		routeStepsControl.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
//				FavouritesDbHelper fp = map.getMyApplication().getFavorites();
//				app.getTargetPointsHelper().addVisibleLocationPoint(fp.getFavouritePoints().get(new Random().nextInt(fp.getFavouritePoints().size())));
				Intent intent = new Intent(app, RoutePointsActivity.class);
				intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				app.startActivity(intent);
			}
		});
		routeStepsControl.setText(null, null);
		routeStepsControl.setImageDrawable(R.drawable.widget_signpost);
		return routeStepsControl;
	}


	public class RoutePoint {
		boolean isNextNavigate;
		int gpxOrder;
		long visitedTime = 0; // 0 not visited
		WptPt wpt;
		boolean delivered;
		public UUID id;

		public String getName() {
			return wpt.name == null ? "" : wpt.name;
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

		public boolean isDelivered() {
			return delivered;
		}

		public int getGpxOrder() {
			return gpxOrder;
		}

		public RoutePoint(WptPt point) {
			id = UUID.randomUUID();
			this.wpt = point;
			if (wpt != null) {
				String delivered = wpt.getExtensionsToRead().get(DELIVERED_KEY);
				this.delivered = Boolean.parseBoolean(delivered);

				String time = wpt.getExtensionsToRead().get(VISITED_KEY);
				try {
					visitedTime = Long.parseLong(time);
				} catch (NumberFormatException e) {
				}
			}
		}

		public String getDistance(RoutePoint rp) {
			double d = MapUtils.getDistance(rp.getPoint(), getPoint());
			return OsmAndFormatter.getFormattedDistance((float) d, app);
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

		public void setDelivered(boolean d) {
			wpt.getExtensionsToWrite().put(DELIVERED_KEY, String.valueOf(d));
			this.delivered = d;
			saveCurrentRoute();
		}

		public void setVisitedTime(long currentTimeMillis) {
			visitedTime = currentTimeMillis;
			wpt.getExtensionsToWrite().put(VISITED_KEY, visitedTime + "");
			saveCurrentRoute();
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
				navigateToNextPoint();
				return;
			}
			if (visited) {
				point.setVisitedTime(System.currentTimeMillis());
			} else {
				point.setVisitedTime(0);
			}
			sortPoints();
		}

		public boolean navigateToNextPoint() {
			if (currentPoints.isEmpty()) {
				return false;
			}

			RoutePoint rp = currentPoints.get(0);
			if (rp.isNextNavigate) {
				rp.setVisitedTime(System.currentTimeMillis());
				rp.isNextNavigate = false;
				sortPoints();
			}

			RoutePoint first = currentPoints.get(0);
			if (!first.isVisited()) {
				app.getTargetPointsHelper().navigateToPoint(first.getPoint(), true, -1, 
						new PointDescription(PointDescription.POINT_TYPE_WPT, first.getName()));
				first.isNextNavigate = true;
				return true;
			} else {
				app.getTargetPointsHelper().clearPointToNavigate(true);
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
				TargetPoint pointToNavigate = targetPointsHelper.getPointToNavigate();
				String locName = pointToNavigate == null ? null : pointToNavigate.getOnlyName(); 
				for (int i = 0; i < rt.points.size(); i++) {
					WptPt wptPt = rt.points.get(i);
					RoutePoint rtp = new RoutePoint(wptPt);
					rtp.gpxOrder = i;

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
			app.getTargetPointsHelper().navigateToPoint(rp.getPoint(), true, -1, 
					new PointDescription(PointDescription.POINT_TYPE_WPT, rp.getName()));
		}

		public void updateCurrentTargetPoint() {
			TargetPointsHelper targetPointsHelper = app.getTargetPointsHelper();
			TargetPoint tp = targetPointsHelper.getPointToNavigate();
			for (int i = 0; i < currentPoints.size(); i++) {
				RoutePoint rtp = currentPoints.get(i);
				rtp.isNextNavigate = rtp.visitedTime == 0 && tp != null && !Algorithms.isEmpty(tp.getOnlyName()) &&
						tp.getOnlyName().equals(rtp.getName());
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

		public void saveGPXAsync() {
			new AsyncTask<RoutePointsPlugin.SelectedRouteGpxFile, Void, Void>() {
				@Override
				protected Void doInBackground(RoutePointsPlugin.SelectedRouteGpxFile... params) {
					saveFile();
					return null;
				}
			}.execute(getCurrentRoute());
		}
	}
	
	@Override
	public Class<? extends Activity> getSettingsActivity() {
		return null;
	}
}
