package net.osmand.plus.helpers;

import java.util.ArrayList;
import java.util.List;

import net.osmand.CallbackWithObject;
import net.osmand.binary.RouteDataObject;
import net.osmand.data.LatLon;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.views.AnimateDraggingMapThread;
import net.osmand.plus.views.ContextMenuLayer;
import net.osmand.router.RoutingConfiguration;
import net.osmand.util.MapUtils;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class AvoidSpecificRoads {
	private List<RouteDataObject> missingRoads;
	private OsmandApplication app;

	public AvoidSpecificRoads(OsmandApplication app) {
		this.app = app;
	}
	
	public List<RouteDataObject> getMissingRoads() {
		if(missingRoads == null) {
			missingRoads = app.getDefaultRoutingConfig().getImpassableRoads();
		}
		return missingRoads;
	}
	
	protected net.osmand.router.RoutingConfiguration.Builder getBuilder() {
		return RoutingConfiguration.getDefault();
	}
	
	public ArrayAdapter<RouteDataObject> createAdapter(final MapActivity ctx) {
		final ArrayList<RouteDataObject> points = new ArrayList<RouteDataObject>();
		points.addAll(getMissingRoads());
		final LatLon mapLocation = ctx.getMapLocation();
		return new ArrayAdapter<RouteDataObject>(ctx,
				R.layout.waypoint_reached, R.id.title, points) {

			@Override
			public View getView(final int position, View convertView, ViewGroup parent) {
				// User super class to create the View
				View v = convertView;
				if (v == null || v.findViewById(R.id.info_close) == null) {
					v = ctx.getLayoutInflater().inflate(R.layout.waypoint_reached, null);
				}
				final RouteDataObject obj = getItem(position);
				v.findViewById(R.id.all_points).setVisibility(View.GONE);
				((ImageView) v.findViewById(R.id.waypoint_icon)).setImageDrawable(
						app.getIconsCache().getContentIcon(R.drawable.ic_action_road_works_dark));
				double dist = MapUtils.getDistance(mapLocation, MapUtils.get31LatitudeY(obj.getPoint31YTile(0)),
						MapUtils.get31LongitudeX(obj.getPoint31XTile(0)));
				((TextView) v.findViewById(R.id.waypoint_dist)).setText(OsmAndFormatter.getFormattedDistance((float) dist, app));

				((TextView) v.findViewById(R.id.waypoint_text)).setText(getText(obj));
				ImageButton remove = (ImageButton) v.findViewById(R.id.info_close);
				remove.setVisibility(View.VISIBLE);
				remove.setImageDrawable(app.getIconsCache().getContentIcon(
						R.drawable.ic_action_gremove_dark));
				remove.setOnClickListener(new View.OnClickListener() {

					@Override
					public void onClick(View v) {
						remove(obj);
						getBuilder().removeImpassableRoad(obj);
						notifyDataSetChanged();
						RoutingHelper rh = app.getRoutingHelper();
						if (rh.isRouteCalculated() || rh.isRouteBeingCalculated()) {
							rh.recalculateRouteDueToSettingsChange();
						}
					}
				});
				return v;
			}


		};
	}


	protected String getText(RouteDataObject obj) {
		return RoutingHelper.formatStreetName(obj.getName(), obj.getRef(), obj.getDestinationName());
	}

	public void showDialog(final MapActivity mapActivity) {
		Builder bld = new AlertDialog.Builder(mapActivity);
		bld.setTitle(R.string.impassable_road);
		if (getMissingRoads().size() == 0){
			bld.setMessage(R.string.avoid_roads_msg);
		} else {
			final ArrayAdapter<?>  listAdapter = createAdapter(mapActivity);
			bld.setAdapter(listAdapter, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					RouteDataObject obj = getMissingRoads().get(which);
					double lat = MapUtils.get31LatitudeY(obj.getPoint31YTile(0));
					double lon = MapUtils.get31LongitudeX(obj.getPoint31XTile(0));
					showOnMap(app, mapActivity, lat, lon, getText(obj), dialog);
				}

			});
		}

		bld.setPositiveButton(R.string.shared_string_select_on_map, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialogInterface, int i) {
				selectFromMap(mapActivity);
			}
		});
		bld.setNegativeButton(R.string.shared_string_close, null);
		bld.show();
	}
	
	
	protected void selectFromMap(final MapActivity mapActivity) {
		ContextMenuLayer cm = mapActivity.getMapLayers().getContextMenuLayer();
		cm.setSelectOnMap(new CallbackWithObject<LatLon>() {
			
			@Override
			public boolean processResult(LatLon result) {
				findRoad(mapActivity, result);
				return true;
			}

		});
	}
	private void findRoad(final MapActivity activity, final LatLon loc) {
		new AsyncTask<LatLon, Void, RouteDataObject>() {
			Exception e = null;

			@Override
			protected RouteDataObject doInBackground(LatLon... params) {
				try {
					return app.getLocationProvider().findRoute(loc.getLatitude(), loc.getLongitude());
				} catch (Exception e) {
					this.e = e;
					e.printStackTrace();
					return null;
				}
			}
			
			protected void onPostExecute(RouteDataObject result) {
				if(e != null) {
					Toast.makeText(activity, R.string.error_avoid_specific_road, Toast.LENGTH_LONG).show();
				} else if(result != null) {
					getBuilder().addImpassableRoad(result);
					RoutingHelper rh = app.getRoutingHelper();
					if(rh.isRouteCalculated() || rh.isRouteBeingCalculated()) {
						rh.recalculateRouteDueToSettingsChange();
					}
					showDialog(activity);
				}
			};
		}.execute(loc);
	}
	
	public static void showOnMap(OsmandApplication app, Activity a, double lat, double lon, String name,
			DialogInterface dialog) {
		if (!(a instanceof MapActivity)) {
			return;
		}
		MapActivity ctx = (MapActivity) a;
		AnimateDraggingMapThread thread = ctx.getMapView().getAnimatedDraggingThread();
		int fZoom = ctx.getMapView().getZoom() < 15 ? 15 : ctx.getMapView().getZoom();
		if (thread.isAnimating()) {
			ctx.getMapView().setIntZoom(fZoom);
			ctx.getMapView().setLatLon(lat, lon);
		} else {
			thread.startMoving(lat, lon, fZoom, true);
		}
		ctx.getMapLayers().getContextMenuLayer().setLocation(new LatLon(lat, lon), name);
		dialog.dismiss();
	}

}
