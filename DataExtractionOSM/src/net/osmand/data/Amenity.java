package net.osmand.data;

import java.util.Collection;
import java.util.List;

import net.osmand.Algoritms;
import net.osmand.osm.Entity;
import net.osmand.osm.MapRenderingTypes;
import net.osmand.osm.Node;
import net.osmand.osm.Relation;
import net.osmand.osm.OSMSettings.OSMTagKey;

public class Amenity extends MapObject {

	private static final long serialVersionUID = 132083949926339552L;
	private String subType;
	private AmenityType type;
	private String openingHours;
	private String phone;
	private String description;
	private String site;

	public Amenity(Entity entity, AmenityType type, String subtype){
		super(entity);
		this.type = type;
		this.subType = subtype;
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
		this.description = entity.getTag(OSMTagKey.DESCRIPTION);
	}

	@Override
	public void setEntity(Entity e) {
		super.setEntity(e);
		// manipulate with id to distinguish way and nodes
		this.id = (e.getId() << 1) + ((e instanceof Node)? 0 : 1);
	}
	
	public Amenity(){
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
	
	public static List<Amenity> parseAmenities(MapRenderingTypes renderingTypes,
			Entity entity, List<Amenity> amenitiesList){
		if(entity instanceof Relation){
			// it could be collection of amenities
			return amenitiesList;
		}
				
		Collection<String> keySet = entity.getTagKeySet();
		if (!keySet.isEmpty()) {
			int shift = 0;
			for (String t : keySet) {
				AmenityType type = renderingTypes.getAmenityType(t, entity.getTag(t));
				if (type != null) {
					String subtype = renderingTypes.getAmenitySubtype(t, entity.getTag(t));
					Amenity a = new Amenity(entity, type, subtype);
					if(checkAmenitiesToAdd(a, amenitiesList)){
						amenitiesList.add(shift, a);
						shift++;
					}
				} else {
					type = renderingTypes.getAmenityType(t, null);
					if (type != null) {
						String subtype = renderingTypes.getAmenitySubtype(t, entity.getTag(t));
						Amenity a = new Amenity(entity, type, subtype);
						if(checkAmenitiesToAdd(a, amenitiesList)){
							// 	add amenity to the end
							amenitiesList.add(a);
						}
					}
					
				}
			}
		}
		return amenitiesList;
	}
	
	private static boolean checkAmenitiesToAdd(Amenity a, List<Amenity> amenitiesList){
		// check amenity for duplication
		for(Amenity b : amenitiesList){
			if(b.getType() == a.getType() && Algoritms.objectEquals(a.getSubType(), b.getSubType())){
				return false;
			}
		}
		return true;
		
	}
	
	public String getOpeningHours() {
		return openingHours;
	}
	
	public void setOpeningHours(String openingHours) {
		this.openingHours = openingHours;
	}
	

	@Override
	public String toString() {
		return type.toString() + " : " + subType + " "+ getName();
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
	
	public String getDescription() {
		return description;
	}
	
	public void setDescription(String description) {
		this.description = description;
	}
	
	
	@Override
	public void doDataPreparation() {
		
	}

}
