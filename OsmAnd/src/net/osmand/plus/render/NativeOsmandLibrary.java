package net.osmand.plus.render;

import java.util.List;

import net.osmand.binary.BinaryMapDataObject;
import net.osmand.plus.render.OsmandRenderer.RenderingContext;
import net.osmand.render.RenderingRuleSearchRequest;
import android.graphics.Bitmap;
import android.graphics.Canvas;

public class NativeOsmandLibrary {
	
	static {
		System.loadLibrary("osmand");  
	}
	
	public static native String generateRendering(RenderingContext rc, BinaryMapDataObject[] objects, Canvas cv, 
			boolean useEnglishNames, RenderingRuleSearchRequest render);
	
}
