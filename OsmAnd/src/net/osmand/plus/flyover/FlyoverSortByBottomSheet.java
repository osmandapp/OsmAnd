package net.osmand.plus.flyover;

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
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;

import java.util.Arrays;
import java.util.List;

/** "Sort by" sheet of My Places > Flyovers, as the one of the tracks and favorites. */
public class FlyoverSortByBottomSheet extends MenuBottomSheetDialogFragment {

	private static final String TAG = FlyoverSortByBottomSheet.class.getSimpleName();
	private static final String SORT_MODE_KEY = "sort_mode_key";

	public interface FlyoverSortListener {
		@NonNull
		FlyoverSortMode getFlyoverSortMode();

		void setFlyoverSortMode(@NonNull FlyoverSortMode sortMode);
	}

	private FlyoverSortMode sortMode = FlyoverSortMode.DATE_NEWEST;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (savedInstanceState != null && savedInstanceState.containsKey(SORT_MODE_KEY)) {
			sortMode = FlyoverSortMode.valueOf(savedInstanceState.getString(SORT_MODE_KEY, ""), sortMode);
		}
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString(SORT_MODE_KEY, sortMode.name());
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
		recyclerView.setAdapter(new SortModesAdapter(Arrays.asList(FlyoverSortMode.values())));

		items.add(new BaseBottomSheetItem.Builder().setCustomView(view).create());
	}

	private class SortModesAdapter extends RecyclerView.Adapter<SortModeViewHolder> {

		private final List<FlyoverSortMode> sortModes;

		SortModesAdapter(@NonNull List<FlyoverSortMode> sortModes) {
			this.sortModes = sortModes;
		}

		@NonNull
		@Override
		public SortModeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
			return new SortModeViewHolder(inflate(R.layout.list_item_two_icons, parent, false));
		}

		@Override
		public void onBindViewHolder(@NonNull SortModeViewHolder holder, int position) {
			FlyoverSortMode mode = sortModes.get(position);
			boolean selected = mode == sortMode;
			int activeColorId = ColorUtilities.getActiveIconColorId(nightMode);
			int iconColorId = selected ? activeColorId : ColorUtilities.getDefaultIconColorId(nightMode);

			holder.title.setText(mode.getNameId());
			holder.icon.setImageDrawable(getIcon(mode.getIconId(), iconColorId));
			holder.selectedIcon.setImageDrawable(getIcon(R.drawable.ic_action_done, activeColorId));
			AndroidUiHelper.updateVisibility(holder.selectedIcon, selected);
			AndroidUiHelper.updateVisibility(holder.divider, mode.showDividerAfter());
			holder.itemView.setOnClickListener(v -> {
				Fragment target = getTargetFragment();
				if (target instanceof FlyoverSortListener listener) {
					listener.setFlyoverSortMode(mode);
				}
				dismiss();
			});
		}

		@Override
		public int getItemCount() {
			return sortModes.size();
		}
	}

	private static class SortModeViewHolder extends RecyclerView.ViewHolder {
		final TextView title;
		final ImageView icon;
		final ImageView selectedIcon;
		final View divider;

		SortModeViewHolder(@NonNull View itemView) {
			super(itemView);
			title = itemView.findViewById(R.id.title);
			icon = itemView.findViewById(R.id.icon);
			selectedIcon = itemView.findViewById(R.id.secondary_icon);
			divider = itemView.findViewById(R.id.divider);
		}
	}

	public static void showInstance(@NonNull FragmentManager manager, @NonNull Fragment target, @NonNull FlyoverSortMode sortMode) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			FlyoverSortByBottomSheet fragment = new FlyoverSortByBottomSheet();
			fragment.sortMode = sortMode;
			fragment.setTargetFragment(target, 0);
			fragment.show(manager, TAG);
		}
	}
}
