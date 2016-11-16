package net.osmand.core.samples.android.sample1.mapcontextmenu;

import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;

import net.osmand.core.samples.android.sample1.MainActivity;
import net.osmand.core.samples.android.sample1.data.PointDescription;
import net.osmand.data.LatLon;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

public class MapMultiSelectionMenu extends BaseMenuController {

	private LatLon latLon;
	private LinkedList<MenuObject> objects = new LinkedList<>();

	public static class MenuObject extends MenuTitleController {

		private LatLon latLon;
		private PointDescription pointDescription;
		private Object object;
		private int order;

		private MainActivity mainActivity;
		private MenuController controller;

		public MenuObject(LatLon latLon, PointDescription pointDescription, Object object, MainActivity mainActivity) {
			this.latLon = latLon;
			this.pointDescription = pointDescription;
			this.object = object;
			this.mainActivity = mainActivity;
			init();
		}

		protected void init() {
			controller = MenuController.getMenuController(mainActivity, latLon, pointDescription, object, MenuController.MenuType.MULTI_LINE);
			controller.setActive(true);
			initTitle();
		}

		@Override
		public LatLon getLatLon() {
			return latLon;
		}

		@Override
		public PointDescription getPointDescription() {
			return pointDescription;
		}

		@Override
		public Object getObject() {
			return object;
		}

		@Override
		public MainActivity getMainActivity() {
			return mainActivity;
		}

		@Override
		public MenuController getMenuController() {
			return controller;
		}

		@Override
		protected boolean needStreetName() {
			return false;
		}
	}

	public MapMultiSelectionMenu(MainActivity mainActivity) {
		super(mainActivity);
	}

	public void setMainActivity(MainActivity mainActivity) {
		super.setMainActivity(mainActivity);
		for (MenuObject o : objects) {
			o.mainActivity = mainActivity;
			o.init();
		}
	}

	public List<MenuObject> getObjects() {
		return objects;
	}

	@Override
	public float getHalfScreenMaxHeightKoef() {
		return 0.5f;
	}

	private void createCollection(List<Object> selectedObjects) {
		objects.clear();
		for (Object selectedObj : selectedObjects) {
			LatLon ll = MenuController.getObjectLocation(selectedObj);
			PointDescription pointDescription = MenuController.getObjectName(selectedObj);

			if (ll == null) {
				ll = latLon;
			}
			if (pointDescription == null) {
				pointDescription = new PointDescription(latLon.getLatitude(), latLon.getLongitude());
			}

			MenuObject menuObject = new MenuObject(ll, pointDescription, selectedObj, getMainActivity());
			menuObject.order = MenuController.getObjectPriority(selectedObj);
			objects.add(menuObject);
		}

		Collections.sort(objects, new Comparator<MenuObject>() {
			@Override
			public int compare(MenuObject obj1, MenuObject obj2) {
				if (obj1.order == obj2.order) {
					return obj1.getTitleStr().compareToIgnoreCase(obj2.getTitleStr());
				} else {
					return obj1.order - obj2.order;
				}
			}
		});
	}

	private void clearMenu() {
		objects.clear();
		getMainActivity().refreshMap();
	}

	public void show(LatLon latLon, List<Object> selectedObjects) {
		if (isVisible()) {
			hide();
		}

		this.latLon = latLon;
		createCollection(selectedObjects);
		updateNightMode();
		MapMultiSelectionMenuFragment.showInstance(getMainActivity());
		getMainActivity().refreshMap();
	}

	public boolean isVisible() {
		Fragment fragment = getMainActivity().getSupportFragmentManager().findFragmentByTag(MapMultiSelectionMenuFragment.TAG);
		return fragment != null;
	}

	public void hide() {
		clearMenu();
		Fragment fragment = getMainActivity().getSupportFragmentManager().findFragmentByTag(MapMultiSelectionMenuFragment.TAG);
		if (fragment != null) {
			MapMultiSelectionMenuFragment menuFragment = (MapMultiSelectionMenuFragment) fragment;
			menuFragment.dismissMenu();
		}
	}

	public void onStop() {
	}

	public void openContextMenu(@NonNull MenuObject menuObject) {
		hide();
		getMainActivity().showContextMenu(
				menuObject.getLatLon(), menuObject.getPointDescription(), menuObject.getObject());
	}
}
