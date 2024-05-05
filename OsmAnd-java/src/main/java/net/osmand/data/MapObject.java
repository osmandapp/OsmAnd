package net.osmand.data;


import net.osmand.Collator;
import net.osmand.OsmAndCollator;
import net.osmand.util.Algorithms;
import net.osmand.util.TransliterationHelper;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.GZIPInputStream;


public abstract class MapObject implements Comparable<MapObject> {

	public static final MapObjectComparator BY_NAME_COMPARATOR = new MapObjectComparator();

	public static final byte AMENITY_ID_RIGHT_SHIFT = 1;
	public static final byte WAY_MODULO_REMAINDER = 1;

	protected String name = null;
	protected String enName = null;
	/**
	 * Looks like: {ru=Москва, dz=མོསི་ཀོ...} and does not contain values of OSM tags "name" and "name:en",
	 * see {@link name} and {@link enName} respectively.
	 */
	protected Map<String, String> names = null;
	protected LatLon location = null;
	protected long fileOffset = 0;
	protected Long id = null;
	private Object referenceFile = null;


	public void setId(Long id) {
		this.id = id;
	}

	public Long getId() {
		if (id != null) {
			return id;
		}
		return null;
	}

	public String getName() {
		if (name != null) {
			return unzipContent(name);
		}
		return ""; //$NON-NLS-1$
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setName(String lang, String name) {
		if (Algorithms.isEmpty(lang)) {
			setName(name);
		} else if (lang.equals("en")) {
			setEnName(name);
		} else {
			if (names == null) {
				names = new HashMap<String, String>();
			}
			names.put(lang, unzipContent(name));
		}
	}

	public void setNames(Map<String, String> name) {
		if (name != null) {
			if (names == null) {
				names = new HashMap<String, String>();
			}
			names.putAll(name);
		}
	}
	
	public Map<String, String> getNamesMap(boolean includeEn) {
		if ((!includeEn || Algorithms.isEmpty(enName)) && names == null) {
			return Collections.emptyMap();
		}
		Map<String, String> mp = new HashMap<String, String>();
		if (names != null) {
			Iterator<Entry<String, String>> it = names.entrySet().iterator();
			while (it.hasNext()) {
				Entry<String, String> e = it.next();
				mp.put(e.getKey(), unzipContent(e.getValue()));
			}
		}
		if (includeEn && !Algorithms.isEmpty(enName)) {
			mp.put("en", unzipContent(enName));
		}
		return mp;
	}

	public List<String> getOtherNames() {
		return getOtherNames(false);
	}
	
	public List<String> getOtherNames(boolean transliterate) {
		List<String> l = new ArrayList<String>();
		String enName = getEnName(transliterate); 
		if (!Algorithms.isEmpty(enName)) {
			l.add(enName);
		}
		if (names != null) {
			l.addAll(names.values());
		}
		return l;
	}

	public void copyNames(String otherName, String otherEnName, Map<String, String> otherNames, boolean overwrite) {
		if (!Algorithms.isEmpty(otherName) && (overwrite || Algorithms.isEmpty(name))) {
			name = otherName;
		}
		if (!Algorithms.isEmpty(otherEnName) && (overwrite || Algorithms.isEmpty(enName))) {
			enName = otherEnName;
		}
		if (!Algorithms.isEmpty(otherNames)) {
			if (otherNames.containsKey("name:en")) {
				enName = otherNames.get("name:en");
			} else if (otherNames.containsKey("en")) {
				enName = otherNames.get("en");
			}

			for (Entry<String, String> e : otherNames.entrySet()) {
				String key = e.getKey();
				if (key.startsWith("name:")) {
					key = key.substring("name:".length());
				}
				if (names == null) {
					names = new HashMap<String, String>();
				}
				if (overwrite || Algorithms.isEmpty(names.get(key))) {
					names.put(key, e.getValue());
				}
			}
		}
	}

	public void copyNames(String otherName, String otherEnName, Map<String, String> otherNames) {
		copyNames(otherName, otherEnName, otherNames, false);
	}

	public void copyNames(MapObject s, boolean copyName, boolean copyEnName, boolean overwrite) {
		copyNames((copyName ? s.name : null), (copyEnName ? s.enName : null), s.names, overwrite);
	}

	public void copyNames(MapObject s) {
		copyNames(s, true, true, false);
	}

	public String getName(String lang) {
		return getName(lang, false);
	}

	public String getName(String lang, boolean transliterate) {
		if (lang != null && lang.length() > 0) {
			if (lang.equals("en")) {
				// for some objects like wikipedia, english name is stored 'name' tag
				String enName = getEnName(transliterate);
				return !Algorithms.isEmpty(enName) ? enName : getName();
			} else {
				// get name
				if (names != null) {
					String nm = names.get(lang);
					if (!Algorithms.isEmpty(nm)) {
						return unzipContent(nm);
					}
					if (transliterate) {
						return TransliterationHelper.transliterate(getName());
					}
				}
			}
		}
		return getName();
	}

	public String getEnName(boolean transliterate) {
		if (!Algorithms.isEmpty(enName)) {
			return unzipContent(this.enName);
		} else if (!Algorithms.isEmpty(getName()) && transliterate) {
			return TransliterationHelper.transliterate(getName());
		}
		return ""; //$NON-NLS-1$
	}

	public void setEnName(String enName) {
		this.enName = enName;
	}

	public LatLon getLocation() {
		return location;
	}

	public void setLocation(double latitude, double longitude) {
		location = new LatLon(latitude, longitude);
	}

	public void setLocation(LatLon loc) {
		location = loc;
	}

	@Override
	public int compareTo(MapObject o) {
		return OsmAndCollator.primaryCollator().compare(getName(), o.getName());
	}

	public long getFileOffset() {
		return fileOffset;
	}

	public void setFileOffset(long fileOffset) {
		this.fileOffset = fileOffset;
	}

	public String toStringEn() {
		return getClass().getSimpleName() + ":" + getEnName(true);
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + " " + name + "(" + id + ")"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MapObject other = (MapObject) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}

	public boolean compareObject(MapObject thatObj) {
		if (this == thatObj) {
			return true;
		} else {
			if(thatObj == null || this.id == null || thatObj.id == null) {
				return false;
			}
			return this.id.longValue() == thatObj.id.longValue() &&
					Algorithms.objectEquals(getLocation(), thatObj.getLocation()) &&
					Algorithms.objectEquals(this.getName(), thatObj.getName()) &&
					Algorithms.objectEquals(this.getNamesMap(true), thatObj.getNamesMap(true));
		}
	}

	public static class MapObjectComparator implements Comparator<MapObject> {
		private final String l;
		Collator collator = OsmAndCollator.primaryCollator();
		private boolean transliterate;

		public MapObjectComparator() {
			this.l = null;
		}

		public MapObjectComparator(String lang, boolean transliterate) {
			this.l = lang;
			this.transliterate = transliterate;
		}

		@Override
		public int compare(MapObject o1, MapObject o2) {
			if (o1 == null ^ o2 == null) {
				return (o1 == null) ? -1 : 1;
			} else if (o1 == o2) {
				return 0;
			} else {
				return collator.compare(o1.getName(l, transliterate), o2.getName(l, transliterate));
			}
		}

		public boolean areEqual(MapObject o1, MapObject o2) {
			if (o1 == null ^ o2 == null) {
				return false;
			} else if (o1 == o2) {
				return true;
			} else {
				return collator.equals(o1.getName(l, transliterate), o2.getName(l, transliterate));
			}
		}
	}
	
	public void setReferenceFile(Object referenceFile) {
		this.referenceFile = referenceFile;
	}
	
	public Object getReferenceFile() {
		return referenceFile;
	}

	public JSONObject toJSON() {
		JSONObject json = new JSONObject();
		json.put("name", unzipContent(name));
		json.put("enName", unzipContent(enName));
		if (names != null && names.size() > 0) {
			JSONObject namesObj = new JSONObject();
			for (Entry<String, String> e : names.entrySet()) {
				namesObj.put(e.getKey(), unzipContent(e.getValue()));
			}
			json.put("names", namesObj);
		}
		if (location != null) {
			json.put("lat", String.format(Locale.US, "%.5f", location.getLatitude()));
			json.put("lon", String.format(Locale.US, "%.5f", location.getLongitude()));
		}
		json.put("id", id);

		return json;
	}
	
	public static String unzipContent(String str) {
		if (isContentZipped(str)) {
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
				// ugly fix of temporary problem of map generation
				if(isContentZipped(str)) {
					str = unzipContent(str);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return str;
	}

	public static boolean isContentZipped(String str) {
		return str != null && str.startsWith(" gz ");
	}

	protected static void parseJSON(JSONObject json, MapObject o) {
		if (json.has("name")) {
			o.name = json.getString("name");
		}
		if (json.has("enName")) {
			o.enName = json.getString("enName");
		}
		if (json.has("names")) {
			JSONObject namesObj = json.getJSONObject("names");
			o.names = new HashMap<>();
			Iterator<String> iterator = namesObj.keys();
			while (iterator.hasNext()) {
				String key = iterator.next();
				String value = namesObj.getString(key);
				o.names.put(key, value);
			}
		}
		if (json.has("lat") && json.has("lon")) {
			o.location = new LatLon(json.getDouble("lat"), json.getDouble("lon"));
		}
		if (json.has("id")) {
			o.id = json.getLong("id");
		}
	}
}
