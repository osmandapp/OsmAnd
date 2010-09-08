package net.osmand.render;

import net.osmand.R;
import net.osmand.osm.MapRenderingTypes;

public class PointRenderer {
	
	/**
	 * @return 0 if there is no icon for specified zoom
	 */
	public static int getPointBitmap(int zoom, int type, int subType) {
		int resId = 0;
		if(type == MapRenderingTypes.HIGHWAY){
			if (zoom > 16) {
				if(subType == 38){
					resId = R.drawable.h_traffic_light;
				} else if(subType == 40){
					resId = R.drawable.h_bus_stop;
				}
			}
		} else if(type == MapRenderingTypes.POWER){
			if (zoom > 14) {
				if (subType == 1) {
					resId = R.drawable.h_power_tower;
				} else if (subType == 7) {
					resId = R.drawable.h_power_wind;
				}
			}
			
		} else if(type == MapRenderingTypes.SHOP){
			if (zoom > 15) {
				switch (subType) {
				case 27:
				case 65:
					resId = R.drawable.h_shop_supermarket;
					break;
				case 17:
				case 53:
					resId = R.drawable.h_department_store;
					break;
				case 13:
					resId = R.drawable.h_shop_clothes;
					break;
				case 31:
					resId = R.drawable.h_shop_hairdresser;
					break;
				}
			}
			if (zoom > 16) {
				switch (subType) {
				case 48:
					resId = R.drawable.h_shop_butcher;
					break;
				case 42:
					resId = R.drawable.h_shop_bakery;
					break;
				case 20:
					resId = R.drawable.h_shop_diy;
					break;
				case 16:
					resId = R.drawable.h_shop_convenience;
					break;
				
					
				}
			}
		} else if(type == MapRenderingTypes.TOURISM){
			if (zoom > 15) {
				switch (subType) {
				case 4:
					resId = R.drawable.h_camp_site;
					break;
				case 5:
					resId = R.drawable.h_caravan_park;
					break;
				case 6:
					resId = R.drawable.h_camp_site; // picnic
					break;
				case 9:
					resId = R.drawable.h_alpinehut;
					break;
				case 10:
				case 11:
					resId = R.drawable.h_guest_house;
					break;
				case 12:
				case 14:
					resId = R.drawable.h_hostel;
					break;
				case 13:
					resId = R.drawable.h_hotel;
					break;
				case 15:
					resId = R.drawable.h_museum;
					break;
				}
			}
		} else if(type == MapRenderingTypes.NATURAL){
			if (zoom > 10) {
				resId = R.drawable.h_peak;
			}
		} else if(type == MapRenderingTypes.HISTORIC){
			if (zoom > 15) {
				if (subType == 6) {
					resId = R.drawable.h_memorial;
				} else if(zoom > 16){
					// something historic
					resId = R.drawable.h_view_point;
				}
				
			}
		} else if(type == MapRenderingTypes.EMERGENCY){
			if(zoom > 15){
				if(subType == 10){
					resId = R.drawable.h_firestation;
				} else if(subType == 7){
					resId = R.drawable.h_sosphone;
				}
			}
		} else if(type == MapRenderingTypes.AMENITY_SUSTENANCE){
			if (zoom > 15) {
				switch (subType) {
				case 1:
					resId = R.drawable.h_restaurant;
					break;
				case 2:
					resId = R.drawable.h_cafe;
					break;
				case 4:
					resId = R.drawable.h_fast_food;
					break;
				case 5:
					resId = R.drawable.h_pub;
					break;
				case 7:
				case 6:
					resId = R.drawable.h_bar;
					break;
				case 8:
					resId = R.drawable.h_food_drinkingtap;
					break;
				}
			}
		} else if(type == MapRenderingTypes.AMENITY_EDUCATION){
			if (zoom > 15){
				if(subType == 2){
					resId = R.drawable.h_school;
				} else if(subType == 4){
					resId = R.drawable.h_library;
				}
			}
		} else if (type == MapRenderingTypes.AMENITY_TRANSPORTATION) {
			if (subType == 1 || subType == 2) {
				resId = R.drawable.h_parking;
			} else if (subType == 4) {
				resId = R.drawable.h_fuel;
			} else if (subType == 18) {
				resId = R.drawable.h_bus_station;
			}
		} else if (type == MapRenderingTypes.AMENITY_FINANCE) {
			if (subType == 1) {
				if (zoom > 16) {
					resId = R.drawable.h_atm;
				}
			} else if (subType == 2) {
				if (zoom > 15) {
					resId = R.drawable.h_bank;
				}
			}
		} else if (type == MapRenderingTypes.AMENITY_HEALTHCARE) {
			if (subType == 1) {
				if (zoom > 15) {
					resId = R.drawable.h_pharmacy;
				}
			} else if (subType == 2) {
				resId = R.drawable.h_hospital;
			}
		} else if (type == MapRenderingTypes.AMENITY_ENTERTAINMENT) {
			if (zoom >= 15) {
				if (subType == 3) {
					resId = R.drawable.h_cinema;
				} else if(subType == 9) {
					resId = R.drawable.h_theatre;
				}
			}
		} else if(type == MapRenderingTypes.AMENITY_OTHER){
			if (zoom > 16) {
				switch (subType) {
				case 10:
					resId = R.drawable.h_police;
					break;
				case 18:
					resId = R.drawable.h_toilets;
					break;
				case 15:
					resId = R.drawable.h_recycling;
					break;
				case 7:
					resId = R.drawable.h_embassy;
					break;
				case 8:
					resId = R.drawable.h_grave_yard;
					break;
				case 17:
					resId = R.drawable.h_telephone;
					break;
				case 11:
					resId = R.drawable.h_postbox;
					break;
				case 12:
					resId = R.drawable.h_postoffice;
					break;
				}
			}
		}
		return resId;
	}
}
