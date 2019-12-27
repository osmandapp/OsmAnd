package net.osmand.plus.activities.actions;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.views.OsmandMapTileView;
import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;

public class OsmAndAction {

	protected MapActivity mapActivity;

	public OsmAndAction(MapActivity mapActivity) {
		this.mapActivity = mapActivity;
		OsmAndDialogs.registerDialogAction(this);
	}
	
	public MapActivity getMapActivity() {
		return mapActivity;
	}
	
	public OsmandMapTileView getMapView() {
		return mapActivity.getMapView();
	}
	
	public OsmandSettings getSettings() {
		return mapActivity.getMyApplication().getSettings();
	}
	
	protected OsmandApplication getMyApplication() {
		return mapActivity.getMyApplication();
	}
	
	protected String getString(int res) {
		return mapActivity.getString(res);
	}

	
	public void run() {
	}
	
	public int getDialogID() {
		return 0;
	}
	
	public Dialog createDialog(Activity activity, Bundle args) {
		return null;
	}
	
	public void prepareDialog(Activity activity, Bundle args, Dialog dlg) {
	}

	public void showDialog() {
		mapActivity.showDialog(getDialogID());		
	}
	
	public boolean isNightMode() {
		return mapActivity.getMyApplication().getDaynightHelper().isNightModeForMapControls();
	}
	
	public int getThemeRes() {
		return isNightMode() ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme;
	}
}
