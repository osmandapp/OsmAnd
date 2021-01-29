package net.osmand.plus.wikivoyage.data;

public class TravelGpx extends TravelArticle {

	public static final String DISTANCE = "distance";
	public static final String DIFF_ELE_UP = "diff_ele_up";
	public static final String DIFF_ELE_DOWN = "diff_ele_down";
	public static final String USER = "user";
	public static final String PROFILE = "profile";

	public String user;
	public String profile;
	public float totalDistance = 0;
	public double diffElevationUp = 0;
	public double diffElevationDown = 0;
}
