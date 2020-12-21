package net.osmand.plus.profiles;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;


public class ProfileDataObject implements Comparable<ProfileDataObject> {

	private String name;
	private String description;
	private int iconRes;
	private String stringKey;
	private boolean isSelected;
	private boolean isEnabled;
	private ProfileIconColors iconColor;

	public ProfileDataObject(String name, String description, String stringKey, int iconRes,  boolean isSelected, ProfileIconColors iconColor) {
		this.name = name;
		this.iconRes = iconRes;
		this.description = description;
		this.isSelected = isSelected;
		this.stringKey = stringKey;
		this.iconColor = iconColor;
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

	public boolean isEnabled() {
		return isEnabled;
	}

	public void setEnabled(boolean enabled) {
		isEnabled = enabled;
	}

	public String getStringKey() {
		return stringKey;
	}

	@ColorRes  public int getIconColor(boolean isNightMode) {
		return iconColor.getColor(isNightMode);
	}

	@Override
	public int compareTo(@NonNull ProfileDataObject another) {
		return this.name.compareToIgnoreCase(another.name);
	}

}
