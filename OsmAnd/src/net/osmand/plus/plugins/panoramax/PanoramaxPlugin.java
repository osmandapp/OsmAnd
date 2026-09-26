package net.osmand.plus.plugins.panoramax;

import static android.content.Intent.ACTION_VIEW;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.PANORAMAX;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.PLUGIN_PANORAMAX;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import net.osmand.PlatformUtil;
import net.osmand.core.android.PanoramaxTilesProvider;
import net.osmand.map.ITileSource;
import net.osmand.map.TileSourceManager;
import net.osmand.map.TileSourceManager.TileSourceTemplate;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.dashboard.DashboardType;
import net.osmand.plus.mapcontextmenu.MenuController;
import net.osmand.plus.plugins.OsmandPlugin;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.settings.backend.preferences.OsmandPreference;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.layers.MapTileLayer;
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

	public final CommonPreference<Boolean> USE_PANORAMAX_FILTER;
	public final CommonPreference<String> PANORAMAX_FILTER_USER_KEY;
	public final CommonPreference<String> PANORAMAX_FILTER_USERNAME;
	public final CommonPreference<Long> PANORAMAX_FILTER_FROM_DATE;
	public final CommonPreference<Long> PANORAMAX_FILTER_TO_DATE;
	public final CommonPreference<Boolean> PANORAMAX_FILTER_PANO;

	// Local metadata identifying the filter state used to render the raster cache.
	public final CommonPreference<String> PANORAMAX_RASTER_CACHE_KEY;

	private MapActivity mapActivity;

	@Nullable
	private PanoramaxVectorLayer vectorLayer;

	public PanoramaxPlugin(OsmandApplication app) {
		super(app);

		SHOW_PANORAMAX = registerBooleanPreference("show_panoramax", false).makeProfile();

		USE_PANORAMAX_FILTER = registerBooleanPreference("use_panoramax_filters", false).makeGlobal().makeShared();
		PANORAMAX_FILTER_USER_KEY = registerStringPreference("panoramax_filter_user_key", "").makeGlobal().makeShared();
		PANORAMAX_FILTER_USERNAME = registerStringPreference("panoramax_filter_username", "").makeGlobal().makeShared();
		PANORAMAX_FILTER_FROM_DATE = registerLongPreference("panoramax_filter_from_date", 0).makeGlobal().makeShared();
		PANORAMAX_FILTER_TO_DATE = registerLongPreference("panoramax_filter_to_date", 0).makeGlobal().makeShared();
		PANORAMAX_FILTER_PANO = registerBooleanPreference("panoramax_filter_pano", false).makeGlobal().makeShared();

		PANORAMAX_RASTER_CACHE_KEY = registerStringPreference("panoramax_raster_cache_key", "").makeGlobal();
	}

	public void reload() {
		if (vectorLayer != null) {
			vectorLayer.reload();
		} else {
			PanoramaxTilesProvider.clearRasterCache(app);
		}
	}

	@Override
	public int getLogoResourceId() {
		// TODO: Replace with a dedicated Panoramax icon when artwork is available.
		return R.drawable.ic_action_photo_street;
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
				applyCanonicalExpiration(vectorSource);
			}
			// zOrder maps to a native layer slot as (int) (zOrder * 100), so it must differ from
			// Mapillary's 0.62f or the two layers overwrite each other's provider under OpenGL.
			updateLayer(mapView, vectorSource, vectorLayer, 0.63f);
		} else {
			mapView.removeLayer(vectorLayer);
			vectorLayer.setMap(null);
		}
		app.getResourceManager().getMapillaryVectorTilesCache()
				.setPanoramaxActive(vectorLayer.getMap() != null);
		app.getOsmandMap().getMapLayers().updateMapSource(mapView, null);
	}

	/**
	 * A source restored without expiration metadata comes back as "never expires", which would
	 * take Panoramax off the one-day policy. Pinning the canonical value here also keeps
	 * TilesCache and the raster provider aligned: both read the expiration from this object.
	 */
	private static void applyCanonicalExpiration(@Nullable ITileSource source) {
		if (source instanceof TileSourceTemplate template) {
			template.setExpirationTimeMillis(
					TileSourceManager.getPanoramaxVectorSource().getExpirationTimeMillis());
		}
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

		// ConfigureMapFragment uses titleId as a view-cache key, so it must be unique.
		adapter.addItem(new ContextMenuItem(PANORAMAX)
				.setTitleId(R.string.panoramax, mapActivity)
				.setDescription(app.getString(R.string.street_level_imagery))
				.setSelected(SHOW_PANORAMAX.get())
				.setColor(app, SHOW_PANORAMAX.get() ? R.color.osmand_orange : ContextMenuItem.INVALID_ID)
				.setIcon(R.drawable.ic_action_photo_street)
				.setSecondaryIcon(R.drawable.ic_action_additional_option)
				.setItemDeleteAction(SHOW_PANORAMAX)
				.setListener(listener));
	}

	// No context menu gallery row: OnlinePhotosGroup has no Panoramax member, so it could only
	// ever render empty. Pictures are reached by tapping the map layer instead.

	@Override
	public boolean isMenuControllerSupported(MenuController menuController) {
		return true;
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
