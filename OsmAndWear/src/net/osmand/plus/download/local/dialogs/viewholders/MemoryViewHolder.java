package net.osmand.plus.download.local.dialogs.viewholders;


import static com.github.mikephil.charting.utils.Fill.Direction.LEFT;
import static com.github.mikephil.charting.utils.Fill.Direction.RIGHT;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.buffer.BarBuffer;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.HorizontalBarChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet;
import com.github.mikephil.charting.renderer.HorizontalBarChartRenderer;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.charts.ChartUtils;
import net.osmand.plus.download.local.dialogs.MemoryInfo;
import net.osmand.plus.download.local.dialogs.MemoryInfo.MemoryItem;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.measurementtool.graph.BaseChartAdapter;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.FlowLayout;

public class MemoryViewHolder extends RecyclerView.ViewHolder {

	private final OsmandApplication app;
	private final MemoryChartAdapter adapter;
	private final boolean nightMode;

	private final TextView size;
	private final HorizontalBarChart chart;
	private final View bottomDivider;


	public MemoryViewHolder(@NonNull View itemView, boolean showHeader, boolean nightMode) {
		super(itemView);
		app = (OsmandApplication) itemView.getContext().getApplicationContext();
		this.nightMode = nightMode;

		size = itemView.findViewById(R.id.size);
		chart = itemView.findViewById(R.id.horizontal_chart);
		bottomDivider = itemView.findViewById(R.id.bottom_divider);

		ChartUtils.setupHorizontalGPXChart(app, chart, 0, 0, 0, false, nightMode);
		chart.getAxisRight().setDrawLabels(false);
		chart.setRenderer(new RoundedChartRenderer(chart));

		adapter = new MemoryChartAdapter(app, chart, true);
		adapter.setBottomInfoContainer(itemView.findViewById(R.id.legend));

		int padding = showHeader ? 0 : app.getResources().getDimensionPixelSize(R.dimen.content_padding);
		AndroidUtils.setMargins((MarginLayoutParams) chart.getLayoutParams(), 0, padding, 0, 0);
		AndroidUiHelper.updateVisibility(bottomDivider, !showHeader);
		AndroidUiHelper.updateVisibility(itemView.findViewById(R.id.header), showHeader);
	}

	public void bindView(@NonNull MemoryInfo memoryInfo, boolean showDivider) {
		BarData barData = ChartUtils.buildStatisticChart(app, chart, memoryInfo, nightMode);
		adapter.updateContent(barData, memoryInfo);

		size.setText(AndroidUtils.formatSize(app, memoryInfo.getSize()));
		AndroidUiHelper.updateVisibility(bottomDivider, showDivider);
	}

	public static class MemoryChartAdapter extends BaseChartAdapter<HorizontalBarChart, BarData, MemoryInfo> {

		private final UiUtilities uiUtilities;
		private final LayoutInflater themedInflater;

		public MemoryChartAdapter(@NonNull OsmandApplication app, @NonNull HorizontalBarChart chart, boolean usedOnMap) {
			super(app, chart, usedOnMap);
			uiUtilities = app.getUIUtilities();
			themedInflater = UiUtilities.getInflater(chart.getContext(), isNightMode());
		}

		@Nullable
		private MemoryInfo getMemoryInfo() {
			return additionalData;
		}

		@Override
		protected void attachBottomInfo() {
			MemoryInfo memoryInfo = getMemoryInfo();
			if (memoryInfo != null) {
				for (MemoryItem item : memoryInfo.getItems()) {
					attachLegend(item);
				}
			}
		}

		private void attachLegend(@NonNull MemoryItem item) {
			View view = themedInflater.inflate(R.layout.local_memory_legend, bottomInfoContainer, false);

			ImageView icon = view.findViewById(R.id.icon);
			icon.setImageDrawable(uiUtilities.getPaintedIcon(R.drawable.ic_action_circle, item.getColor()));

			TextView legend = view.findViewById(R.id.text);
			legend.setText(item.getText());

			bottomInfoContainer.addView(view, new FlowLayout.LayoutParams(AndroidUtils.dpToPx(app, 16), 0));
		}
	}

	public static class RoundedChartRenderer extends HorizontalBarChartRenderer {

		private final Paint dividerPaint = new Paint();
		private final Paint backgroundPaint = new Paint();
		private final float cornersRadius;

		public RoundedChartRenderer(@NonNull BarChart chart) {
			super(chart, chart.getAnimator(), chart.getViewPortHandler());
			Context context = chart.getContext();
			this.cornersRadius = AndroidUtils.dpToPx(context, 3f);
			dividerPaint.setColor(AndroidUtils.getColorFromAttr(context, R.attr.list_background_color));
			dividerPaint.setStrokeWidth(AndroidUtils.dpToPx(context, 2f));
		}

		@Override
		protected void drawRects(Canvas canvas, IBarDataSet dataSet, BarBuffer buffer,
		                         boolean isCustomFill, boolean isSingleColor, boolean isInverted) {
			RectF rect = new RectF(buffer.buffer[0], buffer.buffer[1], buffer.buffer[buffer.size() - 2], buffer.buffer[3]);
			canvas.drawRoundRect(rect, cornersRadius, cornersRadius, backgroundPaint);
			mRenderPaint.setXfermode(new PorterDuffXfermode(Mode.SRC_ATOP));
			dividerPaint.setXfermode(new PorterDuffXfermode(Mode.SRC_ATOP));

			super.drawRects(canvas, dataSet, buffer, isCustomFill, isSingleColor, isInverted);
			mRenderPaint.setXfermode(null);
			dividerPaint.setXfermode(null);
		}

		@Override
		protected void drawRect(Canvas canvas, IBarDataSet dataSet, BarBuffer buffer, int j, int pos,
		                        boolean isCustomFill, boolean isInverted, boolean drawBorder) {
			float left = buffer.buffer[j];
			float top = buffer.buffer[j + 1];
			float right = buffer.buffer[j + 2];
			float bottom = buffer.buffer[j + 3];

			if (isCustomFill) {
				dataSet.getFill(pos).fillRect(canvas, mRenderPaint, left, top, right, bottom, isInverted ? LEFT : RIGHT);
			} else {
				canvas.drawRect(left, top, right, bottom, mRenderPaint);
				if (j + 4 < buffer.size()) {
					canvas.drawLine(right, top, right, bottom, dividerPaint);
				}
			}
			if (drawBorder) {
				canvas.drawRect(left, top, right, bottom, mBarBorderPaint);
			}
		}
	}
}