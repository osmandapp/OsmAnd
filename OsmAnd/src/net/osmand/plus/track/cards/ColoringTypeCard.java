package net.osmand.plus.track.cards;

import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.TextView;

import net.osmand.plus.utils.AndroidUtils;
import net.osmand.gpx.GPXTrackAnalysis;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.routepreparationmenu.cards.MapBaseCard;
import net.osmand.plus.routing.ColoringType;
import net.osmand.router.RouteColorize;
import net.osmand.router.RouteColorize.ColorizationType;

import androidx.annotation.NonNull;

public class ColoringTypeCard extends MapBaseCard {

	private final GPXTrackAnalysis gpxTrackAnalysis;
	private ColoringType coloringType;

	public ColoringTypeCard(@NonNull MapActivity mapActivity, @NonNull GPXTrackAnalysis gpxTrackAnalysis,
	                        @NonNull ColoringType coloringType) {
		super(mapActivity);
		this.gpxTrackAnalysis = gpxTrackAnalysis;
		this.coloringType = coloringType;
	}

	public ColoringTypeCard(@NonNull MapActivity mapActivity, @NonNull ColoringType coloringType) {
		super(mapActivity);
		this.gpxTrackAnalysis = null;
		this.coloringType = coloringType;
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.coloring_type_card;
	}

	@Override
	protected void updateContent() {
		if (coloringType.isSolidSingleColor()) {
			updateVisibility(false);
			return;
		}

		updateVisibility(true);
		boolean isAnalysisProvided = gpxTrackAnalysis != null;
		boolean isRouteAltitude = !isAnalysisProvided && coloringType == ColoringType.ALTITUDE;

		boolean upperSpaceVisible = !coloringType.isRouteInfoAttribute() && isAnalysisProvided || isRouteAltitude;
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.upper_space), upperSpaceVisible);
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.bottom_space), isAnalysisProvided);

		View slopeLegend = view.findViewById(R.id.slope_legend);
		View speedAltitudeLegend = view.findViewById(R.id.speed_altitude_legend);

		if (coloringType.isRouteInfoAttribute()) {
			AndroidUiHelper.setVisibility(View.GONE, slopeLegend, speedAltitudeLegend);
		} else {
			boolean isSlope = coloringType == ColoringType.SLOPE;
			AndroidUiHelper.updateVisibility(slopeLegend, isSlope);
			AndroidUiHelper.updateVisibility(speedAltitudeLegend, !isSlope);

			if (!isSlope) {
				updateSpeedAltitudeLegend(isAnalysisProvided);
			}
		}
	}

	private void updateSpeedAltitudeLegend(boolean isAnalysisProvided) {
		TextView minValue = view.findViewById(R.id.min_value);
		TextView maxValue = view.findViewById(R.id.max_value);

		if (isAnalysisProvided) {
			if (coloringType == ColoringType.SPEED && gpxTrackAnalysis.isSpeedSpecified()
					|| coloringType == ColoringType.ALTITUDE && gpxTrackAnalysis.isElevationSpecified()) {
				ColorizationType colorizationType = coloringType.toGradientScaleType().toColorizationType();
				double min = RouteColorize.getMinValue(colorizationType, gpxTrackAnalysis);
				double max = RouteColorize.getMaxValue(colorizationType, gpxTrackAnalysis, min,
						app.getSettings().getApplicationMode().getMaxSpeed());
				minValue.setText(formatValue(min));
				maxValue.setText(formatValue(max));
			} else if (coloringType == ColoringType.SPEED) {
				minValue.setText(R.string.shared_string_min_speed);
				maxValue.setText(R.string.shared_string_max_speed);
			} else if (coloringType == ColoringType.ALTITUDE) {
				minValue.setText(R.string.shared_string_min_height);
				maxValue.setText(R.string.shared_string_max_height);
			}
		} else {
			if (coloringType == ColoringType.ALTITUDE) {
				minValue.setText(R.string.shared_string_min_height);
				maxValue.setText(R.string.shared_string_max_height);
			}
		}
	}

	public void setColoringType(@NonNull ColoringType coloringType) {
		this.coloringType = coloringType;
		updateContent();
	}

	private CharSequence formatValue(double value) {
		if (coloringType == ColoringType.ALTITUDE) {
			return OsmAndFormatter.getFormattedAlt(value, app);
		} else if (coloringType == ColoringType.SLOPE) {
			value *= 100; // slope value in the range 0..1
			return app.getString(R.string.ltr_or_rtl_combine_via_space, String.valueOf((int) value),  "%");
		}
		String speed = OsmAndFormatter.getFormattedSpeed((float) value, app);
		String speedUnit = app.getSettings().SPEED_SYSTEM.get().toShortString(app);
		Spannable formattedSpeed = new SpannableString(speed);
		formattedSpeed.setSpan(
				new ForegroundColorSpan(AndroidUtils.getColorFromAttr(app, android.R.attr.textColorSecondary)),
				speed.indexOf(speedUnit), speed.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		return formattedSpeed;
	}
}