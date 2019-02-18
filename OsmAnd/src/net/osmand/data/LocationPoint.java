package net.osmand.data;

import android.content.Context;

import net.osmand.GPXUtilities;


/**
 */
public interface LocationPoint {

	public double getLatitude();

	public double getLongitude();

	public int getColor();

	public boolean isVisible();

	public PointDescription getPointDescription(Context ctx);


}
