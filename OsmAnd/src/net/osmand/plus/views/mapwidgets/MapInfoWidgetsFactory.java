package net.osmand.plus.views.mapwidgets;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AlertDialog;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.osmand.Location;
import net.osmand.ValueHolder;
import net.osmand.binary.RouteDataObject;
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
import net.osmand.plus.mapcontextmenu.other.MapRouteInfoMenu;
import net.osmand.plus.monitoring.OsmandMonitoringPlugin;
import net.osmand.plus.routing.RouteDirectionInfo;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.views.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.mapwidgets.NextTurnInfoWidget.TurnDrawable;
import net.osmand.router.TurnType;

import java.util.LinkedList;

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
					vs.value = 0;
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
							app.startNavigationService(NavigationService.USED_BY_WAKE_UP, vs.value);
						}
					});
					dlg.setNegativeButton(R.string.shared_string_cancel, null);
					dlgshow[0] = dlg.show();

				}

			}
		});
		return gpsInfoControl;
	}

	public static abstract class TopToolbarViewController {
		private TopToolbarViewControllerType type;
		private int backButtonIconId = R.drawable.abc_ic_ab_back_mtrl_am_alpha;
		private int closeButtonIconId = R.drawable.ic_action_remove_dark;
		private String title = "";

		public enum TopToolbarViewControllerType {
			QUICK_SEARCH,
			CONTEXT_MENU
		}

		public TopToolbarViewController(TopToolbarViewControllerType type) {
			this.type = type;
		}

		public TopToolbarViewControllerType getType() {
			return type;
		}

		public String getTitle() {
			return title;
		}

		public void setTitle(String title) {
			this.title = title;
		}

		public int getBackButtonIconId() {
			return backButtonIconId;
		}

		public int getCloseButtonIconId() {
			return closeButtonIconId;
		}

		public void updateToolbar(TopToolbarView view) {
			view.getTitleView().setText(title);
		}

		public abstract void onBackPressed(TopToolbarView view);

		public abstract void onTitlePressed(TopToolbarView view);

		public abstract void onClosePressed(TopToolbarView view);
	}

	public static class TopToolbarView {
		private final MapActivity map;
		private LinkedList<TopToolbarViewController> viewControllers = new LinkedList<>();
		private View topbar;
		private View topBarLayout;
		private ImageButton backButton;
		private TextView titleView;
		private ImageButton closeButton;

		public TopToolbarView(final MapActivity map) {
			this.map = map;

			topbar = map.findViewById(R.id.custom_topbar);
			topBarLayout = map.findViewById(R.id.custom_topbar_layout);
			backButton = (ImageButton) map.findViewById(R.id.custom_back_button);
			backButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					TopToolbarViewController viewController = getTopViewController();
					if (viewController != null) {
						viewController.onBackPressed(TopToolbarView.this);
					}
				}
			});
			titleView = (TextView) map.findViewById(R.id.custom_title);
			titleView.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					TopToolbarViewController viewController = getTopViewController();
					if (viewController != null) {
						viewController.onTitlePressed(TopToolbarView.this);
					}
				}
			});
			closeButton = (ImageButton) map.findViewById(R.id.custom_close_button);
			closeButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					TopToolbarViewController viewController = getTopViewController();
					if (viewController != null) {
						viewController.onClosePressed(TopToolbarView.this);
					}
				}
			});
			updateVisibility(false);
		}

		public MapActivity getMap() {
			return map;
		}

		public View getTopbar() {
			return topbar;
		}

		public View getTopBarLayout() {
			return topBarLayout;
		}

		public ImageButton getBackButton() {
			return backButton;
		}

		public TextView getTitleView() {
			return titleView;
		}

		public ImageButton getCloseButton() {
			return closeButton;
		}

		public TopToolbarViewController getTopViewController() {
			if (viewControllers.size() > 0) {
				return viewControllers.get(viewControllers.size() - 1);
			} else {
				return null;
			}
		}

		public void addViewController(TopToolbarViewController viewController) {
			if (!viewControllers.contains(viewController)) {
				viewControllers.add(viewController);
			}
			updateInfo();
		}

		public void removeViewController(TopToolbarViewController viewController) {
			viewControllers.remove(viewController);
			updateInfo();
		}

		public boolean updateVisibility(boolean visible) {
			return updateVisibility(topbar, visible);
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

		public void updateInfo() {
			TopToolbarViewController viewController = getTopViewController();
			if (viewController != null) {
				viewController.updateToolbar(this);
			}
			updateVisibility(viewController != null);
		}

		public void updateTextColor(boolean nightMode, int textColor) {
			OsmandApplication app = map.getMyApplication();
			titleView.setTextColor(textColor);
			TopToolbarViewController viewController = getTopViewController();
			if (viewController != null) {
				backButton.setImageDrawable(app.getIconsCache().getIcon(viewController.getBackButtonIconId(), !nightMode));
				closeButton.setImageDrawable(app.getIconsCache().getIcon(viewController.getCloseButtonIconId(), !nightMode));
			} else {
				backButton.setImageDrawable(app.getIconsCache().getIcon(R.drawable.abc_ic_ab_back_mtrl_am_alpha, !nightMode));
				closeButton.setImageDrawable(app.getIconsCache().getIcon(R.drawable.ic_action_remove_dark, !nightMode));
			}
		}

		public void setBackgroundResource(int boxTop) {
			topBarLayout.setBackgroundResource(boxTop);
		}
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
			turnDrawable = new NextTurnInfoWidget.TurnDrawable(map, true);
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
			if (routingHelper != null && routingHelper.isRouteCalculated() && !routingHelper.isDeviatedFromRoute()) {
				if (routingHelper.isFollowingMode()) {
					if (settings.SHOW_STREET_NAME.get()) {
						text = routingHelper.getCurrentName(type);
						if (text == null) {
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
						text = RoutingHelper.formatStreetName(next.getStreetName(), next.getRef(), next.getDestinationName(), map.getMyApplication().getString(R.string.towards));
//						if (next.distance > 0) {
//							text += " " + OsmAndFormatter.getFormattedDistance(next.distance, map.getMyApplication());
//						}
						if (text == null) {
							text = "";
						}
						
					}
				}
			} else if (map.getMapViewTrackingUtilities().isMapLinkedToLocation() &&
					settings.SHOW_STREET_NAME.get()) {
				RouteDataObject rt = locationProvider.getLastKnownRouteSegment(); 
				if (rt != null) {
					text = RoutingHelper.formatStreetName(rt.getName(settings.MAP_PREFERRED_LOCALE.get()), 
							rt.getRef(), rt.getDestinationName(settings.MAP_PREFERRED_LOCALE.get()), map.getMyApplication().getString(R.string.towards));
				} 
				if (text == null) {
					text = "";
				}
			}
			if (map.isTopToolbarActive()) {
				updateVisibility(false);
			} else if (!showNextTurn && updateWaypoint()) {
				updateVisibility(true);
				updateVisibility(addressText, false);
				updateVisibility(addressTextShadow, false);
			} else if (text == null) {
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
				if (!text.equals(addressText.getText().toString()) ) {
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
