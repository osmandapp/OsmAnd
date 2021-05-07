package net.osmand.plus.track;

import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.widget.TextView;

import net.osmand.AndroidUtils;
import net.osmand.GPXUtilities.GPXTrackAnalysis;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.routepreparationmenu.cards.BaseCard;
import net.osmand.router.RouteColorize;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class GradientCard extends BaseCard {

	private final GPXTrackAnalysis gpxTrackAnalysis;
	private GradientScaleType selectedScaleType;

	private final int minSlope = 0;
	private final int maxSlope = 60;

	public GradientCard(@NonNull MapActivity mapActivity, @NonNull GPXTrackAnalysis gpxTrackAnalysis, @Nullable GradientScaleType selectedScaleType) {
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

		TextView minValue = view.findViewById(R.id.min_value);
		TextView maxValue = view.findViewById(R.id.max_value);

		if (gpxTrackAnalysis != null) {
			AndroidUiHelper.updateVisibility(view, true);
			double min = RouteColorize.getMinValue(selectedScaleType.toColorizationType(), gpxTrackAnalysis);
			double max = RouteColorize.getMaxValue(selectedScaleType.toColorizationType(),
					gpxTrackAnalysis, min, app.getSettings().getApplicationMode().getMaxSpeed());
			minValue.setText(formatValue(min));
			maxValue.setText(formatValue(max));
			AndroidUiHelper.updateVisibility(view.findViewById(R.id.space), true);
		} else {
			if (selectedScaleType == GradientScaleType.ALTITUDE) {
				minValue.setText(R.string.shared_string_min_height);
				maxValue.setText(R.string.shared_string_max_height);
			} else if (selectedScaleType == GradientScaleType.SLOPE) {
				minValue.setText(formatValue(minSlope));
				maxValue.setText(formatValue(maxSlope));
			}
			AndroidUiHelper.updateVisibility(view.findViewById(R.id.space), false);
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