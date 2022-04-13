package net.osmand.plus.plugins.development;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.activities.ContributionVersionActivity;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.dashboard.tools.DashFragmentData;
import net.osmand.plus.plugins.OsmandPlugin;
import net.osmand.plus.plugins.openplacereviews.OpenPlaceReviewsPlugin;
import net.osmand.plus.plugins.osmedit.OsmEditingPlugin;
import net.osmand.plus.settings.fragments.BaseSettingsFragment.SettingsScreenType;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.layers.MapInfoLayer;
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.mapwidgets.WidgetParams;
import net.osmand.plus.views.mapwidgets.WidgetsPanel;
import net.osmand.plus.views.mapwidgets.widgets.RightTextInfoWidget;
import net.osmand.plus.widgets.ctxmenu.ContextMenuAdapter;
import net.osmand.plus.widgets.ctxmenu.data.ContextMenuItem;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.DRAWER_BUILDS_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.PLUGIN_OSMAND_DEV;

public class OsmandDevelopmentPlugin extends OsmandPlugin {

	public OsmandDevelopmentPlugin(OsmandApplication app) {
		super(app);
		//ApplicationMode.regWidgetVisibility("fps", new ApplicationMode[0]);
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
	public void registerLayers(@NonNull Context context, @Nullable MapActivity mapActivity) {
		if (mapActivity != null) {
			registerWidget(mapActivity);
		}
	}

	@Override
	public void registerOptionsMenuItems(final MapActivity mapActivity, ContextMenuAdapter helper) {
		if (Version.isDeveloperVersion(mapActivity.getMyApplication())) {
			helper.addItem(new ContextMenuItem(DRAWER_BUILDS_ID)
					.setTitleId(R.string.version_settings, mapActivity)
					.setIcon(R.drawable.ic_action_apk)
					.setListener((uiAdapter, view, item, isChecked) -> {
						final Intent mapIntent = new Intent(mapActivity, ContributionVersionActivity.class);
						mapActivity.startActivityForResult(mapIntent, 0);
						return true;
					}));
		}

	}

	@Override
	public void updateLayers(@NonNull Context context, @Nullable MapActivity mapActivity) {
		if (mapActivity != null) {
			if (isActive()) {
				registerWidget(mapActivity);
			} else {
				MapInfoLayer mapInfoLayer = mapActivity.getMapLayers().getMapInfoLayer();
				if (mapInfoLayer != null && mapInfoLayer.getSideWidget(FPSTextInfoWidget.class) != null) {
					mapInfoLayer.removeSideWidget(mapInfoLayer.getSideWidget(FPSTextInfoWidget.class));
					mapInfoLayer.recreateControls();
				}
			}
		}
	}

	public static class FPSTextInfoWidget extends RightTextInfoWidget {

		private final OsmandMapTileView mapView;

		public FPSTextInfoWidget(@NonNull MapActivity mapActivity) {
			super(mapActivity);
			this.mapView = mapActivity.getMapView();
			updateInfo(null);
		}

		@Override
		public void updateInfo(@Nullable DrawSettings drawSettings) {
			if (!mapView.isMeasureFPS()) {
				mapView.setMeasureFPS(true);
			}
			setText("", (int) mapView.getFPS() + "/" + (int) mapView.getSecondaryFPS() + " FPS");
		}
	}

	private void registerWidget(@NonNull MapActivity mapActivity) {
		MapInfoLayer mapInfoLayer = mapActivity.getMapLayers().getMapInfoLayer();
		if (mapInfoLayer != null && mapInfoLayer.getSideWidget(FPSTextInfoWidget.class) == null) {
			FPSTextInfoWidget fps = new FPSTextInfoWidget(mapActivity);
			fps.setIcons(R.drawable.widget_fps_day, R.drawable.widget_fps_night);
			mapInfoLayer.registerWidget(WidgetParams.FPS, fps, WidgetsPanel.RIGHT);
			mapInfoLayer.recreateControls();
		}
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