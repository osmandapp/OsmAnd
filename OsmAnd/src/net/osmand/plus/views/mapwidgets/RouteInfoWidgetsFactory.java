package net.osmand.plus.views.mapwidgets;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.hardware.GeomagneticField;
import android.os.BatteryManager;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.text.format.DateFormat;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import net.osmand.AndroidUtils;
import net.osmand.Location;
import net.osmand.StateChangedListener;
import net.osmand.binary.RouteDataObject;
import net.osmand.data.LatLon;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.MapMarkersHelper.MapMarker;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmAndLocationProvider;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.OsmandSettings.OsmandPreference;
import net.osmand.plus.R;
import net.osmand.plus.TargetPointsHelper;
import net.osmand.plus.TargetPointsHelper.TargetPoint;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.MapViewTrackingUtilities;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.helpers.WaypointHelper;
import net.osmand.plus.routepreparationmenu.MapRouteInfoMenu;
import net.osmand.plus.routing.AlarmInfo;
import net.osmand.plus.routing.AlarmInfo.AlarmInfoType;
import net.osmand.plus.routing.RouteCalculationResult.NextDirectionInfo;
import net.osmand.plus.routing.RouteDirectionInfo;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.views.AnimateDraggingMapThread;
import net.osmand.plus.views.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.TurnPathHelper;
import net.osmand.plus.views.mapwidgets.MapWidgetRegistry.WidgetState;
import net.osmand.router.RouteResultPreparation;
import net.osmand.router.TurnType;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RouteInfoWidgetsFactory {

	public NextTurnInfoWidget createNextInfoControl(final Activity activity,
			final OsmandApplication app, boolean horisontalMini) {
		final OsmandSettings settings = app.getSettings();
		final RoutingHelper routingHelper = app.getRoutingHelper();
		final NextTurnInfoWidget nextTurnInfo = new NextTurnInfoWidget(activity, app, horisontalMini) {
			NextDirectionInfo calc1 = new NextDirectionInfo();

			@Override
			public boolean updateInfo(DrawSettings drawSettings) {
				boolean followingMode = routingHelper.isFollowingMode() || app.getLocationProvider().getLocationSimulation().isRouteAnimating();
				TurnType turnType = null;
				boolean deviatedFromRoute = false;
				int turnImminent = 0;
				int nextTurnDistance = 0;
				if (routingHelper != null && routingHelper.isRouteCalculated() && followingMode) {
					deviatedFromRoute = routingHelper.isDeviatedFromRoute();
					
					if (deviatedFromRoute) {
						turnImminent = 0;
						turnType = TurnType.valueOf(TurnType.OFFR, settings.DRIVING_REGION.get().leftHandDriving);
						setDeviatePath((int) routingHelper.getRouteDeviation());
					} else {
						NextDirectionInfo r = routingHelper.getNextRouteDirectionInfo(calc1, true);
						if (r != null && r.distanceTo > 0 && r.directionInfo != null) {
							turnType = r.directionInfo.getTurnType();
							nextTurnDistance = r.distanceTo;
							turnImminent = r.imminent;
						}
					}
				}
				setTurnType(turnType);
				setTurnImminent(turnImminent, deviatedFromRoute);
				setTurnDistance(nextTurnDistance);
				return true;
			}
		};
		nextTurnInfo.setOnClickListener(new View.OnClickListener() {
//			int i = 0;
//			boolean leftSide = false;
			@Override
			public void onClick(View v) {
				// for test rendering purposes
//				final int l = TurnType.predefinedTypes.length;
//				final int exits = 5;
//				i++;
//				if (i % (l + exits) >= l ) {
//					nextTurnInfo.turnType = TurnType.valueOf("EXIT" + (i % (l + exits) - l + 1), leftSide);
//					float a = leftSide?  -180 + (i % (l + exits) - l + 1) * 50:  180 - (i % (l + exits) - l + 1) * 50;
//					nextTurnInfo.turnType.setTurnAngle(a < 0 ? a + 360 : a);
//					nextTurnInfo.exitOut = (i % (l + exits) - l + 1)+"";
//				} else {
//					nextTurnInfo.turnType = TurnType.valueOf(TurnType.predefinedTypes[i % (TurnType.predefinedTypes.length + exits)], leftSide);
//					nextTurnInfo.exitOut = "";
//				}
//				nextTurnInfo.turnImminent = (nextTurnInfo.turnImminent + 1) % 3;
//				nextTurnInfo.nextTurnDirection = 580;
//				TurnPathHelper.calcTurnPath(nextTurnInfo.pathForTurn, nextTurnInfo.turnType,nextTurnInfo.pathTransform);
				if(routingHelper.isRouteCalculated() && !routingHelper.isDeviatedFromRoute()) {
					routingHelper.getVoiceRouter().announceCurrentDirection(null);
				}
			}
		});
		// initial state
		return nextTurnInfo;
	}
	
	public NextTurnInfoWidget createNextNextInfoControl(final Activity activity,
			final OsmandApplication app, boolean horisontalMini) {
		final RoutingHelper routingHelper = app.getRoutingHelper();
		final NextTurnInfoWidget nextTurnInfo = new NextTurnInfoWidget(activity, app, horisontalMini) {
			NextDirectionInfo calc1 = new NextDirectionInfo();
			@Override
			public boolean updateInfo(DrawSettings drawSettings) {
				boolean followingMode = routingHelper.isFollowingMode() || app.getLocationProvider().getLocationSimulation().isRouteAnimating();
				TurnType turnType = null;
				boolean deviatedFromRoute = false;
				int turnImminent = 0;
				int nextTurnDistance = 0;
				if (routingHelper != null && routingHelper.isRouteCalculated() && followingMode) {
					deviatedFromRoute = routingHelper.isDeviatedFromRoute() ;
					NextDirectionInfo r = routingHelper.getNextRouteDirectionInfo(calc1, true);
					if (!deviatedFromRoute) {
						if (r != null) {
							r = routingHelper.getNextRouteDirectionInfoAfter(r, calc1, true);
						}
					}
					if (r != null && r.distanceTo > 0 && r.directionInfo != null) {
						turnType = r.directionInfo.getTurnType();
						turnImminent = r.imminent;
						nextTurnDistance = r.distanceTo;
					}
				}
				setTurnType(turnType);
				setTurnImminent(turnImminent, deviatedFromRoute);
				setTurnDistance(nextTurnDistance);
				return true;
			}
		};
		nextTurnInfo.setOnClickListener(new View.OnClickListener() {
//			int i = 0;
			@Override
			public void onClick(View v) {
				// uncomment to test turn info rendering
//				final int l = TurnType.predefinedTypes.length;
//				final int exits = 5;
//				i++;
//				if (i % (l + exits) >= l ) {
//					nextTurnInfo.turnType = TurnType.valueOf("EXIT" + (i % (l + exits) - l + 1), true);
//					nextTurnInfo.exitOut = (i % (l + exits) - l + 1)+"";
//					float a = 180 - (i % (l + exits) - l + 1) * 50;
//					nextTurnInfo.turnType.setTurnAngle(a < 0 ? a + 360 : a);
//				} else {
//					nextTurnInfo.turnType = TurnType.valueOf(TurnType.predefinedTypes[i % (TurnType.predefinedTypes.length + exits)], true);
//					nextTurnInfo.exitOut = "";
//				}
//				nextTurnInfo.turnImminent = (nextTurnInfo.turnImminent + 1) % 3;
//				nextTurnInfo.nextTurnDirection = 580;
//				TurnPathHelper.calcTurnPath(nextTurnInfo.pathForTurn, nexsweepAngletTurnInfo.turnType,nextTurnInfo.pathTransform);
//				showMiniMap = true;
			}
		});
		// initial state 
		return nextTurnInfo;
	}



	public static class TimeControlWidgetState extends WidgetState {

		public static final int TIME_CONTROL_WIDGET_STATE_ARRIVAL_TIME = R.id.time_control_widget_state_arrival_time;
		public static final int TIME_CONTROL_WIDGET_STATE_TIME_TO_GO = R.id.time_control_widget_state_time_to_go;

		private final OsmandPreference<Boolean> showArrival;
		private final boolean intermediate;

		public TimeControlWidgetState(OsmandApplication ctx, boolean intermediate) {
			super(ctx);
			this.intermediate = intermediate;
			if (intermediate) {
				showArrival = ctx.getSettings().SHOW_INTERMEDIATE_ARRIVAL_TIME_OTHERWISE_EXPECTED_TIME;
			} else {
				showArrival = ctx.getSettings().SHOW_ARRIVAL_TIME_OTHERWISE_EXPECTED_TIME;
			}
		}

		@Override
		public int getMenuTitleId() {
			if (intermediate) {
				return showArrival.get() ? R.string.access_intermediate_arrival_time : R.string.map_widget_intermediate_time;
			}
			return showArrival.get() ? R.string.access_arrival_time : R.string.map_widget_time;
		}

		@Override
		public int getMenuIconId() {
			if (intermediate) {
				return R.drawable.ic_action_intermediate_destination_time;
			}
			return showArrival.get() ? R.drawable.ic_action_time : R.drawable.ic_action_time_to_distance;
		}

		@Override
		public int getMenuItemId() {
			return showArrival.get() ? TIME_CONTROL_WIDGET_STATE_ARRIVAL_TIME : TIME_CONTROL_WIDGET_STATE_TIME_TO_GO;
		}

		@Override
		public int[] getMenuTitleIds() {
			if (intermediate) {
				return new int[]{R.string.access_intermediate_arrival_time, R.string.map_widget_intermediate_time};
			}
			return new int[]{R.string.access_arrival_time, R.string.map_widget_time};
		}

		@Override
		public int[] getMenuIconIds() {
			if (intermediate) {
				return new int[]{R.drawable.ic_action_intermediate_destination_time, R.drawable.ic_action_intermediate_destination_time};
			}
			return new int[]{R.drawable.ic_action_time, R.drawable.ic_action_time_to_distance};
		}

		@Override
		public int[] getMenuItemIds() {
			return new int[]{TIME_CONTROL_WIDGET_STATE_ARRIVAL_TIME, TIME_CONTROL_WIDGET_STATE_TIME_TO_GO};
		}

		@Override
		public void changeState(int stateId) {
			showArrival.set(stateId == TIME_CONTROL_WIDGET_STATE_ARRIVAL_TIME);
		}
	}

	public TextInfoWidget createTimeControl(final MapActivity map, final boolean intermediate){
		final RoutingHelper routingHelper = map.getRoutingHelper();
		final OsmandApplication ctx = map.getMyApplication();
		final OsmandPreference<Boolean> showArrival = intermediate
				? ctx.getSettings().SHOW_INTERMEDIATE_ARRIVAL_TIME_OTHERWISE_EXPECTED_TIME
				: ctx.getSettings().SHOW_ARRIVAL_TIME_OTHERWISE_EXPECTED_TIME;

		final TextInfoWidget leftTimeControl = new TextInfoWidget(map) {
			private long cachedLeftTime = 0;
			
			@Override
			public boolean updateInfo(DrawSettings drawSettings) {
				setTimeControlIcons(this, showArrival.get(), intermediate);
				int time = 0;
				if (routingHelper != null && routingHelper.isRouteCalculated()) {
					//boolean followingMode = routingHelper.isFollowingMode();
					time = intermediate ? routingHelper.getLeftTimeNextIntermediate() : routingHelper.getLeftTime();

					if (time != 0) {
						if (/*followingMode && */showArrival.get()) {
							long toFindTime = time * 1000 + System.currentTimeMillis();
							if (Math.abs(toFindTime - cachedLeftTime) > 30000) {
								cachedLeftTime = toFindTime;
								setContentTitle(map.getString(R.string.access_arrival_time));
								if (DateFormat.is24HourFormat(ctx)) {
									setText(DateFormat.format("k:mm", toFindTime).toString(), null); //$NON-NLS-1$
								} else {
									setText(DateFormat.format("h:mm", toFindTime).toString(), 
											DateFormat.format("aa", toFindTime).toString()); //$NON-NLS-1$
								}
								return true;
							}
						} else {
							if (Math.abs(time - cachedLeftTime) > 30) {
								cachedLeftTime = time;
								int hours = time / (60 * 60);
								int minutes = (time / 60) % 60;
								setContentTitle(map.getString(R.string.map_widget_time));
								setText(String.format("%d:%02d", hours, minutes), null); //$NON-NLS-1$
								return true;
							}
						}
					}
				}
				if (time == 0 && cachedLeftTime != 0) {
					cachedLeftTime = 0;
					setText(null, null);
					return true;
				}
				return false;
			};
		};
		leftTimeControl.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				showArrival.set(!showArrival.get());
				setTimeControlIcons(leftTimeControl, showArrival.get(), intermediate);
				map.getMapView().refreshMap();
			}
			
		});
		leftTimeControl.setText(null, null);
		setTimeControlIcons(leftTimeControl, showArrival.get(), intermediate);
		return leftTimeControl;
	}

	private void setTimeControlIcons(TextInfoWidget timeControl, boolean showArrival, boolean intermediate) {
		int iconLight = intermediate
				? R.drawable.widget_intermediate_time_day
				: showArrival ? R.drawable.widget_time_day : R.drawable.widget_time_to_distance_day;
		int iconDark = intermediate
				? R.drawable.widget_intermediate_time_night
				: showArrival ? R.drawable.widget_time_night : R.drawable.widget_time_to_distance_night;
		timeControl.setIcons(iconLight, iconDark);
	}
	
	
	public TextInfoWidget createPlainTimeControl(final MapActivity map){
		final OsmandApplication ctx = map.getMyApplication();
		final TextInfoWidget plainTimeControl = new TextInfoWidget(map) {
			private long cachedLeftTime = 0;
			
			@Override
			public boolean updateInfo(DrawSettings drawSettings) {
				long time = System.currentTimeMillis();
				if(time - cachedLeftTime > 5000) {
					cachedLeftTime = time;
					if (DateFormat.is24HourFormat(ctx)) {
						setText(DateFormat.format("k:mm", time).toString(), null); //$NON-NLS-1$
					} else {
						setText(DateFormat.format("h:mm", time).toString(),
								DateFormat.format("aa", time).toString()); //$NON-NLS-1$
					}
				}
				return false;
			};
		};
		plainTimeControl.setText(null, null);
		plainTimeControl.setIcons(R.drawable.widget_time_day, R.drawable.widget_time_night);
		return plainTimeControl;
	}


	public TextInfoWidget createBatteryControl(final MapActivity map){
		final int battery = R.drawable.widget_battery_day;
		final int batteryN = R.drawable.widget_battery_night;
		final int batteryCharging = R.drawable.widget_battery_charging_day;
		final int batteryChargingN = R.drawable.widget_battery_charging_night;
		final OsmandApplication ctx = map.getMyApplication();
		final TextInfoWidget batteryControl = new TextInfoWidget(map) {
			private long cachedLeftTime = 0;

			@Override
			public boolean updateInfo(DrawSettings drawSettings) {
				long time = System.currentTimeMillis();
				if (time - cachedLeftTime > 1000) {
					cachedLeftTime = time;
					Intent batteryIntent = ctx.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
					int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
					int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
					int status = batteryIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);

					if (level == -1 || scale == -1 || status == -1) {
						setText("?", null);
						setIcons(battery, batteryN);
					} else {
						boolean charging = ((status == BatteryManager.BATTERY_STATUS_CHARGING) ||
								(status == BatteryManager.BATTERY_STATUS_FULL));
						setText(String.format("%d%%", (level * 100) / scale), null );
						setIcons(charging ? batteryCharging : battery, charging ? batteryChargingN : batteryN);
					}
				}
				return false;
			};
		};
		batteryControl.setText(null, null);
		batteryControl.setIcons(battery, batteryN);
		return batteryControl;
	}

	public TextInfoWidget createMaxSpeedControl(final MapActivity map) {
		final RoutingHelper rh = map.getMyApplication().getRoutingHelper();
		final OsmAndLocationProvider locationProvider = map.getMyApplication().getLocationProvider();
		final MapViewTrackingUtilities trackingUtilities = map.getMapViewTrackingUtilities();
		final TextInfoWidget speedControl = new TextInfoWidget(map) {
			private float cachedSpeed = 0;

			@Override
			public boolean updateInfo(DrawSettings drawSettings) {
				float mx = 0; 
				if ((rh == null || !rh.isFollowingMode() || rh.isDeviatedFromRoute() || rh.getCurrentGPXRoute() != null)
						&& trackingUtilities.isMapLinkedToLocation()) {
					RouteDataObject ro = locationProvider.getLastKnownRouteSegment();
					if(ro != null) {
						mx = ro.getMaximumSpeed(ro.bearingVsRouteDirection(locationProvider.getLastKnownLocation()));
					}
				} else if (rh != null) {
					mx = rh.getCurrentMaxSpeed();
				} else {
					mx = 0f;
				}
				if (cachedSpeed != mx) {
					cachedSpeed = mx;
					if (cachedSpeed == 0) {
						setText(null, null);
					} else if(cachedSpeed == RouteDataObject.NONE_MAX_SPEED) {
						setText(map.getString(R.string.max_speed_none), "");
					} else {
						String ds = OsmAndFormatter.getFormattedSpeed(cachedSpeed, map.getMyApplication());
						int ls = ds.lastIndexOf(' ');
						if (ls == -1) {
							setText(ds, null);
						} else {
							setText(ds.substring(0, ls), ds.substring(ls + 1));
						}
					}
					return true;
				}
				return false;
			}
		};
		speedControl.setIcons(R.drawable.widget_max_speed_day, R.drawable.widget_max_speed_night);
		speedControl.setText(null, null);
		return speedControl;
	}
	
	
	
	
	public TextInfoWidget createSpeedControl(final MapActivity map) {
		final OsmandApplication app = map.getMyApplication();
		final TextInfoWidget speedControl = new TextInfoWidget(map) {
			private float cachedSpeed = 0;

			@Override
			public boolean updateInfo(DrawSettings drawSettings) {
				Location loc = app.getLocationProvider().getLastKnownLocation();
				// draw speed
				if (loc != null && loc.hasSpeed()) {
					// .1 mps == 0.36 kph
					float minDelta = .1f;
					// Update more often at walk/run speeds, since we give higher resolution
					// and use .02 instead of .03 to account for rounding effects.
					if (cachedSpeed < 6) {
						minDelta = .015f;
					}
					if (Math.abs(loc.getSpeed() - cachedSpeed) > minDelta) {
						cachedSpeed = loc.getSpeed();
						String ds = OsmAndFormatter.getFormattedSpeed(cachedSpeed, app);
						int ls = ds.lastIndexOf(' ');
						if (ls == -1) {
							setText(ds, null);
						} else {
							setText(ds.substring(0, ls), ds.substring(ls + 1));
						}
						return true;
					}
				} else if (cachedSpeed != 0) {
					cachedSpeed = 0;
					setText(null, null);
					return true;
				}
				return false;
			}
		};
		speedControl.setIcons(R.drawable.widget_speed_day, R.drawable.widget_speed_night);
		speedControl.setText(null, null);
		return speedControl;
	}
	
	public abstract static class DistanceToPointInfoControl extends TextInfoWidget {

		private final OsmandMapTileView view;
		private float[] calculations = new float[1];
		private int cachedMeters;

		public DistanceToPointInfoControl(MapActivity ma, int res, int resNight) {
			super(ma);
			this.view = ma.getMapView();
			if (res != 0 && resNight != 0) {
				setIcons(res, resNight);
			}
			setText(null, null);
			setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View v) {
					click(view);
				}
			});
		}

		protected void click(final OsmandMapTileView view) {
			AnimateDraggingMapThread thread = view.getAnimatedDraggingThread();
			LatLon pointToNavigate = getPointToNavigate();
			if (pointToNavigate != null) {
				int fZoom = view.getZoom() < 15 ? 15 : view.getZoom();
				thread.startMoving(pointToNavigate.getLatitude(), pointToNavigate.getLongitude(), fZoom, true);
			}
		}
		
		@Override
		public boolean updateInfo(DrawSettings drawSettings) {
			int d = getDistance();
			if (distChanged(cachedMeters, d)) {
				cachedMeters = d;
				if (cachedMeters <= 20) {
					cachedMeters = 0;
					setText(null, null);
				} else {
					String ds = OsmAndFormatter.getFormattedDistance(cachedMeters, view.getApplication());
					int ls = ds.lastIndexOf(' ');
					if (ls == -1) {
						setText(ds, null);
					} else {
						setText(ds.substring(0, ls), ds.substring(ls + 1));
					}
				}
				return true;
			}
			return false;
		}

		public abstract LatLon getPointToNavigate();

		public int getDistance() {
			int d = 0;
			LatLon l = getPointToNavigate();
			if (l != null) {
				Location.distanceBetween(view.getLatitude(), view.getLongitude(), l.getLatitude(), l.getLongitude(), calculations);
				d = (int) calculations[0];
			}
			return d;
		}
	}
	
	public TextInfoWidget createDistanceControl(final MapActivity map) {
		DistanceToPointInfoControl distanceControl = new DistanceToPointInfoControl(map,R.drawable.widget_target_day,
				R.drawable.widget_target_night) {
			@Override
			public LatLon getPointToNavigate() {
				TargetPoint p = map.getPointToNavigate();
				return p == null ? null : p.point;
			}

			@Override
			public int getDistance() {
				if (map.getRoutingHelper().isRouteCalculated()) {
					return map.getRoutingHelper().getLeftDistance();
				}
				return super.getDistance();
			}
		};
		return distanceControl;
	}
	
	public TextInfoWidget createIntermediateDistanceControl(final MapActivity map) {
		final TargetPointsHelper targets = map.getMyApplication().getTargetPointsHelper();
		DistanceToPointInfoControl distanceControl = new DistanceToPointInfoControl(map, R.drawable.widget_intermediate_day,
				R.drawable.widget_intermediate_night) {

			@Override
			protected void click(OsmandMapTileView view) {
				if(targets.getIntermediatePoints().size() > 1) {
					map.getMapActions().openIntermediatePointsDialog();
				} else {
					super.click(view);
				}
			}

			@Override
			public LatLon getPointToNavigate() {
				TargetPoint p = targets.getFirstIntermediatePoint();
				return p == null ? null : p.point;
			}

			@Override
			public int getDistance() {
				if (getPointToNavigate() != null && map.getRoutingHelper().isRouteCalculated()) {
					return map.getRoutingHelper().getLeftDistanceNextIntermediate();
				}
				return super.getDistance();
			}
		};
		return distanceControl;
	}


	public static class BearingWidgetState extends WidgetState {

		public static final int BEARING_WIDGET_STATE_RELATIVE_BEARING = R.id.bearing_widget_state_relative_bearing;
		public static final int BEARING_WIDGET_STATE_MAGNETIC_BEARING = R.id.bearing_widget_state_magnetic_bearing;

		private final OsmandPreference<Boolean> showRelativeBearing;

		public BearingWidgetState(OsmandApplication ctx) {
			super(ctx);
			showRelativeBearing = ctx.getSettings().SHOW_RELATIVE_BEARING_OTHERWISE_REGULAR_BEARING;
		}

		@Override
		public int getMenuTitleId() {
			return showRelativeBearing.get() ? R.string.map_widget_bearing : R.string.map_widget_magnetic_bearing;
		}

		@Override
		public int getMenuIconId() {
			return showRelativeBearing.get() ? R.drawable.ic_action_relative_bearing : R.drawable.ic_action_bearing;
		}

		@Override
		public int getMenuItemId() {
			return showRelativeBearing.get() ? BEARING_WIDGET_STATE_RELATIVE_BEARING : BEARING_WIDGET_STATE_MAGNETIC_BEARING;
		}

		@Override
		public int[] getMenuTitleIds() {
			return new int[]{R.string.map_widget_magnetic_bearing, R.string.map_widget_bearing};
		}

		@Override
		public int[] getMenuIconIds() {
			return new int[]{R.drawable.ic_action_bearing, R.drawable.ic_action_relative_bearing};
		}

		@Override
		public int[] getMenuItemIds() {
			return new int[]{BEARING_WIDGET_STATE_MAGNETIC_BEARING, BEARING_WIDGET_STATE_RELATIVE_BEARING};
		}

		@Override
		public void changeState(int stateId) {
			showRelativeBearing.set(stateId == BEARING_WIDGET_STATE_RELATIVE_BEARING);
		}
	}

	public TextInfoWidget createBearingControl(final MapActivity map) {
		final int bearingResId = R.drawable.widget_bearing_day;
		final int bearingNightResId = R.drawable.widget_bearing_night;
		final int relativeBearingResId = R.drawable.widget_relative_bearing_day;
		final int relativeBearingNightResId = R.drawable.widget_relative_bearing_night;
		final OsmandApplication ctx = map.getMyApplication();
		final OsmandPreference<Boolean> showRelativeBearing = ctx.getSettings().SHOW_RELATIVE_BEARING_OTHERWISE_REGULAR_BEARING;

		final TextInfoWidget bearingControl = new TextInfoWidget(map) {
			private int cachedDegrees;
			private float MIN_SPEED_FOR_HEADING = 1f;
			private boolean angularUnitTypeChanged = false;
			private StateChangedListener<OsmandSettings.AngularConstants> listener = new StateChangedListener<OsmandSettings.AngularConstants>() {
				@Override
				public void stateChanged(OsmandSettings.AngularConstants change) {
					angularUnitTypeChanged = true;
				}
			};
			
			{
				getOsmandApplication().getSettings().ANGULAR_UNITS.addListener(listener);
			}

			private LatLon getNextTargetPoint() {
				List<TargetPoint> points = getOsmandApplication().getTargetPointsHelper().getIntermediatePointsWithTarget();
				return points.isEmpty() ? null : points.get(0).point;
			}

			@Override
			public boolean updateInfo(DrawSettings drawSettings) {
				boolean relative = showRelativeBearing.get();
				boolean modeChanged = setIcons(relative ? relativeBearingResId : bearingResId, relative ? relativeBearingNightResId : bearingNightResId);
				setContentTitle(relative ? R.string.map_widget_bearing : R.string.map_widget_magnetic_bearing);
				int b = getBearing(relative);
				if (angularUnitTypeChanged || degreesChanged(cachedDegrees, b) || modeChanged) {
					angularUnitTypeChanged = false;
					cachedDegrees = b;
					if (b != -1000) {
						setText(OsmAndFormatter.getFormattedAzimuth(b, getOsmandApplication()) + (relative ? "" : " M"), null);
					} else {
						setText(null, null);
					}
					return true;
				}
				return false;
			}

			public int getBearing(boolean relative) {
				int d = -1000;
				Location myLocation = getOsmandApplication().getLocationProvider().getLastKnownLocation();
				LatLon l = getNextTargetPoint();
				if (l == null) {
					List<MapMarker> markers = getOsmandApplication().getMapMarkersHelper().getMapMarkers();
					if (markers.size() > 0) {
						l = markers.get(0).point;
					}
				}
				if (myLocation != null && l != null) {
					Location dest = new Location("");
					dest.setLatitude(l.getLatitude());
					dest.setLongitude(l.getLongitude());
					dest.setBearing(myLocation.bearingTo(dest));
					GeomagneticField destGf = new GeomagneticField((float) dest.getLatitude(), (float) dest.getLongitude(), (float) dest.getAltitude(),
							System.currentTimeMillis());
					float bearingToDest = dest.getBearing() - destGf.getDeclination();
					if (relative) {
						float b = -1000;
						Float heading = getOsmandApplication().getLocationProvider().getHeading();
						if ((myLocation.getSpeed() < MIN_SPEED_FOR_HEADING || !myLocation.hasBearing())
								&& heading != null) {
							b = heading;
						} else if (myLocation.hasBearing()) {
							GeomagneticField myLocGf = new GeomagneticField((float) myLocation.getLatitude(), (float) myLocation.getLongitude(), (float) myLocation.getAltitude(),
									System.currentTimeMillis());
							b = myLocation.getBearing() - myLocGf.getDeclination();
						}
						if (b > -1000) {
							bearingToDest -= b;
							if (bearingToDest > 180f) {
								bearingToDest -= 360f;
							} else if (bearingToDest < -180f) {
								bearingToDest += 360f;
							}
							d = (int) bearingToDest;
						}
					} else {
						d = (int) bearingToDest;
					}
				}
				return d;
			}
		};

		bearingControl.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				showRelativeBearing.set(!showRelativeBearing.get());
				map.refreshMap();
			}

		});
		bearingControl.setText(null, null);
		bearingControl.setIcons(!showRelativeBearing.get() ? bearingResId : relativeBearingResId,
				!showRelativeBearing.get() ? bearingNightResId : relativeBearingNightResId);
		return bearingControl;
	}


	public static class LanesControl {
		private MapViewTrackingUtilities trackingUtilities;
		private OsmAndLocationProvider locationProvider;
		private MapRouteInfoMenu mapRouteInfoMenu;
		private RoutingHelper rh;
		private OsmandSettings settings;
		private ImageView lanesView;
		private TextView lanesText;
		private TextView lanesShadowText;
		private OsmandApplication app;
		private int dist;
		private LanesDrawable lanesDrawable;
		private View centerInfo;
		private int shadowRadius;

		public LanesControl(final MapActivity map, final OsmandMapTileView view) {
			lanesView = (ImageView) map.findViewById(R.id.map_lanes);
			lanesText = (TextView) map.findViewById(R.id.map_lanes_dist_text);
			lanesShadowText = (TextView) map.findViewById(R.id.map_lanes_dist_text_shadow);
			centerInfo = (View) map.findViewById(R.id.map_center_info);
			lanesDrawable = new LanesDrawable(map, map.getMapView().getScaleCoefficient());
			lanesView.setImageDrawable(lanesDrawable);
			trackingUtilities = map.getMapViewTrackingUtilities();
			locationProvider = map.getMyApplication().getLocationProvider();
			settings = map.getMyApplication().getSettings();
			mapRouteInfoMenu = map.getMapRouteInfoMenu();
			rh = map.getMyApplication().getRoutingHelper();
			app = map.getMyApplication();
		}
		
		public void updateTextSize(boolean isNight, int textColor, int textShadowColor, boolean textBold, int shadowRadius) {
			this.shadowRadius = shadowRadius;
			TextInfoWidget.updateTextColor(lanesText, lanesShadowText, textColor, textShadowColor, textBold, shadowRadius);
		}
		
		public boolean updateInfo(DrawSettings drawSettings) {
			boolean visible = false;
			int locimminent = -1;
			int[] loclanes = null;
			int dist = 0;
			// TurnType primary = null;
			if ((rh == null || !rh.isFollowingMode() || rh.isDeviatedFromRoute() || rh.getCurrentGPXRoute() != null)
					&& trackingUtilities.isMapLinkedToLocation() && settings.SHOW_LANES.get()) {
				RouteDataObject ro = locationProvider.getLastKnownRouteSegment();
				Location lp = locationProvider.getLastKnownLocation();
				if(ro != null) {
					float degree = lp == null || !lp.hasBearing() ? 0 : lp.getBearing();
					loclanes = RouteResultPreparation.parseTurnLanes(ro, degree / 180 * Math.PI);
					if(loclanes == null) {
						loclanes = RouteResultPreparation.parseLanes(ro, degree / 180 * Math.PI);
					}
				}
			} else if (rh != null && rh.isRouteCalculated() ) {
				if (rh.isFollowingMode() && settings.SHOW_LANES.get()) {
					NextDirectionInfo r = rh.getNextRouteDirectionInfo(new NextDirectionInfo(), false);
					if(r != null && r.directionInfo != null && r.directionInfo.getTurnType() != null) {
						loclanes  = r.directionInfo.getTurnType().getLanes();
						// primary = r.directionInfo.getTurnType();
						locimminent = r.imminent;
						// Do not show too far 
						if ((r.distanceTo > 800 && r.directionInfo.getTurnType().isSkipToSpeak()) || r.distanceTo > 1200) {
							loclanes = null;
						}
						dist = r.distanceTo;
					}
				} else {
					int di = MapRouteInfoMenu.getDirectionInfo();
					if (di >= 0 && mapRouteInfoMenu.isVisible()
							&& di < rh.getRouteDirections().size()) {
						RouteDirectionInfo next = rh.getRouteDirections().get(di);
						if (next != null) {
							loclanes = next.getTurnType().getLanes();
							// primary = next.getTurnType();
						}
					} else {
						loclanes = null;
					}
				}
			}
			visible = loclanes != null && loclanes.length > 0 && !MapRouteInfoMenu.chooseRoutesVisible && !MapRouteInfoMenu.waypointsVisible;
			if (visible) {
				if (!Arrays.equals(lanesDrawable.lanes, loclanes) || 
						(locimminent == 0) != lanesDrawable.imminent) {
					lanesDrawable.imminent = locimminent == 0;
					lanesDrawable.lanes = loclanes;
					lanesDrawable.updateBounds();
					lanesView.setImageDrawable(null);
					lanesView.setImageDrawable(lanesDrawable);
					lanesView.requestLayout();
					lanesView.invalidate();
				}
				if (distChanged(dist, this.dist)) {
					this.dist = dist;
					if(dist == 0) {
						lanesShadowText.setText("");
						lanesText.setText("");
					} else {
						lanesShadowText.setText(OsmAndFormatter.getFormattedDistance(dist, app));
						lanesText.setText(OsmAndFormatter.getFormattedDistance(dist, app));
					}
					lanesShadowText.invalidate();
					lanesText.invalidate();
				}
			}
			AndroidUiHelper.updateVisibility(lanesShadowText, visible && shadowRadius > 0);
			AndroidUiHelper.updateVisibility(lanesText, visible);
			AndroidUiHelper.updateVisibility(lanesView, visible);
			AndroidUiHelper.updateVisibility(centerInfo, visible);
			return true;
		}
	}
	
	
	private static class LanesDrawable extends Drawable {
		int[] lanes = null;
		boolean imminent = false;
		private Context ctx;
		private Paint paintBlack;
		private Paint paintRouteDirection;
		private Paint paintSecondTurn;
		private float scaleCoefficient;
		private int height;
		private int width;
		private float delta;
		private float laneHalfSize;
		private static final float miniCoeff = 2f;
		private final boolean leftSide;
		private int imgMinDelta;
		private int imgMargin;

		LanesDrawable(MapActivity ctx, float scaleCoefficent) {
			this.ctx = ctx;
			OsmandSettings settings = ctx.getMyApplication().getSettings();
			leftSide = settings.DRIVING_REGION.get().leftHandDriving;
			imgMinDelta = ctx.getResources().getDimensionPixelSize(R.dimen.widget_turn_lane_min_delta);
			imgMargin = ctx.getResources().getDimensionPixelSize(R.dimen.widget_turn_lane_margin);
			laneHalfSize = ctx.getResources().getDimensionPixelSize(R.dimen.widget_turn_lane_size) / 2;

			this.scaleCoefficient = scaleCoefficent;

			paintBlack = new Paint(Paint.ANTI_ALIAS_FLAG);
			paintBlack.setStyle(Style.STROKE);
			paintBlack.setColor(Color.BLACK);
			paintBlack.setStrokeWidth(scaleCoefficent);

			paintRouteDirection = new Paint(Paint.ANTI_ALIAS_FLAG);
			paintRouteDirection.setStyle(Style.FILL);
			paintRouteDirection.setColor(ctx.getResources().getColor(R.color.nav_arrow));

			paintSecondTurn = new Paint(Paint.ANTI_ALIAS_FLAG);
			paintSecondTurn.setStyle(Style.FILL);
			paintSecondTurn.setColor(ctx.getResources().getColor(R.color.nav_arrow_distant));
		}

		void updateBounds() {
			float w = 0;
			float h = 0;
			float delta = imgMinDelta;
			float coef = scaleCoefficient / miniCoeff;
			if (lanes != null) {
				List<RectF> boundsList = new ArrayList<>(lanes.length);
				for (int i = 0; i < lanes.length; i++) {
					int turnType = TurnType.getPrimaryTurn(lanes[i]);
					int secondTurnType = TurnType.getSecondaryTurn(lanes[i]);
					int thirdTurnType = TurnType.getTertiaryTurn(lanes[i]);

					RectF imgBounds = new RectF();
					if (thirdTurnType > 0) {
						Path p = TurnPathHelper.getPathFromTurnType(ctx.getResources(), turnType,
								secondTurnType, thirdTurnType, TurnPathHelper.THIRD_TURN, coef, leftSide, true);
						if (p != null) {
							RectF b = new RectF();
							p.computeBounds(b, true);
							if (!b.isEmpty()) {
								if (imgBounds.isEmpty()) {
									imgBounds.set(b);
								} else {
									imgBounds.union(b);
								}
							}
						}
					}
					if (secondTurnType > 0) {
						Path p = TurnPathHelper.getPathFromTurnType(ctx.getResources(), turnType,
								secondTurnType, thirdTurnType, TurnPathHelper.SECOND_TURN, coef, leftSide, true);
						if (p != null) {
							RectF b = new RectF();
							p.computeBounds(b, true);
							if (!b.isEmpty()) {
								if (imgBounds.isEmpty()) {
									imgBounds.set(b);
								} else {
									imgBounds.union(b);
								}
							}
						}
					}
					Path p = TurnPathHelper.getPathFromTurnType(ctx.getResources(), turnType,
							secondTurnType, thirdTurnType, TurnPathHelper.FIRST_TURN, coef, leftSide, true);
					if (p != null) {
						RectF b = new RectF();
						p.computeBounds(b, true);
						if (!b.isEmpty()) {
							if (imgBounds.isEmpty()) {
								imgBounds.set(b);
							} else {
								imgBounds.union(b);
							}
						}
					}
					if (imgBounds.right > 0)
					{
						boundsList.add(imgBounds);

						float imageHeight = imgBounds.bottom;
						if (imageHeight > h)
							h = imageHeight;
					}
				}
				if (boundsList.size() > 1) {
					for (int i = 1; i < boundsList.size(); i++) {
						RectF b1 = boundsList.get(i - 1);
						RectF b2 = boundsList.get(i);
						float d = b1.right + imgMargin * 2 - b2.left;
						if (delta < d)
							delta = d;
					}
					RectF b1 = boundsList.get(0);
					RectF b2 = boundsList.get(boundsList.size() - 1);
					w = -b1.left + (boundsList.size() - 1) * delta + b2.right;
				} else if (boundsList.size() > 0) {
					RectF b1 = boundsList.get(0);
					w = b1.width();
				}
				if (w > 0) {
					w += 4;
				}
				if (h > 0) {
					h += 4;
				}
			}
			this.width = (int) w;
			this.height = (int) h;
			this.delta = delta;
		}
		
		@Override
		public int getIntrinsicHeight() {
			return height;
		}
		
		@Override
		public int getIntrinsicWidth() {
			return width;
		}


		@Override
		public void draw(@NonNull Canvas canvas) {
			// setup default color
			//canvas.drawColor(0, PorterDuff.Mode.CLEAR);

			//to change color immediately when needed
			if (lanes != null && lanes.length > 0) {
				float coef = scaleCoefficient / miniCoeff;
				canvas.save();
				// canvas.translate((int) (16 * scaleCoefficient), 0);
				for (int i = 0; i < lanes.length; i++) {
					if ((lanes[i] & 1) == 1) {
						paintRouteDirection.setColor(imminent ? ctx.getResources().getColor(R.color.nav_arrow_imminent) :
								ctx.getResources().getColor(R.color.nav_arrow));
					} else {
						paintRouteDirection.setColor(ctx.getResources().getColor(R.color.nav_arrow_distant));
					}
					int turnType = TurnType.getPrimaryTurn(lanes[i]);
					int secondTurnType = TurnType.getSecondaryTurn(lanes[i]);
					int thirdTurnType = TurnType.getTertiaryTurn(lanes[i]);

					RectF imgBounds = new RectF();
					Path thirdTurnPath = null;
					Path secondTurnPath = null;
					Path firstTurnPath = null;

					if (thirdTurnType > 0) {
						Path p = TurnPathHelper.getPathFromTurnType(ctx.getResources(), turnType,
								secondTurnType, thirdTurnType, TurnPathHelper.THIRD_TURN, coef, leftSide, true);
						if (p != null) {
							RectF b = new RectF();
							p.computeBounds(b, true);
							if (!b.isEmpty()) {
								if (imgBounds.isEmpty()) {
									imgBounds.set(b);
								} else {
									imgBounds.union(b);
								}
								thirdTurnPath = p;
							}
						}
					}
					if (secondTurnType > 0) {
						Path p = TurnPathHelper.getPathFromTurnType(ctx.getResources(), turnType,
								secondTurnType, thirdTurnType, TurnPathHelper.SECOND_TURN, coef, leftSide, true);
						if (p != null) {
							RectF b = new RectF();
							p.computeBounds(b, true);
							if (!b.isEmpty()) {
								if (imgBounds.isEmpty()) {
									imgBounds.set(b);
								} else {
									imgBounds.union(b);
								}
								secondTurnPath = p;
							}
						}
					}
					Path p = TurnPathHelper.getPathFromTurnType(ctx.getResources(), turnType,
							secondTurnType, thirdTurnType, TurnPathHelper.FIRST_TURN, coef, leftSide, true);
					if (p != null) {
						RectF b = new RectF();
						p.computeBounds(b, true);
						if (!b.isEmpty()) {
							if (imgBounds.isEmpty()) {
								imgBounds.set(b);
							} else {
								imgBounds.union(b);
							}
							firstTurnPath = p;
						}
					}

					if (firstTurnPath != null || secondTurnPath != null || thirdTurnPath != null) {
						if (i == 0) {
							imgBounds.set(imgBounds.left - 2, imgBounds.top, imgBounds.right + 2, imgBounds.bottom);
							canvas.translate(-imgBounds.left, 0);
						} else {
							canvas.translate(-laneHalfSize, 0);
						}

						// 1st pass
						if (thirdTurnPath != null) {
							//canvas.drawPath(thirdTurnPath, paintSecondTurn);
							canvas.drawPath(thirdTurnPath, paintBlack);
						}
						if (secondTurnPath != null) {
							//canvas.drawPath(secondTurnPath, paintSecondTurn);
							canvas.drawPath(secondTurnPath, paintBlack);
						}
						if (firstTurnPath != null) {
							//canvas.drawPath(firstTurnPath, paintRouteDirection);
							canvas.drawPath(firstTurnPath, paintBlack);
						}

						// 2nd pass
						if (thirdTurnPath != null) {
							canvas.drawPath(thirdTurnPath, paintSecondTurn);
						}
						if (secondTurnPath != null) {
							canvas.drawPath(secondTurnPath, paintSecondTurn);
						}
						if (firstTurnPath != null) {
							canvas.drawPath(firstTurnPath, paintRouteDirection);
						}

						canvas.translate(laneHalfSize + delta, 0);
					}
				}
				canvas.restore();
			}
		}

		@Override
		public void setAlpha(int alpha) {
		}

		@Override
		public void setColorFilter(ColorFilter cf) {
		}

		@Override
		public int getOpacity() {
			return 0;
		}
		
	}
	
	
	public LanesControl createLanesControl(final MapActivity map, final OsmandMapTileView view) {
		return new LanesControl(map, view);
	}

	public static class RulerWidget  {
		
		private View layout;
		private ImageView icon;
		private TextView text;
		private TextView textShadow;
		private MapActivity ma;
		private String cacheRulerText;
		private int maxWidth;
		private float cacheMapDensity;
		private OsmandSettings.OsmandPreference<Float> mapDensity;
		private int cacheRulerZoom;
		private double cacheRulerTileX;
		private double cacheRulerTileY;
		private boolean orientationPortrait;

		public RulerWidget(final OsmandApplication app, MapActivity ma) {
			this.ma = ma;
			layout = ma.findViewById(R.id.map_ruler_layout);
			icon = (ImageView) ma.findViewById(R.id.map_ruler_image);
			text = (TextView) ma.findViewById(R.id.map_ruler_text);
			textShadow = (TextView) ma.findViewById(R.id.map_ruler_text_shadow);
			maxWidth = ma.getResources().getDimensionPixelSize(R.dimen.map_ruler_width);
			orientationPortrait = AndroidUiHelper.isOrientationPortrait(ma);
			mapDensity = ma.getMyApplication().getSettings().MAP_DENSITY;
			cacheMapDensity = mapDensity.get();
		}
		
		public void updateTextSize(boolean isNight, int textColor, int textShadowColor, int shadowRadius) {
			TextInfoWidget.updateTextColor(text, textShadow, textColor, textShadowColor, false, shadowRadius);
			icon.setBackgroundResource(isNight ? R.drawable.ruler_night : R.drawable.ruler);
		}
		
		public boolean updateInfo(RotatedTileBox tb, DrawSettings nightMode) {
			boolean visible = true;
			OsmandMapTileView view = ma.getMapView();
			// update cache
			if (view.isZooming()) {
				visible = false;
			} else if (!tb.isZoomAnimated() && (tb.getZoom() != cacheRulerZoom || Math.abs(tb.getCenterTileX() - cacheRulerTileX) > 1 || Math
					.abs(tb.getCenterTileY() - cacheRulerTileY) > 1 || mapDensity.get() != cacheMapDensity) &&
					tb.getPixWidth() > 0 && maxWidth > 0) {
				cacheRulerZoom = tb.getZoom();
				cacheRulerTileX = tb.getCenterTileX();
				cacheRulerTileY = tb.getCenterTileY();
				cacheMapDensity = mapDensity.get();
				double pixDensity = tb.getPixDensity();
				double roundedDist = OsmAndFormatter.calculateRoundedDist(maxWidth / 
						pixDensity, view.getApplication());

				int cacheRulerDistPix = (int) (pixDensity * roundedDist);
				cacheRulerText = OsmAndFormatter.getFormattedDistance((float) roundedDist, view.getApplication(), false);
				textShadow.setText(cacheRulerText);
				text.setText(cacheRulerText);
				ViewGroup.LayoutParams lp = layout.getLayoutParams();
				lp.width = cacheRulerDistPix;
				layout.setLayoutParams(lp);
				layout.requestLayout();
			}
			AndroidUiHelper.updateVisibility(layout, visible);
			return true;
		}
		
		public void setVisibility(boolean visibility) {
			layout.setVisibility(visibility ? View.VISIBLE : View.GONE);
		}
	}
	
	public static class AlarmWidget  {
		
		private View layout;
		private ImageView icon;
		private TextView text;
		private TextView bottomText;
		private OsmandSettings settings;
		private RoutingHelper rh;
		private MapViewTrackingUtilities trackingUtilities;
		private OsmAndLocationProvider locationProvider;
		private WaypointHelper wh;
		private int imgId;
		private String textString;
		private String bottomTextString;

		public AlarmWidget(final OsmandApplication app, MapActivity ma) {
			layout = ma.findViewById(R.id.map_alarm_warning);
			icon = (ImageView) ma.findViewById(R.id.map_alarm_warning_icon);
			text = (TextView) ma.findViewById(R.id.map_alarm_warning_text);
			bottomText = (TextView) ma.findViewById(R.id.map_alarm_warning_text_bottom);
			settings = app.getSettings();
			rh = ma.getRoutingHelper();
			trackingUtilities = ma.getMapViewTrackingUtilities();
			locationProvider = app.getLocationProvider();
			wh = app.getWaypointHelper();
		}
		
		public boolean updateInfo(DrawSettings drawSettings) {
			boolean showRoutingAlarms = settings.SHOW_ROUTING_ALARMS.get();
			boolean trafficWarnings = settings.SHOW_TRAFFIC_WARNINGS.get();
			boolean cams = settings.SHOW_CAMERAS.get();
			boolean peds = settings.SHOW_PEDESTRIAN.get();
			boolean tunnels = settings.SHOW_TUNNELS.get();
			boolean visible = false;
			if ((rh.isFollowingMode() || trackingUtilities.isMapLinkedToLocation())
					&& showRoutingAlarms && (trafficWarnings || cams)) {
				AlarmInfo alarm;
				if(rh.isFollowingMode() && !rh.isDeviatedFromRoute() && rh.getCurrentGPXRoute() == null) {
					alarm = wh.getMostImportantAlarm(settings.SPEED_SYSTEM.get(), cams);
				} else {
					RouteDataObject ro = locationProvider.getLastKnownRouteSegment();
					Location loc = locationProvider.getLastKnownLocation();
					if(ro != null && loc != null) {
						alarm = wh.calculateMostImportantAlarm(ro, loc, settings.METRIC_SYSTEM.get(),
								settings.SPEED_SYSTEM.get(), cams);
					} else {
						alarm = null;
					}
				}
				if(alarm != null) {
					int locimgId = R.drawable.warnings_limit;
					String text = "";
					String bottomText = "";
					if(alarm.getType() == AlarmInfoType.SPEED_LIMIT) {
						if(settings.DRIVING_REGION.get().americanSigns){
							locimgId = R.drawable.warnings_speed_limit_us;
						//else case is done by drawing red ring
						}
						text = alarm.getIntValue() +"";
					} else if(alarm.getType() == AlarmInfoType.SPEED_CAMERA) {
						locimgId = R.drawable.warnings_speed_camera;
					} else if(alarm.getType() == AlarmInfoType.BORDER_CONTROL) {
						locimgId = R.drawable.warnings_border_control;
					} else if(alarm.getType() == AlarmInfoType.HAZARD) {
						if(settings.DRIVING_REGION.get().americanSigns){
							locimgId = R.drawable.warnings_hazard_us;
						} else {
							locimgId = R.drawable.warnings_hazard;
						}
					} else if(alarm.getType() == AlarmInfoType.TOLL_BOOTH) {
						//image done by drawing red ring
						text = "$";
					} else if(alarm.getType() == AlarmInfoType.TRAFFIC_CALMING) {
						if(settings.DRIVING_REGION.get().americanSigns){
							locimgId = R.drawable.warnings_traffic_calming_us;
						} else {
							locimgId = R.drawable.warnings_traffic_calming;
						}
					} else if(alarm.getType() == AlarmInfoType.STOP) {
						locimgId = R.drawable.warnings_stop;
					} else if(alarm.getType() == AlarmInfoType.RAILWAY) {
						if(settings.DRIVING_REGION.get().americanSigns){
							locimgId = R.drawable.warnings_railways_us;
						} else {
							locimgId = R.drawable.warnings_railways;
						}
					} else if(alarm.getType() == AlarmInfoType.PEDESTRIAN) {
						if(settings.DRIVING_REGION.get().americanSigns){
							locimgId = R.drawable.warnings_pedestrian_us;
						} else {
							locimgId = R.drawable.warnings_pedestrian;
						}
					} else if(alarm.getType() == AlarmInfoType.TUNNEL) {
						if(settings.DRIVING_REGION.get().americanSigns){
							locimgId = R.drawable.warnings_tunnel_us;
						} else {
							locimgId = R.drawable.warnings_tunnel;
						}
						bottomText = OsmAndFormatter.getFormattedAlarmInfoDistance(settings.getContext(), alarm.getFloatValue());
					} else {
						text = null;
						bottomText = null;
					}
					visible = (text != null &&  text.length() > 0) || (locimgId != 0);
					if (visible) {
						if (alarm.getType() == AlarmInfoType.SPEED_CAMERA) {
							visible = cams;
						} else if (alarm.getType() == AlarmInfoType.PEDESTRIAN) {
							visible = peds;
						} else if (alarm.getType() == AlarmInfoType.TUNNEL) {
							visible = tunnels;
						} else {
							visible = trafficWarnings;
						}
					}
					if(visible) {
						if(locimgId != imgId) {
							imgId = locimgId;
							icon.setImageResource(locimgId);
						}
						if (!Algorithms.objectEquals(text, this.textString)) {
							textString = text;
							this.text.setText(this.textString);
							if (alarm.getType() == AlarmInfoType.SPEED_LIMIT && settings.DRIVING_REGION.get().americanSigns) {
								this.text.setPadding(0, AndroidUtils.dpToPx(layout.getContext(), 20f), 0, 0);
							} else {
								this.text.setPadding(0, 0, 0, 0);
							}
						}
						if (!Algorithms.objectEquals(bottomText, this.bottomTextString)) {
							bottomTextString = bottomText;
							this.bottomText.setText(this.bottomTextString);
							this.bottomText.setTextColor(ContextCompat.getColor(layout.getContext(),
									settings.DRIVING_REGION.get().americanSigns ? R.color.color_black : R.color.color_white));
						}
					}
				}
			}
			AndroidUiHelper.updateVisibility(layout, visible);
			return true;
		}

		public void setVisibility(boolean visibility) {
			layout.setVisibility(visibility ? View.VISIBLE : View.GONE);
		}
	}

	public static boolean distChanged(int oldDist, int dist){
		if (oldDist != 0 && Math.abs(oldDist - dist) < 10) {
			return false;
		}
		return true;
	}

	public static boolean degreesChanged(int oldDegrees, int degrees){
		return Math.abs(oldDegrees - degrees) >= 1;
	}

	public AlarmWidget createAlarmInfoControl(OsmandApplication app, MapActivity map) {
		return new AlarmWidget(app, map);
	}
	
	public RulerWidget createRulerControl(OsmandApplication app, MapActivity map) {
		return new RulerWidget(app, map);
	}
}
