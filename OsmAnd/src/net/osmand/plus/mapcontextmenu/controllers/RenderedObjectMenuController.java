package net.osmand.plus.mapcontextmenu.controllers;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.NativeLibrary.RenderedObject;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.osm.AbstractPoiType;
import net.osmand.osm.MapPoiTypes;
import net.osmand.osm.PoiCategory;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.MenuBuilder;
import net.osmand.plus.mapcontextmenu.MenuController;
import net.osmand.plus.render.RenderingIcons;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.util.Algorithms;

import java.util.Map;

public class RenderedObjectMenuController extends MenuController {

	private static final String POI_PREFIX = "poi";

	private RenderedObject renderedObject;
	private PoiCategory poiCategory;
	private AbstractPoiType poiType;

	public RenderedObjectMenuController(@NonNull MapActivity mapActivity,
	                                    @NonNull PointDescription pointDescription,
	                                    @NonNull RenderedObject renderedObject) {
		super(new MenuBuilder(mapActivity), pointDescription, mapActivity);
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
		initTypeAndCategoryIfNeeded();
	}

	private void initTypeAndCategoryIfNeeded() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity == null || !renderedObject.isPolygon()) {
			poiCategory = null;
			poiType = null;
			return;
		}
		OsmandApplication app = mapActivity.getMyApplication();
		MapPoiTypes mapPoiTypes = app.getPoiTypes();
		for (Map.Entry<String, String> entry : renderedObject.getTags().entrySet()) {
			String key = entry.getKey();
			String value = entry.getValue();
			PoiCategory foundCategory = mapPoiTypes.getPoiCategoryByName(key);
			if (poiCategory == null || !mapPoiTypes.isOtherCategory(foundCategory)) {
				poiCategory = foundCategory;
			}
			AbstractPoiType foundType = foundCategory.getPoiTypeByKeyName(value);
			if (foundType == null) {
				foundType = mapPoiTypes.getAnyPoiTypeByKey(key + "_" + value);
			}
			if (foundType != null) {
				poiType = foundType;
				break;
			}
		}
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
		String lang = getPreferredMapLang().toLowerCase();
		String nameTranslation = renderedObject.getName(lang);

		if (!Algorithms.isEmpty(nameTranslation) && !isStartingWithRTLChar(nameTranslation)) {
			return nameTranslation;
		} else if (renderedObject.getTags().size() > 0) {
			nameTranslation = "";
			if (!Algorithms.isEmpty(lang)) {
				nameTranslation = renderedObject.getTagValue("name:" + lang);
			}
			if (Algorithms.isEmpty(nameTranslation)) {
				nameTranslation = renderedObject.getTagValue("name");
			}
		}
		if (!Algorithms.isEmpty(nameTranslation)) {
			return nameTranslation;
		}
		if (renderedObject.isPolygon()) {
			return getString(R.string.shared_string_undefined);
		}
		return searchObjectNameById();
	}

	@NonNull
	@Override
	public String getTypeStr() {
		if (renderedObject.isPolygon()) {
			return poiType != null ? poiType.getTranslation()
					: poiCategory != null ? poiCategory.getTranslation()
					: getString(R.string.shared_string_undefined);
		}
		return super.getTypeStr();
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

	private boolean hasRelatedPoiTypeOrCategory() {
		return poiCategory != null || poiType != null;
	}

	@Nullable
	private String getIconRes() {
		if (hasRelatedPoiTypeOrCategory()) {
			return poiType != null ? poiType.getIconKeyName() : poiCategory.getIconKeyName();
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
