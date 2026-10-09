package net.osmand.plus.mapcontextmenu.builders;

import android.view.View;

import net.osmand.data.Amenity;
import net.osmand.data.TransportStop;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.MenuBuilder;

public class TransportStopMenuBuilder extends MenuBuilder {

	private final TransportStop transportStop;

	public TransportStopMenuBuilder(MapActivity mapActivity, TransportStop transportStop) {
		super(mapActivity);
		this.transportStop = transportStop;
	}

	@Override
	public void buildInternal(View view) {
		Amenity amenity = transportStop.getAmenity();
		if (amenity != null) {
			boolean light = isLightContent();
			AmenityMenuBuilder builder = new AmenityMenuBuilder(mapActivity, amenity);
			builder.setLatLon(getLatLon());
			builder.setLight(light);
			builder.setShowNearestPoi(false);
			builder.buildInternal(view);
		}
	}
}