package net.osmand.plus.profiles;

import android.os.Parcel;


public class RoutingProfileDataObject extends ProfileDataObject {

	private String fileName;

	public RoutingProfileDataObject(String stringKey, String name, String descr,  int iconRes, boolean isSelected, String fileName) {
		super(name, descr, stringKey, iconRes, isSelected);
		this.fileName = fileName;
	}

	public String getFileName() {
		return fileName;
	}

}
