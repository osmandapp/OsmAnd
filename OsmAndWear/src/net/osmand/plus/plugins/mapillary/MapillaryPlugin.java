package net.osmand.plus.plugins.mapillary;

import static android.content.Intent.ACTION_VIEW;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.MAPILLARY;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.PLUGIN_MAPILLARY;
import static net.osmand.plus.mapcontextmenu.gallery.ImageCardType.MAPILLARY_AMENITY;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import net.osmand.PlatformUtil;
import net.osmand.data.Amenity;
import net.osmand.map.ITileSource;
import net.osmand.map.TileSourceManager;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.dashboard.DashboardOnMap;
import net.osmand.plus.mapcontextmenu.CollapsableView;
import net.osmand.plus.mapcontextmenu.MenuBuilder;
import net.osmand.plus.mapcontextmenu.MenuController;
import net.osmand.plus.mapcontextmenu.builders.cards.AbstractCard;
import net.osmand.plus.mapcontextmenu.builders.cards.CardsRowBuilder;
import net.osmand.plus.mapcontextmenu.builders.cards.ImageCard;
import net.osmand.plus.mapcontextmenu.builders.cards.NoImagesCard;
import net.osmand.plus.mapcontextmenu.gallery.GalleryController;
import net.osmand.plus.mapcontextmenu.gallery.ImageCardType;
import net.osmand.plus.mapcontextmenu.gallery.ImageCardsHolder;
import net.osmand.plus.mapcontextmenu.gallery.tasks.GetImageCardsTask.GetImageCardsListener;
import net.osmand.plus.plugins.OsmandPlugin;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.settings.backend.preferences.OsmandPreference;
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
import org.json.JSONException;
import org.json.JSONObject;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MapillaryPlugin extends OsmandPlugin {

	public static final String TYPE_MAPILLARY_PHOTO = "mapillary-photo";
	public static final String TYPE_MAPILLARY_CONTRIBUTE = "mapillary-contribute";

	private static final String MAPILLARY_PACKAGE_ID = "com.mapillary.app";

	private static final Log LOG = PlatformUtil.getLog(MapillaryPlugin.class);

	public final OsmandPreference<Boolean> SHOW_MAPILLARY;
	public final OsmandPreference<Boolean> MAPILLARY_FIRST_DIALOG_SHOWN;

	public final CommonPreference<Boolean> USE_MAPILLARY_FILTER;
	public final CommonPreference<String> MAPILLARY_FILTER_USER_KEY;
	public final CommonPreference<String> MAPILLARY_FILTER_USERNAME;
	public final CommonPreference<Long> MAPILLARY_FILTER_FROM_DATE;
	public final CommonPreference<Long> MAPILLARY_FILTER_TO_DATE;
	public final CommonPreference<Boolean> MAPILLARY_FILTER_PANO;
	public final CommonPreference<Boolean> MAPILLARY_PHOTOS_ROW_COLLAPSED;

	private MapActivity mapActivity;

	@Nullable
	private CardsRowBuilder mapillaryCardsRow;
	private List<AbstractCard> mapillaryCards;

	private MapillaryVectorLayer vectorLayer;
	private MapWidgetInfo mapillaryWidgetRegInfo;

	public MapillaryPlugin(OsmandApplication app) {
		super(app);

		SHOW_MAPILLARY = registerBooleanPreference("show_mapillary", false).makeProfile();
		MAPILLARY_FIRST_DIALOG_SHOWN = registerBooleanPreference("mapillary_first_dialog_shown", false).makeGlobal();

		USE_MAPILLARY_FILTER = registerBooleanPreference("use_mapillary_filters", false).makeGlobal().makeShared();
		MAPILLARY_FILTER_USER_KEY = registerStringPreference("mapillary_filter_user_key", "").makeGlobal().makeShared();
		MAPILLARY_FILTER_USERNAME = registerStringPreference("mapillary_filter_username", "").makeGlobal().makeShared();
		MAPILLARY_FILTER_FROM_DATE = registerLongPreference("mapillary_filter_from_date", 0).makeGlobal().makeShared();
		MAPILLARY_FILTER_TO_DATE = registerLongPreference("mapillary_filter_to_date", 0).makeGlobal().makeShared();
		MAPILLARY_FILTER_PANO = registerBooleanPreference("mapillary_filter_pano", false).makeGlobal().makeShared();
		MAPILLARY_PHOTOS_ROW_COLLAPSED = registerBooleanPreference("mapillary_menu_collapsed", true).makeGlobal().makeShared();
	}

	@Override
	public int getLogoResourceId() {
		return R.drawable.ic_action_mapillary;
	}

	@Override
	public Drawable getAssetResourceImage() {
		return app.getUIUtilities().getIcon(R.drawable.mapillary);
	}

	@Override
	public String getId() {
		return PLUGIN_MAPILLARY;
	}

	@Override
	public CharSequence getDescription(boolean linksEnabled) {
		return app.getString(R.string.plugin_mapillary_descr);
	}

	@Override
	public String getName() {
		return app.getString(R.string.mapillary);
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
		vectorLayer = new MapillaryVectorLayer(context);
	}

	@Override
	protected MapWidget createMapWidgetForParams(@NonNull MapActivity mapActivity, @NonNull WidgetType widgetType, @Nullable String customId, @Nullable WidgetsPanel widgetsPanel) {
		if (widgetType == WidgetType.MAPILLARY) {
			return new MapillaryMapWidget(mapActivity, customId, widgetsPanel);
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
			if (SHOW_MAPILLARY.get() || force) {
				vectorSource = settings.getTileSourceByName(TileSourceManager.getMapillaryVectorSource().getName(), false);
			}
			updateLayer(mapView, vectorSource, vectorLayer, 0.62f);
		} else {
			mapView.removeLayer(vectorLayer);
			vectorLayer.setMap(null);
		}
		app.getOsmandMap().getMapLayers().updateMapSource(mapView, null);
	}

	private void updateLayer(OsmandMapTileView mapView, ITileSource mapillarySource, MapTileLayer layer, float layerOrder) {
		if (!Algorithms.objectEquals(mapillarySource, layer.getMap()) || !mapView.isLayerExists(layer)) {
			if (!mapView.isLayerExists(layer)) {
				mapView.addLayer(layer, layerOrder);
			}
			layer.setMap(mapillarySource);
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
				mapActivity.getDashboard().setDashboardVisibility(true, DashboardOnMap.DashboardType.MAPILLARY, AndroidUtils.getCenterViewCoordinates(view));
				return false;
			}

			@Override
			public boolean onContextMenuClick(@Nullable OnDataChangeUiAdapter uiAdapter, @Nullable View view, @NotNull ContextMenuItem item, boolean isChecked) {
				SHOW_MAPILLARY.set(!SHOW_MAPILLARY.get());
				updateMapLayers(mapActivity, mapActivity, false);
				item.setSelected(SHOW_MAPILLARY.get());
				item.setColor(app, SHOW_MAPILLARY.get() ? R.color.osmand_orange : ContextMenuItem.INVALID_ID);
				uiAdapter.onDataSetChanged();
				return false;
			}
		};

		adapter.addItem(new ContextMenuItem(MAPILLARY)
				.setTitleId(R.string.street_level_imagery, mapActivity)
				.setDescription("Mapillary")
				.setSelected(SHOW_MAPILLARY.get())
				.setColor(app, SHOW_MAPILLARY.get() ? R.color.osmand_orange : ContextMenuItem.INVALID_ID)
				.setIcon(R.drawable.ic_action_mapillary)
				.setSecondaryIcon(R.drawable.ic_action_additional_option)
				.setItemDeleteAction(SHOW_MAPILLARY)
				.setListener(listener));
	}

	@Override
	public void createWidgets(@NonNull MapActivity mapActivity, @NonNull List<MapWidgetInfo> widgetsInfos, @NonNull ApplicationMode appMode) {
		WidgetInfoCreator creator = new WidgetInfoCreator(app, appMode);
		MapWidget widget = createMapWidgetForParams(mapActivity, WidgetType.MAPILLARY);
		widgetsInfos.add(creator.createWidgetInfo(widget));
	}

	@Override
	public void buildContextMenuGalleryRows(@NonNull MenuBuilder menuBuilder, @NonNull View view, @Nullable Object object) {
		GalleryController controller = (GalleryController) app.getDialogManager().findController(GalleryController.PROCESS_ID);
		if (controller == null) {
			return;
		}
		boolean nightMode = app.getDaynightHelper().isNightModeForMapControls();
		boolean needUpdateOnly = mapillaryCardsRow != null && mapillaryCardsRow.getMenuBuilder() == menuBuilder;

		mapillaryCardsRow = new CardsRowBuilder(menuBuilder);
		mapillaryCardsRow.build(controller, false, nightMode);

		LinearLayout parent = new LinearLayout(view.getContext());
		parent.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
				LinearLayout.LayoutParams.WRAP_CONTENT));
		parent.setOrientation(LinearLayout.VERTICAL);
		parent.addView(mapillaryCardsRow.getGalleryView());

		CollapsableView collapsableView = new CollapsableView(parent, menuBuilder, MAPILLARY_PHOTOS_ROW_COLLAPSED);
		collapsableView.setCollapseExpandListener(collapsed -> {
			if (!collapsed && mapillaryCards == null) {
				menuBuilder.startLoadingImages();
			}
		});
		menuBuilder.buildRow(view, R.drawable.ic_action_photo_street, null, app.getString(R.string.street_level_imagery), 0, true,
				collapsableView, false, 1, false, null, false);

		if (needUpdateOnly && mapillaryCards != null) {
			mapillaryCardsRow.setCards(mapillaryCards);
		}
	}

	@Override
	public void clearContextMenuRows() {
		mapillaryCards = null;
		mapillaryCardsRow = null;
	}

	public GetImageCardsListener getImageCardsListener() {
		return new GetImageCardsListener() {
			@Override
			public void onTaskStarted() {
				if (mapillaryCardsRow != null) {
					mapillaryCardsRow.onLoadingImage(true);
				}
			}

			@Override
			public void onFinish(ImageCardsHolder cardsHolder) {
				if (mapillaryCardsRow != null) {
					mapillaryCardsRow.onLoadingImage(false);
				}
				List<AbstractCard> cards = new ArrayList<>(cardsHolder.getMapillaryCards());
				if (mapActivity != null && Algorithms.isEmpty(cards)) {
					cards.add(new NoImagesCard(mapActivity));
				}
				if (mapillaryCardsRow != null) {
					mapillaryCardsRow.setCards(cards);
				}
				mapillaryCards = cards;
			}
		};
	}

	@Override
	public boolean isMenuControllerSupported(Class<? extends MenuController> menuControllerClass) {
		return true;
	}

	public void setWidgetVisible(MapActivity mapActivity, boolean visible) {
		if (mapillaryWidgetRegInfo != null) {
			MapWidgetRegistry widgetRegistry = mapActivity.getMapLayers().getMapWidgetRegistry();
			List<ApplicationMode> allModes = ApplicationMode.allPossibleValues();
			for (ApplicationMode mode : allModes) {
				widgetRegistry.enableDisableWidgetForMode(mode, mapillaryWidgetRegInfo, visible, false);
			}
			MapInfoLayer mil = mapActivity.getMapLayers().getMapInfoLayer();
			if (mil != null) {
				mil.recreateControls();
			}
			mapActivity.refreshMap();
		}
	}

	@Override
	protected void collectContextMenuImageCards(@NonNull ImageCardsHolder holder,
	                                            @NonNull Map<String, String> params,
	                                            @Nullable Map<String, String> additionalParams,
	                                            @Nullable GetImageCardsListener listener) {
		if (mapActivity != null && additionalParams != null) {
			String key = additionalParams.get(Amenity.MAPILLARY);
			if (key != null) {
				JSONObject imageObject = MapillaryOsmTagHelper.getImageByKey(key);
				if (imageObject != null) {
					holder.addCard(MAPILLARY_AMENITY, new MapillaryImageCard(mapActivity, imageObject));
				}
				additionalParams.remove(Amenity.MAPILLARY);
			}
			params.putAll(additionalParams);
		}
	}

	@Override
	protected boolean createContextMenuImageCard(@NonNull ImageCardsHolder holder,
	                                             @NonNull JSONObject imageObject) {
		ImageCard imageCard = null;
		if (mapActivity != null) {
			try {
				if (imageObject.has("type")) {
					String type = imageObject.getString("type");
					if (TYPE_MAPILLARY_PHOTO.equals(type)) {
						imageCard = new MapillaryImageCard(mapActivity, imageObject);
					} else if (TYPE_MAPILLARY_CONTRIBUTE.equals(type)) {
						imageCard = new MapillaryContributeCard(mapActivity, imageObject);
					}
				}
			} catch (JSONException e) {
				LOG.error(e);
			}
		}
		if (imageCard != null) {
			holder.addCard(ImageCardType.MAPILLARY, imageCard);
			return true;
		}
		return false;
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

	public static boolean openMapillary(FragmentActivity activity, String imageKey) {
		boolean success = false;
		OsmandApplication app = (OsmandApplication) activity.getApplication();
		if (PluginsHelper.isPackageInstalled(MAPILLARY_PACKAGE_ID, app)) {
			Uri uri = imageKey != null
					? Uri.parse(MessageFormat.format("mapillary://mapillary/photo/{0}?image_key={0}", imageKey))
					: Uri.parse("mapillary://mapillary/capture");
			Intent intent = new Intent(ACTION_VIEW, uri)
					.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			if (!AndroidUtils.startActivityIfSafe(app, intent)) {
				new MapillaryInstallDialogFragment()
						.show(activity.getSupportFragmentManager(), MapillaryInstallDialogFragment.TAG);
			}
			success = true;
		} else {
			new MapillaryInstallDialogFragment().show(activity.getSupportFragmentManager(), MapillaryInstallDialogFragment.TAG);
		}
		return success;
	}

	public static boolean installMapillary(@NonNull OsmandApplication app) {
		app.logEvent("install_mapillary");
		boolean success = execInstall(app, Version.getUrlWithUtmRef(app, MAPILLARY_PACKAGE_ID));
		if (!success) {
			success = execInstall(app, "https://play.google.com/store/apps/details?id=" + MAPILLARY_PACKAGE_ID);
		}
		return success;
	}

	private static boolean execInstall(OsmandApplication app, String url) {
		Intent intent = new Intent(ACTION_VIEW, Uri.parse(url));
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		return AndroidUtils.startActivityIfSafe(app, intent);
	}
}
