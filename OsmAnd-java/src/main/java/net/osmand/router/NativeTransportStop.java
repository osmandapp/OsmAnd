package net.osmand.router;

public class NativeTransportStop {
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