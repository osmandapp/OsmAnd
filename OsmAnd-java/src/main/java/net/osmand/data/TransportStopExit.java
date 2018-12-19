package net.osmand.data;

import net.osmand.util.MapUtils;

public class TransportStopExit extends MapObject {
	public int x31;
	public int y31;
	@Override
	public void setLocation(double latitude, double longitude) {
		super.setLocation(latitude, longitude);
	}
	public void setLocation(int zoom, int dx, int dy) {
		x31 = dx << (31 - zoom);
		y31 = dy << (31 - zoom);
		setLocation(MapUtils.getLatitudeFromTile(zoom, dy), MapUtils.getLongitudeFromTile(zoom, dx));
	}
}
