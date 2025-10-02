package net.osmand.plus.mapmarkers;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;

import net.osmand.Location;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.OsmAndLocationProvider.OsmAndLocationListener;
import net.osmand.plus.OsmAndTaskManager;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseFullScreenFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.helpers.MapDisplayPositionManager;
import net.osmand.plus.helpers.MapDisplayPositionManager.IMapDisplayPositionProvider;
import net.osmand.plus.helpers.TargetPoint;
import net.osmand.plus.helpers.TargetPointsHelper;
import net.osmand.plus.mapmarkers.PlanRouteOptionsBottomSheetDialogFragment.PlanRouteOptionsFragmentListener;
import net.osmand.plus.mapmarkers.adapters.MapMarkersItemTouchHelperCallback;
import net.osmand.plus.mapmarkers.adapters.MapMarkersListAdapter;
import net.osmand.plus.measurementtool.SnapToRoadBottomSheetDialogFragment;
import net.osmand.plus.measurementtool.SnapToRoadBottomSheetDialogFragment.SnapToRoadFragmentListener;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.enums.MapPosition;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.layers.MapMarkersLayer;
import net.osmand.shared.gpx.primitives.TrkSegment;
import net.osmand.shared.gpx.primitives.WptPt;
import net.osmand.util.MapUtils;

import java.util.ArrayList;
import java.util.List;

public class PlanRouteFragment extends BaseFullScreenFragment
		implements OsmAndLocationListener, IMapDisplayPositionProvider {

	public static final String TAG = "PlanRouteFragment";
	private static final int MIN_DISTANCE_FOR_RECALCULATE = 50; // in meters

	private MapMarkersHelper markersHelper;
	private MarkersPlanRouteContext planRouteContext;

	private MapMarkersListAdapter adapter;
	private PlanRouteToolbarController toolbarController;

	private int selectedCount;
	private int toolbarHeight;
	private int closedListContainerHeight;

	private boolean portrait;
	private boolean fullScreen;
	private boolean isInPlanRouteMode;
	private boolean cancelSnapToRoad = true;

	private Location location;

	private View mainView;
	private RecyclerView markersRv;

	@Override
	protected boolean isUsedOnMap() {
		return true;
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requireMyActivity().getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
			public void handleOnBackPressed() {
				MapActivity mapActivity = getMapActivity();
				if (mapActivity != null && quit(true)) {
					MapMarkersDialogFragment.showInstance(mapActivity);
				}
			}
		});
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
			@Nullable Bundle savedInstanceState) {
		updateNightMode();
		MapActivity mapActivity = getMapActivity();
		markersHelper = app.getMapMarkersHelper();
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
							.setAction(R.string.show_map, view -> showHideMarkersList())
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
			((PlanRouteOptionsBottomSheetDialogFragment) optionsFragment).setListener(createOptionsFragmentListener());
		}

		toolbarHeight = getDimensionPixelSize(R.dimen.dashboard_map_toolbar);

		int backgroundColor = ColorUtilities.getActivityBgColor(mapActivity, nightMode);
		portrait = AndroidUiHelper.isOrientationPortrait(mapActivity);
		fullScreen = portrait && planRouteContext.isMarkersListOpened();
		int layoutRes = fullScreen ? R.layout.fragment_plan_route_full_screen : R.layout.fragment_plan_route_half_screen;

		View view = inflate(layoutRes);

		mainView = fullScreen ? view : view.findViewById(R.id.main_view);

		enterPlanRouteMode();

		View markersListContainer = mainView.findViewById(R.id.markers_list_container);
		if (markersListContainer != null) {
			markersListContainer.setBackgroundColor(backgroundColor);
		}

		if (portrait) {
			mainView.findViewById(R.id.toolbar_divider).setBackgroundColor(ContextCompat.getColor(mapActivity,
					nightMode ? R.color.app_bar_main_dark : R.color.divider_color_light));

			Drawable arrow = getContentIcon(fullScreen ? R.drawable.ic_action_arrow_down : R.drawable.ic_action_arrow_up);
			((ImageView) mainView.findViewById(R.id.up_down_icon)).setImageDrawable(arrow);

			mainView.findViewById(R.id.up_down_row).setOnClickListener(v -> showHideMarkersList());

			mainView.findViewById(R.id.select_all_button).setOnClickListener(v -> {
				selectAllOnClick();
				updateSelectButton();
			});

			int navigationIconResId = AndroidUtils.getNavigationIconResId(mapActivity);
			toolbarController = new PlanRouteToolbarController();
			toolbarController.setBackBtnIconIds(navigationIconResId, navigationIconResId);
			toolbarController.setTitle(getString(R.string.plan_route));
			toolbarController.setOnBackButtonClickListener(v -> {
				if (quit(false)) {
					MapMarkersDialogFragment.showInstance(mapActivity);
				}
			});
			toolbarController.setSaveViewTextId(R.string.shared_string_options);
			toolbarController.setOnSaveViewClickListener(v -> optionsOnClick());
			mapActivity.showTopToolbar(toolbarController);

			if (fullScreen) {
				mapActivity.findViewById(R.id.bottom_controls_container).setVisibility(View.GONE);
				mainView.findViewById(R.id.plan_route_toolbar).setVisibility(View.VISIBLE);
				mainView.findViewById(R.id.toolbar_divider).setVisibility(View.VISIBLE);
			} else {
				int screenH = AndroidUtils.getScreenHeight(mapActivity);
				int statusBarH = AndroidUtils.getStatusBarHeight(mapActivity);
				int navBarH = AndroidUtils.getNavBarHeight(mapActivity);
				int availableHeight = (screenH - statusBarH - navBarH) / 2;

				mainView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
					@Override
					public void onGlobalLayout() {
						int upDownRowH = mainView.findViewById(R.id.up_down_row).getHeight();
						closedListContainerHeight = availableHeight - upDownRowH;
						View listContainer = mainView.findViewById(R.id.markers_list_container);
						listContainer.getLayoutParams().height = closedListContainerHeight;
						listContainer.requestLayout();

						ViewTreeObserver obs = mainView.getViewTreeObserver();
						obs.removeOnGlobalLayoutListener(this);
					}
				});
			}
		}

		Toolbar toolbar = mainView.findViewById(R.id.plan_route_toolbar);
		Drawable icBack = getContentIcon(AndroidUtils.getNavigationIconResId(mapActivity));
		toolbar.setNavigationIcon(icBack);
		toolbar.setNavigationContentDescription(R.string.access_shared_string_navigate_up);
		toolbar.setNavigationOnClickListener(v -> {
			if (quit(false)) {
				MapMarkersDialogFragment.showInstance(mapActivity);
			}
		});

		mainView.findViewById(R.id.options_button).setOnClickListener(v -> optionsOnClick());

		markersRv = mainView.findViewById(R.id.markers_recycler_view);

		adapter = new MapMarkersListAdapter(mapActivity);
		adapter.setHasStableIds(true);
		adapter.setSnappedToRoadPoints(planRouteContext.getSnappedToRoadPoints());
		ItemTouchHelper touchHelper = new ItemTouchHelper(new MapMarkersItemTouchHelperCallback(adapter));
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
					markersHelper.setStartFromMyLocation(!markersHelper.isStartFromMyLocation());
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
					app.getMapMarkersHelper().saveGroups(false);
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
		app.getLocationProvider().addLocationListener(this);
		mapActivity.getMapLayers().getMapControlsLayer().showMapControlsIfHidden();
	}

	@Override
	public void onPause() {
		super.onPause();
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			app.getLocationProvider().removeLocationListener(this);
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
			return nightMode ? R.color.app_bar_main_dark : R.color.status_bar_main_light;
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
			Location location = app.getLocationProvider().getLastStaleKnownLocation();
			boolean newLocation = this.location == null || location == null;
			boolean locationChanged = this.location != null && location != null
					&& this.location.getLatitude() != location.getLatitude()
					&& this.location.getLongitude() != location.getLongitude();
			boolean farEnough = locationChanged && MapUtils.getDistance(this.location.getLatitude(), this.location.getLongitude(),
					location.getLatitude(), location.getLongitude()) >= MIN_DISTANCE_FOR_RECALCULATE;
			if (newLocation || farEnough) {
				app.runInUIThread(() -> {
					PlanRouteFragment.this.location = location;
					adapter.reloadData();
					try {
						adapter.notifyDataSetChanged();
					} catch (Exception e) {
						// to avoid crash because of:
						// java.lang.IllegalStateException: Cannot call this method while RecyclerView is computing a layout or scrolling
					}
				});
			}
		}
	}

	private MapMarkersLayer getMapMarkersLayer() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			return mapActivity.getMapLayers().getMapMarkersLayer();
		}
		return null;
	}

	private Drawable getActiveIcon(@DrawableRes int id) {
		return getIcon(id, nightMode ? R.color.osmand_orange : R.color.color_myloc_distance);
	}

	private void moveMapToPosition(double lat, double lon) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			OsmandMapTileView view = mapActivity.getMapView();
			view.getAnimatedDraggingThread().startMoving(lat, lon, view.getZoom());
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

			private final MapActivity mapActivity = getMapActivity();

			@Override
			public void selectOnClick() {
				selectAllOnClick();
			}

			@Override
			public void navigateOnClick() {
				if (mapActivity != null) {
					boolean hasTargets = false;
					TargetPointsHelper targetPointsHelper = app.getTargetPointsHelper();
					List<MapMarker> markers = markersHelper.getSelectedMarkers();
					if (!markers.isEmpty()) {
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
						if (settings.ROUTE_MAP_MARKERS_ROUND_TRIP.get()) {
							TargetPoint end = targetPointsHelper.getPointToStart();
							if (end == null) {
								Location loc = app.getLocationProvider().getLastKnownLocation();
								if (loc != null) {
									end = TargetPoint.createStartPoint(new LatLon(loc.getLatitude(), loc.getLongitude()),
											new PointDescription(PointDescription.POINT_TYPE_MY_LOCATION,
													mapActivity.getString(R.string.shared_string_my_location)));
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
					mapActivity.getMapActions().doRoute();
				}
			}

			@Override
			public void makeRoundTripOnClick() {
				roundTripOnClick();
			}

			@Override
			public void doorToDoorOnClick() {
				if (mapActivity != null) {
					Location myLoc = app.getLocationProvider().getLastStaleKnownLocation();
					boolean startFromLocation = app.getMapMarkersHelper().isStartFromMyLocation() && myLoc != null;
					if (selectedCount > (startFromLocation ? 0 : 1)) {
						sortSelectedMarkersDoorToDoor(mapActivity, myLoc, startFromLocation);
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
		ProgressBar progressBar = mainView.findViewById(R.id.snap_to_road_progress_bar);
		progressBar.setVisibility(View.VISIBLE);
		progressBar.setMinimumHeight(0);
		progressBar.setProgress(0);
		planRouteContext.setProgressBarVisible(true);
	}

	private void enterPlanRouteMode() {
		MapActivity mapActivity = getMapActivity();
		MapMarkersLayer markersLayer = getMapMarkersLayer();
		if (mapActivity != null && markersLayer != null) {
			isInPlanRouteMode = true;
			markersLayer.setInPlanRouteMode(true);
			mapActivity.disableDrawer();

			AndroidUiHelper.setVisibility(mapActivity, portrait ? View.INVISIBLE : View.GONE,
					R.id.map_left_widgets_panel,
					R.id.map_right_widgets_panel,
					R.id.map_center_info);
			AndroidUiHelper.setVisibility(mapActivity, View.GONE,
					R.id.map_route_info_button,
					R.id.map_menu_button,
					R.id.map_compass_button,
					R.id.map_layers_button,
					R.id.map_search_button,
					R.id.map_quick_actions_button);

			if (planRouteContext.getSnappedMode() == null) {
				planRouteContext.setSnappedMode(ApplicationMode.DEFAULT);
			}
			setupAppModesBtn();
			updateMapDisplayPosition();

			selectedCount = app.getMapMarkersHelper().getSelectedMarkersCount();
			planRouteContext.recreateSnapTrkSegment(planRouteContext.isAdjustMapOnStart());
			planRouteContext.setAdjustMapOnStart(true);
			mapActivity.refreshMap();
			updateSelectButton();
		}
	}

	private void exitPlanRouteMode() {
		MapActivity mapActivity = getMapActivity();
		MapMarkersLayer markersLayer = getMapMarkersLayer();
		if (mapActivity != null && markersLayer != null) {
			isInPlanRouteMode = false;
			markersLayer.setInPlanRouteMode(false);
			mapActivity.enableDrawer();
			if (toolbarController != null) {
				mapActivity.hideTopToolbar(toolbarController);
			}

			AndroidUiHelper.setVisibility(mapActivity, View.VISIBLE,
					R.id.map_left_widgets_panel, R.id.map_right_widgets_panel, R.id.map_center_info,
					R.id.map_route_info_button, R.id.map_menu_button, R.id.map_compass_button,
					R.id.map_layers_button, R.id.map_search_button, R.id.map_quick_actions_button);

			mapActivity.findViewById(R.id.snap_to_road_image_button).setVisibility(View.GONE);
			mainView.findViewById(R.id.snap_to_road_progress_bar).setVisibility(View.GONE);
			mapActivity.findViewById(R.id.bottom_controls_container).setVisibility(View.VISIBLE);

			if (cancelSnapToRoad) {
				planRouteContext.cancelSnapToRoad();
			}
			updateMapDisplayPosition();
			markersLayer.setRoute(null);
			mapActivity.refreshMap();
		}
	}

	private void setupAppModesBtn() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			ImageButton appModesBtn = mapActivity.findViewById(R.id.snap_to_road_image_button);
			appModesBtn.setBackgroundResource(nightMode ? R.drawable.btn_circle_night : R.drawable.btn_circle);
			appModesBtn.setImageDrawable(getActiveIcon(planRouteContext.getSnappedMode().getIconRes()));
			appModesBtn.setOnClickListener(v ->
					SnapToRoadBottomSheetDialogFragment.showInstance(
							mapActivity, createSnapToRoadFragmentListener(), false));
			if (!portrait) {
				FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) appModesBtn.getLayoutParams();
				params.leftMargin = mapActivity.getResources().getDimensionPixelSize(R.dimen.dashboard_land_width);
				appModesBtn.setLayoutParams(params);
			}
			appModesBtn.setVisibility(View.VISIBLE);
		}
	}

	private void optionsOnClick() {
		callMapActivity(mapActivity -> {
			boolean selectAll = !(selectedCount == markersHelper.getMapMarkers().size() && markersHelper.isStartFromMyLocation());
			PlanRouteOptionsBottomSheetDialogFragment.showInstance(mapActivity, selectAll, createOptionsFragmentListener());
		});
	}

	private void updateText() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			TextView distanceTv = mainView.findViewById(R.id.markers_distance_text_view);
			TextView timeTv = mainView.findViewById(R.id.markers_time_text_view);
			TextView countTv = mainView.findViewById(R.id.markers_count_text_view);

			ApplicationMode appMode = planRouteContext.getSnappedMode();
			TrkSegment snapTrkSegment = planRouteContext.getSnapTrkSegment();
			boolean defaultMode = appMode == ApplicationMode.DEFAULT;

			float dist = 0;
			for (int i = 1; i < snapTrkSegment.getPoints().size(); i++) {
				WptPt pt1 = snapTrkSegment.getPoints().get(i - 1);
				WptPt pt2 = snapTrkSegment.getPoints().get(i);
				dist += MapUtils.getDistance(pt1.getLat(), pt1.getLon(), pt2.getLat(), pt2.getLon());
			}
			distanceTv.setText(OsmAndFormatter.getFormattedDistance(dist, app) + (defaultMode ? "" : ","));

			if (defaultMode) {
				timeTv.setText("");
			} else {
				int seconds = (int) (dist / appMode.getDefaultSpeed());
				timeTv.setText("~ " + OsmAndFormatter.getFormattedDuration(seconds, app));
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

	private void showHideMarkersList() {
		if (portrait) {
			callMapActivity(PlanRouteFragment::showInstance);
		}
	}

	private void showMarkersRouteOnMap(boolean adjustMap) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			mapActivity.getMapLayers().getMapMarkersLayer().setRoute(planRouteContext.getSnapTrkSegment());
			mapActivity.refreshMap();
			if (adjustMap) {
				showRouteOnMap(planRouteContext.getSnapTrkSegment().getPoints());
			}
		}
	}

	private void showRouteOnMap(List<WptPt> points) {
		MapActivity mapActivity = getMapActivity();
		if (!points.isEmpty() && mapActivity != null) {
			OsmandMapTileView mapView = mapActivity.getMapView();
			double left = 0, right = 0;
			double top = 0, bottom = 0;
			Location myLocation = app.getLocationProvider().getLastStaleKnownLocation();
			if (app.getMapMarkersHelper().isStartFromMyLocation() && myLocation != null) {
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

			RotatedTileBox tb = mapView.getRotatedTileBox();
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
			activity.getSupportFragmentManager()
					.beginTransaction()
					.remove(this)
					.commitAllowingStateLoss();
		}
	}

	public static boolean showInstance(@NonNull MapActivity mapActivity) {
		FragmentManager fragmentManager = mapActivity.getSupportFragmentManager();
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			boolean portrait = AndroidUiHelper.isOrientationPortrait(mapActivity);
			boolean markersListOpened = mapActivity.getApp().getMapMarkersHelper()
					.getPlanRouteContext()
					.isMarkersListOpened();
			boolean fullscreen = portrait && markersListOpened;
			int containerRes = portrait
					? (fullscreen ? R.id.fragmentContainer : R.id.bottomFragmentContainer)
					: R.id.topFragmentContainer;
			fragmentManager.beginTransaction()
					.add(containerRes, new PlanRouteFragment(), TAG)
					.commitAllowingStateLoss();
			return true;
		}
		return false;
	}

	private void sortSelectedMarkersDoorToDoor(@NonNull MapActivity activity,
			@Nullable Location location, boolean startFromLoc) {
		SortMarkersTask task = new SortMarkersTask(activity, location, startFromLoc, markers -> {
			app.getMapMarkersHelper().addSelectedMarkersToTop(markers);
			adapter.reloadData();
			adapter.notifyDataSetChanged();
			planRouteContext.recreateSnapTrkSegment(false);
			return false;
		});
		OsmAndTaskManager.executeTask(task);
	}

	private void updateMapDisplayPosition() {
		MapDisplayPositionManager manager = app.getMapViewTrackingUtilities().getMapDisplayPositionManager();
		manager.updateMapPositionProviders(this, isInPlanRouteMode);
		manager.updateMapDisplayPosition();
	}

	@Nullable
	@Override
	public MapPosition getMapDisplayPosition() {
		if (isInPlanRouteMode) {
			return portrait ? MapPosition.MIDDLE_TOP : MapPosition.LANDSCAPE_MIDDLE_END;
		}
		return null;
	}
}
