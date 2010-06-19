package com.osmand.data;

import com.osmand.osm.Entity;
import com.osmand.osm.OSMSettings.OSMTagKey;

public class Amenity extends MapObject {

	
	private String subType;
	private AmenityType type;
	private String openingHours;

	public Amenity(Entity entity){
		super(entity);
		this.type = getType(entity);
		this.subType = getSubType(entity);
		this.openingHours = entity.getTag(OSMTagKey.OPENING_HOURS);
	}
	
	public Amenity(){
	}
	
	public String convertToAmenityTag(){
		switch (getType()) {
		case SHOP:
			return OSMTagKey.SHOP.getValue();
		case LEISURE:
			return OSMTagKey.LEISURE.getValue();
		case HISTORIC:
			return OSMTagKey.HISTORIC.getValue();
		case TOURISM:
			return OSMTagKey.TOURISM.getValue();
		case SPORT:
			return OSMTagKey.SPORT.getValue();
		default:
			return OSMTagKey.AMENITY.getValue();
		}
	}
	
	protected String getSubType(Entity node) {
		if (node.getTag(OSMTagKey.SHOP) != null) {
			return node.getTag(OSMTagKey.SHOP);
		} else if (node.getTag(OSMTagKey.TOURISM) != null) {
			return node.getTag(OSMTagKey.TOURISM);
		} else if (node.getTag(OSMTagKey.SPORT) != null) {
			return node.getTag(OSMTagKey.SPORT);
		} else if (node.getTag(OSMTagKey.LEISURE) != null) {
			return node.getTag(OSMTagKey.LEISURE);
		} else if (node.getTag(OSMTagKey.HISTORIC) != null) {
			return node.getTag(OSMTagKey.HISTORIC);
		} else if (node.getTag(OSMTagKey.INTERNET_ACCESS) != null) {
			return "internet_access"; //$NON-NLS-1$
		} else if (node.getTag(OSMTagKey.AMENITY) != null) {
			return node.getTag(OSMTagKey.AMENITY);
		}
		return ""; //$NON-NLS-1$
	}
	
	protected AmenityType getType(Entity node){
		if(node.getTag(OSMTagKey.SHOP) != null){
			return AmenityType.SHOP;
		} else if(node.getTag(OSMTagKey.TOURISM) != null){
			return AmenityType.TOURISM;
		} else if(node.getTag(OSMTagKey.LEISURE) != null){
			return AmenityType.LEISURE;
		} else if(node.getTag(OSMTagKey.SPORT) != null){
			return AmenityType.SPORT;
		} else if(node.getTag(OSMTagKey.HISTORIC) != null){
			return AmenityType.HISTORIC;
		} else if(AmenityType.amenityMap.containsKey(node.getTag(OSMTagKey.AMENITY))){
			return AmenityType.amenityMap.get(node.getTag(OSMTagKey.AMENITY));
		}
		return AmenityType.OTHER;
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
		if(n.getTag(OSMTagKey.AMENITY) != null){
			return true;
		} else if(n.getTag(OSMTagKey.SHOP) != null){
			return true;
		} else if(n.getTag(OSMTagKey.LEISURE) != null){
			return true;
		} else if(n.getTag(OSMTagKey.SPORT) != null){
			return true;
		} else if(n.getTag(OSMTagKey.TOURISM) != null){
			return true;
		} else if(n.getTag(OSMTagKey.HISTORIC) != null){
			return true;
		} else if(n.getTag(OSMTagKey.INTERNET_ACCESS) != null){
			return true;
		}
		return false;
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
