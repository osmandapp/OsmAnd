package net.osmand.plus.dashboard;

import java.util.ArrayList;
import java.util.List;
import net.osmand.Location;
import net.osmand.data.LatLon;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.R;
import net.osmand.plus.views.DirectionDrawable;
import android.annotation.SuppressLint;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

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
	
	public static class DashLocationView {
		public boolean useOnlyMyLocation;
		public ImageView arrow;
		public TextView txt;
		public LatLon loc;
		
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
		screenOrientation = 0;
		screenOrientation = ((WindowManager) getActivity().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRotation();
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
		Sensor compass  = ((SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE)).getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
		if (compass == null) {
			screenOrientation = 0;
		}		
	}
	
	public LatLon getDefaultLocation() {
		DashboardOnMap d = dashboard;
		if(d == null) {
			return null;
		}
		return d.getMapViewLocation();
	}

	public void updateAllWidgets() {
		DashboardOnMap d = dashboard;
		if(d == null) {
			return;
		}
		float head = d.getHeading();
		float mapRotation = d.getMapRotation();
		LatLon mw = d.getMapViewLocation();
		Location l = d.getMyLocation();
		boolean mapLinked = d.isMapLinkedToLocation() && l != null;
		LatLon myLoc = l == null ? null : new LatLon(l.getLatitude(), l.getLongitude());
		for (DashLocationView lv : distances) {
			boolean useCenter = !mapLinked && !lv.useOnlyMyLocation;
			LatLon loc = (useCenter ? mw : myLoc);
			float h = useCenter ? -mapRotation : head;
			float[] mes = new float[2];
			if (loc != null) {
				Location.distanceBetween(lv.loc.getLatitude(), lv.loc.getLongitude(), loc.getLatitude(),
						loc.getLongitude(), mes);
			}
			if (lv.arrow != null) {
				if (!(lv.arrow.getDrawable() instanceof DirectionDrawable)) {
					DirectionDrawable dd = new DirectionDrawable(getActivity(), 10, 10);
					lv.arrow.setImageDrawable(dd);
				}
				DirectionDrawable dd = (DirectionDrawable) lv.arrow.getDrawable();
				dd.setImage(R.drawable.ic_destination_arrow_white, useCenter ? R.color.color_distance
						: R.color.color_myloc_distance);
				if (loc == null) {
					dd.setAngle(0);
				} else {
					dd.setAngle(mes[1] - h + 180 + screenOrientation);
				}
				lv.arrow.invalidate();
			}
			if (loc != null) {
				lv.txt.setTextColor(getActivity().getResources().getColor(useCenter ? R.color.color_distance
						: R.color.color_myloc_distance));
				lv.txt.setText(OsmAndFormatter.getFormattedDistance(mes[0], dashboard.getMyApplication()));
			} else {
				lv.txt.setText("");
			}
		}
	}

	public void updateLocation(boolean centerChanged, boolean locationChanged, boolean compassChanged) {
		if(compassChanged && !dashboard.isMapLinkedToLocation()) {
			boolean update = false;
			for (DashLocationView lv : distances) {
				if(lv.useOnlyMyLocation) {
					update = true;
					break;
				}
			}
			if(!update) {
				return;
			}
		}
		updateAllWidgets();
		
	}
}
