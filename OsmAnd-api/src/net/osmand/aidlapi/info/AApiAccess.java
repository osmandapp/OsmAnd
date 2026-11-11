package net.osmand.aidlapi.info;

import android.os.Bundle;
import android.os.Parcel;

import net.osmand.aidlapi.AidlParams;

import java.util.ArrayList;

/**
 * Access of the calling app to the API: whether the app is turned on in OsmAnd's Connected apps
 * and the permission groups, each with its methods and whether it is granted to the app.
 */
public class AApiAccess extends AidlParams {

	private boolean enabled;
	private ArrayList<APermissionGroup> groups;

	public AApiAccess(boolean enabled, ArrayList<APermissionGroup> groups) {
		this.enabled = enabled;
		this.groups = groups;
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
	 * @return all permission groups; a method that is in no group needs only the app to be turned on
	 */
	public ArrayList<APermissionGroup> getGroups() {
		return groups;
	}

	@Override
	public void writeToBundle(Bundle bundle) {
		bundle.putBoolean("enabled", enabled);
		bundle.putParcelableArrayList("groups", groups);
	}

	@Override
	protected void readFromBundle(Bundle bundle) {
		enabled = bundle.getBoolean("enabled");
		bundle.setClassLoader(APermissionGroup.class.getClassLoader());
		groups = bundle.getParcelableArrayList("groups");
	}
}
