package net.osmand.plus.plugins.srtm;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.CONTOUR_LINES;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.PLUGIN_SRTM;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.RELIEF_3D_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.TERRAIN_CATEGORY_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.TERRAIN_DEPTH_CONTOURS;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.TERRAIN_DESCRIPTION_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.TERRAIN_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.TERRAIN_PROMO_ID;
import static net.osmand.plus.download.DownloadActivityType.GEOTIFF_FILE;
import static net.osmand.plus.plugins.srtm.CollectColorPalletTask.CollectColorPalletListener;
import static net.osmand.plus.widgets.ctxmenu.data.ContextMenuItem.INVALID_ID;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.StateChangedListener;
import net.osmand.core.android.MapRendererContext;
import net.osmand.data.LatLon;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.chooseplan.ChoosePlanFragment;
import net.osmand.plus.chooseplan.OsmAndFeature;
import net.osmand.plus.chooseplan.button.PurchasingUtils;
import net.osmand.plus.dashboard.DashboardOnMap;
import net.osmand.plus.download.DownloadActivityType;
import net.osmand.plus.download.DownloadIndexesThread;
import net.osmand.plus.download.IndexItem;
import net.osmand.plus.inapp.InAppPurchaseUtils;
import net.osmand.plus.plugins.OsmandPlugin;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.development.OsmandDevelopmentPlugin;
import net.osmand.plus.plugins.openseamaps.NauticalMapsPlugin;
import net.osmand.plus.quickaction.QuickActionType;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.corenative.NativeCoreContext;
import net.osmand.plus.widgets.alert.AlertDialogData;
import net.osmand.plus.widgets.alert.CustomAlert;
import net.osmand.plus.widgets.ctxmenu.ContextMenuAdapter;
import net.osmand.plus.widgets.ctxmenu.callback.ItemClickListener;
import net.osmand.plus.widgets.ctxmenu.callback.OnDataChangeUiAdapter;
import net.osmand.plus.widgets.ctxmenu.callback.OnRowItemClick;
import net.osmand.plus.widgets.ctxmenu.data.ContextMenuItem;
import net.osmand.render.RenderingRuleProperty;
import net.osmand.util.Algorithms;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SRTMPlugin extends OsmandPlugin {

	private static final String SRTM_PLUGIN_COMPONENT_PAID = "net.osmand.srtmPlugin.paid";
	private static final String SRTM_PLUGIN_COMPONENT = "net.osmand.srtmPlugin";

	public static final String CONTOUR_PREFIX = "contour";
	public static final String CONTOUR_LINES_ATTR = "contourLines";
	public static final String CONTOUR_LINES_SCHEME_ATTR = "contourColorScheme";
	public static final String CONTOUR_LINES_DISABLED_VALUE = "disabled";
	public static final String CONTOUR_WIDTH_ATTR = "contourWidth";
	public static final String CONTOUR_DENSITY_ATTR = "contourDensity";

	public static final int TERRAIN_MIN_SUPPORTED_ZOOM = 4;
	public static final int TERRAIN_MAX_SUPPORTED_ZOOM = 19;

	public static final float MIN_VERTICAL_EXAGGERATION = 1.0f;
	public static final float MAX_VERTICAL_EXAGGERATION = 3.0f;


	public final CommonPreference<Boolean> TERRAIN;
	public final CommonPreference<String> TERRAIN_MODE;


	public final CommonPreference<String> CONTOUR_LINES_ZOOM;

	private final StateChangedListener<Boolean> enable3DMapsListener;
	private final StateChangedListener<Boolean> terrainListener;
	private final StateChangedListener<String> terrainModeListener;
	private final StateChangedListener<Float> verticalExaggerationListener;

	private TerrainLayer terrainLayer;

	@Override
	public String getId() {
		return PLUGIN_SRTM;
	}

	public SRTMPlugin(OsmandApplication app) {
		super(app);


		TERRAIN = registerBooleanPreference("terrain_layer", true).makeProfile();
		TerrainMode[] tms = TerrainMode.values(app);
		TERRAIN_MODE = registerStringPreference("terrain_mode", tms.length == 0 ? "" : tms[0].getKeyName()).makeProfile();

		CONTOUR_LINES_ZOOM = registerStringPreference("contour_lines_zoom", null).makeProfile().cache();

		enable3DMapsListener = change -> app.runInUIThread(() -> {
			MapRendererContext mapContext = NativeCoreContext.getMapRendererContext();
			if (mapContext != null) {
				mapContext.recreateHeightmapProvider();
			}
		});
		settings.ENABLE_3D_MAPS.addListener(enable3DMapsListener);

		terrainListener = change -> app.runInUIThread(() -> {
			MapRendererContext mapContext = NativeCoreContext.getMapRendererContext();
			if (mapContext != null) {
				mapContext.updateElevationConfiguration();
			}
		});
		TERRAIN.addListener(terrainListener);

		terrainModeListener = change -> app.runInUIThread(() -> {
			MapRendererContext mapContext = NativeCoreContext.getMapRendererContext();
			if (mapContext != null) {
				mapContext.updateElevationConfiguration();
			}
		});
		TERRAIN_MODE.addListener(terrainModeListener);
		verticalExaggerationListener = scale -> {
			MapRendererContext mapContext = NativeCoreContext.getMapRendererContext();
			if (mapContext != null) {
				mapContext.updateVerticalExaggerationScale();
			}
		};
		app.getSettings().VERTICAL_EXAGGERATION_SCALE.addListener(verticalExaggerationListener);
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
		return super.needsInstallation() && !InAppPurchaseUtils.isContourLinesAvailable(app);
	}

	@Override
	protected boolean isAvailable(OsmandApplication app) {
		return super.isAvailable(app) || InAppPurchaseUtils.isContourLinesAvailable(app);
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
	public CharSequence getDescription(boolean linksEnabled) {
		String docsUrl = app.getString(R.string.docs_plugin_srtm);
		String description = app.getString(R.string.srtm_plugin_description, docsUrl);
		return linksEnabled ? UiUtilities.createUrlSpannable(description, docsUrl) : description;
	}

	@Override
	public String getName() {
		return app.getString(R.string.srtm_plugin_name);
	}

	@Nullable
	@Override
	public OsmAndFeature getOsmAndFeature() {
		return OsmAndFeature.TERRAIN;
	}

	@Override
	public boolean init(@NonNull OsmandApplication app, Activity activity) {
		OsmandSettings settings = app.getSettings();
		settings.getCustomRenderProperty(CONTOUR_LINES_ATTR).setDefaultValue("13");
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

	@Override
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		MapRendererContext mapRendererContext = NativeCoreContext.getMapRendererContext();
		if (mapRendererContext != null) {
			updateMapPresentationEnvironment(mapRendererContext);
		}
	}

	// If enabled, map should be rendered with elevation data (in 3D)
	public boolean is3DMapsEnabled() {
		return is3DReliefAllowed() && settings.ENABLE_3D_MAPS.get();
	}

	public boolean is3DReliefAllowed() {
		return app.useOpenGlRenderer() && InAppPurchaseUtils.is3dMapsAvailable(app);
	}

	public boolean isTerrainLayerEnabled() {
		return TERRAIN.get();
	}

	public void setTerrainLayerEnabled(boolean enabled) {
		TERRAIN.set(enabled);
	}


	public boolean isHillshadeMode() {
		return getTerrainMode().isHillshade();
	}

	public TerrainMode getTerrainMode() {
		return TerrainMode.getByKey(TERRAIN_MODE.get());
	}

	public void setTerrainMode(TerrainMode mode) {
		TERRAIN_MODE.set(mode.getKeyName());
	}

	public void setTerrainTransparency(int transparency, TerrainMode mode) {
		mode.setTransparency(transparency);
	}

	public void setTerrainZoomValues(int minZoom, int maxZoom, TerrainMode mode) {
		mode.setZoomValues(minZoom, maxZoom);
	}

	public static float normalizeVerticalExaggerationScale(float scale) {
		return Math.max(MIN_VERTICAL_EXAGGERATION, Math.min(MAX_VERTICAL_EXAGGERATION, scale));
	}

	public float getVerticalExaggerationScale() {
		return normalizeVerticalExaggerationScale(app.getSettings().VERTICAL_EXAGGERATION_SCALE.get());
	}

	public void setVerticalExaggerationScale(float scale) {
		app.getSettings().VERTICAL_EXAGGERATION_SCALE.set(normalizeVerticalExaggerationScale(scale));
	}

	public int getTerrainTransparency() {
		return getTerrainMode().getTransparency();
	}

	public void resetZoomLevelsToDefault() {
		getTerrainMode().resetZoomsToDefault();
	}

	public void resetTransparencyToDefault() {
		getTerrainMode().resetTransparencyToDefault();
	}

	public void resetVerticalExaggerationToDefault() {
		app.getSettings().VERTICAL_EXAGGERATION_SCALE.resetToDefault();
	}

	public int getTerrainMinZoom() {
		int minSupportedZoom = TERRAIN_MIN_SUPPORTED_ZOOM;
		return Math.max(minSupportedZoom, getTerrainMode().getMinZoom());
	}

	public int getTerrainMaxZoom() {
		int maxSupportedZoom = TERRAIN_MAX_SUPPORTED_ZOOM;
		return Math.min(maxSupportedZoom, getTerrainMode().getMaxZoom());
	}

	public static boolean isContourLinesLayerEnabled(OsmandApplication app) {
		boolean contourLinesEnabled = false;

		RenderingRuleProperty contourLinesProp = app.getRendererRegistry().getCustomRenderingRuleProperty(CONTOUR_LINES_ATTR);
		if (contourLinesProp != null) {
			CommonPreference<String> pref = app.getSettings().getCustomRenderProperty(contourLinesProp.getAttrName());
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
	protected void registerConfigureMapCategoryActions(@NonNull ContextMenuAdapter adapter,
	                                                   @NonNull MapActivity mapActivity,
	                                                   @NonNull List<RenderingRuleProperty> customRules) {
		if (isEnabled()) {
			adapter.addItem(new ContextMenuItem(TERRAIN_CATEGORY_ID)
					.setCategory(true)
					.setTitle(app.getString(R.string.srtm_plugin_name))
					.setLayout(R.layout.list_group_title_with_switch));

			if (isLocked()) {
				addTerrainDescriptionItem(adapter, mapActivity);
			} else {
				createContextMenuItems(adapter, mapActivity);
			}
			NauticalMapsPlugin nauticalPlugin = PluginsHelper.getPlugin(NauticalMapsPlugin.class);
			if (nauticalPlugin != null) {
				nauticalPlugin.createAdapterItem(TERRAIN_DEPTH_CONTOURS, adapter, mapActivity, customRules);
			}
		}
	}

	private void addTerrainDescriptionItem(@NonNull ContextMenuAdapter adapter,
	                                       @NonNull MapActivity activity) {
		if (app.useOpenGlRenderer()) {
			adapter.addItem(new ContextMenuItem(TERRAIN_DESCRIPTION_ID)
					.setLayout(R.layout.list_item_terrain_description)
					.setClickable(false)
					.setListener((uiAdapter, view, item, isChecked) -> {
						ChoosePlanFragment.showInstance(activity, OsmAndFeature.TERRAIN);
						return true;
					}));
		} else {
			PurchasingUtils.createPromoItem(adapter, activity, OsmAndFeature.TERRAIN,
					TERRAIN_PROMO_ID,
					R.string.srtm_plugin_name,
					R.string.contour_lines_hillshade_slope);
		}
	}

	private void createContextMenuItems(@NonNull ContextMenuAdapter adapter, @NonNull MapActivity mapActivity) {
		ItemClickListener listener = new OnRowItemClick() {

			@Override
			public boolean onRowItemClick(@NonNull OnDataChangeUiAdapter uiAdapter, @NonNull View view, @NonNull ContextMenuItem item) {
				int[] viewCoordinates = AndroidUtils.getCenterViewCoordinates(view);
				int itemId = item.getTitleId();
				if (itemId == R.string.download_srtm_maps) {
					mapActivity.getDashboard().setDashboardVisibility(true, DashboardOnMap.DashboardType.CONTOUR_LINES, viewCoordinates);
					return false;
				} else if (itemId == R.string.shared_string_terrain) {
					mapActivity.getDashboard().setDashboardVisibility(true, DashboardOnMap.DashboardType.TERRAIN, viewCoordinates);
					return false;
				} else if (itemId == R.string.relief_3d) {
					if (InAppPurchaseUtils.is3dMapsAvailable(app)) {
						mapActivity.getDashboard().setDashboardVisibility(true, DashboardOnMap.DashboardType.RELIEF_3D, viewCoordinates);
					} else {
						ChoosePlanFragment.showInstance(mapActivity, OsmAndFeature.RELIEF_3D);
					}
					return false;
				}
				return true;
			}

			@Override
			public boolean onContextMenuClick(@Nullable OnDataChangeUiAdapter uiAdapter, @Nullable View view, @NotNull ContextMenuItem item, boolean isChecked) {
				int itemId = item.getTitleId();
				if (itemId == R.string.download_srtm_maps) {
					toggleContourLines(mapActivity, isChecked, () -> {
						RenderingRuleProperty contourLinesProp = app.getRendererRegistry().getCustomRenderingRuleProperty(CONTOUR_LINES_ATTR);
						if (contourLinesProp != null) {
							CommonPreference<String> pref = settings.getCustomRenderProperty(contourLinesProp.getAttrName());
							boolean selected = !pref.get().equals(CONTOUR_LINES_DISABLED_VALUE);

							SRTMPlugin plugin = PluginsHelper.getPlugin(SRTMPlugin.class);
							PluginsHelper.enablePluginIfNeeded(mapActivity, mapActivity.getMyApplication(), plugin, true);

							item.setDescription(app.getString(R.string.display_zoom_level,
									getPrefDescription(app, contourLinesProp, pref)));
							item.setColor(app, selected ? R.color.osmand_orange : ContextMenuItem.INVALID_ID);
							item.setSelected(selected);
							uiAdapter.onDataSetChanged();
							mapActivity.refreshMapComplete();
						}
					});
				} else if (itemId == R.string.shared_string_terrain) {
					toggleTerrain(isChecked, () -> {
						boolean selected = TERRAIN.get();
						SRTMPlugin plugin = PluginsHelper.getPlugin(SRTMPlugin.class);
						if (selected) {
							PluginsHelper.enablePluginIfNeeded(mapActivity, mapActivity.getMyApplication(), plugin, true);
						}
						item.setColor(app, selected ? R.color.osmand_orange : ContextMenuItem.INVALID_ID);
						item.setSelected(selected);
						uiAdapter.onDataSetChanged();
						updateLayers(mapActivity, mapActivity);
						mapActivity.refreshMapComplete();
					});
				} else if (itemId == R.string.relief_3d) {
					if (InAppPurchaseUtils.is3dMapsAvailable(app)) {
						settings.ENABLE_3D_MAPS.set(isChecked);
						item.setColor(app, isChecked ? R.color.osmand_orange : ContextMenuItem.INVALID_ID);
						item.setSelected(isChecked);
						item.setDescription(app.getString(isChecked ? R.string.shared_string_on : R.string.shared_string_off));
						uiAdapter.onDataSetChanged();

						app.runInUIThread(() -> app.getOsmandMap().getMapLayers().getMapInfoLayer().recreateAllControls(mapActivity));
					} else {
						ChoosePlanFragment.showInstance(mapActivity, OsmAndFeature.RELIEF_3D);
					}
				}
				return true;
			}
		};

		RenderingRuleProperty contourLinesProp = app.getRendererRegistry().getCustomRenderingRuleProperty(CONTOUR_LINES_ATTR);
		if (contourLinesProp != null) {
			CommonPreference<String> pref = settings.getCustomRenderProperty(contourLinesProp.getAttrName());
			boolean contourLinesSelected = isContourLinesLayerEnabled(app);
			String descr = getPrefDescription(app, contourLinesProp, pref);
			adapter.addItem(new ContextMenuItem(CONTOUR_LINES)
					.setTitleId(R.string.download_srtm_maps, mapActivity)
					.setSelected(contourLinesSelected)
					.setIcon(R.drawable.ic_plugin_srtm)
					.setDescription(app.getString(R.string.display_zoom_level, descr))
					.setColor(app, contourLinesSelected ? R.color.osmand_orange : ContextMenuItem.INVALID_ID)
					.setItemDeleteAction(CONTOUR_LINES_ZOOM)
					.setSecondaryIcon(R.drawable.ic_action_additional_option)
					.setListener(listener));
		}
		boolean terrainEnabled = TERRAIN.get();
		TerrainMode terrainMode = TerrainMode.getByKey(TERRAIN_MODE.get());
		adapter.addItem(new ContextMenuItem(TERRAIN_ID)
				.setTitleId(R.string.shared_string_terrain, mapActivity)
				.setDescription(terrainMode.getType().getName(app))
				.setSelected(terrainEnabled)
				.setColor(app, terrainEnabled ? R.color.osmand_orange : ContextMenuItem.INVALID_ID)
				.setIcon(R.drawable.ic_action_hillshade_dark)
				.setSecondaryIcon(R.drawable.ic_action_additional_option)
				.setItemDeleteAction(TERRAIN, TERRAIN_MODE)
				.setListener(listener)

		);
		if (app.useOpenGlRenderer()) {
			add3DReliefItem(adapter, mapActivity, listener);
		}
	}

	private void add3DReliefItem(@NonNull ContextMenuAdapter adapter, @NonNull MapActivity activity, @NonNull ItemClickListener listener) {
		ContextMenuItem item = new ContextMenuItem(RELIEF_3D_ID)
				.setTitleId(R.string.relief_3d, app)
				.setIcon(R.drawable.ic_action_3d_relief)
				.setListener(listener);

		boolean enabled3DMode = settings.ENABLE_3D_MAPS.get();
		if (!InAppPurchaseUtils.is3dMapsAvailable(app)) {
			boolean nightMode = isNightMode(activity, app);
			item.setUseNaturalSecondIconColor(true);
			item.setSecondaryIcon(nightMode ? R.drawable.img_button_pro_night : R.drawable.img_button_pro_day);
		} else {
			item.setColor(app, enabled3DMode ? R.color.osmand_orange : INVALID_ID);
			item.setSelected(enabled3DMode);
			item.setSecondaryIcon(R.drawable.ic_action_additional_option);
			item.setDescription(app.getString(enabled3DMode ? R.string.shared_string_on : R.string.shared_string_off));
		}
		adapter.addItem(item);
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

			OsmandDevelopmentPlugin plugin = PluginsHelper.getPlugin(OsmandDevelopmentPlugin.class);
			if (!app.useOpenGlRenderer() || plugin != null && plugin.USE_RASTER_SQLITEDB.get()) {
				suggestedMaps.addAll(getMapsForType(latLon, DownloadActivityType.HILLSHADE_FILE));
				suggestedMaps.addAll(getMapsForType(latLon, DownloadActivityType.SLOPE_FILE));
			} else {
				suggestedMaps.addAll(getMapsForType(latLon, GEOTIFF_FILE));
			}
		}

		return suggestedMaps;
	}

	public void toggleContourLines(MapActivity activity, boolean isChecked, Runnable callback) {
		RenderingRuleProperty contourLinesProp = app.getRendererRegistry().getCustomRenderingRuleProperty(CONTOUR_LINES_ATTR);
		if (contourLinesProp != null) {
			CommonPreference<String> pref = settings.getCustomRenderProperty(contourLinesProp.getAttrName());
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

	public void toggleTerrain(boolean isChecked, Runnable callback) {
		TERRAIN.set(isChecked);
		if (callback != null) {
			callback.run();
		}
	}

	public String getPrefDescription(Context ctx, RenderingRuleProperty p, CommonPreference<String> pref) {
		if (!Algorithms.isEmpty(pref.get())) {
			return AndroidUtils.getRenderingStringPropertyValue(ctx, pref.get());
		} else {
			return AndroidUtils.getRenderingStringPropertyValue(ctx, p.getDefaultValueDescription());
		}
	}

	public void selectPropertyValue(MapActivity activity, RenderingRuleProperty p,
	                                CommonPreference<String> pref, Runnable callback) {
		boolean nightMode = isNightMode(activity, app);
		String title = AndroidUtils.getRenderingStringPropertyDescription(activity, p.getAttrName(), p.getName());
		List<String> possibleValuesList = new ArrayList<>(Arrays.asList(p.getPossibleValues()));
		possibleValuesList.remove(CONTOUR_LINES_DISABLED_VALUE);
		String[] possibleValues = possibleValuesList.toArray(new String[0]);

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

		AlertDialogData dialogData = new AlertDialogData(activity, nightMode)
				.setTitle(title)
				.setControlsColor(ColorUtilities.getAppModeColor(app, nightMode))
				.setNegativeButton(R.string.shared_string_dismiss, null)
				.setOnDismissListener(dialog -> {
					if (callback != null) {
						callback.run();
					}
				});

		CustomAlert.showSingleSelection(dialogData, possibleValuesString, i, v -> {
			int which = (int) v.getTag();
			if (which == 0) {
				pref.set("");
			} else {
				pref.set(possibleValues[which - 1]);
			}
			activity.refreshMapComplete();
		});
	}

	private static boolean isNightMode(Activity activity, OsmandApplication app) {
		if (activity == null || app == null) {
			return false;
		}
		return app.getDaynightHelper().isNightMode(activity instanceof MapActivity);
	}

	@Override
	protected List<QuickActionType> getQuickActionTypes() {
		List<QuickActionType> quickActionTypes = new ArrayList<>();
		quickActionTypes.add(ContourLinesAction.TYPE);
		quickActionTypes.add(TerrainAction.TYPE);
		quickActionTypes.add(TerrainColorSchemeAction.TYPE);
		return quickActionTypes;
	}

	@Override
	protected CommonPreference<String> registerRenderingPreference(@NonNull RenderingRuleProperty property) {
		String attrName = property.getAttrName();
		String defValue = CONTOUR_LINES_ATTR.equals(attrName) ? CONTOUR_LINES_DISABLED_VALUE : "";
		return registerRenderingPreference(attrName, defValue);
	}

	public void onIndexItemDownloaded(@NonNull IndexItem item, boolean updatingFile) {
		if (item.getType() == GEOTIFF_FILE) {
			updateHeightmap(updatingFile, item.getTargetFile(app).getAbsolutePath());
		}
	}

	private void updateHeightmap(boolean overwriteExistingFile, @NonNull String filePath) {
		MapRendererContext mapRendererContext = NativeCoreContext.getMapRendererContext();
		if (mapRendererContext != null) {
			if (overwriteExistingFile) {
				mapRendererContext.removeCachedHeightmapTiles(filePath);
			} else {
				mapRendererContext.updateCachedHeightmapTiles();
			}
		}
	}

	@Override
	public void updateMapPresentationEnvironment(@NonNull MapRendererContext mapRendererContext) {
		mapRendererContext.updateElevationConfiguration();
		mapRendererContext.recreateHeightmapProvider();
		mapRendererContext.updateVerticalExaggerationScale();
	}

	public void getTerrainModeIcon(@NonNull String modeKey, @NonNull CollectColorPalletListener listener) {
		app.getColorPaletteHelper().getColorPaletteAsync(modeKey, listener);
	}
}
