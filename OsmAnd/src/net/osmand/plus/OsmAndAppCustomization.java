package net.osmand.plus;

import android.view.Window;
import net.osmand.plus.api.SettingsAPI;

public class OsmAndAppCustomization {
	
	protected OsmandApplication app;

	public void setup(OsmandApplication app) {
		this.app = app;
	}
	
	public OsmandSettings createSettings(SettingsAPI api) {
		return new OsmandSettings(app, api);
	}

	public boolean checkExceptionsOnStart() {
		return true;
	}

	public boolean showFirstTimeRunAndTips(boolean firstTime, boolean appVersionChanged) {
		return true;
	}

	public boolean checkBasemapDownloadedOnStart() {
		return true;
	}

	public void customizeMainMenu(Window window) {
	}
}
