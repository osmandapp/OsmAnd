package net.osmand.plus.extrasettings;


import java.util.Arrays;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.OsmandSettings.OsmandPreference;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.views.MapInfoLayer;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.mapwidgets.MapWidgetRegistry;
import net.osmand.plus.views.mapwidgets.MapWidgetRegistry.MapWidgetRegInfo;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;

public class OsmandExtraSettings extends OsmandPlugin {
	private static final String ID = "osmand.extrasettings";
	private OsmandApplication app;
	private boolean registerControls;
	
	public OsmandExtraSettings(OsmandApplication app) {
		this.app = app;
	}
	
	@Override
	public boolean init(OsmandApplication app) {
		return true;
	}
	
	@Override
	public String getId() {
		return ID;
	}
	@Override
	public String getDescription() {
		return app.getString(R.string.osmand_extra_settings_description);
	}
	@Override
	public String getName() {
		return app.getString(R.string.extra_settings);
	}

	@Override
	public void registerLayers(final MapActivity activity) {
		registerControls = true;
		final OsmandMapTileView view = activity.getMapView();
		final MapInfoLayer mapInfoLayer = activity.getMapLayers().getMapInfoLayer();
		final MapWidgetRegistry mapInfoControls = mapInfoLayer.getMapInfoControls();
	}
	
	@Override
	public void updateLayers(final OsmandMapTileView view, MapActivity activity) {
		if(!registerControls) {
			registerLayers(activity);
		}
	}
	

}
