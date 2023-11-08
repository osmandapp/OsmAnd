package net.osmand;

import net.osmand.render.RenderingRuleProperty;
import net.osmand.render.RenderingRuleSearchRequest;


public class RenderingContext {
	public static enum ShadowRenderingMode {
		// int shadowRenderingMode = 0; // no shadow (minimum CPU)
		// int shadowRenderingMode = 1; // classic shadow (the implementation in master)
		// int shadowRenderingMode = 2; // blur shadow (most CPU, but still reasonable)
		// int shadowRenderingMode = 3; solid border (CPU use like classic version or even smaller)
		NO_SHADOW(0), ONE_STEP(1), BLUR_SHADOW(2), SOLID_SHADOW(3);
		public final int value;

		ShadowRenderingMode(int v) {
			this.value = v;
		}
	}

	public int renderedState = 0;
	// FIELDS OF THAT CLASS ARE USED IN C++
	public boolean interrupted = false;
	public boolean nightMode = false;
	public String preferredLocale = "";
	public boolean transliterate = false;
	public int defaultColor = 0xf1eee8;

	public RenderingContext() {
	}

	public double leftX;
	public double topY;
	public int width;
	public int height;

	public int zoom;
	public double tileDivisor;
	public float rotate;

	// debug purpose
	public int pointCount = 0;
	public int pointInsideCount = 0;
	public int visible = 0;
	public int allObjects = 0;
	public int textRenderingTime = 0;
	public int lastRenderedKey = 0;

	// be aware field is using in C++
	public float screenDensityRatio = 1;
	public float textScale = 1;
	public int shadowRenderingMode = ShadowRenderingMode.SOLID_SHADOW.value;
	public int shadowRenderingColor = 0xff969696;
	public String renderingDebugInfo;
	public double polygonMinSizeToDisplay;
	public long renderingContextHandle;
	
	private float density = 1;
	public boolean saveTextTile = false;
	public String textTile;
	
	public void setDensityValue(float density) {
		this.density =  density ;
	}

	public float getDensityValue(float val) {
		return val * density;
	}
	
	public float getComplexValue(RenderingRuleSearchRequest req, RenderingRuleProperty prop, int defVal) {
		return req.getFloatPropertyValue(prop, 0) * density + req.getIntPropertyValue(prop, defVal);
	}
	
	public float getComplexValue(RenderingRuleSearchRequest req, RenderingRuleProperty prop) {
		return req.getFloatPropertyValue(prop, 0) * density + req.getIntPropertyValue(prop, 0);
	}
	
	protected byte[] getIconRawData(String data) {
		return null;
	}
	
	protected void finalize() throws Throwable {
		super.finalize();
		if (renderingContextHandle != 0) {
			NativeLibrary.deleteRenderingContextHandle(renderingContextHandle);
			renderingContextHandle = 0;
		}
	}
}