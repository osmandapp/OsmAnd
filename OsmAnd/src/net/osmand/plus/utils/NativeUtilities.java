package net.osmand.plus.utils;

import android.graphics.Bitmap;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;

import net.osmand.core.jni.ColorARGB;
import net.osmand.core.jni.FColorARGB;
import net.osmand.core.jni.FColorRGB;
import net.osmand.core.jni.SWIGTYPE_p_sk_spT_SkImage_const_t;
import net.osmand.core.jni.SwigUtilities;

public class NativeUtilities {
	public static SWIGTYPE_p_sk_spT_SkImage_const_t createSkImageFromBitmap(@NonNull Bitmap inputBmp) {
		SWIGTYPE_p_sk_spT_SkImage_const_t swigImg = SwigUtilities.createSkImageARGB888With(
				inputBmp.getWidth(), inputBmp.getHeight(), AndroidUtils.getByteArrayFromBitmap(inputBmp));
		return swigImg;
	}

	public static FColorRGB createFColorRGB(@ColorInt int color) {
		return new FColorRGB((color >> 16 & 0xff) / 255.0f,
				((color >> 8) & 0xff) / 255.0f,
				((color) & 0xff) / 255.0f);
	}

	public static FColorARGB createFColorARGB(@ColorInt int color) {
		float a = (color >> 24) & 0xFF;
		float r = (color >> 16) & 0xFF;
		float g = (color >> 8) & 0xFF;
		float b = (color >> 0) & 0xFF;
		return new FColorARGB(a / 255, r / 255, g / 255, b / 255);
	}

	public static ColorARGB createColorARGB(@ColorInt int color) {
		int a = (color >> 24) & 0xFF;
		int r = (color >> 16) & 0xFF;
		int g = (color >> 8) & 0xFF;
		int b = (color >> 0) & 0xFF;
		return new ColorARGB((short)a, (short)r , (short)g, (short)b);
	}
}
