package net.osmand.plus;

import android.content.Context;

import net.osmand.StateChangedListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class ApplicationMode {
	private static Map<String, Set<ApplicationMode>> widgets = new LinkedHashMap<String, Set<ApplicationMode>>(); 
	private static List<ApplicationMode> values = new ArrayList<ApplicationMode>();
	private static List<ApplicationMode> cachedFilteredValues = new ArrayList<ApplicationMode>();
	/*
	 * DEFAULT("Browse map"), CAR("Car"), BICYCLE("Bicycle"), PEDESTRIAN("Pedestrian");
	 */
	public static final ApplicationMode DEFAULT = create(R.string.app_mode_default, "default").speed(1.5f, 5).arrivalDistance(90).defLocation().
			icon(R.drawable.ic_browse_map, R.drawable.ic_world_globe_dark).reg();
	
	public static final ApplicationMode CAR = create(R.string.app_mode_car, "car").speed(15.3f, 35).arrivalDistance(90).carLocation().
			icon(R.drawable.ic_car, R.drawable.ic_action_car_dark).reg();
	
	public static final ApplicationMode BICYCLE = create(R.string.app_mode_bicycle, "bicycle").speed(5.5f, 15).arrivalDistance(60).bicycleLocation().
			icon(R.drawable.ic_bicycle, R.drawable.ic_action_bicycle_dark).reg();
	
	public static final ApplicationMode PEDESTRIAN = create(R.string.app_mode_pedestrian, "pedestrian").speed(1.5f, 5).arrivalDistance(45).
			icon(R.drawable.ic_pedestrian, R.drawable.ic_action_pedestrian_dark).reg();
	
	public static final ApplicationMode AIRCRAFT = create(R.string.app_mode_aircraft, "aircraft").speed(40f, 100).carLocation().
			icon(R.drawable.ic_aircraft,R.drawable.ic_action_aircraft).reg();
	
	public static final ApplicationMode BOAT = create(R.string.app_mode_boat, "boat").speed(5.5f, 20).carLocation().
			icon(R.drawable.ic_sail_boat, R.drawable.ic_action_sail_boat_dark).reg();
	
	public static final ApplicationMode HIKING = create(R.string.app_mode_hiking, "hiking").speed(1.5f, 5).parent(PEDESTRIAN).
			icon(R.drawable.ic_trekking, R.drawable.ic_action_trekking_dark).reg();
	
	public static final ApplicationMode MOTORCYCLE = create(R.string.app_mode_motorcycle, "motorcycle").speed(15.3f, 40).
			carLocation().parent(CAR).
			icon(R.drawable.ic_motorcycle, R.drawable.ic_action_motorcycle_dark).reg();
	
	public static final ApplicationMode TRUCK = create(R.string.app_mode_truck, "truck").speed(15.3f, 40).
			carLocation().parent(CAR).
			icon(R.drawable.ic_truck, R.drawable.ic_action_truck_dark).reg();
	
	static {
		ApplicationMode[] exceptDefault = new ApplicationMode[] { CAR, PEDESTRIAN, BICYCLE, BOAT, AIRCRAFT };
		ApplicationMode[] exceptPedestrianAndDefault = new ApplicationMode[] { CAR, BICYCLE, BOAT, AIRCRAFT };
		ApplicationMode[] exceptAirBoatDefault = new ApplicationMode[] { CAR, BICYCLE, PEDESTRIAN };
		ApplicationMode[] pedestrian = new ApplicationMode[] { PEDESTRIAN };
		ApplicationMode[] pedestrianBicycle = new ApplicationMode[] { PEDESTRIAN, BICYCLE };
		
		ApplicationMode[] all = null;
		ApplicationMode[] none = new ApplicationMode[] {};
		
		// left
		regWidget("next_turn", exceptPedestrianAndDefault);
		regWidget("next_turn_small", pedestrian);
		regWidget("next_next_turn", exceptPedestrianAndDefault);
		
		// right
		regWidget("intermediate_distance", exceptDefault);
		regWidget("distance", exceptDefault);
		regWidget("time", exceptDefault);
		regWidget("speed", exceptPedestrianAndDefault);
		regWidget("max_speed", CAR);
		regWidget("gps_info", none);
		regWidget("altitude", pedestrianBicycle);
		
		// top
		regWidget("compass", all);
		regWidget("config", none);
		regWidget("street_name", exceptAirBoatDefault);
		regWidget("back_to_location", all);
		regWidget("monitoring_services", none);
		regWidget("bgService", none);
		regWidget("layers", none);
	}
	
	
	private static class ApplicationModeBuilder {
		
	
		private ApplicationMode applicationMode;

		public ApplicationMode reg() {
			values.add(applicationMode);
			return applicationMode;
		}
		
		public ApplicationModeBuilder icon(int bigIcon, int smallIconDark) {
			applicationMode.iconId = bigIcon;
			applicationMode.smallIconDark = smallIconDark;
			return this;
		}
		
		public ApplicationModeBuilder carLocation(){
			applicationMode.bearingIconDay = R.drawable.map_car_bearing;
			applicationMode.bearingIconNight = R.drawable.map_car_bearing_night;
			applicationMode.locationIconDay = R.drawable.map_car_location;
			applicationMode.locationIconNight = R.drawable.map_car_location_night;
			return this;
		}
		
		public ApplicationModeBuilder parent(ApplicationMode parent){
			applicationMode.parent = parent;
			return this;
		}
		
		public ApplicationModeBuilder bicycleLocation(){
			applicationMode.bearingIconDay = R.drawable.map_bicycle_bearing;
			applicationMode.bearingIconNight = R.drawable.map_bicycle_bearing_night;
			applicationMode.locationIconDay = R.drawable.map_bicycle_location;
			applicationMode.locationIconNight = R.drawable.map_bicycle_location_night;
			return this;
		}
		
		public ApplicationModeBuilder defLocation(){
			applicationMode.bearingIconDay = R.drawable.map_pedestrian_bearing;
			applicationMode.bearingIconNight = R.drawable.map_pedestrian_bearing_night;
			applicationMode.locationIconDay = R.drawable.map_pedestrian_location;
			applicationMode.locationIconNight = R.drawable.map_pedestrian_location_night;
			return this;
		}
		
		public ApplicationModeBuilder speed(float defSpeed, int distForTurn) {
			applicationMode.defaultSpeed = defSpeed;
			applicationMode.minDistanceForTurn = distForTurn;
			return this;
		}

		public ApplicationModeBuilder arrivalDistance(int arrivalDistance) {
			applicationMode.arrivalDistance = arrivalDistance;
			return this;
		}
	}
	
	private static ApplicationModeBuilder create(int key, String stringKey) {
		ApplicationModeBuilder builder = new ApplicationModeBuilder();
		builder.applicationMode = new ApplicationMode(key, stringKey);
		return builder;
	}

	private ApplicationMode(int key, String stringKey) {
		this.key = key;
		this.stringKey = stringKey;
	}
	
	public static List<ApplicationMode> values(OsmandSettings settings) {
		if (cachedFilteredValues.isEmpty()) {
			if (listener == null) {
				listener = new StateChangedListener<String>() {
					@Override
					public void stateChanged(String change) {
						cachedFilteredValues = new ArrayList<ApplicationMode>();
					}
				};
				settings.AVAILABLE_APP_MODES.addListener(listener);
			}
			String available = settings.AVAILABLE_APP_MODES.get();
			cachedFilteredValues = new ArrayList<ApplicationMode>();
			for (ApplicationMode v : values) {
				if (available.indexOf(v.getStringKey() + ",") != -1 || v == DEFAULT) {
					cachedFilteredValues.add(v);
				}
			}
		}
		return cachedFilteredValues;
	}
	
	public static List<ApplicationMode> allPossibleValues() {
		return new ArrayList<ApplicationMode>(values);
	}
	
	
	// returns modifiable ! Set<ApplicationMode> to exclude non-wanted derived
	public static Set<ApplicationMode> regWidget(String widgetId, ApplicationMode... am) {
		HashSet<ApplicationMode> set = new HashSet<ApplicationMode>();
		if(am == null) {
			set.addAll(values); 
		} else {
			Collections.addAll(set, am);
		}
		for(ApplicationMode m : values) {
			// add derived modes
			if(set.contains(m.getParent())) {
				set.add(m);
			}
		}
		widgets.put(widgetId, set);
		return set;
	}
	
	public boolean isWidgetCollapsible(String key) {
		return false;
	}
	
	public boolean isWidgetVisible(String key) {
		Set<ApplicationMode> set = widgets.get(key);
		if(set == null) {
			return false;
		}
		return set.contains(this);
	}
	
	
	
	public static List<ApplicationMode> getModesDerivedFrom(ApplicationMode am) {
		List<ApplicationMode> list = new ArrayList<ApplicationMode>();
		for(ApplicationMode a : values) {
			if(a == am || a.getParent() == am) {
				list.add(a);
			}
		}
		return list;
	}
	
	public ApplicationMode getParent() {
		return parent;
	}
	
	public int getSmallIconDark() {
		return smallIconDark ;
	}
	
	public boolean hasFastSpeed(){
		return getDefaultSpeed() > 10;
	}
	
	public int getResourceBearingDay() {
		return bearingIconDay;
	}

	public int getResourceBearingNight() {
		//return bearingIconDay;
		return bearingIconNight;
	}
	
	public int getResourceLocationDay() {
		return locationIconDay;
	}

	public int getResourceLocationNight() {
		//return locationIconDay;
		return locationIconNight;
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
	
	public String toHumanStringCtx(Context ctx) {
		return ctx.getString(key);
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
		return defaultSpeed;
	}
	
	public int getMinDistanceForTurn() {
		return minDistanceForTurn;
	}

	public int getArrivalDistance() {
		return arrivalDistance;
	}

	public boolean isDerivedRoutingFrom(ApplicationMode mode) {
		return this == mode || getParent() == mode;
	}

	private final int key;
	private final String stringKey;

	private ApplicationMode parent;
	private int iconId = R.drawable.ic_browse_map;
	private int smallIconDark = R.drawable.ic_world_globe_dark ;
	private float defaultSpeed = 10f;
	private int minDistanceForTurn = 50;
	private int arrivalDistance = 90;
	private int bearingIconDay = R.drawable.map_pedestrian_bearing;
	private int bearingIconNight = R.drawable.map_pedestrian_bearing_night;
	private int locationIconDay = R.drawable.map_pedestrian_location;
	private int locationIconNight = R.drawable.map_pedestrian_location_night;
	private static StateChangedListener<String> listener;
}