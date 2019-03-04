package net.osmand.osm.edit;

import net.osmand.data.LatLon;
import net.osmand.util.Algorithms;

import java.io.Serializable;
import java.util.Map;

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
