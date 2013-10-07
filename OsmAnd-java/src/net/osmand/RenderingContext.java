package net.osmand;

import net.osmand.render.RenderingRuleProperty;
import net.osmand.render.RenderingRuleSearchRequest;


public class RenderingContext {
	static enum ShadowRenderingMode {
		// int shadowRenderingMode = 0; // no shadow (minumum CPU)
		// int shadowRenderingMode = 1; // classic shadow (the implementaton in master)
		// int shadowRenderingMode = 2; // blur shadow (most CPU, but still reasonable)
		// int shadowRenderingMode = 3; solid border (CPU use like classic version or even smaller)
		NO_SHADOW(0), ONE_STEP(1), BLUR_SHADOW(2), SOLID_SHADOW(3);
		public final int value;

		ShadowRenderingMode(int v) {
			this.value = v;
		}
	}

	// FIELDS OF THAT CLASS ARE USED IN C++
	public boolean interrupted = false;
	public boolean nightMode = false;
	public boolean useEnglishNames = false;
	public int defaultColor = 0xf1eee8;

	public RenderingContext() {
	}
	

	public float leftX;
	public float topY;
	public int width;
	public int height;

	public int zoom;
	public float tileDivisor;
	public float rotate;

	// debug purpose
	public int pointCount = 0;
	public int pointInsideCount = 0;
	public int visible = 0;
	public int allObjects = 0;
	public int textRenderingTime = 0;
	public int lastRenderedKey = 0;

	// be aware field is using in C++
	public int shadowRenderingMode = ShadowRenderingMode.BLUR_SHADOW.value;
	public int shadowRenderingColor = 0xff969696;
	public String renderingDebugInfo;
	
	private float density = 1;
	
	public void setDensityValue(float density) {
		this.density =  density ;
	}

	public float getDensityValue(float val) {
		return val * density;
	}
	
	public float getComplexValue(RenderingRuleSearchRequest req, RenderingRuleProperty prop, float defVal) {
		return req.getFloatPropertyValue(prop, defVal) * density + req.getIntPropertyValue(prop, 0);
	}
	
	protected byte[] getIconRawData(String data) {
		return null;
	}
}