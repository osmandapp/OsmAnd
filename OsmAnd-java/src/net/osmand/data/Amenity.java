package net.osmand.data;


public class Amenity extends MapObject {

	private static final long serialVersionUID = 132083949926339552L;
	private String subType;
	private AmenityType type;
	private String openingHours;
	private String phone;
	private String description;
	private String site;

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
	
	
}
