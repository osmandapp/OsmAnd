package net.osmand.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TransportStopAggregated {

	private Amenity amenity;
	private List<TransportStop> localTransportStops;
	private List<TransportStop> nearbyTransportStops;

	public TransportStopAggregated() {
	}

	public Amenity getAmenity() {
		return amenity;
	}

	public void setAmenity(Amenity amenity) {
		this.amenity = amenity;
	}

	public List<TransportStop> getLocalTransportStops() {
		if (localTransportStops == null) {
			return Collections.emptyList();
		}
		return this.localTransportStops;
	}

	public void addLocalTransportStop(TransportStop stop) {
		if (localTransportStops == null) {
			localTransportStops = new ArrayList<>();
		}
		localTransportStops.add(stop);
	}

	public void addLocalTransportStops(List<TransportStop> stops) {
		if (localTransportStops == null) {
			localTransportStops = new ArrayList<>();
		}
		localTransportStops.addAll(stops);
	}

	public List<TransportStop> getNearbyTransportStops() {
		if (nearbyTransportStops == null) {
			return Collections.emptyList();
		}
		return this.nearbyTransportStops;
	}

	public void addNearbyTransportStop(TransportStop stop) {
		if (nearbyTransportStops == null) {
			nearbyTransportStops = new ArrayList<>();
		}
		nearbyTransportStops.add(stop);
	}

	public void addNearbyTransportStops(List<TransportStop> stops) {
		if (nearbyTransportStops == null) {
			nearbyTransportStops = new ArrayList<>();
		}
		nearbyTransportStops.addAll(stops);
	}
}