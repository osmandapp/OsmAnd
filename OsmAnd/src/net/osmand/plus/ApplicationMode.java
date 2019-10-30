package net.osmand.plus;

import android.content.Context;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.StringRes;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import com.google.gson.reflect.TypeToken;

import net.osmand.StateChangedListener;
import net.osmand.plus.routing.RouteProvider.RouteService;
import net.osmand.util.Algorithms;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.WIDGET_ALTITUDE;
import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.WIDGET_BATTERY;
import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.WIDGET_BEARING;
import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.WIDGET_COMPASS;
import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.WIDGET_DISTANCE;
import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.WIDGET_GPS_INFO;
import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.WIDGET_INTERMEDIATE_DISTANCE;
import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.WIDGET_INTERMEDIATE_TIME;
import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.WIDGET_MARKER_1;
import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.WIDGET_MARKER_2;
import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.WIDGET_MAX_SPEED;
import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.WIDGET_NEXT_NEXT_TURN;
import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.WIDGET_NEXT_TURN;
import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.WIDGET_NEXT_TURN_SMALL;
import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.WIDGET_PLAIN_TIME;
import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.WIDGET_RULER;
import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.WIDGET_SPEED;
import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.WIDGET_TIME;

public class ApplicationMode {

	private static Map<String, Set<ApplicationMode>> widgetsVisibilityMap = new LinkedHashMap<>();
	private static Map<String, Set<ApplicationMode>> widgetsAvailabilityMap = new LinkedHashMap<>();

	private static List<ApplicationMode> defaultValues = new ArrayList<>();
	private static List<ApplicationMode> values = new ArrayList<>();
	private static List<ApplicationMode> cachedFilteredValues = new ArrayList<>();

	private static StateChangedListener<String> listener;
	private static OsmAndAppCustomization.OsmAndAppCustomizationListener customizationListener;

	private final int keyName;
	private final String stringKey;
	private String userProfileName;
	private int descriptionId;

	private ApplicationMode parentAppMode;
	private String iconResName = "ic_world_globe_dark";
	private int iconRes = R.drawable.ic_world_globe_dark;
	private int iconMapRes = R.drawable.map_world_globe_dark;
	private ProfileIconColors iconColor = ProfileIconColors.DEFAULT;
	private String routingProfile = "";
	private RouteService routeService = RouteService.OSMAND;


	private float defaultSpeed = 10f;
	private float initialDefaultSpeed = defaultSpeed;
	private int minDistanceForTurn = 50;
	private int arrivalDistance = 90;
	private int offRouteDistance = 350;
	private int bearingIconDay = R.drawable.map_pedestrian_bearing;
	private int bearingIconNight = R.drawable.map_pedestrian_bearing_night;
	private int headingIconDay = R.drawable.map_pedestrian_location_view_angle;
	private int headingIconNight = R.drawable.map_pedestrian_location_view_angle_night;
	private int locationIconDay = R.drawable.map_pedestrian_location;
	private int locationIconNight = R.drawable.map_pedestrian_location_night;
	private int locationIconDayLost = R.drawable.map_pedestrian_location_lost;
	private int locationIconNightLost = R.drawable.map_pedestrian_location_lost_night;

	private ApplicationMode(int key, String stringKey) {
		this.keyName = key;
		this.stringKey = stringKey;
	}


	/*
	 * DEFAULT("Browse map"), CAR("Car"), BICYCLE("Bicycle"), PEDESTRIAN("Pedestrian"); NAUTICAL("boat"); PUBLIC_TRANSPORT("Public transport"); AIRCRAFT("Aircraft")
	 */
	public static final ApplicationMode DEFAULT = createBase(R.string.app_mode_default, "default").speed(1.5f, 5).arrivalDistance(90).defLocation().
			icon(R.drawable.ic_world_globe_dark, R.drawable.map_world_globe_dark, "ic_world_globe_dark").reg();

	public static final ApplicationMode CAR = createBase(R.string.app_mode_car, "car").speed(12.5f, 35).carLocation().
			icon(R.drawable.ic_action_car_dark, R.drawable.map_action_car_dark, "ic_action_car_dark").setRoutingProfile("car").description(R.string.base_profile_descr_car).reg();

	public static final ApplicationMode BICYCLE = createBase(R.string.app_mode_bicycle, "bicycle").speed(2.77f, 15).arrivalDistance(60).offRouteDistance(50).bicycleLocation().
			icon(R.drawable.ic_action_bicycle_dark, R.drawable.map_action_bicycle_dark,"ic_action_bicycle_dark").setRoutingProfile("bicycle").description(R.string.base_profile_descr_bicycle).reg();

	public static final ApplicationMode PEDESTRIAN = createBase(R.string.app_mode_pedestrian, "pedestrian").speed(1.11f, 5).arrivalDistance(45).offRouteDistance(20).
			icon(R.drawable.ic_action_pedestrian_dark, R.drawable.map_action_pedestrian_dark, "ic_action_pedestrian_dark").setRoutingProfile("pedestrian").description(R.string.base_profile_descr_pedestrian).reg();

	public static final ApplicationMode PUBLIC_TRANSPORT = createBase(R.string.app_mode_public_transport, "public_transport").
			icon(R.drawable.ic_action_bus_dark, R.drawable.map_action_bus_dark,"ic_action_bus_dark").setRoutingProfile("public_transport").description(R.string.base_profile_descr_public_transport).reg();

	public static final ApplicationMode BOAT = createBase(R.string.app_mode_boat, "boat").speed(1.38f, 20).nauticalLocation().
			icon(R.drawable.ic_action_sail_boat_dark, R.drawable.map_action_sail_boat_dark, "ic_action_sail_boat_dark").setRoutingProfile("boat").description(R.string.base_profile_descr_boat).reg();

	public static final ApplicationMode AIRCRAFT = createBase(R.string.app_mode_aircraft, "aircraft").speed(40f, 100).carLocation().
			icon(R.drawable.ic_action_aircraft, R.drawable.map_action_aircraft,"ic_action_aircraft").setRouteService(RouteService.STRAIGHT).setRoutingProfile("STRAIGHT_LINE_MODE").description(R.string.base_profile_descr_aircraft).reg();

	public static final ApplicationMode SKI = createBase(R.string.app_mode_skiing, "ski").speed(1.38f, 15).arrivalDistance(60).offRouteDistance(50).bicycleLocation().
		icon(R.drawable.ic_action_skiing,  R.drawable.ic_action_skiing,"ic_action_skiing").setRoutingProfile("ski").description(R.string.base_profile_descr_ski).reg();


	private static class ApplicationModeBean {
		@Expose String stringKey;
		@Expose String userProfileName;
		@Expose String parent;
		@Expose String iconName = "map_world_globe_dark";
		@Expose ProfileIconColors iconColor = ProfileIconColors.DEFAULT;
		@Expose String routingProfile = null;
		@Expose RouteService routeService = RouteService.OSMAND;

	}

	private static void initRegVisibility() {
		// DEFAULT, CAR, BICYCLE, PEDESTRIAN, PUBLIC_TRANSPORT, BOAT, AIRCRAFT, SKI
		ApplicationMode[] exceptDefault = new ApplicationMode[]{CAR, BICYCLE, PEDESTRIAN, PUBLIC_TRANSPORT, BOAT, AIRCRAFT, SKI};
		ApplicationMode[] all = null;
		ApplicationMode[] none = new ApplicationMode[]{};

		// left
		ApplicationMode[] navigationSet1 = new ApplicationMode[]{CAR, BICYCLE, BOAT, SKI};
		ApplicationMode[] navigationSet2 = new ApplicationMode[]{PEDESTRIAN, PUBLIC_TRANSPORT, AIRCRAFT};

		regWidgetVisibility(WIDGET_NEXT_TURN, navigationSet1);
		regWidgetVisibility(WIDGET_NEXT_TURN_SMALL, navigationSet2);
		regWidgetVisibility(WIDGET_NEXT_NEXT_TURN, navigationSet1);
		regWidgetAvailability(WIDGET_NEXT_TURN, exceptDefault);
		regWidgetAvailability(WIDGET_NEXT_TURN_SMALL, exceptDefault);
		regWidgetAvailability(WIDGET_NEXT_NEXT_TURN, exceptDefault);

		// right
		regWidgetVisibility(WIDGET_INTERMEDIATE_DISTANCE, all);
		regWidgetVisibility(WIDGET_DISTANCE, all);
		regWidgetVisibility(WIDGET_TIME, all);
		regWidgetVisibility(WIDGET_INTERMEDIATE_TIME, all);
		regWidgetVisibility(WIDGET_SPEED, CAR, BICYCLE, BOAT, SKI, PUBLIC_TRANSPORT, AIRCRAFT);
		regWidgetVisibility(WIDGET_MAX_SPEED, CAR, DEFAULT);
		regWidgetVisibility(WIDGET_ALTITUDE, PEDESTRIAN, BICYCLE);
		regWidgetAvailability(WIDGET_INTERMEDIATE_DISTANCE, all);
		regWidgetAvailability(WIDGET_DISTANCE, all);
		regWidgetAvailability(WIDGET_TIME, all);
		regWidgetAvailability(WIDGET_INTERMEDIATE_TIME, all);
		regWidgetAvailability(WIDGET_SPEED, all);
		regWidgetAvailability(WIDGET_MAX_SPEED, CAR, DEFAULT);
		regWidgetAvailability(WIDGET_ALTITUDE, all);


		// all = null everything
		regWidgetAvailability(WIDGET_COMPASS, all);
		regWidgetAvailability(WIDGET_MARKER_1, none);
		regWidgetAvailability(WIDGET_MARKER_2, none);
		regWidgetAvailability(WIDGET_GPS_INFO, all);
		regWidgetAvailability(WIDGET_BATTERY, all);
		regWidgetAvailability(WIDGET_BEARING, all);
		regWidgetAvailability(WIDGET_RULER, all);
		regWidgetAvailability(WIDGET_PLAIN_TIME, all);

		// top
		// settings.SHOW_STREET_NAME
//		regWidgetVisibility(WIDGET_STREET_NAME, new ApplicationMode[]{CAR, BICYCLE, PEDESTRIAN, PUBLIC_TRANSPORT});
//		regWidgetAvailability(WIDGET_STREET_NAME, all);
	}


	public static class ApplicationModeBuilder {
		private ApplicationMode applicationMode;

		public ApplicationMode getApplicationMode() {
			return applicationMode;
		}

		private ApplicationMode reg() {
			values.add(applicationMode);
			defaultValues.add(applicationMode);
			return applicationMode;
		}

		private ApplicationMode customReg() {
			ApplicationMode m = applicationMode;
			m.defaultSpeed = m.parentAppMode.defaultSpeed;
			m.initialDefaultSpeed = m.parentAppMode.initialDefaultSpeed;
			m.minDistanceForTurn = m.parentAppMode.minDistanceForTurn;
			m.arrivalDistance = m.parentAppMode.arrivalDistance;
			m.offRouteDistance = m.parentAppMode.offRouteDistance;
			m.bearingIconDay = m.parentAppMode.bearingIconDay;
			m.bearingIconNight = m.parentAppMode.bearingIconNight;
			m.headingIconDay = m.parentAppMode.headingIconDay;
			m.headingIconNight = m.parentAppMode.headingIconNight;
			m.locationIconDay = m.parentAppMode.locationIconDay;
			m.locationIconNight = m.parentAppMode.locationIconNight;
			m.locationIconDayLost = m.parentAppMode.locationIconDayLost;
			m.locationIconNightLost = m.parentAppMode.locationIconNightLost;
			values.add(applicationMode);
			return applicationMode;
		}

		public ApplicationModeBuilder icon(int iconRes, int iconMapRes, String iconResName) {
			try {
				applicationMode.iconResName = iconResName;
				applicationMode.iconMapRes = iconMapRes;
				applicationMode.iconRes = iconRes;
			} catch (Exception e) {

			}
			return this;
		}

		public ApplicationModeBuilder description(int strId) {
			applicationMode.descriptionId = strId;
			return this;
		}

		public ApplicationModeBuilder icon(Context app, String iconResName) {
			try {
				applicationMode.iconResName = iconResName;
				applicationMode.iconMapRes = applicationMode.iconRes = app.getResources().getIdentifier(iconResName, "drawable", app.getPackageName());
				applicationMode.iconMapRes = app.getResources().getIdentifier(iconResName.replace("ic_", "map_"), "drawable", app.getPackageName());
			} catch (Exception e) {
//				return R.drawable.map_world_globe_dark;
			}
			return this;
		}

		public ApplicationModeBuilder parent(ApplicationMode parent) {
			applicationMode.parentAppMode = parent;
			if (parent == CAR || parent == AIRCRAFT) {
				this.carLocation();
			} else if (parent == BICYCLE || parent == SKI) {
				this.bicycleLocation();
			} else if (parent == BOAT) {
				this.nauticalLocation();
			} else {
				this.defLocation();
			}
			return this;
		}

		public ApplicationModeBuilder carLocation() {
			applicationMode.bearingIconDay = R.drawable.map_car_bearing;
			applicationMode.bearingIconNight = R.drawable.map_car_bearing_night;
			applicationMode.headingIconDay = R.drawable.map_car_location_view_angle;
			applicationMode.headingIconNight = R.drawable.map_car_location_view_angle_night;
			applicationMode.locationIconDay = R.drawable.map_car_location;
			applicationMode.locationIconNight = R.drawable.map_car_location_night;
			applicationMode.locationIconDayLost = R.drawable.map_car_location_lost;
			applicationMode.locationIconNightLost = R.drawable.map_car_location_lost_night;
			return this;
		}

		public ApplicationModeBuilder bicycleLocation() {
			applicationMode.bearingIconDay = R.drawable.map_bicycle_bearing;
			applicationMode.bearingIconNight = R.drawable.map_bicycle_bearing_night;
			applicationMode.headingIconDay = R.drawable.map_bicycle_location_view_angle;
			applicationMode.headingIconNight = R.drawable.map_bicycle_location_view_angle_night;
			applicationMode.locationIconDay = R.drawable.map_bicycle_location;
			applicationMode.locationIconNight = R.drawable.map_bicycle_location_night;
			applicationMode.locationIconDayLost = R.drawable.map_bicycle_location_lost;
			applicationMode.locationIconNightLost = R.drawable.map_bicycle_location_lost_night;
			return this;
		}

		public ApplicationModeBuilder defLocation() {
			applicationMode.bearingIconDay = R.drawable.map_pedestrian_bearing;
			applicationMode.bearingIconNight = R.drawable.map_pedestrian_bearing_night;
			applicationMode.headingIconDay = R.drawable.map_default_location_view_angle;
			applicationMode.headingIconNight = R.drawable.map_default_location_view_angle_night;
			applicationMode.locationIconDay = R.drawable.map_pedestrian_location;
			applicationMode.locationIconNight = R.drawable.map_pedestrian_location_night;
			applicationMode.locationIconDayLost = R.drawable.map_pedestrian_location_lost;
			applicationMode.locationIconNightLost = R.drawable.map_pedestrian_location_lost_night;
			return this;
		}

		public ApplicationModeBuilder nauticalLocation() {
			applicationMode.bearingIconDay = R.drawable.map_nautical_bearing;
			applicationMode.bearingIconNight = R.drawable.map_nautical_bearing_night;
			applicationMode.headingIconDay = R.drawable.map_nautical_location_view_angle;
			applicationMode.headingIconNight = R.drawable.map_nautical_location_view_angle_night;
			applicationMode.locationIconDay = R.drawable.map_nautical_location;
			applicationMode.locationIconNight = R.drawable.map_nautical_location_night;
			return this;
		}

		public ApplicationModeBuilder speed(float defSpeed, int distForTurn) {
			applicationMode.defaultSpeed = defSpeed;
			applicationMode.initialDefaultSpeed = defSpeed;
			applicationMode.minDistanceForTurn = distForTurn;
			return this;
		}

		public ApplicationModeBuilder arrivalDistance(int arrivalDistance) {
			applicationMode.arrivalDistance = arrivalDistance;
			return this;
		}

		public ApplicationModeBuilder offRouteDistance(int offRouteDistance) {
			applicationMode.offRouteDistance = offRouteDistance;
			return this;
		}

		public ApplicationModeBuilder userProfileTitle(String userProfileTitle) {
			applicationMode.userProfileName = userProfileTitle;
			return this;
		}

		public ApplicationModeBuilder setRoutingProfile(String routingProfileName) {
			applicationMode.routingProfile = routingProfileName;
			return this;
		}

		public ApplicationModeBuilder setRouteService(RouteService service) {
			applicationMode.routeService = service;
			return this;
		}

		public ApplicationModeBuilder setColor(ProfileIconColors colorData) {
			applicationMode.iconColor = colorData;
			return this;
		}
	}

	private static ApplicationModeBuilder create(ApplicationMode parent, int key, String stringKey) {
		ApplicationModeBuilder builder = new ApplicationModeBuilder();
		builder.applicationMode = new ApplicationMode(key, stringKey);
		builder.parent(parent);
		return builder;
	}

	private static ApplicationModeBuilder createBase(int key, String stringKey) {
		ApplicationModeBuilder builder = new ApplicationModeBuilder();
		builder.applicationMode = new ApplicationMode(key, stringKey);
		return builder;
	}

	public static ApplicationModeBuilder createCustomMode(ApplicationMode parent, String userProfileTitle, String stringKey) {
		return create(parent, -1, stringKey).userProfileTitle(userProfileTitle);
	}


	public static List<ApplicationMode> values(OsmandApplication app) {
		if (customizationListener == null) {
			customizationListener = new OsmAndAppCustomization.OsmAndAppCustomizationListener() {
				@Override
				public void onOsmAndSettingsCustomized() {
					cachedFilteredValues = new ArrayList<>();
				}
			};
			app.getAppCustomization().addListener(customizationListener);
		}
		if (cachedFilteredValues.isEmpty()) {

			OsmandSettings settings = app.getSettings();
			if (listener == null) {
				listener = new StateChangedListener<String>() {
					@Override
					public void stateChanged(String change) {
						cachedFilteredValues = new ArrayList<>();
					}
				};
				settings.AVAILABLE_APP_MODES.addListener(listener);
			}
			String available = settings.AVAILABLE_APP_MODES.get();
			cachedFilteredValues = new ArrayList<>();
			for (ApplicationMode v : values) {
				if (available.indexOf(v.getStringKey() + ",") != -1 || v == DEFAULT) {
					cachedFilteredValues.add(v);
				}
			}
		}
		return cachedFilteredValues;
	}

	public static List<ApplicationMode> allPossibleValues() {
		return values;
	}

	public static List<ApplicationMode> getDefaultValues() {
		return defaultValues;
	}

	// returns modifiable ! Set<ApplicationMode> to exclude non-wanted derived
	public static Set<ApplicationMode> regWidgetVisibility(String widgetId, ApplicationMode... am) {
		HashSet<ApplicationMode> set = new HashSet<>();
		if (am == null) {
			set.addAll(values);
		} else {
			Collections.addAll(set, am);
		}
		for (ApplicationMode m : values) {
			// add derived modes
			if (set.contains(m.getParent())) {
				set.add(m);
			}
		}
		widgetsVisibilityMap.put(widgetId, set);
		return set;
	}

	public boolean isWidgetCollapsible(String key) {
		return false;
	}

	public boolean isWidgetVisible(OsmandApplication app, String key) {
		if (app.getAppCustomization().areWidgetsCustomized()) {
			return app.getAppCustomization().isWidgetVisible(key, this);
		}
		Set<ApplicationMode> set = widgetsVisibilityMap.get(key);
		if (set == null) {
			return false;
		}
		return set.contains(this);
	}

	public static Set<ApplicationMode> regWidgetAvailability(String widgetId, ApplicationMode... am) {
		HashSet<ApplicationMode> set = new HashSet<>();
		if (am == null) {
			set.addAll(values);
		} else {
			Collections.addAll(set, am);
		}
		for (ApplicationMode m : values) {
			// add derived modes
			if (set.contains(m.getParent())) {
				set.add(m);
			}
		}
		widgetsAvailabilityMap.put(widgetId, set);
		return set;
	}

	public boolean isWidgetAvailable(OsmandApplication app, String key) {
		if (app.getAppCustomization().areWidgetsCustomized()) {
			return app.getAppCustomization().isWidgetAvailable(key, this);
		}
		Set<ApplicationMode> set = widgetsAvailabilityMap.get(key);
		if (set == null) {
			return true;
		}
		return set.contains(this);
	}

	public static List<ApplicationMode> getModesDerivedFrom(ApplicationMode am) {
		List<ApplicationMode> list = new ArrayList<ApplicationMode>();
		for (ApplicationMode a : values) {
			if (a == am || a.getParent() == am) {
				list.add(a);
			}
		}
		return list;
	}

	public boolean isCustomProfile() {
		return parentAppMode != null;
	}

	public ApplicationMode getParent() {
		return parentAppMode;
	}

	public boolean hasFastSpeed() {
		return getDefaultSpeed() > 10;
	}

	public int getResourceBearingDay() {
		return bearingIconDay;
	}

	public int getResourceBearingNight() {
		return bearingIconNight;
	}

	public int getResourceHeadingDay() {
		return headingIconDay;
	}

	public int getResourceHeadingNight() {
		return headingIconNight;
	}

	public int getResourceLocationDay() {
		return locationIconDay;
	}

	public int getResourceLocationNight() {
		return locationIconNight;
	}

	public int getResourceLocationDayLost() {
		return locationIconDayLost;
	}

	public int getResourceLocationNightLost() {
		return locationIconNightLost;
	}

	public String getStringKey() {
		return stringKey;
	}

	public int getNameKeyResource() {
		return keyName;
	}


	public String getCustomProfileName() {
		return userProfileName;
	}

	public String toHumanString(Context ctx) {
		if (Algorithms.isEmpty(userProfileName) && keyName != -1) {
			return ctx.getString(keyName);
		} else {
			return userProfileName;
		}
	}

	public String getDescription(Context ctx) {
		if (descriptionId != 0) {
			return ctx.getString(descriptionId);
		}
		return "";
	}

	public RouteService getRouteService() {
		return routeService;
	}

	public static ApplicationMode valueOfStringKey(String key, ApplicationMode def) {
		for (ApplicationMode p : values) {
			if (p.getStringKey().equals(key)) {
				return p;
			}
		}
		return def;
	}

	public String getIconName() {
		return iconResName;
	}

	public float getDefaultSpeed() {
		return defaultSpeed;
	}

	public void setDefaultSpeed(OsmandApplication app, float defaultSpeed) {
		this.defaultSpeed = defaultSpeed;
		app.getSettings().DEFAULT_SPEED.set(defaultSpeed);

	}

	public void resetDefaultSpeed(OsmandApplication app) {
		this.defaultSpeed = initialDefaultSpeed;
		app.getSettings().DEFAULT_SPEED.setModeValue(this, 0f);
	}

	public int getMinDistanceForTurn() {
		return minDistanceForTurn;
	}

	public int getArrivalDistance() {
		return arrivalDistance;
	}

	public int getOffRouteDistance() {
		return offRouteDistance;
	}

	public boolean isDerivedRoutingFrom(ApplicationMode mode) {
		return this == mode || getParent() == mode;
	}

	public String getRoutingProfile() {
		return routingProfile;
	}


	@DrawableRes public int getIconRes() {
		return iconRes;
	}

	@DrawableRes public int getMapIconRes() {
		return iconMapRes;
	}

	public ProfileIconColors getIconColorInfo() {
		if (iconColor != null) {
			return iconColor;
		} else {
			return ProfileIconColors.DEFAULT;
		}
	}


	public static void onApplicationStart(OsmandApplication app) {
		// load for default profiles to initialize later custom modes
		initDefaultSpeed(app);
		initCustomModes(app);
		initDefaultSpeed(app);
		initRegVisibility();
	}

	private static void initDefaultSpeed(OsmandApplication app) {
		for(ApplicationMode m : values) {
			float spd = app.getSettings().DEFAULT_SPEED.getModeValue(m);
			if(spd > 0) {
				m.defaultSpeed = spd;
			}
		}
	}

	public static ApplicationModeBuilder fromJson(OsmandApplication app, String json) {
		Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
		ApplicationModeBean mb = gson.fromJson(json, ApplicationModeBean.class);

		ApplicationModeBuilder b = createCustomMode(valueOfStringKey(mb.parent, null),
				mb.userProfileName, mb.stringKey);
		b.setRouteService(mb.routeService).setRoutingProfile(mb.routingProfile);
		b.icon(app, mb.iconName);
		b.setColor(mb.iconColor);
		return b;
	}

	public String toJson() {
		ApplicationModeBean mb = new ApplicationModeBean();
		mb.userProfileName = userProfileName;
		mb.iconColor = iconColor;
		mb.iconName = iconResName;
		mb.parent = parentAppMode != null ? parentAppMode.getStringKey() : null;
		mb.stringKey = stringKey;
		mb.routeService = routeService;
		mb.routingProfile = routingProfile;
		Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
		return gson.toJson(mb);
	}

	private static void initCustomModes(OsmandApplication app){
		Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
		Type t = new TypeToken<ArrayList<ApplicationModeBean>>() {}.getType();
		List<ApplicationModeBean> customProfiles = gson.fromJson(app.getSettings().CUSTOM_APP_PROFILES.get(), t);

		if (!Algorithms.isEmpty(customProfiles)) {
			for (ApplicationModeBean m : customProfiles) {
				ApplicationModeBuilder b = createCustomMode(valueOfStringKey(m.parent, CAR),
						m.userProfileName, m.stringKey);
				b.setRouteService(m.routeService).setRoutingProfile(m.routingProfile);
				b.icon(app, m.iconName);
				b.setColor(m.iconColor);
				b.customReg();
			}
		}

	}

	private static void saveCustomModeToSettings(OsmandSettings settings){
		List<ApplicationModeBean> customModes = new ArrayList<>();
		for (ApplicationMode mode : values) {
			if (mode.parentAppMode != null) {
				ApplicationModeBean mb = new ApplicationModeBean();
				mb.userProfileName = mode.userProfileName;
				mb.iconColor = mode.iconColor;
				mb.iconName = mode.iconResName;
				mb.parent = mode.parentAppMode.getStringKey();
				mb.stringKey = mode.stringKey;
				mb.routeService = mode.routeService;
				mb.routingProfile = mode.routingProfile;
				customModes.add(mb);
			}
		}
		Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
		String profiles = gson.toJson(customModes);
		settings.CUSTOM_APP_PROFILES.set(profiles);
	}

	public static ApplicationMode saveCustomProfile(ApplicationModeBuilder builder, OsmandApplication app) {
		ApplicationMode mode = null;
		for(ApplicationMode m : values) {
			if(m.stringKey.equals(builder.applicationMode.stringKey)) {
				mode = m;
				mode.iconResName = builder.applicationMode.iconResName;
				mode.iconRes = builder.applicationMode.iconRes;
				mode.userProfileName = builder.applicationMode.userProfileName;
				mode.parentAppMode = builder.applicationMode.parentAppMode;
				mode.routingProfile = builder.applicationMode.routingProfile;
				mode.routeService = builder.applicationMode.routeService;
				mode.iconColor = builder.applicationMode.iconColor;
				break;
			}
		}
		if(mode == null) {
			mode = builder.customReg();
			initRegVisibility();
		}
		saveCustomModeToSettings(app.getSettings());

		return mode;
	}

	public static void deleteCustomMode(ApplicationMode md, OsmandApplication app) {
		Iterator<ApplicationMode> it = values.iterator();
		while (it.hasNext()) {
			ApplicationMode m = it.next();
			if (m == md) {
				it.remove();
			}
		}
		cachedFilteredValues.remove(md);
		saveCustomModeToSettings(app.getSettings());
	}

	public static boolean changeProfileAvailability(ApplicationMode mode, boolean isSelected, OsmandApplication app) {
		Set<ApplicationMode> selectedModes = new LinkedHashSet<>(ApplicationMode.values(app));
		StringBuilder vls = new StringBuilder(ApplicationMode.DEFAULT.getStringKey() + ",");
		if (allPossibleValues().contains(mode)) {
			if (isSelected) {
				selectedModes.add(mode);
			} else {
				selectedModes.remove(mode);
				if (app.getSettings().APPLICATION_MODE.get() == mode) {
					app.getSettings().APPLICATION_MODE.set(ApplicationMode.DEFAULT);
				}
			}
			for (ApplicationMode m : selectedModes) {
				vls.append(m.getStringKey()).append(",");
			}
			app.getSettings().AVAILABLE_APP_MODES.set(vls.toString());
			return true;
		}
		return false;
	}

	public enum ProfileIconColors {
		DEFAULT(R.string.rendering_value_default_name, R.color.profile_icon_color_blue_light_default,  R.color.profile_icon_color_blue_dark_default),
		PURPLE(R.string.rendering_value_purple_name, R.color.profile_icon_color_purple_light, R.color.profile_icon_color_purple_dark),
		GREEN(R.string.rendering_value_green_name, R.color.profile_icon_color_green_light,  R.color.profile_icon_color_green_dark),
		BLUE(R.string.rendering_value_blue_name, R.color.profile_icon_color_blue_light,  R.color.profile_icon_color_blue_dark),
		RED(R.string.rendering_value_red_name, R.color.profile_icon_color_red_light, R.color.profile_icon_color_red_dark),
		DARK_YELLOW(R.string.rendering_value_darkyellow_name, R.color.profile_icon_color_yellow_light, R.color.profile_icon_color_yellow_dark),
		MAGENTA(R.string.shared_string_color_magenta, R.color.profile_icon_color_magenta_light, R.color.profile_icon_color_magenta_dark);

		@StringRes private int name;
		@ColorRes private int dayColor;
		@ColorRes private int nightColor;

		ProfileIconColors(@StringRes int name, @ColorRes int dayColor, @ColorRes int nightColor) {
			this.name = name;
			this.dayColor = dayColor;
			this.nightColor = nightColor;
		}

		public int getName() {
			return name;
		}

		public int getColor(boolean nightMode) {
			if (nightMode) {
				return nightColor;
			} else {
				return dayColor;
			}
		}
 	}

}