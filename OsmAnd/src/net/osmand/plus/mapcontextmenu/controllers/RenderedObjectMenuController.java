package net.osmand.plus.mapcontextmenu.controllers;

import static net.osmand.core.android.MapRendererContext.RELATED_CATEGORY;
import static net.osmand.core.android.MapRendererContext.RELATED_TYPE;

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
		String iconRes = getActualContent();
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
			return nameTranslation;
		} else if (!Algorithms.isEmpty(nameTranslation)) {
			return nameTranslation;
		}
		return searchObjectNameById();
	}

	@NonNull
	@Override
	public String getTypeStr() {
		Object relatedType = renderedObject.getExtension(RELATED_TYPE);
		if (relatedType instanceof AbstractPoiType poiType) {
			return poiType.getTranslation();
		}
		MapActivity activity = getMapActivity();
		Object relatedCategory = renderedObject.getExtension(RELATED_CATEGORY);
		if (activity != null && relatedCategory instanceof PoiCategory poiCategory) {
			OsmandApplication app = activity.getMyApplication();
			MapPoiTypes mapPoiTypes = app.getPoiTypes();
			return mapPoiTypes.isOtherCategory(poiCategory)
					? app.getString(R.string.shared_string_undefined)
					: poiCategory.getTranslation();
		}
		return super.getTypeStr();
	}

	@NonNull
	@Override
	public String getCommonTypeStr() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			return mapActivity.getString(R.string.shared_string_location);
		} else {
			return "";
		}
	}

	@Override
	public boolean needStreetName() {
		return !hasRelatedPoiTypeOrCategory() && (!getPointDescription().isAddress() || isObjectTypeRecognizedById());
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
		return hasRelatedPoiTypeOrCategory() || !isObjectTypeRecognizedById();
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

	private boolean hasRelatedPoiTypeOrCategory() {
		return renderedObject.getExtension(RELATED_CATEGORY) != null
				|| renderedObject.getExtension(RELATED_TYPE) != null;
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
	private String getActualContent() {
		String content = renderedObject.getIconRes();
		if ("osmand_steps".equals(content)) {
			return "highway_steps";
		}
		return content;
	}
}
