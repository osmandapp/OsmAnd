package net.osmand.plus.routepreparationmenu.cards;

import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.github.mikephil.charting.charts.HorizontalBarChart;
import com.github.mikephil.charting.data.BarData;

import net.osmand.plus.charts.ChartUtils;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.shared.gpx.GpxTrackAnalysis;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.measurementtool.graph.CustomChartAdapter;
import net.osmand.plus.measurementtool.graph.CustomChartAdapter.LegendViewType;
import net.osmand.router.RouteStatisticsHelper.RouteStatistics;

public class RouteInfoCard extends MapBaseCard {
	private final RouteStatistics statistics;
	private final GpxTrackAnalysis analysis;
	private CustomChartAdapter graphAdapter;

	private boolean showLegend;

	public RouteInfoCard(MapActivity mapActivity, RouteStatistics statistics, GpxTrackAnalysis analysis) {
		super(mapActivity);
		this.statistics = statistics;
		this.analysis = analysis;
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.route_info_card;
	}

	@Override
	protected void updateContent() {
		updateHeader();
		LinearLayout container = view.findViewById(R.id.route_items);
		HorizontalBarChart chart = view.findViewById(R.id.chart);
		ChartUtils.setupHorizontalGPXChart(getMyApplication(), chart, 5, 9, 24, true, nightMode);
		BarData barData = ChartUtils.buildStatisticChart(app, chart, statistics, analysis, true, nightMode);
		graphAdapter = new CustomChartAdapter(app, chart, true);
		graphAdapter.setBottomInfoContainer(container);
		graphAdapter.updateData(barData, statistics);
		updateView();

		view.findViewById(R.id.info_type_details_button).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				showLegend = !showLegend;
				updateView();
				setLayoutNeeded();
			}
		});
	}

	private void updateView() {
		updateCollapseIcon();
		graphAdapter.setLegendViewType(showLegend ? LegendViewType.ALL_AS_LIST : LegendViewType.GONE);
		graphAdapter.updateView();
	}

	private void updateHeader() {
		TextView title = view.findViewById(R.id.info_type_title);
		String name = AndroidUtils.getStringRouteInfoPropertyValue(app, statistics.name);
		title.setText(name);
	}

	private void updateCollapseIcon() {
		ImageView ivCollapse = view.findViewById(R.id.up_down_icon);
		Drawable drawable = showLegend ?
				getContentIcon(R.drawable.ic_action_arrow_down) :
				getActiveIcon(R.drawable.ic_action_arrow_up);
		ivCollapse.setImageDrawable(drawable);
	}

	public CustomChartAdapter getGraphAdapter() {
		return graphAdapter;
	}
}