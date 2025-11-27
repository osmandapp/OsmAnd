package net.osmand.plus.views;

import androidx.annotation.NonNull;

import net.osmand.data.Amenity;
import net.osmand.data.BaseDetailsObject;
import net.osmand.plus.views.layers.MapObjectsMerger;

import java.util.ArrayList;
import java.util.List;

public class AmenityObjectsMerger extends MapObjectsMerger<Amenity> {

	public AmenityObjectsMerger(@NonNull String lang) {
		super(lang);
	}

	@NonNull
	@Override
	public List<Amenity> merge(@NonNull List<Amenity> original) {
		if (original.size() == 1) {
			return new ArrayList<>(original);
		}

		List<Amenity> result = new ArrayList<>();
		List<Amenity> unmergedItems = new ArrayList<>();

		List<BaseDetailsObject> groupedDetails = processObjects(original, new MergeStrategy<>() {
			@Override
			public Object unwrap(@NonNull Amenity item) {
				return item;
			}

			@Override
			public void onMergeFailed(@NonNull Amenity item) {
				unmergedItems.add(item);
			}
		});

		for (BaseDetailsObject object : groupedDetails) {
			if (object.getAmenities().size() > 1) {
				result.add(object.getSyntheticAmenity());
			} else {
				Amenity amenity = object.getAmenities().get(0);
				result.add(amenity);
			}
		}

		result.addAll(unmergedItems);
		return result;
	}
}
