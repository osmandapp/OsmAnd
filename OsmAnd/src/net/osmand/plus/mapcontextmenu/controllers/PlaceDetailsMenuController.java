package net.osmand.plus.mapcontextmenu.controllers;

import androidx.annotation.NonNull;

import net.osmand.data.PointDescription;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.builders.PlaceDetailsMenuBuilder;
import net.osmand.plus.views.layers.PlaceDetailsObject;

public class PlaceDetailsMenuController extends AmenityMenuController {

	private PlaceDetailsObject detailsObject;

	public PlaceDetailsMenuController(@NonNull MapActivity activity,
			@NonNull PointDescription description,
			@NonNull PlaceDetailsObject detailsObject) {
		super(activity, new PlaceDetailsMenuBuilder(activity, detailsObject), description, detailsObject.getSyntheticAmenity());
		this.detailsObject = detailsObject;
	}

	@Override
	protected void setObject(Object object) {
		if (object instanceof PlaceDetailsObject) {
			detailsObject = (PlaceDetailsObject) object;
			super.setObject(detailsObject.getSyntheticAmenity());
		} else {
			super.setObject(object);
		}
	}
}
