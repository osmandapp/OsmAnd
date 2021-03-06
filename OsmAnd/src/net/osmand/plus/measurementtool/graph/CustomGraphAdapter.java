package net.osmand.plus.measurementtool.graph;

import android.graphics.drawable.Drawable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.graphics.ColorUtils;

import com.github.mikephil.charting.charts.HorizontalBarChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;

import net.osmand.AndroidUtils;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.helpers.CustomBarChartRenderer;
import net.osmand.router.RouteStatisticsHelper.RouteSegmentAttribute;
import net.osmand.router.RouteStatisticsHelper.RouteStatistics;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static net.osmand.plus.track.ColorsCard.MINIMUM_CONTRAST_RATIO;

public class CustomGraphAdapter extends BaseGraphAdapter<HorizontalBarChart, BarData, RouteStatistics> {

	private String selectedPropertyName;
	private ViewGroup legendContainer;
	private LegendViewType legendViewType;
	private LayoutChangeListener layoutChangeListener;

	public enum LegendViewType {
		ONE_ELEMENT,
		ALL_AS_LIST,
		GONE
	}

	public CustomGraphAdapter(HorizontalBarChart chart, boolean usedOnMap) {
		super(chart, usedOnMap);
	}

	@Override
	protected void prepareChartView() {
		super.prepareChartView();
		legendViewType = LegendViewType.GONE;
		chart.setRenderer(new CustomBarChartRenderer(chart));
		chart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
			@Override
			public void onValueSelected(Entry e, Highlight h) {
				if (getStatistics() == null) return;

				List<RouteSegmentAttribute> elems = getStatistics().elements;
				int i = h.getStackIndex();
				if (i >= 0 && elems.size() > i) {
					selectedPropertyName = elems.get(i).getPropertyName();
					updateLegend();
				} else if (LegendViewType.ONE_ELEMENT == legendViewType && elems.size() == 1) {
					selectedPropertyName = elems.get(0).getPropertyName();
					updateLegend();
				}
			}

			@Override
			public void onNothingSelected() {
				selectedPropertyName = null;
				updateLegend();
			}
		});
	}

	@Override
	public void updateView() {
		chart.setData(chartData);
		updateHighlight();
		updateLegend();
	}

	public void setLegendContainer(ViewGroup legendContainer) {
		this.legendContainer = legendContainer;
	}

	public void setLegendViewType(LegendViewType legendViewType) {
		this.legendViewType = legendViewType;
	}

	public void setLayoutChangeListener(LayoutChangeListener layoutChangeListener) {
		this.layoutChangeListener = layoutChangeListener;
	}

	public void highlight(Highlight h) {
		super.highlight(h);
		Highlight bh = h != null ? chart.getHighlighter().getHighlight(1, h.getXPx()) : null;
		if (bh != null) {
			bh.setDraw(h.getXPx(), 0);
		}
		chart.highlightValue(bh, true);
	}

	private void updateLegend() {
		if (legendContainer != null) {
			legendContainer.removeAllViews();
			attachLegend();
			if (layoutChangeListener != null) {
				layoutChangeListener.onLayoutChanged();
			}
		}
	}

	private void attachLegend() {
		List<RouteSegmentAttribute> attributes = getSegmentsList();
		if (attributes == null) return;

		switch (legendViewType) {
			case ONE_ELEMENT:
				for (RouteSegmentAttribute attribute : attributes) {
					if (attribute.getPropertyName().equals(selectedPropertyName)) {
						attachLegend(Collections.singletonList(attribute), null);
						break;
					}
				}
				break;
			case ALL_AS_LIST:
				attachLegend(attributes, selectedPropertyName);
				break;
		}
	}

	private void attachLegend(List<RouteSegmentAttribute> list,
	                          String propertyNameToFullSpan) {
		OsmandApplication app = getMyApplication();
		LayoutInflater inflater = UiUtilities.getInflater(app, isNightMode());
		for (RouteSegmentAttribute segment : list) {
			View view = inflater.inflate(R.layout.route_details_legend, legendContainer, false);
			int segmentColor = segment.getColor();
			Drawable circle = app.getUIUtilities().getPaintedIcon(R.drawable.ic_action_circle, segmentColor);
			ImageView legendIcon = (ImageView) view.findViewById(R.id.legend_icon_color);
			legendIcon.setImageDrawable(circle);
			double contrastRatio = ColorUtils.calculateContrast(segmentColor,
					AndroidUtils.getColorFromAttr(app, R.attr.card_and_list_background_basic));
			if (contrastRatio < MINIMUM_CONTRAST_RATIO) {
				legendIcon.setBackgroundResource(AndroidUtils.resolveAttribute(app, R.attr.bg_circle_contour));
			}
			String propertyName = segment.getUserPropertyName();
			String name = AndroidUtils.getRenderingStringPropertyName(app, propertyName, propertyName.replaceAll("_", " "));
			boolean selected = segment.getPropertyName().equals(propertyNameToFullSpan);
			Spannable text = getSpanLegend(name, segment, selected);
			TextView legend = (TextView) view.findViewById(R.id.legend_text);
			legend.setText(text);

			legendContainer.addView(view);
		}
	}

	private Spannable getSpanLegend(String title,
	                                RouteSegmentAttribute segment,
	                                boolean fullSpan) {
		String formattedDistance = OsmAndFormatter.getFormattedDistance(segment.getDistance(), getMyApplication());
		title = Algorithms.capitalizeFirstLetter(title);
		SpannableStringBuilder spannable = new SpannableStringBuilder(title);
		spannable.append(": ");
		int startIndex = fullSpan ? -0 : spannable.length();
		spannable.append(formattedDistance);
		spannable.setSpan(new StyleSpan(android.graphics.Typeface.BOLD),
				startIndex, spannable.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		return spannable;
	}

	private List<RouteSegmentAttribute> getSegmentsList() {
		return getStatistics() != null ? new ArrayList<>(getStatistics().partition.values()) : null;
	}

	private RouteStatistics getStatistics() {
		return additionalData;
	}
}
