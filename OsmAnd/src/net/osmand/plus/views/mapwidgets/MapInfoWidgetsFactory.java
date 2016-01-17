package net.osmand.plus.views.mapwidgets;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AlertDialog;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.osmand.Location;
import net.osmand.ValueHolder;
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
import net.osmand.plus.dashboard.DashboardOnMap.DashboardType;
import net.osmand.plus.helpers.WaypointDialogHelper;
import net.osmand.plus.helpers.WaypointHelper;
import net.osmand.plus.helpers.WaypointHelper.LocationPointWrapper;
import net.osmand.plus.monitoring.OsmandMonitoringPlugin;
import net.osmand.plus.routing.RouteDirectionInfo;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.views.OsmandMapLayer.DrawSettings;
import net.osmand.plus.mapcontextmenu.other.MapRouteInfoMenu;
import net.osmand.plus.views.mapwidgets.NextTurnInfoWidget.TurnDrawable;
import net.osmand.router.TurnType;

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
		altitudeControl.setIcons(R.drawable.widget_altitude_day, R.drawable.widget_altitude_night);
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
		gpsInfoControl.setIcons(R.drawable.widget_gps_info_day, R.drawable.widget_gps_info_night);
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
					AlertDialog.Builder dlg = new AlertDialog.Builder(map);
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
							app.startNavigationService(NavigationService.USED_BY_WAKE_UP);
						}
					});
					dlg.setNegativeButton(R.string.shared_string_cancel, null);
					dlgshow[0] = dlg.show();

				}

			}
		});
		return gpsInfoControl;
	}
	

	public static class TopTextView   {
		private final RoutingHelper routingHelper;
		private final MapActivity map;
		private View topBar;
		private TextView addressText;
		private TextView addressTextShadow;
		private OsmAndLocationProvider locationProvider;
		private WaypointHelper waypointHelper;
		private OsmandSettings settings;
		private View waypointInfoBar;
		private LocationPointWrapper lastPoint;
		private TurnDrawable turnDrawable;
		private int shadowRad;

		public TopTextView(OsmandApplication app, MapActivity map) {
			topBar = map.findViewById(R.id.map_top_bar);
			addressText = (TextView) map.findViewById(R.id.map_address_text);
			addressTextShadow = (TextView) map.findViewById(R.id.map_address_text_shadow);
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
		
		public void updateTextColor(boolean nightMode, int textColor, int textShadowColor, boolean bold, int rad) {
			this.shadowRad = rad;
			TextInfoWidget.updateTextColor(addressText, addressTextShadow, textColor, textShadowColor, bold, rad);
			TextInfoWidget.updateTextColor((TextView) waypointInfoBar.findViewById(R.id.waypoint_text),
					(TextView) waypointInfoBar.findViewById(R.id.waypoint_text_shadow),
					textColor, textShadowColor, bold, rad / 2);
			
			ImageView all = (ImageView) waypointInfoBar.findViewById(R.id.waypoint_more);
			ImageView remove = (ImageView) waypointInfoBar.findViewById(R.id.waypoint_close);
			all.setImageDrawable(map.getMyApplication().getIconsCache()
					.getIcon(R.drawable.ic_overflow_menu_white, !nightMode));
			remove.setImageDrawable(map.getMyApplication().getIconsCache()
					.getIcon(R.drawable.ic_action_remove_dark, !nightMode));
		}
		
		

		public boolean updateInfo(DrawSettings d) {
			String text = null;
			TurnType[] type = new TurnType[1];
			boolean showNextTurn = false;
			if (routingHelper != null && routingHelper.isRouteCalculated()) {
				if (routingHelper.isFollowingMode()) {
					if(settings.SHOW_STREET_NAME.get()) {
						text = routingHelper.getCurrentName(type);
						if(text == null) {
							text = "";
						}
					}
				} else {
					int di = MapRouteInfoMenu.getDirectionInfo();
					if (di >= 0 && MapRouteInfoMenu.isControlVisible() &&
							di < routingHelper.getRouteDirections().size()) {
						showNextTurn = true;
						RouteDirectionInfo next = routingHelper.getRouteDirections().get(di);
						type[0] = next.getTurnType();
						text = RoutingHelper.formatStreetName(next.getStreetName(), next.getRef(), next.getDestinationName());
//						if(next.distance > 0) {
//							text += " " + OsmAndFormatter.getFormattedDistance(next.distance, map.getMyApplication());
//						}
						if(text == null) {
							text = "";
						}
						
					}
				}
			} else if(settings.getApplicationMode() != ApplicationMode.DEFAULT &&
					map.getMapViewTrackingUtilities().isMapLinkedToLocation() &&
					settings.SHOW_STREET_NAME.get()) {
				RouteDataObject rt = locationProvider.getLastKnownRouteSegment(); 
				if(rt != null) {
					text = RoutingHelper.formatStreetName(rt.getName(settings.MAP_PREFERRED_LOCALE.get()), 
							rt.getRef(), rt.getDestinationName(settings.MAP_PREFERRED_LOCALE.get()));
				} 
				if(text == null) {
					text = "";
				}
			}
			if (!showNextTurn && updateWaypoint()) {
				updateVisibility(true);
				updateVisibility(addressText, false);
				updateVisibility(addressTextShadow, false);
			} else if(text == null) {
				updateVisibility(false);
			} else {
				updateVisibility(true);
				updateVisibility(waypointInfoBar, false);
				updateVisibility(addressText, true);
				updateVisibility(addressTextShadow,  shadowRad > 0);
				boolean update = turnDrawable.setTurnType(type[0]);
				
				int h = addressText.getHeight() / 4 * 3;
				if (h != turnDrawable.getBounds().bottom) {
					turnDrawable.setBounds(0, 0, h, h);
				}
				if (update) {
					if (type[0] != null) {
						addressTextShadow.setCompoundDrawables(turnDrawable, null, null, null);
						addressTextShadow.setCompoundDrawablePadding(4);
						addressText.setCompoundDrawables(turnDrawable, null, null, null);
						addressText.setCompoundDrawablePadding(4);
					} else {
						addressTextShadow.setCompoundDrawables(null, null, null, null);
						addressText.setCompoundDrawables(null, null, null, null);
					}
				}
				if (!text.equals(addressText.getText().toString())) {
					if (!text.equals("")) {
						topBar.setContentDescription(text);
					} else {
						topBar.setContentDescription(map.getResources().getString(R.string.map_widget_top_text));
					}
					addressTextShadow.setText(text);
					addressText.setText(text);
					return true;
				}
			}
			return false;
		}
		
		public boolean updateWaypoint() {
			final LocationPointWrapper pnt = waypointHelper.getMostImportantLocationPoint(null);
			boolean changed = this.lastPoint != pnt;
			this.lastPoint = pnt;
			if (pnt == null) {
				topBar.setOnClickListener(null);
				updateVisibility(waypointInfoBar, false);
				return false;
			} else {
				updateVisibility(addressText, false);
				updateVisibility(addressTextShadow, false);
				boolean updated = updateVisibility(waypointInfoBar, true);
				// pass top bar to make it clickable
				WaypointDialogHelper.updatePointInfoView(map.getMyApplication(), map, topBar, pnt, true,
						map.getMyApplication().getDaynightHelper().isNightModeForMapControls(), false, true);
				if (updated || changed) {
					ImageView all = (ImageView) waypointInfoBar.findViewById(R.id.waypoint_more);
					ImageView remove = (ImageView) waypointInfoBar.findViewById(R.id.waypoint_close);
					all.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View view) {
							map.getDashboard().setDashboardVisibility(true, DashboardType.WAYPOINTS);
						}
					});
					remove.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View view) {
							waypointHelper.removeVisibleLocationPoint(pnt);
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
