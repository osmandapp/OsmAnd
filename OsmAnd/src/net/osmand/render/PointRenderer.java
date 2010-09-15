package net.osmand.render;

import net.osmand.R;
import net.osmand.osm.MapRenderingTypes;

public class PointRenderer {
	
	/**
	 * @return 0 if there is no icon for specified zoom
	 */
	public static int getPointBitmap(int zoom, int type, int subType) {
		int resId = 0;
		switch (type) {
		case MapRenderingTypes.HIGHWAY: {
			if (zoom >= 16) {
				if (subType == 38) {
					if (zoom > 16) {
						resId = R.drawable.h_traffic_light;
					}
				} else if (subType == 40) {
					resId = zoom == 16 ? R.drawable.h_bus_stop_small : R.drawable.h_bus_stop;
				} else if (subType == 23) {
					resId = R.drawable.h_transport_ford;
				}
			}
			if (subType == 35) {
				if (zoom >= 15) {
					resId = R.drawable.h_mini_round;
				}
			}
		}
			break;
		case MapRenderingTypes.RAILWAY: {
			if (subType == 13) {
				if (zoom == 12) {
					resId = R.drawable.h_halt;
				} else if (zoom == 13 || zoom == 14) {
					resId = R.drawable.h_station_small;
				} else if (zoom >= 15) {
					resId = R.drawable.h_station;
				}
			} else if (subType == 22 || subType == 23) {
				if (zoom == 13 || zoom == 14) {
					resId = R.drawable.h_halt;
				} else if (zoom >= 15) {
					resId = R.drawable.h_station_small;
				}
			} else if (subType == 25) {
				if (zoom == 14 || zoom == 15) {
					resId = R.drawable.h_level_crossing;
				} else if (zoom >= 16) {
					resId = R.drawable.h_level_crossing2;
				}
			}
		}
			break;
		case MapRenderingTypes.AEROWAY: {
			if (subType == 1) {
				if (zoom >= 10 && zoom <= 14) {
					resId = R.drawable.h_aerodrome;
				}
			} else if (subType == 10) {
				if (zoom >= 9 && zoom <= 13) {
					resId = R.drawable.h_airport;
				}
			}
		}
			break;
		case MapRenderingTypes.WATERWAY: {
			if (subType == 8) {
				if (zoom > 14) {
					resId = R.drawable.h_lock_gate;
				}
			}
		}
			break;
		case MapRenderingTypes.AERIALWAY: {
			if (subType == 7) {
				if (zoom == 13 || zoom == 14) {
					resId = R.drawable.h_halt;
				} else if (zoom >= 15) {
					resId = R.drawable.h_station_small;
				}
			}
		}
			break;
		case MapRenderingTypes.POWER: {
			if (zoom > 14) {
				if (subType == 1) {
					resId = R.drawable.h_power_tower;
				} else if (subType == 7) {
					resId = R.drawable.h_power_wind;
				}
			}
		}
			break;
		case MapRenderingTypes.MAN_MADE: {
			if (subType == 25) {
				if (zoom >= 16) {
					resId = R.drawable.h_tower_water;
				}
			} else if (subType == 17) {
				if (zoom >= 15) {
					resId = R.drawable.h_lighthouse;
				}
			} else if (subType == 27) {
				if (zoom >= 16) {
					resId = R.drawable.h_windmill;
				}
			}
		}
			break;
		case MapRenderingTypes.SHOP: {
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
		}
			break;
		case MapRenderingTypes.TOURISM: {
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
				case 17:
					resId = R.drawable.h_view_point;
					break;
				}
			}
		}
			break;
		case MapRenderingTypes.NATURAL: {
			if (subType == 13) {
				if (zoom > 10) {
					resId = R.drawable.h_peak;
				}
			} else if (subType == 3) {
				if (zoom > 14) {
					resId = R.drawable.h_cave_entrance;
				}
			} else if (subType == 17) {
				if (zoom >= 14) {
					resId = R.drawable.h_spring;
				}
			} else if (subType == 19) {
				if (zoom == 16) {
					resId = R.drawable.h_tree;
				} else if (zoom >= 17) {
					resId = R.drawable.h_tree2;
				}
			}
		}
			break;
		case MapRenderingTypes.HISTORIC: {
			if (zoom > 15) {
				if (subType == 6) {
					resId = R.drawable.h_memorial;
				} else {
					// something historic
					resId = R.drawable.h_view_point;
				}

			}
		}
			break;
		case MapRenderingTypes.BARRIER: {
			if (subType == 21) {
				if (zoom >= 15) {
					resId = R.drawable.h_gate2;
				}
			} else if (subType == 22) {
				if (zoom >= 16) {
					resId = R.drawable.h_liftgate;
				}
			} else if (subType == 26) {
				if (zoom >= 16) {
					resId = R.drawable.h_bollard;
				}
			}
		}
			break;
		case MapRenderingTypes.EMERGENCY: {
			if (zoom > 15) {
				if (subType == 10) {
					resId = R.drawable.h_firestation;
				} else if (subType == 7) {
					resId = R.drawable.h_sosphone;
				}
			}
		}
			break;
		case MapRenderingTypes.AMENITY_SUSTENANCE: {
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
		}
			break;
		case MapRenderingTypes.AMENITY_EDUCATION: {
			if (zoom > 15) {
				if (subType == 2) {
					resId = R.drawable.h_school;
				} else if (subType == 4) {
					resId = R.drawable.h_library;
				}
			}
		}
			break;
		case MapRenderingTypes.AMENITY_TRANSPORTATION: {
			if (subType == 1 || subType == 2) {
				if (zoom > 14) {
					resId = R.drawable.h_parking;
				}
			} else if (subType == 4) {
				if (zoom > 13) {
					resId = R.drawable.h_fuel;
				}
			} else if (subType == 17) {
				if (zoom >= 17) {
					resId = R.drawable.h_rental_bicycle;
				}
			} else if (subType == 20) {
				if (zoom >= 16) {
					resId = R.drawable.h_car_share;
				}
			} else if (subType == 18) {
				if (zoom > 14) {
					resId = R.drawable.h_bus_station;
				}
			}
		}
			break;
		case MapRenderingTypes.AMENITY_FINANCE: {
			if (subType == 1) {
				if (zoom > 16) {
					resId = R.drawable.h_atm;
				}
			} else if (subType == 2) {
				if (zoom > 15) {
					resId = R.drawable.h_bank;
				}
			}
		}
			break;
		case MapRenderingTypes.AMENITY_HEALTHCARE: {
			if (subType == 1) {
				if (zoom > 15) {
					resId = R.drawable.h_pharmacy;
				}
			} else if (subType == 2) {
				if (zoom >= 15) {
					resId = R.drawable.h_hospital;
				}
			}
		}
			break;
		case MapRenderingTypes.AMENITY_ENTERTAINMENT: {
			if (zoom >= 15) {
				if (subType == 3) {
					resId = R.drawable.h_cinema;
				} else if (subType == 9) {
					resId = R.drawable.h_theatre;
				}
			}
		}
			break;
		case MapRenderingTypes.AMENITY_OTHER: {
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
				case 16:
					resId = R.drawable.h_shelter;
					break;
				case 12:
					resId = R.drawable.h_postoffice;
					break;
				}
			}
			if (zoom >= 16) {
				if (subType == 26) {
					resId = R.drawable.h_place_of_worship;
				} else if (subType == 13) {
					resId = R.drawable.h_prison;
				}
			}
		}
		}
		return resId;
	}
}
