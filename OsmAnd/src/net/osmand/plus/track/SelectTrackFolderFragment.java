package net.osmand.plus.track;

import static net.osmand.plus.configmap.tracks.TracksAdapter.TYPE_NO_TRACKS;
import static net.osmand.plus.configmap.tracks.TracksAdapter.TYPE_SORT_TRACKS;
import static net.osmand.plus.importfiles.ImportHelper.IMPORT_FILE_REQUEST;
import static net.osmand.plus.importfiles.OnSuccessfulGpxImport.OPEN_GPX_CONTEXT_MENU;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.CallbackWithObject;
import net.osmand.Location;
import net.osmand.data.LatLon;
import net.osmand.shared.gpx.GpxFile;
import net.osmand.plus.OsmAndLocationProvider;
import net.osmand.plus.OsmAndLocationProvider.OsmAndCompassListener;
import net.osmand.plus.OsmAndLocationProvider.OsmAndLocationListener;
import net.osmand.plus.R;
import net.osmand.plus.base.BaseOsmAndDialogFragment;
import net.osmand.plus.configmap.tracks.SortByBottomSheet;
import net.osmand.plus.configmap.tracks.TrackItem;
import net.osmand.plus.configmap.tracks.TrackItemsContainer;
import net.osmand.plus.configmap.tracks.TrackTab;
import net.osmand.plus.configmap.tracks.TrackTabType;
import net.osmand.plus.configmap.tracks.TracksAdapter;
import net.osmand.plus.configmap.tracks.TracksAdapter.ItemVisibilityCallback;
import net.osmand.plus.configmap.tracks.TracksComparator;
import net.osmand.plus.configmap.tracks.viewholders.EmptyTracksViewHolder;
import net.osmand.plus.configmap.tracks.viewholders.SortTracksViewHolder.SortTracksListener;
import net.osmand.plus.configmap.tracks.viewholders.TrackViewHolder.TrackSelectionListener;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.helpers.IntentHelper;
import net.osmand.plus.importfiles.GpxImportListener;
import net.osmand.plus.importfiles.ImportHelper;
import net.osmand.plus.importfiles.MultipleTracksImportListener;
import net.osmand.plus.importfiles.OnSuccessfulGpxImport;
import net.osmand.plus.settings.enums.TracksSortMode;
import net.osmand.plus.track.SelectTrackTabsFragment.GpxDataItemSelectionListener;
import net.osmand.plus.track.data.TrackFolder;
import net.osmand.plus.track.helpers.GpxSelectionHelper;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class SelectTrackFolderFragment extends BaseOsmAndDialogFragment implements OsmAndCompassListener,
		OsmAndLocationListener, TrackItemsContainer, TrackSelectionListener, SortTracksListener, EmptyTracksViewHolder.EmptyTracksListener {

	public static final String TAG = SelectTrackFolderFragment.class.getSimpleName();

	private ImportHelper importHelper;
	private View view;
	private TracksAdapter adapter;
	private RecyclerView recyclerView;
	private TextView toolbarTitle;

	private Location location;
	private Float heading;
	private boolean locationUpdateStarted;
	private boolean compassUpdateAllowed = true;
	private TracksSortMode sortMode;
	private TrackFolder baseTrackFolder;
	private TrackFolder currentTrackFolder;
	private Object fileSelectionListener;
	private ItemVisibilityCallback itemVisibilityCallback;

	@Override
	protected boolean isUsedOnMap() {
		return true;
	}

	@NonNull
	@Override
	public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
		Activity activity = requireActivity();
		int themeId = nightMode ? R.style.OsmandDarkTheme_DarkActionbar : R.style.OsmandLightTheme_DarkActionbar_LightStatusBar;
		Dialog dialog = new Dialog(activity, themeId);
		Window window = dialog.getWindow();
		if (window != null) {
			if (!settings.DO_NOT_USE_ANIMATIONS.get()) {
				window.getAttributes().windowAnimations = R.style.Animations_Alpha;
			}
			window.setStatusBarColor(ContextCompat.getColor(app, getStatusBarColorId()));
		}
		return dialog;
	}

	@ColorRes
	public int getStatusBarColorId() {
		AndroidUiHelper.setStatusBarContentColor(getView(), nightMode);
		return nightMode ? R.color.status_bar_main_dark : R.color.activity_background_color_light;
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		importHelper = app.getImportHelper();
		updateNightMode();
		view = themedInflater.inflate(R.layout.select_track_folder_fragment, container, false);
		view.setBackgroundColor(ContextCompat.getColor(app, nightMode ? R.color.activity_background_color_dark : R.color.list_background_color_light));

		toolbarTitle = view.findViewById(R.id.toolbar_title);
		recyclerView = view.findViewById(R.id.recycler_view);
		recyclerView.setLayoutManager(new LinearLayoutManager(app));
		recyclerView.setItemAnimator(null);
		recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
			@Override
			public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
				super.onScrollStateChanged(recyclerView, newState);
				compassUpdateAllowed = newState == RecyclerView.SCROLL_STATE_IDLE;
			}
		});

		setupToolbar();
		setupAdapter();
		updateToolBarTitle();
		return view;
	}

	private void setupToolbar() {
		View appbar = view.findViewById(R.id.appbar);
		ViewCompat.setElevation(appbar, 5.0f);
		appbar.setBackgroundColor(ContextCompat.getColor(app, nightMode ? R.color.app_bar_main_dark : R.color.card_and_list_background_light));

		Toolbar toolbar = view.findViewById(R.id.toolbar);
		toolbar.findViewById(R.id.back_button).setOnClickListener(v -> {
			if (currentTrackFolder.getParentFolder() == baseTrackFolder) {
				dismiss();
			} else {
				currentTrackFolder = currentTrackFolder.getParentFolder();
				updateAdapter();
			}
		});
	}

	private void setupAdapter() {
		BaseTracksTabsFragment fragment = (BaseTracksTabsFragment) getTargetFragment();
		if (fragment != null) {
			adapter = new TracksAdapter(requireContext(), getUpdatedTrackTab(), fragment, nightMode);
			adapter.setSelectionMode(fragment.selectionMode());
			adapter.setSelectTrackMode(true);
			adapter.setTrackSelectionListener(this);
			adapter.setSortTracksListener(this);
			adapter.setEmptyTracksListener(this);
		}
		recyclerView.setAdapter(adapter);
	}

	private void updateAdapter() {
		if (adapter != null) {
			adapter.setTrackTab(getUpdatedTrackTab());
		}
		updateToolBarTitle();
	}

	private TrackTab getUpdatedTrackTab() {
		TrackTab trackTab = new TrackTab(TrackTabType.FOLDERS);
		List<TrackFolder> subFolders = currentTrackFolder.getSubFolders();
		List<TrackItem> trackItems = currentTrackFolder.getTrackItems();
		if (Algorithms.isEmpty(subFolders) && Algorithms.isEmpty(trackItems)) {
			trackTab.items.add(TYPE_NO_TRACKS);
		} else {
			trackTab.items.add(TYPE_SORT_TRACKS);
			trackTab.items.addAll(currentTrackFolder.getSubFolders());
			trackTab.items.addAll(currentTrackFolder.getTrackItems());
			trackTab.setSortMode(sortMode);
		}
		if (itemVisibilityCallback != null) {
			List<Object> items = new ArrayList<>();
			for (Object object : trackTab.items) {
				if (object instanceof TrackItem && !itemVisibilityCallback.shouldShowItem((TrackItem) object)) {
					items.add(object);
				}
			}
			trackTab.items.removeAll(items);
		}
		return trackTab;
	}

	private void updateToolBarTitle() {
		toolbarTitle.setText(currentTrackFolder.getDirFile().getName());
	}

	@Override
	public void updateItems(@NonNull Set<TrackItem> trackItems) {
		if (adapter != null) {
			adapter.updateItems(trackItems);
		}
	}

	@Override
	public void updateContent() {
		updateAdapter();
	}

	@Override
	public void onResume() {
		super.onResume();
		startLocationUpdate();
	}

	@Override
	public void onPause() {
		super.onPause();
		stopLocationUpdate();
	}

	@Override
	public void updateLocation(Location location) {
		if (!MapUtils.areLatLonEqual(this.location, location)) {
			this.location = location;
			updateLocationUi();
		}
	}

	@Override
	public void updateCompassValue(float value) {
		// 99 in next line used to one-time initialize arrows (with reference vs. fixed-north direction)
		// on non-compass devices
		float lastHeading = heading != null ? heading : 99;
		heading = value;
		if (Math.abs(MapUtils.degreesDiff(lastHeading, heading)) > 5) {
			updateLocationUi();
		} else {
			heading = lastHeading;
		}
	}

	private void updateLocationUi() {
		if (compassUpdateAllowed && adapter != null) {
			app.runInUIThread(() -> {
				if (location == null) {
					location = app.getLocationProvider().getLastKnownLocation();
				}
				adapter.notifyDataSetChanged();
			});
		}
	}

	public void startLocationUpdate() {
		if (!locationUpdateStarted) {
			locationUpdateStarted = true;
			OsmAndLocationProvider locationProvider = app.getLocationProvider();
			locationProvider.removeCompassListener(locationProvider.getNavigationInfo());
			locationProvider.addCompassListener(this);
			locationProvider.addLocationListener(this);
			updateLocationUi();
		}
	}

	public void stopLocationUpdate() {
		if (locationUpdateStarted) {
			locationUpdateStarted = false;
			OsmAndLocationProvider locationProvider = app.getLocationProvider();
			locationProvider.removeLocationListener(this);
			locationProvider.removeCompassListener(this);
			locationProvider.addCompassListener(locationProvider.getNavigationInfo());
		}
	}

	@Override
	public void onTrackFolderSelected(@NonNull TrackFolder trackFolder) {
		currentTrackFolder = trackFolder;
		updateAdapter();
	}

	@Override
	public void onTrackItemsSelected(@NonNull Set<TrackItem> trackItems, boolean selected) {
		TrackItem firstTrackItem = trackItems.iterator().next();
		if (fileSelectionListener instanceof CallbackWithObject) {
			((CallbackWithObject<String>) fileSelectionListener).processResult(firstTrackItem.getPath());
		} else if (fileSelectionListener instanceof SelectTrackTabsFragment.GpxFileSelectionListener) {
			GpxSelectionHelper.getGpxFile(requireActivity(), firstTrackItem.getFile(), true, result -> {
				((SelectTrackTabsFragment.GpxFileSelectionListener) fileSelectionListener).onSelectGpxFile(result);
				return true;
			});
		} else if (fileSelectionListener instanceof GpxDataItemSelectionListener) {
			((GpxDataItemSelectionListener) fileSelectionListener).onSelectGpxDataItem(firstTrackItem.getDataItem());
		}
		dismiss();
		Fragment selectTrackFragment = requireActivity().getSupportFragmentManager().findFragmentByTag(SelectTrackTabsFragment.TAG);
		if (selectTrackFragment instanceof SelectTrackTabsFragment) {
			((SelectTrackTabsFragment) selectTrackFragment).dismiss();
		}
	}

	@Override
	public void showSortByDialog() {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			FragmentManager manager = activity.getSupportFragmentManager();
			SortByBottomSheet.showInstance(manager, getTracksSortMode(), this, isUsedOnMap());
		}
	}

	@NonNull
	@Override
	public TracksSortMode getTracksSortMode() {
		return sortMode;
	}

	@Override
	public void setTracksSortMode(@NonNull TracksSortMode sortMode, boolean sortSubFolders) {
		this.sortMode = sortMode;
		TrackTab trackTab = adapter.getTrackTab();
		trackTab.setSortMode(sortMode);
		LatLon latLon = app.getMapViewTrackingUtilities().getDefaultLocation();
		Collections.sort(trackTab.items, new TracksComparator(trackTab, latLon));
		adapter.notifyDataSetChanged();
	}

	public static void showInstance(@NonNull FragmentManager manager, @NonNull BaseTracksTabsFragment selectTrackFragment, @Nullable TracksSortMode sortMode,
									@Nullable Object fileSelectionListener, @NonNull TrackFolder baseTrackFolder, @NonNull TrackFolder currentTrackFolder,
									@Nullable ItemVisibilityCallback itemVisibilityCallback) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			SelectTrackFolderFragment fragment = new SelectTrackFolderFragment();
			fragment.sortMode = sortMode;
			fragment.baseTrackFolder = baseTrackFolder;
			fragment.currentTrackFolder = currentTrackFolder;
			fragment.fileSelectionListener = fileSelectionListener;
			fragment.itemVisibilityCallback = itemVisibilityCallback;
			fragment.setTargetFragment(selectTrackFragment, 0);
			fragment.setRetainInstance(true);
			fragment.show(manager, TAG);
		}
	}

	@Override
	public void importTracks() {
		Intent intent = ImportHelper.getImportFileIntent();
		intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
		AndroidUtils.startActivityForResultIfSafe(this, intent, IMPORT_FILE_REQUEST);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == IMPORT_FILE_REQUEST && resultCode == Activity.RESULT_OK) {
			if (data != null) {
				List<Uri> filesUri = IntentHelper.getIntentUris(data);
				if (!Algorithms.isEmpty(filesUri)) {
					int filesSize = filesUri.size();
					boolean singleTrack = filesSize == 1;
					File dir;
					if (currentTrackFolder != null) {
						dir = currentTrackFolder.getDirFile();
					} else {
						dir = ImportHelper.getGpxDestinationDir(app, true);
					}
					OnSuccessfulGpxImport onGpxImport = singleTrack ? OPEN_GPX_CONTEXT_MENU : null;

					importHelper.setGpxImportListener(getGpxImportListener(filesSize));
					importHelper.handleGpxFilesImport(filesUri, dir, onGpxImport, true, singleTrack);
				}
			}
		} else {
			super.onActivityResult(requestCode, resultCode, data);
		}
	}

	@NonNull
	private GpxImportListener getGpxImportListener(int filesSize) {
		return new MultipleTracksImportListener(filesSize) {

			@Override
			public void onImportStarted() {}

			@Override
			public void onImportFinished() {
				importHelper.setGpxImportListener(null);
			}

			@Override
			public void onSaveComplete(boolean success, GpxFile gpxFile) {
				if (isAdded() && success) {
					addTrackItem(new TrackItem(new File(gpxFile.getPath())));
				}
				super.onSaveComplete(success, gpxFile);
			}
		};
	}

	protected void addTrackItem(@NonNull TrackItem item) {
		currentTrackFolder.getTrackItems().add(item);
		updateContent();
	}
}