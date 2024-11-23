package net.osmand.plus.helpers;

import static net.osmand.data.PointDescription.POINT_TYPE_POI;

import android.content.Context;

import androidx.annotation.NonNull;

import net.osmand.data.Amenity;
import net.osmand.data.LocationPoint;
import net.osmand.data.PointDescription;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.utils.OsmAndFormatter;

public class AmenityLocationPoint implements LocationPoint {

	private final Amenity amenity;

	public AmenityLocationPoint(@NonNull Amenity amenity) {
		this.amenity = amenity;
	}

	@NonNull
	public Amenity getAmenity() {
		return amenity;
	}

	@Override
	public double getLatitude() {
		return amenity.getLocation().getLatitude();
	}

	@Override
	public double getLongitude() {
		return amenity.getLocation().getLongitude();
	}

	@Override
	public PointDescription getPointDescription(@NonNull Context ctx) {
		OsmandApplication app = (OsmandApplication) ctx.getApplicationContext();
		OsmandSettings settings = app.getSettings();

		String locale = settings.MAP_PREFERRED_LOCALE.get();
		boolean transliterate = settings.MAP_TRANSLITERATE_NAMES.get();
		return new PointDescription(POINT_TYPE_POI, OsmAndFormatter.getPoiStringWithoutType(amenity, locale, transliterate));
	}

	@Override
	public int getColor() {
		return 0;
	}

	@Override
	public boolean isVisible() {
		return true;
	}
}
