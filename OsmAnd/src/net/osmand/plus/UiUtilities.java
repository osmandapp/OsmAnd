package net.osmand.plus;

import gnu.trove.map.hash.TLongObjectHashMap;
import net.osmand.Location;
import net.osmand.data.LatLon;
import net.osmand.plus.views.DirectionDrawable;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.support.annotation.ColorInt;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

public class UiUtilities {

	private TLongObjectHashMap<Drawable> drawableCache = new TLongObjectHashMap<>();
	private OsmandApplication app;
	private static final int ORIENTATION_0 = 0;
	private static final int ORIENTATION_90 = 3;
	private static final int ORIENTATION_270 = 1;
	private static final int ORIENTATION_180 = 2;
	

	public UiUtilities(OsmandApplication app) {
		this.app = app;
	}

	private Drawable getDrawable(@DrawableRes int resId, @ColorRes int clrId) {
		long hash = ((long) resId << 31l) + clrId;
		Drawable d = drawableCache.get(hash);
		if (d == null) {
			d = ContextCompat.getDrawable(app, resId);
			d = DrawableCompat.wrap(d);
			d.mutate();
			if (clrId != 0) {
				DrawableCompat.setTint(d, ContextCompat.getColor(app, clrId));
			}
			drawableCache.put(hash, d);
		}
		return d;
	}

	private Drawable getPaintedDrawable(@DrawableRes int resId, @ColorInt int color) {
		long hash = ((long) resId << 31l) + color;
		Drawable d = drawableCache.get(hash);
		if (d == null) {
			d = ContextCompat.getDrawable(app, resId);
			d = DrawableCompat.wrap(d);
			d.mutate();
			DrawableCompat.setTint(d, color);

			drawableCache.put(hash, d);
		}
		return d;
	}

	public Drawable getPaintedIcon(@DrawableRes int id, @ColorInt int color) {
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
		return getDrawable(id, R.color.description_font_and_bottom_sheet_icons);
	}

	public Drawable getIcon(@DrawableRes int id) {
		return getDrawable(id, 0);
	}

	public Drawable getIcon(@DrawableRes int id, boolean light) {
		return getDrawable(id, light ? R.color.icon_color : 0);
	}

	@ColorRes
	public static int getDefaultColorRes(Context context) {
		final OsmandApplication app = (OsmandApplication) context.getApplicationContext();
		boolean light = app.getSettings().isLightContent();
		return light ? R.color.icon_color : R.color.color_white;
	}

	@ColorInt
	public static int getContrastColor(Context context, @ColorInt int color, boolean transparent) {
		// Counting the perceptive luminance - human eye favors green color...
		double luminance = 1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255;
		return luminance < 0.5 ? transparent ? ContextCompat.getColor(context, R.color.color_black_transparent) : Color.BLACK : Color.WHITE;
	}

	public UpdateLocationViewCache getUpdateLocationViewCache(){
		UpdateLocationViewCache uvc = new UpdateLocationViewCache();
		uvc.screenOrientation = getScreenOrientation();
		return uvc;
	}
	
	public static class UpdateLocationViewCache {
		int screenOrientation;
		public boolean paintTxt = true;
		public int arrowResId;
		public int arrowColor;
		public int textColor;
		public LatLon specialFrom;
	}

	public void updateLocationView(UpdateLocationViewCache cache, ImageView arrow, TextView txt, 
			double toLat, double toLon) {
		updateLocationView(cache, arrow, txt, new LatLon(toLat, toLon));
	}
	public void updateLocationView(UpdateLocationViewCache cache, ImageView arrow, TextView txt, 
			LatLon toLoc) {
		float[] mes = new float[2];
		boolean stale = false;
		LatLon fromLoc = cache == null ? null : cache.specialFrom;
		boolean useCenter = fromLoc != null;
		Float h = null;
		if (fromLoc == null) {
			Location loc = app.getLocationProvider().getLastKnownLocation();
			h = app.getLocationProvider().getHeading();
			if (loc == null) {
				loc = app.getLocationProvider().getLastStaleKnownLocation();
				stale = true;
			}
			if (loc != null) {
				fromLoc = new LatLon(loc.getLatitude(), loc.getLongitude());
			} else {
				useCenter = true;
				stale = false;
				fromLoc = app.getMapViewTrackingUtilities().getMapLocation();
				h = app.getMapViewTrackingUtilities().getMapRotate();
				if(h != null) {
					h = -h;
				}
			}
		}
		if (fromLoc != null && toLoc != null) {
			Location.distanceBetween(toLoc.getLatitude(), toLoc.getLongitude(), fromLoc.getLatitude(),
					fromLoc.getLongitude(), mes);
		}

		if (arrow != null) {
			boolean newImage = false;
			int arrowResId = cache == null ? 0 : cache.arrowResId;
			if (arrowResId == 0) {
				arrowResId = R.drawable.ic_direction_arrow;
			}
			DirectionDrawable dd;
			if (!(arrow.getDrawable() instanceof DirectionDrawable)) {
				newImage = true;
				dd = new DirectionDrawable(app, arrow.getWidth(), arrow.getHeight());
			} else {
				dd = (DirectionDrawable) arrow.getDrawable();
			}
			int imgColorSet = cache == null ? 0 : cache.arrowColor;
			if (imgColorSet == 0) {
				imgColorSet = useCenter ? R.color.color_distance : R.color.color_myloc_distance;
				if (stale) {
					imgColorSet = R.color.icon_color;
				}
			}
			dd.setImage(arrowResId, imgColorSet);
			if (fromLoc == null || h == null || toLoc == null) {
				dd.setAngle(0);
			} else {
				float orientation = (cache == null ? 0 : cache.screenOrientation) ;
				dd.setAngle(mes[1] - h + 180 + orientation);
			}
			if (newImage) {
				arrow.setImageDrawable(dd);
			}
			arrow.invalidate();
		}
		if (txt != null) {
			if (fromLoc != null && toLoc != null) {
				if (cache.paintTxt) {
					int textColorSet = cache.textColor;
					if (textColorSet == 0) {
						textColorSet = useCenter ? R.color.color_distance : R.color.color_myloc_distance;
						if (stale) {
							textColorSet = R.color.icon_color;
						}
					}
					txt.setTextColor(app.getResources().getColor(textColorSet));
				}
				txt.setText(OsmAndFormatter.getFormattedDistance(mes[0], app));
			} else {
				txt.setText("");
			}
		}
	}
	
	public int getScreenOrientation() {
		int screenOrientation = ((WindowManager) app.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRotation();
		switch (screenOrientation) {
			case ORIENTATION_0:   // Device default (normally portrait)
				screenOrientation = 0;
				break;
			case ORIENTATION_90:  // Landscape right
				screenOrientation = 90;
				break;
			case ORIENTATION_270: // Landscape left
				screenOrientation = 270;
				break;
			case ORIENTATION_180: // Upside down
				screenOrientation = 180;
				break;
		}
		//Looks like screenOrientation correction must not be applied for devices without compass?
		Sensor compass = ((SensorManager) app.getSystemService(Context.SENSOR_SERVICE)).getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
		if (compass == null) {
			screenOrientation = 0;
		}
		return screenOrientation;
	}

}
