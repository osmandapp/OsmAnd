package net.osmand.plus.mapmarkers;

import android.app.ProgressDialog;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import net.osmand.AndroidUtils;
import net.osmand.Location;
import net.osmand.TspAnt;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.ApplicationMode;
import net.osmand.plus.GPXUtilities.TrkSegment;
import net.osmand.plus.GPXUtilities.WptPt;
import net.osmand.plus.MapMarkersHelper;
import net.osmand.plus.MapMarkersHelper.MapMarker;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmAndLocationProvider.OsmAndLocationListener;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.TargetPointsHelper;
import net.osmand.plus.TargetPointsHelper.TargetPoint;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.mapmarkers.PlanRouteOptionsBottomSheetDialogFragment.PlanRouteOptionsFragmentListener;
import net.osmand.plus.mapmarkers.adapters.MapMarkersItemTouchHelperCallback;
import net.osmand.plus.mapmarkers.adapters.MapMarkersListAdapter;
import net.osmand.plus.measurementtool.SnapToRoadBottomSheetDialogFragment;
import net.osmand.plus.measurementtool.SnapToRoadBottomSheetDialogFragment.SnapToRoadFragmentListener;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.views.MapMarkersLayer;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.mapwidgets.MapInfoWidgetsFactory;
import net.osmand.plus.views.mapwidgets.MapInfoWidgetsFactory.TopToolbarController;
import net.osmand.util.MapUtils;

import java.util.ArrayList;
import java.util.List;

import static net.osmand.plus.OsmandSettings.LANDSCAPE_MIDDLE_RIGHT_CONSTANT;
import static net.osmand.plus.OsmandSettings.MIDDLE_TOP_CONSTANT;

public class PlanRouteFragment extends BaseOsmAndFragment implements OsmAndLocationListener {

	public static final String TAG = "PlanRouteFragment";
	private static final int MIN_DISTANCE_FOR_RECALCULATE = 50; // in meters

	private MapMarkersHelper markersHelper;
	private MarkersPlanRouteContext planRouteContext;

	private MapMarkersListAdapter adapter;
	private PlanRouteToolbarController toolbarController;

	private int previousMapPosition;
	private int selectedCount = 0;
	private int toolbarHeight;
	private int closedListContainerHeight;

	private boolean nightMode;
	private boolean portrait;
	private boolean fullScreen;
	private boolean wasCollapseButtonVisible;
	private boolean cancelSnapToRoad = true;

	private Location location;

	private View mainView;
	private RecyclerView markersRv;

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		final MapActivity mapActivity = getMapActivity();
		markersHelper = mapActivity.getMyApplication().getMapMarkersHelper();
		planRouteContext = markersHelper.getPlanRouteContext();
		planRouteContext.setListener(new MarkersPlanRouteContext.PlanRouteProgressListener() {
			@Override
			public void showProgressBar() {
				PlanRouteFragment.this.showProgressBar();
			}

			@Override
			public void updateProgress(int progress) {
				((ProgressBar) mainView.findViewById(R.id.snap_to_road_progress_bar)).setProgress(progress);
			}

			@Override
			public void hideProgressBar(boolean canceled) {
				mainView.findViewById(R.id.snap_to_road_progress_bar).setVisibility(View.GONE);
				planRouteContext.setProgressBarVisible(false);
				if (!canceled && portrait && planRouteContext.isMarkersListOpened()) {
					Snackbar.make(mainView, getString(R.string.route_is_calculated) + ":", Snackbar.LENGTH_LONG)
							.setAction(R.string.show_map, new View.OnClickListener() {
								@Override
								public void onClick(View view) {
									showHideMarkersList();
								}
							})
							.show();
				}
			}

			@Override
			public void refresh() {
				adapter.notifyDataSetChanged();
				mapActivity.refreshMap();
			}

			@Override
			public void updateText() {
				PlanRouteFragment.this.updateText();
			}

			@Override
			public void showMarkersRouteOnMap(boolean adjustMap) {
				PlanRouteFragment.this.showMarkersRouteOnMap(adjustMap);
			}
		});

		// Handling screen rotation
		FragmentManager fragmentManager = mapActivity.getSupportFragmentManager();
		Fragment snapToRoadFragment = fragmentManager.findFragmentByTag(SnapToRoadBottomSheetDialogFragment.TAG);
		if (snapToRoadFragment != null) {
			((SnapToRoadBottomSheetDialogFragment) snapToRoadFragment).setListener(createSnapToRoadFragmentListener());
		}
		Fragment optionsFragment = fragmentManager.findFragmentByTag(PlanRouteOptionsBottomSheetDialogFragment.TAG);
		if (optionsFragment != null) {
			PlanRouteOptionsBottomSheetDialogFragment fragment = (PlanRouteOptionsBottomSheetDialogFragment) optionsFragment;
			fragment.setSelectAll(!(selectedCount == markersHelper.getMapMarkers().size() && markersHelper.isStartFromMyLocation()));
			fragment.setListener(createOptionsFragmentListener());
		}

		toolbarHeight = mapActivity.getResources().getDimensionPixelSize(R.dimen.dashboard_map_toolbar);

		nightMode = mapActivity.getMyApplication().getDaynightHelper().isNightModeForMapControls();
		final int themeRes = nightMode ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme;
		final int backgroundColor = ContextCompat.getColor(mapActivity,
				nightMode ? R.color.ctx_menu_info_view_bg_dark : R.color.ctx_menu_info_view_bg_light);
		portrait = AndroidUiHelper.isOrientationPortrait(mapActivity);
		fullScreen = portrait && planRouteContext.isMarkersListOpened();
		int layoutRes = fullScreen ? R.layout.fragment_plan_route_full_screen : R.layout.fragment_plan_route_half_screen;

		View view = View.inflate(new ContextThemeWrapper(getContext(), themeRes), layoutRes, null);

		mainView = fullScreen ? view : view.findViewById(R.id.main_view);

		enterPlanRouteMode();

		View markersListContainer = mainView.findViewById(R.id.markers_list_container);
		if (markersListContainer != null) {
			markersListContainer.setBackgroundColor(backgroundColor);
		}

		if (portrait) {
			mainView.findViewById(R.id.toolbar_divider).setBackgroundColor(ContextCompat.getColor(mapActivity,
					nightMode ? R.color.actionbar_dark_color : R.color.dashboard_divider_light));

			Drawable arrow = getContentIcon(fullScreen ? R.drawable.ic_action_arrow_down : R.drawable.ic_action_arrow_up);
			((ImageView) mainView.findViewById(R.id.up_down_icon)).setImageDrawable(arrow);

			mainView.findViewById(R.id.up_down_row).setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					showHideMarkersList();
				}
			});

			mainView.findViewById(R.id.select_all_button).setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					selectAllOnClick();
					updateSelectButton();
				}
			});

			toolbarController = new PlanRouteToolbarController();
			toolbarController.setBackBtnIconIds(R.drawable.ic_action_mode_back, R.drawable.ic_action_mode_back);
			toolbarController.setTitle(getString(R.string.plan_route));
			toolbarController.setOnBackButtonClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					if (quit(false)) {
						MapMarkersDialogFragment.showInstance(mapActivity);
					}
				}
			});
			toolbarController.setSaveViewTextId(R.string.shared_string_options);
			toolbarController.setOnSaveViewClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					optionsOnClick();
				}
			});
			mapActivity.showTopToolbar(toolbarController);

			if (fullScreen) {
				mapActivity.findViewById(R.id.bottom_controls_container).setVisibility(View.GONE);
				mainView.findViewById(R.id.plan_route_toolbar).setVisibility(View.VISIBLE);
				mainView.findViewById(R.id.toolbar_divider).setVisibility(View.VISIBLE);
			} else {
				final int screenH = AndroidUtils.getScreenHeight(mapActivity);
				final int statusBarH = AndroidUtils.getStatusBarHeight(mapActivity);
				final int navBarH = AndroidUtils.getNavBarHeight(mapActivity);
				final int availableHeight = (screenH - statusBarH - navBarH) / 2;

				mainView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
					@Override
					public void onGlobalLayout() {
						int upDownRowH = mainView.findViewById(R.id.up_down_row).getHeight();
						closedListContainerHeight = availableHeight - upDownRowH;
						View listContainer = mainView.findViewById(R.id.markers_list_container);
						listContainer.getLayoutParams().height = closedListContainerHeight;
						listContainer.requestLayout();

						ViewTreeObserver obs = mainView.getViewTreeObserver();
						if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
							obs.removeOnGlobalLayoutListener(this);
						} else {
							obs.removeGlobalOnLayoutListener(this);
						}
					}
				});
			}
		}

		Toolbar toolbar = (Toolbar) mainView.findViewById(R.id.plan_route_toolbar);
		toolbar.setNavigationIcon(getContentIcon(R.drawable.ic_arrow_back));
		toolbar.setNavigationOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (quit(false)) {
					MapMarkersDialogFragment.showInstance(mapActivity);
				}
			}
		});

		mainView.findViewById(R.id.options_button).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				optionsOnClick();
			}
		});

		markersRv = mainView.findViewById(R.id.markers_recycler_view);

		adapter = new MapMarkersListAdapter(mapActivity);
		adapter.setHasStableIds(true);
		adapter.setSnappedToRoadPoints(planRouteContext.getSnappedToRoadPoints());
		final ItemTouchHelper touchHelper = new ItemTouchHelper(new MapMarkersItemTouchHelperCallback(adapter));
		touchHelper.attachToRecyclerView(markersRv);
		adapter.setAdapterListener(new MapMarkersListAdapter.MapMarkersListAdapterListener() {

			private int fromPosition;
			private int toPosition;

			@Override
			public void onDisableRoundTripClick() {
				roundTripOnClick();
			}

			@Override
			public void onCheckBoxClick(View view) {
				int pos = markersRv.getChildAdapterPosition(view);
				if (pos == RecyclerView.NO_POSITION) {
					return;
				}
				Object item = adapter.getItem(pos);
				if (item instanceof Location) {
					markersHelper.setStartFromMyLocation(!mapActivity.getMyApplication().getSettings().ROUTE_MAP_MARKERS_START_MY_LOC.get());
				} else if (item instanceof MapMarker) {
					MapMarker marker = (MapMarker) item;
					selectedCount = marker.selected ? selectedCount - 1 : selectedCount + 1;
					marker.selected = !marker.selected;
					markersHelper.updateMapMarker(marker, false);
				}
				adapter.reloadData();
				adapter.notifyDataSetChanged();
				updateSelectButton();
				planRouteContext.recreateSnapTrkSegment(false);
			}

			@Override
			public void onItemClick(View v) {
				int pos = markersRv.getChildAdapterPosition(v);
				if (pos == RecyclerView.NO_POSITION) {
					return;
				}
				Object item = adapter.getItem(pos);
				if (item instanceof Location) {
					Location loc = (Location) item;
					moveMapToPosition(loc.getLatitude(), loc.getLongitude());
				} else if (item instanceof MapMarker) {
					MapMarker marker = (MapMarker) item;
					moveMapToPosition(marker.getLatitude(), marker.getLongitude());
				}
			}

			@Override
			public void onDragStarted(RecyclerView.ViewHolder holder) {
				fromPosition = holder.getAdapterPosition();
				touchHelper.startDrag(holder);
			}

			@Override
			public void onDragEnded(RecyclerView.ViewHolder holder) {
				toPosition = holder.getAdapterPosition();
				if (toPosition >= 0 && fromPosition >= 0) {
					mapActivity.getMyApplication().getMapMarkersHelper().reorderActiveMarkersIfNeeded();
					mapActivity.getMyApplication().getSettings().MAP_MARKERS_ORDER_BY_MODE.set(OsmandSettings.MapMarkersOrderByMode.CUSTOM);
					mapActivity.refreshMap();
					adapter.reloadData();
					try {
						adapter.notifyDataSetChanged();
					} catch (Exception e) {
						// to avoid crash because of:
						// java.lang.IllegalStateException: Cannot call this method while RecyclerView is computing a layout or scrolling
					}
					planRouteContext.recreateSnapTrkSegment(false);
				}
			}
		});
		markersRv.setPadding(0, (int) mapActivity.getResources().getDimension(R.dimen.map_markers_recycler_view_padding_top),
				0, (int) mapActivity.getResources().getDimension(R.dimen.map_markers_recycler_view_padding_bottom));
		markersRv.setClipToPadding(false);
		markersRv.setLayoutManager(new LinearLayoutManager(getContext()));
		markersRv.setAdapter(adapter);

		if (planRouteContext.isProgressBarVisible()) {
			showProgressBar();
		}

		return view;
	}

	@Override
	public void onResume() {
		super.onResume();
		MapActivity mapActivity = getMapActivity();
		mapActivity.getMyApplication().getLocationProvider().addLocationListener(this);
		mapActivity.getMapLayers().getMapControlsLayer().showMapControlsIfHidden();
	}

	@Override
	public void onPause() {
		super.onPause();
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			mapActivity.getMyApplication().getLocationProvider().removeLocationListener(this);
		}
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		exitPlanRouteMode();
	}

	@Override
	public int getStatusBarColorId() {
		if (fullScreen || !portrait) {
			return nightMode ? R.color.actionbar_dark_color : R.color.status_bar_route_light;
		}
		return R.color.status_bar_transparent_gradient;
	}

	@Override
	protected boolean isFullScreenAllowed() {
		return !(fullScreen || !portrait);
	}

	@Override
	public void updateLocation(Location loc) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			final Location location = mapActivity.getMyApplication().getLocationProvider().getLastStaleKnownLocation();
			boolean newLocation = (this.location == null && location != null) || location == null;
			boolean locationChanged = this.location != null && location != null
					&& this.location.getLatitude() != location.getLatitude()
					&& this.location.getLongitude() != location.getLongitude();
			boolean farEnough = locationChanged && MapUtils.getDistance(this.location.getLatitude(), this.location.getLongitude(),
					location.getLatitude(), location.getLongitude()) >= MIN_DISTANCE_FOR_RECALCULATE;
			if (newLocation || farEnough) {
				mapActivity.getMyApplication().runInUIThread(new Runnable() {
					@Override
					public void run() {
						PlanRouteFragment.this.location = location;
						adapter.reloadData();
						try {
							adapter.notifyDataSetChanged();
						} catch (Exception e) {
							// to avoid crash because of:
							// java.lang.IllegalStateException: Cannot call this method while RecyclerView is computing a layout or scrolling
						}
					}
				});
			}
		}
	}

	private MapActivity getMapActivity() {
		return (MapActivity) getActivity();
	}

	private MapMarkersLayer getMapMarkersLayer() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			return mapActivity.getMapLayers().getMapMarkersLayer();
		}
		return null;
	}

	@Override
	protected Drawable getContentIcon(@DrawableRes int id) {
		return getIcon(id, nightMode ? R.color.ctx_menu_info_text_dark : R.color.icon_color);
	}

	private Drawable getActiveIcon(@DrawableRes int id) {
		return getIcon(id, nightMode ? R.color.osmand_orange : R.color.color_myloc_distance);
	}

	private void moveMapToPosition(double lat, double lon) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			OsmandMapTileView view = mapActivity.getMapView();
			view.getAnimatedDraggingThread().startMoving(lat, lon, view.getZoom(), true);
			if (planRouteContext.isMarkersListOpened()) {
				planRouteContext.setAdjustMapOnStart(false);
				showHideMarkersList();
			}
		}
	}

	private SnapToRoadFragmentListener createSnapToRoadFragmentListener() {
		return new SnapToRoadFragmentListener() {
			@Override
			public void onDestroyView(boolean snapToRoadEnabled) {

			}

			@Override
			public void onApplicationModeItemClick(ApplicationMode mode) {
				if (planRouteContext.getSnappedMode() != mode) {
					boolean defaultMode = mode == ApplicationMode.DEFAULT;
					MapMarkersLayer layer = getMapMarkersLayer();
					if (layer != null) {
						layer.setDefaultAppMode(defaultMode);
					}
					if (defaultMode) {
						planRouteContext.cancelSnapToRoad();
					}
					planRouteContext.getSnappedToRoadPoints().clear();
					planRouteContext.setSnappedMode(mode);
					planRouteContext.recreateSnapTrkSegment(false);
					setupAppModesBtn();
				}
			}
		};
	}

	private PlanRouteOptionsFragmentListener createOptionsFragmentListener() {
		return new PlanRouteOptionsFragmentListener() {

			private MapActivity mapActivity = getMapActivity();

			@Override
			public void selectOnClick() {
				selectAllOnClick();
			}

			@Override
			public void navigateOnClick() {
				if (mapActivity != null) {
					boolean hasTargets = false;
					TargetPointsHelper targetPointsHelper = mapActivity.getMyApplication().getTargetPointsHelper();
					List<MapMarker> markers = markersHelper.getSelectedMarkers();
					if (markers.size() > 0) {
						int i = 0;
						if (markersHelper.isStartFromMyLocation()) {
							targetPointsHelper.clearStartPoint(false);
						} else {
							MapMarker m = markers.get(i++);
							targetPointsHelper.setStartPoint(new LatLon(m.getLatitude(), m.getLongitude()),
									false, m.getPointDescription(mapActivity));
						}
						List<TargetPoint> targetPoints = new ArrayList<>();
						for (int k = i; k < markers.size(); k++) {
							MapMarker m = markers.get(k);
							TargetPoint t = new TargetPoint(new LatLon(m.getLatitude(), m.getLongitude()),
									m.getPointDescription(mapActivity));
							targetPoints.add(t);
						}
						if (mapActivity.getMyApplication().getSettings().ROUTE_MAP_MARKERS_ROUND_TRIP.get()) {
							TargetPoint end = targetPointsHelper.getPointToStart();
							if (end == null) {
								Location loc = mapActivity.getMyApplication().getLocationProvider().getLastKnownLocation();
								if (loc != null) {
									end = TargetPoint.createStartPoint(new LatLon(loc.getLatitude(), loc.getLongitude()),
											new PointDescription(PointDescription.POINT_TYPE_MY_LOCATION,
													getString(R.string.shared_string_my_location)));
								}
							}
							if (end != null) {
								targetPoints.add(end);
							}
						}
						RoutingHelper routingHelper = mapActivity.getRoutingHelper();
						boolean updateRoute = routingHelper.isFollowingMode() || routingHelper.isRoutePlanningMode();
						targetPointsHelper.reorderAllTargetPoints(targetPoints, updateRoute);
						hasTargets = true;
					} else {
						targetPointsHelper.clearStartPoint(false);
						targetPointsHelper.clearPointToNavigate(false);
					}
					planRouteContext.setNavigationFromMarkers(true);
					dismiss();
					mapActivity.getMapLayers().getMapControlsLayer().doRoute(hasTargets);
				}
			}

			@Override
			public void makeRoundTripOnClick() {
				roundTripOnClick();
			}

			@Override
			public void doorToDoorOnClick() {
				if (mapActivity != null) {
					OsmandApplication app = mapActivity.getMyApplication();
					Location myLoc = app.getLocationProvider().getLastStaleKnownLocation();
					boolean startFromLocation = app.getMapMarkersHelper().isStartFromMyLocation() && myLoc != null;
					if (selectedCount > (startFromLocation ? 0 : 1)) {
						sortSelectedMarkersDoorToDoor(mapActivity, startFromLocation, myLoc);
					}
				}
			}

			@Override
			public void reverseOrderOnClick() {
				if (mapActivity != null) {
					markersHelper.reverseActiveMarkersOrder();
					adapter.reloadData();
					adapter.notifyDataSetChanged();
					planRouteContext.recreateSnapTrkSegment(false);
				}
			}
		};
	}

	private void roundTripOnClick() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			OsmandSettings settings = mapActivity.getMyApplication().getSettings();
			settings.ROUTE_MAP_MARKERS_ROUND_TRIP.set(!settings.ROUTE_MAP_MARKERS_ROUND_TRIP.get());
			adapter.reloadData();
			adapter.notifyDataSetChanged();
			planRouteContext.recreateSnapTrkSegment(false);
		}
	}

	private void selectAllOnClick() {
		boolean adjustMap = false;
		int activeMarkersCount = markersHelper.getMapMarkers().size();
		if (selectedCount == activeMarkersCount && markersHelper.isStartFromMyLocation()) {
			markersHelper.deselectAllActiveMarkers();
			markersHelper.setStartFromMyLocation(false);
			selectedCount = 0;
		} else {
			markersHelper.selectAllActiveMarkers();
			markersHelper.setStartFromMyLocation(true);
			selectedCount = activeMarkersCount;
			adjustMap = true;
		}
		adapter.reloadData();
		adapter.notifyDataSetChanged();
		planRouteContext.recreateSnapTrkSegment(adjustMap);
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			mapActivity.refreshMap();
		}
	}

	private void showProgressBar() {
		ProgressBar progressBar = (ProgressBar) mainView.findViewById(R.id.snap_to_road_progress_bar);
		progressBar.setVisibility(View.VISIBLE);
		progressBar.setMinimumHeight(0);
		progressBar.setProgress(0);
		planRouteContext.setProgressBarVisible(true);
	}

	private void enterPlanRouteMode() {
		final MapActivity mapActivity = getMapActivity();
		MapMarkersLayer markersLayer = getMapMarkersLayer();
		if (mapActivity != null && markersLayer != null) {
			markersLayer.setInPlanRouteMode(true);
			mapActivity.disableDrawer();

			mark(portrait ? View.INVISIBLE : View.GONE,
					R.id.map_left_widgets_panel,
					R.id.map_right_widgets_panel,
					R.id.map_center_info);
			mark(View.GONE,
					R.id.map_route_info_button,
					R.id.map_menu_button,
					R.id.map_compass_button,
					R.id.map_layers_button,
					R.id.map_search_button,
					R.id.map_quick_actions_button);

			View collapseButton = mapActivity.findViewById(R.id.map_collapse_button);
			if (collapseButton != null && collapseButton.getVisibility() == View.VISIBLE) {
				wasCollapseButtonVisible = true;
				collapseButton.setVisibility(View.INVISIBLE);
			} else {
				wasCollapseButtonVisible = false;
			}

			if (planRouteContext.getSnappedMode() == null) {
				planRouteContext.setSnappedMode(ApplicationMode.DEFAULT);
			}
			setupAppModesBtn();

			OsmandMapTileView tileView = mapActivity.getMapView();
			previousMapPosition = tileView.getMapPosition();
			tileView.setMapPosition(portrait ? MIDDLE_TOP_CONSTANT : LANDSCAPE_MIDDLE_RIGHT_CONSTANT);

			selectedCount = mapActivity.getMyApplication().getMapMarkersHelper().getSelectedMarkersCount();
			planRouteContext.recreateSnapTrkSegment(planRouteContext.isAdjustMapOnStart());
			planRouteContext.setAdjustMapOnStart(true);
			mapActivity.refreshMap();
			updateSelectButton();
		}
	}

	private void setupAppModesBtn() {
		final MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			final ImageButton appModesBtn = (ImageButton) mapActivity.findViewById(R.id.snap_to_road_image_button);
			appModesBtn.setBackgroundResource(nightMode ? R.drawable.btn_circle_night : R.drawable.btn_circle);
			appModesBtn.setImageDrawable(getActiveIcon(planRouteContext.getSnappedMode().getMapIconId()));
			appModesBtn.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					SnapToRoadBottomSheetDialogFragment fragment = new SnapToRoadBottomSheetDialogFragment();
					fragment.setListener(createSnapToRoadFragmentListener());
					fragment.setRemoveDefaultMode(false);
					fragment.show(mapActivity.getSupportFragmentManager(), SnapToRoadBottomSheetDialogFragment.TAG);
				}
			});
			if (!portrait) {
				LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) appModesBtn.getLayoutParams();
				params.leftMargin = mapActivity.getResources().getDimensionPixelSize(R.dimen.dashboard_land_width);
				appModesBtn.setLayoutParams(params);
			}
			appModesBtn.setVisibility(View.VISIBLE);
		}
	}

	private void exitPlanRouteMode() {
		MapActivity mapActivity = getMapActivity();
		MapMarkersLayer markersLayer = getMapMarkersLayer();
		if (mapActivity != null && markersLayer != null) {
			markersLayer.setInPlanRouteMode(false);
			mapActivity.enableDrawer();
			if (toolbarController != null) {
				mapActivity.hideTopToolbar(toolbarController);
			}

			mark(View.VISIBLE,
					R.id.map_left_widgets_panel,
					R.id.map_right_widgets_panel,
					R.id.map_center_info,
					R.id.map_route_info_button,
					R.id.map_menu_button,
					R.id.map_compass_button,
					R.id.map_layers_button,
					R.id.map_search_button,
					R.id.map_quick_actions_button);

			View collapseButton = mapActivity.findViewById(R.id.map_collapse_button);
			if (collapseButton != null && wasCollapseButtonVisible) {
				collapseButton.setVisibility(View.VISIBLE);
			}

			mapActivity.findViewById(R.id.snap_to_road_image_button).setVisibility(View.GONE);
			mainView.findViewById(R.id.snap_to_road_progress_bar).setVisibility(View.GONE);
			mapActivity.findViewById(R.id.bottom_controls_container).setVisibility(View.VISIBLE);

			mapActivity.getMapView().setMapPosition(previousMapPosition);

			if (cancelSnapToRoad) {
				planRouteContext.cancelSnapToRoad();
			}
			markersLayer.setRoute(null);
			mapActivity.refreshMap();
		}
	}

	private void optionsOnClick() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			PlanRouteOptionsBottomSheetDialogFragment fragment = new PlanRouteOptionsBottomSheetDialogFragment();
			fragment.setUsedOnMap(true);
			fragment.setSelectAll(!(selectedCount == markersHelper.getMapMarkers().size() && markersHelper.isStartFromMyLocation()));
			fragment.setListener(createOptionsFragmentListener());
			fragment.show(mapActivity.getSupportFragmentManager(), PlanRouteOptionsBottomSheetDialogFragment.TAG);
		}
	}

	private void updateText() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			TextView distanceTv = (TextView) mainView.findViewById(R.id.markers_distance_text_view);
			TextView timeTv = (TextView) mainView.findViewById(R.id.markers_time_text_view);
			TextView countTv = (TextView) mainView.findViewById(R.id.markers_count_text_view);

			ApplicationMode appMode = planRouteContext.getSnappedMode();
			TrkSegment snapTrkSegment = planRouteContext.getSnapTrkSegment();
			boolean defaultMode = appMode == ApplicationMode.DEFAULT;

			float dist = 0;
			for (int i = 1; i < snapTrkSegment.points.size(); i++) {
				WptPt pt1 = snapTrkSegment.points.get(i - 1);
				WptPt pt2 = snapTrkSegment.points.get(i);
				dist += MapUtils.getDistance(pt1.lat, pt1.lon, pt2.lat, pt2.lon);
			}
			distanceTv.setText(OsmAndFormatter.getFormattedDistance(dist, mapActivity.getMyApplication()) + (defaultMode ? "" : ","));

			if (defaultMode) {
				timeTv.setText("");
			} else {
				int seconds = (int) (dist / appMode.getDefaultSpeed());
				timeTv.setText("~ " + OsmAndFormatter.getFormattedDuration(seconds, mapActivity.getMyApplication()));
			}

			countTv.setText(mapActivity.getString(R.string.shared_string_markers) + ": " + selectedCount);
		}
	}

	private void updateSelectButton() {
		if (portrait) {
			if (selectedCount == markersHelper.getMapMarkers().size() && markersHelper.isStartFromMyLocation()) {
				((TextView) mainView.findViewById(R.id.select_all_button)).setText(getString(R.string.shared_string_deselect_all));
			} else {
				((TextView) mainView.findViewById(R.id.select_all_button)).setText(getString(R.string.shared_string_select_all));
			}
		}
	}

	private void mark(int status, int... widgets) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			for (int widget : widgets) {
				View v = mapActivity.findViewById(widget);
				if (v != null) {
					v.setVisibility(status);
				}
			}
		}
	}

	private void showHideMarkersList() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null && portrait) {
			cancelSnapToRoad = false;
			planRouteContext.setMarkersListOpened(!planRouteContext.isMarkersListOpened());
			int containerRes = planRouteContext.isMarkersListOpened() ? R.id.fragmentContainer : R.id.bottomFragmentContainer;
			mapActivity.getSupportFragmentManager().beginTransaction()
					.remove(this)
					.add(containerRes, new PlanRouteFragment(), PlanRouteFragment.TAG)
					.commitAllowingStateLoss();
		}
	}

	private void showMarkersRouteOnMap(boolean adjustMap) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			mapActivity.getMapLayers().getMapMarkersLayer().setRoute(planRouteContext.getSnapTrkSegment());
			mapActivity.refreshMap();
			if (adjustMap) {
				showRouteOnMap(planRouteContext.getSnapTrkSegment().points);
			}
		}
	}

	private void showRouteOnMap(List<WptPt> points) {
		MapActivity mapActivity = getMapActivity();
		if (points.size() > 0 && mapActivity != null) {
			OsmandMapTileView mapView = mapActivity.getMapView();
			double left = 0, right = 0;
			double top = 0, bottom = 0;
			Location myLocation = mapActivity.getMyApplication().getLocationProvider().getLastStaleKnownLocation();
			if (mapActivity.getMyApplication().getMapMarkersHelper().isStartFromMyLocation() && myLocation != null) {
				left = myLocation.getLongitude();
				right = myLocation.getLongitude();
				top = myLocation.getLatitude();
				bottom = myLocation.getLatitude();
			}
			for (WptPt pt : points) {
				if (left == 0) {
					left = pt.getLongitude();
					right = pt.getLongitude();
					top = pt.getLatitude();
					bottom = pt.getLatitude();
				} else {
					left = Math.min(left, pt.getLongitude());
					right = Math.max(right, pt.getLongitude());
					top = Math.max(top, pt.getLatitude());
					bottom = Math.min(bottom, pt.getLatitude());
				}
			}

			RotatedTileBox tb = mapView.getCurrentRotatedTileBox().copy();
			int tileBoxWidthPx = 0;
			int tileBoxHeightPx = 0;

			if (portrait) {
				tileBoxHeightPx = 3 * (tb.getPixHeight() - toolbarHeight) / 4;
			} else {
				tileBoxWidthPx = tb.getPixWidth() - mapActivity.getResources().getDimensionPixelSize(R.dimen.dashboard_land_width);
			}
			mapView.fitRectToMap(left, right, top, bottom, tileBoxWidthPx, tileBoxHeightPx, toolbarHeight * 3 / 2);
		}
	}

	public boolean quit(boolean hideMarkersListFirst) {
		if (portrait && planRouteContext.isMarkersListOpened() && hideMarkersListFirst) {
			showHideMarkersList();
			return false;
		} else {
			dismiss();
			return true;
		}
	}

	private void dismiss() {
		MapActivity activity = getMapActivity();
		if (activity != null) {
			planRouteContext.setFragmentVisible(false);
			activity.getSupportFragmentManager().beginTransaction().remove(this).commitAllowingStateLoss();
		}
	}

	public static boolean showInstance(MapActivity mapActivity) {
		try {
			boolean portrait = AndroidUiHelper.isOrientationPortrait(mapActivity);
			boolean fullscreen = portrait && mapActivity.getMyApplication().getMapMarkersHelper().getPlanRouteContext().isMarkersListOpened();
			int containerRes = portrait ? (fullscreen ? R.id.fragmentContainer : R.id.bottomFragmentContainer) : R.id.topFragmentContainer;
			FragmentManager fm = mapActivity.getSupportFragmentManager();
			fm.beginTransaction()
					.add(containerRes, new PlanRouteFragment(), PlanRouteFragment.TAG)
					.commitAllowingStateLoss();
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	private void sortSelectedMarkersDoorToDoor(final MapActivity mapActivity, final boolean startFromLoc, final Location myLoc) {
		new AsyncTask<Void, Void, List<MapMarker>>() {

			private ProgressDialog dialog;
			private long startDialogTime;

			@Override
			protected void onPreExecute() {
				startDialogTime = System.currentTimeMillis();
				dialog = new ProgressDialog(mapActivity);
				dialog.setTitle("");
				dialog.setMessage(mapActivity.getString(R.string.intermediate_items_sort_by_distance));
				dialog.setCancelable(false);
				dialog.show();
			}

			@Override
			protected List<MapMarker> doInBackground(Void... voids) {
				MapMarkersHelper markersHelper = mapActivity.getMyApplication().getMapMarkersHelper();
				List<MapMarker> selectedMarkers = markersHelper.getSelectedMarkers();
				List<LatLon> selectedLatLon = markersHelper.getSelectedMarkersLatLon();

				LatLon start = startFromLoc ? new LatLon(myLoc.getLatitude(), myLoc.getLongitude()) : selectedLatLon.remove(0);
				LatLon end = selectedLatLon.remove(selectedLatLon.size() - 1);

				int[] sequence = new TspAnt().readGraph(selectedLatLon, start, end).solve();

				List<MapMarker> res = new ArrayList<>();
				for (int i = 0; i < sequence.length; i++) {
					if (i == 0 && startFromLoc) {
						continue;
					}
					int index = sequence[i];
					res.add(selectedMarkers.get(startFromLoc ? index - 1 : index));
				}

				return res;
			}

			@Override
			protected void onPostExecute(List<MapMarker> res) {
				if (dialog != null) {
					long t = System.currentTimeMillis();
					if (t - startDialogTime < 500) {
						mapActivity.getMyApplication().runInUIThread(new Runnable() {
							@Override
							public void run() {
								dialog.dismiss();
							}
						}, 500 - (t - startDialogTime));
					} else {
						dialog.dismiss();
					}
				}

				mapActivity.getMyApplication().getMapMarkersHelper().addSelectedMarkersToTop(res);
				adapter.reloadData();
				adapter.notifyDataSetChanged();
				planRouteContext.recreateSnapTrkSegment(false);
			}
		}.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	private class PlanRouteToolbarController extends TopToolbarController {

		PlanRouteToolbarController() {
			super(MapInfoWidgetsFactory.TopToolbarControllerType.MEASUREMENT_TOOL);
			setBackBtnIconClrIds(0, 0);
			setTitleTextClrIds(R.color.primary_text_dark, R.color.primary_text_dark);
			setDescrTextClrIds(R.color.primary_text_dark, R.color.primary_text_dark);
			setBgIds(R.drawable.gradient_toolbar, R.drawable.gradient_toolbar,
					R.drawable.gradient_toolbar, R.drawable.gradient_toolbar);
			setCloseBtnVisible(false);
			setSaveViewVisible(true);
		}

		@Override
		public void updateToolbar(MapInfoWidgetsFactory.TopToolbarView view) {
			super.updateToolbar(view);
			View shadow = view.getShadowView();
			if (shadow != null) {
				shadow.setVisibility(View.GONE);
			}
		}
	}
}
