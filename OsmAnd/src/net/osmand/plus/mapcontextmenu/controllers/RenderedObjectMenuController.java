package net.osmand.plus.mapcontextmenu.controllers;

import androidx.annotation.NonNull;

import net.osmand.NativeLibrary.RenderedObject;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.osm.AbstractPoiType;
import net.osmand.osm.MapPoiTypes;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.MenuBuilder;
import net.osmand.plus.mapcontextmenu.MenuController;
import net.osmand.plus.render.RenderingIcons;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.util.Algorithms;

import java.util.Map;

public class RenderedObjectMenuController extends MenuController {

	private RenderedObject renderedObject;

	public RenderedObjectMenuController(@NonNull MapActivity mapActivity,
	                                    @NonNull PointDescription pointDescription,
	                                    @NonNull RenderedObject renderedObject) {
		super(new MenuBuilder(mapActivity), pointDescription, mapActivity);
		builder.setShowNearestWiki(true);
		this.renderedObject = renderedObject;
	}

	@Override
	protected void setObject(Object object) {
		if (object instanceof RenderedObject) {
			this.renderedObject = (RenderedObject) object;
		}
	}

	@Override
	protected Object getObject() {
		return renderedObject;
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
		if (renderedObject.getIconRes() != null && RenderingIcons.containsBigIcon(renderedObject.getIconRes())) {
			return RenderingIcons.getBigIconResourceId(renderedObject.getIconRes());
		} else {
			return R.drawable.ic_action_street_name;
		}
	}

	@NonNull
	@Override
	public String getNameStr() {
		if (!Algorithms.isEmpty(renderedObject.getName()) && !isStartingWithRTLChar(renderedObject.getName())) {
			return renderedObject.getName();
		} else if (renderedObject.getTags().size() > 0) {
			String lang = getPreferredMapLang().toLowerCase();
			String name = "";
			if (!Algorithms.isEmpty(lang)) {
				name = renderedObject.getTagValue("name:" + lang);
			}
			if (Algorithms.isEmpty(name)) {
				name = renderedObject.getTagValue("name");
			}
			return name;
		} else if (!Algorithms.isEmpty(renderedObject.getName())) {
			return renderedObject.getName();
		}
		return getKnownObjectName();
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
		return !getPointDescription().isAddress() || isRecognizedObjectType();
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
		return !isRecognizedObjectType();
	}

	private boolean isStartingWithRTLChar(String s) {
		byte directionality = Character.getDirectionality(s.charAt(0));
		return directionality == Character.DIRECTIONALITY_RIGHT_TO_LEFT
				|| directionality == Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC
				|| directionality == Character.DIRECTIONALITY_RIGHT_TO_LEFT_EMBEDDING
				|| directionality == Character.DIRECTIONALITY_RIGHT_TO_LEFT_OVERRIDE;
	}

	private boolean isRecognizedObjectType() {
		return !Algorithms.isEmpty(getKnownObjectName());
	}

	@NonNull
	private String getKnownObjectName() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			String content = renderedObject.getIconRes();
			mapActivity.getMyApplication().showToastMessage(content); // todo delete
			return AndroidUtils.getRenderingStringPropertyName(mapActivity, content, "");
		}
		return "";
	}
}
