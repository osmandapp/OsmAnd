package net.osmand.plus.measurementtool;

import net.osmand.GPXUtilities.GPXFile;
import net.osmand.data.QuadRect;

public class GpxData {

	private final GPXFile gpxFile;
	private final QuadRect rect;

	public GpxData(GPXFile gpxFile) {
		this.gpxFile = gpxFile;
		if (gpxFile != null) {
			this.rect = gpxFile.getRect();
		} else {
			this.rect = new QuadRect(0, 0, 0, 0);
		}
	}

	public GPXFile getGpxFile() {
		return gpxFile;
	}

	public QuadRect getRect() {
		return rect;
	}
}
