package net.osmand.data;

import net.osmand.Location;
import net.osmand.osm.PoiCategory;
import net.osmand.osm.edit.Node;
import net.osmand.util.Algorithms;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.GZIPInputStream;

import gnu.trove.list.array.TIntArrayList;


public class Amenity extends MapObject {

	public static final String WEBSITE = "website";
	public static final String PHONE = "phone";
	public static final String DESCRIPTION = "description";
	public static final String OPENING_HOURS = "opening_hours";
	public static final String CONTENT = "content";
	public static final String CUISINE = "cuisine";

	private String subType;
	private PoiCategory type;
	// duplicate for fast access
	private String openingHours;
	private Map<String, String> additionalInfo;
	private AmenityRoutePoint routePoint; // for search on path
	private TIntArrayList x;
	private TIntArrayList y;

	public Amenity() {
	}

	public static class AmenityRoutePoint {
		public double deviateDistance;
		public boolean deviationDirectionRight;
		public Location pointA;
		public Location pointB;
	}

	public PoiCategory getType() {
		return type;
	}

	public String getSubType() {
		return subType;
	}

	public void setType(PoiCategory type) {
		this.type = type;
	}

	public void setSubType(String subType) {
		this.subType = subType;
	}

	public TIntArrayList getX() {
		if (x == null) {
			x = new TIntArrayList();
		}
		return x;
	}
	
	public void setX(TIntArrayList x) {
		this.x = x;
	}
	
	public void setY(TIntArrayList y) {
		this.y = y;
	}

	public TIntArrayList getY() {
		if (y == null) {
			y = new TIntArrayList();
		}
		return y;
	}

	public String getOpeningHours() {
//		 getAdditionalInfo("opening_hours");
		return openingHours;
	}

	public String getAdditionalInfo(String key) {
		if (additionalInfo == null) {
			return null;
		}
		String str = additionalInfo.get(key);
		str = unzipContent(str);
		return str;
	}

	public String unzipContent(String str) {
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
					br.close();
					str = bld.toString();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return str;
	}

	public Map<String, String> getAdditionalInfo() {
		if (additionalInfo == null) {
			return Collections.emptyMap();
		}
		return additionalInfo;
	}

	public void setAdditionalInfo(Map<String, String> additionalInfo) {
		this.additionalInfo = null;
		openingHours = null;
		if (additionalInfo != null) {
			Iterator<Entry<String, String>> it = additionalInfo.entrySet().iterator();
			while (it.hasNext()) {
				Entry<String, String> e = it.next();
				setAdditionalInfo(e.getKey(), e.getValue());
			}
		}
	}

	public void setRoutePoint(AmenityRoutePoint routePoint) {
		this.routePoint = routePoint;
	}

	public AmenityRoutePoint getRoutePoint() {
		return routePoint;
	}

	public void setAdditionalInfo(String tag, String value) {
		if ("name".equals(tag)) {
			setName(value);
		} else if (tag.startsWith("name:")) {
			setName(tag.substring("name:".length()), value);
		} else {
			if (this.additionalInfo == null) {
				this.additionalInfo = new LinkedHashMap<String, String>();
			}
			this.additionalInfo.put(tag, value);
			if (OPENING_HOURS.equals(tag)) {
				this.openingHours = value;
			}
		}
	}

	@Override
	public String toStringEn() {
		return super.toStringEn() + ":" + type.getKeyName() + ":" + subType;
	}

	@Override
	public String toString() {
		return type.getKeyName() + " : " + subType + " " + getName();
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

	public String getContentLanguage(String tag, String lang, String defLang) {
		if (lang != null) {
			String translateName = getAdditionalInfo(tag + ":" + lang);
			if (!Algorithms.isEmpty(translateName)) {
				return lang;
			}
		}
		String plainContent = getAdditionalInfo(tag);
		if (!Algorithms.isEmpty(plainContent)) {
			return defLang;
		}
		String enName = getAdditionalInfo(tag + ":en");
		if (!Algorithms.isEmpty(enName)) {
			return "en";
		}
		int maxLen = 0;
		String lng = defLang;
		for (String nm : getAdditionalInfo().keySet()) {
			if (nm.startsWith(tag + ":")) {
				String key = nm.substring(tag.length() + 1);
				String cnt = getAdditionalInfo(tag + ":" + key);
				if (!Algorithms.isEmpty(cnt) && cnt.length() > maxLen) {
					maxLen = cnt.length();
					lng = key;
				}
			}
		}
		return lng;
	}

	public List<String> getNames(String tag, String defTag) {
		List<String> l = new ArrayList<String>();
		for (String nm : getAdditionalInfo().keySet()) {
			if (nm.startsWith(tag + ":")) {
				l.add(nm.substring(tag.length() + 1));
			} else if (nm.equals(tag)) {
				l.add(defTag);
			}
		}
		return l;
	}

	public String getTagContent(String tag, String lang) {
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
		String info = getTagContent(DESCRIPTION, lang);
		if (!Algorithms.isEmpty(info)) {
			return info;
		}
		return getTagContent(CONTENT, lang);
	}

	public void setDescription(String description) {
		setAdditionalInfo(DESCRIPTION, description);
	}

	public void setOpeningHours(String openingHours) {
		setAdditionalInfo(OPENING_HOURS, openingHours);
	}
	
	public boolean comparePoi(Amenity thatObj) {
		if (this.type.getKeyName().equals(thatObj.type.getKeyName())) {
			boolean equals = true;
			if (equals) {
				if (this.x == null || thatObj.x == null) {
					equals = this.x == thatObj.x;
				} else if (this.x.size() != thatObj.x.size()){
					equals = false;
				} else {
					for (int i = 0; i < x.size() && equals; i++) {
						equals = this.x.get(i) == thatObj.x.get(i);
					}
				}
			}
			if (equals) {
				if (this.y == null || thatObj.y == null) {
					equals = this.y == thatObj.y;
				} else if (this.y.size() != thatObj.y.size()){
					equals = false;
				} else {
					for (int i = 0; i < y.size() && equals; i++) {
						equals = this.y.get(i) == thatObj.y.get(i);
					}
				}
			}
			if (equals) {
				if (this.subType == null || thatObj.subType == null) {
					equals = this.subType == thatObj.subType;
				} else {
					equals = subType.equals(thatObj.subType);
				}
			}
			if (equals) {
				if (this.additionalInfo == null || thatObj.additionalInfo == null) {
					equals = this.additionalInfo == thatObj.additionalInfo;
				} else {
					equals = this.additionalInfo.equals(thatObj.additionalInfo);
				}
				
			}
			if (equals) {
				if (this.openingHours == null || thatObj.openingHours == null) {
					if (this.openingHours == thatObj.openingHours) {
						equals = true;
					} else {
						equals = false;
					}
				} else {
					if (this.openingHours == null || thatObj.openingHours == null) {
						equals = this.openingHours == thatObj.openingHours;
					} else {
						equals = this.openingHours.equals(thatObj.openingHours);
					}
				}
			}
			if (equals) {
				equals = this.routePoint == thatObj.routePoint;
			}
			return equals;
		}
		return false;
	}
	
	@Override
	public int compareTo(MapObject o) {
		int cmp = super.compareTo(o);
		if(cmp == 0 && o instanceof Amenity) {
			int kn = ((Amenity) o).getType().getKeyName().compareTo(getType().getKeyName());
			if(kn == 0) {
				kn = ((Amenity) o).getSubType().compareTo(getSubType());
			}
			return kn;
		}
		return cmp;
	}

	@Override
	public boolean equals(Object o) {
		boolean res = super.equals(o);
		if (res && o instanceof Amenity) {
			return Algorithms.stringsEqual(((Amenity) o).getType().getKeyName(), getType().getKeyName())
					&& Algorithms.stringsEqual(((Amenity) o).getSubType(), getSubType());
		}
		return res;
	}
}
