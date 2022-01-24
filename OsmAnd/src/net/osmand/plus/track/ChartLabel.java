package net.osmand.plus.track;

import android.content.Context;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.AttributeSet;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart.LabelDisplayData;
import com.github.mikephil.charting.charts.LineChart.YAxisLabelView;

import net.osmand.plus.R;
import net.osmand.plus.utils.AndroidUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class ChartLabel extends YAxisLabelView {

	private static final int SPAN_FLAG = Spannable.SPAN_EXCLUSIVE_EXCLUSIVE;

	private final TextView label;

	public ChartLabel(@NonNull Context context, int layoutId) {
		super(context, layoutId);
		label = findViewById(R.id.label);
	}

	public ChartLabel(@NonNull Context context, @Nullable AttributeSet attrs, int layoutId) {
		super(context, attrs, layoutId);
		label = findViewById(R.id.label);
	}

	public ChartLabel(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int layoutId) {
		super(context, attrs, defStyleAttr, layoutId);
		label = findViewById(R.id.label);
	}

	public ChartLabel(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes, int layoutId) {
		super(context, attrs, defStyleAttr, defStyleRes, layoutId);
		label = findViewById(R.id.label);
	}

	@Override
	public void updateLabel(@NonNull LabelDisplayData leftYAxisData, @Nullable LabelDisplayData rightYAxisData) {
		if (rightYAxisData == null) {
			SpannableString displayText = new SpannableString(leftYAxisData.getText());
			displayText.setSpan(new ForegroundColorSpan(leftYAxisData.getColor()), 0,
					displayText.length(), SPAN_FLAG);
			label.setText(displayText);
		} else {
			String combinedPlainText = getContext().getString(R.string.ltr_or_rtl_combine_via_comma,
					leftYAxisData.getText(), rightYAxisData.getText());
			SpannableString displayText = new SpannableString(combinedPlainText);

			boolean rtl = AndroidUtils.isLayoutRtl(getContext());
			LabelDisplayData first = rtl ? rightYAxisData : leftYAxisData;
			int edge = rtl ? first.getText().length() : first.getText().length() + 1;
			displayText.setSpan(new ForegroundColorSpan(leftYAxisData.getColor()), 0, edge, SPAN_FLAG);
			displayText.setSpan(new ForegroundColorSpan(rightYAxisData.getColor()), edge,
					displayText.length(), SPAN_FLAG);

			label.setText(displayText);
		}
	}
}