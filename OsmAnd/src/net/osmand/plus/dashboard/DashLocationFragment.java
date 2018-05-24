package net.osmand.plus.dashboard;

import java.util.ArrayList;
import java.util.List;

import net.osmand.data.LatLon;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.UiUtilities.UpdateLocationViewCache;
import android.annotation.SuppressLint;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Created by Denis
 * on 26.01.2015.
 */
@SuppressLint("ResourceAsColor")
public abstract class DashLocationFragment extends DashBaseFragment {

	protected List<DashLocationView> distances = new ArrayList<DashLocationFragment.DashLocationView>();

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
		UiUtilities ic = getMyApplication().getUIUtilities();
		UpdateLocationViewCache cache = ic.getUpdateLocationViewCache();
		for (DashLocationView lv : distances) {
			cache.arrowResId = lv.arrowResId;
			cache.paintTxt = lv.paint;
			ic.updateLocationView(cache, lv.arrow, lv.txt, lv.loc);
		}
	}

		public void updateLocation(boolean centerChanged, boolean locationChanged, boolean compassChanged) {
		if (compassChanged && !dashboard.isMapLinkedToLocation()) {
			return;
		}
		updateAllWidgets();
	}
}
