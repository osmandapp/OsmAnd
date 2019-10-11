package net.osmand.aidlapi.favorite;

import android.os.Bundle;
import android.os.Parcel;

import net.osmand.aidlapi.AidlParams;

public class AFavorite extends AidlParams {

	private double lat;
	private double lon;
	private String name;
	private String description;
	private String category;
	private String color;
	private boolean visible;

	public AFavorite(double lat, double lon, String name, String description,
	                 String category, String color, boolean visible) {
		this.lat = lat;
		this.lon = lon;
		this.name = name;
		this.description = description;
		this.category = category;
		this.color = color;
		this.visible = visible;
	}

	public AFavorite(Parcel in) {
		readFromParcel(in);
	}

	public static final Creator<AFavorite> CREATOR = new Creator<AFavorite>() {
		@Override
		public AFavorite createFromParcel(Parcel in) {
			return new AFavorite(in);
		}

		@Override
		public AFavorite[] newArray(int size) {
			return new AFavorite[size];
		}
	};

	public double getLat() {
		return lat;
	}

	public double getLon() {
		return lon;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public String getCategory() {
		return category;
	}

	public String getColor() {
		return color;
	}

	public boolean isVisible() {
		return visible;
	}

	@Override
	public void writeToBundle(Bundle bundle) {
		bundle.putDouble("lat", lat);
		bundle.putDouble("lon", lon);
		bundle.putString("name", name);
		bundle.putString("description", description);
		bundle.putString("category", category);
		bundle.putString("color", color);
		bundle.putBoolean("visible", visible);
	}

	@Override
	protected void readFromBundle(Bundle bundle) {
		lat = bundle.getDouble("lat");
		lon = bundle.getDouble("lon");
		name = bundle.getString("name");
		description = bundle.getString("description");
		category = bundle.getString("category");
		color = bundle.getString("color");
		visible = bundle.getBoolean("visible");
	}
}