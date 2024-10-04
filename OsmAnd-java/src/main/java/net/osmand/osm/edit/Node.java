package net.osmand.osm.edit;

import java.io.Serializable;
import java.util.Map;

import net.osmand.Location;
import net.osmand.data.LatLon;

public class Node extends Entity implements Serializable {

	private static final long serialVersionUID = -2981499160640211082L;
	// currently not used
//	private boolean visible = true;
	
	public Node(double latitude, double longitude, long id){
		super(id, latitude,longitude);
	}
	
	public Node(Node n, long newId) {
		super(n, newId);
	}
	
	@Override
	public LatLon getLatLon() {
		return new LatLon(getLatitude(), getLongitude());
	}
	
	public Location getLocation() {
		Location l = new Location("");
		l.setLatitude(getLatitude());
		l.setLongitude(getLongitude());
		return l;
	}
	
	@Override
	public void initializeLinks(Map<EntityId, Entity> entities) {
		// nothing to initialize
		
	}

	@Override
	public String toString() {
		return "Node{" +
				"latitude=" + getLatitude() +
				", longitude=" + getLongitude() +
				", tags=" + getTags() +
				'}';
	}

	public boolean compareNode(Node thatObj) {
		return this.compareEntity(thatObj);
	}
}
