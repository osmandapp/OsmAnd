package net.osmand.plus.render;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;
import java.nio.ByteBuffer;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;

import org.apache.commons.logging.Log;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.DisplayMetrics;
import android.view.WindowManager;
import android.content.res.AssetFileDescriptor;

import net.osmand.plus.R;
import net.osmand.plus.R.drawable;
import net.osmand.LogUtil;

public class RenderingIcons {
	private static final Log log = LogUtil.getLog(RenderingIcons.class);
	
	private static Map<String, Integer> icons = new LinkedHashMap<String, Integer>();
	private static Map<String, Bitmap> iconsBmp = new LinkedHashMap<String, Bitmap>();
	private static DisplayMetrics dm;
	
	public static boolean containsIcon(String s){
		return icons.containsKey(s);
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
			
			//if(android.graphics.BitmapFactory.decodeByteArray(bitmapData, 0, bitmapData.length) == null)
			//	throw new Exception();
            return bitmapData;
		} catch(Throwable e) {
			log.error("Failed to get byte stream from icon", e); //$NON-NLS-1$
			return null;
		}
	}
	
	public static Bitmap getIcon(Context ctx, String s){
		if(!iconsBmp.containsKey(s)){
			Integer resId = icons.get(s);
			if(resId != null){
				if(dm == null) {
					dm = new DisplayMetrics();
					WindowManager wmgr = (WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE);
					wmgr.getDefaultDisplay().getMetrics(dm);
				}
				Bitmap bmp = UnscaledBitmapLoader.loadFromResource(ctx.getResources(), resId.intValue(), null, dm);
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
			if (f.getName().startsWith("h_") || f.getName().startsWith("g_")) {
				try {
					icons.put(f.getName().substring(2), f.getInt(null));
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
}
