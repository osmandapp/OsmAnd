package net.osmand.plus.mapcontextmenu.builders;

import static net.osmand.NativeLibrary.RenderedObject;

import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.binary.ObfConstants;
import net.osmand.data.Amenity;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.osm.MapPoiTypes;
import net.osmand.osm.PoiType;
import net.osmand.osm.edit.Entity;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.views.MapLayers;
import net.osmand.search.AmenitySearcher;
import net.osmand.util.Algorithms;

import java.lang.ref.WeakReference;
import java.util.LinkedHashMap;
import java.util.Map;

public class RenderedObjectMenuBuilder extends AmenityMenuBuilder {

	private RenderedObject renderedObject;

	public RenderedObjectMenuBuilder(@NonNull MapActivity mapActivity,
			@NonNull RenderedObject renderedObject) {
		super(mapActivity, getSyntheticAmenity(mapActivity, renderedObject));
		this.renderedObject = renderedObject;
	}

	@Override
	public void build(@NonNull ViewGroup view, @Nullable Object object) {
		searchAmenity(view, object);
	}

	private void searchAmenity(@NonNull ViewGroup view, @Nullable Object object) {
		WeakReference<ViewGroup> viewGroupRef = new WeakReference<>(view);
		AmenitySearcher searcher = app.getResourceManager().getAmenitySearcher();
		AmenitySearcher.Settings settings = app.getResourceManager().getDefaultAmenitySearchSettings();
		searcher.searchBaseDetailedObjectAsync(renderedObject, settings, detailsObject -> {
			app.runInUIThread(() -> {
				ViewGroup viewGroup = viewGroupRef.get();
				if (viewGroup == null || mapContextMenu == null) {
					return;
				}
				if (detailsObject != null) {
					LatLon latLon = getLatLon();
					MapLayers mapLayers = mapActivity.getMapLayers();
					PointDescription description = mapLayers.getPoiMapLayer().getObjectName(detailsObject);
					mapContextMenu.update(latLon, description, detailsObject);
				} else {
					super.build(viewGroup, object);
				}
			});
			return true;
		});
	}

	private static Amenity getSyntheticAmenity(@NonNull MapActivity mapActivity,
			@NonNull RenderedObject renderedObject) {
		Amenity am = new Amenity();
		OsmandApplication app = mapActivity.getApp();
		MapPoiTypes mapPoiTypes = app.getPoiTypes();
		am.setType(mapPoiTypes.getOtherPoiCategory());
		am.setSubType("");
		MapPoiTypes.PoiTranslator poiTranslator = mapPoiTypes.getPoiTranslator();
		PoiType pt = null;
		PoiType otherPt = null;
		String subtype = null;
		Map<String, String> additionalInfo = new LinkedHashMap<>();
		for (Map.Entry<String, String> e : renderedObject.getTags().entrySet()) {
			String tag = e.getKey();
			String value = e.getValue();
			if (tag.equals("name")) {
				am.setName(value);
				continue;
			}
			if (e.getKey().startsWith("name:")) {
				am.setName(tag.substring("name:".length()), value);
				continue;
			}
			if (tag.equals("amenity")) {
				if (pt != null) {
					otherPt = pt;
				}
				pt = mapPoiTypes.getPoiTypeByKey(value);
			} else {
				PoiType poiType = mapPoiTypes.getPoiTypeByKey(e.getKey() + "_" + e.getValue());
				if (poiType == null) {
					poiType = mapPoiTypes.getPoiTypeByKey(e.getKey());
				}
				if (poiType != null) {
					otherPt = pt != null ? poiType : otherPt;
					subtype = pt == null ? value : subtype;
					pt = pt == null ? poiType : pt;
				}
			}
			if (Algorithms.isEmpty(value) && otherPt == null) {
				otherPt = mapPoiTypes.getPoiTypeByKey(tag);
			}
			if (otherPt == null) {
				PoiType poiType = mapPoiTypes.getPoiTypeByKey(value);
				if (poiType != null && poiType.getOsmTag().equals(tag)) {
					otherPt = poiType;
				}
			}
			if (!Algorithms.isEmpty(value)) {
				String translate = poiTranslator.getTranslation(tag + "_" + value);
				String translate2 = poiTranslator.getTranslation(value);
				if (translate != null && translate2 != null) {
					additionalInfo.put(translate, translate2);
				} else {
					additionalInfo.put(tag, value);
				}
			}
		}
		if (pt != null) {
			am.setType(pt.getCategory());
		} else if (otherPt != null) {
			am.setType(otherPt.getCategory());
			am.setSubType(otherPt.getKeyName());
		}
		if (subtype != null) {
			am.setSubType(subtype);
		}
		Entity.EntityType type = ObfConstants.getOsmEntityType(renderedObject);
		if (type != null) {
			long osmId = ObfConstants.getOsmObjectId(renderedObject);
			long objectId = ObfConstants.createMapObjectIdFromOsmId(osmId, type);
			am.setId(objectId);
		}
		am.setAdditionalInfo(additionalInfo);
		am.setX(renderedObject.getX());
		am.setY(renderedObject.getY());
		return am;
	}
}
