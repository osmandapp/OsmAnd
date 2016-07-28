package net.osmand.plus.resources;

import java.util.List;

import net.osmand.ResultMatcher;
import net.osmand.data.LatLon;
import net.osmand.data.TransportRoute;
import net.osmand.data.TransportStop;

public interface TransportIndexRepository {
	
	public boolean checkContains(double latitude, double longitude);

	public boolean checkContains(double topLatitude, double leftLongitude, double bottomLatitude, double rightLongitude);
	
	public boolean acceptTransportStop(TransportStop stop);
	
	public void searchTransportStops(double topLatitude, double leftLongitude, double bottomLatitude, double rightLongitude,
			int limit, List<TransportStop> stops, ResultMatcher<TransportStop> matcher);
	
	public List<String> getRouteDescriptionsForStop(TransportStop stop, String format);

	public List<RouteInfoLocation> searchTransportRouteStops(double latitude, double longitude, LatLon locationToGo, int zoom);
		
	public void close();
	
	public static class RouteInfoLocation {
		private TransportStop start;
		private TransportStop stop;
		private TransportRoute route;
		private int stopNumbers;
		private int distToLocation;
		private boolean direction;
		
		public TransportStop getStart() {
			return start;
		}
		
		public TransportRoute getRoute() {
			return route;
		}
		
		public boolean getDirection(){
			return direction;
		}
		
		public void setDirection(boolean direction) {
			this.direction = direction;
		}
		
		public int getDistToLocation() {
			return distToLocation;
		}
		
		public void setStart(TransportStop start) {
			this.start = start;
		}
		
		public TransportStop getStop() {
			return stop;
		}
		
		public int getStopNumbers() {
			return stopNumbers;
		}
		
		public void setStopNumbers(int stopNumbers) {
			this.stopNumbers = stopNumbers;
		}
		
		public void setStop(TransportStop stop) {
			this.stop = stop;
		}
		
		public void setRoute(TransportRoute route) {
			this.route = route;
		}
		
		public void setDistToLocation(int distToLocation) {
			this.distToLocation = distToLocation;
		}
	}
}
