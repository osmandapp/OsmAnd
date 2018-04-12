package net.osmand.plus.dashboard;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import net.osmand.Location;
import net.osmand.data.LatLon;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.views.DirectionDrawable;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Denis
 * on 26.01.2015.
 */
@SuppressLint("ResourceAsColor")
public abstract class DashLocationFragment extends DashBaseFragment {

	private static final int ORIENTATION_0 = 0;
	private static final int ORIENTATION_90 = 3;
	private static final int ORIENTATION_270 = 1;
	private static final int ORIENTATION_180 = 2;
	protected List<DashLocationView> distances = new ArrayList<DashLocationFragment.DashLocationView>();
	private int screenOrientation;
	protected LatLon lastUpdatedLocation;

	public static class DashLocationView {
		public ImageView arrow;
		public TextView txt;
		public LatLon loc;
		public int arrowResId;
		public boolean paint = true;

		public DashLocationView(ImageView arrow, TextView txt, LatLon loc) {
			super();
			this.arrow = arrow;
			this.txt = txt;
			this.loc = loc;
		}


	}


	@Override
	public void onOpenDash() {
		//Hardy: getRotation() is the correction if device's screen orientation != the default display's standard orientation
		screenOrientation = getScreenOrientation(getActivity());
	}

	public static int getScreenOrientation(Activity a) {
		int screenOrientation = ((WindowManager) a.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRotation();
		switch (screenOrientation) {
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
		Sensor compass = ((SensorManager) a.getSystemService(Context.SENSOR_SERVICE)).getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
		if (compass == null) {
			screenOrientation = 0;
		}
		return screenOrientation;
	}

	public LatLon getDefaultLocation() {
		DashboardOnMap d = dashboard;
		if (d == null) {
			return null;
		}
		return d.getMapViewLocation();
	}

	public void updateAllWidgets() {
		DashboardOnMap d = dashboard;
		if (d == null) {
			return;
		}
		float head = d.getHeading();
		float mapRotation = d.getMapRotation();
		LatLon mw = d.getMapViewLocation();
		Location l = d.getMyLocation();
		boolean mapLinked = d.isMapLinkedToLocation() && l != null;
		LatLon myLoc = l == null ? null : new LatLon(l.getLatitude(), l.getLongitude());
		boolean useCenter = !mapLinked;
		LatLon loc = (useCenter ? mw : myLoc);
		float h = useCenter ? -mapRotation : head;
		lastUpdatedLocation = loc;
		for (DashLocationView lv : distances) {
			updateLocationView(useCenter, loc, h, lv.arrow, lv.arrowResId, lv.txt, lv.loc, screenOrientation,
					getMyApplication(), getActivity(), lv.paint);
		}
	}

	public static void updateLocationView(boolean useCenter, LatLon fromLoc, Float h,
										  ImageView arrow, int imgColor, TextView txt, int textColor, double toLat, double toLon,
										  int screenOrientation, OsmandApplication app, Context ctx) {
		updateLocationView(useCenter, fromLoc, h, arrow, 0, imgColor, txt, textColor, new LatLon(toLat, toLon), screenOrientation, app, ctx, true);
	}

	public static void updateLocationView(boolean useCenter, LatLon fromLoc, Float h,
										  ImageView arrow, TextView txt, double toLat, double toLon,
										  int screenOrientation, OsmandApplication app, Context ctx) {
		updateLocationView(useCenter, fromLoc, h, arrow, 0, txt, new LatLon(toLat, toLon), screenOrientation, app, ctx, true);
	}

	public static void updateLocationView(boolean useCenter, LatLon fromLoc, Float h,
										  ImageView arrow, int arrowResId, TextView txt, LatLon toLoc,
										  int screenOrientation, OsmandApplication app, Context ctx, boolean paint) {
		updateLocationView(useCenter, fromLoc, h, arrow, arrowResId, 0, txt, 0, toLoc, screenOrientation, app, ctx, paint);
	}

	public static void updateLocationView(boolean useCenter, LatLon fromLoc, Float h,
										  ImageView arrow, int arrowResId, int imgColor, TextView txt, LatLon toLoc,
										  int screenOrientation, OsmandApplication app, Context ctx, boolean paint) {
		updateLocationView(useCenter, fromLoc, h, arrow, arrowResId, imgColor, txt, 0, toLoc, screenOrientation, app, ctx, paint);
	}

	public static void updateLocationView(boolean useCenter, LatLon fromLoc, Float h,
										  ImageView arrow, int arrowResId, int imgColor, TextView txt, int textColor, LatLon toLoc,
										  int screenOrientation, OsmandApplication app, Context ctx, boolean paint) {
		float[] mes = new float[2];
		if (fromLoc != null && toLoc != null) {
			Location.distanceBetween(toLoc.getLatitude(), toLoc.getLongitude(), fromLoc.getLatitude(), fromLoc.getLongitude(), mes);
		}
		if (arrow != null) {
			boolean newImage = false;
			if (arrowResId == 0) {
				arrowResId = R.drawable.ic_direction_arrow;
			}
			DirectionDrawable dd;
			if(!(arrow.getDrawable() instanceof DirectionDrawable)) {
				newImage = true;
				dd = new DirectionDrawable(ctx, arrow.getWidth(), arrow.getHeight());
			} else {
				dd = (DirectionDrawable) arrow.getDrawable();
			}
			dd.setImage(arrowResId, imgColor == 0 ? useCenter ? R.color.color_distance : R.color.color_myloc_distance : imgColor);
			if (fromLoc == null || h == null || toLoc == null) {
				dd.setAngle(0);
			} else {
				dd.setAngle(mes[1] - h + 180 + screenOrientation);
			}
			if (newImage) {
				arrow.setImageDrawable(dd);
			}
			arrow.invalidate();
		}
		if (txt != null) {
			if (fromLoc != null && toLoc != null) {
				if (paint) {
					txt.setTextColor(app.getResources().getColor(
							textColor == 0 ? useCenter ? R.color.color_distance : R.color.color_myloc_distance : textColor));
				}
				txt.setText(OsmAndFormatter.getFormattedDistance(mes[0], app));
			} else {
				txt.setText("");
			}
		}
	}

	public void updateLocation(boolean centerChanged, boolean locationChanged, boolean compassChanged) {
		if (compassChanged && !dashboard.isMapLinkedToLocation()) {
			return;
		}
		updateAllWidgets();
	}
}
