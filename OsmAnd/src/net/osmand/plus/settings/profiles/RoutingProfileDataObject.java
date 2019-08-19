package net.osmand.plus.settings.profiles;


public class RoutingProfileDataObject extends ProfileDataObject {

	private String fileName;

	public RoutingProfileDataObject(String stringKey, String name, String descr,  int iconRes, boolean isSelected, String fileName) {
		super(name, descr, stringKey, iconRes, isSelected, null);
		this.fileName = fileName;
	}

	public String getFileName() {
		return fileName;
	}

}
