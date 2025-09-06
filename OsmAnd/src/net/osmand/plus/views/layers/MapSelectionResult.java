package net.osmand.plus.views.layers;

import android.graphics.PointF;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.data.Amenity;
import net.osmand.data.BaseDetailsObject;
import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.helpers.LocaleHelper;
import net.osmand.plus.utils.NativeUtilities;
import net.osmand.plus.views.layers.ContextMenuLayer.IContextMenuProvider;
import net.osmand.search.AmenitySearcher;
import net.osmand.shared.gpx.primitives.WptPt;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class MapSelectionResult {

	private final OsmandApplication app;
	private final AmenitySearcher searcher;
	private final AmenitySearcher.Settings searchSettings;
	private final String lang;
	private final PointF point;
	private final LatLon pointLatLon;
	private final RotatedTileBox tileBox;
	private final IContextMenuProvider poiProvider;

	private final List<SelectedMapObject> allObjects = new ArrayList<>();
	private final List<SelectedMapObject> processedObjects = new ArrayList<>();

	private LatLon objectLatLon;
	private List<Amenity> amenities = null;

	public MapSelectionResult(@NonNull OsmandApplication app, @NonNull RotatedTileBox tileBox,
			@NonNull PointF point) {
		this.app = app;
		this.point = point;
		this.tileBox = tileBox;
		this.lang = LocaleHelper.getPreferredPlacesLanguage(app);
		this.poiProvider = app.getOsmandMap().getMapLayers().getPoiMapLayer();
		this.pointLatLon = NativeUtilities.getLatLonFromElevatedPixel(app.getOsmandMap().getMapView().getMapRenderer(), tileBox, point);
		this.searcher = app.getResourceManager().getAmenitySearcher();
		this.searchSettings = app.getResourceManager().getDefaultAmenitySearchSettings();
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
		if (allObjects.size() == 1) {
			processedObjects.addAll(allObjects);
			return;
		}
		List<SelectedMapObject> other = new ArrayList<>();
		List<SelectedMapObject> mapObjects = new ArrayList<>(allObjects);
		List<BaseDetailsObject> detailsObjects = processPointsWithAmenities(mapObjects, other);

		detailsObjects.addAll(processObjects(mapObjects, other));

		for (BaseDetailsObject object : detailsObjects) {
			if (object.getObjects().size() > 1) {
				processedObjects.add(new SelectedMapObject(object, poiProvider));
			} else {
				processedObjects.add(new SelectedMapObject(object.getObjects().get(0), poiProvider));
			}
		}
		processedObjects.addAll(other);
	}

	@NonNull
	private List<BaseDetailsObject> processObjects(@NonNull List<SelectedMapObject> selectedObjects,
			@NonNull List<SelectedMapObject> other) {
		List<BaseDetailsObject> detailsObjects = new ArrayList<>();
		for (SelectedMapObject selectedObject : selectedObjects) {
			Object object = selectedObject.object();
			List<BaseDetailsObject> overlapped = collectOverlappedObjects(object, detailsObjects);

			BaseDetailsObject detailsObject;
			if (Algorithms.isEmpty(overlapped)) {
				detailsObject = new PlaceDetailsObject(this.lang);
			} else {
				detailsObject = overlapped.get(0);
				for (int i = 1; i < overlapped.size(); i++) {
					detailsObject.merge(overlapped.get(i));
				}
				detailsObjects.removeAll(overlapped);
			}
			if (detailsObject.addObject(object)) {
				detailsObjects.add(detailsObject);
			} else {
				other.add(selectedObject);
			}
		}
		return detailsObjects;
	}

	@NonNull
	private List<BaseDetailsObject> processPointsWithAmenities(
			@NonNull List<SelectedMapObject> mapObjects,
			@NonNull List<SelectedMapObject> other) {
		List<BaseDetailsObject> detailsObjects = new ArrayList<>();
		for (Iterator<SelectedMapObject> iterator = mapObjects.iterator(); iterator.hasNext(); ) {
			Object object = iterator.next().object();
			if (object instanceof WptPt point) {
				String originName = point.getAmenityOriginName();
				if (!Algorithms.isEmpty(originName)) {
					List<Amenity> filtered = filterAmenities(originName);
					if (!Algorithms.isEmpty(filtered)) {
						PlaceDetailsObject detailsObject = new PlaceDetailsObject(filtered, searchSettings.language().get());
						detailsObject.addObject(point);
						detailsObjects.add(detailsObject);
						iterator.remove();
					}
				}
			}
			if (object instanceof FavouritePoint point) {
				String originName = point.getAmenityOriginName();
				if (!Algorithms.isEmpty(originName)) {
					List<Amenity> filtered = filterAmenities(originName);
					if (!Algorithms.isEmpty(filtered)) {
						PlaceDetailsObject detailsObject = new PlaceDetailsObject(filtered, searchSettings.language().get());
						detailsObject.addObject(point);
						detailsObjects.add(detailsObject);
						iterator.remove();
					}
				}
			}
		}
		clearOverlappedObjects(mapObjects, detailsObjects);

		return detailsObjects;
	}

	private void clearOverlappedObjects(@NonNull List<SelectedMapObject> mapObjects,
			@NonNull List<BaseDetailsObject> detailsObjects) {
		if (!Algorithms.isEmpty(detailsObjects)) {
			for (Iterator<SelectedMapObject> iterator = mapObjects.iterator(); iterator.hasNext(); ) {
				Object object = iterator.next().object();
				List<BaseDetailsObject> overlapped = collectOverlappedObjects(object, detailsObjects);
				if (!Algorithms.isEmpty(overlapped)) {
					for (BaseDetailsObject detailsObject : overlapped) {
						detailsObject.addObject(object);
					}
					iterator.remove();
				}
			}
		}
	}

	@Nullable
	public List<Amenity> filterAmenities(@NonNull String nameEn) {
		if (amenities == null) {
			amenities = searcher.searchAmenities(pointLatLon, searchSettings);
		}
		List<String> names = Collections.singletonList(nameEn);

		Amenity requestAmenity = new Amenity();
		requestAmenity.setLocation(pointLatLon);
		AmenitySearcher.Request request = new AmenitySearcher.Request(requestAmenity, names, true);
		return searcher.filterAmenities(amenities, request, searchSettings);
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
}