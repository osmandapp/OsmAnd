package net.osmand.plus;

public class CustomOsmandPlugin extends OsmandPlugin {

	private String pluginId;
	private String name;
	private String description;

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
		return 0;
	}

	@Override
	public int getLogoResourceId() {
		return R.drawable.ic_action_skiing;
	}
}