package net.osmand.plus.myplaces.tracks.dialogs;

import static net.osmand.plus.myplaces.tracks.dialogs.TrackFoldersAdapter.TYPE_SORT_TRACKS;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.R;
import net.osmand.plus.configmap.tracks.SearchTrackItemsFragment;
import net.osmand.plus.configmap.tracks.TrackItem;
import net.osmand.plus.myplaces.tracks.GpxActionsHelper;
import net.osmand.plus.track.data.GPXInfo;
import net.osmand.plus.track.data.TrackFolder;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.UiUtilities;

import java.util.ArrayList;
import java.util.List;

public class TrackFolderFragment extends BaseTrackFolderFragment {

	public static final String TAG = TrackFolderFragment.class.getSimpleName();

	@NonNull
	@Override
	protected List<Object> getAdapterItems() {
		List<Object> items = new ArrayList<>();
		items.add(TYPE_SORT_TRACKS);
		items.addAll(selectedFolder.getSubFolders());
		items.addAll(selectedFolder.getTrackItems());
		return items;
	}

	@Override
	protected void setupToolbar(@NonNull View view) {
		super.setupToolbar(view);

		ViewGroup container = view.findViewById(R.id.actions_container);
		container.removeAllViews();

		LayoutInflater inflater = UiUtilities.getInflater(view.getContext(), nightMode);
		setupSearchButton(inflater, container);
		setupMenuButton(inflater, container);
	}

	@Override
	protected void updateContent() {
		super.updateContent();
		toolbarTitle.setText(selectedFolder.getName(app));
	}

	private void setupSearchButton(@NonNull LayoutInflater inflater, @NonNull ViewGroup container) {
		ImageButton button = (ImageButton) inflater.inflate(R.layout.action_button, container, false);
		button.setImageDrawable(getIcon(R.drawable.ic_action_search_dark));
		button.setOnClickListener(v -> {
			FragmentManager manager = getFragmentManager();
			if (manager != null) {
				SearchTrackItemsFragment.showInstance(manager);
			}
		});
		container.addView(button);
	}

	private void setupMenuButton(@NonNull LayoutInflater inflater, @NonNull ViewGroup container) {
		ImageButton button = (ImageButton) inflater.inflate(R.layout.action_button, container, false);
		button.setImageDrawable(getIcon(R.drawable.ic_overflow_menu_white));
		button.setOnClickListener(v -> {
			FragmentManager manager = getFragmentManager();
			if (manager != null) {
				TracksSelectionFragment.showInstance(manager, selectedFolder, getTargetFragment());
			}
		});
		container.addView(button);
	}

	@Override
	public void onTrackItemOptionsSelected(@NonNull View view, @NonNull TrackItem trackItem) {
		GpxActionsHelper gpxActionsHelper = getGpxActionsHelper();
		if (gpxActionsHelper != null) {
			gpxActionsHelper.showItemPopupMenu(view, trackItem);
		}
	}

	public static void showInstance(@NonNull FragmentManager manager, @NonNull TrackFolder trackFolder, @Nullable Fragment target) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			TrackFolderFragment fragment = new TrackFolderFragment();
			fragment.setRetainInstance(true);
			fragment.setTrackFolder(trackFolder);
			fragment.setTargetFragment(target, 0);
			fragment.show(manager, TAG);
		}
	}
}