package net.osmand.plus.mapcontextmenu.builders;

import android.os.AsyncTask;

import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.NativeLibrary;
import net.osmand.data.Amenity;
import net.osmand.data.QuadRect;
import net.osmand.osm.MapPoiTypes;
import net.osmand.osm.PoiType;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.poi.PoiUIFilter;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;
import net.osmand.util.OsmUtils;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RenderedObjectMenuBuilder extends AmenityMenuBuilder {

	QuadRect bbox;
	NativeLibrary.RenderedObject renderedObject;

	public RenderedObjectMenuBuilder(@NonNull MapActivity mapActivity, @NonNull NativeLibrary.RenderedObject renderedObject) {
		super(mapActivity, getSyntheticAmenity(mapActivity, renderedObject));
		bbox = renderedObject.getBbox();
		this.renderedObject = renderedObject;
	}

	private void searchAmenity(ViewGroup view, Object object) {
		WeakReference<ViewGroup> viewGroupRef = new WeakReference<>(view);
		PoiUIFilter filter = getPoiFilterForAmenity(amenity);
		if (filter != null) {
			execute(new SearchAmenitiesTask(filter, new SearchAmenityForRenderedObjectListener() {
				@Override
				public void onFinish(Amenity am) {
					ViewGroup viewGroup = viewGroupRef.get();
					if (viewGroup == null) {
						return;
					}
					if (am != null) {
						amenity = am;
						amenity.setX(renderedObject.getX());
						amenity.setY(renderedObject.getY());
						additionalInfo = amenity.getAmenityExtensions(app.getPoiTypes(), false);
					}
					rebuild(viewGroup, object);
				}
			}));
		} else {
			rebuild(view, object);
		}
	}

	@Override
	public void build(@NonNull ViewGroup view, @Nullable Object object) {
		searchAmenity(view, object);
	}

	private void rebuild(@NonNull ViewGroup view, @Nullable Object object) {
		super.build(view, object);
	}

	private static Amenity getSyntheticAmenity(@NonNull MapActivity mapActivity, @NonNull NativeLibrary.RenderedObject renderedObject) {
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
				pt = mapPoiTypes.getPoiTypeByKey(e.getKey() + "_" + e.getValue());
			}
			if (pt != null) {
				subtype = value;
				continue;
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

	private class SearchAmenitiesTask extends AsyncTask<Void, Void, Amenity> {

		private final PoiUIFilter filter;
		private final SearchAmenityForRenderedObjectListener listener;
		private final QuadRect rect;
		private final long osmId;

		private SearchAmenitiesTask(PoiUIFilter filter, SearchAmenityForRenderedObjectListener listener) {
			this.filter = filter;
			this.listener = listener;
			double l = MapUtils.get31LongitudeX((int)bbox.left);
			double r = MapUtils.get31LongitudeX((int)bbox.right);
			double t = MapUtils.get31LatitudeY((int)bbox.top);
			double b = MapUtils.get31LatitudeY((int)bbox.bottom);
			rect = new QuadRect(l, t, r, b);
			osmId = OsmUtils.getOsmObjectId(renderedObject);
		}

		@Override
		protected Amenity doInBackground(Void... params) {
			List<Amenity> amenities = Collections.emptyList();
			amenities = filter.searchAmenities(rect.top, rect.left, rect.bottom, rect.right, -1, null);
			for (Amenity am : amenities) {
				long id = OsmUtils.getOsmObjectId(am);
				if (id == osmId) {
					return am;
				}
			}
			return null;
		}

		@Override
		protected void onPostExecute(Amenity amenity) {
			if (listener != null) {
				listener.onFinish(amenity);
			}
		}
	}

	private interface SearchAmenityForRenderedObjectListener {
		void onFinish(Amenity amenity);
	}
}
