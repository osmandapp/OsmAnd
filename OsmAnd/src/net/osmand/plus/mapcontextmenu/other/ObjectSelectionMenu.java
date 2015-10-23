package net.osmand.plus.mapcontextmenu.other;

import android.os.Bundle;

import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.BaseMenuController;
import net.osmand.plus.mapcontextmenu.MenuController;
import net.osmand.plus.views.ContextMenuLayer.IContextMenuProvider;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class ObjectSelectionMenu extends BaseMenuController {

	private static final String KEY_OBJ_SEL_MENU_LATLON = "key_obj_sel_menu_latlon";
	private static final String KEY_OBJ_SEL_MENU_OBJECTS = "key_obj_sel_menu_objects";

	private LatLon latLon;
	private LinkedList<MenuMapObject> objects = new LinkedList<>();

	public static class MenuMapObject implements Serializable {

		private LatLon latLon;
		private PointDescription pointDescription;
		private Object object;
		private transient MenuController controller;

		public MenuMapObject(LatLon latLon, PointDescription pointDescription, Object object) {
			this.latLon = latLon;
			this.pointDescription = pointDescription;
			this.object = object;
		}

		public LatLon getLatLon() {
			return latLon;
		}

		public PointDescription getPointDescription() {
			return pointDescription;
		}

		public Object getObject() {
			return object;
		}

		public MenuController getController(MapActivity mapActivity) {
			if (controller == null) {
				controller = MenuController.getMenuController(mapActivity, latLon, pointDescription, object);
			}
			return controller;
		}
	}

	private ObjectSelectionMenu(LatLon latLon, MapActivity mapActivity) {
		super(mapActivity);
		this.latLon = latLon;
	}

	public List<MenuMapObject> getObjects() {
		return objects;
	}

	private void createCollection(Map<Object, IContextMenuProvider> selectedObjects) {
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

			objects.add(new MenuMapObject(ll, pointDescription, selectedObj));
		}
	}

	public static void show(LatLon latLon, MapActivity mapActivity, Map<Object, IContextMenuProvider> selectedObjects) {

		ObjectSelectionMenu menu = new ObjectSelectionMenu(latLon, mapActivity);
		menu.createCollection(selectedObjects);
	}

	public void saveMenu(Bundle bundle) {
		bundle.putSerializable(KEY_OBJ_SEL_MENU_LATLON, latLon);
		bundle.putSerializable(KEY_OBJ_SEL_MENU_OBJECTS, objects);
	}

	public static ObjectSelectionMenu restoreMenu(Bundle bundle, MapActivity mapActivity) {

		LatLon latLon = null;
		Object latLonObj = bundle.getSerializable(KEY_OBJ_SEL_MENU_LATLON);
		if (latLonObj != null) {
			latLon = (LatLon) latLonObj;
		}
		Object objects = bundle.getSerializable(KEY_OBJ_SEL_MENU_OBJECTS);

		ObjectSelectionMenu menu = new ObjectSelectionMenu(latLon, mapActivity);
		if (objects != null) {
			menu.objects = (LinkedList<MenuMapObject>) objects;
		}

		return menu;
	}
}
