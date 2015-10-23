package net.osmand.plus.mapcontextmenu.details;

import android.graphics.drawable.Drawable;
import android.os.Bundle;

import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.search.SearchHistoryFragment;
import net.osmand.plus.mapcontextmenu.MenuController;

public class PointDescriptionMenuController extends MenuController {

	private PointDescription pointDescription;
	private LatLon latLon;

	public PointDescriptionMenuController(OsmandApplication app, MapActivity mapActivity, final PointDescription pointDescription, LatLon latLon) {
		super(new PointDescriptionMenuBuilder(app, pointDescription), mapActivity);
		this.pointDescription = pointDescription;
		this.latLon = latLon;
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
	public void addPlainMenuItems(String typeStr, PointDescription pointDescription) {
		if (pointDescription != null) {
			addPlainMenuItem(R.drawable.map_my_location, PointDescription.getLocationName(getMapActivity(),
					latLon.getLatitude(), latLon.getLongitude(), true).replaceAll("\n", ""));
		}
	}

	@Override
	public void saveEntityState(Bundle bundle, String key) {
	}
}
