package net.osmand.data;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;


public class Amenity extends MapObject {

	private static final long serialVersionUID = 132083949926339552L;
	private String subType;
	private AmenityType type;
	// duplicate for fast access
	private String openingHours;
	private Map<String, String> additionalInfo;

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
//		 getAdditionalInfo("opening_hours");
		return openingHours;
	}
	
	public void setOpeningHours(String openingHours) {
		setAdditionalInfo("opening_hours", openingHours);
		this.openingHours = openingHours;
	}
	
	public String getAdditionalInfo(String key){
		if(additionalInfo == null) {
			return null;
		}
		return additionalInfo.get(key);
	}
	
	public Map<String, String> getAdditionalInfo() {
		if(additionalInfo == null) {
			return Collections.emptyMap();
		}
		return additionalInfo;
	}
	
	public void setAdditionalInfo(Map<String, String> additionalInfo) {
		this.additionalInfo = additionalInfo;
	}
	
	public void setAdditionalInfo(String tag, String value) {
		if(this.additionalInfo == null){
			this.additionalInfo = new LinkedHashMap<String, String>();
		}
		setAdditionalInfo(tag, value);
	}
	

	@Override
	public String toString() {
		return type.toString() + " : " + subType + " "+ getName();
	}
	
	public String getSite() {
		return getAdditionalInfo("website");
	}

	public void setSite(String site) {
		setAdditionalInfo("website", site);
	}

	public String getPhone() {
		return getAdditionalInfo("phone");
	}

	public void setPhone(String phone) {
		setAdditionalInfo("phone", phone);
	}
	
	public String getDescription() {
		return getAdditionalInfo("description");
	}
	
	public void setDescription(String description) {
		setAdditionalInfo("description", description);
	}
	
	
}
