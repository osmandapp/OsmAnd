package net.osmand.plus.srtmplugin;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.ArrayAdapter;

import net.osmand.AndroidUtils;
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
import net.osmand.plus.inapp.InAppPurchaseHelper;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.render.RenderingRuleProperty;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.CONTOUR_LINES;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.HILLSHADE_LAYER;

public class SRTMPlugin extends OsmandPlugin {

	public static final String ID = "osmand.srtm";
	public static final String FREE_ID = "osmand.srtm.paid";

	public static final String CONTOUR_LINES_ATTR = "contourLines";
	public static final String CONTOUR_LINES_SCHEME_ATTR = "contourColorScheme";
	public static final String CONTOUR_LINES_DISABLED_VALUE = "disabled";
	public static final String CONTOUR_WIDTH_ATTR = "contourWidth";
	public static final String CONTOUR_DENSITY_ATTR = "contourDensity";

	private OsmandApplication app;
	private OsmandSettings settings;

	private boolean paid;
	private HillshadeLayer hillshadeLayer;

	@Override
	public String getId() {
		return paid ? ID : FREE_ID;
	}

	public SRTMPlugin(OsmandApplication app) {
		this.app = app;
		settings = app.getSettings();
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
	public boolean needsInstallation() {
		return super.needsInstallation() && !InAppPurchaseHelper.isSubscribedToLiveUpdates(app);
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
	public boolean init(@NonNull final OsmandApplication app, Activity activity) {
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
		if (settings.HILLSHADE.get()) {
			hillshadeLayer = new HillshadeLayer(activity, this);
			activity.getMapView().addLayer(hillshadeLayer, 0.6f);
		}
	}

	public boolean isHillShadeLayerEnabled() {
		return settings.HILLSHADE.get();
	}

	public static boolean isContourLinesLayerEnabled(OsmandApplication app) {
		boolean contourLinesEnabled = false;

		RenderingRuleProperty contourLinesProp = app.getRendererRegistry().getCustomRenderingRuleProperty(CONTOUR_LINES_ATTR);
		if (contourLinesProp != null) {
			final OsmandSettings.CommonPreference<String> pref = app.getSettings().getCustomRenderProperty(contourLinesProp.getAttrName());
			if (!Algorithms.isEmpty(pref.get())) {
				contourLinesEnabled = !pref.get().equals(CONTOUR_LINES_DISABLED_VALUE);
			} else {
				contourLinesEnabled = !contourLinesProp.getDefaultValueDescription().equals(CONTOUR_LINES_DISABLED_VALUE);
			}
		}
		return contourLinesEnabled;
	}

	@Override
	public void updateLayers(OsmandMapTileView mapView, MapActivity activity) {
		if (settings.HILLSHADE.get() && isActive()) {
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
				int[] viewCoordinates = AndroidUtils.getCenterViewCoordinates(view);
				if (itemId == R.string.srtm_plugin_name) {
					mapActivity.getDashboard().setDashboardVisibility(true, DashboardOnMap.DashboardType.CONTOUR_LINES, viewCoordinates);
					return false;
				} else if (itemId == R.string.layer_hillshade) {
					mapActivity.getDashboard().setDashboardVisibility(true, DashboardOnMap.DashboardType.HILLSHADE, viewCoordinates);
					return false;
				}
				return true;
			}

			@Override
			public boolean onContextMenuClick(final ArrayAdapter<ContextMenuItem> adapter,
											  final int itemId,
											  final int position,
											  final boolean isChecked,
											  final int[] viewCoordinates) {
				if (itemId == R.string.srtm_plugin_name) {
					toggleContourLines(mapActivity, isChecked, new Runnable() {
						@Override
						public void run() {
							RenderingRuleProperty contourLinesProp = app.getRendererRegistry().getCustomRenderingRuleProperty(CONTOUR_LINES_ATTR);
							if (contourLinesProp != null) {
								final OsmandSettings.CommonPreference<String> pref = settings.getCustomRenderProperty(contourLinesProp.getAttrName());
								boolean selected = !pref.get().equals(CONTOUR_LINES_DISABLED_VALUE);

								SRTMPlugin plugin = OsmandPlugin.getPlugin(SRTMPlugin.class);
								if (selected && plugin != null && !plugin.isActive() && !plugin.needsInstallation()) {
									OsmandPlugin.enablePlugin(mapActivity, mapActivity.getMyApplication(), plugin, true);
								}

								ContextMenuItem item = adapter.getItem(position);
								if (item != null) {
									item.setDescription(app.getString(R.string.display_zoom_level,
											getPrefDescription(app, contourLinesProp, pref)));
									item.setColorRes(selected ? R.color.osmand_orange : ContextMenuItem.INVALID_ID);
									item.setSelected(selected);
									adapter.notifyDataSetChanged();
								}
								refreshMapComplete(mapActivity);
							}

						}
					});
				} else if (itemId == R.string.layer_hillshade) {
					toggleHillshade(mapActivity, isChecked, new Runnable() {
						@Override
						public void run() {
							boolean selected = settings.HILLSHADE.get();
							SRTMPlugin plugin = OsmandPlugin.getPlugin(SRTMPlugin.class);
							if (selected && plugin != null && !plugin.isActive() && !plugin.needsInstallation()) {
								OsmandPlugin.enablePlugin(mapActivity, mapActivity.getMyApplication(), plugin, true);
							}

							ContextMenuItem item = adapter.getItem(position);
							if (item != null) {
								item.setColorRes(selected ? R.color.osmand_orange : ContextMenuItem.INVALID_ID);
								item.setSelected(selected);
								adapter.notifyDataSetChanged();
							}
							updateLayers(mapView, mapActivity);
							refreshMapComplete(mapActivity);
						}
					});
				}
				return true;
			}
		};

		RenderingRuleProperty contourLinesProp = app.getRendererRegistry().getCustomRenderingRuleProperty(CONTOUR_LINES_ATTR);
		if (contourLinesProp != null) {
			final OsmandSettings.CommonPreference<String> pref = settings.getCustomRenderProperty(contourLinesProp.getAttrName());
			boolean contourLinesSelected = isContourLinesLayerEnabled(app);
			String descr = getPrefDescription(app, contourLinesProp, pref);
			adapter.addItem(new ContextMenuItem.ItemBuilder()
					.setId(CONTOUR_LINES)
					.setTitleId(R.string.srtm_plugin_name, mapActivity)
					.setSelected(contourLinesSelected)
					.setIcon(R.drawable.ic_plugin_srtm)
					.setDescription(app.getString(R.string.display_zoom_level, descr))
					.setColor(contourLinesSelected ? R.color.osmand_orange : ContextMenuItem.INVALID_ID)
					.setSecondaryIcon(R.drawable.ic_action_additional_option)
					.setPosition(12)
					.setListener(listener).createItem());
		}
		boolean hillshadeEnabled = settings.HILLSHADE.get();
		adapter.addItem(new ContextMenuItem.ItemBuilder()
				.setId(HILLSHADE_LAYER)
				.setTitleId(R.string.layer_hillshade, mapActivity)
				.setSelected(hillshadeEnabled)
				.setColor(hillshadeEnabled ? R.color.osmand_orange : ContextMenuItem.INVALID_ID)
				.setIcon(R.drawable.ic_action_hillshade_dark)
				.setSecondaryIcon(R.drawable.ic_action_additional_option)
				.setListener(listener)
				.setPosition(13)
				.createItem());
	}

	public void toggleContourLines(final MapActivity activity,
								   final boolean isChecked,
								   final Runnable callback) {
		RenderingRuleProperty contourLinesProp = app.getRendererRegistry().getCustomRenderingRuleProperty(CONTOUR_LINES_ATTR);
		if (contourLinesProp != null) {
			final OsmandSettings.CommonPreference<String> pref = settings.getCustomRenderProperty(contourLinesProp.getAttrName());
			CommonPreference<String> zoomSetting = settings.CONTOUR_LINES_ZOOM;
			if (!isChecked) {
				zoomSetting.set(pref.get());
				pref.set(CONTOUR_LINES_DISABLED_VALUE);
				if (callback != null) {
					callback.run();
				}
			} else if (zoomSetting.get() != null && !zoomSetting.get().equals(CONTOUR_LINES_DISABLED_VALUE)) {
				pref.set(zoomSetting.get());
				if (callback != null) {
					callback.run();
				}
			} else {
				selectPropertyValue(activity, contourLinesProp, pref, callback);
			}
		}
	}

	public void toggleHillshade(final MapActivity activity,
								   final boolean isChecked,
								   final Runnable callback) {
		settings.HILLSHADE.set(isChecked);
		if (callback != null) {
			callback.run();
		}
	}

	public String getPrefDescription(final Context ctx, final RenderingRuleProperty p, final OsmandSettings.CommonPreference<String> pref) {
		if (!Algorithms.isEmpty(pref.get())) {
			return SettingsActivity.getStringPropertyValue(ctx, pref.get());
		} else {
			return SettingsActivity.getStringPropertyValue(ctx, p.getDefaultValueDescription());
		}
	}

	public void selectPropertyValue(final MapActivity activity,
									 final RenderingRuleProperty p,
									 final OsmandSettings.CommonPreference<String> pref,
									 final Runnable callback) {
		final String propertyDescr = SettingsActivity.getStringPropertyDescription(activity,
				p.getAttrName(), p.getName());
		AlertDialog.Builder b = new AlertDialog.Builder(activity);
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
				refreshMapComplete(activity);
				dialog.dismiss();
			}
		});
		b.setNegativeButton(R.string.shared_string_dismiss, null);
		b.setOnDismissListener(new DialogInterface.OnDismissListener() {
			@Override
			public void onDismiss(DialogInterface dialog) {
				if (callback != null) {
					callback.run();
				}
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

	public static void refreshMapComplete(final MapActivity activity) {
		activity.getMyApplication().getResourceManager().getRenderer().clearCache();
		activity.updateMapSettings();
		activity.getMapView().refreshMap(true);
	}
}
