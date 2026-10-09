package net.osmand.plus.configmap.tracks;

import static net.osmand.plus.myplaces.tracks.TrackFoldersHelper.SORT_SUB_FOLDERS_KEY;
import static net.osmand.plus.settings.enums.TracksSortMode.DATE_DESCENDING;
import static net.osmand.plus.settings.enums.TracksSortMode.DISTANCE_ASCENDING;
import static net.osmand.plus.settings.enums.TracksSortMode.LAST_MODIFIED;
import static net.osmand.plus.settings.enums.TracksSortMode.NAME_DESCENDING;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.R;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.configmap.tracks.viewholders.SortTracksViewHolder.SortTracksListener;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.settings.enums.TracksSortMode;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;

import java.util.Arrays;
import java.util.List;

public class SortByBottomSheet extends MenuBottomSheetDialogFragment {

	private static final String TAG = SortByBottomSheet.class.getSimpleName();

	private static final String TRACKS_SORT_MODE_KEY = "tracks_sort_mode_key";

	private TracksSortMode tracksSortMode;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (savedInstanceState != null) {
			tracksSortMode = AndroidUtils.getSerializable(savedInstanceState, TRACKS_SORT_MODE_KEY, TracksSortMode.class);
		}
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		Context context = requireContext();
		LayoutInflater themedInflater = UiUtilities.getInflater(context, nightMode);
		View view = themedInflater.inflate(R.layout.bottom_sheet_track_group_list, null);

		TextView title = view.findViewById(R.id.title);
		title.setText(R.string.sort_by);
		title.setTextColor(ColorUtilities.getSecondaryTextColor(context, nightMode));

		RecyclerView recyclerView = view.findViewById(R.id.recycler_view);
		recyclerView.setLayoutManager(new LinearLayoutManager(context));
		recyclerView.setAdapter(new SortModesAdapter(Arrays.asList(TracksSortMode.values())));

		items.add(new BaseBottomSheetItem.Builder().setCustomView(view).create());
	}

	public class SortModesAdapter extends RecyclerView.Adapter<SortModeViewHolder> {

		private final List<TracksSortMode> sortModes;
		private final int activeColorId;
		private final int defaultColorId;

		public SortModesAdapter(@NonNull List<TracksSortMode> sortModes) {
			this.sortModes = sortModes;
			activeColorId = ColorUtilities.getActiveIconColorId(nightMode);
			defaultColorId = ColorUtilities.getDefaultIconColorId(nightMode);
		}

		@NonNull
		@Override
		public SortModeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
			LayoutInflater inflater = UiUtilities.getInflater(parent.getContext(), nightMode);
			return new SortModeViewHolder(inflater.inflate(R.layout.list_item_two_icons, parent, false));
		}

		@Override
		public void onBindViewHolder(@NonNull SortModeViewHolder holder, int position) {
			TracksSortMode sortMode = sortModes.get(position);

			holder.title.setText(sortMode.getNameId());

			boolean selected = sortMode == tracksSortMode;
			int colorId = selected ? activeColorId : defaultColorId;
			holder.groupTypeIcon.setImageDrawable(getIcon(sortMode.getIconId(), colorId));

			holder.itemView.setOnClickListener(view -> {
				Fragment target = getTargetFragment();
				int adapterPosition = holder.getAdapterPosition();
				if (adapterPosition != RecyclerView.NO_POSITION && target instanceof SortTracksListener) {
					SortTracksListener sortTracksListener = (SortTracksListener) target;
					boolean sortSubFolders = false;
					Bundle bundle = getArguments();
					if (bundle != null && bundle.containsKey(SORT_SUB_FOLDERS_KEY)){
						sortSubFolders = bundle.getBoolean(SORT_SUB_FOLDERS_KEY);
					}
					TracksSortMode mode = sortModes.get(position);
					sortTracksListener.setTracksSortMode(mode, sortSubFolders);
				}
				dismiss();
			});
			AndroidUiHelper.updateVisibility(holder.selectedIcon, selected);
			AndroidUiHelper.updateVisibility(holder.divider, shouldShowDivider(sortMode));
		}

		private boolean shouldShowDivider(@NonNull TracksSortMode mode) {
			return mode == LAST_MODIFIED || mode == NAME_DESCENDING
					|| mode == DATE_DESCENDING || mode == DISTANCE_ASCENDING;
		}

		@Override
		public int getItemCount() {
			return sortModes.size();
		}
	}

	public static class SortModeViewHolder extends RecyclerView.ViewHolder {

		public final TextView title;
		public final ImageView groupTypeIcon;
		public final ImageView selectedIcon;
		public final View divider;

		public SortModeViewHolder(@NonNull View itemView) {
			super(itemView);
			title = itemView.findViewById(R.id.title);
			groupTypeIcon = itemView.findViewById(R.id.icon);
			selectedIcon = itemView.findViewById(R.id.secondary_icon);
			divider = itemView.findViewById(R.id.divider);
		}
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putSerializable(TRACKS_SORT_MODE_KEY, tracksSortMode);
	}

	public static void showInstance(@NonNull FragmentManager manager, @NonNull TracksSortMode sortMode,
									@NonNull Fragment target, boolean usedOnMap) {
		showInstance(manager, sortMode, target, usedOnMap, false);
	}

	public static void showInstance(@NonNull FragmentManager manager, @NonNull TracksSortMode sortMode,
									@NonNull Fragment target, boolean usedOnMap, boolean sortSubFolders) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			SortByBottomSheet fragment = new SortByBottomSheet();
			fragment.tracksSortMode = sortMode;
			fragment.setUsedOnMap(usedOnMap);
			fragment.setTargetFragment(target, 0);
			Bundle bundle = new Bundle();
			bundle.putBoolean(SORT_SUB_FOLDERS_KEY, sortSubFolders);
			fragment.setArguments(bundle);
			fragment.show(manager, TAG);
		}
	}
}
