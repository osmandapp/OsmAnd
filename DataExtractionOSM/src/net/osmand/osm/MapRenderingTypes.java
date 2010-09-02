package net.osmand.osm;

import net.osmand.data.Amenity;
import net.osmand.osm.OSMSettings.OSMTagKey;


/**
 * Describing types of polygons :
 * 1. Last 3 bits define type of element : polygon, polyline, point 
 */
public class MapRenderingTypes {

	

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
	
	// 2._ - 1bit
	public final static int PL_HW_ONEWAY = 1;
	
	// TODO bicycle access and vehicle access (height, weight ? )
	// TODO max speed class (?)
	// TODO free (?)
	// 2._.1 highway types
	public final static int PL_HW_TRUNK = 1;
	public final static int PL_HW_MOTORWAY = 2;
	public final static int PL_HW_PRIMARY = 3;
	public final static int PL_HW_SECONDARY = 4;
	public final static int PL_HW_TERTIARY = 5;
	public final static int PL_HW_RESIDENTIAL = 6;
	public final static int PL_HW_TRACK = 7;
	public final static int PL_HW_PATH = 8;
	public final static int PL_HW_UNCLASSIFIED = 9;
	public final static int PL_HW_SERVICE = 10;
	
	
	
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
		} else if(hw.equals("UNCLASSIFIED")){ //$NON-NLS-1$
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
