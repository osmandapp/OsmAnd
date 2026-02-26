package net.osmand.plus.track;

import static net.osmand.plus.configmap.tracks.TracksAdapter.TYPE_RECENTLY_VISIBLE_TRACKS;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.CallbackWithObject;
import net.osmand.plus.shared.SharedUtil;
import net.osmand.shared.gpx.GpxFile;
import net.osmand.plus.R;
import net.osmand.shared.gpx.TrackItem;
import net.osmand.plus.configmap.tracks.TrackTab;
import net.osmand.plus.configmap.tracks.TracksAdapter.ItemVisibilityCallback;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.settings.enums.TracksSortMode;
import net.osmand.shared.gpx.data.TrackFolder;
import net.osmand.shared.gpx.GpxDataItem;
import net.osmand.plus.track.helpers.GpxSelectionHelper;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.shared.io.KFile;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class SelectTrackTabsFragment extends BaseTracksTabsFragment {

	public static final String TAG = SelectTrackTabsFragment.class.getSimpleName();

	private Object fileSelectionListener;
	private ItemVisibilityCallback itemVisibilityCallback;

	public boolean getContentStatusBarNightMode() {
		return nightMode;
	}

	@NonNull
	@Override
	public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
		Activity activity = requireActivity();
		int themeId = nightMode ? R.style.OsmandDarkTheme_DarkActionbar : R.style.OsmandLightTheme_DarkActionbar_LightStatusBar;
		Dialog dialog = new Dialog(activity, themeId);
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
		View view = themedInflater.inflate(R.layout.select_track_fragment, container, false);

		setupToolbar(view);
		setupTabLayout(view);

		return view;
	}

	private void setupToolbar(@NonNull View view) {
		progressBar = view.findViewById(R.id.progress_bar);
		View appbar = view.findViewById(R.id.appbar);
		ViewCompat.setElevation(appbar, 5.0f);
		appbar.setBackgroundColor(ContextCompat.getColor(app, nightMode ? R.color.app_bar_main_dark : R.color.card_and_list_background_light));

		Toolbar toolbar = view.findViewById(R.id.toolbar);
		toolbar.findViewById(R.id.back_button).setOnClickListener(v -> dismiss());
	}

	protected void setTabs(@NonNull List<TrackTab> tabs) {
		tabSize = tabs.size();
		setViewPagerAdapter(viewPager, tabs);
		tabLayout.setViewPager(viewPager);
		viewPager.setCurrentItem(0);
	}

	@Override
	public void loadTracksProgress(@NonNull TrackItem... items) {
	}

	@Override
	public void tracksLoaded(@NonNull TrackFolder folder) {
	}

	@Override
	public void loadTracksFinished(@NonNull TrackFolder folder) {
		trackTabsHelper.updateItems(folder);
		AndroidUiHelper.updateVisibility(progressBar, false);
		updateTrackTabs();
		updateTabsContent();
	}

	@Override
	public void deferredLoadTracksFinished(@NonNull TrackFolder folder) {
	}

	@Override
	public void onTrackFolderSelected(@NonNull TrackFolder trackFolder) {
		FragmentActivity activity = getActivity();
		if (activity != null && trackFolder.getParentFolder() != null) {
			FragmentManager manager = activity.getSupportFragmentManager();
			SelectTrackFolderFragment.showInstance(manager, this, getTracksSortMode(), fileSelectionListener, trackFolder.getParentFolder(), trackFolder, itemVisibilityCallback);
		}
	}

	protected void addTrackItem(@NonNull TrackItem item) {
		trackTabsHelper.addTrackItem(item);
		updateTrackTabs();
		setSelectedTab("import");
		updateTabsContent();
	}

	@Override
	public void setTracksSortMode(@NonNull TracksSortMode sortMode, boolean sortSubFolders) {
		TrackTab trackTab = getSelectedTab();
		if (trackTab != null) {
			trackTab.setSortMode(sortMode);
			trackTabsHelper.sortTrackTab(trackTab);
			updateTabsContent();
		}
	}

	@Override
	public void onTrackItemsSelected(@NonNull Set<TrackItem> trackItems, boolean selected) {
		TrackItem firstTrackItem = trackItems.iterator().next();
		if (fileSelectionListener instanceof CallbackWithObject) {
			((CallbackWithObject<String>) fileSelectionListener).processResult(firstTrackItem.getPath());
		} else if (fileSelectionListener instanceof GpxFileSelectionListener) {
			KFile file = firstTrackItem.getFile();
			GpxSelectionHelper.getGpxFile(requireActivity(), file == null ? null : SharedUtil.jFile(file), true, result -> {
				((GpxFileSelectionListener) fileSelectionListener).onSelectGpxFile(result);
				return true;
			});
		} else if (fileSelectionListener instanceof GpxDataItemSelectionListener) {
			((GpxDataItemSelectionListener) fileSelectionListener).onSelectGpxDataItem(firstTrackItem.getDataItem());
		}
		dismiss();
	}

	@Nullable
	public TrackTab getTab(@NonNull String name) {
		for (TrackTab trackTab : getTrackTabs()) {
			if (Algorithms.stringsEqual(name, trackTab.getTypeName())) {
				updateTrackItemsVisibility(trackTab);
				return trackTab;
			}
		}
		return null;
	}

	private void updateTrackItemsVisibility(TrackTab trackTab) {
		if (itemVisibilityCallback != null) {
			List<Object> items = new ArrayList<>();
			for (Object object : trackTab.items) {
				if (object instanceof TrackItem && !itemVisibilityCallback.shouldShowItem((TrackItem) object)) {
					items.add(object);
				}
			}
			trackTab.items.removeAll(items);
			Object lastItem = trackTab.items.get(trackTab.items.size() - 1);
			if (lastItem instanceof Integer && (Integer) lastItem == TYPE_RECENTLY_VISIBLE_TRACKS) {
				trackTab.items.remove(lastItem);
			}
		}
	}

	@Override
	public boolean selectionMode() {
		return false;
	}

	@Override
	public boolean selectTrackMode() {
		return true;
	}

	public static void showInstance(@NonNull FragmentManager manager, Object fileSelectionListener) {
		showInstance(manager, fileSelectionListener, null);
	}

	public static void showInstance(@NonNull FragmentManager manager, Object fileSelectionListener, @Nullable ItemVisibilityCallback itemVisibilityCallback) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			SelectTrackTabsFragment fragment = new SelectTrackTabsFragment();
			fragment.fileSelectionListener = fileSelectionListener;
			fragment.itemVisibilityCallback = itemVisibilityCallback;
			fragment.setRetainInstance(true);
			fragment.show(manager, TAG);
		}
	}

	public interface GpxFileSelectionListener {
		void onSelectGpxFile(@NonNull GpxFile gpxFile);
	}

	public interface GpxDataItemSelectionListener {
		void onSelectGpxDataItem(@Nullable GpxDataItem gpxDataItem);
	}
}