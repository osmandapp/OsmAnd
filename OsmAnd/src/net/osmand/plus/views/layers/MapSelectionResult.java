package net.osmand.plus.views.layers;

import android.graphics.PointF;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.data.Amenity;
import net.osmand.data.BaseDetailsObject;
import net.osmand.data.LatLon;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.helpers.LocaleHelper;
import net.osmand.plus.utils.NativeUtilities;
import net.osmand.plus.views.layers.ContextMenuLayer.IContextMenuProvider;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;

public class MapSelectionResult {

	private final String lang;
	private final PointF point;
	private final LatLon pointLatLon;
	private final RotatedTileBox tileBox;
	private final IContextMenuProvider poiProvider;

	private final List<SelectedMapObject> allObjects = new ArrayList<>();
	private final List<SelectedMapObject> processedObjects = new ArrayList<>();

	protected LatLon objectLatLon;

	public MapSelectionResult(@NonNull OsmandApplication app, @NonNull RotatedTileBox tileBox,
			@NonNull PointF point) {
		this.point = point;
		this.tileBox = tileBox;
		this.lang = LocaleHelper.getPreferredPlacesLanguage(app);
		this.poiProvider = app.getOsmandMap().getMapLayers().getPoiMapLayer();
		this.pointLatLon = NativeUtilities.getLatLonFromElevatedPixel(app.getOsmandMap().getMapView().getMapRenderer(), tileBox, point);
	}

	@NonNull
	public PointF getPoint() {
		return point;
	}

	@NonNull
	public LatLon getPointLatLon() {
		return pointLatLon;
	}

	@NonNull
	public RotatedTileBox getTileBox() {
		return tileBox;
	}

	@NonNull
	public List<SelectedMapObject> getAllObjects() {
		return allObjects;
	}

	@NonNull
	public List<SelectedMapObject> getProcessedObjects() {
		return processedObjects;
	}

	@Nullable
	public LatLon getObjectLatLon() {
		return objectLatLon;
	}

	public void setObjectLatLon(@Nullable LatLon objectLatLon) {
		this.objectLatLon = objectLatLon;
	}

	public void collect(@NonNull Object object, @Nullable IContextMenuProvider provider) {
		allObjects.add(new SelectedMapObject(object, provider));
	}

	public void groupByOsmIdAndWikidataId() {
		List<SelectedMapObject> amenities = new ArrayList<>();
		List<SelectedMapObject> supported = new ArrayList<>();
		List<SelectedMapObject> other = new ArrayList<>();

		for (SelectedMapObject selectedObject : allObjects) {
			Object object = selectedObject.object();
			if (object instanceof Amenity) {
				amenities.add(selectedObject);
			} else if (BaseDetailsObject.isSupportedObjectType(object)) {
				supported.add(selectedObject);
			} else {
				other.add(selectedObject);
			}
		}

		List<BaseDetailsObject> detailsObjects = processObjects(amenities, supported, other);
		for (BaseDetailsObject object : detailsObjects) {
			object.combineData();
			processedObjects.add(new SelectedMapObject(object, poiProvider));
		}
		processedObjects.addAll(other);
	}

	@NonNull
	private List<BaseDetailsObject> processObjects(@NonNull List<SelectedMapObject> amenities,
			@NonNull List<SelectedMapObject> supported, @NonNull List<SelectedMapObject> other) {
		List<BaseDetailsObject> detailsObjects = new ArrayList<>();
		processGroup(amenities, detailsObjects, null);
		processGroup(supported, detailsObjects, other);
		return detailsObjects;
	}

	private void processGroup(@NonNull List<SelectedMapObject> selectedMapObjects,
			@NonNull List<BaseDetailsObject> detailsObjects,
			@Nullable List<SelectedMapObject> nonOverlapped) {

		for (SelectedMapObject selectedObject : selectedMapObjects) {
			Object object = selectedObject.object();
			List<BaseDetailsObject> overlapped = collectOverlappedObjects(object, detailsObjects);

			BaseDetailsObject detailsObject;
			if (Algorithms.isEmpty(overlapped)) {
				if (nonOverlapped != null) {
					nonOverlapped.add(selectedObject);
					continue;
				}
				detailsObject = new BaseDetailsObject(this.lang);
			} else {
				detailsObject = overlapped.get(0);
				for (int i = 1; i < overlapped.size(); i++) {
					detailsObject.merge(overlapped.get(i));
				}
				detailsObjects.removeAll(overlapped);
			}
			detailsObject.addObject(object);
			detailsObjects.add(detailsObject);
		}
	}

	@NonNull
	private List<BaseDetailsObject> collectOverlappedObjects(@NonNull Object object,
			@NonNull List<BaseDetailsObject> detailsObjects) {
		List<BaseDetailsObject> overlapped = new ArrayList<>();
		for (BaseDetailsObject detailsObject : detailsObjects) {
			if (detailsObject.overlapsWith(object)) {
				overlapped.add(detailsObject);
			}
		}
		return overlapped;
	}

	public boolean isEmpty() {
		return allObjects.isEmpty();
	}

	public record SelectedMapObject(@NonNull Object object,
	                                @Nullable IContextMenuProvider provider) {

	}
}
