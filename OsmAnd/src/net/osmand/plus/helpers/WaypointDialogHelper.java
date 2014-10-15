package net.osmand.plus.helpers;

import java.util.ArrayList;
import java.util.List;

import net.osmand.Location;
import net.osmand.data.LatLon;
import net.osmand.data.LocationPoint;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmAndLocationProvider.OsmAndLocationListener;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.PoiFilter;
import net.osmand.plus.R;
import net.osmand.plus.activities.IntermediatePointsDialog;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.WaypointHelper.LocationPointWrapper;
import net.osmand.plus.views.AnimateDraggingMapThread;
import net.osmand.plus.views.MapControlsLayer;
import net.osmand.util.MapUtils;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnDismissListener;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
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
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

/**
 */
public class WaypointDialogHelper implements OsmAndLocationListener {
	private MapActivity mapActivity;
	private OsmandApplication app;
	private LinearLayout mainLayout;
	private WaypointHelper waypointHelper;

	
	private static class RadiusItem {
		int type;
		public RadiusItem(int type) {
			this.type = type;
		}
	}

	public final static boolean OVERLAP_LAYOUT = true; // only true is supported
	private View closePointDialog;
	private List<LocationPointWrapper> many = new ArrayList<WaypointHelper.LocationPointWrapper>();


	public WaypointDialogHelper(MapActivity mapActivity) {
		this.app = mapActivity.getMyApplication();
		waypointHelper = this.app.getWaypointHelper();
		this.mapActivity = mapActivity;
		this.mainLayout = (LinearLayout) mapActivity.findViewById(R.id.dialog_layout);

	}

	public void init() {
		app.getLocationProvider().addLocationListener(this);
	}

	@Override
	public void updateLocation(Location location) {
		if (mapActivity != null) {
			updateDialog();
		}
	}

	public void removeListener() {
		app.getLocationProvider().removeLocationListener(this);
		mapActivity = null;
	}


	@SuppressWarnings("deprecation")
	public void updateDialog() {
		final LocationPointWrapper point = waypointHelper.getMostImportantLocationPoint(many);
		if (point == null) {
			removeDialog();
		} else {
			boolean created = false;
			View dlg = closePointDialog;
			if (dlg == null) {
				created = true;
				final LayoutInflater vi = (LayoutInflater) app.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				dlg = vi.inflate(R.layout.waypoint_reached, null);
			}
			updatePointInfoView(app, mapActivity, dlg, point, null);
			View all = dlg.findViewById(R.id.all_points);
			all.setVisibility(/*many.size() <= 1 ? View.GONE : */View.VISIBLE);
			View btnN = dlg.findViewById(R.id.info_close);
			btnN.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					waypointHelper.removeVisibleLocationPoint(point);
					updateDialog();
				}
			});
			if (created) {
				dlg.setBackgroundDrawable(mapActivity.getResources().getDrawable(R.drawable.view_black_selection));
				((TextView) dlg.findViewById(R.id.waypoint_text)).setTextColor(Color.WHITE);
				all.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						showWaypointsDialog(mapActivity);
					}
				});

				closePointDialog = dlg;
				mainLayout.addView(closePointDialog, getDialogLayoutParams());
				waitBeforeLayoutIsResized(dlg);
			}
			
		}
	}

	private static void updatePointInfoView(final OsmandApplication app, final Activity ctx,
											View localView, final LocationPointWrapper ps, final DialogFragment dialog) {
		WaypointHelper wh = app.getWaypointHelper();
		final LocationPoint point = ps.getPoint();
		TextView text = (TextView) localView.findViewById(R.id.waypoint_text);
		localView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				showOnMap(app, ctx, point, dialog);
			}
		});
		TextView textDist = (TextView) localView.findViewById(R.id.waypoint_dist);
		((ImageView) localView.findViewById(R.id.waypoint_icon)).setImageDrawable(ps.getDrawable(ctx));
		int dist = -1;
		if(!wh.isRouteCalculated()) {
			Location lm = app.getLocationProvider().getLastKnownLocation();
			if(lm != null) {
				dist = (int) MapUtils.getDistance(lm.getLatitude(), lm.getLongitude(), 
					point.getLatitude(), point.getLongitude());
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

	public static void showWaypointsDialogFlat(FragmentActivity fragmentActivity) {
		Bundle args = new Bundle();
		args.putBoolean(WaypointDialogFragment.FLAT_ARG, true);
		WaypointDialogFragment wdf = new WaypointDialogFragment();
		wdf.setArguments(args);
		fragmentActivity.getSupportFragmentManager().beginTransaction().add(wdf, "tag").commit();
	}

	public static void showWaypointsDialog(FragmentActivity fragmentActivity) {
		Bundle args = new Bundle();
		WaypointDialogFragment wdf = new WaypointDialogFragment();
		wdf.setArguments(args);
		fragmentActivity.getSupportFragmentManager().beginTransaction().add(wdf, "tag").commit();
	}

	public static class WaypointDialogFragment extends DialogFragment {

		WaypointHelper waypointHelper;
		private OsmandApplication app;

		public static final String FLAT_ARG = "FLAT_ARG";

		@Override
		public void onAttach(Activity activity) {
			super.onAttach(activity);
			app = (OsmandApplication) activity.getApplication();
			waypointHelper = app.getWaypointHelper();
		}

		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			if (getArguments() != null && getArguments().getBoolean(FLAT_ARG)) {
				return createWaypointsDialogFlat(waypointHelper.getAllPoints());
			}
			return createWaypointsDialog();
		}

		private void selectPoi(final int[] running, final ArrayAdapter<Object> listAdapter, final int type,
							   final boolean enable) {
			if (getActivity() instanceof MapActivity && !PoiFilter.CUSTOM_FILTER_ID.equals(app.getSettings().getPoiFilterForMap())) {
				MapActivity map = (MapActivity) getActivity();
				final PoiFilter[] selected = new PoiFilter[1];
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

		private void recalculatePoints(final int[] running, final ArrayAdapter<Object> listAdapter, final int type){
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

		protected String getHeader(int type, boolean checked) {
			FragmentActivity ctx = getActivity();
			String str = ctx.getString(R.string.waypoints);
			switch (type) {
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
					str = waypointHelper.getPoiFilter() == null || !checked ? ctx.getString(R.string.poi) : waypointHelper
							.getPoiFilter().getName();
					break;
			}
			return str;
		}

		public AlertDialog createWaypointsDialogFlat(final List<LocationPointWrapper> points) {
			final List<LocationPointWrapper> deletedPoints = new ArrayList<WaypointHelper.LocationPointWrapper>();
			final FragmentActivity ctx = getActivity();
			final ArrayAdapter<LocationPointWrapper> listAdapter = new ArrayAdapter<LocationPointWrapper>(ctx, R.layout.waypoint_reached, R.id.title,
					points) {
				@Override
				public View getView(final int position, View convertView, ViewGroup parent) {
					// User super class to create the View
					View v = convertView;
					if (v == null) {
						v = ctx.getLayoutInflater().inflate(R.layout.waypoint_reached, null);
					}
					updatePointInfoView(app, ctx, v, getItem(position), WaypointDialogFragment.this);
					View remove = v.findViewById(R.id.info_close);
					((ImageButton) remove).setImageDrawable(ctx.getResources().getDrawable(
							app.getSettings().isLightContent() ? R.drawable.ic_action_gremove_light :
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
					showOnMap(app, ctx, ps.getPoint(), WaypointDialogFragment.this);
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
			return builder.create();
		}

		public AlertDialog createWaypointsDialog() {
			final List<Object> points = getPoints();
			final List<LocationPointWrapper> deletedPoints = new ArrayList<WaypointHelper.LocationPointWrapper>();
			final FragmentActivity ctx = getActivity();
			final ListView listView = new ListView(ctx);
			final int[] running = new int[]{-1};
			final ArrayAdapter<Object> listAdapter = new ArrayAdapter<Object>(ctx,
					R.layout.waypoint_reached, R.id.title, points) {

				@Override
				public View getView(final int position, View convertView, ViewGroup parent) {
					// User super class to create the View
					View v = convertView;
					final ArrayAdapter<Object> thisAdapter = this;
					boolean labelView = (getItem(position) instanceof Integer);
					boolean viewText = v != null && v.findViewById(R.id.info_close) == null;
					if (getItem(position) instanceof RadiusItem	){
						final int type = ((RadiusItem) getItem(position)).type;
						v = createItemForRadiusProximity(ctx, type, running, position, thisAdapter);
					} else if (labelView) {
						v = createItemForCategory(ctx, (Integer) getItem(position), running, position, thisAdapter);
					} else {
						v = ctx.getLayoutInflater().inflate(R.layout.waypoint_reached, null);
						updatePointInfoView(app, ctx, v, (LocationPointWrapper) getItem(position), WaypointDialogFragment.this);
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

			listView.setAdapter(listAdapter);
			listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> adapterView, View view, int item, long l) {
					if (listAdapter.getItem(item) instanceof LocationPointWrapper) {
						LocationPointWrapper ps = (LocationPointWrapper) listAdapter.getItem(item);
						showOnMap(app, ctx, ps.getPoint(), WaypointDialogFragment.this);
					}
				}
			});
			AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
			builder.setView(listView);
			builder.setNeutralButton(R.string.flat_list_waypoints, new OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					showWaypointsDialogFlat(getActivity());
				}

			});
			builder.setPositiveButton(R.string.default_buttons_ok, new OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					waypointHelper.removeVisibleLocationPoint(deletedPoints);
				}
			});
			builder.setNegativeButton(ctx.getString(R.string.default_buttons_cancel), null);
			return builder.create();
		}

		protected List<Object> getPoints() {
			final List<Object> points = new ArrayList<Object>();
			boolean rc = waypointHelper.isRouteCalculated();
			for (int i = 0; i < WaypointHelper.MAX; i++) {
				List<LocationPointWrapper> tp = waypointHelper.getWaypoints(i);
				if(!rc && i != WaypointHelper.WAYPOINTS && i != WaypointHelper.TARGETS) {
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
		
		protected View createItemForRadiusProximity(final FragmentActivity ctx, final int type, final int[] running,
				final int position, final ArrayAdapter<Object> thisAdapter) {
			View v;
			v = ctx.getLayoutInflater().inflate(R.layout.drawer_list_radius, null);
			final TextView radius = (TextView) v.findViewById(R.id.descr);
			radius.setText(OsmAndFormatter.getFormattedDistance(waypointHelper.getSearchDeviationRadius(type), app));
			radius.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					int length = WaypointHelper.SEARCH_RADIUS_VALUES.length;
					String[] names = new String[length];
					int selected = 0;
					for (int i = 0; i < length; i++) {
						names[i] = OsmAndFormatter.getFormattedDistance(WaypointHelper.SEARCH_RADIUS_VALUES[i], app);
						if (WaypointHelper.SEARCH_RADIUS_VALUES[i] == waypointHelper.getSearchDeviationRadius(type)){
							selected = i;
						}
					}
					new AlertDialog.Builder(getActivity())
							.setSingleChoiceItems(names, selected, new OnClickListener() {
								@Override
								public void onClick(DialogInterface dialogInterface, int i) {
									int value = WaypointHelper.SEARCH_RADIUS_VALUES[i];
									if (waypointHelper.getSearchDeviationRadius(type) != value){
										running[0] = position;
										waypointHelper.setSearchDeviationRadius(type, value);
										radius.setText(OsmAndFormatter.getFormattedDistance(value, app));
										recalculatePoints(running, thisAdapter, WaypointHelper.POI);
										dialogInterface.dismiss();
									}
								}
							}).setTitle(app.getString(R.string.search_radius_proximity))
							.setNegativeButton(R.string.default_buttons_cancel, null)
							.show();
				}
			});
			return v;
		}
		
		protected View createItemForCategory(final FragmentActivity ctx, final int type, final int[] running,
				final int position, final ArrayAdapter<Object> thisAdapter) {
			View v;
			v = ctx.getLayoutInflater().inflate(R.layout.waypoint_header, null);
			ImageView sort = (ImageView) v.findViewById(R.id.sort);
			//sort button in Destination header
			if (type == 0 && sort != null){
				sort.setVisibility(View.VISIBLE);
				if (app.getSettings().isLightContent()){
					sort.setImageResource(R.drawable.ic_sort_waypoint_white);
				} else {
					sort.setImageResource(R.drawable.ic_sort_waypoint_dark);
				}
				sort.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						IntermediatePointsDialog.openIntermediatePointsDialog(ctx, app, true);
					}
				});
			}
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
						selectPoi(running, thisAdapter, type, isChecked);
					} else {
						enableType(running, thisAdapter, type, isChecked);
					}
				}

			});
			TextView tv = (TextView) v.findViewById(R.id.header_text);
			tv.setText(getHeader(type, checked));
			return v;
		}
	}
	
	


	private static void showOnMap(OsmandApplication app, Activity a, LocationPoint locationPoint, DialogFragment dialog) {
		if (!(a instanceof MapActivity)) {
			return;
		}
		MapActivity ctx = (MapActivity) a;
		AnimateDraggingMapThread thread = ctx.getMapView().getAnimatedDraggingThread();
		int fZoom = ctx.getMapView().getZoom() < 15 ? 15 : ctx.getMapView().getZoom();
		boolean di = dialog != null;
		if (thread.isAnimating()) {
			ctx.getMapView().setIntZoom(fZoom);
			ctx.getMapView().setLatLon(locationPoint.getLatitude(), locationPoint.getLongitude());
			app.getAppCustomization().showLocationPoint(ctx, locationPoint);
		} else {
			final double dist = MapUtils.getDistance(ctx.getMapView().getLatitude(), ctx.getMapView().getLongitude(),
					locationPoint.getLatitude(), locationPoint.getLongitude());
			double t = 10;
			if (dist < t) {
				app.getAppCustomization().showLocationPoint(ctx, locationPoint);
			} else {
				thread.startMoving(locationPoint.getLatitude(), locationPoint.getLongitude(), fZoom, true);
			}
			if (di) {
				ctx.getMapLayers().getContextMenuLayer().setSelectedObject(locationPoint);
				ctx.getMapLayers()
						.getContextMenuLayer()
						.setLocation(new LatLon(locationPoint.getLatitude(), locationPoint.getLongitude()),
								locationPoint.getName(ctx));
				dialog.dismiss();
			}
		}
	}


}
