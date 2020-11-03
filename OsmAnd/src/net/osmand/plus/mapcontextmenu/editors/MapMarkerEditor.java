package net.osmand.plus.mapcontextmenu.editors;

import androidx.annotation.NonNull;

import net.osmand.plus.mapmarkers.MapMarker;
import net.osmand.plus.activities.MapActivity;

public class MapMarkerEditor extends PointEditor {

	private MapMarker marker;

	public MapMarkerEditor(@NonNull MapActivity mapActivity) {
		super(mapActivity);
	}

	@Override
	public String getFragmentTag() {
		return MapMarkerEditorFragment.class.getSimpleName();
	}

	public MapMarker getMarker() {
		return marker;
	}

	public void edit(@NonNull MapMarker marker) {
		this.marker = marker;
		isNew = false;
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			MapMarkerEditorFragment.showInstance(mapActivity);
		}
	}
}
