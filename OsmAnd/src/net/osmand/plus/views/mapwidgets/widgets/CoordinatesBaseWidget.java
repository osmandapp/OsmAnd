package net.osmand.plus.views.mapwidgets.widgets;

import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.snackbar.Snackbar;
import com.jwetherell.openmap.common.LatLonPoint;
import com.jwetherell.openmap.common.MGRSPoint;
import com.jwetherell.openmap.common.ZonedUTMPoint;

import net.osmand.LocationConvert;
import net.osmand.PlatformUtil;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.R;
import net.osmand.plus.SwissGridApproximation;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.mapcontextmenu.other.ShareMenu;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.layers.MapInfoLayer;
import net.osmand.plus.views.layers.MapInfoLayer.TextState;
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.mapwidgets.WidgetType;
import net.osmand.plus.views.mapwidgets.WidgetsPanel;

import org.apache.commons.logging.Log;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public abstract class CoordinatesBaseWidget extends MapWidget {
	private static final Log log = PlatformUtil.getLog(CoordinatesMapCenterWidget.class);

	protected LatLon lastLocation;

	protected final View divider;
	protected View firstContainer;
	protected View secondContainer;

	protected TextView firstCoordinate;
	protected TextView secondCoordinate;

	private String firstCoordinateText = "";
	private String secondCoordinateText = "";

	protected ImageView firstIcon;
	protected ImageView secondIcon;

	private boolean cachedLayoutRtl;

	protected int getLayoutId() {
		return R.layout.coordinates_widget;
	}

	public CoordinatesBaseWidget(@NonNull MapActivity mapActivity, WidgetType widgetType) {
		super(mapActivity, widgetType);

		divider = view.findViewById(R.id.divider);
		updateViewIds(isLayoutRtl());

		view.setOnClickListener(v -> copyCoordinates());
		updateVisibility(false);
	}

	@Override
	public void updateInfo(@Nullable DrawSettings drawSettings) {
		checkLayoutDirection();
	}

	private void checkLayoutDirection() {
		boolean isLayoutRtl = isLayoutRtl();
		if (cachedLayoutRtl != isLayoutRtl) {
			cachedLayoutRtl = isLayoutRtl;
			updateViewIds(isLayoutRtl);
		}
	}

	private void updateViewIds(boolean isLayoutRtl) {
		if (isLayoutRtl) {
			firstContainer = view.findViewById(R.id.second_container);
			secondContainer = view.findViewById(R.id.first_coordinates_container);

			firstCoordinate = view.findViewById(R.id.second_coordinate);
			secondCoordinate = view.findViewById(R.id.first_coordinate);

			firstIcon = view.findViewById(R.id.second_icon);
			secondIcon = view.findViewById(R.id.first_icon);
		} else {
			firstContainer = view.findViewById(R.id.first_coordinates_container);
			secondContainer = view.findViewById(R.id.second_container);

			firstCoordinate = view.findViewById(R.id.first_coordinate);
			secondCoordinate = view.findViewById(R.id.second_coordinate);

			firstIcon = view.findViewById(R.id.first_icon);
			secondIcon = view.findViewById(R.id.second_icon);
		}
	}

	protected void copyCoordinates() {
		if (lastLocation != null) {
			String coordinates = firstCoordinateText;
			if (secondContainer.getVisibility() == View.VISIBLE) {
				coordinates += ", " + secondCoordinateText;
			}
			if (ShareMenu.copyToClipboard(app, coordinates)) {
				showShareSnackbar(OsmAndFormatter.markLTR(coordinates));
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

	protected void showFormattedCoordinates(double lat, double lon) {
		int format = app.getSettings().COORDINATES_FORMAT.get();
		lastLocation = new LatLon(lat, lon);

		if (format == PointDescription.UTM_FORMAT) {
			showUtmCoordinates(lat, lon);
		} else if (format == PointDescription.MGRS_FORMAT) {
			showMgrsCoordinates(lat, lon);
		} else if (format == PointDescription.OLC_FORMAT) {
			showOlcCoordinates(lat, lon);
		} else if(format == PointDescription.SWISS_GRID_FORMAT){
			showSwissGrid(lat, lon, false);
		} else if (format == PointDescription.SWISS_GRID_PLUS_FORMAT){
			showSwissGrid(lat, lon, true);
		} else {
			showStandardCoordinates(lat, lon, format);
		}
	}

	private void showUtmCoordinates(double lat, double lon) {
		setupForNonStandardFormat();
		ZonedUTMPoint utmPoint = new ZonedUTMPoint(new LatLonPoint(lat, lon));
		setFirstCoordinateText(utmPoint.format());
	}

	private void showMgrsCoordinates(double lat, double lon) {
		setupForNonStandardFormat();
		MGRSPoint mgrsPoint = new MGRSPoint(new LatLonPoint(lat, lon));
		setFirstCoordinateText(mgrsPoint.toFlavoredString(5));
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
		setFirstCoordinateText(olcCoordinates);
	}

	private void showSwissGrid(double lat, double lon, boolean swissGridPlus){
		LatLon latLon = new LatLon(lat, lon);
		double[] swissGrid = swissGridPlus
				? SwissGridApproximation.convertWGS84ToLV95(latLon)
				: SwissGridApproximation.convertWGS84ToLV03(latLon);
		DecimalFormatSymbols formatSymbols = new DecimalFormatSymbols(Locale.US);
		formatSymbols.setDecimalSeparator('.');
		formatSymbols.setGroupingSeparator(' ');
		DecimalFormat swissGridFormat = new DecimalFormat("###,###.##", formatSymbols);

		firstIcon.setImageDrawable(getLatitudeIcon(lat));
		secondIcon.setImageDrawable(getLongitudeIcon(lon));

		setFirstCoordinateText(swissGridFormat.format(swissGrid[0]));
		setSecondCoordinateText(swissGridFormat.format(swissGrid[1]));
	}

	private void setupForNonStandardFormat() {
		AndroidUiHelper.updateVisibility(firstIcon, true);
		AndroidUiHelper.updateVisibility(divider, false);
		AndroidUiHelper.updateVisibility(firstContainer, true);
		AndroidUiHelper.updateVisibility(secondContainer, false);

		firstIcon.setImageDrawable(getUtmIcon());
	}

	private void showStandardCoordinates(double lat, double lon, int format) {
		AndroidUiHelper.updateVisibility(firstIcon, true);
		AndroidUiHelper.updateVisibility(divider, true);
		AndroidUiHelper.updateVisibility(firstContainer, true);
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

		setFirstCoordinateText(latitude);
		setSecondCoordinateText(longitude);
	}

	protected void setFirstCoordinateText(@NonNull String text) {
		firstCoordinateText = text;
		setCoordinateText(firstCoordinate, text);
	}

	protected void setSecondCoordinateText(@NonNull String text) {
		secondCoordinateText = text;
		setCoordinateText(secondCoordinate, text);
	}

	private void setCoordinateText(@NonNull TextView textView, @NonNull String text) {
		AndroidUtils.setTruncatedText(textView, OsmAndFormatter.markLTR(text));
	}

	protected void setCoordinateIcon(@NonNull ImageView imageView, @NonNull Drawable drawable) {
		imageView.setImageDrawable(drawable);
	}

	@NonNull
	protected Drawable getUtmIcon(){
		int utmIconId = isNightMode()
				? R.drawable.widget_coordinates_utm_night
				: R.drawable.widget_coordinates_utm_day;
		return iconsCache.getIcon(utmIconId);
	}

	@NonNull
	protected Drawable getLatitudeIcon(double lat) {
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
	protected Drawable getLongitudeIcon(double lon) {
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
		if (updatedVisibility && widgetType.getPanel(settings) == WidgetsPanel.TOP) {
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
		checkLayoutDirection();

		divider.setBackgroundColor(ColorUtilities.getDividerColor(app, isNightMode()));
		int textColor = textState.textColor;
		firstCoordinate.setTextColor(textColor);
		secondCoordinate.setTextColor(textColor);

		int typefaceStyle = textState.textBold ? Typeface.BOLD : Typeface.NORMAL;
		firstCoordinate.setTypeface(Typeface.DEFAULT, typefaceStyle);
		secondCoordinate.setTypeface(Typeface.DEFAULT, typefaceStyle);

		view.setBackgroundResource(textState.widgetBackgroundId);
		updateInfo(null);
	}

	private boolean isLayoutRtl() {
		return AndroidUtils.isLayoutMirrored(view);
	}
}
