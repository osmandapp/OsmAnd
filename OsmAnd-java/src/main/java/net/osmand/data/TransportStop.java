package net.osmand.data;

import net.osmand.util.Algorithms;
import net.osmand.util.CollectionUtils;
import net.osmand.util.MapUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class TransportStop extends MapObject {

	private static final int DELETED_STOP = -1;
	public static final String MISSING_STOP_NAME = "#Missing Stop";

	private int[] referencesToRoutes = null;
	private long[] deletedRoutesIds;
	private long[] routesIds;
	public int distance;
	public int x31;
	public int y31;
	private List<TransportStopExit> exits;
	private List<TransportRoute> routes = null;
	private TransportStopAggregated transportStopAggregated;

	public TransportStop() {}
	
	public List<TransportRoute> getRoutes() {
		return routes;
	}
	
	public boolean isMissingStop() {
		return MISSING_STOP_NAME.equals(getName());
	}

	public void setRoutes(List<TransportRoute> routes) {
		this.routes = routes;
	}
	
	public void addRoute(TransportRoute rt) {
		if (this.routes == null) {
			this.routes = new ArrayList<TransportRoute>();
		}
		this.routes.add(rt);
	}

	public int[] getReferencesToRoutes() {
		return referencesToRoutes;
	}

	public void setReferencesToRoutes(int[] referencesToRoutes) {
		this.referencesToRoutes = referencesToRoutes;
	}

	public long[] getRoutesIds() {
		return routesIds;
	}

	public void setRoutesIds(long[] routesIds) {
		// CHECK route ids are sorted (used later)
		this.routesIds = routesIds;
	}
	
	public boolean hasRoute(long routeId) {
		// make assumption that ids are sorted
		return routesIds != null && Arrays.binarySearch(routesIds, routeId) >= 0;
	}

	public boolean isDeleted() {
		return referencesToRoutes != null && referencesToRoutes.length == 1 && referencesToRoutes[0] == DELETED_STOP;
	}

	public void setDeleted() {
		this.referencesToRoutes = new int[] { DELETED_STOP };
	}

	public long[] getDeletedRoutesIds() {
		return deletedRoutesIds;
	}

	public void setDeletedRoutesIds(long[] deletedRoutesIds) {
		this.deletedRoutesIds = deletedRoutesIds;
	}
	
	public void addRouteId(long routeId) {
		// make assumption that ids are sorted
		routesIds = CollectionUtils.addToArrayL(routesIds, routeId, true);
	}
	 
	public void addDeletedRouteId(long routeId) {
		deletedRoutesIds = CollectionUtils.addToArrayL(deletedRoutesIds, routeId, true);
	}

	public boolean isRouteDeleted(long routeId) {
		return deletedRoutesIds != null && Arrays.binarySearch(deletedRoutesIds, routeId) >= 0;
	}

	public boolean hasReferencesToRoutes() {
		return !isDeleted() && referencesToRoutes != null && referencesToRoutes.length > 0;
	}

	public Amenity getAmenity() {
		if (transportStopAggregated != null) {
			return transportStopAggregated.getAmenity();
		}
		return null;
	}

	public void setAmenity(Amenity amenity) {
		if (transportStopAggregated == null) {
			transportStopAggregated = new TransportStopAggregated();
		}
		transportStopAggregated.setAmenity(amenity);
	}

	public List<TransportStop> getLocalTransportStops() {
		if (transportStopAggregated != null) {
			return transportStopAggregated.getLocalTransportStops();
		}
		return Collections.emptyList();
	}

	public void addLocalTransportStop(TransportStop stop) {
		if (transportStopAggregated == null) {
			transportStopAggregated = new TransportStopAggregated();
		}
		transportStopAggregated.addLocalTransportStop(stop);
	}

	public List<TransportStop> getNearbyTransportStops() {
		if (transportStopAggregated != null) {
			return transportStopAggregated.getNearbyTransportStops();
		}
		return Collections.emptyList();
	}

	public void addNearbyTransportStop(TransportStop stop) {
		if (transportStopAggregated == null) {
			transportStopAggregated = new TransportStopAggregated();
		}
		transportStopAggregated.addNearbyTransportStop(stop);
	}

	public TransportStopAggregated getTransportStopAggregated() {
		return transportStopAggregated;
	}

	public void setTransportStopAggregated(TransportStopAggregated stopAggregated) {
		transportStopAggregated = stopAggregated;
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

	public void addExit(TransportStopExit transportStopExit) {
		if (exits == null) {
			exits = new ArrayList<>();
		}
		exits.add(transportStopExit);
	}

	public List<TransportStopExit> getExits() {
		if (exits == null) {
			return Collections.emptyList();
		}
		return this.exits;
	}

	public String getExitsString() {
		String exitsString = "";
		String refString = "";
		if (this.exits != null) {
			int i = 1;
			exitsString = exitsString + " Exits: [";
			for (TransportStopExit e : this.exits) {
				if (e.getRef() != null) {
					refString = " [ref:" + e.getRef() + "] ";
				}
				exitsString = exitsString + " " + i + ")" + refString + e.getName() + " " + e.getLocation() + " ]";
				i++;
			}
		}
		return exitsString;
	}

	public boolean compareStop(TransportStop thatObj) {
		if (this.compareObject(thatObj) &&
				// don't compare routes cause stop could be identical
				// ((this.routesIds == null && thatObj.routesIds == null) || (this.routesIds != null && this.routesIds.equals(thatObj.routesIds))) &&
				((this.exits == null && thatObj.exits == null) || (this.exits != null && thatObj.exits != null && this.exits.size() == thatObj.exits.size()))) {
			if (this.exits != null) {
				for (TransportStopExit exit1 : this.exits) {
					if(exit1 == null) {
						return false;
					}
					boolean contains = false;
					for (TransportStopExit exit2 : thatObj.exits) {
						if (Algorithms.objectEquals(exit1, exit2) ) {
							contains = true;
							if (!exit1.compareExit(exit2)) {
								return false;
							}
							break;
						}
					}
					if (!contains) {
						return false;
					}
				}
			}
		} else {
			return false;
		}
		return true;
	}
}
