package net.osmand.plus.plugins.mapillary;

import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;

import net.osmand.data.PointDescription;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.MenuBuilder;
import net.osmand.plus.mapcontextmenu.MenuController;
import net.osmand.util.Algorithms;

public class MapillaryMenuController extends MenuController {

	private MapillaryImage image;

	public MapillaryMenuController(@NonNull MapActivity mapActivity,
								   @NonNull PointDescription pointDescription,
								   @NonNull MapillaryImage image) {
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
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null && image != null && getMenuType() == MenuType.STANDARD) {
			MapillaryImageDialog.show(mapActivity, image.getLatitude(), image.getLongitude(),
					image.getImageId(), image.getSKey(), image.getCompassAngle(), mapActivity.getApp().getString(R.string.mapillary), null);
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
	public Drawable getRightIcon() {
		return getIcon(R.drawable.ic_action_mapillary, R.color.mapillary_color);
	}

	@Override
	public boolean needStreetName() {
		return true;
	}
}