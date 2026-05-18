package net.osmand.plus.profiles.data;

import androidx.annotation.NonNull;

import static net.osmand.plus.onlinerouting.engine.OnlineRoutingEngine.isPredefinedEngineKey;

public class OnlineRoutingDataObject extends RoutingDataObject {

	private int order;

	public OnlineRoutingDataObject(String name,
	                               String description,
	                               String stringKey,
	                               int iconRes) {
		super(stringKey, name, description, iconRes, false, null, null);
	}

	@Override
	public boolean isOnline() {
		return true;
	}

	@Override
	public boolean isPredefined() {
		return isPredefinedEngineKey(getStringKey());
	}

	public void setOrder(int order) {
		this.order = order;
	}

	@Override
	public int compareTo(@NonNull ProfileDataObject profileDataObject) {
		if (profileDataObject instanceof OnlineRoutingDataObject) {
			OnlineRoutingDataObject another = (OnlineRoutingDataObject) profileDataObject;
			if (!isPredefined() && !another.isPredefined()) {
				return compareByOrder(another);
			} else {
				return compareByName(another);
			}
		}
		return 0;
	}

	public int compareByOrder(@NonNull OnlineRoutingDataObject another) {
		return (this.order < another.order) ? -1 : ((this.order == another.order) ? 0 : 1);
	}
}
