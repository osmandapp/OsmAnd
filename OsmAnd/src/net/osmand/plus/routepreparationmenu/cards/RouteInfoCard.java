package net.osmand.plus.routepreparationmenu.cards;

import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.github.mikephil.charting.charts.HorizontalBarChart;
import com.github.mikephil.charting.data.BarData;

import net.osmand.AndroidUtils;
import net.osmand.GPXUtilities.GPXTrackAnalysis;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.GpxUiHelper;
import net.osmand.plus.measurementtool.graph.CustomGraphAdapter;
import net.osmand.plus.measurementtool.graph.CustomGraphAdapter.LegendViewType;
import net.osmand.router.RouteStatisticsHelper.RouteStatistics;

public class RouteInfoCard extends MapBaseCard {
	private RouteStatistics statistics;
	private GPXTrackAnalysis analysis;
	private CustomGraphAdapter graphAdapter;

	private boolean showLegend;

	public RouteInfoCard(MapActivity mapActivity, RouteStatistics statistics, GPXTrackAnalysis analysis) {
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
		LinearLayout container = (LinearLayout) view.findViewById(R.id.route_items);
		HorizontalBarChart chart = (HorizontalBarChart) view.findViewById(R.id.chart);
		GpxUiHelper.setupHorizontalGPXChart(getMyApplication(), chart, 5, 9, 24, true, nightMode);
		BarData barData = GpxUiHelper.buildStatisticChart(app, chart, statistics, analysis, true, nightMode);
		graphAdapter = new CustomGraphAdapter(app, chart, true);
		graphAdapter.setLegendContainer(container);
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
		TextView title = (TextView) view.findViewById(R.id.info_type_title);
		String name = AndroidUtils.getStringRouteInfoPropertyValue(app, statistics.name);
		title.setText(name);
	}

	private void updateCollapseIcon() {
		ImageView ivCollapse = (ImageView) view.findViewById(R.id.up_down_icon);
		Drawable drawable = showLegend ?
				getContentIcon(R.drawable.ic_action_arrow_down) :
				getActiveIcon(R.drawable.ic_action_arrow_up);
		ivCollapse.setImageDrawable(drawable);
	}

	public CustomGraphAdapter getGraphAdapter() {
		return graphAdapter;
	}
}