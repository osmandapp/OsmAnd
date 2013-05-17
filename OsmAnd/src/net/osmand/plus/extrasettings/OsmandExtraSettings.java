package net.osmand.plus.extrasettings;


import java.util.Arrays;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
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
		
		final OsmandPreference<Float> textSizePref = view.getSettings().MAP_TEXT_SIZE;
		final MapWidgetRegInfo textSize = mapInfoControls.registerAppearanceWidget(R.drawable.widget_text_size, R.string.map_text_size, 
				"text_size", textSizePref);
		textSize.setStateChangeListener(new Runnable() {
			@Override
			public void run() {
				final Float[] floatValues = new Float[] {0.6f, 0.8f, 1.0f, 1.2f, 1.5f, 1.75f, 2f};
				String[] entries = new String[floatValues.length];
				for (int i = 0; i < floatValues.length; i++) {
					entries[i] = (int) (floatValues[i] * 100) +" %";
				}
				Builder b = new AlertDialog.Builder(view.getContext());
				b.setTitle(R.string.map_text_size);
				int i = Arrays.binarySearch(floatValues, textSizePref.get());
				b.setSingleChoiceItems(entries, i, new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						textSizePref.set(floatValues[which]);
						app.getResourceManager().getRenderer().clearCache();
						view.refreshMap(true);
						dialog.dismiss();
					}
				});
				b.show();
			}
		});

		final MapWidgetRegInfo showRuler = mapInfoControls.registerAppearanceWidget(R.drawable.widget_ruler, R.string.map_widget_show_ruler, 
				"showRuler", view.getSettings().SHOW_RULER);
		showRuler.setStateChangeListener(new Runnable() {
			@Override
			public void run() {
				view.getSettings().SHOW_RULER.set(!view.getSettings().SHOW_RULER.get());
				view.refreshMap();
			}
		});
		
		final MapWidgetRegInfo showDestinationArrow = mapInfoControls.registerAppearanceWidget(R.drawable.widget_show_destination_arrow, R.string.map_widget_show_destination_arrow,
				"show_destination_arrow", view.getSettings().SHOW_DESTINATION_ARROW);
		showDestinationArrow.setStateChangeListener(new Runnable() {
			@Override
			public void run() {
				view.getSettings().SHOW_DESTINATION_ARROW.set(!view.getSettings().SHOW_DESTINATION_ARROW.get());
				mapInfoLayer.recreateControls();
			}
		});

		final MapWidgetRegInfo transparent = mapInfoControls.registerAppearanceWidget(R.drawable.widget_transparent_skin, R.string.map_widget_transparent,
				"transparent", view.getSettings().TRANSPARENT_MAP_THEME);
		transparent.setStateChangeListener(new Runnable() {
			@Override
			public void run() {
				view.getSettings().TRANSPARENT_MAP_THEME.set(!view.getSettings().TRANSPARENT_MAP_THEME.get());
				mapInfoLayer.recreateControls();
			}
		});

//		final MapInfoControlRegInfo posMap = mapInfoControls.registerAppearanceWidget(R.drawable.widget_position_marker, R.string.position_on_map, 
//				"position_on_map", textSizePref);
//		posMap.setStateChangeListener(new Runnable() {
//			@Override
//			public void run() {
//				String[]  entries = new String[] { activity.getString(R.string.position_on_map_center), activity.getString(R.string.position_on_map_bottom) };
//				final Integer[] vals = new Integer[] { OsmandSettings.CENTER_CONSTANT, OsmandSettings.BOTTOM_CONSTANT };
//				Builder b = new AlertDialog.Builder(view.getContext());
//				int i = Arrays.binarySearch(vals, posPref.get());
//				b.setSingleChoiceItems(entries, i, new OnClickListener() {
//					@Override
//					public void onClick(DialogInterface dialog, int which) {
//						posPref.set(vals[which]);
//						activity.updateApplicationModeSettings();
//						view.refreshMap(true);
//						dialog.dismiss();
//					}
//				});
//				b.show();
//			}
//		});
	}
	
	@Override
	public void updateLayers(final OsmandMapTileView view, MapActivity activity) {
		if(!registerControls) {
			registerLayers(activity);
		}
	}
	

}
