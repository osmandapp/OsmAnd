package net.osmand.plus.views.layers;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.PlatformUtil;
import net.osmand.binary.ObfConstants;
import net.osmand.data.Amenity;
import net.osmand.data.BaseDetailsObject;
import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.data.MapObject;
import net.osmand.plus.views.layers.ContextMenuLayer.IContextMenuProvider;
import net.osmand.search.AmenitySearcher;
import net.osmand.shared.gpx.primitives.WptPt;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class SelectedMapObjectsMerger extends MapObjectsMerger<SelectedMapObject> {

	private static final Log LOG = PlatformUtil.getLog(SelectedMapObjectsMerger.class);

	private final AmenitySearcher searcher;
	private final AmenitySearcher.Settings searchSettings;
	private final IContextMenuProvider poiProvider;
	private final LatLon pointLatLon;

	private List<Amenity> amenities = null;

	public SelectedMapObjectsMerger(@NonNull AmenitySearcher searcher,
	                                @NonNull AmenitySearcher.Settings searchSettings,
	                                @NonNull IContextMenuProvider poiProvider,
	                                @NonNull String lang,
	                                @NonNull LatLon pointLatLon) {
		super(lang);
		this.searcher = searcher;
		this.searchSettings = searchSettings;
		this.poiProvider = poiProvider;
		this.pointLatLon = pointLatLon;
	}

	@NonNull
	@Override
	public List<SelectedMapObject> merge(@NonNull List<SelectedMapObject> original) {
		if (original.size() == 1) {
			return new ArrayList<>(original);
		}

		List<SelectedMapObject> result = new ArrayList<>();
		List<SelectedMapObject> allItems = new ArrayList<>(original);
		List<SelectedMapObject> unmergedItems = new ArrayList<>();

		List<BaseDetailsObject> groupedDetails = processPoints(allItems);

		groupedDetails.addAll(processObjects(allItems, new MergeStrategy<>() {
			@Override
			public Object unwrap(@NonNull SelectedMapObject item) {
				return item.object();
			}

			@Override
			public void onMergeFailed(@NonNull SelectedMapObject item) {
				unmergedItems.add(item);
			}
		}));

		for (BaseDetailsObject object : groupedDetails) {
			if (object.getLocation() == null) {
				object.getSyntheticAmenity().setLocation(pointLatLon);
				LOG.debug("BaseDetailsObject without location " + object);
			}
			if (object.getObjects().size() > 1) {
				result.add(new SelectedMapObject(object, poiProvider));
			} else {
				result.add(new SelectedMapObject(object.getObjects().get(0), poiProvider));
			}
		}

		result.addAll(unmergedItems);
		return result;
	}

	@NonNull
	private List<BaseDetailsObject> processPoints(@NonNull List<SelectedMapObject> mapObjects) {
		List<BaseDetailsObject> detailsObjects = new ArrayList<>();

		for (Iterator<SelectedMapObject> iterator = mapObjects.iterator(); iterator.hasNext(); ) {
			Object object = iterator.next().object();

			if (object instanceof WptPt point) {
				String originName = point.getAmenityOriginName();
				if (!Algorithms.isEmpty(originName)) {
					List<? extends MapObject> filtered = filterMapObjects(originName, mapObjects);
					if (!Algorithms.isEmpty(filtered)) {
						PlaceDetailsObject detailsObject =
								new PlaceDetailsObject(filtered, searchSettings.language().get());
						detailsObject.addObject(point);
						detailsObjects.add(detailsObject);
						iterator.remove();
					}
				}
			}

			if (object instanceof FavouritePoint point) {
				String originName = point.getAmenityOriginName();
				if (!Algorithms.isEmpty(originName)) {
					List<? extends MapObject> filtered = filterMapObjects(originName, mapObjects);
					if (!Algorithms.isEmpty(filtered)) {
						PlaceDetailsObject detailsObject =
								new PlaceDetailsObject(filtered, searchSettings.language().get());
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
	private List<? extends MapObject> filterMapObjects(
			@NonNull String nameEn,
			@NonNull List<SelectedMapObject> selectedMapObjects) {

		if (nameEn.startsWith("Amenity")) {
			if (amenities == null) {
				amenities = searcher.searchAmenities(pointLatLon, searchSettings);
			}

			List<String> names = Collections.singletonList(nameEn);
			Amenity requestAmenity = new Amenity();
			requestAmenity.setLocation(pointLatLon);

			AmenitySearcher.Request request =
					new AmenitySearcher.Request(requestAmenity, names, true);

			return searcher.filterAmenities(amenities, request, searchSettings);
		}

		if (nameEn.startsWith("MapObject")) {
			List<MapObject> result = new ArrayList<>();

			for (SelectedMapObject smo : selectedMapObjects) {
				Object object = smo.object();
				if (object instanceof MapObject mapObject) {
					long osmId = ObfConstants.getOsmObjectId(mapObject);
					if (nameEn.contains(String.valueOf(osmId))) {
						result.add(mapObject);
					}
				}
			}
			return result;
		}

		return null;
	}
}
