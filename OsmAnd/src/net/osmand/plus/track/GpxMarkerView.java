package net.osmand.plus.track;

import static net.osmand.plus.helpers.GpxUiHelper.SECOND_IN_MILLIS;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.github.mikephil.charting.components.MarkerView;
import com.github.mikephil.charting.data.ChartData;
import com.github.mikephil.charting.data.DataSet;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.utils.MPPointF;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.helpers.GpxUiHelper;
import net.osmand.plus.helpers.GpxUiHelper.GPXDataSetAxisType;
import net.osmand.plus.helpers.GpxUiHelper.GPXDataSetType;
import net.osmand.plus.helpers.GpxUiHelper.GPXHighlight;
import net.osmand.plus.helpers.GpxUiHelper.OrderedLineDataSet;
import net.osmand.plus.settings.enums.MetricsConstants;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.utils.OsmAndFormatter.FormattedValue;
import net.osmand.util.MapUtils;

@SuppressLint("ViewConstructor")
public class GpxMarkerView extends MarkerView {

	private final View altitudeContainer;
	private final View speedContainer;
	private final View slopeContainer;
	private final View xAxisContainer;

	private final View divider;

	private final boolean hasIcon;
	private final long startTimeMillis;
	private final boolean useHours;
	private final boolean includeXAxisDataSet;

	public GpxMarkerView(@NonNull Context context, @Nullable Drawable icon) {
		this(context, icon, 0, false, false);
	}

	public GpxMarkerView(@NonNull Context context, long startTimeMillis, boolean useHours) {
		this(context, null, startTimeMillis, useHours, true);
	}

	private GpxMarkerView(@NonNull Context context,
	                      @Nullable Drawable icon,
	                      long startTimeMillis,
	                      boolean useHours,
	                      boolean includeXAxisDataSet) {
		super(context, R.layout.chart_marker_view);
		this.startTimeMillis = startTimeMillis;
		this.useHours = useHours;
		this.includeXAxisDataSet = includeXAxisDataSet;

		altitudeContainer = findViewById(R.id.altitude_container);
		speedContainer = findViewById(R.id.speed_container);
		xAxisContainer = findViewById(R.id.x_axis_container);
		slopeContainer = findViewById(R.id.slope_container);
		divider = findViewById(R.id.divider);

		if (icon != null) {
			AndroidUiHelper.updateVisibility(findViewById(R.id.icon_divider), true);
			AndroidUiHelper.updateVisibility(findViewById(R.id.icon_container), true);
			((ImageView) findViewById(R.id.icon)).setImageDrawable(icon);
		}

		hasIcon = icon != null;
	}

	@Override
	public void refreshContent(@NonNull Entry entry, @NonNull Highlight highlight) {
		ChartData<?> chartData = getChartView().getData();
		if (hasIcon && highlight instanceof GPXHighlight) {
			boolean showIcon = ((GPXHighlight) highlight).shouldShowIcon();
			AndroidUiHelper.updateVisibility(findViewById(R.id.icon_divider), showIcon);
			AndroidUiHelper.updateVisibility(findViewById(R.id.icon_container), showIcon);
		}
		if (chartData.getDataSetCount() == 1) {
			OrderedLineDataSet dataSet = (OrderedLineDataSet) chartData.getDataSetByIndex(0);
			updateMarkerWithOneDataSet(dataSet, entry);
		} else if (chartData.getDataSetCount() == 2) {
			OrderedLineDataSet firstDataSet = (OrderedLineDataSet) chartData.getDataSetByIndex(0);
			OrderedLineDataSet secondDataSet = ((OrderedLineDataSet) chartData.getDataSetByIndex(1));
			updateMarkerWithTwoDataSets(firstDataSet, secondDataSet, entry);
		} else {
			AndroidUiHelper.setVisibility(GONE,
					altitudeContainer, speedContainer, slopeContainer, xAxisContainer, divider);
		}
		super.refreshContent(entry, highlight);
	}

	private void updateMarkerWithOneDataSet(@NonNull OrderedLineDataSet dataSet, @NonNull Entry entry) {
		int value = (int) (entry.getY() + 0.5);
		String units = dataSet.getUnits();
		GPXDataSetType dataSetType = dataSet.getDataSetType();
		if (dataSetType == GPXDataSetType.ALTITUDE) {
			updateAltitudeMarker(dataSet, entry, value, units);
		} else if (dataSetType == GPXDataSetType.SPEED) {
			updateSpeedMarker(dataSet, entry, value, units);
		} else if (dataSetType == GPXDataSetType.SLOPE) {
			updateSlopeMarker(dataSet, entry, value);
		}
		AndroidUiHelper.updateVisibility(divider, false);
	}

	private void updateAltitudeMarker(OrderedLineDataSet dataSet, Entry entry, int altitude, @NonNull String units) {
		String formattedAltitude = formatValue(altitude);
		((TextView) altitudeContainer.findViewById(R.id.text_alt_value)).setText(formattedAltitude);
		((TextView) altitudeContainer.findViewById(R.id.text_alt_units)).setText(units);
		AndroidUiHelper.updateVisibility(altitudeContainer, true);
		AndroidUiHelper.setVisibility(GONE, speedContainer, slopeContainer, xAxisContainer);
		updateXAxisValue(dataSet, entry);
	}

	private void updateSpeedMarker(@NonNull OrderedLineDataSet dataSet, @NonNull Entry entry, int value, String units) {
		String formattedSpeed = formatValue(value);
		int textColor = dataSet.getColor();

		((TextView) speedContainer.findViewById(R.id.text_spd_value)).setTextColor(textColor);
		((TextView) speedContainer.findViewById(R.id.text_spd_value)).setText(formattedSpeed);
		((TextView) speedContainer.findViewById(R.id.text_spd_units)).setText(units);

		AndroidUiHelper.updateVisibility(speedContainer, true);
		AndroidUiHelper.setVisibility(GONE, altitudeContainer, slopeContainer);

		updateXAxisValue(dataSet, entry);
	}

	private void updateXAxisValue(@NonNull OrderedLineDataSet dataSet, @NonNull Entry entry) {
		if (includeXAxisDataSet) {
			GPXDataSetAxisType xAxisType = dataSet.getDataSetAxisType();
			if (xAxisType == GPXDataSetAxisType.DISTANCE) {
				updateXAxisValueWithDistance(dataSet, entry);
			} else {
				if (xAxisType == GPXDataSetAxisType.TIME) {
					updateXAxisValueWithTime(entry);
				} else if (xAxisType == GPXDataSetAxisType.TIMEOFDAY) {
					updateXAxisValueWithTimeOfDay(entry);
				}
				AndroidUiHelper.updateVisibility(xAxisContainer.findViewById(R.id.x_axis_unit), false);
			}
			AndroidUiHelper.updateVisibility(xAxisContainer, true);
		} else {
			AndroidUiHelper.updateVisibility(xAxisContainer, false);
		}
	}

	@SuppressLint("SetTextI18n")
	private void updateXAxisValueWithDistance(@NonNull OrderedLineDataSet dataSet, @NonNull Entry entry) {
		OsmandApplication app = getMyApplication();
		float meters = entry.getX() * dataSet.getDivX();
		MetricsConstants metricsConstants = app.getSettings().METRIC_SYSTEM.get();
		FormattedValue formattedDistance = OsmAndFormatter.getFormattedDistanceValue(meters, app,
				true, metricsConstants);

		TextView xAxisValueText = xAxisContainer.findViewById(R.id.x_axis_value);
		TextView xAxisUnitText = xAxisContainer.findViewById(R.id.x_axis_unit);

		xAxisValueText.setText(formattedDistance.value + " ");
		xAxisUnitText.setText(formattedDistance.unit);

		AndroidUiHelper.updateVisibility(xAxisUnitText, true);
	}

	private void updateXAxisValueWithTime(@NonNull Entry entry) {
		int seconds = (int) (entry.getX() + 0.5);
		String formattedTime = GpxUiHelper.formatXAxisTime(seconds, useHours);
		((TextView) xAxisContainer.findViewById(R.id.x_axis_value)).setText(formattedTime);
	}

	private void updateXAxisValueWithTimeOfDay(@NonNull Entry entry) {
		long seconds = (long) entry.getX();
		long timeOfDayInSeconds = startTimeMillis + seconds * SECOND_IN_MILLIS;
		String formattedTimeOfDay = OsmAndFormatter.getFormattedFullTime(timeOfDayInSeconds);
		((TextView) xAxisContainer.findViewById(R.id.x_axis_value)).setText(formattedTimeOfDay);
	}

	private void updateSlopeMarker(OrderedLineDataSet dataSet, Entry entry, int slope) {
		String formattedSlope = formatValue(slope);
		((TextView) slopeContainer.findViewById(R.id.text_slp_value)).setText(formattedSlope);
		AndroidUiHelper.updateVisibility(slopeContainer, true);
		AndroidUiHelper.setVisibility(GONE, altitudeContainer, speedContainer, xAxisContainer);
		updateXAxisValue(dataSet, entry);
	}

	private void updateMarkerWithTwoDataSets(@NonNull OrderedLineDataSet firstDataSet,
	                                         @NonNull OrderedLineDataSet secondDataSet,
	                                         @NonNull Entry entry) {
		OrderedLineDataSet altitudeDataSet = getDataSetByType(GPXDataSetType.ALTITUDE, firstDataSet, secondDataSet);
		OrderedLineDataSet speedDataSet = getDataSetByType(GPXDataSetType.SPEED, firstDataSet, secondDataSet);
		OrderedLineDataSet slopeDataSet = getDataSetByType(GPXDataSetType.SLOPE, firstDataSet, secondDataSet);

		updateAltitudeText(altitudeDataSet, entry);
		updateSpeedText(speedDataSet, entry);
		updateSlopeText(slopeDataSet, entry);

		AndroidUiHelper.updateVisibility(xAxisContainer, false);
		AndroidUiHelper.updateVisibility(divider, true);
		updateXAxisValue(firstDataSet, entry);
	}

	@Nullable
	private OrderedLineDataSet getDataSetByType(@NonNull GPXDataSetType dataSetType,
	                                            @NonNull OrderedLineDataSet firstDataSet,
	                                            @NonNull OrderedLineDataSet secondDataSet) {
		if (dataSetType == secondDataSet.getDataSetType()) {
			return secondDataSet;
		} else if (dataSetType == firstDataSet.getDataSetType()) {
			return firstDataSet;
		} else {
			return null;
		}
	}

	private void updateAltitudeText(@Nullable OrderedLineDataSet altitudeDataSet, @NonNull Entry entry) {
		if (altitudeDataSet != null) {
			float y = getInterpolatedY(altitudeDataSet, entry);
			String formattedAltitude = formatValue((int) (y + 0.5));
			((TextView) altitudeContainer.findViewById(R.id.text_alt_value)).setText(formattedAltitude);
			((TextView) altitudeContainer.findViewById(R.id.text_alt_units)).setText(altitudeDataSet.getUnits());
		}
		AndroidUiHelper.updateVisibility(altitudeContainer, altitudeDataSet != null);
	}

	private void updateSpeedText(@Nullable OrderedLineDataSet speedDataSet, @NonNull Entry entry) {
		if (speedDataSet != null) {
			float y = getInterpolatedY(speedDataSet, entry);
			String formattedSpeed = formatValue((int) (y + 0.5));
			((TextView) speedContainer.findViewById(R.id.text_spd_value)).setTextColor(speedDataSet.getColor());
			((TextView) speedContainer.findViewById(R.id.text_spd_value)).setText(formattedSpeed);
			((TextView) speedContainer.findViewById(R.id.text_spd_units)).setText(speedDataSet.getUnits());
		}
		AndroidUiHelper.updateVisibility(speedContainer, speedDataSet != null);
	}

	private void updateSlopeText(@Nullable OrderedLineDataSet slopeDataSet, @NonNull Entry entry) {
		if (slopeDataSet != null) {
			float y = getInterpolatedY(slopeDataSet, entry);
			String formattedSlope = formatValue((int) (y + 0.5));
			((TextView) slopeContainer.findViewById(R.id.text_slp_value)).setText(formattedSlope);
		}
		AndroidUiHelper.updateVisibility(slopeContainer, slopeDataSet != null);
	}

	private float getInterpolatedY(@NonNull OrderedLineDataSet ds, @NonNull Entry e) {
		if (ds.getEntryIndex(e) == -1) {
			Entry upEntry = ds.getEntryForXValue(e.getX(), Float.NaN, DataSet.Rounding.UP);
			Entry downEntry = upEntry;
			int upIndex = ds.getEntryIndex(upEntry);
			if (upIndex > 0) {
				downEntry = ds.getEntryForIndex(upIndex - 1);
			}
			return MapUtils.getInterpolatedY(downEntry.getX(), downEntry.getY(), upEntry.getX(), upEntry.getY(), e.getX());
		} else {
			return e.getY();
		}
	}

	@Override
	public MPPointF getOffset() {
		ChartData<?> chartData = getChartView().getData();
		int dataSetsCount = chartData.getDataSetCount();
		int halfDp = AndroidUtils.dpToPx(getContext(), .5f);
		if (dataSetsCount == 2) {
			int x = divider.getLeft();
			return new MPPointF(-x - halfDp, 0);
		} else {
			if (dataSetsCount == 1) {
				OrderedLineDataSet dataSet = (OrderedLineDataSet) chartData.getDataSetByIndex(0);
				if (dataSet.getDataSetType() == GPXDataSetType.SPEED && includeXAxisDataSet) {
					int x = xAxisContainer.getLeft();
					return new MPPointF(-x - halfDp, 0);
				}
			}
			return new MPPointF(-getWidth() / 2f, 0);
		}
	}

	@Override
	public MPPointF getOffsetForDrawingAtPoint(float posX, float posY) {
		int margin = AndroidUtils.dpToPx(getContext(), 3f);
		MPPointF offset = getOffset();
		offset.y = -posY;
		if (posX + offset.x - margin < 0) {
			offset.x -= (offset.x + posX - margin);
		}
		if (posX + offset.x + getWidth() + margin > getChartView().getWidth()) {
			offset.x -= (getWidth() - (getChartView().getWidth() - posX) + offset.x) + margin;
		}
		return offset;
	}

	@NonNull
	private String formatValue(int value) {
		return OsmAndFormatter.formatIntegerValue(value, "", getMyApplication()).value + " ";
	}

	@NonNull
	private OsmandApplication getMyApplication() {
		return ((OsmandApplication) getContext().getApplicationContext());
	}
}