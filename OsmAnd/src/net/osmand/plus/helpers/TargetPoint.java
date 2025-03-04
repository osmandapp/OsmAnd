package net.osmand.plus.helpers;

import static net.osmand.data.PointDescription.POINT_TYPE_TARGET;

import android.annotation.SuppressLint;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.data.LatLon;
import net.osmand.data.LocationPoint;
import net.osmand.data.PointDescription;
import net.osmand.plus.R;
import net.osmand.util.Algorithms;

public class TargetPoint implements LocationPoint {

	private final LatLon latLon;
	private final PointDescription pointDescription;

	public int index;
	public boolean intermediate;
	public boolean start;

	public TargetPoint(@NonNull LatLon latLon, @Nullable PointDescription name) {
		this.latLon = latLon;
		this.pointDescription = name;
	}

	public TargetPoint(@NonNull LatLon latLon, @Nullable PointDescription name, int index) {
		this.latLon = latLon;
		this.pointDescription = name;
		this.index = index;
		this.intermediate = true;
	}

	@NonNull
	public LatLon getLatLon() {
		return latLon;
	}

	@Nullable
	public PointDescription getOriginalPointDescription() {
		return pointDescription;
	}

	@SuppressLint("StringFormatInvalid")
	public PointDescription getPointDescription(@NonNull Context ctx) {
		String name = getOnlyName();
		if (intermediate) {
			String type = (index + 1) + ". " + ctx.getString(R.string.intermediate_point, "");
			return new PointDescription(POINT_TYPE_TARGET, type, name);
		} else {
			String type = ctx.getString(R.string.destination_point, "");
			return new PointDescription(POINT_TYPE_TARGET, type, name);
		}
	}

	@NonNull
	public String getRoutePointDescription(@NonNull Context ctx, boolean includeAddress) {
		if (pointDescription != null) {
			String name = pointDescription.getName();
			String typeName = pointDescription.getTypeName();

			if (!Algorithms.isEmpty(name)) {
				if (includeAddress && pointDescription.isAddress() && !Algorithms.isEmpty(typeName)) {
					name = ctx.getString(R.string.ltr_or_rtl_combine_via_comma, name, typeName);
				}
				return name.replace(':', ' ');
			}
		}
		return PointDescription.getLocationNamePlain(ctx, latLon.getLatitude(), latLon.getLongitude());
	}

	@NonNull
	public String getOnlyName() {
		return pointDescription == null ? "" : pointDescription.getName();
	}

	public boolean isSearchingAddress(@NonNull Context ctx) {
		return pointDescription != null && pointDescription.isSearchingAddress(ctx);
	}

	public double getLatitude() {
		return latLon.getLatitude();
	}

	public double getLongitude() {
		return latLon.getLongitude();
	}

	@Override
	public int getColor() {
		return 0;
	}

	@Override
	public boolean isVisible() {
		return false;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		TargetPoint targetPoint = (TargetPoint) o;

		if (start != targetPoint.start) return false;
		if (intermediate != targetPoint.intermediate) return false;
		if (index != targetPoint.index) return false;
		return latLon.equals(targetPoint.latLon);

	}

	@Override
	public int hashCode() {
		int result = latLon.hashCode();
		result = 31 * result + index;
		result = 31 * result + (start ? 10 : 20);
		result = 31 * result + (intermediate ? 100 : 200);
		return result;
	}

	@Nullable
	public static TargetPoint create(@Nullable LatLon point, @Nullable PointDescription name) {
		if (point != null) {
			return new TargetPoint(point, name);
		}
		return null;
	}

	@Nullable
	public static TargetPoint createStartPoint(@Nullable LatLon point,
			@Nullable PointDescription name) {
		if (point != null) {
			TargetPoint target = new TargetPoint(point, name);
			target.start = true;
			return target;
		}
		return null;
	}
}