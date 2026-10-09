package net.osmand.plus.mapcontextmenu.controllers;

import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;

import net.osmand.data.PointDescription;
import net.osmand.plus.track.helpers.GpxDisplayItem;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.MenuController;
import net.osmand.plus.mapcontextmenu.builders.GpxItemMenuBuilder;

public class GpxItemMenuController extends MenuController {
	private GpxDisplayItem item;

	public GpxItemMenuController(@NonNull MapActivity mapActivity, @NonNull PointDescription pointDescription, @NonNull GpxDisplayItem item) {
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

	@NonNull
	@Override
	public String getTypeStr() {
		return getPointDescription().getTypeName();
	}

	@NonNull
	@Override
	public String getCommonTypeStr() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			return mapActivity.getString(R.string.gpx_selection_segment_title);
		} else {
			return "";
		}
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
