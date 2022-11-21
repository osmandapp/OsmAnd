package net.osmand.plus.plugins.development;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.DRAWER_BUILDS_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.PLUGIN_OSMAND_DEV;
import static net.osmand.plus.views.mapwidgets.WidgetType.DEV_CAMERA_DISTANCE;
import static net.osmand.plus.views.mapwidgets.WidgetType.DEV_CAMERA_TILT;
import static net.osmand.plus.views.mapwidgets.WidgetType.DEV_FPS;
import static net.osmand.plus.views.mapwidgets.WidgetType.DEV_TARGET_DISTANCE;
import static net.osmand.plus.views.mapwidgets.WidgetType.DEV_ZOOM_LEVEL;

import android.content.Intent;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.StateChangedListener;
import net.osmand.core.android.MapRendererContext;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.dashboard.tools.DashFragmentData;
import net.osmand.plus.inapp.InAppPurchaseHelper;
import net.osmand.plus.plugins.OsmandPlugin;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.development.widget.CameraDistanceWidget;
import net.osmand.plus.plugins.development.widget.CameraTiltWidget;
import net.osmand.plus.plugins.development.widget.FPSTextInfoWidget;
import net.osmand.plus.plugins.development.widget.TargetDistanceWidget;
import net.osmand.plus.plugins.development.widget.ZoomLevelWidget;
import net.osmand.plus.plugins.openplacereviews.OpenPlaceReviewsPlugin;
import net.osmand.plus.plugins.osmedit.OsmEditingPlugin;
import net.osmand.plus.quickaction.QuickActionType;
import net.osmand.plus.quickaction.actions.LocationSimulationAction;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.WidgetsAvailabilityHelper;
import net.osmand.plus.settings.backend.preferences.OsmandPreference;
import net.osmand.plus.settings.fragments.BaseSettingsFragment.SettingsScreenType;
import net.osmand.plus.views.corenative.NativeCoreContext;
import net.osmand.plus.views.mapwidgets.MapWidgetInfo;
import net.osmand.plus.views.mapwidgets.WidgetInfoCreator;
import net.osmand.plus.views.mapwidgets.WidgetType;
import net.osmand.plus.views.mapwidgets.widgets.MapWidget;
import net.osmand.plus.widgets.ctxmenu.ContextMenuAdapter;
import net.osmand.plus.widgets.ctxmenu.data.ContextMenuItem;

import java.util.ArrayList;
import java.util.List;

public class OsmandDevelopmentPlugin extends OsmandPlugin {

	private final StateChangedListener<Boolean> showHeightmapsListener;

	public final OsmandPreference<Boolean> SHOW_HEIGHTMAPS;

	public OsmandDevelopmentPlugin(OsmandApplication app) {
		super(app);

		ApplicationMode[] noAppMode = {};
		WidgetsAvailabilityHelper.regWidgetVisibility(DEV_FPS, noAppMode);
		WidgetsAvailabilityHelper.regWidgetVisibility(DEV_CAMERA_TILT, noAppMode);
		WidgetsAvailabilityHelper.regWidgetVisibility(DEV_CAMERA_DISTANCE, noAppMode);
		WidgetsAvailabilityHelper.regWidgetVisibility(DEV_ZOOM_LEVEL, noAppMode);
		WidgetsAvailabilityHelper.regWidgetVisibility(DEV_TARGET_DISTANCE, noAppMode);

		SHOW_HEIGHTMAPS = registerBooleanPreference("show_heightmaps", false).makeGlobal().makeShared().cache();

		showHeightmapsListener = change -> {
			MapRendererContext mapContext = NativeCoreContext.getMapRendererContext();
			if (mapContext != null && mapContext.isVectorLayerEnabled()) {
				mapContext.recreateHeightmapProvider();
			}
		};
		SHOW_HEIGHTMAPS.addListener(showHeightmapsListener);
	}

	@Override
	public String getId() {
		return PLUGIN_OSMAND_DEV;
	}

	@Override
	public CharSequence getDescription() {
		return app.getString(R.string.osmand_development_plugin_description);
	}

	@Override
	public String getName() {
		return app.getString(R.string.debugging_and_development);
	}

	@Override
	public String getHelpFileName() {
		return "feature_articles/development_plugin.html";
	}

	@Override
	public void registerOptionsMenuItems(MapActivity mapActivity, ContextMenuAdapter helper) {
		if (Version.isDeveloperVersion(mapActivity.getMyApplication())) {
			Class<?> contributionVersionActivityClass = null;
			try {
				ClassLoader classLoader = OsmandDevelopmentPlugin.class.getClassLoader();
				if (classLoader != null) {
					contributionVersionActivityClass = classLoader.loadClass("net.osmand.plus.activities.ContributionVersionActivity");
				}
			} catch (ClassNotFoundException ignore) {
			}
			Class<?> activityClass = contributionVersionActivityClass;
			if (activityClass != null) {
				helper.addItem(new ContextMenuItem(DRAWER_BUILDS_ID)
						.setTitleId(R.string.version_settings, mapActivity)
						.setIcon(R.drawable.ic_action_apk)
						.setListener((uiAdapter, view, item, isChecked) -> {
							Intent mapIntent = new Intent(mapActivity, activityClass);
							mapActivity.startActivityForResult(mapIntent, 0);
							return true;
						}));
			}
		}
	}

	@Override
	public void createWidgets(@NonNull MapActivity mapActivity, @NonNull List<MapWidgetInfo> widgetsInfos, @NonNull ApplicationMode appMode) {
		WidgetInfoCreator creator = new WidgetInfoCreator(app, appMode);

		MapWidget fpsWidget = createMapWidgetForParams(mapActivity, DEV_FPS);
		widgetsInfos.add(creator.createWidgetInfo(fpsWidget));

		MapWidget cameraTiltWidget = createMapWidgetForParams(mapActivity, DEV_CAMERA_TILT);
		widgetsInfos.add(creator.createWidgetInfo(cameraTiltWidget));

		MapWidget cameraDistanceWidget = createMapWidgetForParams(mapActivity, DEV_CAMERA_DISTANCE);
		widgetsInfos.add(creator.createWidgetInfo(cameraDistanceWidget));

		MapWidget zoomLevelWidget = createMapWidgetForParams(mapActivity, DEV_ZOOM_LEVEL);
		widgetsInfos.add(creator.createWidgetInfo(zoomLevelWidget));

		MapWidget targetDistanceWidget = createMapWidgetForParams(mapActivity, DEV_TARGET_DISTANCE);
		widgetsInfos.add(creator.createWidgetInfo(targetDistanceWidget));
	}

	@Override
	protected MapWidget createMapWidgetForParams(@NonNull MapActivity mapActivity, @NonNull WidgetType widgetType) {
		switch (widgetType) {
			case DEV_FPS:
				return new FPSTextInfoWidget(mapActivity);
			case DEV_CAMERA_TILT:
				return new CameraTiltWidget(mapActivity);
			case DEV_CAMERA_DISTANCE:
				return new CameraDistanceWidget(mapActivity);
			case DEV_ZOOM_LEVEL:
				return new ZoomLevelWidget(mapActivity);
			case DEV_TARGET_DISTANCE:
				return new TargetDistanceWidget(mapActivity);
		}
		return null;
	}

	@Nullable
	@Override
	public SettingsScreenType getSettingsScreenType() {
		return SettingsScreenType.DEVELOPMENT_SETTINGS;
	}

	@Override
	public int getLogoResourceId() {
		return R.drawable.ic_action_laptop;
	}

	@Override
	public Drawable getAssetResourceImage() {
		return app.getUIUtilities().getIcon(R.drawable.osmand_development);
	}

	@Override
	public DashFragmentData getCardFragment() {
		return DashSimulateFragment.FRAGMENT_DATA;
	}

	@Override
	public void disable(@NonNull OsmandApplication app) {
		OsmEditingPlugin osmPlugin = PluginsHelper.getPlugin(OsmEditingPlugin.class);
		if (osmPlugin != null && osmPlugin.OSM_USE_DEV_URL.get()) {
			osmPlugin.OSM_USE_DEV_URL.set(false);
			app.getOsmOAuthHelper().resetAuthorization();
		}
		OpenPlaceReviewsPlugin oprPlugin = PluginsHelper.getPlugin(OpenPlaceReviewsPlugin.class);
		if (oprPlugin != null && oprPlugin.OPR_USE_DEV_URL.get()) {
			oprPlugin.OPR_USE_DEV_URL.set(false);
			app.getOprAuthHelper().resetAuthorization();
		}
		super.disable(app);
	}

	@Override
	protected List<QuickActionType> getQuickActionTypes() {
		List<QuickActionType> quickActionTypes = new ArrayList<>();
		quickActionTypes.add(LocationSimulationAction.TYPE);
		return quickActionTypes;
	}

	public boolean isHeightmapEnabled() {
		return isHeightmapAllowed() && SHOW_HEIGHTMAPS.get();
	}

	public boolean isHeightmapAllowed() {
		return app.useOpenGlRenderer() && isHeightmapPurchased();
	}

	public boolean isHeightmapPurchased() {
		return InAppPurchaseHelper.isOsmAndProAvailable(app);
	}
}