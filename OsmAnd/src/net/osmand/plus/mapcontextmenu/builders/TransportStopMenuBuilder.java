package net.osmand.plus.mapcontextmenu.builders;

import android.view.View;

import net.osmand.data.Amenity;
import net.osmand.data.TransportStop;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.MenuBuilder;

public class TransportStopMenuBuilder extends MenuBuilder {

	private final TransportStop transportStop;
	private Amenity amenity;

	public TransportStopMenuBuilder(MapActivity mapActivity, final TransportStop transportStop) {
		super(mapActivity);
		this.transportStop = transportStop;
		acquireOriginObject();
	}

	private void acquireOriginObject() {
		amenity = transportStop.getAmenity();
	}

	@Override
	public void buildInternal(View view) {
		if (amenity != null) {
			AmenityMenuBuilder builder = new AmenityMenuBuilder(mapActivity, amenity);
			builder.setLatLon(getLatLon());
			builder.setLight(light);
			builder.buildInternal(view);
		}
	}
}