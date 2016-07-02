package net.osmand.search.example.core;

import net.osmand.data.LatLon;

public class SearchWord {
	private String word;
	private ObjectType type;
	private Object internalObject;
	private LatLon location;
	
	public ObjectType getType() {
		return type;
	}
	
	public String getWord() {
		return word;
	}
	
	public Object getInternalObject() {
		return internalObject;
	}
	
	public LatLon getLocation() {
		return location;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		result = prime * result + ((word == null) ? 0 : word.hashCode());
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
		SearchWord other = (SearchWord) obj;
		if (type != other.type)
			return false;
		if (word == null) {
			if (other.word != null)
				return false;
		} else if (!word.equals(other.word))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return word;
	}
}