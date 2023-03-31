package net.osmand.plus.configmap.tracks;

import static net.osmand.plus.importfiles.ImportHelper.IMPORT_FILE_REQUEST;
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

import net.osmand.gpx.GPXFile;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseOsmAndDialogFragment;
import net.osmand.plus.configmap.tracks.TrackItemsLoaderTask.LoadTracksListener;
import net.osmand.plus.dashboard.DashboardOnMap;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.helpers.IntentHelper;
import net.osmand.plus.importfiles.ImportHelper;
import net.osmand.plus.importfiles.ImportHelper.GpxImportListener;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.controls.PagerSlidingTabStrip;
import net.osmand.plus.views.controls.PagerSlidingTabStrip.CustomTabProvider;
import net.osmand.plus.widgets.popup.PopUpMenu;
import net.osmand.plus.widgets.popup.PopUpMenuDisplayData;
import net.osmand.plus.widgets.popup.PopUpMenuItem;
import net.osmand.util.Algorithms;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class TracksFragment extends BaseOsmAndDialogFragment implements LoadTracksListener {

	public static final String TAG = TracksFragment.class.getSimpleName();

	private ImportHelper importHelper;
	private SelectedTracksHelper selectedTracksHelper;
	private TrackItemsLoaderTask asyncLoader;

	private ViewPager viewPager;
	private PagerSlidingTabStrip tabLayout;
	private ProgressBar progressBar;
	private TracksTabAdapter adapter;

	private View applyButton;
	private View selectionButton;

	private boolean nightMode;
	private int tabSize;

	@NonNull
	public SelectedTracksHelper getSelectedTracksHelper() {
		return selectedTracksHelper;
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
		nightMode = isNightMode(true);
	}

	@NonNull
	@Override
	public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
		Activity activity = requireActivity();
		int themeId = nightMode ? R.style.OsmandDarkTheme_DarkActionbar : R.style.OsmandLightTheme_DarkActionbar_LightStatusBar;
		Dialog dialog = new Dialog(activity, themeId) {
			@Override
			public void onBackPressed() {
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
		inflater = UiUtilities.getInflater(requireActivity(), nightMode);
		View view = inflater.inflate(R.layout.tracks_fragment, container, false);

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
		ImageView switchGroup = toolbar.findViewById(R.id.switch_group);
		ImageView actionsButton = toolbar.findViewById(R.id.actions_button);

		switchGroup.setOnClickListener(v -> {
			FragmentActivity activity = getActivity();
			if (activity != null) {
				TrackGroupsBottomSheet.showInstance(activity.getSupportFragmentManager(), this);
			}
		});
		actionsButton.setOnClickListener(this::showOptionsMenu);
		toolbar.findViewById(R.id.back_button).setOnClickListener(v -> dismiss());

		int iconColor = ColorUtilities.getColor(app, nightMode ? R.color.icon_color_default_dark : R.color.icon_color_default_light);
		switchGroup.setImageTintList(ColorStateList.valueOf(iconColor));
		actionsButton.setImageTintList(ColorStateList.valueOf(iconColor));
	}

	private void showOptionsMenu(@NonNull View view) {
		List<PopUpMenuItem> items = new ArrayList<>();

		String appearance = getString(R.string.change_appearance);
		String count = "(" + selectedTracksHelper.getSelectedTracks().size() + ")";
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
		applyButton.setOnClickListener(v -> saveChanges());

		selectionButton = view.findViewById(R.id.selection_button);
		selectionButton.setOnClickListener(v -> {
			Set<TrackItem> selectedTracks = selectedTracksHelper.getSelectedTracks();
			if (Algorithms.isEmpty(selectedTracks)) {
				onTrackItemsSelected(selectedTracksHelper.getRecentlyVisibleTracks(), true);
			} else {
				onTrackItemsSelected(selectedTracks, false);
			}
		});
		updateButtonsState();
	}

	private void updateButtonsState() {
		boolean anySelected = !Algorithms.isEmpty(selectedTracksHelper.getSelectedTracks());
		String apply = getString(R.string.shared_string_apply).toUpperCase();
		String select = getString(anySelected ? R.string.shared_string_hide_all : R.string.shared_string_select_recent).toUpperCase();

		applyButton.setEnabled(selectedTracksHelper.hasItemsToApply());
		selectionButton.setEnabled(!Algorithms.isEmpty(selectedTracksHelper.getRecentlyVisibleTracks()) || anySelected);

		UiUtilities.setupDialogButton(nightMode, applyButton, TERTIARY, apply);
		UiUtilities.setupDialogButton(nightMode, selectionButton, TERTIARY, select);
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

	public void showSortByDialog() {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			SortByBottomSheet.showInstance(activity.getSupportFragmentManager(), this);
		}
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
		asyncLoader = new TrackItemsLoaderTask(app, this);
		asyncLoader.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	@Override
	public void loadTracksStarted() {
		AndroidUiHelper.updateVisibility(progressBar, true);
	}

	@Override
	public void loadTracksFinished() {
		AndroidUiHelper.updateVisibility(progressBar, false);
		selectedTracksHelper.updateTrackItems(asyncLoader.getTrackItems());
		updateTrackTabs();
		updateButtonsState();
	}

	private void updateTrackTabs() {
		adapter.setTrackTabs(selectedTracksHelper.getTrackTabs());
	}

	private void saveChanges() {
		selectedTracksHelper.saveTabsSortModes();
		selectedTracksHelper.saveTracksVisibility();

		FragmentActivity activity = getActivity();
		if (activity instanceof MapActivity) {
			MapActivity mapActivity = (MapActivity) activity;
			DashboardOnMap dashboard = mapActivity.getDashboard();
			if (dashboard.isVisible()) {
				dashboard.refreshContent(false);
			}
		}
		app.getOsmandMap().getMapView().refreshMap();
		dismissAllowingStateLoss();
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
					AndroidUiHelper.updateVisibility(progressBar, true);
					importHelper.setGpxImportListener(getGpxImportListener(filesUri.size()));
					importHelper.handleGpxFilesImport(filesUri, true);
				}
			}
		} else {
			super.onActivityResult(requestCode, resultCode, data);
		}
	}

	@NonNull
	private GpxImportListener getGpxImportListener(int filesSize) {
		return new GpxImportListener() {
			private int importCounter;

			@Override
			public void onImportComplete(boolean success) {
				if (!success) {
					importCounter++;
				}
				checkImportFinished();
			}

			@Override
			public void onSaveComplete(boolean success, GPXFile gpxFile) {
				if (isAdded() && success) {
					addTrackItem(new TrackItem(new File(gpxFile.path)));
				}
				importCounter++;
				checkImportFinished();
			}

			private void checkImportFinished() {
				if (importCounter == filesSize) {
					importHelper.setGpxImportListener(null);
					AndroidUiHelper.updateVisibility(progressBar, false);
				}
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

	public void setTracksSortMode(@NonNull TracksSortMode sortMode) {
		TrackTab trackTab = getSelectedTab();
		if (trackTab != null) {
			trackTab.setSortMode(sortMode);
			selectedTracksHelper.sortTrackTab(trackTab);
			selectedTracksHelper.saveTabsSortModes();
			updateTabsContent();
		}
	}

	public void onTrackItemsSelected(@NonNull Set<TrackItem> trackItems, boolean selected) {
		selectedTracksHelper.ontrackItemsSelected(trackItems, selected);
		onTrackItemsSelected(trackItems);
		updateButtonsState();
	}

	private void onTrackItemsSelected(@NonNull Set<TrackItem> trackItems) {
		for (Fragment fragment : getChildFragmentManager().getFragments()) {
			if (fragment instanceof TrackItemsFragment) {
				((TrackItemsFragment) fragment).onTrackItemsSelected(trackItems);
			}
		}
	}

	public void updateTabsContent() {
		for (Fragment fragment : getChildFragmentManager().getFragments()) {
			if (fragment instanceof TrackItemsFragment) {
				((TrackItemsFragment) fragment).updateContent();
			}
		}
	}

	public static void showInstance(@NonNull FragmentManager manager) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			TracksFragment fragment = new TracksFragment();
			fragment.setRetainInstance(true);
			fragment.show(manager, TAG);
		}
	}
}