package net.osmand.plus.configmap.tracks;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.configmap.tracks.TrackGroupsBottomSheet.TrackGroupsAdapter.TrackGroupViewHolder;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.TextViewEx;

import java.util.ArrayList;
import java.util.List;

public class TrackGroupsBottomSheet extends BottomSheetDialogFragment {

	private static final String TAG = TrackGroupsBottomSheet.class.getSimpleName();

	private OsmandApplication app;
	private OsmandSettings settings;
	private UiUtilities iconsCache;

	private List<TrackTab> tabs = new ArrayList<>();
	private TrackTab selectedTrackTab;
	private boolean nightMode;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = (OsmandApplication) requireContext().getApplicationContext();
		settings = app.getSettings();
		iconsCache = app.getUIUtilities();
		nightMode = !settings.isLightContent();

		Fragment target = getTargetFragment();
		if (target instanceof TracksFragment) {
			TracksFragment tracksFragment = (TracksFragment) target;
			tabs = tracksFragment.getTrackTabs();
			selectedTrackTab = tracksFragment.getSelectedTab();
		}
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
	                         @Nullable Bundle savedInstanceState) {
		LayoutInflater themedInflater = UiUtilities.getInflater(requireContext(), nightMode);
		View view = themedInflater.inflate(R.layout.bottom_sheet_track_group_list, null);

		TextViewEx title = view.findViewById(R.id.title);
		title.setText(R.string.switch_folder);

		RecyclerView recyclerView = view.findViewById(R.id.recycler_view);
		recyclerView.setLayoutManager(new LinearLayoutManager(view.getContext()));
		recyclerView.setAdapter(new TrackGroupsAdapter());

		return view;
	}

	public class TrackGroupsAdapter extends RecyclerView.Adapter<TrackGroupViewHolder> {

		int profileColor;

		public TrackGroupsAdapter() {
			profileColor = settings.getApplicationMode().getProfileColor(nightMode);
		}

		@NonNull
		@Override
		public TrackGroupViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
			LayoutInflater inflater = UiUtilities.getInflater(requireContext(), nightMode);
			View view = inflater.inflate(R.layout.list_item_two_icons, null);
			return new TrackGroupViewHolder(view);
		}

		@Override
		public void onBindViewHolder(@NonNull TrackGroupViewHolder holder, int position) {
			TrackTab trackTab = tabs.get(position);

			holder.title.setText(trackTab.name);
			ApplicationMode mode = settings.getApplicationMode();

			boolean selected = trackTab == selectedTrackTab;
			Drawable groupTypeIcon = null;
			if (selected) {
				groupTypeIcon = iconsCache.getPaintedIcon(trackTab.type.iconId, profileColor);
			} else {
				groupTypeIcon = iconsCache.getThemedIcon(trackTab.type.iconId);
			}
			holder.groupTypeIcon.setImageDrawable(groupTypeIcon);

			AndroidUiHelper.updateVisibility(holder.selectedIcon, selected);
			holder.selectedIcon.setImageDrawable(iconsCache.getPaintedIcon(R.drawable.ic_action_done, profileColor));

			holder.button.setOnClickListener(view -> {
				Fragment target = getTargetFragment();
				int adapterPosition = holder.getAdapterPosition();
				if (adapterPosition != RecyclerView.NO_POSITION && target instanceof TracksFragment) {
					((TracksFragment) target).setSelectedTab(adapterPosition);
				}
				dismiss();
			});
			AndroidUiHelper.updateVisibility(holder.divider, trackTab.type == TrackTabType.ALL);
		}

		@Override
		public int getItemCount() {
			return tabs.size();
		}

		class TrackGroupViewHolder extends RecyclerView.ViewHolder {

			final View button;
			final TextViewEx title;
			final AppCompatImageView groupTypeIcon;
			final AppCompatImageView selectedIcon;
			final View divider;

			public TrackGroupViewHolder(@NonNull View itemView) {
				super(itemView);
				title = itemView.findViewById(R.id.title);
				groupTypeIcon = itemView.findViewById(R.id.icon);
				selectedIcon = itemView.findViewById(R.id.secondary_icon);
				button = itemView.findViewById(R.id.button_container);
				divider = itemView.findViewById(R.id.divider);
			}
		}
	}

	public static void showInstance(@NonNull FragmentManager manager, @NonNull Fragment targetFragment) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			TrackGroupsBottomSheet bottomSheet = new TrackGroupsBottomSheet();
			bottomSheet.setTargetFragment(targetFragment, 0);
			bottomSheet.show(manager, TAG);
		}
	}
}

