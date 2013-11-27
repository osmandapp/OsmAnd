package net.osmand.plus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.content.Context;


public class ApplicationMode {
	/*
	 * DEFAULT("Browse map"), CAR("Car"), BICYCLE("Bicycle"), PEDESTRIAN("Pedestrian");
	 */
	public static final ApplicationMode DEFAULT = reg(R.string.app_mode_default, R.drawable.ic_browse_map, "default", null);
	public static final ApplicationMode CAR = reg(R.string.app_mode_car, R.drawable.ic_car, "car", null); 
	public static final ApplicationMode BICYCLE = reg(R.string.app_mode_bicycle, R.drawable.ic_bicycle, "bicycle", null); 
	public static final ApplicationMode PEDESTRIAN = reg(R.string.app_mode_pedestrian, R.drawable.ic_pedestrian, "pedestrian", null);
	
	
	private static List<ApplicationMode> values = new ArrayList<ApplicationMode>();

	private static ApplicationMode reg(int key, int iconId, String stringKey, ApplicationMode parent) {
		return new ApplicationMode(key, iconId, stringKey, parent);
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

}