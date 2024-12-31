package net.osmand.plus.myplaces.tracks;

import static net.osmand.plus.track.fragments.TrackMenuFragment.TrackMenuTab.OVERVIEW;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.shared.SharedUtil;
import net.osmand.plus.R;
import net.osmand.plus.configmap.tracks.SearchTracksAdapter;
import net.osmand.plus.configmap.tracks.viewholders.TrackViewHolder.TrackSelectionListener;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.myplaces.favorites.dialogs.FragmentStateHolder;
import net.osmand.plus.myplaces.tracks.ItemsSelectionHelper.SelectionHelperProvider;
import net.osmand.plus.myplaces.tracks.dialogs.BaseTrackFolderFragment;
import net.osmand.plus.myplaces.tracks.dialogs.MoveGpxFileBottomSheet.OnTrackFileMoveListener;
import net.osmand.plus.myplaces.tracks.dialogs.TracksFilterFragment;
import net.osmand.shared.gpx.SmartFolderUpdateListener;
import net.osmand.plus.track.fragments.TrackMenuFragment;
import net.osmand.plus.track.helpers.SelectGpxTask.SelectGpxTaskListener;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.widgets.dialogbutton.DialogButton;
import net.osmand.shared.gpx.filters.BaseTrackFilter;
import net.osmand.shared.gpx.filters.FilterChangedListener;
import net.osmand.shared.gpx.data.SmartFolder;
import net.osmand.shared.gpx.data.TrackFolder;
import net.osmand.shared.gpx.TrackItem;
import net.osmand.shared.io.KFile;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SearchMyPlacesTracksFragment extends SearchTrackBaseFragment implements SelectGpxTaskListener,
		FragmentStateHolder, SelectionHelperProvider<TrackItem>, OnTrackFileMoveListener, SmartFolderUpdateListener, FilterChangedListener, DialogClosedListener {

	public static final String TAG = SearchMyPlacesTracksFragment.class.getSimpleName();

	private ImageButton selectButton;
	private ImageButton actionButton;
	private View searchContainer;
	private TextView selectedCountTv;
	private SmartFolder smartFolder;
	private TrackFolder currentFolder;
	private DialogButton resetAllButton;
	private DialogButton saveButton;
	private View bottomButtonsContainer;
	private TracksSearchFilter externalFilter;
	private DialogClosedListener dialogClosedListener;

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

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		View view = super.onCreateView(inflater, container, savedInstanceState);
		setupBottomMenu(view);
		return view;
	}

	@NonNull
	protected SearchTracksAdapter createAdapter(@NonNull Context context, List<TrackItem> trackItems) {
		if (externalFilter != null) {
			return new SearchTracksAdapter(app, trackItems, nightMode, selectionMode, externalFilter);
		} else {
			TracksSearchFilter filter = new TracksSearchFilter(app, trackItems, currentFolder);
			return new SearchTracksAdapter(app, trackItems, nightMode, selectionMode, filter);
		}
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
		AndroidUiHelper.setVisibility(smartFolder == null ? View.GONE : View.VISIBLE, bottomButtonsContainer);
		if (saveButton != null && smartFolder != null) {
			boolean filtersChanged = false;
			TracksSearchFilter searchFilter = (TracksSearchFilter) adapter.getFilter();
			List<BaseTrackFilter> currentFilters = searchFilter.getAppliedFilters();
			List<BaseTrackFilter> filters = smartFolder.getFilters();
			if (filters != null) {
				if (currentFilters.size() != filters.size()) {
					filtersChanged = true;
				} else {
					for (BaseTrackFilter folderFilter : filters) {
						BaseTrackFilter currentFilter = searchFilter.getFilterByType(folderFilter.getTrackFilterType());
						if (currentFilter == null || !currentFilter.equals(folderFilter)) {
							filtersChanged = true;
							break;
						}
					}
				}
			}
			saveButton.setEnabled(filtersChanged);
		}
	}

	@Override
	public void onFileMove(@Nullable File src, @NonNull File dest) {
		TrackFoldersHelper foldersHelper = getTrackFoldersHelper();
		if (foldersHelper != null) {
			Set<TrackItem> trackItemsToMove = new HashSet<>();
			for (TrackItem trackItem : selectionHelper.getSelectedItems()) {
				KFile trackItemFile = trackItem.getFile();
				if(trackItemFile != null) {
					File itemFile = SharedUtil.jFile(trackItemFile);
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

	@Override
	public void onDismiss(@NonNull DialogInterface dialog) {
		super.onDismiss(dialog);
		if (dialogClosedListener != null) {
			dialogClosedListener.onDialogClosed();
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
		selectedCountTv.setTextColor(ContextCompat.getColor(app, R.color.card_and_list_background_light));

		searchContainer = view.findViewById(R.id.search_container);
		selectButton = view.findViewById(R.id.select_all_button);
		selectButton.setOnClickListener(v -> {
			Set<TrackItem> items = adapter.getFilteredItems();
			selectionHelper.onItemsSelected(items, !areAllTracksSelected());
			updateItems(items);
		});

		actionButton = view.findViewById(R.id.action_button);
		actionButton.setOnClickListener(v -> {
			TrackFoldersHelper foldersHelper = getTrackFoldersHelper();
			if (foldersHelper != null) {
				Set<TrackItem> trackItems = selectionHelper.getSelectedItems();
				SearchMyPlacesTracksFragment currentFragment = SearchMyPlacesTracksFragment.this;
				foldersHelper.showItemsOptionsMenu(actionButton, null, trackItems, new HashSet<>(),
						currentFragment, currentFragment, app.getDaynightHelper().isNightMode(false));
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
					TrackMenuFragment.openTrack(activity, trackItem.getFile() != null ? SharedUtil.jFile(trackItem.getFile()) : null, null, screenName, OVERVIEW, temporary);
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

	private void setupBottomMenu(View view) {
		bottomButtonsContainer = view.findViewById(R.id.buttons_container);
		resetAllButton = view.findViewById(R.id.reset_all_button);
		resetAllButton.setOnClickListener(v -> {
			TracksSearchFilter filter = (TracksSearchFilter) adapter.getFilter();
			filter.resetCurrentFilters();
			filter.filter();
		});

		saveButton = view.findViewById(R.id.save_button);
		saveButton.setOnClickListener(v -> {
			TracksSearchFilter filter = (TracksSearchFilter) adapter.getFilter();
			if (smartFolder != null) {
				app.getSmartFolderHelper().saveSmartFolder(smartFolder, filter.getCurrentFilters());
				Toast.makeText(app, R.string.smart_folder_saved, Toast.LENGTH_SHORT).show();
				dismiss();
			}
		});
	}

	@Override
	public void onResume() {
		super.onResume();
		setupToolbar(requireView());
		app.getSelectedGpxHelper().addListener(this);
		app.getSmartFolderHelper().addUpdateListener(this);
		((TracksSearchFilter) adapter.getFilter()).addFiltersChangedListener(this);
		updateContent();
	}

	@Override
	public void onSmartFolderSaved(SmartFolder smartFolder) {
		updateContent();
	}

	@Override
	public void onPause() {
		super.onPause();
		app.getSelectedGpxHelper().removeListener(this);
		app.getSmartFolderHelper().removeUpdateListener(this);
		((TracksSearchFilter) adapter.getFilter()).removeFiltersChangedListener(this);
	}

	@NonNull
	@Override
	public ItemsSelectionHelper<TrackItem> getSelectionHelper() {
		return selectionHelper;
	}

	public static void showInstance(@NonNull FragmentManager manager, @Nullable Fragment target,
	                                boolean selectionMode, boolean usedOnMap,
	                                @Nullable SmartFolder smartFolder,
	                                @Nullable TracksSearchFilter externalFilter,
	                                @Nullable DialogClosedListener dialogClosedListener,
	                                @Nullable TrackFolder currentFolder
	) {
		Fragment foundFragment = manager.findFragmentByTag(TAG);
		if (foundFragment instanceof SearchMyPlacesTracksFragment) {
			((SearchMyPlacesTracksFragment) foundFragment).dismiss();
		}
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			SearchMyPlacesTracksFragment fragment = new SearchMyPlacesTracksFragment();
			fragment.smartFolder = smartFolder;
			fragment.currentFolder = currentFolder;
			fragment.usedOnMap = usedOnMap;
			fragment.selectionMode = selectionMode;
			fragment.externalFilter = externalFilter;
			fragment.dialogClosedListener = dialogClosedListener;
			fragment.setRetainInstance(true);
			fragment.setTargetFragment(target, 0);
			fragment.show(manager, TAG);
		}
	}

	@WorkerThread
	@Override
	public void onSmartFolderUpdated(@NonNull SmartFolder smartFolder) {
		app.runInUIThread(this::updateButtonsState);
	}

	@Override
	public void onSmartFoldersUpdated() {
		adapter.updateFilteredItems(new ArrayList<>(selectionHelper.getAllItems()));
		updateButtonsState();
	}

	@Override
	public void showFiltersDialog() {
		FragmentManager manager = getFragmentManager();
		TracksSearchFilter filter = (TracksSearchFilter) adapter.getFilter();
		filter.setCurrentFolder(currentFolder);
		if (manager != null) {
			TracksFilterFragment.Companion.showInstance(app, manager, getTargetFragment(), filter, this, smartFolder, currentFolder);
		}
	}

	@Override
	public void onFilterChanged() {
		updateButtonsState();
	}

	@Override
	public void onDialogClosed() {
		setupFilterCallback();
		List<TrackItem> filteredItems = ((TracksSearchFilter) adapter.getFilter()).getFilteredTrackItems();
		if (filteredItems != null) {
			updateAdapterWithFilteredItems(filteredItems);
		}
	}

	public void setDialogClosedListener(DialogClosedListener dialogClosedListener) {
		this.dialogClosedListener = dialogClosedListener;
	}

	@Override
	public void onSmartFolderCreated(@NonNull SmartFolder smartFolder) {
		dismiss();
	}

	@Override
	public void onSmartFolderRenamed(@NonNull SmartFolder smartFolder) {
	}
}
