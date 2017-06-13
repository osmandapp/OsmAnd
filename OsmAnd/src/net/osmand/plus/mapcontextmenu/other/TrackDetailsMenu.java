package net.osmand.plus.mapcontextmenu.other;

import android.graphics.Matrix;
import android.support.v4.app.Fragment;
import android.support.v4.util.Pair;
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

import net.osmand.data.LatLon;
import net.osmand.data.QuadRect;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.GPXUtilities;
import net.osmand.plus.GPXUtilities.GPXTrackAnalysis;
import net.osmand.plus.GPXUtilities.TrkSegment;
import net.osmand.plus.GPXUtilities.WptPt;
import net.osmand.plus.GPXUtilities.GPXFile;
import net.osmand.plus.GpxSelectionHelper.GpxDisplayItem;
import net.osmand.plus.IconsCache;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.dialogs.DirectionsDialogs;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.helpers.GpxUiHelper;
import net.osmand.plus.helpers.GpxUiHelper.GPXDataSetAxisType;
import net.osmand.plus.helpers.GpxUiHelper.GPXDataSetType;
import net.osmand.plus.helpers.GpxUiHelper.OrderedLineDataSet;
import net.osmand.plus.views.mapwidgets.MapInfoWidgetsFactory;
import net.osmand.plus.views.mapwidgets.MapInfoWidgetsFactory.TopToolbarController;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class TrackDetailsMenu {

	private MapActivity mapActivity;
	private GpxDisplayItem gpxItem;
	private TrackDetailsBarController toolbarController;
	private TrkSegment segment;
	private TrackChartPoints trackChartPoints;

	private static boolean VISIBLE;

	public TrackDetailsMenu(MapActivity mapActivity) {
		this.mapActivity = mapActivity;
	}

	public GpxDisplayItem getGpxItem() {
		return gpxItem;
	}

	public void setGpxItem(GpxDisplayItem gpxItem) {
		this.gpxItem = gpxItem;
	}

	public static boolean isVisible() {
		return VISIBLE;
	}

	public void show() {
		if (!VISIBLE) {
			VISIBLE = true;
			boolean portrait = AndroidUiHelper.isOrientationPortrait(mapActivity);
			if (!portrait) {
				mapActivity.getMapView().setMapPositionX(1);
			} else {
				toolbarController = new TrackDetailsBarController();
				if (gpxItem != null && gpxItem.group != null) {
					toolbarController.setTitle(gpxItem.group.getGpxName());
				} else {
					toolbarController.setTitle(mapActivity.getString(R.string.rendering_category_details));
				}
				toolbarController.setOnBackButtonClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						mapActivity.onBackPressed();
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

			TrackDetailsMenuFragment.showInstance(mapActivity);
			mapActivity.getMapLayers().getContextMenuLayer().enterGpxDetailsMode();
		}
	}

	public void hide() {
		WeakReference<TrackDetailsMenuFragment> fragmentRef = findMenuFragment();
		if (fragmentRef != null) {
			fragmentRef.get().dismiss();
		} else {
			segment = null;
			VISIBLE = false;
		}
	}

	public void update() {
		WeakReference<TrackDetailsMenuFragment> fragmentRef = findMenuFragment();
		if (fragmentRef != null) {
			fragmentRef.get().updateInfo();
		}
	}

	public WeakReference<TrackDetailsMenuFragment> findMenuFragment() {
		Fragment fragment = mapActivity.getSupportFragmentManager().findFragmentByTag(TrackDetailsMenuFragment.TAG);
		if (fragment != null && !fragment.isDetached()) {
			return new WeakReference<>((TrackDetailsMenuFragment) fragment);
		} else {
			return null;
		}
	}

	public void onDismiss() {
		VISIBLE = false;
		if (gpxItem != null && !gpxItem.route && gpxItem.wasHidden && gpxItem.group != null && gpxItem.group.getGpx() != null) {
			mapActivity.getMyApplication().getSelectedGpxHelper().selectGpxFile(gpxItem.group.getGpx(), false, false);
		}
		if (toolbarController != null) {
			mapActivity.hideTopToolbar(toolbarController);
		}
		mapActivity.getMapLayers().getContextMenuLayer().exitGpxDetailsMode();
		mapActivity.getMapLayers().getGpxLayer().setTrackChartPoints(null);
		mapActivity.getMapLayers().getMapInfoLayer().setTrackChartPoints(null);
		mapActivity.getMapView().setMapPositionX(0);
		mapActivity.getMapView().refreshMap();
		segment = null;
		trackChartPoints = null;
	}

	public void updateInfo(final View main) {
		updateView(main);
	}

	private TrkSegment getTrackSegment(LineChart chart) {
		if (segment == null) {
			List<ILineDataSet> ds = chart.getLineData().getDataSets();
			if (ds != null && ds.size() > 0) {
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
			}
		}
		return segment;
	}

	private WptPt getPoint(LineChart chart, float pos) {
		WptPt wpt = null;
		List<ILineDataSet> ds = chart.getLineData().getDataSets();
		if (ds != null && ds.size() > 0) {
			TrkSegment segment = getTrackSegment(chart);
			OrderedLineDataSet dataSet = (OrderedLineDataSet) ds.get(0);
			if (gpxItem.chartAxisType == GPXDataSetAxisType.TIME) {
				float time = pos * 1000;
				for (WptPt p : segment.points) {
					if (p.time - gpxItem.analysis.startTime >= time) {
						wpt = p;
						break;
					}
				}
			} else {
				float distance = pos * dataSet.getDivX();
				for (WptPt p : segment.points) {
					if (p.distance >= distance) {
						wpt = p;
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
		if (ds != null && ds.size() > 0) {
			TrkSegment segment = getTrackSegment(chart);
			OrderedLineDataSet dataSet = (OrderedLineDataSet) ds.get(0);
			if (gpxItem.chartAxisType == GPXDataSetAxisType.TIME) {
				float startTime = startPos * 1000;
				float endTime = endPos * 1000;
				for (WptPt p : segment.points) {
					if (p.time - gpxItem.analysis.startTime >= startTime &&
							p.time - gpxItem.analysis.startTime <= endTime) {
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
				for (WptPt p : segment.points) {
					if (p.distance >= startDistance && p.distance <= endDistance) {
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
			}
		}
		return new QuadRect(left, top, right, bottom);
	}

	private void fitTrackOnMap(LineChart chart, LatLon location, boolean forceFit) {
		QuadRect rect = getRect(chart, chart.getLowestVisibleX(), chart.getHighestVisibleX());
		if (rect != null) {
			RotatedTileBox tb = mapActivity.getMapView().getCurrentRotatedTileBox().copy();
			int tileBoxWidthPx = 0;
			int tileBoxHeightPx = 0;

			WeakReference<TrackDetailsMenuFragment> fragmentRef = findMenuFragment();
			if (fragmentRef != null) {
				TrackDetailsMenuFragment f = fragmentRef.get();
				boolean portrait = AndroidUiHelper.isOrientationPortrait(mapActivity);
				if (!portrait) {
					tileBoxWidthPx = tb.getPixWidth() - f.getWidth();
				} else {
					tileBoxHeightPx = tb.getPixHeight() - f.getHeight();
				}
			}
			if (forceFit) {
				mapActivity.getMapView().fitRectToMap(rect.left, rect.right, rect.top, rect.bottom,
						tileBoxWidthPx, tileBoxHeightPx, 0);
			} else if (location != null &&
					!mapActivity.getMapView().getTileBox(tileBoxWidthPx, tileBoxHeightPx, 0).containsLatLon(location)) {
				boolean animating = mapActivity.getMapView().getAnimatedDraggingThread().isAnimating();
				mapActivity.getMapView().fitLocationToMap(location.getLatitude(), location.getLongitude(),
						mapActivity.getMapView().getZoom(), tileBoxWidthPx, tileBoxHeightPx, 0, !animating);
			} else {
				mapActivity.refreshMap();
			}

		}
	}

	private void refreshChart(LineChart chart, boolean forceFit) {
		Highlight[] highlights = chart.getHighlighted();
		LatLon location = null;
		if (highlights != null && highlights.length > 0) {
			gpxItem.chartHighlightPos = highlights[0].getX();
			WptPt wpt = getPoint(chart, gpxItem.chartHighlightPos);
			if (wpt != null) {
				if (trackChartPoints == null) {
					trackChartPoints = new TrackChartPoints();
					int segmentColor = getTrackSegment(chart).getColor(0);
					trackChartPoints.setSegmentColor(segmentColor);
					trackChartPoints.setGpx(getGpxItem().group.getGpx());
				}
				location = new LatLon(wpt.lat, wpt.lon);
				List<Pair<String, WptPt>> xAxisPoints = getXAxisPoints(chart);
				trackChartPoints.setHighlightedPoint(location);
				trackChartPoints.setXAxisPoints(xAxisPoints);
				if (gpxItem.route) {
					mapActivity.getMapLayers().getMapInfoLayer().setTrackChartPoints(trackChartPoints);
				} else {
					mapActivity.getMapLayers().getGpxLayer().setTrackChartPoints(trackChartPoints);
				}
			}
		} else {
			gpxItem.chartHighlightPos = -1;
		}
		fitTrackOnMap(chart, location, forceFit);
	}

	private List<Pair<String, WptPt>> getXAxisPoints(LineChart chart) {
		List<Pair<String, WptPt>> xAxisPoints = new ArrayList<>();
		float[] entries = chart.getXAxis().mEntries;
		for (int i = 0; i < entries.length; i++) {
			String formattedEntry = chart.getXAxis().getValueFormatter().getFormattedValue(entries[i], chart.getXAxis());
			WptPt pointToAdd = getPoint(chart, entries[i]);
			xAxisPoints.add(new Pair<>(formattedEntry, pointToAdd));
		}
		return xAxisPoints;
	}

	private void updateView(final View parentView) {
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
		final IconsCache ic = app.getIconsCache();

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
			availableTypes.add(new GPXDataSetType[] { GPXDataSetType.ALTITUDE });
			if (gpxItem.chartAxisType != GPXDataSetAxisType.TIME) {
				availableTypes.add(new GPXDataSetType[]{GPXDataSetType.SLOPE});
			}
		}
		if (analysis.hasSpeedData) {
			availableTypes.add(new GPXDataSetType[] { GPXDataSetType.SPEED });
		}
		if (analysis.hasElevationData && gpxItem.chartAxisType != GPXDataSetAxisType.TIME) {
			availableTypes.add(new GPXDataSetType[] { GPXDataSetType.ALTITUDE, GPXDataSetType.SLOPE });
		}
		if (analysis.hasElevationData && analysis.hasSpeedData) {
			availableTypes.add(new GPXDataSetType[] { GPXDataSetType.ALTITUDE, GPXDataSetType.SPEED });
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
					final PopupMenu optionsMenu = new PopupMenu(mapActivity, v);
					DirectionsDialogs.setupPopUpMenuIcon(optionsMenu);
					for (final GPXDataSetType[] types : availableTypes) {
						MenuItem menuItem = optionsMenu.getMenu()
								.add(GPXDataSetType.getName(app, types))
								.setIcon(GPXDataSetType.getImageDrawable(app, types));
						menuItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
							@Override
							public boolean onMenuItemClick(MenuItem mItem) {
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
		} else {
			xAxisIcon.setImageDrawable(ic.getThemedIcon(R.drawable.ic_action_marker_dark));
			xAxisTitle.setText(app.getString(R.string.distance));
		}
		if (analysis.isTimeSpecified() && !hasSlopeChart) {
			xAxis.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					final PopupMenu optionsMenu = new PopupMenu(mapActivity, v);
					DirectionsDialogs.setupPopUpMenuIcon(optionsMenu);
					final GPXDataSetAxisType type;
					if (gpxItem.chartAxisType == GPXDataSetAxisType.TIME) {
						type = GPXDataSetAxisType.DISTANCE;
					} else {
						type = GPXDataSetAxisType.TIME;
					}
					MenuItem menuItem = optionsMenu.getMenu().add(type.getStringId()).setIcon(type.getImageDrawable(app));
					menuItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
						@Override
						public boolean onMenuItemClick(MenuItem mItem) {
							gpxItem.chartAxisType = type;
							gpxItem.chartHighlightPos = -1;
							gpxItem.chartMatrix = null;
							update();
							return true;
						}
					});
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
		if (gpxItem.chartMatrix != null) {
			chart.getViewPortHandler().refresh(new Matrix(gpxItem.chartMatrix), chart, true);
		}
		if (gpxItem.chartHighlightPos != -1) {
			chart.highlightValue(gpxItem.chartHighlightPos, 0);
		} else {
			chart.highlightValue(null);
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
	}

	public class TrackChartPoints {
		private List<Pair<String, WptPt>> xAxisPoints;
		private LatLon highlightedPoint;
		private int segmentColor;
		private GPXFile gpx;

		public List<Pair<String, WptPt>> getXAxisPoints() {
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

		public void setXAxisPoints(List<Pair<String, WptPt>> xAxisPoints) {
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
