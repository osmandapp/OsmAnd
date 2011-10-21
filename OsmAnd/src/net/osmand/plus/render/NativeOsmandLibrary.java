package net.osmand.plus.render;


import net.osmand.binary.BinaryMapDataObject;
import net.osmand.plus.render.OsmandRenderer.RenderingContext;
import net.osmand.render.RenderingRuleSearchRequest;
import android.graphics.Canvas;
import android.graphics.Paint;

public class NativeOsmandLibrary {
	
	public static native String generateRendering(RenderingContext rc, BinaryMapDataObject[] objects, Canvas cv, 
			boolean useEnglishNames, RenderingRuleSearchRequest render, Paint paint);
	
}
