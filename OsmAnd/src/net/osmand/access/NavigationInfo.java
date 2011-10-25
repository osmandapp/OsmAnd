package net.osmand.access;

import java.lang.Math;
import java.util.ArrayList;
import java.util.List;

import net.osmand.osm.LatLon;
import net.osmand.plus.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.location.Location;

public class NavigationInfo {

    private final Activity activity;
    private Location currentLocation;

    public NavigationInfo(final Activity activity) {
        this.activity = activity;
        currentLocation = null;
    }

    private String getDirectionString(float bearing) {
        final int[] direction = new int[] {
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
        return activity.getString(direction[(int)Math.round((bearing + 360.0) / 22.5) % 16]);
    }

    public void setLocation(Location location) {
        currentLocation = location;
    }

    // Get distance and direction string for specified point
    public String getDirection(Location point) {
        if ((currentLocation != null) && (point != null)) {
            double distance = currentLocation.distanceTo(point);
            if (distance <1000.0)
                distance = Math.rint(distance * 100.0) / 100.0;
            else
                distance = Math.rint(distance / 10.0) * 10.0;
            String result = new String(String.valueOf((distance < 1000.0) ? distance : (distance / 1000.0)));
            result += " " + activity.getString((distance < 1000.0) ? R.string.meters : R.string.kilometers); //$NON-NLS-1$
            result += " " + activity.getString(R.string.towards) + " "; //$NON-NLS-1$ //$NON-NLS-2$
            if (currentLocation.hasBearing()) {
                // clockwise direction
                int clockwise = (int)Math.round((currentLocation.bearingTo(point) - currentLocation.getBearing() + 360.0) / 30.0) % 12;
                result += String.valueOf((clockwise != 0) ? clockwise : 12) + " " + activity.getString(R.string.oclock); //$NON-NLS-1$
            } else {
                // compass direction
                result += getDirectionString(currentLocation.bearingTo(point));
            }
            return result;
        }
        return null;
    }

    public String getDirection(final LatLon point) {
        if (point != null) {
            Location destination = new Location("map"); //$NON-NLS-1$
            destination.setLatitude(point.getLatitude());
            destination.setLongitude(point.getLongitude());
            return getDirection(destination);
        }
        return null;
    }

    // Get current travelling speed and direction
    public String getSpeed() {
        if ((currentLocation != null) && currentLocation.hasSpeed()) {
            double speed = Math.rint(currentLocation.getSpeed() * 360.0) / 100.0;
            String result = new String(String.valueOf(speed) + " " + activity.getString(R.string.kilometers_per_hour)); //$NON-NLS-1$
            if (currentLocation.hasBearing())
                result += " " + getDirectionString(currentLocation.getBearing()); //$NON-NLS-1$
            return result;
        }
        return null;
    }

    // Get positioning accuracy and provider information if available
    public String getAccuracy() {
        String provider = currentLocation.getProvider();
        String result = null;
        if ((currentLocation != null) && currentLocation.hasAccuracy()) {
            double accuracy = Math.rint(currentLocation.getAccuracy() * 100.0) / 100.0;
            result = new String(activity.getString(R.string.accuracy) + " " + String.valueOf(accuracy) + " " + activity.getString(R.string.meters)); //$NON-NLS-1$ //$NON-NLS-2$
        }
        if (result != null)
            result += " (" + provider + ")"; //$NON-NLS-1$ //$NON-NLS-2$
        else
            result = provider;
        return result;
    }

    // Get altitude information string
    public String getAltitude() {
        if ((currentLocation != null) && currentLocation.hasAltitude()) {
            double altitude = Math.rint(currentLocation.getAltitude() * 100.0) / 100.0;
            return new String(activity.getString(R.string.altitude) + " " + String.valueOf(altitude) + " " + activity.getString(R.string.meters)); //$NON-NLS-1$ //$NON-NLS-2$
        }
        return null;
    }


    // Show all available info
    public void show(final LatLon point) {
        List<String> attributes = new ArrayList<String>();
        String item;

        item = getDirection(point);
        if (item != null)
            attributes.add(item);
        item = getSpeed();
        if (item != null)
            attributes.add(item);
        item = getAccuracy();
        if (item != null)
            attributes.add(item);
        item = getAltitude();
        if (item != null)
            attributes.add(item);
        if (attributes.isEmpty())
            attributes.add(activity.getString(R.string.no_info));

        AlertDialog.Builder info = new AlertDialog.Builder(activity);
        info.setPositiveButton(R.string.default_buttons_ok, new DialogInterface.OnClickListener() {
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
