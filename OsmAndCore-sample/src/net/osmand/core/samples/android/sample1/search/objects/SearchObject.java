package net.osmand.core.samples.android.sample1.search.objects;

import net.osmand.core.jni.QStringStringHash;

public abstract class SearchObject<T> {

	public enum SearchObjectType {
		CITY,
		VILLAGE,
		POSTCODE,
		STREET,
		STREET_INTERSECTION,
		BUILDING,
		POI_TYPE,
		POI,
		COORDINATES
	}

	private SearchObjectType type;
	private T baseObject;

	private double priority;

	protected SearchObject(SearchObjectType type, T baseObject) {
		this.type = type;
		this.baseObject = baseObject;
	}

	public SearchObjectType getType() {
		return type;
	}

	public T getBaseObject() {
		return baseObject;
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

	public double getPriority() {
		return priority;
	}

	public void setPriority(double priority) {
		this.priority = priority;
	}

	protected abstract QStringStringHash getLocalizedNames();

	@Override
	public String toString() {
		return "SearchObject: " + getNativeName();
	}
}
