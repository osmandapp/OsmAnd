package net.osmand.plus.dashboard;

import net.osmand.Location;
import net.osmand.data.LatLon;
import net.osmand.plus.views.DirectionDrawable;
import net.osmand.util.MapUtils;
import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.view.WindowManager;
import android.widget.ImageView;

/**
 * Created by Denis
 * on 26.01.2015.
 */
public abstract class DashLocationFragment extends DashBaseFragment {

	private static final int ORIENTATION_0 = 0;
	private static final int ORIENTATION_90 = 3;
	private static final int ORIENTATION_270 = 1;
	private static final int ORIENTATION_180 = 2;

	protected Float heading = null;

	protected LatLon loc = null;

	public void updateLocation(Location location) {
		//This is used as origin for both Fav-list and direction arrows
		if (getMyApplication().getSettings().getLastKnownMapLocation() != null) {
			loc = getMyApplication().getSettings().getLastKnownMapLocation();
		} else {
			loc = new LatLon(0f, 0f);
		}
	}

	public static void updateArrow(Activity ctx, LatLon currentLocation, LatLon pointLocation,
								   ImageView direction, int size, int resourceId, Float heading) {
		float[] mes = new float[2];
		Location.distanceBetween(pointLocation.getLatitude(), pointLocation.getLongitude(), currentLocation.getLatitude(), currentLocation.getLongitude(), mes);
		DirectionDrawable draw = new DirectionDrawable(ctx, size, size, resourceId);
		Float h = heading;
		float a = h != null ? h : 0;

		//Hardy: getRotation() is the correction if device's screen orientation != the default display's standard orientation
		int screenOrientation = 0;
		screenOrientation = ((WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRotation();
		switch (screenOrientation)
		{
			case ORIENTATION_0:   // Device default (normally portrait)
				screenOrientation = 0;
				break;
			case ORIENTATION_90:  // Landscape right
				screenOrientation = 90;
				break;
			case ORIENTATION_270: // Landscape left
				screenOrientation = 270;
				break;
			case ORIENTATION_180: // Upside down
				screenOrientation = 180;
				break;
		}

		//Looks like screenOrientation correction must not be applied for devices without compass?
		Sensor compass  = ((SensorManager) ctx.getSystemService(Context.SENSOR_SERVICE)).getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
		if (compass == null) {
			screenOrientation = 0;
		}

		draw.setAngle(mes[1] - a + 180 + screenOrientation);

		direction.setImageDrawable(draw);
	}
	
	public boolean updateCompassValue(float value) {
		//heading = value;
		//updateArrows();
		//99 in next line used to one-time initalize arrows (with reference vs. fixed-north direction) on non-compass devices
		float lastHeading = heading != null ? heading : 99;
		heading = value;
		if (heading != null && Math.abs(MapUtils.degreesDiff(lastHeading, heading)) > 5) {
			return true;
		} else {
			heading = lastHeading;
		}
		return false;
	}
}
