package net.osmand.plus.mapcontextmenu.controllers;

import android.graphics.drawable.Drawable;

import net.osmand.data.PointDescription;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.search.SearchHistoryFragment;
import net.osmand.plus.helpers.SearchHistoryHelper.HistoryEntry;
import net.osmand.plus.mapcontextmenu.MenuBuilder;
import net.osmand.plus.mapcontextmenu.MenuController;

public class HistoryMenuController extends MenuController {

	private HistoryEntry entry;

	public HistoryMenuController(OsmandApplication app, MapActivity mapActivity, PointDescription pointDescription, final HistoryEntry entry) {
		super(new MenuBuilder(app), pointDescription, mapActivity);
		this.entry = entry;
	}

	@Override
	protected int getSupportedMenuStatesPortrait() {
		return MenuState.HEADER_ONLY | MenuState.HALF_SCREEN;
	}

	@Override
	public boolean needTypeStr() {
		String typeName = entry.getName().getTypeName();
		return (typeName != null && !typeName.isEmpty());
	}

	@Override
	public boolean displayDistanceDirection() {
		return true;
	}

	@Override
	public Drawable getLeftIcon() {
		return getIcon(SearchHistoryFragment.getItemIcon(entry.getName()));
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
	public String getTypeStr() {
		if (needTypeStr()) {
			return entry.getName().getTypeName();
		} else {
			return "";
		}
	}

	@Override
	public boolean needStreetName() {
		return !entry.getName().isAddress();
	}
}
