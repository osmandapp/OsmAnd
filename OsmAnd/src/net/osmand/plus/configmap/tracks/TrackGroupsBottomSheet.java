package net.osmand.plus.configmap.tracks;

import android.graphics.drawable.Drawable;
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
import net.osmand.plus.configmap.tracks.TrackGroupsBottomSheet.TrackGroupsAdapter.TrackGroupViewHolder;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.settings.enums.ThemeUsageContext;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.TextViewEx;

import java.util.ArrayList;
import java.util.List;

public class TrackGroupsBottomSheet extends BaseBottomSheetDialogFragment {

	private static final String TAG = TrackGroupsBottomSheet.class.getSimpleName();

	private TrackTab selectedTab;
	private List<TrackTab> trackTabs = new ArrayList<>();

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (getTargetFragment() instanceof TracksTabsFragment fragment) {
			trackTabs = fragment.getSortedTrackTabs(true);
			selectedTab = fragment.getSelectedTab();
		}
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
	                         @Nullable Bundle savedInstanceState) {
		updateNightMode();
		View view = inflate(R.layout.bottom_sheet_track_group_list);

		TextViewEx title = view.findViewById(R.id.title);
		title.setText(R.string.switch_folder);

		RecyclerView recyclerView = view.findViewById(R.id.recycler_view);
		recyclerView.setLayoutManager(new LinearLayoutManager(view.getContext()));
		recyclerView.setAdapter(new TrackGroupsAdapter());

		return view;
	}

	@NonNull
	@Override
	public ThemeUsageContext getThemeUsageContext() {
		return ThemeUsageContext.OVER_MAP;
	}

	public class TrackGroupsAdapter extends RecyclerView.Adapter<TrackGroupViewHolder> {

		private final int activeColorId;
		private final int defaultColorId;

		public TrackGroupsAdapter() {
			activeColorId = ColorUtilities.getActiveIconColorId(nightMode);
			defaultColorId = ColorUtilities.getDefaultIconColorId(nightMode);
		}

		@NonNull
		@Override
		public TrackGroupViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
			LayoutInflater inflater = UiUtilities.getInflater(parent.getContext(), nightMode);
			return new TrackGroupViewHolder(inflater.inflate(R.layout.list_item_two_icons, parent, false));
		}

		@Override
		public void onBindViewHolder(@NonNull TrackGroupViewHolder holder, int position) {
			TrackTab trackTab = trackTabs.get(position);

			holder.title.setText(trackTab.getDirName(true));

			boolean selected = trackTab == selectedTab;
			int colorId = selected ? activeColorId : defaultColorId;
			holder.groupTypeIcon.setImageDrawable(getIcon(trackTab.type.iconId, colorId));

			Drawable drawable = selected ? getIcon(R.drawable.ic_action_done, activeColorId) : null;
			holder.selectedIcon.setImageDrawable(drawable);

			holder.itemView.setOnClickListener(view -> {
				Fragment target = getTargetFragment();
				int adapterPosition = holder.getAdapterPosition();
				if (adapterPosition != RecyclerView.NO_POSITION && target instanceof TracksTabsFragment) {
					TrackTab tab = trackTabs.get(adapterPosition);
					((TracksTabsFragment) target).setSelectedTab(tab.getId());
				}
				dismiss();
			});

			AndroidUiHelper.updateVisibility(holder.selectedIcon, selected);
			AndroidUiHelper.updateVisibility(holder.divider, trackTab.type == TrackTabType.ALL);
		}

		@Override
		public int getItemCount() {
			return trackTabs.size();
		}

		class TrackGroupViewHolder extends RecyclerView.ViewHolder {

			private final TextView title;
			private final ImageView groupTypeIcon;
			private final ImageView selectedIcon;
			private final View divider;

			public TrackGroupViewHolder(@NonNull View itemView) {
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
			TrackGroupsBottomSheet fragment = new TrackGroupsBottomSheet();
			fragment.setTargetFragment(target, 0);
			fragment.show(manager, TAG);
		}
	}
}