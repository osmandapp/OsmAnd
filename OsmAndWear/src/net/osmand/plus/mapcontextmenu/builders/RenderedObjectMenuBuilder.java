package net.osmand.plus.mapcontextmenu.builders;

import static net.osmand.NativeLibrary.RenderedObject;
import android.os.AsyncTask;

import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.CallbackWithObject;
import net.osmand.ResultMatcher;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.ObfConstants;
import net.osmand.data.Amenity;
import net.osmand.data.QuadRect;
import net.osmand.osm.MapPoiTypes;
import net.osmand.osm.PoiType;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.activities.MapActivity;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RenderedObjectMenuBuilder extends AmenityMenuBuilder {

	QuadRect bbox;
	RenderedObject renderedObject;
	private static final int SEARCH_POI_RADIUS = 50;

	public RenderedObjectMenuBuilder(@NonNull MapActivity mapActivity, @NonNull RenderedObject renderedObject) {
		super(mapActivity, getSyntheticAmenity(mapActivity, renderedObject));
		bbox = renderedObject.getBbox();
		this.renderedObject = renderedObject;
	}

	private void searchAmenity(ViewGroup view, Object object) {
		WeakReference<ViewGroup> viewGroupRef = new WeakReference<>(view);
		execute(new SearchAmenitiesTask(app, renderedObject, am -> {
			ViewGroup viewGroup = viewGroupRef.get();
			if (viewGroup == null) {
				return false;
			}
			if (am != null) {
				amenity = am;
				amenity.setX(renderedObject.getX());
				amenity.setY(renderedObject.getY());
				additionalInfo = amenity.getAmenityExtensions(app.getPoiTypes(), false);
			}
			RenderedObjectMenuBuilder.this.rebuild(viewGroup, object);
			return true;
		}));
	}

	@Override
	public void build(@NonNull ViewGroup view, @Nullable Object object) {
		searchAmenity(view, object);
	}

	private void rebuild(@NonNull ViewGroup view, @Nullable Object object) {
		super.build(view, object);
	}

	private static Amenity getSyntheticAmenity(@NonNull MapActivity mapActivity, @NonNull RenderedObject renderedObject) {
		Amenity am = new Amenity();
		OsmandApplication app = mapActivity.getMyApplication();
		MapPoiTypes mapPoiTypes = app.getPoiTypes();
		am.setType(mapPoiTypes.getOtherPoiCategory());
		am.setSubType("");
		MapPoiTypes.PoiTranslator poiTranslator = mapPoiTypes.getPoiTranslator();
		PoiType pt = null;
		PoiType otherPt = null;
		String subtype = null;
		Map<String, String> additionalInfo = new HashMap<>();
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
		} else if(otherPt != null) {
			am.setType(otherPt.getCategory());
			am.setSubType(otherPt.getKeyName());
		}
		if (subtype != null) {
			am.setSubType(subtype);
		}
		am.setId(renderedObject.getId());
		am.setAdditionalInfo(additionalInfo);
		am.setX(renderedObject.getX());
		am.setY(renderedObject.getY());
		return am;
	}

	private static class SearchAmenitiesTask extends AsyncTask<Void, Void, Amenity> {

		private final CallbackWithObject<Amenity> listener;
		private final QuadRect rect;
		private final long osmId;
		private final OsmandApplication app;

		private SearchAmenitiesTask(OsmandApplication application, RenderedObject renderedObject, CallbackWithObject<Amenity> listener) {
			this.listener = listener;
			double lat = MapUtils.get31LatitudeY(renderedObject.getLabelY());
			double lon = MapUtils.get31LongitudeX(renderedObject.getLabelX());
			rect = MapUtils.calculateLatLonBbox(lat, lon, SEARCH_POI_RADIUS);
			osmId = ObfConstants.getOsmObjectId(renderedObject);
			app = application;
		}

		@Override
		protected Amenity doInBackground(Void... params) {
			List<Amenity> amenities = app.getResourceManager().searchAmenities(
					BinaryMapIndexReader.ACCEPT_ALL_POI_TYPE_FILTER,
					rect.top, rect.left, rect.bottom, rect.right,
					-1, true,
					new ResultMatcher<>() {
						@Override
						public boolean publish(Amenity amenity) {
							long id = ObfConstants.getOsmObjectId(amenity);
							return id == osmId;
						}

						@Override
						public boolean isCancelled() {
							return false;
						}
			});
			if (amenities.size() > 0) {
				return amenities.get(0);
			}
			return null;
		}

		@Override
		protected void onPostExecute(Amenity amenity) {
			listener.processResult(amenity);
		}
	}
}
