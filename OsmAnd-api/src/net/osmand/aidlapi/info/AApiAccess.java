package net.osmand.aidlapi.info;

import android.os.Bundle;
import android.os.Parcel;

import net.osmand.aidlapi.AidlParams;

import java.util.ArrayList;

/**
 * Access of the calling app to the API: whether the app is turned on in OsmAnd's Connected apps
 * and the ids of the permission groups granted to it.
 */
public class AApiAccess extends AidlParams {

	private boolean enabled;
	private ArrayList<String> grantedGroups;

	public AApiAccess(boolean enabled, ArrayList<String> grantedGroups) {
		this.enabled = enabled;
		this.grantedGroups = grantedGroups;
	}

	public AApiAccess(Parcel in) {
		readFromParcel(in);
	}

	public static final Creator<AApiAccess> CREATOR = new Creator<AApiAccess>() {
		@Override
		public AApiAccess createFromParcel(Parcel in) {
			return new AApiAccess(in);
		}

		@Override
		public AApiAccess[] newArray(int size) {
			return new AApiAccess[size];
		}
	};

	/**
	 * @return whether the app is turned on in Connected apps; a turned off app may call no method
	 * but getApiAccess
	 */
	public boolean isEnabled() {
		return enabled;
	}

	/**
	 * @return ids of the granted permission groups, such as map, navigation or settings
	 */
	public ArrayList<String> getGrantedGroups() {
		return grantedGroups;
	}

	@Override
	public void writeToBundle(Bundle bundle) {
		bundle.putBoolean("enabled", enabled);
		bundle.putStringArrayList("granted_groups", grantedGroups);
	}

	@Override
	protected void readFromBundle(Bundle bundle) {
		enabled = bundle.getBoolean("enabled");
		grantedGroups = bundle.getStringArrayList("granted_groups");
	}
}
