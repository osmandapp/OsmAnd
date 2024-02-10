package net.osmand.plus.card.color;

import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.gpx.GPXTrackAnalysis;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.routing.ColoringType;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.router.RouteColorize;
import net.osmand.router.RouteColorize.ColorizationType;

public class ColoringTypeCardController implements IColoringTypeCardController {

	protected final OsmandApplication app;
	protected final GPXTrackAnalysis trackAnalysis;
	protected ColoringInfo coloringInfo;
	protected ColoringTypeCard card;

	public ColoringTypeCardController(@NonNull OsmandApplication app,
	                                  @NonNull ColoringInfo coloringInfo,
	                                  @Nullable GPXTrackAnalysis trackAnalysis) {
		this.app = app;
		this.coloringInfo = coloringInfo;
		this.trackAnalysis = trackAnalysis;
	}

	@Override
	public void bindCard(@NonNull ColoringTypeCard card) {
		this.card = card;
	}

	public void setColoringInfo(@NonNull ColoringInfo coloringInfo) {
		this.coloringInfo = coloringInfo;
		if (card != null) {
			card.updateContent();
		}
	}

	@Override
	public boolean shouldHideCard() {
		return coloringInfo.getColoringType().isSolidSingleColor();
	}

	@Override
	public boolean shouldShowUpperSpace() {
		ColoringType coloringType = coloringInfo.getColoringType();
		return trackAnalysis == null || !coloringType.isRouteInfoAttribute() || coloringType == ColoringType.ALTITUDE;
	}

	@Override
	public boolean shouldShowBottomSpace() {
		return trackAnalysis != null;
	}

	@Override
	public boolean shouldShowSpeedAltitudeLegend() {
		ColoringType coloringType = coloringInfo.getColoringType();
		return coloringType == ColoringType.ALTITUDE || coloringType == ColoringType.SPEED;
	}

	@Override
	public boolean shouldShowSlopeLegend() {
		return coloringInfo.getColoringType() == ColoringType.SLOPE;
	}

	@Nullable
	@Override
	public String getTypeDescription() {
		return null;
	}

	@Nullable
	@Override
	public CharSequence[] getLegendHeadlines() {
		ColoringType coloringType = coloringInfo.getColoringType();
		if (isLegendDataSpecified() && coloringType.toGradientScaleType() != null) {
			ApplicationMode appMode = app.getSettings().getApplicationMode();
			ColorizationType colorizationType = coloringType.toGradientScaleType().toColorizationType();
			double min = RouteColorize.getMinValue(colorizationType, trackAnalysis);
			double max = RouteColorize.getMaxValue(colorizationType, trackAnalysis, min, appMode.getMaxSpeed());
			return new CharSequence[] { formatValue(min), formatValue(max) };
		} else if (coloringType == ColoringType.SPEED) {
			return new CharSequence[] {
					app.getString(R.string.shared_string_min_speed),
					app.getString(R.string.shared_string_max_speed)
			};
		} else if (coloringType == ColoringType.ALTITUDE) {
			return new CharSequence[] {
					app.getString(R.string.shared_string_min_height),
					app.getString(R.string.shared_string_max_height)
			};
		}
		return null;
	}

	private boolean isLegendDataSpecified() {
		ColoringType coloringType = coloringInfo.getColoringType();
		if (trackAnalysis != null) {
			boolean useElevationData = coloringType == ColoringType.ALTITUDE && trackAnalysis.isElevationSpecified();
			boolean useSpeedData = coloringType == ColoringType.SPEED && trackAnalysis.isSpeedSpecified();
			return useElevationData || useSpeedData;
		}
		return false;
	}

	@NonNull
	private CharSequence formatValue(double value) {
		ColoringType coloringType = coloringInfo.getColoringType();
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
