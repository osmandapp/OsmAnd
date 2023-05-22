package net.osmand.plus.myplaces.tracks.dialogs;

import static androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.R;
import net.osmand.plus.configmap.tracks.TrackItem;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.myplaces.tracks.ItemsSelectionHelper;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.osmedit.asynctasks.UploadGPXFilesTask.UploadGpxListener;
import net.osmand.plus.track.data.TrackFolder;
import net.osmand.plus.track.data.TracksGroup;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.popup.PopUpMenu;
import net.osmand.plus.widgets.popup.PopUpMenuDisplayData;
import net.osmand.plus.widgets.popup.PopUpMenuItem;
import net.osmand.util.Algorithms;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class TracksSelectionFragment extends BaseTrackFolderFragment implements UploadGpxListener {

	public static final String TAG = TracksSelectionFragment.class.getSimpleName();

	private ItemsSelectionHelper<TrackItem> selectionHelper = new ItemsSelectionHelper<>();

	private TextView toolbarTitle;
	private ImageButton selectionButton;

	@Override
	@ColorRes
	public int getStatusBarColorId() {
		AndroidUiHelper.setStatusBarContentColor(getView(), nightMode);
		return ColorUtilities.getStatusBarActiveColorId(nightMode);
	}

	@Override
	protected int getLayoutId() {
		return R.layout.track_folder_fragment;
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		FragmentActivity activity = requireActivity();
		activity.getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
			@Override
			public void handleOnBackPressed() {
				onBackPressed();
			}
		});
	}

	@Override
	public void setRootFolder(@NonNull TrackFolder rootFolder) {
		super.setRootFolder(rootFolder);
		selectionHelper.clearSelectedItems();
		selectionHelper.setAllItems(rootFolder.getFlattenedTrackItems());
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		View view = super.onCreateView(inflater, container, savedInstanceState);
		if (view != null) {
			setupToolbar(view);
		}
		updateContent();
		updateSelection();
		return view;
	}

	@Override
	protected void setupAdapter(@NonNull View view) {
		super.setupAdapter(view);
		adapter.setSelectionMode(true);
	}

	private void setupToolbar(@NonNull View view) {
		Toolbar toolbar = view.findViewById(R.id.toolbar);
		toolbar.setBackgroundColor(ColorUtilities.getToolbarActiveColor(app, nightMode));
		ViewCompat.setElevation(view.findViewById(R.id.appbar), 5.0f);

		toolbarTitle = toolbar.findViewById(R.id.toolbar_title);

		ImageView closeButton = toolbar.findViewById(R.id.close_button);
		closeButton.setImageDrawable(getIcon(R.drawable.ic_action_close));
		closeButton.setOnClickListener(v -> dismiss());

		setupToolbarActions(view);
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
		button.setOnClickListener(v -> showOptionsMenu(button));
		button.setContentDescription(getString(R.string.shared_string_more));
		container.addView(button);
	}

	private void updateSelection() {
		updateToolbar();
		boolean selected = selectionHelper.isAllItemsSelected();
		int iconId = selected ? R.drawable.ic_action_deselect_all : R.drawable.ic_action_select_all;
		selectionButton.setImageDrawable(getIcon(iconId));
		selectionButton.setContentDescription(getString(selected ? R.string.shared_string_deselect_all : R.string.shared_string_select_all));
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
	public void onResume() {
		super.onResume();
		updateActionBar(false);
	}

	@Override
	public void onPause() {
		super.onPause();
		updateActionBar(true);
	}

	private void showOptionsMenu(@NonNull View view) {
		Set<TrackItem> selectedTracks = selectionHelper.getSelectedItems();

		List<PopUpMenuItem> items = new ArrayList<>();
		items.add(new PopUpMenuItem.Builder(app)
				.setTitleId(R.string.shared_string_show_on_map)
				.setIcon(getContentIcon(R.drawable.ic_show_on_map))
				.setOnClickListener(v -> {
					gpxSelectionHelper.saveTracksVisibility(selectedTracks, this);
					dismiss();
				})
				.create()
		);
		PluginsHelper.onOptionsMenuActivity(requireActivity(), this, selectedTracks, items);

		String delete = app.getString(R.string.shared_string_delete);
		items.add(new PopUpMenuItem.Builder(app)
				.setTitle(delete)
				.setIcon(getContentIcon(R.drawable.ic_action_delete_outlined))
				.setOnClickListener(v -> {
					if (selectedTracks.isEmpty()) {
						showEmptyItemsToast(delete);
					} else {
						showDeleteConfirmationDialog(selectedTracks);
					}
				})
				.showTopDivider(true)
				.create()
		);

		PopUpMenuDisplayData displayData = new PopUpMenuDisplayData();
		displayData.anchorView = view;
		displayData.menuItems = items;
		displayData.nightMode = nightMode;
		PopUpMenu.show(displayData);
	}

	private void showEmptyItemsToast(@NonNull String action) {
		String message = getString(R.string.local_index_no_items_to_do, action.toLowerCase());
		app.showShortToastMessage(Algorithms.capitalizeFirstLetter(message));
	}

	private void showDeleteConfirmationDialog(@NonNull Set<TrackItem> trackItems) {
		String delete = getString(R.string.shared_string_delete);
		AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
		builder.setMessage(getString(R.string.local_index_action_do, delete.toLowerCase(), String.valueOf(trackItems.size())));
		builder.setPositiveButton(delete, (dialog, which) -> {
			List<File> files = new ArrayList<>();
			for (TrackItem trackItem : trackItems) {
				files.add(trackItem.getFile());
			}
			deleteGpxFiles(files.toArray(new File[0]));
			dismiss();
		});
		builder.setNegativeButton(R.string.shared_string_cancel, null);
		builder.show();
	}

	private void onBackPressed() {
		if (rootFolder.equals(selectedFolder)) {
			dismiss();
		} else {
			selectedFolder = selectedFolder.getParentFolder();
			updateContent();
		}
	}

	private void dismiss() {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			activity.getSupportFragmentManager().popBackStack(TAG, POP_BACK_STACK_INCLUSIVE);
		}
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
			List<TrackItem> trackItems = folder.getFlattenedTrackItems();
			return !trackItems.isEmpty() && selectionHelper.isItemsSelected(trackItems);
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

	@Override
	public void onGpxUploaded(String result) {
		dismiss();
	}

	public static void showInstance(@NonNull FragmentManager manager, @NonNull TrackFolder trackFolder, @Nullable Fragment target) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			TracksSelectionFragment fragment = new TracksSelectionFragment();
			fragment.setRetainInstance(true);
			fragment.setRootFolder(trackFolder);
			fragment.setSelectedFolder(trackFolder);
			fragment.setTargetFragment(target, 0);

			manager.beginTransaction()
					.replace(R.id.fragmentContainer, fragment, TAG)
					.addToBackStack(TAG)
					.commitAllowingStateLoss();
		}
	}
}
