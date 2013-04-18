package net.osmand.data;


import java.io.Serializable;
import java.util.Comparator;

import net.osmand.Collator;
import net.osmand.PlatformUtil;


public abstract class MapObject implements Comparable<MapObject>, Serializable {
	protected String name = null;
	protected String enName = null;
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
	
	public String getName(boolean en){
		if(en){
			return getEnName();
		} else {
			return getName();
		}
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
	
	public String getEnName() {
		if(this.enName != null){
			return this.enName;
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
		return PlatformUtil.primaryCollator().compare(getName(), o.getName());
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
		private final boolean en;
		Collator collator = PlatformUtil.primaryCollator();
		public MapObjectComparator(boolean en){
			this.en = en;
		}
		
		@Override
		public int compare(MapObject o1, MapObject o2) {
			if(en){
				return collator.compare(o1.getEnName(), o2.getEnName());
			} else {
				return collator.compare(o1.getName(), o2.getName());
			}
		}
	}	

}
