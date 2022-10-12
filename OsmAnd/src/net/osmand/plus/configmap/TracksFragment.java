package net.osmand.plus.configmap;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import net.osmand.IndexConstants;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.configmap.TrackTab.BottomSheetGroupListener;
import net.osmand.plus.configmap.TracksFragment.BottomSheetRecycleViewAdapter.BottomSheetViewHolder;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.myplaces.ui.AvailableGPXFragment.GpxInfo;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.TextViewEx;

import java.io.File;
import java.util.ArrayList;

public class TracksFragment extends BaseOsmAndFragment {
	public static final String TAG = TracksFragment.class.getSimpleName();
	private OsmandApplication app;
	private OsmandSettings settings;
	private ApplicationMode selectedAppMode;
	private boolean nightMode;

	private Toolbar toolbar;
	private TabLayout tabLayout;
	private ViewPager2 viewPager;
	private LoadGpxTask asyncLoader;
	private ArrayList<TrackTab> tabs = new ArrayList<>();
	private ArrayList<TrackFolder> folders = new ArrayList<>();
	private BottomSheetRecycleViewAdapter tracksGroupsAdapter;
	private TracksTabAdapter tracksTabAdapter;
	private SelectTracksListener selectTracksListener;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = requireMyApplication();
		settings = app.getSettings();
		selectedAppMode = settings.getApplicationMode();
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
	                         @Nullable Bundle savedInstanceState) {
		nightMode = !settings.isLightContent();
		inflater = UiUtilities.getInflater(requireContext(), nightMode);

		View view = inflater.inflate(R.layout.track_fragment, container, false);
		if (Build.VERSION.SDK_INT < 30) {
			AndroidUtils.addStatusBarPadding21v(app, view);
		}
		toolbar = view.findViewById(R.id.toolbar);
		tabLayout = view.findViewById(R.id.tab_layout);
		viewPager = view.findViewById(R.id.view_pager);

		setupToolbar();

		asyncLoader = new LoadGpxTask();
		asyncLoader.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, getActivity());

		selectTracksListener = new SelectTracksListener() {
			@Override
			public void onSelectTracks() {
				for (int i = 0; i < tracksTabAdapter.getItemCount(); i++) {
					if (i != viewPager.getCurrentItem()) {
						tracksTabAdapter.notifyItemChanged(i);
					}
				}
			}
		};

		return view;
	}

	public static void showInstance(@NonNull FragmentActivity activity) {
		FragmentManager manager = activity.getSupportFragmentManager();
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			TracksFragment fragment = new TracksFragment();
			manager.beginTransaction()
					.add(R.id.fragmentContainer, fragment, TAG)
					.addToBackStack(TAG)
					.commit();
		}
	}

	private void updateTabs(ArrayList<TrackFolder> folders) {
		this.folders = folders;
		tabs.clear();

		TrackTab onMapGroup = new TrackTab(getString(R.string.shared_string_on_map), TrackTabType.ON_MAP);
		tabs.add(onMapGroup);

		TrackTab allGroup = new TrackTab(getString(R.string.shared_string_all), TrackTabType.ALL);
		allGroup.hasBottomSheetDivider = true;
		tabs.add(allGroup);

		for (TrackFolder folder : folders) {
			TrackTab folderTab = new TrackTab(folder.folderName, TrackTabType.FOLDER);
			folderTab.trackFolder = folder;
			tabs.add(folderTab);
		}
	}

	private void setupToolbar() {
		ImageButton backButton = toolbar.findViewById(R.id.back_button);
		backButton.setOnClickListener(view -> {
			FragmentActivity activity = getActivity();
			if (activity != null) {
				activity.getOnBackPressedDispatcher().onBackPressed();
			}
		});

		int themeRes = nightMode ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme;
		View sheetDialogView = LayoutInflater.from(new ContextThemeWrapper(app, themeRes))
				.inflate(R.layout.bottom_sheet_track_group_list, null);
		sheetDialogView.setBackgroundColor(ColorUtilities.getListBgColor(app, nightMode));
		BottomSheetDialog dialog = new BottomSheetDialog(getActivity());
		dialog.setContentView(sheetDialogView);

		TextViewEx textViewEx = sheetDialogView.findViewById(R.id.title);
		textViewEx.setText(getString(R.string.switch_folder));
		RecyclerView recyclerView = sheetDialogView.findViewById(R.id.recycler_view);
		recyclerView.setLayoutManager(new LinearLayoutManager(app));

		tracksGroupsAdapter = new BottomSheetRecycleViewAdapter(new BottomSheetGroupListener() {
			@Override
			public void onBottomSheetItemClick(int position) {
				viewPager.setCurrentItem(position);
				dialog.dismiss();
			}
		});
		recyclerView.setAdapter(tracksGroupsAdapter);

		View switchFolderButton = toolbar.findViewById(R.id.switch_folder);
		switchFolderButton.setOnClickListener(v -> {
			FragmentActivity activity = getActivity();
			if (activity != null) {
				tracksGroupsAdapter.notifyDataSetChanged();
				dialog.show();
			}
		});

		View actionsButton = toolbar.findViewById(R.id.actions_button);
		PopupMenu popupMenu = new PopupMenu(app, actionsButton);
		popupMenu.inflate(R.menu.track_action_menu);
		Menu menu = popupMenu.getMenu();
		if (menu instanceof MenuBuilder) {
			((MenuBuilder) menu).setOptionalIconsVisible(true);
		}
		for (int index = 0; index < menu.size(); index++) {
			menu.getItem(index).getIcon().setTint(ColorUtilities.getDefaultIconColor(app, nightMode));
		}

		popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				int i = item.getItemId();

				if (i == R.id.action_change_appearance) {
					// TODO: change appearance
					return true;
				} else if (i == R.id.action_import) {
					// TODO: import
					return true;
				}
				return false;
			}
		});

		actionsButton.setOnClickListener(v -> {
			popupMenu.show();
		});
	}

	private void setupTabLayout() {
		tracksTabAdapter = new TracksTabAdapter(this, tabs);
		viewPager.setAdapter(tracksTabAdapter);

		TabLayoutMediator mediator = new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
			tab.setText(tabs.get(position).tabName);
		});
		mediator.attach();

		int profileColor = selectedAppMode.getProfileColor(nightMode);
		tabLayout.setSelectedTabIndicatorColor(profileColor);
		tabLayout.setTabTextColors(ColorUtilities.getPrimaryTextColor(app, nightMode), selectedAppMode.getProfileColor(nightMode));
	}

	public class LoadGpxTask extends AsyncTask<Activity, GpxInfo, ArrayList<TrackFolder>> {

		@Override
		protected ArrayList<TrackFolder> doInBackground(Activity... params) {
			return loadGpxGroups(app.getAppPath(IndexConstants.GPX_INDEX_DIR));
		}

		@Override
		protected void onPostExecute(ArrayList<TrackFolder> result) {
			updateTabs(result);
			setupTabLayout();
		}

		private ArrayList<TrackFolder> loadGpxGroups(File mapPath) {
			if (mapPath.canRead()) {
				ArrayList<TrackFolder> groups = new ArrayList<>();
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

	public class BottomSheetRecycleViewAdapter extends RecyclerView.Adapter<BottomSheetViewHolder> {
		private final BottomSheetGroupListener listener;

		BottomSheetRecycleViewAdapter(BottomSheetGroupListener listener) {
			this.listener = listener;
		}

		@NonNull
		@Override
		public BottomSheetViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
			int themeRes = nightMode ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme;
			View view = LayoutInflater.from(new ContextThemeWrapper(app, themeRes))
					.inflate(R.layout.list_item_two_icons, null);
			return new BottomSheetViewHolder(view);
		}

		@Override
		public void onBindViewHolder(@NonNull BottomSheetViewHolder holder, int position) {
			holder.viewEx.setText(tabs.get(position).tabName);
			Drawable groupTypeIcon = getPaintedContentIcon(tabs.get(position).tabType.iconId,
					position == viewPager.getCurrentItem()
							? selectedAppMode.getProfileColor(nightMode)
							: ColorUtilities.getDefaultIconColor(app, nightMode));
			holder.groupTypeIcon.setImageDrawable(groupTypeIcon);

			if (position == viewPager.getCurrentItem()) {
				holder.selectedIcon.setVisibility(View.VISIBLE);
				holder.selectedIcon.getDrawable().setTint(selectedAppMode.getProfileColor(nightMode));
			} else {
				holder.selectedIcon.setVisibility(View.INVISIBLE);
			}

			holder.button.setOnClickListener(view -> {
				listener.onBottomSheetItemClick(position);
			});

			holder.divider.setVisibility(tabs.get(position).hasBottomSheetDivider ? View.VISIBLE : View.INVISIBLE);
		}

		@Override
		public int getItemCount() {
			return tabs.size();
		}

		class BottomSheetViewHolder extends RecyclerView.ViewHolder {
			final View button;
			final TextViewEx viewEx;
			final AppCompatImageView groupTypeIcon;
			final AppCompatImageView selectedIcon;
			final View divider;

			public BottomSheetViewHolder(@NonNull View itemView) {
				super(itemView);
				button = itemView.findViewById(R.id.button_container);
				viewEx = itemView.findViewById(R.id.title);
				groupTypeIcon = itemView.findViewById(R.id.icon);
				selectedIcon = itemView.findViewById(R.id.secondary_icon);
				divider = itemView.findViewById(R.id.divider);
			}
		}
	}

	public class TracksTabAdapter extends FragmentStateAdapter {
		ArrayList<TrackTab> trackTabs;

		public TracksTabAdapter(@NonNull Fragment fragment, ArrayList<TrackTab> trackTabs) {
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
					fragment = TracksTreeFragment.createOnMapTracksFragment(app, folders, selectTracksListener, view -> {
						for (int index = 0; index < trackTabs.size(); index++) {
							if (trackTabs.get(index).tabType == TrackTabType.ALL) {
								viewPager.setCurrentItem(index);
							}
						}
					});
					return fragment;
				case ALL:
					fragment = TracksTreeFragment.createAllTracksFragment(app, folders, selectTracksListener);
					return fragment;
				case FOLDER:
					fragment = TracksTreeFragment.createFolderTracksFragment(app, trackTab.trackFolder, selectTracksListener);
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

	@Override
	public int getStatusBarColorId() {
		AndroidUiHelper.setStatusBarContentColor(getView(), nightMode);
		return ColorUtilities.getListBgColorId(nightMode);
	}
}

class TrackTab {
	public String tabName;
	@Nullable
	public TrackFolder trackFolder;
	public final TrackTabType tabType;
	public boolean hasBottomSheetDivider = false;

	public TrackTab(String name, TrackTabType type) {
		this.tabName = name;
		this.tabType = type;
	}

	interface BottomSheetGroupListener {
		void onBottomSheetItemClick(int position);
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


