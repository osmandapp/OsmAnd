package net.osmand.plus.mapcontextmenu.editors;

import androidx.annotation.NonNull;

import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapmarkers.MapMarker;

public class MapMarkerEditor extends PointEditor {

	public static final String TAG = MapMarkerEditor.class.getSimpleName();

	private MapMarker marker;

	public MapMarkerEditor(@NonNull MapActivity mapActivity) {
		super(mapActivity);
	}

	@Override
	public boolean isProcessingTemplate() {
		return false;
	}

	@Override
	public String getFragmentTag() {
		return TAG;
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
