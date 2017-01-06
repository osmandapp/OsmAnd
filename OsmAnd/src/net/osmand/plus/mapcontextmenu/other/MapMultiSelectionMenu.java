package net.osmand.plus.mapcontextmenu.other;

import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;

import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.BaseMenuController;
import net.osmand.plus.mapcontextmenu.MenuController;
import net.osmand.plus.mapcontextmenu.MenuController.MenuType;
import net.osmand.plus.mapcontextmenu.MenuTitleController;
import net.osmand.plus.views.ContextMenuLayer;
import net.osmand.plus.views.ContextMenuLayer.IContextMenuProvider;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class MapMultiSelectionMenu extends BaseMenuController {

	private LatLon latLon;
	private LinkedList<MenuObject> objects = new LinkedList<>();
	private Map<Object, IContextMenuProvider> selectedObjects = new HashMap<>();

	public static class MenuObject extends MenuTitleController {

		private LatLon latLon;
		private PointDescription pointDescription;
		private Object object;
		private int order;

		private MapActivity mapActivity;
		private MenuController controller;

		public MenuObject(LatLon latLon, PointDescription pointDescription, Object object, MapActivity mapActivity) {
			this.latLon = latLon;
			this.pointDescription = pointDescription;
			this.object = object;
			this.mapActivity = mapActivity;
			init();
		}

		protected void init() {
			controller = MenuController.getMenuController(mapActivity, latLon, pointDescription, object, MenuType.MULTI_LINE);
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
		public MapActivity getMapActivity() {
			return mapActivity;
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

	public MapMultiSelectionMenu(MapActivity mapActivity) {
		super(mapActivity);
	}

	public void setMapActivity(MapActivity mapActivity) {
		super.setMapActivity(mapActivity);
		for (MenuObject o : objects) {
			o.mapActivity = mapActivity;
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

	private void createCollection(Map<Object, IContextMenuProvider> selectedObjects) {
		this.selectedObjects.clear();
		this.selectedObjects.putAll(selectedObjects);
		objects.clear();
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

			if (contextObject instanceof ContextMenuLayer.IContextMenuProviderSelection) {
				menuObject.order = ((ContextMenuLayer.IContextMenuProviderSelection) contextObject).getOrder(selectedObj);
			}
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
		clearSelectedObjects();
		objects.clear();
		getMapActivity().refreshMap();
	}

	public void show(LatLon latLon, Map<Object, IContextMenuProvider> selectedObjects) {
		if (isVisible()) {
			hide();
		}

/*
		for (Map.Entry<Object, IContextMenuProvider> e : selectedObjects.entrySet()) {
			if (e.getValue() instanceof ContextMenuLayer.IContextMenuProviderSelection) {
				((ContextMenuLayer.IContextMenuProviderSelection) e.getValue()).setSelectedObject(e.getKey());
			}
		}
*/

		this.latLon = latLon;
		createCollection(selectedObjects);
		updateNightMode();
		MapMultiSelectionMenuFragment.showInstance(getMapActivity());
		getMapActivity().refreshMap();
	}

	public boolean isVisible() {
		Fragment fragment = getFragmentByTag();
		return fragment != null;
	}

	public Fragment getFragmentByTag() {
		return getMapActivity().getSupportFragmentManager().findFragmentByTag(MapMultiSelectionMenuFragment.TAG);
	}

	public void hide() {
		clearMenu();
		Fragment fragment = getFragmentByTag();
		if (fragment != null) {
			MapMultiSelectionMenuFragment menuFragment = (MapMultiSelectionMenuFragment) fragment;
			menuFragment.dismissMenu();
		}
	}

	public void onStop() {
		clearSelectedObjects();
	}

	public void openContextMenu(@NonNull MenuObject menuObject) {
		IContextMenuProvider provider = selectedObjects.remove(menuObject.getObject());
		hide();
		getMapActivity().getMapLayers().getContextMenuLayer().showContextMenu(
				menuObject.getLatLon(), menuObject.getPointDescription(), menuObject.getObject(), provider);
	}

	private void clearSelectedObjects() {
		for(IContextMenuProvider p : selectedObjects.values()) {
			if(p instanceof ContextMenuLayer.IContextMenuProviderSelection){
				((ContextMenuLayer.IContextMenuProviderSelection) p).clearSelectedObject();
			}
		}
		selectedObjects.clear();
	}
}
