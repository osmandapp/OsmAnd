package net.osmand.plus.track;

public class AppearanceListItem {

	private final String attrName;
	private String value;
	private final String localizedValue;
	private int color;
	private boolean lastItem;

	public AppearanceListItem(String attrName, String value, String localizedValue) {
		this.attrName = attrName;
		this.value = value;
		this.localizedValue = localizedValue;
	}

	public AppearanceListItem(String attrName, String value, String localizedValue, int color) {
		this.attrName = attrName;
		this.value = value;
		this.localizedValue = localizedValue;
		this.color = color;
	}

	public String getAttrName() {
		return attrName;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public String getLocalizedValue() {
		return localizedValue;
	}

	public int getColor() {
		return color;
	}

	public boolean isLastItem() {
		return lastItem;
	}

	public void setLastItem(boolean lastItem) {
		this.lastItem = lastItem;
	}
}
