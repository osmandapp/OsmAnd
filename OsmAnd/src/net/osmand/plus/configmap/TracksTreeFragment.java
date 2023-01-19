package net.osmand.plus.configmap;

import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CheckBox;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.gpx.GPXFile;
import net.osmand.gpx.GPXTrackAnalysis;
import net.osmand.gpx.GPXUtilities;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.myplaces.ui.GpxInfo;
import net.osmand.plus.track.GpxAppearanceAdapter;
import net.osmand.plus.track.GpxSelectionParams;
import net.osmand.plus.track.TrackDrawInfo;
import net.osmand.plus.track.helpers.GPXDatabase.GpxDataItem;
import net.osmand.plus.track.helpers.GpxSelectionHelper;
import net.osmand.plus.track.helpers.SelectedGpxFile;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.TextViewEx;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;

public class TracksTreeFragment extends Fragment {

	public static final String TAG = TracksTreeFragment.class.getSimpleName();
	private final OsmandApplication app;
	private boolean nightMode;
	private GpxSelectionHelper selectedGpxHelper;
	private TrackTabType tabType;
	private TracksAdapter adapter;
	private View.OnClickListener onEmptyStateViewClickListener;
	private SelectTracksListener selectTracksListener;
	private TextViewEx applyButton;
	private TextViewEx secondaryButton;
	private final List<Track> originalSelectedTracks = new ArrayList<>();
	private final List<Track> newSelectedTracks = new ArrayList<>();
	private final List<TrackGroup> trackGroups = new ArrayList<>();
	private final List<TrackTreeView> listViews = new ArrayList<>();

	TracksTreeFragment(OsmandApplication app) {
		this.app = app;
	}

	public static TracksTreeFragment createFolderTracksFragment(OsmandApplication app, TrackFolder trackFolder, SelectTracksListener selectTracksListener) {
		TracksTreeFragment fragment = new TracksTreeFragment(app);
		fragment.tabType = TrackTabType.FOLDER;
		fragment.selectTracksListener = selectTracksListener;
		TrackGroup group = new TrackGroup(trackFolder.folderName, false, false);
		fragment.setupTrackGroup(trackFolder.gpxInfos, group);
		fragment.trackGroups.add(group);
		fragment.setupListViews();
		return fragment;
	}

	public static TracksTreeFragment createAllTracksFragment(OsmandApplication app, List<TrackFolder> trackFolders, SelectTracksListener selectTracksListener) {
		TracksTreeFragment fragment = new TracksTreeFragment(app);
		fragment.tabType = TrackTabType.ALL;
		fragment.selectTracksListener = selectTracksListener;
		for (TrackFolder folder : trackFolders) {
			TrackGroup group = new TrackGroup(folder.folderName, true, true);
			fragment.setupTrackGroup(folder.gpxInfos, group);
			fragment.trackGroups.add(group);
		}
		fragment.setupListViews();
		return fragment;
	}

	public static TracksTreeFragment createOnMapTracksFragment(OsmandApplication app, List<TrackFolder> trackFolders, SelectTracksListener selectTracksListener, OnClickListener onEmptyStateViewClickListener) {
		TracksTreeFragment fragment = new TracksTreeFragment(app);
		fragment.tabType = TrackTabType.ON_MAP;
		fragment.selectTracksListener = selectTracksListener;
		fragment.onEmptyStateViewClickListener = onEmptyStateViewClickListener;
		TrackGroup selectedGroup = new TrackGroup(null, false, false);
		fragment.setupTrackGroup(fragment.getSelectedGpxInfos(trackFolders), selectedGroup);
		fragment.trackGroups.add(selectedGroup);
		fragment.setupListViews();
		return fragment;
	}

	private void setupListViews() {
		listViews.clear();
		if (tabType == TrackTabType.ON_MAP) {
			boolean emptySelectedGroup = trackGroups.get(0).tracks.isEmpty();
			if (emptySelectedGroup) {
				listViews.add(new EmptyOnMapTrackView());
			} else {
//				listViews.add(new SortView());
			}
			for (TrackGroup group : trackGroups) {
				if (!group.tracks.isEmpty()) {
					if (group.showGroupHeader) {
						listViews.add(new TrackGroupView(group));
					}
					for (Track track : group.tracks) {
						listViews.add(new TrackView(track));
					}
				}
			}
		} else {
//			listViews.add(new SortView());
			for (TrackGroup group : trackGroups) {
				if (!group.tracks.isEmpty()) {
					if (group.showGroupHeader) {
						listViews.add(new TrackGroupView(group));
					}
					if (!group.collapsed) {
						for (Track track : group.tracks) {
							listViews.add(new TrackView(track));
						}
					}
				}
			}
		}
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
	                         @Nullable Bundle savedInstanceState) {
		nightMode = !app.getSettings().isLightContent();
		inflater = UiUtilities.getInflater(requireContext(), nightMode);
		View view = inflater.inflate(R.layout.tracks_tree_fragment, container, false);
		applyButton = view.findViewById(R.id.apply_button);
		secondaryButton = view.findViewById(R.id.secondary_button);
		setupControlButtons();
		adapter = new TracksAdapter();
		RecyclerView recyclerView = view.findViewById(R.id.recycler_view);
		recyclerView.setLayoutManager(new LinearLayoutManager(app));
		recyclerView.setAdapter(adapter);

		return view;
	}

	private void updateApplyButton() {
		if (newSelectedTracks.containsAll(originalSelectedTracks) && originalSelectedTracks.containsAll(newSelectedTracks)) {
			applyButton.setTextColor(ColorUtilities.getDefaultIconColor(app, nightMode));
			applyButton.setClickable(false);
		} else {
			applyButton.setTextColor(ColorUtilities.getActiveColor(app, nightMode));
			applyButton.setClickable(true);
		}
	}

	private void updateSecondaryButton() {
		if (newSelectedTracks.isEmpty()) {
			secondaryButton.setTextColor(ColorUtilities.getDefaultIconColor(app, nightMode));
			secondaryButton.setClickable(false);
		} else {
			secondaryButton.setTextColor(ColorUtilities.getActiveColor(app, nightMode));
			secondaryButton.setClickable(true);
		}
	}

	private void setupControlButtons() {
		applyButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				for (Track originalTrack : originalSelectedTracks) {
					if (!newSelectedTracks.contains(originalTrack)) {
						GpxSelectionParams params = GpxSelectionParams.newInstance().syncGroup().saveSelection();
						params.hideFromMap();
						GPXFile gpxFile = GPXUtilities.loadGPXFile(originalTrack.gpxInfo.file);
						selectedGpxHelper = app.getSelectedGpxHelper();
						selectedGpxHelper.selectGpxFile(gpxFile, params);
					}
				}

				for (Track newSelectedTrack : newSelectedTracks) {
					if (!originalSelectedTracks.contains(newSelectedTrack)) {
						GpxSelectionParams params = GpxSelectionParams.newInstance().syncGroup().saveSelection();
						params.showOnMap().selectedByUser().addToMarkers().addToHistory();
						GPXFile gpxFile = GPXUtilities.loadGPXFile(newSelectedTrack.gpxInfo.file);
						selectedGpxHelper = app.getSelectedGpxHelper();
						selectedGpxHelper.selectGpxFile(gpxFile, params);
					}
				}

				if (tabType == TrackTabType.ON_MAP) {
					TrackGroup selectedGroup = new TrackGroup(null, false, false);
					selectedGroup.tracks.addAll(newSelectedTracks);
					trackGroups.clear();
					trackGroups.add(selectedGroup);
					setupListViews();
					adapter.notifyDataSetChanged();
				}

				originalSelectedTracks.clear();
				originalSelectedTracks.addAll(newSelectedTracks);
				updateApplyButton();
				updateSecondaryButton();
				selectTracksListener.onSelectTracks();
			}
		});

		if (tabType == TrackTabType.ON_MAP) {
			secondaryButton.setText(getString(R.string.shared_string_select_recent));
		} else {
			secondaryButton.setText(getString(R.string.shared_string_hide_all));
			secondaryButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View view) {
					for (TrackTreeView treeView : listViews) {
						if (treeView instanceof TrackView) {
							((TrackView) treeView).track.selected = false;
						} else if (treeView instanceof TrackGroupView) {
							for (Track track : ((TrackGroupView) treeView).group.tracks) {
								track.selected = false;
							}
						}
					}
					newSelectedTracks.clear();
					updateApplyButton();
					updateSecondaryButton();
					adapter.notifyDataSetChanged();
				}
			});
		}

		updateApplyButton();
		updateSecondaryButton();
	}


	public void setupTrackGroup(List<GpxInfo> gpxInfos, TrackGroup trackGroup) {
		for (GpxInfo gpxInfo : gpxInfos) {
			SelectedGpxFile sgpx = getSelectedGpxFile(gpxInfo, app);
			GPXTrackAnalysis analysis = null;
			boolean selected = false;
			GpxDataItem dataItem = app.getGpxDbHelper().getItem(gpxInfo.file, null);
			if (sgpx != null && sgpx.isLoaded()) {
				analysis = sgpx.getTrackAnalysis(app);
				selected = true;
			} else if (gpxInfo.file != null) {
				if (dataItem != null) {
					analysis = dataItem.getAnalysis();
				}
			}
			if (analysis == null) {
				continue;
			}
			String distance = OsmAndFormatter.getFormattedDistance(analysis.totalDistance, app);
			String time = analysis.isTimeSpecified() ? Algorithms.formatDuration((int) (analysis.timeSpan / 1000.0f + 0.5), app.accessibilityEnabled()) : "";
			int points = (analysis.wptPoints);

			TrackDrawInfo trackDrawInfo = new TrackDrawInfo(gpxInfo.file.getAbsolutePath(), dataItem, false);
			int colorId = trackDrawInfo.getColor();
			if (colorId == 0) {
				colorId = GpxAppearanceAdapter.getTrackColor(app);
			}

			Track track = new Track(trackGroup, gpxInfo, gpxInfo.getFileName(), distance, time, points, gpxInfo.file.getParentFile().getName(), colorId, selected);
			if (selected) {
				originalSelectedTracks.add(track);
				newSelectedTracks.add(track);
			}
			trackGroup.tracks.add(track);
		}
	}

	private SelectedGpxFile getSelectedGpxFile(GpxInfo gpxInfo, OsmandApplication app) {
		GpxSelectionHelper selectedGpxHelper = app.getSelectedGpxHelper();
		return gpxInfo.currentlyRecordingTrack ? selectedGpxHelper.getSelectedCurrentRecordingTrack() :
				selectedGpxHelper.getSelectedFileByName(gpxInfo.getFileName());
	}

	public List<GpxInfo> getSelectedGpxInfos(List<TrackFolder> folders) {
		List<GpxInfo> originalSelectedItems = new ArrayList<>();
		selectedGpxHelper = app.getSelectedGpxHelper();

		for (TrackFolder folder : folders) {
			for (GpxInfo gpxInfo : folder.gpxInfos) {
				SelectedGpxFile sgpx = selectedGpxHelper.getSelectedFileByName(gpxInfo.getFileName());
				if (sgpx != null) {
					gpxInfo.gpx = sgpx.getGpxFile();
					originalSelectedItems.add(gpxInfo);
				}
			}

		}
		return originalSelectedItems;
	}

	class TracksAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
		@NonNull
		@Override
		public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
			int themeRes = nightMode ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme;
			View view;
			if (viewType == TrackTreeViewType.EmptyOnMapTrackView.ordinal()) {
				view = LayoutInflater.from(new ContextThemeWrapper(app, themeRes))
						.inflate(R.layout.empty_state_on_map_tracks, null);

				return new EmptyStateViewHolder(view);
			} else if (viewType == TrackTreeViewType.TrackView.ordinal()) {
				view = LayoutInflater.from(new ContextThemeWrapper(app, themeRes))
						.inflate(R.layout.track_list_item, null);

				return new TrackViewHolder(view);
			} else if (viewType == TrackTreeViewType.SortView.ordinal()) {
				view = LayoutInflater.from(new ContextThemeWrapper(app, themeRes))
						.inflate(R.layout.track_list_sort_item, null);

				return new SortFilterViewHolder(view);
			} else {
				view = LayoutInflater.from(new ContextThemeWrapper(app, themeRes))
						.inflate(R.layout.track_list_group_item, null);

				return new TrackGroupViewHolder(view);
			}
		}

		@Override
		public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
			if (holder instanceof TrackViewHolder) {
				TrackViewHolder viewHolder = (TrackViewHolder) holder;
				TrackView trackView = (TrackView) listViews.get(position);
				Track track = trackView.track;

				viewHolder.tvTittle.setText(track.name);
				viewHolder.tvFolder.setText(track.folderName);
				viewHolder.tvDistance.setText(track.distance);
				viewHolder.tvPoints.setText(String.valueOf(track.points));
				viewHolder.tvTime.setText(track.time);
				viewHolder.trackIcon.getDrawable().setTint(track.colorId);
				updateCheckBox(viewHolder.checkBox, track.selected);

				viewHolder.button.setTag(track);
				viewHolder.button.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View view) {
						if (track.selected) {
							newSelectedTracks.remove(track);
							viewHolder.checkBox.getButtonDrawable().setTint(ColorUtilities.getDefaultIconColor(app, nightMode));
						} else {
							newSelectedTracks.add(track);
							viewHolder.checkBox.getButtonDrawable().setTint(app.getSettings().getApplicationMode().getProfileColor(nightMode));
						}

						track.selected = !track.selected;
						updateCheckBox(viewHolder.checkBox, track.selected);
						updateApplyButton();
						updateSecondaryButton();

						if (track.parentGroup.selected != updateTrackGroupSelection(track.parentGroup)) {
							int indexOfTrack = track.parentGroup.tracks.indexOf(track) + 1;
							notifyItemChanged(holder.getAdapterPosition() - indexOfTrack);
						}

					}
				});

			} else if (holder instanceof EmptyStateViewHolder) {
				EmptyStateViewHolder viewHolder = (EmptyStateViewHolder) holder;
				viewHolder.button.setOnClickListener(onEmptyStateViewClickListener);
			} else if (holder instanceof SortFilterViewHolder) {
				SortFilterViewHolder viewHolder = (SortFilterViewHolder) holder;
			} else if (holder instanceof TrackGroupViewHolder) {
				TrackGroupViewHolder viewHolder = (TrackGroupViewHolder) holder;
				TrackGroupView trackGroupView = (TrackGroupView) listViews.get(position);
				TrackGroup group = trackGroupView.group;

				updateTrackGroupSelection(group);
				updateCheckBox(viewHolder.checkBox, group.selected);
				viewHolder.title.setText(group.groupName);
				viewHolder.folderIcon.setVisibility(group.showIcon ? View.VISIBLE : View.GONE);

				viewHolder.button.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View view) {
						if (group.selected) {
							for (Track track : group.tracks) {
								track.selected = false;
								newSelectedTracks.remove(track);
							}
						} else {
							for (Track track : group.tracks) {
								track.selected = true;
								if (!newSelectedTracks.contains(track)) {
									newSelectedTracks.add(track);
								}
							}
						}

						if (!group.collapsed) {
							for (int i = holder.getAdapterPosition() + 1; i <= holder.getAdapterPosition() + group.tracks.size(); i++) {
								notifyItemChanged(i);
							}
						}
						updateTrackGroupSelection(group);
						updateCheckBox(viewHolder.checkBox, group.selected);
						updateApplyButton();
						updateSecondaryButton();
					}
				});

				viewHolder.collapseButtonImage.setImageResource(group.collapsed ? R.drawable.ic_action_arrow_down : R.drawable.ic_action_arrow_up);
				viewHolder.collapseButton.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View view) {
						group.collapsed = !group.collapsed;
						setupListViews();
						adapter.notifyDataSetChanged();
					}
				});
			}
		}

		private void updateCheckBox(CheckBox checkBox, boolean selected) {
			checkBox.getButtonDrawable().setTint(selected
					? app.getSettings().getApplicationMode().getProfileColor(nightMode)
					: ColorUtilities.getDefaultIconColor(app, nightMode));
			checkBox.setChecked(selected);
		}

		private boolean updateTrackGroupSelection(TrackGroup group) {
			boolean allTracksSelected = true;
			for (Track track : group.tracks) {
				if (!track.selected) {
					allTracksSelected = false;
				}
			}
			group.selected = allTracksSelected;

			return allTracksSelected;
		}

		@Override
		public int getItemViewType(int position) {
			TrackTreeView trackView = listViews.get(position);
			return trackView.viewType.ordinal();
		}

		@Override
		public int getItemCount() {
			return listViews.size();
		}

		public class TrackViewHolder extends RecyclerView.ViewHolder {
			final View button;
			final TextViewEx tvTittle;
			final TextViewEx tvTime;
			final TextViewEx tvDistance;
			final TextViewEx tvPoints;
			final TextViewEx tvFolder;
			final CheckBox checkBox;
			final AppCompatImageView trackIcon;

			public TrackViewHolder(@NonNull View itemView) {
				super(itemView);
				button = itemView.findViewById(R.id.button_container);
				tvTittle = itemView.findViewById(R.id.title);
				tvTime = itemView.findViewById(R.id.time);
				tvDistance = itemView.findViewById(R.id.distance);
				tvPoints = itemView.findViewById(R.id.points_count);
				tvFolder = itemView.findViewById(R.id.folder);
				checkBox = itemView.findViewById(R.id.checkbox);
				trackIcon = itemView.findViewById(R.id.track_icon);
			}
		}

		public class EmptyStateViewHolder extends RecyclerView.ViewHolder {
			final View button;

			public EmptyStateViewHolder(@NonNull View itemView) {
				super(itemView);
				button = itemView.findViewById(R.id.show_all_tracks);
			}
		}

		public class TrackGroupViewHolder extends RecyclerView.ViewHolder {
			final View collapseButton;
			final AppCompatImageView collapseButtonImage;
			final View button;
			final TextViewEx title;
			final AppCompatImageView folderIcon;
			final CheckBox checkBox;

			public TrackGroupViewHolder(@NonNull View itemView) {
				super(itemView);
				button = itemView.findViewById(R.id.button_container);
				collapseButton = itemView.findViewById(R.id.collapse_button);
				title = itemView.findViewById(R.id.title);
				folderIcon = itemView.findViewById(R.id.folder_icon);
				checkBox = itemView.findViewById(R.id.checkbox);
				collapseButtonImage = itemView.findViewById(R.id.collapse_image_view);
			}
		}

		public class SortFilterViewHolder extends RecyclerView.ViewHolder {
			final View sortButton;
			final View filterButton;

			public SortFilterViewHolder(@NonNull View itemView) {
				super(itemView);
				sortButton = itemView.findViewById(R.id.sort_button);
				filterButton = itemView.findViewById(R.id.filter_button);
			}
		}
	}

}

class TrackGroup {
	public String groupName;
	public ArrayList<Track> tracks = new ArrayList<>();
	public boolean showIcon;
	public boolean showGroupHeader;
	public boolean selected;
	public boolean collapsed;

	TrackGroup(@Nullable String groupName, boolean showIcon, boolean showGroupHeader) {
		this.groupName = groupName;
		this.showIcon = showIcon;
		this.showGroupHeader = showGroupHeader;
	}
}

class Track {
	public TrackGroup parentGroup;
	public GpxInfo gpxInfo;
	public String name;
	public String distance;
	public String time;
	public int points;
	public String folderName;
	public int colorId;
	public boolean selected;

	Track(TrackGroup parentGroup, GpxInfo gpxInfo, String name, String distance, String time, int points, String folderName, int colorId, boolean selected) {
		this.parentGroup = parentGroup;
		this.gpxInfo = gpxInfo;
		this.name = name;
		this.distance = distance;
		this.time = time;
		this.points = points;
		this.folderName = folderName;
		this.colorId = colorId;
		this.selected = selected;
	}
}
