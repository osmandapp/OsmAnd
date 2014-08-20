package net.osmand.plus.helpers;

import java.util.ArrayList;
import java.util.List;

import net.osmand.Location;
import net.osmand.data.LatLon;
import net.osmand.data.LocationPoint;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmAndLocationProvider.OsmAndLocationListener;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.WaypointHelper.LocationPointWrapper;
import net.osmand.plus.views.AnimateDraggingMapThread;
import net.osmand.plus.views.MapControlsLayer;
import net.osmand.util.MapUtils;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.SystemClock;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

/**
 */
public class WaypointDialogHelper implements OsmAndLocationListener {
	private MapActivity mapActivity;
	private OsmandApplication app;
	private FrameLayout mainLayout;
	private WaypointHelper waypointHelper;

	public final static boolean OVERLAP_LAYOUT = true; // only true is supported
	private View closePointDialog;
	private List<LocationPointWrapper> many = new ArrayList<WaypointHelper.LocationPointWrapper>();
	private static AlertDialog dialog;


	public WaypointDialogHelper(MapActivity mapActivity) {
		this.app = mapActivity.getMyApplication();
		waypointHelper = this.app.getWaypointHelper();
		this.mapActivity = mapActivity;
		this.mainLayout = (FrameLayout) ((FrameLayout) mapActivity.getLayout()).getChildAt(0);
	}
	
	public void init() {
		app.getLocationProvider().addLocationListener(this);
	}
	
	@Override
	public void updateLocation(Location location) {
		if(mapActivity != null) {
			updateDialog();
		}
	}
	
	public void removeListener() {
		app.getLocationProvider().removeLocationListener(this);
		mapActivity = null;
	}
	

	public void updateDialog() {
		final LocationPointWrapper point = waypointHelper.getMostImportantLocationPoint(many);
		if (point == null) {
			removeDialog();
		} else {
			boolean created = false;
			if (closePointDialog == null) {
				created = true;
				final LayoutInflater vi = (LayoutInflater) app.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				closePointDialog = vi.inflate(R.layout.waypoint_reached, null);
			}
			updatePointInfoView(app, mapActivity, closePointDialog, point);
			View all = closePointDialog.findViewById(R.id.all_points);
			all.setVisibility(/*many.size() <= 1 ? View.GONE : */View.VISIBLE);
			if (created) {
				closePointDialog.setBackgroundColor(mapActivity.getResources().getColor(R.color.color_black));
				((TextView) closePointDialog.findViewById(R.id.waypoint_text)).setTextColor(Color.WHITE);
				all.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						 showWaypointsDialog(app, mapActivity);
					}
				});


				mainLayout.addView(closePointDialog, getDialogLayoutParams());
				waitBeforeLayoutIsResized(closePointDialog);
			}
			View btnN = closePointDialog.findViewById(R.id.info_close);
			btnN.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					waypointHelper.removeVisibleLocationPoint(point);
					updateDialog();
				}
			});
		}
	}

	private static void updatePointInfoView(final OsmandApplication app, final Activity ctx,
			View localView, final LocationPointWrapper ps) {
		WaypointHelper wh = app.getWaypointHelper();
		final LocationPoint point = ps.getPoint();
		TextView text = (TextView) localView.findViewById(R.id.waypoint_text);
		text.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if(ctx instanceof MapActivity) {
					showOnMap(app, (MapActivity) ctx, point);
				}
			}
		});
		TextView textDist = (TextView) localView.findViewById(R.id.waypoint_dist);
		((ImageView) localView.findViewById(R.id.waypoint_icon)).setImageDrawable(ps.getDrawable(ctx));
//		Location lastKnownMapLocation = app.getLocationProvider().getLastKnownLocation();
		int dist = wh.getRouteDistance(ps);
		String dd = OsmAndFormatter.getFormattedDistance(dist, app);
		if (ps.deviationDistance > 0) {
			dd += "\n+" + OsmAndFormatter.getFormattedDistance(ps.deviationDistance, app);
		}
		textDist.setText(dd);
		text.setText(point.getName(app));
//			((Spannable) text.getText()).setSpan(
//					new ForegroundColorSpan(ctx.getResources().getColor(R.color.color_distance)), 0, distance.length() - 1,
//					0);
	}

	public void removeDialog() {
		if (closePointDialog != null) {
			mainLayout.removeView(closePointDialog);
			closePointDialog = null;
			shiftButtons(0);
		}
	}

	private FrameLayout.LayoutParams getDialogLayoutParams() {
		FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
		params.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
		return params;
	}

	private void shiftButtons(int height) {
		MapControlsLayer mapControls = mapActivity.getMapLayers().getMapControlsLayer();
		if (mapControls != null) {
			mapControls.shiftLayout(height);
		}
	}


	private void waitBeforeLayoutIsResized(View reachedView) {
		//this async task is needed because layout height is not set
		// right after you add it so we need to w8 some time
		new AsyncTask<View, Void, Void>() {
			int height;

			@Override
			protected Void doInBackground(View... params) {
				for (int i = 0; i < 10; i++) {
					SystemClock.sleep(50);
					height = params[0].getHeight();
					if (params[0].getHeight() > 0) {
						break;
					}
				}
				return null;
			}

			protected void onPostExecute(Void result) {
				if (height > 0 && OVERLAP_LAYOUT) {
					shiftButtons(height);
				}
			}
		}.execute(reachedView);
	}

	private static void enableType(OsmandApplication app, MapActivity ctx, int type) {
		// TODO Auto-generated method stub
	}

	public static void showWaypointsDialog(final OsmandApplication app, 
			final MapActivity ctx) {
		final WaypointHelper waypointHelper = app.getWaypointHelper();
		final List<Object> points = new ArrayList<Object>();
		for (int i = 0; i < WaypointHelper.MAX; i++) {
			List<LocationPointWrapper> tp = waypointHelper.getWaypoints(i);
			points.add(new Integer(i));
			if (tp != null && tp.size() > 0) {
				points.addAll(tp);
			}
		}
		final List<LocationPointWrapper> deletedPoints = new ArrayList<WaypointHelper.LocationPointWrapper>();
		final ArrayAdapter<Object> listAdapter = new ArrayAdapter<Object>(ctx,
				R.layout.waypoint_reached, R.id.title, points) {
			@Override
			public View getView(final int position, View convertView, ViewGroup parent) {
				// User super class to create the View
				View v = convertView;
				boolean labelView = (getItem(position) instanceof Integer);
				boolean viewText = v != null && v.findViewById(R.id.info_close) == null;
				if (v == null || viewText != labelView) {
					v = ctx.getLayoutInflater().inflate(labelView ? R.layout.waypoint_header : R.layout.waypoint_reached, null);
				}
				if (labelView) {
					final int type = (Integer) getItem(position);
					CompoundButton btn = (CompoundButton) v.findViewById(R.id.check_item);
					btn.setVisibility(type == WaypointHelper.TARGETS ? View.GONE : View.VISIBLE);
					btn.setOnClickListener(new View.OnClickListener() {
						
						@Override
						public void onClick(View v) {
							enableType(app, ctx, type);
						}

					});
					TextView tv = (TextView) v.findViewById(R.id.header_text);
					tv.setText(getHeader(ctx, waypointHelper, type));
				} else {
					updatePointInfoView(app, ctx, v, (LocationPointWrapper) getItem(position));
					View remove = v.findViewById(R.id.info_close);
					((ImageButton) remove).setImageDrawable(ctx.getResources().getDrawable(
							app.getSettings().isLightContent() ? R.drawable.ic_action_gremove_light
									: R.drawable.ic_action_gremove_dark));
					remove.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View view) {
							LocationPointWrapper point = (LocationPointWrapper) points.get(position);
							remove(point);
							deletedPoints.add(point);
							notifyDataSetChanged();
						}
					});
				}
				return v;
			}
		};
		ListView listView = new ListView(ctx);
		listView.setAdapter(listAdapter);
		listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
				if(listAdapter.getItem(i) instanceof LocationPointWrapper) {
					LocationPointWrapper ps = (LocationPointWrapper) listAdapter.getItem(i);
					showOnMap(app, ctx, ps.getPoint());
				}
			}
		});
		AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
		builder.setView(listView);
		builder.setNeutralButton(R.string.flat_list_waypoints, new OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				showWaypointsDialogFlat(app, ctx, waypointHelper.getAllPoints());
				
			}
		});
		builder.setPositiveButton(R.string.default_buttons_ok, new OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				waypointHelper.removeVisibleLocationPoint(deletedPoints);
			}
		});
		builder.setNegativeButton(ctx.getString(R.string.default_buttons_cancel), null);
		dialog = builder.show();
	}

	protected static String getHeader(final MapActivity ctx, final WaypointHelper waypointHelper, int i) {
		String str = ctx.getString(R.string.waypoints);
		switch (i) {
		case WaypointHelper.TARGETS:
			str = ctx.getString(R.string.targets);
			break;
		case WaypointHelper.ALARMS:
			str = ctx.getString(R.string.way_alarms);
			break;
		case WaypointHelper.FAVORITES:
			str = ctx.getString(R.string.my_favorites);
			break;
		case WaypointHelper.WAYPOINTS:
			str = ctx.getString(R.string.waypoints);
			break;
		case WaypointHelper.POI:
			str = waypointHelper.getPoiFilter() == null ? ctx.getString(R.string.poi) : waypointHelper
					.getPoiFilter().getName();
			break;
		}
		return str;
	}
	
	public static void showWaypointsDialogFlat(final OsmandApplication app, final MapActivity ctx, final List<LocationPointWrapper> points){
		final WaypointHelper waypointHelper = app.getWaypointHelper();
		final List<LocationPointWrapper> deletedPoints = new ArrayList<WaypointHelper.LocationPointWrapper>();
		final ArrayAdapter<LocationPointWrapper> listAdapter = new ArrayAdapter<LocationPointWrapper>(ctx, R.layout.waypoint_reached, R.id.title,
				points) {
			@Override
			public View getView(final int position, View convertView, ViewGroup parent) {
				// User super class to create the View
				View v = convertView;
				if (v == null) {
					v = ctx.getLayoutInflater().inflate(R.layout.waypoint_reached, null);
				}
				updatePointInfoView(app, ctx, v, getItem(position));
				View remove = v.findViewById(R.id.info_close);
				((ImageButton) remove).setImageDrawable(ctx.getResources().getDrawable(
						app.getSettings().isLightContent()? R.drawable.ic_action_gremove_light:
								R.drawable.ic_action_gremove_dark));
				remove.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						LocationPointWrapper point = points.get(position);
						remove(point);
						deletedPoints.add(point);
						notifyDataSetChanged();
					}
				});

				return v;
			}
		};

		ListView listView = new ListView(ctx);
		listView.setAdapter(listAdapter);
		listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
				LocationPointWrapper ps = listAdapter.getItem(i);
				showOnMap(app, ctx, ps.getPoint());
			}
		});
		AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
		builder.setView(listView);
		builder.setPositiveButton(R.string.default_buttons_ok, new OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				waypointHelper.removeVisibleLocationPoint(deletedPoints);
			}
		});
		builder.setNegativeButton(ctx.getString(R.string.default_buttons_cancel), null);
		dialog = builder.show();
	}

	private static void showOnMap(OsmandApplication app, MapActivity ctx, LocationPoint locationPoint) {
		AnimateDraggingMapThread thread = ctx.getMapView().getAnimatedDraggingThread();
		int fZoom = ctx.getMapView().getZoom() < 15 ? 15 : ctx.getMapView().getZoom();
		boolean di = dialog != null && dialog.isShowing();
		if (thread.isAnimating() && !di) {
			ctx.getMapView().setIntZoom(fZoom);
			ctx.getMapView().setLatLon(locationPoint.getLatitude(), locationPoint.getLongitude());
		} else {
			final double dist = MapUtils.getDistance(ctx.getMapView().getLatitude(), ctx.getMapView().getLongitude(),
					locationPoint.getLatitude(), locationPoint.getLongitude());
			double t = 10;
			if (dist < t || di) {
				ctx.getMapLayers().getContextMenuLayer().setSelectedObject(locationPoint);
				ctx.getMapLayers()
						.getContextMenuLayer()
						.setLocation(new LatLon(locationPoint.getLatitude(), locationPoint.getLongitude()),
								locationPoint.getName(ctx));
			}
			if (di || dist >= t) {
				thread.startMoving(locationPoint.getLatitude(), locationPoint.getLongitude(), fZoom, true);
			}
			if(di) {
				dialog.dismiss();
				dialog = null;
			}
		}
	}




}
