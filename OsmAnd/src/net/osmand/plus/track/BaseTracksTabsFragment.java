package net.osmand.plus.track;

import static net.osmand.IndexConstants.GPX_INDEX_DIR;
import static net.osmand.plus.configmap.tracks.TrackFolderLoaderTask.LoadTracksListener;
import static net.osmand.plus.configmap.tracks.TrackFolderLoaderTask.Status;
import static net.osmand.plus.importfiles.ImportHelper.IMPORT_FILE_REQUEST;
import static net.osmand.plus.importfiles.OnSuccessfulGpxImport.OPEN_GPX_CONTEXT_MENU;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.viewpager.widget.ViewPager;

import net.osmand.gpx.GPXFile;
import net.osmand.plus.R;
import net.osmand.plus.base.BaseOsmAndDialogFragment;
import net.osmand.plus.configmap.tracks.SortByBottomSheet;
import net.osmand.plus.configmap.tracks.TrackFolderLoaderTask;
import net.osmand.plus.configmap.tracks.TrackItem;
import net.osmand.plus.configmap.tracks.TrackItemsContainer;
import net.osmand.plus.configmap.tracks.TrackTab;
import net.osmand.plus.configmap.tracks.TrackTabsHelper;
import net.osmand.plus.configmap.tracks.TracksTabAdapter;
import net.osmand.plus.configmap.tracks.viewholders.EmptyTracksViewHolder.EmptyTracksListener;
import net.osmand.plus.configmap.tracks.viewholders.SortTracksViewHolder.SortTracksListener;
import net.osmand.plus.configmap.tracks.viewholders.TrackViewHolder.TrackSelectionListener;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.helpers.IntentHelper;
import net.osmand.plus.importfiles.GpxImportListener;
import net.osmand.plus.importfiles.ImportHelper;
import net.osmand.plus.importfiles.MultipleTracksImportListener;
import net.osmand.plus.importfiles.OnSuccessfulGpxImport;
import net.osmand.plus.myplaces.tracks.ItemsSelectionHelper;
import net.osmand.plus.myplaces.tracks.ItemsSelectionHelper.SelectionHelperProvider;
import net.osmand.plus.myplaces.tracks.dialogs.MoveGpxFileBottomSheet.OnTrackFileMoveListener;
import net.osmand.plus.settings.enums.TracksSortMode;
import net.osmand.plus.track.data.TrackFolder;
import net.osmand.plus.track.helpers.GpxSelectionHelper;
import net.osmand.plus.track.helpers.SelectGpxTask.SelectGpxTaskListener;
import net.osmand.plus.track.helpers.SelectedGpxFile;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.FileUtils;
import net.osmand.plus.utils.FileUtils.RenameCallback;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.controls.PagerSlidingTabStrip;
import net.osmand.util.Algorithms;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public abstract class BaseTracksTabsFragment extends BaseOsmAndDialogFragment implements LoadTracksListener,
		SelectionHelperProvider<TrackItem>, OnTrackFileMoveListener, RenameCallback,
		TrackSelectionListener, SortTracksListener, EmptyTracksListener, SelectGpxTaskListener {

	protected ImportHelper importHelper;
	protected TrackTabsHelper trackTabsHelper;
	protected GpxSelectionHelper gpxSelectionHelper;
	protected ItemsSelectionHelper<TrackItem> itemsSelectionHelper;
	protected TrackFolderLoaderTask asyncLoader;

	protected ViewPager viewPager;
	protected PagerSlidingTabStrip tabLayout;
	protected ProgressBar progressBar;
	protected TracksTabAdapter adapter;
	protected int tabSize;

	@Override
	protected boolean isUsedOnMap() {
		return true;
	}

	@ColorRes
	public int getStatusBarColorId() {
		AndroidUiHelper.setStatusBarContentColor(getView(), nightMode);
		return nightMode ? R.color.status_bar_main_dark : R.color.activity_background_color_light;
	}

	@NonNull
	public TrackTabsHelper getTrackTabsHelper() {
		return trackTabsHelper;
	}

	@NonNull
	@Override
	public ItemsSelectionHelper<TrackItem> getSelectionHelper() {
		return itemsSelectionHelper;
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		importHelper = app.getImportHelper();
		trackTabsHelper = new TrackTabsHelper(app);
		gpxSelectionHelper = app.getSelectedGpxHelper();
		itemsSelectionHelper = trackTabsHelper.getItemsSelectionHelper();
	}

	protected void setupTabLayout(@NonNull View view) {
		viewPager = view.findViewById(R.id.view_pager);
		List<TrackTab> tabs = getTrackTabs();
		tabLayout = view.findViewById(R.id.sliding_tabs);
		tabLayout.setTabBackground(nightMode ? R.color.app_bar_main_dark : R.color.card_and_list_background_light);
		tabLayout.setCustomTabProvider(new PagerSlidingTabStrip.CustomTabProvider() {
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
				textView.setText(trackTab.getName(app));
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

	@NonNull
	public List<TrackTab> getTrackTabs() {
		return new ArrayList<>(trackTabsHelper.getTrackTabs().values());
	}

	protected void setViewPagerAdapter(@NonNull ViewPager pager, List<TrackTab> items) {
		adapter = new TracksTabAdapter(app, getChildFragmentManager(), items);
		pager.setAdapter(adapter);
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
			FragmentManager manager = activity.getSupportFragmentManager();
			SortByBottomSheet.showInstance(manager, getTracksSortMode(), this, isUsedOnMap());
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
		gpxSelectionHelper.addListener(this);
	}

	@Override
	public void onPause() {
		super.onPause();
		gpxSelectionHelper.removeListener(this);
	}

	protected void reloadTracks() {
		File gpxDir = FileUtils.getExistingDir(app, GPX_INDEX_DIR);
		TrackFolder folder = new TrackFolder(gpxDir, null);
		asyncLoader = new TrackFolderLoaderTask(app, folder, this);
		asyncLoader.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	@Override
	public void loadTracksStarted() {
		AndroidUiHelper.updateVisibility(progressBar, true);
	}

	protected void updateTrackTabs() {
		adapter.setTrackTabs(trackTabsHelper.getTrackTabs());
	}

	@Override
	public void onGpxSelectionInProgress(@NonNull SelectedGpxFile selectedGpxFile) {
		AndroidUiHelper.updateVisibility(progressBar, true);
		TrackItem item = findTrackItem(selectedGpxFile);
		if (item != null) {
			itemsSelectionHelper.addItemToOriginalSelected(item);
			itemsSelectionHelper.onItemsSelected(Collections.singleton(item), true);
			updateItems(Collections.singleton(item));
		}
	}

	protected void updateItems(@NonNull Set<TrackItem> trackItems) {
		for (Fragment fragment : getChildFragmentManager().getFragments()) {
			if (fragment instanceof TrackItemsContainer) {
				((TrackItemsContainer) fragment).updateItems(trackItems);
			}
		}
	}

	@Nullable
	private TrackItem findTrackItem(@NonNull SelectedGpxFile selectedGpxFile) {
		for (TrackItem item : itemsSelectionHelper.getAllItems()) {
			if (Algorithms.stringsEqual(selectedGpxFile.getGpxFile().path, item.getPath())) {
				return item;
			}
		}
		return null;
	}

	@Override
	public void onGpxSelectionFinished() {
		AndroidUiHelper.updateVisibility(progressBar, isLoadingTracks());
		trackTabsHelper.processVisibleTracks();
		trackTabsHelper.processRecentlyVisibleTracks();
		trackTabsHelper.updateTracksOnMap();
		updateTabsContent();
	}

	public void updateTabsContent() {
		for (Fragment fragment : getChildFragmentManager().getFragments()) {
			if (fragment instanceof TrackItemsContainer) {
				((TrackItemsContainer) fragment).updateContent();
			}
		}
	}

	public boolean isLoadingTracks() {
		return asyncLoader != null && asyncLoader.getStatus() == Status.RUNNING;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		if (isLoadingTracks() && !requireActivity().isChangingConfigurations()) {
			asyncLoader.cancel(false);
		}
	}

	@Override
	public void importTracks() {
		Intent intent = ImportHelper.getImportFileIntent();
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

	abstract protected void addTrackItem(@NonNull TrackItem item);

	abstract protected void setTabs(@NonNull List<TrackTab> tabs);

	public boolean selectionMode() {
		return true;
	}

	public boolean selectTrackMode() {
		return false;
	}

	@Override
	public void onFileMove(@Nullable File src, @NonNull File dest) {
	}

	@Override
	public void fileRenamed(@NonNull File src, @NonNull File dest) {
	}
}