package net.osmand.plus.profiles.data;

import static net.osmand.plus.profiles.data.RoutingProfilesResources.BROUTER_MODE;
import static net.osmand.plus.profiles.data.RoutingProfilesResources.DIRECT_TO_MODE;
import static net.osmand.plus.profiles.data.RoutingProfilesResources.STRAIGHT_LINE_MODE;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.CallbackWithObject;
import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.onlinerouting.OnlineRoutingHelper;
import net.osmand.plus.onlinerouting.engine.OnlineRoutingEngine;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.utils.AndroidUtils;
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

	private static final Log LOG = PlatformUtil.getLog(RoutingDataUtils.class);

	public static final String DOWNLOAD_ENGINES_URL = "https://osmand.net/online-routing-providers.json";

	public static final String OSMAND_NAVIGATION = "osmand_navigation";

	public static final String DERIVED_PROFILES = "derivedProfiles";
	public static final String GEOCODING = "geocoding";
	public static final String PROVIDERS = "providers";
	public static final String DEFAULT = "default";
	public static final String ROUTES = "routes";
	public static final String NAME = "name";
	public static final String TYPE = "type";
	public static final String URL = "url";

	private final OsmandApplication app;

	public RoutingDataUtils(OsmandApplication app) {
		this.app = app;
	}

	public List<ProfilesGroup> getOfflineProfiles() {
		List<ProfilesGroup> result = new ArrayList<>();
		Map<String, RoutingFile> routingFiles = getOfflineRoutingFilesByNames();

		ProfilesGroup profilesGroup = createProfilesGroup(getString(R.string.app_name_osmand), routingFiles.remove(OSMAND_NAVIGATION));
		if (profilesGroup != null) {
			result.add(profilesGroup);
		}
		for (Map.Entry<String, RoutingFile> entry : routingFiles.entrySet()) {
			profilesGroup = createProfilesGroup(entry.getKey(), entry.getValue());
			if (profilesGroup != null) {
				result.add(profilesGroup);
			}
		}
		result.add(new ProfilesGroup(getString(R.string.shared_string_external), getExternalRoutingProfiles()));
		sortItems(result);
		return result;
	}

	@Nullable
	private ProfilesGroup createProfilesGroup(@NonNull String title, @Nullable RoutingFile file) {
		if (file != null) {
			return new ProfilesGroup(title, file.getProfiles());
		}
		return null;
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

	public RoutingProfilesHolder getRoutingProfiles() {
		List<RoutingDataObject> profiles = new ArrayList<>();
		profiles.addAll(getOfflineRoutingProfiles());
		profiles.addAll(getExternalRoutingProfiles());
		profiles.addAll(getOnlineRoutingProfiles(false));

		RoutingProfilesHolder result = new RoutingProfilesHolder();
		for (RoutingDataObject profile : profiles) {
			result.add(profile);
		}
		return result;
	}

	private Map<String, RoutingFile> getOfflineRoutingFilesByNames() {
		Map<String, RoutingFile> map = new HashMap<>();
		for (RoutingDataObject profile : getOfflineRoutingProfiles()) {
			String fileName = profile.getFileName();
			if (fileName == null) {
				fileName = OSMAND_NAVIGATION;
			}
			RoutingFile file = map.get(fileName);
			if (file == null) {
				file = new RoutingFile(fileName);
				map.put(fileName, file);
			}
			file.addProfile(profile);
		}
		return map;
	}

	private List<RoutingDataObject> getOfflineRoutingProfiles() {
		List<RoutingDataObject> result = new ArrayList<>();
		result.add(new RoutingDataObject(STRAIGHT_LINE_MODE.name(),
				getString(STRAIGHT_LINE_MODE.getStringRes()),
				getString(R.string.special_routing_type),
				STRAIGHT_LINE_MODE.getIconRes(),
				false, null, null));
		result.add(new RoutingDataObject(DIRECT_TO_MODE.name(),
				getString(DIRECT_TO_MODE.getStringRes()),
				getString(R.string.special_routing_type),
				DIRECT_TO_MODE.getIconRes(),
				false, null, null));

		List<String> disabledRouterNames = PluginsHelper.getDisabledRouterNames();
		for (RoutingConfiguration.Builder builder : app.getAllRoutingConfigs()) {
			for (Map.Entry<String, GeneralRouter> entry : builder.getAllRouters().entrySet()) {
				String routerKey = entry.getKey();
				GeneralRouter router = entry.getValue();
				if (!Algorithms.objectEquals(routerKey, GEOCODING) && !disabledRouterNames.contains(router.getFilename())) {
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
					RoutingDataObject data = new RoutingDataObject(routerKey, name, description, iconRes, false, fileName, null);
					result.add(data);
					String sDerivedProfiles = router.getAttribute(DERIVED_PROFILES);
					if (!Algorithms.isEmpty(sDerivedProfiles)) {
						for (String derivedProfile : sDerivedProfiles.split(",")) {
							if (Algorithms.objectEquals(derivedProfile, DEFAULT)) {
								continue;
							}
							result.add(createDerivedProfile(data, derivedProfile));
						}
					}
				}
			}
		}
		return result;
	}

	private RoutingDataObject createDerivedProfile(@NonNull RoutingDataObject original, @NonNull String derivedProfile) {
		String translationKey = "app_mode_" + derivedProfile;
		String localizedProfileName = AndroidUtils.getStringByProperty(app, translationKey);
		String name = translationKey.equals(localizedProfileName) ? Algorithms.capitalizeFirstLetter(translationKey) : localizedProfileName;
		int iconRes = getIconResForDerivedProfile(derivedProfile);
		return new RoutingDataObject(
				original.getStringKey(), name, original.getDescription(),
				iconRes, original.isSelected(), original.getFileName(),
				derivedProfile
		);
	}

	private int getIconResForDerivedProfile(@NonNull String derivedProfile) {
		String imageKey = "ic_action_" + derivedProfile;
		int iconId = AndroidUtils.getDrawableId(app, imageKey);
		if (iconId != 0) {
			return iconId;
		}
		// We need to check twice for legacy reasons: some icons have the _dark suffix
		iconId = AndroidUtils.getDrawableId(app, imageKey + "_dark");
		if (iconId != 0) {
			return iconId;
		}
		return R.drawable.ic_action_gdirections_dark;
	}

	private List<RoutingDataObject> getExternalRoutingProfiles() {
		List<RoutingDataObject> result = new ArrayList<>();
		if (app.getBRouterService() != null) {
			result.add(new RoutingDataObject(BROUTER_MODE.name(),
					getString(BROUTER_MODE.getStringRes()),
					getString(R.string.third_party_routing_type),
					BROUTER_MODE.getIconRes(),
					false, null, null));
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

	private List<RoutingDataObject> getOnlineRoutingProfiles(boolean onlyCustom) {
		OnlineRoutingHelper helper = app.getOnlineRoutingHelper();
		List<RoutingDataObject> objects = new ArrayList<>();
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

	public void downloadPredefinedEngines(CallbackWithObject<String> callback) {
		new Thread(() -> {
			String content = null;
			try {
				content = app.getOnlineRoutingHelper().makeRequest(DOWNLOAD_ENGINES_URL);
			} catch (IOException e) {
				LOG.error("Error trying download predefined routing engines list: " + e.getMessage());
			}
			String result = content;
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
			List<RoutingDataObject> engines = new ArrayList<>();
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
			List<RoutingDataObject> profiles = group.getProfiles();
			if (!Algorithms.isEmpty(profiles)) {
				Collections.sort(profiles);
			}
		}
	}

}
