package net.osmand.plus.views.mapwidgets;


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
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.OsmAndLocationProvider;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.preferences.OsmandPreference;
import net.osmand.plus.R;
import net.osmand.plus.helpers.TargetPointsHelper;
import net.osmand.plus.helpers.TargetPointsHelper.TargetPoint;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.MapViewTrackingUtilities;
import net.osmand.plus.routing.RouteCalculationResult.NextDirectionInfo;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.mapwidgets.widgets.AlarmWidget;
import net.osmand.plus.views.mapwidgets.widgets.DistanceToPointWidget;
import net.osmand.plus.views.mapwidgets.widgets.NextTurnWidget;
import net.osmand.plus.views.mapwidgets.widgets.RulerWidget;
import net.osmand.plus.views.mapwidgets.widgets.TextInfoWidget;
import net.osmand.router.TurnType;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class RouteInfoWidgetsFactory {

	private final OsmandApplication app;
	private final OsmandSettings settings;
	private final RoutingHelper routingHelper;
	private final OsmAndLocationProvider locationProvider;

	public RouteInfoWidgetsFactory(@NonNull OsmandApplication app) {
		this.app = app;
		this.settings = app.getSettings();
		this.routingHelper = app.getRoutingHelper();
		this.locationProvider = app.getLocationProvider();
	}

	public NextTurnWidget createNextInfoControl(@NonNull MapActivity mapActivity, boolean horizontalMini) {
		NextTurnWidget nextTurnInfo = new NextTurnWidget(mapActivity, horizontalMini) {
			final NextDirectionInfo calc1 = new NextDirectionInfo();

			@Override
			public void updateInfo(@Nullable DrawSettings drawSettings) {
				boolean followingMode = routingHelper.isFollowingMode() || locationProvider.getLocationSimulation().isRouteAnimating();
				TurnType turnType = null;
				boolean deviatedFromRoute = false;
				int turnImminent = 0;
				int nextTurnDistance = 0;
				if (routingHelper.isRouteCalculated() && followingMode) {
					deviatedFromRoute = routingHelper.isDeviatedFromRoute();
					
					if (deviatedFromRoute) {
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
			}
		};
		nextTurnInfo.setOnClickListener(v -> {
			if (routingHelper.isRouteCalculated() && !routingHelper.isDeviatedFromRoute()) {
				routingHelper.getVoiceRouter().announceCurrentDirection(null);
			}
		});
		return nextTurnInfo;
	}
	
	public NextTurnWidget createNextNextInfoControl(@NonNull MapActivity activity, boolean horizontalMini) {
		NextTurnWidget nextTurnInfo = new NextTurnWidget(activity, horizontalMini) {
			final NextDirectionInfo calc1 = new NextDirectionInfo();
			@Override
			public void updateInfo(@Nullable DrawSettings drawSettings) {
				boolean followingMode = routingHelper.isFollowingMode() || locationProvider.getLocationSimulation().isRouteAnimating();
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
			}
		};
		return nextTurnInfo;
	}

	public TextInfoWidget createTimeControl(@NonNull MapActivity mapActivity, boolean intermediate) {
		final OsmandPreference<Boolean> showArrival = intermediate
				? settings.SHOW_INTERMEDIATE_ARRIVAL_TIME_OTHERWISE_EXPECTED_TIME
				: settings.SHOW_ARRIVAL_TIME_OTHERWISE_EXPECTED_TIME;

		final TextInfoWidget leftTimeControl = new TextInfoWidget(mapActivity) {
			private long cachedLeftTime = 0;
			
			@Override
			public void updateInfo(@Nullable DrawSettings drawSettings) {
				setTimeControlIcons(this, showArrival.get(), intermediate);
				int time = 0;
				if (routingHelper != null && routingHelper.isRouteCalculated()) {
					time = intermediate ? routingHelper.getLeftTimeNextIntermediate() : routingHelper.getLeftTime();

					if (time != 0) {
						if (showArrival.get()) {
							long toFindTime = time * 1000L + System.currentTimeMillis();
							if (Math.abs(toFindTime - cachedLeftTime) > 30000) {
								cachedLeftTime = toFindTime;
								setContentTitle(getString(R.string.access_arrival_time));
								if (DateFormat.is24HourFormat(app)) {
									setText(DateFormat.format("k:mm", toFindTime).toString(), null); //$NON-NLS-1$
								} else {
									setText(DateFormat.format("h:mm", toFindTime).toString(),
											DateFormat.format("aa", toFindTime).toString()); //$NON-NLS-1$
								}
							}
						} else {
							if (Math.abs(time - cachedLeftTime) > 30) {
								cachedLeftTime = time;
								int hours = time / (60 * 60);
								int minutes = (time / 60) % 60;
								setContentTitle(getString(R.string.map_widget_time));
								setText(String.format("%d:%02d", hours, minutes), null); //$NON-NLS-1$
							}
						}
					}
				}
				if (time == 0 && cachedLeftTime != 0) {
					cachedLeftTime = 0;
					setText(null, null);
				}
			}
		};
		leftTimeControl.setOnClickListener(v -> {
			showArrival.set(!showArrival.get());
			setTimeControlIcons(leftTimeControl, showArrival.get(), intermediate);
			mapActivity.refreshMap();
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

	public TextInfoWidget createPlainTimeControl(@NonNull MapActivity mapActivity) {
		TextInfoWidget plainTimeControl = new TextInfoWidget(mapActivity) {
			private long cachedLeftTime = 0;
			
			@Override
			public void updateInfo(@Nullable DrawSettings drawSettings) {
				long time = System.currentTimeMillis();
				if (isUpdateNeeded() || time - cachedLeftTime > 5000) {
					cachedLeftTime = time;
					if (DateFormat.is24HourFormat(app)) {
						setText(DateFormat.format("k:mm", time).toString(), null);
					} else {
						setText(DateFormat.format("h:mm", time).toString(),
								DateFormat.format("aa", time).toString());
					}
				}
			}
		};
		plainTimeControl.setText(null, null);
		plainTimeControl.setIcons(R.drawable.widget_time_day, R.drawable.widget_time_night);
		return plainTimeControl;
	}

	public TextInfoWidget createBatteryControl(@NonNull MapActivity mapActivity) {
		int battery = R.drawable.widget_battery_day;
		int batteryN = R.drawable.widget_battery_night;
		int batteryCharging = R.drawable.widget_battery_charging_day;
		int batteryChargingN = R.drawable.widget_battery_charging_night;
		TextInfoWidget batteryControl = new TextInfoWidget(mapActivity) {
			private long cachedLeftTime = 0;

			@Override
			public void updateInfo(@Nullable DrawSettings drawSettings) {
				long time = System.currentTimeMillis();
				if (isUpdateNeeded() || time - cachedLeftTime > 1000) {
					cachedLeftTime = time;
					Intent batteryIntent = app.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
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
			}
		};
		batteryControl.setText(null, null);
		batteryControl.setIcons(battery, batteryN);
		return batteryControl;
	}

	public TextInfoWidget createMaxSpeedControl(@NonNull MapActivity mapActivity) {
		MapViewTrackingUtilities trackingUtilities = mapActivity.getMapViewTrackingUtilities();
		TextInfoWidget speedControl = new TextInfoWidget(mapActivity) {
			private float cachedSpeed = 0;

			@Override
			public void updateInfo(@Nullable DrawSettings drawSettings) {
				float mx = 0;
				if ((!routingHelper.isFollowingMode()
						|| routingHelper.isDeviatedFromRoute()
						|| (routingHelper.getCurrentGPXRoute() != null && !routingHelper.isCurrentGPXRouteV2()))
						&& trackingUtilities.isMapLinkedToLocation()) {
					RouteDataObject ro = locationProvider.getLastKnownRouteSegment();
					if (ro != null) {
						mx = ro.getMaximumSpeed(ro.bearingVsRouteDirection(locationProvider.getLastKnownLocation()));
					}
				} else {
					mx = routingHelper.getCurrentMaxSpeed();
				}
				if (isUpdateNeeded() || cachedSpeed != mx) {
					cachedSpeed = mx;
					if (cachedSpeed == 0) {
						setText(null, null);
					} else if (cachedSpeed == RouteDataObject.NONE_MAX_SPEED) {
						setText(getString(R.string.max_speed_none), "");
					} else {
						String ds = OsmAndFormatter.getFormattedSpeed(cachedSpeed, app);
						int ls = ds.lastIndexOf(' ');
						if (ls == -1) {
							setText(ds, null);
						} else {
							setText(ds.substring(0, ls), ds.substring(ls + 1));
						}
					}
				}
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

	public TextInfoWidget createSpeedControl(@NonNull MapActivity mapActivity) {
		TextInfoWidget speedControl = new TextInfoWidget(mapActivity) {
			private float cachedSpeed = 0;

			@Override
			public void updateInfo(@Nullable DrawSettings drawSettings) {
				Location loc = locationProvider.getLastKnownLocation();
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
					}
				} else if (cachedSpeed != 0) {
					cachedSpeed = 0;
					setText(null, null);
				}
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

	public TextInfoWidget createDistanceControl(@NonNull MapActivity mapActivity) {
		DistanceToPointWidget distanceControl = new DistanceToPointWidget(mapActivity, R.drawable.widget_target_day,
				R.drawable.widget_target_night) {
			@Override
			public LatLon getPointToNavigate() {
				TargetPoint p = mapActivity.getPointToNavigate();
				return p == null ? null : p.point;
			}

			@Override
			public int getDistance() {
				return routingHelper.isRouteCalculated()
						? routingHelper.getLeftDistance()
						: super.getDistance();
			}
		};
		return distanceControl;
	}
	
	public TextInfoWidget createIntermediateDistanceControl(@NonNull MapActivity mapActivity) {
		TargetPointsHelper targets = app.getTargetPointsHelper();
		DistanceToPointWidget distanceControl = new DistanceToPointWidget(mapActivity, R.drawable.widget_intermediate_day,
				R.drawable.widget_intermediate_night) {

			@Override
			protected void click(OsmandMapTileView view) {
				if (targets.getIntermediatePoints().size() > 1) {
					mapActivity.getMapActions().openIntermediatePointsDialog();
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
				return getPointToNavigate() != null && routingHelper.isRouteCalculated()
						? routingHelper.getLeftDistanceNextIntermediate()
						: super.getDistance();
			}
		};
		return distanceControl;
	}

	public TextInfoWidget createBearingControl(@NonNull MapActivity mapActivity) {
		int bearingResId = R.drawable.widget_bearing_day;
		int bearingNightResId = R.drawable.widget_bearing_night;
		int relativeBearingResId = R.drawable.widget_relative_bearing_day;
		int relativeBearingNightResId = R.drawable.widget_relative_bearing_night;
		OsmandPreference<Boolean> showRelativeBearing = settings.SHOW_RELATIVE_BEARING_OTHERWISE_REGULAR_BEARING;

		TextInfoWidget bearingControl = new TextInfoWidget(mapActivity) {
			private int cachedDegrees;
			private float MIN_SPEED_FOR_HEADING = 1f;

			private LatLon getNextTargetPoint() {
				List<TargetPoint> points = app.getTargetPointsHelper().getIntermediatePointsWithTarget();
				return points.isEmpty() ? null : points.get(0).point;
			}

			@Override
			public void updateInfo(DrawSettings drawSettings) {
				boolean relative = showRelativeBearing.get();
				boolean modeChanged = setIcons(relative ? relativeBearingResId : bearingResId, relative ? relativeBearingNightResId : bearingNightResId);
				setContentTitle(relative ? R.string.map_widget_bearing : R.string.map_widget_magnetic_bearing);
				int b = getBearing(relative);
				if (isUpdateNeeded() || degreesChanged(cachedDegrees, b) || modeChanged) {
					cachedDegrees = b;
					if (b != -1000) {
						setText(OsmAndFormatter.getFormattedAzimuth(b, app) + (relative ? "" : " M"), null);
					} else {
						setText(null, null);
					}
				}
			}

			@Override
			public boolean isAngularUnitsDepended() {
				return true;
			}

			public int getBearing(boolean relative) {
				int d = -1000;
				Location myLocation = locationProvider.getLastKnownLocation();
				LatLon l = getNextTargetPoint();
				if (l == null) {
					List<MapMarker> markers = app.getMapMarkersHelper().getMapMarkers();
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
						Float heading = locationProvider.getHeading();
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

		bearingControl.setOnClickListener(v -> {
			showRelativeBearing.set(!showRelativeBearing.get());
			mapActivity.refreshMap();
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