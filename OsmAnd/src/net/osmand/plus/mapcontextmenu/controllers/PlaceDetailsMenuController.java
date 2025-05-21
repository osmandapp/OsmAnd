package net.osmand.plus.mapcontextmenu.controllers;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.NativeLibrary.RenderedObject;
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
	private RenderedObjectMenuController renderedObjectController;

	public PlaceDetailsMenuController(@NonNull MapActivity activity,
			@NonNull PointDescription description,
			@NonNull BaseDetailsObject detailsObject) {
		super(activity, new PlaceDetailsMenuBuilder(activity, detailsObject), description, detailsObject.getSyntheticAmenity());
		this.detailsObject = detailsObject;
		acquireMenuControllers(activity, description);
	}

	protected void acquireMenuControllers(@NonNull MapActivity activity,
			@NonNull PointDescription description) {
		acquireTransportStopController(activity, description);

		List<RenderedObject> renderedObjects = detailsObject.getRenderedObjects();
		if (!Algorithms.isEmpty(renderedObjects)) {
			renderedObjectController = new RenderedObjectMenuController(activity, description, renderedObjects.get(0));
		}
	}

	public int getRightIconId() {
		int iconId = transportStopController != null ? transportStopController.getRightIconId() : 0;
		if (iconId == 0) {
			for (Amenity amenity : detailsObject.getAmenities()) {
				iconId = getRightIconId(getApplication(), amenity);
				if (iconId != 0) {
					break;
				}
			}
		}
		if (iconId == 0) {
			iconId = renderedObjectController != null ? renderedObjectController.getRightIconId() : 0;
		}
		return iconId;
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
