package net.osmand.plus.charts;

import static android.text.format.DateUtils.SECOND_IN_MILLIS;

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
import net.osmand.plus.settings.enums.MetricsConstants;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.utils.OsmAndFormatter.FormattedValue;
import net.osmand.util.MapUtils;

@SuppressLint("ViewConstructor")
public class GpxMarkerView extends MarkerView {

	private final View firstContainer;
	private final View secondContainer;
	private final View thirdContainer;
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

		firstContainer = findViewById(R.id.first_container);
		secondContainer = findViewById(R.id.second_container);
		thirdContainer = findViewById(R.id.third_container);
		xAxisContainer = findViewById(R.id.x_axis_container);
		divider = findViewById(R.id.divider);

		hasIcon = icon != null;
		((ImageView) findViewById(R.id.icon)).setImageDrawable(icon);
		AndroidUiHelper.updateVisibility(findViewById(R.id.icon_divider), hasIcon);
		AndroidUiHelper.updateVisibility(findViewById(R.id.icon_container), hasIcon);
	}

	@Override
	public void refreshContent(@NonNull Entry entry, @NonNull Highlight highlight) {
		ChartData<?> chartData = getChartView().getData();
		if (hasIcon && highlight instanceof GPXHighlight) {
			boolean showIcon = ((GPXHighlight) highlight).shouldShowLocationIcon();
			AndroidUiHelper.updateVisibility(findViewById(R.id.icon_divider), showIcon);
			AndroidUiHelper.updateVisibility(findViewById(R.id.icon_container), showIcon);
		}
		int dataSetCount = chartData.getDataSetCount();
		OrderedLineDataSet lastDataSet = dataSetCount > 0 ? (OrderedLineDataSet)chartData.getDataSetByIndex(dataSetCount - 1) : null;
		if (lastDataSet != null && lastDataSet.getDataSetType() == GPXDataSetType.ALTITUDE_EXTRM) {
			dataSetCount--;
		}
		if (dataSetCount == 1) {
			OrderedLineDataSet dataSet = (OrderedLineDataSet) chartData.getDataSetByIndex(0);
			updateMarker(firstContainer, dataSet, entry);
			AndroidUiHelper.updateVisibility(divider, false);
		} else if (dataSetCount == 2) {
			OrderedLineDataSet firstDataSet = (OrderedLineDataSet) chartData.getDataSetByIndex(0);
			OrderedLineDataSet secondDataSet = ((OrderedLineDataSet) chartData.getDataSetByIndex(1));
			updateMarkerWithTwoDataSets(firstDataSet, secondDataSet, entry);
		} else {
			AndroidUiHelper.setVisibility(GONE, firstContainer, secondContainer, thirdContainer, xAxisContainer, divider);
		}
		super.refreshContent(entry, highlight);
	}

	private void updateMarker(@NonNull View container, @NonNull OrderedLineDataSet dataSet, @NonNull Entry entry) {
		int value = (int) (entry.getY() + 0.5);
		String formattedValue = formatValue(value);
		updateMarker(container, dataSet, formattedValue);

		AndroidUiHelper.setVisibility(GONE, firstContainer, secondContainer, thirdContainer, xAxisContainer);
		AndroidUiHelper.updateVisibility(container, true);
		updateXAxisValue(dataSet, entry);
	}

	private void updateMarker(@NonNull View container, @NonNull OrderedLineDataSet dataSet, @NonNull String formattedValue) {
		TextView textValue = container.findViewById(R.id.text_value);
		TextView textUnits = container.findViewById(R.id.text_units);

		textValue.setText(formattedValue);
		textValue.setTextColor(dataSet.getColor());
		textUnits.setText(dataSet.getUnits());
	}

	private void updateMarkerWithTwoDataSets(@NonNull OrderedLineDataSet firstDataSet,
	                                         @NonNull OrderedLineDataSet secondDataSet,
	                                         @NonNull Entry entry) {
		OrderedLineDataSet altitudeDataSet = getDataSetByType(GPXDataSetType.ALTITUDE, firstDataSet, secondDataSet);
		OrderedLineDataSet speedDataSet = getDataSetByType(GPXDataSetType.SPEED, firstDataSet, secondDataSet);
		OrderedLineDataSet slopeDataSet = getDataSetByType(GPXDataSetType.SLOPE, firstDataSet, secondDataSet);

		updateMarkerText(firstContainer, altitudeDataSet, entry);
		updateMarkerText(secondContainer, speedDataSet, entry);
		updateMarkerText(thirdContainer, slopeDataSet, entry);

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

	private void updateMarkerText(@NonNull View container, @Nullable OrderedLineDataSet dataSet, @NonNull Entry entry) {
		if (dataSet != null) {
			float y = getInterpolatedY(dataSet, entry);
			String formattedValue = formatValue((int) (y + 0.5));
			updateMarker(container, dataSet, formattedValue);
		}
		AndroidUiHelper.updateVisibility(container, dataSet != null);
	}

	private float getInterpolatedY(@NonNull OrderedLineDataSet dataSet, @NonNull Entry entry) {
		if (dataSet.getEntryIndex(entry) == -1) {
			Entry upEntry = dataSet.getEntryForXValue(entry.getX(), Float.NaN, DataSet.Rounding.UP);
			Entry downEntry = upEntry;
			int upIndex = dataSet.getEntryIndex(upEntry);
			if (upIndex > 0) {
				downEntry = dataSet.getEntryForIndex(upIndex - 1);
			}
			return MapUtils.getInterpolatedY(downEntry.getX(), downEntry.getY(), upEntry.getX(), upEntry.getY(), entry.getX());
		} else {
			return entry.getY();
		}
	}

	private void updateXAxisValue(@NonNull OrderedLineDataSet dataSet, @NonNull Entry entry) {
		if (includeXAxisDataSet) {
			GPXDataSetAxisType xAxisType = dataSet.getDataSetAxisType();
			if (xAxisType == GPXDataSetAxisType.DISTANCE) {
				updateXAxisValueWithDistance(dataSet, entry);
			} else {
				if (xAxisType == GPXDataSetAxisType.TIME) {
					updateXAxisValueWithTime(entry);
				} else if (xAxisType == GPXDataSetAxisType.TIME_OF_DAY) {
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
		String formattedTime = ChartUtils.formatXAxisTime(seconds, useHours);
		((TextView) xAxisContainer.findViewById(R.id.x_axis_value)).setText(formattedTime);
	}

	private void updateXAxisValueWithTimeOfDay(@NonNull Entry entry) {
		long seconds = (long) entry.getX();
		long timeOfDayInSeconds = startTimeMillis + seconds * SECOND_IN_MILLIS;
		String formattedTimeOfDay = OsmAndFormatter.getFormattedFullTime(timeOfDayInSeconds);
		((TextView) xAxisContainer.findViewById(R.id.x_axis_value)).setText(formattedTimeOfDay);
	}

	@Override
	public MPPointF getOffset() {
		ChartData<?> chartData = getChartView().getData();
		int dataSetCount = chartData.getDataSetCount();
		OrderedLineDataSet lastDataSet = dataSetCount > 0 ? (OrderedLineDataSet)chartData.getDataSetByIndex(dataSetCount - 1) : null;
		if (lastDataSet != null && lastDataSet.getDataSetType() == GPXDataSetType.ALTITUDE_EXTRM) {
			dataSetCount--;
		}
		int halfDp = AndroidUtils.dpToPx(getContext(), .5f);
		if (dataSetCount == 2) {
			int x = divider.getLeft();
			return new MPPointF(-x - halfDp, 0);
		} else {
			if (dataSetCount == 1) {
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