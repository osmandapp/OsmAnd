package net.osmand.plus.render;

import java.util.LinkedHashMap;
import java.util.Map;

import net.osmand.plus.R;

public class RenderingIcons {
	
	private static Map<String, Integer> icons = new LinkedHashMap<String, Integer>();
	
	public static Map<String, Integer> getIcons(){
		if(icons.isEmpty()){
			initIcons();
		}
		return icons;
	}

	private static void initIcons() {
		icons.put("aerodrome", R.drawable.h_aerodrome); //$NON-NLS-1$
		icons.put("airport", R.drawable.h_airport); //$NON-NLS-1$
		icons.put("alpine_hut", R.drawable.h_alpine_hut); //$NON-NLS-1$
		icons.put("amenity_court", R.drawable.h_amenity_court); //$NON-NLS-1$
		icons.put("arts_centre", R.drawable.g_amenity_arts_centre); //$NON-NLS-1$
		icons.put("atm", R.drawable.h_atm); //$NON-NLS-1$
		icons.put("bank", R.drawable.h_bank); //$NON-NLS-1$
		icons.put("bar", R.drawable.h_bar); //$NON-NLS-1$
		icons.put("beach", R.drawable.h_beach); //$NON-NLS-1$
		icons.put("bench", R.drawable.g_amenity_bench); //$NON-NLS-1$
		icons.put("biergarten", R.drawable.h_biergarten); //$NON-NLS-1$
		icons.put("bicycle_rental", R.drawable.h_bicycle_rental); //$NON-NLS-1$
		icons.put("bollard", R.drawable.h_bollard); //$NON-NLS-1$
		icons.put("books", R.drawable.g_shop_books); //$NON-NLS-1$
		icons.put("bus_station", R.drawable.h_bus_station); //$NON-NLS-1$
		icons.put("bus_stop_small", R.drawable.h_bus_stop_small); //$NON-NLS-1$
		icons.put("bus_stop", R.drawable.h_bus_stop); //$NON-NLS-1$
		icons.put("cable_car", R.drawable.h_cable_car); //$NON-NLS-1$
		icons.put("cafe", R.drawable.h_cafe); //$NON-NLS-1$
		icons.put("camp_site", R.drawable.h_camp_site); //$NON-NLS-1$
		icons.put("car_sharing", R.drawable.h_car_sharing); //$NON-NLS-1$
		icons.put("caravan_park", R.drawable.h_caravan_park); //$NON-NLS-1$
		icons.put("casino", R.drawable.g_tourist_casino); //$NON-NLS-1$
		icons.put("castle", R.drawable.g_historic_castle); //$NON-NLS-1$
		icons.put("cave_entrance", R.drawable.h_cave_entrance); //$NON-NLS-1$
		icons.put("chair_lift", R.drawable.h_chair_lift); //$NON-NLS-1$
		icons.put("chalet", R.drawable.h_chalet); //$NON-NLS-1$
		icons.put("cinema", R.drawable.h_cinema); //$NON-NLS-1$
		icons.put("cliff", R.drawable.h_cliff); //$NON-NLS-1$
		icons.put("cliff2", R.drawable.h_cliff2); //$NON-NLS-1$
		icons.put("computer", R.drawable.g_shop_computer); //$NON-NLS-1$
		icons.put("confectionery", R.drawable.g_shop_confectionery); //$NON-NLS-1$
		icons.put("courthouse", R.drawable.g_amenity_courthouse); //$NON-NLS-1$
		icons.put("danger", R.drawable.h_danger); //$NON-NLS-1$
		icons.put("department_store", R.drawable.h_department_store); //$NON-NLS-1$
		icons.put("do_it_yourself", R.drawable.g_shop_diy); //$NON-NLS-1$
		icons.put("drinking_water", R.drawable.h_drinking_water); //$NON-NLS-1$
		icons.put("embassy", R.drawable.h_embassy); //$NON-NLS-1$
		icons.put("emergency_phone", R.drawable.h_emergency_phone); //$NON-NLS-1$
		icons.put("entrance", R.drawable.g_barrier_entrance); //$NON-NLS-1$
		icons.put("exchange", R.drawable.g_money_exchange); //$NON-NLS-1$
		icons.put("fast_food", R.drawable.h_fast_food); //$NON-NLS-1$
		icons.put("fire_station", R.drawable.h_fire_station); //$NON-NLS-1$
		icons.put("forest", R.drawable.h_forest); //$NON-NLS-1$
		icons.put("fuel", R.drawable.h_fuel); //$NON-NLS-1$
		icons.put("gate2", R.drawable.h_gate2); //$NON-NLS-1$
		icons.put("geocache_found", R.drawable.h_geocache_found); //$NON-NLS-1$
		icons.put("geocache_not_found", R.drawable.h_geocache_not_found); //$NON-NLS-1$
		icons.put("guest_house", R.drawable.h_guest_house); //$NON-NLS-1$
		icons.put("glacier", R.drawable.h_glacier); //$NON-NLS-1$
		icons.put("grave_yard", R.drawable.h_grave_yard); //$NON-NLS-1$
		icons.put("guest_house", R.drawable.h_guest_house); //$NON-NLS-1$
		icons.put("florist", R.drawable.h_florist); //$NON-NLS-1$
		icons.put("fountain", R.drawable.g_amenity_fountain); //$NON-NLS-1$
		icons.put("garden_centre", R.drawable.g_shop_garden_centre); //$NON-NLS-1$
		icons.put("halt", R.drawable.h_halt); //$NON-NLS-1$
		icons.put("helipad", R.drawable.h_helipad); //$NON-NLS-1$
		icons.put("hospital", R.drawable.h_hospital); //$NON-NLS-1$
		icons.put("hostel", R.drawable.h_hostel); //$NON-NLS-1$
		icons.put("hotel", R.drawable.h_hotel); //$NON-NLS-1$
		icons.put("ice_cream", R.drawable.g_food_ice_cream); //$NON-NLS-1$
		icons.put("information", R.drawable.h_information); //$NON-NLS-1$
		icons.put("jewelry", R.drawable.g_shop_jewelry); //$NON-NLS-1$
		icons.put("kiosk", R.drawable.g_shop_kiosk); //$NON-NLS-1$
		icons.put("level_crossing", R.drawable.h_level_crossing); //$NON-NLS-1$
		icons.put("level_crossing2", R.drawable.h_level_crossing2); //$NON-NLS-1$
		icons.put("library", R.drawable.h_library); //$NON-NLS-1$
		icons.put("liftgate", R.drawable.h_liftgate); //$NON-NLS-1$
		icons.put("lighthouse", R.drawable.h_lighthouse); //$NON-NLS-1$
		icons.put("lock_gate", R.drawable.h_lock_gate); //$NON-NLS-1$
		icons.put("marketplace", R.drawable.g_amenity_marketplace); //$NON-NLS-1$
		icons.put("marsh", R.drawable.h_marsh); //$NON-NLS-1$
		icons.put("mast", R.drawable.h_mast); //$NON-NLS-1$
		icons.put("memorial", R.drawable.h_memorial); //$NON-NLS-1$
		icons.put("motel", R.drawable.h_motel); //$NON-NLS-1$
		icons.put("mini_roundabout", R.drawable.h_mini_roundabout); //$NON-NLS-1$
		icons.put("mud", R.drawable.h_mud); //$NON-NLS-1$
		icons.put("museum", R.drawable.h_museum); //$NON-NLS-1$
		icons.put("music", R.drawable.g_shop_music); //$NON-NLS-1$
		icons.put("nr", R.drawable.h_nr); //$NON-NLS-1$
		icons.put("optician", R.drawable.g_health_optician); //$NON-NLS-1$
		icons.put("orchard", R.drawable.h_orchard); //$NON-NLS-1$
		icons.put("parking", R.drawable.h_parking); //$NON-NLS-1$
		icons.put("peak", R.drawable.h_peak); //$NON-NLS-1$
		icons.put("shop_pet", R.drawable.g_shop_pet); //$NON-NLS-1$
		icons.put("pharmacy", R.drawable.h_pharmacy); //$NON-NLS-1$
		icons.put("picnic_site", R.drawable.h_picnic_site); //$NON-NLS-1$
		icons.put("place_of_worship", R.drawable.h_place_of_worship); //$NON-NLS-1$
		icons.put("playground", R.drawable.h_playground); //$NON-NLS-1$
		icons.put("police", R.drawable.h_police); //$NON-NLS-1$
		icons.put("postbox", R.drawable.h_postbox); //$NON-NLS-1$
		icons.put("postoffice", R.drawable.h_postoffice); //$NON-NLS-1$
		icons.put("power_tower", R.drawable.h_power_tower); //$NON-NLS-1$
		icons.put("power_wind", R.drawable.h_power_wind); //$NON-NLS-1$
		icons.put("prison", R.drawable.h_prison); //$NON-NLS-1$
		icons.put("pub", R.drawable.h_pub); //$NON-NLS-1$
		icons.put("quarry2", R.drawable.h_quarry2); //$NON-NLS-1$
		icons.put("recycling", R.drawable.h_recycling); //$NON-NLS-1$
		icons.put("restaurant", R.drawable.h_restaurant); //$NON-NLS-1$
		icons.put("school", R.drawable.h_school); //$NON-NLS-1$
		icons.put("scrub", R.drawable.h_scrub); //$NON-NLS-1$
		icons.put("shelter", R.drawable.h_shelter); //$NON-NLS-1$
		icons.put("shop_bakery", R.drawable.h_shop_bakery); //$NON-NLS-1$
		icons.put("shop_butcher", R.drawable.h_shop_butcher); //$NON-NLS-1$
		icons.put("shop_clothes", R.drawable.h_shop_clothes); //$NON-NLS-1$
		icons.put("shop_convenience", R.drawable.h_shop_convenience); //$NON-NLS-1$
		icons.put("shop_diy", R.drawable.h_shop_diy); //$NON-NLS-1$
		icons.put("shop_bicycle", R.drawable.h_shop_bicycle); //$NON-NLS-1$
		icons.put("shop_car", R.drawable.h_shop_car); //$NON-NLS-1$
		icons.put("shop_car_repair", R.drawable.h_shop_car_repair); //$NON-NLS-1$
		icons.put("shop_hairdresser", R.drawable.h_shop_hairdresser); //$NON-NLS-1$
		icons.put("shop_supermarket", R.drawable.h_shop_supermarket); //$NON-NLS-1$
		icons.put("slipway", R.drawable.h_slipway); //$NON-NLS-1$
		icons.put("sports_centre", R.drawable.g_leisure_sports_centre); //$NON-NLS-1$
		icons.put("spring", R.drawable.h_spring); //$NON-NLS-1$
		icons.put("station_small", R.drawable.h_station_small); //$NON-NLS-1$
		icons.put("station", R.drawable.h_station); //$NON-NLS-1$
		icons.put("subway_entrance", R.drawable.h_subway_entrance); //$NON-NLS-1$
		icons.put("taxi", R.drawable.g_transport_taxi); //$NON-NLS-1$
		icons.put("telephone", R.drawable.h_telephone); //$NON-NLS-1$
		icons.put("theatre", R.drawable.h_theatre); //$NON-NLS-1$
		icons.put("toilets", R.drawable.h_toilets); //$NON-NLS-1$
		icons.put("toys", R.drawable.g_shop_toys); //$NON-NLS-1$
		icons.put("traffic_light", R.drawable.h_traffic_light); //$NON-NLS-1$
		icons.put("highway_ford", R.drawable.h_highway_ford); //$NON-NLS-1$
		icons.put("tree", R.drawable.h_tree); //$NON-NLS-1$
		icons.put("tree2", R.drawable.h_tree2); //$NON-NLS-1$
		icons.put("university", R.drawable.g_amenity_university); //$NON-NLS-1$
		icons.put("vending_machine", R.drawable.g_amenity_vending_machine); //$NON-NLS-1$
		icons.put("viewpoint", R.drawable.h_viewpoint); //$NON-NLS-1$
		icons.put("vineyard", R.drawable.h_vineyard); //$NON-NLS-1$
		icons.put("volcano", R.drawable.h_volcano); //$NON-NLS-1$
		icons.put("water_tower", R.drawable.h_water_tower); //$NON-NLS-1$
		icons.put("windmill", R.drawable.h_windmill); //$NON-NLS-1$
		icons.put("zoo", R.drawable.h_zoo); //$NON-NLS-1$
		
		icons.put("mot_shield1", R.drawable.mot_shield1); //$NON-NLS-1$
		icons.put("mot_shield2", R.drawable.mot_shield2); //$NON-NLS-1$
		icons.put("mot_shield3", R.drawable.mot_shield3); //$NON-NLS-1$
		icons.put("mot_shield4", R.drawable.mot_shield4); //$NON-NLS-1$
		icons.put("mot_shield5", R.drawable.mot_shield5); //$NON-NLS-1$
		icons.put("mot_shield6", R.drawable.mot_shield6); //$NON-NLS-1$
		icons.put("mot_shield7", R.drawable.mot_shield7); //$NON-NLS-1$
		icons.put("mot_shield8", R.drawable.mot_shield8); //$NON-NLS-1$
		icons.put("pri_shield1", R.drawable.pri_shield1); //$NON-NLS-1$
		icons.put("pri_shield2", R.drawable.pri_shield2); //$NON-NLS-1$
		icons.put("pri_shield3", R.drawable.pri_shield3); //$NON-NLS-1$
		icons.put("pri_shield4", R.drawable.pri_shield4); //$NON-NLS-1$
		icons.put("pri_shield5", R.drawable.pri_shield5); //$NON-NLS-1$
		icons.put("pri_shield6", R.drawable.pri_shield6); //$NON-NLS-1$
		icons.put("pri_shield7", R.drawable.pri_shield7); //$NON-NLS-1$
		icons.put("pri_shield8", R.drawable.pri_shield8); //$NON-NLS-1$
		icons.put("sec_shield1", R.drawable.sec_shield1); //$NON-NLS-1$
		icons.put("sec_shield2", R.drawable.sec_shield2); //$NON-NLS-1$
		icons.put("sec_shield3", R.drawable.sec_shield3); //$NON-NLS-1$
		icons.put("sec_shield4", R.drawable.sec_shield4); //$NON-NLS-1$
		icons.put("sec_shield5", R.drawable.sec_shield5); //$NON-NLS-1$
		icons.put("sec_shield6", R.drawable.sec_shield6); //$NON-NLS-1$
		icons.put("sec_shield7", R.drawable.sec_shield7); //$NON-NLS-1$
		icons.put("sec_shield8", R.drawable.sec_shield8); //$NON-NLS-1$
		icons.put("ter_shield1", R.drawable.ter_shield1); //$NON-NLS-1$
		icons.put("ter_shield2", R.drawable.ter_shield2); //$NON-NLS-1$
		icons.put("ter_shield3", R.drawable.ter_shield3); //$NON-NLS-1$
		icons.put("ter_shield4", R.drawable.ter_shield4); //$NON-NLS-1$
		icons.put("ter_shield5", R.drawable.ter_shield5); //$NON-NLS-1$
		icons.put("ter_shield6", R.drawable.ter_shield6); //$NON-NLS-1$
		icons.put("ter_shield7", R.drawable.ter_shield7); //$NON-NLS-1$
		icons.put("ter_shield8", R.drawable.ter_shield8); //$NON-NLS-1$
		icons.put("tru_shield1", R.drawable.tru_shield1); //$NON-NLS-1$
		icons.put("tru_shield2", R.drawable.tru_shield2); //$NON-NLS-1$
		icons.put("tru_shield3", R.drawable.tru_shield3); //$NON-NLS-1$
		icons.put("tru_shield4", R.drawable.tru_shield4); //$NON-NLS-1$
		icons.put("tru_shield5", R.drawable.tru_shield5); //$NON-NLS-1$
		icons.put("tru_shield6", R.drawable.tru_shield6); //$NON-NLS-1$
		icons.put("tru_shield7", R.drawable.tru_shield7); //$NON-NLS-1$
		icons.put("tru_shield8", R.drawable.tru_shield8); //$NON-NLS-1$
	}

}
