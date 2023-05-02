package net.osmand.plus.configmap.tracks;

import static net.osmand.plus.configmap.tracks.TracksSortMode.DATE_DESCENDING;
import static net.osmand.plus.configmap.tracks.TracksSortMode.DISTANCE_ASCENDING;
import static net.osmand.plus.configmap.tracks.TracksSortMode.LAST_MODIFIED;
import static net.osmand.plus.configmap.tracks.TracksSortMode.NAME_DESCENDING;

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
import net.osmand.plus.base.BaseBottomSheetDialogFragment;
import net.osmand.plus.configmap.tracks.SortByBottomSheet.SortModesAdapter.SortModeViewHolder;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;

import java.util.Arrays;
import java.util.List;

public class SortByBottomSheet extends BaseBottomSheetDialogFragment {

	private static final String TAG = SortByBottomSheet.class.getSimpleName();

	private TracksSortMode tracksSortMode;

	private boolean nightMode;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		nightMode = isNightMode(true);

		Fragment target = getTargetFragment();
		if (target instanceof SortableFragment) {
			TrackTab trackTab = ((SortableFragment) target).getSelectedTab();
			tracksSortMode = trackTab.getSortMode();
		}
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
	                         @Nullable Bundle savedInstanceState) {
		Context context = requireContext();
		inflater = UiUtilities.getInflater(context, nightMode);
		View view = inflater.inflate(R.layout.bottom_sheet_track_group_list, null);

		TextView title = view.findViewById(R.id.title);
		title.setText(R.string.sort_by);
		title.setTextColor(ColorUtilities.getSecondaryTextColor(context, nightMode));

		RecyclerView recyclerView = view.findViewById(R.id.recycler_view);
		recyclerView.setLayoutManager(new LinearLayoutManager(context));
		recyclerView.setAdapter(new SortModesAdapter(Arrays.asList(TracksSortMode.values())));

		return view;
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
			holder.groupTypeIcon.setImageDrawable(uiUtilities.getIcon(sortMode.getIconId(), colorId));

			holder.itemView.setOnClickListener(view -> {
				Fragment target = getTargetFragment();
				int adapterPosition = holder.getAdapterPosition();
				if (adapterPosition != RecyclerView.NO_POSITION && target instanceof SortableFragment) {
					TracksSortMode mode = sortModes.get(position);
					((SortableFragment) target).setTracksSortMode(mode);
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

		class SortModeViewHolder extends RecyclerView.ViewHolder {

			private final TextView title;
			private final ImageView groupTypeIcon;
			private final ImageView selectedIcon;
			private final View divider;

			public SortModeViewHolder(@NonNull View itemView) {
				super(itemView);
				title = itemView.findViewById(R.id.title);
				groupTypeIcon = itemView.findViewById(R.id.icon);
				selectedIcon = itemView.findViewById(R.id.secondary_icon);
				divider = itemView.findViewById(R.id.divider);
			}
		}
	}

	public static void showInstance(@NonNull FragmentManager manager, @NonNull Fragment target) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			SortByBottomSheet fragment = new SortByBottomSheet();
			fragment.setTargetFragment(target, 0);
			fragment.show(manager, TAG);
		}
	}
}
