package net.osmand.access;


import android.content.Context;
import android.content.DialogInterface;
import android.os.SystemClock;
import android.support.v7.app.AlertDialog;

import net.osmand.Location;
import net.osmand.data.LatLon;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.TargetPointsHelper.TargetPoint;
import net.osmand.plus.access.RelativeDirectionStyle;

import java.util.ArrayList;
import java.util.List;

public class NavigationInfo {

	private static final long MIN_NOTIFICATION_PERIOD = 10000;
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
		public RelativeDirection(final Location point) {
			style = settings.DIRECTION_STYLE.get();
			value = directionTo(point, currentLocation.getBearing());
		}

		// The first argument must be not null as well as the currentLocation.
		public RelativeDirection(final Location point, float heading) {
			style = settings.DIRECTION_STYLE.get();
			value = directionTo(point, heading);
		}

		public void clear() {
			value = UNKNOWN;
		}

		// The first argument must be not null as well as the currentLocation.
		public boolean update(final Location point, float heading) {
			boolean result = false;
			final RelativeDirectionStyle newStyle = settings.DIRECTION_STYLE.get();
			if (style != newStyle) {
				style = newStyle;
				result = true;
			}
			final int newValue = directionTo(point, heading);
			if (value != newValue) {
				value = newValue;
				result = true;
			}
			return result;
		}

		// The argument must be not null as well as the currentLocation
		// and currentLocation must have bearing.
		public boolean update(final Location point) {
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

		// The first argument must be not null as well as the currentLocation.
		private int directionTo(final Location point, float heading) {
			final float bearing = currentLocation.bearingTo(point) - heading;
			final int nSectors = (style == RelativeDirectionStyle.CLOCKWISE) ? 12 : direction.length;
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

	private final OsmandApplication context;
	private final OsmandSettings settings;
	private Location currentLocation;
	private RelativeDirection lastDirection;
	private long lastNotificationTime;
	private volatile boolean autoAnnounce;
	private OsmandApplication app;

	public NavigationInfo(OsmandApplication app) {
		this.app = app;
		this.context = app;
		settings = this.context.getSettings();
		currentLocation = null;
		lastDirection = new RelativeDirection();
		lastNotificationTime = SystemClock.uptimeMillis();
		autoAnnounce = false;
	}

	private String getString(int id) {
		return context.getString(id);
	}

	// The argument must be not null as well as the currentLocation
	private String distanceString(final Location point) {
		return OsmAndFormatter.getFormattedDistance(currentLocation.distanceTo(point), context);
	}

	// The argument must be not null as well as the currentLocation
	private String absoluteDirectionString(float bearing) {
		int direction = Math.round(Math.abs(bearing) * (float) cardinal.length / FULL_CIRCLE) % cardinal.length;
		if ((bearing < 0) && (direction != 0))
			direction = cardinal.length - direction;
		return getString(cardinal[direction]);
	}

	// Get distance and direction string for specified point
	public synchronized String getDirectionString(final LatLon apoint, Float heading) {
		if ((currentLocation != null) && (apoint != null)) {
			Location point = new Location("");
			point.setLatitude(apoint.getLatitude());
			point.setLongitude(apoint.getLongitude());
			RelativeDirection direction = null;
			String result = distanceString(point);
			result += " "; //$NON-NLS-1$
			if (currentLocation.hasBearing())
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
			String result = OsmAndFormatter.getFormattedSpeed(currentLocation.getSpeed(), context);
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
				result = getString(R.string.accuracy) + " " + OsmAndFormatter.getFormattedDistance(currentLocation.getAccuracy(), context); //$NON-NLS-1$
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
					+ " " + OsmAndFormatter.getFormattedDistance((float) currentLocation.getAltitude(), context); //$NON-NLS-1$
		return null;
	}

	public synchronized void setLocation(Location location) {
		currentLocation = location;
		if (autoAnnounce && context.accessibilityEnabled()) {
			final TargetPoint point = app.getTargetPointsHelper().getPointToNavigate();
			if (point != null) {
				if ((currentLocation != null) && currentLocation.hasBearing()) {
					final long now = SystemClock.uptimeMillis();
					if ((now - lastNotificationTime) >= MIN_NOTIFICATION_PERIOD) {
						Location destination = new Location("map"); //$NON-NLS-1$
						destination.setLatitude(point.getLatitude());
						destination.setLongitude(point.getLongitude());
						if (lastDirection.update(destination)) {
							final String notification = distanceString(destination) + " " + lastDirection.getString(); //$NON-NLS-1$
							lastNotificationTime = now;
							app.runInUIThread(new Runnable() {
								@Override
								public void run() {
									context.showToastMessage(notification);
								}
							});
						}
					}
				} else {
					lastDirection.clear();
				}
			}
		}
	}

	// Show all available info
	public void show(final TargetPoint point, Float heading, Context ctx) {
		final List<String> attributes = new ArrayList<String>();
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
		info.setItems(attributes.toArray(new String[attributes.size()]), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
			}
		});
		info.show();
	}

}
