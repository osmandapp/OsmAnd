package net.osmand.plus.plugins.panoramax;

import static android.content.Intent.ACTION_VIEW;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.PANORAMAX;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.PLUGIN_PANORAMAX;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import net.osmand.PlatformUtil;
import net.osmand.map.ITileSource;
import net.osmand.map.TileSourceManager;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.dashboard.DashboardType;
import net.osmand.plus.mapcontextmenu.MenuController;
import net.osmand.plus.plugins.OsmandPlugin;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.settings.backend.preferences.OsmandPreference;
import net.osmand.plus.settings.enums.ScreenLayoutMode;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.layers.MapInfoLayer;
import net.osmand.plus.views.layers.MapTileLayer;
import net.osmand.plus.views.mapwidgets.MapWidgetInfo;
import net.osmand.plus.views.mapwidgets.MapWidgetRegistry;
import net.osmand.plus.views.mapwidgets.WidgetInfoCreator;
import net.osmand.plus.views.mapwidgets.WidgetType;
import net.osmand.plus.views.mapwidgets.WidgetsPanel;
import net.osmand.plus.views.mapwidgets.widgets.MapWidget;
import net.osmand.plus.widgets.ctxmenu.ContextMenuAdapter;
import net.osmand.plus.widgets.ctxmenu.callback.ItemClickListener;
import net.osmand.plus.widgets.ctxmenu.callback.OnDataChangeUiAdapter;
import net.osmand.plus.widgets.ctxmenu.callback.OnRowItemClick;
import net.osmand.plus.widgets.ctxmenu.data.ContextMenuItem;
import net.osmand.render.RenderingRuleProperty;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class PanoramaxPlugin extends OsmandPlugin {



	private static final Log LOG = PlatformUtil.getLog(PanoramaxPlugin.class);

	public final OsmandPreference<Boolean> SHOW_PANORAMAX;
	public final OsmandPreference<Boolean> PANORAMAX_FIRST_DIALOG_SHOWN;

	public final CommonPreference<Boolean> USE_PANORAMAX_FILTER;
	public final CommonPreference<String> PANORAMAX_FILTER_USER_KEY;
	public final CommonPreference<String> PANORAMAX_FILTER_USERNAME;
	public final CommonPreference<Long> PANORAMAX_FILTER_FROM_DATE;
	public final CommonPreference<Long> PANORAMAX_FILTER_TO_DATE;
	public final CommonPreference<Boolean> PANORAMAX_FILTER_PANO;

	private MapActivity mapActivity;

	@Nullable
	private PanoramaxVectorLayer vectorLayer;
	private MapWidgetInfo panoramaxWidgetRegInfo;

	public PanoramaxPlugin(OsmandApplication app) {
		super(app);

		SHOW_PANORAMAX = registerBooleanPreference("show_panoramax", false).makeProfile();
		PANORAMAX_FIRST_DIALOG_SHOWN = registerBooleanPreference("panoramax_first_dialog_shown", false).makeGlobal();

		USE_PANORAMAX_FILTER = registerBooleanPreference("use_panoramax_filters", false).makeGlobal().makeShared();
		PANORAMAX_FILTER_USER_KEY = registerStringPreference("panoramax_filter_user_key", "").makeGlobal().makeShared();
		PANORAMAX_FILTER_USERNAME = registerStringPreference("panoramax_filter_username", "").makeGlobal().makeShared();
		PANORAMAX_FILTER_FROM_DATE = registerLongPreference("panoramax_filter_from_date", 0).makeGlobal().makeShared();
		PANORAMAX_FILTER_TO_DATE = registerLongPreference("panoramax_filter_to_date", 0).makeGlobal().makeShared();
		PANORAMAX_FILTER_PANO = registerBooleanPreference("panoramax_filter_pano", false).makeGlobal().makeShared();
	}

	@Override
	public int getLogoResourceId() {
		return R.drawable.ic_action_panoramax;
	}

	@Override
	public Drawable getAssetResourceImage() {
		// Placeholder: OsmAnd ships a branded illustration per plugin (see drawable/mapillary.webp).
		// Panoramax artwork has to be supplied before this can go upstream.
		return app.getUIUtilities().getIcon(R.drawable.ic_action_photo_street);
	}

	@Override
	public String getId() {
		return PLUGIN_PANORAMAX;
	}

	@Override
	public CharSequence getDescription(boolean linksEnabled) {
		return app.getString(R.string.plugin_panoramax_descr);
	}

	@Override
	public String getName() {
		return app.getString(R.string.panoramax);
	}

	@Override
	public boolean isEnableByDefault() {
		return false;
	}

	@Override
	public boolean init(@NonNull OsmandApplication app, Activity activity) {
		if (activity instanceof MapActivity) {
			mapActivity = (MapActivity) activity;
		}
		return true;
	}

	@Override
	public void registerLayers(@NonNull Context context, @Nullable MapActivity mapActivity) {
		if (vectorLayer != null) {
			app.getOsmandMap().getMapView().removeLayer(vectorLayer);
		}
		createLayers(context);
	}

	private void createLayers(@NonNull Context context) {
		vectorLayer = new PanoramaxVectorLayer(context);
	}

	@Override
	protected MapWidget createMapWidgetForParams(@NonNull MapActivity mapActivity, @NonNull WidgetType widgetType, @Nullable String customId, @Nullable WidgetsPanel widgetsPanel) {
		if (widgetType == WidgetType.PANORAMAX) {
			return new PanoramaxMapWidget(mapActivity, customId, widgetsPanel);
		}
		return null;
	}

	@Override
	public void updateLayers(@NonNull Context context, @Nullable MapActivity mapActivity) {
		updateMapLayers(context, mapActivity, false);
	}

	public void updateLayers(@NonNull Context context, @Nullable MapActivity mapActivity, boolean force) {
		updateMapLayers(context, mapActivity, force);
	}

	private void updateMapLayers(@NonNull Context context, @Nullable MapActivity mapActivity, boolean force) {
		if (vectorLayer == null) {
			createLayers(context);
		}
		OsmandApplication app = (OsmandApplication) context.getApplicationContext();
		OsmandSettings settings = app.getSettings();
		OsmandMapTileView mapView = app.getOsmandMap().getMapView();
		if (isActive()) {
			ITileSource vectorSource = null;
			if (SHOW_PANORAMAX.get() || force) {
				vectorSource = settings.getTileSourceByName(TileSourceManager.getPanoramaxVectorSource().getName(), false);
			}
			updateLayer(mapView, vectorSource, vectorLayer, 0.62f);
		} else {
			mapView.removeLayer(vectorLayer);
			vectorLayer.setMap(null);
		}
		app.getOsmandMap().getMapLayers().updateMapSource(mapView, null);
	}

	private void updateLayer(OsmandMapTileView mapView, ITileSource panoramaxSource, MapTileLayer layer, float layerOrder) {
		if (!Algorithms.objectEquals(panoramaxSource, layer.getMap()) || !mapView.isLayerExists(layer)) {
			if (!mapView.isLayerExists(layer)) {
				mapView.addLayer(layer, layerOrder);
			}
			layer.setMap(panoramaxSource);
			mapView.refreshMap();
		}
	}

	@Override
	protected void registerLayerContextMenuActions(@NonNull ContextMenuAdapter adapter, @NonNull MapActivity mapActivity, @NonNull List<RenderingRuleProperty> customRules) {
		if (!isEnabled()) {
			return;
		}
		ItemClickListener listener = new OnRowItemClick() {

			@Override
			public boolean onRowItemClick(@NonNull OnDataChangeUiAdapter uiAdapter,
			                              @NonNull View view, @NonNull ContextMenuItem item) {
				mapActivity.getDashboard().setDashboardVisibility(true, DashboardType.PANORAMAX, AndroidUtils.getCenterViewCoordinates(view));
				return false;
			}

			@Override
			public boolean onContextMenuClick(@Nullable OnDataChangeUiAdapter uiAdapter, @Nullable View view, @NotNull ContextMenuItem item, boolean isChecked) {
				SHOW_PANORAMAX.set(!SHOW_PANORAMAX.get());
				updateMapLayers(mapActivity, mapActivity, false);
				item.setSelected(SHOW_PANORAMAX.get());
				item.setColor(app, SHOW_PANORAMAX.get() ? R.color.osmand_orange : ContextMenuItem.INVALID_ID);
				uiAdapter.onDataSetChanged();
				return false;
			}
		};

		adapter.addItem(new ContextMenuItem(PANORAMAX)
				.setTitleId(R.string.street_level_imagery, mapActivity)
				.setDescription("Panoramax")
				.setSelected(SHOW_PANORAMAX.get())
				.setColor(app, SHOW_PANORAMAX.get() ? R.color.osmand_orange : ContextMenuItem.INVALID_ID)
				.setIcon(R.drawable.ic_action_panoramax)
				.setSecondaryIcon(R.drawable.ic_action_additional_option)
				.setItemDeleteAction(SHOW_PANORAMAX)
				.setListener(listener));
	}

	@Override
	public void createWidgets(@NonNull MapActivity mapActivity, @NonNull List<MapWidgetInfo> widgetsInfos,
			@NonNull ApplicationMode appMode, @Nullable ScreenLayoutMode layoutMode) {
		WidgetInfoCreator creator = new WidgetInfoCreator(app, appMode, layoutMode);
		MapWidget widget = createMapWidgetForParams(mapActivity, WidgetType.PANORAMAX);
		widgetsInfos.add(creator.createWidgetInfo(widget));
	}

		// No context menu gallery row here, unlike MapillaryPlugin. That row is fed by OsmAnd's
	// own online photos service through OnlinePhotosHolder, whose OnlinePhotosGroup enum has
	// only MAPILLARY, WIKIDATA, WIKIMEDIA and OTHER members. Nothing server side supplies
	// Panoramax photos for a place, so such a row could only ever render empty. Pictures are
	// reached by tapping the map layer instead.

	@Override
	public boolean isMenuControllerSupported(MenuController menuController) {
		return true;
	}

	public void setWidgetVisible(MapActivity mapActivity, boolean visible) {
		if (panoramaxWidgetRegInfo != null) {
			MapWidgetRegistry widgetRegistry = mapActivity.getMapLayers().getMapWidgetRegistry();
			List<ApplicationMode> allModes = ApplicationMode.allPossibleValues();
			ScreenLayoutMode layoutMode = ScreenLayoutMode.getDefault(mapActivity);
			for (ApplicationMode mode : allModes) {
				widgetRegistry.enableDisableWidgetForMode(mode, panoramaxWidgetRegInfo, visible, layoutMode, false);
			}
			MapInfoLayer mil = mapActivity.getMapLayers().getMapInfoLayer();
			if (mil != null) {
				mil.recreateControls();
			}
			mapActivity.refreshMap();
		}
	}

	@Override
	public void mapActivityResume(@NonNull MapActivity activity) {
		this.mapActivity = activity;
	}

	@Override
	public void mapActivityResumeOnTop(@NonNull MapActivity activity) {
		this.mapActivity = activity;
	}

	@Override
	public void mapActivityPause(@NonNull MapActivity activity) {
		this.mapActivity = null;
	}

	public static boolean openPanoramax(@NonNull FragmentActivity activity) {
		return openPanoramax(activity, null);
	}

	/**
	 * Opens the picture in the Panoramax web viewer.
	 *
	 * Unlike Mapillary there is no Panoramax Android app to hand off to and no store listing to
	 * fall back on, so there is deliberately no "install the app" flow here. The viewer selects a
	 * picture through URL hash parameters; an instance whose viewer does not understand them just
	 * opens at its default position, so an unknown parameter degrades to a working map rather
	 * than to an error.
	 */
	public static boolean openPanoramax(@NonNull FragmentActivity activity, @Nullable String imageId) {
		OsmandApplication app = (OsmandApplication) activity.getApplication();
		String url = imageId != null
				? PanoramaxConstants.getViewerUrl(imageId)
				: PanoramaxConstants.INSTANCE_URL;
		Intent intent = new Intent(ACTION_VIEW, Uri.parse(url))
				.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		return AndroidUtils.startActivityIfSafe(app, intent);
	}
}
