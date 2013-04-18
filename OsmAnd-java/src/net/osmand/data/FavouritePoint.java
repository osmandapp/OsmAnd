package net.osmand.data;

import java.io.Serializable;

public class FavouritePoint implements Serializable {
	private static final long serialVersionUID = 729654300829771466L;
	private String name;
	private String category = "";
	private double latitude;
	private double longitude;
	private boolean stored = false;
	

	public FavouritePoint(){
	}

	public FavouritePoint(double latitude, double longitude, String name, String category) {
		this.latitude = latitude;
		this.longitude = longitude;
		this.category = category;
		this.name = name;
	}

	public double getLatitude() {
		return latitude;
	}

	public boolean isStored() {
		return stored;
	}
	public void setStored(boolean stored) {
		this.stored = stored;
	}

	public void setLatitude(double latitude) {
		this.latitude = latitude;
	}

	public double getLongitude() {
		return longitude;
	}

	public void setLongitude(double longitude) {
		this.longitude = longitude;
	}
	
	public String getCategory() {
		return category;
	}
	
	public void setCategory(String category) {
		this.category = category;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	@Override
	public String toString() {
		return "Favourite " + getName(); //$NON-NLS-1$
	}
}