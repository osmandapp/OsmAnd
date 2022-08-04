package net.osmand.plus.plugins.development;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.DRAWER_BUILDS_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.PLUGIN_OSMAND_DEV;
import static net.osmand.plus.views.mapwidgets.WidgetType.FPS;

import android.content.Intent;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.dashboard.tools.DashFragmentData;
import net.osmand.plus.plugins.OsmandPlugin;
import net.osmand.plus.plugins.openplacereviews.OpenPlaceReviewsPlugin;
import net.osmand.plus.plugins.osmedit.OsmEditingPlugin;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.fragments.BaseSettingsFragment.SettingsScreenType;
import net.osmand.plus.views.mapwidgets.MapWidgetInfo;
import net.osmand.plus.views.mapwidgets.MapWidgetRegistry;
import net.osmand.plus.views.mapwidgets.WidgetType;
import net.osmand.plus.views.mapwidgets.widgets.MapWidget;
import net.osmand.plus.widgets.ctxmenu.ContextMenuAdapter;
import net.osmand.plus.widgets.ctxmenu.data.ContextMenuItem;

import java.util.List;

public class OsmandDevelopmentPlugin extends OsmandPlugin {

	public OsmandDevelopmentPlugin(OsmandApplication app) {
		super(app);
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
		MapWidgetRegistry widgetRegistry = app.getOsmandMap().getMapLayers().getMapWidgetRegistry();
		MapWidget widget = createMapWidgetForParams(mapActivity, FPS);
		widgetsInfos.add(widgetRegistry.createWidgetInfo(widget, appMode));
	}

	@Override
	protected MapWidget createMapWidgetForParams(@NonNull MapActivity mapActivity, @NonNull WidgetType widgetType) {
		if (widgetType == FPS) {
			return new FPSTextInfoWidget(mapActivity);
		}
		return null;
	}

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
	public void disable(OsmandApplication app) {
		OsmEditingPlugin osmPlugin = OsmandPlugin.getPlugin(OsmEditingPlugin.class);
		if (osmPlugin != null && osmPlugin.OSM_USE_DEV_URL.get()) {
			osmPlugin.OSM_USE_DEV_URL.set(false);
			app.getOsmOAuthHelper().resetAuthorization();
		}
		OpenPlaceReviewsPlugin oprPlugin = OsmandPlugin.getPlugin(OpenPlaceReviewsPlugin.class);
		if (oprPlugin != null && oprPlugin.OPR_USE_DEV_URL.get()) {
			oprPlugin.OPR_USE_DEV_URL.set(false);
			app.getOprAuthHelper().resetAuthorization();
		}
		super.disable(app);
	}
}