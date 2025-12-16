package net.osmand.plus.views.layers;

import androidx.annotation.NonNull;

import net.osmand.data.BaseDetailsObject;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;

public abstract class MapObjectsMerger<T> {

	private final String lang;

	public MapObjectsMerger(@NonNull String lang) {
		this.lang = lang;
	}

	@NonNull
	public abstract List<T> merge(@NonNull List<T> originalObjects);

	@NonNull
	protected List<BaseDetailsObject> processObjects(@NonNull List<T> items,
	                                                 @NonNull MergeStrategy<T> strategy) {

		List<BaseDetailsObject> merged = new ArrayList<>();

		for (T item : items) {
			Object object = strategy.unwrap(item);
			List<BaseDetailsObject> overlapped = collectOverlappedObjects(object, merged);

			BaseDetailsObject detailsObject;
			if (Algorithms.isEmpty(overlapped)) {
				detailsObject = new PlaceDetailsObject(this.lang);
			} else {
				detailsObject = overlapped.get(0);
				for (int i = 1; i < overlapped.size(); i++) {
					detailsObject.merge(overlapped.get(i));
				}
				merged.removeAll(overlapped);
			}

			if (detailsObject.addObject(object)) {
				merged.add(detailsObject);
			} else {
				strategy.onMergeFailed(item);
			}
		}
		return merged;
	}

	@NonNull
	protected List<BaseDetailsObject> collectOverlappedObjects(
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

	protected interface MergeStrategy<T> {
		/**
		 * Extracts the underlying geometry/object used for overlap checks.
		 * e.g., extracts MapObject from SelectedMapObject.
		 */
		Object unwrap(@NonNull T item);

		/**
		 * Called when the item implies no overlap or cannot be merged.
		 * e.g., adds the original item to the 'others' list.
		 */
		void onMergeFailed(@NonNull T item);
	}
}
