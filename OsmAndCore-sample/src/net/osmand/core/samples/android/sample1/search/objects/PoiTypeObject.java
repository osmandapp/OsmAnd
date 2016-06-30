package net.osmand.core.samples.android.sample1.search.objects;

public class PoiTypeObject {

	private String name;
	private String keyName;
	private String categoryKeyName;

	public PoiTypeObject(String name, String keyName, String categoryKeyName) {
		this.name = name;
		this.keyName = keyName;
		this.categoryKeyName = categoryKeyName;
	}

	public String getName() {
		return name;
	}

	public String getKeyName() {
		return keyName;
	}

	public String getCategoryKeyName() {
		return categoryKeyName;
	}
}
