package net.osmand.plus.measurementtool;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.widget.TextViewCompat;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;

import net.osmand.AndroidUtils;
import net.osmand.FileUtils;
import net.osmand.GPXUtilities;
import net.osmand.GPXUtilities.GPXFile;
import net.osmand.GPXUtilities.WptPt;
import net.osmand.data.LatLon;
import net.osmand.data.QuadRect;
import net.osmand.plus.GpxSelectionHelper.SelectedGpxFile;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.TargetPointsHelper;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.Version;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.MapActivityActions;
import net.osmand.plus.activities.TrackActivity;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.base.ContextMenuFragment.MenuState;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.mapcontextmenu.other.TrackDetailsMenu;
import net.osmand.plus.measurementtool.GpxApproximationFragment.GpxApproximationFragmentListener;
import net.osmand.plus.measurementtool.OptionsBottomSheetDialogFragment.OptionsFragmentListener;
import net.osmand.plus.measurementtool.RouteBetweenPointsBottomSheetDialogFragment.RouteBetweenPointsDialogMode;
import net.osmand.plus.measurementtool.RouteBetweenPointsBottomSheetDialogFragment.RouteBetweenPointsDialogType;
import net.osmand.plus.measurementtool.RouteBetweenPointsBottomSheetDialogFragment.RouteBetweenPointsFragmentListener;
import net.osmand.plus.measurementtool.SaveGpxRouteAsyncTask.SaveGpxRouteListener;
import net.osmand.plus.measurementtool.SelectedPointBottomSheetDialogFragment.SelectedPointFragmentListener;
import net.osmand.plus.measurementtool.adapter.MeasurementToolAdapter.MeasurementAdapterListener;
import net.osmand.plus.measurementtool.command.AddPointCommand;
import net.osmand.plus.measurementtool.command.ApplyGpxApproximationCommand;
import net.osmand.plus.measurementtool.command.ChangeRouteModeCommand;
import net.osmand.plus.measurementtool.command.ChangeRouteModeCommand.ChangeRouteType;
import net.osmand.plus.measurementtool.command.ClearPointsCommand;
import net.osmand.plus.measurementtool.command.MovePointCommand;
import net.osmand.plus.measurementtool.command.RemovePointCommand;
import net.osmand.plus.measurementtool.command.ReorderPointCommand;
import net.osmand.plus.measurementtool.command.ReversePointsCommand;
import net.osmand.plus.routepreparationmenu.cards.BaseCard;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.views.layers.MapControlsLayer;
import net.osmand.plus.views.mapwidgets.MapInfoWidgetsFactory.TopToolbarController;
import net.osmand.plus.views.mapwidgets.MapInfoWidgetsFactory.TopToolbarControllerType;
import net.osmand.plus.views.mapwidgets.MapInfoWidgetsFactory.TopToolbarView;
import net.osmand.router.RoutePlannerFrontEnd.GpxRouteApproximation;
import net.osmand.util.Algorithms;

import java.io.File;
import java.lang.ref.WeakReference;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static net.osmand.IndexConstants.GPX_FILE_EXT;
import static net.osmand.IndexConstants.GPX_INDEX_DIR;
import static net.osmand.plus.UiUtilities.CustomRadioButtonType.END;
import static net.osmand.plus.UiUtilities.CustomRadioButtonType.START;
import static net.osmand.plus.measurementtool.MeasurementEditingContext.CalculationMode;
import static net.osmand.plus.measurementtool.MeasurementEditingContext.SnapToRoadProgressListener;
import static net.osmand.plus.measurementtool.SaveAsNewTrackBottomSheetDialogFragment.SaveAsNewTrackFragmentListener;
import static net.osmand.plus.measurementtool.SelectFileBottomSheet.Mode.ADD_TO_TRACK;
import static net.osmand.plus.measurementtool.SelectFileBottomSheet.SelectFileListener;
import static net.osmand.plus.measurementtool.command.ClearPointsCommand.ClearCommandMode;
import static net.osmand.plus.measurementtool.command.ClearPointsCommand.ClearCommandMode.AFTER;
import static net.osmand.plus.measurementtool.command.ClearPointsCommand.ClearCommandMode.ALL;
import static net.osmand.plus.measurementtool.command.ClearPointsCommand.ClearCommandMode.BEFORE;

public class MeasurementToolFragment extends BaseOsmAndFragment implements RouteBetweenPointsFragmentListener,
		OptionsFragmentListener, GpxApproximationFragmentListener, SelectedPointFragmentListener,
		SaveAsNewTrackFragmentListener, MapControlsLayer.MapControlsThemeInfoProvider {

	public static final String TAG = MeasurementToolFragment.class.getSimpleName();
	public static final String TAPS_DISABLED_KEY = "taps_disabled_key";

	private String previousToolBarTitle = "";
	private MeasurementToolBarController toolBarController;
	private TextView distanceTv;
	private TextView pointsTv;
	private TextView distanceToCenterTv;
	private String pointsSt;
	private View additionalInfoContainer;
	private ViewGroup cardsContainer;
	private BaseCard visibleCard;
	private PointsCard pointsCard;
	private GraphsCard graphsCard;
	private LinearLayout customRadioButton;
	private View mainView;
	private ImageView upDownBtn;
	private ImageView undoBtn;
	private ImageView redoBtn;
	private ImageView mainIcon;
	private Snackbar snackbar;
	private String fileName;

	private AdditionalInfoType currentAdditionalInfoType;

	private boolean wasCollapseButtonVisible;
	private boolean progressBarVisible;
	private boolean additionalInfoExpanded;

	private static final int PLAN_ROUTE_MODE = 0x1;
	private static final int DIRECTION_MODE = 0x2;
	private static final int FOLLOW_TRACK_MODE = 0x4;
	private static final int UNDO_MODE = 0x8;
	private int modes = 0x0;

	private boolean portrait;
	private boolean nightMode;
	private int cachedMapPosition;

	private MeasurementEditingContext editingCtx = new MeasurementEditingContext();
	private GraphDetailsMenu detailsMenu;

	private LatLon initialPoint;

	enum SaveType {
		ROUTE,
		LINE
	}

	enum FinalSaveAction {
		SHOW_SNACK_BAR_AND_CLOSE,
		SHOW_TOAST,
		SHOW_IS_SAVED_FRAGMENT
	}

	private enum AdditionalInfoType {
		POINTS,
		GRAPH
	}

	private class GraphDetailsMenu extends TrackDetailsMenu {

		@Override
		protected int getFragmentWidth() {
			return mainView.getWidth();
		}

		@Override
		protected int getFragmentHeight() {
			return mainView.getHeight();
		}

		@Override
		public boolean shouldShowXAxisPoints() {
			return false;
		}
	}

	private void setEditingCtx(MeasurementEditingContext editingCtx) {
		this.editingCtx = editingCtx;
	}

	private void setInitialPoint(LatLon initialPoint) {
		this.initialPoint = initialPoint;
	}

	private void setMode(int mode, boolean on) {
		int modes = this.modes;
		if (on) {
			modes |= mode;
		} else {
			modes &= ~mode;
		}
		this.modes = modes;
	}

	boolean isPlanRouteMode() {
		return (this.modes & PLAN_ROUTE_MODE) == PLAN_ROUTE_MODE;
	}

	private boolean isDirectionMode() {
		return (this.modes & DIRECTION_MODE) == DIRECTION_MODE;
	}

	private boolean isFollowTrackMode() {
		return (this.modes & FOLLOW_TRACK_MODE) == FOLLOW_TRACK_MODE;
	}

	private boolean isUndoMode() {
		return (this.modes & UNDO_MODE) == UNDO_MODE;
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requireMyActivity().getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
			public void handleOnBackPressed() {
				quit(true);
			}
		});
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
							 @Nullable Bundle savedInstanceState) {
		final MapActivity mapActivity = (MapActivity) getActivity();
		if (mapActivity == null) {
			return null;
		}
		final MeasurementToolLayer measurementLayer = mapActivity.getMapLayers().getMeasurementToolLayer();

		editingCtx.setApplication(mapActivity.getMyApplication());
		editingCtx.setProgressListener(new SnapToRoadProgressListener() {
			@Override
			public void showProgressBar() {
				MeasurementToolFragment.this.showProgressBar();
				updateAdditionalInfoView();
			}

			@Override
			public void updateProgress(int progress) {
				((ProgressBar) mainView.findViewById(R.id.snap_to_road_progress_bar)).setProgress(progress);
			}

			@Override
			public void hideProgressBar() {
				((ProgressBar) mainView.findViewById(R.id.snap_to_road_progress_bar)).setVisibility(View.GONE);
				progressBarVisible = false;
				updateAdditionalInfoView();
			}

			@Override
			public void refresh() {
				measurementLayer.refreshMap();
				updateDistancePointsText();
			}
		});

		measurementLayer.setEditingCtx(editingCtx);

		nightMode = mapActivity.getMyApplication().getDaynightHelper().isNightModeForMapControls();
		portrait = AndroidUiHelper.isOrientationPortrait(mapActivity);

		pointsSt = getString(R.string.shared_string_gpx_points).toLowerCase();
		int widthInPixels = getResources().getDimensionPixelOffset(R.dimen.gpx_group_button_width);

		View view = UiUtilities.getInflater(getContext(), nightMode)
				.inflate(R.layout.fragment_measurement_tool, container, false);

		mainView = view.findViewById(R.id.main_view);
		AndroidUtils.setBackground(mapActivity, mainView, nightMode, R.drawable.bg_bottom_menu_light, R.drawable.bg_bottom_menu_dark);
		detailsMenu = new GraphDetailsMenu();
		additionalInfoContainer = mainView.findViewById(R.id.additional_info_container);
		cardsContainer = mainView.findViewById(R.id.cards_container);
		if (portrait) {
			customRadioButton = mainView.findViewById(R.id.custom_radio_buttons);

			View pointListBtn = customRadioButton.findViewById(R.id.left_button_container);
			TextView tvPointListBtn = customRadioButton.findViewById(R.id.left_button);
			tvPointListBtn.setText(R.string.shared_string_gpx_points);
			tvPointListBtn.setEnabled(true);
			pointListBtn.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					changeAdditionalInfoType(AdditionalInfoType.POINTS);
					int pointsCount = editingCtx.getPointsCount();
					if (pointsCount < 1) {
						disable(upDownBtn);
						collapseAdditionalInfoView();
					} else {
						expandAdditionalInfoView();
					}
					updateUpDownBtn();
				}
			});

			View graphBtn = customRadioButton.findViewById(R.id.right_button_container);
			TextView tvGraphBtn = customRadioButton.findViewById(R.id.right_button);
			tvGraphBtn.setText(R.string.shared_string_graph);
			graphBtn.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					changeAdditionalInfoType(AdditionalInfoType.GRAPH);
					expandAdditionalInfoView();
					updateUpDownBtn();
				}
			});
		}
		pointsCard = new PointsCard(mapActivity, this);
		graphsCard = new GraphsCard(mapActivity, detailsMenu, this);

		if (progressBarVisible) {
			showProgressBar();
		}

		distanceTv = (TextView) mainView.findViewById(R.id.measurement_distance_text_view);
		pointsTv = (TextView) mainView.findViewById(R.id.measurement_points_text_view);
		distanceToCenterTv = (TextView) mainView.findViewById(R.id.distance_to_center_text_view);

		mainIcon = (ImageView) mainView.findViewById(R.id.main_icon);
		upDownBtn = (ImageView) mainView.findViewById(R.id.up_down_button);
		updateUpDownBtn();

		mainView.findViewById(R.id.cancel_move_point_button).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				cancelMovePointMode();
			}
		});

		mainView.findViewById(R.id.cancel_point_before_after_button).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				cancelAddPointBeforeOrAfterMode();
			}
		});

		View upDownRow = mainView.findViewById(R.id.up_down_row);
		upDownRow.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				if (!additionalInfoExpanded && editingCtx.getPointsCount() > 0 && editingCtx.getSelectedPointPosition() == -1) {
					expandAdditionalInfoView();
				} else {
					collapseAdditionalInfoView();
				}
			}
		});

		View applyMovePointButton = mainView.findViewById(R.id.apply_move_point_button);
		UiUtilities.setupDialogButton(nightMode, applyMovePointButton, UiUtilities.DialogButtonType.PRIMARY,
				R.string.shared_string_apply);
		applyMovePointButton.setMinimumWidth(widthInPixels);
		applyMovePointButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				applyMovePointMode();
			}
		});


		View applyPointBeforeAfterButton = mainView.findViewById(R.id.apply_point_before_after_point_button);
		UiUtilities.setupDialogButton(nightMode, applyPointBeforeAfterButton, UiUtilities.DialogButtonType.PRIMARY,
				R.string.shared_string_apply);
		applyPointBeforeAfterButton.setMinimumWidth(widthInPixels);
		applyPointBeforeAfterButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				applyAddPointBeforeAfterMode();
			}
		});

		View addPointBeforeAfterButton = mainView.findViewById(R.id.add_point_before_after_button);
		UiUtilities.setupDialogButton(nightMode, addPointBeforeAfterButton, UiUtilities.DialogButtonType.PRIMARY,
				R.string.shared_string_add);
		addPointBeforeAfterButton.setMinimumWidth(widthInPixels);
		addPointBeforeAfterButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				addPointBeforeAfter();
			}
		});

		mainView.findViewById(R.id.options_button).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				boolean trackSnappedToRoad = !editingCtx.isApproximationNeeded();
				OptionsBottomSheetDialogFragment.showInstance(mapActivity.getSupportFragmentManager(),
						MeasurementToolFragment.this,
						trackSnappedToRoad,
						editingCtx.getAppMode().getStringKey()
				);
			}
		});

		undoBtn = ((ImageButton) mainView.findViewById(R.id.undo_point_button));
		redoBtn = ((ImageButton) mainView.findViewById(R.id.redo_point_button));

		Drawable undoDrawable = getActiveIcon(R.drawable.ic_action_undo_dark);
		undoBtn.setImageDrawable(AndroidUtils.getDrawableForDirection(mapActivity, undoDrawable));
		undoBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				editingCtx.getCommandManager().undo();
				updateUndoRedoButton(editingCtx.getCommandManager().canUndo(), undoBtn);
				updateUndoRedoButton(true, redoBtn);
				updateUndoRedoCommonStuff();
			}
		});

		Drawable redoDrawable = getActiveIcon(R.drawable.ic_action_redo_dark);
		redoBtn.setImageDrawable(AndroidUtils.getDrawableForDirection(mapActivity, redoDrawable));
		redoBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				editingCtx.getCommandManager().redo();
				updateUndoRedoButton(editingCtx.getCommandManager().canRedo(), redoBtn);
				updateUndoRedoButton(true, undoBtn);
				updateUndoRedoCommonStuff();
			}
		});

		View addPointButton = mainView.findViewById(R.id.add_point_button);
		UiUtilities.setupDialogButton(nightMode, addPointButton, UiUtilities.DialogButtonType.PRIMARY,
				R.string.shared_string_add);
		addPointButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				addCenterPoint();
			}
		});
		addPointButton.setMinimumWidth(widthInPixels);
		measurementLayer.setOnSingleTapListener(new MeasurementToolLayer.OnSingleTapListener() {
			@Override
			public void onAddPoint() {
				addPoint();
			}

			@Override
			public void onSelectPoint(int selectedPointPos) {
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
				if (additionalInfoExpanded) {
					collapseAdditionalInfoView();
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
		toolBarController.setOnBackButtonClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				MapActivity mapActivity = getMapActivity();
				if (mapActivity != null) {
					GpxApproximationFragment gpxApproximationFragment = mapActivity.getGpxApproximationFragment();
					SnapTrackWarningFragment snapTrackWarningFragment = mapActivity.getSnapTrackWarningBottomSheet();
					if (gpxApproximationFragment != null) {
						gpxApproximationFragment.dismissImmediate();
					} else if (snapTrackWarningFragment != null) {
						snapTrackWarningFragment.dismissImmediate();
					} else {
						quit(false);
					}
				}
			}
		});
		toolBarController.setOnSaveViewClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (isFollowTrackMode()) {
					startTrackNavigation();
				} else {
					saveChanges(FinalSaveAction.SHOW_SNACK_BAR_AND_CLOSE, false);
				}
			}
		});
		updateToolbar();

		final GpxData gpxData = editingCtx.getGpxData();

		ImageButton snapToRoadBtn = (ImageButton) mapActivity.findViewById(R.id.snap_to_road_image_button);
		snapToRoadBtn.setBackgroundResource(nightMode ? R.drawable.btn_circle_night : R.drawable.btn_circle);
		snapToRoadBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				startSnapToRoad(false);
			}
		});
		snapToRoadBtn.setVisibility(View.VISIBLE);

		initMeasurementMode(gpxData, savedInstanceState == null);

		if (savedInstanceState == null) {
			if (fileName != null) {
				addNewGpxData(getGpxFile(fileName));
			} else if (editingCtx.isApproximationNeeded()) {
				enterApproximationMode(mapActivity);
			}
		} else {
			measurementLayer.setTapsDisabled(savedInstanceState.getBoolean(TAPS_DISABLED_KEY));
		}

		return view;
	}

	private void changeAdditionalInfoType(@NonNull AdditionalInfoType type) {
		if (!additionalInfoExpanded || !isCurrentAdditionalInfoType(type)) {
			MapActivity ma = getMapActivity();
			if (ma == null) return;

			OsmandApplication app = ma.getMyApplication();
			if (AdditionalInfoType.POINTS == type) {
				visibleCard = pointsCard;
				UiUtilities.updateCustomRadioButtons(app, customRadioButton, nightMode, START);
			} else if (AdditionalInfoType.GRAPH == type) {
				visibleCard = graphsCard;
				UiUtilities.updateCustomRadioButtons(app, customRadioButton, nightMode, END);
			}
			cardsContainer.removeAllViews();
			View cardView = visibleCard.getView() != null ? visibleCard.getView() : visibleCard.build(ma);
			cardsContainer.addView(cardView);

			currentAdditionalInfoType = type;
			additionalInfoExpanded = true;
			updateUpDownBtn();
		}
	}

	private void updateAdditionalInfoView() {
		updateAdditionalInfoView(pointsCard);
		updateAdditionalInfoView(graphsCard);
	}

	private void updateAdditionalInfoView(OnUpdateAdditionalInfoListener listener) {
		if (listener != null) {
			listener.onUpdateAdditionalInfo();
		}
	}

	public boolean isInEditMode() {
		return !isPlanRouteMode() && !editingCtx.isNewData() && !isDirectionMode() && !isFollowTrackMode();
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public MeasurementEditingContext getEditingCtx() {
		return editingCtx;
	}

	private void updateUndoRedoCommonStuff() {
		collapseAdditionalInfoIfNoPointsEnough();
		if (editingCtx.getPointsCount() > 0) {
			enable(upDownBtn);
		}
		updateAdditionalInfoView();
		updateDistancePointsText();
		updateSnapToRoadControls();
	}

	private void initMeasurementMode(GpxData gpxData, boolean addPoints) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			editingCtx.getCommandManager().setMeasurementLayer(mapActivity.getMapLayers().getMeasurementToolLayer());
			enterMeasurementMode();
			if (gpxData != null && addPoints) {
				if (!isUndoMode()) {
					List<WptPt> points = gpxData.getGpxFile().getRoutePoints();
					if (!points.isEmpty()) {
						ApplicationMode snapToRoadAppMode = ApplicationMode.valueOfStringKey(points.get(points.size() - 1).getProfileType(), null);
						if (snapToRoadAppMode != null) {
							setAppMode(snapToRoadAppMode);
						}
					}
				}
				collectPoints();
			}
			updateSnapToRoadControls();
			setMode(UNDO_MODE, false);
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			detailsMenu.setMapActivity(mapActivity);
			mapActivity.getMapLayers().getMapControlsLayer().addThemeInfoProviderTag(TAG);
			mapActivity.getMapLayers().getMapControlsLayer().showMapControlsIfHidden();
			cachedMapPosition = mapActivity.getMapView().getMapPosition();
			setDefaultMapPosition();
			addInitialPoint();
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			mapActivity.getMapLayers().getMapControlsLayer().removeThemeInfoProviderTag(TAG);
		}
		detailsMenu.onDismiss();
		detailsMenu.setMapActivity(null);
		setMapPosition(cachedMapPosition);
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		cancelModes();
		exitMeasurementMode();
		if (additionalInfoExpanded) {
			collapseAdditionalInfoView();
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
		if (AndroidUtils.isActivityNotDestroyed(activity)) {
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

	public boolean isProgressBarVisible() {
		return progressBarVisible;
	}

	private void updateMainIcon() {
		GpxData gpxData = editingCtx.getGpxData();
		mainIcon.setImageDrawable(getActiveIcon(gpxData != null ? R.drawable.ic_action_polygom_dark : R.drawable.ic_action_ruler));
	}

	public void startSnapToRoad(boolean rememberPreviousTitle) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			if (rememberPreviousTitle) {
				previousToolBarTitle = toolBarController.getTitle();
			}
			toolBarController.setTitle(getString(R.string.route_between_points));
			mapActivity.refreshMap();

			if (editingCtx.isApproximationNeeded()) {
				enterApproximationMode(mapActivity);
			} else {
				RouteBetweenPointsBottomSheetDialogFragment.showInstance(mapActivity.getSupportFragmentManager(),
						this, RouteBetweenPointsDialogType.WHOLE_ROUTE_CALCULATION,
						editingCtx.getLastCalculationMode() == CalculationMode.NEXT_SEGMENT
								? RouteBetweenPointsDialogMode.SINGLE
								: RouteBetweenPointsDialogMode.ALL,
						editingCtx.getAppMode());
			}
		}
	}

	public void saveChanges(FinalSaveAction finalSaveAction, boolean showDialog) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			if (editingCtx.getPointsCount() > 0) {
				if (editingCtx.isNewData() || isInEditMode()) {
					if (showDialog) {
						openSaveAsNewTrackMenu(mapActivity);
					} else {
						saveNewGpx("", getSuggestedFileName(), true, false, finalSaveAction);
					}
				} else {
					addToGpx(mapActivity, finalSaveAction);
				}
			} else {
				Toast.makeText(mapActivity, getString(R.string.none_point_error), Toast.LENGTH_SHORT).show();
			}
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		MapActivity mapActivity = getMapActivity();
		switch (requestCode) {
			case SnapTrackWarningFragment.REQUEST_CODE:
				switch (resultCode) {
					case SnapTrackWarningFragment.CANCEL_RESULT_CODE:
						toolBarController.setSaveViewVisible(true);
						setMode(DIRECTION_MODE, false);
						exitApproximationMode();
						updateToolbar();
						break;
					case SnapTrackWarningFragment.CONTINUE_RESULT_CODE:
						if (mapActivity != null) {
							ApplicationMode mode = editingCtx.getAppMode();
							if (mode == ApplicationMode.DEFAULT || "public_transport".equals(mode.getRoutingProfile())) {
								mode = null;
							}
							List<List<WptPt>> pointsSegments = editingCtx.getPointsSegments(true, false);
							if (!pointsSegments.isEmpty()) {
								GpxApproximationFragment.showInstance(
										mapActivity.getSupportFragmentManager(), this, pointsSegments, mode);
							}
						}
						break;
				}
				break;
			case ExitBottomSheetDialogFragment.REQUEST_CODE:
				switch (resultCode) {
					case ExitBottomSheetDialogFragment.EXIT_RESULT_CODE:
						if (mapActivity != null) {
							dismiss(getMapActivity());
						}
						break;
					case ExitBottomSheetDialogFragment.SAVE_RESULT_CODE:
						if (mapActivity != null) {
							openSaveAsNewTrackMenu(getMapActivity());
						}
						break;
				}
		}
	}

	@Override
	public void snapToRoadOnCLick() {
		startSnapToRoad(true);
	}

	@Override
	public void directionsOnClick() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			OsmandApplication app = mapActivity.getMyApplication();
			MapActivityActions mapActions = mapActivity.getMapActions();
			TargetPointsHelper targetPointsHelper = app.getTargetPointsHelper();
			ApplicationMode appMode = editingCtx.getAppMode();
			if (appMode == ApplicationMode.DEFAULT) {
				appMode = null;
			}
			List<WptPt> points = editingCtx.getPoints();
			if (points.size() > 0) {
				if (points.size() == 1) {
					targetPointsHelper.clearAllPoints(false);
					targetPointsHelper.navigateToPoint(new LatLon(points.get(0).getLatitude(), points.get(0).getLongitude()), false, -1);
					dismiss(mapActivity);
					mapActions.enterRoutePlanningModeGivenGpx(null, appMode, null, null, true, true, MenuState.HEADER_ONLY);
				} else {
					String trackName = getSuggestedFileName();
					if (editingCtx.hasRoute()) {
						GPXFile gpx = editingCtx.exportGpx(trackName);
						if (gpx != null) {
							dismiss(mapActivity);
							runNavigation(gpx, appMode);
						} else {
							Toast.makeText(mapActivity, getString(R.string.error_occurred_saving_gpx), Toast.LENGTH_SHORT).show();
						}
					} else {
						if (editingCtx.isApproximationNeeded()) {
							setMode(DIRECTION_MODE, true);
							enterApproximationMode(mapActivity);
						} else {
							GPXFile gpx = new GPXFile(Version.getFullVersion(requireMyApplication()));
							gpx.addRoutePoints(points, true);
							dismiss(mapActivity);
							targetPointsHelper.clearAllPoints(false);
							mapActions.enterRoutePlanningModeGivenGpx(gpx, appMode, null, null, true, true, MenuState.HEADER_ONLY);
						}
					}
				}
			} else {
				Toast.makeText(mapActivity, getString(R.string.none_point_error), Toast.LENGTH_SHORT).show();
			}
		}
	}

	private void runNavigation(final GPXFile gpx, final ApplicationMode appMode) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			OsmandApplication app = mapActivity.getMyApplication();
			if (app.getRoutingHelper().isFollowingMode()) {
				if (isFollowTrackMode()) {
					mapActivity.getMapActions().setGPXRouteParams(gpx);
					app.getTargetPointsHelper().updateRouteAndRefresh(true);
					app.getRoutingHelper().recalculateRouteDueToSettingsChange();
				} else {
					mapActivity.getMapActions().stopNavigationActionConfirm(new Runnable() {
						@Override
						public void run() {
							MapActivity mapActivity = getMapActivity();
							if (mapActivity != null) {
								mapActivity.getMapActions().enterRoutePlanningModeGivenGpx(gpx, appMode, null, null, true, true, MenuState.HEADER_ONLY);
							}
						}
					});
				}
			} else {
				mapActivity.getMapActions().stopNavigationWithoutConfirm();
				mapActivity.getMapActions().enterRoutePlanningModeGivenGpx(gpx, appMode, null, null, true, true, MenuState.HEADER_ONLY);
			}
		}
	}

	@Override
	public void saveChangesOnClick() {
		if (isFollowTrackMode()) {
			startTrackNavigation();
		} else {
			saveChanges(FinalSaveAction.SHOW_TOAST, true);
		}
	}

	@Override
	public void saveAsNewTrackOnClick() {
		openSaveAsNewTrackMenu(getMapActivity());
	}

	@Override
	public void addToTrackOnClick() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			if (editingCtx.getPointsCount() > 0) {
				showAddToTrackDialog(mapActivity);
			} else {
				Toast.makeText(mapActivity, getString(R.string.none_point_error), Toast.LENGTH_SHORT).show();
			}
		}
	}

	@Override
	public void clearAllOnClick() {
		MeasurementToolLayer measurementLayer = getMeasurementLayer();
		editingCtx.getCommandManager().execute(new ClearPointsCommand(measurementLayer, ALL));
		editingCtx.cancelSnapToRoad();
		if (additionalInfoExpanded) {
			collapseAdditionalInfoView();
		}
		updateUndoRedoButton(false, redoBtn);
		disable(upDownBtn);
		updateDistancePointsText();
	}

	@Override
	public void reverseRouteOnClick() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			List<WptPt> points = editingCtx.getPoints();
			if (points.size() > 1) {
				MeasurementToolLayer measurementLayer = getMeasurementLayer();
				editingCtx.getCommandManager().execute(new ReversePointsCommand(measurementLayer));
				if (additionalInfoExpanded) {
					collapseAdditionalInfoView();
				}
				updateUndoRedoButton(false, redoBtn);
				updateUndoRedoButton(true, undoBtn);
				updateDistancePointsText();
			} else {
				Toast.makeText(mapActivity, getString(R.string.one_point_error), Toast.LENGTH_SHORT).show();
			}
		}
	}

	@Override
	public void onMovePoint() {
		MeasurementToolLayer measurementLayer = getMeasurementLayer();
		if (measurementLayer != null) {
			measurementLayer.enterMovingPointMode();
		}
		switchMovePointMode(true);
	}

	@Override
	public void onDeletePoint() {
		MeasurementToolLayer measurementLayer = getMeasurementLayer();
		if (measurementLayer != null) {
			removePoint(measurementLayer, editingCtx.getSelectedPointPosition());
		}
		editingCtx.setSelectedPointPosition(-1);
	}

	@Override
	public void onAddPointAfter() {
		MeasurementToolLayer measurementLayer = getMeasurementLayer();
		if (measurementLayer != null) {
			measurementLayer.moveMapToPoint(editingCtx.getSelectedPointPosition());
			editingCtx.setInAddPointMode(true, false);
			editingCtx.splitSegments(editingCtx.getSelectedPointPosition() + 1);
		}
		((TextView) mainView.findViewById(R.id.add_point_before_after_text)).setText(mainView.getResources().getString(R.string.add_point_after));
		mainIcon.setImageDrawable(getActiveIcon(R.drawable.ic_action_addpoint_above));
		switchAddPointBeforeAfterMode(true);
	}

	@Override
	public void onAddPointBefore() {
		MeasurementToolLayer measurementLayer = getMeasurementLayer();
		if (measurementLayer != null) {
			measurementLayer.moveMapToPoint(editingCtx.getSelectedPointPosition());
			editingCtx.setInAddPointMode(true, true);
			editingCtx.splitSegments(editingCtx.getSelectedPointPosition());
		}
		((TextView) mainView.findViewById(R.id.add_point_before_after_text)).setText(mainView.getResources().getString(R.string.add_point_before));
		mainIcon.setImageDrawable(getActiveIcon(R.drawable.ic_action_addpoint_below));
		switchAddPointBeforeAfterMode(true);
	}

	@Override
	public void onTrimRouteBefore() {
		trimRoute(BEFORE);
	}

	@Override
	public void onTrimRouteAfter() {
		trimRoute(AFTER);
	}

	private void trimRoute(ClearCommandMode before) {
		MeasurementToolLayer measurementLayer = getMeasurementLayer();
		editingCtx.getCommandManager().execute(new ClearPointsCommand(measurementLayer, before));
		if (additionalInfoExpanded) {
			collapseAdditionalInfoView();
		}
		editingCtx.setSelectedPointPosition(-1);
		editingCtx.splitSegments(editingCtx.getBeforePoints().size() + editingCtx.getAfterPoints().size());
		updateUndoRedoButton(false, redoBtn);
		updateUndoRedoButton(true, undoBtn);
		updateDistancePointsText();
	}

	@Override
	public void onChangeRouteTypeBefore() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			RouteBetweenPointsBottomSheetDialogFragment.showInstance(mapActivity.getSupportFragmentManager(),
					this, RouteBetweenPointsDialogType.PREV_ROUTE_CALCULATION,
					RouteBetweenPointsDialogMode.SINGLE,
					editingCtx.getBeforeSelectedPointAppMode());
		}
	}

	@Override
	public void onChangeRouteTypeAfter() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			RouteBetweenPointsBottomSheetDialogFragment.showInstance(mapActivity.getSupportFragmentManager(),
					this, RouteBetweenPointsDialogType.NEXT_ROUTE_CALCULATION,
					RouteBetweenPointsDialogMode.SINGLE,
					editingCtx.getSelectedPointAppMode());
		}
	}

	@Override
	public void onCloseMenu() {
		setDefaultMapPosition();
	}

	@Override
	public void onClearSelection() {
		editingCtx.setSelectedPointPosition(-1);
	}

	@Override
	public void onCloseRouteDialog() {
		toolBarController.setTitle(previousToolBarTitle);
		editingCtx.setSelectedPointPosition(-1);
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			mapActivity.refreshMap();
		}
	}

	@Override
	public void onChangeApplicationMode(ApplicationMode mode, RouteBetweenPointsDialogType dialogType,
	                                    RouteBetweenPointsDialogMode dialogMode) {
		MeasurementToolLayer measurementLayer = getMeasurementLayer();
		if (measurementLayer != null) {
			ChangeRouteType changeRouteType = ChangeRouteType.NEXT_SEGMENT;
			switch (dialogType) {
				case WHOLE_ROUTE_CALCULATION:
					changeRouteType = dialogMode == RouteBetweenPointsDialogMode.SINGLE
							? ChangeRouteType.LAST_SEGMENT : ChangeRouteType.WHOLE_ROUTE;
					break;
				case NEXT_ROUTE_CALCULATION:
					changeRouteType = dialogMode == RouteBetweenPointsDialogMode.SINGLE
							? ChangeRouteType.NEXT_SEGMENT : ChangeRouteType.ALL_NEXT_SEGMENTS;
					break;
				case PREV_ROUTE_CALCULATION:
					changeRouteType = dialogMode == RouteBetweenPointsDialogMode.SINGLE
							? ChangeRouteType.PREV_SEGMENT : ChangeRouteType.ALL_PREV_SEGMENTS;
					break;
			}
			editingCtx.getCommandManager().execute(new ChangeRouteModeCommand(measurementLayer, mode, changeRouteType, editingCtx.getSelectedPointPosition()));
			updateUndoRedoButton(false, redoBtn);
			updateUndoRedoButton(true, undoBtn);
			disable(upDownBtn);
			updateSnapToRoadControls();
			updateDistancePointsText();
		}
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		MeasurementToolLayer measurementLayer = getMeasurementLayer();
		if (measurementLayer != null) {
			outState.putBoolean(TAPS_DISABLED_KEY, measurementLayer.isTapsDisabled());
		}
	}

	private GPXFile getGpxFile(String gpxFileName) {
		OsmandApplication app = getMyApplication();
		GPXFile gpxFile = null;
		if (app != null) {
			SelectedGpxFile selectedGpxFile = app.getSelectedGpxHelper().getSelectedFileByName(gpxFileName);
			if (selectedGpxFile != null) {
				gpxFile = selectedGpxFile.getGpxFile();
			} else {
				gpxFile = GPXUtilities.loadGPXFile(new File(app.getAppPath(GPX_INDEX_DIR), gpxFileName));
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
					GPXFile gpxFile;
					if (gpxFileName == null) {
						gpxFile = mapActivity.getMyApplication().getSavingTrackHelper().getCurrentGpx();
					} else {
						gpxFile = getGpxFile(gpxFileName);
					}
					SelectedGpxFile selectedGpxFile = mapActivity.getMyApplication().getSelectedGpxHelper()
							.getSelectedFileByPath(gpxFile.path);
					boolean showOnMap = selectedGpxFile != null;
					saveExistingGpx(gpxFile, showOnMap, false, true, FinalSaveAction.SHOW_TOAST);
				}
			}

			@Override
			public void dismissButtonOnClick() {
			}
		};
	}

	public void addNewGpxData(GPXFile gpxFile) {
		GpxData gpxData = setupGpxData(gpxFile);
		initMeasurementMode(gpxData, true);
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null && gpxData != null) {
			QuadRect qr = gpxData.getRect();
			mapActivity.getMapView().fitRectToMap(qr.left, qr.right, qr.top, qr.bottom,
					(int) qr.width(), (int) qr.height(), 0);
		}
	}

	@Nullable
	private GpxData setupGpxData(@Nullable GPXFile gpxFile) {
		GpxData gpxData = null;
		if (gpxFile != null) {
			gpxData = new GpxData(gpxFile);
		}
		editingCtx.setGpxData(gpxData);
		return gpxData;
	}

	private void removePoint(MeasurementToolLayer measurementLayer, int position) {
		if (measurementLayer != null) {
			editingCtx.getCommandManager().execute(new RemovePointCommand(measurementLayer, position));
			updateAdditionalInfoView();
			updateUndoRedoButton(true, undoBtn);
			updateUndoRedoButton(false, redoBtn);
			updateDistancePointsText();
			collapseAdditionalInfoIfNoPointsEnough();
		}
	}

	@Override
	public void onSaveAsNewTrack(String folderName, String fileName, boolean showOnMap, boolean simplified) {
		saveNewGpx(folderName, fileName, showOnMap, simplified, FinalSaveAction.SHOW_IS_SAVED_FRAGMENT);
	}

	MeasurementAdapterListener createMeasurementAdapterListener(final ItemTouchHelper touchHelper) {
		return new MeasurementAdapterListener() {

			final MapActivity mapActivity = getMapActivity();
			final MeasurementToolLayer measurementLayer = getMeasurementLayer();
			private int fromPosition;
			private int toPosition;

			@Override
			public void onRemoveClick(int position) {
				removePoint(measurementLayer, position);
			}

			@Override
			public void onItemClick(int position) {
				if (mapActivity != null && measurementLayer != null) {
					if (additionalInfoExpanded) {
						collapseAdditionalInfoView();
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
						updateAdditionalInfoView();
						updateUndoRedoButton(false, redoBtn);
						updateDistancePointsText();
						mapActivity.refreshMap();
					}
				}
			}
		};
	}

	private void setAppMode(@NonNull ApplicationMode appMode) {
		editingCtx.setAppMode(appMode);
		editingCtx.scheduleRouteCalculateIfNotEmpty();
		updateSnapToRoadControls();
	}

	private void resetAppMode() {
		toolBarController.setTopBarSwitchVisible(false);
		toolBarController.setTitle(previousToolBarTitle);
		mainIcon.setImageDrawable(getActiveIcon(R.drawable.ic_action_ruler));
		editingCtx.resetAppMode();
		editingCtx.cancelSnapToRoad();
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			mainView.findViewById(R.id.snap_to_road_progress_bar).setVisibility(View.GONE);
			mapActivity.refreshMap();
		}
	}

	private void updateSnapToRoadControls() {
		final MapActivity mapActivity = getMapActivity();
		final ApplicationMode appMode = editingCtx.getAppMode();
		if (mapActivity != null) {
			Drawable icon;
			if (isTrackReadyToCalculate()) {
				if (appMode == MeasurementEditingContext.DEFAULT_APP_MODE) {
					icon = getActiveIcon(R.drawable.ic_action_split_interval);
				} else {
					icon = getIcon(appMode.getIconRes(), appMode.getIconColorInfo().getColor(nightMode));
				}
			} else {
				icon = getContentIcon(R.drawable.ic_action_help);
			}
			ImageButton snapToRoadBtn = (ImageButton) mapActivity.findViewById(R.id.snap_to_road_image_button);
			snapToRoadBtn.setImageDrawable(icon);
			mapActivity.refreshMap();
		}
	}

	public boolean isTrackReadyToCalculate() {
		return !editingCtx.isApproximationNeeded() || editingCtx.isNewData();
	}

	private void hideSnapToRoadIcon() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			mapActivity.findViewById(R.id.snap_to_road_image_button).setVisibility(View.GONE);
		}
	}

	private void collectPoints() {
		MeasurementToolLayer measurementLayer = getMeasurementLayer();
		if (measurementLayer != null) {
			if (!isUndoMode()) {
				editingCtx.addPoints();
			}
			updateAdditionalInfoView();
			updateDistancePointsText();
		}
	}

	private void openSelectedPointMenu(MapActivity mapActivity) {
		if (mapActivity != null) {
			SelectedPointBottomSheetDialogFragment.showInstance(mapActivity.getSupportFragmentManager(), this);
		}
	}

	private void openSaveAsNewTrackMenu(MapActivity mapActivity) {
		if (mapActivity != null) {
			if (editingCtx.getPointsCount() > 0) {
				SaveAsNewTrackBottomSheetDialogFragment.showInstance(mapActivity.getSupportFragmentManager(),
						this, "", getSuggestedFileName(), true, true);
			} else {
				Toast.makeText(mapActivity, getString(R.string.none_point_error), Toast.LENGTH_SHORT).show();
			}
		}
	}

	private void showAddToTrackDialog(final MapActivity mapActivity) {
		if (mapActivity != null) {
			SelectFileBottomSheet.showInstance(mapActivity.getSupportFragmentManager(),
					createAddToTrackFileListener(), ADD_TO_TRACK);
		}
	}

	private void applyMovePointMode() {
		switchMovePointMode(false);
		MeasurementToolLayer measurementLayer = getMeasurementLayer();
		if (measurementLayer != null) {
			WptPt oldPoint = editingCtx.getOriginalPointToMove();
			WptPt newPoint = measurementLayer.getMovedPointToApply();
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

	void exitMovePointMode(boolean cancelled) {
		if (cancelled) {
			WptPt pt = editingCtx.getOriginalPointToMove();
			editingCtx.addPoint(pt);
		}
		editingCtx.setOriginalPointToMove(null);
		editingCtx.setSelectedPointPosition(-1);
		editingCtx.splitSegments(editingCtx.getBeforePoints().size() + editingCtx.getAfterPoints().size());
	}

	void cancelModes() {
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
		editingCtx.setInAddPointMode(false, false);
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
		editingCtx.setInAddPointMode(false, false);
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
			markGeneralComponents(enable ? View.GONE : View.VISIBLE);
			AndroidUiHelper.setVisibility(mapActivity, enable ? View.VISIBLE : View.GONE,
					R.id.move_point_text,
					R.id.move_point_controls);
			mainIcon.setImageDrawable(getActiveIcon(enable
					? R.drawable.ic_action_move_point
					: R.drawable.ic_action_ruler));
		}
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
			markGeneralComponents(enable ? View.GONE : View.VISIBLE);
			AndroidUiHelper.setVisibility(mapActivity, enable ? View.VISIBLE : View.GONE,
					R.id.add_point_before_after_text,
					R.id.add_point_before_after_controls);
			if (!enable) {
				mainIcon.setImageDrawable(getActiveIcon(R.drawable.ic_action_ruler));
			}
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
		updateAdditionalInfoView();
	}

	private void expandAdditionalInfoView() {
		if (portrait) {
			MapActivity mapActivity = getMapActivity();
			if (mapActivity != null) {
				additionalInfoContainer.setVisibility(View.VISIBLE);
				AdditionalInfoType typeToShow = currentAdditionalInfoType == null
						? AdditionalInfoType.POINTS : currentAdditionalInfoType;
				changeAdditionalInfoType(typeToShow);
				setMapPosition(portrait
						? OsmandSettings.MIDDLE_TOP_CONSTANT
						: OsmandSettings.LANDSCAPE_MIDDLE_RIGHT_CONSTANT);
			}
		}
	}

	private void collapseAdditionalInfoView() {
		if (portrait) {
			additionalInfoExpanded = false;
			updateUpDownBtn();
			additionalInfoContainer.setVisibility(View.GONE);
			setDefaultMapPosition();
		}
	}

	private void collapseAdditionalInfoIfNoPointsEnough() {
		MeasurementToolLayer measurementLayer = getMeasurementLayer();
		if (measurementLayer != null) {
			int pointsCount = editingCtx.getPointsCount();
			if (isCurrentAdditionalInfoType(AdditionalInfoType.GRAPH) && pointsCount < 2) {
				collapseAdditionalInfoView();
			} else if (pointsCount < 1) {
				disable(upDownBtn);
				collapseAdditionalInfoView();
				if (additionalInfoExpanded) {
					collapseAdditionalInfoView();
				}
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

	private void addToGpx(MapActivity mapActivity, FinalSaveAction finalSaveAction) {
		GpxData gpxData = editingCtx.getGpxData();
		GPXFile gpx = gpxData != null ? gpxData.getGpxFile() : null;
		if (gpx != null) {
			SelectedGpxFile selectedGpxFile =
					mapActivity.getMyApplication().getSelectedGpxHelper().getSelectedFileByPath(gpx.path);
			boolean showOnMap = selectedGpxFile != null;
			saveExistingGpx(gpx, showOnMap, false, false, finalSaveAction);
		}
	}

	private void updateUpDownBtn() {
		Drawable icon = getContentIcon(additionalInfoExpanded
				? R.drawable.ic_action_arrow_down : R.drawable.ic_action_arrow_up);
		upDownBtn.setImageDrawable(icon);
	}

	private boolean isCurrentAdditionalInfoType(@NonNull AdditionalInfoType type) {
		return type.equals(currentAdditionalInfoType);
	}

	public boolean hasVisibleGraph() {
		return graphsCard != null && graphsCard.hasVisibleGraph();
	}

	private String getSuggestedFileName() {
		GpxData gpxData = editingCtx.getGpxData();
		String displayedName = null;
		if (gpxData != null) {
			GPXFile gpxFile = gpxData.getGpxFile();
			if (!Algorithms.isEmpty(gpxFile.path)) {
				displayedName = Algorithms.getFileNameWithoutExtension(new File(gpxFile.path).getName());
			} else if (!Algorithms.isEmpty(gpxFile.tracks)) {
				displayedName = gpxFile.tracks.get(0).name;
			}
		}
		if (gpxData == null || displayedName == null) {
			String suggestedName = new SimpleDateFormat("EEE dd MMM yyyy", Locale.US).format(new Date());
			displayedName = FileUtils.createUniqueFileName(requireMyApplication(), suggestedName, GPX_INDEX_DIR, GPX_FILE_EXT);
		} else {
			displayedName = Algorithms.getFileNameWithoutExtension(new File(gpxData.getGpxFile().path).getName());
		}
		return displayedName;
	}

	private void saveNewGpx(String folderName, String fileName, boolean showOnMap,
							boolean simplified, FinalSaveAction finalSaveAction) {
		OsmandApplication app = getMyApplication();
		if (app != null) {
			File dir = getMyApplication().getAppPath(GPX_INDEX_DIR);
			if (!Algorithms.isEmpty(folderName) && !dir.getName().equals(folderName)) {
				dir = new File(dir, folderName);
			}
			fileName += GPX_FILE_EXT;
			saveNewGpx(dir, fileName, showOnMap, simplified, finalSaveAction);
		}
	}

	private void saveNewGpx(@NonNull File dir, @NonNull String fileName, boolean showOnMap,
							boolean simplified, FinalSaveAction finalSaveAction) {
		saveGpx(new File(dir, fileName), null, simplified, false, finalSaveAction, showOnMap);
	}

	private void saveExistingGpx(@NonNull GPXFile gpx, boolean showOnMap,
								 boolean simplified, boolean addToTrack, FinalSaveAction finalSaveAction) {
		saveGpx(new File(gpx.path), gpx, simplified, addToTrack, finalSaveAction, showOnMap);
	}

	private void saveGpx(@NonNull final File outFile, @Nullable GPXFile gpxFile, boolean simplified,
						 boolean addToTrack, final FinalSaveAction finalSaveAction, final boolean showOnMap) {
		SaveGpxRouteListener saveGpxRouteListener = new SaveGpxRouteListener() {
			@Override
			public void gpxSavingFinished(Exception warning, GPXFile savedGpxFile, File backupFile) {
				onGpxSaved(warning, savedGpxFile, outFile, backupFile, finalSaveAction, showOnMap);
			}
		};

		SaveGpxRouteAsyncTask saveTask = new SaveGpxRouteAsyncTask(this, outFile, gpxFile, simplified,
				addToTrack, showOnMap, saveGpxRouteListener);
		saveTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	private void onGpxSaved(Exception warning, GPXFile savedGpxFile, final File outFile, final File backupFile,
							FinalSaveAction finalSaveAction, final boolean showOnMap) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity == null) {
			return;
		}
		mapActivity.refreshMap();
		if (warning == null) {
			if (editingCtx.isNewData() && savedGpxFile != null) {
				GpxData gpxData = new GpxData(savedGpxFile);
				editingCtx.setGpxData(gpxData);
				updateToolbar();
			}
			if (isInEditMode()) {
				editingCtx.setChangesSaved();
				dismiss(mapActivity);
			} else {
				switch (finalSaveAction) {
					case SHOW_SNACK_BAR_AND_CLOSE:
						final WeakReference<MapActivity> mapActivityRef = new WeakReference<>(mapActivity);
						snackbar = Snackbar.make(mapActivity.getLayout(),
								MessageFormat.format(getString(R.string.gpx_saved_sucessfully), outFile.getName()),
								Snackbar.LENGTH_LONG)
								.setAction(R.string.shared_string_undo, new OnClickListener() {
									@Override
									public void onClick(View view) {
										MapActivity mapActivity = mapActivityRef.get();
										if (mapActivity != null) {
											OsmandApplication app = mapActivity.getMyApplication();
											FileUtils.removeGpxFile(app, outFile);
											if (backupFile != null) {
												FileUtils.renameGpxFile(app, backupFile, outFile);
												GPXFile gpx = GPXUtilities.loadGPXFile(outFile);
												setupGpxData(gpx);
												if (showOnMap) {
													showGpxOnMap(app, gpx, false);
												}
											} else {
												setupGpxData(null);
											}
											setMode(UNDO_MODE, true);
											MeasurementToolFragment.showInstance(mapActivity.getSupportFragmentManager(),
													editingCtx, modes);
										}
									}
								})
								.addCallback(new Snackbar.Callback() {
									@Override
									public void onDismissed(Snackbar transientBottomBar, int event) {
										if (event != DISMISS_EVENT_ACTION) {
											editingCtx.setChangesSaved();
										}
										super.onDismissed(transientBottomBar, event);
									}
								});
						snackbar.getView().<TextView>findViewById(com.google.android.material.R.id.snackbar_action)
								.setAllCaps(false);
						UiUtilities.setupSnackbar(snackbar, nightMode);
						snackbar.show();
						dismiss(mapActivity, false);
						break;
					case SHOW_IS_SAVED_FRAGMENT:
						editingCtx.setChangesSaved();
						SavedTrackBottomSheetDialogFragment.showInstance(mapActivity.getSupportFragmentManager(),
								outFile.getAbsolutePath());
						dismiss(mapActivity);
						break;
					case SHOW_TOAST:
						editingCtx.setChangesSaved();
						if (savedGpxFile != null && !savedGpxFile.showCurrentTrack) {
							Toast.makeText(mapActivity,
									MessageFormat.format(getString(R.string.gpx_saved_sucessfully), outFile.getAbsolutePath()),
									Toast.LENGTH_LONG).show();
						}
				}
			}
		} else {
			Toast.makeText(mapActivity, warning.getMessage(), Toast.LENGTH_LONG).show();
		}
	}

	protected static void showGpxOnMap(OsmandApplication app, GPXFile gpx, boolean isNewGpx) {
		SelectedGpxFile sf = app.getSelectedGpxHelper().selectGpxFile(gpx, true, false);
		if (sf != null && !isNewGpx) {
			sf.processPoints(app);
		}
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
			String distanceStr = OsmAndFormatter.getFormattedDistance((float) editingCtx.getRouteDistance(), requireMyApplication());
			distanceTv.setText(distanceStr + ",");
			pointsTv.setText((portrait ? pointsSt + ": " : "") + editingCtx.getPointsCount());
		}
		updateToolbar();
	}

	private void updateToolbar() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity == null) {
			return;
		}
		String fileName = getSuggestedFileName();
		boolean editMode = isInEditMode();
		String actionStr = getString(editMode ? R.string.edit_line : R.string.plan_route);
		if (!editMode && editingCtx.getPointsCount() > 1) {
			toolBarController.setTitle(fileName.replace('_', ' '));
			toolBarController.setDescription(actionStr);
		} else {
			toolBarController.setTitle(actionStr);
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
			updateMainIcon();
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
		if (isFollowTrackMode()) {
			MapActivity mapActivity = getMapActivity();
			if (mapActivity != null) {
				mapActivity.getMapLayers().getMapControlsLayer().showRouteInfoControlDialog();
				dismiss(mapActivity);
			}
		} else {
			showQuitDialog(hidePointsListFirst);
		}
	}

	private void showQuitDialog(boolean hidePointsListFirst) {
		final MapActivity mapActivity = getMapActivity();
		MeasurementToolLayer measurementLayer = getMeasurementLayer();
		if (mapActivity != null && measurementLayer != null) {
			if (additionalInfoExpanded && hidePointsListFirst) {
				collapseAdditionalInfoView();
				return;
			}
			if (!editingCtx.hasChanges()) {
				dismiss(mapActivity);
				return;
			}
			ExitBottomSheetDialogFragment.showInstance(mapActivity.getSupportFragmentManager(), this);
		}
	}

	private void dismiss(@NonNull MapActivity mapActivity) {
		dismiss(mapActivity, true);
	}

	private void dismiss(@NonNull MapActivity mapActivity, boolean clearContext) {
		try {
			if (clearContext) {
				editingCtx.clearSegments();
			}
			if (additionalInfoExpanded) {
				collapseAdditionalInfoView();
			}
			resetAppMode();
			hideSnapToRoadIcon();
			if (isInEditMode()) {
				GpxData gpxData = editingCtx.getGpxData();
				GPXFile gpx = gpxData != null ? gpxData.getGpxFile() : null;
				if (gpx != null) {
					Intent newIntent = new Intent(mapActivity, mapActivity.getMyApplication().getAppCustomization().getTrackActivity());
					newIntent.putExtra(TrackActivity.TRACK_FILE_NAME, gpx.path);
					newIntent.putExtra(TrackActivity.OPEN_TRACKS_LIST, true);
					newIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
					startActivity(newIntent);
				}
			}
			mapActivity.getSupportFragmentManager().beginTransaction().remove(this).commitAllowingStateLoss();
		} catch (Exception e) {
			// ignore
		}
	}

	public static boolean showInstance(FragmentManager fragmentManager, LatLon initialPoint) {
		MeasurementToolFragment fragment = new MeasurementToolFragment();
		fragment.setInitialPoint(initialPoint);
		fragment.setMode(PLAN_ROUTE_MODE, true);
		return showFragment(fragment, fragmentManager);
	}

	public static boolean showInstance(FragmentManager fragmentManager, String fileName) {
		MeasurementToolFragment fragment = new MeasurementToolFragment();
		fragment.setFileName(fileName);
		fragment.setMode(PLAN_ROUTE_MODE, true);
		return showFragment(fragment, fragmentManager);
	}

	public static boolean showInstance(FragmentManager fragmentManager, GPXFile gpxFile) {
		MeasurementToolFragment fragment = new MeasurementToolFragment();
		fragment.addNewGpxData(gpxFile);
		fragment.setMode(PLAN_ROUTE_MODE, true);
		return showFragment(fragment, fragmentManager);
	}

	public static boolean showInstance(FragmentManager fragmentManager, MeasurementEditingContext editingCtx,
	                                   boolean followTrackMode) {
		MeasurementToolFragment fragment = new MeasurementToolFragment();
		fragment.setEditingCtx(editingCtx);
		fragment.setMode(FOLLOW_TRACK_MODE, followTrackMode);
		return showFragment(fragment, fragmentManager);
	}

	public static boolean showInstance(FragmentManager fragmentManager, MeasurementEditingContext editingCtx) {
		MeasurementToolFragment fragment = new MeasurementToolFragment();
		fragment.setEditingCtx(editingCtx);
		return showFragment(fragment, fragmentManager);
	}

	public static boolean showInstance(FragmentManager fragmentManager) {
		MeasurementToolFragment fragment = new MeasurementToolFragment();
		fragment.setMode(PLAN_ROUTE_MODE, true);
		return showFragment(fragment, fragmentManager);
	}

	private static boolean showInstance(FragmentManager fragmentManager, MeasurementEditingContext editingCtx, int modes) {
		MeasurementToolFragment fragment = new MeasurementToolFragment();
		fragment.setEditingCtx(editingCtx);
		fragment.modes = modes;
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
				AndroidUiHelper.updateVisibility(shadow, false);
			}
		}

		private void setupDoneButton(TopToolbarView view) {
			TextView done = view.getSaveView();
			AndroidUiHelper.updateVisibility(done, isVisible());

			Context ctx = done.getContext();
			done.setAllCaps(false);
			ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) done.getLayoutParams();
			layoutParams.height = ctx.getResources().getDimensionPixelSize(R.dimen.measurement_tool_button_height);
			layoutParams.leftMargin = ctx.getResources().getDimensionPixelSize(R.dimen.context_menu_padding_margin_large);
			layoutParams.rightMargin = ctx.getResources().getDimensionPixelSize(R.dimen.context_menu_padding_margin_large);
			int paddingH = ctx.getResources().getDimensionPixelSize(R.dimen.context_menu_padding_margin_large);
			done.setPadding(paddingH, done.getPaddingTop(), paddingH, done.getPaddingBottom());
			AndroidUtils.setBackground(ctx, done, nightMode, R.drawable.purchase_dialog_outline_btn_bg_light,
					R.drawable.purchase_dialog_outline_btn_bg_dark);
		}

		@Override
		public int getStatusBarColor(Context context, boolean night) {
			return NO_COLOR;
		}
	}

	@Override
	public void onGpxApproximationDone(List<GpxRouteApproximation> gpxApproximations, List<List<WptPt>> pointsList, ApplicationMode mode) {
		MeasurementToolLayer measurementLayer = getMeasurementLayer();
		if (measurementLayer != null) {
			boolean approximationMode = editingCtx.isInApproximationMode();
			editingCtx.setInApproximationMode(true);
			ApplyGpxApproximationCommand command = new ApplyGpxApproximationCommand(measurementLayer, gpxApproximations, pointsList, mode);
			if (!approximationMode || !editingCtx.getCommandManager().update(command)) {
				editingCtx.getCommandManager().execute(command);
			}
			if (additionalInfoExpanded) {
				collapseAdditionalInfoView();
			}
			updateSnapToRoadControls();
		}
	}

	@Override
	public void onApplyGpxApproximation() {
		exitApproximationMode();
		doAddOrMovePointCommonStuff();
		updateSnapToRoadControls();
		if (isDirectionMode() || isFollowTrackMode()) {
			setMode(DIRECTION_MODE, false);
			startTrackNavigation();
		}
	}

	private void startTrackNavigation() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			if (editingCtx.hasRoute()) {
				String trackName = getSuggestedFileName();
				GPXFile gpx = editingCtx.exportGpx(trackName);
				if (gpx != null) {
					ApplicationMode appMode = editingCtx.getAppMode();
					dismiss(mapActivity);
					runNavigation(gpx, appMode);
				} else {
					Toast.makeText(mapActivity, getString(R.string.error_occurred_saving_gpx), Toast.LENGTH_SHORT).show();
				}
			} else {
				Toast.makeText(mapActivity, getString(R.string.error_occurred_saving_gpx), Toast.LENGTH_SHORT).show();
			}
		}
	}

	@Override
	public void onCancelGpxApproximation() {
		editingCtx.getCommandManager().undo();
		exitApproximationMode();
		setMode(DIRECTION_MODE, false);
		updateSnapToRoadControls();
		updateToolbar();
	}

	private void enterApproximationMode(MapActivity mapActivity) {
		MeasurementToolLayer layer = getMeasurementLayer();
		if (layer != null) {
			FragmentManager manager = mapActivity.getSupportFragmentManager();
			manager.beginTransaction()
					.hide(this).commit();
			layer.setTapsDisabled(true);
			SnapTrackWarningFragment.showInstance(mapActivity.getSupportFragmentManager(), this);
			AndroidUiHelper.setVisibility(mapActivity, View.GONE, R.id.map_ruler_container);
		}
	}

	private void exitApproximationMode() {
		editingCtx.setInApproximationMode(false);
		MeasurementToolLayer layer = getMeasurementLayer();
		MapActivity mapActivity = getMapActivity();
		if (layer != null && mapActivity != null) {
			FragmentManager manager = mapActivity.getSupportFragmentManager();
			manager.beginTransaction()
					.show(this).commit();
			layer.setTapsDisabled(false);
			AndroidUiHelper.setVisibility(mapActivity, View.VISIBLE, R.id.map_ruler_container);
		}
	}

	public boolean isNightModeForMapControls() {
		return nightMode;
	}

	public interface OnUpdateAdditionalInfoListener {
		void onUpdateAdditionalInfo();
	}
}