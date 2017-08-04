package net.osmand.plus.mapcontextmenu.editors;

import net.osmand.plus.GPXUtilities.GPXFile;
import net.osmand.plus.GPXUtilities.WptPt;
import net.osmand.plus.activities.MapActivity;

public abstract class GpxPointEditor extends PointEditor {

	protected GPXFile gpxFile;
	protected WptPt wpt;
	protected boolean gpxSelected;

	public GpxPointEditor(MapActivity mapActivity) {
		super(mapActivity);
	}

	public GPXFile getGpxFile() {
		return gpxFile;
	}

	public boolean isGpxSelected() {
		return gpxSelected;
	}

	public WptPt getWptPt() {
		return wpt;
	}
}
