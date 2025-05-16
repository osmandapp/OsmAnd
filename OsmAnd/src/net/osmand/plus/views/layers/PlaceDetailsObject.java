package net.osmand.plus.views.layers;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.data.Amenity;
import net.osmand.data.BaseDetailsObject;
import net.osmand.plus.views.layers.ContextMenuLayer.IContextMenuProvider;
import net.osmand.plus.views.layers.MapSelectionResult.SelectedMapObject;

import java.util.*;

import gnu.trove.list.array.TIntArrayList;

public class PlaceDetailsObject extends BaseDetailsObject {

	private final List<SelectedMapObject> selectedObjects = new ArrayList<>();

	public PlaceDetailsObject() {
		super("en");
	}

	public PlaceDetailsObject(BaseDetailsObject baseDetailsObject, @Nullable IContextMenuProvider provider) {
		super(baseDetailsObject.getLang());
		for (Object obj : baseDetailsObject.getObjects()) {
			addObject(obj, provider);
		}
		combineData();
	}

	public PlaceDetailsObject(@NonNull Object object, @Nullable IContextMenuProvider provider, @Nullable String lang) {
		super(object, lang);
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

	@NonNull
	public List<Amenity> getAmenities() {
		List<Amenity> amenities = new ArrayList<>();
		for (Object object : objects) {
			if (object instanceof Amenity amenity) {
				amenities.add(amenity);
			} else if (object instanceof PlaceDetailsObject detailsObject) {
				amenities.addAll(detailsObject.getAmenities());
			}
		}
		return amenities;
	}

	public void setMapIconName(String mapIconName) {
		this.syntheticAmenity.setMapIconName(mapIconName);
	}

	public void setX(TIntArrayList x) {
		this.syntheticAmenity.getX().addAll(x);
	}

	public void setY(TIntArrayList y) {
		this.syntheticAmenity.getY().addAll(y);
	}

	public void addX(int x) {
		this.syntheticAmenity.getX().add(x);
	}

	public void addY(int y) {
		this.syntheticAmenity.getY().add(y);
	}

	public boolean hasGeometry() {
		return !this.syntheticAmenity.getX().isEmpty() && !this.syntheticAmenity.getY().isEmpty();
	}
}