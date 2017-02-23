package net.osmand.plus.myplaces;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewCompat;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import net.osmand.AndroidUtils;
import net.osmand.plus.GPXUtilities;
import net.osmand.plus.GPXUtilities.GPXTrackAnalysis;
import net.osmand.plus.GpxSelectionHelper;
import net.osmand.plus.GpxSelectionHelper.GpxDisplayItem;
import net.osmand.plus.GpxSelectionHelper.GpxDisplayItemType;
import net.osmand.plus.IconsCache;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.views.controls.PagerSlidingTabStrip;
import net.osmand.plus.views.controls.PagerSlidingTabStrip.CustomTabProvider;
import net.osmand.plus.views.controls.WrapContentHeightViewPager;
import net.osmand.plus.views.controls.WrapContentHeightViewPager.ViewAtPositionInterface;
import net.osmand.util.Algorithms;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TrackSegmentFragment extends SelectedGPXFragment {

	@Override
	protected GpxDisplayItemType[] filterTypes() {
		return new GpxDisplayItemType[] { GpxSelectionHelper.GpxDisplayItemType.TRACK_SEGMENT };
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		MenuItem item = menu.add(R.string.shared_string_show_on_map).setIcon(R.drawable.ic_show_on_map).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				selectSplitDistance();
				return true;
			}
		});
		MenuItemCompat.setShowAsAction(item, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
	}

	@Override
	public ArrayAdapter<GpxDisplayItem> createSelectedGPXAdapter() {
		return new SegmentGPXAdapter(new ArrayList<GpxDisplayItem>());
	}

	class SegmentGPXAdapter extends ArrayAdapter<GpxDisplayItem> {

		private Map<GpxDisplayItem, GPXItemPagerAdapter> pagerAdaptersMap = new HashMap<>();

		SegmentGPXAdapter(List<GpxDisplayItem> items) {
			super(getActivity(), R.layout.gpx_list_item_tab_content, items);
		}

		@Override
		public void clear() {
			super.clear();
			pagerAdaptersMap.clear();
		}

		private GPXItemPagerAdapter getPagerAdapter(PagerSlidingTabStrip tabs, GpxDisplayItem gpxItem) {
			GPXItemPagerAdapter adapter = pagerAdaptersMap.get(gpxItem);
			if (adapter == null) {
				adapter = new GPXItemPagerAdapter(tabs, gpxItem);
				pagerAdaptersMap.put(gpxItem, adapter);
			}
			return adapter;
		}

		@NonNull
		@Override
		public View getView(int position, View convertView, @NonNull ViewGroup parent) {
			View row = convertView;
			PagerSlidingTabStrip tabLayout = null;
			if (row == null) {
				LayoutInflater inflater = getMyActivity().getLayoutInflater();
				row = inflater.inflate(R.layout.gpx_list_item_tab_content, parent, false);

				boolean light = app.getSettings().isLightContent();
				tabLayout = (PagerSlidingTabStrip) row.findViewById(R.id.sliding_tabs);
				tabLayout.setTabBackground(R.color.color_transparent);
				tabLayout.setIndicatorColorResource(light ? R.color.color_dialog_buttons_light : R.color.color_dialog_buttons_dark);
				tabLayout.setIndicatorBgColorResource(light ? R.color.dashboard_divider_light : R.color.dashboard_divider_dark);
				tabLayout.setIndicatorHeight(AndroidUtils.dpToPx(app, 1f));
				tabLayout.setTextColor(tabLayout.getIndicatorColor());
				tabLayout.setTextSize(AndroidUtils.spToPx(app, 12f));
				tabLayout.setShouldExpand(true);
				tabLayout.setTabSelectionType(PagerSlidingTabStrip.TabSelectionType.SOLID_COLOR);
			}

			if (tabLayout != null) {
				final WrapContentHeightViewPager pager = (WrapContentHeightViewPager) row.findViewById(R.id.pager);
				pager.setAdapter(getPagerAdapter(tabLayout, getItem(position)));
				pager.setSwipeable(false);
				tabLayout.setViewPager(pager);
			}
			
			return row;
		}
	}

	enum GPXTabItemType {
		GPX_TAB_ITEM_GENERAL,
		GPX_TAB_ITEM_ALTITUDE,
		GPX_TAB_ITEM_SPEED
	}

	class GPXItemPagerAdapter extends PagerAdapter implements CustomTabProvider, ViewAtPositionInterface {

		protected SparseArray<View> views = new SparseArray<>();
		private PagerSlidingTabStrip tabs;
		private GpxDisplayItem gpxItem;
		private GPXTabItemType[] tabTypes;
		private String[] titles;

		GPXItemPagerAdapter(PagerSlidingTabStrip tabs, GpxDisplayItem gpxItem) {
			super();
			this.tabs = tabs;
			this.gpxItem = gpxItem;
			fetchTabTypes();
		}

		private void fetchTabTypes() {

			List<GPXTabItemType> tabTypeList = new ArrayList<>();
			tabTypeList.add(GPXTabItemType.GPX_TAB_ITEM_GENERAL);
			if (gpxItem != null && gpxItem.analysis != null) {
				if (gpxItem.analysis.elevationData != null) {
					tabTypeList.add(GPXTabItemType.GPX_TAB_ITEM_ALTITUDE);
				}
				if (gpxItem.analysis.isSpeedSpecified()) {
					tabTypeList.add(GPXTabItemType.GPX_TAB_ITEM_SPEED);
				}
			}
			tabTypes = tabTypeList.toArray(new GPXTabItemType[tabTypeList.size()]);

			Context context = tabs.getContext();
			titles = new String[tabTypes.length];
			for (int i = 0; i < titles.length; i++) {
				switch (tabTypes[i]) {
					case GPX_TAB_ITEM_GENERAL:
						titles[i] = context.getString(R.string.general_settings);
						break;
					case GPX_TAB_ITEM_ALTITUDE:
						titles[i] = context.getString(R.string.altitude);
						break;
					case GPX_TAB_ITEM_SPEED:
						titles[i] = context.getString(R.string.map_widget_speed);
						break;
				}
			}
		}

		@Override
		public int getCount() {
			return tabTypes.length;
		}

		@Override
		public CharSequence getPageTitle(int position) {
			return titles[position];
		}

		@Override
		public Object instantiateItem(ViewGroup container, int position) {

			GPXTabItemType tabType = tabTypes[position];
			View view = null;
			switch (tabType) {
				case GPX_TAB_ITEM_GENERAL:
					view = getActivity().getLayoutInflater().inflate(R.layout.gpx_item_general, container, false);
					break;
				case GPX_TAB_ITEM_ALTITUDE:
					view = getActivity().getLayoutInflater().inflate(R.layout.gpx_item_altitude, container, false);
					break;
				case GPX_TAB_ITEM_SPEED:
					view = getActivity().getLayoutInflater().inflate(R.layout.gpx_item_speed, container, false);
					break;
			}

			if (view != null) {
				OsmandApplication app = (OsmandApplication) getActivity().getApplicationContext();
				if (gpxItem != null) {
					GPXTrackAnalysis analysis = gpxItem.analysis;
					LineChart chart = (LineChart) view.findViewById(R.id.chart);
					chart.setOnTouchListener(new View.OnTouchListener() {
						@Override
						public boolean onTouch(View v, MotionEvent event) {
							getListView().requestDisallowInterceptTouchEvent(true);
							return false;
						}
					});

					IconsCache ic = app.getIconsCache();
					switch (tabType) {
						case GPX_TAB_ITEM_GENERAL:
							if (analysis != null) {
								ArrayList<ILineDataSet> dataSets = new ArrayList<>();
								if (analysis.elevationData != null || analysis.isSpeedSpecified()) {
									GPXUtilities.setupGPXChart(app, chart, 4);
									if (analysis.isSpeedSpecified()) {
										LineDataSet dataSet = GPXUtilities.createGPXSpeedDataSet(app, chart, analysis, true);
										dataSets.add(dataSet);
									}
									if (analysis.elevationData != null) {
										LineDataSet dataSet = GPXUtilities.createGPXElevationDataSet(app, chart, analysis, false);
										dataSets.add(dataSet);
									}
									LineData data = new LineData(dataSets);
									chart.setData(data);
									chart.setVisibility(View.VISIBLE);
								} else {
									chart.setVisibility(View.GONE);
								}

								((ImageView) view.findViewById(R.id.distance_icon))
										.setImageDrawable(ic.getThemedIcon(R.drawable.ic_action_polygom_dark));
								((ImageView) view.findViewById(R.id.duration_icon))
										.setImageDrawable(ic.getThemedIcon(R.drawable.ic_action_time_start));
								((ImageView) view.findViewById(R.id.start_time_icon))
										.setImageDrawable(ic.getThemedIcon(R.drawable.ic_action_time_start));
								((ImageView) view.findViewById(R.id.end_time_icon))
										.setImageDrawable(ic.getThemedIcon(R.drawable.ic_action_time_start));

								((TextView) view.findViewById(R.id.distance_text))
										.setText(OsmAndFormatter.getFormattedDistance(analysis.totalDistance, app));
								((TextView) view.findViewById(R.id.duration_text))
										.setText(Algorithms.formatDuration((int) (analysis.timeSpan / 1000), app.accessibilityEnabled()));

								if (analysis.timeSpan > 0) {
									DateFormat tf = SimpleDateFormat.getTimeInstance(DateFormat.SHORT);
									DateFormat df = SimpleDateFormat.getDateInstance(DateFormat.MEDIUM);

									Date start = new Date(analysis.startTime);
									((TextView) view.findViewById(R.id.start_time_text)).setText(tf.format(start));
									((TextView) view.findViewById(R.id.start_date_text)).setText(df.format(start));
									Date end = new Date(analysis.endTime);
									((TextView) view.findViewById(R.id.end_time_text)).setText(tf.format(end));
									((TextView) view.findViewById(R.id.end_date_text)).setText(df.format(end));
								} else {
									view.findViewById(R.id.list_divider).setVisibility(View.GONE);
									view.findViewById(R.id.start_end_time).setVisibility(View.GONE);
								}
							} else {
								chart.setVisibility(View.GONE);
								view.findViewById(R.id.distance_time_span).setVisibility(View.GONE);
								view.findViewById(R.id.list_divider).setVisibility(View.GONE);
								view.findViewById(R.id.start_end_time).setVisibility(View.GONE);
							}
							view.findViewById(R.id.details_view).setOnClickListener(new View.OnClickListener() {
								@Override
								public void onClick(View v) {
									//todo
								}
							});

							break;
						case GPX_TAB_ITEM_ALTITUDE:
							if (analysis != null) {
								if (analysis.elevationData != null) {
									GPXUtilities.setupGPXChart(app, chart, 4);
									GPXUtilities.setGPXElevationChartData(app, chart, analysis, false);
									chart.setVisibility(View.VISIBLE);
								} else {
									chart.setVisibility(View.GONE);
								}
								((ImageView) view.findViewById(R.id.average_icon))
										.setImageDrawable(ic.getThemedIcon(R.drawable.ic_action_altitude_average));
								((ImageView) view.findViewById(R.id.range_icon))
										.setImageDrawable(ic.getThemedIcon(R.drawable.ic_action_altitude_average));
								((ImageView) view.findViewById(R.id.ascent_icon))
										.setImageDrawable(ic.getThemedIcon(R.drawable.ic_action_altitude_ascent));
								((ImageView) view.findViewById(R.id.descent_icon))
										.setImageDrawable(ic.getThemedIcon(R.drawable.ic_action_altitude_descent));

								String min = OsmAndFormatter.getFormattedAlt(analysis.minElevation, app);
								String max = OsmAndFormatter.getFormattedAlt(analysis.maxElevation, app);
								String asc = OsmAndFormatter.getFormattedAlt(analysis.diffElevationUp, app);
								String desc = OsmAndFormatter.getFormattedAlt(analysis.diffElevationDown, app);

								((TextView) view.findViewById(R.id.average_text))
										.setText(OsmAndFormatter.getFormattedAlt(analysis.avgElevation, app));
								((TextView) view.findViewById(R.id.range_text)).setText(min + " - " + max);
								((TextView) view.findViewById(R.id.ascent_text)).setText(asc);
								((TextView) view.findViewById(R.id.descent_text)).setText(desc);

							} else {
								chart.setVisibility(View.GONE);
								view.findViewById(R.id.average_range).setVisibility(View.GONE);
								view.findViewById(R.id.list_divider).setVisibility(View.GONE);
								view.findViewById(R.id.ascent_descent).setVisibility(View.GONE);
							}
							view.findViewById(R.id.details_view).setOnClickListener(new View.OnClickListener() {
								@Override
								public void onClick(View v) {
									//todo
								}
							});

							break;
						case GPX_TAB_ITEM_SPEED:
							if (analysis != null && analysis.isSpeedSpecified()) {
								GPXUtilities.setupGPXChart(app, chart, 4);
								GPXUtilities.setGPXSpeedChartData(app, chart, analysis, false);

								((ImageView) view.findViewById(R.id.average_icon))
										.setImageDrawable(ic.getThemedIcon(R.drawable.ic_action_speed));
								((ImageView) view.findViewById(R.id.max_icon))
										.setImageDrawable(ic.getThemedIcon(R.drawable.ic_action_max_speed));
								((ImageView) view.findViewById(R.id.time_moving_icon))
										.setImageDrawable(ic.getThemedIcon(R.drawable.ic_action_time_span));
								((ImageView) view.findViewById(R.id.distance_icon))
										.setImageDrawable(ic.getThemedIcon(R.drawable.ic_action_polygom_dark));

								String avg = OsmAndFormatter.getFormattedSpeed(analysis.avgSpeed, app);
								String max = OsmAndFormatter.getFormattedSpeed(analysis.maxSpeed, app);

								((TextView) view.findViewById(R.id.average_text)).setText(avg);
								((TextView) view.findViewById(R.id.max_text)).setText(max);
								((TextView) view.findViewById(R.id.time_moving_text))
										.setText(Algorithms.formatDuration((int) (analysis.timeMoving / 1000), app.accessibilityEnabled()));
								((TextView) view.findViewById(R.id.distance_text))
										.setText(OsmAndFormatter.getFormattedDistance(analysis.totalDistance, app));

							} else {
								chart.setVisibility(View.GONE);
								view.findViewById(R.id.average_max).setVisibility(View.GONE);
								view.findViewById(R.id.list_divider).setVisibility(View.GONE);
								view.findViewById(R.id.time_distance).setVisibility(View.GONE);
							}
							view.findViewById(R.id.details_view).setOnClickListener(new View.OnClickListener() {
								@Override
								public void onClick(View v) {
									//todo
								}
							});
							break;
					}
				}
			}

			container.addView(view, 0);
			views.put(position, view);
			return view;
		}

		@Override
		public void destroyItem(ViewGroup collection, int position, Object view) {
			views.remove(position);
			collection.removeView((View) view);
		}

		@Override
		public boolean isViewFromObject(View view, Object object) {
			return view == object;
		}

		@Override
		public View getCustomTabView(ViewGroup parent, int position) {
			View tab = getActivity().getLayoutInflater().inflate(R.layout.gpx_tab, parent, false);
			tab.setTag(tabTypes[position].name());
			deselect(tab);
			return tab;
		}

		private int getImageId(GPXTabItemType tabType) {
			int imageId;
			switch (tabType) {
				case GPX_TAB_ITEM_GENERAL:
					imageId = R.drawable.ic_action_polygom_dark;
					break;
				case GPX_TAB_ITEM_ALTITUDE:
					imageId = R.drawable.ic_action_altitude_average;
					break;
				case GPX_TAB_ITEM_SPEED:
					imageId = R.drawable.ic_action_speed;
					break;
				default:
					imageId = R.drawable.ic_action_folder_stroke;
			}
			return imageId;
		}

		@Override
		public void select(View tab) {
			GPXTabItemType tabType = GPXTabItemType.valueOf((String)tab.getTag());
			ImageView img = (ImageView) tab.findViewById(R.id.tab_image);
			int imageId = getImageId(tabType);
			switch (tabs.getTabSelectionType()) {
				case ALPHA:
					ViewCompat.setAlpha(img, tabs.getTabTextSelectedAlpha());
					break;
				case SOLID_COLOR:
					img.setImageDrawable(app.getIconsCache().getPaintedIcon(imageId, tabs.getTextColor()));
					break;
			}
		}

		@Override
		public void deselect(View tab) {
			GPXTabItemType tabType = GPXTabItemType.valueOf((String)tab.getTag());
			ImageView img = (ImageView) tab.findViewById(R.id.tab_image);
			int imageId = getImageId(tabType);
			switch (tabs.getTabSelectionType()) {
				case ALPHA:
					ViewCompat.setAlpha(img, tabs.getTabTextAlpha());
					break;
				case SOLID_COLOR:
					img.setImageDrawable(app.getIconsCache().getPaintedIcon(imageId, tabs.getTabInactiveTextColor()));
					break;
			}
		}

		@Override
		public View getViewAtPosition(int position) {
			return views.get(position);
		}
	}
}
