package net.osmand.plus.profiles;

import androidx.annotation.NonNull;

import net.osmand.plus.R;

public class OnlineRoutingEngineDataObject extends ProfileDataObject {

	private int order;

	public OnlineRoutingEngineDataObject(String name,
	                                     String description,
	                                     String stringKey,
	                                     int order) {
		super(name, description, stringKey, R.drawable.ic_world_globe_dark, false, null, null);
		this.order = order;
	}

	@Override
	public int compareTo(@NonNull ProfileDataObject profileDataObject) {
		if (profileDataObject instanceof OnlineRoutingEngineDataObject) {
			OnlineRoutingEngineDataObject another = (OnlineRoutingEngineDataObject) profileDataObject;
			return (this.order < another.order) ? -1 : ((this.order == another.order) ? 0 : 1);
		}
		return 0;
	}
}
