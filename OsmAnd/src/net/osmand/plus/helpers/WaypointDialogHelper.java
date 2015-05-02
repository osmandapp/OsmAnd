package net.osmand.plus.helpers;

import java.util.ArrayList;
import java.util.List;

import net.osmand.data.LatLon;
import net.osmand.data.LocationPoint;
import net.osmand.data.PointDescription;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.IconsCache;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.WaypointHelper.LocationPointWrapper;
import net.osmand.plus.poi.PoiLegacyFilter;
import net.osmand.plus.views.AnimateDraggingMapThread;
import net.osmand.util.MapUtils;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnDismissListener;
import android.os.AsyncTask;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

/**
 */
public class WaypointDialogHelper {
	private MapActivity mapActivity;
	private OsmandApplication app;
	private WaypointHelper waypointHelper;


	private static class RadiusItem {
		int type;

		public RadiusItem(int type) {
			this.type = type;
		}
	}

	public WaypointDialogHelper(MapActivity mapActivity) {
		this.app = mapActivity.getMyApplication();
		waypointHelper = this.app.getWaypointHelper();
		this.mapActivity = mapActivity;

	}

	public static void updatePointInfoView(final OsmandApplication app, final Activity activity,
											View localView, final LocationPointWrapper ps, final boolean mapCenter) {
		WaypointHelper wh = app.getWaypointHelper();
		final LocationPoint point = ps.getPoint();
		TextView text = (TextView) localView.findViewById(R.id.waypoint_text);
		TextView textShadow = (TextView) localView.findViewById(R.id.waypoint_text_shadow);
		localView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				showOnMap(app, activity, point, mapCenter);
			}
		});
		TextView textDist = (TextView) localView.findViewById(R.id.waypoint_dist);
		((ImageView) localView.findViewById(R.id.waypoint_icon)).setImageDrawable(ps.getDrawable(activity, app));
		int dist = -1;
		if (!wh.isRouteCalculated()) {
			if (activity instanceof MapActivity) {
				dist = (int) MapUtils.getDistance(((MapActivity) activity).getMapView().getLatitude(), ((MapActivity) activity)
						.getMapView().getLongitude(), point.getLatitude(), point.getLongitude());
			}
		} else {
			dist = wh.getRouteDistance(ps);
		}
		if (dist > 0) {
			String dd = OsmAndFormatter.getFormattedDistance(dist, app);
			if (ps.deviationDistance > 0) {
				dd += "\n+" + OsmAndFormatter.getFormattedDistance(ps.deviationDistance, app);
			}
			textDist.setText(dd);
		} else {
			textDist.setText("");
		}
		String descr = PointDescription.getSimpleName(point, app);
		if(textShadow != null) {
			textShadow.setText(descr);
		}
		text.setText(descr);
		
//			((Spannable) text.getText()).setSpan(
//					new ForegroundColorSpan(ctx.getResources().getColor(R.color.color_distance)), 0, distance.length() - 1,
//					0);
	}


	public ArrayAdapter<Object> getWaypointsDrawerAdapter(
			final boolean edit, final List<LocationPointWrapper> deletedPoints,
			final MapActivity ctx, final int[] running, final boolean flat) {
		final List<Object> points;
		if(flat) {
			points = new ArrayList<Object>(waypointHelper.getAllPoints());
		} else {
			points = getPoints();
		}
		return new ArrayAdapter<Object>(ctx,
				R.layout.waypoint_reached, R.id.title, points) {

			@Override
			public View getView(final int position, View convertView, ViewGroup parent) {
				// User super class to create the View
				View v = convertView;
				final ArrayAdapter<Object> thisAdapter = this;
				boolean labelView = (getItem(position) instanceof Integer);
				if (getItem(position) instanceof RadiusItem) {
					final int type = ((RadiusItem) getItem(position)).type;
					v = createItemForRadiusProximity(ctx, type, running, position, thisAdapter);
				} else if (labelView) {
					v = createItemForCategory(ctx, (Integer) getItem(position), running, position, thisAdapter);
				} else {
					LocationPointWrapper point = (LocationPointWrapper) getItem(position);
					v = updateWaypointItemView(edit, deletedPoints, app, ctx, v, point, this);
				}
				return v;
			}


		};
	}

	
	
	public static View updateWaypointItemView(final boolean edit, final List<LocationPointWrapper> deletedPoints,
			final OsmandApplication app, final Activity ctx, View v, final LocationPointWrapper point,
			final ArrayAdapter adapter) {
		if (v == null || v.findViewById(R.id.info_close) == null) {
			v = ctx.getLayoutInflater().inflate(R.layout.waypoint_reached, null);
		}
		updatePointInfoView(app, ctx, v, point, true);
		View remove = v.findViewById(R.id.info_close);
		if (!edit) {
			remove.setVisibility(View.GONE);
		} else {
			remove.setVisibility(View.VISIBLE);
			((ImageButton) remove).setImageDrawable(app.getIconsCache().getContentIcon(
							R.drawable.ic_action_gremove_dark));
			remove.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					deletedPoints.add(point);
					if(adapter != null) {
						adapter.remove(point);
						adapter.notifyDataSetChanged();
					}
				}
			});
		}
		return v;
	}


	

	protected View createItemForRadiusProximity(final FragmentActivity ctx, final int type, final int[] running,
												final int position, final ArrayAdapter<Object> thisAdapter) {
		View v;
		IconsCache iconsCache  = app.getIconsCache();
		v = ctx.getLayoutInflater().inflate(R.layout.drawer_list_radius, null);
		final TextView radius = (TextView) v.findViewById(R.id.descr);
		((ImageView) v.findViewById(R.id.waypoint_icon)).setImageDrawable(iconsCache.getContentIcon(R.drawable.ic_poi_radius_dark));
		radius.setText(OsmAndFormatter.getFormattedDistance(waypointHelper.getSearchDeviationRadius(type), app));
		radius.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				selectDifferentRadius(type, running, position, thisAdapter, mapActivity);
			}

		});
		return v;
	}

	protected View createItemForCategory(final FragmentActivity ctx, final int type, final int[] running,
										 final int position, final ArrayAdapter<Object> thisAdapter) {
		View v;
		v = ctx.getLayoutInflater().inflate(R.layout.waypoint_header, null);
		final CompoundButton btn = (CompoundButton) v.findViewById(R.id.check_item);
		btn.setVisibility(waypointHelper.isTypeConfigurable(type) ? View.VISIBLE : View.GONE);
		btn.setOnCheckedChangeListener(null);
		final boolean checked = waypointHelper.isTypeEnabled(type);
		btn.setChecked(checked);
		btn.setEnabled(running[0] == -1);
		v.findViewById(R.id.ProgressBar).setVisibility(position == running[0] ? View.VISIBLE : View.GONE);
		btn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				running[0] = position;
				thisAdapter.notifyDataSetInvalidated();
				if (type == WaypointHelper.POI && isChecked) {
					selectPoi(running, thisAdapter, type, isChecked, ctx);
				} else {
					enableType(running, thisAdapter, type, isChecked);
				}
			}

		});

		TextView tv = (TextView) v.findViewById(R.id.header_text);
		tv.setText(getHeader(type, checked, ctx));
		v.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (type == WaypointHelper.POI && btn.isChecked()){
					running[0] = position;
					thisAdapter.notifyDataSetInvalidated();
					MapActivity map = (MapActivity) ctx;
					final PoiLegacyFilter[] selected = new PoiLegacyFilter[1];
					AlertDialog dlg = map.getMapLayers().selectPOIFilterLayer(map.getMapView(), selected);
					dlg.setOnDismissListener(new OnDismissListener() {
						@Override
						public void onDismiss(DialogInterface dialog) {
							if (selected != null) {
								enableType(running, thisAdapter, type, true);
							}
						}
					});
				}
			}
		});
		return v;
	}

	private void selectPoi(final int[] running, final ArrayAdapter<Object> listAdapter, final int type,
						   final boolean enable, Activity ctx) {
		if (ctx instanceof MapActivity &&
				!PoiLegacyFilter.CUSTOM_FILTER_ID.equals(app.getSettings().SELECTED_POI_FILTER_FOR_MAP.get())) {
			MapActivity map = (MapActivity) ctx;
			final PoiLegacyFilter[] selected = new PoiLegacyFilter[1];
			AlertDialog dlg = map.getMapLayers().selectPOIFilterLayer(map.getMapView(), selected);
			dlg.setOnDismissListener(new OnDismissListener() {

				@Override
				public void onDismiss(DialogInterface dialog) {
					if (selected != null) {
						enableType(running, listAdapter, type, enable);
					}
				}
			});
		} else {
			enableType(running, listAdapter, type, enable);
		}
	}

	private void enableType(final int[] running, final ArrayAdapter<Object> listAdapter, final int type,
							final boolean enable) {
		new AsyncTask<Void, Void, Void>() {

			@Override
			protected Void doInBackground(Void... params) {
				app.getWaypointHelper().enableWaypointType(type, enable);
				return null;
			}

			protected void onPostExecute(Void result) {
				running[0] = -1;
				listAdapter.clear();
				for (Object point : getPoints()) {
					listAdapter.add(point);
				}
				listAdapter.notifyDataSetChanged();
			}
		}.execute((Void) null);
	}

	public AdapterView.OnItemClickListener getDrawerItemClickListener(final FragmentActivity ctx, final int[] running,
			final ArrayAdapter<Object> listAdapter) {
		return new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> adapterView, View view, int item, long l) {
				if (listAdapter.getItem(item) instanceof LocationPointWrapper) {
					LocationPointWrapper ps = (LocationPointWrapper) listAdapter.getItem(item);
					showOnMap(app, ctx, ps.getPoint(), false);
//				} else if (new Integer(WaypointHelper.TARGETS).equals(listAdapter.getItem(item))) {
//					IntermediatePointsDialog.openIntermediatePointsDialog(ctx, app, true);
				} else if (listAdapter.getItem(item) instanceof RadiusItem) {
					selectDifferentRadius(((RadiusItem) listAdapter.getItem(item)).type, running, item, listAdapter,
							ctx);
				}
			}
		};
	}

	protected void selectDifferentRadius(final int type, final int[] running, final int position,
										 final ArrayAdapter<Object> thisAdapter, Activity ctx) {
		int length = WaypointHelper.SEARCH_RADIUS_VALUES.length;
		String[] names = new String[length];
		int selected = 0;
		for (int i = 0; i < length; i++) {
			names[i] = OsmAndFormatter.getFormattedDistance(WaypointHelper.SEARCH_RADIUS_VALUES[i], app);
			if (WaypointHelper.SEARCH_RADIUS_VALUES[i] == waypointHelper.getSearchDeviationRadius(type)) {
				selected = i;
			}
		}
		new AlertDialog.Builder(ctx)
				.setSingleChoiceItems(names, selected, new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialogInterface, int i) {
						int value = WaypointHelper.SEARCH_RADIUS_VALUES[i];
						if (waypointHelper.getSearchDeviationRadius(type) != value) {
							running[0] = position;
							waypointHelper.setSearchDeviationRadius(type, value);
							recalculatePoints(running, thisAdapter, type);
							dialogInterface.dismiss();
							thisAdapter.notifyDataSetInvalidated();
						}
					}
				}).setTitle(app.getString(R.string.search_radius_proximity))
				.setNegativeButton(R.string.shared_string_cancel, null)
				.show();
	}

	private void recalculatePoints(final int[] running, final ArrayAdapter<Object> listAdapter, final int type) {
		new AsyncTask<Void, Void, Void>() {

			@Override
			protected Void doInBackground(Void... params) {
				app.getWaypointHelper().recalculatePoints(type);
				return null;
			}

			protected void onPostExecute(Void result) {
				running[0] = -1;
				listAdapter.clear();
				for (Object point : getPoints()) {
					listAdapter.add(point);
				}
				listAdapter.notifyDataSetChanged();
			}
		}.execute((Void) null);
	}

	protected String getHeader(int type, boolean checked, Activity ctx) {
		String str = ctx.getString(R.string.waypoints);
		switch (type) {
			case WaypointHelper.TARGETS:
				str = ctx.getString(R.string.targets);
				break;
			case WaypointHelper.ALARMS:
				str = ctx.getString(R.string.way_alarms);
				break;
			case WaypointHelper.FAVORITES:
				str = ctx.getString(R.string.shared_string_my_favorites);
				break;
			case WaypointHelper.WAYPOINTS:
				str = ctx.getString(R.string.waypoints);
				break;
			case WaypointHelper.POI:
				str = waypointHelper.getPoiFilter() == null || !checked ? ctx.getString(R.string.poi) : waypointHelper
						.getPoiFilter().getName();
				break;
		}
		return str;
	}

	protected List<Object> getPoints() {
		final List<Object> points = new ArrayList<Object>();
		boolean rc = waypointHelper.isRouteCalculated();
		for (int i = 0; i < WaypointHelper.MAX; i++) {
			List<LocationPointWrapper> tp = waypointHelper.getWaypoints(i);
			if (!rc && i != WaypointHelper.WAYPOINTS && i != WaypointHelper.TARGETS) {
				// skip
			} else if (waypointHelper.isTypeVisible(i)) {
				points.add(new Integer(i));
				if ((i == WaypointHelper.POI || i == WaypointHelper.FAVORITES || i == WaypointHelper.WAYPOINTS)
						&& rc) {
					if (waypointHelper.isTypeEnabled(i)) {
						points.add(new RadiusItem(i));
					}
				}
				if (tp != null && tp.size() > 0) {
					points.addAll(tp);
				}
			}
		}
		return points;
	}

	public static void showOnMap(OsmandApplication app, Activity a, LocationPoint locationPoint, boolean center) {
		if (!(a instanceof MapActivity)) {
			return;
		}
		MapActivity ctx = (MapActivity) a;
		AnimateDraggingMapThread thread = ctx.getMapView().getAnimatedDraggingThread();
		int fZoom = ctx.getMapView().getZoom() < 15 ? 15 : ctx.getMapView().getZoom();
		double flat = locationPoint.getLatitude();
		double flon = locationPoint.getLongitude();
		if(!center) {
			RotatedTileBox cp = ctx.getMapView().getCurrentRotatedTileBox().copy();
			cp.setCenterLocation(0.5f, 0.25f);
			cp.setLatLonCenter(flat, flon);
			flat = cp.getLatFromPixel(cp.getPixWidth() / 2, cp.getPixHeight() / 2);
			flon = cp.getLonFromPixel(cp.getPixWidth() / 2, cp.getPixHeight() / 2);
		}
		if (thread.isAnimating()) {
			ctx.getMapView().setIntZoom(fZoom);
			ctx.getMapView().setLatLon(flat, flon);
			app.getAppCustomization().showLocationPoint(ctx, locationPoint);
		} else {
			final double dist = MapUtils.getDistance(ctx.getMapView().getLatitude(), ctx.getMapView().getLongitude(),
					locationPoint.getLatitude(), locationPoint.getLongitude());
			double t = 10;
			if (dist < t) {
				app.getAppCustomization().showLocationPoint(ctx, locationPoint);
			} else {
				thread.startMoving(flat, flon, fZoom, true);
			}
			if(ctx.getDashboard().isVisible()) {
				ctx.getDashboard().hideDashboard();
				ctx.getMapLayers().getContextMenuLayer().setSelectedObject(locationPoint);
				ctx.getMapLayers()
						.getContextMenuLayer()
						.setLocation(new LatLon(locationPoint.getLatitude(), locationPoint.getLongitude()),
								PointDescription.getSimpleName(locationPoint, ctx));
			}
		}
	}

}