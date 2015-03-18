package net.osmand.plus.views.mapwidgets;

import net.osmand.Location;
import net.osmand.access.AccessibleToast;
import net.osmand.binary.RouteDataObject;
import net.osmand.plus.ApplicationMode;
import net.osmand.plus.NavigationService;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmAndLocationProvider;
import net.osmand.plus.OsmAndLocationProvider.GPSInfo;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.actions.StartGPSStatus;
import net.osmand.plus.helpers.WaypointDialogHelper;
import net.osmand.plus.helpers.WaypointHelper;
import net.osmand.plus.helpers.WaypointHelper.LocationPointWrapper;
import net.osmand.plus.monitoring.OsmandMonitoringPlugin;
import net.osmand.plus.monitoring.ValueHolder;
import net.osmand.plus.routing.RouteDirectionInfo;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.views.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.controls.MapRouteInfoControl;
import net.osmand.plus.views.mapwidgets.NextTurnInfoWidget.TurnDrawable;
import net.osmand.router.TurnType;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;
import android.widget.Toast;

public class MapInfoWidgetsFactory {
	
	public TextInfoWidget createAltitudeControl(final MapActivity map) {
		final TextInfoWidget altitudeControl = new TextInfoWidget(map) {
			private int cachedAlt = 0;

			@Override
			public boolean updateInfo(DrawSettings d) {
				// draw speed
				Location loc = map.getMyApplication().getLocationProvider().getLastKnownLocation();
				if (loc != null && loc.hasAltitude()) {
					double compAlt = loc.getAltitude();
					if (cachedAlt != (int) compAlt) {
						cachedAlt = (int) compAlt;
						String ds = OsmAndFormatter.getFormattedAlt(cachedAlt, map.getMyApplication());
						int ls = ds.lastIndexOf(' ');
						if (ls == -1) {
							setText(ds, null);
						} else {
							setText(ds.substring(0, ls), ds.substring(ls + 1));
						}
						return true;
					}
				} else if (cachedAlt != 0) {
					cachedAlt = 0;
					setText(null, null);
					return true;
				}
				return false;
			}
		};
		altitudeControl.setText(null, null);
		altitudeControl.setImageDrawable(R.drawable.widget_altitude);
		return altitudeControl;
	}
	
	public TextInfoWidget createGPSInfoControl(final MapActivity map) {
		final OsmandApplication app = map.getMyApplication();
		final OsmAndLocationProvider loc = app.getLocationProvider();
		final TextInfoWidget gpsInfoControl = new TextInfoWidget(map) {
			private int u = -1;
			private int f = -1;

			@Override
			public boolean updateInfo(DrawSettings d) {
				GPSInfo gpsInfo = loc.getGPSInfo();
				if(gpsInfo.usedSatellites != u || gpsInfo.foundSatellites != f) {
					u = gpsInfo.usedSatellites;
					f = gpsInfo.foundSatellites;
					setText(gpsInfo.usedSatellites+"/"+gpsInfo.foundSatellites, "");
					return true;
				}
				return false;
			}
		};
		gpsInfoControl.setImageDrawable(R.drawable.widget_gps_info);
		gpsInfoControl.setText(null, null);
		gpsInfoControl.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (app.getNavigationService() != null){
					AlertDialog.Builder dlg = new AlertDialog.Builder(map);
					dlg.setTitle(app.getString(R.string.sleep_mode_stop_dialog));

					//Show currently active wake-up interval
					int soi = app.getNavigationService().getServiceOffInterval();
					if (soi == 0) {
						dlg.setMessage(app.getString(R.string.gps_wake_up_timer) + ": " + app.getString(R.string.int_continuosly));
					} else if (soi <= 90000) {
						dlg.setMessage(app.getString(R.string.gps_wake_up_timer) + ": " + Integer.toString(soi/1000) + " " + app.getString(R.string.int_seconds));
					} else {
						dlg.setMessage(app.getString(R.string.gps_wake_up_timer) + ": " + Integer.toString(soi/1000/60) + " " + app.getString(R.string.int_min));
					}

					dlg.setPositiveButton(app.getString(R.string.keep_navigation_service), null);
					dlg.setNegativeButton(app.getString(R.string.stop_navigation_service), new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialogInterface, int i) {
							Intent serviceIntent = new Intent(app, NavigationService.class);
							app.stopService(serviceIntent);
						}
					});
					dlg.show();
					
				} else {
					final ValueHolder<Integer> vs = new ValueHolder<Integer>();
					vs.value = app.getSettings().SERVICE_OFF_INTERVAL.get();
					final AlertDialog[] dlgshow = new AlertDialog[1]; 
					Builder dlg = new AlertDialog.Builder(map);
					dlg.setTitle(app.getString(R.string.enable_sleep_mode));
					WindowManager mgr = (WindowManager) map.getSystemService(Context.WINDOW_SERVICE);
					DisplayMetrics dm = new DisplayMetrics();
					mgr.getDefaultDisplay().getMetrics(dm);
					LinearLayout ll = OsmandMonitoringPlugin.createIntervalChooseLayout(map,
							app.getString(R.string.gps_wake_up_timer) + " : %s",
							OsmandMonitoringPlugin.SECONDS,
							OsmandMonitoringPlugin.MINUTES,
							null, vs, dm);
					if (Version.isGpsStatusEnabled(app)) {
						dlg.setNeutralButton(R.string.gps_status, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								new StartGPSStatus(map).run();								
							}
						});
					}
					dlg.setView(ll);
					dlg.setPositiveButton(R.string.shared_string_ok, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							app.getSettings().SERVICE_OFF_INTERVAL.set(vs.value);
							app.startNavigationService(NavigationService.USED_BY_GPX);
						}
					});
					dlg.setNegativeButton(R.string.shared_string_cancel, null);
					dlgshow[0] = dlg.show();

				}

			}
		});
		return gpsInfoControl;
	}
	



	
	private static boolean isScreenLocked = false;
	private Drawable lockEnabled;
	private Drawable lockDisabled;
	public ImageView createLockInfo(final MapActivity map) {
		final OsmandMapTileView view = map.getMapView();
		final Drawable lockEnabledNormal = view.getResources().getDrawable(R.drawable.lock_enabled);
		final Drawable lockDisabledNormal = view.getResources().getDrawable(R.drawable.lock_disabled);
		final Drawable lockEnabledWhite = view.getResources().getDrawable(R.drawable.lock_enabled_white);
		final Drawable lockDisabledWhite = view.getResources().getDrawable(R.drawable.lock_disabled_white);
		lockDisabled = lockDisabledNormal;
		lockEnabled = lockEnabledNormal;
		final ImageView lockView = null;
//		final ImageViewWidget lockView = new ImageViewWidget(view.getContext()) {
//			private boolean nightMode;
//			@Override
//			public boolean updateInfo(DrawSettings drawSettings) {
//				boolean nightMode = drawSettings == null ? false : drawSettings.isNightMode();
//				if(nightMode != this.nightMode) {
//					this.nightMode = nightMode;
//					lockDisabled = drawSettings.isNightMode() ? lockDisabledWhite : lockDisabledNormal;
//					lockEnabled = drawSettings.isNightMode() ? lockEnabledWhite : lockEnabledNormal;
//					setImageDrawable(isScreenLocked ? lockEnabled : lockDisabled);
//					return true;
//				}
//				return false;
//			}
//		};
		
		if (isScreenLocked) {
			map.getMapViewTrackingUtilities().backToLocationImpl();
			lockView.setBackgroundDrawable(lockEnabled);
		} else {
			lockView.setBackgroundDrawable(lockDisabled);
		}
		final FrameLayout transparentLockView = new FrameLayout(view.getContext());
		FrameLayout.LayoutParams fparams = new FrameLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT, Gravity.CENTER);
		transparentLockView.setLayoutParams(fparams);
		transparentLockView.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (event.getAction() == MotionEvent.ACTION_UP) {
					int[] locs = new int[2];
					lockView.getLocationOnScreen(locs);
					int x = (int) event.getX() - locs[0];
					int y = (int) event.getY() - locs[1];
					transparentLockView.getLocationOnScreen(locs);
					x += locs[0];
					y += locs[1];
					if (lockView.getWidth() >= x && x >= 0 && lockView.getHeight() >= y && y >= 0) {
						lockView.performClick();
						return true;
					}
					blinkIcon();
					AccessibleToast.makeText(transparentLockView.getContext(), R.string.screen_is_locked, Toast.LENGTH_SHORT).show();
					return true;
				}
				return true;
			}

			private void blinkIcon() {
				lockView.setBackgroundDrawable(lockDisabled);
				// TODO!
//				map.postDelayed(new Runnable() {
//					@Override
//					public void run() {
//						lockView.setBackgroundDrawable(lockEnabled);
//					}
//				}, 300);
			}

		});
		final FrameLayout parent = null; // TODO 
		lockView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (isScreenLocked) {
					parent.removeView(transparentLockView);
				}
				isScreenLocked = !isScreenLocked;
				if (isScreenLocked) {
					parent.addView(transparentLockView);
					map.getMapViewTrackingUtilities().backToLocationImpl();
					lockView.setBackgroundDrawable(lockEnabled);
				} else {
					lockView.setBackgroundDrawable(lockDisabled);
				}
			}
		});
		if(isScreenLocked){
			// TODO
//			map.addLockView(transparentLockView);
		}
		return lockView;
	}
	
	
	public static class TopTextView   {
		private final RoutingHelper routingHelper;
		private final MapActivity map;
		private View topBar;
		private TextView addressText;
		private OsmAndLocationProvider locationProvider;
		private WaypointHelper waypointHelper;
		private OsmandSettings settings;
		private View waypointInfoBar;
		private LocationPointWrapper lastPoint;
		private TurnDrawable turnDrawable;

		public TopTextView(OsmandApplication app, MapActivity map) {
			topBar = map.findViewById(R.id.map_top_bar);
			addressText = (TextView) map.findViewById(R.id.map_address_text);
			waypointInfoBar = map.findViewById(R.id.waypoint_info_bar);
			this.routingHelper = app.getRoutingHelper();
			locationProvider = app.getLocationProvider();
			this.map = map;
			settings = app.getSettings();
			waypointHelper = app.getWaypointHelper();
			updateVisibility(false);
			turnDrawable = new NextTurnInfoWidget.TurnDrawable(map);
		}
		
		public boolean updateVisibility(boolean visible) {
			return updateVisibility(topBar, visible);
		}
		
		public boolean updateVisibility(View v, boolean visible) {
			if (visible != (v.getVisibility() == View.VISIBLE)) {
				if (visible) {
					v.setVisibility(View.VISIBLE);
				} else {
					v.setVisibility(View.GONE);
				}
				v.invalidate();
				return true;
			}
			return false;
		}
		
		public void updateTextColor(int textColor, int textShadowColor, boolean bold, int rad) {
			updateTextColor(addressText, textColor, textShadowColor, bold, rad);
			updateTextColor((TextView) waypointInfoBar.findViewById(R.id.waypoint_text),
					textColor, textShadowColor, bold, rad);
		}
		
		private void updateTextColor(TextView tv, int textColor, int textShadowColor, boolean textBold, int rad) {
			tv.setTextColor(textColor);
			tv.setShadowLayer(rad, 0, 0, textShadowColor);
			tv.setTypeface(Typeface.DEFAULT, textBold ? Typeface.BOLD : Typeface.NORMAL);
		}

		public boolean updateInfo(DrawSettings d) {
			String text = null;
			TurnType[] type = new TurnType[1];
			boolean showNextTurn = false;
			if (routingHelper != null && routingHelper.isRouteCalculated()) {
				if (routingHelper.isFollowingMode()) {
					text = routingHelper.getCurrentName(type);
				} else {
					int di = MapRouteInfoControl.getDirectionInfo();
					if (di >= 0 && MapRouteInfoControl.isControlVisible() &&
							di < routingHelper.getRouteDirections().size()) {
						showNextTurn = true;
						RouteDirectionInfo next = routingHelper.getRouteDirections().get(di);
						type[0] = next.getTurnType();
						text = RoutingHelper.formatStreetName(next.getStreetName(), next.getRef(), next.getDestinationName());
//						if(next.distance > 0) {
//							text += " " + OsmAndFormatter.getFormattedDistance(next.distance, map.getMyApplication());
//						}
						
					}
				}
			} else if(settings.getApplicationMode() != ApplicationMode.DEFAULT &&
					map.getMapViewTrackingUtilities().isMapLinkedToLocation()) {
				RouteDataObject rt = locationProvider.getLastKnownRouteSegment(); 
				if(rt != null) {
					text = RoutingHelper.formatStreetName(rt.getName(), rt.getRef(), rt.getDestinationName());
				}
			}
			if(!showNextTurn && updateWaypoint()) {
				updateVisibility(true);
				updateVisibility(addressText, false);
			} else if(text == null) {
				updateVisibility(false);
			} else {
				updateVisibility(true);
				updateVisibility(waypointInfoBar, false);
				updateVisibility(addressText, true);
				boolean update = turnDrawable.setTurnType(type[0]);
				int h = addressText.getHeight() / 4 * 3;
				if (h != turnDrawable.getBounds().bottom) {
					turnDrawable.setBounds(0, 0, h, h);
				}
				if (update) {
					if (type[0] != null) {
						addressText.setCompoundDrawables(turnDrawable, null, null, null);
					} else {
						addressText.setCompoundDrawables(null, null, null, null);
					}
				}
				if (!text.equals(addressText.getText().toString())) {
					if (!text.equals("")) {
						topBar.setContentDescription(text);
					} else {
						topBar.setContentDescription(map.getResources().getString(R.string.map_widget_top_text));
					}
					addressText.setText(text);
					return true;
				}
			}
			return false;
		}
		
		public boolean updateWaypoint() {
			lastPoint = waypointHelper.getMostImportantLocationPoint(null);
			if (lastPoint == null) {
				topBar.setOnClickListener(null);
				updateVisibility(waypointInfoBar, false);
				return false;
			} else {
				updateVisibility(addressText, false);
				boolean updated = updateVisibility(waypointInfoBar, true);
				// pass top bar to make it clickable
				WaypointDialogHelper.updatePointInfoView(map.getMyApplication(), map, topBar, 
						lastPoint, null);
				if (updated) {
					ImageView all = (ImageView) waypointInfoBar.findViewById(R.id.waypoint_more);
					View btnN = waypointInfoBar.findViewById(R.id.waypoint_close);
					all.setImageDrawable(map.getMyApplication().getIconsCache().
							getContentIcon(R.drawable.ic_overflow_menu_light));
					all.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View view) {
							map.getMapActions().showWaypointsDialog(false);
						}
					});
					btnN.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View view) {
							waypointHelper.removeVisibleLocationPoint(lastPoint);
							map.refreshMap();
						}
					});
				}
				return true;
			}
		}

		public void setBackgroundResource(int boxTop) {
			topBar.setBackgroundResource(boxTop);
		}

	}
}
