package net.osmand.plus.profiles.dto;

import androidx.annotation.NonNull;

import net.osmand.plus.R;

public class OnlineRoutingDataObject extends RoutingDataObject {

	private int order;
	private boolean isPredefined;

	public OnlineRoutingDataObject(String name,
	                               String description,
	                               String stringKey,
	                               int iconRes,
	                               boolean isPredefined,
	                               int order) {
		super(stringKey, name, description, iconRes, false, null);
		this.order = order;
		this.isPredefined = isPredefined;
	}

	@Override
	public boolean isOnline() {
		return true;
	}

	@Override
	public boolean isPredefined() {
		return isPredefined;
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
