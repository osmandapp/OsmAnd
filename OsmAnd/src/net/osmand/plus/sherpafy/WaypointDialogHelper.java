package net.osmand.plus.sherpafy;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.SystemClock;
import android.text.Spannable;
import android.text.style.ForegroundColorSpan;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import net.osmand.Location;
import net.osmand.data.LatLon;
import net.osmand.data.LocationPoint;
import net.osmand.plus.*;
import net.osmand.plus.activities.FavouritesActivity;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.FavoriteImageDrawable;
import net.osmand.plus.views.MapControlsLayer;
import net.osmand.util.MapUtils;

import java.util.List;

/**
 * Created by Denis on 25.07.2014.
 */
public class WaypointDialogHelper {
	private MapActivity mapActivity;
	private OsmandApplication app;
	private FrameLayout mainLayout;
	private TargetPointsHelper targetPointsHelper;

	public static boolean OVERLAP_LAYOUT = true;
	private long uiModified;
	private View closePointDialog;

	public WaypointDialogHelper(MapActivity mapActivity) {
		this.app = mapActivity.getMyApplication();
		targetPointsHelper = this.app.getTargetPointsHelper();
		this.mapActivity = mapActivity;
		this.mainLayout = (FrameLayout) ((FrameLayout) mapActivity.getLayout()).getChildAt(0);
	}

	public void updateDialog() {
		List<LocationPoint> vlp = targetPointsHelper.getVisibleLocationPoints();
		long locationPointsModified = targetPointsHelper.getLocationPointsModified();
		if (locationPointsModified != uiModified) {
			uiModified = locationPointsModified;
			if (vlp.isEmpty()) {
				removeDialog();
			} else {
				final LocationPoint point = vlp.get(0);
				boolean created = false;
				if (closePointDialog == null) {
					created = true;
					final LayoutInflater vi = (LayoutInflater) app.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
					closePointDialog = vi.inflate(R.layout.waypoint_reached, null);
				}

				createView(closePointDialog, point);
				closePointDialog.setBackgroundColor(mapActivity.getResources().getColor(R.color.color_black));
				((TextView)closePointDialog.findViewById(R.id.waypoint_text)).setTextColor(Color.WHITE);
				View all = closePointDialog.findViewById(R.id.all_points);
				all.setVisibility(vlp.size() <= 1 ? View.GONE : View.VISIBLE);
				all.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						showAllDialog();
					}
				});

				View btnN = closePointDialog.findViewById(R.id.info_close);
				btnN.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						targetPointsHelper.removeVisibleLocationPoint(point);
						updateDialog();
					}
				});

				if (created) {
					mainLayout.addView(closePointDialog, getDialogLayoutParams());
					waitBeforeLayoutIsResized(closePointDialog);
				}
			}
		}
	}

	private void createView(View localView, final LocationPoint point) {
		TextView text = (TextView) localView.findViewById(R.id.waypoint_text);
		text.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				itemClick(point);
			}
		});

		((ImageView) localView.findViewById(R.id.waypoint_icon)).setImageDrawable(FavoriteImageDrawable.getOrCreate(mapActivity, point.getColor()));
		Location lastKnownMapLocation = app.getLocationProvider().getLastKnownLocation();
//		LatLon lastKnownMapLocation = app.getSettings().getLastKnownMapLocation();
		String distance;
		if (lastKnownMapLocation != null) {
			int dist = (int) (MapUtils.getDistance(point.getLatitude(), point.getLongitude(),
					lastKnownMapLocation.getLatitude(), lastKnownMapLocation.getLongitude()));
			distance = OsmAndFormatter.getFormattedDistance(dist, app) + "  ";
		} else {
			distance = "";
		}
		text.setText(distance + point.getName(), TextView.BufferType.SPANNABLE);
		if (distance.length() > 0) {
			((Spannable) text.getText()).setSpan(
					new ForegroundColorSpan(mapActivity.getResources().getColor(R.color.color_distance)), 0, distance.length() - 1,
					0);
		}
	}

	private void itemClick(LocationPoint point) {
		final Intent favorites = new Intent(mapActivity, app.getAppCustomization().getFavoritesActivity());
		favorites.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
		favorites.putExtra(FavouritesActivity.TAB_PARAM,
				point instanceof GPXUtilities.WptPt ? FavouritesActivity.GPX_TAB : FavouritesActivity.FAVORITES_TAB);
		mapActivity.startActivity(favorites);
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

	private boolean checkIfDialogExists() {
		if (mainLayout == null) {
			return true;
		}

		if (mainLayout.findViewById(R.id.package_delivered_layout) != null) {
			return false;
		}
		return true;
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

	public void showAllDialog(){
		final List<LocationPoint> visibleLocationPoints = targetPointsHelper.getVisibleLocationPoints();
		final ArrayAdapter<LocationPoint> listAdapter = new ArrayAdapter<LocationPoint>(mapActivity, R.layout.waypoint_reached, R.id.title,
				visibleLocationPoints) {
			@Override
			public View getView(final int position, View convertView, ViewGroup parent) {
				// User super class to create the View
				View v = convertView;
				if (v == null) {
					v = mapActivity.getLayoutInflater().inflate(R.layout.waypoint_reached, null);
					int vl = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 32, mapActivity.getResources()
							.getDisplayMetrics());
					final LinearLayout.LayoutParams ll = new LinearLayout.LayoutParams(vl, vl);
					ll.setMargins(vl / 4, vl / 4, vl / 4, vl / 4);
					v.findViewById(R.id.waypoint_icon).setLayoutParams(ll);
				}
				createView(v, getItem(position));
				TextView text = (TextView) v.findViewById(R.id.waypoint_text);
				text.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						showOnMap(visibleLocationPoints.get(position));
					}
				});

				View remove = v.findViewById(R.id.info_close);
				((ImageButton) remove).setImageDrawable(mapActivity.getResources().getDrawable(
						app.getSettings().isLightContent()? R.drawable.ic_action_gremove_light:
								R.drawable.ic_action_gremove_dark));
				remove.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						LocationPoint point = targetPointsHelper.getVisibleLocationPoints().get(position);
						remove(point);
						targetPointsHelper.removeVisibleLocationPoint(point);
						notifyDataSetChanged();
					}
				});

				return v;
			}
		};

		ListView listView = new ListView(mapActivity);
		listView.setAdapter(listAdapter);
		listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
				showOnMap(visibleLocationPoints.get(i));
			}
		});

//		Dialog dlg = new Dialog(mapActivity);
//		dlg.setContentView(listView);
//		dlg.show();
		AlertDialog.Builder builder = new AlertDialog.Builder(mapActivity);
		builder.setView(listView);
		builder.setPositiveButton(R.string.default_buttons_ok, null);
		builder.setNegativeButton(mapActivity.getString(R.string.hide_all_waypoints), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialogInterface, int i) {
				targetPointsHelper.removeAllVisiblePoints();
				updateDialog();
			}
		});
		builder.show();
	}

	private void showOnMap(LocationPoint locationPoint) {
		// AnimateDraggingMapThread thread = mapActivity.getMapView().getAnimatedDraggingThread();
		int fZoom = mapActivity.getMapView().getZoom() < 15 ? 15 : mapActivity.getMapView().getZoom();
		// thread.startMoving(pointToNavigate.getLatitude(), pointToNavigate.getLongitude(), fZoom, true);
		mapActivity.getMapView().setIntZoom(fZoom);
		mapActivity.getMapView().setLatLon(locationPoint.getLatitude(), locationPoint.getLongitude());
	}
}
