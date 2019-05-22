package net.osmand.plus.profiles;

public class ProfileDataObject {

	private String name;
	private String description;
	private int iconRes;
	private String stringKey;
	private boolean isSelected;

	public ProfileDataObject(String name, String description, String stringKey, int iconRes,  boolean isSelected) {
		this.name = name;
		this.iconRes = iconRes;
		this.description = description;
		this.isSelected = isSelected;
		this.stringKey = stringKey;
	}

	public String getName() {
		return name;
	}

	public int getIconRes() {
		return iconRes;
	}

	public String getDescription() {
		return description;
	}

	public boolean isSelected() {
		return isSelected;
	}

	public void setSelected(boolean selected) {
		isSelected = selected;
	}

	public String getStringKey() {
		return stringKey;
	}
}
