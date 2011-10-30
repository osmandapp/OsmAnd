package net.osmand.access;

import java.lang.Math;
import java.util.ArrayList;
import java.util.List;

import net.osmand.access.AccessibleToast;
import net.osmand.access.RelativeDirectionStyle;
import net.osmand.osm.LatLon;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.location.Location;
import android.os.SystemClock;
import android.widget.Toast;

public class NavigationInfo {

    private static final long MIN_NOTIFICATION_PERIOD = 10000;
    private static final float FULL_CIRCLE = 360.0f;

    private class RelativeDirection {

        private static final int UNKNOWN = -1;

        private final int[] direction = {
            R.string.front,
            R.string.front_right,
            R.string.right,
            R.string.back_right,
            R.string.back,
            R.string.back_left,
            R.string.left,
            R.string.front_left
        };

        private RelativeDirectionStyle style;
        private int value;

        public RelativeDirection() {
            style = OsmandSettings.getOsmandSettings(context).DIRECTION_STYLE.get();
            clear();
        }

        // The argument must be not null as well as the currentLocation
        public RelativeDirection(final Location point) {
            style = OsmandSettings.getOsmandSettings(context).DIRECTION_STYLE.get();
            value = directionTo(point);
        }

        public void clear() {
            value = UNKNOWN;
        }

        // The argument must be not null as well as the currentLocation
        public boolean update(final Location point) {
            boolean result = false;
            final RelativeDirectionStyle newStyle = OsmandSettings.getOsmandSettings(context).DIRECTION_STYLE.get();
            if (style != newStyle) {
                style = newStyle;
                result = true;
            }
            final int newValue = directionTo(point);
            if (value != newValue) {
                value = newValue;
                result = true;
            }
            return result;
        }

        public String getString() {
            if (value < 0) // unknown direction
                return null;
            if (style == RelativeDirectionStyle.CLOCKWISE) {
                String result = NavigationInfo.this.getString(R.string.towards);
                result += " " + String.valueOf((value != 0) ? value : 12); //$NON-NLS-1$
                result += " " + NavigationInfo.this.getString(R.string.oclock); //$NON-NLS-1$
                return result;
            } else {
                return NavigationInfo.this.getString(direction[value]);
            }
        }

        // The argument must be not null as well as the currentLocation
        private int directionTo(final Location point) {
            final float bearing = currentLocation.bearingTo(point) - currentLocation.getBearing();
            final int nSectors = (style == RelativeDirectionStyle.CLOCKWISE) ? 12 : direction.length;
            int sector = (int)Math.round(Math.abs(bearing) * (float)nSectors / FULL_CIRCLE) % nSectors;
            if ((bearing < 0) && (sector != 0))
                sector = nSectors - sector;
            return sector;
        }

    }


    private final int[] cardinal = {
        R.string.north,
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
        R.string.north_north_west
    };

    private final Context context;
    private Location currentLocation;
    private RelativeDirection lastDirection;
    private long lastNotificationTime;
    private volatile boolean autoAnnounce;


    public NavigationInfo(final Context context) {
        this.context = context;
        currentLocation = null;
        lastDirection = new RelativeDirection();
        lastNotificationTime = SystemClock.uptimeMillis();
        autoAnnounce = false;
    }


    private String getString(int id) {
        return context.getResources().getString(id);
    }

    // The argument must be not null as well as the currentLocation
    private String distanceString(final Location point) {
        double distance = currentLocation.distanceTo(point);
        if (distance <1000.0)
            distance = Math.rint(distance * 100.0) / 100.0;
        else
            distance = Math.rint(distance / 10.0) * 10.0;
        String result = String.valueOf((distance < 1000.0) ? distance : (distance / 1000.0));
        result += " " + getString((distance < 1000.0) ? R.string.meters : R.string.kilometers); //$NON-NLS-1$
        return result;
    }

    // The argument must be not null as well as the currentLocation
    private String absoluteDirectionString(float bearing) {
        int direction = (int)Math.round(Math.abs(bearing) * (float)cardinal.length / FULL_CIRCLE) % cardinal.length;
        if ((bearing < 0) && (direction != 0))
            direction = cardinal.length - direction;
        return getString(cardinal[direction]);
    }


    // Get distance and direction string for specified point
    public synchronized String getDirectionString(final Location point) {
        if ((currentLocation != null) && (point != null)) {
            String result = distanceString(point);
            result += " "; //$NON-NLS-1$
            if (currentLocation.hasBearing()) {
                // relative direction
                RelativeDirection direction = new RelativeDirection(point);
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

    public synchronized String getDirectionString(final LatLon point) {
        if (point != null) {
            Location destination = new Location("map"); //$NON-NLS-1$
            destination.setLatitude(point.getLatitude());
            destination.setLongitude(point.getLongitude());
            return getDirectionString(destination);
        }
        return null;
    }

    // Get current travelling speed and direction
    public synchronized String getSpeedString() {
        if ((currentLocation != null) && currentLocation.hasSpeed()) {
            double speed = Math.rint(currentLocation.getSpeed() * 360.0) / 100.0;
            String result = String.valueOf(speed) + " " + getString(R.string.kilometers_per_hour); //$NON-NLS-1$
            if (currentLocation.hasBearing())
                result += " " + absoluteDirectionString(currentLocation.getBearing()); //$NON-NLS-1$
            return result;
        }
        return null;
    }

    // Get positioning accuracy and provider information if available
    public synchronized String getAccuracyString() {
        String provider = currentLocation.getProvider();
        String result = null;
        if ((currentLocation != null) && currentLocation.hasAccuracy()) {
            double accuracy = Math.rint(currentLocation.getAccuracy() * 100.0) / 100.0;
            result = getString(R.string.accuracy) + " " + String.valueOf(accuracy) + " " + getString(R.string.meters); //$NON-NLS-1$ //$NON-NLS-2$
        }
        if (result != null)
            result += " (" + provider + ")"; //$NON-NLS-1$ //$NON-NLS-2$
        else
            result = provider;
        return result;
    }

    // Get altitude information string
    public synchronized String getAltitudeString() {
        if ((currentLocation != null) && currentLocation.hasAltitude()) {
            double altitude = Math.rint(currentLocation.getAltitude() * 100.0) / 100.0;
            return getString(R.string.altitude) + " " + String.valueOf(altitude) + " " + getString(R.string.meters); //$NON-NLS-1$ //$NON-NLS-2$
        }
        return null;
    }


    public synchronized void setLocation(Location location) {
        currentLocation = location;
        if (autoAnnounce) {
            final LatLon point = OsmandSettings.getOsmandSettings(context).getPointToNavigate();
            if (point != null) {
                if ((currentLocation != null) && currentLocation.hasBearing()) {
                    final long now = SystemClock.uptimeMillis();
                    if ((now - lastNotificationTime) >= MIN_NOTIFICATION_PERIOD) {
                        Location destination = new Location("map"); //$NON-NLS-1$
                        destination.setLatitude(point.getLatitude());
                        destination.setLongitude(point.getLongitude());
                        if (lastDirection.update(destination)) {
                            String notification = distanceString(destination);
                            notification += " " + lastDirection.getString(); //$NON-NLS-1$
                            lastNotificationTime = now;
                            AccessibleToast.makeText(context, notification, Toast.LENGTH_LONG).show();
                        }
                    }
                } else {
                    lastDirection.clear();
                }
            }
        }
    }


    // Show all available info
    public void show(final LatLon point) {
        List<String> attributes = new ArrayList<String>();
        String item;

        item = getDirectionString(point);
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

        AlertDialog.Builder info = new AlertDialog.Builder(context);
        if (point != null)
            info.setPositiveButton(autoAnnounce ? R.string.auto_announce_off : R.string.auto_announce_on,
                                   new DialogInterface.OnClickListener() {
                                       @Override
                                       public void onClick(DialogInterface dialog, int id) {
                                           autoAnnounce = !autoAnnounce;
                                           dialog.cancel();
                                       }
                                   });
        info.setNegativeButton(R.string.close, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                    dialog.cancel();
                }
            });
        info.setItems(attributes.toArray(new String[attributes.size()]),
                      new DialogInterface.OnClickListener() {
                          @Override
                          public void onClick(DialogInterface dialog, int which) {
                          }
                      });
        info.show();
    }

}
