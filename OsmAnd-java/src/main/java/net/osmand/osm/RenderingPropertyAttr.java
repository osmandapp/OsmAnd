package net.osmand.osm;

public class RenderingPropertyAttr {

	public static final String HIKING_ROUTES = "hikingRoutesOSMC";
	public static final String CYCLE_ROUTES = "showCycleRoutes";
	public static final String MTB_ROUTES = "showMtbRoutes";
	public static final String ALPINE_HIKING = "alpineHiking";
	public static final String HORSE_ROUTES = "horseRoutes";
	public static final String PISTE_ROUTES = "pisteRoutes";
	public static final String WHITE_WATER_SPORTS = "whiteWaterSports";
	public static final String RUNNING_ROUTES = "showRunningRoutes";
	public static final String FITNESS_TRAILS = "showFitnessTrails";
	public static final String DIRTBIKE_ROUTES = "showDirtbikeTrails";
	public static final String CLIMBING_ROUTES = "showClimbingRoutes";
	public static final String SHOW_MTB_SCALE = "showMtbScale";
	public static final String SHOW_MTB_IMBA_SCALE = "showMtbScaleIMBATrails";

	public static String getRenderingClassNameForAttr(String attrName) {
		switch (attrName) {
			case HIKING_ROUTES:
				return ".route.hiking";
			case CYCLE_ROUTES:
				return ".route.bicycle";
			case MTB_ROUTES:
				return ".route.mtb";
			case SHOW_MTB_SCALE:
				return ".route.mtb.mtb_scale";
			case SHOW_MTB_IMBA_SCALE:
				return ".route.mtb.mtb_scale_imba";
			case ALPINE_HIKING:
				return ".road.alpinehiking";
			case HORSE_ROUTES:
				return ".route.horse";
			case PISTE_ROUTES:
				return ".route.piste";
			case RUNNING_ROUTES:
				return ".route.running";
			case FITNESS_TRAILS:
				return ".route.fitness_trail";
			case DIRTBIKE_ROUTES:
				return ".route.dirtbike";
			default:
				return attrName;
		}
	}
}
