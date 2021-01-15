package net.osmand.plus.profiles;

import androidx.annotation.NonNull;

import net.osmand.plus.R;

public class OnlineRoutingEngineDataObject extends ProfileDataObject {

	private int order;

	public OnlineRoutingEngineDataObject(String name,
	                                     String description,
	                                     String stringKey,
	                                     int order) {
		super(name, description, stringKey, R.drawable.ic_world_globe_dark, false, null);
		this.order = order;
	}

	@Override
	public int compareTo(@NonNull ProfileDataObject another) {
		if (another instanceof OnlineRoutingEngineDataObject) {
			OnlineRoutingEngineDataObject anotherEngine = (OnlineRoutingEngineDataObject) another;
			return Integer.compare(this.order, anotherEngine.order);
		}
		return 0;
	}
}
