package net.osmand.plus.mapcontextmenu.controllers;

import net.osmand.NativeLibrary.RenderedObject;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.MenuBuilder;
import net.osmand.plus.mapcontextmenu.MenuController;
import net.osmand.plus.mapillary.MapillaryPlugin;
import net.osmand.plus.osmedit.OsmEditingPlugin;
import net.osmand.plus.render.RenderingIcons;
import net.osmand.util.Algorithms;

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
	public int getLeftIconId() {
		if (renderedObject.getIconRes() != null && RenderingIcons.containsBigIcon(renderedObject.getIconRes())) {
			return RenderingIcons.getBigIconResourceId(renderedObject.getIconRes());
		} else {
			return R.drawable.ic_type_address;
		}
	}

	@Override
	public String getNameStr() {
		if (!Algorithms.isEmpty(renderedObject.getName())) {
			return renderedObject.getName();
		} else if (renderedObject.getTags().size() > 0) {
			String lang = getMapActivity().getMyApplication().getSettings().MAP_PREFERRED_LOCALE.get().toLowerCase();
			String name = "";
			if (!Algorithms.isEmpty(lang)) {
				name = renderedObject.getTags().get("name:" + lang);
			}
			if (Algorithms.isEmpty(name)) {
				name = renderedObject.getTags().get("name");
			}
			return name;
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
		boolean osmEditingEnabled = OsmandPlugin.getEnabledPlugin(OsmEditingPlugin.class) != null;
		if (osmEditingEnabled && renderedObject.getId() != null
				&& renderedObject.getId() > 0 && 
				(renderedObject.getId() % 2 == 1 || (renderedObject.getId() >> 7) < Integer.MAX_VALUE)) {
			String link;
			if ((renderedObject.getId() >> 6) % 2 == 1) {
				link = "http://www.openstreetmap.org/node/";
			} else {
				link = "http://www.openstreetmap.org/way/";
			}
			addPlainMenuItem(R.drawable.ic_action_info_dark, link + (renderedObject.getId() >> 7), true, true, null);
		}
		addMyLocationToPlainItems(latLon);
	}
}
