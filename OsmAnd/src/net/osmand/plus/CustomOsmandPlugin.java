package net.osmand.plus;

import android.content.res.Configuration;

import androidx.annotation.NonNull;

import net.osmand.PlatformUtil;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class CustomOsmandPlugin extends OsmandPlugin {

	private static final Log LOG = PlatformUtil.getLog(CustomOsmandPlugin.class);

	public String pluginId;
	public Map<String, String> names = new HashMap<>();
	public Map<String, String> descriptions = new HashMap<>();

	public List<String> rendererNames = new ArrayList<>();
	public List<String> routerNames = new ArrayList<>();

	public CustomOsmandPlugin(@NonNull OsmandApplication app, @NonNull JSONObject json) throws JSONException {
		super(app);
		pluginId = json.getString("pluginId");

		JSONObject nameJson = json.getJSONObject("name");
		if (nameJson != null) {
			for (Iterator<String> it = nameJson.keys(); it.hasNext(); ) {
				String localeKey = it.next();
				String name = nameJson.getString(localeKey);
				names.put(localeKey, name);
			}
		}
		JSONObject descriptionJson = json.getJSONObject("description");
		if (descriptionJson != null) {
			for (Iterator<String> it = descriptionJson.keys(); it.hasNext(); ) {
				String localeKey = it.next();
				String name = descriptionJson.getString(localeKey);
				descriptions.put(localeKey, name);
			}
		}
	}

//	Prepare ".opr" desert-package manually + add all resources inside (extend json to describe package).
//
//Desert package
//1. Add to Plugins list
//1.1 Description / image / icon / name
//1.2 Enable description bottom sheet on Install
//2. Add custom rendering style to list Configure Map
//3. Include Special profile for navigation with selected style
//4. Add custom navigation icon (as example to use another car)
//
//P.S.: Functionality similar to Nautical / Ski Maps plugin,
// so we could remove all code for Nautical / Ski Maps from OsmAnd
// and put to separate "skimaps.opr", "nautical.opr" in future

	@Override
	public String getId() {
		return pluginId;
	}

	@Override
	public String getName() {
		Configuration config = app.getResources().getConfiguration();
		String lang = config.locale.getLanguage();
		String name = names.get(lang);
		if (Algorithms.isEmpty(name)) {
			name = names.get("");
		}
		if (Algorithms.isEmpty(name)) {
			name = app.getString(R.string.custom_osmand_plugin);
		}
		return name;
	}

	@Override
	public String getDescription() {
		Configuration config = app.getResources().getConfiguration();
		String lang = config.locale.getLanguage();
		String description = descriptions.get(lang);
		if (Algorithms.isEmpty(description)) {
			description = descriptions.get("");
		}
		return description;
	}

	@Override
	public int getAssetResourceName() {
		return R.drawable.contour_lines;
	}

	@Override
	public int getLogoResourceId() {
		return R.drawable.ic_action_skiing;
	}

	public String toJson() throws JSONException {
		JSONObject json = new JSONObject();

		json.put("type", SettingsHelper.SettingsItemType.PLUGIN.name());
		json.put("pluginId", getId());

		JSONObject nameJson = new JSONObject();
		for (Map.Entry<String, String> entry : names.entrySet()) {
			nameJson.put(entry.getKey(), entry.getValue());
		}
		json.put("name", nameJson);

		JSONObject descriptionJson = new JSONObject();
		for (Map.Entry<String, String> entry : descriptions.entrySet()) {
			descriptionJson.put(entry.getKey(), entry.getValue());
		}
		json.put("description", descriptionJson);

		return json.toString();
	}

	@Override
	public List<String> getRendererNames() {
		return rendererNames;
	}

	@Override
	public List<String> getRouterNames() {
		return routerNames;
	}
}