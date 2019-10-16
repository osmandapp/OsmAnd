package net.osmand.aidlapi.favorite.group;

import android.os.Bundle;
import android.os.Parcel;

import net.osmand.aidlapi.AidlParams;

public class AFavoriteGroup extends AidlParams {

	private String name;
	private String color;
	private boolean visible;

	public AFavoriteGroup(String name, String color, boolean visible) {
		this.name = name;
		this.color = color;
		this.visible = visible;
	}

	public AFavoriteGroup(Parcel in) {
		readFromParcel(in);
	}

	public static final Creator<AFavoriteGroup> CREATOR = new Creator<AFavoriteGroup>() {
		@Override
		public AFavoriteGroup createFromParcel(Parcel in) {
			return new AFavoriteGroup(in);
		}

		@Override
		public AFavoriteGroup[] newArray(int size) {
			return new AFavoriteGroup[size];
		}
	};

	public String getName() {
		return name;
	}

	public String getColor() {
		return color;
	}

	public boolean isVisible() {
		return visible;
	}

	@Override
	public void writeToBundle(Bundle bundle) {
		bundle.putString("name", name);
		bundle.putString("color", color);
		bundle.putBoolean("visible", visible);
	}

	@Override
	protected void readFromBundle(Bundle bundle) {
		name = bundle.getString("name");
		color = bundle.getString("color");
		visible = bundle.getBoolean("visible");
	}
}