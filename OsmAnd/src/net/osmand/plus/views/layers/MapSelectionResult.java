package net.osmand.plus.views.layers;

import android.graphics.PointF;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.binary.ObfConstants;
import net.osmand.data.Amenity;
import net.osmand.data.BaseDetailsObject;
import net.osmand.data.LatLon;
import net.osmand.data.MapObject;
import net.osmand.data.RotatedTileBox;
import net.osmand.data.TransportStop;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.helpers.LocaleHelper;
import net.osmand.plus.utils.NativeUtilities;
import net.osmand.plus.views.layers.ContextMenuLayer.IContextMenuProvider;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
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

	public void groupOtherObjects() {
		processedObjects.sort((o1, o2) -> {
			int ord1 = getClassOrder(o1.object);
			int ord2 = getClassOrder(o2.object);
			if (ord1 != ord2) {
				return ord2 > ord1 ? -1 : 1;
			}
			return 0;
		});
		HashMap<Long, Integer> osmIds = new HashMap<>();
		Iterator<SelectedMapObject> it = processedObjects.iterator();
		//List<Integer> amenitiesIndexes = new ArrayList<>();
		while (it.hasNext()) {
			SelectedMapObject selectedMapObject = it.next();
			Long osmId = getOsmId(selectedMapObject.object);
			if (osmId == null)
				continue;
			Integer index = osmIds.get(osmId);
//			if (index == null) {
//				index = matchTransportStop(amenitiesIndexes, selectedMapObject.object);
//			}
			if (index != null) {
				SelectedMapObject other = processedObjects.get(index);
				if (other.object instanceof BaseDetailsObject bdo) {
					bdo.merge(selectedMapObject.object);
				} else {
					BaseDetailsObject bdo = new BaseDetailsObject(other.object, lang);
					bdo.merge(selectedMapObject.object);
					if (!BaseDetailsObject.shouldAdd(other.object)) {
						bdo.merge(other.object);
					}
				}
				it.remove();
			} else {
				int ind = processedObjects.indexOf(selectedMapObject);
//				if (selectedMapObject.object instanceof Amenity || selectedMapObject.object instanceof BaseDetailsObject) {
//					amenitiesIndexes.add(ind);
//				}
				osmIds.put(osmId, ind);
			}
		}
		allObjects.clear();
		allObjects.addAll(processedObjects);
	}

	/*private Integer matchTransportStop(List<Integer> amenitiesIndexes, Object object) {
		if (object instanceof TransportStop transportStop) {
			for (int i : amenitiesIndexes) {
				Object obj = processedObjects.get(i).object;
				Amenity amenity = obj instanceof Amenity ? (Amenity) obj : ((BaseDetailsObject) obj).getSyntheticAmenity();
				if (TransportStopHelper.matchAmenityAndTransportStop(transportStop, amenity)) {
					return i;
				}
			}
		}
		return null;
	}*/

	private int getClassOrder(Object object) {
		if (object instanceof BaseDetailsObject) {
			return 1;
		}
		if (object instanceof Amenity) {
			return 2;
		}
		if (object instanceof TransportStop) {
			return 3;
		}
		return 4;
	}

	private Long getOsmId(Object object) {
		if (object instanceof BaseDetailsObject baseDetailsObject) {
			return baseDetailsObject.getSyntheticAmenity().getOsmId();
		}
		if (object instanceof MapObject mapObject) {
			return ObfConstants.getOsmObjectId(mapObject);
		}
		return null;
	}

	public boolean isEmpty() {
		return allObjects.isEmpty();
	}

	public record SelectedMapObject(@NonNull Object object,
	                                @Nullable IContextMenuProvider provider) {

	}
}
