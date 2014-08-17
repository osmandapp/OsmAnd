package net.osmand.plus.helpers;

import java.util.List;

import net.osmand.Location;
import net.osmand.data.LocationPoint;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.FavoriteImageDrawable;
import net.osmand.plus.helpers.WaypointHelper.LocationPointWrapper;
import net.osmand.plus.views.MapControlsLayer;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
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
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

/**
 */
public class WaypointDialogHelper {
	private MapActivity mapActivity;
	private OsmandApplication app;
	private FrameLayout mainLayout;
	private WaypointHelper waypointHelper;

	public final static boolean OVERLAP_LAYOUT = true; // only true is supported
	private View closePointDialog;


	public WaypointDialogHelper(MapActivity mapActivity) {
		this.app = mapActivity.getMyApplication();
		waypointHelper = this.app.getWaypointHelper();
		this.mapActivity = mapActivity;
		this.mainLayout = (FrameLayout) ((FrameLayout) mapActivity.getLayout()).getChildAt(0);
	}

	public void updateDialog() {
		List<LocationPointWrapper> vlp = waypointHelper.getWaypoints(WaypointHelper.FAVORITES);
		if (vlp.isEmpty()) {
			removeDialog();
		} else {
			final LocationPointWrapper point = vlp.get(0);
			boolean created = false;
			if (closePointDialog == null) {
				created = true;
				final LayoutInflater vi = (LayoutInflater) app.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				closePointDialog = vi.inflate(R.layout.waypoint_reached, null);
			}
			updatePointInfoView(app, mapActivity, closePointDialog, point);
			View all = closePointDialog.findViewById(R.id.all_points);
			all.setVisibility(vlp.size() <= 1 ? View.GONE : View.VISIBLE);
			if (created) {
				closePointDialog.setBackgroundColor(mapActivity.getResources().getColor(R.color.color_black));
				((TextView) closePointDialog.findViewById(R.id.waypoint_text)).setTextColor(Color.WHITE);
				all.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						 showAllDialog(app, mapActivity, waypointHelper.getAllPoints());
					}
				});

				View btnN = closePointDialog.findViewById(R.id.info_close);
				btnN.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						waypointHelper.removeVisibleLocationPoint(point);
						updateDialog();
					}
				});

				mainLayout.addView(closePointDialog, getDialogLayoutParams());
				waitBeforeLayoutIsResized(closePointDialog);
			}
		}
	}

	private static void updatePointInfoView(OsmandApplication app, Context ctx,
			View localView, final LocationPointWrapper ps) {
		WaypointHelper wh = app.getWaypointHelper();
		LocationPoint point = ps.getPoint();
		TextView text = (TextView) localView.findViewById(R.id.waypoint_text);
		text.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				// TODO
				//itemClick(point);
			}
		});
		TextView textDist = (TextView) localView.findViewById(R.id.waypoint_text);
		((ImageView) localView.findViewById(R.id.waypoint_icon)).setImageDrawable(FavoriteImageDrawable.getOrCreate(ctx, point.getColor()));
		Location lastKnownMapLocation = app.getLocationProvider().getLastKnownLocation();
		String dd = "";
		if (lastKnownMapLocation != null) {
			int dist = wh.getRouteDistance(ps);
			dd = OsmAndFormatter.getFormattedDistance(dist, app) ;
			if(ps.deviationDistance > 0 ) {
				dd += "\n "+OsmAndFormatter.getFormattedDistance(ps.deviationDistance, app); 
			}
		}
		textDist.setText(dd);
		text.setText(point.getName());
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


	public static void showAllDialog(final OsmandApplication app, final MapActivity ctx, final List<LocationPointWrapper> visibleLocationPoints){
		final WaypointHelper waypointHelper = app.getWaypointHelper();
		final ArrayAdapter<LocationPointWrapper> listAdapter = new ArrayAdapter<LocationPointWrapper>(ctx, R.layout.waypoint_reached, R.id.title,
				visibleLocationPoints) {
			@Override
			public View getView(final int position, View convertView, ViewGroup parent) {
				// User super class to create the View
				View v = convertView;
				if (v == null) {
					v = ctx.getLayoutInflater().inflate(R.layout.waypoint_reached, null);
					int vl = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 32, ctx.getResources()
							.getDisplayMetrics());
					final LinearLayout.LayoutParams ll = new LinearLayout.LayoutParams(vl, vl);
					ll.setMargins(vl / 4, vl / 4, vl / 4, vl / 4);
					v.findViewById(R.id.waypoint_icon).setLayoutParams(ll);
				}
				updatePointInfoView(app, ctx, v, getItem(position));
				TextView text = (TextView) v.findViewById(R.id.waypoint_text);
				text.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						showOnMap(app, ctx, visibleLocationPoints.get(position));
					}
				});

				View remove = v.findViewById(R.id.info_close);
				((ImageButton) remove).setImageDrawable(ctx.getResources().getDrawable(
						app.getSettings().isLightContent()? R.drawable.ic_action_gremove_light:
								R.drawable.ic_action_gremove_dark));
				remove.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						LocationPointWrapper point = visibleLocationPoints.get(position);
						remove(point);
						waypointHelper.removeVisibleLocationPoint(point);
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
				showOnMap(app, ctx, visibleLocationPoints.get(i));
			}
		});
		AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
		builder.setView(listView);
		builder.setPositiveButton(R.string.default_buttons_ok, null);
		builder.setNegativeButton(ctx.getString(R.string.hide_all_waypoints), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialogInterface, int i) {
				waypointHelper.clearAllVisiblePoints();
				// TODO
				// updateDialog(app, ctx);
			}
		});
		builder.show();
	}

	private static void showOnMap(OsmandApplication app, MapActivity ctx, LocationPointWrapper locationPoint) {
		// TODO
		LocationPoint point = locationPoint.getPoint();
		// AnimateDraggingMapThread thread = mapActivity.getMapView().getAnimatedDraggingThread();
		int fZoom = ctx.getMapView().getZoom() < 15 ? 15 : ctx.getMapView().getZoom();
		// thread.startMoving(pointToNavigate.getLatitude(), pointToNavigate.getLongitude(), fZoom, true);
		ctx.getMapView().setIntZoom(fZoom);
		ctx.getMapView().setLatLon(point.getLatitude(), point.getLongitude());
	}
}
