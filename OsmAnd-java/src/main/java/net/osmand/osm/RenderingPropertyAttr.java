package net.osmand.osm;

public enum RenderingPropertyAttr {

	HIKING_ROUTES("hikingRoutesOSMC", ".route.hiking"),
	CYCLE_ROUTES("showCycleRoutes", ".route.bicycle"),
	MTB_ROUTES("showMtbRoutes", ".route.mtb"),
	ALPINE_HIKING("alpineHiking", ".road.alpinehiking"),
	HORSE_ROUTES("horseRoutes", ".route.horse"),
	PISTE_ROUTES("pisteRoutes", ".route.piste"),
	WHITE_WATER_SPORTS("whiteWaterSports", null),
	RUNNING_ROUTES("showRunningRoutes", ".route.running"),
	FITNESS_TRAILS("showFitnessTrails", ".route.fitness_trail"),
	DIRTBIKE_ROUTES("showDirtbikeTrails", ".route.dirtbike"),
	CLIMBING_ROUTES("showClimbingRoutes", null),
	SHOW_MTB_SCALE("showMtbScale", ".route.mtb.mtb_scale"),
	SHOW_MTB_IMBA_SCALE("showMtbScaleIMBATrails", ".route.mtb.mtb_scale_imba");

	private final String attrName;
	private final String renderingClassName;

	RenderingPropertyAttr(String attrName, String renderingClassName) {
		this.attrName = attrName;
		this.renderingClassName = renderingClassName;
	}

	public String getAttrName() {
		return attrName;
	}

	public String getRenderingClassName() {
		return renderingClassName;
	}

	public static String getRenderingClassName(String attrName) {
		RenderingPropertyAttr attr = fromAttrName(attrName);
		return attr != null && attr.renderingClassName != null ? attr.renderingClassName : attrName;
	}

	public static RenderingPropertyAttr fromAttrName(String attrName) {
		for (RenderingPropertyAttr attr : values()) {
			if (attr.attrName.equals(attrName)) {
				return attr;
			}
		}
		return null;
	}
}