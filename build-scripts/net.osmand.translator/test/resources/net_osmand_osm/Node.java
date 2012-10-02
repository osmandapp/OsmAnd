package net.osmand.osm;

import java.io.Serializable;
import java.util.Map;

public class Node extends Entity implements Serializable {

	private static final long serialVersionUID = -2981499160640211082L;
	private double latitude;
	private double longitude;
	// currently not used
//	private boolean visible = true;
	
	public Node(double latitude, double longitude, long id){
		super(id);
		this.latitude = latitude;
		this.longitude = longitude;
	}
	
	public Node(Node n, long newId) {
		super(n, newId);
		this.latitude = n.latitude;
		this.longitude = n.longitude;
	}

	public double getLatitude() {
		return latitude;
	}
	
	public double getLongitude() {
		return longitude;
	}
	
	public void setLatitude(double latitude) {
		this.latitude = latitude;
	}
	
	public void setLongitude(double longitude) {
		this.longitude = longitude;
	}
	
	@Override
	public LatLon getLatLon() {
		return new LatLon(latitude, longitude);
	}
	
	@Override
	public void initializeLinks(Map<EntityId, Entity> entities) {
		// nothing to initialize
		
	}

}
