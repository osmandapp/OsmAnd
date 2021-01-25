package net.osmand.data;


import net.osmand.util.Algorithms;

import org.json.JSONObject;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public class Building extends MapObject {
	
	private String postcode;
	private LatLon latLon2;
	private BuildingInterpolation interpolationType;
	private int interpolationInterval;
	private String name2;
	private Map<String, LatLon> entrances = null;
	
	public enum BuildingInterpolation {
		ALL(-1), EVEN(-2), ODD(-3), ALPHABETIC(-4);
		private final int val;

		BuildingInterpolation(int val) {
			this.val = val;
		}
		public int getValue() {
			return val;
		}
		
		public static BuildingInterpolation fromValue(int i){
			for(BuildingInterpolation b : values()) {
				if(b.val == i) {
					return b;
				}
			}
			return null;
		}
	}

	public Building(){}
	
	public String getPostcode() {
		return postcode;
	}
	
	public Map<String, LatLon> getEntrances() {
		if(entrances == null) {
			return Collections.emptyMap();
		}
		return entrances;
	}
	
	public void addEntrance(String ref, LatLon location) {
		if(entrances == null) {
			entrances = new LinkedHashMap<>();
		}
		entrances.put(ref, location);
	}
	
	public int getInterpolationInterval() {
		return interpolationInterval;
	}
	public void setInterpolationInterval(int interpolationNumber) {
		this.interpolationInterval = interpolationNumber;
	}
	
	public BuildingInterpolation getInterpolationType() {
		return interpolationType;
	}
	
	public void setInterpolationType(BuildingInterpolation interpolationType) {
		this.interpolationType = interpolationType;
	}
	
	public LatLon getLatLon2() {
		return latLon2;
	}
	public void setLatLon2(LatLon latlon2) {
		this.latLon2 = latlon2;
	}
	public String getName2() {
		return name2;
	}
	
	public void setName2(String name2) {
		this.name2 = name2;
	}
	
	public void setPostcode(String postcode) {
		this.postcode = postcode;
	}
	
	@Override
	public String getName(String lang) {
		String fname = super.getName(lang);
		if (interpolationInterval != 0) {
			return fname + "-" + name2 + " (+" + interpolationInterval + ") ";
		} else if (interpolationType != null) {
			return fname + "-" + name2 + " (" + interpolationType.toString().toLowerCase() + ") ";
		}
		return name;
	}	
	

	public float interpolation(String hno) {
		if (getInterpolationType() != null || getInterpolationInterval() > 0
		// || checkNameAsInterpolation() // disable due to situation in NL #4284
		) {
			int num = Algorithms.extractFirstIntegerNumber(hno);
			String fname = super.getName();
			int numB = Algorithms.extractFirstIntegerNumber(fname);
			int numT = numB;
			String sname = getName2();
			if (getInterpolationType() == BuildingInterpolation.ALPHABETIC) {
				if (num != numB) {
					// currently not supported
					return -1;
				}
				int hint = (int) hno.charAt(hno.length() - 1);
				int fch = (int) fname.charAt(fname.length() - 1);
				int sch = sname.charAt(sname.length() - 1);
				if (fch == sch) {
					return -1;
				}
				float res = ((float) hint - fch) / (((float) sch - fch));
				if (res > 1 || res < 0) {
					return -1;
				} 
				return res;
			}
			if (num >= numB) {
				if (fname.contains("-") && sname == null) {
					int l = fname.indexOf('-');
					sname = fname.substring(l + 1, fname.length());
				}
				if (sname != null) {
					numT = Algorithms.extractFirstIntegerNumber(sname);
					if (numT < num) {
						return -1;
					}
				}
				if (getInterpolationType() == BuildingInterpolation.EVEN && num % 2 == 1) {
					return -1;
				}
				if (getInterpolationType() == BuildingInterpolation.ODD && num % 2 == 0) {
					return -1;
				}
				if (getInterpolationInterval() != 0 && (num - numB) % getInterpolationInterval() != 0) {
					return -1;
				}
			} else {
				return -1;
			}
			if (numT > numB) {
				return ((float) num - numB) / (((float) numT - numB));
			}
			return 0;
		}
		return -1;
	}
	
	protected boolean checkNameAsInterpolation() {
		String nm = super.getName();
		boolean interpolation = nm.contains("-");
		if(interpolation) {
			for(int i = 0; i < nm.length(); i++) {
				if(!(nm.charAt(i) >= '0' && nm.charAt(i) <= '9') && nm.charAt(i) != '-') {
					interpolation = false;
					break;
				}
			}
		}
		return interpolation;
	}

	public boolean belongsToInterpolation(String hno) {
		return interpolation(hno) >= 0;
	}
	
	@Override
	public String toString() {
		if (interpolationInterval != 0) {
			return name + "-" + name2 + " (+" + interpolationInterval + ") ";
		} else if (interpolationType != null) {
			return name + "-" + name2 + " (" + interpolationType + ") ";
		}
		return name;
	}

	public LatLon getLocation(float interpolation) {
		LatLon loc = getLocation();
		LatLon latLon2 = getLatLon2();
		if (latLon2 != null) {
			double lat1 = loc.getLatitude();
			double lat2 = latLon2.getLatitude();
			double lon1 = loc.getLongitude();
			double lon2 = latLon2.getLongitude();
			return new LatLon(interpolation * (lat2 - lat1) + lat1, interpolation * (lon2 - lon1) + lon1);
		}
		return loc;
	}
	
	
	@Override
	public boolean equals(Object o) {
		boolean res = super.equals(o);
		if (res && o instanceof Building) {
			return Algorithms.stringsEqual(((MapObject) o).getName(), getName());
		}
		return res;
	}

	public String getInterpolationName(double coeff) {
		if (!Algorithms.isEmpty(getName2())) {
			int fi = Algorithms.extractFirstIntegerNumber(getName());
			int si = Algorithms.extractFirstIntegerNumber(getName2());
			if (si != 0 && fi != 0) {
				int num = (int) (fi + (si - fi) * coeff);
				BuildingInterpolation type = getInterpolationType();
				if (type == BuildingInterpolation.EVEN || type == BuildingInterpolation.ODD) {
					if (num % 2 == (type == BuildingInterpolation.EVEN ? 1 : 0)) {
						num--;
					}
				} else if (getInterpolationInterval() > 0) {
					int intv = getInterpolationInterval();
					if ((num - fi) % intv != 0) {
						num = ((num - fi) / intv) * intv + fi;
					}
				}
				return num + "";
			}
		}
		return "";
	}

	public JSONObject toJSON() {
		JSONObject json = super.toJSON();
		json.put("postcode", postcode);
		if (latLon2 != null) {
			json.put("lat2", String.format(Locale.US, "%.5f", latLon2.getLatitude()));
			json.put("lon2", String.format(Locale.US, "%.5f", latLon2.getLongitude()));
		}
		if (interpolationType != null) {
			json.put("interpolationType", interpolationType.name());
		}
		if (interpolationInterval != 0) {
			json.put("interpolationInterval", interpolationInterval);
		}
		json.put("name2", name2);

		return json;
	}

	public static Building parseJSON(JSONObject json) throws IllegalArgumentException {
		Building b = new Building();
		MapObject.parseJSON(json, b);

		if (json.has("postcode")) {
			b.postcode = json.getString("postcode");
		}
		if (json.has("lat2") && json.has("lon2")) {
			b.latLon2 = new LatLon(json.getDouble("lat2"), json.getDouble("lon2"));
		}
		if (json.has("interpolationType")) {
			b.interpolationType = BuildingInterpolation.valueOf(json.getString("interpolationType"));
		}
		if (json.has("interpolationInterval")) {
			b.interpolationInterval = json.getInt("interpolationInterval");
		}
		if (json.has("name2")) {
			b.name2 = json.getString("name2");
		}
		return b;
	}
}
