package net.osmand.data;

import android.content.Context;


/**
 */
public interface LocationPoint {

	double getLatitude();

	double getLongitude();

	int getColor();

	boolean isVisible();

	PointDescription getPointDescription(Context ctx);


}
