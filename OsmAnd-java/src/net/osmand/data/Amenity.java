package net.osmand.data;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import net.osmand.Location;
import net.osmand.osm.PoiCategory;
import net.osmand.util.Algorithms;


public class Amenity extends MapObject  {

	public static final String WEBSITE = "website";
	public static final String PHONE = "phone";
	public static final String DESCRIPTION = "description";
	public static final String OPENING_HOURS = "opening_hours";
	public static final String CONTENT = "content";
	
	private static final long serialVersionUID = 132083949926339552L;
	private String subType;
	private PoiCategory type;
	// duplicate for fast access
	private String openingHours;
	private Map<String, String> additionalInfo;
	private AmenityRoutePoint routePoint; // for search on path

	public Amenity(){
	}
	
	public static class AmenityRoutePoint {
		public double deviateDistance;
		public Location pointA;
		public Location pointB;
	}
	
	public PoiCategory getType(){
		return type;
	}
	
	public String getSubType(){
		return subType;
	}
	
	public void setType(PoiCategory type) {
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
		String str = additionalInfo.get(key);
		if (str != null) {
			if (str.startsWith(" gz ")) {
				try {
					int ind = 4;
					byte[] bytes = new byte[str.length() - ind];
					for (int i = ind; i < str.length(); i++) {
						char ch = str.charAt(i);
						bytes[i - ind] = (byte) ((int) ch - 128 - 32);

					}
					GZIPInputStream gzn = new GZIPInputStream(new ByteArrayInputStream(bytes));
					BufferedReader br = new BufferedReader(new InputStreamReader(gzn, "UTF-8"));
					StringBuilder bld = new StringBuilder();
					String s;
					while ((s = br.readLine()) != null) {
						bld.append(s);
					}
					str = bld.toString();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return str;
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

	public void setRoutePoint(AmenityRoutePoint routePoint) {
		this.routePoint = routePoint;
	}
	
	public AmenityRoutePoint getRoutePoint() {
		return routePoint;
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
	
	public String getName(String lang) {
		if (lang != null) {
			String translateName;
			if (lang.equals("en")) {
				translateName = getEnName();
			} else {
				translateName = getAdditionalInfo("name:" + lang);
			}
			if (!Algorithms.isEmpty(translateName)) {
				return translateName;
			}
		}
		if (!Algorithms.isEmpty(getName())) {
			return getName();
		}
		for (String nm : getAdditionalInfo().keySet()) {
			if (nm.startsWith("name:")) {
				return getAdditionalInfo(nm);
			}
		}
		return "";
	}
	
	public String getContentLang(String tag, String lang) {
		if (lang != null) {
			String translateName = getAdditionalInfo(tag + ":" + lang);
			if (!Algorithms.isEmpty(translateName)) {
				return translateName;
			}
		}
		String plainName = getAdditionalInfo(tag);
		if (!Algorithms.isEmpty(plainName)) {
			return plainName;
		}
		String enName = getAdditionalInfo(tag + ":en");
		if (!Algorithms.isEmpty(enName)) {
			return enName;
		}
		for (String nm : getAdditionalInfo().keySet()) {
			if (nm.startsWith(tag + ":")) {
				return getAdditionalInfo(nm);
			}
		}
		return null;
	}
	
	public String getDescription(String lang) {
		String info = getContentLang(DESCRIPTION, lang);
		if(!Algorithms.isEmpty(info)) {
			return info;
		}
		return getContentLang(CONTENT, lang);
	}
	
	public void setDescription(String description) {
		setAdditionalInfo(DESCRIPTION, description);
	}
	
	public void setOpeningHours(String openingHours) {
		setAdditionalInfo(OPENING_HOURS, openingHours);
	}
	
	
}
