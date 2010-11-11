package net.osmand.data;

import java.util.Collection;

import net.osmand.osm.Entity;
import net.osmand.osm.MapRenderingTypes;
import net.osmand.osm.Node;
import net.osmand.osm.Relation;
import net.osmand.osm.OSMSettings.OSMTagKey;

public class Amenity extends MapObject {

	
	private String subType;
	private AmenityType type;
	private String openingHours;
	private String phone;
	private String site;

	public Amenity(Entity entity){
		super(entity);
		// manipulate with id to distinguish way and nodes
		this.id = entity.getId() << 1 + ((entity instanceof Node)? 0 : 1);
		initTypeSubtype(entity, this);
		this.openingHours = entity.getTag(OSMTagKey.OPENING_HOURS);
		this.phone = entity.getTag(OSMTagKey.PHONE);
		if (this.phone == null) {
			this.phone = entity.getTag(OSMTagKey.CONTACT_PHONE);
		}
		this.site = entity.getTag(OSMTagKey.WIKIPEDIA);
		if (this.site != null) {
			if (!this.site.startsWith("http://")) { //$NON-NLS-1$
				int i = this.site.indexOf(':');
				if (i == -1) {
					this.site = "http://en.wikipedia.org/wiki/" + this.site; //$NON-NLS-1$
				} else {
					this.site = "http://" + this.site.substring(0, i) + ".wikipedia.org/wiki/" + this.site.substring(i + 1); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}
		} else {
			this.site = entity.getTag(OSMTagKey.WEBSITE);
			if (this.site == null) {
				this.site = entity.getTag(OSMTagKey.URL);
				if (this.site == null) {
					this.site = entity.getTag(OSMTagKey.CONTACT_WEBSITE);
				}
			}
		}
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
	
	public String getSite() {
		return site;
	}

	public void setSite(String site) {
		this.site = site;
	}

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}
	
	
	public void doDataPreparation() {
		
	}

}
