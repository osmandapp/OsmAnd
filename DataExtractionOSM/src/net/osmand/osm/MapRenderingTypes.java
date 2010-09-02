package net.osmand.osm;

import net.osmand.data.Amenity;
import net.osmand.osm.OSMSettings.OSMTagKey;


/**
 * SOURCE : http://wiki.openstreetmap.org/wiki/Map_Features
 * 
 * Describing types of polygons :
 * 1. Last 3 bits define type of element : polygon, polyline, point 
 */
public class MapRenderingTypes {
	// OSM keys :
	// 1. highway (lines) 		+
	// 2. highway (node)     	- [stop, roundabout, speed_camera]
	// 3. traffic_calming		-
	// 4. service 				- [parking_aisle]
	// 5. barrier 				-
	// 6. cycleway     			- [? different kinds of cycleways]
	// 7. waterway				-
	// 8. railway				-
	// 9. aeroway/aerialway		-
	// 10. power				-
	// 11. man_made				-
	// 12. building 			+
	//	---------------------------------
	// 13. leisure, amenity, shop, tourism, historic, sport -
	// 14. emergency			-
	// 15. landuse				-
	// 16. natural				-
	// 17. military				-
	
	// 18. route (?)
	// 19. boundary (?)
	// 20. RESTRICTIONS
	
	

	public final static int TYPE_MASK = (1 << 3) - 1;
	public final static int POLYGON_TYPE = 3;
	public final static int POLYLINE_TYPE = 2;
	public final static int POINT_TYPE = 1;
	
	
	// 1. polygon
	public final static int PG_AREA_MASK = (1 << 6) - 1;
	public final static int PG_BUILDING_TYPE = 1;
	
	
	// 2. polyline
	public final static int PL_TYPE_MASK = (1 << 4) - 1;
	public final static int PL_HIGHWAY_TYPE = 1;
	
	// highway :    sss|aaa|f|ttttt|o|0001|011
	// o - oneway, t - type of way, f - free or toll, a - acess, max speed - s = 20 bits
	
	// 2._ - 1bit
	public final static int PL_HW_ONEWAY = 1;
	
	// TODO bicycle access and vehicle access (height, weight ? )
	// TODO max speed class (?)
	// TODO free (?)
	// 2._.1 highway types
	public final static int PL_HW_TYPE_MASK = (1 << 5) -1;
	
	public final static int PL_HW_TRUNK = 1;
	public final static int PL_HW_MOTORWAY = 2;
	public final static int PL_HW_PRIMARY = 3;
	public final static int PL_HW_SECONDARY = 4;
	public final static int PL_HW_TERTIARY = 5;
	public final static int PL_HW_RESIDENTIAL = 6;
	public final static int PL_HW_SERVICE = 7;
	public final static int PL_HW_UNCLASSIFIED = 8;
	public final static int PL_HW_TRACK = 9;
	public final static int PL_HW_PATH = 10;
	public final static int PL_HW_LIVING_STREET = 11;

	public final static int PL_HW_CYCLEWAY = 17;
	public final static int PL_HW_FOOTWAY = 18;
	public final static int PL_HW_STEPS = 19;
	public final static int PL_HW_BRIDLEWAY = 20;
	
	public final static int PL_HW_CONSTRUCTION = 31;
	
	
	
	
	public static boolean isPolygonBuilding(int type){
		return ((type & TYPE_MASK) == POLYGON_TYPE) &&  
			   ((getSubType(TYPE_MASK, type) & PG_AREA_MASK) == PG_BUILDING_TYPE);
	}
	
	public static boolean isHighway(int type){
		return ((type & TYPE_MASK) == POLYLINE_TYPE) &&  
		   ((getSubType(TYPE_MASK, type) & PL_TYPE_MASK) == PL_HIGHWAY_TYPE);
	}
	
	public static int getHighwayType(int type){
		return type >>= (Integer.numberOfTrailingZeros(TYPE_MASK + 1) + Integer.numberOfTrailingZeros(PL_TYPE_MASK + 1) + 1);
	}
	
	private static int getSubType(int mask, int type){
		return type >> Integer.numberOfTrailingZeros(mask + 1);
	}

	
	private static int getHighwayType(String hw){
		if(hw == null){
			return 0;
		}
		// TODO Others type !!!
		hw = hw.toUpperCase();
		if(hw.equals("TRUNK") || hw.equals("TRUNK_LINK")){ //$NON-NLS-1$ //$NON-NLS-2$
			return PL_HW_TRUNK;
		} else if(hw.equals("MOTORWAY") || hw.equals("MOTORWAY_LINK")){ //$NON-NLS-1$ //$NON-NLS-2$
			return PL_HW_MOTORWAY;
		} else if(hw.equals("PRIMARY") || hw.equals("PRIMARY_LINK")){  //$NON-NLS-1$//$NON-NLS-2$
			return PL_HW_PRIMARY;
		} else if(hw.equals("SECONDARY") || hw.equals("SECONDARY_LINK")){ //$NON-NLS-1$ //$NON-NLS-2$
			return PL_HW_SECONDARY;
		} else if(hw.equals("TERTIARY")){ //$NON-NLS-1$
			return PL_HW_TERTIARY;
		} else if(hw.equals("RESIDENTIAL")){ //$NON-NLS-1$
			return PL_HW_RESIDENTIAL;
		} else if(hw.equals("TRACK")){ //$NON-NLS-1$
			return PL_HW_TRACK;
		} else if(hw.equals("PATH")){ //$NON-NLS-1$
			return PL_HW_PATH;
		} else if(hw.equals("SERVICE") || hw.equals("SERVICES")){ //$NON-NLS-1$ //$NON-NLS-2$
			return PL_HW_SERVICE;
		} else if(hw.equals("LIVING_STREET")){ //$NON-NLS-1$
			return PL_HW_LIVING_STREET;
		} else if(hw.equals("CONSTRUCTION")){ //$NON-NLS-1$
			return PL_HW_CONSTRUCTION;
		} else if(hw.equals("STEPS")){ //$NON-NLS-1$
			return PL_HW_STEPS;
		} else if(hw.equals("BRIDLEWAY")){ //$NON-NLS-1$
			return PL_HW_BRIDLEWAY;
		} else if(hw.equals("CYCLEWAY")){ //$NON-NLS-1$
			return PL_HW_CYCLEWAY;
		} else if(hw.equals("PEDESTRIAN") | hw.equals("FOOTWAY")){ //$NON-NLS-1$ //$NON-NLS-2$
			return PL_HW_FOOTWAY;
		} else if(hw.equals("UNCLASSIFIED") || hw.equals("ROAD")){ //$NON-NLS-1$ //$NON-NLS-2$
			return PL_HW_UNCLASSIFIED;
		} 
		return 0;
	}
	
	
	// if type equals 0 no need to save that point
	public static int encodeEntityWithType(Entity e){
		int type = 0;
		if(e instanceof Relation){
			// change in future (?)
			// multypoligon, forest ... 
			return type;
		}
		
		int i = 0;
		if(e instanceof Node){
			if(Amenity.isAmenity(e)){
				type |= POINT_TYPE;
			}
		} else {
			if(e.getTag(OSMTagKey.BUILDING) != null){
				type <<= Integer.numberOfTrailingZeros(PG_AREA_MASK + 1);
				type |= PG_BUILDING_TYPE;
				
				type <<= Integer.numberOfTrailingZeros(TYPE_MASK + 1);
				type |= POLYGON_TYPE;
				
			} else if((i = getHighwayType(e.getTag(OSMTagKey.HIGHWAY))) > 0){
				type = i;
				type <<= 1;
				String one = e.getTag(OSMTagKey.ONEWAY);
				type |= (one == null || one.equals("no"))? 1 : 0; //$NON-NLS-1$
				
				type <<= Integer.numberOfTrailingZeros(PL_TYPE_MASK + 1);
				type |= PL_HIGHWAY_TYPE;
				
				type <<= Integer.numberOfTrailingZeros(TYPE_MASK + 1);
				type |= POLYLINE_TYPE;
			} else {
				type |= POLYLINE_TYPE;
			}
		}
		
		return type;
	}
	
	public static String getEntityName(Entity e){
		String name = e.getTag(OSMTagKey.NAME);
		if(name == null){
			if(e.getTag(OSMTagKey.BUILDING) != null){
				name = e.getTag(OSMTagKey.ADDR_HOUSE_NUMBER);
			}
		}
		return name;
	}
	
}
