package net.osmand.plus.helpers;

import net.osmand.StateChangedListener;
import net.osmand.plus.OsmandApplication;

public class LocaleHelper implements StateChangedListener<String> {

	private boolean needRestart = false;

	private OsmandApplication app;

	public LocaleHelper(OsmandApplication app) {
		this.app = app;
	}

	public void listenLocaleChanges() {
		app.getSettings().PREFERRED_LOCALE.addListener(this);
	}

	public void stopListeningLocaleChanges() {
		app.getSettings().PREFERRED_LOCALE.removeListener(this);
	}

	public boolean needRestart() {
		return needRestart;
	}

	public void setNeedRestart(boolean needRestart) {
		this.needRestart = needRestart;
	}

	@Override
	public void stateChanged(String change) {
		needRestart = true;
		app.checkPreferredLocale();
	}
}
