package net.osmand.data;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import net.osmand.Collator;
import net.osmand.OsmAndCollator;
import net.osmand.util.Algorithms;
import net.sf.junidecode.Junidecode;


public abstract class MapObject implements Comparable<MapObject> {
	protected String name = null;
	protected String enName = null;
	protected Map<String, String> names = null;
	protected LatLon location = null;
	protected int fileOffset = 0;
	protected Long id = null;

	public void setId(Long id) {
		this.id = id;
	}
	
	public Long getId() {
		if(id != null){
			return id;
		}
		return null;
	}
	
	public String getName() {
		if (this.name != null) {
			return this.name;
		}
		return ""; //$NON-NLS-1$
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public void setName(String lang, String name) {
		if(names == null) {
			names = new HashMap<String, String>();
			
		}
		names.put(lang, name);
	}
	
	public Map<String, String> getNamesMap(boolean includeEn) {
		if (!includeEn || Algorithms.isEmpty(enName)) {
			if (names == null) {
				return Collections.emptyMap();
			}
			return names;
		}
		Map<String, String> mp = new HashMap<String, String>();
		if(names != null) {
			mp.putAll(names);
		}
		mp.put("en", enName);		
		return mp;
	}
	
	public List<String> getAllNames() {
		List<String> l = new ArrayList<String>();
		if(!Algorithms.isEmpty(enName)) {
			l.add(enName);
		}
		if(names != null) {
			l.addAll(names.values());
		}
		return l;
	}
	
	public void copyNames(MapObject s) {
		if(Algorithms.isEmpty(name)) {
			name = s.name;
		}
		if(Algorithms.isEmpty(enName)) {
			enName = s.enName;
		}
		copyNames(s.names);
	}
	
	public void copyNames(Map<String, String> snames) {
		if(snames != null && snames.containsKey("name:en")){
			enName = snames.get("name:en");
		}
		if(snames != null && snames.containsKey("en")){
			enName = snames.get("en");
		}
		if(snames != null){
			Iterator<Entry<String, String>> it = snames.entrySet().iterator();
			while(it.hasNext()) {
				Entry<String, String> e = it.next();
				String key = e.getKey();
				if(key.startsWith("name:")) {
					key = key.substring("name:".length());
				}
				if(names == null) {
					names = new HashMap<String, String>();
				}
				if(Algorithms.isEmpty(names.get(key))) {
					names.put(key, e.getValue());
				}
			}
		}		
	}
	
	public String getName(String lang) {
		return getName(lang, false);
	}
	
	public String getName(String lang, boolean transliterate) {
		if (lang != null) {
			if (lang.equals("en")) {
				// ignore transliterate option here for backward compatibility
				return getEnName(true);
			} else {
				// get name
				if(names != null) {
					String nm = names.get(lang);
					if(!Algorithms.isEmpty(nm)) {
						return nm;
					}
					if(transliterate) {
						return Junidecode.unidecode(getName());
					}
				}
			}
		}
		return getName();
	}
	
	public String getEnName(boolean transliterate) {
		if(!Algorithms.isEmpty(enName)){
			return this.enName;
		} else if(!Algorithms.isEmpty(getName()) && transliterate){
			return Junidecode.unidecode(getName());
		}
		return ""; //$NON-NLS-1$
	}
	
	public void setEnName(String enName) {
		this.enName = enName;
	}
	
	public LatLon getLocation(){
		return location;
	}
	
	public void setLocation(double latitude, double longitude){
		location = new LatLon(latitude, longitude);
	}
	
	@Override
	public int compareTo(MapObject o) {
		return OsmAndCollator.primaryCollator().compare(getName(), o.getName());
	}
	
	public int getFileOffset() {
		return fileOffset;
	}
	
	public void setFileOffset(int fileOffset) {
		this.fileOffset = fileOffset;
	}
	
	@Override
	public String toString() {
		return getClass().getSimpleName() + " " + name +"("+id+")"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
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
	
	public static class MapObjectComparator implements Comparator<MapObject>{
		private final String l;
		Collator collator = OsmAndCollator.primaryCollator();
		public MapObjectComparator(String lang){
			this.l = lang;
		}
		
		@Override
		public int compare(MapObject o1, MapObject o2) {
			return collator.compare(o1.getName(l), o2.getName(l));
		}
	}	

}
