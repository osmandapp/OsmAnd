package net.osmand.plus.routepreparationmenu.cards;

import android.graphics.drawable.Drawable;
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

import net.osmand.GPXUtilities;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.SettingsNavigationActivity;
import net.osmand.plus.helpers.GpxUiHelper;
import net.osmand.router.RouteStatistics.Boundaries;
import net.osmand.router.RouteStatistics.RouteSegmentAttribute;
import net.osmand.router.RouteStatistics.StatisticType;
import net.osmand.router.RouteStatistics.Statistics;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static net.osmand.router.RouteStatistics.UNDEFINED_ATTR;

public class RouteInfoCard extends BaseCard {

	private Statistics routeStatistics;
	private GPXUtilities.GPXTrackAnalysis analysis;

	private boolean showLegend;

	public RouteInfoCard(MapActivity mapActivity, Statistics routeStatistics, GPXUtilities.GPXTrackAnalysis analysis) {
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
		updateHeader();
		final HorizontalBarChart chart = (HorizontalBarChart) view.findViewById(R.id.chart);
		GpxUiHelper.setupHorizontalGPXChart(app, chart, 5, 9, 24, true, nightMode);
		BarData barData = GpxUiHelper.buildStatisticChart(app, chart, routeStatistics, analysis, true, nightMode);
		chart.setData(barData);
		final LinearLayout container = (LinearLayout) view.findViewById(R.id.route_items);
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

	private Drawable getCollapseIcon(boolean collapsed) {
		return collapsed ? getContentIcon(R.drawable.ic_action_arrow_down) : getActiveIcon(R.drawable.ic_action_arrow_up);
	}

	private void updateHeader() {
		TextView title = (TextView) view.findViewById(R.id.info_type_title);
		String name = getInfoType();
		title.setText(name);
	}

	private String getInfoType() {
		if (routeStatistics.getStatisticType() == StatisticType.CLASS) {
			return app.getString(R.string.road_types);
		} else if (routeStatistics.getStatisticType() == StatisticType.STEEPNESS) {
			return app.getString(R.string.route_steepness_stat_container);
		} else if (routeStatistics.getStatisticType() == StatisticType.SMOOTHNESS) {
			return app.getString(R.string.route_smoothness_stat_container);
		} else if (routeStatistics.getStatisticType() == StatisticType.SURFACE) {
			return app.getString(R.string.route_surface_stat_container);
		} else {
			return "";
		}
	}

	private <E> void attachLegend(ViewGroup container, Statistics<E> routeStatistics) {
		Map<E, RouteSegmentAttribute<E>> partition = routeStatistics.getPartition();
		List<E> list = new ArrayList<E>(partition.keySet());
		sortRouteSegmentAttributes(list);
		ContextThemeWrapper ctx = new ContextThemeWrapper(mapActivity, !nightMode ? R.style.OsmandLightTheme : R.style.OsmandDarkTheme);
		LayoutInflater inflater = LayoutInflater.from(ctx);
		for (E key : list) {
			RouteSegmentAttribute<E> segment = partition.get(key);
			View view = inflater.inflate(R.layout.route_details_legend, container, false);
			Drawable circle = app.getUIUtilities().getPaintedIcon(R.drawable.ic_action_circle, segment.getColor());
			ImageView legendIcon = (ImageView) view.findViewById(R.id.legend_icon_color);
			legendIcon.setImageDrawable(circle);
			String propertyName = segment.getPropertyName();
			String name = SettingsNavigationActivity.getStringPropertyName(app, propertyName, propertyName.replaceAll("_", " "));
			Spannable text = getSpanLegend(name, segment);
			TextView legend = (TextView) view.findViewById(R.id.legend_text);
			legend.setText(text);

			container.addView(view);
		}
	}

	private <E> void sortRouteSegmentAttributes(List<E> list) {
		Collections.sort(list, new Comparator<E>() {
			@Override
			public int compare(E o1, E o2) {
				if (o1 instanceof String && o2 instanceof String) {
					String name1 = (String) o1;
					String name2 = (String) o2;

					if (name1.equalsIgnoreCase(UNDEFINED_ATTR)) {
						return 1;
					}
					if (name2.equalsIgnoreCase(UNDEFINED_ATTR)) {
						return -1;
					}
					return name1.compareTo(name2);
				} else if (o1 instanceof Boundaries && o2 instanceof Boundaries) {
					return ((Boundaries) o1).compareTo((Boundaries) o2);
				}
				return 0;
			}
		});
	}

	private Spannable getSpanLegend(String title, RouteSegmentAttribute segment) {
		String formattedDistance = OsmAndFormatter.getFormattedDistance(segment.getDistance(), getMyApplication());
		title = Algorithms.capitalizeFirstLetter(title);
		SpannableStringBuilder spannable = new SpannableStringBuilder(title);
		spannable.append(": ");
		int startIndex = spannable.length();
		spannable.append(formattedDistance);
		spannable.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), startIndex, spannable.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

		return spannable;
	}
}