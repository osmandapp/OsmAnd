package net.osmand.plus.profiles.data;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

public class RoutingFile {
	private final String fileName;
	private final List<RoutingDataObject> profiles = new ArrayList<>();

	public RoutingFile(String fileName) {
		this.fileName = fileName;
	}

	public String getFileName() {
		return fileName;
	}

	public List<RoutingDataObject> getProfiles() {
		return profiles;
	}

	public void addProfile(@NonNull RoutingDataObject profile) {
		profiles.add(profile);
	}
}
