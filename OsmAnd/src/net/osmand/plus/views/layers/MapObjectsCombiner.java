package net.osmand.plus.views.layers;

import androidx.annotation.NonNull;

import net.osmand.data.BaseDetailsObject;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;

public abstract class MapObjectsCombiner<T> {

	private final String lang;

	public MapObjectsCombiner(@NonNull String lang) {
		this.lang = lang;
	}

	@NonNull
	public abstract List<T> combine(@NonNull List<T> inputObjects);

	@NonNull
	protected List<BaseDetailsObject> processObjects(@NonNull List<T> input,
	                                                 @NonNull ProcessObjectsListener<T> listener) {

		List<BaseDetailsObject> mergedObjects = new ArrayList<>();

		for (T o : input) {
			Object object = listener.getObjectToCombine(o);
			List<BaseDetailsObject> overlapped = collectOverlappedObjects(object, mergedObjects);

			BaseDetailsObject detailsObject;
			if (Algorithms.isEmpty(overlapped)) {
				detailsObject = new PlaceDetailsObject(this.lang);
			} else {
				detailsObject = overlapped.get(0);
				for (int i = 1; i < overlapped.size(); i++) {
					detailsObject.merge(overlapped.get(i));
				}
				mergedObjects.removeAll(overlapped);
			}

			if (detailsObject.addObject(object)) {
				mergedObjects.add(detailsObject);
			} else {
				listener.onObjectNotCombined(o);
			}
		}
		return mergedObjects;
	}

	@NonNull
	private List<BaseDetailsObject> collectOverlappedObjects(
			@NonNull Object object,
			@NonNull List<BaseDetailsObject> detailsObjects) {

		List<BaseDetailsObject> overlapped = new ArrayList<>();

		for (BaseDetailsObject detailsObject : detailsObjects) {
			if (detailsObject.overlapsWith(object)) {
				overlapped.add(detailsObject);
			}
		}

		return overlapped;
	}

	protected interface ProcessObjectsListener<T> {
		Object getObjectToCombine(@NonNull T item);

		void onObjectNotCombined(@NonNull T item);
	}
}
