package net.osmand.telegram.utils;


import android.content.Context;

/**
 */
public interface LocationPoint {

	public double getLatitude();

	public double getLongitude();

	public int getColor();

	public boolean isVisible();

//	public PointDescription getPointDescription(Context ctx);

//	public String getSpeakableName();

	//public void prepareCommandPlayer(CommandBuilder cmd, String names);

}