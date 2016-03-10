package net.osmand.plus.mapcontextmenu;

import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.LinearLayout;

import net.osmand.binary.RouteDataObject;
import net.osmand.data.Amenity;
import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.TransportStop;
import net.osmand.plus.GPXUtilities.WptPt;
import net.osmand.plus.GpxSelectionHelper.GpxDisplayItem;
import net.osmand.plus.MapMarkersHelper;
import net.osmand.plus.MapMarkersHelper.MapMarker;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.TargetPointsHelper.TargetPoint;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.audionotes.AudioVideoNoteMenuController;
import net.osmand.plus.audionotes.AudioVideoNotesPlugin.Recording;
import net.osmand.plus.helpers.SearchHistoryHelper;
import net.osmand.plus.mapcontextmenu.controllers.AmenityMenuController;
import net.osmand.plus.mapcontextmenu.controllers.FavouritePointMenuController;
import net.osmand.plus.mapcontextmenu.controllers.GpxItemMenuController;
import net.osmand.plus.mapcontextmenu.controllers.HistoryMenuController;
import net.osmand.plus.mapcontextmenu.controllers.ImpassibleRoadsMenuController;
import net.osmand.plus.mapcontextmenu.controllers.MapDataMenuController;
import net.osmand.plus.mapcontextmenu.controllers.MapMarkerMenuController;
import net.osmand.plus.mapcontextmenu.controllers.MyLocationMenuController;
import net.osmand.plus.mapcontextmenu.controllers.PointDescriptionMenuController;
import net.osmand.plus.mapcontextmenu.controllers.TargetPointMenuController;
import net.osmand.plus.mapcontextmenu.controllers.TransportStopController;
import net.osmand.plus.mapcontextmenu.controllers.WptPtMenuController;
import net.osmand.plus.mapcontextmenu.other.ShareMenu;
import net.osmand.plus.osmedit.EditPOIMenuController;
import net.osmand.plus.osmedit.OsmBugMenuController;
import net.osmand.plus.osmedit.OsmBugsLayer.OpenStreetNote;
import net.osmand.plus.osmedit.OsmPoint;
import net.osmand.plus.osmo.OsMoGroupsStorage.OsMoDevice;
import net.osmand.plus.osmo.OsMoMenuController;
import net.osmand.plus.parkingpoint.ParkingPositionMenuController;
import net.osmand.plus.views.DownloadedRegionsLayer.DownloadMapObject;

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

	private MenuBuilder builder;
	private int currentMenuState;
	private MenuType menuType = MenuType.STANDARD;
	private PointDescription pointDescription;

	protected TitleButtonController leftTitleButtonController;
	protected TitleButtonController rightTitleButtonController;
	protected TitleButtonController topRightTitleButtonController;

	protected TitleProgressController titleProgressController;

	public MenuController(MenuBuilder builder, PointDescription pointDescription, MapActivity mapActivity) {
		super(mapActivity);
		this.pointDescription = pointDescription;
		this.builder = builder;
		this.currentMenuState = getInitialMenuState();
		this.builder.setLight(isLight());
	}

	public void build(View rootView) {
		builder.build(rootView);
	}

	public static MenuController getMenuController(MapActivity mapActivity,
												   PointDescription pointDescription, Object object, MenuType menuType) {
		OsmandApplication app = mapActivity.getMyApplication();
		MenuController menuController = null;
		if (object != null) {
			if (object instanceof Amenity) {
				menuController = new AmenityMenuController(app, mapActivity, pointDescription, (Amenity) object);
			} else if (object instanceof FavouritePoint) {
				menuController = new FavouritePointMenuController(app, mapActivity, pointDescription, (FavouritePoint) object);
			} else if (object instanceof SearchHistoryHelper.HistoryEntry) {
				menuController = new HistoryMenuController(app, mapActivity, pointDescription, (SearchHistoryHelper.HistoryEntry) object);
			} else if (object instanceof TargetPoint) {
				menuController = new TargetPointMenuController(app, mapActivity, pointDescription, (TargetPoint) object);
			} else if (object instanceof OsMoDevice) {
				menuController = new OsMoMenuController(app, mapActivity, pointDescription, (OsMoDevice) object);
			} else if (object instanceof Recording) {
				menuController = new AudioVideoNoteMenuController(app, mapActivity, pointDescription, (Recording) object);
			} else if (object instanceof OsmPoint) {
				menuController = new EditPOIMenuController(app, mapActivity, pointDescription, (OsmPoint) object);
			} else if (object instanceof WptPt) {
				menuController = new WptPtMenuController(app, mapActivity, pointDescription, (WptPt) object);
			} else if (object instanceof DownloadMapObject) {
				menuController = new MapDataMenuController(app, mapActivity, pointDescription, (DownloadMapObject) object);
			} else if (object instanceof OpenStreetNote) {
				menuController = new OsmBugMenuController(app, mapActivity, pointDescription, (OpenStreetNote) object);
			} else if (object instanceof GpxDisplayItem) {
				menuController = new GpxItemMenuController(app, mapActivity, pointDescription, (GpxDisplayItem) object);
			} else if (object instanceof MapMarker) {
				menuController = new MapMarkerMenuController(app, mapActivity, pointDescription, (MapMarker) object);
			} else if (object instanceof TransportStop) {
				menuController = new TransportStopController(app, mapActivity, pointDescription, (TransportStop) object);
			} else if (object instanceof LatLon) {
				if (pointDescription.isParking()) {
					menuController = new ParkingPositionMenuController(app, mapActivity, pointDescription);
				} else if (pointDescription.isMyLocation()) {
					menuController = new MyLocationMenuController(app, mapActivity, pointDescription);
				}
			} else if (object instanceof RouteDataObject) {
				menuController = new ImpassibleRoadsMenuController(app, mapActivity,
						pointDescription, (RouteDataObject) object);
			}
		}
		if (menuController == null) {
			menuController = new PointDescriptionMenuController(app, mapActivity, pointDescription);
		}
		menuController.menuType = menuType;
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

	public void addPlainMenuItem(int iconId, String text, boolean needLinks, boolean isUrl) {
		builder.addPlainMenuItem(iconId, text, needLinks, isUrl);
	}

	public void clearPlainMenuItems() {
		builder.clearPlainMenuItems();
	}

	public void addPlainMenuItems(String typeStr, PointDescription pointDescription, LatLon latLon) {
		addMyLocationToPlainItems(latLon);
	}

	protected void addMyLocationToPlainItems(LatLon latLon) {
		addPlainMenuItem(R.drawable.ic_action_get_my_location, PointDescription.getLocationName(getMapActivity(),
				latLon.getLatitude(), latLon.getLongitude(), true).replaceAll("\n", ""), false, false);
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

	public TitleProgressController getTitleProgressController() {
		return titleProgressController;
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

	public int getLeftIconId() { return 0; }

	public Drawable getLeftIcon() { return null; }

	public Drawable getSecondLineTypeIcon() { return null; }

	public int getFavActionIconId() { return R.drawable.map_action_fav_dark; }

	public String getTypeStr() { return ""; }

	public String getCommonTypeStr() { return ""; }

	public String getNameStr() { return pointDescription.getName(); }

	public void share(LatLon latLon, String title) {
		ShareMenu.show(latLon, title, getMapActivity());
	}

	public void updateData() {
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
}