package net.osmand.plus.configmap.tracks;

import static net.osmand.plus.track.fragments.TrackMenuFragment.TrackMenuTab.OVERVIEW;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.OsmAndLocationProvider.OsmAndCompassListener;
import net.osmand.plus.OsmAndLocationProvider.OsmAndLocationListener;
import net.osmand.plus.R;
import net.osmand.plus.configmap.tracks.viewholders.SortTracksViewHolder.SortTracksListener;
import net.osmand.plus.configmap.tracks.viewholders.TrackViewHolder.TrackSelectionListener;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.myplaces.tracks.ItemsSelectionHelper;
import net.osmand.plus.myplaces.tracks.SearchTrackBaseFragment;
import net.osmand.plus.myplaces.tracks.TrackFoldersHelper;
import net.osmand.plus.myplaces.tracks.dialogs.BaseTrackFolderFragment;
import net.osmand.plus.myplaces.tracks.dialogs.ScreenPositionData;
import net.osmand.plus.myplaces.tracks.dialogs.TracksSelectionFragment;
import net.osmand.plus.track.data.TrackFolder;
import net.osmand.plus.track.fragments.TrackMenuFragment;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.widgets.dialogbutton.DialogButton;

import java.util.Collections;
import java.util.Set;

public class SearchTrackItemsFragment extends SearchTrackBaseFragment implements OsmAndCompassListener,
		OsmAndLocationListener, TrackItemsContainer, SortTracksListener {

	public static final String TAG = SearchTrackItemsFragment.class.getSimpleName();

	private DialogButton applyButton;
	private View buttonsContainer;
	private DialogButton selectionButton;

	@Override
	protected boolean isUsedOnMap() {
		return usedOnMap;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (!selectionHelper.hasAnyItems()) {
			setupSelectionHelper();
		}
	}

	@LayoutRes
	protected int getLayoutId() {
		return R.layout.gpx_search_items_fragment;
	}

	protected void setupFragment(View view) {
		setupButtons(view);
	}

	private void setupButtons(@NonNull View view) {
		buttonsContainer = view.findViewById(R.id.buttons_container);
		applyButton = view.findViewById(R.id.apply_button);
		applyButton.setOnClickListener(v -> saveChanges());

		selectionButton = view.findViewById(R.id.selection_button);
		selectionButton.setOnClickListener(v -> {
			Set<TrackItem> items = adapter.getFilteredItems();
			selectionHelper.onItemsSelected(items, !areAllTracksSelected());
			updateItems(items);
		});
		updateButtonsState();
	}

	private void saveChanges() {
		Fragment fragment = getTargetFragment();
		if (fragment instanceof TracksTabsFragment) {
			TracksTabsFragment tracksFragment = (TracksTabsFragment) fragment;
			ItemsSelectionHelper<TrackItem> originalHelper = tracksFragment.getSelectionHelper();
			originalHelper.syncWith(selectionHelper);
			tracksFragment.saveChanges();
		}
		dismiss();
	}

	protected void updateButtonsState() {
		String select = getString(!areAllTracksSelected() ? R.string.shared_string_select_all : R.string.shared_string_deselect_all);
		String count = "(" + adapter.getFilteredItems().size() + ")";
		select = getString(R.string.ltr_or_rtl_combine_via_space, select, count);
		selectionButton.setTitle(select);

		applyButton.setEnabled(selectionHelper.hasItemsToApply());
		boolean visible = selectionMode && adapter.getFilteredItems().size() > 0;
		AndroidUiHelper.updateVisibility(buttonsContainer, visible);
	}

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
//					ScreenPositionData positionData = new ScreenPositionData(trackItem, getViewOnScreenY(view));
//					showTracksSelection(trackItem, positionData);
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

			private void showTracksSelection(@NonNull TrackItem trackItem,
			                                 @Nullable ScreenPositionData screenPositionData) {
				Fragment target = getTargetFragment();
				FragmentManager manager = getFragmentManager();
				if (target instanceof BaseTrackFolderFragment && manager != null) {
					BaseTrackFolderFragment fragment = (BaseTrackFolderFragment) target;
					TrackFolder trackFolder = fragment.getSelectedFolder();
					TracksSelectionFragment.showInstance(manager, trackFolder, fragment, Collections.singleton(trackItem), null, screenPositionData);

					app.runInUIThread(() -> dismissAllowingStateLoss());
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

	public static void showInstance(@NonNull FragmentManager manager, @Nullable Fragment target,
	                                boolean selectionMode, boolean usedOnMap) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			SearchTrackItemsFragment fragment = new SearchTrackItemsFragment();
			fragment.usedOnMap = usedOnMap;
			fragment.selectionMode = selectionMode;
			fragment.setRetainInstance(true);
			fragment.setTargetFragment(target, 0);
			fragment.show(manager, TAG);
		}
	}
}
