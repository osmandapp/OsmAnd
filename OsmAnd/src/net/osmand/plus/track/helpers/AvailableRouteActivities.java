package net.osmand.plus.track.helpers;

public interface AvailableRouteActivities { // TODO delete

	String SOURCE_FILE_CONTENT = "{\n" +
			"    \"groups\": [\n" +
			"        {\n" +
			"            \"label\": \"Driving\",\n" +
			"            \"id\": \"driving\",\n" +
			"            \"activities\": [\n" +
			"                {\n" +
			"                    \"label\": \"Car\",\n" +
			"                    \"id\": \"car\",\n" +
			"                    \"icon_name\": \"ic_action_car_dark\"\n" +
			"                },\n" +
			"                {\n" +
			"                    \"label\": \"Off-Road\",\n" +
			"                    \"id\": \"off_road\",\n" +
			"                    \"icon_name\": \"ic_action_offroad\"\n" +
			"                },\n" +
			"                {\n" +
			"                    \"label\": \"ATV (All-Terrain Vehicle)\",\n" +
			"                    \"id\": \"all_terrain_vehicle\",\n" +
			"                    \"icon_name\": \"ic_action_utv\"\n" +
			"                },\n" +
			"                {\n" +
			"                    \"label\": \"Truck (HGV)\",\n" +
			"                    \"id\": \"truck_hgv\",\n" +
			"                    \"icon_name\": \"ic_action_truck_dark\"\n" +
			"                },\n" +
			"                {\n" +
			"                    \"label\": \"Karting\",\n" +
			"                    \"id\": \"karting\",\n" +
			"                    \"icon_name\": \"ic_action_go_cart\"\n" +
			"                }\n" +
			"            ]\n" +
			"        },\n" +
			"        {\n" +
			"            \"label\": \"Motorcycling\",\n" +
			"            \"id\": \"motorcycling\",\n" +
			"            \"activities\": [\n" +
			"                {\n" +
			"                    \"label\": \"Adventure Motorcycling\",\n" +
			"                    \"id\": \"adventure_motorcycling\",\n" +
			"                    \"icon_name\": \"ic_action_motorcycle_dark\"\n" +
			"                },\n" +
			"                {\n" +
			"                    \"label\": \"Enduro Motorcycling\",\n" +
			"                    \"id\": \"enduro_motorcycling\",\n" +
			"                    \"icon_name\": \"ic_action_enduro_motorcycle\"\n" +
			"                },\n" +
			"                {\n" +
			"                    \"label\": \"Motocross\",\n" +
			"                    \"id\": \"motocross\",\n" +
			"                    \"icon_name\": \"ic_action_enduro_motorcycle\"\n" +
			"                },\n" +
			"                {\n" +
			"                    \"label\": \"Off-Road Motorcycling (Dirt Biking)\",\n" +
			"                    \"id\": \"off_road_motorcycling_dirt_biking\",\n" +
			"                    \"icon_name\": \"ic_action_dirt_motorcycle\"\n" +
			"                },\n" +
			"                {\n" +
			"                    \"label\": \"Road Motorcycling\",\n" +
			"                    \"id\": \"road_motorcycling\",\n" +
			"                    \"icon_name\": \"ic_action_motorcycle_dark\"\n" +
			"                },\n" +
			"                {\n" +
			"                    \"label\": \"Sport Motorcycling\",\n" +
			"                    \"id\": \"sport_motorcycling\",\n" +
			"                    \"icon_name\": \"ic_sample\"\n" +
			"                },\n" +
			"                {\n" +
			"                    \"label\": \"Touring Motorcycling\",\n" +
			"                    \"id\": \"touring_motorcycling\",\n" +
			"                    \"icon_name\": \"ic_sample\"\n" +
			"                },\n" +
			"                {\n" +
			"                    \"label\": \"Track Racing\",\n" +
			"                    \"id\": \"track_racing\",\n" +
			"                    \"icon_name\": \"ic_sample\"\n" +
			"                },\n" +
			"                {\n" +
			"                    \"label\": \"Motor scooter\",\n" +
			"                    \"id\": \"motor_scooter\",\n" +
			"                    \"icon_name\": \"ic_action_motor_scooter\"\n" +
			"                }\n" +
			"            ]\n" +
			"        },\n" +
			"        {\n" +
			"            \"label\": \"Foot\",\n" +
			"            \"id\": \"foot\",\n" +
			"            \"activities\": [\n" +
			"                {\n" +
			"                    \"label\": \"Backpacking\",\n" +
			"                    \"id\": \"backpacking\",\n" +
			"                    \"icon_name\": \"ic_sample\"\n" +
			"                },\n" +
			"                {\n" +
			"                    \"label\": \"Hiking\",\n" +
			"                    \"id\": \"hiking\",\n" +
			"                    \"icon_name\": \"ic_sample\"\n" +
			"                },\n" +
			"                {\n" +
			"                    \"label\": \"Hill climbing\",\n" +
			"                    \"id\": \"hill_climbing\",\n" +
			"                    \"icon_name\": \"ic_action_hill_climbing\"\n" +
			"                },\n" +
			"                {\n" +
			"                    \"label\": \"Road Running\",\n" +
			"                    \"id\": \"road_running\",\n" +
			"                    \"icon_name\": \"running\"\n" +
			"                },\n" +
			"                {\n" +
			"                    \"label\": \"Trail Running\",\n" +
			"                    \"id\": \"trail_running\",\n" +
			"                    \"icon_name\": \"ic_sample\"\n" +
			"                },\n" +
			"                {\n" +
			"                    \"label\": \"Trekking\",\n" +
			"                    \"id\": \"trekking\",\n" +
			"                    \"icon_name\": \"ic_action_trekking_dark\"\n" +
			"                },\n" +
			"                {\n" +
			"                    \"label\": \"Walking\",\n" +
			"                    \"id\": \"walking\",\n" +
			"                    \"icon_name\": \"ic_action_pedestrian_dark\"\n" +
			"                }\n" +
			"            ]\n" +
			"        },\n" +
			"        {\n" +
			"            \"label\": \"Winter sport\",\n" +
			"            \"id\": \"winter_sport\",\n" +
			"            \"activities\": [\n" +
			"                {\n" +
			"                    \"label\": \"Cross-Country Skiing\",\n" +
			"                    \"id\": \"cross_country_skiing\",\n" +
			"                    \"icon_name\": \"ic_sample\"\n" +
			"                },\n" +
			"                {\n" +
			"                    \"label\": \"Ice Skating\",\n" +
			"                    \"id\": \"ice_skating\",\n" +
			"                    \"icon_name\": \"ic_sample\"\n" +
			"                },\n" +
			"                {\n" +
			"                    \"label\": \"Ski Touring\",\n" +
			"                    \"id\": \"ski_touring\",\n" +
			"                    \"icon_name\": \"ic_action_ski_touring\"\n" +
			"                },\n" +
			"                {\n" +
			"                    \"label\": \"Skiing\",\n" +
			"                    \"id\": \"skiing\",\n" +
			"                    \"icon_name\": \"ic_action_skiing\"\n" +
			"                },\n" +
			"                {\n" +
			"                    \"label\": \"Snowboarding\",\n" +
			"                    \"id\": \"snowboarding\",\n" +
			"                    \"icon_name\": \"ic_action_snowboarding\"\n" +
			"                },\n" +
			"                {\n" +
			"                    \"label\": \"Snowmobiling\",\n" +
			"                    \"id\": \"snowmobiling\",\n" +
			"                    \"icon_name\": \"ic_action_snowmobile\"\n" +
			"                },\n" +
			"                {\n" +
			"                    \"label\": \"Snowshoeing\",\n" +
			"                    \"id\": \"snowshoeing\",\n" +
			"                    \"icon_name\": \"ic_action_snowshoeing\"\n" +
			"                }\n" +
			"            ]\n" +
			"        },\n" +
			"        {\n" +
			"            \"label\": \"Cycling\",\n" +
			"            \"id\": \"cycling\",\n" +
			"            \"activities\": [\n" +
			"                {\n" +
			"                    \"label\": \"E-Biking\",\n" +
			"                    \"id\": \"e_biking\",\n" +
			"                    \"icon_name\": \"ic_action_electric_bike\"\n" +
			"                },\n" +
			"                {\n" +
			"                    \"label\": \"E-mountain bike\",\n" +
			"                    \"id\": \"e_mountain_bike\",\n" +
			"                    \"icon_name\": \"ic_sample\"\n" +
			"                },\n" +
			"                {\n" +
			"                    \"label\": \"Gravel Biking\",\n" +
			"                    \"id\": \"gravel_biking\",\n" +
			"                    \"icon_name\": \"ic_sample\"\n" +
			"                },\n" +
			"                {\n" +
			"                    \"label\": \"Mountain biking\",\n" +
			"                    \"id\": \"mountain_biking\",\n" +
			"                    \"icon_name\": \"ic_action_mountain_bike\"\n" +
			"                },\n" +
			"                {\n" +
			"                    \"label\": \"Road cycling\",\n" +
			"                    \"id\": \"road_cycling\",\n" +
			"                    \"icon_name\": \"ic_action_bicycle_dark\"\n" +
			"                },\n" +
			"                {\n" +
			"                    \"label\": \"Touring\",\n" +
			"                    \"id\": \"touring\",\n" +
			"                    \"icon_name\": \"ic_sample\"\n" +
			"                }\n" +
			"            ]\n" +
			"        },\n" +
			"        {\n" +
			"            \"label\": \"Air Sports\",\n" +
			"            \"id\": \"air_sports\",\n" +
			"            \"activities\": [\n" +
			"                {\n" +
			"                    \"label\": \"Aviation\",\n" +
			"                    \"id\": \"aviation\",\n" +
			"                    \"icon_name\": \"ic_action_light_aircraft\"\n" +
			"                },\n" +
			"                {\n" +
			"                    \"label\": \"Drone Flying\",\n" +
			"                    \"id\": \"drone_flying\",\n" +
			"                    \"icon_name\": \"ic_sample\"\n" +
			"                },\n" +
			"                {\n" +
			"                    \"label\": \"Gliding (Sailplane)\",\n" +
			"                    \"id\": \"gliding_sailplane\",\n" +
			"                    \"icon_name\": \"ic_sample\"\n" +
			"                },\n" +
			"                {\n" +
			"                    \"label\": \"Hang gliding\",\n" +
			"                    \"id\": \"hang_gliding\",\n" +
			"                    \"icon_name\": \"ic_aciton_hang_gliding\"\n" +
			"                },\n" +
			"                {\n" +
			"                    \"label\": \"Hot Air Ballooning\",\n" +
			"                    \"id\": \"hot_air_ballooning\",\n" +
			"                    \"icon_name\": \"ic_sample\"\n" +
			"                },\n" +
			"                {\n" +
			"                    \"label\": \"Parachuting (Skydiving)\",\n" +
			"                    \"id\": \"parachuting_skydiving\",\n" +
			"                    \"icon_name\": \"ic_action_paragliding\"\n" +
			"                },\n" +
			"                {\n" +
			"                    \"label\": \"Paragliding\",\n" +
			"                    \"id\": \"paragliding\",\n" +
			"                    \"icon_name\": \"ic_action_paragliding\"\n" +
			"                },\n" +
			"                {\n" +
			"                    \"label\": \"Powered Paragliding (Paramotoring)\",\n" +
			"                    \"id\": \"powered_paragliding_paramotoring\",\n" +
			"                    \"icon_name\": \"ic_sample\"\n" +
			"                },\n" +
			"                {\n" +
			"                    \"label\": \"Ultralight Aviation\",\n" +
			"                    \"id\": \"ultralight_aviation\",\n" +
			"                    \"icon_name\": \"ic_sample\"\n" +
			"                }\n" +
			"            ]\n" +
			"        },\n" +
			"        {\n" +
			"            \"label\": \"Water sport\",\n" +
			"            \"id\": \"water_sport\",\n" +
			"            \"activities\": [\n" +
			"                {\n" +
			"                    \"label\": \"Canoe\",\n" +
			"                    \"id\": \"canoe\",\n" +
			"                    \"icon_name\": \"canoe\"\n" +
			"                },\n" +
			"                {\n" +
			"                    \"label\": \"Diving\",\n" +
			"                    \"id\": \"diving\",\n" +
			"                    \"icon_name\": \"diving\"\n" +
			"                },\n" +
			"                {\n" +
			"                    \"label\": \"Kayak\",\n" +
			"                    \"id\": \"kayak\",\n" +
			"                    \"icon_name\": \"ic_action_kayak\"\n" +
			"                },\n" +
			"                {\n" +
			"                    \"label\": \"Surfing\",\n" +
			"                    \"id\": \"surfing\",\n" +
			"                    \"icon_name\": \"surfing\"\n" +
			"                },\n" +
			"                {\n" +
			"                    \"label\": \"Swimming outdoor\",\n" +
			"                    \"id\": \"swimming_outdoor\",\n" +
			"                    \"icon_name\": \"swimming_outdoor\"\n" +
			"                },\n" +
			"                {\n" +
			"                    \"label\": \"Waterskiing\",\n" +
			"                    \"id\": \"waterskiing\",\n" +
			"                    \"icon_name\": \"waterskiing\"\n" +
			"                },\n" +
			"                {\n" +
			"                    \"label\": \"Windsurfing\",\n" +
			"                    \"id\": \"windsurfing\",\n" +
			"                    \"icon_name\": \"windsurfing\"\n" +
			"                },\n" +
			"                {\n" +
			"                    \"label\": \"Motorboat\",\n" +
			"                    \"id\": \"motorboat\",\n" +
			"                    \"icon_name\": \"ic_action_motorboat\"\n" +
			"                },\n" +
			"                {\n" +
			"                    \"label\": \"Rafting\",\n" +
			"                    \"id\": \"rafting\",\n" +
			"                    \"icon_name\": \"ic_sample\"\n" +
			"                }\n" +
			"            ]\n" +
			"        },\n" +
			"        {\n" +
			"            \"label\": \"Other\",\n" +
			"            \"id\": \"other\",\n" +
			"            \"activities\": [\n" +
			"                {\n" +
			"                    \"label\": \"Horse riding\",\n" +
			"                    \"id\": \"horse_riding\",\n" +
			"                    \"icon_name\": \"horse_riding\"\n" +
			"                },\n" +
			"                {\n" +
			"                    \"label\": \"Mushrooms picking\",\n" +
			"                    \"id\": \"mushrooms_picking\",\n" +
			"                    \"icon_name\": \"ic_sample\"\n" +
			"                },\n" +
			"                {\n" +
			"                    \"label\": \"Harvesting\",\n" +
			"                    \"id\": \"harvesting\",\n" +
			"                    \"icon_name\": \"ic_sample\"\n" +
			"                },\n" +
			"                {\n" +
			"                    \"label\": \"Tree Climbing\",\n" +
			"                    \"id\": \"tree_climbing\",\n" +
			"                    \"icon_name\": \"ic_sample\"\n" +
			"                },\n" +
			"                {\n" +
			"                    \"label\": \"Scuba diving\",\n" +
			"                    \"id\": \"scuba_diving\",\n" +
			"                    \"icon_name\": \"ic_sample\"\n" +
			"                },\n" +
			"                {\n" +
			"                    \"label\": \"Caving\",\n" +
			"                    \"id\": \"caving\",\n" +
			"                    \"icon_name\": \"ic_sample\"\n" +
			"                },\n" +
			"                {\n" +
			"                    \"label\": \"Golfing\",\n" +
			"                    \"id\": \"golfing\",\n" +
			"                    \"icon_name\": \"ic_sample\"\n" +
			"                }\n" +
			"            ]\n" +
			"        }\n" +
			"    ]\n" +
			"}";

}
