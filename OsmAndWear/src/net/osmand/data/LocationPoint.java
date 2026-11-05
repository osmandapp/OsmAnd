package net.osmand.data;

import android.content.Context;

import androidx.annotation.NonNull;

public interface LocationPoint {

	double getLatitude();

	double getLongitude();

	int getColor();

	boolean isVisible();

	PointDescription getPointDescription(@NonNull Context ctx);
}
