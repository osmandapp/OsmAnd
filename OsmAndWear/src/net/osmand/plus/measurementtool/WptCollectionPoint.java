package net.osmand.plus.measurementtool;

import android.content.Context;
import android.graphics.Bitmap;

import androidx.annotation.NonNull;

import net.osmand.shared.gpx.primitives.WptPt;
import net.osmand.plus.views.layers.core.TilePointsProvider;

class WptCollectionPoint implements TilePointsProvider.ICollectionPoint {

	private final WptPt point;
	private final Bitmap image;

	public WptCollectionPoint(@NonNull WptPt point, @NonNull Bitmap image) {
		this.point = point;
		this.image = image;
	}

	@Override
	public double getLatitude() {
		return point.getLatitude();
	}

	@Override
	public double getLongitude() {
		return point.getLongitude();
	}

	@Override
	public Bitmap getBigImage(@NonNull Context ctx, float textScale, float density) {
		return image;
	}

	@Override
	public Bitmap getSmallImage(@NonNull Context ctx, float textScale, float density) {
		return image;
	}

	@Override
	@NonNull
	public String getCaption(@NonNull Context ctx) {
		return "";
	}
}
