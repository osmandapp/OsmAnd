package net.osmand.plus.helpers;

import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import net.osmand.CallbackWithObject;
import net.osmand.Location;
import net.osmand.ResultMatcher;
import net.osmand.binary.RouteDataObject;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.MapContextMenu;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.views.AnimateDraggingMapThread;
import net.osmand.plus.views.ContextMenuLayer;
import net.osmand.router.RoutingConfiguration;
import net.osmand.router.RoutingConfiguration.Builder;
import net.osmand.util.MapUtils;

import java.util.ArrayList;
import java.util.List;

public class AvoidSpecificRoads {
	private List<RouteDataObject> missingRoads;
	private OsmandApplication app;

	public AvoidSpecificRoads(OsmandApplication app) {
		this.app = app;
	}

	public void initPreservedData() {
		List<LatLon> impassibleRoads = app.getSettings().getImpassableRoadPoints();
		for (LatLon impassibleRoad : impassibleRoads) {
			addImpassableRoad(null, impassibleRoad, false, null, true);
		}
	}

	public List<RouteDataObject> getMissingRoads() {
		if (missingRoads == null) {
			missingRoads = app.getDefaultRoutingConfig().getImpassableRoads();
		}
		return missingRoads;
	}


	public ArrayAdapter<RouteDataObject> createAdapter(final MapActivity ctx) {
		final ArrayList<RouteDataObject> points = new ArrayList<>();
		points.addAll(getMissingRoads());
		final LatLon mapLocation = ctx.getMapLocation();
		return new ArrayAdapter<RouteDataObject>(ctx,
				R.layout.waypoint_reached, R.id.title, points) {

			@Override
			public View getView(final int position, View convertView, ViewGroup parent) {
				// User super class to create the View
				View v = convertView;
				if (v == null || v.findViewById(R.id.info_close) == null) {
					v = ctx.getLayoutInflater().inflate(R.layout.waypoint_reached, parent, false);
				}
				final RouteDataObject obj = getItem(position);
				v.findViewById(R.id.all_points).setVisibility(View.GONE);
				((ImageView) v.findViewById(R.id.waypoint_icon)).setImageDrawable(
						app.getIconsCache().getThemedIcon(R.drawable.ic_action_road_works_dark));
				double dist = MapUtils.getDistance(mapLocation, MapUtils.get31LatitudeY(obj.getPoint31YTile(0)),
						MapUtils.get31LongitudeX(obj.getPoint31XTile(0)));
				((TextView) v.findViewById(R.id.waypoint_dist)).setText(OsmAndFormatter.getFormattedDistance((float) dist, app));

				((TextView) v.findViewById(R.id.waypoint_text)).setText(getText(obj));
				ImageButton remove = (ImageButton) v.findViewById(R.id.info_close);
				remove.setVisibility(View.VISIBLE);
				remove.setImageDrawable(app.getIconsCache().getThemedIcon(
						R.drawable.ic_action_remove_dark));
				remove.setOnClickListener(new View.OnClickListener() {

					@Override
					public void onClick(View v) {
						remove(obj);
						removeImpassableRoad(obj);
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

	public void removeImpassableRoad(RouteDataObject obj) {
		app.getSettings().removeImpassableRoad(getLocation(obj));
		app.getDefaultRoutingConfig().removeImpassableRoad(obj);
	}


	protected String getText(RouteDataObject obj) {
		return RoutingHelper.formatStreetName(obj.getName(app.getSettings().MAP_PREFERRED_LOCALE.get(),
				app.getSettings().MAP_TRANSLITERATE_NAMES.get()), 
				obj.getRef(app.getSettings().MAP_PREFERRED_LOCALE.get(), app.getSettings().MAP_TRANSLITERATE_NAMES.get(), true), 
				obj.getDestinationName(app.getSettings().MAP_PREFERRED_LOCALE.get(), app.getSettings().MAP_TRANSLITERATE_NAMES.get(), true), 
				app.getString(R.string.towards));
	}

	public void showDialog(@NonNull final MapActivity mapActivity) {
		AlertDialog.Builder bld = new AlertDialog.Builder(mapActivity);
		bld.setTitle(R.string.impassable_road);
		if (getMissingRoads().size() == 0) {
			bld.setMessage(R.string.avoid_roads_msg);
		} else {
			final ArrayAdapter<?> listAdapter = createAdapter(mapActivity);
			bld.setAdapter(listAdapter, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					RouteDataObject obj = getMissingRoads().get(which);
					double lat = MapUtils.get31LatitudeY(obj.getPoint31YTile(0));
					double lon = MapUtils.get31LongitudeX(obj.getPoint31XTile(0));
					showOnMap(mapActivity, lat, lon, getText(obj), dialog);
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
				addImpassableRoad(mapActivity, result, true, null, false);
				return true;
			}

		});
	}

	public void addImpassableRoad(@Nullable final MapActivity activity,
								  @NonNull final LatLon loc,
								  final boolean showDialog,
								  @Nullable final AvoidSpecificRoadsCallback callback,
								  final boolean skipWritingSettings) {
		final Location ll = new Location("");
		ll.setLatitude(loc.getLatitude());
		ll.setLongitude(loc.getLongitude());
		app.getLocationProvider().getRouteSegment(ll, new ResultMatcher<RouteDataObject>() {

			@Override
			public boolean publish(RouteDataObject object) {
				if (object == null) {
					if (activity != null) {
						Toast.makeText(activity, R.string.error_avoid_specific_road, Toast.LENGTH_LONG).show();
					}
					if (callback != null) {
						callback.onAddImpassableRoad(false, null);
					}
				} else {
					addImpassableRoadInternal(object, ll, showDialog, activity, loc);

					if (callback != null) {
						callback.onAddImpassableRoad(true, object);
					}
				}
				return true;
			}

			@Override
			public boolean isCancelled() {
				return callback != null && callback.isCancelled();
			}

		});
		if (!skipWritingSettings) {
			app.getSettings().addImpassableRoad(loc.getLatitude(), loc.getLongitude());
		}
	}

	public void replaceImpassableRoad(final MapActivity activity, final RouteDataObject currentObject,
									  final LatLon loc, final boolean showDialog,
									  final AvoidSpecificRoadsCallback callback) {

		LatLon latLon = getLocation(currentObject);
		app.getSettings().moveImpassableRoad(latLon, loc);

		final Location ll = new Location("");
		ll.setLatitude(loc.getLatitude());
		ll.setLongitude(loc.getLongitude());
		app.getLocationProvider().getRouteSegment(ll, new ResultMatcher<RouteDataObject>() {

			@Override
			public boolean publish(RouteDataObject object) {
				if (object == null) {
					Toast.makeText(activity, R.string.error_avoid_specific_road, Toast.LENGTH_LONG).show();
					if (callback != null) {
						callback.onAddImpassableRoad(false, null);
					}
				} else {
					app.getDefaultRoutingConfig().removeImpassableRoad(currentObject);
					addImpassableRoadInternal(object, ll, showDialog, activity, loc);

					if (callback != null) {
						callback.onAddImpassableRoad(true, object);
					}
				}
				return true;
			}

			@Override
			public boolean isCancelled() {
				return callback != null && callback.isCancelled();
			}
		});
	}

	private void addImpassableRoadInternal(@NonNull RouteDataObject object,
										   @NonNull Location ll,
										   boolean showDialog,
										   @Nullable MapActivity activity,
										   @NonNull LatLon loc) {
		if(!app.getDefaultRoutingConfig().addImpassableRoad(object, ll)) {
			LatLon location = getLocation(object);
			if(location != null) {
				app.getSettings().removeImpassableRoad(getLocation(object));
			}
		}
		RoutingHelper rh = app.getRoutingHelper();
		if (rh.isRouteCalculated() || rh.isRouteBeingCalculated()) {
			rh.recalculateRouteDueToSettingsChange();
		}
		if (activity != null) {
			if (showDialog) {
				showDialog(activity);
			}
			MapContextMenu menu = activity.getContextMenu();
			if (menu.isActive() && menu.getLatLon().equals(loc)) {
				menu.close();
			}
			activity.refreshMap();
		}
	}

	private void showOnMap(MapActivity ctx, double lat, double lon, String name,
						   DialogInterface dialog) {
		AnimateDraggingMapThread thread = ctx.getMapView().getAnimatedDraggingThread();
		int fZoom = ctx.getMapView().getZoom() < 15 ? 15 : ctx.getMapView().getZoom();
		if (thread.isAnimating()) {
			ctx.getMapView().setIntZoom(fZoom);
			ctx.getMapView().setLatLon(lat, lon);
		} else {
			thread.startMoving(lat, lon, fZoom, true);
		}
		ctx.getContextMenu().show(new LatLon(lat, lon), new PointDescription("", name), null);
		dialog.dismiss();
	}

	private LatLon getLocation(RouteDataObject object) {
		Location location = app.getDefaultRoutingConfig().getImpassableRoadLocations().get(object.getId());
		return location == null ? null : new LatLon(location.getLatitude(), location.getLongitude());
	}

	public interface AvoidSpecificRoadsCallback {

		void onAddImpassableRoad(boolean success, RouteDataObject newObject);

		boolean isCancelled();
	}
}
