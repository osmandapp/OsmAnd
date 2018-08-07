package net.osmand.router;

public class TransportRoutingConfiguration {

	public static final String KEY = "public_transport";
	
	public int ZOOM_TO_LOAD_TILES = 14;
	
	public int walkRadius = 1500; // ? 3000
	
	public int walkChangeRadius = 300; 
	
	public double walkSpeed = 3.6 / 3.6; // m/s
	
	public double travelSpeed = 36 / 3.6; // m/s
	
	public int stopTime = 30;
	
	public int changeTime = 300;
	
	public int maxNumberOfChanges = 5;  
	
	public int finishTimeSeconds = 1200;

	public int maxRouteTime = 60 * 60 * 1000; // 1000 hours
	
	
	public TransportRoutingConfiguration(RoutingConfiguration.Builder builder) {
		GeneralRouter router = builder == null ? null : builder.getRouter("public_transport");
		if(router != null) {
			walkRadius =  router.getIntAttribute("walkRadius", walkRadius);
			walkChangeRadius =  router.getIntAttribute("walkChangeRadius", walkRadius);
			ZOOM_TO_LOAD_TILES =  router.getIntAttribute("zoomToLoadTiles", ZOOM_TO_LOAD_TILES);
			maxNumberOfChanges =  router.getIntAttribute("maxNumberOfChanges", maxNumberOfChanges);
			finishTimeSeconds =  router.getIntAttribute("delayForAlternativesRoutes", finishTimeSeconds);
			
			travelSpeed =  router.getFloatAttribute("defaultTravelSpeed", (float) travelSpeed);
			walkSpeed =  router.getFloatAttribute("defaultWalkSpeed", (float) walkSpeed);
			stopTime =  router.getIntAttribute("defaultStopTime", stopTime);
			changeTime =  router.getIntAttribute("defaultChangeTime", changeTime);
			
			
		}
	}
	
}
