package net.osmand.plus.mapillary;

import android.graphics.drawable.Drawable;

import net.osmand.data.PointDescription;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.MenuBuilder;
import net.osmand.plus.mapcontextmenu.MenuController;
import net.osmand.util.Algorithms;

public class MapillaryMenuController extends MenuController {

	private MapillaryImage image;

	public MapillaryMenuController(MapActivity mapActivity, PointDescription pointDescription, MapillaryImage image) {
		super(new MenuBuilder(mapActivity), pointDescription, mapActivity);
		this.image = image;
	}

	@Override
	protected void setObject(Object object) {
		if (object instanceof MapillaryImage) {
			this.image = (MapillaryImage) object;
		}
	}

	@Override
	public boolean setActive(boolean active) {
		if (image != null && getMenuType() == MenuType.STANDARD) {
			MapillaryImageDialog.show(getMapActivity(), image.getLatitude(), image.getLongitude(),
					image.getKey(), image.getSKey(), image.getCa(), getMapActivity().getMyApplication().getString(R.string.mapillary), null);
			return false;
		} else {
			return super.setActive(active);
		}
	}

	@Override
	protected Object getObject() {
		return image;
	}

	public MapillaryImage getMapillaryImage() {
		return image;
	}

	@Override
	public boolean needTypeStr() {
		return !Algorithms.isEmpty(getNameStr());
	}

	@Override
	public boolean displayDistanceDirection() {
		return true;
	}

	@Override
	public Drawable getLeftIcon() {
		return getIcon(R.drawable.ic_action_mapillary, R.color.mapillary_color);
	}

	@Override
	public boolean needStreetName() {
		return true;
	}
}