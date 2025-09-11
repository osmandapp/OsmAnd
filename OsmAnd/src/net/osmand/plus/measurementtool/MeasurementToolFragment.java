package net.osmand.plus.measurementtool;

import static android.util.TypedValue.COMPLEX_UNIT_SP;
import static net.osmand.IndexConstants.GPX_FILE_EXT;
import static net.osmand.IndexConstants.GPX_INDEX_DIR;
import static net.osmand.plus.backup.BackupHelper.SERVER_URL;
import static net.osmand.plus.measurementtool.MeasurementEditingContext.DEFAULT_APP_MODE;
import static net.osmand.plus.measurementtool.RouteBetweenPointsBottomSheetDialogFragment.RouteBetweenPointsDialogType.NEXT_ROUTE_CALCULATION;
import static net.osmand.plus.measurementtool.RouteBetweenPointsBottomSheetDialogFragment.RouteBetweenPointsDialogType.PREV_ROUTE_CALCULATION;
import static net.osmand.plus.measurementtool.RouteBetweenPointsBottomSheetDialogFragment.RouteBetweenPointsDialogType.WHOLE_ROUTE_CALCULATION;
import static net.osmand.plus.measurementtool.SaveAsNewTrackBottomSheetDialogFragment.SaveAsNewTrackFragmentListener;
import static net.osmand.plus.measurementtool.SelectFileBottomSheet.SelectFileListener;
import static net.osmand.plus.measurementtool.command.ClearPointsCommand.ClearCommandMode;
import static net.osmand.plus.measurementtool.command.ClearPointsCommand.ClearCommandMode.AFTER;
import static net.osmand.plus.measurementtool.command.ClearPointsCommand.ClearCommandMode.ALL;
import static net.osmand.plus.measurementtool.command.ClearPointsCommand.ClearCommandMode.BEFORE;
import static net.osmand.plus.routing.TransportRoutingHelper.PUBLIC_TRANSPORT_KEY;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.MarginLayoutParams;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.*;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.DrawableRes;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;
import androidx.core.widget.TextViewCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.github.ksoichiro.android.observablescrollview.ScrollUtils;
import com.google.android.material.snackbar.Snackbar;

import net.osmand.CallbackWithObject;
import net.osmand.data.LatLon;
import net.osmand.data.QuadRect;
import net.osmand.plus.OsmAndTaskManager;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.MapActivityActions;
import net.osmand.plus.base.BaseFullScreenFragment;
import net.osmand.plus.base.ContextMenuFragment.MenuState;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.helpers.MapDisplayPositionManager;
import net.osmand.plus.helpers.MapDisplayPositionManager.IMapDisplayPositionProvider;
import net.osmand.plus.helpers.MapFragmentsHelper;
import net.osmand.plus.helpers.TargetPointsHelper;
import net.osmand.plus.measurementtool.MeasurementEditingContext.CalculationMode;
import net.osmand.plus.measurementtool.OptionsBottomSheetDialogFragment.OptionsFragmentListener;
import net.osmand.plus.measurementtool.RouteBetweenPointsBottomSheetDialogFragment.RouteBetweenPointsDialogMode;
import net.osmand.plus.measurementtool.RouteBetweenPointsBottomSheetDialogFragment.RouteBetweenPointsDialogType;
import net.osmand.plus.measurementtool.RouteBetweenPointsBottomSheetDialogFragment.RouteBetweenPointsFragmentListener;
import net.osmand.plus.measurementtool.SaveGpxRouteAsyncTask.SaveGpxRouteListener;
import net.osmand.plus.measurementtool.SelectedPointBottomSheetDialogFragment.SelectedPointFragmentListener;
import net.osmand.plus.measurementtool.adapter.MeasurementToolAdapter.MeasurementAdapterListener;
import net.osmand.plus.measurementtool.command.*;
import net.osmand.plus.measurementtool.command.ChangeRouteModeCommand.ChangeRouteType;
import net.osmand.plus.routepreparationmenu.RouteOptionsBottomSheet;
import net.osmand.plus.routepreparationmenu.RouteOptionsBottomSheet.DialogMode;
import net.osmand.plus.routepreparationmenu.cards.MapBaseCard;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.enums.MapPosition;
import net.osmand.plus.shared.SharedUtil;
import net.osmand.plus.track.GpxSelectionParams;
import net.osmand.plus.track.SelectTrackTabsFragment;
import net.osmand.plus.track.fragments.GpsFilterFragment;
import net.osmand.plus.track.fragments.GpsFilterFragment.GpsFilterFragmentLister;
import net.osmand.plus.track.fragments.TrackAltitudeBottomSheet;
import net.osmand.plus.track.fragments.TrackAltitudeBottomSheet.CalculateAltitudeListener;
import net.osmand.plus.track.fragments.TrackMenuFragment;
import net.osmand.plus.track.helpers.GpxFileLoaderTask;
import net.osmand.plus.track.helpers.SelectedGpxFile;
import net.osmand.plus.utils.*;
import net.osmand.plus.utils.AndroidNetworkUtils.NetworkResult;
import net.osmand.plus.utils.AndroidNetworkUtils.OnFileUploadCallback;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.layers.MapControlsLayer.MapControlsThemeProvider;
import net.osmand.plus.widgets.dialogbutton.DialogButtonType;
import net.osmand.plus.widgets.multistatetoggle.IconToggleButton;
import net.osmand.plus.widgets.multistatetoggle.IconToggleButton.IconRadioItem;
import net.osmand.plus.widgets.multistatetoggle.MultiStateToggleButton;
import net.osmand.plus.widgets.multistatetoggle.RadioItem;
import net.osmand.plus.widgets.multistatetoggle.RadioItem.OnRadioItemClickListener;
import net.osmand.plus.widgets.multistatetoggle.TextToggleButton;
import net.osmand.plus.widgets.multistatetoggle.TextToggleButton.TextRadioItem;
import net.osmand.router.GpxRouteApproximation;
import net.osmand.shared.gpx.GpxFile;
import net.osmand.shared.gpx.GpxTrackAnalysis;
import net.osmand.shared.gpx.GpxUtilities;
import net.osmand.shared.gpx.primitives.WptPt;
import net.osmand.util.Algorithms;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.ref.WeakReference;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MeasurementToolFragment extends BaseFullScreenFragment implements RouteBetweenPointsFragmentListener,
		OptionsFragmentListener, GpxApproximationFragmentListener, SelectedPointFragmentListener,
		SaveAsNewTrackFragmentListener, MapControlsThemeProvider, GpsFilterFragmentLister,
		OnFileUploadCallback, CalculateAltitudeListener, IMapDisplayPositionProvider, CallbackWithObject<String> {

	public static final String TAG = MeasurementToolFragment.class.getSimpleName();
	public static final String TAPS_DISABLED_KEY = "taps_disabled_key";

	private static final String MODES_KEY = "modes_key";
	private static final String INITIAL_POINT_KEY = "initial_point_key";
	private static final String SHOW_SNAP_WARNING_KEY = "show_snap_warning_key";
	private static final String PROCESS_SRTM_URL = SERVER_URL + "/gpx/process-srtm";

	public static final int PLAN_ROUTE_MODE = 0x1;
	public static final int DIRECTION_MODE = 0x2;
	public static final int FOLLOW_TRACK_MODE = 0x4;
	public static final int UNDO_MODE = 0x8;
	public static final int ATTACH_ROADS_MODE = 0x10;
	public static final int CALCULATE_SRTM_MODE = 0x20;
	public static final int CALCULATE_HEIGHTMAP_MODE = 0x40;

	@Retention(RetentionPolicy.SOURCE)
	@IntDef({PLAN_ROUTE_MODE, DIRECTION_MODE, FOLLOW_TRACK_MODE, UNDO_MODE, ATTACH_ROADS_MODE, CALCULATE_SRTM_MODE, CALCULATE_HEIGHTMAP_MODE})
	public @interface MeasurementToolMode {
	}

	private MapDisplayPositionManager mapDisplayPositionManager;

	private String previousToolBarTitle = "";
	private MeasurementToolBarController toolBarController;
	private TextView distanceTv;
	private TextView pointsTv;
	private TextView distanceToCenterTv;
	private String pointsSt;
	private ViewGroup cardsContainer;
	private MapBaseCard visibleCard;
	private PointsCard pointsCard;
	private ChartsCard chartsCard;
	private MultiStateToggleButton infoTypeBtn;
	private RadioItem pointsBtn;
	private RadioItem graphBtn;
	private View mainView;
	private View bottomMapControls;
	private View topMapControls;
	private ImageView upDownBtn;
	private ImageView undoBtn;
	private ImageView redoBtn;
	private ImageView mainIcon;
	private OnBackPressedCallback onBackPressedCallback;
	private OnGlobalLayoutListener widgetsLayoutListener;

	private String filePath;
	private boolean showSnapWarning;
	private boolean adjustMapPosition = true;

	private InfoType currentInfoType;

	private boolean progressBarVisible;
	private boolean infoExpanded;

	private int modes;

	private boolean portrait;

	private MeasurementEditingContext editingCtx;
	private GraphDetailsMenu detailsMenu;

	private LatLon initialPoint;
	private UploadFileTask calculateSrtmTask;
	private HeightsResolverTask calculateHeightmapTask;

	enum FinalSaveAction {
		SHOW_SNACK_BAR_AND_CLOSE,
		SHOW_TOAST,
		SHOW_IS_SAVED_FRAGMENT
	}

	private enum InfoType {
		POINTS,
		GRAPH
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

	private boolean isAttachRoadsMode() {
		return (this.modes & ATTACH_ROADS_MODE) == ATTACH_ROADS_MODE;
	}

	protected boolean isCalculateSrtmMode() {
		return (this.modes & CALCULATE_SRTM_MODE) == CALCULATE_SRTM_MODE;
	}

	protected boolean isCalculateHeightmapMode() {
		return (this.modes & CALCULATE_HEIGHTMAP_MODE) == CALCULATE_HEIGHTMAP_MODE;
	}

	private boolean isUndoMode() {
		return (this.modes & UNDO_MODE) == UNDO_MODE;
	}

	@Override
	protected boolean isUsedOnMap() {
		return true;
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mapDisplayPositionManager = app.getMapViewTrackingUtilities().getMapDisplayPositionManager();
		if (editingCtx == null) {
			editingCtx = new MeasurementEditingContext(app);
		}
		onBackPressedCallback = new OnBackPressedCallback(true) {
			public void handleOnBackPressed() {
				quit(true);
			}
		};
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
	                         @Nullable Bundle savedInstanceState) {
		MapActivity mapActivity = (MapActivity) getActivity();
		if (mapActivity == null) {
			return null;
		}

		updateNightMode();

		MeasurementToolLayer measurementLayer = mapActivity.getMapLayers().getMeasurementToolLayer();

		app.setMeasurementEditingContext(editingCtx);
		editingCtx.setProgressListener(new SnapToRoadProgressListener() {
			@Override
			public void showProgressBar() {
				MeasurementToolFragment.this.showProgressBar();
				updateInfoView();
				updateInfoViewAppearance();
			}

			@Override
			public void updateProgress(int progress) {
				((ProgressBar) mainView.findViewById(R.id.snap_to_road_progress_bar)).setProgress(progress);
			}

			@Override
			public void hideProgressBar() {
				mainView.findViewById(R.id.snap_to_road_progress_bar).setVisibility(View.INVISIBLE);
				progressBarVisible = false;
				updateInfoView();
				updateInfoViewAppearance();
				recalculateHeightmapIfNeeded();
			}

			@Override
			public void refresh() {
				measurementLayer.refreshMap();
				updateDistancePointsText();
			}
		});
		editingCtx.setupRouteSettingsListener();

		measurementLayer.setEditingCtx(editingCtx);

		portrait = AndroidUiHelper.isOrientationPortrait(mapActivity);
		int btnWidth = getResources().getDimensionPixelOffset(R.dimen.gpx_group_button_width);

		pointsSt = getString(R.string.shared_string_gpx_points).toLowerCase();

		View view = inflate(R.layout.fragment_measurement_tool, container, false);

		mainView = view.findViewById(R.id.main_view);
		detailsMenu = new GraphDetailsMenu(mainView);
		LinearLayout infoButtonsContainer = mainView.findViewById(R.id.custom_radio_buttons);
		if (portrait) {
			cardsContainer = mainView.findViewById(R.id.cards_container);
			infoTypeBtn = new TextToggleButton(app, infoButtonsContainer, nightMode);

			String pointsBtnTitle = getString(R.string.shared_string_gpx_points);
			pointsBtn = new TextRadioItem(pointsBtnTitle);

			String graphBtnTitle = getString(R.string.shared_string_graph);
			graphBtn = new TextRadioItem(graphBtnTitle);

		} else {
			cardsContainer = mapActivity.findViewById(R.id.left_side_menu);
			bottomMapControls = mapActivity.findViewById(R.id.bottom_controls_container);
			topMapControls = mapActivity.findViewById(R.id.top_controls_container);

			infoTypeBtn = new IconToggleButton(app, infoButtonsContainer, nightMode);
			pointsBtn = new IconRadioItem(R.drawable.ic_action_plan_route_point_colored).setUseDefaultColor();
			graphBtn = new IconRadioItem(R.drawable.ic_action_analyze_intervals);

			ScrollUtils.addOnGlobalLayoutListener(mainView, this::updateInfoViewAppearance);
		}
		pointsBtn.setOnClickListener(getInfoTypeBtnListener(InfoType.POINTS));
		graphBtn.setOnClickListener(getInfoTypeBtnListener(InfoType.GRAPH));
		infoTypeBtn.setItems(pointsBtn, graphBtn);

		pointsCard = new PointsCard(mapActivity, this);
		chartsCard = new ChartsCard(mapActivity, detailsMenu, this);

		if (progressBarVisible) {
			showProgressBar();
		}

		distanceTv = mainView.findViewById(R.id.measurement_distance_text_view);
		pointsTv = mainView.findViewById(R.id.measurement_points_text_view);
		distanceToCenterTv = mainView.findViewById(R.id.distance_to_center_text_view);

		mainIcon = mainView.findViewById(R.id.main_icon);
		upDownBtn = mainView.findViewById(R.id.up_down_button);
		updateUpDownBtn();

		mainView.findViewById(R.id.cancel_move_point_button).setOnClickListener(v -> cancelMovePointMode());

		mainView.findViewById(R.id.cancel_point_before_after_button).setOnClickListener(v -> cancelAddPointBeforeOrAfterMode());

		View upDownRow = mainView.findViewById(R.id.up_down_row);
		upDownRow.setOnClickListener(v -> {
			if (infoExpanded) {
				collapseInfoView();
			} else if (setInfoType(InfoType.POINTS)) {
				infoTypeBtn.setSelectedItem(pointsBtn);
			}
		});

		View applyMovePointButton = mainView.findViewById(R.id.apply_move_point_button);
		UiUtilities.setupDialogButton(nightMode, applyMovePointButton,
				DialogButtonType.PRIMARY, R.string.shared_string_apply);
		applyMovePointButton.setMinimumWidth(btnWidth);
		applyMovePointButton.setOnClickListener(v -> applyMovePointMode());

		View applyPointBeforeAfterButton = mainView.findViewById(R.id.apply_point_before_after_point_button);
		UiUtilities.setupDialogButton(nightMode, applyPointBeforeAfterButton,
				DialogButtonType.PRIMARY, R.string.shared_string_apply);
		applyPointBeforeAfterButton.setMinimumWidth(btnWidth);
		applyPointBeforeAfterButton.setOnClickListener(v -> applyAddPointBeforeAfterMode());

		View addPointBeforeAfterButton = mainView.findViewById(R.id.add_point_before_after_button);
		UiUtilities.setupDialogButton(nightMode, addPointBeforeAfterButton,
				DialogButtonType.PRIMARY, R.string.shared_string_add);
		addPointBeforeAfterButton.setMinimumWidth(btnWidth);
		addPointBeforeAfterButton.setOnClickListener(v -> addPointBeforeAfter());

		mainView.findViewById(R.id.options_button).setOnClickListener(v -> {
			FragmentActivity activity = getActivity();
			if (activity != null) {
				FragmentManager manager = activity.getSupportFragmentManager();
				OptionsBottomSheetDialogFragment.showInstance(manager, this);
			}
		});

		undoBtn = mainView.findViewById(R.id.undo_point_button);
		redoBtn = mainView.findViewById(R.id.redo_point_button);

		Drawable undoDrawable = getActiveIcon(R.drawable.ic_action_undo_dark);
		undoBtn.setImageDrawable(AndroidUtils.getDrawableForDirection(mapActivity, undoDrawable));
		undoBtn.setOnClickListener(v -> {
			editingCtx.getCommandManager().undo();
			updateUndoRedoButton(editingCtx.getCommandManager().canUndo(), undoBtn);
			updateUndoRedoButton(true, redoBtn);
			updateUndoRedoCommonStuff();
		});

		Drawable redoDrawable = getActiveIcon(R.drawable.ic_action_redo_dark);
		redoBtn.setImageDrawable(AndroidUtils.getDrawableForDirection(mapActivity, redoDrawable));
		redoBtn.setOnClickListener(v -> {
			editingCtx.getCommandManager().redo();
			updateUndoRedoButton(editingCtx.getCommandManager().canRedo(), redoBtn);
			updateUndoRedoButton(true, undoBtn);
			updateUndoRedoCommonStuff();
		});

		View addPointButton = mainView.findViewById(R.id.add_point_button);
		UiUtilities.setupDialogButton(nightMode, addPointButton,
				DialogButtonType.PRIMARY, R.string.shared_string_add);
		addPointButton.setMinimumWidth(btnWidth);
		addPointButton.setOnClickListener(v -> addCenterPoint());

		measurementLayer.setOnSingleTapListener(new MeasurementToolLayer.OnSingleTapListener() {
			@Override
			public void onAddPoint() {
				addPoint();
			}

			@Override
			public void onSelectPoint(int selectedPointPos) {
				if (selectedPointPos != -1) {
					callMapActivity(mapActivity -> openSelectedPointMenu(mapActivity));
				}
			}

			@Override
			public void onSelectProfileIcon(int startPointPos) {
				if (startPointPos != -1) {
					onChangeRouteTypeAfter();
				}
			}
		});

		measurementLayer.setOnMeasureDistanceToCenterListener((distance, bearing) -> {
			String distStr = OsmAndFormatter.getFormattedDistance(distance, app);
			String azimuthStr = OsmAndFormatter.getFormattedAzimuth(bearing, app);
			distanceToCenterTv.setText(String.format("%1$s • %2$s", distStr, azimuthStr));
			TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(distanceToCenterTv,
					14, 18, 2, COMPLEX_UNIT_SP);
		});

		measurementLayer.setOnEnterMovePointModeListener(() -> {
			collapseInfoViewIfExpanded();
			switchMovePointMode(true);
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

		toolBarController = new MeasurementToolBarController(this);
		if (editingCtx.getSelectedPointPosition() != -1) {
			int navigationIconResId = AndroidUtils.getNavigationIconResId(mapActivity);
			toolBarController.setBackBtnIconIds(navigationIconResId, navigationIconResId);
		} else {
			toolBarController.setBackBtnIconIds(R.drawable.ic_action_remove_dark, R.drawable.ic_action_remove_dark);
		}
		toolBarController.setOnBackButtonClickListener(v -> {
			callMapActivity(activity -> {
				MapFragmentsHelper fragmentsHelper = activity.getFragmentsHelper();
				GpxApproximationFragment gpxApproximationFragment = fragmentsHelper.getGpxApproximationFragment();
				SnapTrackWarningFragment snapTrackWarningFragment = fragmentsHelper.getSnapTrackWarningBottomSheet();
				if (gpxApproximationFragment != null) {
					gpxApproximationFragment.dismissImmediate();
				} else if (snapTrackWarningFragment != null) {
					snapTrackWarningFragment.dismissImmediate();
				} else {
					quit(false);
				}
			});
		});
		toolBarController.setOnSaveViewClickListener(v -> {
			if (isFollowTrackMode()) {
				startTrackNavigation();
			} else if (editingCtx.isNewData() || editingCtx.hasChanges()
					|| (isCalculateSrtmMode() || isCalculateHeightmapMode()) && editingCtx.hasElevationData()) {
				saveChanges(FinalSaveAction.SHOW_SNACK_BAR_AND_CLOSE, false);
			} else {
				callMapActivity(activity -> dismiss(activity, false));
			}
		});

		ImageButton snapToRoadBtn = mapActivity.findViewById(R.id.snap_to_road_image_button);
		snapToRoadBtn.setBackgroundResource(nightMode ? R.drawable.btn_circle_night : R.drawable.btn_circle);
		snapToRoadBtn.setOnClickListener(v -> startSnapToRoad(false));
		snapToRoadBtn.setVisibility(View.VISIBLE);
		LinearLayout profileWithConfig = mapActivity.findViewById(R.id.profile_with_config_btn);

		View background = profileWithConfig.findViewById(R.id.btn_background);
		AndroidUtils.setBackground(background, AppCompatResources.getDrawable(view.getContext(),
				AndroidUtils.resolveAttribute(view.getContext(), R.attr.bg_round_btn)));
		View divider = profileWithConfig.findViewById(R.id.divider);
		divider.setBackgroundResource(AndroidUtils.resolveAttribute(view.getContext(), R.attr.divider_color));
		ImageButton profileBtn = profileWithConfig.findViewById(R.id.profile);
		profileBtn.setBackgroundResource(nightMode ? R.drawable.btn_circle_night_no_shadow : R.drawable.btn_circle_no_shadow);
		profileBtn.setOnClickListener(v -> startSnapToRoad(false));
		ImageButton configBtn = profileWithConfig.findViewById(R.id.profile_config);
		configBtn.setBackgroundResource(nightMode ? R.drawable.btn_circle_night_no_shadow : R.drawable.btn_circle_no_shadow);
		configBtn.setImageDrawable(getContentIcon(R.drawable.ic_action_settings));
		configBtn.setOnClickListener(v ->
				callMapActivity(activity -> {
					String appModeKey = editingCtx.getAppMode().getStringKey();
					RouteOptionsBottomSheet.showInstance(activity, this, DialogMode.PLAN_ROUTE, appModeKey);
				})
		);
		GpxData gpxData = editingCtx.getGpxData();
		initMeasurementMode(gpxData, savedInstanceState == null);
		if (savedInstanceState == null) {
			if (filePath != null) {
				getGpxFile(filePath, gpxFile -> {
					addNewGpxData(gpxFile);
					return true;
				});
			} else if (gpxData != null && isCalculateSrtmMode()) {
				if (!hasAltitude()) {
					calculateSrtmTrack();
				}
				setInfoType(InfoType.GRAPH);
				infoTypeBtn.setSelectedItem(graphBtn);
			} else if (gpxData != null && isCalculateHeightmapMode()) {
				if (!hasAltitude()) {
					calculateHeightmapTrack();
				}
				setInfoType(InfoType.GRAPH);
				infoTypeBtn.setSelectedItem(graphBtn);
			} else if ((isFollowTrackMode() || isAttachRoadsMode()) && isShowSnapWarning()) {
				enterApproximationMode(mapActivity);
			} else if (gpxData != null) {
				adjustMapPosition(gpxData);
			}
		} else {
			measurementLayer.setTapsDisabled(savedInstanceState.getBoolean(TAPS_DISABLED_KEY));
			if (initialPoint == null && savedInstanceState.containsKey(INITIAL_POINT_KEY)) {
				initialPoint = AndroidUtils.getSerializable(savedInstanceState, INITIAL_POINT_KEY, LatLon.class);
			}
			modes = savedInstanceState.getInt(MODES_KEY);
			showSnapWarning = savedInstanceState.getBoolean(SHOW_SNAP_WARNING_KEY);
		}

		return view;
	}

	public OnBackPressedCallback getOnBackPressedCallback() {
		return onBackPressedCallback;
	}

	private OnRadioItemClickListener getInfoTypeBtnListener(@NonNull InfoType type) {
		return (radioItem, view) -> {
			if (isCurrentInfoType(type)) {
				collapseInfoView();
				return false;
			}
			return setInfoType(type);
		};
	}

	private boolean setInfoType(@NonNull InfoType type) {
		if ((!infoExpanded || !isCurrentInfoType(type)) && app != null) {
			if (editingCtx.getPointsCount() > 0 && editingCtx.getSelectedPointPosition() == -1) {
				expandInfoView();
				currentInfoType = type;
				if (InfoType.POINTS == type) {
					visibleCard = pointsCard;
				} else if (InfoType.GRAPH == type) {
					visibleCard = chartsCard;
				}
				cardsContainer.removeAllViews();
				View cardView = visibleCard.getView() != null ? visibleCard.getView() : visibleCard.build(app);
				cardsContainer.addView(cardView);
				return true;
			} else {
				collapseInfoView();
			}
		}
		return false;
	}

	private void expandInfoView() {
		infoExpanded = true;
		if (!portrait) {
			shiftMapControls(false);
		}
		updateMapDisplayPosition();
		cardsContainer.setVisibility(View.VISIBLE);
		updateUpDownBtn();
	}

	private void collapseInfoViewIfExpanded() {
		if (infoExpanded) {
			collapseInfoView();
		}
	}

	private void collapseInfoView() {
		infoExpanded = false;
		currentInfoType = null;
		cardsContainer.setVisibility(View.GONE);
		if (!portrait) {
			shiftMapControls(true);
		}
		infoTypeBtn.setSelectedItem(null);
		updateMapDisplayPosition();
		updateUpDownBtn();
	}

	private void collapseInfoIfNotEnoughPoints() {
		int pointsCount = editingCtx.getPointsCount();
		if (isCurrentInfoType(InfoType.GRAPH) && pointsCount < 2) {
			collapseInfoView();
		} else if (pointsCount < 1) {
			disable(upDownBtn);
			collapseInfoViewIfExpanded();
		}
	}

	private void updateInfoView() {
		updateInfoView(pointsCard);
		updateInfoView(chartsCard);
	}

	private void updateInfoView(OnUpdateInfoListener listener) {
		if (listener != null) {
			listener.onUpdateInfo();
		}
	}

	private void updateInfoViewAppearance() {
		if (portrait) return;

		View toolsPanel = mainView.findViewById(R.id.measure_mode_controls);
		View snapToRoadProgress = mainView.findViewById(R.id.snap_to_road_progress_bar);

		int infoViewWidth = mainView.getWidth() - toolsPanel.getWidth();
		int bottomMargin = toolsPanel.getHeight();
		if (progressBarVisible) {
			bottomMargin += snapToRoadProgress.getHeight();
		}

		ViewGroup.MarginLayoutParams params = null;
		if (mainView.getParent() instanceof FrameLayout) {
			params = new FrameLayout.LayoutParams(infoViewWidth, -1);
		} else if (mainView.getParent() instanceof LinearLayout) {
			params = new LinearLayout.LayoutParams(infoViewWidth, -1);
		}
		if (params != null) {
			AndroidUtils.setMargins(params, 0, 0, 0, bottomMargin);
			cardsContainer.setLayoutParams(params);
		}
	}

	private void shiftMapControls(boolean toInitialPosition) {
		boolean isLayoutRtl = AndroidUtils.isLayoutRtl(getContext());
		int shiftPosition = toInitialPosition ? 0 : cardsContainer.getWidth();
		int start = isLayoutRtl ? 0 : shiftPosition;
		int end = isLayoutRtl ? shiftPosition : 0;
		AndroidUtils.setMargins((MarginLayoutParams) bottomMapControls.getLayoutParams(), start, 0, end, 0);
		AndroidUtils.setMargins((MarginLayoutParams) topMapControls.getLayoutParams(), start, 0, end, 0);
	}

	public boolean isInEditMode() {
		return !isPlanRouteMode() && !editingCtx.isNewData() && !isDirectionMode()
				&& !isFollowTrackMode() && !isAttachRoadsMode()
				&& !isCalculateSrtmMode() && !isCalculateHeightmapMode();
	}

	public boolean isShowSnapWarning() {
		return this.showSnapWarning;
	}

	public void setShowSnapWarning(boolean showSnapWarning) {
		this.showSnapWarning = showSnapWarning;
	}

	public MeasurementEditingContext getEditingCtx() {
		return editingCtx;
	}

	private void updateUndoRedoCommonStuff() {
		collapseInfoIfNotEnoughPoints();
		if (editingCtx.getPointsCount() > 0) {
			enable(upDownBtn);
		}
		updateInfoView();
		updateDistancePointsText();
		updateSnapToRoadControls();
	}

	private void initMeasurementMode(GpxData gpxData, boolean addPoints) {
		callMapActivity(mapActivity -> {
			editingCtx.getCommandManager().setMeasurementLayer(mapActivity.getMapLayers().getMeasurementToolLayer());
			enterMeasurementMode();
			if (gpxData != null && addPoints) {
				if (!isUndoMode()) {
					List<WptPt> points = gpxData.getGpxFile().getRoutePoints();
					if (!points.isEmpty()) {
						ApplicationMode snapToRoadAppMode = ApplicationMode.valueOfStringKey(points.get(points.size() - 1).getProfileType(), null);
						if (snapToRoadAppMode != null) {
							setupAppMode(snapToRoadAppMode);
						}
					}
				}
				collectPoints();
			}
			updateSnapToRoadControls();
			setMode(UNDO_MODE, false);
		});
	}

	@Override
	public void onResume() {
		super.onResume();
		mapDisplayPositionManager.registerMapPositionProvider(this);
		callMapActivity(mapActivity -> {
			if (mapActivity.getMapLayers().hasMapActivity()) {
				mapActivity.getOnBackPressedDispatcher().addCallback(this, onBackPressedCallback);
				onBackPressedCallback.setEnabled(true);
				detailsMenu.setMapActivity(mapActivity);
				mapActivity.getMapLayers().getMapControlsLayer().addThemeInfoProviderTag(TAG);
				mapActivity.getMapLayers().getMapControlsLayer().showMapControlsIfHidden();
				updateMapDisplayPosition();
				addInitialPoint();
				updateToolbar();
			}
		});
	}

	@Override
	public void onPause() {
		super.onPause();
		mapDisplayPositionManager.unregisterMapPositionProvider(this);
		callMapActivity(mapActivity -> {
			mapActivity.getMapLayers().getMapControlsLayer().removeThemeInfoProviderTag(TAG);
		});
		detailsMenu.onDismiss();
		detailsMenu.setMapActivity(null);
		updateMapDisplayPosition();
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		cancelModes();
		exitMeasurementMode();
		collapseInfoViewIfExpanded();

		MeasurementToolLayer layer = getMeasurementLayer();
		layer.setOnSingleTapListener(null);
		layer.setOnEnterMovePointModeListener(null);
	}

	@Override
	public int getStatusBarColorId() {
		return R.color.status_bar_transparent_gradient;
	}

	@NonNull
	private MeasurementToolLayer getMeasurementLayer() {
		return app.getOsmandMap().getMapLayers().getMeasurementToolLayer();
	}

	private Drawable getActiveIcon(@DrawableRes int id) {
		return getIcon(id, nightMode ? R.color.osmand_orange : R.color.color_myloc_distance);
	}

	private void showProgressBar() {
		ProgressBar progressBar = mainView.findViewById(R.id.snap_to_road_progress_bar);
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
		callMapActivity(mapActivity -> {
			if (rememberPreviousTitle) {
				previousToolBarTitle = toolBarController.getTitle();
			}
			toolBarController.setTitle(getString(R.string.route_between_points));
			mapActivity.refreshMap();

			if (editingCtx.shouldCheckApproximation() && editingCtx.isApproximationNeeded() && editingCtx.hasTimestamps()) {
				enterApproximationMode(mapActivity);
			} else {
				RouteBetweenPointsBottomSheetDialogFragment.showInstance(mapActivity.getSupportFragmentManager(),
						this, WHOLE_ROUTE_CALCULATION,
						editingCtx.getLastCalculationMode() == CalculationMode.NEXT_SEGMENT
								? RouteBetweenPointsDialogMode.SINGLE
								: RouteBetweenPointsDialogMode.ALL,
						editingCtx.getAppMode());
			}
		});
	}

	private void calculateSrtmTrack() {
		if (isCalculateSrtmMode() && calculateSrtmTask == null) {
			try {
				GpxFile gpxFile = generateGpxFile();
				InputStream inputStream = new ByteArrayInputStream(GpxUtilities.INSTANCE.asString(gpxFile).getBytes("UTF-8"));
				calculateSrtmTask = AndroidNetworkUtils.uploadFileAsync(PROCESS_SRTM_URL, inputStream,
						getSuggestedFileName(), false, Collections.emptyMap(), null, this);
			} catch (IOException e) {
				app.showToastMessage(e.getMessage());
			}
		}
	}

	public boolean isCalculatingSrtmData() {
		return calculateSrtmTask != null && calculateSrtmTask.getStatus() == Status.RUNNING;
	}

	public void stopUploadFileTask() {
		if (isCalculatingSrtmData()) {
			calculateSrtmTask.cancel(false);
		}
		quit(false);
	}

	private void calculateHeightmapTrack() {
		if (isCalculateHeightmapMode() && calculateHeightmapTask == null) {
			GpxFile gpxFile = generateGpxFile();
			calculateHeightmapTask = new HeightsResolverTask(gpxFile, gpx -> {
				calculateHeightmapTask = null;

				if (gpx == null) {
					app.showToastMessage(R.string.error_calculate);
				} else {
					List<WptPt> sourcePoints = gpxFile.getAllSegmentsPoints();
					List<WptPt> targetPoints = editingCtx.getAllBeforePoints();
					if (sourcePoints.size() == targetPoints.size()) {
						for (int i = 0; i < sourcePoints.size(); i++) {
							targetPoints.get(i).setEle(sourcePoints.get(i).getEle());
						}
					}
				}

				updateInfoView();
			});
			OsmAndTaskManager.executeTask(calculateHeightmapTask);
		}
	}

	public boolean isCalculatingHeightmapData() {
		return calculateHeightmapTask != null && calculateHeightmapTask.getStatus() == Status.RUNNING;
	}

	public void stopCalculatingHeightMapTask(boolean quit) {
		if (isCalculatingHeightmapData()) {
			calculateHeightmapTask.cancel(false);
			calculateHeightmapTask = null;
		}
		if (quit) {
			quit(false);
		}
	}

	public void saveChanges(FinalSaveAction finalSaveAction, boolean showDialog) {
		callMapActivity(mapActivity -> {
			if (editingCtx.getPointsCount() > 0) {
				if (editingCtx.isNewData()) {
					if (showDialog) {
						openSaveAsNewTrackMenu(mapActivity);
					} else {
						saveNewGpx("", getSuggestedFileName(), true, false, finalSaveAction);
					}
				} else {
					addToGpx(finalSaveAction);
				}
			} else {
				app.showShortToastMessage(R.string.none_point_error);
			}
		});
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == SnapTrackWarningFragment.REQUEST_CODE) {
			onSnapTrackWarningResult(resultCode);
		} else if (requestCode == ExitBottomSheetDialogFragment.REQUEST_CODE) {
			onExitDialogResult(resultCode);
		}
	}

	private void onSnapTrackWarningResult(int resultCode) {
		if (resultCode == SnapTrackWarningFragment.CANCEL_RESULT_CODE) {
			onCancelSnapTrackWarning();
		} else if (resultCode == SnapTrackWarningFragment.CONTINUE_RESULT_CODE) {
			ApplicationMode mode = editingCtx.getAppMode();
			if (mode == ApplicationMode.DEFAULT || PUBLIC_TRANSPORT_KEY.equals(mode.getRoutingProfile())) {
				mode = null;
			}
			List<List<WptPt>> pointsSegments = editingCtx.getSegmentsPoints(true, true);
			if (Algorithms.isEmpty(pointsSegments)) {
				onCancelSnapTrackWarning();
			} else {
				GpxApproximationParams params = new GpxApproximationParams();
				params.setTrackPoints(pointsSegments);
				params.setAppMode(mode);

				callMapActivity(mapActivity -> {
					FragmentManager fragmentManager = mapActivity.getSupportFragmentManager();
					GpxApproximationFragment.showInstance(app, fragmentManager, this, params);
				});
			}
		} else if (resultCode == SnapTrackWarningFragment.CONNECT_STRAIGHT_LINE_RESULT_CODE) {
			MeasurementToolLayer measurementLayer = getMeasurementLayer();
			editingCtx.getCommandManager().execute(new DisableApproximationCheckCommand(measurementLayer));
			updateUndoRedoButton(false, redoBtn);
			updateUndoRedoButton(true, undoBtn);
			updateSnapToRoadControls();
		}
	}

	private void onCancelSnapTrackWarning() {
		toolBarController.setSaveViewVisible(true);
		setMode(DIRECTION_MODE, false);
		exitApproximationMode();
		updateToolbar();
	}

	private void onExitDialogResult(int resultCode) {
		callMapActivity(mapActivity -> {
			if (resultCode == ExitBottomSheetDialogFragment.EXIT_RESULT_CODE) {
				dismiss(mapActivity);
			} else if (resultCode == ExitBottomSheetDialogFragment.SAVE_RESULT_CODE) {
				openSaveAsNewTrackMenu(mapActivity);
			}
		});
	}

	@Override
	public void snapToRoadOnCLick() {
		startSnapToRoad(true);
	}

	@Override
	public void addNewSegmentOnClick() {
		onSplitPointsAfter();
	}

	@Override
	public void directionsOnClick() {
		callMapActivity(mapActivity -> {
			MapActivityActions mapActions = mapActivity.getMapActions();
			TargetPointsHelper targetPointsHelper = app.getTargetPointsHelper();
			ApplicationMode appMode = editingCtx.getAppMode();
			if (appMode == ApplicationMode.DEFAULT) {
				appMode = null;
			}
			List<WptPt> points = editingCtx.getPoints();
			if (!points.isEmpty()) {
				if (points.size() == 1) {
					targetPointsHelper.clearAllPoints(false);
					targetPointsHelper.navigateToPoint(new LatLon(points.get(0).getLatitude(), points.get(0).getLongitude()), false, -1);
					dismiss(mapActivity);
					mapActions.enterRoutePlanningModeGivenGpx(null, appMode, null, null, true, true, MenuState.HEADER_ONLY);
				} else {
					String trackName = getSuggestedFileName();
					if (editingCtx.hasRoute()) {
						GpxFile gpx = editingCtx.exportGpx(trackName);
						if (gpx != null) {
							dismiss(mapActivity);
							runNavigation(gpx, appMode);
						} else {
							app.showShortToastMessage(R.string.error_occurred_saving_gpx);
						}
					} else {
						if (editingCtx.shouldCheckApproximation() && editingCtx.isApproximationNeeded() && editingCtx.hasTimestamps()) {
							setMode(DIRECTION_MODE, true);
							enterApproximationMode(mapActivity);
						} else {
							GpxFile gpx = new GpxFile(Version.getFullVersion(app));
							gpx.addRoutePoints(points, true);
							dismiss(mapActivity);
							targetPointsHelper.clearAllPoints(false);
							mapActions.enterRoutePlanningModeGivenGpx(gpx, appMode, null, null, true, true, MenuState.HEADER_ONLY);
						}
					}
				}
			} else {
				app.showShortToastMessage(R.string.none_point_error);
			}
		});
	}

	private void runNavigation(GpxFile gpx, ApplicationMode appMode) {
		callMapActivity(mapActivity -> {
			if (app.getRoutingHelper().isFollowingMode()) {
				if (isFollowTrackMode()) {
					mapActivity.getMapActions().setGPXRouteParams(gpx);
					app.getTargetPointsHelper().updateRouteAndRefresh(true);
					app.getRoutingHelper().onSettingsChanged(true);
				} else {
					mapActivity.getMapActions().stopNavigationActionConfirm(null, () -> {
						callMapActivity(activity -> activity.getMapActions().enterRoutePlanningModeGivenGpx(gpx, appMode, null, null, true, true, MenuState.HEADER_ONLY));
					});
				}
			} else {
				mapActivity.getMapActions().stopNavigationWithoutConfirm();
				mapActivity.getMapActions().enterRoutePlanningModeGivenGpx(gpx, appMode, null, null, true, true, MenuState.HEADER_ONLY);
			}
		});
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
		callMapActivity(this::openSaveAsNewTrackMenu);
	}

	@Override
	public void addToTrackOnClick() {
		callMapActivity(mapActivity -> {
			if (editingCtx.getPointsCount() > 0) {
				showAddToTrackDialog(mapActivity);
			} else {
				app.showShortToastMessage(R.string.none_point_error);
			}
		});
	}

	@Override
	public void clearAllOnClick() {
		MeasurementToolLayer measurementLayer = getMeasurementLayer();
		editingCtx.getCommandManager().execute(new ClearPointsCommand(measurementLayer, ALL));
		editingCtx.cancelSnapToRoad();
		collapseInfoViewIfExpanded();
		updateUndoRedoButton(false, redoBtn);
		disable(upDownBtn);
		updateDistancePointsText();
	}

	@Override
	public void reverseRouteOnClick() {
		callMapActivity(mapActivity -> {
			List<WptPt> points = editingCtx.getPoints();
			if (points.size() > 1) {
				MeasurementToolLayer measurementLayer = getMeasurementLayer();
				editingCtx.getCommandManager().execute(new ReversePointsCommand(measurementLayer));
				collapseInfoViewIfExpanded();
				updateUndoRedoButton(false, redoBtn);
				updateUndoRedoButton(true, undoBtn);
				updateDistancePointsText();
			} else {
				app.showShortToastMessage(R.string.one_point_error);
			}
		});
	}

	@Override
	public void attachToRoadsClick() {
		attachToRoadsSelected(-1);
	}

	@Override
	public void gpsFilterOnClick() {
		callMapActivity(mapActivity -> {
			GpxFile gpxFile = generateGpxFile();

			GpxSelectionParams params = GpxSelectionParams.getDefaultSelectionParams();
			SelectedGpxFile selectedGpxFile = app.getSelectedGpxHelper().selectGpxFile(gpxFile, params);

			hide();
			AndroidUiHelper.setVisibility(mapActivity, View.GONE, R.id.snap_to_road_image_button, R.id.map_ruler_layout);
			GpsFilterFragment.showInstance(mapActivity.getSupportFragmentManager(), selectedGpxFile, this);
		});
	}

	@NonNull
	public GpxFile generateGpxFile() {
		GpxData gpxData = editingCtx.getGpxData();
		GpxFile sourceGpx = gpxData != null ? gpxData.getGpxFile() : new GpxFile(Version.getFullVersion(app));
		GpxFile gpxFile = SaveGpxRouteAsyncTask.generateGpxFile(editingCtx, getSuggestedFileName(), sourceGpx, false, false);
		gpxFile.setPath(gpxData != null ? gpxData.getGpxFile().getPath() : getDefaultGpxPath());
		return gpxFile;
	}

	@NonNull
	private String getDefaultGpxPath() {
		return app.getAppPath(GPX_INDEX_DIR) + "/" + getSuggestedFileName() + GPX_FILE_EXT;
	}

	@Override
	public void getAltitudeClick() {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			FragmentManager manager = activity.getSupportFragmentManager();
			TrackAltitudeBottomSheet.showInstance(manager, this, editingCtx.getSelectedSegment());
		}
	}

	@Override
	public void attachToRoadsSelected(int segmentIndex) {
		if (editingCtx.isApproximationNeeded()) {
			callMapActivity(this::enterApproximationMode);
		}
	}

	@Override
	public void calculateOnlineSelected(int segmentIndex) {
		setMode(CALCULATE_SRTM_MODE, true);
		calculateSrtmTrack();
		setInfoType(InfoType.GRAPH);
		infoTypeBtn.setSelectedItem(graphBtn);
		updateInfoView();
	}

	@Override
	public void calculateOfflineSelected(int segmentIndex) {
		setMode(CALCULATE_HEIGHTMAP_MODE, true);
		editingCtx.setInsertIntermediates(true);
		calculateHeightmapTrack();
		setInfoType(InfoType.GRAPH);
		infoTypeBtn.setSelectedItem(graphBtn);
		updateInfoView();
	}

	@Override
	public void onMovePoint() {
		getMeasurementLayer().enterMovingPointMode();
		switchMovePointMode(true);
	}

	@Override
	public void onDeletePoint() {
		removePoint(getMeasurementLayer(), editingCtx.getSelectedPointPosition());
		editingCtx.setSelectedPointPosition(-1);
	}

	@Override
	public void onAddPointAfter() {
		getMeasurementLayer().moveMapToPoint(editingCtx.getSelectedPointPosition());
		editingCtx.setInAddPointMode(true, false);
		editingCtx.splitSegments(editingCtx.getSelectedPointPosition() + 1);

		((TextView) mainView.findViewById(R.id.add_point_before_after_text)).setText(mainView.getResources().getString(R.string.add_point_after));
		mainIcon.setImageDrawable(getActiveIcon(R.drawable.ic_action_addpoint_above));
		switchAddPointBeforeAfterMode(true);
	}

	@Override
	public void onAddPointBefore() {
		getMeasurementLayer().moveMapToPoint(editingCtx.getSelectedPointPosition());
		editingCtx.setInAddPointMode(true, true);
		editingCtx.splitSegments(editingCtx.getSelectedPointPosition());

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

	@Override
	public void onSplitPointsAfter() {
		MeasurementToolLayer measurementLayer = getMeasurementLayer();
		editingCtx.getCommandManager().execute(new SplitPointsCommand(measurementLayer, true));
		collapseInfoViewIfExpanded();
		editingCtx.setSelectedPointPosition(-1);
		updateUndoRedoButton(false, redoBtn);
		updateUndoRedoButton(true, undoBtn);
		updateDistancePointsText();
	}

	@Override
	public void onSplitPointsBefore() {
		MeasurementToolLayer measurementLayer = getMeasurementLayer();
		editingCtx.getCommandManager().execute(new SplitPointsCommand(measurementLayer, false));
		collapseInfoViewIfExpanded();
		editingCtx.setSelectedPointPosition(-1);
		updateUndoRedoButton(false, redoBtn);
		updateUndoRedoButton(true, undoBtn);
		updateDistancePointsText();
	}

	@Override
	public void onJoinPoints() {
		MeasurementToolLayer measurementLayer = getMeasurementLayer();
		editingCtx.getCommandManager().execute(new JoinPointsCommand(measurementLayer));
		collapseInfoViewIfExpanded();
		editingCtx.setSelectedPointPosition(-1);
		updateUndoRedoButton(false, redoBtn);
		updateUndoRedoButton(true, undoBtn);
		updateDistancePointsText();
	}

	private void trimRoute(ClearCommandMode before) {
		MeasurementToolLayer measurementLayer = getMeasurementLayer();
		editingCtx.getCommandManager().execute(new ClearPointsCommand(measurementLayer, before));
		collapseInfoViewIfExpanded();
		editingCtx.setSelectedPointPosition(-1);
		editingCtx.splitSegments(editingCtx.getBeforePoints().size() + editingCtx.getAfterPoints().size());
		updateUndoRedoButton(false, redoBtn);
		updateUndoRedoButton(true, undoBtn);
		updateDistancePointsText();
	}

	@Override
	public void onChangeRouteTypeBefore() {
		callMapActivity(mapActivity -> {
			FragmentManager fragmentManager = mapActivity.getSupportFragmentManager();
			RouteBetweenPointsBottomSheetDialogFragment.showInstance(fragmentManager,
					this, PREV_ROUTE_CALCULATION, RouteBetweenPointsDialogMode.SINGLE,
					editingCtx.getBeforeSelectedPointAppMode()
			);
		});
	}

	@Override
	public void onChangeRouteTypeAfter() {
		callMapActivity(mapActivity -> {
			FragmentManager fragmentManager = mapActivity.getSupportFragmentManager();
			RouteBetweenPointsBottomSheetDialogFragment.showInstance(fragmentManager,
					this, NEXT_ROUTE_CALCULATION,
					RouteBetweenPointsDialogMode.SINGLE, editingCtx.getSelectedPointAppMode()
			);
		});
	}

	@Override
	public void onCloseMenu() {
		updateMapDisplayPosition();
	}

	@Override
	public void onClearSelection() {
		editingCtx.setSelectedPointPosition(-1);
	}

	@Override
	public void onRecalculateAll() {
		editingCtx.recalculateRouteSegments(null);
	}

	@Override
	public void onCloseRouteDialog() {
		toolBarController.setTitle(previousToolBarTitle);
		editingCtx.setSelectedPointPosition(-1);
		callMapActivity(MapActivity::refreshMap);
	}

	@Override
	public void onChangeApplicationMode(ApplicationMode mode, RouteBetweenPointsDialogType dialogType,
	                                    RouteBetweenPointsDialogMode dialogMode) {
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
		changeApplicationMode(mode, changeRouteType);
	}

	public void changeApplicationMode(@NonNull ApplicationMode mode, @NonNull ChangeRouteType changeRouteType) {
		MeasurementToolLayer measurementLayer = getMeasurementLayer();
		editingCtx.getCommandManager().execute(new ChangeRouteModeCommand(measurementLayer, mode, changeRouteType, editingCtx.getSelectedPointPosition()));
		updateUndoRedoButton(false, redoBtn);
		updateUndoRedoButton(true, undoBtn);
		disable(upDownBtn);
		updateSnapToRoadControls();
		updateDistancePointsText();
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		MeasurementToolLayer measurementLayer = getMeasurementLayer();
		outState.putBoolean(TAPS_DISABLED_KEY, measurementLayer.isTapsDisabled());
		outState.putInt(MODES_KEY, modes);
		outState.putBoolean(SHOW_SNAP_WARNING_KEY, showSnapWarning);
		if (initialPoint != null) {
			outState.putSerializable(INITIAL_POINT_KEY, initialPoint);
		}
	}

	private SelectFileListener createAddToTrackFileListener() {
		return new SelectFileListener() {
			@Override
			public void selectFileOnCLick(String filePath) {
				callMapActivity(mapActivity -> {
					getGpxFile(filePath, gpxFile -> {
						SelectedGpxFile selectedGpxFile = app.getSelectedGpxHelper().getSelectedFileByPath(gpxFile.getPath());
						boolean showOnMap = selectedGpxFile != null;
						saveExistingGpx(gpxFile, showOnMap, false, true, FinalSaveAction.SHOW_IS_SAVED_FRAGMENT);
						return true;
					});
				});
			}

			@Override
			public void dismissButtonOnClick() {
			}
		};
	}

	private void getGpxFile(@Nullable String filePath, @NonNull CallbackWithObject<GpxFile> callback) {
		SelectedGpxFile selectedGpxFile = app.getSelectedGpxHelper().getSelectedFileByPath(filePath);
		if (filePath == null) {
			callback.processResult(app.getSavingTrackHelper().getCurrentGpx());
		} else if (selectedGpxFile != null) {
			callback.processResult(selectedGpxFile.getGpxFile());
		} else {
			GpxFileLoaderTask.loadGpxFile(new File(filePath), getActivity(), callback);
		}
	}

	public void addNewGpxData(GpxFile gpxFile) {
		GpxData gpxData = setupGpxData(gpxFile);
		initMeasurementMode(gpxData, true);
		if (gpxData != null) {
			adjustMapPosition(gpxData);
		}
	}

	private void adjustMapPosition(@NonNull GpxData gpxData) {
		MapActivity mapActivity = getMapActivity();
		if (adjustMapPosition && mapActivity != null) {
			QuadRect quadRect = gpxData.getRect();
			mapActivity.getMapView().fitRectToMap(quadRect.left, quadRect.right, quadRect.top, quadRect.bottom,
					((int) quadRect.width()), ((int) quadRect.height()), 0);
		}
		adjustMapPosition = true;
	}

	@Nullable
	private GpxData setupGpxData(@Nullable GpxFile gpxFile) {
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
			updateInfoView();
			updateUndoRedoButton(true, undoBtn);
			updateUndoRedoButton(false, redoBtn);
			updateDistancePointsText();
			collapseInfoIfNotEnoughPoints();
		}
	}

	@Override
	public void onSaveAsNewTrack(@NonNull String folderPath, @NonNull String fileName, boolean showOnMap, boolean simplified) {
		saveNewGpx(folderPath, fileName, showOnMap, simplified, FinalSaveAction.SHOW_IS_SAVED_FRAGMENT);
	}

	MeasurementAdapterListener createMeasurementAdapterListener(ItemTouchHelper touchHelper) {
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
				if (mapActivity != null) {
					collapseInfoViewIfExpanded();
					updateMapDisplayPosition();
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
				if (mapActivity != null) {
					toPosition = holder.getAdapterPosition();
					if (toPosition >= 0 && fromPosition >= 0 && toPosition != fromPosition) {
						editingCtx.getCommandManager().execute(new ReorderPointCommand(measurementLayer, fromPosition, toPosition));
						updateInfoView();
						updateUndoRedoButton(false, redoBtn);
						updateDistancePointsText();
						mapActivity.refreshMap();
					}
				}
			}
		};
	}

	private void setupAppMode(@NonNull ApplicationMode appMode) {
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
		callMapActivity(mapActivity -> {
			mainView.findViewById(R.id.snap_to_road_progress_bar).setVisibility(View.INVISIBLE);
			mapActivity.refreshMap();
		});
	}

	private void updateSnapToRoadControls() {
		ApplicationMode appMode = editingCtx.getAppMode();
		callMapActivity(mapActivity -> {
			Drawable icon;
			ImageButton snapToRoadBtn = mapActivity.findViewById(R.id.snap_to_road_image_button);
			LinearLayout profileWithConfig = mapActivity.findViewById(R.id.profile_with_config_btn);
			ImageButton configBtn = profileWithConfig.findViewById(R.id.profile);
			if (isTrackReadyToCalculate()) {
				if (appMode == DEFAULT_APP_MODE) {
					icon = getActiveIcon(R.drawable.ic_action_split_interval);
					snapToRoadBtn.setVisibility(View.VISIBLE);
					profileWithConfig.setVisibility(View.GONE);
				} else {
					icon = getPaintedIcon(appMode.getIconRes(), appMode.getProfileColor(nightMode));
					snapToRoadBtn.setVisibility(View.GONE);
					profileWithConfig.setVisibility(View.VISIBLE);
				}
			} else {
				icon = getContentIcon(R.drawable.ic_action_help);
			}
			snapToRoadBtn.setImageDrawable(icon);
			configBtn.setImageDrawable(icon);
			mapActivity.refreshMap();
		});
	}

	public boolean isTrackReadyToCalculate() {
		return !editingCtx.shouldCheckApproximation() || !editingCtx.isApproximationNeeded() || editingCtx.isNewData();
	}

	private void hideSnapToRoadIcon() {
		callMapActivity(mapActivity -> {
			mapActivity.findViewById(R.id.snap_to_road_image_button).setVisibility(View.GONE);
			mapActivity.findViewById(R.id.profile_with_config_btn).setVisibility(View.GONE);
		});
	}

	private void collectPoints() {
		if (!isUndoMode()) {
			editingCtx.addPoints();
		}
		updateInfoView();
		updateDistancePointsText();
	}

	private void openSelectedPointMenu(@NonNull MapActivity mapActivity) {
		SelectedPointBottomSheetDialogFragment.showInstance(mapActivity.getSupportFragmentManager(), this);
	}

	private void openSaveAsNewTrackMenu(@NonNull MapActivity mapActivity) {
		if (editingCtx.getPointsCount() > 0) {
			FragmentManager manager = mapActivity.getSupportFragmentManager();
			SaveAsNewTrackBottomSheetDialogFragment.showInstance(manager, getSuggestedFileName(), this, true, true);
		} else {
			app.showShortToastMessage(R.string.none_point_error);
		}
	}

	private void showAddToTrackDialog(@NonNull MapActivity mapActivity) {
		SelectTrackTabsFragment.showInstance(mapActivity.getSupportFragmentManager(), this);
	}

	@Override
	public boolean processResult(String filePath) {
		callMapActivity(mapActivity -> {
			getGpxFile(filePath, gpxFile -> {
				SelectedGpxFile selectedGpxFile = app.getSelectedGpxHelper().getSelectedFileByPath(gpxFile.getPath());
				boolean showOnMap = selectedGpxFile != null;
				saveExistingGpx(gpxFile, showOnMap, false, true, FinalSaveAction.SHOW_IS_SAVED_FRAGMENT);
				return true;
			});
		});
		return true;
	}

	private void applyMovePointMode() {
		switchMovePointMode(false);

		MeasurementToolLayer measurementLayer = getMeasurementLayer();
		WptPt oldPoint = editingCtx.getOriginalPointToMove();
		WptPt newPoint = measurementLayer.getMovedPointToApply();
		if (oldPoint != null && newPoint != null) {
			int position = editingCtx.getSelectedPointPosition();
			editingCtx.getCommandManager().execute(new MovePointCommand(measurementLayer, oldPoint, newPoint, position));
			editingCtx.addPoint(newPoint);
		}
		exitMovePointMode(false);
		doAddOrMovePointCommonStuff();
		measurementLayer.refreshMap();
	}

	private void cancelMovePointMode() {
		switchMovePointMode(false);
		exitMovePointMode(true);
		callMapActivity(MapActivity::refreshMap);
	}

	void exitMovePointMode(boolean cancelled) {
		if (cancelled) {
			WptPt pt = editingCtx.getOriginalPointToMove();
			if (pt != null) {
				editingCtx.addPoint(pt);
			}
		}
		editingCtx.setOriginalPointToMove(null);
		editingCtx.setSelectedPointPosition(-1);
		editingCtx.splitSegments(editingCtx.getBeforePoints().size() + editingCtx.getAfterPoints().size());
		getMeasurementLayer().exitMovePointMode();
	}

	void cancelModes() {
		if (editingCtx.getOriginalPointToMove() != null) {
			cancelMovePointMode();
		} else if (editingCtx.isInAddPointMode()) {
			cancelAddPointBeforeOrAfterMode();
		}
	}

	private void addPointBeforeAfter() {
		int selectedPoint = editingCtx.getSelectedPointPosition();
		int pointsCount = editingCtx.getPointsCount();
		if (addCenterPoint()) {
			if (selectedPoint == pointsCount) {
				editingCtx.splitSegments(editingCtx.getPointsCount() - 1);
			} else {
				editingCtx.setSelectedPointPosition(selectedPoint + 1);
			}
			getMeasurementLayer().refreshMap();
		}
	}

	private void applyAddPointBeforeAfterMode() {
		switchAddPointBeforeAfterMode(false);
		editingCtx.splitSegments(editingCtx.getBeforePoints().size() + editingCtx.getAfterPoints().size());
		editingCtx.setSelectedPointPosition(-1);
		editingCtx.setInAddPointMode(false, false);
		useLastPointAppMode();
		getMeasurementLayer().refreshMap();
		updateDistancePointsText();
	}

	private void cancelAddPointBeforeOrAfterMode() {
		switchAddPointBeforeAfterMode(false);
		editingCtx.splitSegments(editingCtx.getBeforePoints().size() + editingCtx.getAfterPoints().size());
		editingCtx.setSelectedPointPosition(-1);
		editingCtx.setInAddPointMode(false, false);
		useLastPointAppMode();
		getMeasurementLayer().refreshMap();
	}

	private void useLastPointAppMode() {
		ApplicationMode appMode = editingCtx.getAppMode();
		ApplicationMode lastPointAppMode = editingCtx.getLastPointAppMode();
		if (appMode != lastPointAppMode) {
			changeApplicationMode(lastPointAppMode, ChangeRouteType.LAST_SEGMENT);
		}
	}

	private void switchMovePointMode(boolean enable) {
		callMapActivity(mapActivity -> {
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
		});
	}

	private void switchAddPointBeforeAfterMode(boolean enable) {
		callMapActivity(mapActivity -> {
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
		});
	}

	private void markGeneralComponents(int status) {
		callMapActivity(mapActivity -> {
			AndroidUiHelper.setVisibility(mapActivity, status,
					R.id.measurement_distance_text_view,
					R.id.measurement_points_text_view,
					R.id.distance_to_center_text_view,
					R.id.up_down_button,
					R.id.measure_mode_controls,
					R.id.info_type_buttons_container);
		});
	}

	private void addInitialPoint() {
		if (initialPoint != null) {
			MeasurementToolLayer layer = getMeasurementLayer();
			editingCtx.getCommandManager().execute(new AddPointCommand(layer, initialPoint));
			doAddOrMovePointCommonStuff();
			initialPoint = null;
		}
	}

	private void addPoint() {
		MeasurementToolLayer measurementLayer = getMeasurementLayer();
		editingCtx.getCommandManager().execute(new AddPointCommand(measurementLayer, false));
		doAddOrMovePointCommonStuff();
	}

	private boolean addCenterPoint() {
		MeasurementToolLayer layer = getMeasurementLayer();
		boolean added = editingCtx.getCommandManager().execute(new AddPointCommand(layer, true));
		doAddOrMovePointCommonStuff();
		return added;
	}

	private void doAddOrMovePointCommonStuff() {
		recalculateHeightmapIfNeeded();
		enable(upDownBtn);
		updateUndoRedoButton(true, undoBtn);
		updateUndoRedoButton(false, redoBtn);
		updateDistancePointsText();
		updateInfoView();
	}

	private void recalculateHeightmapIfNeeded() {
		if (!isProgressBarVisible() && isCalculateHeightmapMode()) {
			stopCalculatingHeightMapTask(false);
			calculateHeightmapTrack();
		}
	}

	private void updateMapDisplayPosition() {
		mapDisplayPositionManager.updateMapDisplayPosition(true);
	}

	@Nullable
	@Override
	public MapPosition getMapDisplayPosition() {
		if (infoExpanded) {
			return portrait ? MapPosition.MIDDLE_TOP : MapPosition.LANDSCAPE_MIDDLE_END;
		}
		return MapPosition.CENTER;
	}

	private void addToGpx(FinalSaveAction finalSaveAction) {
		GpxData gpxData = editingCtx.getGpxData();
		GpxFile gpx = gpxData != null ? gpxData.getGpxFile() : null;
		if (gpx != null) {
			SelectedGpxFile selectedGpxFile = app.getSelectedGpxHelper().getSelectedFileByPath(gpx.getPath());
			boolean showOnMap = selectedGpxFile != null;
			saveExistingGpx(gpx, showOnMap, false, false, finalSaveAction);
		}
	}

	private void updateUpDownBtn() {
		Drawable icon = getContentIcon(infoExpanded
				? R.drawable.ic_action_arrow_down : R.drawable.ic_action_arrow_up);
		upDownBtn.setImageDrawable(icon);
	}

	private boolean isCurrentInfoType(@NonNull InfoType type) {
		return Algorithms.objectEquals(currentInfoType, type);
	}

	private String getSuggestedFileName() {
		return getSuggestedFileName(app, editingCtx.getGpxData());
	}

	public static String getSuggestedFileName(@NonNull OsmandApplication app, @Nullable GpxData gpxData) {
		String displayedName = null;
		if (gpxData != null) {
			GpxFile gpxFile = gpxData.getGpxFile();
			if (!Algorithms.isEmpty(gpxFile.getPath())) {
				displayedName = Algorithms.getFileNameWithoutExtension(new File(gpxFile.getPath()).getName());
			} else if (!Algorithms.isEmpty(gpxFile.getTracks())) {
				displayedName = gpxFile.getTracks().get(0).getName();
			}
		}
		if (gpxData == null || displayedName == null) {
			String suggestedName = new SimpleDateFormat("EEE dd MMM yyyy", Locale.US).format(new Date());
			displayedName = FileUtils.createUniqueFileName(app, suggestedName, GPX_INDEX_DIR, GPX_FILE_EXT);
		} else {
			displayedName = Algorithms.getFileNameWithoutExtension(new File(gpxData.getGpxFile().getPath()).getName());
		}
		return displayedName;
	}

	private void saveNewGpx(String folderPath, String fileName, boolean showOnMap,
	                        boolean simplified, FinalSaveAction finalSaveAction) {
		fileName += GPX_FILE_EXT;
		File dir = Algorithms.isEmpty(folderPath) ? app.getAppPath(GPX_INDEX_DIR) : new File(folderPath);
		saveNewGpx(dir, fileName, showOnMap, simplified, finalSaveAction);
	}

	private void saveNewGpx(@NonNull File dir, @NonNull String fileName, boolean showOnMap,
	                        boolean simplified, FinalSaveAction finalSaveAction) {
		saveGpx(new File(dir, fileName), null, simplified, false, finalSaveAction, showOnMap);
	}

	private void saveExistingGpx(@NonNull GpxFile gpx, boolean showOnMap,
	                             boolean simplified, boolean addToTrack, FinalSaveAction finalSaveAction) {
		saveGpx(new File(gpx.getPath()), gpx, simplified, addToTrack, finalSaveAction, showOnMap);
	}

	private void saveGpx(@NonNull File outFile, @Nullable GpxFile gpxFile, boolean simplified,
	                     boolean addToTrack, FinalSaveAction finalSaveAction, boolean showOnMap) {
		SaveGpxRouteListener listener = (warning, savedGpxFile, backupFile) -> onGpxSaved(warning, savedGpxFile, outFile, backupFile, finalSaveAction, showOnMap);
		SaveGpxRouteAsyncTask saveTask = new SaveGpxRouteAsyncTask(this, outFile, gpxFile, simplified,
				addToTrack, showOnMap, listener);
		OsmAndTaskManager.executeTask(saveTask);
	}

	private void onGpxSaved(Exception warning, GpxFile savedGpxFile, File outFile, File backupFile,
	                        FinalSaveAction finalSaveAction, boolean showOnMap) {
		callMapActivity(mapActivity -> {
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
							showSnackbarAndDismiss(mapActivity, outFile, backupFile, showOnMap);
							break;
						case SHOW_IS_SAVED_FRAGMENT:
							editingCtx.setChangesSaved();
							SavedTrackBottomSheetDialogFragment.showInstance(mapActivity.getSupportFragmentManager(),
									outFile.getAbsolutePath(), true);
							dismiss(mapActivity);
							break;
						case SHOW_TOAST:
							editingCtx.setChangesSaved();
							if (savedGpxFile != null && !savedGpxFile.isShowCurrentTrack()) {
								app.showToastMessage(R.string.gpx_saved_sucessfully, outFile.getAbsolutePath());
							}
					}
				}
			} else {
				app.showToastMessage(warning.getMessage());
			}
		});
	}

	private void showSnackbarAndDismiss(@NonNull MapActivity mapActivity,
	                                    @NonNull File outFile,
	                                    @Nullable File backupFile,
	                                    boolean showOnMap) {
		WeakReference<MapActivity> mapActivityRef = new WeakReference<>(mapActivity);
		Snackbar snackbar = Snackbar.make(mapActivity.getLayout(),
						MessageFormat.format(getString(R.string.gpx_saved_sucessfully), outFile.getName()),
						Snackbar.LENGTH_LONG)
				.setAction(R.string.shared_string_undo, view -> {
					MapActivity activity = mapActivityRef.get();
					if (activity != null) {
						undoSave(activity, outFile, backupFile, showOnMap);
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
	}

	private void undoSave(@NonNull MapActivity mapActivity, @NonNull File outFile, @Nullable File backupFile,
	                      boolean showOnMap) {
		FileUtils.removeGpxFile(app, outFile);
		if (backupFile != null) {
			FileUtils.renameGpxFile(app, backupFile, outFile);
			GpxFileLoaderTask.loadGpxFile(outFile, mapActivity, gpxFile -> {
				setupGpxData(gpxFile);
				if (showOnMap) {
					showGpxOnMap(app, gpxFile, false);
				}
				if (AndroidUtils.isActivityNotDestroyed(mapActivity)) {
					setMode(UNDO_MODE, true);
					showInstance(mapActivity.getSupportFragmentManager(),
							editingCtx, modes);
				}
				return true;
			});
		} else {
			setupGpxData(null);
			setMode(UNDO_MODE, true);
			showInstance(mapActivity.getSupportFragmentManager(),
					editingCtx, modes);
		}
	}

	protected static void showGpxOnMap(@NonNull OsmandApplication app, @NonNull GpxFile gpx, boolean isNewGpx) {
		GpxSelectionParams params = GpxSelectionParams.getDefaultSelectionParams();
		SelectedGpxFile sf = app.getSelectedGpxHelper().selectGpxFile(gpx, params);
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

	private boolean hasAltitude() {
		GpxData gpxData = editingCtx.getGpxData();
		if (gpxData == null) {
			return false;
		}
		GpxFile gpxFile = gpxData.getGpxFile();
		SelectedGpxFile selectedGpxFile = app.getSelectedGpxHelper().getSelectedFileByPath(gpxFile.getPath());
		GpxTrackAnalysis analysis = selectedGpxFile != null
				? selectedGpxFile.getTrackAnalysis(app)
				: gpxFile.getAnalysis(System.currentTimeMillis());
		return analysis.hasElevationData();
	}

	private void updateDistancePointsText() {
		String distanceStr = OsmAndFormatter.getFormattedDistance((float) editingCtx.getRouteDistance(), app);
		distanceTv.setText(distanceStr + ",");
		pointsTv.setText((portrait ? pointsSt + ": " : "") + editingCtx.getPointsCount());
		updateToolbar();
	}

	private void updateToolbar() {
		callMapActivity(mapActivity -> {
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
			boolean editGpx = isPlanRouteMode() && !editingCtx.isNewData();
			toolBarController.setSaveViewTextId(editGpx ? R.string.shared_string_save : R.string.shared_string_done);
			mapActivity.showTopToolbar(toolBarController);
		});
	}

	private void enterMeasurementMode() {
		callMapActivity(mapActivity -> {
			MeasurementToolLayer measurementLayer = getMeasurementLayer();
			measurementLayer.setInMeasurementMode(true);
			measurementLayer.refreshMap();
			mapActivity.disableDrawer();

			mainView.getViewTreeObserver().addOnGlobalLayoutListener(getWidgetsLayoutListener());

			updateMainIcon();
			updateDistancePointsText();
		});
	}

	private OnGlobalLayoutListener getWidgetsLayoutListener() {
		if (widgetsLayoutListener == null) {
			widgetsLayoutListener = () -> {
				callMapActivity(mapActivity -> {
					View rightWidgetsPanel = mapActivity.findViewById(R.id.map_right_widgets_panel);
					if (rightWidgetsPanel.getVisibility() != View.GONE) {
						AndroidUiHelper.setVisibility(mapActivity, View.GONE,
								R.id.map_left_widgets_panel,
								R.id.map_right_widgets_panel,
								R.id.map_center_info,
								R.id.map_route_info_button,
								R.id.map_menu_button,
								R.id.map_quick_actions_button);
					}
				});
			};
		}
		return widgetsLayoutListener;
	}

	private void exitMeasurementMode() {
		callMapActivity(mapActivity -> {
			if (toolBarController != null) {
				mapActivity.hideTopToolbar(toolBarController);
			}
			getMeasurementLayer().setInMeasurementMode(false);
			mapActivity.enableDrawer();

			ViewTreeObserver observer = mainView.getViewTreeObserver();
			observer.removeOnGlobalLayoutListener(getWidgetsLayoutListener());
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

			mapActivity.refreshMap();
		});
	}

	public void quit(boolean hideInfoViewFirst) {
		if (editingCtx.getOriginalPointToMove() != null) {
			cancelMovePointMode();
			return;
		} else if (editingCtx.isInAddPointMode()) {
			cancelAddPointBeforeOrAfterMode();
			return;
		}
		if (isFollowTrackMode()) {
			callMapActivity(mapActivity -> {
				mapActivity.getMapActions().showRouteInfoControlDialog();
				dismiss(mapActivity);
			});
		} else {
			showQuitDialog(hideInfoViewFirst);
		}
	}

	private void showQuitDialog(boolean hideInfoViewFirst) {
		callMapActivity(mapActivity -> {
			if (infoExpanded && hideInfoViewFirst) {
				collapseInfoView();
				return;
			}
			if (!editingCtx.hasChanges()) {
				dismiss(mapActivity);
				return;
			}
			FragmentManager manager = mapActivity.getSupportFragmentManager();
			ExitBottomSheetDialogFragment.showInstance(manager, this, getString(R.string.plan_route_exit_dialog_descr));
		});
	}

	private void dismiss(@NonNull MapActivity mapActivity) {
		dismiss(mapActivity, true);
	}

	private void dismiss(@NonNull MapActivity mapActivity, boolean clearContext) {
		try {
			if (clearContext) {
				editingCtx.clearSegments();
			}
			collapseInfoViewIfExpanded();
			resetAppMode();
			hideSnapToRoadIcon();
			if (isInEditMode()) {
				GpxData gpxData = editingCtx.getGpxData();
				GpxFile gpx = gpxData != null ? gpxData.getGpxFile() : null;
				if (gpx != null) {
					TrackMenuFragment.openTrack(mapActivity, new File(gpx.getPath()), null);
				}
			}
			editingCtx.resetRouteSettingsListener();
			app.setMeasurementEditingContext(null);
			mapActivity.getSupportFragmentManager().beginTransaction().remove(this).commitAllowingStateLoss();
		} catch (Exception e) {
			// ignore
		}
	}

	public static boolean showSnapToRoadsDialog(@NonNull MapActivity activity, boolean showSnapWarning) {
		OsmandApplication app = activity.getApp();
		GpxFile gpxFile = app.getRoutingHelper().getCurrentGPX();
		if (gpxFile != null) {
			GpxData gpxData = new GpxData(gpxFile);
			MeasurementEditingContext editingContext = new MeasurementEditingContext(app);
			editingContext.setGpxData(gpxData);
			editingContext.setAppMode(app.getRoutingHelper().getAppMode());
			editingContext.setSelectedSegment(app.getSettings().GPX_SEGMENT_INDEX.get());
			MeasurementToolFragment.showInstance(activity.getSupportFragmentManager(), editingContext, FOLLOW_TRACK_MODE, showSnapWarning);
			return true;
		}
		return false;
	}

	public static boolean showInstance(FragmentManager fragmentManager, LatLon initialPoint) {
		MeasurementToolFragment fragment = new MeasurementToolFragment();
		fragment.setInitialPoint(initialPoint);
		fragment.setMode(PLAN_ROUTE_MODE, true);
		return showFragment(fragment, fragmentManager);
	}

	public static boolean showInstance(FragmentManager fragmentManager, String filePath, boolean adjustMapPosition) {
		MeasurementToolFragment fragment = new MeasurementToolFragment();
		fragment.filePath = filePath;
		fragment.adjustMapPosition = adjustMapPosition;
		fragment.setMode(PLAN_ROUTE_MODE, true);
		return showFragment(fragment, fragmentManager);
	}

	public static boolean showInstance(@NonNull FragmentManager manager,
	                                   @Nullable MeasurementEditingContext editingCtx,
	                                   @MeasurementToolMode int mode,
	                                   boolean showSnapWarning) {
		MeasurementToolFragment fragment = new MeasurementToolFragment();
		fragment.setEditingCtx(editingCtx);
		fragment.setMode(mode, true);
		fragment.setShowSnapWarning(showSnapWarning);
		return showFragment(fragment, manager);
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

	public static boolean showInstance(@NonNull MapActivity activity, @NonNull GpxFile gpxFile,
	                                   int segmentIndex, int modes) {
		OsmandApplication app = activity.getApp();
		GpxData gpxData = new GpxData(gpxFile);

		QuadRect rect = gpxData.getRect();
		OsmandMapTileView mapView = app.getOsmandMap().getMapView();
		mapView.fitRectToMap(rect.left, rect.right, rect.top, rect.bottom, (int) rect.width(), (int) rect.height(), 0);

		MeasurementEditingContext editingCtx = new MeasurementEditingContext(app);
		editingCtx.setGpxData(gpxData);
		editingCtx.setSelectedSegment(segmentIndex);
		editingCtx.setAppMode(app.getSettings().getApplicationMode());

		return showInstance(activity.getSupportFragmentManager(), editingCtx, modes, true);
	}

	private static boolean showFragment(@NonNull MeasurementToolFragment fragment,
	                                    @NonNull FragmentManager fragmentManager) {
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			fragment.setRetainInstance(true);
			fragmentManager.beginTransaction()
					.add(R.id.bottomFragmentContainer, fragment, TAG)
					.commitAllowingStateLoss();
			return true;
		}
		return false;
	}

	@Override
	public void onGpxApproximationDone(List<GpxRouteApproximation> gpxApproximations, List<List<WptPt>> pointsList, ApplicationMode mode) {
		MeasurementToolLayer measurementLayer = getMeasurementLayer();
		boolean approximationMode = editingCtx.isInApproximationMode();
		editingCtx.setInApproximationMode(true);
		ApplyGpxApproximationCommand command = new ApplyGpxApproximationCommand(measurementLayer, gpxApproximations, pointsList, mode);
		if (!approximationMode || !editingCtx.getCommandManager().update(command)) {
			editingCtx.getCommandManager().execute(command);
		}
		collapseInfoViewIfExpanded();
		updateSnapToRoadControls();
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
		callMapActivity(mapActivity -> {
			String trackName = getSuggestedFileName();
			GpxFile gpx = editingCtx.exportGpx(trackName);
			if (gpx != null) {
				ApplicationMode appMode = editingCtx.getAppMode();
				dismiss(mapActivity);
				runNavigation(gpx, appMode);
			} else {
				app.showShortToastMessage(R.string.error_occurred_saving_gpx);
			}
		});
	}

	@Override
	public void onCancelGpxApproximation() {
		editingCtx.getCommandManager().undo();
		exitApproximationMode();
		setMode(DIRECTION_MODE, false);
		updateSnapToRoadControls();
		updateToolbar();
	}

	private void enterApproximationMode(@NonNull MapActivity mapActivity) {
		getMeasurementLayer().setTapsDisabled(true);
		hide();
		FragmentManager manager = mapActivity.getSupportFragmentManager();
		SnapTrackWarningFragment.showInstance(manager, this);
		AndroidUiHelper.setVisibility(mapActivity, View.GONE, R.id.map_ruler_layout);
	}

	private void exitApproximationMode() {
		editingCtx.setInApproximationMode(false);
		callMapActivity(mapActivity -> {
			getMeasurementLayer().setTapsDisabled(false);
			show();
			AndroidUiHelper.setVisibility(mapActivity, View.VISIBLE, R.id.map_ruler_layout);
		});
	}

	private void show() {
		callMapActivity(mapActivity -> mapActivity.getSupportFragmentManager()
				.beginTransaction()
				.show(this)
				.commitAllowingStateLoss());
	}

	private void hide() {
		callMapActivity(mapActivity -> mapActivity.getSupportFragmentManager()
				.beginTransaction()
				.hide(this)
				.commitAllowingStateLoss());
	}

	public boolean isNightModeForMapControls() {
		return nightMode;
	}

	@Override
	public void onFinishFiltering(@NonNull GpxFile filteredGpxFile) {
	}

	@Override
	public void onDismissGpsFilterFragment(boolean savedCopy, @Nullable String savedFilePath) {
		callMapActivity(mapActivity -> {
			if (savedCopy) {
				dismiss(mapActivity);
			} else {
				updateSnapToRoadControls();
				AndroidUiHelper.setVisibility(mapActivity, View.VISIBLE, R.id.map_ruler_layout);
				show();

				boolean modifiedByFilter = !Algorithms.isEmpty(savedFilePath);
				if (modifiedByFilter) {
					editingCtx.clearSegments();
					editingCtx.clearCommands();

					updateUndoRedoButton(false, undoBtn);
					updateUndoRedoButton(false, redoBtn);
					updateUndoRedoCommonStuff();

					GpxFileLoaderTask.loadGpxFile(new File(savedFilePath), mapActivity, gpxFile -> {
						addNewGpxData(gpxFile);
						return true;
					});
				}
			}
		});
	}

	@Override
	public void onFileUploadDone(@NonNull NetworkResult networkResult) {
		calculateSrtmTask = null;

		String error = networkResult.getError();
		String response = networkResult.getResponse();

		if (error == null && !Algorithms.isEmpty(response)) {
			InputStream inputStream = new ByteArrayInputStream(response.getBytes());
			GpxFileLoaderTask.loadGpxFile(inputStream, getActivity(), gpxFile -> {
				if (gpxFile.getError() == null) {
					List<WptPt> points = editingCtx.getPoints();
					List<WptPt> segmentsPoints = gpxFile.getAllSegmentsPoints();
					if (points.size() == segmentsPoints.size()) {
						for (int i = 0; i < points.size(); i++) {
							WptPt point = points.get(i);
							point.setEle(segmentsPoints.get(i).getEle());
						}
					}
				} else {
					app.showToastMessage(SharedUtil.jException(gpxFile.getError()).getMessage());
				}
				updateInfoView();

				return true;
			});
		}
		if (!Algorithms.isEmpty(error)) {
			updateInfoView();
			app.showToastMessage(error);
		}
	}

	public interface OnUpdateInfoListener {
		void onUpdateInfo();
	}
}