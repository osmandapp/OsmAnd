package net.osmand.plus.profiles.data;


public class RoutingDataObject extends ProfileDataObject implements Cloneable {

	private final String fileName;
	private final String derivedProfile;

	public RoutingDataObject(String stringKey,
	                         String name,
	                         String description,
	                         int iconRes,
	                         boolean isSelected,
	                         String fileName,
	                         String derivedProfile) {
		super(name, description, stringKey, iconRes, isSelected, null, null);
		this.fileName = fileName;
		this.derivedProfile = derivedProfile;
	}

	public String getFileName() {
		return fileName;
	}

	public String getDerivedProfile() {
		return derivedProfile;
	}

	public boolean isOnline() {
		return false;
	}

	public boolean isPredefined() {
		return false;
	}

}
