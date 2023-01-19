package net.osmand.plus.configmap;

import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import net.osmand.IndexConstants;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.myplaces.ui.GpxInfo;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.popup.PopUpMenuHelper;
import net.osmand.plus.widgets.popup.PopUpMenuItem;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class TracksFragment extends BaseOsmAndFragment {

	private static final String TAG = TracksFragment.class.getSimpleName();

	private OsmandApplication app;
	private OsmandSettings settings;
	private UiUtilities iconsCache;

	private final List<TrackTab> trackTabs = new ArrayList<>();

	private TabLayout tabLayout;
	private ViewPager2 viewPager;
	private TracksTabAdapter tabAdapter;

	private boolean nightMode;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = requireMyApplication();
		settings = app.getSettings();
		iconsCache = app.getUIUtilities();
		nightMode = isNightMode(false);
	}

	@ColorRes
	public int getStatusBarColorId() {
		AndroidUiHelper.setStatusBarContentColor(getView(), nightMode);
		return ColorUtilities.getActivityBgColorId(nightMode);
	}

	@NonNull
	public List<TrackTab> getTrackTabs() {
		return trackTabs;
	}

	@NonNull
	public TrackTab getSelectedTab() {
		return trackTabs.get(viewPager.getCurrentItem());
	}

	public void setSelectedTab(int position) {
		viewPager.setCurrentItem(position);
	}

	public static void showInstance(@NonNull FragmentManager manager) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			TracksFragment fragment = new TracksFragment();
			manager.beginTransaction()
					.add(R.id.fragmentContainer, fragment, TAG)
					.addToBackStack(TAG)
					.commit();
		}
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
	                         @Nullable Bundle savedInstanceState) {
		LayoutInflater themedInflater = UiUtilities.getInflater(requireContext(), nightMode);
		View view = themedInflater.inflate(R.layout.track_fragment, container, false);
		if (Build.VERSION.SDK_INT < 30) {
			AndroidUtils.addStatusBarPadding21v(requireActivity(), view);
		}

		tabLayout = view.findViewById(R.id.tab_layout);
		viewPager = view.findViewById(R.id.view_pager);

		setupToolbar(view);

		asyncLoader = new LoadGpxTask();
		asyncLoader.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

		listener = new SelectTracksListener() {
			@Override
			public void onSelectTracks() {
				for (int i = 0; i < tabAdapter.getItemCount(); i++) {
					if (i != viewPager.getCurrentItem()) {
						tabAdapter.notifyItemChanged(i);
					}
				}
			}
		};

		return view;
	}

	private void setupTabLayout() {
		tabAdapter = new TracksTabAdapter(this, trackTabs);
		viewPager.setAdapter(tabAdapter);

		TabLayoutMediator mediator = new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
			tab.setText(trackTabs.get(position).tabName);
		});
		mediator.attach();

		ApplicationMode mode = settings.getApplicationMode();
		int profileColor = mode.getProfileColor(nightMode);
		tabLayout.setSelectedTabIndicatorColor(profileColor);
		tabLayout.setTabTextColors(ColorUtilities.getPrimaryTextColor(app, nightMode), profileColor);
	}

	private void setupToolbar(@NonNull View view) {
		Toolbar toolbar = view.findViewById(R.id.toolbar);
		toolbar.findViewById(R.id.back_button).setOnClickListener(v -> {
			FragmentActivity activity = getActivity();
			if (activity != null) {
				activity.onBackPressed();
			}
		});
		toolbar.findViewById(R.id.switch_group).setOnClickListener(v -> {
			FragmentActivity activity = getActivity();
			if (activity != null) {
				TrackGroupsBottomSheet.showInstance(activity.getSupportFragmentManager(), this);
			}
		});

		View actionsButton = toolbar.findViewById(R.id.actions_button);
		actionsButton.setOnClickListener(v -> {
			List<PopUpMenuItem> items = new ArrayList<>();
			items.add(new PopUpMenuItem.Builder(view.getContext())
					.setTitleId(R.string.change_appearance)
					.setIcon(iconsCache.getThemedIcon(R.drawable.ic_action_appearance))
					.setOnClickListener(new OnClickListener() {
						@Override
						public void onClick(View v) {

						}
					})
					.create()
			);
			items.add(new PopUpMenuItem.Builder(view.getContext())
					.setTitleId(R.string.shared_string_import)
					.setIcon(iconsCache.getThemedIcon(R.drawable.ic_action_import_to))
					.setOnClickListener(new OnClickListener() {
						@Override
						public void onClick(View v) {

						}
					})
					.create()
			);
			new PopUpMenuHelper.Builder(actionsButton, items, nightMode).show();
		});
	}

	private LoadGpxTask asyncLoader;
	private final List<TrackTab> tabs = new ArrayList<>();
	private TrackTab SelectedTab;
	private List<TrackFolder> folders = new ArrayList<>();
	private SelectTracksListener listener;

	private void updateTabs(List<TrackFolder> folders) {
		this.folders = folders;
		trackTabs.clear();

		trackTabs.add(new TrackTab(getString(R.string.shared_string_on_map), TrackTabType.ON_MAP));
		trackTabs.add(new TrackTab(getString(R.string.shared_string_all), TrackTabType.ALL));

		for (TrackFolder folder : folders) {
			TrackTab folderTab = new TrackTab(folder.folderName, TrackTabType.FOLDER);
			folderTab.trackFolder = folder;
			trackTabs.add(folderTab);
		}
	}

	public class LoadGpxTask extends AsyncTask<Void, GpxInfo, List<TrackFolder>> {

		@Override
		protected List<TrackFolder> doInBackground(Void... params) {
			return loadGpxGroups(app.getAppPath(IndexConstants.GPX_INDEX_DIR));
		}

		@Override
		protected void onPostExecute(List<TrackFolder> result) {
			updateTabs(result);
			setupTabLayout();
		}

		private List<TrackFolder> loadGpxGroups(File mapPath) {
			if (mapPath.canRead()) {
				List<TrackFolder> groups = new ArrayList<>();
				File[] listFiles = mapPath.listFiles();

				if (listFiles != null && listFiles.length != 0) {
					TrackFolder trackFolder = new TrackFolder(mapPath.getName());
					groups.add(trackFolder);
					for (File file : listFiles) {
						if (file.isDirectory()) {
							TrackFolder folderGroup = loadGPXFolder(file);
							if (folderGroup != null)
								groups.add(folderGroup);
						} else if (file.isFile() && file.getName().toLowerCase().endsWith(IndexConstants.GPX_FILE_EXT)) {
							GpxInfo info = new GpxInfo();
							info.subfolder = file.getParent();
							info.file = file;
							trackFolder.gpxInfos.add(info);
						}
					}
					if (trackFolder.gpxInfos.isEmpty()) {
						groups.remove(trackFolder);
					}
				}
				return groups;
			}
			return null;
		}

		private TrackFolder loadGPXFolder(File mapPath) {
			if (mapPath.canRead()) {
				File[] listFiles = mapPath.listFiles();
				if (listFiles != null && listFiles.length != 0) {
					TrackFolder trackFolder = new TrackFolder(mapPath.getName());
					for (File file : listFiles) {
						if (file.isDirectory()) {
							TrackFolder folder = loadGPXFolder(file);
							if (folder != null)
								trackFolder.subFolders.add(folder);
						} else if (file.isFile() && file.getName().toLowerCase().endsWith(IndexConstants.GPX_FILE_EXT)) {
							GpxInfo info = new GpxInfo();
							info.subfolder = file.getParent();
							info.file = file;
							trackFolder.gpxInfos.add(info);
						}
					}
					return trackFolder;
				}
			}
			return null;
		}
	}

	public class TracksTabAdapter extends FragmentStateAdapter {
		List<TrackTab> trackTabs;

		public TracksTabAdapter(@NonNull Fragment fragment, List<TrackTab> trackTabs) {
			super(fragment);
			this.trackTabs = trackTabs;
		}

		@NonNull
		@Override
		public Fragment createFragment(int position) {
			TrackTab trackTab = trackTabs.get(position);
			TrackTabType folderType = trackTab.tabType;
			TracksTreeFragment fragment;
			switch (folderType) {
				case ON_MAP:
					fragment = TracksTreeFragment.createOnMapTracksFragment(app, folders, listener, view -> {
						for (int index = 0; index < trackTabs.size(); index++) {
							if (trackTabs.get(index).tabType == TrackTabType.ALL) {
								viewPager.setCurrentItem(index);
							}
						}
					});
					return fragment;
				case ALL:
					fragment = TracksTreeFragment.createAllTracksFragment(app, folders, listener);
					return fragment;
				case FOLDER:
					fragment = TracksTreeFragment.createFolderTracksFragment(app, trackTab.trackFolder, listener);
					return fragment;
				case FILTER:
					break;
			}
			return new Fragment();
		}

		@Override
		public int getItemCount() {
			return trackTabs.size();
		}
	}
}

class TrackTab {

	public final String tabName;
	public final TrackTabType tabType;

	@Nullable
	public TrackFolder trackFolder;

	public TrackTab(String name, TrackTabType type) {
		this.tabName = name;
		this.tabType = type;
	}
}

class TrackFolder {
	public String folderName;
	public ArrayList<GpxInfo> gpxInfos = new ArrayList<>();
	public ArrayList<TrackFolder> subFolders = new ArrayList<>();

	TrackFolder(String name) {
		this.folderName = name;
	}
}

interface SelectTracksListener {
	void onSelectTracks();
}