package net.osmand.plus.mapcontextmenu.controllers;

import android.graphics.drawable.Drawable;

import net.osmand.data.PointDescription;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.search.SearchHistoryFragment;
import net.osmand.plus.mapcontextmenu.MenuBuilder;
import net.osmand.plus.mapcontextmenu.MenuController;
import net.osmand.util.Algorithms;

public class PointDescriptionMenuController extends MenuController {

	public PointDescriptionMenuController(OsmandApplication app, MapActivity mapActivity, final PointDescription pointDescription) {
		super(new MenuBuilder(app), pointDescription, mapActivity);
	}

	@Override
	protected int getSupportedMenuStatesPortrait() {
		return MenuState.HEADER_ONLY | MenuState.HALF_SCREEN | MenuState.FULL_SCREEN;
	}

	@Override
	public boolean needTypeStr() {
		String typeName = getPointDescription().getTypeName();
		return (typeName != null && !Algorithms.isEmpty(typeName));
	}

	@Override
	public boolean displayStreetNameInTitle() {
		return true;
	}

	@Override
	public boolean displayDistanceDirection() {
		return true;
	}

	@Override
	public Drawable getLeftIcon() {
		return getIcon(SearchHistoryFragment.getItemIcon(getPointDescription()));
	}

	@Override
	public Drawable getSecondLineIcon() {
		if (needTypeStr()) {
			return getIcon(R.drawable.ic_small_group);
		} else {
			return null;
		}
	}

	@Override
	public String getNameStr() {
		return getPointDescription().getName();
	}

	@Override
	public String getTypeStr() {
		if (needTypeStr()) {
			return getPointDescription().getTypeName();
		} else {
			return "";
		}
	}

	@Override
	public boolean needStreetName() {
		return !getPointDescription().isAddress();
	}
}
