package net.osmand.plus.myplaces.favorites.dialogs;

import static net.osmand.plus.myplaces.tracks.TrackFoldersHelper.SORT_SUB_FOLDERS_KEY;

import android.content.Context;
import android.os.Bundle;
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
import net.osmand.plus.configmap.tracks.SortByBottomSheet;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.myplaces.favorites.dialogs.SortFavoriteViewHolder.SortFavoriteListener;
import net.osmand.plus.settings.enums.FavoriteListSortMode;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;

import java.util.Arrays;
import java.util.List;

public class FavoriteSortByBottomSheet extends MenuBottomSheetDialogFragment {

	private static final String TAG = SortByBottomSheet.class.getSimpleName();

	private static final String PRESELECTED_SORT_MODE_KEY = "preselected_sort_mode_key";
	public final static String INCLUDE_POINTS = "include_points";

	private FavoriteListSortMode sortMode;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (savedInstanceState != null) {
			sortMode = AndroidUtils.getSerializable(savedInstanceState, PRESELECTED_SORT_MODE_KEY, FavoriteListSortMode.class);
		}
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		Context context = requireContext();
		View view = inflate(R.layout.bottom_sheet_track_group_list);

		TextView title = view.findViewById(R.id.title);
		title.setText(R.string.sort_by);
		title.setTextColor(ColorUtilities.getSecondaryTextColor(context, nightMode));

		RecyclerView recyclerView = view.findViewById(R.id.recycler_view);
		recyclerView.setLayoutManager(new LinearLayoutManager(context));

		boolean includePoints = false;
		Bundle bundle = getArguments();
		if (bundle != null && bundle.containsKey(INCLUDE_POINTS)){
			includePoints = bundle.getBoolean(INCLUDE_POINTS);
		}

		recyclerView.setAdapter(new FavoriteSortByBottomSheet.SortModesAdapter(Arrays.asList(FavoriteListSortMode.getSortModes(includePoints))));

		items.add(new BaseBottomSheetItem.Builder().setCustomView(view).create());
	}

	public class SortModesAdapter extends RecyclerView.Adapter<SortModeViewHolder> {

		private final List<FavoriteListSortMode> sortModes;
		private final int activeColorId;
		private final int defaultColorId;

		public SortModesAdapter(@NonNull List<FavoriteListSortMode> sortModes) {
			this.sortModes = sortModes;
			activeColorId = ColorUtilities.getActiveIconColorId(nightMode);
			defaultColorId = ColorUtilities.getDefaultIconColorId(nightMode);
		}

		@NonNull
		@Override
		public SortModeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
			return new SortModeViewHolder(inflate(R.layout.list_item_two_icons, parent, false));
		}

		@Override
		public void onBindViewHolder(@NonNull SortModeViewHolder holder, int position) {
			FavoriteListSortMode sortMode = sortModes.get(position);

			holder.title.setText(sortMode.getNameId());

			boolean selected = sortMode == FavoriteSortByBottomSheet.this.sortMode;
			int colorId = selected ? activeColorId : defaultColorId;
			holder.groupTypeIcon.setImageDrawable(getIcon(sortMode.getIconId(), colorId));

			holder.itemView.setOnClickListener(view -> {
				Fragment target = getTargetFragment();
				int adapterPosition = holder.getAdapterPosition();
				if (adapterPosition != RecyclerView.NO_POSITION && target instanceof SortFavoriteListener sortTracksListener) {
					boolean sortSubFolders = false;
					Bundle bundle = getArguments();
					if (bundle != null && bundle.containsKey(SORT_SUB_FOLDERS_KEY)){
						sortSubFolders = bundle.getBoolean(SORT_SUB_FOLDERS_KEY);
					}
					FavoriteListSortMode mode = sortModes.get(position);
					sortTracksListener.setTracksSortMode(mode, sortSubFolders);
				}
				dismiss();
			});
			AndroidUiHelper.updateVisibility(holder.selectedIcon, selected);
			AndroidUiHelper.updateVisibility(holder.divider, shouldShowDivider(sortMode));
		}

		private boolean shouldShowDivider(@NonNull FavoriteListSortMode mode) {
			return mode == FavoriteListSortMode.LAST_MODIFIED || mode == FavoriteListSortMode.NAME_DESCENDING
					|| mode == FavoriteListSortMode.FARTHEST;
					//|| mode == FavoriteSortMode.DATE_DESCENDING || mode == FavoriteSortMode.DISTANCE_ASCENDING;
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
		outState.putSerializable(PRESELECTED_SORT_MODE_KEY, sortMode);
	}

	public static void showInstance(@NonNull FragmentManager manager,
	                                @NonNull FavoriteListSortMode sortMode, @NonNull Fragment target,
	                                boolean usedOnMap, boolean sortSubFolders, boolean includePoints) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			FavoriteSortByBottomSheet fragment = new FavoriteSortByBottomSheet();
			fragment.sortMode = sortMode;
			fragment.setUsedOnMap(usedOnMap);
			fragment.setTargetFragment(target, 0);
			Bundle bundle = new Bundle();
			bundle.putBoolean(SORT_SUB_FOLDERS_KEY, sortSubFolders);
			bundle.putBoolean(INCLUDE_POINTS, includePoints);
			fragment.setArguments(bundle);
			fragment.show(manager, TAG);
		}
	}
}
