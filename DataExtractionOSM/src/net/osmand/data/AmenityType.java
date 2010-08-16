package net.osmand.data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.osmand.Messages;


// http://wiki.openstreetmap.org/wiki/Amenity
// POI tags : amenity, leisure, shop, sport, tourism, historic; accessories (internet-access), natural ?
public enum AmenityType {
	// Some of those types are subtypes of Amenity tag 
	SUSTENANCE("amenity_type_sustenance"), // restaurant, cafe ... //$NON-NLS-1$
	EDUCATION("amenity_type_education"), // school, ... //$NON-NLS-1$
	TRANSPORTATION("amenity_type_transportation"), // car_wash, parking, ... //$NON-NLS-1$
	FINANCE("amenity_type_finance"), // bank, atm, ... //$NON-NLS-1$
	HEALTHCARE("amenity_type_healthcare"), // hospital ... //$NON-NLS-1$
	ENTERTAINMENT("amenity_type_entertainment"), // cinema, ... (+! sauna, brothel) //$NON-NLS-1$
	TOURISM("amenity_type_tourism"), // [TAG] hotel, sights, museum .. //$NON-NLS-1$ 
	HISTORIC("amenity_type_historic"), // [TAG] historic places, monuments (should we unify tourism/historic) //$NON-NLS-1$
	NATURAL("amenity_type_natural"), // [TAG] natural places, monuments (should we unify tourism/historic) //$NON-NLS-1$
	SHOP("amenity_type_shop"), // [TAG] convenience (product), clothes... //$NON-NLS-1$
	LEISURE("amenity_type_leisure"), // [TAG] leisure //$NON-NLS-1$
	SPORT("amenity_type_sport"), // [TAG] sport //$NON-NLS-1$
	OTHER("amenity_type_other"), // grave-yard, police, post-office [+Internet_access] //$NON-NLS-1$
	;
	
	private String name;
	
	private AmenityType(String name)
	{
		this.name = name;	
	}
	
	public static AmenityType fromString(String s){
		try {
			return AmenityType.valueOf(s.toUpperCase());
		} catch (IllegalArgumentException e) {
			return AmenityType.OTHER;
		}
	}
	
	public static String valueToString(AmenityType t){
		return t.toString().toLowerCase();
	}
	
	public static AmenityType[] getCategories(){
		return AmenityType.values();
	}
	
	public static List<String> getSubCategories(AmenityType t){
		List<String> list = new ArrayList<String>();
		for(String s : amenityMap.keySet()){
			if(amenityMap.get(s) == t){
				list.add(s);
			}
		}
		return list;
	}
	
	public static Map<String, AmenityType> getAmenityMap(){
		return amenityMap;
	}
	
	public static String toPublicString(AmenityType t){
//		return Algoritms.capitalizeFirstLetterAndLowercase(t.toString().replace('_', ' '));
		return Messages.getMessage(t.name);
	}
	

	protected static Map<String, AmenityType> amenityMap = new LinkedHashMap<String, AmenityType>();
	static {
		
		amenityMap.put("alpine_hut", AmenityType.TOURISM); //$NON-NLS-1$
		amenityMap.put("attraction", AmenityType.TOURISM); //$NON-NLS-1$
		amenityMap.put("artwork", AmenityType.TOURISM); //$NON-NLS-1$
		amenityMap.put("camp_site", AmenityType.TOURISM); //$NON-NLS-1$
		amenityMap.put("caravan_site", AmenityType.TOURISM); //$NON-NLS-1$
		amenityMap.put("chalet", AmenityType.TOURISM); //$NON-NLS-1$
		amenityMap.put("guest_house", AmenityType.TOURISM); //$NON-NLS-1$
		amenityMap.put("hostel", AmenityType.TOURISM); //$NON-NLS-1$
		amenityMap.put("hotel", AmenityType.TOURISM); //$NON-NLS-1$
		amenityMap.put("information", AmenityType.TOURISM); //$NON-NLS-1$
		amenityMap.put("motel", AmenityType.TOURISM); //$NON-NLS-1$
		amenityMap.put("museum", AmenityType.TOURISM); //$NON-NLS-1$
		amenityMap.put("picnic_site", AmenityType.TOURISM); //$NON-NLS-1$
		amenityMap.put("theme_park", AmenityType.TOURISM); //$NON-NLS-1$
		amenityMap.put("viewpoint", AmenityType.TOURISM); //$NON-NLS-1$
		amenityMap.put("zoo", AmenityType.TOURISM); //$NON-NLS-1$
		
		amenityMap.put("archaeological_site", AmenityType.HISTORIC); //$NON-NLS-1$
		amenityMap.put("battlefield", AmenityType.HISTORIC); //$NON-NLS-1$
		amenityMap.put("boundary_stone", AmenityType.HISTORIC); //$NON-NLS-1$
		amenityMap.put("castle", AmenityType.HISTORIC); //$NON-NLS-1$
		amenityMap.put("fort", AmenityType.HISTORIC); //$NON-NLS-1$
		amenityMap.put("memorial", AmenityType.HISTORIC); //$NON-NLS-1$
		amenityMap.put("pa", AmenityType.HISTORIC); //$NON-NLS-1$
		amenityMap.put("monument", AmenityType.HISTORIC); //$NON-NLS-1$
		amenityMap.put("ruins", AmenityType.HISTORIC); //$NON-NLS-1$
		amenityMap.put("wayside_cross", AmenityType.HISTORIC); //$NON-NLS-1$
		amenityMap.put("wayside_shrine", AmenityType.HISTORIC); //$NON-NLS-1$
		amenityMap.put("wreck", AmenityType.HISTORIC); //$NON-NLS-1$
		
		amenityMap.put("alcohol", AmenityType.SHOP); //$NON-NLS-1$
		amenityMap.put("bakery", AmenityType.SHOP); //$NON-NLS-1$
		amenityMap.put("beauty", AmenityType.SHOP); //$NON-NLS-1$
		amenityMap.put("beverages", AmenityType.SHOP); //$NON-NLS-1$
		amenityMap.put("bicycle", AmenityType.SHOP); //$NON-NLS-1$
		amenityMap.put("books", AmenityType.SHOP); //$NON-NLS-1$
		amenityMap.put("boutique", AmenityType.SHOP); //$NON-NLS-1$
		amenityMap.put("butcher", AmenityType.SHOP); //$NON-NLS-1$
		amenityMap.put("car", AmenityType.SHOP); //$NON-NLS-1$
		amenityMap.put("car_repair", AmenityType.SHOP); //$NON-NLS-1$
		amenityMap.put("charity", AmenityType.SHOP); //$NON-NLS-1$
		amenityMap.put("chemist", AmenityType.SHOP); //$NON-NLS-1$
		amenityMap.put("clothes", AmenityType.SHOP); //$NON-NLS-1$
		amenityMap.put("computer", AmenityType.SHOP); //$NON-NLS-1$
		amenityMap.put("confectionery", AmenityType.SHOP); //$NON-NLS-1$
		amenityMap.put("convenience", AmenityType.SHOP); //$NON-NLS-1$
		amenityMap.put("department_store", AmenityType.SHOP); //$NON-NLS-1$
		amenityMap.put("dry_cleaning", AmenityType.SHOP); //$NON-NLS-1$
		amenityMap.put("doityourself", AmenityType.SHOP); //$NON-NLS-1$
		amenityMap.put("electronics", AmenityType.SHOP); //$NON-NLS-1$
		amenityMap.put("fabrics", AmenityType.SHOP); //$NON-NLS-1$
		amenityMap.put("farm", AmenityType.SHOP); //$NON-NLS-1$
		amenityMap.put("florist", AmenityType.SHOP); //$NON-NLS-1$
		amenityMap.put("funeral_directors", AmenityType.SHOP); //$NON-NLS-1$
		amenityMap.put("furniture", AmenityType.SHOP); //$NON-NLS-1$
		amenityMap.put("garden_centre", AmenityType.SHOP); //$NON-NLS-1$
		amenityMap.put("general", AmenityType.SHOP); //$NON-NLS-1$
		amenityMap.put("gift", AmenityType.SHOP); //$NON-NLS-1$
		amenityMap.put("glaziery", AmenityType.SHOP); //$NON-NLS-1$
		amenityMap.put("greengrocer", AmenityType.SHOP); //$NON-NLS-1$
		amenityMap.put("hairdresser", AmenityType.SHOP); //$NON-NLS-1$
		amenityMap.put("hardware", AmenityType.SHOP); //$NON-NLS-1$
		amenityMap.put("hearing_aids", AmenityType.SHOP); //$NON-NLS-1$
		amenityMap.put("hifi", AmenityType.SHOP); //$NON-NLS-1$
		amenityMap.put("ice_cream", AmenityType.SHOP); //$NON-NLS-1$
		amenityMap.put("hardware", AmenityType.SHOP); //$NON-NLS-1$
		amenityMap.put("hearing_aids", AmenityType.SHOP); //$NON-NLS-1$
		amenityMap.put("hifi", AmenityType.SHOP); //$NON-NLS-1$
		amenityMap.put("ice_cream", AmenityType.SHOP); //$NON-NLS-1$
		amenityMap.put("jewelry", AmenityType.SHOP); //$NON-NLS-1$
		amenityMap.put("kiosk", AmenityType.SHOP); //$NON-NLS-1$
		amenityMap.put("laundry", AmenityType.SHOP); //$NON-NLS-1$
		amenityMap.put("mall", AmenityType.SHOP); //$NON-NLS-1$
		amenityMap.put("massage", AmenityType.SHOP); //$NON-NLS-1$
		amenityMap.put("money_lender", AmenityType.SHOP); //$NON-NLS-1$
		amenityMap.put("motorcycle", AmenityType.SHOP); //$NON-NLS-1$
		amenityMap.put("newsagent", AmenityType.SHOP); //$NON-NLS-1$
		amenityMap.put("optician", AmenityType.SHOP); //$NON-NLS-1$
		amenityMap.put("organic", AmenityType.SHOP); //$NON-NLS-1$
		amenityMap.put("outdoor", AmenityType.SHOP); //$NON-NLS-1$
		amenityMap.put("pawnbroker", AmenityType.SHOP); //$NON-NLS-1$
		amenityMap.put("second_hand", AmenityType.SHOP); //$NON-NLS-1$
		amenityMap.put("sports", AmenityType.SHOP); //$NON-NLS-1$
		amenityMap.put("stationery", AmenityType.SHOP); //$NON-NLS-1$
		amenityMap.put("supermarket", AmenityType.SHOP); //$NON-NLS-1$
		amenityMap.put("shoes", AmenityType.SHOP); //$NON-NLS-1$
		amenityMap.put("tattoo", AmenityType.SHOP); //$NON-NLS-1$
		amenityMap.put("toys", AmenityType.SHOP); //$NON-NLS-1$
		amenityMap.put("travel_agency", AmenityType.SHOP); //$NON-NLS-1$
		amenityMap.put("variety_store", AmenityType.SHOP); //$NON-NLS-1$
		amenityMap.put("video", AmenityType.SHOP); //$NON-NLS-1$
		
		
		amenityMap.put("dog_park", AmenityType.LEISURE); //$NON-NLS-1$
		amenityMap.put("sports_centre", AmenityType.LEISURE); //$NON-NLS-1$
		amenityMap.put("golf_course", AmenityType.LEISURE); //$NON-NLS-1$
		amenityMap.put("stadium", AmenityType.LEISURE); //$NON-NLS-1$
		amenityMap.put("track", AmenityType.LEISURE); //$NON-NLS-1$
		amenityMap.put("pitch", AmenityType.LEISURE); //$NON-NLS-1$
		amenityMap.put("water_park", AmenityType.LEISURE); //$NON-NLS-1$
		amenityMap.put("marina", AmenityType.LEISURE); //$NON-NLS-1$
		amenityMap.put("slipway", AmenityType.LEISURE); //$NON-NLS-1$
		amenityMap.put("fishing", AmenityType.LEISURE); //$NON-NLS-1$
		amenityMap.put("nature_reserve", AmenityType.LEISURE); //$NON-NLS-1$
		amenityMap.put("park", AmenityType.LEISURE); //$NON-NLS-1$
		amenityMap.put("playground", AmenityType.LEISURE); //$NON-NLS-1$
		amenityMap.put("garden", AmenityType.LEISURE); //$NON-NLS-1$
		amenityMap.put("common", AmenityType.LEISURE); //$NON-NLS-1$
		amenityMap.put("ice_rink", AmenityType.LEISURE); //$NON-NLS-1$
		amenityMap.put("miniature_golf", AmenityType.LEISURE); //$NON-NLS-1$
		amenityMap.put("dance", AmenityType.LEISURE); //$NON-NLS-1$
		
		
		amenityMap.put("9pin", AmenityType.SPORT); //$NON-NLS-1$
		amenityMap.put("10pin", AmenityType.SPORT); //$NON-NLS-1$
		amenityMap.put("archery", AmenityType.SPORT); //$NON-NLS-1$
		amenityMap.put("athletics", AmenityType.SPORT); //$NON-NLS-1$
		amenityMap.put("australian_football", AmenityType.SPORT); //$NON-NLS-1$
		amenityMap.put("baseball", AmenityType.SPORT); //$NON-NLS-1$
		amenityMap.put("basketball", AmenityType.SPORT); //$NON-NLS-1$
		amenityMap.put("beachvolleyball", AmenityType.SPORT); //$NON-NLS-1$
		amenityMap.put("boules", AmenityType.SPORT); //$NON-NLS-1$
		amenityMap.put("bowls", AmenityType.SPORT); //$NON-NLS-1$
		amenityMap.put("canoe", AmenityType.SPORT); //$NON-NLS-1$
		amenityMap.put("chess", AmenityType.SPORT); //$NON-NLS-1$
		amenityMap.put("climbing", AmenityType.SPORT); //$NON-NLS-1$
		amenityMap.put("cricket", AmenityType.SPORT); //$NON-NLS-1$
		amenityMap.put("cricket_nets", AmenityType.SPORT); //$NON-NLS-1$
		amenityMap.put("croquet", AmenityType.SPORT); //$NON-NLS-1$
		amenityMap.put("cycling", AmenityType.SPORT); //$NON-NLS-1$
		amenityMap.put("diving", AmenityType.SPORT); //$NON-NLS-1$
		amenityMap.put("dog_racing", AmenityType.SPORT); //$NON-NLS-1$
		amenityMap.put("equestrian", AmenityType.SPORT); //$NON-NLS-1$
		amenityMap.put("football", AmenityType.SPORT); //$NON-NLS-1$
		amenityMap.put("golf", AmenityType.SPORT); //$NON-NLS-1$
		amenityMap.put("gymnastics", AmenityType.SPORT); //$NON-NLS-1$
		amenityMap.put("hockey", AmenityType.SPORT); //$NON-NLS-1$
		amenityMap.put("horse_racing", AmenityType.SPORT); //$NON-NLS-1$
		amenityMap.put("ice_stock", AmenityType.SPORT); //$NON-NLS-1$
		amenityMap.put("korfball", AmenityType.SPORT); //$NON-NLS-1$
		amenityMap.put("motor", AmenityType.SPORT); //$NON-NLS-1$
		amenityMap.put("multi", AmenityType.SPORT); //$NON-NLS-1$
		amenityMap.put("orienteering", AmenityType.SPORT); //$NON-NLS-1$
		amenityMap.put("paddle_tennis", AmenityType.SPORT); //$NON-NLS-1$
		amenityMap.put("paragliding", AmenityType.SPORT); //$NON-NLS-1$
		amenityMap.put("pelota", AmenityType.SPORT); //$NON-NLS-1$
		amenityMap.put("racquet", AmenityType.SPORT); //$NON-NLS-1$
		amenityMap.put("rowing", AmenityType.SPORT); //$NON-NLS-1$
		amenityMap.put("rugby", AmenityType.SPORT); //$NON-NLS-1$
		amenityMap.put("shooting", AmenityType.SPORT); //$NON-NLS-1$
		amenityMap.put("skating", AmenityType.SPORT); //$NON-NLS-1$
		amenityMap.put("skateboard", AmenityType.SPORT); //$NON-NLS-1$
		amenityMap.put("skiing", AmenityType.SPORT); //$NON-NLS-1$
		amenityMap.put("soccer", AmenityType.SPORT); //$NON-NLS-1$
		amenityMap.put("swimming", AmenityType.SPORT); //$NON-NLS-1$
		amenityMap.put("table_tennis", AmenityType.SPORT); //$NON-NLS-1$
		amenityMap.put("team_handball", AmenityType.SPORT); //$NON-NLS-1$
		amenityMap.put("tennis", AmenityType.SPORT); //$NON-NLS-1$
		amenityMap.put("toboggan", AmenityType.SPORT); //$NON-NLS-1$
		amenityMap.put("volleyball", AmenityType.SPORT); //$NON-NLS-1$
		
		// amenity sub type
		amenityMap.put("place_of_worship", AmenityType.OTHER); //$NON-NLS-1$
		
		amenityMap.put("restaurant", AmenityType.SUSTENANCE); //$NON-NLS-1$
		amenityMap.put("food_court", AmenityType.SUSTENANCE); //$NON-NLS-1$
		amenityMap.put("fast_food", AmenityType.SUSTENANCE); //$NON-NLS-1$
		amenityMap.put("drinking_water", AmenityType.SUSTENANCE); //$NON-NLS-1$
		amenityMap.put("bbq", AmenityType.SUSTENANCE); //$NON-NLS-1$
		amenityMap.put("pub", AmenityType.SUSTENANCE); //$NON-NLS-1$
		amenityMap.put("bar", AmenityType.SUSTENANCE); //$NON-NLS-1$
		amenityMap.put("cafe", AmenityType.SUSTENANCE); //$NON-NLS-1$
		amenityMap.put("biergarten", AmenityType.SUSTENANCE); //$NON-NLS-1$
		
		amenityMap.put("kindergarten", AmenityType.EDUCATION); //$NON-NLS-1$
		amenityMap.put("school", AmenityType.EDUCATION); //$NON-NLS-1$
		amenityMap.put("college", AmenityType.EDUCATION); //$NON-NLS-1$
		amenityMap.put("library", AmenityType.EDUCATION); //$NON-NLS-1$
		amenityMap.put("university", AmenityType.EDUCATION); //$NON-NLS-1$

		amenityMap.put("ferry_terminal", AmenityType.TRANSPORTATION); //$NON-NLS-1$
		amenityMap.put("bicycle_parking", AmenityType.TRANSPORTATION); //$NON-NLS-1$
		amenityMap.put("bicycle_rental", AmenityType.TRANSPORTATION); //$NON-NLS-1$
		amenityMap.put("bus_station", AmenityType.TRANSPORTATION); //$NON-NLS-1$
		amenityMap.put("car_rental", AmenityType.TRANSPORTATION); //$NON-NLS-1$
		amenityMap.put("car_sharing", AmenityType.TRANSPORTATION); //$NON-NLS-1$
		amenityMap.put("fuel", AmenityType.TRANSPORTATION); //$NON-NLS-1$
		amenityMap.put("car_wash", AmenityType.TRANSPORTATION); //$NON-NLS-1$
		amenityMap.put("grit_bin", AmenityType.TRANSPORTATION); //$NON-NLS-1$
		amenityMap.put("parking", AmenityType.TRANSPORTATION); //$NON-NLS-1$
		amenityMap.put("taxi", AmenityType.TRANSPORTATION); //$NON-NLS-1$
		
		amenityMap.put("atm", AmenityType.FINANCE); //$NON-NLS-1$
		amenityMap.put("bank", AmenityType.FINANCE); //$NON-NLS-1$
		amenityMap.put("bureau_de_change", AmenityType.FINANCE); //$NON-NLS-1$
		
		amenityMap.put("pharmacy", AmenityType.HEALTHCARE); //$NON-NLS-1$
		amenityMap.put("hospital", AmenityType.HEALTHCARE); //$NON-NLS-1$
		amenityMap.put("baby_hatch", AmenityType.HEALTHCARE); //$NON-NLS-1$
		amenityMap.put("dentist", AmenityType.HEALTHCARE); //$NON-NLS-1$
		amenityMap.put("doctors", AmenityType.HEALTHCARE); //$NON-NLS-1$
		amenityMap.put("veterinary", AmenityType.HEALTHCARE); //$NON-NLS-1$
		amenityMap.put("first_aid", AmenityType.HEALTHCARE); //$NON-NLS-1$
		
		amenityMap.put("architect_office", AmenityType.ENTERTAINMENT); //$NON-NLS-1$
		amenityMap.put("arts_centre", AmenityType.ENTERTAINMENT); //$NON-NLS-1$
		amenityMap.put("cinema", AmenityType.ENTERTAINMENT); //$NON-NLS-1$
		amenityMap.put("community_centre", AmenityType.ENTERTAINMENT); //$NON-NLS-1$
		amenityMap.put("fountain", AmenityType.ENTERTAINMENT); //$NON-NLS-1$
		amenityMap.put("nightclub", AmenityType.ENTERTAINMENT); //$NON-NLS-1$
		amenityMap.put("stripclub", AmenityType.ENTERTAINMENT); //$NON-NLS-1$
		amenityMap.put("studio", AmenityType.ENTERTAINMENT); //$NON-NLS-1$
		amenityMap.put("theatre", AmenityType.ENTERTAINMENT); //$NON-NLS-1$
		amenityMap.put("sauna", AmenityType.ENTERTAINMENT); //$NON-NLS-1$
		amenityMap.put("brothel", AmenityType.ENTERTAINMENT); //$NON-NLS-1$
		
		amenityMap.put("bay", AmenityType.NATURAL); //$NON-NLS-1$
		amenityMap.put("beach", AmenityType.NATURAL); //$NON-NLS-1$
		amenityMap.put("cave_entrance", AmenityType.NATURAL); //$NON-NLS-1$
		amenityMap.put("cliff", AmenityType.NATURAL); //$NON-NLS-1$
		amenityMap.put("coastline", AmenityType.NATURAL); //$NON-NLS-1$
		amenityMap.put("fell", AmenityType.NATURAL); //$NON-NLS-1$
		amenityMap.put("glacier", AmenityType.NATURAL); //$NON-NLS-1$
		amenityMap.put("heath", AmenityType.NATURAL); //$NON-NLS-1$
		amenityMap.put("land", AmenityType.NATURAL); //$NON-NLS-1$
		amenityMap.put("heath", AmenityType.NATURAL); //$NON-NLS-1$
		amenityMap.put("marsh", AmenityType.NATURAL); //$NON-NLS-1$
		amenityMap.put("mud", AmenityType.NATURAL); //$NON-NLS-1$
		amenityMap.put("peak", AmenityType.NATURAL); //$NON-NLS-1$
		amenityMap.put("sand", AmenityType.NATURAL); //$NON-NLS-1$
		amenityMap.put("scree", AmenityType.NATURAL); //$NON-NLS-1$
		amenityMap.put("scrub", AmenityType.NATURAL); //$NON-NLS-1$
		amenityMap.put("spring", AmenityType.NATURAL); //$NON-NLS-1$
		amenityMap.put("stone", AmenityType.NATURAL); //$NON-NLS-1$
		amenityMap.put("tree", AmenityType.NATURAL); //$NON-NLS-1$
		amenityMap.put("volcano", AmenityType.NATURAL); //$NON-NLS-1$
		amenityMap.put("water", AmenityType.NATURAL); //$NON-NLS-1$
		amenityMap.put("wetland", AmenityType.NATURAL); //$NON-NLS-1$
		
		
		
		amenityMap.put("internet_access", AmenityType.OTHER); //$NON-NLS-1$
		amenityMap.put("bench", AmenityType.OTHER); //$NON-NLS-1$
		amenityMap.put("clock", AmenityType.OTHER); //$NON-NLS-1$
		amenityMap.put("courthouse", AmenityType.OTHER); //$NON-NLS-1$
		amenityMap.put("crematorium", AmenityType.OTHER); //$NON-NLS-1$
		amenityMap.put("embassy", AmenityType.OTHER); //$NON-NLS-1$
		amenityMap.put("emergency_phone", AmenityType.OTHER); //$NON-NLS-1$
		amenityMap.put("fire_hydrant", AmenityType.OTHER); //$NON-NLS-1$
		amenityMap.put("fire_station", AmenityType.OTHER); //$NON-NLS-1$
		amenityMap.put("grave_yard", AmenityType.OTHER); //$NON-NLS-1$
		amenityMap.put("hunting_stand", AmenityType.OTHER); //$NON-NLS-1$
		amenityMap.put("marketplace", AmenityType.OTHER); //$NON-NLS-1$
		amenityMap.put("police", AmenityType.OTHER); //$NON-NLS-1$
		amenityMap.put("post_box", AmenityType.OTHER); //$NON-NLS-1$
		amenityMap.put("post_office", AmenityType.OTHER); //$NON-NLS-1$
		amenityMap.put("prison", AmenityType.OTHER); //$NON-NLS-1$
		amenityMap.put("public_building", AmenityType.OTHER); //$NON-NLS-1$
		amenityMap.put("recycling", AmenityType.OTHER); //$NON-NLS-1$
		amenityMap.put("shelter", AmenityType.OTHER); //$NON-NLS-1$
		amenityMap.put("telephone", AmenityType.OTHER); //$NON-NLS-1$
		amenityMap.put("toilets", AmenityType.OTHER); //$NON-NLS-1$
		amenityMap.put("townhall", AmenityType.OTHER); //$NON-NLS-1$
		amenityMap.put("vending_machine", AmenityType.OTHER); //$NON-NLS-1$
		amenityMap.put("waste_basket", AmenityType.OTHER); //$NON-NLS-1$
		amenityMap.put("waste_disposal", AmenityType.OTHER); //$NON-NLS-1$
		
	}
	
}