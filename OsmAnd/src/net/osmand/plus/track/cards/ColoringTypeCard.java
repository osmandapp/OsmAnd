package net.osmand.plus.track.cards;

import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import net.osmand.gpx.GPXTrackAnalysis;
import net.osmand.plus.R;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.routepreparationmenu.cards.BaseCard;
import net.osmand.plus.routing.ColoringType;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.router.RouteColorize;
import net.osmand.router.RouteColorize.ColorizationType;

public class ColoringTypeCard extends BaseCard {

	private final GPXTrackAnalysis analysis;
	private ColoringType coloringType;

	public ColoringTypeCard(@NonNull FragmentActivity activity,
	                        @Nullable GPXTrackAnalysis analysis,
	                        @NonNull ColoringType coloringType) {
		super(activity);
		this.analysis = analysis;
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
		boolean isAnalysisProvided = analysis != null;
		boolean isRouteAltitude = coloringType == ColoringType.ALTITUDE;

		boolean upperSpaceVisible = !coloringType.isRouteInfoAttribute() || isRouteAltitude || !isAnalysisProvided;
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

		if (isAnalysisProvided && (coloringType == ColoringType.SPEED && analysis.isSpeedSpecified()
				|| coloringType == ColoringType.ALTITUDE && analysis.isElevationSpecified())) {
			ColorizationType colorizationType = coloringType.toGradientScaleType().toColorizationType();
			double min = RouteColorize.getMinValue(colorizationType, analysis);
			double max = RouteColorize.getMaxValue(colorizationType, analysis, min,
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
			return app.getString(R.string.ltr_or_rtl_combine_via_space, String.valueOf((int) value), "%");
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