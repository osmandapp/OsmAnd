package net.osmand.plus.track;

import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.TextView;

import net.osmand.AndroidUtils;
import net.osmand.GPXUtilities.GPXTrackAnalysis;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.routepreparationmenu.cards.MapBaseCard;
import net.osmand.router.RouteColorize;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class GradientCard extends MapBaseCard {

	private final GPXTrackAnalysis gpxTrackAnalysis;
	private GradientScaleType selectedScaleType;

	public GradientCard(@NonNull MapActivity mapActivity, @NonNull GPXTrackAnalysis gpxTrackAnalysis,
	                    @Nullable GradientScaleType selectedScaleType) {
		super(mapActivity);
		this.gpxTrackAnalysis = gpxTrackAnalysis;
		this.selectedScaleType = selectedScaleType;
	}

	public GradientCard(@NonNull MapActivity mapActivity, @Nullable GradientScaleType scaleType) {
		super(mapActivity);
		this.gpxTrackAnalysis = null;
		this.selectedScaleType = scaleType;
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.gradient_card;
	}

	@Override
	protected void updateContent() {
		if (selectedScaleType == null) {
			AndroidUiHelper.updateVisibility(view, false);
			return;
		}

		boolean isAnalysisProvided = gpxTrackAnalysis != null;
		boolean isRouteAltitude = !isAnalysisProvided && selectedScaleType == GradientScaleType.ALTITUDE;
		AndroidUiHelper.updateVisibility(view, true);
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.upper_space), isAnalysisProvided || isRouteAltitude);
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.bottom_space), isAnalysisProvided);

		View slopeLegend = view.findViewById(R.id.slope_legend);
		View speedAltitudeLegend = view.findViewById(R.id.speed_altitude_legend);

		if (selectedScaleType == GradientScaleType.SLOPE) {
			AndroidUiHelper.updateVisibility(slopeLegend, true);
			AndroidUiHelper.updateVisibility(speedAltitudeLegend, false);
			return;
		}

		AndroidUiHelper.updateVisibility(slopeLegend, false);
		AndroidUiHelper.updateVisibility(speedAltitudeLegend, true);

		TextView minValue = view.findViewById(R.id.min_value);
		TextView maxValue = view.findViewById(R.id.max_value);

		if (isAnalysisProvided) {
			AndroidUiHelper.updateVisibility(view, true);
			if (selectedScaleType == GradientScaleType.SPEED && gpxTrackAnalysis.isSpeedSpecified()
					|| selectedScaleType == GradientScaleType.ALTITUDE && gpxTrackAnalysis.isElevationSpecified()) {
				double min = RouteColorize.getMinValue(selectedScaleType.toColorizationType(), gpxTrackAnalysis);
				double max = RouteColorize.getMaxValue(selectedScaleType.toColorizationType(),
						gpxTrackAnalysis, min, app.getSettings().getApplicationMode().getMaxSpeed());
				minValue.setText(formatValue(min));
				maxValue.setText(formatValue(max));
			} else if (selectedScaleType == GradientScaleType.SPEED) {
				minValue.setText(R.string.shared_string_min_speed);
				maxValue.setText(R.string.shared_string_max_speed);
			} else if (selectedScaleType == GradientScaleType.ALTITUDE) {
				minValue.setText(R.string.shared_string_min_height);
				maxValue.setText(R.string.shared_string_max_height);
			}
		} else {
			if (selectedScaleType == GradientScaleType.ALTITUDE) {
				minValue.setText(R.string.shared_string_min_height);
				maxValue.setText(R.string.shared_string_max_height);
			}
		}
	}

	public void setSelectedScaleType(GradientScaleType type) {
		this.selectedScaleType = type;
		updateContent();
	}

	private CharSequence formatValue(double value) {
		if (selectedScaleType == GradientScaleType.ALTITUDE) {
			return OsmAndFormatter.getFormattedAlt(value, app);
		} else if (selectedScaleType == GradientScaleType.SLOPE) {
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