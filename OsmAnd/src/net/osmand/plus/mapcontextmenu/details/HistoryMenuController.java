package net.osmand.plus.mapcontextmenu.details;

import android.graphics.drawable.Drawable;
import android.os.Bundle;

import net.osmand.data.PointDescription;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.search.SearchHistoryFragment;
import net.osmand.plus.helpers.SearchHistoryHelper.HistoryEntry;
import net.osmand.plus.mapcontextmenu.MenuController;

public class HistoryMenuController extends MenuController {

	private HistoryEntry entry;

	public HistoryMenuController(OsmandApplication app, MapActivity mapActivity, final HistoryEntry entry) {
		super(new HistoryMenuBuilder(app, entry), mapActivity);
		this.entry = entry;
	}

	@Override
	protected int getInitialMenuStatePortrait() {
		return MenuState.HEADER_ONLY;
	}

	@Override
	protected int getSupportedMenuStatesPortrait() {
		return MenuState.HEADER_ONLY | MenuState.HALF_SCREEN | MenuState.FULL_SCREEN;
	}

	@Override
	public boolean needTypeStr() {
		String typeName = entry.getName().getTypeName();
		return (typeName != null && !typeName.isEmpty());
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
	public String getNameStr() {
		return entry.getName().getSimpleName(getMapActivity(), false);
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

	@Override
	public void addPlainMenuItems(String typeStr, PointDescription pointDescription) {
		if (pointDescription != null) {
			addPlainMenuItem(R.drawable.map_my_location, PointDescription.getLocationName(getMapActivity(),
					entry.getLat(), entry.getLon(), true).replaceAll("\n", ""));
		}
	}

	@Override
	public void saveEntityState(Bundle bundle, String key) {
		bundle.putSerializable(key, entry);
	}
}
