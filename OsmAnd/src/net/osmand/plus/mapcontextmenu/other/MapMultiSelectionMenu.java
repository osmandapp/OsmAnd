package net.osmand.plus.mapcontextmenu.other;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import net.osmand.NativeLibrary.RenderedObject;
import net.osmand.OnCompleteCallback;
import net.osmand.data.LatLon;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.BaseMenuController;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.views.layers.ContextMenuLayer;
import net.osmand.plus.views.layers.ContextMenuLayer.IContextMenuProvider;
import net.osmand.plus.views.layers.ContextMenuLayer.IContextMenuProviderSelection;
import net.osmand.plus.views.layers.SelectedMapObject;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class MapMultiSelectionMenu extends BaseMenuController {

	private LatLon latLon;
	private final LinkedList<MenuObject> objects = new LinkedList<>();
	private final List<SelectedMapObject> selectedObjects = new ArrayList<>();
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

	private void createCollection(List<SelectedMapObject> selectedObjects) {
		this.selectedObjects.clear();
		this.selectedObjects.addAll(selectedObjects);
		objects.clear();
		for (SelectedMapObject selectedMapObject : selectedObjects) {
			Object object = selectedMapObject.object();
			IContextMenuProvider provider = selectedMapObject.provider();

			MenuObject menuObject = MenuObjectUtils.createMenuObject(object, provider, latLon, getMapActivity());
			if (menuObject.hasEmptyNameStr() && object instanceof RenderedObject) {
				// Do not display nameless RenderedObject(s). Explanation:
				// Actual menuObject.nameStr is calculated from name-tags and iconRes.
				// Default Map Style renders some objects using different icon names, for example:
				// "barrier=bollard" at z17 has icon "barrier_small_red_2" which is not translated (nameStr="")
				// "barrier=bollard" at z18 has icon "barrier_bollard" and translated using "poi_bollard" ("Bollard")
				continue;
			}
			if (menuObject.needStreetName()) {
				menuObject.setOnSearchAddressDoneCallback(onSearchAddressDone);
			}
			objects.add(menuObject);

			if (provider instanceof IContextMenuProviderSelection providerSelection) {
				menuObject.setOrder(providerSelection.getOrder(object));
			}
		}
		objects.sort(new MultiSelectionMenuComparator(getAppMode()));
	}

	private ApplicationMode getAppMode() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			return mapActivity.getSettings().getApplicationMode();
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

	public void show(LatLon latLon, List<SelectedMapObject> selectedObjects) {
		if (isVisible()) {
			hide();
		}
		this.latLon = latLon;
		createCollection(selectedObjects);
		updateNightMode();
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null && !objects.isEmpty()) {
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
		IContextMenuProvider provider = null;
		Iterator<SelectedMapObject> iterator = selectedObjects.listIterator();
		while (iterator.hasNext()) {
			SelectedMapObject selectedMapObject = iterator.next();
			if (Algorithms.objectEquals(selectedMapObject.object(), menuObject.getObject())) {
				iterator.remove();
				provider = selectedMapObject.provider();
			}
		}

		hide();
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			ContextMenuLayer contextMenuLayer = mapActivity.getMapLayers().getContextMenuLayer();
			contextMenuLayer.showContextMenu(menuObject.getLatLon(), menuObject.getPointDescription(), menuObject.getObject(), provider);
		}
	}

	private void clearSelectedObjects() {
		for (SelectedMapObject selectedMapObject : selectedObjects) {
			if (selectedMapObject.provider() instanceof IContextMenuProviderSelection provider) {
				provider.clearSelectedObject();
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
