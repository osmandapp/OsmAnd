package net.osmand.plus.mapcontextmenu.details;

import android.graphics.drawable.Drawable;
import android.os.Bundle;

import net.osmand.data.PointDescription;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.search.SearchHistoryFragment;
import net.osmand.plus.mapcontextmenu.MenuBuilder;
import net.osmand.plus.mapcontextmenu.MenuController;

public class PointDescriptionMenuController extends MenuController {

	private PointDescription pointDescription;

	public PointDescriptionMenuController(OsmandApplication app, MapActivity mapActivity, final PointDescription pointDescription) {
		super(new MenuBuilder(app), mapActivity);
		this.pointDescription = pointDescription;
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
		String typeName = pointDescription.getTypeName();
		return (typeName != null && !typeName.isEmpty());
	}

	@Override
	public Drawable getLeftIcon() {
		return getIcon(SearchHistoryFragment.getItemIcon(pointDescription));
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
		return pointDescription.getSimpleName(getMapActivity(), false);
	}

	@Override
	public String getTypeStr() {
		if (needTypeStr()) {
			return pointDescription.getTypeName();
		} else {
			return "";
		}
	}

	@Override
	public boolean needStreetName() {
		return !pointDescription.isAddress();
	}

	@Override
	public void saveEntityState(Bundle bundle, String key) {
	}
}
