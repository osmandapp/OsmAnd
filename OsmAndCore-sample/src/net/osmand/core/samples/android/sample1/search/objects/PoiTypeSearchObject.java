package net.osmand.core.samples.android.sample1.search.objects;

import net.osmand.core.jni.QStringStringHash;

public class PoiTypeSearchObject extends SearchObject<PoiTypeObject> {

	private ObjectType objectType;

	public enum ObjectType {
		CATEGORY,
		FILTER,
		TYPE
	}

	public PoiTypeSearchObject(ObjectType objectType, PoiTypeObject poiTypeObject) {
		super(SearchObjectType.POI_TYPE, poiTypeObject);
		this.objectType = objectType;
	}

	public ObjectType getObjectType() {
		return objectType;
	}

	public String getKeyName() {
		return getBaseObject().getKeyName();
	}

	public String getCategoryKeyName() {
		return getBaseObject().getCategoryKeyName();
	}

	@Override
	public String getNativeName() {
		return getBaseObject().getName();
	}

	@Override
	protected QStringStringHash getLocalizedNames() {
		return null;
	}
}
