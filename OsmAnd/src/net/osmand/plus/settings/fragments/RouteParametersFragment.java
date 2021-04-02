package net.osmand.plus.settings.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;
import androidx.preference.PreferenceViewHolder;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.slider.Slider;

import net.osmand.AndroidUtils;
import net.osmand.StateChangedListener;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.development.OsmandDevelopmentPlugin;
import net.osmand.plus.routing.RouteService;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.BooleanPreference;
import net.osmand.plus.settings.backend.CommonPreference;
import net.osmand.plus.settings.backend.OsmandPreference;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.bottomsheets.ElevationDateBottomSheet;
import net.osmand.plus.settings.bottomsheets.RecalculateRouteInDeviationBottomSheet;
import net.osmand.plus.settings.preferences.ListPreferenceEx;
import net.osmand.plus.settings.preferences.MultiSelectBooleanPreference;
import net.osmand.plus.settings.preferences.SwitchPreferenceEx;
import net.osmand.router.GeneralRouter;
import net.osmand.router.GeneralRouter.RoutingParameter;
import net.osmand.router.GeneralRouter.RoutingParameterType;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static net.osmand.plus.routepreparationmenu.RoutingOptionsHelper.DRIVING_STYLE;
import static net.osmand.plus.settings.backend.OsmandSettings.ROUTING_PREFERENCE_PREFIX;
import static net.osmand.router.GeneralRouter.USE_HEIGHT_OBSTACLES;

public class RouteParametersFragment extends BaseSettingsFragment implements OnPreferenceChanged {

	public static final String TAG = RouteParametersFragment.class.getSimpleName();

	private static final String AVOID_ROUTING_PARAMETER_PREFIX = "avoid_";
	private static final String PREFER_ROUTING_PARAMETER_PREFIX = "prefer_";
	private static final String ROUTE_PARAMETERS_INFO = "route_parameters_info";
	private static final String ROUTE_PARAMETERS_IMAGE = "route_parameters_image";
	public static final String RELIEF_SMOOTHNESS_FACTOR = "relief_smoothness_factor";
	private static final String ROUTING_SHORT_WAY = "prouting_short_way";
	private static final String ROUTING_RECALC_DISTANCE = "routing_recalc_distance";
	private static final String ROUTING_RECALC_WRONG_DIRECTION = "disable_wrong_direction_recalc";

	public static final float DISABLE_MODE = -1.0f;
	public static final float DEFAULT_MODE = 0.0f;

	private List<RoutingParameter> avoidParameters = new ArrayList<RoutingParameter>();
	private List<RoutingParameter> preferParameters = new ArrayList<RoutingParameter>();
	private List<RoutingParameter> drivingStyleParameters = new ArrayList<RoutingParameter>();
	private List<RoutingParameter> reliefFactorParameters = new ArrayList<RoutingParameter>();
	private List<RoutingParameter> otherRoutingParameters = new ArrayList<RoutingParameter>();

	private StateChangedListener<Boolean> booleanRoutingPrefListener;
	private StateChangedListener<String> customRoutingPrefListener;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		booleanRoutingPrefListener = new StateChangedListener<Boolean>() {
			@Override
			public void stateChanged(Boolean change) {
				recalculateRoute(app, getSelectedAppMode());
			}
		};
		customRoutingPrefListener = new StateChangedListener<String>() {
			@Override
			public void stateChanged(String change) {
				recalculateRoute(app, getSelectedAppMode());
			}
		};
	}

	@Override
	public RecyclerView onCreateRecyclerView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
		final RecyclerView view = super.onCreateRecyclerView(inflater, parent, savedInstanceState);
		//To prevent icons from flashing when the user turns switch on/off
		view.setItemAnimator(null);
		view.setLayoutAnimation(null);
		return view;
	}

	@Override
	protected void setupPreferences() {
		setupRouteParametersImage();

		Preference routeParametersInfo = findPreference(ROUTE_PARAMETERS_INFO);
		routeParametersInfo.setIcon(getContentIcon(R.drawable.ic_action_info_dark));
		routeParametersInfo.setTitle(getString(R.string.route_parameters_info, getSelectedAppMode().toHumanString()));

		setupRoutingPrefs();
	}

	@Override
	protected void onBindPreferenceViewHolder(Preference preference, PreferenceViewHolder holder) {
		super.onBindPreferenceViewHolder(preference, holder);

		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) {
			return;
		}
		String key = preference.getKey();
		if (ROUTE_PARAMETERS_INFO.equals(key)) {
			int colorRes = isNightMode() ? R.color.activity_background_color_dark : R.color.activity_background_color_light;
			holder.itemView.setBackgroundColor(ContextCompat.getColor(app, colorRes));
		} else if (ROUTE_PARAMETERS_IMAGE.equals(key)) {
			ImageView imageView = (ImageView) holder.itemView.findViewById(R.id.device_image);
			if (imageView != null) {
				int bgResId = isNightMode() ? R.drawable.img_settings_device_bottom_dark : R.drawable.img_settings_device_bottom_light;
				Drawable layerDrawable = app.getUIUtilities().getLayeredIcon(bgResId, R.drawable.img_settings_sreen_route_parameters);

				imageView.setImageDrawable(layerDrawable);
			}
		}
	}

	private void setupRouteParametersImage() {
		Preference routeParametersImage = findPreference(ROUTE_PARAMETERS_IMAGE);
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) {
			routeParametersImage.setVisible(false);
		}
	}

	private void setupTimeConditionalRoutingPref() {
		SwitchPreferenceEx timeConditionalRouting = createSwitchPreferenceEx(settings.ENABLE_TIME_CONDITIONAL_ROUTING.getId(),
				R.string.temporary_conditional_routing, R.layout.preference_with_descr_dialog_and_switch);
		timeConditionalRouting.setIcon(getRoutingPrefIcon(settings.ENABLE_TIME_CONDITIONAL_ROUTING.getId()));
		timeConditionalRouting.setSummaryOn(R.string.shared_string_on);
		timeConditionalRouting.setSummaryOff(R.string.shared_string_off);
		timeConditionalRouting.setDescription(R.string.temporary_conditional_routing_descr);
		getPreferenceScreen().addPreference(timeConditionalRouting);
	}

	private void setupOsmLiveForPublicTransportPref() {
		SwitchPreferenceEx useOsmLiveForPublicTransport = createSwitchPreferenceEx(settings.USE_OSM_LIVE_FOR_PUBLIC_TRANSPORT.getId(),
				R.string.use_live_public_transport, R.layout.preference_with_descr_dialog_and_switch);
		useOsmLiveForPublicTransport.setDescription(getString(R.string.use_osm_live_public_transport_description));
		useOsmLiveForPublicTransport.setSummaryOn(R.string.shared_string_enabled);
		useOsmLiveForPublicTransport.setSummaryOff(R.string.shared_string_disabled);
		useOsmLiveForPublicTransport.setIcon(getContentIcon(R.drawable.ic_action_osm_live));
		useOsmLiveForPublicTransport.setIconSpaceReserved(true);
		getPreferenceScreen().addPreference(useOsmLiveForPublicTransport);
	}

	private void setupNativePublicTransport() {
		SwitchPreferenceEx setupNativePublicTransport = createSwitchPreferenceEx(settings.PT_SAFE_MODE.getId(),
				R.string.use_native_pt, R.layout.preference_with_descr_dialog_and_switch);
		setupNativePublicTransport.setDescription(getString(R.string.use_native_pt_desc));
		setupNativePublicTransport.setSummaryOn(R.string.shared_string_enabled);
		setupNativePublicTransport.setSummaryOff(R.string.shared_string_disabled);
		setupNativePublicTransport.setIconSpaceReserved(true);
		getPreferenceScreen().addPreference(setupNativePublicTransport);
	}

	private void setupOsmLiveForRoutingPref() {
		SwitchPreferenceEx useOsmLiveForRouting = createSwitchPreferenceEx(settings.USE_OSM_LIVE_FOR_ROUTING.getId(),
				R.string.use_live_routing, R.layout.preference_with_descr_dialog_and_switch);
		useOsmLiveForRouting.setDescription(getString(R.string.use_osm_live_routing_description));
		useOsmLiveForRouting.setSummaryOn(R.string.shared_string_enabled);
		useOsmLiveForRouting.setSummaryOff(R.string.shared_string_disabled);
		useOsmLiveForRouting.setIcon(getContentIcon(R.drawable.ic_action_osm_live));
		useOsmLiveForRouting.setIconSpaceReserved(true);
		getPreferenceScreen().addPreference(useOsmLiveForRouting);
	}

	private void setupDisableComplexRoutingPref() {
		SwitchPreferenceEx disableComplexRouting = createSwitchPreferenceEx(settings.DISABLE_COMPLEX_ROUTING.getId(),
				R.string.use_two_phase_routing, R.layout.preference_with_descr_dialog_and_switch);
		disableComplexRouting.setDescription(getString(R.string.complex_routing_descr));
		disableComplexRouting.setSummaryOn(R.string.shared_string_enabled);
		disableComplexRouting.setSummaryOff(R.string.shared_string_disabled);
		disableComplexRouting.setIconSpaceReserved(true);
		getPreferenceScreen().addPreference(disableComplexRouting);
	}

	private void setupFastRecalculationPref() {
		SwitchPreferenceEx useFastRecalculation = createSwitchPreferenceEx(settings.USE_FAST_RECALCULATION.getId(),
				R.string.use_fast_recalculation, R.layout.preference_with_descr_dialog_and_switch);
		useFastRecalculation.setDescription(getString(R.string.use_fast_recalculation_desc));
		useFastRecalculation.setSummaryOn(R.string.shared_string_enabled);
		useFastRecalculation.setSummaryOff(R.string.shared_string_disabled);
		useFastRecalculation.setIcon(getContentIcon(R.drawable.ic_action_route_part));
		useFastRecalculation.setIconSpaceReserved(true);
		getPreferenceScreen().addPreference(useFastRecalculation);
	}

	private void setupRoutingPrefs() {
		OsmandApplication app = getMyApplication();
		if (app == null) {
			return;
		}
		PreferenceScreen screen = getPreferenceScreen();

		ApplicationMode am = getSelectedAppMode();

		SwitchPreferenceEx fastRoute = createSwitchPreferenceEx(app.getSettings().FAST_ROUTE_MODE.getId(), R.string.fast_route_mode, R.layout.preference_with_descr_dialog_and_switch);
		fastRoute.setIcon(getRoutingPrefIcon(app.getSettings().FAST_ROUTE_MODE.getId()));
		fastRoute.setDescription(getString(R.string.fast_route_mode_descr));
		fastRoute.setSummaryOn(R.string.shared_string_on);
		fastRoute.setSummaryOff(R.string.shared_string_off);

		if (am.getRouteService() == RouteService.OSMAND) {
			GeneralRouter router = app.getRouter(am);
			clearParameters();
			if (router != null) {
				Map<String, RoutingParameter> parameters = router.getParameters();
				if (!am.isDerivedRoutingFrom(ApplicationMode.CAR)) {
					screen.addPreference(fastRoute);
				}
				for (Map.Entry<String, RoutingParameter> e : parameters.entrySet()) {
					String param = e.getKey();
					RoutingParameter routingParameter = e.getValue();
					if (param.startsWith(AVOID_ROUTING_PARAMETER_PREFIX)) {
						avoidParameters.add(routingParameter);
					} else if (param.startsWith(PREFER_ROUTING_PARAMETER_PREFIX)) {
						preferParameters.add(routingParameter);
					} else if (RELIEF_SMOOTHNESS_FACTOR.equals(routingParameter.getGroup())) {
						reliefFactorParameters.add(routingParameter);
					} else if (DRIVING_STYLE.equals(routingParameter.getGroup())) {
						drivingStyleParameters.add(routingParameter);
					} else if ((!param.equals(GeneralRouter.USE_SHORTEST_WAY) || am.isDerivedRoutingFrom(ApplicationMode.CAR))
							&& !param.equals(GeneralRouter.VEHICLE_HEIGHT)
							&& !param.equals(GeneralRouter.VEHICLE_WEIGHT)
							&& !param.equals(GeneralRouter.VEHICLE_WIDTH)
							&& !param.equals(GeneralRouter.VEHICLE_LENGTH)) {
						otherRoutingParameters.add(routingParameter);
					}
				}
				if (drivingStyleParameters.size() > 0) {
					ListPreferenceEx drivingStyleRouting = createRoutingBooleanListPreference(DRIVING_STYLE, drivingStyleParameters);
					screen.addPreference(drivingStyleRouting);
				}
				if (avoidParameters.size() > 0) {
					String title;
					String description;
					if (am.isDerivedRoutingFrom(ApplicationMode.PUBLIC_TRANSPORT)) {
						title = getString(R.string.avoid_pt_types);
						description = getString(R.string.avoid_pt_types_descr);
					} else {
						title = getString(R.string.impassable_road);
						description = getString(R.string.avoid_in_routing_descr_);
					}
					MultiSelectBooleanPreference avoidRouting = createRoutingBooleanMultiSelectPref(AVOID_ROUTING_PARAMETER_PREFIX, title, description, avoidParameters);
					screen.addPreference(avoidRouting);
				}
				if (preferParameters.size() > 0) {
					String title = getString(R.string.prefer_in_routing_title);
					MultiSelectBooleanPreference preferRouting = createRoutingBooleanMultiSelectPref(PREFER_ROUTING_PARAMETER_PREFIX, title, "", preferParameters);
					screen.addPreference(preferRouting);
				}
				for (RoutingParameter p : otherRoutingParameters) {
					String title = AndroidUtils.getRoutingStringPropertyName(app, p.getId(), p.getName());
					String description = AndroidUtils.getRoutingStringPropertyDescription(app, p.getId(), p.getDescription());

					if (p.getType() == RoutingParameterType.BOOLEAN) {
						OsmandPreference pref = settings.getCustomRoutingBooleanProperty(p.getId(), p.getDefaultBoolean());

						SwitchPreferenceEx switchPreferenceEx = createSwitchPreferenceEx(pref.getId(), title, description, R.layout.preference_with_descr_dialog_and_switch);
						switchPreferenceEx.setDescription(description);
						switchPreferenceEx.setIcon(getRoutingPrefIcon(p.getId()));
						screen.addPreference(switchPreferenceEx);

						setupOtherBooleanParameterSummary(am, p, switchPreferenceEx);
					} else {
						Object[] vls = p.getPossibleValues();
						String[] svlss = new String[vls.length];
						int i = 0;
						for (Object o : vls) {
							svlss[i++] = o.toString();
						}
						OsmandPreference pref = settings.getCustomRoutingProperty(p.getId(), p.getType() == RoutingParameterType.NUMERIC ? "0.0" : "-");

						ListPreferenceEx listPreferenceEx = (ListPreferenceEx) createListPreferenceEx(pref.getId(), p.getPossibleValueDescriptions(), svlss, title, R.layout.preference_with_descr);
						listPreferenceEx.setDescription(description);
						listPreferenceEx.setIcon(getRoutingPrefIcon(p.getId()));
						screen.addPreference(listPreferenceEx);
					}
				}
			}
			setupTimeConditionalRoutingPref();
		} else if (am.getRouteService() == RouteService.BROUTER) {
			screen.addPreference(fastRoute);
			setupTimeConditionalRoutingPref();
		} else if (am.getRouteService() == RouteService.STRAIGHT) {
			Preference straightAngle = new Preference(app.getApplicationContext());
			straightAngle.setPersistent(false);
			straightAngle.setKey(settings.ROUTE_STRAIGHT_ANGLE.getId());
			straightAngle.setTitle(getString(R.string.recalc_angle_dialog_title));
			straightAngle.setSummary(String.format(getString(R.string.shared_string_angle_param),
					String.valueOf((int) am.getStrAngle())));
			straightAngle.setLayoutResource(R.layout.preference_with_descr);
			straightAngle.setIcon(getRoutingPrefIcon("routing_recalc_distance")); //TODO change for appropriate icon when available
			getPreferenceScreen().addPreference(straightAngle);
		}

		addDivider(screen);
		setupRouteRecalcHeader(screen);
		setupSelectRouteRecalcDistance(screen);
		setupReverseDirectionRecalculation(screen);

		if (OsmandPlugin.isPluginEnabled(OsmandDevelopmentPlugin.class)) {
			setupDevelopmentCategoryPreferences(screen, am);
		}
	}

	private void setupOtherBooleanParameterSummary(ApplicationMode am, RoutingParameter p, SwitchPreferenceEx switchPreferenceEx) {
		if (USE_HEIGHT_OBSTACLES.equals(p.getId()) && !Algorithms.isEmpty(reliefFactorParameters)) {
			String summaryOn = getString(R.string.shared_string_enabled);
			for (RoutingParameter parameter : reliefFactorParameters) {
				if (isRoutingParameterSelected(settings, am, parameter)) {
					summaryOn = getString(R.string.ltr_or_rtl_combine_via_comma, summaryOn, getRoutingParameterTitle(app, parameter));
				}
			}
			switchPreferenceEx.setSummaryOn(summaryOn);
			switchPreferenceEx.setSummaryOff(R.string.shared_string_disabled);
		} else {
			switchPreferenceEx.setSummaryOn(R.string.shared_string_on);
			switchPreferenceEx.setSummaryOff(R.string.shared_string_off);
		}
	}

	private void addDivider(PreferenceScreen screen) {
		Preference divider = new Preference(requireContext());
		divider.setLayoutResource(R.layout.simple_divider_item);
		screen.addPreference(divider);
	}

	private void setupReverseDirectionRecalculation(PreferenceScreen screen) {
		OsmandPreference<Boolean> preference = settings.DISABLE_WRONG_DIRECTION_RECALC;
		SwitchPreferenceEx switchPreference = createSwitchPreferenceEx(preference.getId(),
				R.string.in_case_of_reverse_direction,
				R.layout.preference_with_descr_dialog_and_switch);
		switchPreference.setIcon(getRoutingPrefIcon(preference.getId()));
		switchPreference.setSummaryOn(R.string.shared_string_enabled);
		switchPreference.setSummaryOff(R.string.shared_string_disabled);
		screen.addPreference(switchPreference);
	}

	private void setupRouteRecalcHeader(PreferenceScreen screen) {
		PreferenceCategory routingCategory = new PreferenceCategory(requireContext());
		routingCategory.setLayoutResource(R.layout.preference_category_with_descr);
		routingCategory.setTitle(R.string.recalculate_route);
		screen.addPreference(routingCategory);
	}

	private void setupDevelopmentCategoryPreferences(PreferenceScreen screen, ApplicationMode am) {
		addDivider(screen);
		setupDevelopmentCategoryHeader(screen);
		if (am.isDerivedRoutingFrom(ApplicationMode.PUBLIC_TRANSPORT)) {
			setupOsmLiveForPublicTransportPref();
			setupNativePublicTransport();
		}
		if (am.isDerivedRoutingFrom(ApplicationMode.CAR)) {
			setupOsmLiveForRoutingPref();
			setupDisableComplexRoutingPref();
		}
		setupFastRecalculationPref();
	}

	private void setupDevelopmentCategoryHeader(PreferenceScreen screen) {
		PreferenceCategory developmentCategory = new PreferenceCategory(requireContext());
		developmentCategory.setLayoutResource(R.layout.preference_category_with_descr);
		developmentCategory.setTitle(R.string.development);
		screen.addPreference(developmentCategory);
	}

	@Override
	public boolean onPreferenceClick(Preference preference) {
		if (preference.getKey().equals(settings.ROUTE_STRAIGHT_ANGLE.getId())) {
			showSeekbarSettingsDialog(getActivity(), getSelectedAppMode());
		}
		return super.onPreferenceClick(preference);
	}

	@Override
	public void onDisplayPreferenceDialog(Preference preference) {
		if (preference.getKey().equals(settings.ROUTE_RECALCULATION_DISTANCE.getId())) {
			FragmentManager fragmentManager = getFragmentManager();
			if (fragmentManager != null) {
				RecalculateRouteInDeviationBottomSheet.showInstance(getFragmentManager(), preference.getKey(), this, false, getSelectedAppMode());
			}
		} else if (!reliefFactorParameters.isEmpty() && preference.getKey().equals(ROUTING_PREFERENCE_PREFIX + USE_HEIGHT_OBSTACLES)) {
			FragmentManager fragmentManager = getFragmentManager();
			if (fragmentManager != null) {
				ApplicationMode appMode = getSelectedAppMode();
				ElevationDateBottomSheet.showInstance(fragmentManager, appMode, this, false);
			}
		} else {
			super.onDisplayPreferenceDialog(preference);
		}
	}

	private void showSeekbarSettingsDialog(Activity activity, final ApplicationMode mode) {
		if (activity == null || mode == null) {
			return;
		}
		final OsmandApplication app = (OsmandApplication) activity.getApplication();
		final float[] angleValue = new float[]{mode.getStrAngle()};
		boolean nightMode = !app.getSettings().isLightContentForMode(mode);
		Context themedContext = UiUtilities.getThemedContext(activity, nightMode);
		AlertDialog.Builder builder = new AlertDialog.Builder(themedContext);
		View sliderView = LayoutInflater.from(themedContext).inflate(
				R.layout.recalculation_angle_dialog, null, false);
		builder.setView(sliderView);
		builder.setPositiveButton(R.string.shared_string_ok, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				mode.setStrAngle(angleValue[0]);
				updateAllSettings();
				app.getRoutingHelper().onSettingsChanged(mode);
			}
		});
		builder.setNegativeButton(R.string.shared_string_cancel, null);

		int selectedModeColor = mode.getProfileColor(nightMode);
		setupAngleSlider(angleValue, sliderView, nightMode, selectedModeColor);
		builder.show();
	}

	private static void setupAngleSlider(final float[] angleValue,
										 View sliderView,
										 final boolean nightMode,
										 final int activeColor) {

		final Slider angleBar = sliderView.findViewById(R.id.angle_slider);
		final TextView angleTv = sliderView.findViewById(R.id.angle_text);

		angleTv.setText(String.valueOf(angleValue[0]));
		angleBar.setValue((int) angleValue[0]);
		angleBar.addOnChangeListener(new Slider.OnChangeListener() {
			@Override
			public void onValueChange(@NonNull Slider slider, float value, boolean fromUser) {
				angleValue[0] = value;
				angleTv.setText(String.valueOf(value));
			}
		});
		UiUtilities.setupSlider(angleBar, nightMode, activeColor, true);
	}

	private void setupSelectRouteRecalcDistance(PreferenceScreen screen) {
		final SwitchPreferenceEx switchPref = createSwitchPreferenceEx(ROUTING_RECALC_DISTANCE,
				R.string.route_recalculation_dist_title, R.layout.preference_with_descr_dialog_and_switch);
		switchPref.setIcon(getRoutingPrefIcon(ROUTING_RECALC_DISTANCE));
		screen.addPreference(switchPref);
		updateRouteRecalcDistancePref();
	}

	private void updateRouteRecalcDistancePref() {
		SwitchPreferenceEx switchPref = (SwitchPreferenceEx) findPreference(ROUTING_RECALC_DISTANCE);
		if (switchPref == null) {
			return;
		}
		ApplicationMode appMode = getSelectedAppMode();
		float allowedValue = settings.ROUTE_RECALCULATION_DISTANCE.getModeValue(appMode);
		boolean enabled = allowedValue != DISABLE_MODE;
		if (allowedValue <= 0) {
			allowedValue = RoutingHelper.getDefaultAllowedDeviation(settings, appMode);
		}
		String summary = String.format(getString(R.string.ltr_or_rtl_combine_via_bold_point),
				enabled ? getString(R.string.shared_string_enabled) : getString(R.string.shared_string_disabled),
				OsmAndFormatter.getFormattedDistance(allowedValue, app, false));
		switchPref.setSummary(summary);
		switchPref.setChecked(enabled);
	}

	@Override
	public void onResume() {
		super.onResume();
		addRoutingPrefListeners();
	}

	@Override
	public void onPause() {
		super.onPause();
		removeRoutingPrefListeners();
	}

	@Override
	public void onAppModeChanged(ApplicationMode appMode) {
		removeRoutingPrefListeners();
		super.onAppModeChanged(appMode);
		addRoutingPrefListeners();
	}

	private void addRoutingPrefListeners() {
		settings.FAST_ROUTE_MODE.addListener(booleanRoutingPrefListener);
		settings.ENABLE_TIME_CONDITIONAL_ROUTING.addListener(booleanRoutingPrefListener);

		for (RoutingParameter parameter : otherRoutingParameters) {
			if (parameter.getType() == RoutingParameterType.BOOLEAN) {
				CommonPreference<Boolean> pref = settings.getCustomRoutingBooleanProperty(parameter.getId(), parameter.getDefaultBoolean());
				pref.addListener(booleanRoutingPrefListener);
			} else {
				CommonPreference<String> pref = settings.getCustomRoutingProperty(parameter.getId(), parameter.getType() == RoutingParameterType.NUMERIC ? "0.0" : "-");
				pref.addListener(customRoutingPrefListener);
			}
		}
	}

	private void removeRoutingPrefListeners() {
		settings.FAST_ROUTE_MODE.removeListener(booleanRoutingPrefListener);
		settings.ENABLE_TIME_CONDITIONAL_ROUTING.removeListener(booleanRoutingPrefListener);

		for (RoutingParameter parameter : otherRoutingParameters) {
			if (parameter.getType() == RoutingParameterType.BOOLEAN) {
				CommonPreference<Boolean> pref = settings.getCustomRoutingBooleanProperty(parameter.getId(), parameter.getDefaultBoolean());
				pref.removeListener(booleanRoutingPrefListener);
			} else {
				CommonPreference<String> pref = settings.getCustomRoutingProperty(parameter.getId(), parameter.getType() == RoutingParameterType.NUMERIC ? "0.0" : "-");
				pref.removeListener(customRoutingPrefListener);
			}
		}
	}

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		if ((settings.DISABLE_COMPLEX_ROUTING.getId().equals(preference.getKey()) ||
				settings.DISABLE_WRONG_DIRECTION_RECALC.getId().equals(preference.getKey())) &&
				newValue instanceof Boolean) {
			return onConfirmPreferenceChange(preference.getKey(), !(Boolean) newValue, getApplyQueryType()); // pref ui was inverted
		}
		return onConfirmPreferenceChange(preference.getKey(), newValue, getApplyQueryType());
	}

	@Override
	public void onApplyPreferenceChange(String prefId, boolean applyToAllProfiles, Object newValue) {
		if ((RELIEF_SMOOTHNESS_FACTOR.equals(prefId) || DRIVING_STYLE.equals(prefId)) && newValue instanceof String) {
			List<RoutingParameter> routingParameters = DRIVING_STYLE.equals(prefId) ? drivingStyleParameters : reliefFactorParameters;
			updateSelectedParameters(app, getSelectedAppMode(), routingParameters, (String) newValue);
		} else if (ROUTING_SHORT_WAY.equals(prefId) && newValue instanceof Boolean) {
			applyPreference(ROUTING_SHORT_WAY, applyToAllProfiles, newValue);
			applyPreference(settings.FAST_ROUTE_MODE.getId(), applyToAllProfiles, !(Boolean) newValue);
		} else if (ROUTING_RECALC_DISTANCE.equals(prefId)) {
			boolean enabled = false;
			float valueToSave = DISABLE_MODE;
			if (newValue instanceof Boolean) {
				enabled = (boolean) newValue;
				valueToSave = enabled ? DEFAULT_MODE : DISABLE_MODE;
			} else if (newValue instanceof Float) {
				valueToSave = (float) newValue;
				enabled = valueToSave != DISABLE_MODE;
			}
			applyPreference(ROUTING_RECALC_DISTANCE, applyToAllProfiles, valueToSave);
			applyPreference(settings.DISABLE_OFFROUTE_RECALC.getId(), applyToAllProfiles, !enabled);
			updateRouteRecalcDistancePref();
		} else {
			super.onApplyPreferenceChange(prefId, applyToAllProfiles, newValue);
		}
	}

	@Override
	public void onPreferenceChanged(String prefId) {
		if (AVOID_ROUTING_PARAMETER_PREFIX.equals(prefId) || PREFER_ROUTING_PARAMETER_PREFIX.equals(prefId)) {
			recalculateRoute(app, getSelectedAppMode());
		}
	}

	private ListPreferenceEx createRoutingBooleanListPreference(String groupKey, List<RoutingParameter> routingParameters) {
		String defaultTitle = Algorithms.capitalizeFirstLetterAndLowercase(groupKey.replace('_', ' '));
		String title = AndroidUtils.getRoutingStringPropertyName(app, groupKey, defaultTitle);
		String description  = AndroidUtils.getRoutingStringPropertyDescription(app, groupKey, "");
		ApplicationMode am = getSelectedAppMode();

		Object[] entryValues = new Object[routingParameters.size()];
		String[] entries = new String[entryValues.length];

		String selectedParameterId = null;
		for (int i = 0; i < routingParameters.size(); i++) {
			RoutingParameter parameter = routingParameters.get(i);
			entryValues[i] = parameter.getId();
			entries[i] = getRoutingParameterTitle(app, parameter);
			if (isRoutingParameterSelected(settings, am, parameter)) {
				selectedParameterId = parameter.getId();
			}
		}

		ListPreferenceEx routingListPref = createListPreferenceEx(groupKey, entries, entryValues, title, R.layout.preference_with_descr);
		routingListPref.setPersistent(false);
		routingListPref.setValue(selectedParameterId);
		routingListPref.setIcon(getRoutingPrefIcon(groupKey));
		if (!Algorithms.isEmpty(defaultTitle)) {
			routingListPref.setDescription(description);
		}

		return routingListPref;
	}

	private MultiSelectBooleanPreference createRoutingBooleanMultiSelectPref(String groupKey, String title, String descr, List<RoutingParameter> routingParameters) {
		MultiSelectBooleanPreference multiSelectPref = new MultiSelectBooleanPreference(app);
		multiSelectPref.setKey(groupKey);
		multiSelectPref.setTitle(title);
		multiSelectPref.setSummary(descr);
		multiSelectPref.setDescription(descr);
		multiSelectPref.setLayoutResource(R.layout.preference_with_descr);
		multiSelectPref.setIcon(getRoutingPrefIcon(groupKey));
		multiSelectPref.setIconSpaceReserved(true);

		String[] entries = new String[routingParameters.size()];
		String[] prefsIds = new String[routingParameters.size()];
		Set<String> enabledPrefsIds = new HashSet<>();

		ApplicationMode selectedMode = getSelectedAppMode();
		for (int i = 0; i < routingParameters.size(); i++) {
			RoutingParameter p = routingParameters.get(i);
			BooleanPreference booleanRoutingPref = (BooleanPreference) settings.getCustomRoutingBooleanProperty(p.getId(), p.getDefaultBoolean());

			entries[i] = AndroidUtils.getRoutingStringPropertyName(app, p.getId(), p.getName());
			prefsIds[i] = booleanRoutingPref.getId();

			if (booleanRoutingPref.getModeValue(selectedMode)) {
				enabledPrefsIds.add(booleanRoutingPref.getId());
			}
		}

		multiSelectPref.setEntries(entries);
		multiSelectPref.setEntryValues(prefsIds);
		multiSelectPref.setValues(enabledPrefsIds);

		return multiSelectPref;
	}

	private static void recalculateRoute(@NonNull OsmandApplication app, ApplicationMode mode) {
		app.getRoutingHelper().onSettingsChanged(mode);
	}

	private void clearParameters() {
		avoidParameters.clear();
		preferParameters.clear();
		drivingStyleParameters.clear();
		reliefFactorParameters.clear();
		otherRoutingParameters.clear();
	}

	public static String getRoutingParameterTitle(Context context, RoutingParameter parameter) {
		return AndroidUtils.getRoutingStringPropertyName(context, parameter.getId(), parameter.getName());
	}

	public static boolean isRoutingParameterSelected(OsmandSettings settings, ApplicationMode mode, RoutingParameter parameter) {
		CommonPreference<Boolean> property = settings.getCustomRoutingBooleanProperty(parameter.getId(), parameter.getDefaultBoolean());
		if (mode != null) {
			return property.getModeValue(mode);
		} else {
			return property.get();
		}
	}

	public static void updateSelectedParameters(OsmandApplication app, ApplicationMode mode,
												List<RoutingParameter> parameters, String selectedParameterId) {
		for (RoutingParameter p : parameters) {
			String parameterId = p.getId();
			setRoutingParameterSelected(app.getSettings(), mode, parameterId, p.getDefaultBoolean(), parameterId.equals(selectedParameterId));
		}
		recalculateRoute(app, mode);
	}

	private static void setRoutingParameterSelected(OsmandSettings settings, ApplicationMode mode,
													String parameterId, boolean defaultBoolean, boolean isChecked) {
		CommonPreference<Boolean> property = settings.getCustomRoutingBooleanProperty(parameterId, defaultBoolean);
		if (mode != null) {
			property.setModeValue(mode, isChecked);
		} else {
			property.set(isChecked);
		}
	}

	private Drawable getRoutingPrefIcon(String prefId) {
		switch (prefId) {
			case GeneralRouter.ALLOW_PRIVATE:
				return getPersistentPrefIcon(R.drawable.ic_action_private_access);
			case GeneralRouter.USE_SHORTEST_WAY:
				return getPersistentPrefIcon(R.drawable.ic_action_fuel);
			case GeneralRouter.ALLOW_MOTORWAYS:
				Drawable disabled = getContentIcon(R.drawable.ic_action_avoid_motorways);
				Drawable enabled = getActiveIcon(R.drawable.ic_action_motorways);
				return getPersistentPrefIcon(enabled, disabled);
			case USE_HEIGHT_OBSTACLES:
			case RELIEF_SMOOTHNESS_FACTOR:
				return getPersistentPrefIcon(R.drawable.ic_action_altitude_average);
			case AVOID_ROUTING_PARAMETER_PREFIX:
				return getPersistentPrefIcon(R.drawable.ic_action_alert);
			case DRIVING_STYLE:
				return getPersistentPrefIcon(R.drawable.ic_action_bicycle_dark);
			case "fast_route_mode":
				return getPersistentPrefIcon(R.drawable.ic_action_fastest_route);
			case "enable_time_conditional_routing":
				return getPersistentPrefIcon(R.drawable.ic_action_road_works_dark);
			case ROUTING_RECALC_DISTANCE:
				return getPersistentPrefIcon(R.drawable.ic_action_minimal_distance);
			case ROUTING_RECALC_WRONG_DIRECTION:
				return getPersistentPrefIcon(R.drawable.ic_action_reverse_direction);
			default:
				return null;
		}
	}
}