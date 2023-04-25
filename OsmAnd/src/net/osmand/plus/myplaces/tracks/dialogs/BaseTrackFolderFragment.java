package net.osmand.plus.myplaces.tracks.dialogs;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.configmap.tracks.SortByBottomSheet;
import net.osmand.plus.configmap.tracks.TrackItem;
import net.osmand.plus.configmap.tracks.viewholders.SortTracksViewHolder.SortTracksListener;
import net.osmand.plus.configmap.tracks.viewholders.TrackViewHolder.TrackSelectionListener;
import net.osmand.plus.myplaces.MyPlacesActivity;
import net.osmand.plus.myplaces.tracks.dialogs.TrackFolderViewHolder.FolderSelectionListener;
import net.osmand.plus.track.data.TrackFolder;
import net.osmand.plus.utils.UiUtilities;

import java.util.Set;

public abstract class BaseTrackFolderFragment extends BaseOsmAndFragment implements TrackSelectionListener, SortTracksListener, FolderSelectionListener {


	protected OsmandApplication app;

	protected TrackFolder trackFolder;

	protected TrackFoldersAdapter adapter;

	protected boolean nightMode;


	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = requireMyApplication();
		nightMode = isNightMode(true);
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		LayoutInflater themedInflater = UiUtilities.getInflater(requireContext(), nightMode);
		View view = themedInflater.inflate(R.layout.gpx_info_items_fragment, container, false);
		view.setBackgroundColor(ContextCompat.getColor(app, nightMode ? R.color.activity_background_color_dark : R.color.list_background_color_light));

		adapter = new TrackFoldersAdapter(app, nightMode);
		adapter.setTrackSelectionListener(this);
		adapter.setSortTracksListener(this);
		adapter.setFolderSelectionListener(this);

		RecyclerView recyclerView = view.findViewById(R.id.recycler_view);
		recyclerView.setLayoutManager(new LinearLayoutManager(app));
		recyclerView.setItemAnimator(null);
		recyclerView.setAdapter(adapter);

		if (trackFolder != null) {
			adapter.updateFolder(trackFolder);
		}

		return view;
	}

	@Override
	public void showSortByDialog() {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			SortByBottomSheet.showInstance(activity.getSupportFragmentManager(), this);
		}
	}

	public void showProgressBar() {
		if (getActivity() != null) {
			((MyPlacesActivity) getActivity()).setSupportProgressBarIndeterminateVisibility(true);
		}
	}

	public void hideProgressBar() {
		if (getActivity() != null) {
			((MyPlacesActivity) getActivity()).setSupportProgressBarIndeterminateVisibility(false);
		}
	}

	@Override
	public boolean isTrackItemSelected(@NonNull TrackItem trackItem) {
		return false;
	}

	@Override
	public void onTrackItemsSelected(@NonNull Set<TrackItem> trackItems, boolean selected) {

	}

	@Override
	public void onTrackItemLongClick(@NonNull View view, @NonNull TrackItem trackItem) {

	}

	@Override
	public void onFolderSelected(@NonNull TrackFolder folder) {
		FragmentManager manager = getActivity().getSupportFragmentManager();
		if (manager != null) {
			TrackFolderFragment.showInstance(manager, folder);
		}
	}

	@Override
	public void onFolderOptionsSelected(@NonNull TrackFolder folder) {

	}
}
