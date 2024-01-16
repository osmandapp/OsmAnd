package net.osmand.plus.configmap.tracks;

import static net.osmand.plus.track.fragments.TrackMenuFragment.TrackMenuTab.OVERVIEW;
import static net.osmand.plus.utils.FileUtils.RenameCallback;

import android.app.Activity;
import android.app.Dialog;
import android.content.res.ColorStateList;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.viewpager.widget.ViewPager.SimpleOnPageChangeListener;

import net.osmand.gpx.GPXFile;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.configmap.tracks.TrackFolderLoaderTask.LoadTracksListener;
import net.osmand.plus.configmap.tracks.viewholders.EmptyTracksViewHolder.EmptyTracksListener;
import net.osmand.plus.configmap.tracks.viewholders.SortTracksViewHolder.SortTracksListener;
import net.osmand.plus.configmap.tracks.viewholders.TrackViewHolder.TrackSelectionListener;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.myplaces.tracks.ItemsSelectionHelper.SelectionHelperProvider;
import net.osmand.plus.myplaces.tracks.dialogs.MoveGpxFileBottomSheet;
import net.osmand.plus.myplaces.tracks.dialogs.MoveGpxFileBottomSheet.OnTrackFileMoveListener;
import net.osmand.plus.myplaces.tracks.tasks.DeleteTracksTask;
import net.osmand.plus.myplaces.tracks.tasks.DeleteTracksTask.GpxFilesDeletionListener;
import net.osmand.plus.myplaces.tracks.tasks.OpenGpxDetailsTask;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.monitoring.SavingTrackHelper;
import net.osmand.plus.plugins.osmedit.OsmEditingPlugin;
import net.osmand.plus.settings.enums.TracksSortMode;
import net.osmand.plus.track.BaseTracksTabsFragment;
import net.osmand.plus.track.data.TrackFolder;
import net.osmand.plus.track.fragments.TrackMenuFragment;
import net.osmand.plus.track.helpers.GpxSelectionHelper;
import net.osmand.plus.track.helpers.GpxUiHelper;
import net.osmand.plus.track.helpers.SelectGpxTask.SelectGpxTaskListener;
import net.osmand.plus.track.helpers.save.SaveGpxHelper;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.FileUtils;
import net.osmand.plus.widgets.dialogbutton.DialogButton;
import net.osmand.plus.widgets.popup.PopUpMenu;
import net.osmand.plus.widgets.popup.PopUpMenuDisplayData;
import net.osmand.plus.widgets.popup.PopUpMenuItem;
import net.osmand.util.Algorithms;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TracksTabsFragment extends BaseTracksTabsFragment implements LoadTracksListener,
		SelectionHelperProvider<TrackItem>, OnTrackFileMoveListener, RenameCallback,
		TrackSelectionListener, SortTracksListener, EmptyTracksListener, SelectGpxTaskListener {

	public static final String TAG = TracksTabsFragment.class.getSimpleName();

	private ImageView searchButton;

	private DialogButton applyButton;
	private DialogButton selectionButton;

	@Nullable
	private PreselectedTabParams preselectedTabParams;

	public boolean getContentStatusBarNightMode() {
		return nightMode;
	}

	@NonNull
	@Override
	public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
		Activity activity = requireActivity();
		int themeId = nightMode ? R.style.OsmandDarkTheme_DarkActionbar : R.style.OsmandLightTheme_DarkActionbar_LightStatusBar;
		Dialog dialog = new Dialog(activity, themeId) {
			@Override
			public void onBackPressed() {
				if (preselectedTabParams != null && activity instanceof MapActivity) {
					((MapActivity) activity).launchPrevActivityIntent();
				}
				dismiss();
			}
		};
		Window window = dialog.getWindow();
		if (window != null) {
			if (!settings.DO_NOT_USE_ANIMATIONS.get()) {
				window.getAttributes().windowAnimations = R.style.Animations_Alpha;
			}
			window.setStatusBarColor(ContextCompat.getColor(app, getStatusBarColorId()));
		}
		return dialog;
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		updateNightMode();
		View view = themedInflater.inflate(R.layout.tracks_fragment, container, false);

		setupToolbar(view);
		setupTabLayout(view);
		setupButtons(view);

		return view;
	}

	private void setupToolbar(@NonNull View view) {
		progressBar = view.findViewById(R.id.progress_bar);
		View appbar = view.findViewById(R.id.appbar);
		ViewCompat.setElevation(appbar, 5.0f);
		appbar.setBackgroundColor(ContextCompat.getColor(app, nightMode ? R.color.app_bar_main_dark : R.color.card_and_list_background_light));

		Toolbar toolbar = view.findViewById(R.id.toolbar);
		searchButton = toolbar.findViewById(R.id.search);
		ImageView switchGroup = toolbar.findViewById(R.id.switch_group);
		ImageView actionsButton = toolbar.findViewById(R.id.actions_button);

		switchGroup.setOnClickListener(v -> {
			FragmentActivity activity = getActivity();
			if (activity != null) {
				TrackGroupsBottomSheet.showInstance(activity.getSupportFragmentManager(), this);
			}
		});
		actionsButton.setOnClickListener(this::showOptionsMenu);
		searchButton.setOnClickListener((v) -> {
			FragmentActivity activity = getActivity();
			if (activity != null) {
				SearchTrackItemsFragment.showInstance(activity.getSupportFragmentManager(), this, true, isUsedOnMap());
			}
		});
		toolbar.findViewById(R.id.back_button).setOnClickListener(v -> dismiss());

		int iconColor = ColorUtilities.getColor(app, nightMode ? R.color.icon_color_default_dark : R.color.icon_color_default_light);
		switchGroup.setImageTintList(ColorStateList.valueOf(iconColor));
		actionsButton.setImageTintList(ColorStateList.valueOf(iconColor));
	}

	private void showOptionsMenu(@NonNull View view) {
		List<PopUpMenuItem> items = new ArrayList<>();

		String appearance = getString(R.string.change_appearance);
		String count = "(" + itemsSelectionHelper.getSelectedItemsSize() + ")";
		items.add(new PopUpMenuItem.Builder(view.getContext())
				.setTitle(getString(R.string.ltr_or_rtl_combine_via_space, appearance, count))
				.setIcon(getContentIcon(R.drawable.ic_action_appearance))
				.setOnClickListener(v -> changeAppearance()).create());

		items.add(new PopUpMenuItem.Builder(view.getContext())
				.setTitleId(R.string.shared_string_import)
				.setIcon(getContentIcon(R.drawable.ic_action_import_to))
				.setOnClickListener(v -> importTracks()).create());

		PopUpMenuDisplayData displayData = new PopUpMenuDisplayData();
		displayData.anchorView = view;
		displayData.menuItems = items;
		displayData.nightMode = nightMode;
		displayData.layoutId = R.layout.simple_popup_menu_item;
		PopUpMenu.show(displayData);
	}

	protected void setTabs(@NonNull List<TrackTab> tabs) {
		tabSize = tabs.size();
		setViewPagerAdapter(viewPager, tabs);
		tabLayout.setViewPager(viewPager);
		viewPager.setCurrentItem(0);
		viewPager.addOnPageChangeListener(new SimpleOnPageChangeListener() {
			@Override
			public void onPageSelected(int position) {
				updateButtonsState();
			}
		});
	}

	private void setupButtons(@NonNull View view) {
		applyButton = view.findViewById(R.id.apply_button);
		applyButton.setOnClickListener(v -> {
			saveChanges();
			dismiss();
		});

		selectionButton = view.findViewById(R.id.selection_button);
		selectionButton.setOnClickListener(getSelectionButtonClickListener());
		updateButtonsState();
	}

	private void updateButtonsState() {
		TrackTab trackTab = getSelectedTab();
		if (trackTab != null) {
			if (TrackTabType.ON_MAP == trackTab.type && !Algorithms.isEmpty(trackTabsHelper.getRecentlyVisibleTracks())) {
				boolean anySelected = itemsSelectionHelper.hasSelectedItems();
				selectionButton.setTitleId(anySelected ? R.string.shared_string_hide_all : R.string.shared_string_select_recent);
				selectionButton.setEnabled(!Algorithms.isEmpty(trackTabsHelper.getRecentlyVisibleTracks()) || anySelected);
			} else {
				boolean notAllSelected = !itemsSelectionHelper.isItemsSelected(trackTab.getTrackItems());
				selectionButton.setTitleId(notAllSelected ? R.string.shared_string_select_all : R.string.shared_string_deselect_all);
				selectionButton.setEnabled(!Algorithms.isEmpty(itemsSelectionHelper.getSelectedItems()) || notAllSelected);
			}
			applyButton.setEnabled(itemsSelectionHelper.hasItemsToApply());
			TrackTab allTracksTab = trackTabsHelper.getTrackTabs().get(TrackTabType.ALL.name());
			searchButton.setVisibility(allTracksTab == null ? View.GONE : View.VISIBLE);
		}
	}

	@NonNull
	private View.OnClickListener getSelectionButtonClickListener() {
		return v -> {
			TrackTab tab = getSelectedTab();
			if (tab != null) {
				if (TrackTabType.ON_MAP == tab.type && !Algorithms.isEmpty(trackTabsHelper.getRecentlyVisibleTracks())) {
					Set<TrackItem> selectedItems = itemsSelectionHelper.getSelectedItems();
					boolean hasSelectedItems = !Algorithms.isEmpty(selectedItems);
					Set<TrackItem> selectTracks = hasSelectedItems ? selectedItems : trackTabsHelper.getRecentlyVisibleTracks();
					onTrackItemsSelected(selectTracks, !hasSelectedItems);
				} else {
					Set<TrackItem> trackItems = new HashSet<>(tab.getTrackItems());
					boolean itemsSelected = itemsSelectionHelper.isItemsSelected(trackItems);
					onTrackItemsSelected(trackItems, !itemsSelected);
				}
			}
		};
	}

	@Override
	public void tracksLoaded(@NonNull TrackFolder folder) {
		trackTabsHelper.updateTrackItems(folder.getFlattenedTrackItems());
	}

	@Override
	public void loadTracksFinished(@NonNull TrackFolder folder) {
		AndroidUiHelper.updateVisibility(progressBar, false);
		updateTrackTabs();
		applyPreselectedParams();
		updateTabsContent();
		updateButtonsState();
		preselectedTabParams = null;
	}

	private void applyPreselectedParams() {
		if (preselectedTabParams != null) {
			String tabName = preselectedTabParams.getPreselectedTabName(app, getTrackTabs());
			TrackTab trackTab = getTab(tabName);
			if (trackTab != null) {
				setSelectedTab(tabName);

				if (preselectedTabParams.shouldSelectAll()) {
					itemsSelectionHelper.onItemsSelected(trackTab.getTrackItems(), true);
				}
			}
		}
	}

	public void saveChanges() {
		trackTabsHelper.saveTabsSortModes();
		trackTabsHelper.saveTracksVisibility();
	}

	public void changeAppearance() {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			TracksAppearanceFragment.showInstance(activity.getSupportFragmentManager(), this);
		}
	}

	protected void addTrackItem(@NonNull TrackItem item) {
		trackTabsHelper.addTrackItem(item);
		updateTrackTabs();
		setSelectedTab("import");
		updateTabsContent();
		updateButtonsState();
	}

	@Override
	public void setTracksSortMode(@NonNull TracksSortMode sortMode) {
		TrackTab trackTab = getSelectedTab();
		if (trackTab != null) {
			trackTab.setSortMode(sortMode);
			trackTabsHelper.sortTrackTab(trackTab);
			trackTabsHelper.saveTabsSortModes();
			updateTabsContent();
		}
	}

	@Override
	public boolean isTrackItemSelected(@NonNull TrackItem trackItem) {
		return itemsSelectionHelper.isItemSelected(trackItem);
	}

	@Override
	public void onTrackItemsSelected(@NonNull Set<TrackItem> trackItems, boolean selected) {
		itemsSelectionHelper.onItemsSelected(trackItems, selected);
		updateItems(trackItems);
		updateButtonsState();
	}

	@Override
	public void onTrackItemLongClick(@NonNull View view, @NonNull TrackItem trackItem) {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			openTrackOptions(activity, view, trackItem);
		}
	}

	private void openTrackOptions(@NonNull FragmentActivity activity, @NonNull View view, @NonNull TrackItem trackItem) {
		List<PopUpMenuItem> items = new ArrayList<>();

		items.add(new PopUpMenuItem.Builder(app)
				.setTitleId(R.string.shared_string_show_on_map)
				.setIcon(getContentIcon(R.drawable.ic_show_on_map))
				.setOnClickListener(v -> showTrackOnMap(trackItem))
				.create());

		File file = trackItem.getFile();
		items.add(new PopUpMenuItem.Builder(app)
				.setTitleId(R.string.analyze_on_map)
				.setIcon(getContentIcon(R.drawable.ic_action_info_dark))
				.setOnClickListener(v -> GpxSelectionHelper.getGpxFile(activity, file, true, result -> {
					OpenGpxDetailsTask detailsTask = new OpenGpxDetailsTask(activity, result, null);
					detailsTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
					dismiss();
					return true;
				}))
				.create());

		if (file != null) {
			items.add(new PopUpMenuItem.Builder(app)
					.setTitleId(R.string.shared_string_move)
					.setIcon(getContentIcon(R.drawable.ic_action_folder_stroke))
					.setOnClickListener(v -> {
						FragmentManager manager = activity.getSupportFragmentManager();
						MoveGpxFileBottomSheet.showInstance(manager, file, file.getParentFile(), this, false, false);
					})
					.create());

			items.add(new PopUpMenuItem.Builder(app)
					.setTitleId(R.string.shared_string_rename)
					.setIcon(getContentIcon(R.drawable.ic_action_edit_dark))
					.setOnClickListener(v -> FileUtils.renameFile(activity, file, this, false))
					.create());

		}
		items.add(new PopUpMenuItem.Builder(app)
				.setTitleId(R.string.shared_string_share)
				.setIcon(getContentIcon(R.drawable.ic_action_gshare_dark))
				.setOnClickListener(v -> GpxSelectionHelper.getGpxFile(activity, file, true, gpxFile -> {
					if (gpxFile.showCurrentTrack) {
						GpxUiHelper.saveAndShareCurrentGpx(app, gpxFile);
					} else if (!Algorithms.isEmpty(gpxFile.path)) {
						GpxUiHelper.saveAndShareGpxWithAppearance(app, gpxFile);
					}
					return true;
				}))
				.create());

		OsmEditingPlugin plugin = PluginsHelper.getActivePlugin(OsmEditingPlugin.class);
		if (plugin != null) {
			items.add(new PopUpMenuItem.Builder(app)
					.setTitleId(R.string.shared_string_export)
					.setIcon(getContentIcon(R.drawable.ic_action_export))
					.setOnClickListener(v -> exportTrackItem(plugin, trackItem))
					.create());
		}
		if (file != null) {
			items.add(new PopUpMenuItem.Builder(app)
					.setTitleId(R.string.shared_string_delete)
					.setIcon(getContentIcon(R.drawable.ic_action_delete_dark))
					.setOnClickListener(v -> showDeleteConfirmationDialog(Collections.singleton(trackItem)))
					.create());
		}

		PopUpMenuDisplayData displayData = new PopUpMenuDisplayData();
		displayData.anchorView = view;
		displayData.menuItems = items;
		displayData.nightMode = nightMode;
		PopUpMenu.show(displayData);
	}

	public void showTrackOnMap(@NonNull TrackItem trackItem) {
		MapActivity mapActivity = (MapActivity) requireActivity();
		boolean temporary = app.getSelectedGpxHelper().getSelectedFileByPath(trackItem.getPath()) == null;
		TrackMenuFragment.openTrack(mapActivity, trackItem.getFile(), null, null, OVERVIEW, temporary);

		dismiss();
		mapActivity.getDashboard().hideDashboard();
	}

	private void exportTrackItem(@NonNull OsmEditingPlugin plugin, @NonNull TrackItem trackItem) {
		FragmentActivity activity = requireActivity();
		if (trackItem.isShowCurrentTrack()) {
			SavingTrackHelper savingTrackHelper = app.getSavingTrackHelper();
			GPXFile gpxFile = savingTrackHelper.getCurrentTrack().getGpxFile();

			SaveGpxHelper.saveCurrentTrack(app, gpxFile, errorMessage -> {
				if (errorMessage == null) {
					plugin.sendGPXFiles(activity, this, new File(gpxFile.path));
				}
			});
		} else {
			plugin.sendGPXFiles(activity, this, trackItem.getFile());
		}
	}

	private void showDeleteConfirmationDialog(@NonNull Set<TrackItem> trackItems) {
		String size = String.valueOf(trackItems.size());
		String delete = app.getString(R.string.shared_string_delete);
		AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
		builder.setMessage(app.getString(R.string.local_index_action_do, delete.toLowerCase(), size));
		builder.setPositiveButton(delete, (dialog, which) -> deleteTracks(trackItems));
		builder.setNegativeButton(R.string.shared_string_cancel, null);
		builder.show();
	}

	public void deleteTracks(@Nullable Set<TrackItem> trackItems) {
		DeleteTracksTask deleteFilesTask = new DeleteTracksTask(app, trackItems, null, new GpxFilesDeletionListener() {
			@Override
			public void onGpxFilesDeletionFinished() {
				reloadTracks();
			}
		});
		deleteFilesTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	@Override
	public void onFileMove(@Nullable File src, @NonNull File dest) {
		if (dest.exists()) {
			app.showToastMessage(R.string.file_with_name_already_exists);
		} else if (src != null && FileUtils.renameGpxFile(app, src, dest) != null) {
			reloadTracks();
		} else {
			app.showToastMessage(R.string.file_can_not_be_moved);
		}
	}

	@Override
	public void fileRenamed(@NonNull File src, @NonNull File dest) {
		reloadTracks();
	}

	public static void showInstance(@NonNull FragmentManager manager) {
		showInstance(manager, null);
	}

	public static void showInstance(@NonNull FragmentManager manager, @Nullable PreselectedTabParams params) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			TracksTabsFragment fragment = new TracksTabsFragment();
			fragment.preselectedTabParams = params;
			fragment.setRetainInstance(true);
			fragment.show(manager, TAG);
		}
	}
}