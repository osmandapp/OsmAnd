package net.osmand.plus.render;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;

import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import net.osmand.PlatformUtil;
import net.osmand.plus.R;
import net.osmand.plus.R.drawable;

import org.apache.commons.logging.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class RenderingIcons {
	private static final Log log = PlatformUtil.getLog(RenderingIcons.class);
	
	private static Map<String, Integer> shaderIcons = new LinkedHashMap<String, Integer>();
	private static Map<String, Integer> smallIcons = new LinkedHashMap<String, Integer>();
	private static Map<String, Integer> bigIcons = new LinkedHashMap<String, Integer>();
	private static Map<String, Bitmap> iconsBmp = new LinkedHashMap<String, Bitmap>();
	private static Map<String, Drawable> iconsDrawable = new LinkedHashMap<String, Drawable>();
//	private static DisplayMetrics dm;

	private static Bitmap cacheBmp = null;

	public static boolean containsSmallIcon(String s){
		return smallIcons.containsKey(s);
	}
	
	public static boolean containsBigIcon(String s){
		return bigIcons.containsKey(s);
	}

	public static synchronized Bitmap getBitmapFromVectorDrawable(Context context, int drawableId) {
		Drawable drawable = AppCompatResources.getDrawable(context, drawableId);
		if (drawable == null) {
			return null;
		}
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
			drawable = (DrawableCompat.wrap(drawable)).mutate();
		}
		if(cacheBmp == null || cacheBmp.getWidth() != drawable.getIntrinsicWidth() ||
				cacheBmp.getHeight() != drawable.getIntrinsicHeight()) {
			cacheBmp = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
					drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
		}
		cacheBmp.eraseColor(Color.TRANSPARENT);
		Canvas canvas = new Canvas(cacheBmp);
		drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
		drawable.draw(canvas);
		return cacheBmp;
	}

	private static synchronized byte[] getPngFromVectorDrawable(Context context, int drawableId) {
		Bitmap bmp = getBitmapFromVectorDrawable(context, drawableId);
		if (bmp == null) {
			return null;
		}
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		bmp.compress(Bitmap.CompressFormat.PNG, 100, baos);
		return baos.toByteArray();
	}
	
	public static byte[] getIconRawData(Context ctx, String s) {
		Integer resId = shaderIcons.get(s);
		if (resId == null) {
			 resId = smallIcons.get(s);
		}
		if (resId == null) {
			return null;
		}
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
			if (isVectorData(bitmapData)) {
				return getPngFromVectorDrawable(ctx, resId.intValue());
			}
//			log.info("Icon data length is " + bitmapData.length); //$NON-NLS-1$
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

	private static boolean isVectorData(byte[] bitmapData) {
		for (int i = 0; i < bitmapData.length - 8 && i < 32; i++) {
			int ind = 0;
			if (bitmapData[i] == 'P' && bitmapData[i + 1] == 'N' &&
					bitmapData[i + 2] == 'G') {
				return false;
			}
		}
		if (bitmapData.length > 4 && bitmapData[0] == 3 && bitmapData[1] == 0 &&
				bitmapData[2] == 8 && bitmapData[3] == 0) {
			return true;
		}
		return false;
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
			return AppCompatResources.getDrawable(ctx, resId);
		}
		return null;
	}

	/**
	 * Return vector icon as bitmap. Used for java rendering only.
	 *
	 * @deprecated Use getDrawableIcon instead.
	 */
	@Deprecated
	public static Bitmap getIcon(Context ctx, String s, boolean includeShader) {
		if(s == null) {
			return null;
		}
		if(includeShader && shaderIcons.containsKey(s)) {
			s = "h_" + s;
		}
		if (!iconsBmp.containsKey(s)) {
			Integer resId = s.startsWith("h_") ? shaderIcons.get(s.substring(2)) : smallIcons.get(s);
			if (resId != null) {
				Bitmap bmp = getBitmapFromVectorDrawable(ctx, resId);
				iconsBmp.put(s, bmp);
			} else {
				iconsBmp.put(s, null);
			}
		}
		return iconsBmp.get(s);
	}

	public static Drawable getDrawableIcon(Context ctx, String s, boolean includeShader) {
		if (s == null) {
			return null;
		}
		if (includeShader && shaderIcons.containsKey(s)) {
			s = "h_" + s;
		}
		Drawable d = iconsDrawable.get(s);
		if (d == null) {
			Integer drawableId = s.startsWith("h_") ? shaderIcons.get(s.substring(2)) : smallIcons.get(s);
			if (drawableId != null) {
				d = AppCompatResources.getDrawable(ctx, drawableId);
				if (d != null) {
					d = DrawableCompat.wrap(d);
					d.mutate();
					iconsDrawable.put(s, d);
				}
			}
		}
		return d;
	}

	public static Integer getResId(String id) {
		return id.startsWith("h_") ? shaderIcons.get(id.substring(2)) : smallIcons.get(id);
	}

	static {
		initIcons();
	}

	public static void initIcons() {
		Class<? extends drawable> cl = R.drawable.class;
		for (Field f : cl.getDeclaredFields()) {
			try {
				if (f.getName().startsWith("h_")) {
					shaderIcons.put(f.getName().substring(2), f.getInt(null));
				} else if( f.getName().startsWith("mm_")) {
					smallIcons.put(f.getName().substring(3), f.getInt(null));
				} else if (f.getName().startsWith("mx_")) {
					bigIcons.put(f.getName().substring(3), f.getInt(null));
				}
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
		}
	}
	
}
