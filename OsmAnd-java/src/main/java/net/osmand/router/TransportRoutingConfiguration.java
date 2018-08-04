package net.osmand.router;

public class TransportRoutingConfiguration {

	
	public int ZOOM_TO_LOAD_TILES = 14;
	
	public int walkRadius = 3000;
	
	public int walkChangeRadius = 300; 
	
	public double walkSpeed = 3.6 / 3.6; // m/s
	
	public double travelSpeed = 36 / 3.6; // m/s
	
	public int stopTime = 30;
	
	public int changeTime = 300;
	
	public int maxNumberOfChanges = 7; 
	
	public int finishTimeSeconds = 1200;

	public int maxRouteTime = 60 * 60 * 1000; // 1000 hours
}
