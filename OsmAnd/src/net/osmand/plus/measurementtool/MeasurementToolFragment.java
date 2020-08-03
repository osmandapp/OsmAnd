package net.osmand.plus.measurementtool;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;
import androidx.core.widget.TextViewCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;

import net.osmand.AndroidUtils;
import net.osmand.FileUtils;
import net.osmand.GPXUtilities;
import net.osmand.GPXUtilities.GPXFile;
import net.osmand.GPXUtilities.Route;
import net.osmand.GPXUtilities.Track;
import net.osmand.GPXUtilities.TrkSegment;
import net.osmand.GPXUtilities.WptPt;
import net.osmand.IndexConstants;
import net.osmand.data.LatLon;
import net.osmand.data.QuadRect;
import net.osmand.plus.GpxSelectionHelper.SelectedGpxFile;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.Version;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.TrackActivity;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.measurementtool.NewGpxData.ActionType;
import net.osmand.plus.measurementtool.OptionsBottomSheetDialogFragment.OptionsFragmentListener;
import net.osmand.plus.measurementtool.RouteBetweenPointsBottomSheetDialogFragment.RouteBetweenPointsFragmentListener;
import net.osmand.plus.measurementtool.SaveAsNewTrackBottomSheetDialogFragment.SaveAsNewTrackFragmentListener;
import net.osmand.plus.measurementtool.SelectedPointBottomSheetDialogFragment.SelectedPointFragmentListener;
import net.osmand.plus.measurementtool.adapter.MeasurementToolAdapter;
import net.osmand.plus.measurementtool.adapter.MeasurementToolAdapter.MeasurementAdapterListener;
import net.osmand.plus.measurementtool.command.AddPointCommand;
import net.osmand.plus.measurementtool.command.ClearPointsCommand;
import net.osmand.plus.measurementtool.command.MovePointCommand;
import net.osmand.plus.measurementtool.command.RemovePointCommand;
import net.osmand.plus.measurementtool.command.ReorderPointCommand;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.views.MapControlsLayer;
import net.osmand.plus.views.controls.ReorderItemTouchHelperCallback;
import net.osmand.plus.views.mapwidgets.MapInfoWidgetsFactory.TopToolbarController;
import net.osmand.plus.views.mapwidgets.MapInfoWidgetsFactory.TopToolbarControllerType;
import net.osmand.plus.views.mapwidgets.MapInfoWidgetsFactory.TopToolbarView;

import java.io.File;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static net.osmand.IndexConstants.GPX_FILE_EXT;
import static net.osmand.plus.measurementtool.MeasurementEditingContext.*;
import static net.osmand.plus.measurementtool.SelectFileBottomSheet.Mode.*;
import static net.osmand.plus.measurementtool.SelectFileBottomSheet.SelectFileListener;
import static net.osmand.plus.measurementtool.SnapTrackWarningBottomSheet.*;
import static net.osmand.plus.measurementtool.StartPlanRouteBottomSheet.StartPlanRouteListener;

public class MeasurementToolFragment extends BaseOsmAndFragment {

	public static final String TAG = MeasurementToolFragment.class.getSimpleName();

	private RecyclerView pointsRv;
	private String previousToolBarTitle = "";
	private MeasurementToolBarController toolBarController;
	private MeasurementToolAdapter adapter;
	private TextView distanceTv;
	private TextView pointsTv;
	private TextView distanceToCenterTv;
	private String pointsSt;
	private Drawable upIcon;
	private Drawable downIcon;
	private View pointsListContainer;
	private View upDownRow;
	private View mainView;
	private ImageView upDownBtn;
	private ImageView undoBtn;
	private ImageView redoBtn;
	private ImageView mainIcon;
	private Snackbar snackbar;

	private boolean wasCollapseButtonVisible;
	private boolean progressBarVisible;
	private boolean pointsListOpened;
	private boolean planRouteMode = false;
	private boolean firstShow = true;
	private Boolean saved;
	private boolean portrait;
	private boolean nightMode;
	private int cachedMapPosition;
	private boolean gpxPointsAdded;

	private MeasurementEditingContext editingCtx = new MeasurementEditingContext();

	private LatLon initialPoint;

	private enum SaveType {
		ROUTE_POINT,
		LINE
	}

	private void setEditingCtx(MeasurementEditingContext editingCtx) {
		this.editingCtx = editingCtx;
	}

	private void setInitialPoint(LatLon initialPoint) {
		this.initialPoint = initialPoint;
	}

	public void setPlanRouteMode(boolean planRouteMode) {
		this.planRouteMode = planRouteMode;
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		final MapActivity mapActivity = (MapActivity) getActivity();
		final MeasurementToolLayer measurementLayer = mapActivity.getMapLayers().getMeasurementToolLayer();

		editingCtx.setApplication(mapActivity.getMyApplication());
		editingCtx.setProgressListener(new SnapToRoadProgressListener() {
			@Override
			public void showProgressBar() {
				MeasurementToolFragment.this.showProgressBar();
			}

			@Override
			public void updateProgress(int progress) {
				((ProgressBar) mainView.findViewById(R.id.snap_to_road_progress_bar)).setProgress(progress);
			}

			@Override
			public void hideProgressBar() {
				((ProgressBar) mainView.findViewById(R.id.snap_to_road_progress_bar)).setVisibility(View.GONE);
				progressBarVisible = false;
			}

			@Override
			public void refresh() {
				measurementLayer.refreshMap();
				updateDistancePointsText();
			}
		});

		measurementLayer.setEditingCtx(editingCtx);

		// Handling screen rotation
		FragmentManager fragmentManager = mapActivity.getSupportFragmentManager();
		Fragment selectedPointFragment = fragmentManager.findFragmentByTag(SelectedPointBottomSheetDialogFragment.TAG);
		if (selectedPointFragment != null) {
			SelectedPointBottomSheetDialogFragment fragment = (SelectedPointBottomSheetDialogFragment) selectedPointFragment;
			fragment.setListener(createSelectedPointFragmentListener());
		}
		Fragment saveAsNewTrackFragment = mapActivity.getSupportFragmentManager().findFragmentByTag(SaveAsNewTrackBottomSheetDialogFragment.TAG);
		if (saveAsNewTrackFragment != null) {
			((SaveAsNewTrackBottomSheetDialogFragment) saveAsNewTrackFragment).setListener(createSaveAsNewTrackFragmentListener());
		}
		// If rotate the screen from landscape to portrait when the list of points is displayed then
		// the RecyclerViewFragment will exist without view. This is necessary to remove it.
		if (!portrait) {
			hidePointsListFragment();
		}

		nightMode = mapActivity.getMyApplication().getDaynightHelper().isNightModeForMapControls();
		portrait = AndroidUiHelper.isOrientationPortrait(mapActivity);

		upIcon = getContentIcon(R.drawable.ic_action_arrow_up);
		downIcon = getContentIcon(R.drawable.ic_action_arrow_down);
		pointsSt = getString(R.string.shared_string_gpx_points).toLowerCase();

		View view = UiUtilities.getInflater(getContext(), nightMode).inflate(R.layout.fragment_measurement_tool, null);

		mainView = view.findViewById(R.id.main_view);
		AndroidUtils.setBackground(mapActivity, mainView, nightMode, R.drawable.bg_bottom_menu_light, R.drawable.bg_bottom_menu_dark);
		pointsListContainer = view.findViewById(R.id.points_list_container);
		if (portrait && pointsListContainer != null) {
			final int backgroundColor = ContextCompat.getColor(mapActivity, nightMode ? R.color.activity_background_color_dark : R.color.activity_background_color_light);
			pointsListContainer.setBackgroundColor(backgroundColor);
		}

		if (progressBarVisible) {
			showProgressBar();
		}

		distanceTv = (TextView) mainView.findViewById(R.id.measurement_distance_text_view);
		pointsTv = (TextView) mainView.findViewById(R.id.measurement_points_text_view);
		distanceToCenterTv = (TextView) mainView.findViewById(R.id.distance_to_center_text_view);

		mainIcon = (ImageView) mainView.findViewById(R.id.main_icon);
		final NewGpxData newGpxData = editingCtx.getNewGpxData();
		if (newGpxData != null) {
			ActionType actionType = newGpxData.getActionType();
			if (actionType == ActionType.ADD_SEGMENT || actionType == ActionType.EDIT_SEGMENT) {
				mainIcon.setImageDrawable(getActiveIcon(R.drawable.ic_action_polygom_dark));
			} else {
				mainIcon.setImageDrawable(getActiveIcon(R.drawable.ic_action_markers_dark));
			}
		} else {
			mainIcon.setImageDrawable(getActiveIcon(R.drawable.ic_action_ruler));
		}

		upDownBtn = (ImageView) mainView.findViewById(R.id.up_down_button);
		upDownBtn.setImageDrawable(upIcon);

		mainView.findViewById(R.id.cancel_move_point_button).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				cancelMovePointMode();
			}
		});

		mainView.findViewById(R.id.cancel_point_before_after_button).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				cancelAddPointBeforeOrAfterMode();
			}
		});

		upDownRow = mainView.findViewById(R.id.up_down_row);
		upDownRow.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (!pointsListOpened && editingCtx.getPointsCount() > 0 && editingCtx.getSelectedPointPosition() == -1) {
					showPointsList();
				} else {
					hidePointsList();
				}
			}
		});

		mainView.findViewById(R.id.apply_move_point_button).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				applyMovePointMode();
			}
		});

		mainView.findViewById(R.id.apply_point_before_after_point_button).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				applyAddPointBeforeAfterMode();
			}
		});

		mainView.findViewById(R.id.add_point_before_after_button).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				addPointBeforeAfter();
			}
		});

		mainView.findViewById(R.id.options_button).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				OptionsBottomSheetDialogFragment.showInstance(mapActivity.getSupportFragmentManager(), true,
						editingCtx.isInSnapToRoadMode(), createOptionsFragmentListener(),
						editingCtx.getSnapToRoadAppMode());
			}
		});

		undoBtn = ((ImageButton) mainView.findViewById(R.id.undo_point_button));
		redoBtn = ((ImageButton) mainView.findViewById(R.id.redo_point_button));

		Drawable undoDrawable = getActiveIcon(R.drawable.ic_action_undo_dark);
		undoBtn.setImageDrawable(AndroidUtils.getDrawableForDirection(mapActivity, undoDrawable));
		undoBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				editingCtx.getCommandManager().undo();
				updateUndoRedoButton(editingCtx.getCommandManager().canUndo(), undoBtn);
				hidePointsListIfNoPoints();
				if (editingCtx.getPointsCount() > 0) {
					enable(upDownBtn);
				}
				adapter.notifyDataSetChanged();
				updateUndoRedoButton(true, redoBtn);
				updateDistancePointsText();
			}
		});

		Drawable redoDrawable = getActiveIcon(R.drawable.ic_action_redo_dark);
		redoBtn.setImageDrawable(AndroidUtils.getDrawableForDirection(mapActivity, redoDrawable));
		redoBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				editingCtx.getCommandManager().redo();
				updateUndoRedoButton(editingCtx.getCommandManager().canRedo(), redoBtn);
				hidePointsListIfNoPoints();
				if (editingCtx.getPointsCount() > 0) {
					enable(upDownBtn);
				}
				adapter.notifyDataSetChanged();
				updateUndoRedoButton(true, undoBtn);
				updateDistancePointsText();
			}
		});

		mainView.findViewById(R.id.add_point_button).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				addCenterPoint();
			}
		});

		measurementLayer.setOnSingleTapListener(new MeasurementToolLayer.OnSingleTapListener() {
			@Override
			public void onAddPoint() {
				addPoint();
			}

			@Override
			public void onSelectPoint(int selectedPointPos) {
				if (pointsListOpened) {
					hidePointsList();
				}
				if (selectedPointPos != -1) {
					openSelectedPointMenu(mapActivity);
				}
			}
		});

		measurementLayer.setOnMeasureDistanceToCenterListener(new MeasurementToolLayer.OnMeasureDistanceToCenter() {
			@Override
			public void onMeasure(float distance, float bearing) {
				String distStr = OsmAndFormatter.getFormattedDistance(distance, mapActivity.getMyApplication());
				String azimuthStr = OsmAndFormatter.getFormattedAzimuth(bearing, getMyApplication());
				distanceToCenterTv.setText(String.format("%1$s • %2$s", distStr, azimuthStr));
				TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(
						distanceToCenterTv, 14, 18, 2,
						TypedValue.COMPLEX_UNIT_SP);
			}
		});

		measurementLayer.setOnEnterMovePointModeListener(new MeasurementToolLayer.OnEnterMovePointModeListener() {
			@Override
			public void onEnterMovePointMode() {
				if (pointsListOpened) {
					hidePointsList();
				}
				switchMovePointMode(true);
			}
		});

		if (!editingCtx.getCommandManager().canUndo()) {
			updateUndoRedoButton(false, undoBtn);
		}
		if (!editingCtx.getCommandManager().canRedo()) {
			updateUndoRedoButton(false, redoBtn);
		}
		if (editingCtx.getPointsCount() < 1) {
			disable(upDownBtn);
		}

		toolBarController = new MeasurementToolBarController();
		if (editingCtx.getSelectedPointPosition() != -1) {
			int navigationIconResId = AndroidUtils.getNavigationIconResId(mapActivity);
			toolBarController.setBackBtnIconIds(navigationIconResId, navigationIconResId);
		} else {
			toolBarController.setBackBtnIconIds(R.drawable.ic_action_remove_dark, R.drawable.ic_action_remove_dark);
		}
		if (newGpxData != null) {
			ActionType actionType = newGpxData.getActionType();
			if (actionType == ActionType.ADD_ROUTE_POINTS) {
				toolBarController.setTitle(getString(R.string.add_route_points));
			} else if (actionType == ActionType.ADD_SEGMENT) {
				toolBarController.setTitle(getString(R.string.add_line));
			} else if (actionType == ActionType.EDIT_SEGMENT) {
				toolBarController.setTitle(getString(R.string.edit_line));
			}
		} else {
			toolBarController.setTitle(getString(R.string.plan_route));
		}
		toolBarController.setOnBackButtonClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				quit(false);
			}
		});
		toolBarController.setOnSaveViewClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (editingCtx.getPointsCount() > 0) {
					if (newGpxData != null && newGpxData.getActionType() == ActionType.EDIT_SEGMENT
							&& editingCtx.isInSnapToRoadMode()) {
						if (mapActivity != null) {
							if (editingCtx.getPointsCount() > 0) {
								openSaveAsNewTrackMenu(mapActivity);
							} else {
								Toast.makeText(mapActivity, getString(R.string.none_point_error), Toast.LENGTH_SHORT).show();
							}
						}
					} else {
						if (newGpxData == null) {
							final File dir = mapActivity.getMyApplication().getAppPath(IndexConstants.GPX_INDEX_DIR);
							String fileName = getSuggestedName(dir) + GPX_FILE_EXT;
							saveNewGpx(dir, fileName, true, SaveType.ROUTE_POINT, true);
						} else {
							addToGpx(mapActivity);
						}
					}
				} else {
					Toast.makeText(mapActivity, getString(R.string.none_point_error), Toast.LENGTH_SHORT).show();
				}
			}
		});
		toolBarController.setOnSwitchCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
				if (!checked) {
					disableSnapToRoadMode();
				}
			}
		});
		mapActivity.showTopToolbar(toolBarController);

		adapter = new MeasurementToolAdapter(getMapActivity(), editingCtx.getPoints(),
				newGpxData != null ? newGpxData.getActionType() : null);
		if (portrait) {
			pointsRv = mainView.findViewById(R.id.measure_points_recycler_view);
		} else {
			pointsRv = new RecyclerView(getActivity());
		}
		ItemTouchHelper touchHelper = new ItemTouchHelper(new ReorderItemTouchHelperCallback(adapter));
		touchHelper.attachToRecyclerView(pointsRv);
		adapter.setAdapterListener(createMeasurementAdapterListener(touchHelper));
		pointsRv.setLayoutManager(new LinearLayoutManager(getContext()));
		pointsRv.setAdapter(adapter);

		initMeasurementMode(newGpxData);

		if (planRouteMode && firstShow) {
			StartPlanRouteBottomSheet.showInstance(mapActivity.getSupportFragmentManager(),
					createStartPlanRouteListener());
			firstShow = false;
		}
		return view;
	}

	private void initMeasurementMode(NewGpxData newGpxData) {
		editingCtx.getCommandManager().resetMeasurementLayer(getMapActivity().getMapLayers().getMeasurementToolLayer());
		enterMeasurementMode();

		showSnapToRoadControls();

		if (newGpxData != null && !gpxPointsAdded) {
			List<WptPt> points = newGpxData.getGpxFile().getRoutePoints();
			if (!points.isEmpty()) {
				ApplicationMode snapToRoadAppMode = ApplicationMode
						.valueOfStringKey(points.get(points.size() - 1).getProfileType(), null);
				if (snapToRoadAppMode != null) {
					enableSnapToRoadMode(snapToRoadAppMode);
				}
			}
			ActionType actionType = newGpxData.getActionType();
			if (actionType == ActionType.ADD_ROUTE_POINTS) {
				displayRoutePoints();
				gpxPointsAdded = true;
			} else if (actionType == ActionType.EDIT_SEGMENT) {
				displaySegmentPoints();
				gpxPointsAdded = true;
			}
		}

		if (saved == null) {
			saved = newGpxData != null && (newGpxData.getActionType() == ActionType.ADD_ROUTE_POINTS || newGpxData.getActionType() == ActionType.EDIT_SEGMENT);
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			mapActivity.getMapLayers().getMapControlsLayer().showMapControlsIfHidden();
			cachedMapPosition = mapActivity.getMapView().getMapPosition();
			setDefaultMapPosition();
			addInitialPoint();
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		setMapPosition(cachedMapPosition);
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		cancelModes();
		exitMeasurementMode();
		adapter.setAdapterListener(null);
		if (pointsListOpened) {
			hidePointsList();
		}
		MeasurementToolLayer layer = getMeasurementLayer();
		if (layer != null) {
			layer.setOnSingleTapListener(null);
			layer.setOnEnterMovePointModeListener(null);
		}
	}

	@Override
	public int getStatusBarColorId() {
		return R.color.status_bar_transparent_gradient;
	}

	@Nullable
	private MapActivity getMapActivity() {
		Activity activity = getActivity();
		if (activity instanceof MapActivity && !activity.isFinishing()) {
			return (MapActivity) activity;
		}
		return null;
	}

	private MeasurementToolLayer getMeasurementLayer() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			return mapActivity.getMapLayers().getMeasurementToolLayer();
		}
		return null;
	}

	@Override
	protected Drawable getContentIcon(@DrawableRes int id) {
		return getIcon(id, nightMode ? R.color.icon_color_default_dark : R.color.icon_color_default_light);
	}

	private Drawable getActiveIcon(@DrawableRes int id) {
		return getIcon(id, nightMode ? R.color.osmand_orange : R.color.color_myloc_distance);
	}

	private void showProgressBar() {
		ProgressBar progressBar = (ProgressBar) mainView.findViewById(R.id.snap_to_road_progress_bar);
		progressBar.setVisibility(View.VISIBLE);
		progressBar.setMinimumHeight(0);
		progressBar.setProgress(0);
		progressBarVisible = true;
	}

	private void showRouteBetweenPointsMenu(boolean rememberPreviousTitle) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			if (rememberPreviousTitle) {
				previousToolBarTitle = toolBarController.getTitle();
			}
			toolBarController.setTitle(getString(R.string.route_between_points));
			mapActivity.refreshMap();
			if(editingCtx.isSnapToRoadTrack()) {
				RouteBetweenPointsBottomSheetDialogFragment.showInstance(mapActivity.getSupportFragmentManager(),
						createRouteBetweenPointsFragmentListener(), editingCtx.getCalculationType(),
						editingCtx.getSnapToRoadAppMode());
			} else {
				toolBarController.setSaveViewVisible(false);
				updateToolbar();
				SnapTrackWarningBottomSheet.showInstance(mapActivity.getSupportFragmentManager(),
						createSnapTrackWarningListener());
			}
		}
	}

	private SnapTrackWarningListener createSnapTrackWarningListener() {
		return new SnapTrackWarningListener() {
			@Override
			public void continueButtonOnClick() {
				MapActivity mapActivity = getMapActivity();
				if (mapActivity != null) {

					GpxApproximationFragment.showInstance(mapActivity.getSupportFragmentManager());
				}
			}

			@Override
			public void dismissButtonOnClick() {
				toolBarController.setSaveViewVisible(true);
				updateToolbar();
			}
		};
	}

	private OptionsFragmentListener createOptionsFragmentListener() {
		return new OptionsFragmentListener() {

			final MapActivity mapActivity = getMapActivity();
			final MeasurementToolLayer measurementLayer = getMeasurementLayer();

			@Override
			public void snapToRoadOnCLick() {
				showRouteBetweenPointsMenu(true);
			}

			@Override
			public void directionsOnClick() {
				if (mapActivity != null) {
					MapControlsLayer mapControlsLayer = mapActivity.getMapLayers().getMapControlsLayer();
					if (mapControlsLayer != null) {
						mapControlsLayer.doRoute(false);
					}
				}
			}

			@Override
			public void addToGpxOnClick() {
				if (mapActivity != null && measurementLayer != null) {
					if (editingCtx.getPointsCount() > 0) {
						if (editingCtx.isInSnapToRoadMode()) {
							editingCtx.getPoints().clear();
							editingCtx.getPoints().addAll(editingCtx.getBeforePoints());
							editingCtx.getBeforePoints().clear();
							editingCtx.getBeforePoints().addAll(editingCtx.getBeforeTrkSegmentLine().points);
						}
						addToGpx(mapActivity);
					} else {
						Toast.makeText(mapActivity, getString(R.string.none_point_error), Toast.LENGTH_SHORT).show();
					}
				}
			}

			@Override
			public void saveAsNewTrackOnClick() {
				if (mapActivity != null && measurementLayer != null) {
					if (editingCtx.getPointsCount() > 0) {
						openSaveAsNewTrackMenu(mapActivity);
					} else {
						Toast.makeText(mapActivity, getString(R.string.none_point_error), Toast.LENGTH_SHORT).show();
					}
				}
			}

			@Override
			public void addToTheTrackOnClick() {
				if (mapActivity != null && measurementLayer != null) {
					if (editingCtx.getPointsCount() > 0) {
						showAddToTrackDialog(mapActivity);
					} else {
						Toast.makeText(mapActivity, getString(R.string.none_point_error), Toast.LENGTH_SHORT).show();
					}
				}
			}

			@Override
			public void clearAllOnClick() {
				editingCtx.getCommandManager().execute(new ClearPointsCommand(measurementLayer));
				editingCtx.cancelSnapToRoad();
				if (pointsListOpened) {
					hidePointsList();
				}
				updateUndoRedoButton(false, redoBtn);
				disable(upDownBtn);
				updateDistancePointsText();
				saved = false;
			}

			@Override
			public void reverseRouteOnClick() {

			}
		};
	}

	private SelectedPointFragmentListener createSelectedPointFragmentListener() {
		return new SelectedPointFragmentListener() {

			final MeasurementToolLayer measurementLayer = getMeasurementLayer();

			@Override
			public void moveOnClick() {
				if (measurementLayer != null) {
					measurementLayer.enterMovingPointMode();
				}
				switchMovePointMode(true);
			}

			@Override
			public void deleteOnClick() {
				if (measurementLayer != null) {
					removePoint(measurementLayer, editingCtx.getSelectedPointPosition());
				}
				editingCtx.setSelectedPointPosition(-1);
			}

			@Override
			public void addPointAfterOnClick() {
				if (measurementLayer != null) {
					measurementLayer.moveMapToPoint(editingCtx.getSelectedPointPosition());
					editingCtx.setInAddPointMode(true);
					editingCtx.splitSegments(editingCtx.getSelectedPointPosition() + 1);
				}
				((TextView) mainView.findViewById(R.id.add_point_before_after_text)).setText(mainView.getResources().getString(R.string.add_point_after));
				mainIcon.setImageDrawable(getActiveIcon(R.drawable.ic_action_addpoint_above));
				switchAddPointBeforeAfterMode(true);
			}

			@Override
			public void addPointBeforeOnClick() {
				if (measurementLayer != null) {
					measurementLayer.moveMapToPoint(editingCtx.getSelectedPointPosition());
					editingCtx.setInAddPointMode(true);
					editingCtx.splitSegments(editingCtx.getSelectedPointPosition());
				}
				((TextView) mainView.findViewById(R.id.add_point_before_after_text)).setText(mainView.getResources().getString(R.string.add_point_before));
				mainIcon.setImageDrawable(getActiveIcon(R.drawable.ic_action_addpoint_below));
				switchAddPointBeforeAfterMode(true);
			}

			@Override
			public void onCloseMenu() {
				setDefaultMapPosition();
			}

			@Override
			public void onClearSelection() {
				editingCtx.setSelectedPointPosition(-1);
			}
		};
	}

	private RouteBetweenPointsFragmentListener createRouteBetweenPointsFragmentListener() {
		return new RouteBetweenPointsFragmentListener() {
			@Override
			public void onDestroyView(boolean snapToRoadEnabled) {
				if (!snapToRoadEnabled && !editingCtx.isInSnapToRoadMode()) {
					toolBarController.setTitle(previousToolBarTitle);
					MapActivity mapActivity = getMapActivity();
					if (mapActivity != null) {
						mapActivity.refreshMap();
					}
				}
			}

			@Override
			public void onApplicationModeItemClick(ApplicationMode mode) {
				if (mode == null) {
					disableSnapToRoadMode();
					editingCtx.setSnapToRoadAppMode(null);
					showSnapToRoadControls();
				} else {
					enableSnapToRoadMode(mode);
				}
			}

			@Override
			public void onCalculationTypeChanges(CalculationType calculationType) {
				editingCtx.setCalculationType(calculationType);
			}
		};
	}

	private StartPlanRouteListener createStartPlanRouteListener() {
		return new StartPlanRouteListener() {
			@Override
			public void openExistingTrackOnClick() {
				MapActivity mapActivity = getMapActivity();
				if (mapActivity != null) {
					SelectFileBottomSheet.showInstance(mapActivity.getSupportFragmentManager(),
							createSelectFileListener(), OPEN_TRACK);
				}
			}

			@Override
			public void openLastEditTrackOnClick(String gpxFileName) {
				addNewGpxData(getGpxFile(gpxFileName));
			}

			@Override
			public void dismissButtonOnClick() {
				quit(true);
			}
		};
	}

	private SelectFileListener createSelectFileListener() {
		return new SelectFileListener() {
			@Override
			public void selectFileOnCLick(String gpxFileName) {
				addNewGpxData(getGpxFile(gpxFileName));
			}

			@Override
			public void dismissButtonOnClick() {
				MapActivity mapActivity = getMapActivity();
				if (mapActivity != null) {
					StartPlanRouteBottomSheet.showInstance(mapActivity.getSupportFragmentManager(),
							createStartPlanRouteListener());
				}
			}
		};
	}

	private GPXFile getGpxFile(String gpxFileName) {
		OsmandApplication app = getMyApplication();
		GPXFile gpxFile = null;
		if (app != null) {
			SelectedGpxFile selectedGpxFile = app.getSelectedGpxHelper().getSelectedFileByName(gpxFileName);
			if (selectedGpxFile != null) {
				gpxFile = selectedGpxFile.getGpxFile();
			} else {
				gpxFile = GPXUtilities.loadGPXFile(new File(
						getMyApplication().getAppPath(IndexConstants.GPX_INDEX_DIR), gpxFileName));
			}

		}
		return gpxFile;
	}

	private SelectFileListener createAddToTrackFileListener() {
		final MapActivity mapActivity = getMapActivity();
		return new SelectFileListener() {
			@Override
			public void selectFileOnCLick(String gpxFileName) {
				if (mapActivity != null) {
					GPXFile gpxFile = getGpxFile(gpxFileName);
					SelectedGpxFile selectedGpxFile = mapActivity.getMyApplication().getSelectedGpxHelper()
							.getSelectedFileByPath(gpxFile.path);
					boolean showOnMap = selectedGpxFile != null;
					saveExistingGpx(gpxFile, showOnMap, ActionType.ADD_SEGMENT, false);
				}
			}

			@Override
			public void dismissButtonOnClick() {
			}
		};
	}

	public void addNewGpxData(GPXFile gpxFile) {
		QuadRect rect = gpxFile.getRect();
		TrkSegment segment = getTrkSegment(gpxFile);
		NewGpxData newGpxData = new NewGpxData(gpxFile, rect, segment == null
				? ActionType.ADD_ROUTE_POINTS
				: ActionType.EDIT_SEGMENT,
				segment);
		editingCtx.setNewGpxData(newGpxData);
		initMeasurementMode(newGpxData);
		QuadRect qr = newGpxData.getRect();
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			mapActivity.getMapView().fitRectToMap(qr.left, qr.right, qr.top, qr.bottom,
					(int) qr.width(), (int) qr.height(), 0);
		}
	}

	private TrkSegment getTrkSegment(GPXFile gpxFile) {
		for (GPXUtilities.Track t : gpxFile.tracks) {
			for (TrkSegment s : t.segments) {
				if (s.points.size() > 0) {
					return s;
				}
			}
		}
		return null;
	}

	private void removePoint(MeasurementToolLayer layer, int position) {
		editingCtx.getCommandManager().execute(new RemovePointCommand(layer, position));
		adapter.notifyDataSetChanged();
		updateUndoRedoButton(true, undoBtn);
		updateUndoRedoButton(false, redoBtn);
		updateDistancePointsText();
		saved = false;
		hidePointsListIfNoPoints();
	}

	private SaveAsNewTrackFragmentListener createSaveAsNewTrackFragmentListener() {
		return new SaveAsNewTrackFragmentListener() {
			@Override
			public void saveAsRoutePointOnClick() {
				saveAsGpx(SaveType.ROUTE_POINT);
			}

			@Override
			public void saveAsLineOnClick() {
				saveAsGpx(SaveType.LINE);
			}
		};
	}

	private MeasurementAdapterListener createMeasurementAdapterListener(final ItemTouchHelper touchHelper) {
		return new MeasurementAdapterListener() {

			final MapActivity mapActivity = getMapActivity();
			final MeasurementToolLayer measurementLayer = getMeasurementLayer();
			private int fromPosition;
			private int toPosition;

			@Override
			public void onRemoveClick(int position) {
				if (measurementLayer != null) {
					removePoint(measurementLayer, position);
				}
			}

			@Override
			public void onItemClick(int position) {
				if (mapActivity != null && measurementLayer != null) {
					if (pointsListOpened) {
						hidePointsList();
					}
					if (portrait) {
						setMapPosition(OsmandSettings.MIDDLE_TOP_CONSTANT);
					}
					measurementLayer.moveMapToPoint(position);
					measurementLayer.selectPoint(position);
				}
			}

			@Override
			public void onDragStarted(RecyclerView.ViewHolder holder) {
				fromPosition = holder.getAdapterPosition();
				touchHelper.startDrag(holder);
			}

			@Override
			public void onDragEnded(RecyclerView.ViewHolder holder) {
				if (mapActivity != null && measurementLayer != null) {
					toPosition = holder.getAdapterPosition();
					if (toPosition >= 0 && fromPosition >= 0 && toPosition != fromPosition) {
						editingCtx.getCommandManager().execute(new ReorderPointCommand(measurementLayer, fromPosition, toPosition));
						adapter.notifyDataSetChanged();
						updateUndoRedoButton(false, redoBtn);
						updateDistancePointsText();
						mapActivity.refreshMap();
						saved = false;
					}
				}
			}
		};
	}

	private void enableSnapToRoadMode(ApplicationMode appMode) {
		editingCtx.setSnapToRoadAppMode(appMode);
		editingCtx.setInSnapToRoadMode(true);
		editingCtx.scheduleRouteCalculateIfNotEmpty();
		showSnapToRoadControls();
	}

	private void showSnapToRoadControls() {
		final MapActivity mapActivity = getMapActivity();
		final ApplicationMode appMode = editingCtx.getSnapToRoadAppMode();
		if (mapActivity != null) {
			Drawable icon;
			if (appMode == null) {
				icon = getActiveIcon(R.drawable.ic_action_split_interval);
			} else {
				icon = getIcon(appMode.getIconRes(), appMode.getIconColorInfo().getColor(nightMode));
			}
			ImageButton snapToRoadBtn = (ImageButton) mapActivity.findViewById(R.id.snap_to_road_image_button);
			snapToRoadBtn.setBackgroundResource(nightMode ? R.drawable.btn_circle_night : R.drawable.btn_circle);
			snapToRoadBtn.setImageDrawable(icon);
			snapToRoadBtn.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					showRouteBetweenPointsMenu(false);
				}
			});
			snapToRoadBtn.setVisibility(View.VISIBLE);
			mapActivity.refreshMap();
		}
	}

	private void disableSnapToRoadMode() {
		toolBarController.setTopBarSwitchVisible(false);
		toolBarController.setTitle(previousToolBarTitle);
		mainIcon.setImageDrawable(getActiveIcon(R.drawable.ic_action_ruler));
		editingCtx.setInSnapToRoadMode(false);
		editingCtx.cancelSnapToRoad();
		visibleSnapToRoadIcon(false);
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			mainView.findViewById(R.id.snap_to_road_progress_bar).setVisibility(View.GONE);
			mapActivity.refreshMap();
		}
	}

	private void visibleSnapToRoadIcon(boolean show) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			if (show) {
				mapActivity.findViewById(R.id.snap_to_road_image_button).setVisibility(View.VISIBLE);
			} else {
				mapActivity.findViewById(R.id.snap_to_road_image_button).setVisibility(View.GONE);
			}
		}
	}

	private void displayRoutePoints() {
		final MeasurementToolLayer measurementLayer = getMeasurementLayer();

		GPXFile gpx = editingCtx.getNewGpxData().getGpxFile();
		List<WptPt> points = gpx.getRoutePoints();
		if (measurementLayer != null) {
			editingCtx.addPoints(points);
			adapter.notifyDataSetChanged();
			updateDistancePointsText();
		}
	}

	private void displaySegmentPoints() {
		final MeasurementToolLayer measurementLayer = getMeasurementLayer();
		if (measurementLayer != null) {
			editingCtx.addPoints();
			adapter.notifyDataSetChanged();
			updateDistancePointsText();
		}
	}

	private void openSelectedPointMenu(MapActivity mapActivity) {
		SelectedPointBottomSheetDialogFragment fragment = new SelectedPointBottomSheetDialogFragment();
		fragment.setUsedOnMap(true);
		fragment.setListener(createSelectedPointFragmentListener());
		fragment.show(mapActivity.getSupportFragmentManager(), SelectedPointBottomSheetDialogFragment.TAG);
	}

	private void openSaveAsNewTrackMenu(MapActivity mapActivity) {
		SaveAsNewTrackBottomSheetDialogFragment fragment = new SaveAsNewTrackBottomSheetDialogFragment();
		fragment.setUsedOnMap(true);
		fragment.setListener(createSaveAsNewTrackFragmentListener());
		fragment.show(mapActivity.getSupportFragmentManager(), SaveAsNewTrackBottomSheetDialogFragment.TAG);
	}

//	private AlertDialog showAddToTrackDialog(final MapActivity mapActivity) {
//
//		CallbackWithObject<GPXFile[]> callbackWithObject = new CallbackWithObject<GPXFile[]>() {
//			@Override
//			public boolean processResult(GPXFile[] result) {
//				GPXFile gpxFile;
//				if (result != null && result.length > 0) {
//					gpxFile = result[0];
//					SelectedGpxFile selectedGpxFile = mapActivity.getMyApplication().getSelectedGpxHelper().getSelectedFileByPath(gpxFile.path);
//					boolean showOnMap = selectedGpxFile != null;
//					saveExistingGpx(gpxFile, showOnMap, ActionType.ADD_SEGMENT, false);
//				}
//				return true;
//			}
//		};
//
//		return GpxUiHelper.selectGPXFile(mapActivity, false, false, callbackWithObject, nightMode);
//	}

	private void showAddToTrackDialog(final MapActivity mapActivity) {
		if (mapActivity != null) {
			SelectFileBottomSheet.showInstance(mapActivity.getSupportFragmentManager(),
					createAddToTrackFileListener(),ADD_TO_TRACK);
		}
		/*
		CallbackWithObject<GPXFile[]> callbackWithObject = new CallbackWithObject<GPXFile[]>() {
			@Override
			public boolean processResult(GPXFile[] result) {
				GPXFile gpxFile;
				if (result != null && result.length > 0) {
					gpxFile = result[0];
					SelectedGpxFile selectedGpxFile = mapActivity.getMyApplication().getSelectedGpxHelper().getSelectedFileByPath(gpxFile.path);
					boolean showOnMap = selectedGpxFile != null;
					saveExistingGpx(gpxFile, showOnMap, ActionType.ADD_SEGMENT, false);
				}
				return true;
			}
		};

		return GpxUiHelper.selectGPXFile(mapActivity, false, false, callbackWithObject, nightMode);*/
	}

	private void applyMovePointMode() {
		switchMovePointMode(false);
		MeasurementToolLayer measurementLayer = getMeasurementLayer();
		if (measurementLayer != null) {
			WptPt newPoint = measurementLayer.getMovedPointToApply();
			ApplicationMode applicationMode = editingCtx.getSnapToRoadAppMode();
			if (applicationMode != null) {
				newPoint.setProfileType(applicationMode.getStringKey());
			}
			WptPt oldPoint = editingCtx.getOriginalPointToMove();
			int position = editingCtx.getSelectedPointPosition();
			editingCtx.getCommandManager().execute(new MovePointCommand(measurementLayer, oldPoint, newPoint, position));
			editingCtx.addPoint(newPoint);
			exitMovePointMode(false);
			doAddOrMovePointCommonStuff();
			measurementLayer.refreshMap();
		}
	}

	private void cancelMovePointMode() {
		switchMovePointMode(false);
		exitMovePointMode(true);
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			mapActivity.refreshMap();
		}
	}

	void exitMovePointMode(boolean saveOriginalPoint) {
		if (saveOriginalPoint) {
			WptPt pt = editingCtx.getOriginalPointToMove();
			editingCtx.addPoint(pt);
		}
		editingCtx.setOriginalPointToMove(null);
		editingCtx.setSelectedPointPosition(-1);
		editingCtx.splitSegments(editingCtx.getBeforePoints().size() + editingCtx.getAfterPoints().size());
	}

	private void cancelModes() {
		if (editingCtx.getOriginalPointToMove() != null) {
			cancelMovePointMode();
		} else if (editingCtx.isInAddPointMode()) {
			cancelAddPointBeforeOrAfterMode();
		}
	}

	private void addPointBeforeAfter() {
		MeasurementToolLayer measurementLayer = getMeasurementLayer();
		if (measurementLayer != null) {
			int selectedPoint = editingCtx.getSelectedPointPosition();
			int pointsCount = editingCtx.getPointsCount();
			if (addCenterPoint()) {
				if (selectedPoint == pointsCount) {
					editingCtx.splitSegments(editingCtx.getPointsCount() - 1);
				} else {
					editingCtx.setSelectedPointPosition(selectedPoint + 1);
				}
				measurementLayer.refreshMap();
			}
		}
	}

	private void applyAddPointBeforeAfterMode() {
		switchAddPointBeforeAfterMode(false);
		editingCtx.splitSegments(editingCtx.getBeforePoints().size() + editingCtx.getAfterPoints().size());
		editingCtx.setSelectedPointPosition(-1);
		editingCtx.setInAddPointMode(false);
		MeasurementToolLayer measurementLayer = getMeasurementLayer();
		if (measurementLayer != null) {
			measurementLayer.refreshMap();
		}
		updateDistancePointsText();
	}

	private void cancelAddPointBeforeOrAfterMode() {
		switchAddPointBeforeAfterMode(false);
		editingCtx.splitSegments(editingCtx.getBeforePoints().size() + editingCtx.getAfterPoints().size());
		editingCtx.setSelectedPointPosition(-1);
		editingCtx.setInAddPointMode(false);
		MeasurementToolLayer measurementToolLayer = getMeasurementLayer();
		if (measurementToolLayer != null) {
			measurementToolLayer.refreshMap();
		}
	}

	private void switchMovePointMode(boolean enable) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			if (enable) {
				int navigationIconResId = AndroidUtils.getNavigationIconResId(mapActivity);
				toolBarController.setBackBtnIconIds(navigationIconResId, navigationIconResId);
			} else {
				toolBarController.setBackBtnIconIds(R.drawable.ic_action_remove_dark, R.drawable.ic_action_remove_dark);
			}
			mapActivity.showTopToolbar(toolBarController);
		}
		markGeneralComponents(enable ? View.GONE : View.VISIBLE);
		AndroidUiHelper.setVisibility(mapActivity, enable ? View.VISIBLE : View.GONE,
				R.id.move_point_text,
				R.id.move_point_controls);
		mainIcon.setImageDrawable(getActiveIcon(enable
				? R.drawable.ic_action_move_point
				: R.drawable.ic_action_ruler));
	}

	private void switchAddPointBeforeAfterMode(boolean enable) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			if (enable) {
				int navigationIconResId = AndroidUtils.getNavigationIconResId(mapActivity);
				toolBarController.setBackBtnIconIds(navigationIconResId, navigationIconResId);
			} else {
				toolBarController.setBackBtnIconIds(R.drawable.ic_action_remove_dark, R.drawable.ic_action_remove_dark);
			}
			mapActivity.showTopToolbar(toolBarController);
		}
		markGeneralComponents(enable ? View.GONE : View.VISIBLE);
		AndroidUiHelper.setVisibility(mapActivity,enable ? View.VISIBLE : View.GONE,
				R.id.add_point_before_after_text,
				R.id.add_point_before_after_controls);
		if (!enable) {
			mainIcon.setImageDrawable(getActiveIcon(R.drawable.ic_action_ruler));
		}
	}

	private void markGeneralComponents(int status) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			AndroidUiHelper.setVisibility(mapActivity, status,
					R.id.measurement_distance_text_view,
					R.id.measurement_points_text_view,
					R.id.distance_to_center_text_view,
					R.id.up_down_button,
					R.id.measure_mode_controls);
		}
	}

	private void addInitialPoint() {
		if (initialPoint != null) {
			MeasurementToolLayer layer = getMeasurementLayer();
			if (layer != null) {
				editingCtx.getCommandManager().execute(new AddPointCommand(layer, initialPoint));
				doAddOrMovePointCommonStuff();
				initialPoint = null;
			}
		}
	}

	private void addPoint() {
		MeasurementToolLayer measurementLayer = getMeasurementLayer();
		if (measurementLayer != null) {
			editingCtx.getCommandManager().execute(new AddPointCommand(measurementLayer, false));
			doAddOrMovePointCommonStuff();
		}
	}

	private boolean addCenterPoint() {
		boolean added = false;
		MeasurementToolLayer measurementLayer = getMeasurementLayer();
		if (measurementLayer != null) {
			added = editingCtx.getCommandManager().execute(new AddPointCommand(measurementLayer, true));
			doAddOrMovePointCommonStuff();
		}
		return added;
	}

	private void doAddOrMovePointCommonStuff() {
		enable(upDownBtn);
		updateUndoRedoButton(true, undoBtn);
		updateUndoRedoButton(false, redoBtn);
		updateDistancePointsText();
		adapter.notifyDataSetChanged();
		saved = false;
	}

	private void showPointsList() {
		pointsListOpened = true;
		upDownBtn.setImageDrawable(downIcon);
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			if (portrait && pointsListContainer != null) {
				pointsListContainer.setVisibility(View.VISIBLE);
			} else {
				showPointsListFragment();
			}
			setMapPosition(portrait
					? OsmandSettings.MIDDLE_TOP_CONSTANT
					: OsmandSettings.LANDSCAPE_MIDDLE_RIGHT_CONSTANT);
		}
	}

	private void hidePointsList() {
		pointsListOpened = false;
		upDownBtn.setImageDrawable(upIcon);
		if (portrait && pointsListContainer != null) {
			pointsListContainer.setVisibility(View.GONE);
		} else {
			hidePointsListFragment();
		}
		setDefaultMapPosition();
	}

	private void hidePointsListIfNoPoints() {
		MeasurementToolLayer measurementLayer = getMeasurementLayer();
		if (measurementLayer != null) {
			if (editingCtx.getPointsCount() < 1) {
				disable(upDownBtn);
				if (pointsListOpened) {
					hidePointsList();
				}
			}
		}
	}

	private void showPointsListFragment() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			boolean transparentStatusBar = Build.VERSION.SDK_INT >= 21;
			int statusBarHeight = transparentStatusBar ? 0 : AndroidUtils.getStatusBarHeight(mapActivity);
			int screenHeight = AndroidUtils.getScreenHeight(mapActivity) - statusBarHeight;
			RecyclerViewFragment fragment = new RecyclerViewFragment();
			fragment.setRecyclerView(pointsRv);
			fragment.setWidth(upDownRow.getWidth());
			fragment.setHeight(screenHeight - upDownRow.getHeight());
			fragment.setTransparentStatusBar(transparentStatusBar);
			mapActivity.getSupportFragmentManager().beginTransaction()
					.add(R.id.fragmentContainer, fragment, RecyclerViewFragment.TAG)
					.commitAllowingStateLoss();
		}
	}

	private void hidePointsListFragment() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			try {
				FragmentManager manager = mapActivity.getSupportFragmentManager();
				Fragment fragment = manager.findFragmentByTag(RecyclerViewFragment.TAG);
				if (fragment != null) {
					manager.beginTransaction().remove(fragment).commitAllowingStateLoss();
				}
			} catch (Exception e) {
				// ignore
			}
		}
	}

	private void setDefaultMapPosition() {
		setMapPosition(OsmandSettings.CENTER_CONSTANT);
	}

	private void setMapPosition(int position) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			mapActivity.getMapView().setMapPosition(position);
			mapActivity.refreshMap();
		}
	}

	private void addToGpx(MapActivity mapActivity) {
		GPXFile gpx = editingCtx.getNewGpxData().getGpxFile();
		SelectedGpxFile selectedGpxFile = mapActivity.getMyApplication().getSelectedGpxHelper().getSelectedFileByPath(gpx.path);
		boolean showOnMap = selectedGpxFile != null;
		ActionType actionType = editingCtx.getNewGpxData().getActionType();
		saveExistingGpx(gpx, showOnMap, actionType, true);
	}

	private void saveAsGpx(final SaveType saveType) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			final File dir = mapActivity.getMyApplication().getAppPath(IndexConstants.GPX_INDEX_DIR);
			final View view = UiUtilities.getInflater(mapActivity, nightMode).inflate(R.layout.save_gpx_dialog, null);
			final EditText nameEt = (EditText) view.findViewById(R.id.gpx_name_et);
			final TextView warningTextView = (TextView) view.findViewById(R.id.file_exists_text_view);
			final View buttonView = view.findViewById(R.id.button_view);
			final SwitchCompat showOnMapToggle = (SwitchCompat) view.findViewById(R.id.toggle_show_on_map);
			UiUtilities.setupCompoundButton(showOnMapToggle, nightMode, UiUtilities.CompoundButtonType.GLOBAL);
			buttonView.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					showOnMapToggle.setChecked(!showOnMapToggle.isChecked());
				}
			});
			showOnMapToggle.setChecked(true);

			String displayedName = getSuggestedName(dir);
			nameEt.setText(displayedName);
			nameEt.setSelection(displayedName.length());
			final boolean[] textChanged = new boolean[1];

			AlertDialog.Builder builder = new AlertDialog.Builder(UiUtilities.getThemedContext(mapActivity, nightMode))
					.setTitle(R.string.enter_gpx_name)
					.setView(view)
					.setPositiveButton(R.string.shared_string_save, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							final String name = nameEt.getText().toString();
							String fileName = name + GPX_FILE_EXT;
							if (textChanged[0]) {
								File fout = new File(dir, fileName);
								int ind = 1;
								while (fout.exists()) {
									fileName = name + "_" + (++ind) + GPX_FILE_EXT;
									fout = new File(dir, fileName);
								}
							}
							saveNewGpx(dir, fileName, showOnMapToggle.isChecked(), saveType, false);
						}
					})
					.setNegativeButton(R.string.shared_string_cancel, null);
			final AlertDialog dialog = builder.create();
			dialog.show();

			nameEt.addTextChangedListener(new TextWatcher() {
				@Override
				public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

				}

				@Override
				public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

				}

				@Override
				public void afterTextChanged(Editable editable) {
					if (new File(dir, editable.toString() + GPX_FILE_EXT).exists()) {
						warningTextView.setVisibility(View.VISIBLE);
						warningTextView.setText(R.string.file_with_name_already_exists);
						dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
					} else if (editable.toString().trim().isEmpty()) {
						warningTextView.setVisibility(View.VISIBLE);
						warningTextView.setText(R.string.enter_the_file_name);
						dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
					} else {
						warningTextView.setVisibility(View.INVISIBLE);
						dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
					}
					textChanged[0] = true;
				}
			});
		}
	}

	private String getSuggestedName(File dir) {
		final String suggestedName = new SimpleDateFormat("yyyy-MM-dd_HH-mm_EEE", Locale.US).format(new Date());
		String displayedName = suggestedName;
		File fout = new File(dir, suggestedName + GPX_FILE_EXT);
		int ind = 1;
		while (fout.exists()) {
			displayedName = suggestedName + "_" + (++ind);
			fout = new File(dir, displayedName + GPX_FILE_EXT);
		}
		return displayedName;
	}

	private void saveNewGpx(File dir, String fileName, boolean showOnMap, SaveType saveType, boolean close) {
		saveGpx(dir, fileName, showOnMap, null, false, null, saveType, close);
	}

	private void saveExistingGpx(GPXFile gpx, boolean showOnMap, ActionType actionType, boolean openTrackActivity) {
		saveGpx(null, null, showOnMap, gpx, openTrackActivity, actionType, null, false);
	}

	@SuppressLint("StaticFieldLeak")
	private void saveGpx(final File dir,
	                     final String fileName,
	                     final boolean showOnMap,
	                     final GPXFile gpx,
	                     final boolean openTrackActivity,
	                     final ActionType actionType,
	                     final SaveType saveType,
	                     final boolean close) {

		new AsyncTask<Void, Void, Exception>() {

			private ProgressDialog progressDialog;
			private File toSave;

			@Override
			protected void onPreExecute() {
				cancelModes();
				MapActivity activity = getMapActivity();
				if (activity != null) {
					progressDialog = new ProgressDialog(activity);
					progressDialog.setMessage(getString(R.string.saving_gpx_tracks));
					progressDialog.show();
				}
			}

			@Override
			protected Exception doInBackground(Void... voids) {
				MeasurementToolLayer measurementLayer = getMeasurementLayer();
				OsmandApplication app = getMyApplication();
				if (app == null) {
					return null;
				}
				List<WptPt> points = editingCtx.getPoints();
				TrkSegment before = editingCtx.getBeforeTrkSegmentLine();
				TrkSegment after = editingCtx.getAfterTrkSegmentLine();
				if (gpx == null) {
					toSave = new File(dir, fileName);
					String trackName = fileName.substring(0, fileName.length() - GPX_FILE_EXT.length());
					GPXFile gpx = new GPXFile(Version.getFullVersion(app));
					if (measurementLayer != null) {
						if (saveType == SaveType.LINE) {
							TrkSegment segment = new TrkSegment();
							if (editingCtx.isInSnapToRoadMode()) {
								segment.points.addAll(before.points);
								segment.points.addAll(after.points);
							} else {
								segment.points.addAll(points);
							}
							Track track = new Track();
							track.name = trackName;
							track.segments.add(segment);
							gpx.tracks.add(track);
						} else if (saveType == SaveType.ROUTE_POINT) {
							if (editingCtx.isInSnapToRoadMode()) {
								editingCtx.exportRouteAsGpx(trackName, new ExportAsGpxListener() {
									@Override
									public void onExportAsGpxFinished(GPXFile gpx) {
										gpx.addRoutePoints(editingCtx.getPoints());
										final Exception res = GPXUtilities.writeGpxFile(toSave, gpx);
										gpx.path = toSave.getAbsolutePath();
										OsmandApplication app = getMyApplication();
										if (showOnMap && app != null) {
											app.getSelectedGpxHelper().selectGpxFile(gpx, true, false);
											app.runInUIThread(new Runnable() {
												@Override
												public void run() {
													onGpxSaved(res);
												}
											});
										}
									}
								});
								return null;
							} else {
								Route rt = new Route();
								rt.name = trackName;
								gpx.routes.add(rt);
								rt.points.addAll(points);
							}
						}
					}
					Exception res = GPXUtilities.writeGpxFile(toSave, gpx);
					gpx.path = toSave.getAbsolutePath();
					if (showOnMap) {
						app.getSelectedGpxHelper().selectGpxFile(gpx, true, false);
					}
					return res;
				} else {
					toSave = new File(gpx.path);
					if (measurementLayer != null) {
						if (actionType != null) {
							switch (actionType) {
								case ADD_SEGMENT:
									if (editingCtx.isInSnapToRoadMode()) {
										List<WptPt> snappedPoints = new ArrayList<>();
										snappedPoints.addAll(before.points);
										snappedPoints.addAll(after.points);
										gpx.addTrkSegment(snappedPoints);
									} else {
										gpx.addTrkSegment(points);
									}
									break;
								case ADD_ROUTE_POINTS:
									gpx.replaceRoutePoints(points);
									break;
								case EDIT_SEGMENT:
									TrkSegment segment = new TrkSegment();
									segment.points.addAll(points);
									gpx.replaceSegment(editingCtx.getNewGpxData().getTrkSegment(), segment);
									break;
								case OVERWRITE_SEGMENT:
									List<WptPt> snappedPoints = new ArrayList<>();
									snappedPoints.addAll(before.points);
									snappedPoints.addAll(after.points);
									TrkSegment segment1 = new TrkSegment();
									segment1.points.addAll(snappedPoints);
									gpx.replaceSegment(editingCtx.getNewGpxData().getTrkSegment(), segment1);
									break;
							}
						} else {
							gpx.addRoutePoints(points);
						}
					}
					Exception res = GPXUtilities.writeGpxFile(toSave, gpx);
					if (showOnMap) {
						SelectedGpxFile sf = app.getSelectedGpxHelper().selectGpxFile(gpx, true, false);
						if (sf != null) {
							if (actionType == ActionType.ADD_SEGMENT || actionType == ActionType.EDIT_SEGMENT) {
								sf.processPoints(getMyApplication());
							}
						}
					}
					return res;
				}
			}

			@Override
			protected void onPostExecute(Exception warning) {
				onGpxSaved(warning);
			}

			private void onGpxSaved(Exception warning) {
				final MapActivity mapActivity = getMapActivity();
				if (mapActivity == null) {
					return;
				}
				if (progressDialog != null && progressDialog.isShowing()) {
					progressDialog.dismiss();
				}
				mapActivity.refreshMap();
				if (warning == null) {
					saved = true;
					if (openTrackActivity) {
						dismiss(mapActivity);
					} else {
						if (close) {
							snackbar = Snackbar.make(mapActivity.getLayout(),
									MessageFormat.format(getString(R.string.gpx_saved_sucessfully), toSave.getName()),
									Snackbar.LENGTH_LONG)
									.setAction(R.string.shared_string_rename, new View.OnClickListener() {
										@Override
										public void onClick(View view) {
											if (AndroidUtils.isActivityNotDestroyed(mapActivity)) {
												FileUtils.renameFile(mapActivity, toSave, null);
											}
										}
									});
							snackbar.getView().<TextView>findViewById(com.google.android.material.R.id.snackbar_action)
									.setAllCaps(false);
							UiUtilities.setupSnackbar(snackbar, nightMode);
							snackbar.show();
							dismiss(mapActivity);
						} else {
							Toast.makeText(mapActivity,
									MessageFormat.format(getString(R.string.gpx_saved_sucessfully), toSave.getAbsolutePath()),
									Toast.LENGTH_LONG).show();
						}
					}
				} else {
					Toast.makeText(mapActivity, warning.getMessage(), Toast.LENGTH_LONG).show();
				}
			}
		}.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	private void updateUndoRedoButton(boolean enable, View view) {
		view.setEnabled(enable);
		int color = enable
				? nightMode ? R.color.icon_color_active_dark : R.color.icon_color_active_light
				: nightMode ? R.color.icon_color_secondary_dark : R.color.icon_color_secondary_light;
		ImageView imageView = ((ImageView) view);
		imageView.setImageDrawable(UiUtilities.tintDrawable(imageView.getDrawable(),
				ContextCompat.getColor(view.getContext(), color)));
	}

	private void enable(View view) {
		view.setEnabled(true);
		view.setAlpha(1);
	}

	private void disable(View view) {
		view.setEnabled(false);
		view.setAlpha(.5f);
	}

	private void updateDistancePointsText() {
		MeasurementToolLayer measurementLayer = getMeasurementLayer();
		if (measurementLayer != null) {
			distanceTv.setText(measurementLayer.getDistanceSt() + ",");
			pointsTv.setText((portrait ? pointsSt + ": " : "") + editingCtx.getPointsCount());
		}
		updateToolbar();
	}

	private void updateToolbar() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity == null) {
			return;
		}
		if (editingCtx.getPointsCount() > 1) {
			final File dir = mapActivity.getMyApplication().getAppPath(IndexConstants.GPX_INDEX_DIR);
			toolBarController.setTitle(getSuggestedName(dir));
			toolBarController.setDescription(getString(R.string.plan_route));
		} else {
			toolBarController.setTitle(getString(R.string.plan_route));
			toolBarController.setDescription(null);
		}
		mapActivity.showTopToolbar(toolBarController);
	}

	private void enterMeasurementMode() {
		MapActivity mapActivity = getMapActivity();
		MeasurementToolLayer measurementLayer = getMeasurementLayer();
		if (mapActivity != null && measurementLayer != null) {
			measurementLayer.setInMeasurementMode(true);
			mapActivity.refreshMap();
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

			View collapseButton = mapActivity.findViewById(R.id.map_collapse_button);
			if (collapseButton != null && collapseButton.getVisibility() == View.VISIBLE) {
				wasCollapseButtonVisible = true;
				collapseButton.setVisibility(View.INVISIBLE);
			} else {
				wasCollapseButtonVisible = false;
			}
			updateDistancePointsText();
		}
	}

	private void exitMeasurementMode() {
		MapActivity mapActivity = getMapActivity();
		MeasurementToolLayer measurementLayer = getMeasurementLayer();
		if (mapActivity != null && measurementLayer != null) {
			if (toolBarController != null) {
				mapActivity.hideTopToolbar(toolBarController);
			}
			measurementLayer.setInMeasurementMode(false);
			mapActivity.enableDrawer();

			AndroidUiHelper.setVisibility(mapActivity, View.VISIBLE,
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

			mapActivity.refreshMap();
		}
	}

	public void quit(boolean hidePointsListFirst) {
		if (editingCtx.getOriginalPointToMove() != null) {
			cancelMovePointMode();
			return;
		} else if (editingCtx.isInAddPointMode()) {
			cancelAddPointBeforeOrAfterMode();
			return;
		}
		showQuitDialog(hidePointsListFirst);
	}

	private void showQuitDialog(boolean hidePointsListFirst) {
		final MapActivity mapActivity = getMapActivity();
		MeasurementToolLayer measurementLayer = getMeasurementLayer();
		if (mapActivity != null && measurementLayer != null) {
			if (pointsListOpened && hidePointsListFirst) {
				hidePointsList();
				return;
			}
			if (editingCtx.getPointsCount() == 0 || saved) {
				dismiss(mapActivity);
				return;
			}
			AlertDialog.Builder builder = new AlertDialog.Builder(UiUtilities.getThemedContext(mapActivity, nightMode));
			if (editingCtx.getNewGpxData() == null) {
				final File dir = mapActivity.getMyApplication().getAppPath(IndexConstants.GPX_INDEX_DIR);
				final View view = UiUtilities.getInflater(mapActivity, nightMode).inflate(R.layout.close_measurement_tool_dialog, null);
				final SwitchCompat showOnMapToggle = (SwitchCompat) view.findViewById(R.id.toggle_show_on_map);

				view.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						showOnMapToggle.setChecked(!showOnMapToggle.isChecked());
					}
				});

				builder.setView(view);
				builder.setPositiveButton(R.string.shared_string_ok, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						if (showOnMapToggle.isChecked()) {
							final String name = new SimpleDateFormat("yyyy-MM-dd_HH-mm_EEE", Locale.US).format(new Date());
							String fileName = name + GPX_FILE_EXT;
							File fout = new File(dir, fileName);
							int ind = 1;
							while (fout.exists()) {
								fileName = name + "_" + (++ind) + GPX_FILE_EXT;
								fout = new File(dir, fileName);
							}
							saveNewGpx(dir, fileName, true, SaveType.LINE, true);
						} else {
							dismiss(mapActivity);
						}
					}
				});
				UiUtilities.setupCompoundButton(showOnMapToggle, nightMode, UiUtilities.CompoundButtonType.GLOBAL);
			} else {
				builder.setPositiveButton(R.string.shared_string_ok, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialogInterface, int i) {
						dismiss(mapActivity);
					}
				});
			}
			builder.setTitle(getString(R.string.exit_without_saving))
					.setMessage(getString(R.string.unsaved_changes_will_be_lost))
					.setNegativeButton(R.string.shared_string_cancel, null);
			builder.show();
		}
	}

	private void dismiss(MapActivity mapActivity) {
		try {
			editingCtx.clearSegments();
			if (pointsListOpened) {
				hidePointsList();
			}
			if (editingCtx.isInSnapToRoadMode()) {
				disableSnapToRoadMode();
			} else {
				visibleSnapToRoadIcon(false);
			}
			if (editingCtx.getNewGpxData() != null && !planRouteMode) {
				GPXFile gpx = editingCtx.getNewGpxData().getGpxFile();
				Intent newIntent = new Intent(mapActivity, mapActivity.getMyApplication().getAppCustomization().getTrackActivity());
				newIntent.putExtra(TrackActivity.TRACK_FILE_NAME, gpx.path);
				newIntent.putExtra(TrackActivity.OPEN_TRACKS_LIST, true);
				newIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(newIntent);
			}
			mapActivity.getSupportFragmentManager().beginTransaction().remove(this).commitAllowingStateLoss();
		} catch (Exception e) {
			// ignore
		}
	}

	public static boolean showInstance(FragmentManager fragmentManager, LatLon initialPoint) {
		MeasurementToolFragment fragment = new MeasurementToolFragment();
		fragment.setInitialPoint(initialPoint);
		return showFragment(fragment, fragmentManager);
	}

	public static boolean showInstance(FragmentManager fragmentManager, MeasurementEditingContext editingCtx) {
		MeasurementToolFragment fragment = new MeasurementToolFragment();
		fragment.setEditingCtx(editingCtx);
		return showFragment(fragment, fragmentManager);
	}

	public static boolean showInstance(FragmentManager fragmentManager) {
		MeasurementToolFragment fragment = new MeasurementToolFragment();
		fragment.setPlanRouteMode(true);
		return showFragment(fragment, fragmentManager);
	}

	private static boolean showFragment(MeasurementToolFragment fragment, FragmentManager fragmentManager) {
		try {
			fragment.setRetainInstance(true);
			fragmentManager.beginTransaction()
					.add(R.id.bottomFragmentContainer, fragment, MeasurementToolFragment.TAG)
					.commitAllowingStateLoss();
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	private class MeasurementToolBarController extends TopToolbarController {

		MeasurementToolBarController() {
			super(TopToolbarControllerType.MEASUREMENT_TOOL);
			setBackBtnIconClrIds(0, 0);
			setTitleTextClrIds(R.color.text_color_tab_active_light, R.color.text_color_tab_active_dark);
			setDescrTextClrIds(R.color.text_color_tab_active_light, R.color.text_color_tab_active_dark);
			setBgIds(R.drawable.gradient_toolbar, R.drawable.gradient_toolbar,
					R.drawable.gradient_toolbar, R.drawable.gradient_toolbar);
			setCloseBtnVisible(false);
			setSaveViewVisible(true);
			setSingleLineTitle(false);
			setSaveViewTextId(R.string.shared_string_done);
		}

		@Override
		public void updateToolbar(TopToolbarView view) {
			super.updateToolbar(view);
			setupDoneButton(view);
			View shadow = view.getShadowView();
			if (shadow != null) {
				shadow.setVisibility(View.GONE);
			}
		}

		private void setupDoneButton(TopToolbarView view) {
			TextView done = view.getSaveView();
			Context ctx = done.getContext();
			done.setAllCaps(false);
			ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) done.getLayoutParams();
			layoutParams.height = ctx.getResources().getDimensionPixelSize(R.dimen.measurement_tool_button_height);
			layoutParams.leftMargin = ctx.getResources().getDimensionPixelSize(R.dimen.context_menu_padding_margin_large);
			layoutParams.rightMargin = ctx.getResources().getDimensionPixelSize(R.dimen.context_menu_padding_margin_large);
			int paddingH = ctx.getResources().getDimensionPixelSize(R.dimen.context_menu_padding_margin_large);
			int paddingV = ctx.getResources().getDimensionPixelSize(R.dimen.context_menu_padding_margin_small);
			done.setPadding(paddingH, paddingV, paddingH, paddingV);
			AndroidUtils.setBackground(ctx, done, nightMode, R.drawable.dlg_btn_stroked_light,
					R.drawable.dlg_btn_stroked_dark);
		}

		@Override
		public int getStatusBarColor(Context context, boolean night) {
			return NO_COLOR;
		}
	}
}
