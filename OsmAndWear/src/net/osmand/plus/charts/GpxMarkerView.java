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
import net.osmand.shared.settings.enums.MetricsConstants;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.utils.OsmAndFormatter.FormattedValue;
import net.osmand.util.MapUtils;

@SuppressLint("ViewConstructor")
public class GpxMarkerView extends MarkerView {

	private final View firstYAxisContainer;
	private final View secondYAxisContainer;
	private final View xAxisContainer;

	private final boolean hasIcon;
	private final long startTimeMillis;
	private final boolean useHours;
	private final boolean showXAxisValue;

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
	                      boolean showXAxisValue) {
		super(context, R.layout.chart_marker_view);
		this.startTimeMillis = startTimeMillis;
		this.useHours = useHours;
		this.showXAxisValue = showXAxisValue;

		firstYAxisContainer = findViewById(R.id.first_container);
		secondYAxisContainer = findViewById(R.id.second_container);
		xAxisContainer = findViewById(R.id.x_axis_container);

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
		OrderedLineDataSet lastDataSet = dataSetCount > 0 ? (OrderedLineDataSet) chartData.getDataSetByIndex(dataSetCount - 1) : null;
		if (lastDataSet != null && lastDataSet.getDataSetType() == GPXDataSetType.ALTITUDE_EXTRM) {
			dataSetCount--;
		}

		if (dataSetCount == 1 || dataSetCount == 2) {
			OrderedLineDataSet firstDataSet = (OrderedLineDataSet) chartData.getDataSetByIndex(0);
			OrderedLineDataSet secondDataSet = dataSetCount == 2
					? (OrderedLineDataSet) chartData.getDataSetByIndex(1)
					: null;
			if (dataSetCount == 2 && !firstDataSet.isLeftAxis()) {
				OrderedLineDataSet temp = firstDataSet;
				firstDataSet = secondDataSet;
				secondDataSet = temp;
			}

			updateYAxisValue(entry, firstDataSet, firstYAxisContainer);
			updateYAxisValue(entry, secondDataSet, secondYAxisContainer);
			updateXAxisValue(firstDataSet, entry);
		} else {
			AndroidUiHelper.setVisibility(GONE, firstYAxisContainer, secondYAxisContainer, xAxisContainer);
		}

		super.refreshContent(entry, highlight);
	}

	private void updateYAxisValue(@NonNull Entry entry, @Nullable OrderedLineDataSet dataSet, @NonNull View container) {
		AndroidUiHelper.updateVisibility(container, dataSet != null);
		if (dataSet == null) {
			return;
		}

		TextView textValue = container.findViewById(R.id.text_value);
		TextView textUnits = container.findViewById(R.id.text_units);

		float y = getOrInterpolateY(dataSet, entry);
		String formattedValue = dataSet.getMarkerValueFormatter().formatValue(getMyApplication(), y);

		textValue.setText(formattedValue);
		textValue.setTextColor(dataSet.getColor());
		textUnits.setText(dataSet.getUnits());
	}

	private float getOrInterpolateY(@NonNull OrderedLineDataSet dataSet, @NonNull Entry entry) {
		if (dataSet.getEntryIndex(entry) == -1) {
			Entry upEntry = dataSet.getEntryForXValue(entry.getX(), Float.NaN, DataSet.Rounding.UP);
			Entry downEntry = upEntry;
			int upIndex = dataSet.getEntryIndex(upEntry);
			if (upIndex > 0) {
				downEntry = dataSet.getEntryForIndex(upIndex - 1);
			}
			if (downEntry != null && upEntry != null) {
				return MapUtils.getInterpolatedY(downEntry.getX(), downEntry.getY(), upEntry.getX(), upEntry.getY(), entry.getX());
			}
		}
		return entry.getY();
	}

	private void updateXAxisValue(@NonNull OrderedLineDataSet dataSet, @NonNull Entry entry) {
		if (showXAxisValue) {
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
		FormattedValue formattedDistance = OsmAndFormatter.getFormattedDistanceValue(meters, app);

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
		OrderedLineDataSet lastDataSet = dataSetCount > 0 ? (OrderedLineDataSet) chartData.getDataSetByIndex(dataSetCount - 1) : null;
		if (lastDataSet != null && lastDataSet.getDataSetType() == GPXDataSetType.ALTITUDE_EXTRM) {
			dataSetCount--;
		}
		int halfDp = AndroidUtils.dpToPx(getContext(), .5f);
		float offsetX;
		if (dataSetCount == 2) {
			offsetX = -secondYAxisContainer.getLeft() - halfDp;
		} else if (dataSetCount == 1 && showXAxisValue) {
			offsetX = -xAxisContainer.getLeft() - halfDp;
		} else {
			offsetX = -getWidth() / 2f;
		}
		return new MPPointF(offsetX, 0);
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
	private OsmandApplication getMyApplication() {
		return ((OsmandApplication) getContext().getApplicationContext());
	}

	public interface MarkerValueFormatter {
		@NonNull
		String formatValue(@NonNull OsmandApplication app, float value);
	}
}