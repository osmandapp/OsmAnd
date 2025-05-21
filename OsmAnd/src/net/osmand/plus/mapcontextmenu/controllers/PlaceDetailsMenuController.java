package net.osmand.plus.mapcontextmenu.controllers;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.data.Amenity;
import net.osmand.data.BaseDetailsObject;
import net.osmand.data.PointDescription;
import net.osmand.data.TransportStop;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.builders.PlaceDetailsMenuBuilder;
import net.osmand.util.Algorithms;

import java.util.List;

public class PlaceDetailsMenuController extends AmenityMenuController {

	private BaseDetailsObject detailsObject;

	public PlaceDetailsMenuController(@NonNull MapActivity activity,
			@NonNull PointDescription description,
			@NonNull BaseDetailsObject detailsObject) {
		super(activity, new PlaceDetailsMenuBuilder(activity, detailsObject), description, detailsObject.getSyntheticAmenity());
		this.detailsObject = detailsObject;
		acquireTransportStopController(activity, description);
	}

	public int getRightIconId() {
		int iconId = getRightIconId(getApplication(), amenity);
		if (iconId != 0) {
			return iconId;
		}
		if (transportStopController != null) {
			return transportStopController.getRightIconId();
		}
		return 0;
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
	protected void acquireTransportStopController(@NonNull MapActivity activity,
			@NonNull PointDescription description) {
		if (detailsObject != null) {
			transportStopController = getTransportStopController(activity, description);
		}
		if (transportStopController != null) {
			transportStopController.processRoutes();
		}
	}

	@Nullable
	private TransportStopController getTransportStopController(@NonNull MapActivity activity,
			@NonNull PointDescription description) {
		for (Amenity amenity : detailsObject.getAmenities()) {
			TransportStopController controller = acquireTransportStopController(amenity, activity, description);
			if (controller != null) {
				return controller;
			}
		}
		List<TransportStop> stops = detailsObject.getTransportStops();
		if (!Algorithms.isEmpty(stops)) {
			return new TransportStopController(activity, description, stops.get(0));
		}
		return null;
	}
}
