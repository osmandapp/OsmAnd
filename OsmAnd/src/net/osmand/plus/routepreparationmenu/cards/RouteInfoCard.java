package net.osmand.plus.routepreparationmenu.cards;

import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.StyleSpan;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.github.mikephil.charting.charts.HorizontalBarChart;
import com.github.mikephil.charting.data.BarData;

import net.osmand.AndroidUtils;
import net.osmand.GPXUtilities;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.GpxUiHelper;
import net.osmand.router.RouteStatistics;
import net.osmand.util.Algorithms;

import java.util.Map;

public class RouteInfoCard extends BaseCard {

	private MapActivity mapActivity;
	private RouteStatistics.Statistics routeStatistics;
	private GPXUtilities.GPXTrackAnalysis analysis;

	public RouteInfoCard(MapActivity mapActivity, RouteStatistics.Statistics routeStatistics, GPXUtilities.GPXTrackAnalysis analysis) {
		super(mapActivity);
		this.mapActivity = mapActivity;
		this.routeStatistics = routeStatistics;
		this.analysis = analysis;
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.route_info_card;
	}

	@Override
	protected void updateContent() {
		updateTitle();
		final HorizontalBarChart chart = (HorizontalBarChart) view.findViewById(R.id.chart);
		GpxUiHelper.setupHorizontalGPXChart(chart, 5, 10, 10, true);
		BarData barData = GpxUiHelper.buildStatisticChart(app, chart, routeStatistics, analysis, true, nightMode);
		chart.setData(barData);
		LinearLayout container = view.findViewById(R.id.route_items);
		attachLegend(container, routeStatistics);
	}

	@Override
	protected void applyDayNightMode() {
		view.setBackgroundColor(ContextCompat.getColor(mapActivity, nightMode ? R.color.route_info_bg_dark : R.color.route_info_bg_light));
		TextView details = (TextView) view.findViewById(R.id.info_type_details);
		details.setTextColor(ContextCompat.getColor(app, nightMode ? R.color.active_buttons_and_links_dark : R.color.active_buttons_and_links_light));
		TextView title = (TextView) view.findViewById(R.id.info_type_title);
		AndroidUtils.setTextPrimaryColor(app, title, nightMode);
	}

	private void updateTitle() {
		TextView title = (TextView) view.findViewById(R.id.info_type_title);
		String name = getInfoType();
		title.setText(name);
	}

	private String getInfoType() {
		if (routeStatistics.getStatisticType() == RouteStatistics.StatisticType.CLASS) {
			return app.getString(R.string.road_types);
		} else if (routeStatistics.getStatisticType() == RouteStatistics.StatisticType.STEEPNESS) {
			return app.getString(R.string.route_steepness_stat_container);
		} else if (routeStatistics.getStatisticType() == RouteStatistics.StatisticType.SMOOTHNESS) {
			return app.getString(R.string.route_smoothness_stat_container);
		} else if (routeStatistics.getStatisticType() == RouteStatistics.StatisticType.SURFACE) {
			return app.getString(R.string.route_surface_stat_container);
		} else {
			return "";
		}
	}

	private <E> void attachLegend(ViewGroup container, RouteStatistics.Statistics<E> routeStatistics) {
		Map<E, RouteStatistics.RouteSegmentAttribute<E>> partition = routeStatistics.getPartition();
		for (E key : partition.keySet()) {
			RouteStatistics.RouteSegmentAttribute<E> segment = partition.get(key);
			int color = GpxUiHelper.getColorFromRouteSegmentAttribute(app, segment, nightMode);
			Drawable circle = app.getUIUtilities().getPaintedIcon(R.drawable.ic_action_circle, color);
			Spannable text = getSpanLegend(key.toString(), segment);

			TextView legend = new TextView(app);
			AndroidUtils.setTextPrimaryColor(app, legend, nightMode);
			legend.setTextSize(15);
			legend.setGravity(Gravity.CENTER_VERTICAL);
			legend.setCompoundDrawablePadding(AndroidUtils.dpToPx(app, 16));
			legend.setPadding(AndroidUtils.dpToPx(app, 16), AndroidUtils.dpToPx(app, 4), AndroidUtils.dpToPx(app, 16), AndroidUtils.dpToPx(app, 4));
			legend.setCompoundDrawablesWithIntrinsicBounds(circle, null, null, null);
			legend.setText(text);

			container.addView(legend);
		}
	}

	private Spannable getSpanLegend(String title, RouteStatistics.RouteSegmentAttribute segment) {
		String formattedDistance = OsmAndFormatter.getFormattedDistance(segment.getDistance(), getMyApplication());
		title = Algorithms.capitalizeFirstLetter(title);
		SpannableStringBuilder spannable = new SpannableStringBuilder(title);
		spannable.append(": ");
		spannable.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 0, spannable.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		spannable.append(formattedDistance);

		return spannable;
	}
}