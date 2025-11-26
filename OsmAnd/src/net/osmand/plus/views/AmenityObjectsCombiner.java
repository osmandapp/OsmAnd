package net.osmand.plus.views;

import androidx.annotation.NonNull;

import net.osmand.data.Amenity;
import net.osmand.data.BaseDetailsObject;
import net.osmand.plus.views.layers.MapObjectsCombiner;

import java.util.ArrayList;
import java.util.List;

public class AmenityObjectsCombiner extends MapObjectsCombiner<Amenity> {

	public AmenityObjectsCombiner(@NonNull String lang) {
		super(lang);
	}

	@NonNull
	@Override
	public List<Amenity> combine(@NonNull List<Amenity> inputObjects) {
		List<Amenity> processed = new ArrayList<>();

		if (inputObjects.size() == 1) {
			processed.addAll(inputObjects);
			return processed;
		}

		List<Amenity> mapObjects = new ArrayList<>(inputObjects);
		List<Amenity> other = new ArrayList<>();

		List<BaseDetailsObject> detailsObjects = processObjects(mapObjects, new ProcessObjectsListener<>() {
			@Override
			public Object getObjectToCombine(@NonNull Amenity item) {
				return item;
			}

			@Override
			public void onObjectNotCombined(@NonNull Amenity item) {
				other.add(item);
			}
		});

		for (BaseDetailsObject object : detailsObjects) {
			if (object.getAmenities().size() > 1) {
				processed.add(object.getSyntheticAmenity());
			} else {
				Amenity amenity = object.getAmenities().get(0);
				processed.add(amenity);
			}
		}

		processed.addAll(other);
		return processed;
	}
}
