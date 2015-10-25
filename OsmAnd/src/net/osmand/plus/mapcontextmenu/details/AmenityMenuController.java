package net.osmand.plus.mapcontextmenu.details;

import android.os.Bundle;

import net.osmand.data.Amenity;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.osm.PoiCategory;
import net.osmand.osm.PoiType;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.MenuController;
import net.osmand.plus.render.RenderingIcons;
import net.osmand.util.Algorithms;

public class AmenityMenuController extends MenuController {

	private final Amenity amenity;

	public AmenityMenuController(OsmandApplication app, MapActivity mapActivity, final Amenity amenity) {
		super(new AmenityMenuBuilder(app, amenity), mapActivity);
		this.amenity = amenity;
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
	public boolean needStreetName() {
		if (amenity.getSubType() != null && amenity.getType() != null) {
			PoiType pt = amenity.getType().getPoiTypeByKeyName(amenity.getSubType());
			if (pt != null && pt.getOsmTag() != null && pt.getOsmTag().equals("place")) {
				return false;
			}
		}
		return true;
	}

	@Override
	public int getLeftIconId() {
		String id = null;
		PoiType st = amenity.getType().getPoiTypeByKeyName(amenity.getSubType());
		if (st != null) {
			if (RenderingIcons.containsBigIcon(st.getIconKeyName())) {
				id = st.getIconKeyName();
			} else if (RenderingIcons.containsBigIcon(st.getOsmTag() + "_" + st.getOsmValue())) {
				id = st.getOsmTag() + "_" + st.getOsmValue();
			}
		}
		if (id != null) {
			return RenderingIcons.getBigIconResourceId(id);
		}
		return 0;
	}

	@Override
	public String getTypeStr() {
		PoiCategory pc = amenity.getType();
		PoiType pt = pc.getPoiTypeByKeyName(amenity.getSubType());
		String typeStr = amenity.getSubType();
		if (pt != null) {
			typeStr = pt.getTranslation();
		} else if (typeStr != null) {
			typeStr = Algorithms.capitalizeFirstLetterAndLowercase(typeStr.replace('_', ' '));
		}
		return typeStr;
	}

	@Override
	public void addPlainMenuItems(String typeStr, PointDescription pointDescription, LatLon latLon) {
		if (!Algorithms.isEmpty(typeStr)) {
			addPlainMenuItem(R.drawable.ic_action_info_dark, typeStr);
		}
		addMyLocationToPlainItems(pointDescription, amenity.getLocation());
	}

	@Override
	public String getNameStr() {
		return amenity.getName(getMapActivity().getMyApplication().getSettings().MAP_PREFERRED_LOCALE.get());
	}

	@Override
	public void saveEntityState(Bundle bundle, String key) {
		bundle.putSerializable(key, amenity);
	}
}
