package net.osmand.plus;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.support.annotation.ColorInt;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.view.MenuItem;

import gnu.trove.map.hash.TLongObjectHashMap;

public class IconsCache {

	private TLongObjectHashMap<Drawable> drawable = new TLongObjectHashMap<>();
	private OsmandApplication app;
	
	public IconsCache(OsmandApplication app) {
		this.app = app;
	}

	public Drawable scaleImage(Drawable image, float scaleFactor) {
		if ((image == null) || !(image instanceof BitmapDrawable)) {
			return image;
		}
		Bitmap b = ((BitmapDrawable)image).getBitmap();

		int sizeX = Math.round(image.getIntrinsicWidth() * scaleFactor);
		int sizeY = Math.round(image.getIntrinsicHeight() * scaleFactor);

		Bitmap bitmapResized = Bitmap.createScaledBitmap(b, sizeX, sizeY, false);
		return new BitmapDrawable(app.getResources(), bitmapResized);
	}

	@Deprecated
	private Drawable getDrawable(@DrawableRes int resId, @ColorRes int clrId) {
		return getDrawable(resId, clrId, 0);
	}

	@Deprecated
	private Drawable getDrawable(@DrawableRes int resId, @ColorRes int clrId, float scale) {
		long hash = ((long)resId << 31l) + clrId + (int)(scale * 10000f);
		Drawable d = drawable.get(hash);
		if(d == null) {
			if (scale > 0) {
				d = scaleImage(app.getResources().getDrawable(resId).mutate(), scale);
			} else {
				d = app.getResources().getDrawable(resId).mutate();
			}
			d.clearColorFilter();
			if (clrId != 0) {
				d.setColorFilter(app.getResources().getColor(clrId), PorterDuff.Mode.SRC_IN);
			}
			drawable.put(hash, d);
		}
		return d;
	}

	@Deprecated
	private Drawable getPaintedDrawable(@DrawableRes int resId, @ColorInt int color){
		long hash = ((long)resId << 31l) + color;
		Drawable d = drawable.get(hash);
		if(d == null) {
			d = app.getResources().getDrawable(resId).mutate();
			d.clearColorFilter();
			d.setColorFilter(color, PorterDuff.Mode.SRC_IN);
			drawable.put(hash, d);
		}
		return d;
	}

	@Deprecated
	public Drawable getPaintedContentIcon(@DrawableRes int id, @ColorInt int color){
		return getPaintedDrawable(id, color);
	}

	@Deprecated
	public Drawable getIcon(@DrawableRes int id, @ColorRes int colorId) {
		return getDrawable(id, colorId);
	}


	@Deprecated
	public Drawable getIcon(@DrawableRes int backgroundId, @DrawableRes int id, @ColorRes int colorId) {
		Drawable b = getDrawable(backgroundId, 0);
		Drawable f = getDrawable(id, colorId);
		Drawable[] layers = new Drawable[2];
		layers[0] = b;
		layers[1] = f;
		return new LayerDrawable(layers);
	}

	@Deprecated
	public Drawable getContentIcon(@DrawableRes int id) {
		return getDrawable(id, app.getSettings().isLightContent() ? R.color.icon_color : 0);
	}

	@Deprecated
	public Drawable getContentIcon(@DrawableRes int id, boolean isLightContent) {
		return getDrawable(id, isLightContent ? R.color.icon_color : 0);
	}

	@Deprecated
	public Drawable getIcon(@DrawableRes int id) {
		return getDrawable(id, 0);
	}

	@Deprecated
	public Drawable getIcon(@DrawableRes int id, boolean light) {
		return getDrawable(id, light ? R.color.icon_color : 0);
	}

	public static Drawable getContentIconCompat(Context context, @DrawableRes int id) {
		Drawable drawable = ContextCompat.getDrawable(context, id);
		@ColorInt int color = ContextCompat.getColor(context, getDefaultColorRes(context));
		drawable = DrawableCompat.wrap(drawable);
		drawable.mutate();
		DrawableCompat.setTint(drawable, color);
		return drawable;
	}

	public static void paintMenuItem(Context context, MenuItem menuItem) {
		Drawable drawable = menuItem.getIcon();
		drawable = DrawableCompat.wrap(drawable);
		drawable.mutate();
		int color = ContextCompat.getColor(context, getDefaultColorRes(context));
		DrawableCompat.setTint(drawable, color);
	}

	@ColorRes
	public static int getDefaultColorRes(Context context) {
		final OsmandApplication app = (OsmandApplication) context.getApplicationContext();
		boolean light = app.getSettings().isLightContent();
		return light ? R.color.icon_color : R.color.color_white;
	}
}
