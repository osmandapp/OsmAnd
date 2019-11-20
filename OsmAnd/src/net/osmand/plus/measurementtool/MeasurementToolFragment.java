package net.osmand.plus.measurementtool;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.TextViewCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.helper.ItemTouchHelper;
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

import net.osmand.AndroidUtils;
import net.osmand.CallbackWithObject;
import net.osmand.GPXUtilities;
import net.osmand.GPXUtilities.GPXFile;
import net.osmand.GPXUtilities.Route;
import net.osmand.GPXUtilities.Track;
import net.osmand.GPXUtilities.TrkSegment;
import net.osmand.GPXUtilities.WptPt;
import net.osmand.IndexConstants;
import net.osmand.data.LatLon;
import net.osmand.plus.ApplicationMode;
import net.osmand.plus.GpxSelectionHelper.SelectedGpxFile;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.Version;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.TrackActivity;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.helpers.GpxUiHelper;
import net.osmand.plus.measurementtool.NewGpxData.ActionType;
import net.osmand.plus.measurementtool.OptionsBottomSheetDialogFragment.OptionsFragmentListener;
import net.osmand.plus.measurementtool.SaveAsNewTrackBottomSheetDialogFragment.SaveAsNewTrackFragmentListener;
import net.osmand.plus.measurementtool.SelectedPointBottomSheetDialogFragment.SelectedPointFragmentListener;
import net.osmand.plus.measurementtool.SnapToRoadBottomSheetDialogFragment.SnapToRoadFragmentListener;
import net.osmand.plus.measurementtool.adapter.MeasurementToolAdapter;
import net.osmand.plus.measurementtool.adapter.MeasurementToolAdapter.MeasurementAdapterListener;
import net.osmand.plus.measurementtool.adapter.MeasurementToolItemTouchHelperCallback;
import net.osmand.plus.measurementtool.command.AddPointCommand;
import net.osmand.plus.measurementtool.command.ClearPointsCommand;
import net.osmand.plus.measurementtool.command.MovePointCommand;
import net.osmand.plus.measurementtool.command.RemovePointCommand;
import net.osmand.plus.measurementtool.command.ReorderPointCommand;
import net.osmand.plus.views.mapwidgets.MapInfoWidgetsFactory;
import net.osmand.plus.views.mapwidgets.MapInfoWidgetsFactory.TopToolbarController;

import java.io.File;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static net.osmand.plus.helpers.ImportHelper.GPX_SUFFIX;

public class MeasurementToolFragment extends BaseOsmAndFragment {

	public static final String TAG = "MeasurementToolFragment";

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

	private boolean wasCollapseButtonVisible;
	private boolean progressBarVisible;
	private boolean pointsListOpened;
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

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		final MapActivity mapActivity = (MapActivity) getActivity();
		final MeasurementToolLayer measurementLayer = mapActivity.getMapLayers().getMeasurementToolLayer();

		editingCtx.setApplication(mapActivity.getMyApplication());
		editingCtx.setProgressListener(new MeasurementEditingContext.SnapToRoadProgressListener() {
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
				updateText();
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
		Fragment optionsFragment = fragmentManager.findFragmentByTag(OptionsBottomSheetDialogFragment.TAG);
		if (optionsFragment != null) {
			((OptionsBottomSheetDialogFragment) optionsFragment).setListener(createOptionsFragmentListener());
		}
		Fragment snapToRoadFragment = fragmentManager.findFragmentByTag(SnapToRoadBottomSheetDialogFragment.TAG);
		if (snapToRoadFragment != null) {
			((SnapToRoadBottomSheetDialogFragment) snapToRoadFragment).setListener(createSnapToRoadFragmentListener());
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

		editingCtx.getCommandManager().resetMeasurementLayer(measurementLayer);
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
		if (editingCtx.getNewGpxData() != null) {
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
				Bundle args = new Bundle();
				args.putBoolean(OptionsBottomSheetDialogFragment.SNAP_TO_ROAD_ENABLED_KEY, editingCtx.isInSnapToRoadMode());
				args.putBoolean(OptionsBottomSheetDialogFragment.ADD_LINE_MODE_KEY, newGpxData != null);
				OptionsBottomSheetDialogFragment fragment = new OptionsBottomSheetDialogFragment();
				fragment.setArguments(args);
				fragment.setUsedOnMap(true);
				fragment.setListener(createOptionsFragmentListener());
				fragment.show(mapActivity.getSupportFragmentManager(), OptionsBottomSheetDialogFragment.TAG);
			}
		});

		undoBtn = ((ImageButton) mainView.findViewById(R.id.undo_point_button));
		redoBtn = ((ImageButton) mainView.findViewById(R.id.redo_point_button));

		undoBtn.setImageDrawable(getContentIcon(R.drawable.ic_action_undo_dark));
		undoBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				editingCtx.getCommandManager().undo();
				if (editingCtx.getCommandManager().canUndo()) {
					enable(undoBtn);
				} else {
					disable(undoBtn);
				}
				hidePointsListIfNoPoints();
				if (editingCtx.getPointsCount() > 0) {
					enable(upDownBtn);
				}
				adapter.notifyDataSetChanged();
				enable(redoBtn);
				updateText();
			}
		});

		redoBtn.setImageDrawable(getContentIcon(R.drawable.ic_action_redo_dark));
		redoBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				editingCtx.getCommandManager().redo();
				if (editingCtx.getCommandManager().canRedo()) {
					enable(redoBtn);
				} else {
					disable(redoBtn);
				}
				hidePointsListIfNoPoints();
				if (editingCtx.getPointsCount() > 0) {
					enable(upDownBtn);
				}
				adapter.notifyDataSetChanged();
				enable(undoBtn);
				updateText();
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
				distanceToCenterTv.setText(String.format(" – %1$s • %2$s", distStr, azimuthStr));
				TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(
						distanceToCenterTv, 12, 18, 2, TypedValue.COMPLEX_UNIT_SP
				);
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
			disable(undoBtn);
		}
		if (!editingCtx.getCommandManager().canRedo()) {
			disable(redoBtn);
		}
		if (editingCtx.getPointsCount() < 1) {
			disable(upDownBtn);
		}

		toolBarController = new MeasurementToolBarController(newGpxData);
		if (editingCtx.getSelectedPointPosition() != -1) {
			toolBarController.setBackBtnIconIds(R.drawable.ic_action_mode_back, R.drawable.ic_action_mode_back);
		} else {
			toolBarController.setBackBtnIconIds(R.drawable.ic_action_remove_dark, R.drawable.ic_action_remove_dark);
		}
		if (newGpxData != null) {
			NewGpxData.ActionType actionType = newGpxData.getActionType();
			if (actionType == NewGpxData.ActionType.ADD_ROUTE_POINTS) {
				toolBarController.setTitle(getString(R.string.add_route_points));
			} else if (actionType == NewGpxData.ActionType.ADD_SEGMENT) {
				toolBarController.setTitle(getString(R.string.add_line));
			} else if (actionType == NewGpxData.ActionType.EDIT_SEGMENT) {
				toolBarController.setTitle(getString(R.string.edit_line));
			}
		} else {
			toolBarController.setTitle(getString(R.string.measurement_tool_action_bar));
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
					if (newGpxData != null && newGpxData.getActionType() == NewGpxData.ActionType.EDIT_SEGMENT
							&& editingCtx.isInSnapToRoadMode()) {
						if (mapActivity != null && measurementLayer != null) {
							if (editingCtx.getPointsCount() > 0) {
								openSaveAsNewTrackMenu(mapActivity);
							} else {
								Toast.makeText(mapActivity, getString(R.string.none_point_error), Toast.LENGTH_SHORT).show();
							}
						}
					} else {
						addToGpx(mapActivity);
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
		final ItemTouchHelper touchHelper = new ItemTouchHelper(new MeasurementToolItemTouchHelperCallback(adapter));
		touchHelper.attachToRecyclerView(pointsRv);
		adapter.setAdapterListener(createMeasurementAdapterListener(touchHelper));
		pointsRv.setLayoutManager(new LinearLayoutManager(getContext()));
		pointsRv.setAdapter(adapter);

		enterMeasurementMode();

		if (editingCtx.isInSnapToRoadMode()) {
			showSnapToRoadControls();
		}

		if (newGpxData != null && !gpxPointsAdded) {
			NewGpxData.ActionType actionType = newGpxData.getActionType();
			if (actionType == NewGpxData.ActionType.ADD_ROUTE_POINTS) {
				displayRoutePoints();
				gpxPointsAdded = true;
			} else if (actionType == NewGpxData.ActionType.EDIT_SEGMENT) {
				displaySegmentPoints();
				gpxPointsAdded = true;
			}
		}

		if (saved == null) {
			saved = newGpxData != null && (newGpxData.getActionType() == NewGpxData.ActionType.ADD_ROUTE_POINTS || newGpxData.getActionType() == NewGpxData.ActionType.EDIT_SEGMENT);
		}

		return view;
	}

	@Override
	public void onResume() {
		super.onResume();
		getMapActivity().getMapLayers().getMapControlsLayer().showMapControlsIfHidden();
		cachedMapPosition = getMapActivity().getMapView().getMapPosition();
		setDefaultMapPosition();
		addInitialPoint();
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

	private MapActivity getMapActivity() {
		return (MapActivity) getActivity();
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

	private void showSnapToRoadMenu(boolean rememberPreviousTitle) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			if (rememberPreviousTitle) {
				previousToolBarTitle = toolBarController.getTitle();
			}
			toolBarController.setTitle(getString(R.string.snap_to_road));
			mapActivity.refreshMap();
			SnapToRoadBottomSheetDialogFragment fragment = new SnapToRoadBottomSheetDialogFragment();
			fragment.setListener(createSnapToRoadFragmentListener());
			fragment.show(mapActivity.getSupportFragmentManager(), SnapToRoadBottomSheetDialogFragment.TAG);
		}
	}

	private OptionsFragmentListener createOptionsFragmentListener() {
		return new OptionsFragmentListener() {

			final MapActivity mapActivity = getMapActivity();
			final MeasurementToolLayer measurementLayer = getMeasurementLayer();

			@Override
			public void snapToRoadOnCLick() {
				if (!editingCtx.isInSnapToRoadMode()) {
					showSnapToRoadMenu(true);
				} else {
					disableSnapToRoadMode();
				}
			}

			@Override
			public void addToGpxOnClick() {
				if (mapActivity != null && measurementLayer != null) {
					if (editingCtx.getPointsCount() > 0) {
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
			public void overwriteOldTrackOnClick() {
				if (mapActivity != null && measurementLayer != null) {
					if (editingCtx.getPointsCount() > 0) {
						overwriteGpx(mapActivity);
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
				disable(redoBtn, upDownBtn);
				updateText();
				saved = false;
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

	private SnapToRoadFragmentListener createSnapToRoadFragmentListener() {
		return new SnapToRoadFragmentListener() {
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
				enableSnapToRoadMode(mode);
			}
		};
	}

	private void removePoint(MeasurementToolLayer layer, int position) {
		editingCtx.getCommandManager().execute(new RemovePointCommand(layer, position));
		adapter.notifyDataSetChanged();
		disable(redoBtn);
		updateText();
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
						disable(redoBtn);
						updateText();
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
		if (mapActivity != null && appMode != null) {
			toolBarController.setTopBarSwitchVisible(true);
			toolBarController.setTopBarSwitchChecked(true);
			mainIcon.setImageDrawable(getActiveIcon(R.drawable.ic_action_snap_to_road));

			ImageButton snapToRoadBtn = (ImageButton) mapActivity.findViewById(R.id.snap_to_road_image_button);
			snapToRoadBtn.setBackgroundResource(nightMode ? R.drawable.btn_circle_night : R.drawable.btn_circle);
			snapToRoadBtn.setImageDrawable(getIcon(appMode.getMapIconRes(), appMode.getIconColorInfo().getColor(nightMode)));
			snapToRoadBtn.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					showSnapToRoadMenu(false);
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
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			mapActivity.findViewById(R.id.snap_to_road_image_button).setVisibility(View.GONE);
			mainView.findViewById(R.id.snap_to_road_progress_bar).setVisibility(View.GONE);
			mapActivity.refreshMap();
		}
	}

	private void displayRoutePoints() {
		final MeasurementToolLayer measurementLayer = getMeasurementLayer();

		GPXFile gpx = editingCtx.getNewGpxData().getGpxFile();
		List<WptPt> points = gpx.getRoutePoints();
		if (measurementLayer != null) {
			editingCtx.addPoints(points);
			adapter.notifyDataSetChanged();
			updateText();
		}
	}

	private void displaySegmentPoints() {
		final MeasurementToolLayer measurementLayer = getMeasurementLayer();

		TrkSegment segment = editingCtx.getNewGpxData().getTrkSegment();
		List<WptPt> points = segment.points;
		if (measurementLayer != null) {
			editingCtx.addPoints(points);
			adapter.notifyDataSetChanged();
			updateText();
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

	private AlertDialog showAddToTrackDialog(final MapActivity mapActivity) {
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

		return GpxUiHelper.selectGPXFile(mapActivity, false, false, callbackWithObject, nightMode);
	}

	private void applyMovePointMode() {
		switchMovePointMode(false);
		MeasurementToolLayer measurementLayer = getMeasurementLayer();
		if (measurementLayer != null) {
			WptPt newPoint = measurementLayer.getMovedPointToApply();
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
		updateText();
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
		if (enable) {
			toolBarController.setBackBtnIconIds(R.drawable.ic_action_mode_back, R.drawable.ic_action_mode_back);
		} else {
			toolBarController.setBackBtnIconIds(R.drawable.ic_action_remove_dark, R.drawable.ic_action_remove_dark);
		}
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			mapActivity.showTopToolbar(toolBarController);
		}
		markGeneralComponents(enable ? View.GONE : View.VISIBLE);
		mark(enable ? View.VISIBLE : View.GONE,
				R.id.move_point_text,
				R.id.move_point_controls);
		mainIcon.setImageDrawable(getActiveIcon(enable
				? R.drawable.ic_action_move_point
				: R.drawable.ic_action_ruler));
	}

	private void switchAddPointBeforeAfterMode(boolean enable) {
		if (enable) {
			toolBarController.setBackBtnIconIds(R.drawable.ic_action_mode_back, R.drawable.ic_action_mode_back);
		} else {
			toolBarController.setBackBtnIconIds(R.drawable.ic_action_remove_dark, R.drawable.ic_action_remove_dark);
		}
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			mapActivity.showTopToolbar(toolBarController);
		}
		markGeneralComponents(enable ? View.GONE : View.VISIBLE);
		mark(enable ? View.VISIBLE : View.GONE,
				R.id.add_point_before_after_text,
				R.id.add_point_before_after_controls);
		if (!enable) {
			mainIcon.setImageDrawable(getActiveIcon(R.drawable.ic_action_ruler));
		}
	}

	private void markGeneralComponents(int status) {
		mark(status,
				R.id.measurement_distance_text_view,
				R.id.measurement_points_text_view,
				R.id.distance_to_center_text_view,
				R.id.up_down_button,
				R.id.measure_mode_controls);
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
		enable(undoBtn, upDownBtn);
		disable(redoBtn);
		updateText();
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

	private void overwriteGpx(MapActivity mapActivity) {
		GPXFile gpx = editingCtx.getNewGpxData().getGpxFile();
		SelectedGpxFile selectedGpxFile = mapActivity.getMyApplication().getSelectedGpxHelper().getSelectedFileByPath(gpx.path);
		boolean showOnMap = selectedGpxFile != null;
		ActionType actionType = ActionType.OVERWRITE_SEGMENT;
		saveExistingGpx(gpx, showOnMap, actionType, true);
	}

	private void saveAsGpx(final SaveType saveType) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			final File dir = mapActivity.getMyApplication().getAppPath(IndexConstants.GPX_INDEX_DIR);
			final View view = UiUtilities.getInflater(mapActivity, nightMode).inflate(R.layout.save_gpx_dialog, null);
			final EditText nameEt = (EditText) view.findViewById(R.id.gpx_name_et);
			final TextView warningTextView = (TextView) view.findViewById(R.id.file_exists_text_view);
			final SwitchCompat showOnMapToggle = (SwitchCompat) view.findViewById(R.id.toggle_show_on_map);
			showOnMapToggle.setChecked(true);

			final String suggestedName = new SimpleDateFormat("yyyy-MM-dd_HH-mm_EEE", Locale.US).format(new Date());
			String displayedName = suggestedName;
			File fout = new File(dir, suggestedName + GPX_SUFFIX);
			int ind = 1;
			while (fout.exists()) {
				displayedName = suggestedName + "_" + (++ind);
				fout = new File(dir, displayedName + GPX_SUFFIX);
			}
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
							String fileName = name + GPX_SUFFIX;
							if (textChanged[0]) {
								File fout = new File(dir, fileName);
								int ind = 1;
								while (fout.exists()) {
									fileName = name + "_" + (++ind) + GPX_SUFFIX;
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
					if (new File(dir, editable.toString() + GPX_SUFFIX).exists()) {
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

	private void saveNewGpx(File dir, String fileName, boolean checked, SaveType saveType, boolean close) {
		saveGpx(dir, fileName, checked, null, false, null, saveType, close);
	}

	private void saveExistingGpx(GPXFile gpx, boolean showOnMap, NewGpxData.ActionType actionType, boolean openTrackActivity) {
		saveGpx(null, null, showOnMap, gpx, openTrackActivity, actionType, null, false);
	}

	private void saveGpx(final File dir,
	                     final String fileName,
	                     final boolean showOnMap,
	                     final GPXFile gpx,
	                     final boolean openTrackActivity,
	                     final NewGpxData.ActionType actionType,
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
				MapActivity activity = getMapActivity();
				List<WptPt> points = editingCtx.getPoints();
				TrkSegment before = editingCtx.getBeforeTrkSegmentLine();
				TrkSegment after = editingCtx.getAfterTrkSegmentLine();
				if (gpx == null) {
					toSave = new File(dir, fileName);
					String trackName = fileName.substring(0, fileName.length() - GPX_SUFFIX.length());
					GPXFile gpx = new GPXFile(Version.getFullVersion(activity.getMyApplication()));
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
								TrkSegment segment = new TrkSegment();
								segment.points.addAll(before.points);
								segment.points.addAll(after.points);
								Track track = new Track();
								track.name = trackName;
								track.segments.add(segment);
								gpx.tracks.add(track);
							}
							Route rt = new Route();
							rt.name = trackName;
							gpx.routes.add(rt);
							rt.points.addAll(points);
						}
					}
					if (activity != null) {
						Exception res = GPXUtilities.writeGpxFile(toSave, gpx);
						gpx.path = toSave.getAbsolutePath();
						if (showOnMap) {
							activity.getMyApplication().getSelectedGpxHelper().selectGpxFile(gpx, true, false);
						}
						return res;
					}
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
					if (activity != null) {
						Exception res = GPXUtilities.writeGpxFile(toSave, gpx);
						if (showOnMap) {
							SelectedGpxFile sf = activity.getMyApplication().getSelectedGpxHelper().selectGpxFile(gpx, true, false);
							if (sf != null) {
								if (actionType == NewGpxData.ActionType.ADD_SEGMENT || actionType == NewGpxData.ActionType.EDIT_SEGMENT) {
									sf.processPoints(getMyApplication());
								}
							}
						}
						return res;
					}
				}
				return null;
			}

			@Override
			protected void onPostExecute(Exception warning) {
				MapActivity activity = getMapActivity();
				if (progressDialog != null && progressDialog.isShowing()) {
					progressDialog.dismiss();
				}
				if (activity != null) {
					activity.refreshMap();
					if (warning == null) {
						saved = true;
						if (openTrackActivity) {
							dismiss(activity);
						} else {
							Toast.makeText(activity,
									MessageFormat.format(getString(R.string.gpx_saved_sucessfully), toSave.getAbsolutePath()),
									Toast.LENGTH_LONG).show();
							if (close) {
								dismiss(activity);
							}
						}
					} else {
						Toast.makeText(activity, warning.getMessage(), Toast.LENGTH_LONG).show();
					}
				}
			}
		}.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	private void enable(View... views) {
		for (View view : views) {
			view.setEnabled(true);
			view.setAlpha(1);
		}
	}

	private void disable(View... views) {
		for (View view : views) {
			view.setEnabled(false);
			view.setAlpha(.5f);
		}
	}

	private void updateText() {
		MeasurementToolLayer measurementLayer = getMeasurementLayer();
		if (measurementLayer != null) {
			distanceTv.setText(measurementLayer.getDistanceSt() + ",");
			pointsTv.setText((portrait ? pointsSt + ": " : "") + editingCtx.getPointsCount());
		}
	}

	private void enterMeasurementMode() {
		MapActivity mapActivity = getMapActivity();
		MeasurementToolLayer measurementLayer = getMeasurementLayer();
		if (mapActivity != null && measurementLayer != null) {
			measurementLayer.setInMeasurementMode(true);
			mapActivity.refreshMap();
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

			updateText();
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

			mapActivity.refreshMap();
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

				builder.setView(view);
				builder.setPositiveButton(R.string.shared_string_ok, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						if (showOnMapToggle.isChecked()) {
							final String name = new SimpleDateFormat("yyyy-MM-dd_HH-mm_EEE", Locale.US).format(new Date());
							String fileName = name + GPX_SUFFIX;
							File fout = new File(dir, fileName);
							int ind = 1;
							while (fout.exists()) {
								fileName = name + "_" + (++ind) + GPX_SUFFIX;
								fout = new File(dir, fileName);
							}
							saveNewGpx(dir, fileName, true, SaveType.LINE, true);
						} else {
							dismiss(mapActivity);
						}
					}
				});
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
			}
			if (editingCtx.getNewGpxData() != null) {
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
		return showFragment(new MeasurementToolFragment(), fragmentManager);
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

		MeasurementToolBarController(NewGpxData newGpxData) {
			super(MapInfoWidgetsFactory.TopToolbarControllerType.MEASUREMENT_TOOL);
			setBackBtnIconClrIds(0, 0);
			setTitleTextClrIds(R.color.text_color_tab_active_light, R.color.text_color_tab_active_dark);
			setDescrTextClrIds(R.color.text_color_tab_active_light, R.color.text_color_tab_active_dark);
			setBgIds(R.drawable.gradient_toolbar, R.drawable.gradient_toolbar,
					R.drawable.gradient_toolbar, R.drawable.gradient_toolbar);
			setCloseBtnVisible(false);
			if (newGpxData != null) {
				setSaveViewVisible(true);
			}
			setSingleLineTitle(false);
		}

		@Override
		public void updateToolbar(MapInfoWidgetsFactory.TopToolbarView view) {
			super.updateToolbar(view);
			View shadow = view.getShadowView();
			if (shadow != null) {
				shadow.setVisibility(View.GONE);
			}
		}

		@Override
		public int getStatusBarColor(Context context, boolean night) {
			return NO_COLOR;
		}
	}
}
