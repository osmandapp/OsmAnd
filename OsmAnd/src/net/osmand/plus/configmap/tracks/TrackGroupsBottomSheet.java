package net.osmand.plus.configmap.tracks;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.configmap.tracks.TrackGroupsBottomSheet.TrackGroupsAdapter.TrackGroupViewHolder;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.TextViewEx;

import java.util.ArrayList;
import java.util.List;

public class TrackGroupsBottomSheet extends BottomSheetDialogFragment {

	private static final String TAG = TrackGroupsBottomSheet.class.getSimpleName();

	private OsmandApplication app;
	private UiUtilities iconsCache;

	private TrackTab selectedTab;
	private List<TrackTab> trackTabs = new ArrayList<>();

	private boolean nightMode;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = (OsmandApplication) requireContext().getApplicationContext();
		iconsCache = app.getUIUtilities();
		nightMode = app.getDaynightHelper().isNightModeForMapControls();

		Fragment target = getTargetFragment();
		if (target instanceof TracksFragment) {
			TracksFragment fragment = (TracksFragment) target;
			trackTabs = fragment.getTrackTabs();
			selectedTab = fragment.getSelectedTab();
		}
	}

	@NonNull
	@Override
	public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
		Dialog dialog = super.onCreateDialog(savedInstanceState);
		if (!AndroidUiHelper.isOrientationPortrait(requireActivity())) {
			dialog.setOnShowListener(dialogInterface -> {
				BottomSheetDialog bottomSheetDialog = (BottomSheetDialog) dialogInterface;
				FrameLayout bottomSheet = bottomSheetDialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
				if (bottomSheet != null) {
					BottomSheetBehavior.from(bottomSheet).setState(BottomSheetBehavior.STATE_EXPANDED);
				}
			});
		}
		return dialog;
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
	                         @Nullable Bundle savedInstanceState) {
		inflater = UiUtilities.getInflater(requireContext(), nightMode);
		View view = inflater.inflate(R.layout.bottom_sheet_track_group_list, null);

		TextViewEx title = view.findViewById(R.id.title);
		title.setText(R.string.switch_folder);

		RecyclerView recyclerView = view.findViewById(R.id.recycler_view);
		recyclerView.setLayoutManager(new LinearLayoutManager(view.getContext()));
		recyclerView.setAdapter(new TrackGroupsAdapter());

		return view;
	}

	public class TrackGroupsAdapter extends RecyclerView.Adapter<TrackGroupViewHolder> {

		private final int activeColor;

		public TrackGroupsAdapter() {
			activeColor = ColorUtilities.getSelectedProfileColor(app, nightMode);
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
			TrackTab trackTab = trackTabs.get(position);

			holder.title.setText(trackTab.name);

			boolean selected = trackTab == selectedTab;
			if (selected) {
				holder.groupTypeIcon.setImageDrawable(iconsCache.getPaintedIcon(trackTab.type.iconId, activeColor));
			} else {
				holder.groupTypeIcon.setImageDrawable(iconsCache.getThemedIcon(trackTab.type.iconId));
			}
			holder.selectedIcon.setImageDrawable(iconsCache.getPaintedIcon(R.drawable.ic_action_done, activeColor));

			holder.button.setOnClickListener(view -> {
				Fragment target = getTargetFragment();
				int adapterPosition = holder.getAdapterPosition();
				if (adapterPosition != RecyclerView.NO_POSITION && target instanceof TracksFragment) {
					TrackTab tab = trackTabs.get(adapterPosition);
					((TracksFragment) target).setSelectedTab(tab.name);
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

			private final View button;
			private final TextView title;
			private final ImageView groupTypeIcon;
			private final ImageView selectedIcon;
			private final View divider;

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

	public static void showInstance(@NonNull FragmentManager manager, @NonNull Fragment target) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			TrackGroupsBottomSheet fragment = new TrackGroupsBottomSheet();
			fragment.setTargetFragment(target, 0);
			fragment.show(manager, TAG);
		}
	}
}