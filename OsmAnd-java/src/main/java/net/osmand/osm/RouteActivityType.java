package net.osmand.osm;

import java.util.ArrayList;
import java.util.List;

public class RouteActivityType {
	private static final List<RouteActivityType> values = new ArrayList<>();
	public static final String DEFAULT_ICON = "special_marker";
	public static final String DEFAULT_COLOR = "orange";

	public static final RouteActivityType WATER = createType("water", "yellow").icon("special_kayak").reg();
	public static final RouteActivityType WINTER = createType("winter", "yellow").icon("special_skiing").reg();
	public static final RouteActivityType SNOWMOBILE = createType("snowmobile", "yellow").icon("special_snowmobile").reg();
	public static final RouteActivityType RIDING = createType("riding", "yellow").icon("special_horse").reg();
	public static final RouteActivityType RACING = createType("racing", "yellow").icon("raceway").reg();
	public static final RouteActivityType MOUNTAINBIKE = createType("mountainbike", "blue").icon("sport_cycling").reg();
	public static final RouteActivityType CYCLING = createType("cycling", "blue").icon("special_bicycle").reg();
	public static final RouteActivityType HIKING = createType("hiking", "orange").icon("special_trekking").reg();
	public static final RouteActivityType RUNNING = createType("running", "orange").icon("running").reg();
	public static final RouteActivityType WALKING = createType("walking", "orange").icon("special_walking").reg();
	public static final RouteActivityType OFFROAD = createType("offroad", "yellow").icon("special_offroad").reg();
	public static final RouteActivityType MOTORBIKE = createType("motorbike", "green").icon("special_motorcycle").reg();
	public static final RouteActivityType CAR = createType("car", "green").icon("shop_car").reg();
	// less specific bottom order
	String name;
	String color;
	String icon;

	RouteActivityType(String nm, String clr) {
		this.name = nm;
		this.color = clr;
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

	public static RouteActivityType getOrCreateTypeFromName(String name) {
		for (RouteActivityType rat : values) {
			if (rat.name.equalsIgnoreCase(name)) {
				return rat;
			}
		}
		return createType(name.toLowerCase(), DEFAULT_COLOR).icon(DEFAULT_ICON).reg();
	}

	private static RouteActivityTypeBuilder createType(String name, String color) {
		RouteActivityTypeBuilder builder = new RouteActivityTypeBuilder();
		builder.routeActivityType = new RouteActivityType(name, color);
		return builder;
	}

	public static RouteActivityType getTypeFromTags(String[] tags) {
		RouteActivityType activityType = null;
		for (String tg : tags) {
			RouteActivityType rat = RouteActivityType.convertFromOsmGPXTag(tg);
			if (rat != null) {
				if (activityType == null || values.indexOf(activityType) > values.indexOf(rat)) {
					activityType = rat;
				}
			}
		}
		return activityType;
	}

	public static RouteActivityType convertFromOsmGPXTag(String tg) {
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
		case "FVbike":
		case "Motorrad":
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

	public static class RouteActivityTypeBuilder {

		private RouteActivityType routeActivityType;

		public RouteActivityTypeBuilder icon(String icon) {
			routeActivityType.icon = icon;
			return this;
		}

		private RouteActivityType reg() {
			values.add(routeActivityType);
			return routeActivityType;
		}
	}
}