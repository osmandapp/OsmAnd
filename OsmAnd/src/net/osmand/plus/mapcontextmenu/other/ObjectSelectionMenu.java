package net.osmand.plus.mapcontextmenu.other;

import android.os.Bundle;
import android.support.v4.app.Fragment;

import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.BaseMenuController;
import net.osmand.plus.mapcontextmenu.MenuController;
import net.osmand.plus.mapcontextmenu.MenuTitleController;
import net.osmand.plus.views.ContextMenuLayer;
import net.osmand.plus.views.ContextMenuLayer.IContextMenuProvider;

import java.io.Serializable;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class ObjectSelectionMenu extends BaseMenuController {

	private static final String KEY_OBJ_SEL_MENU_LATLON = "key_obj_sel_menu_latlon";
	private static final String KEY_OBJ_SEL_MENU_OBJECTS = "key_obj_sel_menu_objects";

	private LatLon latLon;
	private LinkedList<MenuObject> objects = new LinkedList<>();
	private Map<Object, IContextMenuProvider> selectedObjects = new HashMap<>();

	public static class MenuObject extends MenuTitleController implements Serializable {

		private LatLon latLon;
		private PointDescription pointDescription;
		private Object object;

		private transient MapActivity mapActivity;
		private transient MenuController controller;

		public MenuObject(LatLon latLon, PointDescription pointDescription, Object object, MapActivity mapActivity) {
			this.latLon = latLon;
			this.pointDescription = pointDescription;
			this.object = object;
			this.mapActivity = mapActivity;
			init();
		}

		protected void init() {
			controller = MenuController.getMenuController(mapActivity, pointDescription, object);
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
		public MapActivity getMapActivity() {
			return mapActivity;
		}

		@Override
		public MenuController getMenuController() {
			return controller;
		}

		@Override
		protected void refreshMenuTitle() {
		}

		@Override
		protected boolean needStreetName() {
			return false;
		}
	}

	private ObjectSelectionMenu(LatLon latLon, MapActivity mapActivity) {
		super(mapActivity);
		this.latLon = latLon;
	}

	public List<MenuObject> getObjects() {
		return objects;
	}

	@Override
	public float getHalfScreenMaxHeightKoef() {
		return 0.5f;
	}

	private void createCollection(Map<Object, IContextMenuProvider> selectedObjects) {
		this.selectedObjects.putAll(selectedObjects);
		for (Map.Entry<Object, IContextMenuProvider> e : selectedObjects.entrySet()) {
			Object selectedObj = e.getKey();
			IContextMenuProvider contextObject = selectedObjects.get(selectedObj);
			LatLon ll = null;
			PointDescription pointDescription = null;
			if (contextObject != null) {
				ll = contextObject.getObjectLocation(selectedObj);
				pointDescription = contextObject.getObjectName(selectedObj);
			}
			if (ll == null) {
				ll = latLon;
			}
			if (pointDescription == null) {
				pointDescription = new PointDescription(latLon.getLatitude(), latLon.getLongitude());
			}

			MenuObject menuObject = new MenuObject(ll, pointDescription, selectedObj, getMapActivity());
			objects.add(menuObject);
		}
		Collections.sort(objects, new Comparator<MenuObject>() {
			@Override
			public int compare(MenuObject obj1, MenuObject obj2) {
				return obj1.getTitleStr().compareToIgnoreCase(obj2.getTitleStr());
			}
		});
	}

	public static void show(LatLon latLon, Map<Object, IContextMenuProvider> selectedObjects, MapActivity mapActivity) {

		if (isVisible(mapActivity)) {
			hide(mapActivity);
		}

		ObjectSelectionMenu menu = new ObjectSelectionMenu(latLon, mapActivity);
		menu.createCollection(selectedObjects);
		ObjectSelectionMenuFragment.showInstance(menu);
	}

	public static boolean isVisible(MapActivity mapActivity) {
		Fragment fragment = mapActivity.getSupportFragmentManager().findFragmentByTag(ObjectSelectionMenuFragment.TAG);
		return fragment != null;
	}

	public static void hide(MapActivity mapActivity) {
		Fragment fragment = mapActivity.getSupportFragmentManager().findFragmentByTag(ObjectSelectionMenuFragment.TAG);
		if (fragment != null) {
			ObjectSelectionMenuFragment menuFragment = (ObjectSelectionMenuFragment) fragment;
			menuFragment.dismissMenu();
		}
	}

	public void onDismiss() {
		clearSelectedObjects();
	}

	public void openContextMenu(MenuObject menuObject) {
		if (selectedObjects.containsKey(menuObject.getObject())) {
			selectedObjects.remove(menuObject.getObject());
		}
		hide(getMapActivity());
		getMapActivity().getContextMenu()
				.show(menuObject.getLatLon(), menuObject.getPointDescription(), menuObject.getObject());
	}

	private void clearSelectedObjects() {
		for(IContextMenuProvider p : selectedObjects.values()) {
			if(p instanceof ContextMenuLayer.IContextMenuProviderSelection){
				((ContextMenuLayer.IContextMenuProviderSelection) p).clearSelectedObject();
			}
		}
	}

	public void saveMenu(Bundle bundle) {
		bundle.putSerializable(KEY_OBJ_SEL_MENU_LATLON, latLon);
		bundle.putSerializable(KEY_OBJ_SEL_MENU_OBJECTS, objects);
	}

	@SuppressWarnings("unchecked")
	public static ObjectSelectionMenu restoreMenu(Bundle bundle, MapActivity mapActivity) {

		LatLon latLon = null;
		Object latLonObj = bundle.getSerializable(KEY_OBJ_SEL_MENU_LATLON);
		if (latLonObj != null) {
			latLon = (LatLon) latLonObj;
		}
		Object objects = bundle.getSerializable(KEY_OBJ_SEL_MENU_OBJECTS);

		ObjectSelectionMenu menu = new ObjectSelectionMenu(latLon, mapActivity);
		if (objects != null) {
			menu.objects = (LinkedList<MenuObject>) objects;
			for (MenuObject menuObject : menu.objects) {
				menuObject.mapActivity = mapActivity;
				menuObject.init();
			}
		}

		return menu;
	}
}
