package net.osmand.plus.mapcontextmenu.other;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import net.osmand.OnCompleteCallback;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.BaseMenuController;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.views.layers.ContextMenuLayer;
import net.osmand.plus.views.layers.ContextMenuLayer.IContextMenuProvider;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class MapMultiSelectionMenu extends BaseMenuController {

	private LatLon latLon;
	private final LinkedList<MenuObject> objects = new LinkedList<>();
	private final Map<Object, IContextMenuProvider> selectedObjects = new HashMap<>();
	private final OnCompleteCallback onSearchAddressDone = this::updateDialogContent;

	public MapMultiSelectionMenu(@NonNull MapActivity mapActivity) {
		super(mapActivity);
	}

	public void setMapActivity(@Nullable MapActivity mapActivity) {
		super.setMapActivity(mapActivity);
		for (MenuObject o : objects) {
			o.setMapActivity(mapActivity);
			if (mapActivity != null) {
				o.init();
			} else {
				o.deinit();
			}
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
			IContextMenuProvider contextObject = e.getValue();

			MenuObject menuObject = MenuObjectUtils.createMenuObject(selectedObj, contextObject, latLon, getMapActivity());
			if (menuObject.needStreetName()) {
				menuObject.setOnSearchAddressDoneCallback(onSearchAddressDone);
			}
			objects.add(menuObject);

			if (contextObject instanceof ContextMenuLayer.IContextMenuProviderSelection) {
				menuObject.setOrder(((ContextMenuLayer.IContextMenuProviderSelection) contextObject).getOrder(selectedObj));
			}
		}
		Collections.sort(objects, new MultiSelectionMenuComparator(getAppMode()));
	}

	private ApplicationMode getAppMode() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			return mapActivity.getMyApplication().getSettings().getApplicationMode();
		}
		return null;
	}

	private void clearMenu() {
		clearSelectedObjects();
		objects.clear();
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			mapActivity.refreshMap();
		}
	}

	public void show(LatLon latLon, Map<Object, IContextMenuProvider> selectedObjects) {
		if (isVisible()) {
			hide();
		}
		this.latLon = latLon;
		createCollection(selectedObjects);
		updateNightMode();
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			MapMultiSelectionMenuFragment.showInstance(mapActivity);
			mapActivity.refreshMap();
		}
	}

	public boolean isVisible() {
		Fragment fragment = getFragmentByTag();
		return fragment != null;
	}

	@Nullable
	public Fragment getFragmentByTag() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			return mapActivity.getSupportFragmentManager().findFragmentByTag(MapMultiSelectionMenuFragment.TAG);
		} else {
			return null;
		}
	}

	public void hide() {
		clearMenu();
		Fragment fragment = getFragmentByTag();
		if (fragment != null) {
			MapMultiSelectionMenuFragment menuFragment = (MapMultiSelectionMenuFragment) fragment;
			menuFragment.dismiss();
		}
	}

	public void onStop() {
		clearSelectedObjects();
	}

	public void openContextMenu(@NonNull MenuObject menuObject) {
		IContextMenuProvider provider = selectedObjects.remove(menuObject.getObject());
		hide();
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			ContextMenuLayer contextMenuLayer = mapActivity.getMapLayers().getContextMenuLayer();
			contextMenuLayer.showContextMenu(menuObject.getLatLon(), menuObject.getPointDescription(), menuObject.getObject(), provider);
		}
	}

	private void clearSelectedObjects() {
		for(IContextMenuProvider p : selectedObjects.values()) {
			if(p instanceof ContextMenuLayer.IContextMenuProviderSelection){
				((ContextMenuLayer.IContextMenuProviderSelection) p).clearSelectedObject();
			}
		}
		selectedObjects.clear();
	}

	private void updateDialogContent() {
		Fragment fragmentByTag = getFragmentByTag();
		if (fragmentByTag instanceof MapMultiSelectionMenuFragment fragment) {
			fragment.updateContent();
		}
	}
}
