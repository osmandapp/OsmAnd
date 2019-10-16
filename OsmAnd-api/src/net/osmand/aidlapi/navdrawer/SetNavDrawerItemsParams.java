package net.osmand.aidlapi.navdrawer;

import android.os.Bundle;
import android.os.Parcel;
import android.support.annotation.NonNull;

import net.osmand.aidlapi.AidlParams;

import java.util.ArrayList;
import java.util.List;

public class SetNavDrawerItemsParams extends AidlParams {

	private String appPackage;
	private ArrayList<NavDrawerItem> items = new ArrayList<>();

	public SetNavDrawerItemsParams(@NonNull String appPackage, @NonNull List<NavDrawerItem> items) {
		this.appPackage = appPackage;
		this.items.addAll(items);
	}

	protected SetNavDrawerItemsParams(Parcel in) {
		readFromParcel(in);
	}

	public String getAppPackage() {
		return appPackage;
	}

	public List<NavDrawerItem> getItems() {
		return items;
	}

	@Override
	protected void readFromBundle(Bundle bundle) {
		appPackage = bundle.getString("appPackage");
		bundle.setClassLoader(NavDrawerItem.class.getClassLoader());
		items = bundle.getParcelableArrayList("items");
	}

	@Override
	public void writeToBundle(Bundle bundle) {
		bundle.putString("appPackage", appPackage);
		bundle.putParcelableArrayList("items", items);
	}

	public static final Creator<SetNavDrawerItemsParams> CREATOR = new Creator<SetNavDrawerItemsParams>() {
		@Override
		public SetNavDrawerItemsParams createFromParcel(Parcel in) {
			return new SetNavDrawerItemsParams(in);
		}

		@Override
		public SetNavDrawerItemsParams[] newArray(int size) {
			return new SetNavDrawerItemsParams[size];
		}
	};
}