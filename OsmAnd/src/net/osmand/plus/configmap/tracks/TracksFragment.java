package net.osmand.plus.configmap.tracks;

import static net.osmand.IndexConstants.GPX_INDEX_DIR;
import static net.osmand.plus.importfiles.ImportHelper.IMPORT_FILE_REQUEST;
import static net.osmand.plus.importfiles.ImportHelper.OnSuccessfulGpxImport.OPEN_GPX_CONTEXT_MENU;
import static net.osmand.plus.utils.FileUtils.RenameCallback;
import static net.osmand.plus.utils.UiUtilities.DialogButtonType.TERTIARY;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.ProgressBar;
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
import androidx.viewpager.widget.ViewPager;

import net.osmand.CallbackWithObject;
import net.osmand.gpx.GPXFile;
import net.osmand.gpx.GPXUtilities.WptPt;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseOsmAndDialogFragment;
import net.osmand.plus.configmap.tracks.TrackFolderLoaderTask.LoadTracksListener;
import net.osmand.plus.configmap.tracks.viewholders.EmptyTracksViewHolder.EmptyTracksListener;
import net.osmand.plus.configmap.tracks.viewholders.SortTracksViewHolder.SortTracksListener;
import net.osmand.plus.configmap.tracks.viewholders.TrackViewHolder.TrackSelectionListener;
import net.osmand.plus.dashboard.DashboardOnMap;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.helpers.IntentHelper;
import net.osmand.plus.importfiles.ImportHelper;
import net.osmand.plus.importfiles.ImportHelper.GpxImportListener;
import net.osmand.plus.importfiles.ImportHelper.OnSuccessfulGpxImport;
import net.osmand.plus.importfiles.MultipleTracksImportListener;
import net.osmand.plus.myplaces.tracks.ItemsSelectionHelper;
import net.osmand.plus.myplaces.tracks.ItemsSelectionHelper.SelectionHelperProvider;
import net.osmand.plus.myplaces.tracks.dialogs.MoveGpxFileBottomSheet;
import net.osmand.plus.myplaces.tracks.dialogs.MoveGpxFileBottomSheet.OnTrackFileMoveListener;
import net.osmand.plus.settings.enums.TracksSortMode;
import net.osmand.plus.track.data.TrackFolder;
import net.osmand.plus.track.helpers.GpxFileLoaderTask;
import net.osmand.plus.track.helpers.GpxUiHelper;
import net.osmand.plus.track.helpers.SelectedGpxFile;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.FileUtils;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.controls.PagerSlidingTabStrip;
import net.osmand.plus.views.controls.PagerSlidingTabStrip.CustomTabProvider;
import net.osmand.plus.widgets.popup.PopUpMenu;
import net.osmand.plus.widgets.popup.PopUpMenuDisplayData;
import net.osmand.plus.widgets.popup.PopUpMenuItem;
import net.osmand.plus.widgets.popup.PopUpMenuWidthMode;
import net.osmand.util.Algorithms;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class TracksFragment extends BaseOsmAndDialogFragment implements LoadTracksListener,
		SelectionHelperProvider<TrackItem>, OnTrackFileMoveListener, RenameCallback,
		TrackSelectionListener, SortTracksListener, EmptyTracksListener {

	public static final String TAG = TracksFragment.class.getSimpleName();

	public static final String OPEN_TRACKS_TAB = "open_tracks_tab";

	private ImportHelper importHelper;
	private SelectedTracksHelper selectedTracksHelper;
	private ItemsSelectionHelper<TrackItem> itemsSelectionHelper;
	private TrackFolderLoaderTask asyncLoader;

	private ViewPager viewPager;
	private PagerSlidingTabStrip tabLayout;
	private ProgressBar progressBar;
	private TracksTabAdapter adapter;
	private ImageView searchButton;

	private View applyButton;
	private View selectionButton;

	@Nullable
	private String preselectedTabName;
	private int tabSize;

	@NonNull
	public SelectedTracksHelper getSelectedTracksHelper() {
		return selectedTracksHelper;
	}

	@NonNull
	@Override
	public ItemsSelectionHelper<TrackItem> getSelectionHelper() {
		return itemsSelectionHelper;
	}

	@ColorRes
	public int getStatusBarColorId() {
		AndroidUiHelper.setStatusBarContentColor(getView(), nightMode);
		return nightMode ? R.color.status_bar_color_dark : R.color.activity_background_color_light;
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		importHelper = new ImportHelper(requireActivity());
		selectedTracksHelper = new SelectedTracksHelper(app);
		itemsSelectionHelper = selectedTracksHelper.getItemsSelectionHelper();
	}

	@Override
	protected boolean isUsedOnMap() {
		return true;
	}

	@NonNull
	@Override
	public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
		Activity activity = requireActivity();
		int themeId = nightMode ? R.style.OsmandDarkTheme_DarkActionbar : R.style.OsmandLightTheme_DarkActionbar_LightStatusBar;
		Dialog dialog = new Dialog(activity, themeId) {
			@Override
			public void onBackPressed() {
				if (preselectedTabName != null && activity instanceof MapActivity) {
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
		appbar.setBackgroundColor(ContextCompat.getColor(app, nightMode ? R.color.app_bar_color_dark : R.color.card_and_list_background_light));

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
				SearchTrackItemsFragment.showInstance(activity.getSupportFragmentManager(), this);
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

	private void setupTabLayout(@NonNull View view) {
		viewPager = view.findViewById(R.id.view_pager);
		List<TrackTab> tabs = getTrackTabs();
		tabLayout = view.findViewById(R.id.sliding_tabs);
		tabLayout.setTabBackground(nightMode ? R.color.app_bar_color_dark : R.color.card_and_list_background_light);
		tabLayout.setCustomTabProvider(new CustomTabProvider() {
			@Override
			public View getCustomTabView(@NonNull ViewGroup parent, int position) {
				TrackTab trackTab = getTrackTabs().get(position);

				int activeColor = ColorUtilities.getActiveColor(app, nightMode);
				int textColor = ColorUtilities.getPrimaryTextColor(app, nightMode);
				int sidePadding = AndroidUtils.dpToPx(app, 12);

				LayoutInflater inflater = UiUtilities.getInflater(parent.getContext(), nightMode);
				View customView = inflater.inflate(R.layout.tab_title_view, parent, false);
				TextView textView = customView.findViewById(android.R.id.text1);
				textView.setPadding(sidePadding, textView.getPaddingTop(), sidePadding, textView.getPaddingBottom());
				textView.setTextColor(AndroidUtils.createColorStateList(android.R.attr.state_selected, activeColor, textColor));
				textView.setText(trackTab.getName(app, false));
				return customView;
			}

			@Override
			public void select(View tab) {
				tab.setSelected(true);
			}

			@Override
			public void deselect(View tab) {
				tab.setSelected(false);
			}

			@Override
			public void tabStylesUpdated(View tabsContainer, int currentPosition) {

			}
		});
		setTabs(tabs);

	}

	private void setTabs(List<TrackTab> tabs) {
		tabSize = tabs.size();
		setViewPagerAdapter(viewPager, tabs);
		tabLayout.setViewPager(viewPager);
		viewPager.setCurrentItem(0);
	}

	protected void setViewPagerAdapter(@NonNull ViewPager pager, List<TrackTab> items) {
		adapter = new TracksTabAdapter(app, getChildFragmentManager(), items);
		pager.setAdapter(adapter);
	}

	private void setupButtons(@NonNull View view) {
		applyButton = view.findViewById(R.id.apply_button);
		applyButton.setOnClickListener(v -> {
			saveChanges();
			dismiss();
		});

		selectionButton = view.findViewById(R.id.selection_button);
		selectionButton.setOnClickListener(v -> {
			Set<TrackItem> selectedTracks = itemsSelectionHelper.getSelectedItems();
			if (Algorithms.isEmpty(selectedTracks)) {
				onTrackItemsSelected(selectedTracksHelper.getRecentlyVisibleTracks(), true);
			} else {
				onTrackItemsSelected(selectedTracks, false);
			}
		});
		updateButtonsState();
	}

	private void updateButtonsState() {
		boolean anySelected = itemsSelectionHelper.hasSelectedItems();
		String apply = getString(R.string.shared_string_apply).toUpperCase();
		String select = getString(anySelected ? R.string.shared_string_hide_all : R.string.shared_string_select_recent).toUpperCase();

		applyButton.setEnabled(itemsSelectionHelper.hasItemsToApply());
		selectionButton.setEnabled(!Algorithms.isEmpty(selectedTracksHelper.getRecentlyVisibleTracks()) || anySelected);

		UiUtilities.setupDialogButton(nightMode, applyButton, TERTIARY, apply);
		UiUtilities.setupDialogButton(nightMode, selectionButton, TERTIARY, select);
		TrackTab allTracksTab = selectedTracksHelper.getTrackTabs().get(TrackTabType.ALL.name());
		searchButton.setVisibility(allTracksTab == null ? View.GONE : View.VISIBLE);
	}

	@NonNull
	public List<TrackTab> getTrackTabs() {
		return new ArrayList<>(selectedTracksHelper.getTrackTabs().values());
	}

	@Nullable
	public TrackTab getSelectedTab() {
		List<TrackTab> trackTabs = getTrackTabs();
		return trackTabs.isEmpty() ? null : trackTabs.get(viewPager.getCurrentItem());
	}

	public void setSelectedTab(@NonNull String name) {
		List<TrackTab> trackTabs = getTrackTabs();
		for (int i = 0; i < trackTabs.size(); i++) {
			TrackTab tab = trackTabs.get(i);
			if (Algorithms.stringsEqual(tab.getTypeName(), name)) {
				viewPager.setCurrentItem(i);
				break;
			}
		}
	}

	@Nullable
	public TrackTab getTab(@NonNull String name) {
		for (TrackTab trackTab : getTrackTabs()) {
			if (Algorithms.stringsEqual(name, trackTab.getTypeName())) {
				return trackTab;
			}
		}
		return null;
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
		return getSelectedTab().getSortMode();
	}

	@Override
	public void onResume() {
		super.onResume();
		List<TrackTab> tabs = getTrackTabs();
		if (tabs.size() != tabSize) {
			setTabs(tabs);
		}
		if (asyncLoader == null) {
			reloadTracks();
		}
	}

	private void reloadTracks() {
		File gpxDir = FileUtils.getExistingDir(app, GPX_INDEX_DIR);
		asyncLoader = new TrackFolderLoaderTask(app, gpxDir, this);
		asyncLoader.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	@Override
	public void loadTracksStarted() {
		AndroidUiHelper.updateVisibility(progressBar, true);
	}

	@Override
	public void loadTracksFinished(@NonNull TrackFolder folder) {
		AndroidUiHelper.updateVisibility(progressBar, false);
		selectedTracksHelper.updateTrackItems(folder.getFlattenedTrackItems());
		updateTrackTabs();
		updateTabsContent();
		updateButtonsState();

		if (!Algorithms.isEmpty(preselectedTabName)) {
			setSelectedTab(preselectedTabName);
			preselectedTabName = "";
		}
	}

	private void updateTrackTabs() {
		adapter.setTrackTabs(selectedTracksHelper.getTrackTabs());
	}

	public void saveChanges() {
		selectedTracksHelper.saveTabsSortModes();
		selectedTracksHelper.saveTracksVisibility();
		selectedTracksHelper.updateTracksOnMap();

		FragmentActivity activity = getActivity();
		if (activity instanceof MapActivity) {
			MapActivity mapActivity = (MapActivity) activity;
			DashboardOnMap dashboard = mapActivity.getDashboard();
			if (dashboard.isVisible()) {
				dashboard.refreshContent(false);
			}
		}
		app.getOsmandMap().getMapView().refreshMap();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		boolean loadingTracks = asyncLoader != null && asyncLoader.getStatus() == Status.RUNNING;
		if (loadingTracks && !requireActivity().isChangingConfigurations()) {
			asyncLoader.cancel(false);
		}
	}

	public void changeAppearance() {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			TracksAppearanceFragment.showInstance(activity.getSupportFragmentManager(), this);
		}
	}

	@Override
	public void importTracks() {
		Intent intent = ImportHelper.getImportTrackIntent();
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
					File dir = ImportHelper.getGpxDestinationDir(app, true);
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
			public void onImportStarted() {
				AndroidUiHelper.updateVisibility(progressBar, true);
			}

			@Override
			public void onImportFinished() {
				importHelper.setGpxImportListener(null);
				AndroidUiHelper.updateVisibility(progressBar, false);
			}

			@Override
			public void onSaveComplete(boolean success, GPXFile gpxFile) {
				if (isAdded() && success) {
					addTrackItem(new TrackItem(new File(gpxFile.path)));
				}
				super.onSaveComplete(success, gpxFile);
			}
		};
	}

	private void addTrackItem(@NonNull TrackItem item) {
		selectedTracksHelper.addTrackItem(item);
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
			selectedTracksHelper.sortTrackTab(trackTab);
			selectedTracksHelper.saveTabsSortModes();
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
		onTrackItemsSelected(trackItems);
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
				.setIcon(iconsCache.getThemedIcon(R.drawable.ic_show_on_map))
				.setOnClickListener(v -> showGpxOnMap(trackItem, activity))
				.create());

		File file = trackItem.getFile();
		if (file != null) {
			items.add(new PopUpMenuItem.Builder(app)
					.setTitleId(R.string.shared_string_move)
					.setIcon(iconsCache.getThemedIcon(R.drawable.ic_action_folder_stroke))
					.setOnClickListener(v -> moveGpxFile(file))
					.create());


			items.add(new PopUpMenuItem.Builder(app)
					.setTitleId(R.string.shared_string_rename)
					.setIcon(iconsCache.getThemedIcon(R.drawable.ic_action_edit_dark))
					.setOnClickListener(v -> FileUtils.renameFile(activity, file, this, false)).create());
		}

		items.add(new PopUpMenuItem.Builder(app)
				.setTitleId(R.string.shared_string_share)
				.setIcon(iconsCache.getThemedIcon(R.drawable.ic_action_gshare_dark))
				.setOnClickListener(v -> {
					if (trackItem.isShowCurrentTrack()) {
						GPXFile gpxFile = app.getSavingTrackHelper().getCurrentGpx();
						GpxUiHelper.saveAndShareCurrentGpx(app, gpxFile);
					} else if (file != null) {
						SelectedGpxFile selectedGpxFile = app.getSelectedGpxHelper().getSelectedFileByPath(file.getAbsolutePath());
						if (selectedGpxFile == null) {
							GpxFileLoaderTask.loadGpxFile(file, activity, result -> {
								GpxUiHelper.saveAndShareGpxWithAppearance(app, result);
								return false;
							});
						} else {
							GpxUiHelper.saveAndShareGpxWithAppearance(app, selectedGpxFile.getGpxFile());
						}
					}
				}).create());

		PopUpMenuDisplayData displayData = new PopUpMenuDisplayData();
		displayData.anchorView = view;
		displayData.menuItems = items;
		displayData.nightMode = nightMode;
		displayData.widthMode = PopUpMenuWidthMode.STANDARD;
		PopUpMenu.show(displayData);
	}

	private void showGpxOnMap(@NonNull TrackItem trackItem, @NonNull Activity activity) {
		getGpxFile(trackItem, gpxFile -> {
			WptPt loc = gpxFile.findPointToShow();
			if (loc != null) {
				settings.setMapLocationToShow(loc.lat, loc.lon, settings.getLastKnownMapZoom());
				app.getSelectedGpxHelper().setGpxFileToDisplay(gpxFile);
				MapActivity.launchMapActivityMoveToTop(activity, null);
			} else {
				app.showToastMessage(R.string.gpx_file_is_empty);
			}
			dismiss();
			return true;
		});
	}

	private void getGpxFile(@NonNull TrackItem trackItem, @NonNull CallbackWithObject<GPXFile> callback) {
		File file = trackItem.getFile();
		FragmentActivity activity = getActivity();
		if (activity != null && file != null) {
			GpxFileLoaderTask.loadGpxFile(file, activity, callback);
		}
	}

	private void moveGpxFile(@NonNull File file) {
		FragmentManager manager = getFragmentManager();
		if (manager != null) {
			MoveGpxFileBottomSheet.showInstance(manager, file, this, false, false);
		}
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
	public void renamedTo(File file) {
		reloadTracks();
	}

	private void onTrackItemsSelected(@NonNull Set<TrackItem> trackItems) {
		for (Fragment fragment : getChildFragmentManager().getFragments()) {
			if (fragment instanceof TrackItemsContainer) {
				((TrackItemsContainer) fragment).onTrackItemsSelected(trackItems);
			}
		}
	}

	public void updateTabsContent() {
		for (Fragment fragment : getChildFragmentManager().getFragments()) {
			if (fragment instanceof TrackItemsContainer) {
				((TrackItemsContainer) fragment).updateContent();
			}
		}
	}

	public static void showInstance(@NonNull FragmentManager manager) {
		showInstance(manager, null);
	}

	public static void showInstance(@NonNull FragmentManager manager, @Nullable String preselectedTabName) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			TracksFragment fragment = new TracksFragment();
			fragment.preselectedTabName = preselectedTabName;
			fragment.setRetainInstance(true);
			fragment.show(manager, TAG);
		}
	}
}