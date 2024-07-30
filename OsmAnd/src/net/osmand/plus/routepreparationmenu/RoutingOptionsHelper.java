package net.osmand.plus.routepreparationmenu;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.*;
import static net.osmand.plus.settings.fragments.RouteParametersFragment.AVOID_ROUTING_PARAMETER_PREFIX;
import static net.osmand.router.GeneralRouter.*;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatCheckedTextView;

import net.osmand.CallbackWithObject;
import net.osmand.Collator;
import net.osmand.IndexConstants;
import net.osmand.Location;
import net.osmand.OsmAndCollator;
import net.osmand.data.LatLon;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.download.DownloadActivity;
import net.osmand.plus.download.DownloadActivityType;
import net.osmand.plus.helpers.FileNameTranslationHelper;
import net.osmand.plus.helpers.TargetPointsHelper;
import net.osmand.plus.helpers.TargetPointsHelper.TargetPoint;
import net.osmand.plus.routing.GPXRouteParams.GPXRouteParamsBuilder;
import net.osmand.plus.routing.RouteService;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.routing.RoutingHelperUtils;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.settings.backend.preferences.OsmandPreference;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.voice.JsMediaCommandPlayer;
import net.osmand.plus.voice.JsTtsCommandPlayer;
import net.osmand.plus.widgets.alert.AlertDialogData;
import net.osmand.plus.widgets.alert.CustomAlert;
import net.osmand.plus.widgets.ctxmenu.ContextMenuAdapter;
import net.osmand.plus.widgets.ctxmenu.ContextMenuUtils;
import net.osmand.plus.widgets.ctxmenu.data.ContextMenuItem;
import net.osmand.router.GeneralRouter;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import java.io.File;
import java.util.*;


public class RoutingOptionsHelper {

	public static final String MORE_VALUE = "MORE_VALUE";
	public static final String DRIVING_STYLE = "driving_style";

	private final OsmandApplication app;

	private final Map<ApplicationMode, RouteMenuAppModes> modes = new HashMap<>();

	public RoutingOptionsHelper(OsmandApplication application) {
		app = application;
	}

	private void addRouteMenuAppModes(ApplicationMode am, List<String> routingParameters) {
		modes.put(am, new RouteMenuAppModes(am, getRoutingParameters(am, routingParameters)));
	}

	public void addNewRouteMenuParameter(ApplicationMode applicationMode, LocalRoutingParameter parameter) {
		RouteMenuAppModes mode = modes.get(applicationMode);
		if (mode != null) {
			if (parameter.canAddToRouteMenu() && mode.am.equals(applicationMode) && !mode.containsParameter(parameter)) {
				mode.parameters.add(parameter);
			}
		} else if (parameter.canAddToRouteMenu()) {
			List<LocalRoutingParameter> list = new ArrayList<>();
			list.add(parameter);
			modes.put(applicationMode, new RouteMenuAppModes(applicationMode, list));
		}
	}

	@Nullable
	public RouteMenuAppModes getRouteMenuAppMode(ApplicationMode appMode) {
		if (isFollowGpxTrack()) {
			return new RouteMenuAppModes(appMode, getRoutingParameters(appMode, PermanentAppModeOptions.OTHER.routingParameters));
		}
		if (!modes.containsKey(appMode)) {
			addRouteMenuAppModes(appMode, getRoutingParametersForProfileType(appMode));
		}
		return modes.get(appMode);
	}

	public void switchSound() {
		RoutingHelper routingHelper = app.getRoutingHelper();
		ApplicationMode mode = routingHelper.getAppMode();
		boolean mute = !routingHelper.getVoiceRouter().isMuteForMode(mode);
		routingHelper.getVoiceRouter().setMuteForMode(mode, mute);
	}

	public void selectVoiceGuidance(MapActivity mapActivity, CallbackWithObject<String> callback, ApplicationMode applicationMode) {
		ContextMenuAdapter contextMenuAdapter = new ContextMenuAdapter(app);

		String[] entries;
		String[] entrieValues;
		Set<String> voiceFiles = getVoiceFiles(mapActivity);
		entries = new String[voiceFiles.size() + 2];
		entrieValues = new String[voiceFiles.size() + 2];
		int k = 0;
		int selected = -1;
		String selectedValue = mapActivity.getMyApplication().getSettings().VOICE_PROVIDER.getModeValue(applicationMode);
		entrieValues[k] = OsmandSettings.VOICE_PROVIDER_NOT_USE;
		entries[k] = mapActivity.getResources().getString(R.string.shared_string_do_not_use);
		contextMenuAdapter.addItem(new ContextMenuItem(null).setTitle(entries[k]));
		if (OsmandSettings.VOICE_PROVIDER_NOT_USE.equals(selectedValue)) {
			selected = k;
		}
		k++;
		for (String s : voiceFiles) {
			entries[k] = (s.contains("tts") ? mapActivity.getResources().getString(R.string.ttsvoice) + " " : "") +
					FileNameTranslationHelper.getVoiceName(mapActivity, s);
			entrieValues[k] = s;
			contextMenuAdapter.addItem(new ContextMenuItem(null).setTitle(entries[k]));
			if (s.equals(selectedValue)) {
				selected = k;
			}
			k++;
		}
		entrieValues[k] = MORE_VALUE;
		entries[k] = mapActivity.getResources().getString(R.string.install_more);
		contextMenuAdapter.addItem(new ContextMenuItem(null).setTitle(entries[k]));

		boolean nightMode = isNightMode();
		AlertDialogData dialogData = new AlertDialogData(mapActivity, nightMode)
				.setControlsColor(ColorUtilities.getAppModeColor(app, nightMode));

		CustomAlert.showSingleSelection(dialogData, entries, selected, v -> {
			int which = (int) v.getTag();
			String value = entrieValues[which];
			if (MORE_VALUE.equals(value)) {
				Intent intent = new Intent(mapActivity, DownloadActivity.class);
				intent.putExtra(DownloadActivity.TAB_TO_OPEN, DownloadActivity.DOWNLOAD_TAB);
				intent.putExtra(DownloadActivity.FILTER_CAT, DownloadActivityType.VOICE_FILE.getTag());
				mapActivity.startActivity(intent);
			} else {
				if (callback != null) {
					callback.processResult(value);
				}
			}
		});
	}

	public String getVoiceProviderName(Context ctx, String value) {
		if (OsmandSettings.VOICE_PROVIDER_NOT_USE.equals(value)) {
			return ctx.getResources().getString(R.string.shared_string_do_not_use);
		} else {
			return (value.contains("tts") ? ctx.getResources().getString(R.string.ttsvoice) + " " : "") +
					FileNameTranslationHelper.getVoiceName(ctx, value);
		}
	}

	public void applyVoiceProvider(MapActivity mapActivity, String provider, boolean applyAllModes) {
		OsmandApplication app = mapActivity.getMyApplication();
		ApplicationMode selectedAppMode = app.getRoutingHelper().getAppMode();
		OsmandPreference<String> VP = app.getSettings().VOICE_PROVIDER;
		if (applyAllModes) {
			for (ApplicationMode mode : ApplicationMode.allPossibleValues()) {
				VP.setModeValue(mode, provider);
			}
		}
		VP.setModeValue(selectedAppMode, provider);
		app.initVoiceCommandPlayer(mapActivity, selectedAppMode, null,
				false, true, false, applyAllModes);
	}

	public Set<String> getVoiceFiles(Activity activity) {
		// read available voice data
		OsmandApplication app = ((OsmandApplication) activity.getApplication());
		File extStorage = app.getAppPath(IndexConstants.VOICE_INDEX_DIR);
		Set<String> setFiles = new LinkedHashSet<>();
		if (extStorage.exists()) {
			File[] voiceDirs = extStorage.listFiles();
			if (voiceDirs != null) {
				for (File f : voiceDirs) {
					if (f.isDirectory()) {
						if (JsMediaCommandPlayer.isMyData(f) || JsTtsCommandPlayer.isMyData(f)) {
							setFiles.add(f.getName());
						}
					}
				}
			}
		}
		return setFiles;
	}

	public void applyRoutingParameter(LocalRoutingParameter rp, boolean isChecked) {
		OsmandSettings settings = app.getSettings();
		RoutingHelper routingHelper = app.getRoutingHelper();
		// if short way that it should set valut to fast mode opposite of current
		if (rp.routingParameter != null && rp.routingParameter.getId().equals(USE_SHORTEST_WAY)) {
			settings.FAST_ROUTE_MODE.setModeValue(routingHelper.getAppMode(), !isChecked);
		}
		rp.setSelected(settings, isChecked);

		if (rp instanceof OtherLocalRoutingParameter) {
			updateGpxRoutingParameter((OtherLocalRoutingParameter) rp);
		}
		routingHelper.onSettingsChanged(rp.getApplicationMode(), true);
	}

	public void updateGpxRoutingParameter(OtherLocalRoutingParameter gpxParam) {
		GPXRouteParamsBuilder rp = app.getRoutingHelper().getCurrentGPXRoute();
		OsmandSettings settings = app.getSettings();
		boolean selected = gpxParam.isSelected(settings);
		if (rp != null) {
			if (gpxParam.id == R.string.gpx_option_reverse_route) {
				rp.setReverse(selected);
				TargetPointsHelper tg = app.getTargetPointsHelper();
				List<Location> ps = rp.getPoints(app);
				if (ps.size() > 0) {
					Location firstLoc = ps.get(0);
					Location lastLoc = ps.get(ps.size() - 1);
					TargetPoint pointToStart = tg.getPointToStart();
					TargetPoint pointToNavigate = tg.getPointToNavigate();
					if (rp.getFile().hasRoute()) {
						LatLon firstLatLon = new LatLon(firstLoc.getLatitude(), firstLoc.getLongitude());
						LatLon endLocation = pointToStart != null ? pointToStart.point : new LatLon(lastLoc.getLatitude(), lastLoc.getLongitude());
						LatLon startLocation = pointToNavigate != null ? pointToNavigate.point : firstLatLon;
						tg.navigateToPoint(endLocation, false, -1);
						if (pointToStart != null) {
							tg.setStartPoint(startLocation, false, null);
						}
						tg.updateRouteAndRefresh(true);
					} else {
						boolean update = false;
						if (pointToNavigate == null
								|| MapUtils.getDistance(pointToNavigate.point, new LatLon(firstLoc.getLatitude(), firstLoc.getLongitude())) < 10) {
							tg.navigateToPoint(new LatLon(lastLoc.getLatitude(), lastLoc.getLongitude()), false, -1);
							update = true;
						}
						if (pointToStart != null
								&& MapUtils.getDistance(pointToStart.point,
								new LatLon(lastLoc.getLatitude(), lastLoc.getLongitude())) < 10) {
							tg.setStartPoint(new LatLon(firstLoc.getLatitude(), firstLoc.getLongitude()), false, null);
							update = true;
						}
						if (update) {
							tg.updateRouteAndRefresh(true);
						}
					}
				}
			} else if (gpxParam.id == R.string.gpx_option_calculate_first_last_segment) {
				rp.setCalculateOsmAndRouteParts(selected);
				settings.GPX_ROUTE_CALC_OSMAND_PARTS.set(selected);
			} else if (gpxParam.id == R.string.gpx_option_from_start_point) {
				rp.setPassWholeRoute(selected);
			} else if (gpxParam.id == R.string.calculate_osmand_route_gpx) {
				settings.GPX_ROUTE_CALC.set(selected);
				rp.setCalculateOsmAndRoute(selected);
			} else if (gpxParam.id == R.string.connect_track_points_as) {
				rp.setConnectPointStraightly(selected);
			}
		}
		if (gpxParam.id == R.string.calculate_osmand_route_without_internet) {
			settings.GPX_ROUTE_CALC_OSMAND_PARTS.set(selected);
		}
		if (gpxParam.id == R.string.fast_route_mode) {
			settings.FAST_ROUTE_MODE.set(selected);
		}
		if (gpxParam.id == R.string.speak_favorites) {
			settings.ANNOUNCE_NEARBY_FAVORITES.set(selected);
		}
	}

	public List<LocalRoutingParameter> getRoutingParameters(ApplicationMode am, List<String> routingParameters) {
		List<LocalRoutingParameter> list = new ArrayList<>();
		getAppModeItems(am, list, routingParameters);
		return list;
	}

	public List<LocalRoutingParameter> getAppModeItems(ApplicationMode am, List<LocalRoutingParameter> list, List<String> routingParameters) {
		for (String itemId : routingParameters) {
			LocalRoutingParameter item = getItem(am, itemId);
			if (item != null) {
				updateRoutingParameterIcons(item);
				list.add(item);
				if (item instanceof TimeConditionalRoutingItem) {
					list.addAll(getOsmandRouterParameters(am));
				}
			}
		}
		return list;
	}

	public interface OnClickListener {
		void onClick();
	}

	public void showLocalRoutingParameterGroupDialog(LocalRoutingParameterGroup group, MapActivity mapActivity, OnClickListener listener) {
		OsmandSettings settings = app.getSettings();
		ContextMenuAdapter adapter = new ContextMenuAdapter(app);
		int i = 0;
		int selectedIndex = -1;
		for (LocalRoutingParameter p : group.getRoutingParameters()) {
			adapter.addItem(new ContextMenuItem(null)
					.setTitle(p.getText(mapActivity))
					.setSelected(false));
			if (p.isSelected(settings)) {
				selectedIndex = i;
			}
			i++;
		}
		if (selectedIndex == -1) {
			selectedIndex = 0;
		}

		boolean nightMode = isNightMode();
		Context themedContext = UiUtilities.getThemedContext(mapActivity, nightMode);
		ApplicationMode selectedAppMode = app.getRoutingHelper().getAppMode();
		int selectedModeColor = selectedAppMode.getProfileColor(nightMode);
		AlertDialog.Builder builder = new AlertDialog.Builder(themedContext);
		final int layout = R.layout.list_menu_item_native_singlechoice;

		List<String> names = ContextMenuUtils.getNames(adapter.getItems());
		ArrayAdapter<String> listAdapter = new ArrayAdapter<String>(themedContext, layout, R.id.text1, names) {
			@NonNull
			@Override
			public View getView(int position, View convertView, @NonNull ViewGroup parent) {
				// User super class to create the View
				View v = convertView;
				if (v == null) {
					v = UiUtilities.getInflater(mapActivity, nightMode).inflate(layout, parent, false);
				}
				ContextMenuItem item = adapter.getItem(position);
				AppCompatCheckedTextView tv = v.findViewById(R.id.text1);
				tv.setText(item.getTitle());
				tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f);
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
					UiUtilities.setupCompoundButtonDrawable(app, nightMode, selectedModeColor, tv.getCheckMarkDrawable());
				}

				return v;
			}
		};

		int[] selectedPosition = {selectedIndex};
		builder.setSingleChoiceItems(listAdapter, selectedIndex, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int position) {
				selectedPosition[0] = position;
			}
		});
		builder.setTitle(group.getText(mapActivity))
				.setPositiveButton(R.string.shared_string_ok, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						OsmandSettings settings = app.getSettings();
						int position = selectedPosition[0];
						if (position >= 0 && position < group.getRoutingParameters().size()) {
							for (int i = 0; i < group.getRoutingParameters().size(); i++) {
								LocalRoutingParameter rp = group.getRoutingParameters().get(i);
								rp.setSelected(settings, i == position);
							}
							mapActivity.getRoutingHelper().onSettingsChanged(true);
							if (listener != null) {
								listener.onClick();
							}
						}
					}
				})
				.setNegativeButton(R.string.shared_string_cancel, null);
		builder.create().show();
	}

	public LocalRoutingParameter getItem(ApplicationMode am, String parameter) {
		switch (parameter) {
			case MuteSoundRoutingParameter.KEY:
				return new MuteSoundRoutingParameter();
			case DividerItem.KEY:
				return new DividerItem();
			case RouteSimulationItem.KEY:
				return new RouteSimulationItem();
			case CalculateAltitudeItem.KEY:
				return new CalculateAltitudeItem();
			case ShowAlongTheRouteItem.KEY:
				return new ShowAlongTheRouteItem();
			case AvoidPTTypesRoutingParameter.KEY:
				return new AvoidPTTypesRoutingParameter();
			case AvoidRoadsRoutingParameter.KEY:
				return new AvoidRoadsRoutingParameter();
			case GpxLocalRoutingParameter.KEY:
				return new GpxLocalRoutingParameter();
			case TimeConditionalRoutingItem.KEY:
				return new TimeConditionalRoutingItem();
			case OtherSettingsRoutingParameter.KEY:
				return new OtherSettingsRoutingParameter();
			case CustomizeRouteLineRoutingParameter.KEY:
				return new CustomizeRouteLineRoutingParameter();
			default:
				return getRoutingParameterInnerById(am, parameter);
		}
	}

	public LocalRoutingParameter getRoutingParameterInnerById(ApplicationMode am, String parameterId) {
		GeneralRouter rm = app.getRouter(am);
		if (rm == null || (isFollowNonApproximatedGpxTrack())) {
			return null;
		}

		LocalRoutingParameter rp;
		Map<String, RoutingParameter> parameters = RoutingHelperUtils.getParametersForDerivedProfile(am, rm);
		RoutingParameter routingParameter = parameters.get(parameterId);

		if (routingParameter != null) {
			rp = new LocalRoutingParameter(am);
			rp.routingParameter = routingParameter;
		} else {
			LocalRoutingParameterGroup rpg = null;
			for (RoutingParameter r : parameters.values()) {
				if (r.getType() == RoutingParameterType.BOOLEAN
						&& !Algorithms.isEmpty(r.getGroup()) && r.getGroup().equals(parameterId)) {
					if (rpg == null) {
						rpg = new LocalRoutingParameterGroup(am, r.getGroup());
					}
					rpg.addRoutingParameter(r);
				}
			}
			return rpg;
		}
		return rp;
	}

	public List<LocalRoutingParameter> getAllRoutingParameters(ApplicationMode am) {
		List<LocalRoutingParameter> list = getRoutingParametersInner(am);
		list.add(0, new MuteSoundRoutingParameter());
		list.add(1, new VoiceGuidanceRoutingParameter());
		list.add(2, new InterruptMusicRoutingParameter());
		list.add(3, new AvoidRoadsRoutingParameter());
		list.add(4, new TimeConditionalRoutingItem());
		list.add(new GpxLocalRoutingParameter());
		list.add(new OtherSettingsRoutingParameter());
		return list;
	}

	public List<LocalRoutingParameter> getOsmandRouterParameters(ApplicationMode am) {
		OsmandSettings settings = app.getSettings();
		List<LocalRoutingParameter> list = new ArrayList<LocalRoutingParameter>();
		boolean osmandRouter = am.getRouteService() == RouteService.OSMAND;
		if (!osmandRouter) {
			list.add(new OtherLocalRoutingParameter(R.string.calculate_osmand_route_without_internet,
					app.getString(R.string.calculate_osmand_route_without_internet), settings.GPX_ROUTE_CALC_OSMAND_PARTS.get()));
			list.add(new OtherLocalRoutingParameter(R.string.fast_route_mode, app.getString(R.string.fast_route_mode),
					settings.FAST_ROUTE_MODE.get()));
		}
		return list;
	}

	public List<LocalRoutingParameter> getGpxRouterParameters(ApplicationMode am) {
		RoutingHelper routingHelper = app.getRoutingHelper();
		List<LocalRoutingParameter> list = new ArrayList<LocalRoutingParameter>();
		GPXRouteParamsBuilder rparams = routingHelper.getCurrentGPXRoute();
		boolean osmandRouter = am.getRouteService() == RouteService.OSMAND;
		if (rparams != null && osmandRouter) {
			if (!routingHelper.isCurrentGPXRouteV2()) {
				list.add(new OtherLocalRoutingParameter(R.string.gpx_option_reverse_route,
						app.getString(R.string.gpx_option_reverse_route), rparams.isReverse()));
			}
			if (!rparams.shouldUseIntermediateRtePoints()) {
				list.add(new OtherLocalRoutingParameter(R.string.gpx_option_from_start_point,
						app.getString(R.string.gpx_option_from_start_point), rparams.isPassWholeRoute()));
				list.add(new OtherLocalRoutingParameter(R.string.gpx_option_calculate_first_last_segment,
						app.getString(R.string.gpx_option_calculate_first_last_segment), rparams
						.isCalculateOsmAndRouteParts()));
			}
		}
		return list;
	}

	public List<LocalRoutingParameter> getRoutingParametersInner(ApplicationMode am) {
		boolean osmandRouter = am.getRouteService() == RouteService.OSMAND;
		if (!osmandRouter) {
			return getOsmandRouterParameters(am);
		}

		List<LocalRoutingParameter> list = new ArrayList<LocalRoutingParameter>(getGpxRouterParameters(am));
		GeneralRouter rm = app.getRouter(am);
		if (rm == null || isFollowNonApproximatedGpxTrack()) {
			return list;
		}
		Map<String, RoutingParameter> parameters = RoutingHelperUtils.getParametersForDerivedProfile(am, rm);
		for (RoutingParameter r : parameters.values()) {
			if (r.getType() == RoutingParameterType.BOOLEAN) {
				if ("relief_smoothness_factor".equals(r.getGroup())) {
					continue;
				}
				if (!Algorithms.isEmpty(r.getGroup())) {
					LocalRoutingParameterGroup rpg = getLocalRoutingParameterGroup(list, r.getGroup());
					if (rpg == null) {
						rpg = new LocalRoutingParameterGroup(am, r.getGroup());
						list.add(rpg);
					}
					rpg.addRoutingParameter(r);
				} else {
					LocalRoutingParameter rp = new LocalRoutingParameter(am);
					rp.routingParameter = r;
					updateRoutingParameterIcons(rp);
					list.add(rp);
				}
			}
		}

		return list;
	}

	private boolean isFollowNonApproximatedGpxTrack() {
		GPXRouteParamsBuilder rparams = app.getRoutingHelper().getCurrentGPXRoute();
		return rparams != null && !rparams.isCalculateOsmAndRoute() && !rparams.getFile().hasRtePt();
	}

	private boolean isFollowGpxTrack() {
		GPXRouteParamsBuilder rparams = app.getRoutingHelper().getCurrentGPXRoute();
		return rparams != null && !rparams.isCalculateOsmAndRoute();
	}

	private static void updateRoutingParameterIcons(LocalRoutingParameter rp) {
		if (rp instanceof LocalRoutingParameterGroup) {
			LocalRoutingParameterGroup group = (LocalRoutingParameterGroup) rp;
			if (group.groupName.equals(DRIVING_STYLE)) {
				rp.activeIconId = R.drawable.ic_action_bicycle_dark;
				rp.disabledIconId = R.drawable.ic_action_bicycle_dark;
			}
		}
		if (rp.routingParameter == null) {
			return;
		}
		switch (rp.routingParameter.getId()) {
			case USE_SHORTEST_WAY, AVOID_UNPAVED, AVOID_TOLL, AVOID_FERRIES -> {
				rp.activeIconId = R.drawable.ic_action_fuel;
				rp.disabledIconId = R.drawable.ic_action_fuel;
			}
			case USE_HEIGHT_OBSTACLES -> {
				rp.activeIconId = R.drawable.ic_action_altitude_average;
				rp.disabledIconId = R.drawable.ic_action_altitude_average;
			}
			case AVOID_MOTORWAY, ALLOW_MOTORWAYS, PREFER_MOTORWAYS -> {
				rp.activeIconId = R.drawable.ic_action_motorways;
				rp.disabledIconId = R.drawable.ic_action_avoid_motorways;
			}
			case ALLOW_PRIVATE, ALLOW_PRIVATE_FOR_TRUCK -> {
				rp.activeIconId = R.drawable.ic_action_allow_private_access;
				rp.disabledIconId = R.drawable.ic_action_forbid_private_access;
			}
		}
	}

	public static LocalRoutingParameterGroup getLocalRoutingParameterGroup(List<LocalRoutingParameter> list, String groupName) {
		for (LocalRoutingParameter p : list) {
			if (p instanceof LocalRoutingParameterGroup && groupName.equals(((LocalRoutingParameterGroup) p).getGroupName())) {
				return (LocalRoutingParameterGroup) p;
			}
		}
		return null;
	}

	@NonNull
	public List<RoutingParameter> getAvoidParameters(@NonNull ApplicationMode mode) {
		List<RoutingParameter> list = new ArrayList<>();
		GeneralRouter router = app.getRouter(mode);
		if (router != null) {
			Map<String, RoutingParameter> parameters = RoutingHelperUtils.getParametersForDerivedProfile(mode, router);
			for (Map.Entry<String, RoutingParameter> entry : parameters.entrySet()) {
				String key = entry.getKey();
				if (key.startsWith(AVOID_ROUTING_PARAMETER_PREFIX)) {
					list.add(entry.getValue());
				}
			}
		}
		Collator collator = OsmAndCollator.primaryCollator();
		list.sort((o1, o2) -> {
			String name1 = AndroidUtils.getRoutingStringPropertyName(app, o1.getId(), o1.getName());
			String name2 = AndroidUtils.getRoutingStringPropertyName(app, o2.getId(), o2.getName());

			return collator.compare(name1, name2);
		});
		return list;
	}

	@NonNull
	public Map<RoutingParameter, Boolean> getAvoidParametersWithStates(@NonNull OsmandApplication app) {
		Map<RoutingParameter, Boolean> map = new LinkedHashMap<>();
		ApplicationMode mode = app.getRoutingHelper().getAppMode();
		List<RoutingParameter> parameters = getAvoidParameters(mode);

		for (RoutingParameter parameter : parameters) {
			CommonPreference<Boolean> preference = app.getSettings().getCustomRoutingBooleanProperty(parameter.getId(), parameter.getDefaultBoolean());
			map.put(parameter, preference.getModeValue(mode));
		}

		return map;
	}

	public RoutingParameter getRoutingPrefsForAppModeById(ApplicationMode applicationMode, String parameterId) {
		GeneralRouter router = app.getRouter(applicationMode);
		RoutingParameter parameter = null;

		if (router != null) {
			parameter = RoutingHelperUtils.getParameterForDerivedProfile(parameterId, applicationMode, router);
		}

		return parameter;
	}

	public boolean isNightMode() {
		return app.getDaynightHelper().isNightModeForMapControls();
	}

	public static class LocalRoutingParameter {

		public static final String KEY = NAVIGATION_LOCAL_ROUTING_ID;

		public RoutingParameter routingParameter;

		private final ApplicationMode mode;

		@DrawableRes
		public
		int activeIconId = -1;

		@DrawableRes
		int disabledIconId = -1;

		public boolean canAddToRouteMenu() {
			return true;
		}

		public String getKey() {
			if (routingParameter != null) {
				return routingParameter.getId();
			}
			return KEY;
		}

		public int getActiveIconId() {
			return activeIconId;
		}

		public int getDisabledIconId() {
			return disabledIconId;
		}

		public LocalRoutingParameter(ApplicationMode mode) {
			this.mode = mode;
		}

		public String getText(MapActivity mapActivity) {
			return AndroidUtils.getRoutingStringPropertyName(mapActivity, routingParameter.getId(),
					routingParameter.getName());
		}

		public boolean isSelected(OsmandSettings settings) {
			CommonPreference<Boolean> property =
					settings.getCustomRoutingBooleanProperty(routingParameter.getId(), routingParameter.getDefaultBoolean());
			if (mode != null) {
				return property.getModeValue(mode);
			} else {
				return property.get();
			}
		}

		public void setSelected(OsmandSettings settings, boolean isChecked) {
			CommonPreference<Boolean> property =
					settings.getCustomRoutingBooleanProperty(routingParameter.getId(), routingParameter.getDefaultBoolean());
			if (mode != null) {
				property.setModeValue(mode, isChecked);
			} else {
				property.set(isChecked);
			}
		}

		public ApplicationMode getApplicationMode() {
			return mode;
		}
	}

	public static class LocalRoutingParameterGroup extends LocalRoutingParameter {

		public static final String KEY = NAVIGATION_LOCAL_ROUTING_GROUP_ID;

		private final String groupName;
		private final List<LocalRoutingParameter> routingParameters = new ArrayList<>();

		public String getKey() {
			if (groupName != null) {
				return groupName;
			}
			return KEY;
		}

		public LocalRoutingParameterGroup(ApplicationMode am, String groupName) {
			super(am);
			this.groupName = groupName;
		}

		public void addRoutingParameter(RoutingParameter routingParameter) {
			LocalRoutingParameter p = new LocalRoutingParameter(getApplicationMode());
			p.routingParameter = routingParameter;
			routingParameters.add(p);
		}

		public String getGroupName() {
			return groupName;
		}

		public List<LocalRoutingParameter> getRoutingParameters() {
			return routingParameters;
		}

		@Override
		public String getText(MapActivity mapActivity) {
			return AndroidUtils.getRoutingStringPropertyName(mapActivity, groupName,
					Algorithms.capitalizeFirstLetterAndLowercase(groupName.replace('_', ' ')));
		}

		@Override
		public boolean isSelected(OsmandSettings settings) {
			return false;
		}

		@Override
		public void setSelected(OsmandSettings settings, boolean isChecked) {
		}

		public LocalRoutingParameter getSelected(OsmandSettings settings) {
			for (LocalRoutingParameter p : routingParameters) {
				if (p.isSelected(settings)) {
					return p;
				}
			}
			return null;
		}
	}

	public static class MuteSoundRoutingParameter extends LocalRoutingParameter {

		public static final String KEY = NAVIGATION_SOUND_ID;

		public MuteSoundRoutingParameter() {
			super(null);
		}

		public String getKey() {
			return KEY;
		}

		@Override
		public int getActiveIconId() {
			return R.drawable.ic_action_volume_up;
		}

		@Override
		public int getDisabledIconId() {
			return R.drawable.ic_action_volume_mute;
		}
	}

	public static class DividerItem extends LocalRoutingParameter {

		public static final String KEY = NAVIGATION_DIVIDER_ID;

		public String getKey() {
			return KEY;
		}

		public boolean canAddToRouteMenu() {
			return false;
		}

		public DividerItem() {
			super(null);
		}
	}

	public static class RouteSimulationItem extends LocalRoutingParameter {

		public static final String KEY = NAVIGATION_ROUTE_SIMULATION_ID;

		public String getKey() {
			return KEY;
		}

		public boolean canAddToRouteMenu() {
			return false;
		}

		public RouteSimulationItem() {
			super(null);
		}
	}

	public static class CalculateAltitudeItem extends LocalRoutingParameter {

		public static final String KEY = NAVIGATION_ROUTE_CALCULATE_ALTITUDE_ID;

		public String getKey() {
			return KEY;
		}

		public boolean canAddToRouteMenu() {
			return false;
		}

		public CalculateAltitudeItem() {
			super(null);
		}
	}

	public static class TimeConditionalRoutingItem extends LocalRoutingParameter {

		public static final String KEY = NAVIGATION_TIME_CONDITIONAL_ID;

		public String getKey() {
			return KEY;
		}

		public boolean canAddToRouteMenu() {
			return false;
		}

		public TimeConditionalRoutingItem() {
			super(null);
		}

		@Override
		public int getActiveIconId() {
			return R.drawable.ic_action_road_works_dark;
		}

		@Override
		public int getDisabledIconId() {
			return R.drawable.ic_action_road_works_dark;
		}
	}

	public static class ShowAlongTheRouteItem extends LocalRoutingParameter {

		public static final String KEY = NAVIGATION_SHOW_ALONG_THE_ROUTE_ID;

		public ShowAlongTheRouteItem() {
			super(null);
		}

		public String getKey() {
			return KEY;
		}

		@Override
		public int getActiveIconId() {
			return R.drawable.ic_action_show_along_route;
		}

		@Override
		public int getDisabledIconId() {
			return R.drawable.ic_action_show_along_route;
		}
	}

	public static class AvoidRoadsRoutingParameter extends LocalRoutingParameter {

		public static final String KEY = NAVIGATION_AVOID_ROADS_ID;

		public AvoidRoadsRoutingParameter() {
			super(null);
		}

		public String getKey() {
			return KEY;
		}

		@Override
		public int getActiveIconId() {
			return R.drawable.ic_action_alert;
		}

		@Override
		public int getDisabledIconId() {
			return R.drawable.ic_action_alert;
		}
	}

	public static class AvoidPTTypesRoutingParameter extends LocalRoutingParameter {

		public static final String KEY = NAVIGATION_AVOID_PT_TYPES_ID;

		public AvoidPTTypesRoutingParameter() {
			super(null);
		}

		public String getKey() {
			return KEY;
		}

		@Override
		public int getActiveIconId() {
			return R.drawable.ic_action_bus_dark;
		}

		@Override
		public int getDisabledIconId() {
			return R.drawable.ic_action_bus_dark;
		}
	}

	public static class GpxLocalRoutingParameter extends LocalRoutingParameter {

		public static final String KEY = NAVIGATION_FOLLOW_TRACK_ID;

		public String getKey() {
			return KEY;
		}

		public boolean canAddToRouteMenu() {
			return false;
		}

		public GpxLocalRoutingParameter() {
			super(null);
		}

		@Override
		public int getActiveIconId() {
			return R.drawable.ic_action_polygom_dark;
		}

		@Override
		public int getDisabledIconId() {
			return R.drawable.ic_action_polygom_dark;
		}
	}

	public static class OtherSettingsRoutingParameter extends LocalRoutingParameter {

		public static final String KEY = NAVIGATION_OTHER_SETTINGS_ID;

		public OtherSettingsRoutingParameter() {
			super(null);
		}

		public String getKey() {
			return KEY;
		}

		public boolean canAddToRouteMenu() {
			return false;
		}

		@Override
		public int getActiveIconId() {
			return R.drawable.ic_action_settings;
		}

		@Override
		public int getDisabledIconId() {
			return R.drawable.ic_action_settings;
		}
	}

	public static class CustomizeRouteLineRoutingParameter extends LocalRoutingParameter {

		public static final String KEY = NAVIGATION_CUSTOMIZE_ROUTE_LINE_ID;

		public CustomizeRouteLineRoutingParameter() {
			super(null);
		}

		@Override
		public String getKey() {
			return KEY;
		}

		@Override
		public boolean canAddToRouteMenu() {
			return false;
		}

		@Override
		public int getActiveIconId() {
			return R.drawable.ic_action_appearance;
		}

		@Override
		public int getDisabledIconId() {
			return R.drawable.ic_action_appearance;
		}
	}

	public static class OtherLocalRoutingParameter extends LocalRoutingParameter {

		public static final String KEY = NAVIGATION_OTHER_LOCAL_ROUTING_ID;

		public String getKey() {
			return KEY;
		}

		public boolean canAddToRouteMenu() {
			return false;
		}

		public String text;
		public boolean selected;
		public int id;

		public OtherLocalRoutingParameter(int id, String text, boolean selected) {
			super(null);
			this.text = text;
			this.selected = selected;
			this.id = id;
		}

		@Override
		public String getText(MapActivity mapActivity) {
			return text;
		}

		@Override
		public boolean isSelected(OsmandSettings settings) {
			return selected;
		}

		@Override
		public void setSelected(OsmandSettings settings, boolean isChecked) {
			selected = isChecked;
		}
	}

	public static class InterruptMusicRoutingParameter extends LocalRoutingParameter {

		public static final String KEY = NAVIGATION_INTERRUPT_MUSIC_ID;

		public String getKey() {
			return KEY;
		}

		public InterruptMusicRoutingParameter() {
			super(null);
		}
	}

	public static class VoiceGuidanceRoutingParameter extends LocalRoutingParameter {

		public static final String KEY = NAVIGATION_VOICE_GUIDANCE_ID;

		public String getKey() {
			return KEY;
		}

		public VoiceGuidanceRoutingParameter() {
			super(null);
		}
	}

	public static class RouteMenuAppModes {

		public ApplicationMode am;

		public List<LocalRoutingParameter> parameters;

		public RouteMenuAppModes(ApplicationMode am, List<LocalRoutingParameter> parameters) {
			this.am = am;
			this.parameters = parameters;
		}

		public boolean containsParameter(LocalRoutingParameter parameter) {
			for (LocalRoutingParameter p : parameters) {
				if (p.getKey().equals(parameter.getKey())) {
					return true;
				}
			}
			return false;
		}
	}

	public enum PermanentAppModeOptions {

		CAR(MuteSoundRoutingParameter.KEY, AvoidRoadsRoutingParameter.KEY),
		BICYCLE(MuteSoundRoutingParameter.KEY, DRIVING_STYLE, USE_HEIGHT_OBSTACLES),
		PEDESTRIAN(MuteSoundRoutingParameter.KEY, USE_HEIGHT_OBSTACLES),
		PUBLIC_TRANSPORT(MuteSoundRoutingParameter.KEY, AvoidPTTypesRoutingParameter.KEY),
		BOAT(MuteSoundRoutingParameter.KEY),
		AIRCRAFT(MuteSoundRoutingParameter.KEY),
		SKI(MuteSoundRoutingParameter.KEY, DRIVING_STYLE, USE_HEIGHT_OBSTACLES),
		HORSE(MuteSoundRoutingParameter.KEY),
		OTHER(MuteSoundRoutingParameter.KEY);

		List<String> routingParameters;

		PermanentAppModeOptions(String... routingParameters) {
			this.routingParameters = Arrays.asList(routingParameters);
		}
	}

	private List<String> getRoutingParametersForProfileType(ApplicationMode appMode) {
		if (appMode != null) {
			boolean osmandRouter = appMode.getRouteService() == RouteService.OSMAND;
			if (!osmandRouter) {
				return PermanentAppModeOptions.OTHER.routingParameters;
			} else if (appMode.isDerivedRoutingFrom(ApplicationMode.CAR)) {
				return PermanentAppModeOptions.CAR.routingParameters;
			} else if (appMode.isDerivedRoutingFrom(ApplicationMode.BICYCLE)) {
				return PermanentAppModeOptions.BICYCLE.routingParameters;
			} else if (appMode.isDerivedRoutingFrom(ApplicationMode.PEDESTRIAN)) {
				return PermanentAppModeOptions.PEDESTRIAN.routingParameters;
			} else if (appMode.isDerivedRoutingFrom(ApplicationMode.PUBLIC_TRANSPORT)) {
				return PermanentAppModeOptions.PUBLIC_TRANSPORT.routingParameters;
			} else if (appMode.isDerivedRoutingFrom(ApplicationMode.BOAT)) {
				return PermanentAppModeOptions.BOAT.routingParameters;
			} else if (appMode.isDerivedRoutingFrom(ApplicationMode.AIRCRAFT)) {
				return PermanentAppModeOptions.AIRCRAFT.routingParameters;
			} else if (appMode.isDerivedRoutingFrom(ApplicationMode.SKI)) {
				return PermanentAppModeOptions.SKI.routingParameters;
			} else if (appMode.isDerivedRoutingFrom(ApplicationMode.HORSE)) {
				return PermanentAppModeOptions.HORSE.routingParameters;
			} else {
				return PermanentAppModeOptions.OTHER.routingParameters;
			}
		}
		return null;
	}
}