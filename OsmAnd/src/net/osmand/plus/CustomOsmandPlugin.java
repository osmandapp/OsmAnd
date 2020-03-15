package net.osmand.plus;

import org.json.JSONException;
import org.json.JSONObject;

public class CustomOsmandPlugin extends OsmandPlugin {

	public String pluginId;
	public String name;
	public String description;

	public CustomOsmandPlugin(OsmandApplication app) {
		super(app);
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
		return name;
	}

	@Override
	public String getDescription() {
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
		json.put("name", getName());
		json.put("Description", getDescription());

		return json.toString();
	}
}