package net.osmand.plus.mapcontextmenu.controllers;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.NativeLibrary.RenderedObject;
import net.osmand.data.Amenity;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.osm.AbstractPoiType;
import net.osmand.osm.MapPoiTypes;
import net.osmand.osm.PoiType;
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

	private String nameStr = null;

	@Nullable
	private final MapPoiTypes mapPoiTypes;
	@Nullable
	private final MapPoiTypes.PoiTranslator poiTranslator;

	public RenderedObjectMenuController(@NonNull MapActivity mapActivity,
	                                    @NonNull PointDescription pointDescription,
	                                    @NonNull RenderedObject renderedObject) {
		super(new RenderedObjectMenuBuilder(mapActivity, renderedObject), pointDescription, mapActivity);
		builder.setShowNearestWiki(true);
		setRenderedObject(renderedObject);
		mapPoiTypes = mapActivity.getApp().getPoiTypes();
		poiTranslator = mapPoiTypes != null ? mapPoiTypes.getPoiTranslator() : null;
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
		} else if (iconRes != null && RenderingIcons.containsSmallIcon(iconRes)) {
			return RenderingIcons.getResId(iconRes);
		} else {
			return R.drawable.ic_action_street_name;
		}
	}

	@NonNull
	@Override
	public String getNameStr() {
		if (!Algorithms.isEmpty(nameStr)) {
			return nameStr; // cached
		}

		String lang = getPreferredMapLangLC();
		boolean transliterate = isTransliterateNames();
		nameStr = renderedObject.getName(lang, transliterate);

		if (!Algorithms.isEmpty(nameStr) && !isStartingWithRTLChar(nameStr)) {
			return nameStr;
		} else if (!renderedObject.getTags().isEmpty()) {
			if (!Algorithms.isEmpty(lang)) {
				nameStr = renderedObject.getTagValue("name:" + lang);
			}
			if (Algorithms.isEmpty(nameStr)) {
				nameStr = renderedObject.getTagValue("name");
			}
		}

		if (Algorithms.isEmpty(nameStr) && builder instanceof RenderedObjectMenuBuilder that) {
			nameStr = searchObjectNameByAmenityTags(that.getAmenity());
		}

		if (Algorithms.isEmpty(nameStr)) {
			nameStr = searchObjectNameByIconRes();
		}

		return nameStr != null ? nameStr : "";
	}

	@Nullable
	private String searchObjectNameByAmenityTags(@NonNull Amenity amenity) {
		if (poiTranslator == null) {
			return null;
		}

		String translation = poiTranslator.getTranslation(amenity.getSubType());

		for (String key : amenity.getAdditionalInfoKeys()) {
			String translationKey = key.replace("osmand_", "").replace(":", "_");
			String value = amenity.getAdditionalInfo(key);
			if (!Algorithms.isEmpty(translation)) {
				break;
			}
			// nameStr uses (key_value - value - key) sequence
			if (Algorithms.isEmpty(translation)) {
				translation = poiTranslator.getTranslation(translationKey + "_" + value);
			}
			if (Algorithms.isEmpty(translation)) {
				translation = poiTranslator.getTranslation(value);
			}
			if (Algorithms.isEmpty(translation)) {
				translation = poiTranslator.getTranslation(translationKey);
			}
		}

		return translation;
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
		if (poiTranslator == null || mapPoiTypes == null) {
			return "";
		}
		PoiType pt = null;
		PoiType otherPt = null;
		String translated = null;
		String firstTag = "";
		String separate = null;
		String single = null;
		for (Map.Entry<String, String> e : renderedObject.getTags().entrySet()) {
			String key = e.getKey();
			String value = e.getValue();
			String translationKey = key.replace("osmand_", "").replace(":", "_");
			if (key.startsWith("name")) {
				continue;
			}
			if (Algorithms.isEmpty(value) && otherPt == null) {
				otherPt = mapPoiTypes.getPoiTypeByKey(key);
			}
			pt = mapPoiTypes.getPoiTypeByKey(key + "_" + value);
			if (pt == null && key.startsWith("osmand_")) {
				pt = mapPoiTypes.getPoiTypeByKey(key.replace("osmand_", "") + "_" + value);
			}
			if (pt != null) {
				break;
			}
			firstTag = firstTag.isEmpty() ? key + ": " + value : firstTag;
			if (!Algorithms.isEmpty(value)) {
				// typeStr uses (key - key_value - value) sequence
				String t = poiTranslator.getTranslation(translationKey);
				if (Algorithms.isEmpty(t)) {
					t = poiTranslator.getTranslation(translationKey + "_" + value);
				}
				if (Algorithms.isEmpty(t)) {
					t = poiTranslator.getTranslation(value);
				}
				if (translated == null && !Algorithms.isEmpty(t)) {
					translated = t;
				}
				String t1 = poiTranslator.getTranslation(key);
				String t2 = poiTranslator.getTranslation(value);
				if (separate == null && t1 != null && t2 != null) {
					separate = t1 + ": " + t2.toLowerCase();
				}
				if (single == null && t2 != null && !value.equals("yes") && !value.equals("no")) {
					single = t2;
				}
				if (key.equals("amenity")) {
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
		return !renderedObject.isPolygon() && (!getPointDescription().isAddress() || isObjectTypeRecognized());
	}

	@Override
	public void addPlainMenuItems(String typeStr, PointDescription pointDescription, LatLon latLon) {
		if (mapPoiTypes != null) {
			for (Map.Entry<String, String> entry : renderedObject.getTags().entrySet()) {
				if (entry.getKey().equalsIgnoreCase("maxheight")) {
					AbstractPoiType pt = mapPoiTypes.getAnyPoiAdditionalTypeByKey(entry.getKey());
					if (pt != null) {
						addPlainMenuItem(R.drawable.ic_action_note_dark, null, pt.getTranslation() + ": " + entry.getValue(), false, false, null);
					}
				}
			}
		}
	}

	@Override
	public boolean needTypeStr() {
		return renderedObject.isPolygon() || !isObjectTypeRecognized();
	}

	private boolean isStartingWithRTLChar(String s) {
		byte directionality = Character.getDirectionality(s.charAt(0));
		return directionality == Character.DIRECTIONALITY_RIGHT_TO_LEFT
				|| directionality == Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC
				|| directionality == Character.DIRECTIONALITY_RIGHT_TO_LEFT_EMBEDDING
				|| directionality == Character.DIRECTIONALITY_RIGHT_TO_LEFT_OVERRIDE;
	}

	private boolean isObjectTypeRecognized() {
		return !Algorithms.isEmpty(getNameStr());
	}

	@Nullable
	private String searchObjectNameByIconRes() {
		String content = getActualContentFromIconRes();
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
		return null;
	}

	@Nullable
	private String getIconRes() {
		if (mapPoiTypes != null && renderedObject.isPolygon()) {
			Map<String, String> t  = renderedObject.getTags();
			for (Map.Entry<String, String> e : t.entrySet()) {
				PoiType pt = mapPoiTypes.getPoiTypeByKey(e.getValue());
				if (pt != null) {
					return pt.getIconKeyName();
				}
			}
		}
		return getActualContentFromIconRes();
	}

	@Nullable
	private String getActualContentFromIconRes() {
		String content = renderedObject.getIconRes();
		if ("osmand_steps".equals(content)) {
			return "highway_steps";
		}
		return content;
	}
}
