package net.osmand.data;

import java.io.Serializable;

public class FavouritePoint implements Serializable, LocationPoint {
	private static final long serialVersionUID = 729654300829771466L;
	private String name;
	private String category = "";
	private double latitude;
	private double longitude;
	private int color;
	private int extraParam = -1;
	private boolean visible = true;
	private boolean removeable = true;

	public FavouritePoint(){
	}



	public FavouritePoint(double latitude, double longitude, String name, String category) {
		this.latitude = latitude;
		this.longitude = longitude;
		this.category = category;
		this.name = name;
	}
	
	public int getExtraParam() {
		return extraParam;
	}
	
	public void setExtraParam(int extraParam) {
		this.extraParam = extraParam;
	}
	
	public boolean isRemoveable() {
		return removeable;
	}
	
	public void setRemoveable(boolean removeable) {
		this.removeable = removeable;
	}
	
	public int getColor() {
		return color;
	}
	
	public void setColor(int color) {
		this.color = color;
	}
	
	public boolean isVisible() {
		return visible;
	}
	
	public void setVisible(boolean visible) {
		this.visible = visible;
	}
	

	public double getLatitude() {
		return latitude;
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