package net.osmand.plus.quickaction.actions;

import static net.osmand.plus.quickaction.QuickActionIds.SHOW_HIDE_TRACKS_ACTION_ID;
import static net.osmand.plus.track.helpers.GpxSelectionHelper.CURRENT_TRACK;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.configmap.tracks.TracksTabsFragment;
import net.osmand.plus.configmap.tracks.TracksTabsFragment.TracksSelectionListener;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.quickaction.CreateEditActionDialog;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.QuickActionType;
import net.osmand.plus.settings.enums.ThemeUsageContext;
import net.osmand.plus.track.GpxSelectionParams;
import net.osmand.plus.track.helpers.GpxSelectionHelper;
import net.osmand.plus.track.helpers.GpxUiHelper;
import net.osmand.plus.track.helpers.SelectedGpxFile;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.multistatetoggle.TextToggleButton;
import net.osmand.plus.widgets.multistatetoggle.TextToggleButton.TextRadioItem;
import net.osmand.shared.gpx.GpxHelper;
import net.osmand.shared.gpx.TrackItem;
import net.osmand.shared.io.KFile;
import net.osmand.util.Algorithms;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ShowHideTracksAction extends QuickAction implements TracksSelectionListener {

	public static final QuickActionType TYPE = new QuickActionType(SHOW_HIDE_TRACKS_ACTION_ID,
			"tracks.showhide", ShowHideTracksAction.class)
			.nameActionRes(R.string.quick_action_verb_show_hide)
			.nameRes(R.string.shared_string_tracks)
			.iconRes(R.drawable.ic_action_polygom_dark)
			.category(QuickActionType.CONFIGURE_MAP);

	public static final String KEY_ALWAYS_ASK = "always_ask";
	public static final String KEY_TRACKS = "tracks";

	private transient EditText title;
	private transient TextToggleButton trackToggleButton;
	private transient TracksAdapter adapter;

	public ShowHideTracksAction() {
		super(TYPE);
	}

	public ShowHideTracksAction(QuickAction quickAction) {
		super(quickAction);
	}

	@Override
	public void execute(@NonNull MapActivity mapActivity, @Nullable Bundle params) {
		OsmandApplication app = mapActivity.getApp();
		List<String> paths = getExistingTracksPaths();

		if (shouldAskEveryTime() || Algorithms.isEmpty(paths)) {
			TracksTabsFragment.showInstance(mapActivity);
		} else if (areTracksVisible(app, paths)) {
			hideTracks(app, paths);
		} else {
			showTracks(app, paths);
		}
	}

	private void showTracks(@NonNull OsmandApplication app, @NonNull List<String> paths) {
		List<TrackItem> trackItems = new ArrayList<>();
		for (String path : paths) {
			if (CURRENT_TRACK.equals(path)) {
				trackItems.add(new TrackItem(app.getSavingTrackHelper().getCurrentTrack().getGpxFile()));
			} else {
				trackItems.add(new TrackItem(new KFile(path)));
			}
		}
		app.getSelectedGpxHelper().saveTracksVisibility(trackItems, false);
	}

	private void hideTracks(@NonNull OsmandApplication app, @NonNull List<String> paths) {
		GpxSelectionHelper helper = app.getSelectedGpxHelper();
		GpxSelectionParams params = GpxSelectionParams.newInstance()
				.hideFromMap().syncGroup().saveSelection();
		for (String path : paths) {
			SelectedGpxFile selectedGpxFile = getSelectedGpxFile(app, path);
			if (selectedGpxFile != null) {
				helper.selectGpxFile(selectedGpxFile.getGpxFile(), params);
			}
		}
		app.getOsmandMap().refreshMap();
	}

	private boolean areTracksVisible(@NonNull OsmandApplication app, @NonNull List<String> paths) {
		for (String path : paths) {
			if (getSelectedGpxFile(app, path) == null) {
				return false;
			}
		}
		return !Algorithms.isEmpty(paths);
	}

	@Nullable
	private SelectedGpxFile getSelectedGpxFile(@NonNull OsmandApplication app, @NonNull String path) {
		GpxSelectionHelper helper = app.getSelectedGpxHelper();
		return CURRENT_TRACK.equals(path)
				? helper.getSelectedCurrentRecordingTrack()
				: helper.getSelectedFileByPath(path);
	}

	@Override
	public String getActionText(@NonNull OsmandApplication app) {
		String actionName;
		if (shouldAskEveryTime()) {
			actionName = app.getString(getActionNameRes());
		} else {
			actionName = isActionWithSlash(app)
					? app.getString(R.string.shared_string_hide)
					: app.getString(R.string.shared_string_show);
		}
		return app.getString(R.string.ltr_or_rtl_combine_via_dash, actionName, getName(app));
	}

	@Override
	public boolean isActionWithSlash(@NonNull OsmandApplication app) {
		return !shouldAskEveryTime() && areTracksVisible(app, getExistingTracksPaths());
	}

	@Override
	public void setAutoGeneratedTitle(EditText title) {
		this.title = title;
	}

	@Override
	public void drawUI(@NonNull ViewGroup parent, @NonNull MapActivity mapActivity, boolean nightMode) {
		View view = UiUtilities.inflate(parent.getContext(), nightMode, R.layout.quick_action_show_hide_tracks, parent, false);
		parent.addView(view);

		setupTracksList(view, mapActivity);
		setupAddTrackButton(view, mapActivity, nightMode);
		setupTrackToggleButton(view, mapActivity);
	}

	private void setupTracksList(@NonNull View container, @NonNull MapActivity mapActivity) {
		List<String> paths = getTracksPaths();
		List<String> existingPaths = getExistingTracksPaths();
		if (existingPaths.size() != paths.size()) {
			OsmandApplication app = mapActivity.getApp();
			saveTracksPaths(existingPaths);
			updateAutoGeneratedTitle(app, getGeneratedTitle(app, paths), existingPaths);
		}
		adapter = new TracksAdapter(mapActivity, existingPaths);

		RecyclerView recyclerView = container.findViewById(R.id.list);
		recyclerView.setAdapter(adapter);
	}

	private void setupAddTrackButton(@NonNull View container, @NonNull MapActivity mapActivity, boolean nightMode) {
		View buttonContainer = container.findViewById(R.id.add_track_button_container);
		View button = container.findViewById(R.id.add_track_button);

		AndroidUtils.setBackground(container.getContext(), buttonContainer, nightMode,
				R.drawable.ripple_light, R.drawable.ripple_dark);
		AndroidUtils.setBackground(container.getContext(), button, nightMode,
				R.drawable.btn_solid_border_light, R.drawable.btn_solid_border_dark);

		buttonContainer.setOnClickListener(v -> showSelectTracksDialog(mapActivity));
	}

	private void setupTrackToggleButton(@NonNull View container, @NonNull MapActivity mapActivity) {
		OsmandApplication app = mapActivity.getApp();
		boolean nightMode = app.getDaynightHelper().isNightMode(ThemeUsageContext.OVER_MAP);

		LinearLayout toggleContainer = container.findViewById(R.id.track_toggle);
		trackToggleButton = new TextToggleButton(app, toggleContainer, nightMode);

		TextRadioItem alwaysAskButton = new TextRadioItem(app.getString(R.string.confirm_every_run));
		TextRadioItem selectTracksButton = new TextRadioItem(app.getString(R.string.shared_string_select));

		alwaysAskButton.setOnClickListener((radioItem, view) -> {
			updateTracksVisibility(container, true);
			return true;
		});
		selectTracksButton.setOnClickListener((radioItem, view) -> {
			updateTracksVisibility(container, false);
			return true;
		});

		trackToggleButton.setItems(alwaysAskButton, selectTracksButton);
		trackToggleButton.setSelectedItem(shouldAskEveryTime() ? alwaysAskButton : selectTracksButton);
		updateTracksVisibility(container, shouldAskEveryTime());
	}

	private void updateTracksVisibility(@NonNull View container, boolean alwaysAsk) {
		AndroidUiHelper.updateVisibility(container.findViewById(R.id.always_ask_description), alwaysAsk);
		AndroidUiHelper.updateVisibility(container.findViewById(R.id.selected_tracks_container), !alwaysAsk);
	}

	private void showSelectTracksDialog(@NonNull MapActivity mapActivity) {
		if (adapter != null) {
			TracksTabsFragment.showSelectTracksInstance(mapActivity, adapter.paths, CreateEditActionDialog.TAG);
		}
	}

	@Override
	public void onTracksSelected(@NonNull List<String> selectedPaths) {
		if (adapter != null) {
			Set<String> selected = new HashSet<>(selectedPaths);
			List<String> paths = new ArrayList<>();
			for (String path : adapter.paths) {
				if (selected.remove(path)) {
					paths.add(path);
				}
			}
			OsmandApplication app = adapter.mapActivity.getApp();
			List<String> added = new ArrayList<>(selected);
			added.sort((path1, path2) -> getTrackName(app, path1).compareToIgnoreCase(getTrackName(app, path2)));
			paths.addAll(added);

			adapter.setTracks(paths);
		}
	}

	@Override
	public boolean fillParams(@NonNull View root, @NonNull MapActivity mapActivity) {
		boolean alwaysAsk = trackToggleButton == null || trackToggleButton.getSelectedItemIndex() == 0;
		getParams().put(KEY_ALWAYS_ASK, String.valueOf(alwaysAsk));

		return alwaysAsk || !Algorithms.isEmpty(getTracksPaths());
	}

	public boolean onGpxFileRenamed(@NonNull OsmandApplication app, @NonNull String src, @NonNull String dest) {
		List<String> paths = getTracksPaths();
		int index = paths.indexOf(src);
		if (index == -1) {
			return false;
		}
		String previousTitle = getGeneratedTitle(app, paths);
		if (paths.contains(dest)) {
			paths.remove(index);
		} else {
			paths.set(index, dest);
		}
		saveTracksPaths(paths);

		if (Algorithms.stringsEqual(getRawName(), previousTitle)) {
			setName(getGeneratedTitle(app, paths));
		}
		return true;
	}

	private boolean shouldAskEveryTime() {
		String alwaysAsk = getParams().get(KEY_ALWAYS_ASK);
		return alwaysAsk == null || Boolean.parseBoolean(alwaysAsk);
	}

	@NonNull
	private List<String> getExistingTracksPaths() {
		List<String> paths = new ArrayList<>();
		for (String path : getTracksPaths()) {
			if (CURRENT_TRACK.equals(path) || new File(path).exists()) {
				paths.add(path);
			}
		}
		return paths;
	}

	@NonNull
	private List<String> getTracksPaths() {
		String tracks = getParams().get(KEY_TRACKS);
		if (Algorithms.isBlank(tracks)) {
			return new ArrayList<>();
		}
		try {
			List<String> paths = new ArrayList<>();
			JSONArray jsonArray = new JSONArray(tracks);
			for (int i = 0; i < jsonArray.length(); i++) {
				String path = jsonArray.getString(i);
				if (!Algorithms.isBlank(path)) {
					paths.add(path);
				}
			}
			return paths;
		} catch (JSONException e) {
			return Collections.emptyList();
		}
	}

	private void saveTracksPaths(@NonNull List<String> paths) {
		getParams().put(KEY_TRACKS, new JSONArray(paths).toString());
	}

	@NonNull
	private String getTrackName(@NonNull OsmandApplication app, @NonNull String path) {
		return CURRENT_TRACK.equals(path)
				? app.getString(R.string.shared_string_currently_recording_track)
				: GpxHelper.INSTANCE.getGpxTitle(new File(path).getName());
	}

	@Nullable
	private String getTrackFolderName(@NonNull String path) {
		if (CURRENT_TRACK.equals(path)) {
			return null;
		}
		String folderName = GpxUiHelper.getTrackFolderName(new KFile(path), true);
		return folderName != null ? Algorithms.capitalizeFirstLetter(folderName) : null;
	}

	@NonNull
	private String getGeneratedTitle(@NonNull OsmandApplication app, @NonNull List<String> paths) {
		if (Algorithms.isEmpty(paths)) {
			return "";
		}
		String name = getTrackName(app, paths.get(0));
		return paths.size() > 1 ? name + " +" + (paths.size() - 1) : name;
	}

	private void updateAutoGeneratedTitle(@NonNull OsmandApplication app, @NonNull String previousTitle,
			@NonNull List<String> paths) {
		if (title == null) {
			return;
		}
		String currentTitle = title.getText().toString();
		if (currentTitle.equals(previousTitle) || currentTitle.equals(app.getString(getNameRes()))) {
			title.setText(getGeneratedTitle(app, paths));
		}
	}

	private class TracksAdapter extends RecyclerView.Adapter<TracksAdapter.TrackViewHolder> {

		private final MapActivity mapActivity;
		private final List<String> paths;

		public TracksAdapter(@NonNull MapActivity mapActivity, @NonNull List<String> paths) {
			this.mapActivity = mapActivity;
			this.paths = paths;
		}

		public void setTracks(@NonNull List<String> newPaths) {
			OsmandApplication app = mapActivity.getApp();
			String previousTitle = getGeneratedTitle(app, paths);

			paths.clear();
			paths.addAll(newPaths);
			saveTracksPaths(paths);
			notifyDataSetChanged();

			updateAutoGeneratedTitle(app, previousTitle, paths);
		}

		@NonNull
		@Override
		public TrackViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
			LayoutInflater inflater = LayoutInflater.from(parent.getContext());
			return new TrackViewHolder(inflater.inflate(R.layout.quick_action_deletable_list_item, parent, false));
		}

		@Override
		public void onBindViewHolder(@NonNull TrackViewHolder holder, int position) {
			OsmandApplication app = mapActivity.getApp();
			String path = paths.get(position);

			holder.icon.setImageDrawable(app.getUIUtilities().getThemedIcon(R.drawable.ic_action_polygom_dark));
			holder.title.setText(getTrackName(app, path));
			String folderName = getTrackFolderName(path);
			holder.description.setText(folderName);
			AndroidUiHelper.updateVisibility(holder.description, !Algorithms.isEmpty(folderName));
			holder.delete.setOnClickListener(view -> {
				int index = holder.getBindingAdapterPosition();
				if (index != RecyclerView.NO_POSITION) {
					String previousTitle = getGeneratedTitle(app, paths);

					paths.remove(index);
					saveTracksPaths(paths);
					notifyDataSetChanged();

					updateAutoGeneratedTitle(app, previousTitle, paths);
				}
			});
		}

		@Override
		public int getItemCount() {
			return paths.size();
		}

		class TrackViewHolder extends RecyclerView.ViewHolder {

			private final TextView title;
			private final TextView description;
			private final ImageView icon;
			private final ImageView delete;

			public TrackViewHolder(@NonNull View itemView) {
				super(itemView);

				title = itemView.findViewById(R.id.title);
				description = itemView.findViewById(R.id.description);
				icon = itemView.findViewById(R.id.icon);
				delete = itemView.findViewById(R.id.delete);
			}
		}
	}
}
