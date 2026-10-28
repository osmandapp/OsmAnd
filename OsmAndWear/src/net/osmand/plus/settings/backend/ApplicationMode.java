package net.osmand.plus.settings.backend;

import static net.osmand.binary.BinaryMapRouteReaderAdapter.RouteTypeRule;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.osmand.StateChangedListener;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.profiles.ProfileIconColors;
import net.osmand.plus.routing.RouteService;
import net.osmand.plus.settings.backend.OsmAndAppCustomization.OsmAndAppCustomizationListener;
import net.osmand.plus.settings.enums.MarkerDisplayOption;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class ApplicationMode {

	public static final String CUSTOM_MODE_KEY_SEPARATOR = "_";
	public static final float FAST_SPEED_THRESHOLD = 10;
	private static final float MIN_VALUE_KM_H = -10;
	private static final float MAX_VALUE_KM_H = 20;

	private static final List<ApplicationMode> defaultValues = new ArrayList<>();
	private static final List<ApplicationMode> values = new ArrayList<>();
	private static List<ApplicationMode> cachedFilteredValues = new ArrayList<>();

	private static StateChangedListener<String> listener;
	private static StateChangedListener<String> iconNameListener;
	private static OsmAndAppCustomizationListener customizationListener;

	private OsmandApplication app;

	private final int keyName;
	private final String stringKey;
	private int descriptionId;

	private ApplicationMode parentAppMode;
	private int iconRes = R.drawable.ic_world_globe_dark;

	private ApplicationMode(int key, @NonNull String stringKey) {
		this.keyName = key;
		this.stringKey = stringKey;
	}

	public static final ApplicationMode DEFAULT = createBase(R.string.app_mode_default, "default")
			.icon(R.drawable.ic_world_globe_dark).reg();

	public static final ApplicationMode CAR = createBase(R.string.app_mode_car, "car")
			.icon(R.drawable.ic_action_car_dark)
			.description(R.string.base_profile_descr_car).reg();

	public static final ApplicationMode BICYCLE = createBase(R.string.app_mode_bicycle, "bicycle")
			.icon(R.drawable.ic_action_bicycle_dark)
			.description(R.string.base_profile_descr_bicycle).reg();

	public static final ApplicationMode PEDESTRIAN = createBase(R.string.app_mode_pedestrian, "pedestrian")
			.icon(R.drawable.ic_action_pedestrian_dark)
			.description(R.string.base_profile_descr_pedestrian).reg();

	public static final ApplicationMode TRUCK = create(CAR, R.string.app_mode_truck, "truck")
			.icon(R.drawable.ic_action_truck_dark)
			.description(R.string.app_mode_truck).reg();

	public static final ApplicationMode MOTORCYCLE = create(CAR, R.string.app_mode_motorcycle, "motorcycle")
			.icon(R.drawable.ic_action_motorcycle_dark)
			.description(R.string.app_mode_motorcycle).reg();
	public static final ApplicationMode MOPED = create(BICYCLE, R.string.app_mode_moped, "moped")
			.icon(R.drawable.ic_action_motor_scooter)
			.description(R.string.app_mode_moped).reg();

	public static final ApplicationMode PUBLIC_TRANSPORT = createBase(R.string.app_mode_public_transport, "public_transport")
			.icon(R.drawable.ic_action_bus_dark)
			.description(R.string.base_profile_descr_public_transport).reg();

	public static final ApplicationMode TRAIN = createBase(R.string.app_mode_train, "train")
			.icon(R.drawable.ic_action_train)
			.description(R.string.app_mode_train).reg();

	public static final ApplicationMode BOAT = createBase(R.string.app_mode_boat, "boat")
			.icon(R.drawable.ic_action_sail_boat_dark)
			.description(R.string.base_profile_descr_boat).reg();

	public static final ApplicationMode AIRCRAFT = createBase(R.string.app_mode_aircraft, "aircraft")
			.icon(R.drawable.ic_action_aircraft)
			.description(R.string.base_profile_descr_aircraft).reg();

	public static final ApplicationMode SKI = createBase(R.string.app_mode_skiing, "ski")
			.icon(R.drawable.ic_action_skiing)
			.description(R.string.base_profile_descr_ski).reg();

	public static final ApplicationMode HORSE = createBase(R.string.horseback_riding, "horse")
			.icon(R.drawable.ic_action_horse)
			.description(R.string.horseback_riding).reg();

	public static List<ApplicationMode> values(@NonNull OsmandApplication app) {
		if (customizationListener == null) {
			customizationListener = () -> cachedFilteredValues = new ArrayList<>();
			app.getAppCustomization().addListener(customizationListener);
		}
		if (cachedFilteredValues.isEmpty()) {

			OsmandSettings settings = app.getSettings();
			if (listener == null) {
				listener = change -> cachedFilteredValues = new ArrayList<>();
				settings.AVAILABLE_APP_MODES.addListener(listener);
			}
			String available = settings.AVAILABLE_APP_MODES.get();
			cachedFilteredValues = new ArrayList<>();
			for (ApplicationMode v : values) {
				if (available.contains(v.getStringKey() + ",") || v == DEFAULT) {
					cachedFilteredValues.add(v);
				}
			}
		}
		return cachedFilteredValues;
	}

	@NonNull
	public static List<ApplicationMode> allPossibleValues() {
		return values;
	}

	@NonNull
	public static List<ApplicationMode> getDefaultValues() {
		return defaultValues;
	}

	@NonNull
	public static List<ApplicationMode> getCustomValues() {
		List<ApplicationMode> customModes = new ArrayList<>();
		for (ApplicationMode mode : values) {
			if (mode.isCustomProfile()) {
				customModes.add(mode);
			}
		}
		return customModes;
	}

	public static ApplicationMode valueOfStringKey(String key, ApplicationMode def) {
		for (ApplicationMode p : values) {
			if (p.getStringKey().equals(key)) {
				return p;
			}
		}
		return def;
	}

	@NonNull
	public static List<ApplicationMode> getModesDerivedFrom(ApplicationMode am) {
		List<ApplicationMode> list = new ArrayList<>();
		for (ApplicationMode a : values) {
			if (a == am || a.getParent() == am) {
				list.add(a);
			}
		}
		return list;
	}

	@NonNull
	public static List<ApplicationMode> getModesForRouting(@NonNull OsmandApplication app) {
		List<ApplicationMode> modes = new ArrayList<>(ApplicationMode.values(app));
		modes.remove(DEFAULT);
		return modes;
	}

	@NonNull
	public String getStringKey() {
		return stringKey;
	}

	public boolean isCustomProfile() {
		return isCustomProfile(getStringKey());
	}

	public static boolean isCustomProfile(@NonNull String key) {
		for (ApplicationMode mode : defaultValues) {
			if (Algorithms.stringsEqual(mode.getStringKey(), key)) {
				return false;
			}
		}
		return true;
	}

	public int getRouteTypeProfile() {
		if (isDerivedRoutingFrom(TRUCK)) {
			return RouteTypeRule.PROFILE_TRUCK;
		} else if (isDerivedRoutingFrom(CAR)) {
			return RouteTypeRule.PROFILE_CAR;
		}
		return RouteTypeRule.PROFILE_NONE;
	}

	public boolean isDerivedRoutingFrom(@NonNull ApplicationMode mode) {
		return this == mode || getParent() == mode;
	}

	@Nullable
	public ApplicationMode getParent() {
		return parentAppMode;
	}

	public void setParentAppMode(@NonNull ApplicationMode parentAppMode) {
		if (isCustomProfile()) {
			this.parentAppMode = parentAppMode;
			app.getSettings().PARENT_APP_MODE.setModeValue(this, parentAppMode.getStringKey());
		}
	}

	@StringRes
	public int getNameKeyResource() {
		return keyName;
	}

	@NonNull
	public String toHumanString() {
		String userProfileName = getUserProfileName();
		if (Algorithms.isEmpty(userProfileName)) {
			if (keyName != -1) {
				return app.getString(keyName);
			} else {
				return Algorithms.capitalizeFirstLetter(getStringKey());
			}
		} else {
			return userProfileName;
		}
	}

	@NonNull
	public String getDescription() {
		if (descriptionId != 0) {
			return app.getString(descriptionId);
		}
		return "";
	}

	@DrawableRes
	public int getIconRes() {
		return iconRes;
	}

	public void setIconResName(String iconResName) {
		if (!Algorithms.isEmpty(iconResName)) {
			app.getSettings().ICON_RES_NAME.setModeValue(this, iconResName);
		}
	}

	private void updateAppModeIcon() {
		try {
			String iconResName = app.getSettings().ICON_RES_NAME.getModeValue(this);
			int iconRes = app.getResources().getIdentifier(iconResName, "drawable", app.getPackageName());
			if (iconRes != 0) {
				this.iconRes = iconRes;
			}
		} catch (Exception e) {
		}
	}

	public String getIconName() {
		return app.getSettings().ICON_RES_NAME.getModeValue(this);
	}

	public int getMinDistanceForTurn() {
		// used to be: 50 kmh - 35 m, 10 kmh - 15 m, 4 kmh - 5 m, 10 kmh - 20 m, 400 kmh - 100 m,
		float speed = Math.max(getDefaultSpeed(), 0.3f);
		// 2 sec + 7 m: 50 kmh - 35 m, 10 kmh - 12 m, 4 kmh - 9 m, 400 kmh - 230 m
		return (int) (7 + speed * 2);
	}

	public boolean hasFastSpeed() {
		return getDefaultSpeed() > FAST_SPEED_THRESHOLD;
	}

	public float getDefaultSpeed() {
		return app.getSettings().DEFAULT_SPEED.getModeValue(this);
	}

	public void setDefaultSpeed(float defaultSpeed) {
		app.getSettings().DEFAULT_SPEED.setModeValue(this, defaultSpeed);
	}

	public void resetDefaultSpeed() {
		app.getSettings().DEFAULT_SPEED.resetModeToDefault(this);
	}

	public float getMinSpeed() {
		return app.getSettings().MIN_SPEED.getModeValue(this);
	}

	public void setMinSpeed(float defaultSpeed) {
		app.getSettings().MIN_SPEED.setModeValue(this, defaultSpeed);
	}

	public float getMaxSpeed() {
		return app.getSettings().MAX_SPEED.getModeValue(this);
	}

	public void setMaxSpeed(float defaultSpeed) {
		app.getSettings().MAX_SPEED.setModeValue(this, defaultSpeed);
	}

	public float getMaxSpeedToleranceLimit() {
		return Math.min(getDefaultSpeed(), MAX_VALUE_KM_H / 3.6f);
	}

	public float getMinSpeedToleranceLimit() {
		return Math.max(-getDefaultSpeed() / 2, MIN_VALUE_KM_H / 3.6f);
	}

	public boolean isSpeedToleranceBigRange() {
		return (getMaxSpeedToleranceLimit() - getMinSpeedToleranceLimit()) * 3.6 > 6;
	}

	public float getStrAngle() {
		return app.getSettings().ROUTE_STRAIGHT_ANGLE.getModeValue(this);
	}

	public void setStrAngle(float angle) {
		app.getSettings().ROUTE_STRAIGHT_ANGLE.setModeValue(this, angle);
	}

	public String getUserProfileName() {
		return app.getSettings().USER_PROFILE_NAME.getModeValue(this);
	}

	public void setUserProfileName(String userProfileName) {
		if (!Algorithms.isEmpty(userProfileName)) {
			app.getSettings().USER_PROFILE_NAME.setModeValue(this, userProfileName);
		}
	}

	public void setDerivedProfile(String derivedProfile) {
		if (Algorithms.isEmpty(derivedProfile)) {
			derivedProfile = "default";
		}
		app.getSettings().DERIVED_PROFILE.setModeValue(this, derivedProfile);
	}

	public String getDerivedProfile() {
		return app.getSettings().DERIVED_PROFILE.getModeValue(this);
	}

	public String getDefaultDerivedProfile() {
		return app.getSettings().DERIVED_PROFILE.getProfileDefaultValue(this);
	}

	public String getRoutingProfile() {
		return app.getSettings().ROUTING_PROFILE.getModeValue(this);
	}

	public String getDefaultRoutingProfile() {
		return app.getSettings().ROUTING_PROFILE.getProfileDefaultValue(this);
	}

	public void setRoutingProfile(String routingProfile) {
		if (!Algorithms.isEmpty(routingProfile)) {
			app.getSettings().ROUTING_PROFILE.setModeValue(this, routingProfile);
		}
	}

	public RouteService getRouteService() {
		return app.getSettings().ROUTE_SERVICE.getModeValue(this);
	}

	public void setRouteService(RouteService routeService) {
		if (routeService != null) {
			app.getSettings().ROUTE_SERVICE.setModeValue(this, routeService);
		}
	}

	@NonNull
	public String getNavigationIcon() {
		return app.getSettings().NAVIGATION_ICON.getModeValue(this);
	}

	public void setNavigationIcon(@Nullable String navigationIcon) {
		if (!Algorithms.isEmpty(navigationIcon)) {
			app.getSettings().NAVIGATION_ICON.setModeValue(this, navigationIcon);
		}
	}

	@NonNull
	public String getLocationIcon() {
		return app.getSettings().LOCATION_ICON.getModeValue(this);
	}

	@ColorInt
	public int getProfileColor(boolean nightMode) {
		Integer customProfileColor = getCustomIconColor();
		if (customProfileColor != null) {
			return customProfileColor;
		}
		return ContextCompat.getColor(app, getIconColorInfo().getColor(nightMode));
	}

	public void setLocationIcon(@Nullable String locationIcon) {
		if (!Algorithms.isEmpty(locationIcon)) {
			app.getSettings().LOCATION_ICON.setModeValue(this, locationIcon);
		}
	}

	public ProfileIconColors getIconColorInfo() {
		return app.getSettings().ICON_COLOR.getModeValue(this);
	}

	public void setIconColor(ProfileIconColors iconColor) {
		if (iconColor != null) {
			app.getSettings().ICON_COLOR.setModeValue(this, iconColor);
		}
	}

	public void setViewAngleVisibility(@NonNull MarkerDisplayOption viewAngle) {
		app.getSettings().VIEW_ANGLE_VISIBILITY.setModeValue(this, viewAngle);
	}

	@NonNull
	public MarkerDisplayOption getViewAngleVisibility() {
		return app.getSettings().VIEW_ANGLE_VISIBILITY.getModeValue(this);
	}

	public void setLocationRadius(@NonNull MarkerDisplayOption locationRadius) {
		app.getSettings().LOCATION_RADIUS_VISIBILITY.setModeValue(this, locationRadius);
	}

	@NonNull
	public MarkerDisplayOption getLocationRadiusVisibility() {
		return app.getSettings().LOCATION_RADIUS_VISIBILITY.getModeValue(this);
	}

	public Integer getCustomIconColor() {
		try {
			String customColor = app.getSettings().CUSTOM_ICON_COLOR.getModeValue(this);
			return Algorithms.isEmpty(customColor) ? null : Algorithms.parseColor(customColor);
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	public void updateCustomIconColor(@Nullable Integer color) {
		if (!setCustomIconColor(color)) {
			app.getSettings().CUSTOM_ICON_COLOR.resetModeToDefault(this);
		}
	}

	public boolean setCustomIconColor(@Nullable Integer color) {
		if (color != null) {
			String valueToSave = Algorithms.colorToString(color);
			app.getSettings().CUSTOM_ICON_COLOR.setModeValue(this, valueToSave);
			return true;
		}
		return false;
	}

	public int getOrder() {
		return app.getSettings().APP_MODE_ORDER.getModeValue(this);
	}

	public void setOrder(int order) {
		app.getSettings().APP_MODE_ORDER.setModeValue(this, order);
	}

	public int getVersion() {
		return app.getSettings().APP_MODE_VERSION.getModeValue(this);
	}

	public void setVersion(int version) {
		app.getSettings().APP_MODE_VERSION.setModeValue(this, version);
	}

	public static void onApplicationStart(OsmandApplication app) {
		initCustomModes(app);
		initModesParams(app);
		WidgetsAvailabilityHelper.initRegVisibility();
		reorderAppModes();
	}

	private static void initModesParams(@NonNull OsmandApplication app) {
		OsmandSettings settings = app.getSettings();
		if (iconNameListener == null) {
			iconNameListener = change -> {
				List<ApplicationMode> modes = new ArrayList<>(allPossibleValues());
				for (ApplicationMode mode : modes) {
					mode.updateAppModeIcon();
				}
			};
			settings.ICON_RES_NAME.addListener(iconNameListener);
		}
		for (ApplicationMode mode : allPossibleValues()) {
			mode.app = app;
			mode.updateAppModeIcon();
		}

		if (settings.APP_MODE_ORDER.isSetForMode(PUBLIC_TRANSPORT) && !settings.APP_MODE_ORDER.isSetForMode(TRAIN)) {
			TRAIN.setOrder(PUBLIC_TRANSPORT.getOrder() + 1);
		}
		if (settings.APP_MODE_ORDER.isSetForMode(PEDESTRIAN)) {
			if (!settings.APP_MODE_ORDER.isSetForMode(TRUCK)) {
				TRUCK.setOrder(PEDESTRIAN.getOrder() + 1);
			}
			if (!settings.APP_MODE_ORDER.isSetForMode(MOTORCYCLE)) {
				MOTORCYCLE.setOrder(PEDESTRIAN.getOrder() + 1);
			}
			if (!settings.APP_MODE_ORDER.isSetForMode(MOPED)) {
				MOPED.setOrder(PEDESTRIAN.getOrder() + 1);
			}
		}
		if (settings.APP_MODE_ORDER.isSetForMode(SKI) && !settings.APP_MODE_ORDER.isSetForMode(HORSE)) {
			HORSE.setOrder(SKI.getOrder() + 1);
		}
	}

	private static void initCustomModes(OsmandApplication app) {
		OsmandSettings settings = app.getSettings();
		for (String appModeKey : settings.getCustomAppModesKeys()) {
			Object profilePreferences = settings.getProfilePreferences(appModeKey);
			String parent = settings.PARENT_APP_MODE.getValue(profilePreferences, null);
			int order = settings.APP_MODE_ORDER.getValue(profilePreferences, values.size());

			ApplicationModeBuilder builder = createCustomMode(valueOfStringKey(parent, CAR), appModeKey, app);
			builder.setOrder(order);
			builder.customReg();
		}
	}

	public static void reorderAppModes() {
		Comparator<ApplicationMode> comparator = (mode1, mode2) -> Integer.compare(mode1.getOrder(), mode2.getOrder());
		Collections.sort(values, comparator);
		Collections.sort(defaultValues, comparator);
		Collections.sort(cachedFilteredValues, comparator);
		updateAppModesOrder();
	}

	private static void updateAppModesOrder() {
		for (int i = 0; i < values.size(); i++) {
			ApplicationMode mode = values.get(i);
			if (mode.getOrder() != i) {
				mode.setOrder(i);
			}
		}
	}

	private static void saveCustomAppModesToSettings(OsmandApplication app) {
		OsmandSettings settings = app.getSettings();
		StringBuilder stringBuilder = new StringBuilder();
		Iterator<ApplicationMode> it = getCustomValues().iterator();
		while (it.hasNext()) {
			stringBuilder.append(it.next().getStringKey());
			if (it.hasNext()) {
				stringBuilder.append(",");
			}
		}
		if (!stringBuilder.toString().equals(settings.CUSTOM_APP_MODES_KEYS.get())) {
			settings.CUSTOM_APP_MODES_KEYS.set(stringBuilder.toString());
		}
	}

	public static ApplicationMode saveProfile(ApplicationModeBuilder builder, OsmandApplication app) {
		ApplicationMode mode = valueOfStringKey(builder.applicationMode.stringKey, null);
		if (mode != null) {
			mode.setParentAppMode(builder.applicationMode.parentAppMode);
			mode.setIconResName(builder.iconResName);
			mode.setUserProfileName(builder.userProfileName);
			mode.setRoutingProfile(builder.routingProfile);
			mode.setRouteService(builder.routeService);
			mode.setIconColor(builder.iconColor);
			mode.setCustomIconColor(builder.customIconColor);
			mode.setLocationIcon(builder.locationIcon);
			mode.setNavigationIcon(builder.navigationIcon);
			mode.setOrder(builder.order);
			mode.setVersion(builder.version);
			mode.setViewAngleVisibility(builder.viewAngle);
			mode.setLocationRadius(builder.locationRadius);
		} else {
			mode = builder.customReg();
			WidgetsAvailabilityHelper.initRegVisibility();
		}
		reorderAppModes();
		saveCustomAppModesToSettings(app);
		return mode;
	}

	@NonNull
	public static ApplicationModeBean fromJson(@NonNull OsmandApplication app, @NonNull String json) {
		Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
		ApplicationModeBean modeBean = gson.fromJson(json, ApplicationModeBean.class);
		ApplicationModeBean.checkAndReplaceInvalidValues(app, modeBean);
		return modeBean;
	}

	@NonNull
	public static ApplicationModeBuilder fromModeBean(@NonNull OsmandApplication app, @NonNull ApplicationModeBean modeBean) {
		ApplicationModeBuilder builder = createCustomMode(valueOfStringKey(modeBean.parent, null), modeBean.stringKey, app);
		builder.setUserProfileName(modeBean.userProfileName);
		builder.setIconResName(modeBean.iconName);
		builder.setIconColor(modeBean.iconColor);
		builder.setCustomIconColor(modeBean.customIconColor);
		builder.setRoutingProfile(modeBean.routingProfile);
		builder.setRouteService(modeBean.routeService);
		builder.setLocationIcon(modeBean.locIcon);
		builder.setNavigationIcon(modeBean.navIcon);
		builder.setOrder(modeBean.order);
		builder.setVersion(modeBean.version);

		return builder;
	}

	public String toJson() {
		Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
		return gson.toJson(toModeBean());
	}

	public ApplicationModeBean toModeBean() {
		ApplicationModeBean mb = new ApplicationModeBean();
		mb.stringKey = stringKey;
		mb.userProfileName = getUserProfileName();
		mb.iconColor = getIconColorInfo();
		mb.customIconColor = getCustomIconColor();
		mb.iconName = getIconName();
		mb.parent = parentAppMode != null ? parentAppMode.getStringKey() : null;
		mb.routeService = getRouteService();
		mb.routingProfile = getRoutingProfile();
		mb.locIcon = getLocationIcon();
		mb.navIcon = getNavigationIcon();
		mb.order = getOrder();
		mb.version = getVersion();
		return mb;
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
		saveCustomAppModesToSettings(app);
	}

	public static boolean changeProfileAvailability(ApplicationMode mode, boolean isSelected, OsmandApplication app) {
		Set<ApplicationMode> selectedModes = new LinkedHashSet<>(values(app));
		StringBuilder vls = new StringBuilder(DEFAULT.getStringKey() + ",");
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

	public static ApplicationModeBuilder createCustomMode(ApplicationMode parent, String stringKey, OsmandApplication app) {
		ApplicationModeBuilder builder = create(parent, -1, stringKey);
		builder.getApplicationMode().app = app;
		return builder;
	}

	public static class ApplicationModeBuilder {

		private ApplicationMode applicationMode;

		private String userProfileName;
		private RouteService routeService;
		private String routingProfile;
		private String iconResName;
		private ProfileIconColors iconColor;
		private Integer customIconColor;
		private String locationIcon;
		private String navigationIcon;
		private MarkerDisplayOption viewAngle;
		private MarkerDisplayOption locationRadius;
		private int order = -1;
		private int version = -1;

		public ApplicationMode getApplicationMode() {
			return applicationMode;
		}

		private ApplicationMode reg() {
			values.add(applicationMode);
			defaultValues.add(applicationMode);
			return applicationMode;
		}

		private ApplicationMode customReg() {
			values.add(applicationMode);

			ApplicationMode parent = applicationMode.parentAppMode;
			applicationMode.setParentAppMode(parent);
			applicationMode.setUserProfileName(userProfileName);
			applicationMode.setRouteService(routeService);
			applicationMode.setRoutingProfile(routingProfile);
			applicationMode.setIconResName(iconResName);
			applicationMode.setCustomIconColor(customIconColor);
			applicationMode.setIconColor(iconColor);
			applicationMode.setLocationIcon(locationIcon);
			applicationMode.setNavigationIcon(navigationIcon);
			applicationMode.setOrder(order != -1 ? order : values.size());
			applicationMode.setVersion(version);
			applicationMode.setViewAngleVisibility(viewAngle);
			applicationMode.setLocationRadius(locationRadius);

			return applicationMode;
		}

		public ApplicationModeBuilder icon(int iconRes) {
			applicationMode.iconRes = iconRes;
			return this;
		}

		public ApplicationModeBuilder description(int strId) {
			applicationMode.descriptionId = strId;
			return this;
		}

		public ApplicationModeBuilder parent(ApplicationMode parent) {
			applicationMode.parentAppMode = parent;
			return this;
		}

		public ApplicationModeBuilder setUserProfileName(String userProfileName) {
			this.userProfileName = userProfileName;
			return this;
		}

		public ApplicationModeBuilder setRouteService(RouteService routeService) {
			this.routeService = routeService;
			return this;
		}

		public ApplicationModeBuilder setRoutingProfile(String routingProfile) {
			this.routingProfile = routingProfile;
			return this;
		}

		public ApplicationModeBuilder setIconResName(String iconResName) {
			this.iconResName = iconResName;
			return this;
		}

		public ApplicationModeBuilder setIconColor(ProfileIconColors iconColor) {
			this.iconColor = iconColor;
			return this;
		}

		public ApplicationModeBuilder setCustomIconColor(Integer customIconColor) {
			this.customIconColor = customIconColor;
			return this;
		}

		public ApplicationModeBuilder setOrder(int order) {
			this.order = order;
			return this;
		}

		public ApplicationModeBuilder setVersion(int version) {
			this.version = version;
			return this;
		}

		public ApplicationModeBuilder setLocationIcon(String locIcon) {
			this.locationIcon = locIcon;
			return this;
		}

		public ApplicationModeBuilder setNavigationIcon(String navIcon) {
			this.navigationIcon = navIcon;
			return this;
		}
		public ApplicationModeBuilder setViewAngle(@NonNull MarkerDisplayOption viewAngle) {
			this.viewAngle = viewAngle;
			return this;
		}
		public ApplicationModeBuilder setLocationRadius(@NonNull MarkerDisplayOption locationRadius) {
			this.locationRadius = locationRadius;
			return this;
		}
	}

	@NonNull
	@Override
	public String toString() {
		return getStringKey();
	}
}