package net.osmand.plus.card.color.cstyle;

import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.shared.gpx.GpxTrackAnalysis;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.card.color.ColoringStyle;
import net.osmand.shared.routing.ColoringType;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.shared.routing.RouteColorize;
import net.osmand.shared.routing.RouteColorize.ColorizationType;

public class ColoringStyleDetailsCardController implements IColoringStyleDetailsController {

	protected final OsmandApplication app;
	protected final GpxTrackAnalysis analysis;
	protected ColoringStyle coloringStyle;
	protected ColoringStyleDetailsCard card;

	public ColoringStyleDetailsCardController(@NonNull OsmandApplication app,
	                                          @NonNull ColoringStyle coloringStyle) {
		this(app, coloringStyle, null);
	}

	public ColoringStyleDetailsCardController(@NonNull OsmandApplication app,
	                                          @NonNull ColoringStyle coloringStyle,
	                                          @Nullable GpxTrackAnalysis analysis) {
		this.app = app;
		this.coloringStyle = coloringStyle;
		this.analysis = analysis;
	}

	@Override
	public void bindCard(@NonNull ColoringStyleDetailsCard card) {
		this.card = card;
	}

	@Override
	public void setColoringStyle(@NonNull ColoringStyle coloringStyle) {
		this.coloringStyle = coloringStyle;
		if (card != null) {
			card.updateContent();
		}
	}

	@Override
	public boolean shouldHideCard() {
		ColoringType coloringType = coloringStyle.getType();
		return !coloringType.isDefault() && coloringType.isSolidSingleColor();
	}

	@Override
	public boolean shouldShowUpperSpace() {
		ColoringType coloringType = coloringStyle.getType();
		return analysis == null || !coloringType.isRouteInfoAttribute() || coloringType == ColoringType.ALTITUDE;
	}

	@Override
	public boolean shouldShowBottomSpace() {
		return analysis != null;
	}

	@Override
	public boolean shouldShowSpeedAltitudeLegend() {
		ColoringType coloringType = coloringStyle.getType();
		return coloringType == ColoringType.ALTITUDE || coloringType == ColoringType.SPEED;
	}

	@Override
	public boolean shouldShowSlopeLegend() {
		return coloringStyle.getType() == ColoringType.SLOPE;
	}

	@Nullable
	@Override
	public String getTypeDescription() {
		return null;
	}

	@Nullable
	@Override
	public CharSequence[] getLegendHeadlines() {
		ColoringType coloringType = coloringStyle.getType();
		if (isLegendDataSpecified() && coloringType.toGradientScaleType() != null) {
			ApplicationMode appMode = app.getSettings().getApplicationMode();
			ColorizationType colorizationType = coloringType.toGradientScaleType().toColorizationType();
			double min = RouteColorize.Companion.getMinValue(colorizationType, analysis);
			double max = RouteColorize.Companion.getMaxValue(colorizationType, analysis, min, appMode.getMaxSpeed());
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
		ColoringType coloringType = coloringStyle.getType();
		if (analysis != null) {
			boolean useElevationData = coloringType == ColoringType.ALTITUDE && analysis.isElevationSpecified();
			boolean useSpeedData = coloringType == ColoringType.SPEED && analysis.isSpeedSpecified();
			return useElevationData || useSpeedData;
		}
		return false;
	}

	@NonNull
	private CharSequence formatValue(double value) {
		ColoringType coloringType = coloringStyle.getType();
		if (coloringType == ColoringType.ALTITUDE) {
			return OsmAndFormatter.getFormattedAlt(value, app);
		} else if (coloringType == ColoringType.SLOPE) {
			value *= 100; // slope value in the range 0..1
			return app.getString(R.string.ltr_or_rtl_combine_via_space, String.valueOf((int) value), "%");
		}
		String speed = OsmAndFormatter.getFormattedSpeed((float) value, app);
		String speedUnit = app.getSettings().SPEED_SYSTEM.get().toShortString();
		Spannable formattedSpeed = new SpannableString(speed);
		formattedSpeed.setSpan(
				new ForegroundColorSpan(AndroidUtils.getColorFromAttr(app, android.R.attr.textColorSecondary)),
				speed.indexOf(speedUnit), speed.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		return formattedSpeed;
	}
}
