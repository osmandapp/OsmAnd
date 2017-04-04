package net.osmand.plus.srtmplugin;

import android.app.Activity;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.ArrayAdapter;

import net.osmand.plus.ApplicationMode;
import net.osmand.plus.ContextMenuAdapter;
import net.osmand.plus.ContextMenuItem;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.OsmandSettings.CommonPreference;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.SettingsActivity;
import net.osmand.plus.dashboard.DashboardOnMap;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.render.RenderingRuleProperty;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SRTMPlugin extends OsmandPlugin {

	public static final String ID = "osmand.srtm";
	public static final String FREE_ID = "osmand.srtm.paid";

	public static final String CONTOUR_LINES_ATTR = "contourLines";
	public static final String CONTOUR_LINES_SCHEME_ATTR = "contourColorScheme";
	public static final String CONTOUR_LINES_DISABLED_VALUE = "disabled";

	private OsmandApplication app;
	private boolean paid;
	private HillshadeLayer hillshadeLayer;
	private CommonPreference<Boolean> HILLSHADE;
	
	@Override
	public String getId() {
		return paid ? ID : FREE_ID;
	}

	public SRTMPlugin(OsmandApplication app) {
		this.app = app;
		HILLSHADE = app.getSettings().registerBooleanPreference("hillshade_layer", true);
	}
	
	@Override
	public int getLogoResourceId() {
		return R.drawable.ic_plugin_srtm;
	}
	
	@Override
	public int getAssetResourceName() {
		return R.drawable.contour_lines;
	}
	

	@Override
	public String getDescription() {
		return app.getString(R.string.srtm_plugin_description);
	}

	@Override
	public String getName() {
		return app.getString(R.string.srtm_plugin_name);
	}

	@Override
	public String getHelpFileName() {
		return "feature_articles/contour-lines-plugin.html";
	}
	@Override
	public boolean init(final OsmandApplication app, Activity activity) {
		OsmandSettings settings = app.getSettings();
		CommonPreference<String> pref = settings.getCustomRenderProperty("contourLines");
		if(pref.get().equals("")) {
			for(ApplicationMode m : ApplicationMode.allPossibleValues()) {
				if(pref.getModeValue(m).equals("")) {
					pref.setModeValue(m, "13");
				}
			}
		}
		return true;
	}

	@Override
	public void registerLayers(MapActivity activity) {
		if (hillshadeLayer != null) {
			activity.getMapView().removeLayer(hillshadeLayer);
		}
		if (HILLSHADE.get()) {
			hillshadeLayer = new HillshadeLayer(activity, this);
			activity.getMapView().addLayer(hillshadeLayer, 0.6f);
		}
	}

	public boolean isHillShadeLayerEnabled() {
		return HILLSHADE.get();
	}

	@Override
	public void updateLayers(OsmandMapTileView mapView, MapActivity activity) {
		if (HILLSHADE.get() && isActive()) {
			if (hillshadeLayer == null) {
				registerLayers(activity);
			}
		} else {
			if (hillshadeLayer != null) {
				mapView.removeLayer(hillshadeLayer);
				hillshadeLayer = null;
				activity.refreshMap();
			}
		}
	}
	
	@Override
	public void registerLayerContextMenuActions(final OsmandMapTileView mapView, ContextMenuAdapter adapter, final MapActivity mapActivity) {
		ContextMenuAdapter.ItemClickListener listener = new ContextMenuAdapter.OnRowItemClick() {

			@Override
			public boolean onRowItemClick(ArrayAdapter<ContextMenuItem> adapter, View view, int itemId, int position) {
				if (itemId == R.string.srtm_plugin_name) {
					mapActivity.getDashboard().setDashboardVisibility(true, DashboardOnMap.DashboardType.CONTOUR_LINES);
					return false;
				}
				return true;
			}

			@Override
			public boolean onContextMenuClick(ArrayAdapter<ContextMenuItem> adapter, int itemId, int position, boolean isChecked) {
				if (itemId == R.string.srtm_plugin_name) {
					toggleContourLines(mapActivity, adapter, itemId, position, isChecked);
					adapter.notifyDataSetChanged();
				} else if (itemId == R.string.layer_hillshade) {
					HILLSHADE.set(!HILLSHADE.get());
					adapter.getItem(position).setColorRes(HILLSHADE.get() ? R.color.osmand_orange : ContextMenuItem.INVALID_ID);
					adapter.notifyDataSetChanged();
					updateLayers(mapView, mapActivity);
				}
				return true;
			}
		};

		//app.getSettings().CONTOUR_LINES_ZOOM.set(null);
		RenderingRuleProperty contourLinesProp = app.getRendererRegistry().getCustomRenderingRuleProperty(CONTOUR_LINES_ATTR);
		if (contourLinesProp != null) {
			final OsmandSettings.CommonPreference<String> pref =
					app.getSettings().getCustomRenderProperty(contourLinesProp.getAttrName());
			String descr;
			boolean contourLinesSelected;
			if (!Algorithms.isEmpty(pref.get())) {
				descr = SettingsActivity.getStringPropertyValue(mapActivity, pref.get());
				contourLinesSelected = !pref.get().equals(CONTOUR_LINES_DISABLED_VALUE);
			} else {
				descr = SettingsActivity.getStringPropertyValue(
						mapActivity, contourLinesProp.getDefaultValueDescription());
				contourLinesSelected = !contourLinesProp.getDefaultValueDescription().equals(CONTOUR_LINES_DISABLED_VALUE);
			}
			adapter.addItem(new ContextMenuItem.ItemBuilder()
					.setTitleId(R.string.srtm_plugin_name, mapActivity)
					.setSelected(contourLinesSelected)
					.setIcon(R.drawable.ic_plugin_srtm)
					.setDescription(app.getString(R.string.display_zoom_level, descr))
					.setColor(contourLinesSelected ? R.color.osmand_orange : ContextMenuItem.INVALID_ID)
					.setSecondaryIcon(R.drawable.ic_action_additional_option)
					.setListener(listener).createItem());
		}
		adapter.addItem(new ContextMenuItem.ItemBuilder()
				.setTitleId(R.string.layer_hillshade, mapActivity)
				.setSelected(HILLSHADE.get())
				.setColor(HILLSHADE.get() ? R.color.osmand_orange : ContextMenuItem.INVALID_ID)
				.setIcon(R.drawable.ic_action_hillshade_dark)
				.setListener(listener)
				.setPosition(13)
				.createItem());
	}

	private void toggleContourLines(final MapActivity activity,
									final ArrayAdapter<ContextMenuItem> adapter, final int itemId,
									final int pos, final boolean isChecked) {
		RenderingRuleProperty contourLinesProp = app.getRendererRegistry().getCustomRenderingRuleProperty(CONTOUR_LINES_ATTR);
		if (contourLinesProp != null) {
			OsmandSettings settings = app.getSettings();
			final OsmandSettings.CommonPreference<String> pref =
					settings.getCustomRenderProperty(contourLinesProp.getAttrName());
			CommonPreference<String> zoomSetting = settings.CONTOUR_LINES_ZOOM;
			if (!isChecked) {
				zoomSetting.set(pref.get());
				pref.set(CONTOUR_LINES_DISABLED_VALUE);
				ContextMenuItem item = adapter.getItem(pos);
				if (item != null) {
					item.setDescription(app.getString(R.string.display_zoom_level,
							SettingsActivity.getStringPropertyValue(app, pref.get())));
					item.setColorRes(ContextMenuItem.INVALID_ID);
				}
			} else if (!Algorithms.isEmpty(zoomSetting.get())) {
				pref.set(zoomSetting.get());
				ContextMenuItem item = adapter.getItem(pos);
				if (item != null) {
					item.setDescription(app.getString(R.string.display_zoom_level,
							SettingsActivity.getStringPropertyValue(app, pref.get())));
					item.setColorRes(R.color.osmand_orange);
				}
			} else {
				selectZoomLevel(activity, adapter, contourLinesProp, pref, pos);
			}
		}
	}

	private void selectZoomLevel(final MapActivity activity,
								 final ArrayAdapter<ContextMenuItem> adapter,
								 final RenderingRuleProperty p,
								 final OsmandSettings.CommonPreference<String> pref,
								 final int pos) {
		final String propertyDescr = SettingsActivity.getStringPropertyDescription(activity,
				p.getAttrName(), p.getName());
		AlertDialog.Builder b = new AlertDialog.Builder(activity);
		// test old descr as title
		b.setTitle(propertyDescr);

		List<String> possibleValuesList = new ArrayList<>(Arrays.asList(p.getPossibleValues()));
		possibleValuesList.remove(CONTOUR_LINES_DISABLED_VALUE);
		final String[] possibleValues = possibleValuesList.toArray(new String[possibleValuesList.size()]);

		int i = possibleValuesList.indexOf(pref.get());
		if (i >= 0) {
			i++;
		} else if (Algorithms.isEmpty(pref.get())) {
			i = 0;
		}

		String[] possibleValuesString = new String[possibleValues.length + 1];
		possibleValuesString[0] = SettingsActivity.getStringPropertyValue(activity,
				p.getDefaultValueDescription());

		for (int j = 0; j < possibleValues.length; j++) {
			possibleValuesString[j + 1] = SettingsActivity.getStringPropertyValue(activity,
					possibleValues[j]);
		}

		b.setSingleChoiceItems(possibleValuesString, i, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				if (which == 0) {
					pref.set("");
				} else {
					pref.set(possibleValues[which - 1]);
				}
				ContourLinesMenu.refreshMapComplete(activity);
				dialog.dismiss();
			}
		});
		b.setNegativeButton(R.string.shared_string_dismiss, null);
		b.setOnDismissListener(new DialogInterface.OnDismissListener() {
			@Override
			public void onDismiss(DialogInterface dialog) {
				ContextMenuItem item = adapter.getItem(pos);
				if (item != null) {
					String description = activity.getString(R.string.display_zoom_level,
							SettingsActivity.getStringPropertyValue(activity, pref.get()));
					item.setDescription(description);
					if (pref.get().equals(CONTOUR_LINES_DISABLED_VALUE)) {
						item.setSelected(false);
						item.setColorRes(ContextMenuItem.INVALID_ID);
					} else {
						item.setSelected(true);
						item.setColorRes(R.color.osmand_orange);
					}
				}
				adapter.notifyDataSetChanged();
			}
		});
		b.show();
	}

	@Override
	public void disable(OsmandApplication app) {
	}
	
	@Override
	public Class<? extends Activity> getSettingsActivity() {
		return null;
	}

}
