package net.osmand.core.samples.android.sample1.search.objects;

import net.osmand.core.jni.QStringStringHash;

public class PoiTypeSearchObject extends SearchObject {

	private ObjectType objectType;
	private String name;
	private String keyName;
	private String categoryKeyName;

	public enum ObjectType {
		CATEGORY,
		FILTER,
		TYPE
	}

	public PoiTypeSearchObject(ObjectType objectType, String name, String keyName, String categoryKeyName) {
		super(SearchObjectType.POI_TYPE, null);
		this.objectType = objectType;
		this.name = name;
		this.keyName = keyName;
		this.categoryKeyName = categoryKeyName;
	}

	public ObjectType getObjectType() {
		return objectType;
	}

	public String getKeyName() {
		return keyName;
	}

	public String getCategoryKeyName() {
		return categoryKeyName;
	}

	@Override
	public String getNativeName() {
		return name;
	}

	@Override
	protected QStringStringHash getLocalizedNames() {
		return null;
	}
}
