package net.osmand.core.samples.android.sample1;

import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.support.annotation.ColorInt;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;

import net.osmand.core.android.CoreResourcesFromAndroidAssets;

import gnu.trove.map.hash.TLongObjectHashMap;

public class IconsCache {

	private TLongObjectHashMap<Drawable> drawable = new TLongObjectHashMap<>();
	private TLongObjectHashMap<Drawable> osmandDrawable = new TLongObjectHashMap<>();
	private SampleApplication app;
	private CoreResourcesFromAndroidAssets assets;
	private float displayDensityFactor;

	public IconsCache(CoreResourcesFromAndroidAssets assets, SampleApplication app) {
		this.assets = assets;
		this.app = app;
	}

	public float getDisplayDensityFactor() {
		return displayDensityFactor;
	}

	public void setDisplayDensityFactor(float displayDensityFactor) {
		this.displayDensityFactor = displayDensityFactor;
	}

	public Drawable getMapIcon(String name) {
		return assets.getIcon("map/icons/" + name + ".png", displayDensityFactor);
	}

	private Drawable getDrawable(@DrawableRes int resId, @ColorRes int clrId) {
		long hash = ((long)resId << 31L) + clrId;
		Drawable d = drawable.get(hash);
		if (d == null) {
			d = ContextCompat.getDrawable(app, resId);
			d = DrawableCompat.wrap(d);
			d.mutate();
			if (clrId != 0) {
				DrawableCompat.setTint(d, ContextCompat.getColor(app, clrId));
			}
			drawable.put(hash, d);
		}
		return d;
	}

	private Drawable getPaintedDrawable(@DrawableRes int resId, @ColorInt int color){
		long hash = ((long)resId << 31L) + color;
		Drawable d = drawable.get(hash);
		if(d == null) {
			d = ContextCompat.getDrawable(app, resId);
			d = DrawableCompat.wrap(d);
			d.mutate();
			DrawableCompat.setTint(d, color);

			drawable.put(hash, d);
		}
		return d;
	}

	public Drawable getPaintedIcon(@DrawableRes int id, @ColorInt int color){
		return getPaintedDrawable(id, color);
	}

	public Drawable getIcon(@DrawableRes int id, @ColorRes int colorId) {
		return getDrawable(id, colorId);
	}

	public Drawable getIcon(@DrawableRes int backgroundId, @DrawableRes int id, @ColorRes int colorId) {
		Drawable b = getDrawable(backgroundId, 0);
		Drawable f = getDrawable(id, colorId);
		Drawable[] layers = new Drawable[2];
		layers[0] = b;
		layers[1] = f;
		return new LayerDrawable(layers);
	}

	public Drawable getThemedIcon(@DrawableRes int id) {
		return getDrawable(id, R.color.icon_color);
	}

	public Drawable getIcon(@DrawableRes int id) {
		return getDrawable(id, 0);
	}

	public Drawable getIcon(@DrawableRes int id, boolean light) {
		return getDrawable(id, light ? R.color.icon_color : 0);
	}


	// Osmand resources
	private Drawable getOsmandDrawable(int resId, @ColorRes int clrId) {
		long hash = ((long)resId << 31L) + clrId;
		Drawable d = osmandDrawable.get(hash);
		if (d == null) {
			d = OsmandResources.getDrawableNonCached(resId);
			if (d != null) {
				d = DrawableCompat.wrap(d);
				d.mutate();
				if (clrId != 0) {
					DrawableCompat.setTint(d, ContextCompat.getColor(app, clrId));
				}
				osmandDrawable.put(hash, d);
			}
		}
		return d;
	}

	private Drawable getPaintedOsmandDrawable(int resId, @ColorInt int color){
		long hash = ((long)resId << 31L) + color;
		Drawable d = osmandDrawable.get(hash);
		if(d == null) {
			d = OsmandResources.getDrawableNonCached(resId);
			if (d != null) {
				d = DrawableCompat.wrap(d);
				d.mutate();
				DrawableCompat.setTint(d, color);
				osmandDrawable.put(hash, d);
			}
		}
		return d;
	}

	public Drawable getPaintedOsmandIcon(int resId, @ColorInt int color){
		return getPaintedOsmandDrawable(resId, color);
	}

	public Drawable getPaintedIcon(String osmandId, @ColorInt int color){
		int id = OsmandResources.getDrawableId(osmandId);
		if (id != 0) {
			return getPaintedOsmandDrawable(id, color);
		}
		return null;
	}

	public Drawable getOsmandIcon(@DrawableRes int id, @ColorRes int colorId) {
		return getOsmandDrawable(id, colorId);
	}

	public Drawable getIcon(String osmandId, @ColorRes int colorId) {
		int id = OsmandResources.getDrawableId(osmandId);
		if (id != 0) {
			return getOsmandDrawable(id, colorId);
		}
		return null;
	}

	public Drawable getIcon(String osmandBackgroundId, String osmandId, @ColorRes int colorId) {
		int backgroundId = OsmandResources.getDrawableId(osmandBackgroundId);
		int id = OsmandResources.getDrawableId(osmandId);
		if (backgroundId != 0 && id != 0) {
			Drawable b = getOsmandDrawable(backgroundId, 0);
			Drawable f = getOsmandDrawable(id, colorId);
			Drawable[] layers = new Drawable[2];
			layers[0] = b;
			layers[1] = f;
			return new LayerDrawable(layers);
		}
		return null;
	}

	public Drawable getThemedIcon(String osmandId) {
		int id = OsmandResources.getDrawableId(osmandId);
		if (id != 0) {
			return getOsmandDrawable(id, R.color.icon_color);
		}
		return null;
	}

	public Drawable getIcon(String osmandId) {
		int id = OsmandResources.getDrawableId(osmandId);
		if (id != 0) {
			return getOsmandDrawable(id, 0);
		}
		return null;
	}

	public Drawable getIcon(String osmandId, boolean light) {
		int id = OsmandResources.getDrawableId(osmandId);
		if (id != 0) {
			return getOsmandDrawable(id, light ? R.color.icon_color : 0);
		}
		return null;
	}
}
