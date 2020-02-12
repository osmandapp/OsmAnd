package net.osmand.plus.resources;

import net.osmand.ResultMatcher;
import net.osmand.data.TransportRoute;
import net.osmand.data.TransportStop;

import java.util.List;

public interface TransportIndexRepository {
	
	public boolean checkContains(double latitude, double longitude);

	public boolean checkContains(double topLatitude, double leftLongitude, double bottomLatitude, double rightLongitude);
	
	public boolean acceptTransportStop(TransportStop stop);
	
	public void searchTransportStops(double topLatitude, double leftLongitude, double bottomLatitude, double rightLongitude,
			int limit, List<TransportStop> stops, ResultMatcher<TransportStop> matcher);
	
	public List<TransportRoute> getRoutesForStop(TransportStop stop);

	public List<TransportRoute> getRoutesForReferences(int[] referencesToRoutes);

	public boolean isUseForPublicTransport();
}
