package net.osmand.plus.routepreparationmenu;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatCheckedTextView;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import net.osmand.CallbackWithObject;
import net.osmand.GPXUtilities;
import net.osmand.IndexConstants;
import net.osmand.Location;
import net.osmand.data.LatLon;
import net.osmand.plus.ApplicationMode;
import net.osmand.plus.ContextMenuAdapter;
import net.osmand.plus.ContextMenuItem;
import net.osmand.plus.DialogListItemAdapter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.TargetPointsHelper;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.SettingsBaseActivity;
import net.osmand.plus.activities.SettingsNavigationActivity;
import net.osmand.plus.dashboard.DashboardOnMap;
import net.osmand.plus.download.DownloadActivity;
import net.osmand.plus.download.DownloadActivityType;
import net.osmand.plus.helpers.FileNameTranslationHelper;
import net.osmand.plus.routing.RouteProvider;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.voice.JSMediaCommandPlayerImpl;
import net.osmand.plus.voice.JSTTSCommandPlayerImpl;
import net.osmand.plus.voice.MediaCommandPlayerImpl;
import net.osmand.plus.voice.TTSCommandPlayerImpl;
import net.osmand.router.GeneralRouter;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static net.osmand.plus.activities.SettingsNavigationActivity.getRouter;

public class RoutingOptionsHelper {

	public static final String MORE_VALUE = "MORE_VALUE";
	public static final String DRIVING_STYLE = "driving_style";

	private OsmandApplication app;

	private Map<ApplicationMode, RouteMenuAppModes> modes = new HashMap<>();

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

	public RouteMenuAppModes getRouteMenuAppMode(ApplicationMode appMode) {
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

	public void switchMusic() {
		OsmandSettings settings = app.getSettings();
		boolean mt = !settings.INTERRUPT_MUSIC.get();
		settings.INTERRUPT_MUSIC.set(mt);
	}

	public void selectRestrictedRoads(final MapActivity mapActivity) {
		mapActivity.getDashboard().setDashboardVisibility(false, DashboardOnMap.DashboardType.ROUTE_PREFERENCES);
		mapActivity.getMapRouteInfoMenu().hide();
		mapActivity.getMyApplication().getAvoidSpecificRoads().showDialog(mapActivity);
	}

	public void selectVoiceGuidance(final MapActivity mapActivity, final CallbackWithObject<String> callback) {
		final ContextMenuAdapter adapter = new ContextMenuAdapter();

		String[] entries;
		final String[] entrieValues;
		Set<String> voiceFiles = getVoiceFiles(mapActivity);
		entries = new String[voiceFiles.size() + 2];
		entrieValues = new String[voiceFiles.size() + 2];
		int k = 0;
		int selected = -1;
		String selectedValue = mapActivity.getMyApplication().getSettings().VOICE_PROVIDER.get();
		entrieValues[k] = OsmandSettings.VOICE_PROVIDER_NOT_USE;
		entries[k] = mapActivity.getResources().getString(R.string.shared_string_do_not_use);
		ContextMenuItem.ItemBuilder itemBuilder = new ContextMenuItem.ItemBuilder();
		adapter.addItem(itemBuilder.setTitle(entries[k]).createItem());
		if (OsmandSettings.VOICE_PROVIDER_NOT_USE.equals(selectedValue)) {
			selected = k;
		}
		k++;
		for (String s : voiceFiles) {
			entries[k] = (s.contains("tts") ? mapActivity.getResources().getString(R.string.ttsvoice) + " " : "") +
					FileNameTranslationHelper.getVoiceName(mapActivity, s);
			entrieValues[k] = s;
			adapter.addItem(itemBuilder.setTitle(entries[k]).createItem());
			if (s.equals(selectedValue)) {
				selected = k;
			}
			k++;
		}
		entrieValues[k] = MORE_VALUE;
		entries[k] = mapActivity.getResources().getString(R.string.install_more);
		adapter.addItem(itemBuilder.setTitle(entries[k]).createItem());

		boolean nightMode = isNightMode(app);
		Context themedContext = UiUtilities.getThemedContext(mapActivity, nightMode);
		int themeRes = getThemeRes(app);
		ApplicationMode selectedAppMode = app.getRoutingHelper().getAppMode();
		int selectedModeColor = ContextCompat.getColor(app, selectedAppMode.getIconColorInfo().getColor(nightMode));
		DialogListItemAdapter dialogAdapter = DialogListItemAdapter.createSingleChoiceAdapter(
				entries, nightMode, selected, app, selectedModeColor, themeRes, new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						int which = (int) v.getTag();
						String value = entrieValues[which];
						if (MORE_VALUE.equals(value)) {
							final Intent intent = new Intent(mapActivity, DownloadActivity.class);
							intent.putExtra(DownloadActivity.TAB_TO_OPEN, DownloadActivity.DOWNLOAD_TAB);
							intent.putExtra(DownloadActivity.FILTER_CAT, DownloadActivityType.VOICE_FILE.getTag());
							mapActivity.startActivity(intent);
						} else {
							if (callback != null) {
								callback.processResult(value);
							}
						}
					}
				}
		);
		AlertDialog.Builder bld = new AlertDialog.Builder(themedContext);
		bld.setAdapter(dialogAdapter, null);
		dialogAdapter.setDialog(bld.show());
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
		if (applyAllModes) {
			for (ApplicationMode mode : ApplicationMode.allPossibleValues()) {
				app.getSettings().VOICE_PROVIDER.setModeValue(mode, provider);
			}
		} else {
			app.getSettings().VOICE_PROVIDER.setModeValue(selectedAppMode, provider);
		}
		app.initVoiceCommandPlayer(mapActivity, selectedAppMode, false, null, true, false, applyAllModes);
	}

	public Set<String> getVoiceFiles(Activity activity) {
		// read available voice data
		OsmandApplication app = ((OsmandApplication) activity.getApplication());
		File extStorage = app.getAppPath(IndexConstants.VOICE_INDEX_DIR);
		Set<String> setFiles = new LinkedHashSet<String>();
		if (extStorage.exists()) {
			for (File f : extStorage.listFiles()) {
				if (f.isDirectory()) {
					if (JSMediaCommandPlayerImpl.isMyData(f) || JSTTSCommandPlayerImpl.isMyData(f)
							|| MediaCommandPlayerImpl.isMyData(f) || TTSCommandPlayerImpl.isMyData(f)) {
						setFiles.add(f.getName());
					}
				}
			}
		}
		return setFiles;
	}

	public void applyRoutingParameter(LocalRoutingParameter rp, boolean isChecked) {
		final OsmandSettings settings = app.getSettings();
		RoutingHelper routingHelper = app.getRoutingHelper();
		// if short way that it should set valut to fast mode opposite of current
		if (rp.routingParameter != null && rp.routingParameter.getId().equals(GeneralRouter.USE_SHORTEST_WAY)) {
			settings.FAST_ROUTE_MODE.setModeValue(routingHelper.getAppMode(), !isChecked);
		}
		rp.setSelected(settings, isChecked);

		if (rp instanceof OtherLocalRoutingParameter) {
			updateGpxRoutingParameter((OtherLocalRoutingParameter) rp);
		}
		routingHelper.recalculateRouteDueToSettingsChange();
	}

	public void updateGpxRoutingParameter(OtherLocalRoutingParameter gpxParam) {
		RouteProvider.GPXRouteParamsBuilder rp = app.getRoutingHelper().getCurrentGPXRoute();
		final OsmandSettings settings = app.getSettings();
		boolean selected = gpxParam.isSelected(settings);
		if (rp != null) {
			if (gpxParam.id == R.string.gpx_option_reverse_route) {
				rp.setReverse(selected);
				TargetPointsHelper tg = app.getTargetPointsHelper();
				List<Location> ps = rp.getPoints();
				if (ps.size() > 0) {
					Location first = ps.get(0);
					Location end = ps.get(ps.size() - 1);
					TargetPointsHelper.TargetPoint pn = tg.getPointToNavigate();
					boolean update = false;
					if (pn == null
							|| MapUtils.getDistance(pn.point, new LatLon(first.getLatitude(), first.getLongitude())) < 10) {
						tg.navigateToPoint(new LatLon(end.getLatitude(), end.getLongitude()), false, -1);
						update = true;
					}
					if (tg.getPointToStart() == null
							|| MapUtils.getDistance(tg.getPointToStart().point,
							new LatLon(end.getLatitude(), end.getLongitude())) < 10) {
						tg.setStartPoint(new LatLon(first.getLatitude(), first.getLongitude()), false, null);
						update = true;
					}
					if (update) {
						tg.updateRouteAndRefresh(true);
					}
				}
			} else if (gpxParam.id == R.string.gpx_option_calculate_first_last_segment) {
				rp.setCalculateOsmAndRouteParts(selected);
				settings.GPX_ROUTE_CALC_OSMAND_PARTS.set(selected);
			} else if (gpxParam.id == R.string.gpx_option_from_start_point) {
				rp.setPassWholeRoute(selected);
			} else if (gpxParam.id == R.string.use_points_as_intermediates) {
				settings.GPX_CALCULATE_RTEPT.set(selected);
				rp.setUseIntermediatePointsRTE(selected);
			} else if (gpxParam.id == R.string.calculate_osmand_route_gpx) {
				settings.GPX_ROUTE_CALC.set(selected);
				rp.setCalculateOsmAndRoute(selected);
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
				if (item instanceof GpxLocalRoutingParameter) {
					list.addAll(getGpxRouterParameters(am));
				} else if (item instanceof TimeConditionalRoutingItem) {
					list.addAll(getOsmandRouterParameters(am));
				}
			}
		}
		return list;
	}

	public interface OnClickListener {
		void onClick();
	}

	public void showLocalRoutingParameterGroupDialog(final LocalRoutingParameterGroup group, final MapActivity mapActivity, final OnClickListener listener) {
		OsmandSettings settings = app.getSettings();
		final ContextMenuAdapter adapter = new ContextMenuAdapter();
		int i = 0;
		int selectedIndex = -1;
		for (LocalRoutingParameter p : group.getRoutingParameters()) {
			adapter.addItem(ContextMenuItem.createBuilder(p.getText(mapActivity)).setSelected(false).createItem());
			if (p.isSelected(settings)) {
				selectedIndex = i;
			}
			i++;
		}
		if (selectedIndex == -1) {
			selectedIndex = 0;
		}

		final boolean nightMode = isNightMode(app);
		Context themedContext = UiUtilities.getThemedContext(mapActivity, nightMode);
		ApplicationMode selectedAppMode = app.getRoutingHelper().getAppMode();
		final int selectedModeColor = ContextCompat.getColor(app, selectedAppMode.getIconColorInfo().getColor(nightMode));
		AlertDialog.Builder builder = new AlertDialog.Builder(themedContext);
		final int layout = R.layout.list_menu_item_native_singlechoice;

		final ArrayAdapter<String> listAdapter = new ArrayAdapter<String>(themedContext, layout, R.id.text1, adapter.getItemNames()) {
			@NonNull
			@Override
			public View getView(final int position, View convertView, @NonNull ViewGroup parent) {
				// User super class to create the View
				View v = convertView;
				if (v == null) {
					v = UiUtilities.getInflater(mapActivity, nightMode).inflate(layout, parent, false);
				}
				final ContextMenuItem item = adapter.getItem(position);
				AppCompatCheckedTextView tv = (AppCompatCheckedTextView) v.findViewById(R.id.text1);
				tv.setText(item.getTitle());
				tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f);
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
					UiUtilities.setupCompoundButtonDrawable(app, nightMode, selectedModeColor, tv.getCheckMarkDrawable());
				}

				return v;
			}
		};

		final int[] selectedPosition = {selectedIndex};
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
							mapActivity.getRoutingHelper().recalculateRouteDueToSettingsChange();
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
			default:
				return getRoutingParameterInnerById(am, parameter);
		}
	}

	public LocalRoutingParameter getRoutingParameterInnerById(ApplicationMode am, String parameterId) {
		RouteProvider.GPXRouteParamsBuilder rparams = app.getRoutingHelper().getCurrentGPXRoute();
		GeneralRouter rm = getRouter(app.getRoutingConfig(), am);
		if (rm == null || (rparams != null && !rparams.isCalculateOsmAndRoute()) && !rparams.getFile().hasRtePt()) {
			return null;
		}

		LocalRoutingParameter rp;
		Map<String, GeneralRouter.RoutingParameter> parameters = rm.getParameters();
		GeneralRouter.RoutingParameter routingParameter = parameters.get(parameterId);

		if (routingParameter != null) {
			rp = new LocalRoutingParameter(am);
			rp.routingParameter = routingParameter;
		} else {
			LocalRoutingParameterGroup rpg = null;
			for (GeneralRouter.RoutingParameter r : rm.getParameters().values()) {
				if (r.getType() == GeneralRouter.RoutingParameterType.BOOLEAN
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
		boolean osmandRouter = am.getRouteService() == RouteProvider.RouteService.OSMAND;
		if (!osmandRouter) {
			list.add(new OtherLocalRoutingParameter(R.string.calculate_osmand_route_without_internet,
					app.getString(R.string.calculate_osmand_route_without_internet), settings.GPX_ROUTE_CALC_OSMAND_PARTS.get()));
			list.add(new OtherLocalRoutingParameter(R.string.fast_route_mode, app.getString(R.string.fast_route_mode),
					settings.FAST_ROUTE_MODE.get()));
		}
		return list;
	}

	public List<LocalRoutingParameter> getGpxRouterParameters(ApplicationMode am) {
		List<LocalRoutingParameter> list = new ArrayList<LocalRoutingParameter>();
		RouteProvider.GPXRouteParamsBuilder rparams = app.getRoutingHelper().getCurrentGPXRoute();
		boolean osmandRouter = am.getRouteService() == RouteProvider.RouteService.OSMAND;
		if (rparams != null && osmandRouter) {
			GPXUtilities.GPXFile fl = rparams.getFile();
			if (fl.hasRtePt()) {
				list.add(new OtherLocalRoutingParameter(R.string.use_points_as_intermediates,
						app.getString(R.string.use_points_as_intermediates), rparams.isUseIntermediatePointsRTE()));
			}
			list.add(new OtherLocalRoutingParameter(R.string.gpx_option_reverse_route,
					app.getString(R.string.gpx_option_reverse_route), rparams.isReverse()));
			if (!rparams.isUseIntermediatePointsRTE()) {
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
		boolean osmandRouter = am.getRouteService() == RouteProvider.RouteService.OSMAND;
		if (!osmandRouter) {
			return getOsmandRouterParameters(am);
		}

		RouteProvider.GPXRouteParamsBuilder rparams = app.getRoutingHelper().getCurrentGPXRoute();
		List<LocalRoutingParameter> list = new ArrayList<LocalRoutingParameter>(getGpxRouterParameters(am));
		GeneralRouter rm = SettingsNavigationActivity.getRouter(app.getRoutingConfig(), am);
		if (rm == null || (rparams != null && !rparams.isCalculateOsmAndRoute()) && !rparams.getFile().hasRtePt()) {
			return list;
		}
		for (GeneralRouter.RoutingParameter r : rm.getParameters().values()) {
			if (r.getType() == GeneralRouter.RoutingParameterType.BOOLEAN) {
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
			case GeneralRouter.USE_SHORTEST_WAY:
				rp.activeIconId = R.drawable.ic_action_fuel;
				rp.disabledIconId = R.drawable.ic_action_fuel;
				break;
			case GeneralRouter.USE_HEIGHT_OBSTACLES:
				rp.activeIconId = R.drawable.ic_action_elevation;
				rp.disabledIconId = R.drawable.ic_action_elevation;
				break;
			case GeneralRouter.AVOID_FERRIES:
				rp.activeIconId = R.drawable.ic_action_fuel;
				rp.disabledIconId = R.drawable.ic_action_fuel;
				break;
			case GeneralRouter.AVOID_TOLL:
				rp.activeIconId = R.drawable.ic_action_fuel;
				rp.disabledIconId = R.drawable.ic_action_fuel;
				break;
			case GeneralRouter.AVOID_MOTORWAY:
				rp.activeIconId = R.drawable.ic_action_motorways;
				rp.disabledIconId = R.drawable.ic_action_avoid_motorways;
				break;
			case GeneralRouter.AVOID_UNPAVED:
				rp.activeIconId = R.drawable.ic_action_fuel;
				rp.disabledIconId = R.drawable.ic_action_fuel;
				break;
			case GeneralRouter.PREFER_MOTORWAYS:
				rp.activeIconId = R.drawable.ic_action_motorways;
				rp.activeIconId = R.drawable.ic_action_avoid_motorways;
				break;
			case GeneralRouter.ALLOW_PRIVATE:
				rp.activeIconId = R.drawable.ic_action_allow_private_access;
				rp.disabledIconId = R.drawable.ic_action_forbid_private_access;
				break;
			case GeneralRouter.ALLOW_MOTORWAYS:
				rp.activeIconId = R.drawable.ic_action_motorways;
				rp.disabledIconId = R.drawable.ic_action_avoid_motorways;
				break;
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

	public List<GeneralRouter.RoutingParameter> getAvoidRoutingPrefsForAppMode(ApplicationMode applicationMode) {
		List<GeneralRouter.RoutingParameter> avoidParameters = new ArrayList<GeneralRouter.RoutingParameter>();
		GeneralRouter router = getRouter(app.getRoutingConfig(), applicationMode);
		if (router != null) {
			for (Map.Entry<String, GeneralRouter.RoutingParameter> e : router.getParameters().entrySet()) {
				String param = e.getKey();
				GeneralRouter.RoutingParameter routingParameter = e.getValue();
				if (param.startsWith("avoid_")) {
					avoidParameters.add(routingParameter);
				}
			}
		}
		return avoidParameters;
	}

	public GeneralRouter.RoutingParameter getRoutingPrefsForAppModeById(ApplicationMode applicationMode, String parameterId) {
		GeneralRouter router = getRouter(app.getRoutingConfig(), applicationMode);
		GeneralRouter.RoutingParameter parameter = null;

		if (router != null) {
			parameter = router.getParameters().get(parameterId);
		}

		return parameter;
	}
	
	public boolean isNightMode(OsmandApplication app) {
		if (app == null) {
			return false;
		}
		return app.getDaynightHelper().isNightModeForMapControls();
	}
	
	public int getThemeRes(OsmandApplication app) {
		return isNightMode(app) ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme;
	}

	public static class LocalRoutingParameter {

		public static final String KEY = "LocalRoutingParameter";

		public GeneralRouter.RoutingParameter routingParameter;

		private ApplicationMode am;

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

		public LocalRoutingParameter(ApplicationMode am) {
			this.am = am;
		}

		public String getText(MapActivity mapActivity) {
			return SettingsBaseActivity.getRoutingStringPropertyName(mapActivity, routingParameter.getId(),
					routingParameter.getName());
		}

		public boolean isSelected(OsmandSettings settings) {
			final OsmandSettings.CommonPreference<Boolean> property =
					settings.getCustomRoutingBooleanProperty(routingParameter.getId(), routingParameter.getDefaultBoolean());
			if (am != null) {
				return property.getModeValue(am);
			} else {
				return property.get();
			}
		}

		public void setSelected(OsmandSettings settings, boolean isChecked) {
			final OsmandSettings.CommonPreference<Boolean> property =
					settings.getCustomRoutingBooleanProperty(routingParameter.getId(), routingParameter.getDefaultBoolean());
			if (am != null) {
				property.setModeValue(am, isChecked);
			} else {
				property.set(isChecked);
			}
		}

		public ApplicationMode getApplicationMode() {
			return am;
		}
	}

	public static class LocalRoutingParameterGroup extends LocalRoutingParameter {

		public static final String KEY = "LocalRoutingParameterGroup";

		private String groupName;
		private List<LocalRoutingParameter> routingParameters = new ArrayList<>();

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

		public void addRoutingParameter(GeneralRouter.RoutingParameter routingParameter) {
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
			return SettingsBaseActivity.getRoutingStringPropertyName(mapActivity, groupName,
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

		public static final String KEY = "MuteSoundRoutingParameter";

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

		public static final String KEY = "DividerItem";

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

		public static final String KEY = "RouteSimulationItem";

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

	public static class TimeConditionalRoutingItem extends LocalRoutingParameter {

		public static final String KEY = "TimeConditionalRoutingItem";

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

		public static final String KEY = "ShowAlongTheRouteItem";

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

		public static final String KEY = "AvoidRoadsRoutingParameter";

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

		public static final String KEY = "AvoidPTTypesRoutingParameter";

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

		public static final String KEY = "GpxLocalRoutingParameter";

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

		public static final String KEY = "OtherSettingsRoutingParameter";

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
			return R.drawable.map_action_settings;
		}

		@Override
		public int getDisabledIconId() {
			return R.drawable.map_action_settings;
		}
	}

	public static class OtherLocalRoutingParameter extends LocalRoutingParameter {

		public static final String KEY = "OtherLocalRoutingParameter";

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

		public static final String KEY = "InterruptMusicRoutingParameter";

		public String getKey() {
			return KEY;
		}

		public InterruptMusicRoutingParameter() {
			super(null);
		}
	}

	public static class VoiceGuidanceRoutingParameter extends LocalRoutingParameter {

		public static final String KEY = "VoiceGuidanceRoutingParameter";

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
		BICYCLE(MuteSoundRoutingParameter.KEY, DRIVING_STYLE, GeneralRouter.USE_HEIGHT_OBSTACLES),
		PEDESTRIAN(MuteSoundRoutingParameter.KEY, GeneralRouter.USE_HEIGHT_OBSTACLES),
		PUBLIC_TRANSPORT(MuteSoundRoutingParameter.KEY, AvoidPTTypesRoutingParameter.KEY),
		BOAT(MuteSoundRoutingParameter.KEY),
		AIRCRAFT(MuteSoundRoutingParameter.KEY),
		SKI(MuteSoundRoutingParameter.KEY, DRIVING_STYLE, GeneralRouter.USE_HEIGHT_OBSTACLES),
		OTHER(MuteSoundRoutingParameter.KEY);

		List<String> routingParameters;

		PermanentAppModeOptions(String... routingParameters) {
			this.routingParameters = Arrays.asList(routingParameters);
		}
	}

	private List<String> getRoutingParametersForProfileType(ApplicationMode appMode) {
		if (appMode != null) {
			boolean osmandRouter = appMode.getRouteService() == RouteProvider.RouteService.OSMAND;
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
			} else {
				return PermanentAppModeOptions.OTHER.routingParameters;
			}
		}
		return null;
	}
}