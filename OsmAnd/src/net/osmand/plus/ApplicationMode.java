package net.osmand.plus;

import android.content.Context;
import android.support.annotation.DrawableRes;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import com.google.gson.reflect.TypeToken;

import net.osmand.StateChangedListener;
import net.osmand.plus.profiles.LocationIcon;
import net.osmand.plus.profiles.NavigationIcon;
import net.osmand.plus.profiles.ProfileIconColors;
import net.osmand.plus.routing.RouteProvider.RouteService;
import net.osmand.util.Algorithms;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
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
	private int order;

	private ApplicationMode parentAppMode;
	private String iconResName = "ic_world_globe_dark";
	private int iconRes = R.drawable.ic_world_globe_dark;
	private int iconMapRes = R.drawable.map_world_globe_dark;
	private ProfileIconColors iconColor = ProfileIconColors.DEFAULT;
	private String routingProfile = "";
	private RouteService routeService = RouteService.OSMAND;

	private float defaultSpeed = 10f;
	private int minDistanceForTurn = 50;
	private int arrivalDistance = 90;
	private int offRouteDistance = 350;
	private NavigationIcon navigationIcon = NavigationIcon.DEFAULT;
	private LocationIcon locationIcon = LocationIcon.DEFAULT;

	private ApplicationMode(int key, String stringKey) {
		this.keyName = key;
		this.stringKey = stringKey;
	}

	/*
	 * DEFAULT("Browse map"), CAR("Car"), BICYCLE("Bicycle"), PEDESTRIAN("Pedestrian"); NAUTICAL("boat"); PUBLIC_TRANSPORT("Public transport"); AIRCRAFT("Aircraft")
	 */
	public static final ApplicationMode DEFAULT = createBase(R.string.app_mode_default, "default")
			.speed(1.5f, 5).arrivalDistance(90)
			.locationIcon(LocationIcon.DEFAULT).navigationIcon(NavigationIcon.DEFAULT)
			.icon(R.drawable.ic_world_globe_dark, R.drawable.map_world_globe_dark, "ic_world_globe_dark").reg();

	public static final ApplicationMode CAR = createBase(R.string.app_mode_car, "car")
			.speed(12.5f, 35)
			.locationIcon(LocationIcon.CAR).navigationIcon(NavigationIcon.DEFAULT)
			.icon(R.drawable.ic_action_car_dark, R.drawable.map_action_car_dark, "ic_action_car_dark")
			.setRoutingProfile("car").description(R.string.base_profile_descr_car).reg();

	public static final ApplicationMode BICYCLE = createBase(R.string.app_mode_bicycle, "bicycle")
			.speed(2.77f, 15).arrivalDistance(60).offRouteDistance(50)
			.locationIcon(LocationIcon.BICYCLE).navigationIcon(NavigationIcon.DEFAULT)
			.icon(R.drawable.ic_action_bicycle_dark, R.drawable.map_action_bicycle_dark, "ic_action_bicycle_dark")
			.setRoutingProfile("bicycle").description(R.string.base_profile_descr_bicycle).reg();

	public static final ApplicationMode PEDESTRIAN = createBase(R.string.app_mode_pedestrian, "pedestrian")
			.speed(1.11f, 5).arrivalDistance(45).offRouteDistance(20)
			.icon(R.drawable.ic_action_pedestrian_dark, R.drawable.map_action_pedestrian_dark, "ic_action_pedestrian_dark")
			.setRoutingProfile("pedestrian").description(R.string.base_profile_descr_pedestrian).reg();

	public static final ApplicationMode PUBLIC_TRANSPORT = createBase(R.string.app_mode_public_transport, "public_transport")
			.icon(R.drawable.ic_action_bus_dark, R.drawable.map_action_bus_dark, "ic_action_bus_dark")
			.setRoutingProfile("public_transport").description(R.string.base_profile_descr_public_transport).reg();

	public static final ApplicationMode BOAT = createBase(R.string.app_mode_boat, "boat")
			.speed(1.38f, 20)
			.locationIcon(LocationIcon.DEFAULT).navigationIcon(NavigationIcon.NAUTICAL)
			.icon(R.drawable.ic_action_sail_boat_dark, R.drawable.map_action_sail_boat_dark, "ic_action_sail_boat_dark")
			.setRoutingProfile("boat").description(R.string.base_profile_descr_boat).reg();

	public static final ApplicationMode AIRCRAFT = createBase(R.string.app_mode_aircraft, "aircraft")
			.speed(40f, 100)
			.locationIcon(LocationIcon.CAR).navigationIcon(NavigationIcon.DEFAULT)
			.icon(R.drawable.ic_action_aircraft, R.drawable.map_action_aircraft, "ic_action_aircraft").setRouteService(RouteService.STRAIGHT)
			.setRoutingProfile("STRAIGHT_LINE_MODE").description(R.string.base_profile_descr_aircraft).reg();

	public static final ApplicationMode SKI = createBase(R.string.app_mode_skiing, "ski")
			.speed(1.38f, 15).arrivalDistance(60).offRouteDistance(50)
			.locationIcon(LocationIcon.BICYCLE).navigationIcon(NavigationIcon.DEFAULT)
			.icon(R.drawable.ic_action_skiing, R.drawable.map_action_skiing, "ic_action_skiing")
			.setRoutingProfile("ski").description(R.string.base_profile_descr_ski).reg();

	private static class ApplicationModeBean {
		@Expose
		String stringKey;
		@Expose
		String userProfileName;
		@Expose
		String parent;
		@Expose
		String iconName = "map_world_globe_dark";
		@Expose
		ProfileIconColors iconColor = ProfileIconColors.DEFAULT;
		@Expose
		String routingProfile = null;
		@Expose
		RouteService routeService = RouteService.OSMAND;
		@Expose
		LocationIcon locIcon = null;
		@Expose
		NavigationIcon navIcon = null;
		@Expose
		int order;
	}

	private static void initRegVisibility() {
		// DEFAULT, CAR, BICYCLE, PEDESTRIAN, PUBLIC_TRANSPORT, BOAT, AIRCRAFT, SKI
		ApplicationMode[] exceptDefault = new ApplicationMode[] {CAR, BICYCLE, PEDESTRIAN, PUBLIC_TRANSPORT, BOAT, AIRCRAFT, SKI};
		ApplicationMode[] all = null;
		ApplicationMode[] none = new ApplicationMode[] {};

		// left
		ApplicationMode[] navigationSet1 = new ApplicationMode[] {CAR, BICYCLE, BOAT, SKI};
		ApplicationMode[] navigationSet2 = new ApplicationMode[] {PEDESTRIAN, PUBLIC_TRANSPORT, AIRCRAFT};

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
		regWidgetVisibility(WIDGET_MAX_SPEED, CAR);
		regWidgetVisibility(WIDGET_ALTITUDE, PEDESTRIAN, BICYCLE);
		regWidgetAvailability(WIDGET_INTERMEDIATE_DISTANCE, all);
		regWidgetAvailability(WIDGET_DISTANCE, all);
		regWidgetAvailability(WIDGET_TIME, all);
		regWidgetAvailability(WIDGET_INTERMEDIATE_TIME, all);
		regWidgetAvailability(WIDGET_SPEED, all);
		regWidgetAvailability(WIDGET_MAX_SPEED, CAR);
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
			if (applicationMode.getOrder() == 0 && !values.isEmpty()) {
				applicationMode.order = values.size();
			}
			return applicationMode;
		}

		private ApplicationMode customReg() {
			values.add(applicationMode);
			if (applicationMode.getOrder() == 0 && !values.isEmpty()) {
				applicationMode.order = values.size();
			}
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
			updateAppModeIcon(app, iconResName, applicationMode);
			return this;
		}

		public ApplicationModeBuilder parent(ApplicationMode parent) {
			applicationMode.parentAppMode = parent;
			return this;
		}

		public ApplicationModeBuilder locationIcon(LocationIcon locationIcon) {
			applicationMode.locationIcon = locationIcon;
			return this;
		}

		public ApplicationModeBuilder navigationIcon(NavigationIcon navigationIcon) {
			applicationMode.navigationIcon = navigationIcon;
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

		public ApplicationModeBuilder setOrder(int order) {
			applicationMode.order = order;
			return this;
		}
	}

	private static void updateAppModeIcon(Context app, String iconResName, ApplicationMode mode) {
		try {
			int iconRes = app.getResources().getIdentifier(iconResName, "drawable", app.getPackageName());
			int iconMapRes = app.getResources().getIdentifier(iconResName.replace("ic_", "map_"), "drawable", app.getPackageName());
			if (iconRes != 0 && iconMapRes != 0) {
				mode.iconResName = iconResName;
				mode.iconRes = iconRes;
				mode.iconMapRes = iconMapRes;
			}
		} catch (Exception e) {
//				return R.drawable.map_world_globe_dark;
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

	public static ApplicationModeBuilder changeBaseMode(ApplicationMode applicationMode) {
		ApplicationModeBuilder builder = new ApplicationModeBuilder();
		builder.applicationMode = applicationMode;
		return builder;
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

	public static List<ApplicationMode> getCustomValues() {
		List<ApplicationMode> customModes = new ArrayList<>();
		for (ApplicationMode mode : values) {
			if (mode.isCustomProfile()) {
				customModes.add(mode);
			}
		}
		return customModes;
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

	public NavigationIcon getNavigationIcon() {
		return navigationIcon;
	}

	public LocationIcon getLocationIcon() {
		return locationIcon;
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

	public void setIconResName(OsmandApplication app, String iconResName) {
		updateAppModeIcon(app, iconResName, this);
		app.getSettings().ICON_RES_NAME.setModeValue(this, iconResName);
	}

	public void setIconColor(OsmandApplication app, ProfileIconColors iconColor) {
		this.iconColor = iconColor;
		app.getSettings().ICON_COLOR.setModeValue(this, iconColor);
	}

	public void setUserProfileName(OsmandApplication app, String userProfileName) {
		this.userProfileName = userProfileName;
		app.getSettings().USER_PROFILE_NAME.setModeValue(this, userProfileName);
	}

	public void setParentAppMode(OsmandApplication app, ApplicationMode parentAppMode) {
		if (isCustomProfile()) {
			this.parentAppMode = parentAppMode;
			app.getSettings().PARENT_APP_MODE.setModeValue(this, parentAppMode.getStringKey());
		}
	}

	public void setRoutingProfile(OsmandApplication app, String routingProfile) {
		this.routingProfile = routingProfile;
		app.getSettings().ROUTING_PROFILE.setModeValue(this, routingProfile);
	}

	public void setRouteService(OsmandApplication app, RouteService routeService) {
		this.routeService = routeService;
		app.getSettings().ROUTE_SERVICE.setModeValue(this, routeService);
	}

	public void setNavigationIcon(OsmandApplication app, NavigationIcon navigationIcon) {
		this.navigationIcon = navigationIcon;
		app.getSettings().NAVIGATION_ICON.setModeValue(this, navigationIcon);
	}

	public void setLocationIcon(OsmandApplication app, LocationIcon locationIcon) {
		this.locationIcon = locationIcon;
		app.getSettings().LOCATION_ICON.setModeValue(this, locationIcon);
	}

	public void setDefaultSpeed(OsmandApplication app, float defaultSpeed) {
		this.defaultSpeed = defaultSpeed;
		app.getSettings().DEFAULT_SPEED.setModeValue(this, defaultSpeed);
	}

	public void resetDefaultSpeed(OsmandApplication app) {
		OsmandSettings settings = app.getSettings();
		settings.DEFAULT_SPEED.resetModeToDefault(this);
		this.defaultSpeed = settings.DEFAULT_SPEED.getModeValue(this);
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

	@DrawableRes
	public int getIconRes() {
		return iconRes;
	}

	@DrawableRes
	public int getMapIconRes() {
		return iconMapRes;
	}

	public ProfileIconColors getIconColorInfo() {
		if (iconColor != null) {
			return iconColor;
		} else {
			return ProfileIconColors.DEFAULT;
		}
	}

	public int getOrder() {
		return order;
	}

	public void setOrder(int order) {
		this.order = order;
	}

	public static void onApplicationStart(OsmandApplication app) {
		// load for default profiles to initialize later custom modes
		initDefaultModesParams(app);
		initCustomModes(app);
		initModesParams(app);
		initRegVisibility();
		reorderAppModes();
	}

	private static void initModesParams(OsmandApplication app) {
		OsmandSettings settings = app.getSettings();
		Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
		Type t = new TypeToken<Map<String, Integer>>() {
		}.getType();
		Map<String, Integer> appModesOrders = gson.fromJson(app.getSettings().APP_MODES_ORDERS.get(), t);

		for (ApplicationMode mode : allPossibleValues()) {
			mode.routingProfile = settings.ROUTING_PROFILE.getModeValue(mode);
			mode.routeService = settings.ROUTE_SERVICE.getModeValue(mode);
			mode.defaultSpeed = settings.DEFAULT_SPEED.getModeValue(mode);
			mode.minDistanceForTurn = settings.MIN_DISTANCE_FOR_TURN.getModeValue(mode);
			mode.arrivalDistance = settings.ARRIVAL_DISTANCE.getModeValue(mode);
			mode.offRouteDistance = settings.OFF_ROUTE_DISTANCE.getModeValue(mode);
			mode.userProfileName = settings.USER_PROFILE_NAME.getModeValue(mode);
			mode.navigationIcon = settings.NAVIGATION_ICON.getModeValue(mode);
			mode.locationIcon = settings.LOCATION_ICON.getModeValue(mode);
			mode.iconColor = settings.ICON_COLOR.getModeValue(mode);
			Integer order = appModesOrders.get(mode.getStringKey());
			if (order != null) {
				mode.order = order;
			}
			updateAppModeIcon(app, settings.ICON_RES_NAME.getModeValue(mode), mode);
		}
	}

	public static void reorderAppModes() {
		Comparator<ApplicationMode> comparator = new Comparator<ApplicationMode>() {
			@Override
			public int compare(ApplicationMode mode1, ApplicationMode mode2) {
				return (mode1.order < mode2.order) ? -1 : ((mode1.order == mode2.order) ? 0 : 1);
			}
		};
		Collections.sort(values, comparator);
		Collections.sort(defaultValues, comparator);
		Collections.sort(cachedFilteredValues, comparator);
		updateAppModesOrder();
	}

	private static void updateAppModesOrder() {
		for (int i = 0; i < values.size(); i++) {
			values.get(i).setOrder(i);
		}
	}

	public static ApplicationModeBuilder fromJson(OsmandApplication app, String json) {
		Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
		ApplicationModeBean mb = gson.fromJson(json, ApplicationModeBean.class);

		ApplicationModeBuilder b = createCustomMode(valueOfStringKey(mb.parent, null),
				mb.userProfileName, mb.stringKey);
		b.setRouteService(mb.routeService);
		b.setRoutingProfile(mb.routingProfile);
		b.icon(app, mb.iconName);
		b.setColor(mb.iconColor);
		b.setOrder(mb.order);
		b.locationIcon(mb.locIcon);
		b.navigationIcon(mb.navIcon);
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
		mb.locIcon = locationIcon;
		mb.navIcon = navigationIcon;
		mb.order = order;
		Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
		return gson.toJson(mb);
	}

	private static void initDefaultModesParams(OsmandApplication app) {
		OsmandSettings settings = app.getSettings();
		if (settings.DEFAULT_APP_PROFILES.isSet()) {
			Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
			Type t = new TypeToken<ArrayList<ApplicationModeBean>>() {
			}.getType();
			List<ApplicationModeBean> defaultAppModeBeans = gson.fromJson(settings.DEFAULT_APP_PROFILES.get(), t);
			if (!Algorithms.isEmpty(defaultAppModeBeans)) {
				for (ApplicationModeBean modeBean : defaultAppModeBeans) {
					ApplicationMode mode = ApplicationMode.valueOfStringKey(modeBean.stringKey, null);
					if (mode != null) {
						ApplicationModeBuilder builder = changeBaseMode(mode)
								.setRouteService(modeBean.routeService)
								.setRoutingProfile(modeBean.routingProfile)
								.icon(app, modeBean.iconName)
								.setColor(modeBean.iconColor)
								.locationIcon(modeBean.locIcon)
								.navigationIcon(modeBean.navIcon)
								.setOrder(modeBean.order);
						saveProfile(builder, app);
					}
				}
			}
			settings.DEFAULT_APP_PROFILES.resetToDefault();
		}
	}

	private static void initCustomModes(OsmandApplication app) {
		OsmandSettings settings = app.getSettings();
		if (settings.CUSTOM_APP_PROFILES.isSet()) {
			Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
			Type t = new TypeToken<ArrayList<ApplicationModeBean>>() {
			}.getType();
			List<ApplicationModeBean> customProfiles = gson.fromJson(app.getSettings().CUSTOM_APP_PROFILES.get(), t);
			if (!Algorithms.isEmpty(customProfiles)) {
				for (ApplicationModeBean m : customProfiles) {
					ApplicationMode parentMode = valueOfStringKey(m.parent, CAR);
					ApplicationModeBuilder builder = createCustomMode(parentMode, m.userProfileName, m.stringKey)
							.setRouteService(m.routeService)
							.setRoutingProfile(m.routingProfile)
							.icon(app, m.iconName)
							.setColor(m.iconColor)
							.locationIcon(m.locIcon)
							.navigationIcon(m.navIcon)
							.setOrder(m.order);
					saveProfile(builder, app);
				}
			}
			settings.CUSTOM_APP_PROFILES.resetToDefault();
		}
		if (settings.CUSTOM_APP_MODES_KEYS.isSet()) {
			Set<String> customModesKeys = settings.getCustomAppModesKeys();
			for (String appModeKey : customModesKeys) {
				Object profilePreferences = settings.getProfilePreferences(appModeKey);
				String parent = settings.PARENT_APP_MODE.getValue(profilePreferences, null);
				String userProfileName = settings.USER_PROFILE_NAME.getValue(profilePreferences, "");
				createCustomMode(valueOfStringKey(parent, CAR), userProfileName, appModeKey).customReg();
			}
		}
	}

	public static void saveAppModesToSettings(OsmandApplication app) {
		OsmandSettings settings = app.getSettings();
		StringBuilder stringBuilder = new StringBuilder();
		Iterator<ApplicationMode> it = ApplicationMode.getCustomValues().iterator();
		while (it.hasNext()) {
			stringBuilder.append(it.next().getStringKey());
			if (it.hasNext()) {
				stringBuilder.append(",");
			}
		}
		if (!stringBuilder.toString().equals(settings.CUSTOM_APP_MODES_KEYS.get())) {
			settings.CUSTOM_APP_MODES_KEYS.set(stringBuilder.toString());
		}

		Map<String, Integer> appModesOrders = new HashMap<>();
		for (ApplicationMode mode : allPossibleValues()) {
			appModesOrders.put(mode.getStringKey(), mode.getOrder());
		}
		Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
		String modesOrdersStr = gson.toJson(appModesOrders);
		settings.APP_MODES_ORDERS.set(modesOrdersStr);
	}

	public static ApplicationMode saveProfile(ApplicationModeBuilder builder, OsmandApplication app) {
		ApplicationMode mode = ApplicationMode.valueOfStringKey(builder.applicationMode.stringKey, null);
		if (mode == null) {
			mode = builder.customReg();
			initRegVisibility();
		}

		mode.setIconResName(app, builder.applicationMode.iconResName);
		mode.setUserProfileName(app, builder.applicationMode.userProfileName);
		mode.setParentAppMode(app, builder.applicationMode.parentAppMode);
		mode.setRoutingProfile(app, builder.applicationMode.routingProfile);
		mode.setRouteService(app, builder.applicationMode.routeService);
		mode.setIconColor(app, builder.applicationMode.iconColor);
		mode.setLocationIcon(app, builder.applicationMode.locationIcon);
		mode.setNavigationIcon(app, builder.applicationMode.navigationIcon);
		mode.setOrder(builder.applicationMode.order);

		saveAppModesToSettings(app);
		return mode;
	}

	public static void deleteCustomModes(List<ApplicationMode> modes, OsmandApplication app) {
		Iterator<ApplicationMode> it = values.iterator();
		while (it.hasNext()) {
			ApplicationMode m = it.next();
			if (modes.contains(m)) {
				it.remove();
			}
		}
		OsmandSettings settings = app.getSettings();
		if (modes.contains(settings.APPLICATION_MODE.get())) {
			settings.APPLICATION_MODE.resetToDefault();
		}
		cachedFilteredValues.removeAll(modes);
		saveAppModesToSettings(app);
	}

	public static boolean changeProfileAvailability(ApplicationMode mode, boolean isSelected, OsmandApplication app) {
		Set<ApplicationMode> selectedModes = new LinkedHashSet<>(ApplicationMode.values(app));
		StringBuilder vls = new StringBuilder(ApplicationMode.DEFAULT.getStringKey() + ",");
		if (allPossibleValues().contains(mode)) {
			OsmandSettings settings = app.getSettings();
			if (isSelected) {
				selectedModes.add(mode);
			} else {
				selectedModes.remove(mode);
				if (settings.APPLICATION_MODE.get() == mode) {
					settings.APPLICATION_MODE.resetToDefault();
				}
			}
			for (ApplicationMode m : selectedModes) {
				vls.append(m.getStringKey()).append(",");
			}
			settings.AVAILABLE_APP_MODES.set(vls.toString());
			return true;
		}
		return false;
	}
}