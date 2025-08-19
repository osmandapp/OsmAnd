package net.osmand.data;

import android.content.Context;

import androidx.annotation.NonNull;

import net.osmand.shared.gpx.primitives.WptPt;

public class WptLocationPoint implements LocationPoint {

	WptPt pt;

	public WptLocationPoint(WptPt p) {
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
		return !pt.isHidden();
	}

	public PointDescription getPointDescription(@NonNull Context ctx) {
		return new PointDescription(PointDescription.POINT_TYPE_WPT, pt.getName());
	}

	public WptPt getPt() {
		return pt;
	}
}