package net.osmand.plus.render;

public class PerformanceFlags {

	// TimeLoadingMap = Rendering (%25) + Searching(%40) + Other 
	
	// It is needed to not draw object twice if user have map index that intersects by boundaries
	// Takes 25% TimeLoadingMap (?) - Long.valueOf - 12, add - 10, contains - 3.
	public static boolean checkForDuplicateObjectIds = true;
	
	
}
