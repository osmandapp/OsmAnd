package net.osmand.plus.render;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;

import net.osmand.PlatformUtil;
import net.osmand.plus.R;
import net.osmand.plus.R.drawable;

import org.apache.commons.logging.Log;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;
import android.view.WindowManager;

public class RenderingIcons {
	private static final Log log = PlatformUtil.getLog(RenderingIcons.class);
	
	private static Map<String, Integer> icons = new LinkedHashMap<String, Integer>();
	private static Map<String, Integer> smallIcons = new LinkedHashMap<String, Integer>();
	private static Map<String, Integer> bigIcons = new LinkedHashMap<String, Integer>();
	private static Map<String, Bitmap> iconsBmp = new LinkedHashMap<String, Bitmap>();
	private static DisplayMetrics dm;
	
	public static boolean containsIcon(String s){
		return icons.containsKey(s);
	}
	
	public static boolean containsBigIcon(String s){
		return bigIcons.containsKey(s);
	}
	
	public static byte[] getIconRawData(Context ctx, String s) {
		Integer resId = icons.get(s);
		
		// Quite bad error
		if(resId == null)
			return null;
			
		try {
			final InputStream inputStream = ctx.getResources().openRawResource(resId.intValue());
			final ByteArrayOutputStream proxyOutputStream = new ByteArrayOutputStream(1024);
            final byte[] ioBuffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(ioBuffer)) >= 0) {
				proxyOutputStream.write(ioBuffer, 0, bytesRead);
			}
			inputStream.close();
			final byte[] bitmapData = proxyOutputStream.toByteArray();
			log.info("Icon data length is " + bitmapData.length); //$NON-NLS-1$
//			Bitmap dm = android.graphics.BitmapFactory.decodeByteArray(bitmapData, 0, bitmapData.length) ;
//			if(dm != null){
//				System.out.println("IC " + s +" " + dm.getHeight() + "x" + dm.getWidth());
//			}
			//if(android.graphics.BitmapFactory.decodeByteArray(bitmapData, 0, bitmapData.length) == null)
			//	throw new Exception();
            return bitmapData;
		} catch(Throwable e) {
			log.error("Failed to get byte stream from icon", e); //$NON-NLS-1$
			return null;
		}
	}
	
	public static int getBigIconResourceId(String s) {
		Integer i = bigIcons.get(s);
		if (i == null) {
			return 0;
		}
		return i;
	}
	
	public static Drawable getBigIcon(Context ctx, String s) {
		Integer resId = bigIcons.get(s);
		if (resId != null) {
			return ctx.getResources().getDrawable(resId);
		}
		return null;
	}
	
	public static Bitmap getSmallPoiIcon(Context ctx, String s) {
		Integer resId = smallIcons.get(s);
		if (resId != null) {
			return BitmapFactory.decodeResource(ctx.getResources(), resId, null);
		}
		return null;
	}
	
	public static Bitmap getIcon(Context ctx, String s) {
		if (!iconsBmp.containsKey(s)) {
			Integer resId = icons.get(s);
			if (resId != null) {
				if (dm == null) {
					dm = new DisplayMetrics();
					WindowManager wmgr = (WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE);
					wmgr.getDefaultDisplay().getMetrics(dm);
				}
//				BitmapFactory.Options options = new BitmapFactory.Options();
//	            options.inScaled = false;
//	            options.inTargetDensity = dm.densityDpi;
//				options.inDensity = dm.densityDpi;
				Bitmap bmp = BitmapFactory.decodeResource(ctx.getResources(), resId, null);
//				Bitmap bmp = UnscaledBitmapLoader.loadFromResource(ctx.getResources(), resId.intValue(), null, dm);
				iconsBmp.put(s, bmp);
			} else {
				iconsBmp.put(s, null);
			}
		}
		return iconsBmp.get(s);
	}
	
	
	static {
		initIcons();
	}

	public static void initIcons() {
		Class<? extends drawable> cl = R.drawable.class;
		for (Field f : cl.getDeclaredFields()) {
			if (f.getName().startsWith("h_") || f.getName().startsWith("mm_")) {
				try {
					String id = f.getName().substring(f.getName().startsWith("mm_") ? 3 : 2);
					int i = f.getInt(null);
					// don't override shader or map icons (h) 
					if(f.getName().startsWith("h_") || !icons.containsKey(id)) {
						icons.put(id, i);
					}
					if(f.getName().startsWith("mm_")) {
						smallIcons.put(id, i);
					}
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				}
			}
			if (f.getName().startsWith("mx_") ) {
				try {
					bigIcons.put(f.getName().substring(3), f.getInt(null));
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
}
