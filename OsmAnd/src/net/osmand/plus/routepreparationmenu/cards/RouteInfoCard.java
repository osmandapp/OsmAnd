package net.osmand.plus.routepreparationmenu.cards;

import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.ColorUtils;
import android.support.v7.view.ContextThemeWrapper;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.github.mikephil.charting.charts.HorizontalBarChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;

import net.osmand.GPXUtilities.GPXTrackAnalysis;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.SettingsBaseActivity;
import net.osmand.plus.activities.SettingsNavigationActivity;
import net.osmand.plus.helpers.GpxUiHelper;
import net.osmand.router.RouteStatisticsHelper.RouteSegmentAttribute;
import net.osmand.router.RouteStatisticsHelper.RouteStatistics;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RouteInfoCard extends BaseCard {

	private static final int MINIMUM_CONTRAST_RATIO = 3;

	private RouteStatistics routeStatistics;
	private GPXTrackAnalysis analysis;
	private String selectedPropertyName;

	private boolean showLegend;

	public RouteInfoCard(MapActivity mapActivity, RouteStatistics routeStatistics, GPXTrackAnalysis analysis) {
		super(mapActivity);
		this.routeStatistics = routeStatistics;
		this.analysis = analysis;
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.route_info_card;
	}

	@Override
	protected void updateContent() {
		updateContent(routeStatistics);
	}

	@Nullable
	public HorizontalBarChart getChart() {
		return (HorizontalBarChart) view.findViewById(R.id.chart);
	}

	private void updateContent(final RouteStatistics routeStatistics) {
		updateHeader();
		final HorizontalBarChart chart = (HorizontalBarChart) view.findViewById(R.id.chart);
		GpxUiHelper.setupHorizontalGPXChart(app, chart, 5, 9, 24, true, nightMode);
		chart.setExtraRightOffset(16);
		chart.setExtraLeftOffset(16);
		BarData barData = GpxUiHelper.buildStatisticChart(app, chart, routeStatistics, analysis, true, nightMode);
		chart.setData(barData);
		chart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
			@Override
			public void onValueSelected(Entry e, Highlight h) {
				List<RouteSegmentAttribute> elems = routeStatistics.elements;
				int i = h.getStackIndex();
				if (i >= 0 && elems.size() > i) {
					selectedPropertyName = elems.get(i).getPropertyName();
					if (showLegend) {
						updateLegend(routeStatistics);
					}
				}
			}

			@Override
			public void onNothingSelected() {
				selectedPropertyName = null;
				if (showLegend) {
					updateLegend(routeStatistics);
				}
			}
		});
		LinearLayout container = (LinearLayout) view.findViewById(R.id.route_items);
		container.removeAllViews();
		if (showLegend) {
			attachLegend(container, routeStatistics);
		}
		final ImageView iconViewCollapse = (ImageView) view.findViewById(R.id.up_down_icon);
		iconViewCollapse.setImageDrawable(getCollapseIcon(!showLegend));
		view.findViewById(R.id.info_type_details_button).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				showLegend = !showLegend;
				updateContent();
				setLayoutNeeded();
			}
		});
	}

	protected void updateLegend(RouteStatistics routeStatistics) {
		LinearLayout container = (LinearLayout) view.findViewById(R.id.route_items);
		container.removeAllViews();
		attachLegend(container, routeStatistics);
		setLayoutNeeded();
	}

	private Drawable getCollapseIcon(boolean collapsed) {
		return collapsed ? getContentIcon(R.drawable.ic_action_arrow_down) : getActiveIcon(R.drawable.ic_action_arrow_up);
	}

	private void updateHeader() {
		TextView title = (TextView) view.findViewById(R.id.info_type_title);
		String name = SettingsBaseActivity.getStringRouteInfoPropertyValue(app, routeStatistics.name);
		title.setText(name);
	}

	private void attachLegend(ViewGroup container, RouteStatistics routeStatistics) {
		Map<String, RouteSegmentAttribute> partition = routeStatistics.partition;
		List<Map.Entry<String, RouteSegmentAttribute>> list = new ArrayList<>(partition.entrySet());
		ContextThemeWrapper ctx = new ContextThemeWrapper(mapActivity, !nightMode ? R.style.OsmandLightTheme : R.style.OsmandDarkTheme);
		LayoutInflater inflater = LayoutInflater.from(ctx);
		for (Map.Entry<String, RouteSegmentAttribute> entry : list) {
			RouteSegmentAttribute segment = entry.getValue();
			View view = inflater.inflate(R.layout.route_details_legend, container, false);
			int segmentColor = segment.getColor();
			Drawable circle = app.getUIUtilities().getPaintedIcon(R.drawable.ic_action_circle, segmentColor);
			ImageView legendIcon = (ImageView) view.findViewById(R.id.legend_icon_color);
			legendIcon.setImageDrawable(circle);
			double contrastRatio = ColorUtils.calculateContrast(segmentColor, ContextCompat.getColor(app, nightMode ? R.color.card_and_list_background_dark : R.color.card_and_list_background_light));
			if (contrastRatio < MINIMUM_CONTRAST_RATIO) {
				legendIcon.setBackgroundResource(nightMode ? R.drawable.circle_contour_bg_dark : R.drawable.circle_contour_bg_light);
			}
			String propertyName = segment.getUserPropertyName();
			String name = SettingsNavigationActivity.getStringPropertyName(app, propertyName, propertyName.replaceAll("_", " "));
			Spannable text = getSpanLegend(name, segment, segment.getUserPropertyName().equals(selectedPropertyName));
			TextView legend = (TextView) view.findViewById(R.id.legend_text);
			legend.setText(text);

			container.addView(view);
		}
	}

	private Spannable getSpanLegend(String title, RouteSegmentAttribute segment, boolean selected) {
		String formattedDistance = OsmAndFormatter.getFormattedDistance(segment.getDistance(), getMyApplication());
		title = Algorithms.capitalizeFirstLetter(title);
		SpannableStringBuilder spannable = new SpannableStringBuilder(title);
		spannable.append(": ");
		int startIndex = selected ? -0 : spannable.length();
		spannable.append(formattedDistance);
		spannable.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), startIndex, spannable.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

		return spannable;
	}
}