package net.osmand.plus.views.mapwidgets.widgets;

import static net.osmand.plus.views.mapwidgets.WidgetType.COORDINATES;

import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.google.android.material.snackbar.Snackbar;
import com.jwetherell.openmap.common.LatLonPoint;
import com.jwetherell.openmap.common.MGRSPoint;
import com.jwetherell.openmap.common.ZonedUTMPoint;

import net.osmand.Location;
import net.osmand.LocationConvert;
import net.osmand.PlatformUtil;
import net.osmand.data.PointDescription;
import net.osmand.plus.OsmAndLocationProvider;
import net.osmand.plus.OsmAndLocationProvider.GPSInfo;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.mapcontextmenu.other.ShareMenu;
import net.osmand.plus.settings.backend.preferences.OsmandPreference;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.layers.MapInfoLayer;
import net.osmand.plus.views.layers.MapInfoLayer.TextState;
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings;

import org.apache.commons.logging.Log;

public class CoordinatesWidget extends MapWidget {

	private static final Log log = PlatformUtil.getLog(CoordinatesWidget.class);

	private final OsmAndLocationProvider locationProvider;
	private Location lastKnownLocation;

	private final View divider;
	private final View secondContainer;

	private final TextView firstCoordinate;
	private final TextView secondCoordinate;

	private final ImageView firstIcon;
	private final ImageView secondIcon;

	@Override
	protected int getLayoutId() {
		return R.layout.coordinates_widget;
	}

	@Nullable
	@Override
	public OsmandPreference<Boolean> getWidgetVisibilityPref() {
		return settings.SHOW_COORDINATES_WIDGET;
	}

	public CoordinatesWidget(@NonNull MapActivity mapActivity) {
		super(mapActivity, COORDINATES);

		locationProvider = app.getLocationProvider();

		divider = view.findViewById(R.id.divider);
		secondContainer = view.findViewById(R.id.second_container);

		firstCoordinate = view.findViewById(R.id.first_coordinate);
		secondCoordinate = view.findViewById(R.id.second_coordinate);

		firstIcon = view.findViewById(R.id.first_icon);
		secondIcon = view.findViewById(R.id.second_icon);

		view.setOnClickListener(v -> copyCoordinates());
		updateVisibility(false);
	}

	private void copyCoordinates() {
		if (lastKnownLocation != null) {
			String coordinates = firstCoordinate.getText().toString();
			if (secondContainer.getVisibility() == View.VISIBLE) {
				coordinates += ", " + secondCoordinate.getText().toString();
			}
			if (ShareMenu.copyToClipboard(app, coordinates)) {
				showShareSnackbar(coordinates);
			}
		}
	}

	private void showShareSnackbar(@NonNull String coordinates) {
		String clipboardText = getString(R.string.copied_to_clipboard);
		String text = getString(R.string.ltr_or_rtl_combine_via_colon, clipboardText, "")
				+ "\n" + coordinates;
		Snackbar snackbar = Snackbar.make(mapActivity.getLayout(), text, Snackbar.LENGTH_LONG)
				.setAction(R.string.shared_string_share, view -> {
					Intent intent = new Intent(Intent.ACTION_SEND);
					intent.setAction(Intent.ACTION_SEND);
					intent.putExtra(Intent.EXTRA_TEXT, coordinates);
					intent.setType("text/plain");
					Intent chooserIntent = Intent.createChooser(intent, getString(R.string.send_location));
					AndroidUtils.startActivityIfSafe(mapActivity, intent, chooserIntent);
				});
		UiUtilities.setupSnackbar(snackbar, isNightMode(), 5);
		snackbar.show();
	}

	@Override
	public void updateInfo(@Nullable DrawSettings drawSettings) {
		boolean visible = mapActivity.getWidgetsVisibilityHelper().shouldShowTopCoordinatesWidget();
		updateVisibility(visible);
		if (visible) {
			lastKnownLocation = locationProvider.getLastKnownLocation();
			if (lastKnownLocation == null) {
				showSearchingGpsMessage();
			} else {
				showFormattedCoordinates(lastKnownLocation);
			}
		}
	}

	private void showSearchingGpsMessage() {
		AndroidUiHelper.updateVisibility(firstIcon, false);
		AndroidUiHelper.updateVisibility(divider, false);
		AndroidUiHelper.updateVisibility(secondContainer, false);
		GPSInfo gpsInfo = locationProvider.getGPSInfo();
		String message = getString(R.string.searching_gps) + "… " + gpsInfo.usedSatellites + "/" + gpsInfo.foundSatellites;
		firstCoordinate.setText(message);
	}

	private void showFormattedCoordinates(@NonNull Location location) {
		int format = app.getSettings().COORDINATES_FORMAT.get();
		double lat = location.getLatitude();
		double lon = location.getLongitude();

		if (format == PointDescription.UTM_FORMAT) {
			showUtmCoordinates(lat, lon);
		} else if (format == PointDescription.MGRS_FORMAT) {
			showMgrsCoordinates(lat, lon);
		} else if (format == PointDescription.OLC_FORMAT) {
			showOlcCoordinates(lat, lon);
		} else {
			showStandardCoordinates(lat, lon, format);
		}
	}

	private void showUtmCoordinates(double lat, double lon) {
		setupForNonStandardFormat();
		ZonedUTMPoint utmPoint = new ZonedUTMPoint(new LatLonPoint(lat, lon));
		firstCoordinate.setText(utmPoint.format());
	}

	private void showMgrsCoordinates(double lat, double lon) {
		setupForNonStandardFormat();
		MGRSPoint mgrsPoint = new MGRSPoint(new LatLonPoint(lat, lon));
		firstCoordinate.setText(mgrsPoint.toFlavoredString(5));
	}

	private void showOlcCoordinates(double lat, double lon) {
		setupForNonStandardFormat();

		String olcCoordinates;
		try {
			olcCoordinates = PointDescription.getLocationOlcName(lat, lon);
		} catch (RuntimeException e) {
			log.error("Failed to define OLC location", e);
			olcCoordinates = "0, 0";
		}
		firstCoordinate.setText(olcCoordinates);
	}

	private void setupForNonStandardFormat() {
		AndroidUiHelper.updateVisibility(firstIcon, true);
		AndroidUiHelper.updateVisibility(divider, false);
		AndroidUiHelper.updateVisibility(secondContainer, false);

		int utmIconId = isNightMode()
				? R.drawable.widget_coordinates_utm_night
				: R.drawable.widget_coordinates_utm_day;
		firstIcon.setImageDrawable(iconsCache.getIcon(utmIconId));
	}

	private void showStandardCoordinates(double lat, double lon, int format) {
		AndroidUiHelper.updateVisibility(firstIcon, true);
		AndroidUiHelper.updateVisibility(divider, true);
		AndroidUiHelper.updateVisibility(secondContainer, true);

		String latitude = "";
		String longitude = "";
		try {
			latitude = LocationConvert.convertLatitude(lat, format, true);
			longitude = LocationConvert.convertLongitude(lon, format, true);
		} catch (RuntimeException e) {
			log.error("Failed to convert coordinates", e);
		}

		firstIcon.setImageDrawable(getLatitudeIcon(lat));
		secondIcon.setImageDrawable(getLongitudeIcon(lon));

		firstCoordinate.setText(latitude);
		secondCoordinate.setText(longitude);
	}

	@NonNull
	private Drawable getLatitudeIcon(double lat) {
		int latDayIconId = lat >= 0
				? R.drawable.widget_coordinates_latitude_north_day
				: R.drawable.widget_coordinates_latitude_south_day;
		int latNightIconId = lat >= 0
				? R.drawable.widget_coordinates_latitude_north_night
				: R.drawable.widget_coordinates_latitude_south_night;
		int latIconId = isNightMode() ? latNightIconId : latDayIconId;
		return iconsCache.getIcon(latIconId);
	}

	@NonNull
	private Drawable getLongitudeIcon(double lon) {
		int lonDayIconId = lon >= 0
				? R.drawable.widget_coordinates_longitude_east_day
				: R.drawable.widget_coordinates_longitude_west_day;
		int lonNightIconId = lon >= 0
				? R.drawable.widget_coordinates_longitude_east_night
				: R.drawable.widget_coordinates_longitude_west_night;
		int lonIconId = isNightMode() ? lonNightIconId : lonDayIconId;
		return iconsCache.getIcon(lonIconId);
	}

	@Override
	protected boolean updateVisibility(boolean visible) {
		boolean updatedVisibility = super.updateVisibility(visible);
		if (updatedVisibility) {
			MapInfoLayer mapInfoLayer = mapActivity.getMapLayers().getMapInfoLayer();
			if (mapInfoLayer != null) {
				mapInfoLayer.recreateTopWidgetsPanel();
			}
			mapActivity.updateStatusBarColor();
		}
		return updatedVisibility;
	}

	public void updateColors(@NonNull TextState textState) {
		super.updateColors(textState);

		divider.setBackgroundColor(ColorUtilities.getDividerColor(app, isNightMode()));

		int textColor = ContextCompat.getColor(app, R.color.activity_background_light);
		firstCoordinate.setTextColor(textColor);
		secondCoordinate.setTextColor(textColor);

		int typefaceStyle = textState.textBold ? Typeface.BOLD : Typeface.NORMAL;
		firstCoordinate.setTypeface(Typeface.DEFAULT, typefaceStyle);
		secondCoordinate.setTypeface(Typeface.DEFAULT, typefaceStyle);
	}
}