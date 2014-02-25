package net.osmand;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.osmand.StateChangedListener;
import net.osmand.plus.R;
import android.content.Context;


public class ApplicationMode {
	private static Map<String, Set<ApplicationMode>> widgets = new LinkedHashMap<String, Set<ApplicationMode>>(); 
	private static List<ApplicationMode> values = new ArrayList<ApplicationMode>();
	private static List<ApplicationMode> cachedFilteredValues = new ArrayList<ApplicationMode>();
	private static boolean listenerRegistered = false;
	/*
	 * DEFAULT("Browse map"), CAR("Car"), BICYCLE("Bicycle"), PEDESTRIAN("Pedestrian");
	 */
	public static final ApplicationMode DEFAULT = create(R.string.app_mode_default, "default").speed(1.5f, 5).defLocation().
			icon(R.drawable.ic_browse_map, R.drawable.app_mode_globus_light, R.drawable.app_mode_globus_dark).reg();
	
	public static final ApplicationMode CAR = create(R.string.app_mode_car, "car").speed(15.3f, 35).carLocation().
			icon(R.drawable.ic_car, R.drawable.ic_action_car_light, R.drawable.ic_action_car_dark).reg();
	
	public static final ApplicationMode BICYCLE = create(R.string.app_mode_bicycle, "bicycle").speed(5.5f, 15).bicycleLocation().
			icon(R.drawable.ic_bicycle, R.drawable.ic_action_bicycle_light, R.drawable.ic_action_bicycle_dark).reg();
	
	public static final ApplicationMode PEDESTRIAN = create(R.string.app_mode_pedestrian, "pedestrian").speed(1.5f, 5).
			icon(R.drawable.ic_pedestrian, R.drawable.ic_action_pedestrian_light, R.drawable.ic_action_pedestrian_dark).reg();
	
	public static final ApplicationMode AIRCRAFT = create(R.string.app_mode_aircraft, "aircraft").speed(40f, 100).carLocation().
			icon(R.drawable.ic_aircraft, R.drawable.ic_action_aircraft_light, R.drawable.ic_action_aircraft_dark).reg();
	
	public static final ApplicationMode BOAT = create(R.string.app_mode_boat, "boat").speed(5.5f, 20).carLocation().
			icon(R.drawable.ic_sail_boat, R.drawable.ic_action_sail_boat_light, R.drawable.ic_action_sail_boat_dark).reg();
	
	public static final ApplicationMode HIKING = create(R.string.app_mode_hiking, "hiking").speed(1.5f, 5).parent(PEDESTRIAN).
			icon(R.drawable.ic_trekking, R.drawable.ic_action_trekking_light, R.drawable.ic_action_trekking_dark).reg();
	
	public static final ApplicationMode MOTORCYCLE = create(R.string.app_mode_motorcycle, "motorcycle").speed(15.3f, 40).
			carLocation().parent(CAR).
			icon(R.drawable.ic_motorcycle, R.drawable.ic_action_motorcycle_light, R.drawable.ic_action_motorcycle_dark).reg();
	
	static {
		ApplicationMode[] exceptPedestrian = new ApplicationMode[] { DEFAULT, CAR, BICYCLE, BOAT, AIRCRAFT };
		ApplicationMode[] exceptAirBoat = new ApplicationMode[] { DEFAULT, CAR, BICYCLE};
		ApplicationMode[] exceptCarBoatAir = new ApplicationMode[] { DEFAULT, BICYCLE, PEDESTRIAN };
		ApplicationMode[] pedestrian = new ApplicationMode[] { PEDESTRIAN };
		
		ApplicationMode[] all = null;
		ApplicationMode[] none = new ApplicationMode[] {};
		
		// left
		regWidget("next_turn", exceptPedestrian);
		regWidget("next_turn_small", pedestrian);
		regWidget("next_next_turn", exceptPedestrian);
		
		// right		
		regWidget("intermediate_distance", all);
		regWidget("distance", all);
		regWidget("time", all);
		regWidget("speed", exceptPedestrian);
		regWidget("max_speed", exceptAirBoat);
		regWidget("gps_info", exceptCarBoatAir);
		regWidget("altitude", exceptCarBoatAir);
		
		// top
		regWidget("compass", all);
		regWidget("config", all);
		regWidget("street_name", exceptAirBoat);
		regWidget("back_to_location", all);
		regWidget("monitoring_services", exceptCarBoatAir);
		regWidget("bgService", none);
		regWidget("layers", none);
	}
	
	
	private static class ApplicationModeBuilder {
		
	
		private ApplicationMode applicationMode;

		public ApplicationMode reg() {
			values.add(applicationMode);
			return applicationMode;
		}
		
		public ApplicationModeBuilder icon(int bigIcon, int smallIconLight, int smallIconDark) {
			applicationMode.iconId = bigIcon;
			applicationMode.smallIconLight = smallIconLight;
			applicationMode.smallIconDark = smallIconDark;
			return this;
		}
		
		public ApplicationModeBuilder carLocation(){
			applicationMode.bearingIcon = R.drawable.car_bearing;
			applicationMode.locationIcon = R.drawable.car_location;
			return this;
		}
		
		public ApplicationModeBuilder parent(ApplicationMode parent){
			applicationMode.parent = parent;
			return this;
		}
		
		public ApplicationModeBuilder bicycleLocation(){
			applicationMode.bearingIcon = R.drawable.bicycle_bearing;
			applicationMode.locationIcon = R.drawable.bicycle_location;
			return this;
		}
		
		public ApplicationModeBuilder defLocation(){
			applicationMode.bearingIcon = R.drawable.pedestrian_bearing;
			applicationMode.locationIcon = R.drawable.pedestrian_location;
			return this;
		}
		
		public ApplicationModeBuilder speed(float defSpeed, int distForTurn) {
			applicationMode.defaultSpeed = defSpeed;
			applicationMode.minDistanceForTurn = distForTurn;
			return this;
		}
	}
	
	private static ApplicationModeBuilder create(int key, String stringKey) {
		ApplicationModeBuilder builder = new ApplicationModeBuilder();
		builder.applicationMode = new ApplicationMode(key, stringKey);
		return builder;
	}
	
	
	
	private final int key;
	private final String stringKey;
	
	private ApplicationMode parent;
	private int iconId = R.drawable.ic_browse_map;
	private int smallIconDark = R.drawable.app_mode_globus_dark ;
	private int smallIconLight = R.drawable.app_mode_globus_light ;
	private float defaultSpeed = 10f;
	private int minDistanceForTurn = 50;
	private int bearingIcon = R.drawable.pedestrian_bearing;
	private int locationIcon = R.drawable.pedestrian_location;

	private ApplicationMode(int key, String stringKey) {
		this.key = key;
		this.stringKey = stringKey;
	}
	
	public static List<ApplicationMode> values(OsmandSettings settings) {
		if (cachedFilteredValues.isEmpty()) {
			if (!listenerRegistered) {
				settings.AVAILABLE_APP_MODES.addListener(new StateChangedListener<String>() {
					@Override
					public void stateChanged(String change) {
						cachedFilteredValues = new ArrayList<ApplicationMode>();
					}
				});
				listenerRegistered = true;
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
	
	public static List<ApplicationMode> allPossibleValues(OsmandSettings settings) {
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
	
	public int getSmallIcon(boolean nightMode) {
		return nightMode? smallIconDark : smallIconLight;
	}
	
	public boolean hasFastSpeed(){
		return getDefaultSpeed() > 10;
	}
	
	public int getResourceBearing() {
		return bearingIcon;
	}
	
	public int getResourceLocation() {
		return locationIcon;
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
	
	public String toHumanStringCtx(ClientContext ctx) {
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

	
	public boolean isDerivedRoutingFrom(ApplicationMode mode) {
		return this == mode || getParent() == mode;
	}
	

	

}