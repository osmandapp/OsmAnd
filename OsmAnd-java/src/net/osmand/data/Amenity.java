package net.osmand.data;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;


public class Amenity extends MapObject implements LocationPoint {

	public static final String WEBSITE = "website";
	public static final String PHONE = "phone";
	public static final String DESCRIPTION = "description";
	public static final String OPENING_HOURS = "opening_hours";
	
	private static final long serialVersionUID = 132083949926339552L;
	private String subType;
	private AmenityType type;
	// duplicate for fast access
	private String openingHours;
	private Map<String, String> additionalInfo;
	private double deviateDistance; // for search on path

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
		openingHours = additionalInfo.get(OPENING_HOURS);
	}
	
	public double getDeviateDistance() {
		return deviateDistance;
	}
	
	public void setDeviateDistance(double deviateDistance) {
		this.deviateDistance = deviateDistance;
	}

	public void setAdditionalInfo(String tag, String value) {
		if(this.additionalInfo == null){
			this.additionalInfo = new LinkedHashMap<String, String>();
		}
		if("name".equals(tag)) {
			setName(value);
		} else if("name:en".equals(tag)) {
			setEnName(value);
		} else {
			this.additionalInfo.put(tag, value);
			if (OPENING_HOURS.equals(tag)) {
				this.openingHours = value;
			}
		}
	}
	

	@Override
	public String toString() {
		return type.toString() + " : " + subType + " "+ getName();
	}
	
	public String getSite() {
		return getAdditionalInfo(WEBSITE);
	}

	public void setSite(String site) {
		setAdditionalInfo(WEBSITE, site);
	}

	public String getPhone() {
		return getAdditionalInfo(PHONE);
	}

	public void setPhone(String phone) {
		setAdditionalInfo(PHONE, phone);
	}
	
	public String getDescription() {
		return getAdditionalInfo(DESCRIPTION);
	}
	
	public void setDescription(String description) {
		setAdditionalInfo(DESCRIPTION, description);
	}
	
	public void setOpeningHours(String openingHours) {
		setAdditionalInfo(OPENING_HOURS, openingHours);
	}


	@Override
	public double getLatitude() {
		return getLocation().getLatitude();
	}

	@Override
	public double getLongitude() {
		return getLocation().getLongitude();
	}

	@Override
	public int getColor() {
		return 0;
	}

	@Override
	public String getName() {
		return super.getName();
	}
}
