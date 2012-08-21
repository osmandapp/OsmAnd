package net.osmand.plus;

import java.lang.Comparable;
import java.util.HashMap;
import java.util.Map;

import net.osmand.data.Amenity;
import net.osmand.data.AmenityType;

public class AmenityTypeIcons {

    static final int DEFAULT_ICON_RESOURCE = R.drawable.poi;
    static AmenityTypeIcons singleton;
    Map<AmenityTypeKey, Integer> iconMap;
    
    public static int getIconResource(Amenity amenity) {
        return singleton.getIcon(amenity);
    }
    
    static {
        singleton = new AmenityTypeIcons();
    }

    private AmenityTypeIcons() {
        iconMap = new HashMap<AmenityTypeKey, Integer>();
        
        iconMap.put(new AmenityTypeKey(AmenityType.SUSTENANCE, "restaurant"), R.drawable.h_restaurant);
        iconMap.put(new AmenityTypeKey(AmenityType.SUSTENANCE, "cafe"), R.drawable.h_cafe);
        iconMap.put(new AmenityTypeKey(AmenityType.SUSTENANCE, "fast_food"), R.drawable.h_fast_food);
        iconMap.put(new AmenityTypeKey(AmenityType.SUSTENANCE, "pub"), R.drawable.h_pub);
        iconMap.put(new AmenityTypeKey(AmenityType.SUSTENANCE, "bar"), R.drawable.h_bar);
        iconMap.put(new AmenityTypeKey(AmenityType.SUSTENANCE, "biergarten"), R.drawable.h_biergarten);
        iconMap.put(new AmenityTypeKey(AmenityType.SUSTENANCE, "drinking_water"), R.drawable.h_drinking_water);

        iconMap.put(new AmenityTypeKey(AmenityType.EDUCATION, "school"), R.drawable.h_school);
        iconMap.put(new AmenityTypeKey(AmenityType.EDUCATION, "college"), R.drawable.h_school);
        iconMap.put(new AmenityTypeKey(AmenityType.EDUCATION, "library"), R.drawable.h_library);
        iconMap.put(new AmenityTypeKey(AmenityType.EDUCATION, "kindergarten"), R.drawable.h_kindergarten);

        iconMap.put(new AmenityTypeKey(AmenityType.TRANSPORTATION, "fuel"), R.drawable.h_fuel);
        iconMap.put(new AmenityTypeKey(AmenityType.TRANSPORTATION, "bus_station"), R.drawable.h_bus_station);
        iconMap.put(new AmenityTypeKey(AmenityType.TRANSPORTATION, "bus_stop"), R.drawable.h_bus_stop);
        iconMap.put(new AmenityTypeKey(AmenityType.TRANSPORTATION, "station"), R.drawable.h_station);
        iconMap.put(new AmenityTypeKey(AmenityType.TRANSPORTATION, "halt"), R.drawable.h_halt);
        iconMap.put(new AmenityTypeKey(AmenityType.TRANSPORTATION, "tram_stop"), R.drawable.h_halt);
        iconMap.put(new AmenityTypeKey(AmenityType.TRANSPORTATION, "level_crossing"), R.drawable.h_level_crossing);
        iconMap.put(new AmenityTypeKey(AmenityType.TRANSPORTATION, "subway_entrance"), R.drawable.h_subway_entrance);
        iconMap.put(new AmenityTypeKey(AmenityType.TRANSPORTATION, "helipad"), R.drawable.h_helipad);
        iconMap.put(new AmenityTypeKey(AmenityType.TRANSPORTATION, "aerodrome"), R.drawable.h_aerodrome);
        iconMap.put(new AmenityTypeKey(AmenityType.TRANSPORTATION, "airport"), R.drawable.h_airport);
        iconMap.put(new AmenityTypeKey(AmenityType.TRANSPORTATION, "bicycle_rental"), R.drawable.h_bicycle_rental);
        iconMap.put(new AmenityTypeKey(AmenityType.TRANSPORTATION, "car_sharing"), R.drawable.h_car_sharing);

        iconMap.put(new AmenityTypeKey(AmenityType.FINANCE, "atm"), R.drawable.h_atm);
        iconMap.put(new AmenityTypeKey(AmenityType.FINANCE, "bank"), R.drawable.h_bank);
        
        iconMap.put(new AmenityTypeKey(AmenityType.HEALTHCARE, "pharmacy"), R.drawable.h_pharmacy);
        iconMap.put(new AmenityTypeKey(AmenityType.HEALTHCARE, "hospital"), R.drawable.h_hospital);
        iconMap.put(new AmenityTypeKey(AmenityType.HEALTHCARE, "veterinary"), R.drawable.h_veterinary);
        iconMap.put(new AmenityTypeKey(AmenityType.HEALTHCARE, "doctors"), R.drawable.h_doctors);
        iconMap.put(new AmenityTypeKey(AmenityType.HEALTHCARE, "dentist"), R.drawable.h_dentist);

        iconMap.put(new AmenityTypeKey(AmenityType.ENTERTAINMENT, "cinema"), R.drawable.h_cinema);
        iconMap.put(new AmenityTypeKey(AmenityType.ENTERTAINMENT, "theatre"), R.drawable.h_theatre);

        iconMap.put(new AmenityTypeKey(AmenityType.TOURISM, "camp_site"), R.drawable.h_camp_site);
        iconMap.put(new AmenityTypeKey(AmenityType.TOURISM, "caravan_site"), R.drawable.h_caravan_site);
        iconMap.put(new AmenityTypeKey(AmenityType.TOURISM, "picnic_site"), R.drawable.h_picnic_site);
        iconMap.put(new AmenityTypeKey(AmenityType.TOURISM, "alpine_hut"), R.drawable.h_alpine_hut);
        iconMap.put(new AmenityTypeKey(AmenityType.TOURISM, "chalet"), R.drawable.h_chalet);
        iconMap.put(new AmenityTypeKey(AmenityType.TOURISM, "guest_house"), R.drawable.h_guest_house);
        iconMap.put(new AmenityTypeKey(AmenityType.TOURISM, "hostel"), R.drawable.h_hostel);
        iconMap.put(new AmenityTypeKey(AmenityType.TOURISM, "hotel"), R.drawable.h_hotel);
        iconMap.put(new AmenityTypeKey(AmenityType.TOURISM, "motel"), R.drawable.h_motel);
        iconMap.put(new AmenityTypeKey(AmenityType.TOURISM, "museum"), R.drawable.h_museum);
        iconMap.put(new AmenityTypeKey(AmenityType.TOURISM, "information"), R.drawable.h_information);
        iconMap.put(new AmenityTypeKey(AmenityType.TOURISM, "viewpoint"), R.drawable.h_viewpoint);
        iconMap.put(new AmenityTypeKey(AmenityType.TOURISM, "theme_park"), R.drawable.h_theme_park);

        iconMap.put(new AmenityTypeKey(AmenityType.HISTORIC, "castle"), R.drawable.h_historic_castle);
        iconMap.put(new AmenityTypeKey(AmenityType.HISTORIC, "monument"), R.drawable.h_historic_monument);
        iconMap.put(new AmenityTypeKey(AmenityType.HISTORIC, "memorial"), R.drawable.h_historic_memorial);
        iconMap.put(new AmenityTypeKey(AmenityType.HISTORIC, "battlefield"), R.drawable.h_historic_battlefield);
        iconMap.put(new AmenityTypeKey(AmenityType.HISTORIC, "fort"), R.drawable.h_viewpoint);
        iconMap.put(new AmenityTypeKey(AmenityType.HISTORIC, "ruins"), R.drawable.h_historic_ruins);
        iconMap.put(new AmenityTypeKey(AmenityType.HISTORIC, "archaeological_site"), R.drawable.h_historic_archaeological_site);
        iconMap.put(new AmenityTypeKey(AmenityType.HISTORIC, "wreck"), R.drawable.h_historic_wreck);
        iconMap.put(new AmenityTypeKey(AmenityType.HISTORIC, "wayside_cross"), R.drawable.h_historic_wayside_cross);
        iconMap.put(new AmenityTypeKey(AmenityType.HISTORIC, "wayside_shrine"), R.drawable.h_historic_wayside_shrine);
        iconMap.put(new AmenityTypeKey(AmenityType.HISTORIC, "boundary_stone"), R.drawable.h_viewpoint);

        iconMap.put(new AmenityTypeKey(AmenityType.NATURAL, "peak"), R.drawable.h_peak);
        iconMap.put(new AmenityTypeKey(AmenityType.NATURAL, "cave_entrance"), R.drawable.h_cave_entrance);
        iconMap.put(new AmenityTypeKey(AmenityType.NATURAL, "spring"), R.drawable.h_spring);
        iconMap.put(new AmenityTypeKey(AmenityType.NATURAL, "tree"), R.drawable.h_tree);
        iconMap.put(new AmenityTypeKey(AmenityType.NATURAL, "volcano"), R.drawable.h_volcano);

        iconMap.put(new AmenityTypeKey(AmenityType.SHOP, "bakery"), R.drawable.h_shop_bakery);
        iconMap.put(new AmenityTypeKey(AmenityType.SHOP, "butcher"), R.drawable.h_shop_butcher);
        iconMap.put(new AmenityTypeKey(AmenityType.SHOP, "clothes"), R.drawable.h_shop_clothes);
        iconMap.put(new AmenityTypeKey(AmenityType.SHOP, "fashion"), R.drawable.h_shop_clothes);
        iconMap.put(new AmenityTypeKey(AmenityType.SHOP, "boutique"), R.drawable.h_shop_clothes);
        iconMap.put(new AmenityTypeKey(AmenityType.SHOP, "bicycle"), R.drawable.h_shop_bicycle);
        iconMap.put(new AmenityTypeKey(AmenityType.SHOP, "car"), R.drawable.h_shop_car);
        iconMap.put(new AmenityTypeKey(AmenityType.SHOP, "car_repair"), R.drawable.h_shop_car_repair);
        iconMap.put(new AmenityTypeKey(AmenityType.SHOP, "convenience"), R.drawable.h_shop_convenience);
        iconMap.put(new AmenityTypeKey(AmenityType.SHOP, "department_store"), R.drawable.h_department_store);
        iconMap.put(new AmenityTypeKey(AmenityType.SHOP, "electronics"), R.drawable.h_shop_electronics);
        iconMap.put(new AmenityTypeKey(AmenityType.SHOP, "general"), R.drawable.h_shop_supermarket);
        iconMap.put(new AmenityTypeKey(AmenityType.SHOP, "outdoor"), R.drawable.h_shop_outdoor);
        iconMap.put(new AmenityTypeKey(AmenityType.LEISURE, "fishing"), R.drawable.h_shop_outdoor);
        iconMap.put(new AmenityTypeKey(AmenityType.SHOP, "florist"), R.drawable.h_florist);
        iconMap.put(new AmenityTypeKey(AmenityType.SHOP, "hairdresser"), R.drawable.h_shop_hairdresser);
        iconMap.put(new AmenityTypeKey(AmenityType.SHOP, "mall"), R.drawable.h_department_store);
        iconMap.put(new AmenityTypeKey(AmenityType.SHOP, "supermarket"), R.drawable.h_shop_supermarket);
        iconMap.put(new AmenityTypeKey(AmenityType.SHOP, "alcohol"), R.drawable.h_shop_alcohol);
        iconMap.put(new AmenityTypeKey(AmenityType.SHOP, "kiosk"), R.drawable.g_shop_kiosk);
        iconMap.put(new AmenityTypeKey(AmenityType.SHOP, "musical_instrument"), R.drawable.g_shop_musical_instrument);
        iconMap.put(new AmenityTypeKey(AmenityType.SHOP, "optician"), R.drawable.h_optician);
        iconMap.put(new AmenityTypeKey(AmenityType.SHOP, "bureau_de_change"), R.drawable.h_bureau_de_change);
        iconMap.put(new AmenityTypeKey(AmenityType.SHOP, "video"), R.drawable.h_shop_video);
        iconMap.put(new AmenityTypeKey(AmenityType.SHOP, "ice_cream"), R.drawable.g_food_ice_cream);
        iconMap.put(new AmenityTypeKey(AmenityType.SHOP, "confectionery"), R.drawable.g_shop_confectionery);
        iconMap.put(new AmenityTypeKey(AmenityType.SHOP, "laundry"), R.drawable.h_laundry);
        iconMap.put(new AmenityTypeKey(AmenityType.SHOP, "tobacco"), R.drawable.h_shop_tobacco);
        iconMap.put(new AmenityTypeKey(AmenityType.SHOP, "motorcycle"), R.drawable.h_shop_motorcycle);
        iconMap.put(new AmenityTypeKey(AmenityType.SHOP, "hardware"), R.drawable.h_shop_diy);
        iconMap.put(new AmenityTypeKey(AmenityType.SHOP, "copyshop"), R.drawable.h_shop_copyshop);
        iconMap.put(new AmenityTypeKey(AmenityType.SHOP, "greengrocer"), R.drawable.h_shop_greengrocer);
        iconMap.put(new AmenityTypeKey(AmenityType.SHOP, "mobile_phone"), R.drawable.h_shop_mobile_phone);
        iconMap.put(new AmenityTypeKey(AmenityType.SHOP, "vending_machine"), R.drawable.h_vending_machine);

        iconMap.put(new AmenityTypeKey(AmenityType.LEISURE, "playground"), R.drawable.h_playground);
        iconMap.put(new AmenityTypeKey(AmenityType.LEISURE, "water_park"), R.drawable.h_leisure_water_park);
        iconMap.put(new AmenityTypeKey(AmenityType.LEISURE, "leisure_sports_centre"), R.drawable.h_sports_centre);
        iconMap.put(new AmenityTypeKey(AmenityType.LEISURE, "bird_hide"), R.drawable.h_bird_hide);

        iconMap.put(new AmenityTypeKey(AmenityType.SPORT, "horse_racing"), R.drawable.h_horse_racing);
        iconMap.put(new AmenityTypeKey(AmenityType.SPORT, "soccer"), R.drawable.h_sport_soccer);
        iconMap.put(new AmenityTypeKey(AmenityType.SPORT, "archery"), R.drawable.h_sport_archery);
        iconMap.put(new AmenityTypeKey(AmenityType.SPORT, "baseball"), R.drawable.h_sport_baseball);
        iconMap.put(new AmenityTypeKey(AmenityType.SPORT, "stadium"), R.drawable.h_leisure_stadium);
        iconMap.put(new AmenityTypeKey(AmenityType.SPORT, "canoe"), R.drawable.h_sport_canoe);
        iconMap.put(new AmenityTypeKey(AmenityType.SPORT, "tennis"), R.drawable.h_sport_tennis);
        iconMap.put(new AmenityTypeKey(AmenityType.SPORT, "shooting"), R.drawable.h_sport_shooting);
        iconMap.put(new AmenityTypeKey(AmenityType.SPORT, "golf"), R.drawable.h_sport_golf);
        iconMap.put(new AmenityTypeKey(AmenityType.SPORT, "swimming"), R.drawable.h_leisure_water_park);
        iconMap.put(new AmenityTypeKey(AmenityType.SPORT, "skiing"), R.drawable.h_sport_ski);
        iconMap.put(new AmenityTypeKey(AmenityType.SPORT, "diving"), R.drawable.h_sport_diving);

        iconMap.put(new AmenityTypeKey(AmenityType.EMERGENCY, "phone"), R.drawable.h_emergency_phone);

        iconMap.put(new AmenityTypeKey(AmenityType.GEOCACHE, "found"), R.drawable.h_geocache_found);
        iconMap.put(new AmenityTypeKey(AmenityType.GEOCACHE, ""), R.drawable.h_geocache_not_found);
        
        iconMap.put(new AmenityTypeKey(AmenityType.OTHER, "fire_station"), R.drawable.h_fire_station);
        iconMap.put(new AmenityTypeKey(AmenityType.OTHER, "embassy"), R.drawable.h_embassy);
        iconMap.put(new AmenityTypeKey(AmenityType.OTHER, "grave_yard"), R.drawable.h_grave_yard);
        iconMap.put(new AmenityTypeKey(AmenityType.OTHER, "police"), R.drawable.h_police);
        iconMap.put(new AmenityTypeKey(AmenityType.OTHER, "post_box"), R.drawable.h_postbox);
        iconMap.put(new AmenityTypeKey(AmenityType.OTHER, "post_office"), R.drawable.h_postoffice);
        iconMap.put(new AmenityTypeKey(AmenityType.OTHER, "prison"), R.drawable.h_prison);
        iconMap.put(new AmenityTypeKey(AmenityType.OTHER, "recycling"), R.drawable.h_recycling);
        iconMap.put(new AmenityTypeKey(AmenityType.OTHER, "shelter"), R.drawable.h_shelter);
        iconMap.put(new AmenityTypeKey(AmenityType.OTHER, "telephone"), R.drawable.h_telephone);
        iconMap.put(new AmenityTypeKey(AmenityType.OTHER, "toilets"), R.drawable.h_toilets);
        iconMap.put(new AmenityTypeKey(AmenityType.OTHER, "place_of_worship"), R.drawable.h_place_of_worship);

        /*
            AmenityType.BARRIER
            AmenityType.LANDUSE
            AmenityType.MAN_MADE
            AmenityType.OFFICE
            AmenityType.MILITARY
            AmenityType.ADMINISTRATIVE
            AmenityType.OSMWIKI("osmwiki"),  //$NON-NLS-1$
            AmenityType.USER_DEFINED("user_defined"),  //$NON-NLS-1$
         */
         
        /*
            TODO(natashaj): below are unmapped tags that I'm not sure need to have icons
            <!-- Locations -->
            <filter minzoom="4" tag="place" icon="city" value="city"/>
            <filter minzoom="6" tag="place" icon="city" value="town"/>
            
            <!-- Traffic -->
            <filter appMode="car" minzoom="16" icon="traffic_light" tag="highway" value="traffic_signals"/>
            <filter minzoom="17" icon="traffic_light" tag="highway" value="traffic_signals"/>
            <filter minzoom="15" icon="mini_roundabout" tag="highway" value="mini_roundabout"/>
            <filter minzoom="15" icon="gate2" tag="highway" value="gate"/>
            <filter minzoom="15" icon="gate2" tag="barrier" value="gate"/>
            <filter minzoom="16" icon="liftgate" tag="barrier" value="lift_gate"/>
            <filter minzoom="16" icon="bollard" tag="barrier" value="sally_port"/>
            <filter minzoom="13" icon="highway_ford" tag="highway" value="ford"/>
            <filter minzoom="15" icon="barrier_block" tag="barrier" value="block"/>
            <filter minzoom="15" icon="barrier_kissing_gate" tag="barrier" value="kissing_gate"/>
            <filter minzoom="14" icon="barrier_toll_booth" tag="barrier" value="toll_booth"/>
            <filter minzoom="14" icon="liftgate" tag="barrier" value="border_control"/>
            <filter minzoom="15" icon="parking" tag="amenity" value="parking"/>
            <filter minzoom="16" icon="parking" tag="amenity" value="bicycle_parking"/>
        
            <!-- Buildings -->
            <filter minzoom="15" icon="power_tower" tag="power" value="tower"/>
            <filter minzoom="15" icon="power_wind" tag="power" value="generator"/>
            <filter minzoom="15" icon="lighthouse" tag="man_made" value="lighthouse"/>
            <filter minzoom="16" icon="water_tower" tag="man_made" value="water_tower"/>
            <filter minzoom="16" icon="windmill" tag="man_made" value="windmill"/>
            <filter minzoom="16" icon="amenity_fountain" tag="amenity" value="fountain"/>
            <filter minzoom="16" icon="amenity_marketplace" tag="amenity" value="marketplace"/>
            <filter minzoom="16" icon="barrier_entrance" tag="barrier" value="entrance"/>
            <filter minzoom="16" icon="barrier_entrance" tag="building" value="entrance"/>
            <filter minzoom="16" icon="townhall" tag="amenity" value="townhall"/>
            <filter minzoom="16" icon="amenity_court" tag="amenity" value="courthouse"/>
        
            <!-- Water -->
            <filter minzoom="15" icon="slipway" tag="leisure" value="slipway"/>
            <filter minzoom="15" icon="lock_gate" tag="waterway" value="lock_gate"/>
            <filter minzoom="15" icon="lock_gate" tag="waterway" value="lock"/>
        
            <!-- Outdoor, tourism, leisure -->
            <filter minzoom="17" icon="amenity_bench" tag="amenity" value="bench"/>
         */
    }

    public int getIcon(Amenity amenity) {
        Integer iconResource = iconMap.get(new AmenityTypeKey(amenity.getType(), amenity.getSubType()));
        return (iconResource == null) ? DEFAULT_ICON_RESOURCE : iconResource.intValue();
    }

    private class AmenityTypeKey implements Comparable<AmenityTypeKey> {
        AmenityType amenityType;
        String subType;
        
        public AmenityTypeKey(AmenityType amenityType, String subType) {
            this.amenityType = amenityType;
            this.subType = subType;
        }
        
        public AmenityType getAmenityType() {
            return amenityType;
        }
        
        public String getSubType() {
            return subType;
        }
        
        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof AmenityTypeKey)) {
                throw new ClassCastException("Cannot compare AmenityTypeKey to object of different type");
            }
            return compareTo((AmenityTypeKey)obj) == 0;
        }
        
        @Override
        public int hashCode() {
            return getAmenityType().hashCode() * getSubType().hashCode();
        }

        @Override
        public int compareTo(AmenityTypeKey other) {
            if ((getAmenityType() == other.getAmenityType()) && (getSubType() == other.getSubType())) {
                return 0; 
            } else {
                return -1;
            }
        }
    }
}
