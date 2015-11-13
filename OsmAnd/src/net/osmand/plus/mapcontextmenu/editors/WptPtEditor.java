package net.osmand.plus.mapcontextmenu.editors;

import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.GPXUtilities.WptPt;
import net.osmand.plus.activities.MapActivity;

public class WptPtEditor extends PointEditor {

	private WptPt wpt;

	public static final String TAG = "WptPtEditorFragment";

	public WptPtEditor(MapActivity mapActivity) {
		super(mapActivity);
	}

	@Override
	public String getFragmentTag() {
		return TAG;
	}

	public WptPt getWptPt() {
		return wpt;
	}

	public void add(LatLon latLon, String title) {
		if (latLon == null) {
			return;
		}
		isNew = true;
		wpt = new WptPt(latLon.getLatitude(), latLon.getLongitude(),
				System.currentTimeMillis(), Double.NaN, 0, Double.NaN);
		wpt.name = title;
		WptPtEditorFragment.showInstance(mapActivity);
	}

	public void edit(WptPt wpt) {
		if (wpt == null) {
			return;
		}
		isNew = false;
		this.wpt = wpt;
		WptPtEditorFragment.showInstance(mapActivity);
	}
}
