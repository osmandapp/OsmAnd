package net.osmand.plus.plugins.accessibility;


import android.content.Context;
import android.content.DialogInterface;
import android.os.SystemClock;
import android.os.Vibrator;

import androidx.appcompat.app.AlertDialog;

import net.osmand.Location;
import net.osmand.data.LatLon;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.OsmAndLocationProvider.OsmAndCompassListener;
import net.osmand.plus.OsmAndLocationProvider.OsmAndLocationListener;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.helpers.TargetPointsHelper.TargetPoint;
import net.osmand.plus.base.MapViewTrackingUtilities;
import net.osmand.plus.routing.RouteCalculationResult.NextDirectionInfo;
import net.osmand.plus.routing.RoutingHelper;

import java.util.ArrayList;
import java.util.List;

public class NavigationInfo implements OsmAndCompassListener, OsmAndLocationListener {

	private static final float FULL_CIRCLE = 360.0f;

	private class RelativeDirection {

		private static final int UNKNOWN = -1;

		private final int[] direction = {R.string.front,
				R.string.front_right,
				R.string.right,
				R.string.back_right,
				R.string.back,
				R.string.back_left,
				R.string.left,
				R.string.front_left};

		private RelativeDirectionStyle style;
		private int value;

		public RelativeDirection() {
			style = settings.DIRECTION_STYLE.get();
			clear();
		}

		// The argument must be not null as well as the currentLocation
		// and currentLocation must have bearing.
		public RelativeDirection(Location point) {
			style = settings.DIRECTION_STYLE.get();
			value = directionTo(point, currentLocation.getBearing());
		}

		// The first argument must be not null as well as the currentLocation.
		public RelativeDirection(Location point, float heading) {
			style = settings.DIRECTION_STYLE.get();
			value = directionTo(point, heading);
		}

		public void clear() {
			value = UNKNOWN;
		}

		// The first argument must be not null as well as the currentLocation.
		public boolean update(Location point, float heading) {
			boolean result = false;
			RelativeDirectionStyle newStyle = settings.DIRECTION_STYLE.get();
			if (style != newStyle) {
				style = newStyle;
				result = true;
			}
			int newValue = directionTo(point, heading);
			if (value != newValue) {
				value = newValue;
				result = true;
			}
			return result;
		}

		// The argument must be not null as well as the currentLocation
		// and currentLocation must have bearing.
		public boolean update(Location point) {
			return update(point, currentLocation.getBearing());
		}

		public String getString() {
			if (value < 0) // unknown direction
				return null;
			if (style == RelativeDirectionStyle.CLOCKWISE) {
				String result = NavigationInfo.this.getString(R.string.towards);
				result += " " + ((value != 0) ? value : 12); //$NON-NLS-1$
				result += " " + NavigationInfo.this.getString(R.string.oclock); //$NON-NLS-1$
				return result;
			} else {
				return NavigationInfo.this.getString(direction[value]);
			}
		}

		public Integer getInclination() {
			if (value < 0) // unknown direction
				return null;
			int nSectors = (style == RelativeDirectionStyle.CLOCKWISE) ? 12 : direction.length;
			int halfRound = nSectors / 2;
			if (value == halfRound) // opposite direction
				return null;
			if (value > halfRound)
				return value - nSectors;
			return value;
		}

		protected int getValue() {
			return value;
		}

		// The first argument must be not null as well as the currentLocation.
		private int directionTo(Location point, float heading) {
			float bearing = currentLocation.bearingTo(point) - heading;
			int nSectors = (style == RelativeDirectionStyle.CLOCKWISE) ? 12 : direction.length;
			int sector = Math.round(Math.abs(bearing) * (float) nSectors / FULL_CIRCLE) % nSectors;
			if ((bearing < 0) && (sector != 0))
				sector = nSectors - sector;
			return sector;
		}

	}

	private final int[] cardinal = {R.string.north,
			R.string.north_north_east,
			R.string.north_east,
			R.string.east_north_east,
			R.string.east,
			R.string.east_south_east,
			R.string.south_east,
			R.string.south_south_east,
			R.string.south,
			R.string.south_south_west,
			R.string.south_west,
			R.string.west_south_west,
			R.string.west,
			R.string.west_north_west,
			R.string.north_west,
			R.string.north_north_west};

	private final long[] HAPTIC_INCLINATION_LEFT = { 0, 60 };
	private final long[] HAPTIC_INCLINATION_RIGHT = { 0, 20, 80, 20 };

	private final OsmandApplication app;
	private final OsmandSettings settings;
	private Location currentLocation;
	private final RelativeDirection lastDirection;
	private long lastNotificationTime;
	private volatile boolean autoAnnounce;
	private volatile boolean targetDirectionFlag;

	public NavigationInfo(OsmandApplication app) {
		this.app = app;
		settings = app.getSettings();
		currentLocation = null;
		lastDirection = new RelativeDirection();
		lastNotificationTime = SystemClock.uptimeMillis();
		autoAnnounce = false;
		targetDirectionFlag = false;
	}

	private String getString(int id) {
		return app.getString(id);
	}

	// The argument must be not null as well as the currentLocation
	private String distanceString(Location point) {
		return OsmAndFormatter.getFormattedDistance(currentLocation.distanceTo(point), app);
	}

	// The argument must be not null as well as the currentLocation
	private String absoluteDirectionString(float bearing) {
		int direction = Math.round(Math.abs(bearing) * (float) cardinal.length / FULL_CIRCLE) % cardinal.length;
		if ((bearing < 0) && (direction != 0))
			direction = cardinal.length - direction;
		return getString(cardinal[direction]);
	}

	// Get distance and direction string for specified point
	public synchronized String getDirectionString(LatLon apoint, Float heading) {
		if ((currentLocation != null) && (apoint != null)) {
			Location point = new Location("");
			point.setLatitude(apoint.getLatitude());
			point.setLongitude(apoint.getLongitude());
			RelativeDirection direction = null;
			String result = distanceString(point);
			result += " "; //$NON-NLS-1$
			if (currentLocation.hasBearing() && !MapViewTrackingUtilities.isSmallSpeedForCompass(currentLocation))
				direction = new RelativeDirection(point);
			else if (heading != null)
				direction = new RelativeDirection(point, heading);
			if (direction != null) {
				// relative direction
				result += direction.getString();
			} else {
				// absolute direction
				result += getString(R.string.towards) + " "; //$NON-NLS-1$
				result += absoluteDirectionString(currentLocation.bearingTo(point));
			}
			return result;
		}
		return null;
	}

	// Get current travelling speed and direction
	public synchronized String getSpeedString() {
		if ((currentLocation != null) && currentLocation.hasSpeed()) {
			String result = OsmAndFormatter.getFormattedSpeed(currentLocation.getSpeed(), app);
			if (currentLocation.hasBearing())
				result += " " + absoluteDirectionString(currentLocation.getBearing()); //$NON-NLS-1$
			return result;
		}
		return null;
	}

	// Get positioning accuracy and provider information if available
	public synchronized String getAccuracyString() {
		String result = null;
		if (currentLocation != null) {
			String provider = currentLocation.getProvider();
			if (currentLocation.hasAccuracy())
				result = getString(R.string.accuracy) + " " + OsmAndFormatter.getFormattedDistance(currentLocation.getAccuracy(), app); //$NON-NLS-1$
			if (result != null)
				result += " (" + provider + ")"; //$NON-NLS-1$ //$NON-NLS-2$
			else
				result = provider;
		}
		return result;
	}

	// Get altitude information string
	public synchronized String getAltitudeString() {
		if ((currentLocation != null) && currentLocation.hasAltitude())
			return getString(R.string.altitude)
					+ " " + OsmAndFormatter.getFormattedDistance((float) currentLocation.getAltitude(), app); //$NON-NLS-1$
		return null;
	}

	@Override
	public synchronized void updateLocation(Location location) {
		currentLocation = location;
		if (autoAnnounce && app.accessibilityEnabled()) {
			TargetPoint point = app.getTargetPointsHelper().getPointToNavigate();
			if (point != null) {
				if ((currentLocation != null) && currentLocation.hasBearing() && !MapViewTrackingUtilities.isSmallSpeedForCompass(currentLocation)) {
					long now = SystemClock.uptimeMillis();
					if ((now - lastNotificationTime) >= settings.ACCESSIBILITY_AUTOANNOUNCE_PERIOD.get()) {
						Location destination = new Location("map"); //$NON-NLS-1$
						destination.setLatitude(point.getLatitude());
						destination.setLongitude(point.getLongitude());
						if (lastDirection.update(destination) || !settings.ACCESSIBILITY_SMART_AUTOANNOUNCE.get()) {
							String notification = distanceString(destination) + " " + lastDirection.getString(); //$NON-NLS-1$
							lastNotificationTime = now;
							app.runInUIThread(() -> app.showToastMessage(notification));
						}
					}
				} else {
					lastDirection.clear();
				}
			}
		}
	}

	public synchronized void updateTargetDirection(Location point, float heading) {
		if ((currentLocation != null) && (point != null)) {
			RelativeDirection direction = new RelativeDirection(point, heading);
			Integer inclination = direction.getInclination();
			if (targetDirectionFlag && ((inclination == null) || (inclination != 0))) {
				targetDirectionFlag = false;
				if (settings.DIRECTION_AUDIO_FEEDBACK.get()) {
					AccessibilityPlugin accessibilityPlugin = PluginsHelper.getActivePlugin(AccessibilityPlugin.class);
					if (accessibilityPlugin != null) {
						if (inclination == null) {
							accessibilityPlugin.playSoundIcon(AccessibilityPlugin.DIRECTION_NOTIFICATION);
						} else if (inclination > 0) {
							accessibilityPlugin.playSoundIcon(AccessibilityPlugin.INCLINATION_LEFT);
						} else {
							accessibilityPlugin.playSoundIcon(AccessibilityPlugin.INCLINATION_RIGHT);
						}
					}
				}
				if (settings.DIRECTION_HAPTIC_FEEDBACK.get()) {
					Vibrator haptic = (Vibrator)app.getSystemService(Context.VIBRATOR_SERVICE);
					if ((haptic != null) && haptic.hasVibrator()) {
						if (inclination == null) {
							haptic.vibrate(200);
						} else if (inclination > 0) {
							haptic.vibrate(HAPTIC_INCLINATION_LEFT, -1);
						} else {
							haptic.vibrate(HAPTIC_INCLINATION_RIGHT, -1);
						}
					}
				}
			} else if ((!targetDirectionFlag) && (direction.getValue() == 0)) {
				targetDirectionFlag = true;
			}
		}
	}

	public synchronized void updateTargetDirection(LatLon point, float heading) {
		if (point != null) {
			Location destination = new Location("map"); //$NON-NLS-1$
			destination.setLatitude(point.getLatitude());
			destination.setLongitude(point.getLongitude());
			updateTargetDirection(destination, heading);
		}
	}

	@Override
	public synchronized void updateCompassValue(float heading) {
		RoutingHelper router = app.getRoutingHelper();
		if (router.isFollowingMode() && router.isRouteCalculated()) {
			synchronized (router) {
				NextDirectionInfo nextDirection = router.getNextRouteDirectionInfo(new NextDirectionInfo(), true);
				if (nextDirection != null) {
					updateTargetDirection(router.getLocationFromRouteDirection(nextDirection.directionInfo), heading);
				}
			}
		} else {
			TargetPoint target = app.getTargetPointsHelper().getPointToNavigate();
			updateTargetDirection((target != null) ? target.point : null, heading);
		}
	}

	// Show all available info
	public void show(TargetPoint point, Float heading, Context ctx) {
		List<String> attributes = new ArrayList<String>();
		String item;

		item = getDirectionString(point == null ? null : point.point, heading);
		if (item != null)
			attributes.add(item);
		item = getSpeedString();
		if (item != null)
			attributes.add(item);
		item = getAccuracyString();
		if (item != null)
			attributes.add(item);
		item = getAltitudeString();
		if (item != null)
			attributes.add(item);
		if (attributes.isEmpty())
			attributes.add(getString(R.string.no_info));

		AlertDialog.Builder info = new AlertDialog.Builder(ctx);
		if (point != null)
			info.setPositiveButton(autoAnnounce ? R.string.auto_announce_off : R.string.auto_announce_on,
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int id) {
							autoAnnounce = !autoAnnounce;
							dialog.cancel();
						}
					});
		info.setNegativeButton(R.string.shared_string_close, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int id) {
				dialog.cancel();
			}
		});
		info.setItems(attributes.toArray(new String[0]), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
			}
		});
		info.show();
	}

}
