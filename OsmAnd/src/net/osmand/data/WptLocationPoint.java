package net.osmand.data;

import android.content.Context;

import androidx.annotation.NonNull;

import net.osmand.gpx.GPXUtilities;

public class WptLocationPoint implements LocationPoint {

	GPXUtilities.WptPt pt;

	public WptLocationPoint(GPXUtilities.WptPt p) {
		this.pt = p;
	}

	@Override
	public double getLatitude() {
		return pt.lat;
	}

	@Override
	public double getLongitude() {
		return pt.lon;
	}

	@Override
	public int getColor() {
		return pt.getColor();
	}

	@Override
	public boolean isVisible() {
		return pt.isVisible();
	}

	public PointDescription getPointDescription(@NonNull Context ctx) {
		return new PointDescription(PointDescription.POINT_TYPE_WPT, pt.name);
	}

	public GPXUtilities.WptPt getPt() {
		return pt;
	}
}