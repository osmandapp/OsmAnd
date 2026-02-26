package net.osmand.plus.myplaces.tracks;

import androidx.annotation.NonNull;

import net.osmand.data.LatLon;
import net.osmand.plus.OsmandApplication;

public class PointBitmapDrawer extends MapBitmapDrawer {

	private final LatLon latLon;

	public PointBitmapDrawer(@NonNull OsmandApplication app, @NonNull MapDrawParams params, @NonNull LatLon latLon) {
		super(app, params);
		this.latLon = latLon;
	}

	@Override
	protected void createTileBox() {
		tileBox = app.getOsmandMap().getMapView().getRotatedTileBox();
		tileBox.setLatLonCenter(latLon.getLatitude(), latLon.getLongitude());
		tileBox.setPixelDimensions(params.widthPixels, params.heightPixels);
	}
}
