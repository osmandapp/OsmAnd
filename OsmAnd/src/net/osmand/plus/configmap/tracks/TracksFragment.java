package net.osmand.plus.configmap.tracks;

import static net.osmand.plus.configmap.tracks.TracksAdapter.TYPE_NO_TRACKS;
import static net.osmand.plus.configmap.tracks.TracksAdapter.TYPE_NO_VISIBLE_TRACKS;
import static net.osmand.plus.configmap.tracks.TracksAdapter.TYPE_RECENTLY_VISIBLE_TRACKS;
import static net.osmand.plus.helpers.GpxUiHelper.loadGPXFileInDifferentThread;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import net.osmand.CallbackWithObject;
import net.osmand.IndexConstants;
import net.osmand.gpx.GPXFile;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.configmap.tracks.TracksAdapter.TracksVisibilityListener;
import net.osmand.plus.dashboard.DashboardOnMap;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.importfiles.ImportHelper;
import net.osmand.plus.myplaces.ui.LoadGpxInfosTask;
import net.osmand.plus.myplaces.ui.LoadGpxInfosTask.LoadTracksListener;
import net.osmand.plus.track.helpers.GpxSelectionHelper;
import net.osmand.plus.track.helpers.SelectedGpxFile;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.widgets.popup.PopUpMenuHelper;
import net.osmand.plus.widgets.popup.PopUpMenuItem;
import net.osmand.util.Algorithms;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TracksFragment extends BaseOsmAndFragment implements LoadTracksListener, TracksVisibilityListener {

	private static final String TAG = TracksFragment.class.getSimpleName();

	private OsmandApplication app;
	private GpxSelectionHelper selectionHelper;

	private final Set<GPXInfo> selectedGpxInfo = new HashSet<>();
	private final Set<GPXInfo> originalSelectedGpxInfo = new HashSet<>();
	private final Set<GPXInfo> recentlyVisibleGpxInfo = new HashSet<>();
	private final Map<String, TrackTab> trackTabs = new LinkedHashMap<>();

	private LoadGpxInfosTask asyncLoader;

	private ViewPager2 viewPager;
	private TracksTabAdapter adapter;
	private TextView applyButton;
	private TextView selectionButton;

	private boolean nightMode;

	@ColorRes
	public int getStatusBarColorId() {
		AndroidUiHelper.setStatusBarContentColor(getView(), nightMode);
		return ColorUtilities.getActivityBgColorId(nightMode);
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = requireMyApplication();
		selectionHelper = app.getSelectedGpxHelper();
		nightMode = isNightMode(false);

		FragmentActivity activity = requireActivity();
		activity.getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
			public void handleOnBackPressed() {
				dismiss();
			}
		});
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		FragmentActivity activity = requireActivity();
		inflater = UiUtilities.getInflater(activity, nightMode);
		View view = inflater.inflate(R.layout.tracks_fragment, container, false);
		AndroidUtils.addStatusBarPadding21v(activity, view);

		setupToolbar(view);
		setupTabLayout(view);
		setupButtons(view);

		return view;
	}

	private void setupButtons(@NonNull View view) {
		boolean hasVisibleTracks = !originalSelectedGpxInfo.isEmpty();
		selectionButton = view.findViewById(R.id.selection_button);
		selectionButton.setOnClickListener(v -> {
			if (hasVisibleTracks) {
				selectionHelper.clearAllGpxFilesToShow(true);
			} else {
				selectionHelper.restoreSelectedGpxFiles();
			}
		});
		selectionButton.setText(hasVisibleTracks ? R.string.shared_string_hide_all : R.string.shared_string_select_recent);

		applyButton = view.findViewById(R.id.apply_button);
		applyButton.setOnClickListener(v -> updateTracksVisibility());

		int disabledColor = ColorUtilities.getSecondaryTextColorId(nightMode);
		int activeColor = nightMode ? R.color.active_color_primary_dark : R.color.button_color_active_light;
		ColorStateList colorStateList = AndroidUtils.createEnabledColorStateList(app, disabledColor, activeColor);

		applyButton.setTextColor(colorStateList);
		selectionButton.setTextColor(colorStateList);

		updateButtonsState();
	}

	private void updateTracksVisibility() {
		MapActivity activity = getMapActivity();
		if (activity != null) {
			selectionHelper.clearAllGpxFilesToShow(false);

			OsmandMapTileView mapView = activity.getMapView();
			DashboardOnMap dashboard = activity.getDashboard();
			CallbackWithObject<GPXFile[]> callback = result -> {
				app.getSelectedGpxHelper().setGpxFileToDisplay(result);
				mapView.refreshMap();
				if (dashboard.isVisible()) {
					dashboard.refreshContent(false);
				}
				return true;
			};
			File dir = app.getAppPath(IndexConstants.GPX_INDEX_DIR);
			List<String> selectedGpxNames = new ArrayList<>();
			for (GPXInfo gpxInfo : selectedGpxInfo) {
				if (!gpxInfo.isCurrentRecordingTrack()) {
					String fileName = gpxInfo.getFileName();
					String name = Algorithms.isEmpty(gpxInfo.subfolder) ? fileName : gpxInfo.subfolder + "/" + fileName;
					selectedGpxNames.add(name);
				}
			}
			loadGPXFileInDifferentThread(activity, callback, dir, null,
					selectedGpxNames.toArray(new String[0]));
		}
		adapter.notifyDataSetChanged();
	}

	protected void updateButtonsState() {
		applyButton.setEnabled(!Algorithms.objectEquals(selectedGpxInfo, originalSelectedGpxInfo));
		selectionButton.setEnabled(!originalSelectedGpxInfo.isEmpty() || !selectionHelper.getSelectedGpxFilesBackUp().isEmpty());
	}

	private void setupToolbar(@NonNull View view) {
		ViewCompat.setElevation(view.findViewById(R.id.appbar), 5.0f);

		Toolbar toolbar = view.findViewById(R.id.toolbar);
		toolbar.findViewById(R.id.back_button).setOnClickListener(v -> dismiss());
		toolbar.findViewById(R.id.switch_group).setOnClickListener(v -> {
			FragmentActivity activity = getActivity();
			if (activity != null) {
				TrackGroupsBottomSheet.showInstance(activity.getSupportFragmentManager(), this);
			}
		});
		toolbar.findViewById(R.id.actions_button).setOnClickListener(this::showOptionsMenu);
	}

	private void showOptionsMenu(@NonNull View view) {
		List<PopUpMenuItem> items = new ArrayList<>();
//		items.add(new PopUpMenuItem.Builder(view.getContext())
//				.setTitleId(R.string.change_appearance)
//				.setIcon(getContentIcon(R.drawable.ic_action_appearance))
//				.setOnClickListener(v -> changeAppearance()).create());
		items.add(new PopUpMenuItem.Builder(view.getContext())
				.setTitleId(R.string.shared_string_import)
				.setIcon(getContentIcon(R.drawable.ic_action_import_to))
				.setOnClickListener(v -> importTracks()).create());
		new PopUpMenuHelper.Builder(view, items, nightMode).show();
	}

	private void setupTabLayout(@NonNull View view) {
		adapter = new TracksTabAdapter(this);

		viewPager = view.findViewById(R.id.view_pager);
		viewPager.setAdapter(adapter);

		TabLayout tabLayout = view.findViewById(R.id.tab_layout);
		TabLayoutMediator mediator = new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
			List<String> names = new ArrayList<>(trackTabs.keySet());
			tab.setText(Algorithms.getFileWithoutDirs(names.get(position)));
		});
		mediator.attach();

		int color = ColorUtilities.getSelectedProfileColor(app, nightMode);
		tabLayout.setSelectedTabIndicatorColor(color);
		tabLayout.setTabTextColors(ColorUtilities.getPrimaryTextColor(app, nightMode), color);
	}

	@NonNull
	public Set<GPXInfo> getSelectedGpxInfo() {
		return selectedGpxInfo;
	}

	@NonNull
	public List<TrackTab> getTrackTabs() {
		return new ArrayList<>(trackTabs.values());
	}

	@NonNull
	public TrackTab getSelectedTab() {
		return trackTabs.get(viewPager.getCurrentItem());
	}

	public void setSelectedTab(int position) {
		viewPager.setCurrentItem(position);
	}

	private void updateTrackTabs(@NonNull Map<String, TrackTab> folderTabs, @NonNull List<GPXInfo> gpxInfos) {
		trackTabs.clear();

		TrackTab allTab = getAllTracksTab(gpxInfos);
		TrackTab mapTab = getTracksOnMapTab(gpxInfos);

		trackTabs.put(mapTab.name, mapTab);
		trackTabs.put(allTab.name, allTab);
		trackTabs.putAll(folderTabs);

		adapter.setTrackTabs(trackTabs);

		updateButtonsState();
	}

	private TrackTab getAllTracksTab(@NonNull List<GPXInfo> gpxInfos) {
		TrackTab trackTab = new TrackTab(getString(R.string.shared_string_all), TrackTabType.ALL);
		trackTab.items.addAll(gpxInfos);
		return trackTab;
	}

	private TrackTab getTracksOnMapTab(@NonNull List<GPXInfo> gpxInfos) {
		TrackTab trackTab = new TrackTab(getString(R.string.shared_string_on_map), TrackTabType.ON_MAP);

		if (selectionHelper.isAnyGpxFileSelected()) {
			for (GPXInfo info : gpxInfos) {
				SelectedGpxFile selectedGpxFile = selectionHelper.getSelectedFileByName(info.getFileName());
				if (selectedGpxFile != null) {
					trackTab.items.add(info);
					selectedGpxInfo.add(info);
					originalSelectedGpxInfo.add(info);
				}
			}
		} else if (Algorithms.isEmpty(gpxInfos)) {
			trackTab.items.add(TYPE_NO_TRACKS);
		} else {
			trackTab.items.add(TYPE_NO_VISIBLE_TRACKS);
		}
		Map<GPXFile, Long> selectedGpxFilesBackUp = selectionHelper.getSelectedGpxFilesBackUp();
		if (!selectedGpxFilesBackUp.isEmpty()) {
			trackTab.items.add(TYPE_RECENTLY_VISIBLE_TRACKS);
			for (GPXFile gpxFile : selectedGpxFilesBackUp.keySet()) {
				File file = new File(gpxFile.path);
				GPXInfo info = new GPXInfo(file.getName(), file);
				trackTab.items.add(info);
				recentlyVisibleGpxInfo.add(info);
			}
		}
		return trackTab;
	}

	public void reloadTracks() {
		asyncLoader = new LoadGpxInfosTask(app, this);
		asyncLoader.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	@Override
	public void onResume() {
		super.onResume();

		if (asyncLoader == null || asyncLoader.getGpxInfos() == null) {
			reloadTracks();
		} else {
			adapter.notifyDataSetChanged();
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (asyncLoader != null && asyncLoader.getStatus() == AsyncTask.Status.RUNNING) {
			asyncLoader.cancel(false);
		}
	}

	@Override
	public void loadTracksStarted() {

	}

	@Override
	public void loadTracksProgress(GPXInfo[] gpxInfos) {

	}

	@Override
	public void loadTracksFinished() {
		List<GPXInfo> gpxInfos = asyncLoader.getGpxInfos();
		Map<String, TrackTab> trackTabs = new LinkedHashMap<>();
		for (GPXInfo info : gpxInfos) {
			addLocalIndexInfo(trackTabs, info);
		}
		updateTrackTabs(trackTabs, gpxInfos);
	}

	public void addLocalIndexInfo(@NonNull Map<String, TrackTab> trackTabs, @NonNull GPXInfo info) {
		String name = info.isCurrentRecordingTrack() ? info.getName() : info.subfolder;
		if (Algorithms.isEmpty(name)) {
			name = getString(R.string.shared_string_tracks);
		}

		TrackTab trackTab = trackTabs.get(name);
		if (trackTab == null) {
			trackTab = new TrackTab(name, TrackTabType.FOLDER);
			trackTabs.put(name, trackTab);
		}
		trackTab.items.add(info);
	}

	protected void dismiss() {
		FragmentManager manager = getFragmentManager();
		if (manager != null && !manager.isStateSaved()) {
			manager.popBackStack(TAG, FragmentManager.POP_BACK_STACK_INCLUSIVE);
		}
	}

	public static void showInstance(@NonNull FragmentManager manager) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			TracksFragment fragment = new TracksFragment();
			fragment.setRetainInstance(true);
			manager.beginTransaction()
					.add(R.id.fragmentContainer, fragment, TAG)
					.addToBackStack(TAG)
					.commit();
		}
	}

	@Override
	public void onTrackItemSelected(@NonNull GPXInfo gpxInfo, boolean selected) {
		if (selected) {
			selectedGpxInfo.add(gpxInfo);
		} else {
			selectedGpxInfo.remove(gpxInfo);
		}
		for (Fragment fragment : getChildFragmentManager().getFragments()) {
			if (fragment instanceof TracksTreeFragment) {
				TracksTreeFragment tracksTreeFragment = (TracksTreeFragment) fragment;
				tracksTreeFragment.onTrackItemSelected(gpxInfo);
			}
		}
		updateButtonsState();
	}

	public void importTracks() {
		Intent intent = ImportHelper.getImportTrackIntent();
		AndroidUtils.startActivityForResultIfSafe(this, intent, 1001);
	}

	@Override
	public void showAllTracks() {
		viewPager.setCurrentItem(1);
	}

	@Override
	public void selectRecentlyVisibleTracks(boolean selected) {
		if (selected) {
			selectedGpxInfo.addAll(recentlyVisibleGpxInfo);
		} else {
			selectedGpxInfo.removeAll(recentlyVisibleGpxInfo);
		}
		for (Fragment fragment : getChildFragmentManager().getFragments()) {
			if (fragment instanceof TracksTreeFragment) {
				TracksTreeFragment tracksTreeFragment = (TracksTreeFragment) fragment;
				tracksTreeFragment.updateContent();
			}
		}
		updateButtonsState();
	}

	public MapActivity getMapActivity() {
		return (MapActivity) getActivity();
	}

	@Override
	public boolean isTrackSelected(@NonNull GPXInfo gpxInfo) {
		return selectedGpxInfo.contains(gpxInfo);
	}

	@Override
	public boolean isRecentlyTracksSelected() {
		return selectedGpxInfo.containsAll(recentlyVisibleGpxInfo);
	}
}