package net.osmand.plus.profiles.dto;

import androidx.annotation.NonNull;

import net.osmand.plus.R;

public class OnlineRoutingDataObject extends ProfileDataObject {

	private int order;

	public OnlineRoutingDataObject(String name,
	                               String description,
	                               String stringKey,
	                               int order) {
		super(name, description, stringKey, R.drawable.ic_world_globe_dark, false, null, null);
		this.order = order;
	}

	@Override
	public int compareTo(@NonNull ProfileDataObject profileDataObject) {
		if (profileDataObject instanceof OnlineRoutingDataObject) {
			OnlineRoutingDataObject another = (OnlineRoutingDataObject) profileDataObject;
			return (this.order < another.order) ? -1 : ((this.order == another.order) ? 0 : 1);
		}
		return 0;
	}
}
