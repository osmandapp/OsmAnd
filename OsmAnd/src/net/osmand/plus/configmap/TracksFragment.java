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
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import net.osmand.IndexConstants;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.configmap.TrackGroup.BottomSheetGroupListener;
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

	private Toolbar toolbar;
	private TabLayout tabLayout;
	private ViewPager2 viewPager;
	private LoadGpxTask asyncLoader;
	private ArrayList<TrackGroup> groups = new ArrayList<>();
	private GroupListAdapter tracksGroupsAdapter;

	private TracksTabAdapter tracksTabAdapter;

	private boolean nightMode;

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
		tabLayout.setTabMode(TabLayout.MODE_SCROLLABLE);
		viewPager = view.findViewById(R.id.view_pager);

		setupToolbar();

		asyncLoader = new LoadGpxTask();
		asyncLoader.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, getActivity());

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

	private void updateGroups(ArrayList<TrackGroup> gpxInfos) {
		groups.clear();

		TrackGroup onMapGroup = new TrackGroup(null, getString(R.string.shared_string_on_map), TrackGroupType.ON_MAP);
		groups.add(onMapGroup);

		TrackGroup allGroup = new TrackGroup(null, getString(R.string.shared_string_all), TrackGroupType.ALL);
		for (TrackGroup gpxGroup : gpxInfos) {
			allGroup.gpxInfos.addAll(gpxGroup.gpxInfos);
		}
		allGroup.hasDivider = true;
		groups.add(allGroup);

		groups.addAll(gpxInfos);
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

		tracksGroupsAdapter = new GroupListAdapter(new BottomSheetGroupListener() {
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
		for(int index = 0; index < menu.size(); index++){
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
		tracksTabAdapter = new TracksTabAdapter(this, groups);
		viewPager.setAdapter(tracksTabAdapter);

		TabLayoutMediator mediator = new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
			tab.setText(groups.get(position).groupName);
		});
		mediator.attach();

		int profileColor = selectedAppMode.getProfileColor(nightMode);
		tabLayout.setSelectedTabIndicatorColor(profileColor);
		tabLayout.setTabTextColors(ColorUtilities.getPrimaryTextColor(app, nightMode), selectedAppMode.getProfileColor(nightMode));
	}

	public class LoadGpxTask extends AsyncTask<Activity, GpxInfo, ArrayList<TrackGroup>> {

		@Override
		protected ArrayList<TrackGroup> doInBackground(Activity... params) {
			return loadGpxGroups(app.getAppPath(IndexConstants.GPX_INDEX_DIR));
		}
		@Override
		protected void onPreExecute() {
			//showProgressBar();
		}

		@Override
		protected void onProgressUpdate(GpxInfo... values) {

		}

		@Override
		protected void onPostExecute(ArrayList<TrackGroup> result) {
			updateGroups(result);
			setupTabLayout();
		}

		private ArrayList<TrackGroup> loadGpxGroups(File mapPath) {
			if (mapPath.canRead()) {
				ArrayList<TrackGroup> groups = new ArrayList<>();
				File[] listFiles = mapPath.listFiles();

				if (listFiles != null && listFiles.length != 0) {
					TrackGroup trackGroup = new TrackGroup(mapPath, mapPath.getName(), TrackGroupType.FOLDER);
					groups.add(trackGroup);
					for (File file : listFiles) {
						if (file.isDirectory()) {
							TrackGroup folderGroup = loadGPXFolder(file);
							if (folderGroup != null)
								groups.add(folderGroup);
						} else if (file.isFile() && file.getName().toLowerCase().endsWith(IndexConstants.GPX_FILE_EXT)) {
							GpxInfo info = new GpxInfo();
							info.subfolder = file.getName();
							info.file = file;
							trackGroup.gpxInfos.add(info);
						}
					}
					if (trackGroup.gpxInfos.isEmpty()) {
						groups.remove(trackGroup);
					}
				}
				return groups;
			}
			return null;
		}
	}

	private TrackGroup loadGPXFolder(File mapPath) {
		if (mapPath.canRead()) {
			File[] listFiles = mapPath.listFiles();
			if (listFiles != null && listFiles.length != 0) {
				TrackGroup trackGroup = new TrackGroup(mapPath, mapPath.getName(), TrackGroupType.FOLDER);
				for (File file : listFiles) {
					if (file.isDirectory()) {
						TrackGroup folderGroup = loadGPXFolder(file);
						if (folderGroup != null)
							trackGroup.subGroups.add(folderGroup);
					} else if (file.isFile() && file.getName().toLowerCase().endsWith(IndexConstants.GPX_FILE_EXT)) {
						GpxInfo info = new GpxInfo();
						info.subfolder = file.getName();
						info.file = file;
						trackGroup.gpxInfos.add(info);
					}
				}
				return trackGroup;
			}
		}
		return null;
	}

	public class GroupListAdapter extends RecyclerView.Adapter<GroupListAdapter.ViewHolder> {
		private final BottomSheetGroupListener listener;

		GroupListAdapter(BottomSheetGroupListener listener) {
			this.listener = listener;
		}

		@NonNull
		@Override
		public GroupListAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
			int themeRes = nightMode ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme;
			View view = LayoutInflater.from(new ContextThemeWrapper(app, themeRes))
					.inflate(R.layout.list_item_two_icons, null);
			return new GroupListAdapter.ViewHolder(view);
		}

		@Override
		public void onBindViewHolder(@NonNull GroupListAdapter.ViewHolder holder, int position) {
			holder.viewEx.setText(groups.get(position).groupName);
			Drawable groupTypeIcon = getPaintedContentIcon(groups.get(position).groupType.iconId,
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

			holder.divider.setVisibility(groups.get(position).hasDivider ? View.VISIBLE : View.INVISIBLE);
		}

		@Override
		public int getItemCount() {
			return groups.size();
		}

		public class ViewHolder extends RecyclerView.ViewHolder {

			final View button;
			final TextViewEx viewEx;
			final AppCompatImageView groupTypeIcon;
			final AppCompatImageView selectedIcon;
			final View divider;

			public ViewHolder(@NonNull View itemView) {
				super(itemView);
				button = itemView.findViewById(R.id.button_container);
				viewEx = itemView.findViewById(R.id.title);
				groupTypeIcon = itemView.findViewById(R.id.icon);
				selectedIcon = itemView.findViewById(R.id.secondary_icon);
				divider = itemView.findViewById(R.id.divider);
			}
		}
	}

	@Override
	public int getStatusBarColorId() {
		AndroidUiHelper.setStatusBarContentColor(getView(), nightMode);
		return ColorUtilities.getListBgColorId(nightMode);
	}
}

class TrackGroup {
	public String groupName;
	public ArrayList<GpxInfo> gpxInfos = new ArrayList<>();
	public ArrayList<TrackGroup> subGroups = new ArrayList<>();
	@Nullable
	private File gpxFile;
	public final TrackGroupType groupType;
	public boolean hasDivider = false;

	public TrackGroup(@Nullable File file, String name, TrackGroupType type) {
		this.gpxFile = file;
		this.groupName = name;
		this.groupType = type;
	}

	interface BottomSheetGroupListener{
		void onBottomSheetItemClick(int position);
	}
}
