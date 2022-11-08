package net.osmand.plus.plugins;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.IProgress;
import net.osmand.Location;
import net.osmand.PlatformUtil;
import net.osmand.data.Amenity;
import net.osmand.data.LatLon;
import net.osmand.data.MapObject;
import net.osmand.map.WorldRegion;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.TabActivity.TabItem;
import net.osmand.plus.dashboard.tools.DashFragmentData;
import net.osmand.plus.download.DownloadActivityType;
import net.osmand.plus.download.DownloadResources;
import net.osmand.plus.download.IndexItem;
import net.osmand.plus.mapcontextmenu.MenuBuilder;
import net.osmand.plus.mapcontextmenu.MenuController;
import net.osmand.plus.mapcontextmenu.builders.cards.ImageCard.GetImageCardsTask.GetImageCardsListener;
import net.osmand.plus.mapcontextmenu.builders.cards.ImageCard.ImageCardsHolder;
import net.osmand.plus.myplaces.ui.FavoritesActivity;
import net.osmand.plus.poi.PoiUIFilter;
import net.osmand.plus.quickaction.QuickActionType;
import net.osmand.plus.search.QuickSearchDialogFragment;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.settings.backend.preferences.ListStringPreference;
import net.osmand.plus.settings.backend.preferences.OsmandPreference;
import net.osmand.plus.settings.fragments.BaseSettingsFragment.SettingsScreenType;
import net.osmand.plus.views.mapwidgets.MapWidgetInfo;
import net.osmand.plus.views.mapwidgets.WidgetType;
import net.osmand.plus.views.mapwidgets.widgets.MapWidget;
import net.osmand.plus.widgets.ctxmenu.ContextMenuAdapter;
import net.osmand.render.RenderingRuleProperty;
import net.osmand.search.core.SearchPhrase;

import org.apache.commons.logging.Log;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class OsmandPlugin {

	private static final Log LOG = PlatformUtil.getLog(PluginsHelper.class);

	protected OsmandApplication app;

	protected List<OsmandPreference> pluginPreferences = new ArrayList<>();

	private boolean enabled;
	private String installURL;

	public OsmandPlugin(@NonNull OsmandApplication app) {
		this.app = app;
	}

	public abstract String getId();

	public abstract String getName();

	public abstract CharSequence getDescription();

	@Nullable
	public Drawable getAssetResourceImage() {
		return null;
	}

	@DrawableRes
	public int getLogoResourceId() {
		return R.drawable.ic_extension_dark;
	}

	@NonNull
	public Drawable getLogoResource() {
		return app.getUIUtilities().getIcon(getLogoResourceId());
	}

	public SettingsScreenType getSettingsScreenType() {
		return null;
	}

	public List<OsmandPreference> getPreferences() {
		return pluginPreferences;
	}

	public String getPrefsDescription() {
		return null;
	}

	public int getVersion() {
		return -1;
	}

	/**
	 * Initialize plugin runs just after creation
	 */
	public boolean init(@NonNull OsmandApplication app, @Nullable Activity activity) {
		if (activity != null) {
			// called from UI
			for (ApplicationMode appMode : getAddedAppModes()) {
				ApplicationMode.changeProfileAvailability(appMode, true, app);
			}
		}
		return true;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public boolean isLocked() {
		return needsInstallation();
	}

	public boolean isActive() {
		return isEnabled() && !isLocked();
	}

	public boolean isEnableByDefault() {
		return false;
	}

	public boolean isMarketPlugin() {
		return false;
	}

	public boolean isPaid() {
		return false;
	}

	public boolean needsInstallation() {
		return installURL != null;
	}

	public void setInstallURL(String installURL) {
		this.installURL = installURL;
	}

	public String getInstallURL() {
		return installURL;
	}

	public String getComponentId1() {
		return null;
	}

	public String getComponentId2() {
		return null;
	}

	public List<ApplicationMode> getAddedAppModes() {
		return Collections.emptyList();
	}

	public List<IndexItem> getSuggestedMaps() {
		return Collections.emptyList();
	}

	public List<WorldRegion> getDownloadMaps() {
		return Collections.emptyList();
	}

	public List<String> getRendererNames() {
		return Collections.emptyList();
	}

	public List<String> getRouterNames() {
		return Collections.emptyList();
	}

	protected List<QuickActionType> getQuickActionTypes() {
		return Collections.emptyList();
	}

	protected List<PoiUIFilter> getCustomPoiFilters() {
		return Collections.emptyList();
	}

	protected void attachAdditionalInfoToRecordedTrack(Location location, JSONObject json) throws JSONException {
	}


	protected void collectContextMenuImageCards(@NonNull ImageCardsHolder holder,
	                                            @NonNull Map<String, String> params,
	                                            @Nullable Map<String, String> additionalParams,
	                                            @Nullable GetImageCardsListener listener) {
	}

	protected boolean createContextMenuImageCard(@NonNull ImageCardsHolder holder,
	                                             @NonNull JSONObject imageObject) {
		return false;
	}

	public boolean disablePreferences() {
		return !isActive();
	}

	/**
	 * Plugin was installed
	 */
	public void onInstall(@NonNull OsmandApplication app, @Nullable Activity activity) {
		for (ApplicationMode appMode : getAddedAppModes()) {
			ApplicationMode.changeProfileAvailability(appMode, true, app);
		}
		showInstallDialog(activity);
	}

	public void showInstallDialog(@Nullable Activity activity) {
		if (activity instanceof FragmentActivity) {
			FragmentManager fragmentManager = ((FragmentActivity) activity).getSupportFragmentManager();
			PluginInstalledBottomSheetDialog.showInstance(fragmentManager, getId(), activity instanceof MapActivity);
		}
	}

	public void showDisableDialog(@Nullable Activity activity) {
		if (activity instanceof FragmentActivity) {
			FragmentManager fragmentManager = ((FragmentActivity) activity).getSupportFragmentManager();
			PluginDisabledBottomSheet.showInstance(fragmentManager, getId(), activity instanceof MapActivity);
		}
	}

	public void disable(@NonNull OsmandApplication app) {
		for (ApplicationMode appMode : getAddedAppModes()) {
			ApplicationMode.changeProfileAvailability(appMode, false, app);
		}
	}

	public String getHelpFileName() {
		return null;
	}

	/*
	 * Return true in case if plugin should fill the map context menu with buildContextMenuRows method.
	 */
	public boolean isMenuControllerSupported(Class<? extends MenuController> menuControllerClass) {
		return false;
	}

	/*
	 * Add menu rows to the map context menu.
	 */
	public void buildContextMenuRows(@NonNull MenuBuilder menuBuilder, @NonNull View view, @Nullable Object object) {
	}

	/*
	 * Clear resources after menu was closed
	 */
	public void clearContextMenuRows() {
	}

	protected boolean isAvailable(OsmandApplication app) {
		return PluginsHelper.checkPluginPackage(app, this) || !isPaid();
	}

	protected List<IndexItem> getMapsForType(@NonNull LatLon latLon, @NonNull DownloadActivityType type) {
		try {
			return DownloadResources.findIndexItemsAt(app, latLon, type);
		} catch (IOException e) {
			LOG.error(e);
		}
		return Collections.emptyList();
	}

	public void updateLayers(@NonNull Context context, @Nullable MapActivity mapActivity) {
	}

	public void registerLayers(@NonNull Context context, @Nullable MapActivity mapActivity) {
	}

	public void createWidgets(@NonNull MapActivity mapActivity, @NonNull List<MapWidgetInfo> widgetInfos, @NonNull ApplicationMode appMode) {
	}

	public void mapActivityCreate(@NonNull MapActivity activity) {
	}

	public void mapActivityResume(@NonNull MapActivity activity) {
	}

	public void mapActivityResumeOnTop(@NonNull MapActivity activity) {
	}

	public void mapActivityPause(@NonNull MapActivity activity) {
	}

	public void mapActivityDestroy(@NonNull MapActivity activity) {
	}

	public void mapActivityScreenOff(@NonNull MapActivity activity) {
	}

	public void handleRequestPermissionsResult(int requestCode, String[] permissions,
	                                           int[] grantResults) {
	}

	protected void registerLayerContextMenuActions(@NonNull ContextMenuAdapter adapter, @NonNull MapActivity mapActivity, List<RenderingRuleProperty> customRules) {
	}

	protected void registerConfigureMapCategoryActions(@NonNull ContextMenuAdapter adapter, @NonNull MapActivity mapActivity, @NonNull List<RenderingRuleProperty> customRules) {
	}

	@Nullable
	protected String getRenderPropertyPrefix() {
		return null;
	}

	protected void registerMapContextMenuActions(@NonNull MapActivity mapActivity, double latitude, double longitude,
	                                             ContextMenuAdapter adapter, Object selectedObj, boolean configureMenu) {
	}

	protected void registerOptionsMenuItems(MapActivity mapActivity, ContextMenuAdapter helper) {
	}

	public DashFragmentData getCardFragment() {
		return null;
	}

	public void updateLocation(Location location) {
	}

	protected void addMyPlacesTab(FavoritesActivity favoritesActivity, List<TabItem> mTabs, Intent intent) {
	}

	protected void optionsMenuFragment(FragmentActivity activity, Fragment fragment, ContextMenuAdapter optionsMenuAdapter) {
	}

	protected boolean searchFinished(QuickSearchDialogFragment searchFragment, SearchPhrase phrase, boolean isResultEmpty) {
		return false;
	}

	protected void newDownloadIndexes(Fragment fragment) {
	}

	protected void prepareExtraTopPoiFilters(Set<PoiUIFilter> poiUIFilter) {
	}

	protected String getMapObjectsLocale(Amenity amenity, String preferredLocale) {
		return null;
	}

	protected String getMapObjectPreferredLang(MapObject object, String defaultLanguage) {
		return null;
	}

	protected MapWidget createMapWidgetForParams(@NonNull MapActivity mapActivity, @NonNull WidgetType widgetType) {
		return null;
	}

	public List<String> indexingFiles(@Nullable IProgress progress) {
		return null;
	}

	public boolean mapActivityKeyUp(MapActivity mapActivity, int keyCode) {
		return false;
	}

	public void onMapActivityExternalResult(int requestCode, int resultCode, Intent data) {
	}

	protected CommonPreference<Boolean> registerBooleanPreference(@NonNull String prefId, boolean defValue) {
		CommonPreference<Boolean> preference = app.getSettings().registerBooleanPreference(prefId, defValue);
		preference.setRelatedPlugin(this);
		pluginPreferences.add(preference);
		return preference;
	}

	protected CommonPreference<Boolean> registerBooleanAccessibilityPreference(@NonNull String prefId, boolean defValue) {
		CommonPreference<Boolean> preference = app.getSettings().registerBooleanAccessibilityPreference(prefId, defValue);
		preference.setRelatedPlugin(this);
		pluginPreferences.add(preference);
		return preference;
	}

	protected CommonPreference<String> registerStringPreference(@NonNull String prefId, @Nullable String defValue) {
		CommonPreference<String> preference = app.getSettings().registerStringPreference(prefId, defValue);
		preference.setRelatedPlugin(this);
		pluginPreferences.add(preference);
		return preference;
	}

	protected CommonPreference<Integer> registerIntPreference(@NonNull String prefId, int defValue) {
		CommonPreference<Integer> preference = app.getSettings().registerIntPreference(prefId, defValue);
		preference.setRelatedPlugin(this);
		pluginPreferences.add(preference);
		return preference;
	}

	protected CommonPreference<Long> registerLongPreference(@NonNull String prefId, long defValue) {
		CommonPreference<Long> preference = app.getSettings().registerLongPreference(prefId, defValue);
		preference.setRelatedPlugin(this);
		pluginPreferences.add(preference);
		return preference;
	}

	protected CommonPreference<Float> registerFloatPreference(@NonNull String prefId, float defValue) {
		CommonPreference<Float> preference = app.getSettings().registerFloatPreference(prefId, defValue);
		preference.setRelatedPlugin(this);
		pluginPreferences.add(preference);
		return preference;
	}

	protected <T extends Enum<?>> CommonPreference<T> registerEnumStringPreference(@NonNull String prefId, @NonNull Enum<?> defaultValue,
	                                                                               @NonNull Enum<?>[] values, @NonNull Class<T> clz) {
		CommonPreference<T> preference = app.getSettings().registerEnumStringPreference(prefId, defaultValue, values, clz);
		preference.setRelatedPlugin(this);
		pluginPreferences.add(preference);
		return preference;
	}

	protected ListStringPreference registerListStringPreference(@NonNull String prefId, @Nullable String defValue, @NonNull String delimiter) {
		ListStringPreference preference = app.getSettings().registerStringListPreference(prefId, defValue, delimiter);
		preference.setRelatedPlugin(this);
		pluginPreferences.add(preference);
		return preference;
	}

	protected CommonPreference<String> registerRenderingPreference(@NonNull RenderingRuleProperty property) {
		return registerRenderingPreference(property.getAttrName(), "");
	}

	protected CommonPreference<Boolean> registerBooleanRenderingPreference(@NonNull RenderingRuleProperty property) {
		return registerBooleanRenderingPreference(property.getAttrName(), false);
	}

	protected CommonPreference<String> registerRenderingPreference(@NonNull String prefId, @Nullable String defValue) {
		CommonPreference<String> preference = app.getSettings().registerCustomRenderProperty(prefId, defValue);
		preference.setRelatedPlugin(this);
		pluginPreferences.add(preference);
		return preference;
	}

	private CommonPreference<Boolean> registerBooleanRenderingPreference(@NonNull String prefId, boolean defValue) {
		CommonPreference<Boolean> preference = app.getSettings().registerCustomRenderBooleanProperty(prefId, defValue);
		preference.setRelatedPlugin(this);
		pluginPreferences.add(preference);
		return preference;
	}
}
