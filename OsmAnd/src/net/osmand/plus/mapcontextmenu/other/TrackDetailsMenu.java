package net.osmand.plus.mapcontextmenu.other;

import android.graphics.Matrix;
import android.support.v4.app.Fragment;
import android.support.v7.widget.PopupMenu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.listener.ChartTouchListener;
import com.github.mikephil.charting.listener.OnChartGestureListener;

import net.osmand.data.LatLon;
import net.osmand.plus.GPXUtilities;
import net.osmand.plus.GPXUtilities.GPXTrackAnalysis;
import net.osmand.plus.GPXUtilities.WptPt;
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
import net.osmand.plus.views.MapControlsLayer;
import net.osmand.plus.views.OsmandMapTileView;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class TrackDetailsMenu {

	private MapActivity mapActivity;
	private OsmandMapTileView mapView;
	private MapControlsLayer mapControlsLayer;
	private GpxDisplayItem gpxItem;

	private static boolean VISIBLE;
	private boolean nightMode;

	public TrackDetailsMenu(MapActivity mapActivity, MapControlsLayer mapControlsLayer) {
		this.mapActivity = mapActivity;
		this.mapControlsLayer = mapControlsLayer;
		mapView = mapActivity.getMapView();
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
			}

			mapActivity.refreshMap();

			TrackDetailsMenuFragment.showInstance(mapActivity);

			mapActivity.findViewById(R.id.MapHudButtonsOverlay).setVisibility(View.INVISIBLE);
		}
	}

	public void hide() {
		WeakReference<TrackDetailsMenuFragment> fragmentRef = findMenuFragment();
		if (fragmentRef != null) {
			fragmentRef.get().dismiss();
		} else {
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
		mapActivity.getMapLayers().getGpxLayer().setSelectedPointLatLon(null);
		mapActivity.getMapView().setMapPositionX(0);
		mapActivity.getMapView().refreshMap();
		mapActivity.findViewById(R.id.MapHudButtonsOverlay).setVisibility(View.VISIBLE);
	}

	public void updateInfo(final View main) {
		nightMode = mapActivity.getMyApplication().getDaynightHelper().isNightModeForMapControls();
		updateView(main);
	}

	private void updateView(final View parentView) {
		final LineChart chart = (LineChart) parentView.findViewById(R.id.chart);
		chart.setOnChartGestureListener(new OnChartGestureListener() {
			@Override
			public void onChartGestureStart(MotionEvent me, ChartTouchListener.ChartGesture lastPerformedGesture) {
			}

			@Override
			public void onChartGestureEnd(MotionEvent me, ChartTouchListener.ChartGesture lastPerformedGesture) {
				gpxItem.chartMatrix = new Matrix(chart.getViewPortHandler().getMatrixTouch());
				Highlight[] highlights = chart.getHighlighted();
				if (highlights != null && highlights.length > 0) {
					gpxItem.chartHighlightPos = highlights[0].getX();

					List<ILineDataSet> ds = chart.getLineData().getDataSets();
					if (ds != null && ds.size() > 0) {
						GPXUtilities.TrkSegment segment = null;
						for (GPXUtilities.Track t : gpxItem.group.getGpx().tracks) {
							for (GPXUtilities.TrkSegment s : t.segments) {
								if (s.points.size() > 0 && s.points.get(0).equals(gpxItem.analysis.locationStart)) {
									segment = s;
									break;
								}
							}
							if (segment != null) {
								break;
							}
						}
						if (segment != null) {
							WptPt wpt = null;
							OrderedLineDataSet dataSet = (OrderedLineDataSet) ds.get(0);
							if (gpxItem.chartAxisType == GPXDataSetAxisType.TIME) {
								float time = gpxItem.chartHighlightPos * 1000;
								for (WptPt p : segment.points) {
									if (p.time - gpxItem.analysis.startTime >= time) {
										wpt = p;
										break;
									}
								}
							} else {
								float distance = gpxItem.chartHighlightPos * dataSet.getDivX();
								for (WptPt p : segment.points) {
									if (p.distance >= distance) {
										wpt = p;
										break;
									}
								}
							}
							if (wpt != null) {
								mapActivity.getMapLayers().getGpxLayer().setSelectedPointLatLon(new LatLon(wpt.lat, wpt.lon));
								mapActivity.setMapLocation(wpt.lat, wpt.lon);
							}
						}
					}
				} else {
					gpxItem.chartHighlightPos = -1;
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
			}
		});

		final OsmandApplication app = mapActivity.getMyApplication();
		final IconsCache ic = app.getIconsCache();
		GPXTrackAnalysis analysis = gpxItem.analysis;
		if (analysis != null) {
			GpxUiHelper.setupGPXChart(app, chart, 4);

			if (gpxItem.chartType != null) {
				List<ILineDataSet> dataSets = new ArrayList<>();
				OrderedLineDataSet dataSet = null;
				if (gpxItem.chartType != null) {
					switch (gpxItem.chartType) {
						case ALTITUDE:
							dataSet = GpxUiHelper.createGPXElevationDataSet(app, chart, analysis,
									gpxItem.chartAxisType, false, true);
							break;
						case SPEED:
							dataSet = GpxUiHelper.createGPXSpeedDataSet(app, chart, analysis,
									gpxItem.chartAxisType, false, true);
							break;
						case SLOPE:
							dataSet = GpxUiHelper.createGPXSlopeDataSet(app, chart, analysis,
									gpxItem.chartAxisType, null, false, true);
							break;
					}
				}
				dataSets.add(dataSet);

				chart.setData(new LineData(dataSets));
				updateChart(chart);
			}
		}

		View yAxis = parentView.findViewById(R.id.y_axis);
		ImageView yAxisIcon = (ImageView) parentView.findViewById(R.id.y_axis_icon);
		TextView yAxisTitle = (TextView) parentView.findViewById(R.id.y_axis_title);
		View yAxisArrow = parentView.findViewById(R.id.y_axis_arrow);
		final List<GPXDataSetType> availableTypes = new ArrayList<>();
		if (analysis != null) {
			if (analysis.elevationData != null) {
				availableTypes.add(GPXDataSetType.ALTITUDE);
				availableTypes.add(GPXDataSetType.SLOPE);
			}
			if (analysis.isSpeedSpecified()) {
				availableTypes.add(GPXDataSetType.SPEED);
			}
		}
		availableTypes.remove(gpxItem.chartType);
		yAxisIcon.setImageDrawable(gpxItem.chartType.getImageDrawable(app));
		yAxisTitle.setText(gpxItem.chartType.getName(app));
		if (availableTypes.size() > 0) {
			yAxis.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					final PopupMenu optionsMenu = new PopupMenu(mapActivity, v);
					DirectionsDialogs.setupPopUpMenuIcon(optionsMenu);
					for (final GPXDataSetType type : availableTypes) {
						MenuItem menuItem = optionsMenu.getMenu().add(type.getStringId()).setIcon(type.getImageDrawable(app));
						menuItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
							@Override
							public boolean onMenuItemClick(MenuItem mItem) {
								gpxItem.chartType = type;
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
		if (analysis != null && analysis.isTimeSpecified()) {
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
			xAxisArrow.setVisibility(View.GONE);
		}
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
}
