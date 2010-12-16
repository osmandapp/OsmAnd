package net.osmand.render;

import java.util.LinkedHashMap;
import java.util.Map;

import net.osmand.R;

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
		icons.put("alpinehut", R.drawable.h_alpinehut); //$NON-NLS-1$
		icons.put("atm", R.drawable.h_atm); //$NON-NLS-1$
		icons.put("bank", R.drawable.h_bank); //$NON-NLS-1$
		icons.put("bar", R.drawable.h_bar); //$NON-NLS-1$
		icons.put("beach", R.drawable.h_beach); //$NON-NLS-1$
		icons.put("bollard", R.drawable.h_bollard); //$NON-NLS-1$
		icons.put("bus_station", R.drawable.h_bus_station); //$NON-NLS-1$
		icons.put("bus_stop_small", R.drawable.h_bus_stop_small); //$NON-NLS-1$
		icons.put("bus_stop", R.drawable.h_bus_stop); //$NON-NLS-1$
		icons.put("cable_car", R.drawable.h_cable_car); //$NON-NLS-1$
		icons.put("cafe", R.drawable.h_cafe); //$NON-NLS-1$
		icons.put("camp_site", R.drawable.h_camp_site); //$NON-NLS-1$
		icons.put("car_share", R.drawable.h_car_share); //$NON-NLS-1$
		icons.put("caravan_park", R.drawable.h_caravan_park); //$NON-NLS-1$
		icons.put("cave_entrance", R.drawable.h_cave_entrance); //$NON-NLS-1$
		icons.put("chair_lift", R.drawable.h_chair_lift); //$NON-NLS-1$
		icons.put("cinema", R.drawable.h_cinema); //$NON-NLS-1$
		icons.put("cliff", R.drawable.h_cliff); //$NON-NLS-1$
		icons.put("cliff2", R.drawable.h_cliff2); //$NON-NLS-1$
		icons.put("danger", R.drawable.h_danger); //$NON-NLS-1$
		icons.put("department_store", R.drawable.h_department_store); //$NON-NLS-1$
		icons.put("embassy", R.drawable.h_embassy); //$NON-NLS-1$
		icons.put("fast_food", R.drawable.h_fast_food); //$NON-NLS-1$
		icons.put("firestation", R.drawable.h_firestation); //$NON-NLS-1$
		icons.put("food_drinkingtap", R.drawable.h_food_drinkingtap); //$NON-NLS-1$
		icons.put("forest", R.drawable.h_forest); //$NON-NLS-1$
		icons.put("fuel", R.drawable.h_fuel); //$NON-NLS-1$
		icons.put("gate2", R.drawable.h_gate2); //$NON-NLS-1$
		icons.put("geocache_found", R.drawable.h_geocache_found); //$NON-NLS-1$
		icons.put("geocache_not_found", R.drawable.h_geocache_not_found); //$NON-NLS-1$
		icons.put("guest_house", R.drawable.h_guest_house); //$NON-NLS-1$
		icons.put("glacier", R.drawable.h_glacier); //$NON-NLS-1$
		icons.put("grave_yard", R.drawable.h_grave_yard); //$NON-NLS-1$
		icons.put("guest_house", R.drawable.h_guest_house); //$NON-NLS-1$
		icons.put("halt", R.drawable.h_halt); //$NON-NLS-1$
		icons.put("hospital", R.drawable.h_hospital); //$NON-NLS-1$
		icons.put("hostel", R.drawable.h_hostel); //$NON-NLS-1$
		icons.put("hotel", R.drawable.h_hotel); //$NON-NLS-1$
		icons.put("level_crossing", R.drawable.h_level_crossing); //$NON-NLS-1$
		icons.put("level_crossing2", R.drawable.h_level_crossing2); //$NON-NLS-1$
		icons.put("library", R.drawable.h_library); //$NON-NLS-1$
		icons.put("liftgate", R.drawable.h_liftgate); //$NON-NLS-1$
		icons.put("lighthouse", R.drawable.h_lighthouse); //$NON-NLS-1$
		icons.put("lock_gate", R.drawable.h_lock_gate); //$NON-NLS-1$
		icons.put("marsh", R.drawable.h_marsh); //$NON-NLS-1$
		icons.put("mast", R.drawable.h_mast); //$NON-NLS-1$
		icons.put("memorial", R.drawable.h_memorial); //$NON-NLS-1$
		icons.put("mini_round", R.drawable.h_mini_round); //$NON-NLS-1$
		icons.put("mud", R.drawable.h_mud); //$NON-NLS-1$
		icons.put("museum", R.drawable.h_museum); //$NON-NLS-1$
		icons.put("nr", R.drawable.h_nr); //$NON-NLS-1$
		icons.put("orchard", R.drawable.h_orchard); //$NON-NLS-1$
		icons.put("parking", R.drawable.h_parking); //$NON-NLS-1$
		icons.put("peak", R.drawable.h_peak); //$NON-NLS-1$
		icons.put("pharmacy", R.drawable.h_pharmacy); //$NON-NLS-1$
		icons.put("place_of_worship", R.drawable.h_place_of_worship); //$NON-NLS-1$
		icons.put("police", R.drawable.h_police); //$NON-NLS-1$
		icons.put("postbox", R.drawable.h_postbox); //$NON-NLS-1$
		icons.put("postoffice", R.drawable.h_postoffice); //$NON-NLS-1$
		icons.put("power_tower", R.drawable.h_power_tower); //$NON-NLS-1$
		icons.put("power_wind", R.drawable.h_power_wind); //$NON-NLS-1$
		icons.put("prison", R.drawable.h_prison); //$NON-NLS-1$
		icons.put("pub", R.drawable.h_pub); //$NON-NLS-1$
		icons.put("quarry2", R.drawable.h_quarry2); //$NON-NLS-1$
		icons.put("recycling", R.drawable.h_recycling); //$NON-NLS-1$
		icons.put("rental_bicycle", R.drawable.h_rental_bicycle); //$NON-NLS-1$
		icons.put("restaurant", R.drawable.h_restaurant); //$NON-NLS-1$
		icons.put("school", R.drawable.h_school); //$NON-NLS-1$
		icons.put("scrub", R.drawable.h_scrub); //$NON-NLS-1$
		icons.put("shelter", R.drawable.h_shelter); //$NON-NLS-1$
		icons.put("shop_bakery", R.drawable.h_shop_bakery); //$NON-NLS-1$
		icons.put("shop_butcher", R.drawable.h_shop_butcher); //$NON-NLS-1$
		icons.put("shop_clothes", R.drawable.h_shop_clothes); //$NON-NLS-1$
		icons.put("shop_convenience", R.drawable.h_shop_convenience); //$NON-NLS-1$
		icons.put("shop_diy", R.drawable.h_shop_diy); //$NON-NLS-1$
		icons.put("shop_hairdresser", R.drawable.h_shop_hairdresser); //$NON-NLS-1$
		icons.put("shop_supermarket", R.drawable.h_shop_supermarket); //$NON-NLS-1$
		icons.put("sosphone", R.drawable.h_sosphone); //$NON-NLS-1$
		icons.put("spring", R.drawable.h_spring); //$NON-NLS-1$
		icons.put("station_small", R.drawable.h_station_small); //$NON-NLS-1$
		icons.put("station", R.drawable.h_station); //$NON-NLS-1$
		icons.put("telephone", R.drawable.h_telephone); //$NON-NLS-1$
		icons.put("theatre", R.drawable.h_theatre); //$NON-NLS-1$
		icons.put("toilets", R.drawable.h_toilets); //$NON-NLS-1$
		icons.put("tower_water", R.drawable.h_tower_water); //$NON-NLS-1$
		icons.put("traffic_light", R.drawable.h_traffic_light); //$NON-NLS-1$
		icons.put("transport_ford", R.drawable.h_transport_ford); //$NON-NLS-1$
		icons.put("tree", R.drawable.h_tree); //$NON-NLS-1$
		icons.put("tree2", R.drawable.h_tree2); //$NON-NLS-1$
		icons.put("view_point", R.drawable.h_view_point); //$NON-NLS-1$
		icons.put("vineyard", R.drawable.h_vineyard); //$NON-NLS-1$
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
