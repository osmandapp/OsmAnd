package net.osmand.data;
import net.osmand.plus.voice.CommandBuilder;

/**
 */
public interface LocationPoint {

	public double getLatitude();

	public double getLongitude();

	public String getName();

	public int getColor();
	
	public boolean isVisible();

//	public String getSpeakableName();
	
	//public void prepareCommandPlayer(CommandBuilder cmd, String names);
	
}
