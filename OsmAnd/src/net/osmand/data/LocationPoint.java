package net.osmand.data;

import android.content.Context;

/**
 */
public interface LocationPoint {

	public double getLatitude();

	public double getLongitude();

	public String getName(Context ctx);

	public int getColor();
	
	public boolean isVisible();

//	public String getSpeakableName();
	
	//public void prepareCommandPlayer(CommandBuilder cmd, String names);
	
}
