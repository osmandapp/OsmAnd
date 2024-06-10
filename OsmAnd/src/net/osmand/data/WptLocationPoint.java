package net.osmand.data;

import android.content.Context;

import androidx.annotation.NonNull;

import net.osmand.shared.gpx.GpxUtilities;

public class WptLocationPoint implements LocationPoint {

	GpxUtilities.WptPt pt;

	public WptLocationPoint(GpxUtilities.WptPt p) {
		this.pt = p;
	}

	@Override
	public double getLatitude() {
		return pt.getLat();
	}

	@Override
	public double getLongitude() {
		return pt.getLon();
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
		return new PointDescription(PointDescription.POINT_TYPE_WPT, pt.getName());
	}

	public GpxUtilities.WptPt getPt() {
		return pt;
	}
}