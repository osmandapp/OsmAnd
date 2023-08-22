package net.osmand.plus.myplaces.tracks;

import static net.osmand.plus.track.fragments.TrackMenuFragment.TrackMenuTab.OVERVIEW;

import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.R;
import net.osmand.plus.configmap.tracks.TrackItem;
import net.osmand.plus.configmap.tracks.viewholders.TrackViewHolder.TrackSelectionListener;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.myplaces.favorites.dialogs.FragmentStateHolder;
import net.osmand.plus.myplaces.tracks.ItemsSelectionHelper.SelectionHelperProvider;
import net.osmand.plus.myplaces.tracks.dialogs.BaseTrackFolderFragment;
import net.osmand.plus.myplaces.tracks.dialogs.MoveGpxFileBottomSheet.OnTrackFileMoveListener;
import net.osmand.plus.track.fragments.TrackMenuFragment;
import net.osmand.plus.track.helpers.SelectGpxTask.SelectGpxTaskListener;
import net.osmand.plus.utils.AndroidUtils;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class SearchMyPlacesTracksFragment extends SearchTrackBaseFragment implements SelectGpxTaskListener,
		FragmentStateHolder, SelectionHelperProvider<TrackItem>, OnTrackFileMoveListener {

	public static final String TAG = SearchMyPlacesTracksFragment.class.getSimpleName();

	private ImageButton selectButton;
	private ImageButton actionButton;
	private View searchContainer;
	private TextView selectedCountTv;

	@Override
	protected int getLayoutId() {
		return R.layout.search_myplaces_tracks_fragment;
	}

	@Override
	protected void setupFragment(View view) {
		requireDialog().setOnKeyListener((dialog, keyCode, event) -> {
			if (KeyEvent.KEYCODE_BACK == keyCode && KeyEvent.ACTION_UP == event.getAction()) {
				dismiss();
				return true;
			}
			return false;
		});
	}

	@Override
	protected void updateButtonsState() {
		AndroidUiHelper.setVisibility(selectionMode ? View.VISIBLE : View.GONE, selectButton, actionButton, selectedCountTv);
		AndroidUiHelper.setVisibility(!selectionMode ? View.VISIBLE : View.GONE, searchContainer);
		if (selectionMode) {
			boolean allTracksSelected = areAllTracksSelected();

			int iconId = allTracksSelected ? R.drawable.ic_action_deselect_all : R.drawable.ic_action_select_all;
			selectButton.setImageDrawable(getIcon(iconId));
			selectButton.setContentDescription(getString(allTracksSelected ? R.string.shared_string_deselect_all : R.string.shared_string_select_all));

			String count = String.valueOf(selectionHelper.getSelectedItems().size());
			selectedCountTv.setText(count);
		}
	}

	@Override
	public void onFileMove(@Nullable File src, @NonNull File dest) {
		TrackFoldersHelper foldersHelper = getTrackFoldersHelper();
		if (foldersHelper != null) {
			Set<TrackItem> trackItemsToMove = new HashSet<>();
			for (TrackItem trackItem : selectionHelper.getSelectedItems()) {
				File itemFile = trackItem.getFile();
				if (itemFile != null) {
					File destFile = new File(dest, itemFile.getName());
					if (destFile.exists() && itemFile.length() == destFile.length()
							&& destFile.getAbsolutePath().equals(itemFile.getAbsolutePath()) && destFile.equals(itemFile)) {
						continue;
					}
					trackItemsToMove.add(trackItem);
				}
			}
			foldersHelper.moveTracks(trackItemsToMove, Collections.emptySet(), dest, result -> {
				reloadTracks();
				dismiss(true);
				return false;
			});
		}
	}

	private void reloadTracks() {
		TrackFoldersHelper foldersHelper = getTrackFoldersHelper();
		if (foldersHelper != null) {
			foldersHelper.reloadTracks();
		}
	}

	@Override
	public void dismiss() {
		dismiss(false);
	}

	public void dismiss(boolean dismissImmediately) {
		if (dismissImmediately || !selectionMode) {
			super.dismiss();
		} else {
			selectionMode = false;
			adapter.setSelectionMode(false);
			adapter.notifyDataSetChanged();
			updateButtonsState();
		}
	}

	public void updateTargetFragment() {
		Fragment fragment = getTargetFragment();
		if (fragment instanceof BaseTrackFolderFragment) {
			((BaseTrackFolderFragment) fragment).updateContent();
		}
	}

	protected void setupToolbar(@NonNull View view) {
		super.setupToolbar(view);

		selectedCountTv = view.findViewById(R.id.selected_count);
		selectedCountTv.setTextColor(ContextCompat.getColor(app, R.color.color_white));

		searchContainer = view.findViewById(R.id.search_container);
		selectButton = view.findViewById(R.id.select_all_button);
		selectButton.setOnClickListener(v -> {
			Set<TrackItem> items = adapter.getFilteredItems();
			selectionHelper.onItemsSelected(items, !areAllTracksSelected());
			onTrackItemsSelected(items);
		});

		actionButton = view.findViewById(R.id.action_button);
		actionButton.setOnClickListener(v -> {
			TrackFoldersHelper foldersHelper = getTrackFoldersHelper();
			if (foldersHelper != null) {
				Set<TrackItem> trackItems = selectionHelper.getSelectedItems();
				SearchMyPlacesTracksFragment currentFragment = SearchMyPlacesTracksFragment.this;
				foldersHelper.showItemsOptionsMenu(actionButton, null, trackItems, new HashSet<>(),
						currentFragment, currentFragment,
						currentFragment, app.getDaynightHelper().isNightMode(false));
			}
		});

		updateButtonsState();
	}

	@Nullable
	private TrackFoldersHelper getTrackFoldersHelper() {
		Fragment target = getTargetFragment();
		if (target instanceof BaseTrackFolderFragment) {
			return ((BaseTrackFolderFragment) target).getTrackFoldersHelper();
		}
		return null;
	}

	@Override
	@NonNull
	protected TrackSelectionListener getTrackSelectionListener() {
		return new TrackSelectionListener() {
			@Override
			public boolean isTrackItemSelected(@NonNull TrackItem trackItem) {
				return selectionHelper.isItemSelected(trackItem);
			}

			@Override
			public void onTrackItemsSelected(@NonNull Set<TrackItem> trackItems, boolean selected) {
				if (selectionMode) {
					selectionHelper.onItemsSelected(trackItems, selected);
					adapter.onItemsSelected(trackItems);
					updateButtonsState();

				} else if (!trackItems.isEmpty()) {
					showTrackOnMap(trackItems.iterator().next());
				}
			}

			@Override
			public void onTrackItemLongClick(@NonNull View view, @NonNull TrackItem trackItem) {
				if (!selectionMode) {
					selectionMode = true;
					selectionHelper.clearSelectedItems();
					selectionHelper.onItemsSelected(Collections.singleton(trackItem), true);
					AndroidUtils.hideSoftKeyboard(requireActivity(), searchEditText);
					adapter.setSelectionMode(true);
					adapter.notifyDataSetChanged();
					updateButtonsState();
				}
			}

			@Override
			public void onTrackItemOptionsSelected(@NonNull View view, @NonNull TrackItem trackItem) {
				showItemOptionsMenu(view, trackItem);
			}

			private void showItemOptionsMenu(@NonNull View view, @NonNull TrackItem trackItem) {
				Fragment targetFragment = getTargetFragment();
				if (targetFragment instanceof BaseTrackFolderFragment) {
					BaseTrackFolderFragment fragment = (BaseTrackFolderFragment) targetFragment;

					TrackFoldersHelper foldersHelper = fragment.getTrackFoldersHelper();
					if (foldersHelper != null) {
						foldersHelper.showItemOptionsMenu(trackItem, view, fragment);
					}
				}
			}

			private void showTrackOnMap(@NonNull TrackItem trackItem) {
				FragmentActivity activity = getActivity();
				if (activity != null) {
					String screenName = getString(R.string.shared_string_tracks);
					boolean temporary = app.getSelectedGpxHelper().getSelectedFileByPath(trackItem.getPath()) == null;
					TrackMenuFragment.openTrack(activity, trackItem.getFile(), null, screenName, OVERVIEW, temporary);
				}
			}
		};
	}

	@Override
	public Bundle storeState() {
		return null;
	}

	@Override
	public void restoreState(Bundle bundle) {
	}

	@NonNull
	@Override
	public ItemsSelectionHelper<TrackItem> getSelectionHelper() {
		return selectionHelper;
	}

	public static void showInstance(@NonNull FragmentManager manager, @Nullable Fragment target,
	                                boolean selectionMode, boolean usedOnMap) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			SearchMyPlacesTracksFragment fragment = new SearchMyPlacesTracksFragment();
			fragment.usedOnMap = usedOnMap;
			fragment.selectionMode = selectionMode;
			fragment.setRetainInstance(true);
			fragment.setTargetFragment(target, 0);
			fragment.show(manager, TAG);
		}
	}
}
