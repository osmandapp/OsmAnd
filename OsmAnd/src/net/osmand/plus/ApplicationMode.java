package net.osmand.plus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.content.Context;


public class ApplicationMode {
	private static List<ApplicationMode> values = new ArrayList<ApplicationMode>();
	/*
	 * DEFAULT("Browse map"), CAR("Car"), BICYCLE("Bicycle"), PEDESTRIAN("Pedestrian");
	 */
	public static final ApplicationMode DEFAULT = reg(R.string.app_mode_default, R.drawable.ic_browse_map, "default", null);
	public static final ApplicationMode CAR = reg(R.string.app_mode_car, R.drawable.ic_car, "car", null); 
	public static final ApplicationMode BICYCLE = reg(R.string.app_mode_bicycle, R.drawable.ic_bicycle, "bicycle", null); 
	public static final ApplicationMode PEDESTRIAN = reg(R.string.app_mode_pedestrian, R.drawable.ic_pedestrian, "pedestrian", null);
	public static final ApplicationMode AIRCRAFT = reg(R.string.app_mode_aircraft, R.drawable.ic_browse_map, "aircraft", null);
	public static final ApplicationMode BOAT = reg(R.string.app_mode_boat, R.drawable.ic_browse_map, "boat", null);
	
	
	private static ApplicationMode reg(int key, int iconId, String stringKey, ApplicationMode parent) {
		ApplicationMode mode = new ApplicationMode(key, iconId, stringKey, parent);
		values.add(mode);
		return mode;
	}
	private final int key;
	private final ApplicationMode parent;
	private String stringKey;
	private int iconId;

	private ApplicationMode(int key, int iconId, String stringKey, ApplicationMode parent) {
		this.key = key;
		this.iconId = iconId;
		this.stringKey = stringKey;
		this.stringKey = stringKey;
		this.parent = parent;
	}
	
	public static List<ApplicationMode> getAplicationModes(ClientContext ctx) {
		// TODO
		OsmandSettings sets = ctx.getSettings();
		return values;
	}
	
	public static List<ApplicationMode> values(ClientContext ctx) {
		// TODO
		OsmandSettings sets = ctx.getSettings();
		return values;
	}
	
	public static List<ApplicationMode> values(OsmandSettings settings) {
		// TODO
		return values;
	}
	
	public static List<ApplicationMode> allPossibleValues(ClientContext ctx) {
		// TODO
		OsmandSettings sets = ctx.getSettings();
		return values;
	}
	
	public ApplicationMode getParent() {
		return parent;
	}
	
	public int getSmallIcon(boolean nightMode) {
		if(this == ApplicationMode.CAR){
			return nightMode? R.drawable.ic_action_car_dark : R.drawable.ic_action_car_light;
		} else if(this == ApplicationMode.BICYCLE){
			return nightMode? R.drawable.ic_action_bicycle_dark : R.drawable.ic_action_bicycle_light;
		} else if(this == ApplicationMode.PEDESTRIAN){
			return nightMode? R.drawable.ic_action_pedestrian_dark : R.drawable.ic_action_pedestrian_light;
		} else {
			return nightMode? R.drawable.app_mode_globus_dark : R.drawable.app_mode_globus_light;
		}
	}
	
	public boolean hasFastSpeed(){
		return getDefaultSpeed() > 10;
	}
	
	public int getResourceBearing() {
		if (this == ApplicationMode.CAR) {
			return R.drawable.car_bearing;
		} else if (this == ApplicationMode.BICYCLE) {
			return R.drawable.bicycle_bearing;
		}		
		return R.drawable.pedestrian_bearing;
	}
	
	public int getResourceLocation() {
		if (this == ApplicationMode.CAR) {
			return R.drawable.car_location;
		} else if (this == ApplicationMode.BICYCLE) {
			return R.drawable.bicycle_location;
		}		
		return R.drawable.pedestrian_location;
	}
	
	public String getStringKey() {
		return stringKey;
	}
	
	public int getIconId() {
		return iconId;
	}
	
	public int getStringResource() {
		return key;
	}

	public String toHumanString(Context ctx) {
		return ctx.getString(key);
	}

	public static Set<ApplicationMode> allOf() {
		return new HashSet<ApplicationMode>(values);
	}
	
	public static Set<ApplicationMode> noneOf() {
		return new HashSet<ApplicationMode>();
	}

	public static Set<ApplicationMode> of(ApplicationMode... modes ) {
		HashSet<ApplicationMode> ts = new HashSet<ApplicationMode>();
		Collections.addAll(ts, modes);
		return ts;
	}
	
	public static ApplicationMode valueOfStringKey(String key, ApplicationMode def) {
		for(ApplicationMode p : values) {
			if(p.getStringKey().equals(key)) {
				return p;
			}
		}
		return def;
	}

	public float getDefaultSpeed() {
		float speed = 1.5f; 
		if(this == ApplicationMode.CAR){
			speed = 15.3f;
		} else if(this == ApplicationMode.BICYCLE || this == ApplicationMode.BOAT){
			speed = 5.5f;
		} else if(this == ApplicationMode.AIRCRAFT){
			speed = 40f;
		}
		return speed;
	}
	
	public int getMinDistanceForTurn() {
		int minDistanceForTurn = 5;
		if(this == ApplicationMode.CAR){
			minDistanceForTurn = 35;
		} else if(this == ApplicationMode.AIRCRAFT){
			minDistanceForTurn = 100;
		} else if(this == ApplicationMode.BICYCLE || this == ApplicationMode.BOAT){
			minDistanceForTurn = 12;
		}
		return minDistanceForTurn;
	}

}