package net.osmand.plus.mapcontextmenu.controllers;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.NativeLibrary.RenderedObject;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.osm.AbstractPoiType;
import net.osmand.osm.MapPoiTypes;
import net.osmand.osm.PoiType;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.MenuController;
import net.osmand.plus.mapcontextmenu.builders.RenderedObjectMenuBuilder;
import net.osmand.plus.render.RenderingIcons;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.util.Algorithms;

import java.util.Map;

public class RenderedObjectMenuController extends MenuController {

	private static final String POI_PREFIX = "poi";

	private RenderedObject renderedObject;

	public RenderedObjectMenuController(@NonNull MapActivity mapActivity,
	                                    @NonNull PointDescription pointDescription,
	                                    @NonNull RenderedObject renderedObject) {
		super(new RenderedObjectMenuBuilder(mapActivity, renderedObject), pointDescription, mapActivity);
		builder.setShowNearestWiki(true);
		setRenderedObject(renderedObject);
	}

	@Override
	protected void setObject(Object object) {
		if (object instanceof RenderedObject) {
			setRenderedObject((RenderedObject) object);
		}
	}

	@Override
	protected Object getObject() {
		return renderedObject;
	}

	private void setRenderedObject(@NonNull RenderedObject renderedObject) {
		this.renderedObject = renderedObject;
	}

	@Override
	public boolean displayStreetNameInTitle() {
		return Algorithms.isEmpty(getNameStr());
	}

	@Override
	public boolean displayDistanceDirection() {
		return true;
	}

	@Override
	public int getRightIconId() {
		String iconRes = getIconRes();
		if (iconRes != null && RenderingIcons.containsBigIcon(iconRes)) {
			return RenderingIcons.getBigIconResourceId(iconRes);
		} else {
			return R.drawable.ic_action_street_name;
		}
	}

	@NonNull
	@Override
	public String getNameStr() {
		String lang = getPreferredMapLangLC();
		boolean transliterate = isTransliterateNames();
		String name = renderedObject.getName(lang, transliterate);

		if (!Algorithms.isEmpty(name) && !isStartingWithRTLChar(name)) {
			return name;
		} else if (renderedObject.getTags().size() > 0) {
			name = "";
			if (!Algorithms.isEmpty(lang)) {
				name = renderedObject.getTagValue("name:" + lang);
			}
			if (Algorithms.isEmpty(name)) {
				name = renderedObject.getTagValue("name");
			}
		}
		if (!Algorithms.isEmpty(name)) {
			return name;
		}
		return searchObjectNameById();
	}

	@NonNull
	@Override
	public String getTypeStr() {
		if (renderedObject.isPolygon()) {
			return getTranslatedType(renderedObject);
		}
		return super.getTypeStr();
	}

	private String getTranslatedType(RenderedObject renderedObject) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity == null) {
			return "";
		}
		OsmandApplication app = mapActivity.getMyApplication();
		MapPoiTypes mapPoiTypes = app.getPoiTypes();
		MapPoiTypes.PoiTranslator poiTranslator = mapPoiTypes.getPoiTranslator();
		PoiType pt = null;
		PoiType otherPt = null;
		String translated = null;
		String firstTag = "";
		String separate = null;
		String single = null;
		for (Map.Entry<String, String> e : renderedObject.getTags().entrySet()) {
			if (e.getKey().startsWith("name")) {
				continue;
			}
			if (Algorithms.isEmpty(e.getValue()) && otherPt == null) {
				otherPt = mapPoiTypes.getPoiTypeByKey(e.getKey());
			}
			pt = mapPoiTypes.getPoiTypeByKey(e.getKey() + "_" + e.getValue());
			if (pt != null) {
				break;
			}
			firstTag = firstTag.isEmpty() ? e.getKey() + ": " + e.getValue() : firstTag;
			if (poiTranslator != null && !Algorithms.isEmpty(e.getValue())) {
				String t = poiTranslator.getTranslation(e.getKey() + "_" + e.getValue());
				if (translated == null && !Algorithms.isEmpty(t)) {
					translated = t;
				}
				String t1 = poiTranslator.getTranslation(e.getKey());
				String t2 = poiTranslator.getTranslation(e.getValue());
				if (separate == null && t1 != null && t2 != null) {
					separate = t1 + ": " + t2.toLowerCase();
				}
				if (single == null && t2 != null && !e.getValue().equals("yes") && !e.getValue().equals("no")) {
					single = t2;
				}
				if (e.getKey().equals("amenity")) {
					translated = t2;
				}
			}
		}
		if (pt != null) {
			return pt.getTranslation();
		}
		if (translated != null) {
			return translated;
		}
		if (otherPt != null) {
			return otherPt.getTranslation();
		}
		if (separate != null) {
			return separate;
		}
		if (single != null) {
			return single;
		}
		return firstTag;
	}

	@NonNull
	@Override
	public String getCommonTypeStr() {
		return getString(R.string.shared_string_location);
	}

	@Override
	public boolean needStreetName() {
		return !renderedObject.isPolygon() && (!getPointDescription().isAddress() || isObjectTypeRecognizedById());
	}

	@Override
	public void addPlainMenuItems(String typeStr, PointDescription pointDescription, LatLon latLon) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			MapPoiTypes poiTypes = mapActivity.getMyApplication().getPoiTypes();
			for (Map.Entry<String, String> entry : renderedObject.getTags().entrySet()) {
				if (entry.getKey().equalsIgnoreCase("maxheight")) {
					AbstractPoiType pt = poiTypes.getAnyPoiAdditionalTypeByKey(entry.getKey());
					if (pt != null) {
						addPlainMenuItem(R.drawable.ic_action_note_dark, null, pt.getTranslation() + ": " + entry.getValue(), false, false, null);
					}
				}
			}
		}
	}

	@Override
	public boolean needTypeStr() {
		return renderedObject.isPolygon() || !isObjectTypeRecognizedById();
	}

	private boolean isStartingWithRTLChar(String s) {
		byte directionality = Character.getDirectionality(s.charAt(0));
		return directionality == Character.DIRECTIONALITY_RIGHT_TO_LEFT
				|| directionality == Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC
				|| directionality == Character.DIRECTIONALITY_RIGHT_TO_LEFT_EMBEDDING
				|| directionality == Character.DIRECTIONALITY_RIGHT_TO_LEFT_OVERRIDE;
	}

	private boolean isObjectTypeRecognizedById() {
		return !Algorithms.isEmpty(searchObjectNameById());
	}

	@NonNull
	private String searchObjectNameById() {
		String content = getActualContent();
		MapActivity mapActivity = getMapActivity();
		if (content != null && mapActivity != null) {
			String[] contentParts = content.split("_");
			for (int i = 0; i < contentParts.length; i++) {
				StringBuilder property = new StringBuilder(POI_PREFIX);
				for (int j = i; j < contentParts.length; j++) {
					property.append("_");
					property.append(contentParts[j]);
				}
				String foundTranslation = AndroidUtils.getStringByProperty(mapActivity, property.toString());
				if (!Algorithms.isEmpty(foundTranslation)) {
					return foundTranslation;
				}
			}
		}
		return "";
	}

	@Nullable
	private String getIconRes() {
		if (getMapActivity() != null && renderedObject.isPolygon()) {
			OsmandApplication app = getMapActivity().getMyApplication();
			MapPoiTypes mapPoiTypes = app.getPoiTypes();
			Map<String, String> t  = renderedObject.getTags();
			for (Map.Entry<String, String> e : t.entrySet()) {
				PoiType pt = mapPoiTypes.getPoiTypeByKey(e.getValue());
				if (pt != null) {
					return pt.getIconKeyName();
				}
			}
		}
		return getActualContent();
	}

	@Nullable
	private String getActualContent() {
		String content = renderedObject.getIconRes();
		if ("osmand_steps".equals(content)) {
			return "highway_steps";
		}
		return content;
	}
}
