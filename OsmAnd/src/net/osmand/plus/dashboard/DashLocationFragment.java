package net.osmand.plus.dashboard;

import android.annotation.SuppressLint;
import android.widget.ImageView;
import android.widget.TextView;

import net.osmand.data.LatLon;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.utils.UpdateLocationUtils;
import net.osmand.plus.utils.UpdateLocationUtils.UpdateLocationViewCache;

import java.util.ArrayList;
import java.util.List;

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
		if (dashboard == null) {
			return;
		}
		UpdateLocationViewCache cache = UpdateLocationUtils.getUpdateLocationViewCache(requireContext());
		for (DashLocationView view : distances) {
			cache.arrowResId = view.arrowResId;
			cache.paintTxt = view.paint;
			UpdateLocationUtils.updateLocationView(app, cache, view.arrow, view.txt, view.loc);
		}
	}

		public void updateLocation(boolean centerChanged, boolean locationChanged, boolean compassChanged) {
		if (compassChanged && !dashboard.isMapLinkedToLocation()) {
			return;
		}
		updateAllWidgets();
	}
}
