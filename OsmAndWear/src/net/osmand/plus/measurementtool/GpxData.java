package net.osmand.plus.measurementtool;

import net.osmand.plus.shared.SharedUtil;
import net.osmand.shared.gpx.GpxFile;
import net.osmand.data.QuadRect;

public class GpxData {

	private final GpxFile gpxFile;
	private final QuadRect rect;

	public GpxData(GpxFile gpxFile) {
		this.gpxFile = gpxFile;
		if (gpxFile != null) {
			this.rect = SharedUtil.jQuadRect(gpxFile.getRect());
		} else {
			this.rect = new QuadRect(0, 0, 0, 0);
		}
	}

	public GpxFile getGpxFile() {
		return gpxFile;
	}

	public QuadRect getRect() {
		return rect;
	}
}
