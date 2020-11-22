package net.osmand.plus.views.mapwidgets;


import android.app.Activity;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.GeomagneticField;
import android.os.BatteryManager;
import android.text.format.DateFormat;
import android.view.View;

import net.osmand.Location;
import net.osmand.binary.RouteDataObject;
import net.osmand.data.LatLon;
import net.osmand.plus.mapmarkers.MapMarker;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmAndLocationProvider;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.OsmandPreference;
import net.osmand.plus.R;
import net.osmand.plus.TargetPointsHelper;
import net.osmand.plus.TargetPointsHelper.TargetPoint;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.MapViewTrackingUtilities;
import net.osmand.plus.routing.RouteCalculationResult.NextDirectionInfo;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.views.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.mapwidgets.widgets.AlarmWidget;
import net.osmand.plus.views.mapwidgets.widgets.DistanceToPointWidget;
import net.osmand.plus.views.mapwidgets.widgets.NextTurnWidget;
import net.osmand.plus.views.mapwidgets.widgets.RulerWidget;
import net.osmand.plus.views.mapwidgets.widgets.TextInfoWidget;
import net.osmand.router.TurnType;

import java.util.List;

public class RouteInfoWidgetsFactory {

	public NextTurnWidget createNextInfoControl(final Activity activity,
	                                            final OsmandApplication app, boolean horisontalMini) {
		final OsmandSettings settings = app.getSettings();
		final RoutingHelper routingHelper = app.getRoutingHelper();
		final NextTurnWidget nextTurnInfo = new NextTurnWidget(activity, app, horisontalMini) {
			NextDirectionInfo calc1 = new NextDirectionInfo();

			@Override
			public boolean updateInfo(DrawSettings drawSettings) {
				boolean followingMode = routingHelper.isFollowingMode() || app.getLocationProvider().getLocationSimulation().isRouteAnimating();
				TurnType turnType = null;
				boolean deviatedFromRoute = false;
				int turnImminent = 0;
				int nextTurnDistance = 0;
				if (routingHelper.isRouteCalculated() && followingMode) {
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
				if (routingHelper.isRouteCalculated() && !routingHelper.isDeviatedFromRoute()) {
					routingHelper.getVoiceRouter().announceCurrentDirection(null);
				}
			}
		});
		// initial state
		return nextTurnInfo;
	}
	
	public NextTurnWidget createNextNextInfoControl(final Activity activity,
	                                                final OsmandApplication app, boolean horisontalMini) {
		final RoutingHelper routingHelper = app.getRoutingHelper();
		final NextTurnWidget nextTurnInfo = new NextTurnWidget(activity, app, horisontalMini) {
			NextDirectionInfo calc1 = new NextDirectionInfo();
			@Override
			public boolean updateInfo(DrawSettings drawSettings) {
				boolean followingMode = routingHelper.isFollowingMode() || app.getLocationProvider().getLocationSimulation().isRouteAnimating();
				TurnType turnType = null;
				boolean deviatedFromRoute = false;
				int turnImminent = 0;
				int nextTurnDistance = 0;
				if (routingHelper.isRouteCalculated() && followingMode) {
					deviatedFromRoute = routingHelper.isDeviatedFromRoute();
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

	public TextInfoWidget createTimeControl(final MapActivity map, final boolean intermediate) {
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
			}
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

	public TextInfoWidget createPlainTimeControl(final MapActivity map) {
		final OsmandApplication ctx = map.getMyApplication();
		final TextInfoWidget plainTimeControl = new TextInfoWidget(map) {
			private long cachedLeftTime = 0;
			
			@Override
			public boolean updateInfo(DrawSettings drawSettings) {
				long time = System.currentTimeMillis();
				if (isUpdateNeeded() || time - cachedLeftTime > 5000) {
					cachedLeftTime = time;
					if (DateFormat.is24HourFormat(ctx)) {
						setText(DateFormat.format("k:mm", time).toString(), null); //$NON-NLS-1$
					} else {
						setText(DateFormat.format("h:mm", time).toString(),
								DateFormat.format("aa", time).toString()); //$NON-NLS-1$
					}
				}
				return false;
			}
		};
		plainTimeControl.setText(null, null);
		plainTimeControl.setIcons(R.drawable.widget_time_day, R.drawable.widget_time_night);
		return plainTimeControl;
	}

	public TextInfoWidget createBatteryControl(final MapActivity map) {
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
				if (isUpdateNeeded() || time - cachedLeftTime > 1000) {
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
						setText(String.format("%d%%", (level * 100) / scale), null);
						setIcons(charging ? batteryCharging : battery, charging ? batteryChargingN : batteryN);
					}
				}
				return false;
			}
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
				if ((rh == null || !rh.isFollowingMode() || rh.isDeviatedFromRoute() || (rh.getCurrentGPXRoute() != null && !rh.isCurrentGPXRouteV2()))
						&& trackingUtilities.isMapLinkedToLocation()) {
					RouteDataObject ro = locationProvider.getLastKnownRouteSegment();
					if (ro != null) {
						mx = ro.getMaximumSpeed(ro.bearingVsRouteDirection(locationProvider.getLastKnownLocation()));
					}
				} else if (rh != null) {
					mx = rh.getCurrentMaxSpeed();
				} else {
					mx = 0f;
				}
				if (isUpdateNeeded() || cachedSpeed != mx) {
					cachedSpeed = mx;
					if (cachedSpeed == 0) {
						setText(null, null);
					} else if (cachedSpeed == RouteDataObject.NONE_MAX_SPEED) {
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

			@Override
			public boolean isMetricSystemDepended() {
				return true;
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
					if (isUpdateNeeded() || Math.abs(loc.getSpeed() - cachedSpeed) > minDelta) {
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

			@Override
			public boolean isMetricSystemDepended() {
				return true;
			}
		};
		speedControl.setIcons(R.drawable.widget_speed_day, R.drawable.widget_speed_night);
		speedControl.setText(null, null);
		return speedControl;
	}

	public TextInfoWidget createDistanceControl(final MapActivity map) {
		DistanceToPointWidget distanceControl = new DistanceToPointWidget(map, R.drawable.widget_target_day,
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
		DistanceToPointWidget distanceControl = new DistanceToPointWidget(map, R.drawable.widget_intermediate_day,
				R.drawable.widget_intermediate_night) {

			@Override
			protected void click(OsmandMapTileView view) {
				if (targets.getIntermediatePoints().size() > 1) {
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

			private LatLon getNextTargetPoint() {
				List<TargetPoint> points = getApplication().getTargetPointsHelper().getIntermediatePointsWithTarget();
				return points.isEmpty() ? null : points.get(0).point;
			}

			@Override
			public boolean updateInfo(DrawSettings drawSettings) {
				boolean relative = showRelativeBearing.get();
				boolean modeChanged = setIcons(relative ? relativeBearingResId : bearingResId, relative ? relativeBearingNightResId : bearingNightResId);
				setContentTitle(relative ? R.string.map_widget_bearing : R.string.map_widget_magnetic_bearing);
				int b = getBearing(relative);
				if (isUpdateNeeded() || degreesChanged(cachedDegrees, b) || modeChanged) {
					cachedDegrees = b;
					if (b != -1000) {
						setText(OsmAndFormatter.getFormattedAzimuth(b, getApplication()) + (relative ? "" : " M"), null);
					} else {
						setText(null, null);
					}
					return true;
				}
				return false;
			}

			@Override
			public boolean isAngularUnitsDepended() {
				return true;
			}

			public int getBearing(boolean relative) {
				int d = -1000;
				Location myLocation = getApplication().getLocationProvider().getLastKnownLocation();
				LatLon l = getNextTargetPoint();
				if (l == null) {
					List<MapMarker> markers = getApplication().getMapMarkersHelper().getMapMarkers();
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
						Float heading = getApplication().getLocationProvider().getHeading();
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

	public static boolean distChanged(int oldDist, int dist) {
		return oldDist == 0 || Math.abs(oldDist - dist) >= 10;
	}

	public static boolean degreesChanged(int oldDegrees, int degrees) {
		return Math.abs(oldDegrees - degrees) >= 1;
	}

	public static LanesControl createLanesControl(final MapActivity map, final OsmandMapTileView view) {
		return new LanesControl(map, view);
	}

	public static AlarmWidget createAlarmInfoControl(OsmandApplication app, MapActivity map) {
		return new AlarmWidget(app, map);
	}

	public static RulerWidget createRulerControl(MapActivity map, View view) {
		return new RulerWidget(map, view);
	}
}