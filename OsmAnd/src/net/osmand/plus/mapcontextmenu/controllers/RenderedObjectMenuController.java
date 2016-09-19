package net.osmand.plus.mapcontextmenu.controllers;

import net.osmand.NativeLibrary.RenderedObject;
import net.osmand.data.PointDescription;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.MenuBuilder;
import net.osmand.plus.mapcontextmenu.MenuController;
import net.osmand.plus.render.RenderingIcons;
import net.osmand.util.Algorithms;

public class RenderedObjectMenuController extends MenuController {

	private RenderedObject renderedObject;

	public RenderedObjectMenuController(OsmandApplication app, MapActivity mapActivity, PointDescription pointDescription, final RenderedObject renderedObject) {
		super(new MenuBuilder(app), pointDescription, mapActivity);
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
	protected int getSupportedMenuStatesPortrait() {
		return MenuState.HEADER_ONLY | MenuState.HALF_SCREEN | MenuState.FULL_SCREEN;
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
			String name;
			if (!Algorithms.isEmpty(lang)) {
				name = renderedObject.getTags().get("name:" + lang);
			} else {
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
}
