package net.osmand.plus.profiles.data;

import androidx.annotation.Nullable;

import net.osmand.CallbackWithObject;
import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.onlinerouting.OnlineRoutingHelper;
import net.osmand.plus.onlinerouting.engine.OnlineRoutingEngine;
import net.osmand.plus.profiles.data.RoutingDataObject.RoutingProfilesResources;
import net.osmand.router.GeneralRouter;
import net.osmand.router.RoutingConfiguration;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RoutingDataUtils {

	private final static Log LOG = PlatformUtil.getLog(RoutingDataUtils.class);

	public static final String DOWNLOAD_ENGINES_URL = "https://osmand.net/online-routing-providers.json";

	public static final String OSMAND_NAVIGATION = "osmand_navigation";

	public static final String PROVIDERS = "providers";
	public static final String ROUTES = "routes";
	public static final String NAME = "name";
	public static final String TYPE = "type";
	public static final String URL = "url";

	private OsmandApplication app;

	public RoutingDataUtils(OsmandApplication app) {
		this.app = app;
	}

	public List<ProfilesGroup> getOfflineProfiles() {
		List<ProfilesGroup> result = new ArrayList<>();
		Map<String, List<ProfileDataObject>> byFileNames = getOfflineRoutingProfilesByFileNames();
		result.add(new ProfilesGroup(getString(R.string.app_name_osmand), byFileNames.get(OSMAND_NAVIGATION)));
		for (String key : byFileNames.keySet()) {
			if (!key.equals(OSMAND_NAVIGATION)) {
				result.add(new ProfilesGroup(key, byFileNames.get(key)));
			}
		}
		result.add(new ProfilesGroup(getString(R.string.shared_string_external), getExternalRoutingProfiles()));
		sortItems(result);
		return result;
	}

	public List<ProfilesGroup> getOnlineProfiles(@Nullable List<ProfilesGroup> predefined) {
		List<ProfilesGroup> result = new ArrayList<>();
		if (!Algorithms.isEmpty(predefined)) {
			result.addAll(predefined);
		}
		result.add(new ProfilesGroup(getString(R.string.shared_string_custom), getOnlineRoutingProfiles(true)));
		sortItems(result);
		return result;
	}

	public Map<String, ProfileDataObject> getRoutingProfiles() {
		List<ProfileDataObject> profiles = new ArrayList<>();
		profiles.addAll(getOfflineRoutingProfiles());
		profiles.addAll(getExternalRoutingProfiles());
		profiles.addAll(getOnlineRoutingProfiles(false));

		Map<String, ProfileDataObject> result = new HashMap<>();
		for (ProfileDataObject onlineEngine : profiles) {
			result.put(onlineEngine.getStringKey(), onlineEngine);
		}
		return result;
	}

	private Map<String, List<ProfileDataObject>> getOfflineRoutingProfilesByFileNames() {
		Map<String, List<ProfileDataObject>> result = new HashMap<>();
		for (final ProfileDataObject profile : getOfflineRoutingProfiles()) {
			String fileName = null;
			if (profile instanceof RoutingDataObject) {
				fileName = ((RoutingDataObject) profile).getFileName();
			}
			fileName = fileName != null ? fileName : OSMAND_NAVIGATION;
			if (result.containsKey(fileName)) {
				result.get(fileName).add(profile);
			} else {
				result.put(fileName, new ArrayList<ProfileDataObject>() {
					{ add(profile); }
				});
			}
		}
		return result;
	}

	private List<ProfileDataObject> getOfflineRoutingProfiles() {
		List<ProfileDataObject> result = new ArrayList<>();
		result.add(new RoutingDataObject(
				RoutingProfilesResources.STRAIGHT_LINE_MODE.name(),
				getString(RoutingProfilesResources.STRAIGHT_LINE_MODE.getStringRes()),
				getString(R.string.special_routing_type),
				RoutingProfilesResources.STRAIGHT_LINE_MODE.getIconRes(),
				false, null));
		result.add(new RoutingDataObject(
				RoutingProfilesResources.DIRECT_TO_MODE.name(),
				getString(RoutingProfilesResources.DIRECT_TO_MODE.getStringRes()),
				getString(R.string.special_routing_type),
				RoutingProfilesResources.DIRECT_TO_MODE.getIconRes(),
				false, null));

		List<String> disabledRouterNames = OsmandPlugin.getDisabledRouterNames();
		for (RoutingConfiguration.Builder builder : app.getAllRoutingConfigs()) {
			for (Map.Entry<String, GeneralRouter> entry : builder.getAllRouters().entrySet()) {
				String routerKey = entry.getKey();
				GeneralRouter router = entry.getValue();
				if (!routerKey.equals("geocoding") && !disabledRouterNames.contains(router.getFilename())) {
					int iconRes = R.drawable.ic_action_gdirections_dark;
					String name = router.getProfileName();
					String fileName = router.getFilename();
					String description = getString(R.string.osmand_default_routing);
					if (!Algorithms.isEmpty(fileName)) {
						description = fileName;
					} else if (RoutingProfilesResources.isRpValue(name.toUpperCase())) {
						iconRes = RoutingProfilesResources.valueOf(name.toUpperCase()).getIconRes();
						name = getString(RoutingProfilesResources.valueOf(name.toUpperCase()).getStringRes());
					}
					result.add(new RoutingDataObject(routerKey, name, description, iconRes, false, fileName));
				}
			}
		}
		return result;
	}

	private List<ProfileDataObject> getExternalRoutingProfiles() {
		List<ProfileDataObject> result = new ArrayList<>();
		if (app.getBRouterService() != null) {
			result.add(new RoutingDataObject(
					RoutingProfilesResources.BROUTER_MODE.name(),
					getString(RoutingProfilesResources.BROUTER_MODE.getStringRes()),
					getString(R.string.third_party_routing_type),
					RoutingProfilesResources.BROUTER_MODE.getIconRes(),
					false, null));
		}
		return result;
	}

	@Nullable
	public ProfileDataObject getOnlineEngineByKey(String stringKey) {
		OnlineRoutingHelper helper = app.getOnlineRoutingHelper();
		OnlineRoutingEngine engine = helper.getEngineByKey(stringKey);
		if (engine != null) {
			return convertOnlineEngineToDataObject(engine);
		}
		return null;
	}

	private List<ProfileDataObject> getOnlineRoutingProfiles(boolean onlyCustom) {
		OnlineRoutingHelper helper = app.getOnlineRoutingHelper();
		List<ProfileDataObject> objects = new ArrayList<>();
		List<OnlineRoutingEngine> engines = onlyCustom ? helper.getOnlyCustomEngines() : helper.getEngines();
		for (int i = 0; i < engines.size(); i++) {
			OnlineRoutingDataObject profile = convertOnlineEngineToDataObject(engines.get(i));
			profile.setOrder(i);
			objects.add(profile);
		}
		return objects;
	}

	private OnlineRoutingDataObject convertOnlineEngineToDataObject(OnlineRoutingEngine engine) {
		return new OnlineRoutingDataObject(engine.getName(app),
				engine.getBaseUrl(), engine.getStringKey(), R.drawable.ic_world_globe_dark);
	}

	public void downloadPredefinedEngines(final CallbackWithObject<String> callback) {
		new Thread(() -> {
			String content = null;
			try {
				content = app.getOnlineRoutingHelper().makeRequest(DOWNLOAD_ENGINES_URL);
			} catch (IOException e) {
				LOG.error("Error trying download predefined routing engines list: " + e.getMessage());
			}
			final String result = content;
			app.runInUIThread(() -> callback.processResult(result));
		}).start();
	}

	public List<ProfilesGroup> parsePredefinedEngines(String content) {
		try {
			return parsePredefinedEnginesImpl(content);
		} catch (JSONException e) {
			LOG.error("Error trying parse JSON: " + e.getMessage());
		}
		return null;
	}

	private List<ProfilesGroup> parsePredefinedEnginesImpl(String content) throws JSONException {
		JSONObject root = new JSONObject(content);
		JSONArray providers = root.getJSONArray(PROVIDERS);
		List<ProfilesGroup> result = new ArrayList<>();
		for (int i = 0; i < providers.length(); i++) {
			JSONObject groupObject = providers.getJSONObject(i);
			String providerName = groupObject.getString(NAME);
			String providerType = groupObject.getString(TYPE);
			String providerUrl = groupObject.getString(URL);
			JSONArray items = groupObject.getJSONArray(ROUTES);
			List<ProfileDataObject> engines = new ArrayList<>();
			for (int j = 0; j < items.length(); j++) {
				JSONObject item = items.getJSONObject(j);
				String engineName = item.getString(NAME);
				String engineUrl = item.getString(URL);
				int iconRes = R.drawable.ic_world_globe_dark;
				String type = item.getString(TYPE).toUpperCase();
				if (RoutingProfilesResources.isRpValue(type)) {
					iconRes = RoutingProfilesResources.valueOf(type).getIconRes();
					engineName = getString(RoutingProfilesResources.valueOf(type).getStringRes());
				}
				String key = OnlineRoutingEngine.generatePredefinedKey(providerName, type);
				OnlineRoutingDataObject engine = new OnlineRoutingDataObject(engineName, engineUrl, key, iconRes);
				engines.add(engine);
			}
			ProfilesGroup group = new PredefinedProfilesGroup(providerName, providerType, engines);
			group.setDescription(providerUrl);
			result.add(group);
		}
		return result;
	}

	private String getString(int id) {
		return app.getString(id);
	}

	private static void sortItems(List<ProfilesGroup> groups) {
		for (ProfilesGroup group : groups) {
			List<ProfileDataObject> profiles = group.getProfiles();
			if (!Algorithms.isEmpty(profiles)) {
				Collections.sort(profiles);
			}
		}
	}

}
