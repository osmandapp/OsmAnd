package net.osmand;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

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
	private File iconsBaseDir;

	// FIELDS OF THAT CLASS ARE USED IN C++
	public boolean interrupted = false;
	public boolean nightMode = false;
	public boolean useEnglishNames = false;
	public int defaultColor = 0xf1eee8;

	public RenderingContext() {
	}
	
	public RenderingContext(File iconsBaseDir){
		this.iconsBaseDir = iconsBaseDir;
		
	}

	public float leftX;
	public float topY;
	public int width;
	public int height;

	public int zoom;
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
	
	public void setDensityValue(boolean highResMode, float mapTextSize, float density) {
//		boolean highResMode = false;
//		float mapTextSize = 1;
		if (highResMode && density > 1) {
			this.density =  density * mapTextSize;
		} else {
			this.density =  mapTextSize;
		}
	}

	public float getDensityValue(float val) {
		return val * density;
	}
	
	private Map<String, byte[]> precache = new HashMap<String, byte[]>(); 
	
	protected byte[] getIconRawData(String data) {
		if(iconsBaseDir != null) {
			File fs = new File(iconsBaseDir.getAbsolutePath()+"/h_"+data+".png");
			if(!fs.exists()) {
				fs = new File(iconsBaseDir.getAbsolutePath()+"/g_"+data+".png");
			}
			if (fs.exists()) {
				try {
					byte[] dta = new byte[(int) fs.length()];
					FileInputStream fis = new FileInputStream(fs);
					int l = fis.read(dta);
					fis.close();
					if (l == dta.length) {
						precache.put(data, dta);
						return dta;
					} else {
						System.err.println("Read data " + l + " however was expected " + dta.length + " for " + data);
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return null;
	}
}