package net.osmand.plus.mapcontextmenu;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;

import net.osmand.data.Amenity;
import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.IconsCache;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.SearchHistoryHelper;
import net.osmand.plus.mapcontextmenu.details.AmenityMenuController;
import net.osmand.plus.mapcontextmenu.details.FavouritePointMenuController;
import net.osmand.plus.mapcontextmenu.details.HistoryMenuController;
import net.osmand.plus.mapcontextmenu.details.ParkingPositionController;
import net.osmand.plus.mapcontextmenu.details.PointDescriptionMenuController;

public abstract class MenuController extends BaseMenuController {

	public class MenuState {
		public static final int HEADER_ONLY = 1;
		public static final int HALF_SCREEN = 2;
		public static final int FULL_SCREEN = 4;
	}

	private MenuBuilder builder;
	private int currentMenuState;

	public MenuController(MenuBuilder builder, MapActivity mapActivity) {
		super(mapActivity);
		this.builder = builder;
		this.currentMenuState = getInitialMenuState();
	}

	public void build(View rootView) {
		builder.build(rootView);
	}

	public static MenuController getMenuController(MapActivity mapActivity,
												   LatLon latLon, PointDescription pointDescription, Object object) {
		OsmandApplication app = mapActivity.getMyApplication();
		MenuController menuController = null;
		if (object != null) {
			if (object instanceof Amenity) {
				menuController = new AmenityMenuController(app, mapActivity, (Amenity) object);
			} else if (object instanceof FavouritePoint) {
				menuController = new FavouritePointMenuController(app, mapActivity, (FavouritePoint) object);
			} else if (object instanceof SearchHistoryHelper.HistoryEntry) {
				menuController = new HistoryMenuController(app, mapActivity, (SearchHistoryHelper.HistoryEntry) object);
			} else if (object instanceof LatLon) {
				if (pointDescription.isParking()) {
					menuController = new ParkingPositionController(app, mapActivity, pointDescription, (LatLon) object);
				}
			}
		} else {
			menuController = new PointDescriptionMenuController(app, mapActivity, pointDescription, latLon);
		}
		return menuController;
	}

	public void addPlainMenuItem(int iconId, String text) {
		builder.addPlainMenuItem(iconId, text);
	}

	public void addPlainMenuItems(String typeStr, PointDescription pointDescription) {
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

	public boolean hasTitleButton() {
		return false;
	}

	public String getTitleButtonCaption() {
		return "";
	}

	public void titleButtonPressed() {
	}

	public boolean shouldShowButtons() {
		return true;
	}

	public boolean handleSingleTapOnMap() {
		return false;
	}

	public boolean needStreetName() {
		return true;
	}

	public boolean needTypeStr() {
		return false;
	}

	public int getLeftIconId() { return 0; }

	public Drawable getLeftIcon() { return null; }

	public Drawable getSecondLineIcon() { return null; }

	public int getFavActionIconId() { return R.drawable.ic_action_fav_dark; }

	public String getTypeStr() { return ""; }

	public String getNameStr() { return ""; }

	public abstract void saveEntityState(Bundle bundle, String key);
}