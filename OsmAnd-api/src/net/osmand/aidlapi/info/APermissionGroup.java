package net.osmand.aidlapi.info;

import android.os.Bundle;
import android.os.Parcel;

import net.osmand.aidlapi.AidlParams;

import java.util.ArrayList;

/**
 * A permission group of the API: the methods it allows and whether it is granted to the calling app.
 * An app asks for groups by their ids with the net.osmand.aidl.REQUEST_PERMISSIONS intent.
 */
public class APermissionGroup extends AidlParams {

	private String id;
	private boolean granted;
	private ArrayList<String> methods;

	public APermissionGroup(String id, boolean granted, ArrayList<String> methods) {
		this.id = id;
		this.granted = granted;
		this.methods = methods;
	}

	public APermissionGroup(Parcel in) {
		readFromParcel(in);
	}

	public static final Creator<APermissionGroup> CREATOR = new Creator<APermissionGroup>() {
		@Override
		public APermissionGroup createFromParcel(Parcel in) {
			return new APermissionGroup(in);
		}

		@Override
		public APermissionGroup[] newArray(int size) {
			return new APermissionGroup[size];
		}
	};

	/**
	 * @return group id, such as map, navigation or settings
	 */
	public String getId() {
		return id;
	}

	public boolean isGranted() {
		return granted;
	}

	/**
	 * @return names of the methods in this group; each IOsmAndAidlInterface method of the group is among them
	 */
	public ArrayList<String> getMethods() {
		return methods;
	}

	@Override
	public void writeToBundle(Bundle bundle) {
		bundle.putString("id", id);
		bundle.putBoolean("granted", granted);
		bundle.putStringArrayList("methods", methods);
	}

	@Override
	protected void readFromBundle(Bundle bundle) {
		id = bundle.getString("id");
		granted = bundle.getBoolean("granted");
		methods = bundle.getStringArrayList("methods");
	}
}
