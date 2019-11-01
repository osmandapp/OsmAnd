package net.osmand.plus.helpers;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.LayoutInflater;
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
import net.osmand.plus.ApplicationMode;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.MapContextMenu;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.views.ContextMenuLayer;
import net.osmand.util.MapUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public class AvoidSpecificRoads {

	private OsmandApplication app;

	private Map<LatLon, RouteDataObject> impassableRoads = new LinkedHashMap<>();

	public AvoidSpecificRoads(final OsmandApplication app) {
		this.app = app;
		for (LatLon latLon : app.getSettings().getImpassableRoadPoints()) {
			impassableRoads.put(latLon, null);
		}
	}

	public Map<LatLon, RouteDataObject> getImpassableRoads() {
		return impassableRoads;
	}

	public void initRouteObjects() {
		for (LatLon latLon : impassableRoads.keySet()) {
			addImpassableRoad(null, latLon, false, true);
		}
	}

	private ArrayAdapter<LatLon> createAdapter(MapActivity mapActivity, boolean nightMode) {
		final ArrayList<LatLon> points = new ArrayList<>(impassableRoads.keySet());
		final LatLon mapLocation = mapActivity.getMapLocation();
		final LayoutInflater inflater = UiUtilities.getInflater(mapActivity, nightMode);
		Context themedContext = UiUtilities.getThemedContext(mapActivity, nightMode);

		return new ArrayAdapter<LatLon>(themedContext, R.layout.waypoint_reached, R.id.title, points) {
			@NonNull
			@Override
			public View getView(final int position, View convertView, @NonNull ViewGroup parent) {
				View v = convertView;
				if (v == null || v.findViewById(R.id.info_close) == null) {
					v = inflater.inflate(R.layout.waypoint_reached, parent, false);
				}
				final LatLon item = getItem(position);
				v.findViewById(R.id.all_points).setVisibility(View.GONE);
				((ImageView) v.findViewById(R.id.waypoint_icon))
						.setImageDrawable(getIcon(R.drawable.ic_action_road_works_dark));
				((TextView) v.findViewById(R.id.waypoint_dist)).setText(getDist(mapLocation, item));
				((TextView) v.findViewById(R.id.waypoint_text)).setText(getText(item));
				ImageButton remove = (ImageButton) v.findViewById(R.id.info_close);
				remove.setVisibility(View.VISIBLE);
				remove.setImageDrawable(getIcon(R.drawable.ic_action_remove_dark));
				remove.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						remove(item);
						removeImpassableRoad(item);
						notifyDataSetChanged();
						recalculateRoute();
					}
				});
				return v;
			}
		};
	}

	private Drawable getIcon(@DrawableRes int iconId) {
		return app.getUIUtilities().getThemedIcon(iconId);
	}

	private String getDist(@NonNull LatLon loc, @Nullable LatLon point) {
		double dist = point == null ? 0 : MapUtils.getDistance(loc, point);
		return OsmAndFormatter.getFormattedDistance((float) dist, app);
	}

	public String getText(@Nullable LatLon point) {
		if (point != null) {
			RouteDataObject obj = impassableRoads.get(point);
			if (obj != null) {
				String locale = app.getSettings().MAP_PREFERRED_LOCALE.get();
				boolean transliterate = app.getSettings().MAP_TRANSLITERATE_NAMES.get();
				String name = RoutingHelper.formatStreetName(
						obj.getName(locale, transliterate),
						obj.getRef(locale, transliterate, true),
						obj.getDestinationName(locale, transliterate, true),
						app.getString(R.string.towards)
				);
				if (!TextUtils.isEmpty(name)) {
					return name;
				}
			}
		}
		return app.getString(R.string.shared_string_road);
	}

	public String getText(@Nullable RouteDataObject obj) {
		if (obj != null) {
			String locale = app.getSettings().MAP_PREFERRED_LOCALE.get();
			boolean transliterate = app.getSettings().MAP_TRANSLITERATE_NAMES.get();
			String name = RoutingHelper.formatStreetName(
					obj.getName(locale, transliterate),
					obj.getRef(locale, transliterate, true),
					obj.getDestinationName(locale, transliterate, true),
					app.getString(R.string.towards)
			);
			if (!TextUtils.isEmpty(name)) {
				return name;
			}
		}
		return app.getString(R.string.shared_string_road);
	}

	private void recalculateRoute() {
		RoutingHelper rh = app.getRoutingHelper();
		if (rh.isRouteCalculated() || rh.isRouteBeingCalculated()) {
			rh.recalculateRouteDueToSettingsChange();
		}
	}

	public void removeImpassableRoad(LatLon latLon) {
		app.getSettings().removeImpassableRoad(latLon);
		RouteDataObject obj = impassableRoads.remove(latLon);
		if (obj != null) {
			app.getRoutingConfig().removeImpassableRoad(obj);
		}
	}

	public void removeImpassableRoad(RouteDataObject obj) {
		removeImpassableRoad(getLocation(obj));
	}

	public void showDialog(@NonNull final MapActivity mapActivity) {
		boolean nightMode = app.getDaynightHelper().isNightModeForMapControls();
		Context themedContext = UiUtilities.getThemedContext(mapActivity, nightMode);

		AlertDialog.Builder bld = new AlertDialog.Builder(themedContext);
		bld.setTitle(R.string.impassable_road);
		if (impassableRoads.isEmpty()) {
			bld.setMessage(R.string.avoid_roads_msg);
		} else {
			final ArrayAdapter<LatLon> listAdapter = createAdapter(mapActivity, nightMode);
			bld.setAdapter(listAdapter, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					LatLon point = listAdapter.getItem(which);
					if (point != null) {
						showOnMap(mapActivity, point.getLatitude(), point.getLongitude(), getText(point));
					}
					dialog.dismiss();
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

	public void selectFromMap(final MapActivity mapActivity) {
		ContextMenuLayer cm = mapActivity.getMapLayers().getContextMenuLayer();
		cm.setSelectOnMap(new CallbackWithObject<LatLon>() {
			@Override
			public boolean processResult(LatLon result) {
				addImpassableRoad(mapActivity, result, true, false);
				return true;
			}
		});
	}

	public void addImpassableRoad(@Nullable final MapActivity activity,
								  @NonNull final LatLon loc,
								  final boolean showDialog,
								  final boolean skipWritingSettings) {
		final Location ll = new Location("");
		ll.setLatitude(loc.getLatitude());
		ll.setLongitude(loc.getLongitude());
		ApplicationMode appMode = app.getRoutingHelper().getAppMode();

		app.getLocationProvider().getRouteSegment(ll, appMode, false, new ResultMatcher<RouteDataObject>() {

			@Override
			public boolean publish(RouteDataObject object) {
				if (object == null) {
					if (activity != null) {
						Toast.makeText(activity, R.string.error_avoid_specific_road, Toast.LENGTH_LONG).show();
					}
				} else {
					addImpassableRoadInternal(object, ll, showDialog, activity, loc);
				}
				return true;
			}

			@Override
			public boolean isCancelled() {
				return false;
			}

		});
		if (!skipWritingSettings) {
			app.getSettings().addImpassableRoad(loc.getLatitude(), loc.getLongitude());
		}
	}

	public void replaceImpassableRoad(final MapActivity activity,
									  final RouteDataObject currentObject,
									  final LatLon newLoc,
									  final boolean showDialog,
									  final AvoidSpecificRoadsCallback callback) {
		final Location ll = new Location("");
		ll.setLatitude(newLoc.getLatitude());
		ll.setLongitude(newLoc.getLongitude());
		ApplicationMode appMode = app.getRoutingHelper().getAppMode();

		app.getLocationProvider().getRouteSegment(ll, appMode, false, new ResultMatcher<RouteDataObject>() {

			@Override
			public boolean publish(RouteDataObject object) {
				if (object == null) {
					Toast.makeText(activity, R.string.error_avoid_specific_road, Toast.LENGTH_LONG).show();
					if (callback != null) {
						callback.onAddImpassableRoad(false, null);
					}
				} else {
					final LatLon oldLoc = getLocation(currentObject);
					app.getSettings().moveImpassableRoad(oldLoc, newLoc);
					impassableRoads.remove(oldLoc);
					app.getRoutingConfig().removeImpassableRoad(currentObject);
					addImpassableRoadInternal(object, ll, showDialog, activity, newLoc);

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
		if (app.getRoutingConfig().addImpassableRoad(object, ll)) {
			impassableRoads.put(loc, object);
		} else {
			LatLon location = getLocation(object);
			if (location != null) {
				app.getSettings().removeImpassableRoad(location);
			}
		}
		recalculateRoute();
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

	private void showOnMap(MapActivity ctx, double lat, double lon, String name) {
		int zoom = ctx.getMapView().getZoom() < 15 ? 15 : ctx.getMapView().getZoom();
		PointDescription pd = new PointDescription("", name);
		ctx.getMyApplication().getSettings().setMapLocationToShow(lat, lon, zoom, pd, false, null);
		MapActivity.launchMapActivityMoveToTop(ctx);
	}

	public LatLon getLocation(RouteDataObject object) {
		Location location = app.getRoutingConfig().getImpassableRoadLocations().get(object.getId());
		return location == null ? null : new LatLon(location.getLatitude(), location.getLongitude());
	}

	public interface AvoidSpecificRoadsCallback {

		void onAddImpassableRoad(boolean success, RouteDataObject newObject);

		boolean isCancelled();
	}
}
