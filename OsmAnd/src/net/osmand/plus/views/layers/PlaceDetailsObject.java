package net.osmand.plus.views.layers;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import net.osmand.data.BaseDetailsObject;
import net.osmand.plus.views.layers.ContextMenuLayer.IContextMenuProvider;
import net.osmand.plus.views.layers.MapSelectionResult.SelectedMapObject;

import java.util.*;

public class PlaceDetailsObject extends BaseDetailsObject {

	private final List<SelectedMapObject> selectedObjects = new ArrayList<>();

	public PlaceDetailsObject() {
		super();
	}

	public PlaceDetailsObject(@NonNull Object object, @Nullable IContextMenuProvider provider) {
		super(object);
		addObject(object, provider);
		combineData();
	}

	public void addObject(@NonNull Object object, @Nullable IContextMenuProvider provider) {
		if (shouldSkip(object)) {
			return;
		}
		super.addObject(object);
		selectedObjects.add(new SelectedMapObject(object, provider));
	}

	@NonNull
	public List<SelectedMapObject> getSelectedObjects() {
		return selectedObjects;
	}


}