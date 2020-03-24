package net.osmand.router;

public class NativeTransportRoutingResult {

	public NativeTransportRouteResultSegment[] segments;
	public double finishWalkDist;
	public double routeTime;

	public static class NativeTransportRouteResultSegment {
		public NativeTransportRoute route;
		public double walkTime;
		public double travelDistApproximate;
		public double travelTime;
		public int start;
		public int end;
		public double walkDist ;
		public int depTime;
	}

	public static class NativeTransportRoute {
		//MapObject part:
		public long id;
		public double routeLat;
		public double routeLon;
		public String name;
		public String enName;
		//to HashMap <string, string> names
		public String[] namesLng;
		public String[] namesNames;
		public int fileOffset;
		//-----
		public NativeTransportStop[] forwardStops;
		public String ref;
		public String routeOperator;
		public String type;
		public int dist;
		public String color;

		//	Convert into TransportSchedule:
		public int[] intervals;
		public int[] avgStopIntervals;
		public int[] avgWaitIntervals;

		//	Convert into ways (and nodes):
		public long[] waysIds;
		public long[][] waysNodesIds;
		public double[][] waysNodesLats;
		public double[][] waysNodesLons;
	}

	public static class NativeTransportStop {
		//MapObject part:
		public long id;
		public double stopLat;
		public double stopLon;
		public String name;
		public String enName;
		public String[] namesLng;
		public String[] namesNames;
		public int fileOffset;
		//Leave next 3 field as arrays:
		public int[] referencesToRoutes;
		public long[] deletedRoutesIds;
		public long[] routesIds;
		public int distance;
		public int x31;
		public int y31;

		public NativeTransportRoute[] routes;
		// Convert into List<TransportStopExit> exits:
		public int[] pTStopExit_x31s;
		public int[] pTStopExit_y31s;
		public String[] pTStopExit_refs;
		// Convert into LinkedHashMap<String, int[]>
		public String[] referenceToRoutesKeys;
		public int[][] referenceToRoutesVals;
	}
}
