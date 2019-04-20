package net.osmand.plus.mapcontextmenu.other;

import android.content.Context;
import android.graphics.Matrix;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.PopupMenu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.listener.ChartTouchListener.ChartGesture;
import com.github.mikephil.charting.listener.OnChartGestureListener;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;

import net.osmand.AndroidUtils;
import net.osmand.GPXUtilities;
import net.osmand.GPXUtilities.GPXFile;
import net.osmand.GPXUtilities.GPXTrackAnalysis;
import net.osmand.GPXUtilities.TrkSegment;
import net.osmand.GPXUtilities.WptPt;
import net.osmand.data.LatLon;
import net.osmand.data.QuadRect;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.GpxSelectionHelper.GpxDisplayItem;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.dialogs.DirectionsDialogs;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.helpers.GpxUiHelper;
import net.osmand.plus.helpers.GpxUiHelper.GPXDataSetAxisType;
import net.osmand.plus.helpers.GpxUiHelper.GPXDataSetType;
import net.osmand.plus.helpers.GpxUiHelper.OrderedLineDataSet;
import net.osmand.plus.views.mapwidgets.MapInfoWidgetsFactory;
import net.osmand.plus.views.mapwidgets.MapInfoWidgetsFactory.TopToolbarController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class TrackDetailsMenu {

	@Nullable
	private MapActivity mapActivity;
	@Nullable
	private GpxDisplayItem gpxItem;
	@Nullable
	private TrackDetailsBarController toolbarController;
	@Nullable
	private TrkSegment segment;
	@Nullable
	private TrackChartPoints trackChartPoints;
	@Nullable
	private List<WptPt> xAxisPoints;
	private int topMarginPx;
	private boolean visible;
	private boolean hidding;

	public TrackDetailsMenu() {
	}

	@Nullable
	public MapActivity getMapActivity() {
		return mapActivity;
	}

	public void setMapActivity(@Nullable MapActivity mapActivity) {
		this.mapActivity = mapActivity;
		if (mapActivity != null) {
			if (topMarginPx == 0) {
				topMarginPx = AndroidUtils.dpToPx(mapActivity, 48f);
			}
		}
	}

	@Nullable
	public GpxDisplayItem getGpxItem() {
		return gpxItem;
	}

	public void setGpxItem(@NonNull GpxDisplayItem gpxItem) {
		this.gpxItem = gpxItem;
	}

	public boolean isVisible() {
		return visible;
	}

	public void show() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null && getGpxItem() != null) {
			visible = true;
			TrackDetailsMenuFragment.showInstance(mapActivity);
		}
	}

	public void dismiss() {
		TrackDetailsMenuFragment fragment = getMenuFragment();
		if (fragment != null) {
			fragment.dismiss();
		}
	}

	public void hide() {
		TrackDetailsMenuFragment fragment = getMenuFragment();
		if (fragment != null) {
			hidding = true;
			fragment.dismiss();
		} else {
			segment = null;
			trackChartPoints = null;
		}
	}

	public void update() {
		TrackDetailsMenuFragment fragment = getMenuFragment();
		if (fragment != null) {
			fragment.updateInfo();
		}
	}

	private TrackDetailsMenuFragment getMenuFragment() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			TrackDetailsMenuFragment fragment = (TrackDetailsMenuFragment) mapActivity.getSupportFragmentManager()
					.findFragmentByTag(TrackDetailsMenuFragment.TAG);
			if (fragment != null && !fragment.isDetached()) {
				return fragment;
			}
		}
		return null;
	}

	public void onShow() {
		MapActivity mapActivity = getMapActivity();
		GpxDisplayItem gpxItem = getGpxItem();
		if (mapActivity != null && gpxItem != null) {
			OsmandApplication app = mapActivity.getMyApplication();
			GPXFile groupGpx = gpxItem.group.getGpx();
			if (groupGpx != null) {
				gpxItem.wasHidden = app.getSelectedGpxHelper().getSelectedFileByPath(groupGpx.path) == null;
				app.getSelectedGpxHelper().setGpxFileToDisplay(groupGpx);
			}
			boolean portrait = AndroidUiHelper.isOrientationPortrait(mapActivity);
			if (!portrait) {
				mapActivity.getMapView().setMapPositionX(1);
			} else {
				TrackDetailsBarController toolbarController = new TrackDetailsBarController();
				this.toolbarController = toolbarController;
				if (gpxItem.group != null) {
					toolbarController.setTitle(gpxItem.group.getGpxName());
				} else {
					toolbarController.setTitle(mapActivity.getString(R.string.rendering_category_details));
				}
				toolbarController.setOnBackButtonClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						MapActivity mapActivity = getMapActivity();
						if (mapActivity != null) {
							mapActivity.onBackPressed();
						}
					}
				});
				toolbarController.setOnCloseButtonClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						hide();
					}
				});
				mapActivity.showTopToolbar(toolbarController);
			}
			mapActivity.refreshMap();
			mapActivity.getMapLayers().getContextMenuLayer().enterGpxDetailsMode();
		}
	}

	public void onDismiss() {
		GpxDisplayItem gpxItem = getGpxItem();
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			if (gpxItem != null && !gpxItem.route && gpxItem.wasHidden && gpxItem.group != null && gpxItem.group.getGpx() != null) {
				mapActivity.getMyApplication().getSelectedGpxHelper().selectGpxFile(gpxItem.group.getGpx(), false, false);
			}
			TrackDetailsBarController toolbarController = this.toolbarController;
			if (toolbarController != null) {
				mapActivity.hideTopToolbar(toolbarController);
			}
			mapActivity.getMapLayers().getContextMenuLayer().exitGpxDetailsMode();
			mapActivity.getMapLayers().getGpxLayer().setTrackChartPoints(null);
			mapActivity.getMapLayers().getMapInfoLayer().setTrackChartPoints(null);
			mapActivity.getMapView().setMapPositionX(0);
			mapActivity.getMapView().refreshMap();
		}
		if (hidding) {
			hidding = false;
			visible = false;
			segment = null;
			trackChartPoints = null;
		}
	}

	public void updateInfo(final View main) {
		updateView(main);
	}

	private TrkSegment getTrackSegment(LineChart chart) {
		TrkSegment segment = this.segment;
		if (segment == null) {
			List<ILineDataSet> ds = chart.getLineData().getDataSets();
			GpxDisplayItem gpxItem = getGpxItem();
			if (ds != null && ds.size() > 0 && gpxItem != null) {
				for (GPXUtilities.Track t : gpxItem.group.getGpx().tracks) {
					for (TrkSegment s : t.segments) {
						if (s.points.size() > 0 && s.points.get(0).equals(gpxItem.analysis.locationStart)) {
							segment = s;
							break;
						}
					}
					if (segment != null) {
						break;
					}
				}
				this.segment = segment;
			}
		}
		return segment;
	}

	private WptPt getPoint(LineChart chart, float pos) {
		WptPt wpt = null;
		List<ILineDataSet> ds = chart.getLineData().getDataSets();
		GpxDisplayItem gpxItem = getGpxItem();
		if (ds != null && ds.size() > 0 && gpxItem != null) {
			TrkSegment segment = getTrackSegment(chart);
			OrderedLineDataSet dataSet = (OrderedLineDataSet) ds.get(0);
			if (gpxItem.chartAxisType == GPXDataSetAxisType.TIME ||
					gpxItem.chartAxisType == GPXDataSetAxisType.TIMEOFDAY) {
				float time = pos * 1000;
				for (WptPt p : segment.points) {
					if (p.time - gpxItem.analysis.startTime >= time) {
						wpt = p;
						break;
					}
				}
			} else {
				float distance = pos * dataSet.getDivX();
				double previousSplitDistance = 0;
				for (int i = 0; i < segment.points.size(); i++) {
					WptPt currentPoint = segment.points.get(i);
					if (i != 0) {
						WptPt previousPoint = segment.points.get(i - 1);
						if (currentPoint.distance < previousPoint.distance) {
							previousSplitDistance += previousPoint.distance;
						}
					}
					if (previousSplitDistance + currentPoint.distance >= distance) {
						wpt = currentPoint;
						break;
					}
				}
			}
		}
		return wpt;
	}

	private QuadRect getRect(LineChart chart, float startPos, float endPos) {
		double left = 0, right = 0;
		double top = 0, bottom = 0;
		List<ILineDataSet> ds = chart.getLineData().getDataSets();
		GpxDisplayItem gpxItem = getGpxItem();
		if (ds != null && ds.size() > 0 && gpxItem != null) {
			TrkSegment segment = getTrackSegment(chart);
			OrderedLineDataSet dataSet = (OrderedLineDataSet) ds.get(0);
			if (gpxItem.chartAxisType == GPXDataSetAxisType.TIME || gpxItem.chartAxisType == GPXDataSetAxisType.TIMEOFDAY) {
				float startTime = startPos * 1000;
				float endTime = endPos * 1000;
				for (WptPt p : segment.points) {
					if (p.time - gpxItem.analysis.startTime >= startTime && p.time - gpxItem.analysis.startTime <= endTime) {
						if (left == 0 && right == 0) {
							left = p.getLongitude();
							right = p.getLongitude();
							top = p.getLatitude();
							bottom = p.getLatitude();
						} else {
							left = Math.min(left, p.getLongitude());
							right = Math.max(right, p.getLongitude());
							top = Math.max(top, p.getLatitude());
							bottom = Math.min(bottom, p.getLatitude());
						}
					}
				}
			} else {
				float startDistance = startPos * dataSet.getDivX();
				float endDistance = endPos * dataSet.getDivX();
				double previousSplitDistance = 0;
				for (int i = 0; i < segment.points.size(); i++) {
					WptPt currentPoint = segment.points.get(i);
					if (i != 0) {
						WptPt previousPoint = segment.points.get(i - 1);
						if (currentPoint.distance < previousPoint.distance) {
							previousSplitDistance += previousPoint.distance;
						}
					}
					if (previousSplitDistance + currentPoint.distance >= startDistance && previousSplitDistance + currentPoint.distance <= endDistance) {
						if (left == 0 && right == 0) {
							left = currentPoint.getLongitude();
							right = currentPoint.getLongitude();
							top = currentPoint.getLatitude();
							bottom = currentPoint.getLatitude();
						} else {
							left = Math.min(left, currentPoint.getLongitude());
							right = Math.max(right, currentPoint.getLongitude());
							top = Math.max(top, currentPoint.getLatitude());
							bottom = Math.min(bottom, currentPoint.getLatitude());
						}
					}
				}
			}
		}
		return new QuadRect(left, top, right, bottom);
	}

	private void fitTrackOnMap(LineChart chart, LatLon location, boolean forceFit) {
		QuadRect rect = getRect(chart, chart.getLowestVisibleX(), chart.getHighestVisibleX());
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			RotatedTileBox tb = mapActivity.getMapView().getCurrentRotatedTileBox().copy();
			int tileBoxWidthPx = 0;
			int tileBoxHeightPx = 0;

			boolean portrait = AndroidUiHelper.isOrientationPortrait(mapActivity);
			if (!portrait) {
				int width = getFragmentWidth();
				tileBoxWidthPx = width != -1 ? tb.getPixWidth() - width : 0;
			} else {
				int height = getFragmentHeight();
				tileBoxHeightPx = height != -1 ? tb.getPixHeight() - height : 0;
			}
			if (tileBoxHeightPx > 0 || tileBoxWidthPx > 0) {
				if (forceFit) {
					mapActivity.getMapView().fitRectToMap(rect.left, rect.right, rect.top, rect.bottom,
							tileBoxWidthPx, tileBoxHeightPx, topMarginPx);
				} else if (location != null &&
						!mapActivity.getMapView().getTileBox(tileBoxWidthPx, tileBoxHeightPx, topMarginPx).containsLatLon(location)) {
					boolean animating = mapActivity.getMapView().getAnimatedDraggingThread().isAnimating();
					mapActivity.getMapView().fitLocationToMap(location.getLatitude(), location.getLongitude(),
							mapActivity.getMapView().getZoom(), tileBoxWidthPx, tileBoxHeightPx, topMarginPx, !animating);
				} else {
					mapActivity.refreshMap();
				}
			}
		}
	}

	protected int getFragmentWidth() {
		TrackDetailsMenuFragment fragment = getMenuFragment();
		if (fragment != null) {
			return fragment.getWidth();
		}
		return -1;
	}

	protected int getFragmentHeight() {
		TrackDetailsMenuFragment fragment = getMenuFragment();
		if (fragment != null) {
			return fragment.getHeight();
		}
		return -1;
	}

	public void refreshChart(LineChart chart, boolean forceFit) {
		MapActivity mapActivity = getMapActivity();
		GpxDisplayItem gpxItem = getGpxItem();
		if (mapActivity == null || gpxItem == null) {
			return;
		}

		Highlight[] highlights = chart.getHighlighted();
		LatLon location = null;

		TrackChartPoints trackChartPoints = this.trackChartPoints;
		if (trackChartPoints == null) {
			trackChartPoints = new TrackChartPoints();
			TrkSegment segment = getTrackSegment(chart);
			int segmentColor = segment != null ? segment.getColor(0) : 0;
			trackChartPoints.setSegmentColor(segmentColor);
			trackChartPoints.setGpx(gpxItem.group.getGpx());
			this.trackChartPoints = trackChartPoints;
		}

		float minimumVisibleXValue = chart.getLowestVisibleX();
		float maximumVisibleXValue = chart.getHighestVisibleX();

		if (highlights != null && highlights.length > 0) {
			if (minimumVisibleXValue != 0 && maximumVisibleXValue != 0) {
				if (highlights[0].getX() < minimumVisibleXValue) {
					float difference = (maximumVisibleXValue - minimumVisibleXValue) * 0.1f;
					gpxItem.chartHighlightPos = minimumVisibleXValue + difference;
					chart.highlightValue(minimumVisibleXValue + difference, 0);
				} else if (highlights[0].getX() > maximumVisibleXValue) {
					float difference = (maximumVisibleXValue - minimumVisibleXValue) * 0.1f;
					gpxItem.chartHighlightPos = maximumVisibleXValue - difference;
					chart.highlightValue(maximumVisibleXValue - difference, 0);
				} else {
					gpxItem.chartHighlightPos = highlights[0].getX();
				}
			} else {
				gpxItem.chartHighlightPos = highlights[0].getX();
			}
			WptPt wpt = getPoint(chart, gpxItem.chartHighlightPos);
			if (wpt != null) {
				location = new LatLon(wpt.lat, wpt.lon);
				trackChartPoints.setHighlightedPoint(location);
			}
		} else {
			gpxItem.chartHighlightPos = -1;
		}
		trackChartPoints.setXAxisPoints(getXAxisPoints(chart));
		if (gpxItem.route) {
			mapActivity.getMapLayers().getMapInfoLayer().setTrackChartPoints(trackChartPoints);
		} else {
			mapActivity.getMapLayers().getGpxLayer().setTrackChartPoints(trackChartPoints);
		}
		fitTrackOnMap(chart, location, forceFit);
	}

	private List<WptPt> getXAxisPoints(LineChart chart) {
		float[] entries = chart.getXAxis().mEntries;
		float maxXValue = chart.getLineData().getXMax();
		if (entries.length >= 2) {
			float interval = entries[1] - entries[0];
			if (interval > 0) {
				xAxisPoints = new ArrayList<>();
				float currentPointEntry = interval;
				while (currentPointEntry < maxXValue) {
					WptPt pointToAdd = getPoint(chart, currentPointEntry);
					xAxisPoints.add(pointToAdd);
					currentPointEntry += interval;
				}
			}
		}
		return xAxisPoints;
	}

	private void updateView(final View parentView) {
		MapActivity mapActivity = getMapActivity();
		GpxDisplayItem gpxItem = getGpxItem();
		if (mapActivity == null || gpxItem == null) {
			return;
		}
		GPXTrackAnalysis analysis = gpxItem.analysis;
		if (analysis == null || gpxItem.chartTypes == null) {
			parentView.setVisibility(View.GONE);
			if (analysis != null && analysis.isBoundsCalculated()) {
				mapActivity.getMapView()
						.fitRectToMap(analysis.left, analysis.right, analysis.top, analysis.bottom, 0, 0, 0);
			}
			return;
		}

		final LineChart chart = (LineChart) parentView.findViewById(R.id.chart);
		chart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
			@Override
			public void onValueSelected(Entry e, Highlight h) {
				refreshChart(chart, false);
			}

			@Override
			public void onNothingSelected() {

			}
		});
		chart.setOnChartGestureListener(new OnChartGestureListener() {
			boolean hasTranslated = false;
			float highlightDrawX = -1;

			@Override
			public void onChartGestureStart(MotionEvent me, ChartGesture lastPerformedGesture) {
				hasTranslated = false;
				if (chart.getHighlighted() != null && chart.getHighlighted().length > 0) {
					highlightDrawX = chart.getHighlighted()[0].getDrawX();
				} else {
					highlightDrawX = -1;
				}
			}

			@Override
			public void onChartGestureEnd(MotionEvent me, ChartGesture lastPerformedGesture) {
				GpxDisplayItem gpxItem = getGpxItem();
				if (gpxItem != null) {
					if ((lastPerformedGesture == ChartGesture.DRAG && hasTranslated) ||
							lastPerformedGesture == ChartGesture.X_ZOOM ||
							lastPerformedGesture == ChartGesture.Y_ZOOM ||
							lastPerformedGesture == ChartGesture.PINCH_ZOOM ||
							lastPerformedGesture == ChartGesture.DOUBLE_TAP ||
							lastPerformedGesture == ChartGesture.ROTATE) {

						gpxItem.chartMatrix = new Matrix(chart.getViewPortHandler().getMatrixTouch());
						refreshChart(chart, true);
					}
				}
			}

			@Override
			public void onChartLongPressed(MotionEvent me) {
			}

			@Override
			public void onChartDoubleTapped(MotionEvent me) {
			}

			@Override
			public void onChartSingleTapped(MotionEvent me) {
			}

			@Override
			public void onChartFling(MotionEvent me1, MotionEvent me2, float velocityX, float velocityY) {
			}

			@Override
			public void onChartScale(MotionEvent me, float scaleX, float scaleY) {
			}

			@Override
			public void onChartTranslate(MotionEvent me, float dX, float dY) {
				hasTranslated = true;
				if (highlightDrawX != -1) {
					Highlight h = chart.getHighlightByTouchPoint(highlightDrawX, 0f);
					if (h != null) {
						chart.highlightValue(h);
						refreshChart(chart, false);
					}
				}
			}
		});

		final OsmandApplication app = mapActivity.getMyApplication();
		final UiUtilities ic = app.getUIUtilities();

		GpxUiHelper.setupGPXChart(app, chart, 4);

		List<ILineDataSet> dataSets = new ArrayList<>();
		if (gpxItem.chartTypes != null && gpxItem.chartTypes.length > 0) {
			for (int i = 0; i < gpxItem.chartTypes.length; i++) {
				OrderedLineDataSet dataSet = null;
				switch (gpxItem.chartTypes[i]) {
					case ALTITUDE:
						dataSet = GpxUiHelper.createGPXElevationDataSet(app, chart, analysis,
								gpxItem.chartAxisType, false, true);
						break;
					case SPEED:
						dataSet = GpxUiHelper.createGPXSpeedDataSet(app, chart, analysis,
								gpxItem.chartAxisType, gpxItem.chartTypes.length > 1, true);
						break;
					case SLOPE:
						dataSet = GpxUiHelper.createGPXSlopeDataSet(app, chart, analysis,
								gpxItem.chartAxisType, null, gpxItem.chartTypes.length > 1, true);
						break;
				}
				if (dataSet != null) {
					dataSets.add(dataSet);
				}
			}
		}

		Collections.sort(dataSets, new Comparator<ILineDataSet>() {
			@Override
			public int compare(ILineDataSet ds1, ILineDataSet ds2) {
				OrderedLineDataSet dataSet1 = (OrderedLineDataSet) ds1;
				OrderedLineDataSet dataSet2 = (OrderedLineDataSet) ds2;
				return dataSet1.getPriority() > dataSet2.getPriority() ? -1 : (dataSet1.getPriority() == dataSet2.getPriority() ? 0 : 1);
			}
		});
		chart.setData(new LineData(dataSets));
		updateChart(chart);

		View yAxis = parentView.findViewById(R.id.y_axis);
		ImageView yAxisIcon = (ImageView) parentView.findViewById(R.id.y_axis_icon);
		TextView yAxisTitle = (TextView) parentView.findViewById(R.id.y_axis_title);
		View yAxisArrow = parentView.findViewById(R.id.y_axis_arrow);
		final List<GPXDataSetType[]> availableTypes = new ArrayList<>();
		boolean hasSlopeChart = false;
		if (analysis.hasElevationData) {
			availableTypes.add(new GPXDataSetType[]{GPXDataSetType.ALTITUDE});
			if (gpxItem.chartAxisType != GPXDataSetAxisType.TIME
					&& gpxItem.chartAxisType != GPXDataSetAxisType.TIMEOFDAY) {
				availableTypes.add(new GPXDataSetType[]{GPXDataSetType.SLOPE});
			}
		}
		if (analysis.hasSpeedData) {
			availableTypes.add(new GPXDataSetType[]{GPXDataSetType.SPEED});
		}
		if (analysis.hasElevationData && gpxItem.chartAxisType != GPXDataSetAxisType.TIME
				&& gpxItem.chartAxisType != GPXDataSetAxisType.TIMEOFDAY) {
			availableTypes.add(new GPXDataSetType[]{GPXDataSetType.ALTITUDE, GPXDataSetType.SLOPE});
		}
		if (analysis.hasElevationData && analysis.hasSpeedData) {
			availableTypes.add(new GPXDataSetType[]{GPXDataSetType.ALTITUDE, GPXDataSetType.SPEED});
		}

		for (GPXDataSetType t : gpxItem.chartTypes) {
			if (t == GPXDataSetType.SLOPE) {
				hasSlopeChart = true;
				break;
			}
		}
		yAxisIcon.setImageDrawable(GPXDataSetType.getImageDrawable(app, gpxItem.chartTypes));
		yAxisTitle.setText(GPXDataSetType.getName(app, gpxItem.chartTypes));
		if (availableTypes.size() > 0) {
			yAxis.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					PopupMenu optionsMenu = new PopupMenu(v.getContext(), v);
					DirectionsDialogs.setupPopUpMenuIcon(optionsMenu);
					for (final GPXDataSetType[] types : availableTypes) {
						MenuItem menuItem = optionsMenu.getMenu()
								.add(GPXDataSetType.getName(app, types))
								.setIcon(GPXDataSetType.getImageDrawable(app, types));
						menuItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
							@Override
							public boolean onMenuItemClick(MenuItem mItem) {
								GpxDisplayItem gpxItem = getGpxItem();
								gpxItem.chartTypes = types;
								update();
								return true;
							}
						});
					}
					optionsMenu.show();
				}
			});
			yAxisArrow.setVisibility(View.VISIBLE);
		} else {
			yAxis.setOnClickListener(null);
			yAxis.setBackgroundResource(0);
			yAxisArrow.setVisibility(View.GONE);
		}

		View xAxis = parentView.findViewById(R.id.x_axis);
		ImageView xAxisIcon = (ImageView) parentView.findViewById(R.id.x_axis_icon);
		TextView xAxisTitle = (TextView) parentView.findViewById(R.id.x_axis_title);
		View xAxisArrow = parentView.findViewById(R.id.x_axis_arrow);
		if (gpxItem.chartAxisType == GPXDataSetAxisType.TIME) {
			xAxisIcon.setImageDrawable(ic.getThemedIcon(R.drawable.ic_action_time));
			xAxisTitle.setText(app.getString(R.string.shared_string_time));
		} else if (gpxItem.chartAxisType == GPXDataSetAxisType.TIMEOFDAY) {
			xAxisIcon.setImageDrawable(ic.getThemedIcon(R.drawable.ic_action_time_span));
			xAxisTitle.setText(app.getString(R.string.time_of_day));
		} else {
			xAxisIcon.setImageDrawable(ic.getThemedIcon(R.drawable.ic_action_marker_dark));
			xAxisTitle.setText(app.getString(R.string.distance));
		}
		if (analysis.isTimeSpecified() && !hasSlopeChart) {
			xAxis.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					final PopupMenu optionsMenu = new PopupMenu(v.getContext(), v);
					DirectionsDialogs.setupPopUpMenuIcon(optionsMenu);
					for (final GPXDataSetAxisType type : GPXDataSetAxisType.values()) {
						MenuItem menuItem = optionsMenu.getMenu()
								.add(type.getStringId()).setIcon(type.getImageDrawable(app));
						menuItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
							@Override
							public boolean onMenuItemClick(MenuItem mItem) {
								GpxDisplayItem gpxItem = getGpxItem();
								if (gpxItem != null) {
									gpxItem.chartAxisType = type;
									gpxItem.chartHighlightPos = -1;
									gpxItem.chartMatrix = null;
									update();
									return true;
								} else {
									return false;
								}
							}
						});
					}
					optionsMenu.show();
				}
			});
			xAxisArrow.setVisibility(View.VISIBLE);
		} else {
			xAxis.setOnClickListener(null);
			xAxis.setBackgroundResource(0);
			xAxisArrow.setVisibility(View.GONE);
		}

		refreshChart(chart, true);
	}

	private void updateChart(LineChart chart) {
		GpxDisplayItem gpxItem = getGpxItem();
		chart.notifyDataSetChanged();
		chart.invalidate();
		if (gpxItem != null) {
			if (gpxItem.chartMatrix != null) {
				chart.getViewPortHandler().refresh(new Matrix(gpxItem.chartMatrix), chart, true);
			}
			if (gpxItem.chartHighlightPos != -1) {
				chart.highlightValue(gpxItem.chartHighlightPos, 0);
			} else {
				chart.highlightValue(null);
			}
		}
	}

	private static class TrackDetailsBarController extends TopToolbarController {

		TrackDetailsBarController() {
			super(MapInfoWidgetsFactory.TopToolbarControllerType.TRACK_DETAILS);
			setBackBtnIconClrIds(0, 0);
			setRefreshBtnIconClrIds(0, 0);
			setCloseBtnIconClrIds(0, 0);
			setTitleTextClrIds(R.color.primary_text_dark, R.color.primary_text_dark);
			setDescrTextClrIds(R.color.primary_text_dark, R.color.primary_text_dark);
			setBgIds(R.drawable.gradient_toolbar, R.drawable.gradient_toolbar,
					R.drawable.gradient_toolbar, R.drawable.gradient_toolbar);
		}

		@Override
		public void updateToolbar(MapInfoWidgetsFactory.TopToolbarView view) {
			super.updateToolbar(view);
			view.getShadowView().setVisibility(View.GONE);
		}

		@Override
		public int getStatusBarColor(Context context, boolean night) {
			return NO_COLOR;
		}
	}

	public static class TrackChartPoints {
		private List<WptPt> xAxisPoints;
		private LatLon highlightedPoint;
		private int segmentColor;
		private GPXFile gpx;

		public List<WptPt> getXAxisPoints() {
			return xAxisPoints;
		}

		public LatLon getHighlightedPoint() {
			return highlightedPoint;
		}

		public int getSegmentColor() {
			return segmentColor;
		}

		public GPXFile getGpx() {
			return gpx;
		}

		public void setXAxisPoints(List<WptPt> xAxisPoints) {
			this.xAxisPoints = xAxisPoints;
		}

		public void setHighlightedPoint(LatLon highlightedPoint) {
			this.highlightedPoint = highlightedPoint;
		}

		public void setSegmentColor(int segmentColor) {
			this.segmentColor = segmentColor;
		}

		public void setGpx(GPXFile gpx) {
			this.gpx = gpx;
		}
	}
}
