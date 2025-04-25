package net.osmand.plus.views.layers;

import android.graphics.PointF;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.data.LatLon;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.utils.NativeUtilities;
import net.osmand.plus.views.layers.ContextMenuLayer.IContextMenuProvider;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;

public class MapSelectionResult {

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
		List<PlaceDetailsObject> detailsObjects = new ArrayList<>();
		for (SelectedMapObject selectedObject : allObjects) {
			Object object = selectedObject.object();
			if (PlaceDetailsObject.shouldSkip(object)) {
				processedObjects.add(selectedObject);
				continue;
			}
			List<PlaceDetailsObject> overlapped = new ArrayList<>();
			for (PlaceDetailsObject detailsObject : detailsObjects) {
				if (detailsObject.overlapsWith(object)) {
					overlapped.add(detailsObject);
				}
			}
			PlaceDetailsObject detailsObject;
			if (Algorithms.isEmpty(overlapped)) {
				detailsObject = new PlaceDetailsObject();
			} else {
				detailsObject = overlapped.get(0);
				for (int i = 1; i < overlapped.size(); i++) {
					detailsObject.merge(overlapped.get(i));
				}
				detailsObjects.removeAll(overlapped);
			}
			detailsObject.addObject(object, selectedObject.provider());
			detailsObjects.add(detailsObject);
		}
		for (PlaceDetailsObject object : detailsObjects) {
			object.combineData();
			processedObjects.add(new SelectedMapObject(object, poiProvider));
		}
	}

	public boolean isEmpty() {
		return allObjects.isEmpty();
	}

	public record SelectedMapObject(@NonNull Object object,
	                                @Nullable IContextMenuProvider provider) {

	}
}
