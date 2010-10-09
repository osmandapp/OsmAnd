package net.osmand.data;

import java.util.Collection;

import net.osmand.osm.Entity;
import net.osmand.osm.MapRenderingTypes;
import net.osmand.osm.Relation;
import net.osmand.osm.OSMSettings.OSMTagKey;

public class Amenity extends MapObject {

	
	private String subType;
	private AmenityType type;
	private String openingHours;

	public Amenity(Entity entity){
		super(entity);
		initTypeSubtype(entity, this);
		this.openingHours = entity.getTag(OSMTagKey.OPENING_HOURS);
	}
	
	public Amenity(){
	}
	
	private static AmenityType initTypeSubtype(Entity entity, Amenity init) {
		Collection<String> keySet = entity.getTagKeySet();
		if (!keySet.isEmpty()) {
			for (String t : keySet) {
				AmenityType type = MapRenderingTypes.getAmenityType(t, entity.getTag(t));
				if (type != null) {
					if (init != null) {
						init.type = type;
						init.subType = MapRenderingTypes.getAmenitySubtype(t, entity.getTag(t));
					}
					return type;
				}
			}
			for (String t : keySet) {
				AmenityType type = MapRenderingTypes.getAmenityType(t, null);
				if (type != null) {
					if (init != null) {
						init.type = type;
						init.subType = MapRenderingTypes.getAmenitySubtype(t, entity.getTag(t));
					}
					return type;
				}
			}
		}
		return null;
	}
	public AmenityType getType(){
		return type;
	}
	
	public String getSubType(){
		return subType;
	}
	
	public void setType(AmenityType type) {
		this.type = type;
	}
	
	public void setSubType(String subType) {
		this.subType = subType;
	}
	
	public static boolean isAmenity(Entity n){
		if(n instanceof Relation){
			// it could be collection of amenities
			return false;
		}
		return initTypeSubtype(n, null) != null;
	}
	
	public String getOpeningHours() {
		return openingHours;
	}
	
	public void setOpeningHours(String openingHours) {
		this.openingHours = openingHours;
	}
	

	public String getSimpleFormat(boolean en){
		return AmenityType.toPublicString(type) + " : " + getStringWithoutType(en); //$NON-NLS-1$
	}
	
	public String getStringWithoutType(boolean en){
		String n = getName(en);
		if(n.length() == 0){
			return getSubType();
		}
		return getSubType() + " " + n; //$NON-NLS-1$
	}
	
	@Override
	public String toString() {
		return getSimpleFormat(false);
	}
	
	
	public void doDataPreparation() {
		
	}

}
