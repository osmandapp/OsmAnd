package net.osmand.plus.profiles.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;

public class RoutingProfilesHolder {

	private final Map<String, RoutingDataObject> map = new HashMap<>();

	@Nullable
	public RoutingDataObject get(@NonNull String routingProfileKey, @Nullable String derivedProfile) {
		return map.get(createFullKey(routingProfileKey, derivedProfile));
	}

	public void add(@NonNull RoutingDataObject profile) {
		map.put(createFullKey(profile.getStringKey(), profile.getDerivedProfile()), profile);
	}

	public void setSelected(@NonNull RoutingDataObject selected) {
		for (RoutingDataObject profile : map.values()) {
			profile.setSelected(false);
		}
		selected.setSelected(true);
	}

	private String createFullKey(@NonNull String routingProfileKey, @Nullable String derivedProfile) {
		if (derivedProfile == null) {
			derivedProfile = "default";
		}
		return routingProfileKey + "_" + derivedProfile;
	}

}
