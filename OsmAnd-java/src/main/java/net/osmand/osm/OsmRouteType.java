package net.osmand.osm;

import static net.osmand.osm.OsmRouteType.RenderingPropertyAttr.*;

import net.osmand.binary.BinaryMapDataObject;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.BinaryMapRouteReaderAdapter;
import net.osmand.binary.RouteDataObject;
import net.osmand.router.network.NetworkRouteSelector;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

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
	public static final OsmRouteType PISTE = createType("piste").reg();
	public static final OsmRouteType RAILWAY = createType("railway").reg();
	public static final OsmRouteType SKI = createType("ski").renderingPropertyAttr(PISTE_ROUTES).reg();
	public static final OsmRouteType ALPINE = createType("alpine").renderingPropertyAttr(ALPINE_HIKING).reg();
	public static final OsmRouteType FITNESS = createType("fitness").renderingPropertyAttr(FITNESS_TRAILS).reg();
	public static final OsmRouteType INLINE_SKATES = createType("inline_skates").reg();
	public static final OsmRouteType SUBWAY = createType("subway").reg();
	public static final OsmRouteType TRAIN = createType("train").reg();
	public static final OsmRouteType TRACKS = createType("tracks").reg();
	public static final OsmRouteType TRAM = createType("tram").reg();
	public static final OsmRouteType TROLLEYBUS = createType("trolleybus").reg();

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
				return MOUNTAINBIKE;
			case "hiking":
			case "route=hiking":
			case "mountain hiking":
			case "hiking trail":
			case "wandern":
			case "hike":
			case "randonnée":
			case "trekking":
			case "climbing":
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
				return WINTER;
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

	public static List<NetworkRouteSelector.RouteKey> getRouteKeys(RouteDataObject obj) {
		Map<String, String> tags = new TreeMap<>();
		for (int i = 0; obj.nameIds != null && i < obj.nameIds.length; i++) {
			int nameId = obj.nameIds[i];
			String value = obj.names.get(nameId);
			BinaryMapRouteReaderAdapter.RouteTypeRule rt = obj.region.quickGetEncodingRule(nameId);
			if (rt != null) {
				tags.put(rt.getTag(), value);
			}
		}
		for (int i = 0; obj.types != null && i < obj.types.length; i++) {
			BinaryMapRouteReaderAdapter.RouteTypeRule rt = obj.region.quickGetEncodingRule(obj.types[i]);
			if (rt != null) {
				tags.put(rt.getTag(), rt.getValue());
			}
		}
		return getRouteKeys(tags);
	}

	public static List<NetworkRouteSelector.RouteKey> getRouteKeys(BinaryMapDataObject bMdo) {
		Map<String, String> tags = new TreeMap<>();
		for (int i = 0; i < bMdo.getObjectNames().keys().length; i++) {
			int keyInd = bMdo.getObjectNames().keys()[i];
			BinaryMapIndexReader.TagValuePair tp = bMdo.getMapIndex().decodeType(keyInd);
			String value = bMdo.getObjectNames().get(keyInd);
			if (tp != null) {
				tags.put(tp.tag, value);
			}
		}
		int[] tps = bMdo.getAdditionalTypes();
		for (int i = 0; i < tps.length; i++) {
			BinaryMapIndexReader.TagValuePair tp = bMdo.getMapIndex().decodeType(tps[i]);
			if (tp != null) {
				tags.put(tp.tag, tp.value);
			}
		}
		tps = bMdo.getTypes();
		for (int i = 0; i < tps.length; i++) {
			BinaryMapIndexReader.TagValuePair tp = bMdo.getMapIndex().decodeType(tps[i]);
			if (tp != null) {
				tags.put(tp.tag, tp.value);
			}
		}
		return getRouteKeys(tags);
	}

	private static int getRouteQuantity(Map<String, String> tags, OsmRouteType rType) {
		int q = 0;
		for (String tag : tags.keySet()) {
			if (tag.startsWith(rType.tagPrefix)) {
				int num = Algorithms.extractIntegerNumber(tag);
				if (num > 0 && tag.equals(rType.tagPrefix + num)) {
					q = Math.max(q, num);
				}
			}
		}
		return q;
	}

	public static List<NetworkRouteSelector.RouteKey> getRouteKeys(Map<String, String> tags) {
		List<NetworkRouteSelector.RouteKey> lst = new ArrayList<>();
		for (OsmRouteType routeType : OsmRouteType.values) {
			int rq = getRouteQuantity(tags, routeType);
			for (int routeIdx = 1; routeIdx <= rq; routeIdx++) {
				String prefix = routeType.tagPrefix + routeIdx;
				NetworkRouteSelector.RouteKey routeKey = new NetworkRouteSelector.RouteKey(routeType);
				for (Map.Entry<String, String> e : tags.entrySet()) {
					String tag = e.getKey();
					if (tag.startsWith(prefix) && tag.length() > prefix.length()) {
						String key = tag.substring(prefix.length() + 1);
						routeKey.addTag(key, e.getValue());
					}
				}
				lst.add(routeKey);
			}
		}
		return lst;
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

		public RouteActivityTypeBuilder renderingPropertyAttr(String renderingPropertyAttr) {
			osmRouteType.renderingPropertyAttr = renderingPropertyAttr;
			return this;
		}

		private OsmRouteType reg() {
			values.add(osmRouteType);
			return osmRouteType;
		}
	}

	protected static class RenderingPropertyAttr {
		static final String HIKING_ROUTES = "hikingRoutesOSMC";
		static final String CYCLE_ROUTES = "showCycleRoutes";
		static final String MTB_ROUTES = "showMtbRoutes";
		static final String ALPINE_HIKING = "alpineHiking";
		static final String HORSE_ROUTES = "horseRoutes";
		static final String PISTE_ROUTES = "pisteRoutes";
		static final String WHITE_WATER_SPORTS = "whiteWaterSports";
		static final String RUNNING_ROUTES = "showRunningRoutes";
		static final String FITNESS_TRAILS = "showFitnessTrails";
		static final String DIRTBIKE_ROUTES = "showDirtbikeTrails";
	}
}