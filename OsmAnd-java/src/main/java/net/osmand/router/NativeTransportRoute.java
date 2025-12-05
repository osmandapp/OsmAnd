package net.osmand.router;

public class NativeTransportRoute {
	//MapObject part:
	public long id = -1l;
	public double routeLat = -1;
	public double routeLon = -1;
	public String name = "";
	public String enName = "";
	//to HashMap <string, string> names
	public String[] namesLng;
	public String[] namesNames;
	public int fileOffset;
	//-----
	public NativeTransportStop[] forwardStops;
	public String ref  = "";
	public String routeOperator = "";
	public String type = "";
	public int dist = -1;
	public String color = "";

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
