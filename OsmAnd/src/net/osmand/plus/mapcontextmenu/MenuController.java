package net.osmand.plus.mapcontextmenu;

import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;

import net.osmand.IndexConstants;
import net.osmand.NativeLibrary.RenderedObject;
import net.osmand.aidl.maplayer.point.AMapPoint;
import net.osmand.binary.BinaryMapDataObject;
import net.osmand.binary.BinaryMapIndexReader.TagValuePair;
import net.osmand.binary.RouteDataObject;
import net.osmand.data.Amenity;
import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.TransportStop;
import net.osmand.map.OsmandRegions;
import net.osmand.map.WorldRegion;
import net.osmand.plus.GPXUtilities.WptPt;
import net.osmand.plus.GpxSelectionHelper.GpxDisplayItem;
import net.osmand.plus.MapMarkersHelper.MapMarker;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.TargetPointsHelper.TargetPoint;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.audionotes.AudioVideoNoteMenuController;
import net.osmand.plus.audionotes.AudioVideoNotesPlugin.Recording;
import net.osmand.plus.download.DownloadActivityType;
import net.osmand.plus.download.DownloadIndexesThread;
import net.osmand.plus.download.DownloadValidationManager;
import net.osmand.plus.download.IndexItem;
import net.osmand.plus.helpers.SearchHistoryHelper;
import net.osmand.plus.mapcontextmenu.controllers.AMapPointMenuController;
import net.osmand.plus.mapcontextmenu.controllers.AmenityMenuController;
import net.osmand.plus.mapcontextmenu.controllers.FavouritePointMenuController;
import net.osmand.plus.mapcontextmenu.controllers.GpxItemMenuController;
import net.osmand.plus.mapcontextmenu.controllers.HistoryMenuController;
import net.osmand.plus.mapcontextmenu.controllers.ImpassibleRoadsMenuController;
import net.osmand.plus.mapcontextmenu.controllers.MapDataMenuController;
import net.osmand.plus.mapcontextmenu.controllers.MapMarkerMenuController;
import net.osmand.plus.mapcontextmenu.controllers.MyLocationMenuController;
import net.osmand.plus.mapcontextmenu.controllers.PointDescriptionMenuController;
import net.osmand.plus.mapcontextmenu.controllers.RenderedObjectMenuController;
import net.osmand.plus.mapcontextmenu.controllers.TargetPointMenuController;
import net.osmand.plus.mapcontextmenu.controllers.TransportRouteController;
import net.osmand.plus.mapcontextmenu.controllers.TransportStopController;
import net.osmand.plus.mapcontextmenu.controllers.TransportStopController.TransportStopRoute;
import net.osmand.plus.mapcontextmenu.controllers.WptPtMenuController;
import net.osmand.plus.mapcontextmenu.other.ShareMenu;
import net.osmand.plus.osmedit.EditPOIMenuController;
import net.osmand.plus.osmedit.OsmBugMenuController;
import net.osmand.plus.osmedit.OsmBugsLayer.OpenStreetNote;
import net.osmand.plus.osmedit.OsmPoint;
import net.osmand.plus.osmo.OsMoGroupsStorage.OsMoDevice;
import net.osmand.plus.osmo.OsMoMenuController;
import net.osmand.plus.parkingpoint.ParkingPositionMenuController;
import net.osmand.plus.resources.ResourceManager;
import net.osmand.plus.views.DownloadedRegionsLayer.DownloadMapObject;
import net.osmand.plus.views.mapwidgets.MapInfoWidgetsFactory.TopToolbarController;
import net.osmand.plus.views.mapwidgets.MapInfoWidgetsFactory.TopToolbarControllerType;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public abstract class MenuController extends BaseMenuController {

	public static class MenuState {
		public static final int HEADER_ONLY = 1;
		public static final int HALF_SCREEN = 2;
		public static final int FULL_SCREEN = 4;
	}

	public enum MenuType {
		STANDARD,
		MULTI_LINE
	}

	protected MenuBuilder builder;
	private int currentMenuState;
	private MenuType menuType = MenuType.STANDARD;
	private PointDescription pointDescription;
	private LatLon latLon;
	private boolean active;

	protected TitleButtonController leftTitleButtonController;
	protected TitleButtonController rightTitleButtonController;
	protected TitleButtonController topRightTitleButtonController;

	protected TitleButtonController leftDownloadButtonController;
	protected TitleButtonController rightDownloadButtonController;
	protected TitleProgressController titleProgressController;

	protected TopToolbarController toolbarController;

	protected IndexItem indexItem;
	protected boolean downloaded;
	private BinaryMapDataObject downloadMapDataObject;
	private WorldRegion downloadRegion;
	private DownloadIndexesThread downloadThread;

	public MenuController(MenuBuilder builder, PointDescription pointDescription, MapActivity mapActivity) {
		super(mapActivity);
		this.pointDescription = pointDescription;
		this.builder = builder;
		this.currentMenuState = getInitialMenuState();
		this.builder.setLight(isLight());
	}

	public void build(View rootView) {
		for (OsmandPlugin plugin : OsmandPlugin.getEnabledPlugins()) {
			if (plugin.isMenuControllerSupported(this.getClass())) {
				builder.addMenuPlugin(plugin);
			}
		}
		builder.build(rootView);
	}

	public static MenuController getMenuController(MapActivity mapActivity,
												   LatLon latLon, PointDescription pointDescription, Object object,
												   MenuType menuType) {
		OsmandApplication app = mapActivity.getMyApplication();
		MenuController menuController = null;
		if (object != null) {
			if (object instanceof Amenity) {
				menuController = new AmenityMenuController(mapActivity, pointDescription, (Amenity) object);
			} else if (object instanceof FavouritePoint) {
				menuController = new FavouritePointMenuController(mapActivity, pointDescription, (FavouritePoint) object);
			} else if (object instanceof SearchHistoryHelper.HistoryEntry) {
				menuController = new HistoryMenuController(mapActivity, pointDescription, (SearchHistoryHelper.HistoryEntry) object);
			} else if (object instanceof TargetPoint) {
				menuController = new TargetPointMenuController(mapActivity, pointDescription, (TargetPoint) object);
			} else if (object instanceof OsMoDevice) {
				menuController = new OsMoMenuController(mapActivity, pointDescription, (OsMoDevice) object);
			} else if (object instanceof Recording) {
				menuController = new AudioVideoNoteMenuController(mapActivity, pointDescription, (Recording) object);
			} else if (object instanceof OsmPoint) {
				menuController = new EditPOIMenuController(mapActivity, pointDescription, (OsmPoint) object);
			} else if (object instanceof WptPt) {
				menuController = new WptPtMenuController(mapActivity, pointDescription, (WptPt) object);
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
			} else if (object instanceof AMapPoint) {
				menuController = new AMapPointMenuController(mapActivity, pointDescription, (AMapPoint) object);
			} else if (object instanceof LatLon) {
				if (pointDescription.isParking()) {
					menuController = new ParkingPositionMenuController(mapActivity, pointDescription);
				} else if (pointDescription.isMyLocation()) {
					menuController = new MyLocationMenuController(mapActivity, pointDescription);
				}
			} else if (object instanceof RouteDataObject) {
				menuController = new ImpassibleRoadsMenuController(mapActivity, pointDescription, (RouteDataObject) object);
			} else if (object instanceof RenderedObject) {
				menuController = new RenderedObjectMenuController(mapActivity, pointDescription, (RenderedObject) object);
			}
		}
		if (menuController == null) {
			menuController = new PointDescriptionMenuController(mapActivity, pointDescription);
		}
		menuController.menuType = menuType;
		menuController.setLatLon(latLon);
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

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public void addPlainMenuItem(int iconId, String text, boolean needLinks, boolean isUrl, OnClickListener onClickListener) {
		builder.addPlainMenuItem(iconId, text, needLinks, isUrl, onClickListener);
	}

	public void clearPlainMenuItems() {
		builder.clearPlainMenuItems();
	}

	public void addPlainMenuItems(String typeStr, PointDescription pointDescription, LatLon latLon) {
		addMyLocationToPlainItems(latLon);
	}

	protected void addMyLocationToPlainItems(LatLon latLon) {
		OsmandSettings st = ((OsmandApplication) getMapActivity().getApplicationContext()).getSettings();
		addPlainMenuItem(R.drawable.ic_action_get_my_location, PointDescription.getLocationName(getMapActivity(),
				latLon.getLatitude(), latLon.getLongitude(), true).replaceAll("\n", " "), false, false, null);
		if (st.COORDINATES_FORMAT.get() != PointDescription.OLC_FORMAT)
			addPlainMenuItem(R.drawable.ic_action_get_my_location, PointDescription.getLocationOlcName(
					latLon.getLatitude(), latLon.getLongitude()).replaceAll("\n", " "), false, false, null);
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

	public TitleButtonController getTopRightTitleButtonController() {
		return topRightTitleButtonController;
	}

	public TitleButtonController getLeftDownloadButtonController() {
		return leftDownloadButtonController;
	}

	public TitleButtonController getRightDownloadButtonController() {
		return rightDownloadButtonController;
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
		return true;
	}

	public boolean fabVisible() {
		return true;
	}

	public boolean buttonsVisible() {
		return true;
	}

	public boolean handleSingleTapOnMap() {
		return false;
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

	public int getLeftIconId() {
		return 0;
	}

	public Drawable getLeftIcon() {
		return null;
	}

	public Drawable getSecondLineTypeIcon() {
		return null;
	}

	public int getFavActionIconId() {
		return R.drawable.map_action_fav_dark;
	}

	public int getFavActionStringId() {
		return R.string.shared_string_add_to_favorites;
	}

	public String getTypeStr() {
		return "";
	}

	public String getCommonTypeStr() {
		return "";
	}

	public String getNameStr() {
		return pointDescription.getName();
	}

	public void share(LatLon latLon, String title, String address) {
		ShareMenu.show(latLon, title, address, getMapActivity());
	}

	public void updateData() {
		if (downloadMapDataObject != null) {
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
			leftDownloadButtonController.leftIconId = R.drawable.ic_action_import;

			boolean downloadIndexes = getMapActivity().getMyApplication().getSettings().isInternetConnectionAvailable()
					&& !downloadThread.getIndexes().isDownloadedFromInternet
					&& !downloadThread.getIndexes().downloadFromInternetFailed;

			boolean isDownloading = indexItem != null && downloadThread.isDownloading(indexItem);
			if (isDownloading) {
				titleProgressController.setMapDownloadMode();
				if (downloadThread.getCurrentDownloadingItem() == indexItem) {
					titleProgressController.indeterminate = false;
					titleProgressController.progress = downloadThread.getCurrentDownloadingItemProgress();
				} else {
					titleProgressController.indeterminate = true;
					titleProgressController.progress = 0;
				}
				double mb = indexItem.getArchiveSizeMB();
				String v;
				if (titleProgressController.progress != -1) {
					v = getMapActivity().getString(R.string.value_downloaded_of_max, mb * titleProgressController.progress / 100, mb);
				} else {
					v = getMapActivity().getString(R.string.file_size_in_mb, mb);
				}
				if (indexItem.getType() == DownloadActivityType.ROADS_FILE) {
					titleProgressController.caption = indexItem.getType().getString(getMapActivity()) + " • " + v;
				} else {
					titleProgressController.caption = v;
				}
				titleProgressController.visible = true;
			} else if (downloadIndexes) {
				titleProgressController.setIndexesDownloadMode();
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

	public abstract class TitleButtonController {
		public String caption = "";
		public int leftIconId = 0;
		public boolean needRightText = false;
		public String rightTextCaption = "";
		public boolean visible = true;

		public Drawable getLeftIcon() {
			if (leftIconId != 0) {
				return getIcon(leftIconId, isLight() ? R.color.map_widget_blue : R.color.osmand_orange);
			} else {
				return null;
			}
		}

		public abstract void buttonPressed();
	}

	public abstract class TitleProgressController {
		public String caption = "";
		public int progress = 0;
		public boolean indeterminate;
		public boolean visible;
		public boolean buttonVisible;

		public void setIndexesDownloadMode() {
			caption = getMapActivity().getString(R.string.downloading_list_indexes);
			indeterminate = true;
			buttonVisible = false;
		}

		public void setMapDownloadMode() {
			indeterminate = false;
			buttonVisible = true;
		}

		public abstract void buttonPressed();
	}

	public void onShow() {
		if (toolbarController != null) {
			getMapActivity().showTopToolbar(toolbarController);
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
		if (toolbarController != null) {
			getMapActivity().hideTopToolbar(toolbarController);
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

	public void buildMapDownloadButton(LatLon latLon) {
		int point31x = MapUtils.get31TileNumberX(latLon.getLongitude());
		int point31y = MapUtils.get31TileNumberY(latLon.getLatitude());

		ResourceManager rm = getMapActivity().getMyApplication().getResourceManager();
		OsmandRegions osmandRegions = rm.getOsmandRegions();

		List<BinaryMapDataObject> mapDataObjects = null;
		try {
			mapDataObjects = osmandRegions.queryBbox(point31x, point31x, point31y, point31y);
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (mapDataObjects != null) {
			Iterator<BinaryMapDataObject> it = mapDataObjects.iterator();
			while (it.hasNext()) {
				BinaryMapDataObject o = it.next();
				if (o.getTypes() != null) {
					boolean isRegion = true;
					for (int i = 0; i < o.getTypes().length; i++) {
						TagValuePair tp = o.getMapIndex().decodeType(o.getTypes()[i]);
						if ("boundary".equals(tp.value)) {
							isRegion = false;
							break;
						}
					}
					if (!isRegion || !osmandRegions.contain(o, point31x, point31y)) {
						it.remove();
					}
				}
			}
			String selectedFullName = "";
			double smallestArea = -1;
			downloadMapDataObject = null;
			for (BinaryMapDataObject o : mapDataObjects) {
				String downloadName = osmandRegions.getDownloadName(o);
				if (!Algorithms.isEmpty(downloadName)) {
					boolean downloaded = checkIfObjectDownloaded(rm, downloadName);
					if (downloaded) {
						downloadMapDataObject = null;
						break;
					} else {
						String fullName = osmandRegions.getFullName(o);
						WorldRegion region = osmandRegions.getRegionData(fullName);
						if (region != null && region.isRegionMapDownload()) {
							double area = OsmandRegions.getArea(o);
							if (smallestArea == -1) {
								smallestArea = area;
								selectedFullName = fullName;
								downloadMapDataObject = o;
							} else if (area < smallestArea) {
								smallestArea = area;
								selectedFullName = fullName;
								downloadMapDataObject = o;
							}
						}
					}
				}
			}

			downloaded = downloadMapDataObject == null;
			if (!downloaded) {
				downloadThread = getMapActivity().getMyApplication().getDownloadThread();
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

				leftDownloadButtonController = new TitleButtonController() {
					@Override
					public void buttonPressed() {
						if (indexItem != null) {
							if (indexItem.getType() == DownloadActivityType.NORMAL_FILE) {
								new DownloadValidationManager(getMapActivity().getMyApplication())
										.startDownload(getMapActivity(), indexItem);
							}
						}
					}
				};
				leftDownloadButtonController.caption =
						downloadRegion != null ? downloadRegion.getLocaleName() : getMapActivity().getString(R.string.shared_string_download);
				leftDownloadButtonController.leftIconId = R.drawable.ic_action_import;

				titleProgressController = new TitleProgressController() {
					@Override
					public void buttonPressed() {
						if (indexItem != null) {
							downloadThread.cancelDownload(indexItem);
						}
					}
				};

				if (!downloadThread.getIndexes().isDownloadedFromInternet) {
					if (getMapActivity().getMyApplication().getSettings().isInternetConnectionAvailable()) {
						downloadThread.runReloadIndexFiles();
					}
				}

				updateData();
			}
		}
	}

	private boolean checkIfObjectDownloaded(ResourceManager rm, String downloadName) {
		final String regionName = Algorithms.capitalizeFirstLetterAndLowercase(downloadName)
				+ IndexConstants.BINARY_MAP_INDEX_EXT;
		final String roadsRegionName = Algorithms.capitalizeFirstLetterAndLowercase(downloadName) + ".road"
				+ IndexConstants.BINARY_MAP_INDEX_EXT;
		boolean downloaded = rm.getIndexFileNames().containsKey(regionName) || rm.getIndexFileNames().containsKey(roadsRegionName);
		if (!downloaded) {
			WorldRegion region = rm.getOsmandRegions().getRegionDataByDownloadName(downloadName);
			if (region != null && region.getSuperregion() != null && region.getSuperregion().isRegionMapDownload()) {
				return checkIfObjectDownloaded(rm, region.getSuperregion().getRegionDownloadName());
			}
		}
		return downloaded;
	}

	public static class ContextMenuToolbarController extends TopToolbarController {

		private MenuController menuController;

		public ContextMenuToolbarController(MenuController menuController) {
			super(TopToolbarControllerType.CONTEXT_MENU);
			this.menuController = menuController;
		}

		public MenuController getMenuController() {
			return menuController;
		}
	}
}