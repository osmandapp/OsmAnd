package net.osmand.plus.utils;


import android.content.Context;
import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiContext;

import net.osmand.Location;
import net.osmand.data.LatLon;
import net.osmand.plus.OsmAndLocationProvider;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.helpers.AndroidUiHelper;

import net.osmand.plus.views.DirectionDrawable;
import net.osmand.plus.widgets.style.CustomTypefaceSpan;

public class UpdateLocationUtils {

	@NonNull
	public static UpdateLocationViewCache getUpdateLocationViewCache(@NonNull @UiContext Context context) {
		return getUpdateLocationViewCache(context, true);
	}

	@NonNull
	public static UpdateLocationViewCache getUpdateLocationViewCache(@NonNull @UiContext Context context, boolean useScreenOrientation) {
		UpdateLocationViewCache viewCache = new UpdateLocationViewCache();
		if (useScreenOrientation) {
			viewCache.screenRotation = AndroidUiHelper.getScreenRotation(context);
		}
		return viewCache;
	}

	public static void updateLocationView(@NonNull OsmandApplication app, @Nullable UpdateLocationViewCache cache,
	                                      ImageView arrow, TextView txt, double toLat, double toLon) {
		updateLocationView(app, cache, arrow, txt, new LatLon(toLat, toLon));
	}

	public static void updateLocationView(@NonNull OsmandApplication app, @Nullable UpdateLocationViewCache cache,
	                                      @Nullable ImageView arrow, @Nullable TextView txt, @Nullable LatLon toLoc) {
		LatLon specialFrom = cache == null ? null : cache.specialFrom;
		UpdateLocationInfo info = new UpdateLocationInfo(app, specialFrom, toLoc);
		if (arrow != null) {
			updateDirectionDrawable(app, arrow, info, cache);
		}
		if (txt != null) {
			txt.setText(getFormattedDistance(app, info, cache));
		}
	}

	public static void updateDirectionDrawable(@NonNull OsmandApplication app, @NonNull ImageView view,
	                                           @NonNull UpdateLocationInfo info, @Nullable UpdateLocationViewCache cache) {
		if (view.getDrawable() instanceof DirectionDrawable) {
			DirectionDrawable drawable = (DirectionDrawable) view.getDrawable();
			setupDirectionDrawable(drawable, info, cache);
		} else {
			DirectionDrawable drawable = new DirectionDrawable(app, view.getWidth(), view.getHeight());
			setupDirectionDrawable(drawable, info, cache);
			view.setImageDrawable(drawable);
		}
		view.invalidate();
	}

	@NonNull
	public static CharSequence getFormattedDistance(@NonNull OsmandApplication app,
	                                                @NonNull UpdateLocationInfo info,
	                                                @Nullable UpdateLocationViewCache cache) {
		boolean hasToLocation = info.toLocation != null;
		CharSequence distance = hasToLocation ? OsmAndFormatter.getFormattedDistance(info.mes[0], app) : "";
		if (hasToLocation) {
			Integer color = getTextColor(app, cache, info.stale, info.useCenter);
			if (color != null) {
				SpannableString spannable = new SpannableString(distance);
				spannable.setSpan(new ForegroundColorSpan(color), 0, spannable.length(), 0);
				spannable.setSpan(new CustomTypefaceSpan(FontCache.getMediumFont()), 0, spannable.length(), 0);
				return spannable;
			}
		}
		return distance;
	}

	public static void setupDirectionDrawable(@NonNull DirectionDrawable drawable,
	                                          @NonNull UpdateLocationInfo info,
	                                          @Nullable UpdateLocationViewCache cache) {
		int imgColorSet = cache == null ? 0 : cache.arrowColor;
		if (imgColorSet == 0) {
			if (info.stale) {
				imgColorSet = R.color.icon_color_default_light;
			} else {
				imgColorSet = info.useCenter ? R.color.color_distance : R.color.color_myloc_distance;
			}
		}
		int arrowResId = cache == null ? 0 : cache.arrowResId;
		if (arrowResId == 0) {
			arrowResId = R.drawable.ic_direction_arrow;
		}
		drawable.setImage(arrowResId, imgColorSet);

		if (info.heading == null || info.toLocation == null) {
			drawable.setAngle(0);
		} else {
			float orientation = (cache == null ? 0 : cache.screenRotation);
			drawable.setAngle(info.mes[1] - info.heading + 180 + orientation);
		}
	}

	@ColorInt
	@Nullable
	private static Integer getTextColor(@NonNull OsmandApplication app, @Nullable UpdateLocationViewCache cache, boolean stale, boolean useCenter) {
		if (cache != null && cache.paintTxt) {
			int textColorSet = cache.textColor;
			if (textColorSet == 0) {
				if (stale) {
					textColorSet = R.color.icon_color_default_light;
				} else {
					textColorSet = useCenter ? R.color.color_distance : R.color.color_myloc_distance;
				}
			}
			return ColorUtilities.getColor(app, textColorSet);
		}
		return null;
	}

	public static class UpdateLocationInfo {
		private LatLon toLocation;
		private LatLon fromLocation;
		private Float heading;
		private final float[] mes = new float[2];
		private boolean stale;
		private boolean useCenter;

		public UpdateLocationInfo(@NonNull OsmandApplication app, @Nullable LatLon specialFrom, @Nullable LatLon toLoc) {
			toLocation = toLoc;
			fromLocation = specialFrom;
			useCenter = fromLocation != null;
			if (fromLocation == null) {
				OsmAndLocationProvider locationProvider = app.getLocationProvider();
				Location lastKnownLocation = locationProvider.getLastKnownLocation();
				heading = locationProvider.getHeading();
				if (lastKnownLocation == null) {
					lastKnownLocation = locationProvider.getLastStaleKnownLocation();
					stale = true;
				}
				if (lastKnownLocation != null) {
					fromLocation = new LatLon(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
				} else {
					useCenter = true;
					stale = false;
					fromLocation = app.getMapViewTrackingUtilities().getMapLocation();
					heading = app.getMapViewTrackingUtilities().getMapRotate();
					if (heading != null) {
						heading = -heading;
					}
				}
			}
			if (toLocation != null) {
				Location.distanceBetween(toLocation.getLatitude(), toLocation.getLongitude(),
						fromLocation.getLatitude(), fromLocation.getLongitude(), mes);
			}
		}
	}

	public static class UpdateLocationViewCache {
		public int screenRotation;
		public boolean paintTxt = true;
		public int arrowResId;
		public int arrowColor;
		public int textColor;
		public LatLon specialFrom;
	}
}
