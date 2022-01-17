package net.osmand.plus.track;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.github.mikephil.charting.components.MarkerView;
import com.github.mikephil.charting.data.ChartData;
import com.github.mikephil.charting.data.DataSet;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.utils.MPPointF;

import net.osmand.plus.R;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.helpers.GpxUiHelper.GPXDataSetType;
import net.osmand.plus.helpers.GpxUiHelper.GPXHighlight;
import net.osmand.plus.helpers.GpxUiHelper.OrderedLineDataSet;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.util.MapUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

@SuppressLint("ViewConstructor")
public class GpxMarkerView extends MarkerView {

	private final View altitudeContainer;
	private final View speedContainer;
	private final View slopeContainer;

	private final View divider;

	private final boolean hasIcon;

	public GpxMarkerView(@NonNull Context context, @Nullable Drawable icon) {
		super(context, R.layout.chart_marker_view);
		altitudeContainer = findViewById(R.id.altitude_container);
		speedContainer = findViewById(R.id.speed_container);
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
			updateOneDataSetMarker(dataSet, entry);
		} else if (chartData.getDataSetCount() == 2) {
			OrderedLineDataSet firstDataSet = (OrderedLineDataSet) chartData.getDataSetByIndex(0);
			OrderedLineDataSet secondDataSet = ((OrderedLineDataSet) chartData.getDataSetByIndex(1));
			updateTwoDataSetsMarker(firstDataSet, secondDataSet, entry);
		} else {
			AndroidUiHelper.setVisibility(GONE,
					altitudeContainer, speedContainer, slopeContainer, divider);
		}
		super.refreshContent(entry, highlight);
	}

	private void updateOneDataSetMarker(@NonNull OrderedLineDataSet dataSet, @NonNull Entry entry) {
		String value = (int) entry.getY() + " ";
		String units = dataSet.getUnits();
		switch (dataSet.getDataSetType()) {
			case ALTITUDE:
				((TextView) altitudeContainer.findViewById(R.id.text_alt_value)).setText(value);
				((TextView) altitudeContainer.findViewById(R.id.text_alt_units)).setText(units);
				AndroidUiHelper.updateVisibility(altitudeContainer, true);
				AndroidUiHelper.setVisibility(GONE, speedContainer, slopeContainer);
				break;
			case SPEED:
				((TextView) speedContainer.findViewById(R.id.text_spd_value)).setTextColor(dataSet.getColor());
				((TextView) speedContainer.findViewById(R.id.text_spd_value)).setText(value);
				((TextView) speedContainer.findViewById(R.id.text_spd_units)).setText(units);
				AndroidUiHelper.updateVisibility(speedContainer, true);
				AndroidUiHelper.setVisibility(GONE, altitudeContainer, slopeContainer);
				break;
			case SLOPE:
				((TextView) slopeContainer.findViewById(R.id.text_slp_value)).setText(value);
				AndroidUiHelper.updateVisibility(slopeContainer, true);
				AndroidUiHelper.setVisibility(GONE, altitudeContainer, speedContainer);
				break;
		}
		AndroidUiHelper.updateVisibility(divider, false);
	}

	private void updateTwoDataSetsMarker(@NonNull OrderedLineDataSet firstDataSet,
	                                     @NonNull OrderedLineDataSet secondDataSet,
	                                     @NonNull Entry entry) {
		OrderedLineDataSet altitudeDataSet = getDataSetByType(GPXDataSetType.ALTITUDE, firstDataSet, secondDataSet);
		OrderedLineDataSet speedDataSet = getDataSetByType(GPXDataSetType.SPEED, firstDataSet, secondDataSet);
		OrderedLineDataSet slopeDataSet = getDataSetByType(GPXDataSetType.SLOPE, firstDataSet, secondDataSet);

		updateAltitudeText(altitudeDataSet, entry);
		updateSpeedText(speedDataSet, entry);
		updateSlopeText(slopeDataSet, entry);

		AndroidUiHelper.updateVisibility(divider, true);
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
			((TextView) altitudeContainer.findViewById(R.id.text_alt_value)).setText((int) y + " ");
			((TextView) altitudeContainer.findViewById(R.id.text_alt_units)).setText(altitudeDataSet.getUnits());
		}
		AndroidUiHelper.updateVisibility(altitudeContainer, altitudeDataSet != null);
	}

	private void updateSpeedText(@Nullable OrderedLineDataSet speedDataSet, @NonNull Entry entry) {
		if (speedDataSet != null) {
			float y = getInterpolatedY(speedDataSet, entry);
			((TextView) speedContainer.findViewById(R.id.text_spd_value)).setTextColor(speedDataSet.getColor());
			((TextView) speedContainer.findViewById(R.id.text_spd_value)).setText((int) y + " ");
			((TextView) speedContainer.findViewById(R.id.text_spd_units)).setText(speedDataSet.getUnits());
		}
		AndroidUiHelper.updateVisibility(speedContainer, speedDataSet != null);
	}

	private void updateSlopeText(@Nullable OrderedLineDataSet slopeDataSet, @NonNull Entry entry) {
		if (slopeDataSet != null) {
			float y = getInterpolatedY(slopeDataSet, entry);
			((TextView) slopeContainer.findViewById(R.id.text_slp_value)).setText((int) y + " ");
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
		if (getChartView().getData().getDataSetCount() == 2) {
			int x = divider.getLeft();
			return new MPPointF(-x - AndroidUtils.dpToPx(getContext(), .5f), 0);
		} else {
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
}