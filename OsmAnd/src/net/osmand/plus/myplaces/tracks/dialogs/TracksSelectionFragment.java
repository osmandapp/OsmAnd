package net.osmand.plus.myplaces.tracks.dialogs;

import static net.osmand.IndexConstants.GPX_INDEX_DIR;
import static net.osmand.plus.myplaces.tracks.dialogs.TrackFoldersAdapter.TYPE_SORT_TRACKS;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import net.osmand.CallbackWithObject;
import net.osmand.plus.R;
import net.osmand.plus.configmap.tracks.TrackItem;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.myplaces.tracks.GpxActionsHelper;
import net.osmand.plus.myplaces.tracks.ItemsSelectionHelper;
import net.osmand.plus.myplaces.tracks.VisibleTracksGroup;
import net.osmand.plus.track.data.TrackFolder;
import net.osmand.plus.track.data.TracksGroup;
import net.osmand.plus.track.helpers.GpxSelectionHelper;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class TracksSelectionFragment extends BaseTrackFolderFragment {

	public static final String TAG = TrackFolderFragment.class.getSimpleName();

	private ItemsSelectionHelper<TrackItem> selectionHelper;

	private TextView toolbarTitle;
	private ImageButton selectionButton;

	@Override
	@ColorRes
	public int getStatusBarColorId() {
		AndroidUiHelper.setStatusBarContentColor(getView(), nightMode);
		return ColorUtilities.getStatusBarActiveColorId(nightMode);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		selectionHelper = new ItemsSelectionHelper<>();
		selectionHelper.setAllItems(rootFolder.getFlattenedTrackItems());
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		View view = super.onCreateView(inflater, container, savedInstanceState);
		adapter.setSelectionMode(true);
		updateSelection();
		return view;
	}

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

		Toolbar toolbar = view.findViewById(R.id.toolbar);
		toolbar.setBackgroundColor(ColorUtilities.getToolbarActiveColor(app, nightMode));

		toolbarTitle = toolbar.findViewById(R.id.toolbar_title);

		ImageView closeButton = toolbar.findViewById(R.id.close_button);
		closeButton.setImageDrawable(getIcon(R.drawable.ic_action_close));
		closeButton.setOnClickListener(v -> dismiss());

		setupToolbarActions(view);
		ViewCompat.setElevation(view.findViewById(R.id.appbar), 5.0f);
	}

	private void setupToolbarActions(@NonNull View view) {
		ViewGroup container = view.findViewById(R.id.actions_container);
		container.removeAllViews();

		LayoutInflater inflater = UiUtilities.getInflater(view.getContext(), nightMode);
		setupSelectionButton(inflater, container);
		setupMenuButton(inflater, container);
	}

	private void setupSelectionButton(@NonNull LayoutInflater inflater, @NonNull ViewGroup container) {
		selectionButton = (ImageButton) inflater.inflate(R.layout.action_button, container, false);
		selectionButton.setOnClickListener(v -> {
			if (selectionHelper.isAllItemsSelected()) {
				selectionHelper.clearSelectedItems();
			} else {
				selectionHelper.selectAllItems();
			}
			updateSelection();
			adapter.notifyDataSetChanged();
		});
		updateSelection();
		container.addView(selectionButton);
	}

	private void setupMenuButton(@NonNull LayoutInflater inflater, @NonNull ViewGroup container) {
		ImageButton button = (ImageButton) inflater.inflate(R.layout.action_button, container, false);
		button.setImageDrawable(getIcon(R.drawable.ic_overflow_menu_white));
		button.setOnClickListener(v -> {
			GpxActionsHelper gpxActionsHelper = getGpxActionsHelper();
			if (gpxActionsHelper != null) {
				CallbackWithObject<Boolean> callback = result -> {
					dismiss();
					return true;
				};
				gpxActionsHelper.showItemsPopupMenu(button, selectionHelper.getSelectedItems(), callback);
			}
		});
		container.addView(button);
	}

	private void updateSelection() {
		updateToolbar();
		boolean selected = selectionHelper.isAllItemsSelected();
		int iconId = selected ? R.drawable.ic_action_deselect_all : R.drawable.ic_action_select_all;
		selectionButton.setImageDrawable(getIcon(iconId));
	}

	@Override
	protected void updateContent() {
		super.updateContent();
		updateToolbar();
	}

	private void updateToolbar() {
		toolbarTitle.setText(String.valueOf(selectionHelper.getSelectedItemsSize()));
	}

	@Override
	public boolean isTrackItemSelected(@NonNull TrackItem trackItem) {
		return selectionHelper.isItemSelected(trackItem);
	}

	@Override
	public void onTrackItemsSelected(@NonNull Set<TrackItem> trackItems, boolean selected) {
		selectionHelper.onItemsSelected(trackItems, selected);
		adapter.onItemsSelected(trackItems);
		updateSelection();
	}

	@Override
	public boolean isTracksGroupSelected(@NonNull TracksGroup group) {
		if (group instanceof TrackFolder) {
			TrackFolder folder = (TrackFolder) group;
			return selectionHelper.isItemsSelected(folder.getFlattenedTrackItems());
		}
		return selectionHelper.isItemsSelected(group.getTrackItems());
	}

	@Override
	public void onTracksGroupSelected(@NonNull TracksGroup group, boolean selected) {
		if (group instanceof TrackFolder) {
			TrackFolder folder = (TrackFolder) group;
			selectionHelper.onItemsSelected(folder.getFlattenedTrackItems(), selected);
		} else {
			selectionHelper.onItemsSelected(group.getTrackItems(), selected);
		}
		adapter.onItemsSelected(Collections.singleton(group));
		updateSelection();
	}

	public static void showInstance(@NonNull FragmentManager manager, @NonNull TrackFolder trackFolder, @Nullable Fragment target) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			TracksSelectionFragment fragment = new TracksSelectionFragment();
			fragment.setRetainInstance(true);
			fragment.setTrackFolder(trackFolder);
			fragment.setTargetFragment(target, 0);
			fragment.show(manager, TAG);
		}
	}
}
