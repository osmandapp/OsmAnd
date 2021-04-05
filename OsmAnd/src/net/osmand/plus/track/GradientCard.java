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
		double min = RouteColorize.getMinValue(selectedScaleType.toColorizationType(), gpxTrackAnalysis);
		double max = RouteColorize.getMaxValue(selectedScaleType.toColorizationType(),
				gpxTrackAnalysis, min, app.getSettings().getApplicationMode().getMaxSpeed());
		minValue.setText(formatValue(min));
		maxValue.setText(formatValue(max));
	}

	public void setSelectedScaleType(GradientScaleType type) {
		this.selectedScaleType = type;
		updateContent();
	}

	private CharSequence formatValue(double value) {
		if (selectedScaleType == GradientScaleType.ALTITUDE) {
			return OsmAndFormatter.getFormattedAlt(value, app);
		} else if (selectedScaleType == GradientScaleType.SLOPE) {
			return (int) value + " %";
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