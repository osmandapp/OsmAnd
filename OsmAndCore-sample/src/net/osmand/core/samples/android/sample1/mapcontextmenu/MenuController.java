package net.osmand.core.samples.android.sample1.mapcontextmenu;

import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;

import net.osmand.core.samples.android.sample1.MainActivity;
import net.osmand.core.samples.android.sample1.OsmandResources;
import net.osmand.core.samples.android.sample1.R;
import net.osmand.core.samples.android.sample1.SampleApplication;
import net.osmand.core.samples.android.sample1.SampleUtils;
import net.osmand.core.samples.android.sample1.data.PointDescription;
import net.osmand.core.samples.android.sample1.mapcontextmenu.controllers.AmenityMenuController;
import net.osmand.core.samples.android.sample1.mapcontextmenu.controllers.MyLocationMenuController;
import net.osmand.core.samples.android.sample1.mapcontextmenu.controllers.PointDescriptionMenuController;
import net.osmand.data.Amenity;
import net.osmand.data.LatLon;

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

	//protected TopToolbarController toolbarController;

	public MenuController(MenuBuilder builder, PointDescription pointDescription, MainActivity mainActivity) {
		super(mainActivity);
		this.pointDescription = pointDescription;
		this.builder = builder;
		this.currentMenuState = getInitialMenuState();
		this.builder.setLight(isLight());
	}

	public void build(View rootView) {
		builder.build(rootView);
	}

	public static MenuController getMenuController(MainActivity mainActivity,
												   LatLon latLon, PointDescription pointDescription, Object object,
												   MenuType menuType) {
		SampleApplication app = mainActivity.getMyApplication();
		MenuController menuController = null;
		if (object != null) {
			if (object instanceof Amenity) {
				menuController = new AmenityMenuController(mainActivity, pointDescription, (Amenity) object);
			} else if (object instanceof LatLon) {
				if (pointDescription.isMyLocation()) {
					menuController = new MyLocationMenuController(mainActivity, pointDescription);
				}
			}
		}
		if (menuController == null) {
			menuController = new PointDescriptionMenuController(mainActivity, pointDescription);
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
		addPlainMenuItem(OsmandResources.getDrawableId("ic_action_get_my_location"), PointDescription.getLocationName(getMainActivity().getMyApplication(),
				latLon.getLatitude(), latLon.getLongitude(), true).replaceAll("\n", " "), false, false, null);
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

	/*
	public TopToolbarController getToolbarController() {
		return toolbarController;
	}

	public boolean hasBackAction() {
		return toolbarController != null;
	}
	*/

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

	public String getTypeStr() {
		return "";
	}

	public String getCommonTypeStr() {
		return "";
	}

	public String getNameStr() {
		return pointDescription.getName();
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

	public void onShow() {
		/*
		if (toolbarController != null) {
			getMainActivity().showTopToolbar(toolbarController);
		}
		*/
	}

	public void onHide() {
	}

	public void onClose() {
		/*
		if (toolbarController != null) {
			getMainActivity().hideTopToolbar(toolbarController);
		}
		*/
	}

	public void onAcquireNewController(PointDescription pointDescription, Object object) {
	}

	public void setLatLon(@NonNull LatLon latLon) {
		this.latLon = latLon;
		if (builder != null) {
			builder.setLatLon(latLon);
		}
	}

	/*
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
	*/

	public static int getObjectPriority(Object o) {
		if (o instanceof Amenity) {
			return 100;
		}
		return 1000;
	}

	public static LatLon getObjectLocation(Object o) {
		if (o instanceof Amenity) {
			return ((Amenity) o).getLocation();
		}
		return null;
	}

	public static PointDescription getObjectName(Object o) {
		if (o instanceof Amenity) {
			return new PointDescription(PointDescription.POINT_TYPE_POI, ((Amenity) o).getName(
					SampleApplication.LANGUAGE, SampleApplication.TRANSLITERATE));
		} else if (o instanceof PointDescription) {
			return (PointDescription) o;
		}
		return null;
	}

}