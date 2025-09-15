package net.osmand.plus.mapcontextmenu;

import static net.osmand.plus.download.DownloadValidationManager.MAXIMUM_AVAILABLE_FREE_DOWNLOADS;

import android.graphics.drawable.Drawable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Pair;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.Location;
import net.osmand.NativeLibrary.RenderedObject;
import net.osmand.OnResultCallback;
import net.osmand.aidl.AidlMapPointWrapper;
import net.osmand.binary.BinaryMapDataObject;
import net.osmand.core.android.MapRendererView;
import net.osmand.data.*;
import net.osmand.map.OsmandRegions;
import net.osmand.map.WorldRegion;
import net.osmand.plus.OsmAndTaskManager;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.avoidroads.AvoidRoadInfo;
import net.osmand.plus.chooseplan.ChoosePlanFragment;
import net.osmand.plus.chooseplan.OsmAndFeature;
import net.osmand.plus.download.DownloadActivityType;
import net.osmand.plus.download.DownloadIndexesThread;
import net.osmand.plus.download.DownloadValidationManager;
import net.osmand.plus.download.IndexItem;
import net.osmand.plus.helpers.TargetPoint;
import net.osmand.plus.mapcontextmenu.MenuBuilder.CollapseExpandListener;
import net.osmand.plus.mapcontextmenu.controllers.*;
import net.osmand.plus.mapcontextmenu.controllers.SelectedGpxMenuController.SelectedGpxPoint;
import net.osmand.plus.mapcontextmenu.other.ShareMenu;
import net.osmand.plus.mapmarkers.MapMarker;
import net.osmand.plus.plugins.OsmandPlugin;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.aistracker.AisObject;
import net.osmand.plus.plugins.aistracker.AisObjectMenuController;
import net.osmand.plus.plugins.audionotes.AudioVideoNoteMenuController;
import net.osmand.plus.plugins.audionotes.Recording;
import net.osmand.plus.plugins.mapillary.MapillaryImage;
import net.osmand.plus.plugins.mapillary.MapillaryMenuController;
import net.osmand.plus.plugins.osmedit.OsmBugsLayer.OpenStreetNote;
import net.osmand.plus.plugins.osmedit.data.OsmPoint;
import net.osmand.plus.plugins.osmedit.menu.EditPOIMenuController;
import net.osmand.plus.plugins.osmedit.menu.OsmBugMenuController;
import net.osmand.plus.plugins.parking.ParkingPositionMenuController;
import net.osmand.plus.plugins.srtm.SRTMPlugin;
import net.osmand.plus.resources.SearchOsmandRegionTask;
import net.osmand.plus.search.history.HistoryEntry;
import net.osmand.plus.track.clickable.ClickableWay;
import net.osmand.plus.track.helpers.GpxDisplayItem;
import net.osmand.plus.transport.TransportStopRoute;
import net.osmand.plus.utils.NativeUtilities;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.layers.DownloadedRegionsLayer.DownloadMapObject;
import net.osmand.plus.views.layers.PlaceDetailsObject;
import net.osmand.plus.views.mapwidgets.TopToolbarController;
import net.osmand.router.network.NetworkRouteSelector.RouteKey;
import net.osmand.shared.gpx.primitives.WptPt;
import net.osmand.util.OpeningHoursParser.OpeningHours;

import java.util.LinkedList;
import java.util.List;

public abstract class MenuController extends BaseMenuController implements CollapseExpandListener {

	public static class MenuState {
		public static final int HEADER_ONLY = 1;
		public static final int HALF_SCREEN = 2;
		public static final int FULL_SCREEN = 4;
	}

	public enum MenuType {
		STANDARD,
		MULTI_LINE
	}

	protected MapContextMenu mapContextMenu;
	protected MenuBuilder builder;
	private int currentMenuState;
	private MenuType menuType = MenuType.STANDARD;
	private PointDescription pointDescription;
	private LatLon latLon;
	private boolean active;

	protected TitleButtonController leftTitleButtonController;
	protected TitleButtonController rightTitleButtonController;
	protected TitleButtonController bottomTitleButtonController;

	protected TitleButtonController leftDownloadButtonController;
	protected TitleButtonController rightDownloadButtonController;
	protected List<Pair<TitleButtonController, TitleButtonController>> additionalButtonsControllers;
	protected TitleProgressController titleProgressController;

	protected TopToolbarController toolbarController;

	protected IndexItem indexItem;
	protected boolean downloaded;
	private BinaryMapDataObject downloadMapDataObject;
	private WorldRegion downloadRegion;
	private DownloadIndexesThread downloadThread;

	protected List<OpeningHours.Info> openingHoursInfo;

	public MenuController(MenuBuilder builder, PointDescription pointDescription, @NonNull MapActivity mapActivity) {
		super(mapActivity);
		this.pointDescription = pointDescription;
		this.builder = builder;
		this.builder.setCollapseExpandListener(this);
		this.currentMenuState = getInitialMenuState();
		this.builder.setLight(isLight());
	}

	@NonNull
	public MenuBuilder getBuilder() {
		return builder;
	}

	protected void onCreated() {
	}

	@Override
	public void onCollapseExpand(boolean collapsed) {
		if (mapContextMenu != null) {
			mapContextMenu.updateLayout();
		}
	}

	public String getPreferredMapLangLC() {
		return getPreferredMapLang().toLowerCase();
	}

	public String getPreferredMapLang() {
		return builder.getPreferredMapLang();
	}

	public String getPreferredMapAppLang() {
		return builder.getPreferredMapAppLang();
	}

	public boolean isTransliterateNames() {
		return builder.isTransliterateNames();
	}

	public void setMapContextMenu(@Nullable MapContextMenu mapContextMenu) {
		this.mapContextMenu = mapContextMenu;
		builder.setMapContextMenu(mapContextMenu);
	}

	public void build(ViewGroup rootView) {
		for (OsmandPlugin plugin : PluginsHelper.getEnabledPlugins()) {
			if (plugin.isMenuControllerSupported(this)) {
				builder.addMenuPlugin(plugin);
			}
		}
		builder.build(rootView, getObject());
	}

	@Override
	public void updateNightMode() {
		super.updateNightMode();
		if (builder != null) {
			builder.setLight(!nightMode);
		}
	}

	public static MenuController getMenuController(@NonNull MapActivity mapActivity,
	                                               @NonNull LatLon latLon,
	                                               @NonNull PointDescription pointDescription,
	                                               @Nullable Object object,
	                                               @NonNull MenuType menuType) {
		MenuController menuController = null;
		if (object != null) {
			if (object instanceof Amenity) {
				menuController = new AmenityMenuController(mapActivity, pointDescription, (Amenity) object);
			} else if (object instanceof FavouritePoint point) {
				if (pointDescription.isParking() || SpecialPointType.PARKING == point.getSpecialPointType()) {
					menuController = new ParkingPositionMenuController(mapActivity, pointDescription, point);
				} else {
					menuController = new FavouritePointMenuController(mapActivity, pointDescription, point, null);
				}
			} else if (object instanceof HistoryEntry) {
				menuController = new HistoryMenuController(mapActivity, pointDescription, (HistoryEntry) object);
			} else if (object instanceof TargetPoint) {
				menuController = new TargetPointMenuController(mapActivity, pointDescription, (TargetPoint) object);
			} else if (object instanceof Recording) {
				menuController = new AudioVideoNoteMenuController(mapActivity, pointDescription, (Recording) object);
			} else if (object instanceof OsmPoint) {
				menuController = new EditPOIMenuController(mapActivity, pointDescription, (OsmPoint) object);
			} else if (object instanceof WptPt) {
				menuController = WptPtMenuController.getInstance(mapActivity, pointDescription, (WptPt) object, null);
			} else if (object instanceof DownloadMapObject) {
				menuController = new MapDataMenuController(mapActivity, pointDescription, (DownloadMapObject) object);
			} else if (object instanceof OpenStreetNote) {
				menuController = new OsmBugMenuController(mapActivity, pointDescription, (OpenStreetNote) object);
			} else if (object instanceof GpxDisplayItem) {
				menuController = new GpxItemMenuController(mapActivity, pointDescription, (GpxDisplayItem) object);
			} else if (object instanceof MapMarker) {
				menuController = new MapMarkerMenuController(mapActivity, pointDescription, (MapMarker) object);
			} else if (object instanceof TransportStopRoute) {
				menuController = new TransportRouteController(mapActivity, pointDescription, (TransportStopRoute) object);
			} else if (object instanceof TransportStop) {
				menuController = new TransportStopController(mapActivity, pointDescription, (TransportStop) object);
			} else if (object instanceof AidlMapPointWrapper) {
				menuController = new AMapPointMenuController(mapActivity, pointDescription, (AidlMapPointWrapper) object);
			} else if (object instanceof LatLon) {
				if (pointDescription.isMyLocation()) {
					menuController = new MyLocationMenuController(mapActivity, pointDescription);
				}
			} else if (object instanceof AvoidRoadInfo) {
				menuController = new ImpassibleRoadsMenuController(mapActivity, pointDescription, (AvoidRoadInfo) object);
			} else if (object instanceof RenderedObject) {
				menuController = new RenderedObjectMenuController(mapActivity, pointDescription, (RenderedObject) object);
			} else if (object instanceof MapillaryImage) {
				menuController = new MapillaryMenuController(mapActivity, pointDescription, (MapillaryImage) object);
			} else if (object instanceof AisObject) {
				menuController = new AisObjectMenuController(mapActivity, pointDescription, (AisObject) object);
			} else if (object instanceof SelectedGpxPoint) {
				menuController = new SelectedGpxMenuController(mapActivity, pointDescription, (SelectedGpxPoint) object);
			} else if (object instanceof ClickableWay) {
				SelectedGpxPoint point = ((ClickableWay) object).getSelectedGpxPoint();
				menuController = new SelectedGpxMenuController(mapActivity, pointDescription, point);
			} else if (object instanceof BaseDetailsObject detailsObject) {
				if (detailsObject instanceof PlaceDetailsObject placeDetailsObject) {
					WptPt wptPt = placeDetailsObject.getWptPt();
					if (wptPt != null) {
						menuController = WptPtMenuController.getInstance(mapActivity, pointDescription, wptPt, placeDetailsObject);
					}
					FavouritePoint point = placeDetailsObject.getFavouritePoint();
					if (point != null) {
						if (pointDescription.isParking() || SpecialPointType.PARKING == point.getSpecialPointType()) {
							menuController = new ParkingPositionMenuController(mapActivity, pointDescription, point);
						} else {
							menuController = new FavouritePointMenuController(mapActivity, pointDescription, point, detailsObject.getSyntheticAmenity());
						}
					}
				}
				if (menuController == null) {
					menuController = new PlaceDetailsMenuController(mapActivity, pointDescription, detailsObject);
				}
			} else if (object instanceof Pair) {
				Pair<?, ?> pair = (Pair<?, ?>) object;
				if (pair.second instanceof SelectedGpxPoint) {
					menuController = new SelectedGpxMenuController(mapActivity, pointDescription,
							(SelectedGpxPoint) ((Pair<?, ?>) object).second);
				} else if (pair.first instanceof RouteKey && pair.second instanceof QuadRect) {
					menuController = new NetworkRouteMenuController(mapActivity, pointDescription, pair);
				}
			}
		}
		if (menuController == null) {
			menuController = new PointDescriptionMenuController(mapActivity, pointDescription);
		}
		menuController.menuType = menuType;
		menuController.setLatLon(latLon);
		menuController.onCreated();
		return menuController;
	}

	public void update(PointDescription pointDescription, Object object) {
		setPointDescription(pointDescription);
		setObject(object);
	}

	protected void setPointDescription(PointDescription pointDescription) {
		this.pointDescription = pointDescription;
	}

	protected abstract void setObject(Object object);

	protected abstract Object getObject();

	protected Object getCorrespondingMapObject() {
		return null;
	}

	public boolean isActive() {
		return active;
	}

	public boolean setActive(boolean active) {
		this.active = active;
		return true;
	}

	public void addPlainMenuItem(int iconId, String buttonText, String text, boolean needLinks, boolean isUrl, OnClickListener onClickListener) {
		builder.addPlainMenuItem(iconId, buttonText, text, needLinks, isUrl, onClickListener);
	}

	public void clearPlainMenuItems() {
		builder.clearPlainMenuItems();
	}

	public void addPlainMenuItems(String typeStr, PointDescription pointDescription, LatLon latLon) {
		if (pointDescription.isMyLocation()) {
			addSpeedToPlainItems();
			addAltitudeToPlainItems();
			addPrecisionToPlainItems();
		}
	}

	protected void addSpeedToPlainItems() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			OsmandApplication app = mapActivity.getApp();
			Location l = app.getLocationProvider().getLastKnownLocation();
			if (l != null && l.hasSpeed() && l.getSpeed() > 0f) {
				String speed = OsmAndFormatter.getFormattedSpeed(l.getSpeed(), app);
				addPlainMenuItem(R.drawable.ic_action_speed, null, speed, false, false, null);
			}
		}
	}

	protected void addAltitudeToPlainItems() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			OsmandApplication app = mapActivity.getApp();
			Location l = app.getLocationProvider().getLastKnownLocation();
			if (l != null && l.hasAltitude()) {
				String alt = OsmAndFormatter.getFormattedAlt(l.getAltitude(), app);
				addPlainMenuItem(R.drawable.ic_action_altitude_average, null, alt, false, false, null);
			}
		}
	}

	protected void addPrecisionToPlainItems() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			OsmandApplication app = mapActivity.getApp();
			Location l = app.getLocationProvider().getLastKnownLocation();
			if (l != null && l.hasAccuracy()) {
				String acc;
				if (l.hasVerticalAccuracy()) {
					acc = String.format(app.getString(R.string.precision_hdop_and_vdop),
							OsmAndFormatter.getFormattedDistance(l.getAccuracy(), app),
							OsmAndFormatter.getFormattedDistance(l.getVerticalAccuracy(), app));
				} else {
					acc = String.format(app.getString(R.string.precision_hdop),
							OsmAndFormatter.getFormattedDistance(l.getAccuracy(), app));
				}

				addPlainMenuItem(R.drawable.ic_action_ruler_circle, null, acc, false, false, null);
			}
		}
	}

	public PointDescription getPointDescription() {
		return pointDescription;
	}

	public int getInitialMenuState() {
		if (isLandscapeLayout()) {
			return MenuState.FULL_SCREEN;
		} else {
			return getInitialMenuStatePortrait();
		}
	}

	public int getSupportedMenuStates() {
		if (isLandscapeLayout()) {
			return MenuState.FULL_SCREEN;
		} else {
			return getSupportedMenuStatesPortrait();
		}
	}

	protected int getInitialMenuStatePortrait() {
		return MenuState.HEADER_ONLY;
	}

	protected int getSupportedMenuStatesPortrait() {
		return MenuState.HEADER_ONLY | MenuState.HALF_SCREEN | MenuState.FULL_SCREEN;
	}

	public int getCurrentMenuState() {
		return currentMenuState;
	}

	public MenuType getMenuType() {
		return menuType;
	}

	public boolean slideUp() {
		int v = currentMenuState;
		for (int i = 0; i < 2; i++) {
			v = v << 1;
			if ((v & getSupportedMenuStates()) != 0) {
				currentMenuState = v;
				return true;
			}
		}
		return false;
	}

	public boolean slideDown() {
		int v = currentMenuState;
		for (int i = 0; i < 2; i++) {
			v = v >> 1;
			if ((v & getSupportedMenuStates()) != 0) {
				currentMenuState = v;
				return true;
			}
		}
		return false;
	}

	public void setCurrentMenuState(int currentMenuState) {
		this.currentMenuState = currentMenuState;
	}

	public TitleButtonController getLeftTitleButtonController() {
		return leftTitleButtonController;
	}

	public TitleButtonController getRightTitleButtonController() {
		return rightTitleButtonController;
	}

	public TitleButtonController getBottomTitleButtonController() {
		return bottomTitleButtonController;
	}

	public TitleButtonController getLeftDownloadButtonController() {
		return leftDownloadButtonController;
	}

	public TitleButtonController getRightDownloadButtonController() {
		return rightDownloadButtonController;
	}

	public List<Pair<TitleButtonController, TitleButtonController>> getAdditionalButtonsControllers() {
		return additionalButtonsControllers;
	}

	public TitleProgressController getTitleProgressController() {
		return titleProgressController;
	}

	public TopToolbarController getToolbarController() {
		return toolbarController;
	}

	public boolean hasBackAction() {
		return toolbarController != null;
	}

	public LatLon getLatLon() {
		return latLon;
	}

	public boolean supportZoomIn() {
		return false;
	}

	public boolean navigateButtonVisible() {
		return true;
	}

	public boolean zoomButtonsVisible() {
		return !isLandscapeLayout();
	}

	public boolean buttonsVisible() {
		return true;
	}

	public boolean handleSingleTapOnMap() {
		return false;
	}

	public boolean isClosable() {
		return true;
	}

	public boolean needStreetName() {
		return !displayDistanceDirection();
	}

	public boolean needTypeStr() {
		return true;
	}

	public boolean displayStreetNameInTitle() {
		return false;
	}

	public boolean displayDistanceDirection() {
		return false;
	}

	public void getFormattedAltitude(@NonNull OnResultCallback<String> callback) {
		MapActivity activity = getMapActivity();
		SRTMPlugin srtmPlugin = PluginsHelper.getActivePlugin(SRTMPlugin.class);
		if (activity != null && srtmPlugin != null && srtmPlugin.is3DReliefAllowed()) {
			OsmandApplication app = activity.getApp();
			OsmandMapTileView mapView = activity.getMapView();
			MapRendererView mapRenderer = mapView.getMapRenderer();
			NativeUtilities.getAltitudeForLatLon(mapRenderer, getLatLon(), altitude -> {
				if (altitude != null) {
					callback.onResult(OsmAndFormatter.getFormattedAlt(altitude, app));
				} else {
					callback.onResult(null);
				}
			});
		} else {
			callback.onResult(null);
		}
	}

	public int getRightIconId() {
		return 0;
	}

	@Nullable
	public Drawable getRightIcon() {
		return null;
	}

	public boolean isBigRightIcon() {
		return false;
	}

	@Nullable
	public Drawable getSecondLineTypeIcon() {
		return null;
	}

	@Nullable
	public Drawable getSubtypeIcon() {
		return null;
	}

	public boolean navigateInPedestrianMode() {
		return false;
	}

	public int getFavActionIconId() {
		return R.drawable.ic_action_favorite_stroke;
	}

	public int getFavActionStringId() {
		return R.string.shared_string_add;
	}

	public boolean isFavButtonEnabled() {
		return true;
	}

	public int getWaypointActionIconId() {
		return R.drawable.ic_action_flag_stroke;
	}

	public int getWaypointActionStringId() {
		return R.string.shared_string_marker;
	}

	public boolean isWaypointButtonEnabled() {
		return true;
	}

	@NonNull
	public String getTypeStr() {
		return "";
	}

	@NonNull
	public CharSequence getSubtypeStr() {
		return "";
	}

	@ColorRes
	public int getAdditionalInfoColorId() {
		if (openingHoursInfo != null) {
			boolean open = false;
			for (OpeningHours.Info info : openingHoursInfo) {
				if (info.isOpened() || info.isOpened24_7()) {
					open = true;
					break;
				}
			}
			return open ? R.color.text_color_positive : R.color.text_color_negative;
		} else if (shouldShowMapSize()) {
			return R.color.icon_color_default_light;
		}
		return 0;
	}

	public CharSequence getAdditionalInfoStr() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			if (openingHoursInfo != null) {
				int colorOpen = mapActivity.getColor(R.color.text_color_positive);
				int colorClosed = mapActivity.getColor(R.color.text_color_negative);
				return getSpannableOpeningHours(openingHoursInfo, colorOpen, colorClosed);
			} else if (shouldShowMapSize()) {
				return mapActivity.getString(R.string.file_size_in_mb, indexItem.getArchiveSizeMB());
			}
		}
		return "";
	}

	@DrawableRes
	public int getAdditionalInfoIconRes() {
		if (openingHoursInfo != null) {
			return R.drawable.ic_action_opening_hour_16;
		} else if (shouldShowMapSize()) {
			return R.drawable.ic_sdcard_16;
		}
		return 0;
	}

	public static SpannableString getSpannableOpeningHours(List<OpeningHours.Info> openingHoursInfo,
	                                                       int colorOpen,
	                                                       int colorClosed) {
		StringBuilder sb = new StringBuilder();
		int[] pos = new int[openingHoursInfo.size()];
		for (int i = 0; i < openingHoursInfo.size(); i++) {
			OpeningHours.Info info = openingHoursInfo.get(i);
			if (sb.length() > 0) {
				sb.append("\n");
			}
			sb.append(info.getInfo());
			pos[i] = sb.length();
		}
		SpannableString infoStr = new SpannableString(sb.toString());
		int k = 0;
		for (int i = 0; i < openingHoursInfo.size(); i++) {
			OpeningHours.Info info = openingHoursInfo.get(i);
			boolean opened = info.isFallback() ? openingHoursInfo.get(i - 1).isOpened() : info.isOpened();
			int color = opened ? colorOpen : colorClosed;
			infoStr.setSpan(new ForegroundColorSpan(color), k, pos[i], 0);
			k = pos[i];
		}
		return infoStr;
	}

	private boolean shouldShowMapSize() {
		return indexItem != null && !downloaded;
	}

	@NonNull
	public String getCommonTypeStr() {
		return "";
	}

	@NonNull
	public String getNameStr() {
		return pointDescription.getName();
	}

	@NonNull
	public String getFirstNameStr() {
		return "";
	}

	@Nullable
	public List<TransportStopRoute> getTransportStopRoutes() {
		return null;
	}

	@Nullable
	protected List<TransportStopRoute> getSubTransportStopRoutes(boolean nearby) {
		return null;
	}

	@Nullable
	public List<TransportStopRoute> getLocalTransportStopRoutes() {
		return getSubTransportStopRoutes(false);
	}

	@Nullable
	public List<TransportStopRoute> getNearbyTransportStopRoutes() {
		return getSubTransportStopRoutes(true);
	}

	public void share(LatLon latLon, String title, String address) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			ShareMenu.show(latLon, title, address, mapActivity);
		}
	}

	public void updateData() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null && downloadMapDataObject != null) {
			if (indexItem == null) {
				List<IndexItem> indexItems = new LinkedList<>(downloadThread.getIndexes().getIndexItems(downloadRegion));
				for (IndexItem item : indexItems) {
					if (item.getType() == DownloadActivityType.NORMAL_FILE) {
						indexItem = item;
						break;
					}
				}
			}

			if (indexItem != null) {
				downloaded = indexItem.isDownloaded();
			}

			leftDownloadButtonController.visible = !downloaded;
			leftDownloadButtonController.startIconId = R.drawable.ic_action_import;

			boolean internetConnectionAvailable =
					mapActivity.getSettings().isInternetConnectionAvailable();

			boolean isDownloading = indexItem != null && downloadThread.isDownloading(indexItem);
			if (isDownloading) {
				titleProgressController.setMapDownloadMode();
				if (downloadThread.getCurrentDownloadingItem() == indexItem) {
					titleProgressController.indeterminate = false;
					titleProgressController.progress = downloadThread.getCurrentDownloadProgress();
				} else {
					titleProgressController.indeterminate = true;
					titleProgressController.progress = 0;
				}
				double mb = indexItem.getArchiveSizeMB();
				String v;
				if (titleProgressController.progress != -1) {
					v = mapActivity.getString(R.string.value_downloaded_of_max, mb * titleProgressController.progress / 100, mb);
				} else {
					v = mapActivity.getString(R.string.file_size_in_mb, mb);
				}
				if (indexItem.getType() == DownloadActivityType.ROADS_FILE) {
					titleProgressController.caption = indexItem.getType().getString(mapActivity) + " • " + v;
				} else {
					titleProgressController.caption = v;
				}
				titleProgressController.visible = true;
			} else if (downloadThread.shouldDownloadIndexes()) {
				titleProgressController.setIndexesDownloadMode(mapActivity);
				titleProgressController.visible = true;
			} else if (!internetConnectionAvailable) {
				titleProgressController.setNoInternetConnectionMode(mapActivity);
				titleProgressController.visible = true;
			} else {
				titleProgressController.visible = false;
			}
		}
	}

	public boolean hasCustomAddressLine() {
		return builder.hasCustomAddressLine();
	}

	public void buildCustomAddressLine(LinearLayout ll) {
		builder.buildCustomAddressLine(ll);
	}

	public void onShow() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null && toolbarController != null) {
			mapActivity.showTopToolbar(toolbarController);
		}
	}

	public void onHide() {
		if (builder != null) {
			builder.onHide();
		}
	}

	public void onClose() {
		if (builder != null) {
			builder.onClose();
		}
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null && toolbarController != null) {
			mapActivity.hideTopToolbar(toolbarController);
		}
	}

	public void onAcquireNewController(PointDescription pointDescription, Object object) {
	}

	public boolean isMapDownloaded() {
		return downloaded;
	}

	public void setLatLon(@NonNull LatLon latLon) {
		this.latLon = latLon;
		if (builder != null) {
			builder.setLatLon(latLon);
		}
	}

	public void requestMapDownloadInfo(LatLon latLon) {
		MapActivity mapActivity = getMapActivity();
		OsmandMapTileView mapView = mapActivity != null ? mapActivity.getMapView() : null;
		if (mapView != null) {
			int zoom = mapView.getCurrentRotatedTileBox().getZoom();
			OsmAndTaskManager.executeTask(new SearchOsmandRegionTask(this, latLon, zoom));
		}
	}

	protected void startDownload(MapActivity mapActivity, IndexItem indexItem) {
		OsmandApplication app = mapActivity.getApp();
		if (!Version.isPaidVersion(app)
				&& DownloadActivityType.isCountedInDownloads(indexItem)
				&& app.getSettings().NUMBER_OF_FREE_DOWNLOADS.get() >= MAXIMUM_AVAILABLE_FREE_DOWNLOADS) {
			ChoosePlanFragment.showInstance(mapActivity, OsmAndFeature.UNLIMITED_MAP_DOWNLOADS);
		} else {
			new DownloadValidationManager(mapActivity.getApp())
					.startDownload(mapActivity, indexItem);
		}
	}

	public void createMapDownloadControls(BinaryMapDataObject binaryMapDataObject, String selectedFullName) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			OsmandRegions osmandRegions = mapActivity.getApp().getResourceManager().getOsmandRegions();
			downloadMapDataObject = binaryMapDataObject;
			downloaded = downloadMapDataObject == null;
			if (!downloaded) {
				downloadThread = mapActivity.getApp().getDownloadThread();
				downloadRegion = osmandRegions.getRegionData(selectedFullName);
				if (downloadRegion != null && downloadRegion.isRegionMapDownload()) {
					List<IndexItem> indexItems = downloadThread.getIndexes().getIndexItems(downloadRegion);
					for (IndexItem item : indexItems) {
						if (item.getType() == DownloadActivityType.NORMAL_FILE
								&& (item.isDownloaded() || downloadThread.isDownloading(item))) {
							indexItem = item;
						}
					}
				}

				leftDownloadButtonController = new TitleButtonController(this) {
					@Override
					public void buttonPressed() {
						MapActivity mapActivity = getMapActivity();
						if (indexItem != null && mapActivity != null) {
							if (indexItem.getType() == DownloadActivityType.NORMAL_FILE) {
								startDownload(mapActivity, indexItem);
							}
						}
					}
				};
				leftDownloadButtonController.caption =
						downloadRegion != null ? downloadRegion.getLocaleName() : mapActivity.getString(R.string.shared_string_download);
				leftDownloadButtonController.startIconId = R.drawable.ic_action_import;

				titleProgressController = new TitleProgressController() {
					@Override
					public void buttonPressed() {
						if (indexItem != null) {
							downloadThread.cancelDownload(indexItem);
						}
					}
				};

				if (!downloadThread.getIndexes().isDownloadedFromInternet) {
					if (mapActivity.getSettings().isInternetConnectionAvailable()) {
						downloadThread.runReloadIndexFiles();
					}
				}

				if (mapContextMenu != null) {
					mapContextMenu.updateMenuUI();
				}
			}
		}
	}
}