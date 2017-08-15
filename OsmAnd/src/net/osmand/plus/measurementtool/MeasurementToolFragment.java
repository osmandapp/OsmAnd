package net.osmand.plus.measurementtool;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import net.osmand.AndroidUtils;
import net.osmand.CallbackWithObject;
import net.osmand.IndexConstants;
import net.osmand.plus.GPXUtilities;
import net.osmand.plus.GPXUtilities.Route;
import net.osmand.plus.GPXUtilities.WptPt;
import net.osmand.plus.GpxSelectionHelper.SelectedGpxFile;
import net.osmand.plus.IconsCache;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.TrackActivity;
import net.osmand.plus.activities.TrackActivity.NewGpxLine;
import net.osmand.plus.activities.TrackActivity.NewGpxLine.LineType;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.helpers.GpxUiHelper;
import net.osmand.plus.measurementtool.adapter.MeasurementToolAdapter;
import net.osmand.plus.measurementtool.adapter.MeasurementToolItemTouchHelperCallback;
import net.osmand.plus.measurementtool.command.AddPointCommand;
import net.osmand.plus.measurementtool.command.ClearPointsCommand;
import net.osmand.plus.measurementtool.command.CommandManager;
import net.osmand.plus.measurementtool.command.MovePointCommand;
import net.osmand.plus.measurementtool.command.RemovePointCommand;
import net.osmand.plus.measurementtool.command.ReorderPointCommand;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.mapwidgets.MapInfoWidgetsFactory;
import net.osmand.plus.views.mapwidgets.MapInfoWidgetsFactory.TopToolbarController;

import java.io.File;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import static net.osmand.plus.GPXUtilities.GPXFile;
import static net.osmand.plus.OsmandSettings.LANDSCAPE_MIDDLE_RIGHT_CONSTANT;
import static net.osmand.plus.OsmandSettings.MIDDLE_TOP_CONSTANT;
import static net.osmand.plus.helpers.GpxImportHelper.GPX_SUFFIX;

public class MeasurementToolFragment extends Fragment {

	public static final String TAG = "MeasurementToolFragment";

	private final CommandManager commandManager = new CommandManager();
	private IconsCache iconsCache;
	private RecyclerView pointsRv;
	private MeasurementToolBarController toolBarController;
	private MeasurementToolAdapter adapter;
	private TextView distanceTv;
	private TextView pointsTv;
	private String pointsSt;
	private Drawable upIcon;
	private Drawable downIcon;
	private View pointsListContainer;
	private View upDownRow;
	private ImageView upDownBtn;
	private ImageView undoBtn;
	private ImageView redoBtn;

	private boolean wasCollapseButtonVisible;
	private boolean pointsListOpened;
	private boolean saved;
	private boolean portrait;
	private boolean nightMode;
	private int previousMapPosition;
	private NewGpxLine newGpxLine;

	private boolean inMovePointMode;
	private boolean inAddPointAfterMode;
	private boolean inAddPointBeforeMode;

	private int positionToAddPoint = -1;

	public void setNewGpxLine(NewGpxLine newGpxLine) {
		this.newGpxLine = newGpxLine;
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		final MapActivity mapActivity = (MapActivity) getActivity();
		final MeasurementToolLayer measurementLayer = mapActivity.getMapLayers().getMeasurementToolLayer();
		iconsCache = mapActivity.getMyApplication().getIconsCache();
		nightMode = mapActivity.getMyApplication().getDaynightHelper().isNightModeForMapControls();
		final int themeRes = nightMode ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme;
		final int backgroundColor = ContextCompat.getColor(getActivity(),
				nightMode ? R.color.ctx_menu_info_view_bg_dark : R.color.ctx_menu_info_view_bg_light);
		portrait = AndroidUiHelper.isOrientationPortrait(getActivity());

		upIcon = getContentIcon(R.drawable.ic_action_arrow_up);
		downIcon = getContentIcon(R.drawable.ic_action_arrow_down);
		pointsSt = getString(R.string.points).toLowerCase();

		View view = View.inflate(new ContextThemeWrapper(getContext(), themeRes), R.layout.fragment_measurement_tool, null);

		final View mainView = view.findViewById(R.id.main_view);
		AndroidUtils.setBackground(mapActivity, mainView, nightMode, R.drawable.bg_bottom_menu_light, R.drawable.bg_bottom_menu_dark);
		pointsListContainer = view.findViewById(R.id.points_list_container);
		if (portrait && pointsListContainer != null) {
			pointsListContainer.setBackgroundColor(backgroundColor);
		}

		distanceTv = (TextView) mainView.findViewById(R.id.measurement_distance_text_view);
		pointsTv = (TextView) mainView.findViewById(R.id.measurement_points_text_view);

		((ImageView) mainView.findViewById(R.id.ruler_icon))
				.setImageDrawable(iconsCache.getIcon(R.drawable.ic_action_ruler, nightMode ? R.color.osmand_orange : R.color.color_myloc_distance));

		((ImageView) mainView.findViewById(R.id.move_point_icon))
				.setImageDrawable(iconsCache.getIcon(R.drawable.ic_action_move_point, nightMode ? R.color.osmand_orange : R.color.color_myloc_distance));

		((ImageView) mainView.findViewById(R.id.add_point_after_icon))
				.setImageDrawable(iconsCache.getIcon(R.drawable.ic_action_addpoint_above, nightMode ? R.color.osmand_orange : R.color.color_myloc_distance));

		((ImageView) mainView.findViewById(R.id.add_point_before_icon))
				.setImageDrawable(iconsCache.getIcon(R.drawable.ic_action_addpoint_below, nightMode ? R.color.osmand_orange : R.color.color_myloc_distance));

		upDownBtn = (ImageView) mainView.findViewById(R.id.up_down_button);
		upDownBtn.setImageDrawable(upIcon);

		mainView.findViewById(R.id.cancel_point_button).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (inMovePointMode) {
					cancelMovePointMode();
				} else if (inAddPointAfterMode) {
					cancelAddPointAfterMode();
				} else if (inAddPointBeforeMode) {
					cancelAddPointBeforeMode();
				}
			}
		});

		upDownRow = mainView.findViewById(R.id.up_down_row);
		upDownRow.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (!pointsListOpened && measurementLayer.getPointsCount() > 0 && !measurementLayer.isInMovePointMode() && !measurementLayer.isInAddPointAfterMode() && !measurementLayer.isInAddPointBeforeMode()) {
					showPointsList();
				} else {
					hidePointsList();
				}
			}
		});

		mainView.findViewById(R.id.apply_point_button).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (inMovePointMode) {
					applyMovePointMode();
				} else if (inAddPointAfterMode) {
					applyAddPointAfterMode();
				} else if (inAddPointBeforeMode) {
					applyAddPointBeforeMode();
				}
			}
		});

		mainView.findViewById(R.id.options_button).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				OptionsBottomSheetDialogFragment fragment = new OptionsBottomSheetDialogFragment();
				fragment.setOptionsOnClickListener(new OptionsBottomSheetDialogFragment.OptionsOnClickListener() {
					@Override
					public void snapToRoadOnCLick() {
						SnapToRoadBottomSheetDialogFragment fragment = new SnapToRoadBottomSheetDialogFragment();
						fragment.show(mapActivity.getSupportFragmentManager(), SnapToRoadBottomSheetDialogFragment.TAG);
					}

					@Override
					public void addToGpxOnClick() {
						if (measurementLayer.getPointsCount() > 0) {
							addToGpx(mapActivity);
						} else {
							Toast.makeText(mapActivity, getString(R.string.none_point_error), Toast.LENGTH_SHORT).show();
						}
					}

					@Override
					public void saveAsNewTrackOnClick() {
						if (measurementLayer.getPointsCount() > 0) {
							saveAsGpxOnClick(mapActivity);
						} else {
							Toast.makeText(mapActivity, getString(R.string.none_point_error), Toast.LENGTH_SHORT).show();
						}
					}

					@Override
					public void addToTheTrackOnClick() {
						if (measurementLayer.getPointsCount() > 0) {
//										showAddSegmentDialog(mapActivity);
						} else {
							Toast.makeText(mapActivity, getString(R.string.none_point_error), Toast.LENGTH_SHORT).show();
						}
					}

					@Override
					public void clearAllOnClick() {
						commandManager.execute(new ClearPointsCommand(measurementLayer));
						if (pointsListOpened) {
							hidePointsList();
						}
						disable(redoBtn, upDownBtn);
						updateText();
						saved = false;
					}
				});
				fragment.setAddLineMode(newGpxLine != null);
				fragment.show(mapActivity.getSupportFragmentManager(), OptionsBottomSheetDialogFragment.TAG);
			}
		});

		undoBtn = ((ImageButton) mainView.findViewById(R.id.undo_point_button));
		redoBtn = ((ImageButton) mainView.findViewById(R.id.redo_point_button));

		undoBtn.setImageDrawable(getContentIcon(R.drawable.ic_action_undo_dark));
		undoBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				commandManager.undo();
				if (commandManager.canUndo()) {
					enable(undoBtn);
				} else {
					disable(undoBtn);
				}
				hidePointsListIfNoPoints();
				if (measurementLayer.getPointsCount() > 0) {
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
				commandManager.redo();
				if (commandManager.canRedo()) {
					enable(redoBtn);
				} else {
					disable(redoBtn);
				}
				hidePointsListIfNoPoints();
				if (measurementLayer.getPointsCount() > 0) {
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
				addPoint();
			}
		});

		measurementLayer.setOnSingleTapListener(new MeasurementToolLayer.OnSingleTapListener() {
			@Override
			public void onAddPoint() {
				addPoint();
			}

			@Override
			public void onSelectPoint() {
				openSelectedPointMenu(mapActivity);
			}
		});

		measurementLayer.setOnEnterMovePointModeListener(new MeasurementToolLayer.OnEnterMovePointModeListener() {
			@Override
			public void onEnterMovePointMode() {
				if (pointsListOpened) {
					hidePointsList();
				}
				enterMovePointMode();
			}
		});

		disable(undoBtn, redoBtn, upDownBtn);

		enterMeasurementMode();

		toolBarController = new MeasurementToolBarController(newGpxLine);
		if (newGpxLine != null) {
			toolBarController.setTitle(getString(R.string.add_line));
		} else {
			toolBarController.setTitle(getString(R.string.measurement_tool_action_bar));
		}
		toolBarController.setOnBackButtonClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				showQuitDialog(false);
			}
		});
		toolBarController.setOnSaveViewClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (measurementLayer.getPointsCount() > 0) {
					addToGpx(mapActivity);
				} else {
					Toast.makeText(mapActivity, getString(R.string.none_point_error), Toast.LENGTH_SHORT).show();
				}
			}
		});
		mapActivity.showTopToolbar(toolBarController);

		adapter = new MeasurementToolAdapter(getMapActivity(), measurementLayer.getMeasurementPoints());
		if (portrait) {
			pointsRv = mainView.findViewById(R.id.measure_points_recycler_view);
		} else {
			pointsRv = new RecyclerView(getActivity());
		}
		final ItemTouchHelper touchHelper = new ItemTouchHelper(new MeasurementToolItemTouchHelperCallback(adapter));
		touchHelper.attachToRecyclerView(pointsRv);
		adapter.setAdapterListener(new MeasurementToolAdapter.MeasurementAdapterListener() {

			private int fromPosition;
			private int toPosition;

			@Override
			public void onRemoveClick(int position) {
				commandManager.execute(new RemovePointCommand(measurementLayer, position));
				adapter.notifyDataSetChanged();
				disable(redoBtn);
				updateText();
				saved = false;
				hidePointsListIfNoPoints();
			}

			@Override
			public void onItemClick(View view) {
				measurementLayer.moveMapToPoint(pointsRv.indexOfChild(view));
			}

			@Override
			public void onDragStarted(RecyclerView.ViewHolder holder) {
				fromPosition = holder.getAdapterPosition();
				touchHelper.startDrag(holder);
			}

			@Override
			public void onDragEnded(RecyclerView.ViewHolder holder) {
				toPosition = holder.getAdapterPosition();
				if (toPosition >= 0 && fromPosition >= 0 && toPosition != fromPosition) {
					commandManager.execute(new ReorderPointCommand(measurementLayer, fromPosition, toPosition));
					adapter.notifyDataSetChanged();
					disable(redoBtn);
					mapActivity.refreshMap();
					saved = false;
				}
			}
		});
		pointsRv.setLayoutManager(new LinearLayoutManager(getContext()));
		pointsRv.setAdapter(adapter);

		return view;
	}

	private void openSelectedPointMenu(MapActivity mapActivity) {
		final MeasurementToolLayer measurementLayer = mapActivity.getMapLayers().getMeasurementToolLayer();

		SelectedPointMenuBottomSheetDialogFragment fragment = new SelectedPointMenuBottomSheetDialogFragment();
		fragment.setSelectedPointOptionOnClickListener(new SelectedPointMenuBottomSheetDialogFragment.SelectedPointOptionOnClickListener() {
			@Override
			public void moveOnClick() {
				if (measurementLayer != null) {
					measurementLayer.enterMovingPointMode();
				}
				enterMovePointMode();
			}

			@Override
			public void deleteOnClick() {
				if (measurementLayer != null) {
					int position = measurementLayer.getSelectedPointPos();
					commandManager.execute(new RemovePointCommand(measurementLayer, position));
					adapter.notifyDataSetChanged();
					disable(redoBtn);
					updateText();
					saved = false;
					hidePointsListIfNoPoints();
					measurementLayer.clearSelection();
				}
			}

			@Override
			public void addPointAfterOnClick() {
				if (measurementLayer != null) {
					positionToAddPoint = measurementLayer.getSelectedPointPos() + 1;
					measurementLayer.enterAddingPointAfterMode();
				}
				enterAddPointAfterMode();
			}

			@Override
			public void addPointBeforeOnClick() {
				if (measurementLayer != null) {
					positionToAddPoint = measurementLayer.getSelectedPointPos();
					measurementLayer.enterAddingPointBeforeMode();
				}
				enterAddPointBeforeMode();
			}
		});
		fragment.show(mapActivity.getSupportFragmentManager(), SelectedPointMenuBottomSheetDialogFragment.TAG);
	}

	private AlertDialog showAddSegmentDialog(final MapActivity mapActivity) {
		CallbackWithObject<GPXFile[]> callbackWithObject = new CallbackWithObject<GPXFile[]>() {
			@Override
			public boolean processResult(GPXFile[] result) {
				GPXFile gpxFile;
				if (result != null && result.length > 0) {
					gpxFile = result[0];
				}
				return true;
			}
		};

		return GpxUiHelper.selectSingleGPXFile(mapActivity, false, callbackWithObject);
	}

	private void cancelMovePointMode() {
		if (inMovePointMode) {
			exitMovePointMode();
		}
		MeasurementToolLayer measurementToolLayer = getMeasurementLayer();
		if (measurementToolLayer != null) {
			measurementToolLayer.exitMovePointMode();
			measurementToolLayer.clearSelection();
			measurementToolLayer.refreshMap();
		}
	}

	private void cancelAddPointAfterMode() {
		if (inAddPointAfterMode) {
			exitAddPointAfterMode();
		}
		MeasurementToolLayer measurementToolLayer = getMeasurementLayer();
		if (measurementToolLayer != null) {
			measurementToolLayer.exitAddPointAfterMode();
			measurementToolLayer.clearSelection();
			measurementToolLayer.refreshMap();
		}
		positionToAddPoint = -1;
	}

	private void cancelAddPointBeforeMode() {
		if (inAddPointBeforeMode) {
			exitAddPointBeforeMode();
		}
		MeasurementToolLayer measurementToolLayer = getMeasurementLayer();
		if (measurementToolLayer != null) {
			measurementToolLayer.exitAddPointBeforeMode();
			measurementToolLayer.clearSelection();
			measurementToolLayer.refreshMap();
		}
		positionToAddPoint = -1;
	}

	private void applyMovePointMode() {
		if (inMovePointMode) {
			exitMovePointMode();
		}
		MeasurementToolLayer measurementLayer = getMeasurementLayer();
		if (measurementLayer != null) {
			WptPt newPoint = measurementLayer.getMovedPointToApply();
			WptPt oldPoint = measurementLayer.getSelectedCachedPoint();
			int position = measurementLayer.getSelectedPointPos();
			commandManager.execute(new MovePointCommand(measurementLayer, oldPoint, newPoint, position));
			enable(undoBtn, upDownBtn);
			disable(redoBtn);
			updateText();
			adapter.notifyDataSetChanged();
			saved = false;
			measurementLayer.exitMovePointMode();
			measurementLayer.clearSelection();
			measurementLayer.refreshMap();
		}
	}

	private void applyAddPointAfterMode() {
		if (inAddPointAfterMode) {
			exitAddPointAfterMode();
		}
		MeasurementToolLayer measurementLayer = getMeasurementLayer();
		if (measurementLayer != null && positionToAddPoint != -1) {
			addPointToPosition(positionToAddPoint);
			measurementLayer.exitAddPointAfterMode();
			measurementLayer.clearSelection();
			measurementLayer.refreshMap();
		}
		positionToAddPoint = -1;
	}

	private void applyAddPointBeforeMode() {
		if (inAddPointBeforeMode) {
			exitAddPointBeforeMode();
		}
		MeasurementToolLayer measurementLayer = getMeasurementLayer();
		if (measurementLayer != null && positionToAddPoint != -1) {
			addPointToPosition(positionToAddPoint);
			measurementLayer.exitAddPointBeforeMode();
			measurementLayer.clearSelection();
			measurementLayer.refreshMap();
		}
		positionToAddPoint = -1;
	}

	private void enterMovePointMode() {
		inMovePointMode = true;
		mark(View.GONE,
				R.id.ruler_icon,
				R.id.measurement_distance_text_view,
				R.id.measurement_points_text_view,
				R.id.up_down_button,
				R.id.measure_mode_controls);
		mark(View.VISIBLE,
				R.id.move_point_icon,
				R.id.move_point_text,
				R.id.selected_point_controls);
	}

	private void exitMovePointMode() {
		inMovePointMode = false;
		mark(View.GONE,
				R.id.move_point_icon,
				R.id.move_point_text,
				R.id.selected_point_controls);
		mark(View.VISIBLE,
				R.id.ruler_icon,
				R.id.measurement_distance_text_view,
				R.id.measurement_points_text_view,
				R.id.up_down_button,
				R.id.measure_mode_controls);
	}

	private void enterAddPointAfterMode() {
		inAddPointAfterMode = true;
		mark(View.GONE,
				R.id.ruler_icon,
				R.id.measurement_distance_text_view,
				R.id.measurement_points_text_view,
				R.id.up_down_button,
				R.id.measure_mode_controls);
		mark(View.VISIBLE,
				R.id.add_point_after_icon,
				R.id.add_point_after_text,
				R.id.selected_point_controls);
	}

	private void enterAddPointBeforeMode() {
		inAddPointBeforeMode = true;
		mark(View.GONE,
				R.id.ruler_icon,
				R.id.measurement_distance_text_view,
				R.id.measurement_points_text_view,
				R.id.up_down_button,
				R.id.measure_mode_controls);
		mark(View.VISIBLE,
				R.id.add_point_before_icon,
				R.id.add_point_before_text,
				R.id.selected_point_controls);
	}

	private void exitAddPointAfterMode() {
		inAddPointAfterMode = false;
		mark(View.GONE,
				R.id.add_point_after_icon,
				R.id.add_point_after_text,
				R.id.selected_point_controls);
		mark(View.VISIBLE,
				R.id.ruler_icon,
				R.id.measurement_distance_text_view,
				R.id.measurement_points_text_view,
				R.id.up_down_button,
				R.id.measure_mode_controls);
	}

	private void exitAddPointBeforeMode() {
		inAddPointBeforeMode = false;
		mark(View.GONE,
				R.id.add_point_before_icon,
				R.id.add_point_before_text,
				R.id.selected_point_controls);
		mark(View.VISIBLE,
				R.id.ruler_icon,
				R.id.measurement_distance_text_view,
				R.id.measurement_points_text_view,
				R.id.up_down_button,
				R.id.measure_mode_controls);
	}

	private void hidePointsListIfNoPoints() {
		MeasurementToolLayer measurementLayer = getMeasurementLayer();
		if (measurementLayer != null) {
			if (measurementLayer.getPointsCount() < 1) {
				disable(upDownBtn);
				if (pointsListOpened) {
					hidePointsList();
				}
			}
		}
	}

	private void addPoint() {
		MeasurementToolLayer measurementLayer = getMeasurementLayer();
		if (measurementLayer != null) {
			commandManager.execute(new AddPointCommand(measurementLayer));
			enable(undoBtn, upDownBtn);
			disable(redoBtn);
			updateText();
			adapter.notifyDataSetChanged();
			saved = false;
		}
	}

	private void addPointToPosition(int position) {
		MeasurementToolLayer measurementLayer = getMeasurementLayer();
		if (measurementLayer != null) {
			commandManager.execute(new AddPointCommand(measurementLayer, position));
			enable(undoBtn, upDownBtn);
			disable(redoBtn);
			updateText();
			adapter.notifyDataSetChanged();
			saved = false;
		}
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
			OsmandMapTileView tileView = mapActivity.getMapView();
			previousMapPosition = tileView.getMapPosition();
			if (portrait) {
				tileView.setMapPosition(MIDDLE_TOP_CONSTANT);
			} else {
				tileView.setMapPosition(LANDSCAPE_MIDDLE_RIGHT_CONSTANT);
			}
			mapActivity.refreshMap();
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
		setPreviousMapPosition();
	}

	private void showPointsListFragment() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			MeasurePointsListFragment fragment = new MeasurePointsListFragment();
			int screenHeight = AndroidUtils.getScreenHeight(mapActivity) - AndroidUtils.getStatusBarHeight(mapActivity);
			fragment.setRecyclerView(pointsRv);
			fragment.setWidth(upDownRow.getWidth());
			fragment.setHeight(screenHeight - upDownRow.getHeight());
			mapActivity.getSupportFragmentManager().beginTransaction()
					.add(R.id.fragmentContainer, fragment, MeasurePointsListFragment.TAG)
					.addToBackStack(MeasurePointsListFragment.TAG)
					.commitAllowingStateLoss();
		}
	}

	private void hidePointsListFragment() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			try {
				mapActivity.getSupportFragmentManager()
						.popBackStackImmediate(MeasurePointsListFragment.TAG, FragmentManager.POP_BACK_STACK_INCLUSIVE);
			} catch (Exception e) {
				// ignore
			}
		}
	}

	private void setPreviousMapPosition() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			mapActivity.getMapView().setMapPosition(previousMapPosition);
			mapActivity.refreshMap();
		}
	}

	private void addToGpx(MapActivity mapActivity) {
		GPXFile gpx = newGpxLine.getGpxFile();
		SelectedGpxFile selectedGpxFile = mapActivity.getMyApplication().getSelectedGpxHelper().getSelectedFileByPath(gpx.path);
		boolean showOnMap = selectedGpxFile != null;
		LineType lineType = newGpxLine.getLineType();
		saveExistingGpx(gpx, showOnMap, lineType);
	}

	private void saveAsGpxOnClick(MapActivity mapActivity) {
		final File dir = mapActivity.getMyApplication().getAppPath(IndexConstants.GPX_INDEX_DIR);
		final LayoutInflater inflater = mapActivity.getLayoutInflater();
		final View view = inflater.inflate(R.layout.save_gpx_dialog, null);
		final EditText nameEt = (EditText) view.findViewById(R.id.gpx_name_et);
		final TextView fileExistsTv = (TextView) view.findViewById(R.id.file_exists_text_view);
		final SwitchCompat showOnMapToggle = (SwitchCompat) view.findViewById(R.id.toggle_show_on_map);
		showOnMapToggle.setChecked(true);

		final String suggestedName = new SimpleDateFormat("yyyy-M-dd_HH-mm_EEE", Locale.US).format(new Date());
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
					fileExistsTv.setVisibility(View.VISIBLE);
				} else {
					fileExistsTv.setVisibility(View.INVISIBLE);
				}
				textChanged[0] = true;
			}
		});

		new AlertDialog.Builder(mapActivity)
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
						saveNewGpx(dir, fileName, showOnMapToggle.isChecked());
					}
				})
				.setNegativeButton(R.string.shared_string_cancel, null)
				.show();
	}

	private void saveNewGpx(File dir, String fileName, boolean checked) {
		saveGpx(dir, fileName, checked, null, false, null);
	}

	private void saveExistingGpx(GPXFile gpx, boolean showOnMap, LineType lineType) {
		saveGpx(null, null, showOnMap, gpx, true, lineType);
	}

	private void saveGpx(final File dir, final String fileName, final boolean showOnMap, final GPXFile gpx, final boolean openTrackActivity, final LineType lineType) {
		new AsyncTask<Void, Void, String>() {

			private ProgressDialog progressDialog;
			private File toSave;

			@Override
			protected void onPreExecute() {
				MapActivity activity = getMapActivity();
				if (activity != null) {
					progressDialog = new ProgressDialog(activity);
					progressDialog.setMessage(getString(R.string.saving_gpx_tracks));
					progressDialog.show();
				}
			}

			@Override
			protected String doInBackground(Void... voids) {
				MeasurementToolLayer measurementLayer = getMeasurementLayer();
				MapActivity activity = getMapActivity();
				if (gpx == null) {
					toSave = new File(dir, fileName);
					GPXFile gpx = new GPXFile();
					if (measurementLayer != null) {
						LinkedList<WptPt> points = measurementLayer.getMeasurementPoints();
						if (points.size() == 1) {
							gpx.points.add(points.getFirst());
						} else if (points.size() > 1) {
							Route rt = new Route();
							gpx.routes.add(rt);
							rt.points.addAll(points);
						}
					}
					if (activity != null) {
						String res = GPXUtilities.writeGpxFile(toSave, gpx, activity.getMyApplication());
						gpx.path = toSave.getAbsolutePath();
						if (showOnMap) {
							activity.getMyApplication().getSelectedGpxHelper().selectGpxFile(gpx, true, false);
						}
						return res;
					}
				} else {
					toSave = new File(gpx.path);
					if (measurementLayer != null) {
						List<WptPt> points = measurementLayer.getMeasurementPoints();
						switch (lineType) {
							case SEGMENT:
								gpx.addTrkSegment(points);
								break;
							case ROUTE_POINTS:
								gpx.addRtePts(points);
								break;
						}
					}
					if (activity != null) {
						String res = GPXUtilities.writeGpxFile(toSave, gpx, activity.getMyApplication());
						if (showOnMap) {
							SelectedGpxFile sf = activity.getMyApplication().getSelectedGpxHelper().selectGpxFile(gpx, true, false);
							if (sf != null) {
								if (lineType == LineType.SEGMENT) {
									sf.processPoints();
								}
							}
						}
						return res;
					}
				}
				return null;
			}

			@Override
			protected void onPostExecute(String warning) {
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
						}
					} else {
						Toast.makeText(activity, warning, Toast.LENGTH_LONG).show();
					}
				}
			}
		}.execute();
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		exitMeasurementMode();
		adapter.setAdapterListener(null);
		if (pointsListOpened) {
			setPreviousMapPosition();
		}
		MeasurementToolLayer layer = getMeasurementLayer();
		if (layer != null) {
			layer.setOnSingleTapListener(null);
		}
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

	private Drawable getContentIcon(@DrawableRes int id) {
		return iconsCache.getIcon(id, nightMode ? R.color.ctx_menu_info_text_dark : R.color.icon_color);
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
			pointsTv.setText((portrait ? pointsSt + ": " : "") + measurementLayer.getPointsCount());
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

			measurementLayer.getMeasurementPoints().clear();
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

	public void showQuitDialog(boolean hidePointsListFirst) {
		final MapActivity mapActivity = getMapActivity();
		MeasurementToolLayer measurementLayer = getMeasurementLayer();
		if (mapActivity != null && measurementLayer != null) {
			if (pointsListOpened && hidePointsListFirst) {
				hidePointsList();
				return;
			}
			if (measurementLayer.getPointsCount() < 1 || saved) {
				dismiss(mapActivity);
				return;
			}
			new AlertDialog.Builder(mapActivity)
					.setTitle(getString(R.string.are_you_sure))
					.setMessage(getString(R.string.unsaved_changes_will_be_lost))
					.setPositiveButton(R.string.shared_string_ok, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							dismiss(mapActivity);
						}
					})
					.setNegativeButton(R.string.shared_string_cancel, null)
					.show();
		}
	}

	private void dismiss(MapActivity mapActivity) {
		try {
			if (inMovePointMode) {
				exitMovePointMode();
			}
			if (inAddPointAfterMode) {
				exitAddPointAfterMode();
			}
			if (inAddPointBeforeMode) {
				exitAddPointBeforeMode();
			}
			MeasurementToolLayer measurementToolLayer = getMeasurementLayer();
			if (measurementToolLayer != null) {
				measurementToolLayer.clearSelection();
				if (measurementToolLayer.isInMovePointMode()) {
					measurementToolLayer.exitMovePointMode();
				}
				if (measurementToolLayer.isInAddPointAfterMode()) {
					measurementToolLayer.exitAddPointAfterMode();
				}
				if (measurementToolLayer.isInAddPointBeforeMode()) {
					measurementToolLayer.exitAddPointBeforeMode();
				}
			}
			if (newGpxLine != null) {
				GPXFile gpx = newGpxLine.getGpxFile();
				Intent newIntent = new Intent(mapActivity, mapActivity.getMyApplication().getAppCustomization().getTrackActivity());
				newIntent.putExtra(TrackActivity.TRACK_FILE_NAME, gpx.path);
				newIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(newIntent);
			}
			mapActivity.getSupportFragmentManager().popBackStackImmediate(TAG, FragmentManager.POP_BACK_STACK_INCLUSIVE);
		} catch (Exception e) {
			// ignore
		}
	}

	private class MeasurementToolBarController extends TopToolbarController {

		MeasurementToolBarController(NewGpxLine newGpxLine) {
			super(MapInfoWidgetsFactory.TopToolbarControllerType.MEASUREMENT_TOOL);
			setBackBtnIconClrIds(0, 0);
			setTitleTextClrIds(R.color.primary_text_dark, R.color.primary_text_dark);
			setDescrTextClrIds(R.color.primary_text_dark, R.color.primary_text_dark);
			setBgIds(R.drawable.gradient_toolbar, R.drawable.gradient_toolbar,
					R.drawable.gradient_toolbar, R.drawable.gradient_toolbar);
			setCloseBtnVisible(false);
			if (newGpxLine != null) {
				setSaveViewVisible(true);
			}
			setBackBtnIconIds(R.drawable.ic_action_remove_dark, R.drawable.ic_action_remove_dark);
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
	}
}
