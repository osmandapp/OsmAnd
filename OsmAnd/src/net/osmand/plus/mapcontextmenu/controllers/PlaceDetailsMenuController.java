package net.osmand.plus.mapcontextmenu.controllers;

import androidx.annotation.NonNull;

import net.osmand.data.Amenity;
import net.osmand.data.BaseDetailsObject;
import net.osmand.data.PointDescription;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.builders.PlaceDetailsMenuBuilder;

public class PlaceDetailsMenuController extends AmenityMenuController {

	private BaseDetailsObject detailsObject;

	public PlaceDetailsMenuController(@NonNull MapActivity activity,
			@NonNull PointDescription description,
			@NonNull BaseDetailsObject detailsObject) {
		super(activity, new PlaceDetailsMenuBuilder(activity, detailsObject), description, detailsObject.getSyntheticAmenity());
		this.detailsObject = detailsObject;
		acquireTransportStopController(activity, description);
	}

	@Override
	protected void setObject(Object object) {
		if (object instanceof BaseDetailsObject) {
			detailsObject = (BaseDetailsObject) object;
			super.setObject(detailsObject.getSyntheticAmenity());
		} else {
			super.setObject(object);
		}
	}

	@Override
	protected void acquireTransportStopController(@NonNull MapActivity mapActivity,
			@NonNull PointDescription pointDescription) {
		if (detailsObject != null) {
			for (Amenity amenity : detailsObject.getAmenities()) {
				transportStopController = acquireTransportStopController(amenity, mapActivity, pointDescription);
				if (transportStopController != null) {
					transportStopController.processRoutes();
					break;
				}
			}
		}
	}
}
