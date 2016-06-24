package net.osmand.core.samples.android.sample1.search.objects;

import net.osmand.core.jni.QStringStringHash;

public abstract class SearchObject {

	public enum SearchObjectType {
		CITY,
		VILLAGE,
		POSTCODE,
		STREET,
		BUILDING,
		POI_TYPE,
		POI_FILTER,
		POI,
		COORDINATES
	}

	private SearchObjectType type;
	private Object internalObject;

	private float priority;
	private boolean sortByName;

	protected SearchObject(SearchObjectType type, Object internalObject) {
		this.type = type;
		this.internalObject = internalObject;
	}

	public SearchObjectType getType() {
		return type;
	}

	protected Object getInternalObject() {
		return internalObject;
	}

	public abstract String getNativeName();

	public String getName(String lang) {
		QStringStringHash locNames = getLocalizedNames();
		if (locNames != null && lang != null) {
			String locName = null;
			if (locNames.has_key(lang)) {
				locName = locNames.get(lang);
			}
			return locName == null ? getNativeName() : locName;
		} else {
			return getNativeName();
		}
	}

	public String getNameEn() {
		QStringStringHash locNames = getLocalizedNames();
		if (locNames != null && locNames.has_key("en")) {
			return locNames.get("en");
		} else {
			return null;
		}
	}

	public float getPriority() {
		return priority;
	}

	public void setPriority(float priority) {
		this.priority = priority;
	}

	public boolean isSortByName() {
		return sortByName;
	}

	public void setSortByName(boolean sortByName) {
		this.sortByName = sortByName;
	}

	protected abstract QStringStringHash getLocalizedNames();

	@Override
	public String toString() {
		return "SearchObject: " + getNativeName();
	}
}
