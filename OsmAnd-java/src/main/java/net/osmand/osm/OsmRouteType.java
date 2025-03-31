package net.osmand.osm;

import static net.osmand.osm.RenderingPropertyAttr.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class OsmRouteType {

	private static final List<OsmRouteType> values = new ArrayList<>();

	public static final String DEFAULT_ICON = "special_marker";
	public static final String DEFAULT_COLOR = "orange";

	public static final OsmRouteType WATER = createType("water").color("yellow").icon("special_kayak")
			.renderingPropertyAttr(WHITE_WATER_SPORTS).reg();
	public static final OsmRouteType WINTER = createType("winter").color("yellow").icon("special_skiing").reg();
	public static final OsmRouteType SNOWMOBILE = createType("snowmobile").color("yellow").icon("special_snowmobile").reg();
	public static final OsmRouteType RIDING = createType("riding").color("yellow").icon("special_horse").reg();
	public static final OsmRouteType RACING = createType("racing").color("yellow").icon("raceway").reg();
	public static final OsmRouteType MOUNTAINBIKE = createType("mountainbike").color("blue").icon("sport_cycling").reg();
	public static final OsmRouteType BICYCLE = createType("bicycle").renderingPropertyAttr(CYCLE_ROUTES).reg();
	public static final OsmRouteType DIRTBIKE = createType("dirtbike").renderingPropertyAttr(DIRTBIKE_ROUTES).reg();
	public static final OsmRouteType MTB = createType("mtb").renderingPropertyAttr(MTB_ROUTES).reg();
	public static final OsmRouteType CYCLING = createType("cycling").color("blue").icon("special_bicycle").reg();
	public static final OsmRouteType HIKING = createType("hiking").color("orange").icon("special_trekking")
			.renderingPropertyAttr(HIKING_ROUTES).reg();
	public static final OsmRouteType RUNNING = createType("running").color("orange").icon("running")
			.renderingPropertyAttr(RUNNING_ROUTES).reg();
	public static final OsmRouteType WALKING = createType("walking").color("orange").icon("special_walking").reg();
	public static final OsmRouteType OFFROAD = createType("offroad").color("yellow").icon("special_offroad").reg();
	public static final OsmRouteType MOTORBIKE = createType("motorbike").color("green").icon("special_motorcycle").reg();
	public static final OsmRouteType CAR = createType("car").color("green").icon("shop_car").reg();

	public static final OsmRouteType HORSE = createType("horse").renderingPropertyAttr(HORSE_ROUTES).reg();

	public static final OsmRouteType ROAD = createType("road").reg();
	public static final OsmRouteType DETOUR = createType("detour").reg();
	public static final OsmRouteType BUS = createType("bus").reg();
	public static final OsmRouteType CANOE = createType("canoe").reg();
	public static final OsmRouteType FERRY = createType("ferry").reg();
	public static final OsmRouteType FOOT = createType("foot").reg();
	public static final OsmRouteType LIGHT_RAIL = createType("light_rail").reg();
	public static final OsmRouteType RAILWAY = createType("railway").reg();
	public static final OsmRouteType SKI = createType("piste").renderingPropertyAttr(PISTE_ROUTES).reg();
	public static final OsmRouteType ALPINE = createType("alpine").renderingPropertyAttr(ALPINE_HIKING).reg();
	public static final OsmRouteType FITNESS = createType("fitness_trail").renderingPropertyAttr(FITNESS_TRAILS).reg();
	public static final OsmRouteType INLINE_SKATES = createType("inline_skates").reg();
	public static final OsmRouteType SUBWAY = createType("subway").reg();
	public static final OsmRouteType TRAIN = createType("train").reg();
	public static final OsmRouteType TRACKS = createType("tracks").reg();
	public static final OsmRouteType TRAM = createType("tram").reg();
	public static final OsmRouteType TROLLEYBUS = createType("trolleybus").reg();
	public static final OsmRouteType CLIMBING = createType("climbing").renderingPropertyAttr(CLIMBING_ROUTES).reg();

	// OsmRouteType.UNKNOWN is used for TravelGpx OSM routes.
	// It allows us to reuse code of RouteInfoCard, RouteKey icons, etc.
	public static final OsmRouteType UNKNOWN = createType("unknown").reg();

	// less specific bottom order
	private final String name;
	private final String tagPrefix;

	private String color;
	private String icon;
	private String renderingPropertyAttr;

	OsmRouteType(String name) {
		this.name = name;
		tagPrefix = "route_" + name + "_";
	}

	public String getName() {
		return name;
	}

	public String getColor() {
		return color;
	}

	public String getIcon() {
		return icon;
	}

	public String getRenderingPropertyAttr() {
		return renderingPropertyAttr;
	}

	public String getTagPrefix() {
		return tagPrefix;
	}

	public static List<OsmRouteType> getAllValues() {
		return values;
	}

	public static OsmRouteType getOrCreateTypeFromName(String name) {
		for (OsmRouteType rat : values) {
			if (rat.name.equalsIgnoreCase(name)) {
				return rat;
			}
		}
		return createType(name.toLowerCase()).color(DEFAULT_COLOR).icon(DEFAULT_ICON).reg();
	}

	private static RouteActivityTypeBuilder createType(String name) {
		RouteActivityTypeBuilder builder = new RouteActivityTypeBuilder();
		builder.osmRouteType = new OsmRouteType(name);
		return builder;
	}

	public static OsmRouteType getTypeFromTags(String[] tags) {
		OsmRouteType activityType = null;
		for (String tg : tags) {
			OsmRouteType rat = OsmRouteType.convertFromOsmGPXTag(tg);
			if (rat != null) {
				if (activityType == null || values.indexOf(activityType) > values.indexOf(rat)) {
					activityType = rat;
				}
			}
		}
		return activityType;
	}

	public static OsmRouteType getTypeFromOSMTags(Map<String, String> tags) {
		String rt = tags.get("route");
		if (rt != null) {
			return convertFromOsmGPXTag(rt);
		}
		return null;
	}

	public static OsmRouteType convertFromOsmGPXTag(String tg) {
		String t = tg.toLowerCase();
		if ("mountain hiking".equalsIgnoreCase(t)) {
			return HIKING;
		}
		if ("motorcar".equalsIgnoreCase(t)) {
			return CAR;
		}
		if ("laufen".equalsIgnoreCase(t)) {
			return RUNNING;
		}
		if ("pedestrian".equalsIgnoreCase(t)) {
			return WALKING;
		}
		switch (t) {
			case "mountainbiking":
			case "mtb":
			case "mountainbike":
			case "mountain bike":
			case "mountain biking":
			case "mountbarker":
			case "mtb-tour":
			case "ciclismo-mtb-gravel":
			case "vtt":
			case "btt":
			case "vth":
			case "mtb ride":
			case "disused:mtb":
			case "abandoned:mtb":
			case "mtb:scale":
				return MTB;
			case "hiking":
			case "route=hiking":
			case "mountain hiking":
			case "hiking trail":
			case "wandern":
			case "hike":
			case "randonnée":
			case "trekking":
			case "climbing":
			case "ferrata":
			case "via_ferrata":
			case "proposed:hiking":
			case "deprecated:hiking":
			case "abandoned:hiking":
			case "old_hiking":
			case "canyoning":
				return HIKING;
			case "bike":
			case "biking":
			case "bicycling":
			case "bicycle":
			case "cycling":
			case "cycle":
			case "cycleway":
			case "cykel":
			case "handcycle":
			case "cyclotourisme":
			case "route=bicycle":
			case "cyclotourism":
			case "fietsen":
			case "вело":
			case "велосипед":
			case "rower":
			case "trasa rowerem":
			case "vélo":
			case "velo":
			case "radtour":
			case "bici":
			case "fiets":
			case "fahrrad":
			case "ncn":
			case "icn":
			case "lcn":
			case "network=ncn":
			case "network=icn":
			case "network=lcn":
			case "proposed:bicycle":
			case "old_bicycle":
				return CYCLING;
			case "car":
			case "motorcar":
			case "by car":
			case "auto":
			case "автомобиль":
			case "automobile":
			case "autós":
			case "driving":
			case "drive":
			case "van":
			case "авто":
			case "на автомобиле":
			case "bus":
			case "truck":
			case "taxi":
				return CAR;
			case "running":
			case "run":
			case "rungis":
			case "trail running":
			case "trailrunning":
			case "бег":
			case "laufen":
			case "langlauf":
			case "lauf":
			case "course":
			case "jogging":
			case "fitotrack":
				return RUNNING;
			case "wanderung":
			case "walking":
			case "walk":
			case "nightwalk":
			case "walkway":
			case "пешком":
			case "пеший":
			case "pěšky":
			case "marche":
			case "pedestrian":
			case "foot":
			case "footing":
			case "on_foot":
			case "byfoot":
			case "onfoot":
			case "sightseeing":
			case "geocaching":
			case "etnanatura":
			case "etna":
			case "iwn":
			case "lwn":
			case "rwn":
			case "network=iwn":
			case "network=lwn":
			case "network=rwn":
			case "unmarked:foot":
			case "nordic_walking":
				return WALKING;
			case "ling-moto":
			case "motorbiking":
			case "motorcycle":
			case "motorrad":
			case "motorbike":
			case "motor bike":
			case "fvbike":
				return MOTORBIKE;
			case "offroad":
			case "off-road":
			case "off road":
			case "4x4":
			case "terrain":
			case "quad":
			case "enduro":
			case "feldwege":
			case "feldweg":
			case "rally":
				return OFFROAD;
			case "boat":
			case "water":
			case "boating":
			case "kayak":
			case "river":
			case "lake":
			case "lakes":
			case "canal":
			case "waterway":
			case "motorboat":
			case "canoe":
				return WATER;
			case "ski":
			case "skiing":
			case "skating":
			case "skitour":
			case "winter":
			case "wintersports":
			case "snowboard":
			case "лыжи":
			case "лыжня":
			case "nordic":
			case "piste":
			case "piste:type":
			case "piste:difficulty":
				return SKI;
			case "snowmobile=designated":
			case "snowmobile=permissive":
			case "snowmobile=yes":
			case "snowmobile":
				return SNOWMOBILE;
			case "ride":
			case "horse":
			case "horse trail":
				return RIDING;
			case "racing":
				return RACING;
			case "inline_skates":
				return INLINE_SKATES;
			case "fitness_trail":
				return FITNESS;
			case "dirtbike":
			case "dirtbike:scale":
				return DIRTBIKE;
		}
		return null;
	}

	public static OsmRouteType getByTag(String tag) {
		for (OsmRouteType routeType : values) {
			if (routeType.name.equals(tag)) {
				return routeType;
			}
		}
		return null;
	}

	public static OsmRouteType getByRenderingProperty(String renderingProperty) {
		for (OsmRouteType routeType : values) {
			if (routeType.renderingPropertyAttr != null && routeType.renderingPropertyAttr.equals(renderingProperty)) {
				return routeType;
			}
		}
		return null;
	}

	public static class RouteActivityTypeBuilder {

		private OsmRouteType osmRouteType;

		public RouteActivityTypeBuilder color(String color) {
			osmRouteType.color = color;
			return this;
		}

		public RouteActivityTypeBuilder icon(String icon) {
			osmRouteType.icon = icon;
			return this;
		}

		public RouteActivityTypeBuilder renderingPropertyAttr(String propertyAttr) {
			osmRouteType.renderingPropertyAttr = propertyAttr;
			return this;
		}

		public RouteActivityTypeBuilder renderingPropertyAttr(RenderingPropertyAttr propertyAttr) {
			renderingPropertyAttr(propertyAttr.getAttrName());
			return this;
		}

		private OsmRouteType reg() {
			values.add(osmRouteType);
			return osmRouteType;
		}
	}
}
