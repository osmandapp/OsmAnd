package net.osmand.plus.importfiles.ui;

import static net.osmand.IndexConstants.GPX_IMPORT_DIR;
import static net.osmand.IndexConstants.GPX_INDEX_DIR;
import static net.osmand.plus.myplaces.MyPlacesActivity.GPX_TAB;
import static net.osmand.plus.myplaces.MyPlacesActivity.TAB_ID;
import static net.osmand.plus.myplaces.tracks.dialogs.AvailableTracksFragment.SELECTED_FOLDER_KEY;

import android.app.Dialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.PlatformUtil;
import net.osmand.shared.gpx.GpxFile;
import net.osmand.shared.gpx.primitives.WptPt;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseOsmAndDialogFragment;
import net.osmand.plus.configmap.tracks.TracksTabsFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.importfiles.GpxImportListener;
import net.osmand.plus.importfiles.ImportHelper;
import net.osmand.plus.importfiles.SaveImportedGpxListener;
import net.osmand.plus.importfiles.tasks.CollectTracksTask;
import net.osmand.plus.importfiles.tasks.CollectTracksTask.CollectTracksListener;
import net.osmand.plus.importfiles.tasks.SaveGpxAsyncTask;
import net.osmand.plus.importfiles.tasks.SaveTracksTask;
import net.osmand.plus.importfiles.ui.ExitImportBottomSheet.OnExitConfirmedListener;
import net.osmand.plus.importfiles.ui.ImportTracksAdapter.ImportTracksListener;
import net.osmand.plus.importfiles.ui.SelectPointsFragment.PointsSelectionListener;
import net.osmand.plus.importfiles.ui.SelectTrackDirectoryBottomSheet.FolderSelectionListener;
import net.osmand.plus.myplaces.MyPlacesActivity;
import net.osmand.plus.myplaces.tracks.MapDrawParams;
import net.osmand.plus.myplaces.tracks.dialogs.AddNewTrackFolderBottomSheet;
import net.osmand.plus.myplaces.tracks.dialogs.AddNewTrackFolderBottomSheet.OnTrackFolderAddListener;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.widgets.dialogbutton.DialogButton;
import net.osmand.plus.widgets.dialogbutton.DialogButtonType;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ImportTracksFragment extends BaseOsmAndDialogFragment implements OnExitConfirmedListener,
		FolderSelectionListener, OnTrackFolderAddListener, ImportTracksListener, PointsSelectionListener {

	public static final String TAG = ImportTracksFragment.class.getSimpleName();

	private static final Log log = PlatformUtil.getLog(ImportTracksFragment.class);

	private static final String SELECTED_DIRECTORY_KEY = "selected_directory_key";

	private final List<ImportTrackItem> trackItems = new ArrayList<>();
	private final Set<ImportTrackItem> selectedTracks = new HashSet<>();

	private GpxFile gpxFile;
	private String fileName;
	private String selectedFolder;
	private long fileSize;

	private GpxImportListener importListener;

	private SaveGpxAsyncTask saveAsOneTrackTask;
	private SaveTracksTask saveTracksTask;
	private CollectTracksTask collectTracksTask;

	private ImportTracksAdapter adapter;

	private DialogButton importButton;
	private DialogButton selectAllButton;
	private View buttonsContainer;
	private View progressContainer;
	private TextView progressTitle;
	private RecyclerView recyclerView;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (savedInstanceState == null) {
			collectTracks();
		} else {
			selectedFolder = savedInstanceState.getString(SELECTED_DIRECTORY_KEY);
		}
		if (Algorithms.isEmpty(selectedFolder)) {
			selectedFolder = app.getAppPath(GPX_IMPORT_DIR).getAbsolutePath();
		}

		FragmentActivity activity = requireActivity();
		activity.getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
			public void handleOnBackPressed() {
				showExitDialog();
			}
		});
	}

	@Override
	protected boolean isUsedOnMap() {
		return true;
	}

	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		return new Dialog(requireContext(), getTheme()) {
			@Override
			public void onBackPressed() {
				showExitDialog();
			}
		};
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		updateNightMode();
		View view = themedInflater.inflate(R.layout.fragment_import_tracks, container, false);

		setupToolbar(view);
		setupButtons(view);
		setupProgress(view);
		setupRecyclerView(view);
		updateButtonsState();
		updateProgress();

		return view;
	}

	protected void setupToolbar(@NonNull View view) {
		View appbar = view.findViewById(R.id.appbar);
		ViewCompat.setElevation(appbar, 5.0f);

		TextView title = appbar.findViewById(R.id.toolbar_title);
		ImageView closeButton = appbar.findViewById(R.id.close_button);

		title.setText(R.string.import_tracks);
		closeButton.setOnClickListener(v -> showExitDialog());
		closeButton.setImageDrawable(getIcon(R.drawable.ic_action_close));
	}

	private void setupProgress(@NonNull View view) {
		progressContainer = view.findViewById(R.id.progress_container);
		progressTitle = progressContainer.findViewById(R.id.title);
	}

	private void updateProgress() {
		if (!isAdded()) {
			return;
		}
		boolean savingTracks = isSavingTracks();
		boolean collectingTracks = isCollectingTracks();
		if (progressTitle != null) {
			if (collectingTracks) {
				progressTitle.setText(R.string.shared_string_reading_file);
			} else if (savingTracks) {
				progressTitle.setText(getString(R.string.importing_from, fileName));
			}
		}
		boolean progressVisible = collectingTracks | savingTracks;
		AndroidUiHelper.updateVisibility(recyclerView, !progressVisible);
		AndroidUiHelper.updateVisibility(buttonsContainer, !progressVisible);
		AndroidUiHelper.updateVisibility(progressContainer, progressVisible);
	}

	private void setupButtons(@NonNull View view) {
		buttonsContainer = view.findViewById(R.id.control_buttons);
		View container = buttonsContainer.findViewById(R.id.buttons_container);
		container.setBackgroundColor(ColorUtilities.getListBgColor(app, nightMode));

		importButton = container.findViewById(R.id.right_bottom_button);
		importButton.setOnClickListener(v -> importTracks());

		selectAllButton = container.findViewById(R.id.dismiss_button);
		selectAllButton.setOnClickListener(v -> {
			if (selectedTracks.containsAll(trackItems)) {
				selectedTracks.clear();
			} else {
				selectedTracks.addAll(trackItems);
			}
			updateButtonsState();
			adapter.notifyDataSetChanged();
		});
		AndroidUiHelper.updateVisibility(importButton, true);
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.buttons_divider), true);

		TextView textView = selectAllButton.findViewById(R.id.button_text);
		FrameLayout.LayoutParams params = (LayoutParams) textView.getLayoutParams();
		params.gravity = Gravity.START | Gravity.CENTER_VERTICAL;
		textView.setLayoutParams(params);
	}

	protected void updateButtonsState() {
		if (!isAdded()) {
			return;
		}
		String allTracks = String.valueOf(trackItems.size());
		String selectedItems = String.valueOf(selectedTracks.size());
		String count = getString(R.string.ltr_or_rtl_combine_via_slash, selectedItems, allTracks);
		String importText = getString(R.string.ltr_or_rtl_combine_via_space, getString(R.string.shared_string_import), count);

		importButton.setEnabled(!selectedTracks.isEmpty());
		importButton.setButtonType(DialogButtonType.PRIMARY);
		importButton.setTitle(importText);

		boolean allSelected = selectedTracks.containsAll(trackItems);
		String selectAllText = getString(allSelected ? R.string.shared_string_deselect_all : R.string.shared_string_select_all);
		selectAllButton.setButtonType(DialogButtonType.SECONDARY_ACTIVE);
		selectAllButton.setTitle(selectAllText);
		selectAllButton.setIconId(R.drawable.ic_action_deselect_all);

		TextView textView = selectAllButton.findViewById(R.id.button_text);
		textView.setCompoundDrawablePadding(AndroidUtils.dpToPx(app, 12));
	}

	private void setupRecyclerView(@NonNull View view) {
		adapter = new ImportTracksAdapter(app, gpxFile, fileName, nightMode);
		adapter.setListener(this);
		adapter.setDrawParams(getTracksDrawParams());
		adapter.setSelectedFolder(selectedFolder);
		adapter.updateItems(trackItems, selectedTracks);

		recyclerView = view.findViewById(R.id.recycler_view);
		recyclerView.setLayoutManager(new LinearLayoutManager(view.getContext()));
		recyclerView.setAdapter(adapter);
	}

	@Override
	public void onResume() {
		super.onResume();
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			mapActivity.disableDrawer();
		}
		app.getOsmandMap().getMapLayers().getMapVectorLayer().setVisible(false);
	}

	@Override
	public void onPause() {
		super.onPause();
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			mapActivity.enableDrawer();
		}
		app.getOsmandMap().getMapLayers().getMapVectorLayer().setVisible(true);
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		for (ImportTrackItem item : trackItems) {
			if (item.bitmapDrawer != null) {
				item.bitmapDrawer.setDrawingAllowed(false);
			}
			item.bitmap = null;
			item.bitmapDrawer = null;
		}
	}

	private boolean isSavingTracks() {
		boolean savingAsOneTrack = saveAsOneTrackTask != null && saveAsOneTrackTask.getStatus() == Status.RUNNING;
		boolean savingMultiTracks = saveTracksTask != null && saveTracksTask.getStatus() == Status.RUNNING;
		return savingAsOneTrack || savingMultiTracks;
	}

	private boolean isCollectingTracks() {
		return collectTracksTask != null && collectTracksTask.getStatus() == Status.RUNNING;
	}

	private void importTracks() {
		File folder = new File(selectedFolder);
		SaveImportedGpxListener saveGpxListener = getSaveGpxListener(() -> saveTracksTask = null);
		saveTracksTask = new SaveTracksTask(app, new ArrayList<>(selectedTracks), folder, saveGpxListener);
		saveTracksTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	private void collectTracks() {
		collectTracksTask = new CollectTracksTask(app, gpxFile, fileName, getCollectTracksListener());
		collectTracksTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	@NonNull
	private MapDrawParams getTracksDrawParams() {
		DisplayMetrics metrics = new DisplayMetrics();
		AndroidUtils.getDisplay(requireContext()).getMetrics(metrics);

		int width = metrics.widthPixels - AndroidUtils.dpToPx(app, 32);
		int height = getResources().getDimensionPixelSize(R.dimen.track_image_height);

		return new MapDrawParams(metrics.density, width, height);
	}

	@Override
	public void onImportAsOneTrackClicked() {
		String existingFilePath = ImportHelper.getExistingFilePath(app, fileName, fileSize);
		if (existingFilePath != null) {
			app.showToastMessage(R.string.file_already_imported);
			dismissAndOpenTracks();
		} else {
			File destinationDir = new File(selectedFolder);
			SaveImportedGpxListener saveGpxListener = getSaveGpxListener(() -> saveAsOneTrackTask = null);
			saveAsOneTrackTask = new SaveGpxAsyncTask(app, gpxFile, destinationDir, fileName, saveGpxListener, false);
			saveAsOneTrackTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		}
	}

	@Override
	public void onFolderSelected(@NonNull String folderPath) {
		selectedFolder = folderPath;
		adapter.setSelectedFolder(selectedFolder);
	}

	@Override
	public void onFolderSelected(@NonNull File folder) {
		selectedFolder = folder.getAbsolutePath();
		adapter.setSelectedFolder(selectedFolder);
		adapter.notifyItemChanged(adapter.getItemCount() - 1);
	}

	@Override
	public void onTrackFolderAdd(String folderName) {
		File gpxDir = app.getAppPath(GPX_INDEX_DIR);
		File folder = new File(gpxDir, folderName);

		folder.mkdirs();
		onFolderSelected(folder);
	}

	@Override
	public void onAddFolderSelected() {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			String name = Algorithms.getFileNameWithoutExtension(fileName);
			FragmentManager manager = activity.getSupportFragmentManager();
			AddNewTrackFolderBottomSheet.showInstance(manager, null, name, this, true);
		}
	}

	@Override
	public void onFoldersListSelected() {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			String name = Algorithms.getFileNameWithoutExtension(fileName);
			FragmentManager manager = activity.getSupportFragmentManager();
			SelectTrackDirectoryBottomSheet.showInstance(manager, selectedFolder, name, this, true);
		}
	}

	@Override
	public void onTrackItemPointsSelected(@NonNull ImportTrackItem item) {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			SelectPointsFragment.showInstance(activity.getSupportFragmentManager(), item, gpxFile.getPointsList(), this);
		}
	}

	@Override
	public void onPointsSelected(@NonNull ImportTrackItem trackItem, @NonNull Set<WptPt> selectedPoints) {
		trackItem.selectedPoints.clear();
		trackItem.selectedPoints.addAll(selectedPoints);

		int position = adapter.getItemPosition(trackItem);
		if (position != -1) {
			adapter.notifyItemChanged(position);
		}
	}

	@Override
	public void onExitConfirmed() {
		dismiss();
	}

	private void showExitDialog() {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			ExitImportBottomSheet.showInstance(activity.getSupportFragmentManager(), this, true);
		}
	}

	@Override
	public void onTrackItemSelected(@NonNull ImportTrackItem item, boolean selected) {
		if (selected) {
			selectedTracks.add(item);
		} else {
			selectedTracks.remove(item);
		}
		updateButtonsState();
	}

	@NonNull
	private CollectTracksListener getCollectTracksListener() {
		return new CollectTracksListener() {

			@Override
			public void tracksCollectionStarted() {
				updateProgress();
			}

			@Override
			public void tracksCollectionFinished(List<ImportTrackItem> items) {
				collectTracksTask = null;
				trackItems.addAll(items);
				selectedTracks.addAll(items);
				adapter.updateItems(trackItems, selectedTracks);

				updateProgress();
				updateButtonsState();
			}
		};
	}

	@NonNull
	private SaveImportedGpxListener getSaveGpxListener(@NonNull Runnable clearTaskCallback) {
		return new SaveImportedGpxListener() {

			@Override
			public void onGpxSavingStarted() {
				updateProgress();
			}

			@Override
			public void onGpxSaved(@Nullable String error, @NonNull GpxFile gpxFile) {
				app.runInUIThread(() -> {
					if (importListener != null) {
						importListener.onSaveComplete(error == null, gpxFile);
					}
				});
			}

			@Override
			public void onGpxSavingFinished(@NonNull List<String> warnings) {
				clearTaskCallback.run();
				updateProgress();
				dismissAndOpenTracks();
			}
		};
	}

	private void dismissAndOpenTracks() {
		FragmentActivity activity = getActivity();
		TracksTabsFragment tracksFragment = null;
		if (activity instanceof MapActivity) {
			tracksFragment = ((MapActivity) activity).getFragmentsHelper().getFragment(TracksTabsFragment.TAG);
		}
		if (!(activity instanceof MyPlacesActivity) && tracksFragment == null) {
			openTracksTabInMyPlaces();
		}
		dismissAllowingStateLoss();
	}

	private void openTracksTabInMyPlaces() {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			String folder = GPX_INDEX_DIR.equals(selectedFolder + "/") ? "" : selectedFolder;
			Bundle bundle = new Bundle();
			bundle.putInt(TAB_ID, GPX_TAB);
			bundle.putString(SELECTED_FOLDER_KEY, folder);

			Intent intent = new Intent(app, app.getAppCustomization().getMyPlacesActivity());
			intent.putExtra(MapActivity.INTENT_PARAMS, bundle);
			activity.startActivity(intent);
		}
	}

	@Nullable
	public MapActivity getMapActivity() {
		FragmentActivity activity = getActivity();
		if (activity instanceof MapActivity) {
			return (MapActivity) activity;
		} else {
			return null;
		}
	}

	public static void showInstance(@NonNull FragmentManager manager,
	                                @NonNull GpxFile gpxFile,
	                                @NonNull String fileName,
	                                @Nullable String selectedFolder,
	                                @Nullable GpxImportListener importListener,
	                                long fileSize) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			ImportTracksFragment fragment = new ImportTracksFragment();
			fragment.gpxFile = gpxFile;
			fragment.fileName = fileName;
			fragment.selectedFolder = selectedFolder;
			fragment.fileSize = fileSize;
			fragment.importListener = importListener;
			fragment.setRetainInstance(true);
			fragment.show(manager, TAG);
		}
	}
}