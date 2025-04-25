package net.osmand.plus.views.layers;

import static net.osmand.data.Amenity.DEFAULT_ELO;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.binary.BinaryMapIndexReader.TagValuePair;
import net.osmand.binary.ObfConstants;
import net.osmand.data.Amenity;
import net.osmand.data.LatLon;
import net.osmand.data.MapObject;
import net.osmand.osm.PoiCategory;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.views.layers.ContextMenuLayer.IContextMenuProvider;
import net.osmand.plus.views.layers.MapSelectionResult.SelectedMapObject;
import net.osmand.util.Algorithms;

import java.util.*;

import gnu.trove.list.array.TIntArrayList;

public class PlaceDetailsObject {

	private final Set<Long> osmIds = new HashSet<>();
	private final Set<String> wikidataIds = new HashSet<>();
	private final List<SelectedMapObject> selectedObjects = new ArrayList<>();

	private final Amenity syntheticAmenity = new Amenity();

	@NonNull
	public Amenity getSyntheticAmenity() {
		return syntheticAmenity;
	}

	@NonNull
	public List<SelectedMapObject> getSelectedObjects() {
		return selectedObjects;
	}

	public void addObject(@NonNull Object object, @Nullable IContextMenuProvider provider) {
		if (shouldSkip(object)) {
			return;
		}
		selectedObjects.add(new SelectedMapObject(object, provider));
		if (object instanceof MapObject mapObject) {
			long osmId = ObfConstants.getOsmObjectId(mapObject);
			osmIds.add(osmId);
		}
		if (object instanceof Amenity amenity) {
			String wikidata = amenity.getWikidata();
			if (!Algorithms.isEmpty(wikidata)) {
				wikidataIds.add(wikidata);
			}
		}
	}

	public boolean overlapsWith(@NonNull Object object) {
		Long osmId = (object instanceof MapObject) ? ObfConstants.getOsmObjectId((MapObject) object) : null;
		String wikidata = (object instanceof Amenity) ? ((Amenity) object).getWikidata() : null;

		return (osmId != null && osmIds.contains(osmId))
				|| (!Algorithms.isEmpty(wikidata) && wikidataIds.contains(wikidata));
	}

	public void merge(@NonNull PlaceDetailsObject other) {
		osmIds.addAll(other.osmIds);
		wikidataIds.addAll(other.wikidataIds);
		selectedObjects.addAll(other.getSelectedObjects());
	}

	public void combineData(@NonNull OsmandApplication app) {
		Set<String> contentLocales = new TreeSet<>();
		for (SelectedMapObject selectedObject : selectedObjects) {
			Object object = selectedObject.object();
			if (object instanceof Amenity amenity) {
				processAmenity(amenity, contentLocales);
			}
		}
		if (!Algorithms.isEmpty(contentLocales)) {
			syntheticAmenity.updateContentLocales(contentLocales);
		}
	}

	private void processAmenity(@NonNull Amenity amenity, @NonNull Set<String> contentLocales) {
		Long id = amenity.getId();
		if (syntheticAmenity.getId() == null && id != null) {
			syntheticAmenity.setId(id);
		}
		LatLon location = amenity.getLocation();
		if (syntheticAmenity.getLocation() == null && location != null) {
			syntheticAmenity.setLocation(location);
		}
		PoiCategory type = amenity.getType();
		if (syntheticAmenity.getType() == null && type != null) {
			syntheticAmenity.setType(type);
		}
		String subType = amenity.getSubType();
		if (syntheticAmenity.getSubType() == null && subType != null) {
			syntheticAmenity.setSubType(subType);
		}
		String mapIconName = amenity.getMapIconName();
		if (syntheticAmenity.getMapIconName() == null && mapIconName != null) {
			syntheticAmenity.setMapIconName(mapIconName);
		}
		Map<Integer, List<TagValuePair>> groups = amenity.getTagGroups();
		if (syntheticAmenity.getTagGroups() == null && groups != null) {
			syntheticAmenity.setTagGroups(new HashMap<>(groups));
		}
		int travelElo = amenity.getTravelEloNumber();
		if (syntheticAmenity.getTravelEloNumber() == DEFAULT_ELO && travelElo != DEFAULT_ELO) {
			syntheticAmenity.setTravelEloNumber(travelElo);
		}
		TIntArrayList x = amenity.getX();
		if (syntheticAmenity.getX().isEmpty() && !x.isEmpty()) {
			syntheticAmenity.getX().addAll(x);
		}
		TIntArrayList y = amenity.getY();
		if (syntheticAmenity.getY().isEmpty() && !y.isEmpty()) {
			syntheticAmenity.getY().addAll(y);
		}
		syntheticAmenity.copyNames(amenity);
		syntheticAmenity.copyAdditionalInfo(amenity, false);

		contentLocales.addAll(amenity.getSupportedContentLocales());
	}

	public static boolean shouldSkip(@NonNull Object object) {
		return !(object instanceof Amenity);
	}
}