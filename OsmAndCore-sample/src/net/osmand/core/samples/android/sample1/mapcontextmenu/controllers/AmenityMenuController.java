package net.osmand.core.samples.android.sample1.mapcontextmenu.controllers;

import net.osmand.core.samples.android.sample1.MainActivity;
import net.osmand.core.samples.android.sample1.OsmandResources;
import net.osmand.core.samples.android.sample1.data.PointDescription;
import net.osmand.core.samples.android.sample1.mapcontextmenu.MenuBuilder;
import net.osmand.core.samples.android.sample1.mapcontextmenu.MenuController;
import net.osmand.core.samples.android.sample1.mapcontextmenu.builders.AmenityMenuBuilder;
import net.osmand.data.Amenity;
import net.osmand.data.LatLon;
import net.osmand.osm.PoiCategory;
import net.osmand.osm.PoiType;
import net.osmand.util.Algorithms;

public class AmenityMenuController extends MenuController {

	private Amenity amenity;

	public AmenityMenuController(MainActivity mainActivity, PointDescription pointDescription, Amenity amenity) {
		super(new AmenityMenuBuilder(mainActivity, amenity), pointDescription, mainActivity);
		this.amenity = amenity;
	}

	@Override
	protected void setObject(Object object) {
		if (object instanceof Amenity) {
			this.amenity = (Amenity) object;
		}
	}

	@Override
	protected Object getObject() {
		return amenity;
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
		return getLeftIconId(amenity);
	}

	public static int getLeftIconId(Amenity amenity) {
		int id = 0;
		PoiType st = amenity.getType().getPoiTypeByKeyName(amenity.getSubType());
		if (st != null) {
			id = OsmandResources.getBigDrawableId(st.getIconKeyName());
			if (id == 0) {
				id = OsmandResources.getBigDrawableId(st.getOsmTag() + "_" + st.getOsmValue());
			}
		}
		return id;
	}

	@Override
	public boolean displayDistanceDirection() {
		return true;
	}

	@Override
	public String getTypeStr() {
		return getTypeStr(amenity);
	}

	public static String getTypeStr(Amenity amenity) {
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
	public String getCommonTypeStr() {
		PoiCategory pc = amenity.getType();
		return pc.getTranslation();
	}

	@Override
	public void addPlainMenuItems(String typeStr, PointDescription pointDescription, LatLon latLon) {
		addPlainMenuItems(amenity, typeStr, builder);
	}

	public static void addPlainMenuItems(Amenity amenity, String typeStr, MenuBuilder builder) {
		if (!Algorithms.isEmpty(typeStr)) {
			int resId = getLeftIconId(amenity);
			if (resId == 0) {
				PoiCategory pc = amenity.getType();
				resId = OsmandResources.getBigDrawableId(pc.getIconKeyName());
			}
			if (resId == 0) {
				resId = OsmandResources.getDrawableId("ic_action_folder_stroke");
			}
			builder.addPlainMenuItem(resId, typeStr, false, false, null);
		}
	}
}
