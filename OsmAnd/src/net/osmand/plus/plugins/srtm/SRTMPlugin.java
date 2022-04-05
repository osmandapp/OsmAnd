package net.osmand.plus.plugins.srtm;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import net.osmand.data.LatLon;
import net.osmand.plus.DialogListItemAdapter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.chooseplan.OsmAndFeature;
import net.osmand.plus.chooseplan.button.PurchasingUtils;
import net.osmand.plus.dashboard.DashboardOnMap;
import net.osmand.plus.download.DownloadActivityType;
import net.osmand.plus.download.DownloadIndexesThread;
import net.osmand.plus.download.IndexItem;
import net.osmand.plus.inapp.InAppPurchaseHelper;
import net.osmand.plus.plugins.OsmandPlugin;
import net.osmand.plus.quickaction.QuickActionType;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.widgets.cmadapter.ContextMenuAdapter;
import net.osmand.plus.widgets.cmadapter.callback.ItemClickListener;
import net.osmand.plus.widgets.cmadapter.callback.OnRowItemClick;
import net.osmand.plus.widgets.cmadapter.item.ContextMenuItem;
import net.osmand.render.RenderingRuleProperty;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.CONTOUR_LINES;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.PLUGIN_SRTM;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.TERRAIN_ID;

public class SRTMPlugin extends OsmandPlugin {

	private static final String SRTM_PLUGIN_COMPONENT_PAID = "net.osmand.srtmPlugin.paid";
	private static final String SRTM_PLUGIN_COMPONENT = "net.osmand.srtmPlugin";

	public static final String CONTOUR_PREFIX = "contour";
	public static final String CONTOUR_LINES_ATTR = "contourLines";
	public static final String CONTOUR_LINES_SCHEME_ATTR = "contourColorScheme";
	public static final String CONTOUR_LINES_DISABLED_VALUE = "disabled";
	public static final String CONTOUR_WIDTH_ATTR = "contourWidth";
	public static final String CONTOUR_DENSITY_ATTR = "contourDensity";

	public static final int TERRAIN_MIN_ZOOM = 3;
	public static final int TERRAIN_MAX_ZOOM = 19;

	public final CommonPreference<Integer> HILLSHADE_MIN_ZOOM;
	public final CommonPreference<Integer> HILLSHADE_MAX_ZOOM;
	public final CommonPreference<Integer> HILLSHADE_TRANSPARENCY;

	public final CommonPreference<Integer> SLOPE_MIN_ZOOM;
	public final CommonPreference<Integer> SLOPE_MAX_ZOOM;
	public final CommonPreference<Integer> SLOPE_TRANSPARENCY;

	public final CommonPreference<Boolean> TERRAIN;
	public final CommonPreference<TerrainMode> TERRAIN_MODE;

	public final CommonPreference<String> CONTOUR_LINES_ZOOM;

	private final OsmandSettings settings;

	private TerrainLayer terrainLayer;

	@Override
	public String getId() {
		return PLUGIN_SRTM;
	}

	public SRTMPlugin(OsmandApplication app) {
		super(app);
		settings = app.getSettings();

		HILLSHADE_MIN_ZOOM = registerIntPreference("hillshade_min_zoom", 3).makeProfile();
		HILLSHADE_MAX_ZOOM = registerIntPreference("hillshade_max_zoom", 17).makeProfile();
		HILLSHADE_TRANSPARENCY = registerIntPreference("hillshade_transparency", 100).makeProfile();

		SLOPE_MIN_ZOOM = registerIntPreference("slope_min_zoom", 3).makeProfile();
		SLOPE_MAX_ZOOM = registerIntPreference("slope_max_zoom", 17).makeProfile();
		SLOPE_TRANSPARENCY = registerIntPreference("slope_transparency", 80).makeProfile();

		TERRAIN = registerBooleanPreference("terrain_layer", true).makeProfile();
		TERRAIN_MODE = registerEnumIntPreference("terrain_mode", TerrainMode.HILLSHADE, TerrainMode.values(), TerrainMode.class).makeProfile();

		CONTOUR_LINES_ZOOM = registerStringPreference("contour_lines_zoom", null).makeProfile().cache();
	}

	@Override
	public int getLogoResourceId() {
		return R.drawable.ic_plugin_srtm;
	}

	@Override
	public Drawable getAssetResourceImage() {
		return app.getUIUtilities().getIcon(R.drawable.contour_lines);
	}

	@Override
	public boolean needsInstallation() {
		return super.needsInstallation()
				&& !InAppPurchaseHelper.isContourLinesPurchased(app);
	}

	@Override
	protected boolean isAvailable(OsmandApplication app) {
		return super.isAvailable(app)
				|| InAppPurchaseHelper.isContourLinesPurchased(app);
	}

	@Override
	public boolean isMarketPlugin() {
		return true;
	}

	@Override
	public boolean isPaid() {
		return true;
	}

	@Override
	public boolean isEnableByDefault() {
		return true;
	}

	@Override
	public String getComponentId1() {
		return SRTM_PLUGIN_COMPONENT_PAID;
	}

	@Override
	public String getComponentId2() {
		return SRTM_PLUGIN_COMPONENT;
	}

	@Override
	public CharSequence getDescription() {
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
		if (pref.get().isEmpty()) {
			for (ApplicationMode m : ApplicationMode.allPossibleValues()) {
				if (pref.getModeValue(m).isEmpty()) {
					pref.setModeValue(m, "13");
				}
			}
		}
		return true;
	}

	@Override
	public void registerLayers(@NonNull Context context, @Nullable MapActivity mapActivity) {
		OsmandApplication app = (OsmandApplication) context.getApplicationContext();
		if (terrainLayer != null) {
			app.getOsmandMap().getMapView().removeLayer(terrainLayer);
		}
		if (TERRAIN.get()) {
			terrainLayer = new TerrainLayer(context, this);
			app.getOsmandMap().getMapView().addLayer(terrainLayer, 0.6f);
		}
	}

	public boolean isTerrainLayerEnabled() {
		return TERRAIN.get();
	}

	public void setTerrainLayerEnabled(boolean enabled) {
		TERRAIN.set(enabled);
	}

	public TerrainMode getTerrainMode() {
		return TERRAIN_MODE.get();
	}

	public void setTerrainMode(TerrainMode mode) {
		TERRAIN_MODE.set(mode);
	}

	public void setTerrainTransparency(int transparency, TerrainMode mode) {
		switch (mode) {
			case HILLSHADE:
				HILLSHADE_TRANSPARENCY.set(transparency);
				break;
			case SLOPE:
				SLOPE_TRANSPARENCY.set(transparency);
				break;
		}
	}

	public void setTerrainZoomValues(int minZoom, int maxZoom, TerrainMode mode) {
		switch (mode) {
			case HILLSHADE:
				HILLSHADE_MIN_ZOOM.set(minZoom);
				HILLSHADE_MAX_ZOOM.set(maxZoom);
				break;
			case SLOPE:
				SLOPE_MIN_ZOOM.set(minZoom);
				SLOPE_MAX_ZOOM.set(maxZoom);
				break;
		}
	}

	public int getTerrainTransparency() {
		switch (getTerrainMode()) {
			case HILLSHADE:
				return HILLSHADE_TRANSPARENCY.get();
			case SLOPE:
				return SLOPE_TRANSPARENCY.get();
		}
		return 100;
	}

	public int getTerrainMinZoom() {
		switch (getTerrainMode()) {
			case HILLSHADE:
				return HILLSHADE_MIN_ZOOM.get();
			case SLOPE:
				return SLOPE_MIN_ZOOM.get();
		}
		return TERRAIN_MIN_ZOOM;
	}

	public int getTerrainMaxZoom() {
		switch (getTerrainMode()) {
			case HILLSHADE:
				return HILLSHADE_MAX_ZOOM.get();
			case SLOPE:
				return SLOPE_MAX_ZOOM.get();
		}
		return TERRAIN_MAX_ZOOM;
	}

	public static boolean isContourLinesLayerEnabled(OsmandApplication app) {
		boolean contourLinesEnabled = false;

		RenderingRuleProperty contourLinesProp = app.getRendererRegistry().getCustomRenderingRuleProperty(CONTOUR_LINES_ATTR);
		if (contourLinesProp != null) {
			final CommonPreference<String> pref = app.getSettings().getCustomRenderProperty(contourLinesProp.getAttrName());
			if (!Algorithms.isEmpty(pref.get())) {
				contourLinesEnabled = !CONTOUR_LINES_DISABLED_VALUE.equals(pref.get());
			} else {
				contourLinesEnabled = !CONTOUR_LINES_DISABLED_VALUE.equals(contourLinesProp.getDefaultValueDescription());
			}
		}
		return contourLinesEnabled;
	}

	@Override
	public void updateLayers(@NonNull Context context, @Nullable MapActivity mapActivity) {
		OsmandMapTileView mapView = app.getOsmandMap().getMapView();
		if (TERRAIN.get() && isActive()) {
			removeTerrainLayer(mapView);
			registerLayers(context, mapActivity);
		} else {
			removeTerrainLayer(mapView);
		}
	}

	private void removeTerrainLayer(@NonNull OsmandMapTileView mapView) {
		if (terrainLayer != null) {
			mapView.removeLayer(terrainLayer);
			terrainLayer = null;
			mapView.refreshMap();
		}
	}

	@Override
	protected void registerLayerContextMenuActions(@NonNull ContextMenuAdapter adapter, @NonNull MapActivity mapActivity, @NonNull List<RenderingRuleProperty> customRules) {
		if (isLocked()) {
			PurchasingUtils.createPromoItem(adapter, mapActivity, OsmAndFeature.TERRAIN,
					TERRAIN_ID,
					R.string.shared_string_terrain,
					R.string.contour_lines_hillshades_slope);
		} else {
			createContextMenuItems(adapter, mapActivity);
		}
	}

	private void createContextMenuItems(@NonNull ContextMenuAdapter adapter, @NonNull MapActivity mapActivity) {
		ItemClickListener listener = new OnRowItemClick() {

			@Override
			public boolean onRowItemClick(ArrayAdapter<ContextMenuItem> adapter, View view, int itemId, int position) {
				int[] viewCoordinates = AndroidUtils.getCenterViewCoordinates(view);
				if (itemId == R.string.srtm_plugin_name) {
					mapActivity.getDashboard().setDashboardVisibility(true, DashboardOnMap.DashboardType.CONTOUR_LINES, viewCoordinates);
					return false;
				} else if (itemId == R.string.shared_string_terrain) {
					mapActivity.getDashboard().setDashboardVisibility(true, DashboardOnMap.DashboardType.TERRAIN, viewCoordinates);
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
								final CommonPreference<String> pref = settings.getCustomRenderProperty(contourLinesProp.getAttrName());
								boolean selected = !pref.get().equals(CONTOUR_LINES_DISABLED_VALUE);

								SRTMPlugin plugin = OsmandPlugin.getPlugin(SRTMPlugin.class);
								OsmandPlugin.enablePluginIfNeeded(mapActivity, mapActivity.getMyApplication(), plugin, true);

								ContextMenuItem item = adapter.getItem(position);
								if (item != null) {
									item.setDescription(app.getString(R.string.display_zoom_level,
											getPrefDescription(app, contourLinesProp, pref)));
									item.setColor(app, selected ? R.color.osmand_orange : ContextMenuItem.INVALID_ID);
									item.setSelected(selected);
									adapter.notifyDataSetChanged();
								}
								mapActivity.refreshMapComplete();
							}
						}
					});
				} else if (itemId == R.string.shared_string_terrain) {
					toggleTerrain(mapActivity, isChecked, new Runnable() {
						@Override
						public void run() {
							boolean selected = TERRAIN.get();
							SRTMPlugin plugin = OsmandPlugin.getPlugin(SRTMPlugin.class);
							if (selected) {
								OsmandPlugin.enablePluginIfNeeded(mapActivity, mapActivity.getMyApplication(), plugin, true);
							}
							ContextMenuItem item = adapter.getItem(position);
							if (item != null) {
								item.setColor(app, selected ? R.color.osmand_orange : ContextMenuItem.INVALID_ID);
								item.setSelected(selected);
								adapter.notifyDataSetChanged();
							}
							updateLayers(mapActivity, mapActivity);
							mapActivity.refreshMapComplete();
						}
					});
				}
				return true;
			}
		};

		RenderingRuleProperty contourLinesProp = app.getRendererRegistry().getCustomRenderingRuleProperty(CONTOUR_LINES_ATTR);
		if (contourLinesProp != null) {
			final CommonPreference<String> pref = settings.getCustomRenderProperty(contourLinesProp.getAttrName());
			boolean contourLinesSelected = isContourLinesLayerEnabled(app);
			String descr = getPrefDescription(app, contourLinesProp, pref);
			adapter.addItem(new ContextMenuItem(CONTOUR_LINES)
					.setTitleId(R.string.srtm_plugin_name, mapActivity)
					.setSelected(contourLinesSelected)
					.setIcon(R.drawable.ic_plugin_srtm)
					.setDescription(app.getString(R.string.display_zoom_level, descr))
					.setColor(app, contourLinesSelected ? R.color.osmand_orange : ContextMenuItem.INVALID_ID)
					.setItemDeleteAction(CONTOUR_LINES_ZOOM)
					.setSecondaryIcon(R.drawable.ic_action_additional_option)
					.setListener(listener));
		}
		boolean terrainEnabled = TERRAIN.get();
		TerrainMode terrainMode = TERRAIN_MODE.get();
		adapter.addItem(new ContextMenuItem(TERRAIN_ID)
				.setTitleId(R.string.shared_string_terrain, mapActivity)
				.setDescription(app.getString(terrainMode == TerrainMode.HILLSHADE
						? R.string.shared_string_hillshade
						: R.string.download_slope_maps))
				.setSelected(terrainEnabled)
				.setColor(app, terrainEnabled ? R.color.osmand_orange : ContextMenuItem.INVALID_ID)
				.setIcon(R.drawable.ic_action_hillshade_dark)
				.setSecondaryIcon(R.drawable.ic_action_additional_option)
				.setItemDeleteAction(TERRAIN, TERRAIN_MODE)
				.setListener(listener)

		);
	}

	@Nullable
	@Override
	protected String getRenderPropertyPrefix() {
		return CONTOUR_PREFIX;
	}

	@Override
	public List<IndexItem> getSuggestedMaps() {
		List<IndexItem> suggestedMaps = new ArrayList<>();

		DownloadIndexesThread downloadThread = app.getDownloadThread();
		if (!downloadThread.getIndexes().isDownloadedFromInternet && settings.isInternetConnectionAvailable()) {
			downloadThread.runReloadIndexFiles();
		}

		if (!downloadThread.shouldDownloadIndexes()) {
			LatLon latLon = app.getMapViewTrackingUtilities().getMapLocation();
			suggestedMaps.addAll(getMapsForType(latLon, DownloadActivityType.SRTM_COUNTRY_FILE));
			suggestedMaps.addAll(getMapsForType(latLon, DownloadActivityType.HILLSHADE_FILE));
			suggestedMaps.addAll(getMapsForType(latLon, DownloadActivityType.SLOPE_FILE));
		}

		return suggestedMaps;
	}

	public void toggleContourLines(final MapActivity activity,
	                               final boolean isChecked,
	                               final Runnable callback) {
		RenderingRuleProperty contourLinesProp = app.getRendererRegistry().getCustomRenderingRuleProperty(CONTOUR_LINES_ATTR);
		if (contourLinesProp != null) {
			final CommonPreference<String> pref = settings.getCustomRenderProperty(contourLinesProp.getAttrName());
			CommonPreference<String> zoomSetting = CONTOUR_LINES_ZOOM;
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

	public void toggleTerrain(final MapActivity activity,
	                          final boolean isChecked,
	                          final Runnable callback) {
		TERRAIN.set(isChecked);
		if (callback != null) {
			callback.run();
		}
	}

	public String getPrefDescription(final Context ctx, final RenderingRuleProperty p, final CommonPreference<String> pref) {
		if (!Algorithms.isEmpty(pref.get())) {
			return AndroidUtils.getRenderingStringPropertyValue(ctx, pref.get());
		} else {
			return AndroidUtils.getRenderingStringPropertyValue(ctx, p.getDefaultValueDescription());
		}
	}

	public void selectPropertyValue(final MapActivity activity,
	                                final RenderingRuleProperty p,
	                                final CommonPreference<String> pref,
	                                final Runnable callback) {
		final String propertyDescr = AndroidUtils.getRenderingStringPropertyDescription(activity,
				p.getAttrName(), p.getName());
		boolean nightMode = isNightMode(activity, app);
		int themeRes = getThemeRes(activity, app);
		AlertDialog.Builder b = new AlertDialog.Builder(new ContextThemeWrapper(activity, themeRes));
		b.setTitle(propertyDescr);

		List<String> possibleValuesList = new ArrayList<>(Arrays.asList(p.getPossibleValues()));
		possibleValuesList.remove(CONTOUR_LINES_DISABLED_VALUE);
		final String[] possibleValues = possibleValuesList.toArray(new String[0]);

		int i = possibleValuesList.indexOf(pref.get());
		if (i >= 0) {
			i++;
		} else if (Algorithms.isEmpty(pref.get())) {
			i = 0;
		}

		String[] possibleValuesString = new String[possibleValues.length + 1];
		possibleValuesString[0] = AndroidUtils.getRenderingStringPropertyValue(activity,
				p.getDefaultValueDescription());

		for (int j = 0; j < possibleValues.length; j++) {
			possibleValuesString[j + 1] = AndroidUtils.getRenderingStringPropertyValue(activity,
					possibleValues[j]);
		}

		int selectedModeColor = settings.getApplicationMode().getProfileColor(nightMode);
		DialogListItemAdapter dialogAdapter = DialogListItemAdapter.createSingleChoiceAdapter(
				possibleValuesString, nightMode, i, app, selectedModeColor, themeRes, new View.OnClickListener() {

					@Override
					public void onClick(View v) {
						int which = (int) v.getTag();
						if (which == 0) {
							pref.set("");
						} else {
							pref.set(possibleValues[which - 1]);
						}
						activity.refreshMapComplete();
					}
				}
		);
		b.setNegativeButton(R.string.shared_string_dismiss, null);
		b.setOnDismissListener(new DialogInterface.OnDismissListener() {
			@Override
			public void onDismiss(DialogInterface dialog) {
				if (callback != null) {
					callback.run();
				}
			}
		});
		b.setAdapter(dialogAdapter, null);
		dialogAdapter.setDialog(b.show());
	}

	@Override
	public void disable(OsmandApplication app) {
	}

	private static boolean isNightMode(Activity activity, OsmandApplication app) {
		if (activity == null || app == null) {
			return false;
		}
		return activity instanceof MapActivity ? app.getDaynightHelper().isNightModeForMapControls() : !app.getSettings().isLightContent();
	}

	private static int getThemeRes(Activity activity, OsmandApplication app) {
		return isNightMode(activity, app) ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme;
	}

	@Override
	protected List<QuickActionType> getQuickActionTypes() {
		List<QuickActionType> quickActionTypes = new ArrayList<>();
		quickActionTypes.add(ContourLinesAction.TYPE);
		quickActionTypes.add(TerrainAction.TYPE);
		return quickActionTypes;
	}

	@Override
	protected CommonPreference<String> registerRenderingPreference(@NonNull String prefId, @Nullable String defValue) {
		if (CONTOUR_LINES_ATTR.equals(prefId)) {
			defValue = CONTOUR_LINES_DISABLED_VALUE;
		}
		return super.registerRenderingPreference(prefId, defValue);
	}
}
