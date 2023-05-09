package net.osmand.plus.myplaces.tracks.dialogs;

import static net.osmand.IndexConstants.GPX_INDEX_DIR;
import static net.osmand.plus.configmap.tracks.TracksFragment.OPEN_TRACKS_TAB;
import static net.osmand.plus.myplaces.MyPlacesActivity.GPX_TAB;
import static net.osmand.plus.myplaces.MyPlacesActivity.TAB_ID;
import static net.osmand.plus.myplaces.tracks.dialogs.TrackFoldersAdapter.TYPE_SORT_TRACKS;
import static net.osmand.plus.myplaces.tracks.dialogs.TrackFoldersAdapter.TYPE_VISIBLE_TRACKS;

import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.configmap.tracks.SortByBottomSheet;
import net.osmand.plus.configmap.tracks.TrackFolderLoaderTask;
import net.osmand.plus.configmap.tracks.TrackFolderLoaderTask.LoadTracksListener;
import net.osmand.plus.configmap.tracks.TrackItem;
import net.osmand.plus.configmap.tracks.TrackItemsFragment;
import net.osmand.plus.configmap.tracks.TrackTabType;
import net.osmand.plus.configmap.tracks.viewholders.SortTracksViewHolder.SortTracksListener;
import net.osmand.plus.configmap.tracks.viewholders.TrackViewHolder.TrackSelectionListener;
import net.osmand.plus.myplaces.MyPlacesActivity;
import net.osmand.plus.myplaces.tracks.GpxActionsHelper;
import net.osmand.plus.myplaces.tracks.dialogs.viewholders.TrackFolderViewHolder.FolderSelectionListener;
import net.osmand.plus.myplaces.tracks.dialogs.viewholders.VisibleTracksViewHolder.VisibleTracksListener;
import net.osmand.plus.myplaces.tracks.tasks.DeleteGpxFilesTask.GpxFilesDeletionListener;
import net.osmand.plus.settings.enums.TracksSortMode;
import net.osmand.plus.track.data.GPXInfo;
import net.osmand.plus.track.data.TrackFolder;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.FileUtils;
import net.osmand.plus.utils.UiUtilities;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class AvailableTracksFragment extends BaseOsmAndFragment implements LoadTracksListener,
		SortTracksListener, FolderSelectionListener, VisibleTracksListener, GpxFilesDeletionListener, TrackSelectionListener {

	public static final String TAG = TrackItemsFragment.class.getSimpleName();

	private OsmandApplication app;
	private GpxActionsHelper gpxActionsHelper;

	private TrackFolder trackFolder;
	private TrackFolderLoaderTask asyncLoader;
	private TrackFoldersAdapter adapter;

	private boolean nightMode;


	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = requireMyApplication();
		gpxActionsHelper = new GpxActionsHelper(requireActivity(), nightMode);
		gpxActionsHelper.setTargetFragment(this);
		gpxActionsHelper.setDeletionListener(this);
	}

	@NonNull
	protected List<Object> getAdapterItems() {
		List<Object> items = new ArrayList<>();
		items.add(TYPE_SORT_TRACKS);
		items.add(TYPE_VISIBLE_TRACKS);
		items.addAll(trackFolder.getSubFolders());
		items.addAll(trackFolder.getTrackItems());
		return items;
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		FragmentActivity activity = requireActivity();
		LayoutInflater themedInflater = UiUtilities.getInflater(activity, nightMode);
		View view = themedInflater.inflate(R.layout.recycler_view_fragment, container, false);
		view.setBackgroundColor(ColorUtilities.getListBgColor(app, nightMode));

		adapter = new TrackFoldersAdapter(app, nightMode);
		adapter.setSortTracksListener(this);
		adapter.setVisibleTracksListener(this);
		adapter.setTrackSelectionListener(this);
		adapter.setFolderSelectionListener(this);

		RecyclerView recyclerView = view.findViewById(R.id.recycler_view);
		recyclerView.setLayoutManager(new LinearLayoutManager(app));
		recyclerView.setItemAnimator(null);
		recyclerView.setAdapter(adapter);

		if (trackFolder != null) {
			updateAdapter();
		}

		return view;
	}

	private void updateAdapter() {
		adapter.updateItems(getAdapterItems());
	}

	private void reloadTracks() {
		File gpxDir = FileUtils.getExistingDir(app, GPX_INDEX_DIR);
		asyncLoader = new TrackFolderLoaderTask(app, gpxDir, this);
		asyncLoader.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	public void updateProgressVisibility(boolean visible) {
		MyPlacesActivity activity = (MyPlacesActivity) getActivity();
		if (activity != null) {
			activity.setSupportProgressBarIndeterminateVisibility(visible);
		}
	}

	@Override
	public void onResume() {
		super.onResume();

		if (trackFolder == null && (asyncLoader == null || asyncLoader.getStatus() != Status.RUNNING)) {
			reloadTracks();
		}
	}

	@Override
	public void loadTracksStarted() {
		updateProgressVisibility(true);
	}

	@Override
	public void loadTracksFinished(@NonNull TrackFolder folder) {
		trackFolder = folder;
		updateAdapter();
		updateProgressVisibility(false);
	}

	@Override
	public void showSortByDialog() {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			SortByBottomSheet.showInstance(activity.getSupportFragmentManager(), this);
		}
	}

	@NonNull
	@Override
	public TracksSortMode getTracksSortMode() {
		return TracksSortMode.getDefaultSortMode();
	}

	@Override
	public void setTracksSortMode(@NonNull TracksSortMode sortMode) {

	}

	@Override
	public void onFolderSelected(@NonNull TrackFolder folder) {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			TrackFolderFragment.showInstance(activity.getSupportFragmentManager(), folder);
		}
	}

	@Override
	public void onTrackItemOptionsSelected(@NonNull View view, @NonNull TrackItem trackItem) {
		GPXInfo gpxInfo = new GPXInfo(trackItem.getName(), trackItem.getFile());
		gpxActionsHelper.openPopUpMenu(view, gpxInfo);
	}

	@Override
	public void showTracksDialog() {
		Bundle bundle = new Bundle();
		bundle.putString(OPEN_TRACKS_TAB, TrackTabType.ALL.name());

		Bundle prevIntentParams = new Bundle();
		prevIntentParams.putInt(TAB_ID, GPX_TAB);

		MapActivity.launchMapActivityMoveToTop(requireActivity(), prevIntentParams, null, bundle);
	}

	@Override
	public void onGpxFilesDeletionStarted() {

	}

	@Override
	public void onGpxFilesDeleted(GPXInfo... values) {

	}

	@Override
	public void onGpxFilesDeletionFinished(boolean shouldUpdateFolders) {

	}
}