package net.osmand.data;

import net.osmand.util.MapUtils;

public class TransportStop extends MapObject {
	private int[] referencesToRoutes = null;
	private Amenity amenity;
	public int distance;
	public int x31;
	public int y31;
	

	public TransportStop(){
	}
	
	public int[] getReferencesToRoutes() {
		return referencesToRoutes;
	}
	
	public void setReferencesToRoutes(int[] referencesToRoutes) {
		this.referencesToRoutes = referencesToRoutes;
	}

	public Amenity getAmenity() {
		return amenity;
	}

	public void setAmenity(Amenity amenity) {
		this.amenity = amenity;
	}
	
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
