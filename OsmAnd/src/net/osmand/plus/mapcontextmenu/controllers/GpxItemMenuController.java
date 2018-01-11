package net.osmand.plus.mapcontextmenu.controllers;

import android.graphics.drawable.Drawable;

import net.osmand.data.PointDescription;
import net.osmand.plus.GpxSelectionHelper.GpxDisplayItem;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.MenuController;
import net.osmand.plus.mapcontextmenu.builders.GpxItemMenuBuilder;

public class GpxItemMenuController extends MenuController {
	private GpxDisplayItem item;

	public GpxItemMenuController(MapActivity mapActivity, PointDescription pointDescription, GpxDisplayItem item) {
		super(new GpxItemMenuBuilder(mapActivity, item), pointDescription, mapActivity);
		this.item = item;
	}

	@Override
	protected void setObject(Object object) {
		if (object instanceof GpxDisplayItem) {
			this.item = (GpxDisplayItem) object;
		}
	}

	@Override
	protected Object getObject() {
		return item;
	}

	@Override
	public String getTypeStr() {
		return getPointDescription().getTypeName();
	}

	@Override
	public String getCommonTypeStr() {
		return getMapActivity().getString(R.string.gpx_selection_segment_title);
	}

	@Override
	public boolean needStreetName() {
		return false;
	}

	@Override
	public Drawable getRightIcon() {
		return getIcon(R.drawable.ic_action_polygom_dark, R.color.osmand_orange);
	}
}
