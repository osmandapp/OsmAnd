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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class GradientCard extends BaseCard {

	private GPXTrackAnalysis gpxTrackAnalysis;
	private GradientScaleType selectedScaleType;

	public GradientCard(@NonNull MapActivity mapActivity, @NonNull GPXTrackAnalysis gpxTrackAnalysis, @Nullable GradientScaleType selectedScaleType) {
		super(mapActivity);
		this.gpxTrackAnalysis = gpxTrackAnalysis;
		this.selectedScaleType = selectedScaleType;
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

		AndroidUiHelper.updateVisibility(view, true);
		TextView minValue = view.findViewById(R.id.min_value);
		TextView maxValue = view.findViewById(R.id.max_value);
		float min = getMinValue();
		float max = getMaxValue(min);
		minValue.setText(formatValue(min));
		maxValue.setText(formatValue(max));
	}

	public void setSelectedScaleType(GradientScaleType type) {
		this.selectedScaleType = type;
		updateContent();
	}

	private float getMinValue() {
		return (float) (selectedScaleType == GradientScaleType.ALTITUDE ? gpxTrackAnalysis.minElevation : 0.0);
	}

	private float getMaxValue(float minValue) {
		if (selectedScaleType == GradientScaleType.SPEED) {
			return (Math.max(gpxTrackAnalysis.maxSpeed, app.getSettings().getApplicationMode().getMaxSpeed()));
		} else if (selectedScaleType == GradientScaleType.ALTITUDE) {
			return (float) Math.max(gpxTrackAnalysis.maxElevation, minValue + 50);
		} else {
			return 25;
		}
	}

	private CharSequence formatValue(float value) {
		if (selectedScaleType == GradientScaleType.ALTITUDE) {
			return OsmAndFormatter.getFormattedAlt(value, app);
		} else if (selectedScaleType == GradientScaleType.SLOPE) {
			return (int) value + " %";
		}
		String speed = OsmAndFormatter.getFormattedSpeed(value, app);
		String speedUnit = app.getSettings().SPEED_SYSTEM.get().toShortString(app);
		Spannable formattedSpeed = new SpannableString(speed);
		formattedSpeed.setSpan(
				new ForegroundColorSpan(AndroidUtils.getColorFromAttr(app, android.R.attr.textColorSecondary)),
				speed.indexOf(speedUnit), speed.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		return formattedSpeed;
	}
}