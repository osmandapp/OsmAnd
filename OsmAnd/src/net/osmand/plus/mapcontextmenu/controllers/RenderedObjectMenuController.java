package net.osmand.plus.mapcontextmenu.controllers;

import net.osmand.NativeLibrary.RenderedObject;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.osm.AbstractPoiType;
import net.osmand.osm.MapPoiTypes;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.MenuBuilder;
import net.osmand.plus.mapcontextmenu.MenuController;
import net.osmand.plus.osmedit.OsmEditingPlugin;
import net.osmand.plus.render.RenderingIcons;
import net.osmand.util.Algorithms;

import java.util.Map;

public class RenderedObjectMenuController extends MenuController {

	private RenderedObject renderedObject;

	public RenderedObjectMenuController(MapActivity mapActivity, PointDescription pointDescription, final RenderedObject renderedObject) {
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
			return R.drawable.ic_type_address;
		}
	}

	@Override
	public String getNameStr() {
		if (!Algorithms.isEmpty(renderedObject.getName()) && !isStartingWithRTLChar(renderedObject.getName())) {
			return renderedObject.getName();
		} else if (renderedObject.getTags().size() > 0) {
			String lang = getPreferredMapLang().toLowerCase();
			String name = "";
			if (!Algorithms.isEmpty(lang)) {
				name = renderedObject.getTags().get("name:" + lang);
			}
			if (Algorithms.isEmpty(name)) {
				name = renderedObject.getTags().get("name");
			}
			return name;
		} else if (!Algorithms.isEmpty(renderedObject.getName())) {
			return renderedObject.getName();
		}
		return "";
	}

	@Override
	public String getCommonTypeStr() {
		return getMapActivity().getString(R.string.shared_string_location);
	}

	@Override
	public boolean needStreetName() {
		return !getPointDescription().isAddress();
	}

	@Override
	public void addPlainMenuItems(String typeStr, PointDescription pointDescription, final LatLon latLon) {

		MapPoiTypes poiTypes = getMapActivity().getMyApplication().getPoiTypes();
		for (Map.Entry<String, String> entry : renderedObject.getTags().entrySet()) {
			if (entry.getKey().equalsIgnoreCase("maxheight")) {
				AbstractPoiType pt = poiTypes.getAnyPoiAdditionalTypeByKey(entry.getKey());
				if (pt != null) {
					addPlainMenuItem(R.drawable.ic_action_note_dark, null, pt.getTranslation() + ": " + entry.getValue(), false, false, null);
				}
			}
		}

		boolean osmEditingEnabled = OsmandPlugin.getEnabledPlugin(OsmEditingPlugin.class) != null;
		if (osmEditingEnabled && renderedObject.getId() != null
				&& renderedObject.getId() > 0 && 
				(renderedObject.getId() % 2 == 1 || (renderedObject.getId() >> 7) < Integer.MAX_VALUE)) {
			String link;
			if ((renderedObject.getId() >> 6) % 2 == 1) {
				link = "https://www.openstreetmap.org/node/";
			} else {
				link = "https://www.openstreetmap.org/way/";
			}
			addPlainMenuItem(R.drawable.ic_action_info_dark, null, link + (renderedObject.getId() >> 7), true, true, null);
		}
		addMyLocationToPlainItems(latLon);
	}

	private boolean isStartingWithRTLChar(String s) {
		byte directionality = Character.getDirectionality(s.charAt(0));
		return directionality == Character.DIRECTIONALITY_RIGHT_TO_LEFT
				|| directionality == Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC
				|| directionality == Character.DIRECTIONALITY_RIGHT_TO_LEFT_EMBEDDING
				|| directionality == Character.DIRECTIONALITY_RIGHT_TO_LEFT_OVERRIDE;
	}

}
