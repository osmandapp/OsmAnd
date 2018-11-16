package net.osmand.plus.routepreparationmenu;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AlertDialog;

import net.osmand.CallbackWithObject;
import net.osmand.IndexConstants;
import net.osmand.Location;
import net.osmand.data.LatLon;
import net.osmand.plus.ApplicationMode;
import net.osmand.plus.ContextMenuAdapter;
import net.osmand.plus.ContextMenuItem;
import net.osmand.plus.GPXUtilities;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.TargetPointsHelper;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static net.osmand.plus.activities.SettingsNavigationActivity.getRouter;

public class RoutingOptionsHelper {

	private OsmandApplication app;
	private OsmandSettings settings;
	private RoutingHelper routingHelper;


	public RoutingOptionsHelper(OsmandApplication application) {
		app = application;
		settings = app.getSettings();
		routingHelper = app.getRoutingHelper();
	}

	public static final String MORE_VALUE = "MORE_VALUE";

	public void switchSound() {
		boolean mt = !routingHelper.getVoiceRouter().isMute();
		settings.VOICE_MUTE.set(mt);
		routingHelper.getVoiceRouter().setMute(mt);
	}

	public void switchMusic() {
		boolean mt = !settings.INTERRUPT_MUSIC.get();
		settings.INTERRUPT_MUSIC.set(mt);
	}

	public void selectRestrictedRoads(final MapActivity mapActivity) {
		mapActivity.getDashboard().setDashboardVisibility(false, DashboardOnMap.DashboardType.ROUTE_PREFERENCES);
		mapActivity.getMapLayers().getMapControlsLayer().getMapRouteInfoMenu().hide();
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

		AlertDialog.Builder bld = new AlertDialog.Builder(mapActivity);
		bld.setSingleChoiceItems(entries, selected, new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
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
				dialog.dismiss();
			}
		});
		bld.show();
	}

	public String getVoiceProviderName(Context ctx, String value) {
		if (OsmandSettings.VOICE_PROVIDER_NOT_USE.equals(value)) {
			return ctx.getResources().getString(R.string.shared_string_do_not_use);
		} else {
			return (value.contains("tts") ? ctx.getResources().getString(R.string.ttsvoice) + " " : "") +
					FileNameTranslationHelper.getVoiceName(ctx, value);
		}
	}

	public void applyVoiceProvider(MapActivity mapActivity, String provider) {
		OsmandApplication app = mapActivity.getMyApplication();
		app.getSettings().VOICE_PROVIDER.set(provider);
		app.initVoiceCommandPlayer(mapActivity, app.getRoutingHelper().getAppMode(), false, null, true, false);
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
		if (rp.routingParameter != null && rp.routingParameter.getId().equals("short_way")) {
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

	public List<LocalRoutingParameter> getRoutingParameters(ApplicationMode am) {
		List<LocalRoutingParameter> list = new ArrayList<>();
		if (am.equals(ApplicationMode.CAR)) {
			getAppModeItems(am, list, AppModeOptions.CAR.routingParameters);
		} else if (am.equals(ApplicationMode.BICYCLE)) {
			getAppModeItems(am, list, AppModeOptions.BICYCLE.routingParameters);
		} else if (am.equals(ApplicationMode.PEDESTRIAN)) {
			getAppModeItems(am, list, AppModeOptions.PEDESTRIAN.routingParameters);
		}
		return list;
	}

	public List<LocalRoutingParameter> getAppModeItems(ApplicationMode am, List<LocalRoutingParameter> list, List<String> routingParameters) {
		for (String itemId : routingParameters) {
			LocalRoutingParameter item = getItem(am, itemId);
			if (item != null) {
				list.add(item);
			}
		}
		return list;
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
			case AvoidRoadsTypesRoutingParameter.KEY:
				return new AvoidRoadsTypesRoutingParameter();
			case AvoidRoadsRoutingParameter.KEY:
				return new AvoidRoadsRoutingParameter();
			case GpxLocalRoutingParameter.KEY:
				return new GpxLocalRoutingParameter();
			case OtherSettingsRoutingParameter.KEY:
				return new OtherSettingsRoutingParameter();
			default:
				return getRoutingParametersInner(am, parameter);
		}
	}

	public LocalRoutingParameter getRoutingParametersInner(ApplicationMode am, String parameter) {
		RouteProvider.GPXRouteParamsBuilder rparams = app.getRoutingHelper().getCurrentGPXRoute();
		GeneralRouter rm = getRouter(app.getDefaultRoutingConfig(), am);
		if (rm == null || (rparams != null && !rparams.isCalculateOsmAndRoute()) && !rparams.getFile().hasRtePt()) {
			return null;
		}

		LocalRoutingParameter rp;
		Map<String, GeneralRouter.RoutingParameter> parameters = rm.getParameters();
		GeneralRouter.RoutingParameter routingParameter = parameters.get(parameter);

		if (routingParameter != null) {
			rp = new LocalRoutingParameter(am);
			rp.routingParameter = routingParameter;
		} else {
			LocalRoutingParameterGroup rpg = null;
			for (GeneralRouter.RoutingParameter r : rm.getParameters().values()) {
				if (r.getType() == GeneralRouter.RoutingParameterType.BOOLEAN
						&& !Algorithms.isEmpty(r.getGroup()) && r.getGroup().equals(parameter)) {
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
		list.add(new GpxLocalRoutingParameter());
		list.add(new OtherSettingsRoutingParameter());
		return list;
	}

	public List<LocalRoutingParameter> getRoutingParametersInner(ApplicationMode am) {
		final OsmandSettings settings = app.getSettings();
		List<LocalRoutingParameter> list = new ArrayList<LocalRoutingParameter>();
		RouteProvider.GPXRouteParamsBuilder rparams = app.getRoutingHelper().getCurrentGPXRoute();
		boolean osmandRouter = settings.ROUTER_SERVICE.getModeValue(am) == RouteProvider.RouteService.OSMAND;
		if (!osmandRouter) {
			list.add(new OtherLocalRoutingParameter(R.string.calculate_osmand_route_without_internet,
					app.getString(R.string.calculate_osmand_route_without_internet), settings.GPX_ROUTE_CALC_OSMAND_PARTS
					.get()));
			list.add(new OtherLocalRoutingParameter(R.string.fast_route_mode, app.getString(R.string.fast_route_mode),
					settings.FAST_ROUTE_MODE.get()));
			return list;
		}
		if (rparams != null) {
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
		GeneralRouter rm = SettingsNavigationActivity.getRouter(app.getDefaultRoutingConfig(), am);
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
					list.add(rp);
				}
			}
		}

		return list;
	}

	public static LocalRoutingParameterGroup getLocalRoutingParameterGroup(List<LocalRoutingParameter> list, String groupName) {
		for (LocalRoutingParameter p : list) {
			if (p instanceof LocalRoutingParameterGroup && groupName.equals(((LocalRoutingParameterGroup) p).getGroupName())) {
				return (LocalRoutingParameterGroup) p;
			}
		}
		return null;
	}

	public static class LocalRoutingParameter {

		public static final String KEY = "LocalRoutingParameter";

		public GeneralRouter.RoutingParameter routingParameter;

		private ApplicationMode am;

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
	}

	public static class DividerItem extends LocalRoutingParameter {

		public static final String KEY = "DividerItem";

		public DividerItem() {
			super(null);
		}
	}

	public static class RouteSimulationItem extends LocalRoutingParameter {

		public static final String KEY = "RouteSimulationItem";

		public RouteSimulationItem() {
			super(null);
		}
	}

	public static class ShowAlongTheRouteItem extends LocalRoutingParameter {

		public static final String KEY = "ShowAlongTheRouteItem";

		public ShowAlongTheRouteItem() {
			super(null);
		}
	}

	public static class AvoidRoadsRoutingParameter extends LocalRoutingParameter {

		public static final String KEY = "AvoidRoadsRoutingParameter";

		public AvoidRoadsRoutingParameter() {
			super(null);
		}

	}

	public static class AvoidRoadsTypesRoutingParameter extends LocalRoutingParameter {

		public static final String KEY = "AvoidRoadsTypesRoutingParameter";

		public AvoidRoadsTypesRoutingParameter() {
			super(null);
		}

	}

	public static class GpxLocalRoutingParameter extends LocalRoutingParameter {

		public static final String KEY = "GpxLocalRoutingParameter";

		public GpxLocalRoutingParameter() {
			super(null);
		}
	}

	public static class OtherSettingsRoutingParameter extends LocalRoutingParameter {

		public static final String KEY = "OtherSettingsRoutingParameter";

		public OtherSettingsRoutingParameter() {
			super(null);
		}
	}

	public static class OtherLocalRoutingParameter extends LocalRoutingParameter {

		public static final String KEY = "OtherLocalRoutingParameter";

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

		public InterruptMusicRoutingParameter() {
			super(null);
		}
	}

	public static class VoiceGuidanceRoutingParameter extends LocalRoutingParameter {

		public static final String KEY = "VoiceGuidanceRoutingParameter";

		public VoiceGuidanceRoutingParameter() {
			super(null);
		}
	}

	public enum AppModeOptions {

		CAR(MuteSoundRoutingParameter.KEY,
				DividerItem.KEY,
				AvoidRoadsRoutingParameter.KEY,
				ShowAlongTheRouteItem.KEY,
				GeneralRouter.ALLOW_PRIVATE,
				GeneralRouter.USE_SHORTEST_WAY,
				DividerItem.KEY,
				GpxLocalRoutingParameter.KEY,
				OtherSettingsRoutingParameter.KEY,
				RouteSimulationItem.KEY),

		BICYCLE(MuteSoundRoutingParameter.KEY,
				"driving_style",
				GeneralRouter.USE_HEIGHT_OBSTACLES,
				DividerItem.KEY,
				AvoidRoadsTypesRoutingParameter.KEY,
				ShowAlongTheRouteItem.KEY,
				DividerItem.KEY,
				GpxLocalRoutingParameter.KEY,
				OtherSettingsRoutingParameter.KEY,
				RouteSimulationItem.KEY),

		PEDESTRIAN(MuteSoundRoutingParameter.KEY,
				GeneralRouter.USE_HEIGHT_OBSTACLES,
				DividerItem.KEY,
				AvoidRoadsTypesRoutingParameter.KEY,
				ShowAlongTheRouteItem.KEY,
				DividerItem.KEY,
				GpxLocalRoutingParameter.KEY,
				OtherSettingsRoutingParameter.KEY,
				RouteSimulationItem.KEY);


		List<String> routingParameters;

		AppModeOptions(String... routingParameters) {
			this.routingParameters = Arrays.asList(routingParameters);
		}
	}
}