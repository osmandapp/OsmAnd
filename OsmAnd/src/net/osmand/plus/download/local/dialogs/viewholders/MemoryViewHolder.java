package net.osmand.plus.download.local.dialogs.viewholders;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.charts.HorizontalBarChart;
import com.github.mikephil.charting.data.BarData;

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


	public MemoryViewHolder(@NonNull View itemView, boolean showHeader, boolean nightMode) {
		super(itemView);
		app = (OsmandApplication) itemView.getContext().getApplicationContext();
		this.nightMode = nightMode;

		size = itemView.findViewById(R.id.size);
		chart = itemView.findViewById(R.id.horizontal_chart);

		ChartUtils.setupHorizontalGPXChart(app, chart, 0, 0, 0, false, nightMode);
		chart.getAxisRight().setDrawLabels(false);

		adapter = new MemoryChartAdapter(app, chart, true);
		adapter.setBottomInfoContainer(itemView.findViewById(R.id.legend));

		int padding = showHeader ? 0 : app.getResources().getDimensionPixelSize(R.dimen.content_padding);
		AndroidUtils.setMargins((MarginLayoutParams) chart.getLayoutParams(), 0, padding, 0, 0);
		AndroidUiHelper.updateVisibility(itemView.findViewById(R.id.header), showHeader);
		AndroidUiHelper.updateVisibility(itemView.findViewById(R.id.bottom_divider), !showHeader);
	}

	public void bindView(@NonNull MemoryInfo memoryInfo) {
		BarData barData = ChartUtils.buildStatisticChart(app, chart, memoryInfo, nightMode);
		adapter.updateContent(barData, memoryInfo);

		size.setText(AndroidUtils.formatSize(app, memoryInfo.getSize()));
	}

	private static class MemoryChartAdapter extends BaseChartAdapter<HorizontalBarChart, BarData, MemoryInfo> {

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
}