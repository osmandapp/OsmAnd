package net.osmand.plus.download.local.dialogs;

import static net.osmand.plus.settings.enums.LocalSortMode.COUNTRY_NAME_DESCENDING;
import static net.osmand.plus.settings.enums.LocalSortMode.DATE_DESCENDING;
import static net.osmand.plus.settings.enums.LocalSortMode.NAME_DESCENDING;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
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
import net.osmand.plus.configmap.tracks.SortByBottomSheet.SortModeViewHolder;
import net.osmand.plus.download.local.LocalItemType;
import net.osmand.plus.download.local.LocalItemUtils;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.settings.enums.LocalSortMode;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;

public class SortMapsBottomSheet extends MenuBottomSheetDialogFragment {

	private static final String TAG = SortMapsBottomSheet.class.getSimpleName();

	private static final String SORT_MODE_KEY = "sort_mode_key";
	private static final String ITEM_TYPE_KEY = "item_type_key";

	private LocalItemType type;
	private LocalSortMode sortMode;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		type = AndroidUtils.getSerializable(requireArguments(), ITEM_TYPE_KEY, LocalItemType.class);
		if (savedInstanceState != null) {
			sortMode = AndroidUtils.getSerializable(savedInstanceState, SORT_MODE_KEY, LocalSortMode.class);
		} else if (type != null) {
			sortMode = LocalItemUtils.getSortModePref(app, type).get();
		}
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		Context themedContext = getThemedContext();
		View view = inflate(R.layout.bottom_sheet_track_group_list);

		TextView title = view.findViewById(R.id.title);
		title.setText(R.string.sort_by);
		title.setTextColor(ColorUtilities.getSecondaryTextColor(app, nightMode));

		RecyclerView recyclerView = view.findViewById(R.id.recycler_view);
		recyclerView.setLayoutManager(new LinearLayoutManager(themedContext));
		recyclerView.setAdapter(new SortModesAdapter());

		items.add(new BaseBottomSheetItem.Builder().setCustomView(view).create());
	}

	private void setMapsSortMode(@NonNull LocalSortMode sortMode) {
		if (getTargetFragment() instanceof MapsSortModeListener listener) {
			listener.setMapsSortMode(sortMode);
		}
	}

	public class SortModesAdapter extends RecyclerView.Adapter<SortModeViewHolder> {

		private final LocalSortMode[] sortModes = LocalSortMode.getSupportedModes(type);
		private final int activeColorId = ColorUtilities.getActiveIconColorId(nightMode);
		private final int defaultColorId = ColorUtilities.getDefaultIconColorId(nightMode);


		@NonNull
		@Override
		public SortModeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
			return new SortModeViewHolder(inflate(R.layout.list_item_two_icons, parent, false));
		}

		@Override
		public void onBindViewHolder(@NonNull SortModeViewHolder holder, int position) {
			LocalSortMode sortMode = sortModes[position];

			holder.title.setText(sortMode.getNameId());

			boolean selected = sortMode == SortMapsBottomSheet.this.sortMode;
			int colorId = selected ? activeColorId : defaultColorId;
			holder.groupTypeIcon.setImageDrawable(getIcon(sortMode.getIconId(), colorId));

			holder.itemView.setOnClickListener(view -> {
				int adapterPosition = holder.getAdapterPosition();
				if (adapterPosition != RecyclerView.NO_POSITION) {
					setMapsSortMode(sortModes[position]);
				}
				dismiss();
			});
			AndroidUiHelper.updateVisibility(holder.selectedIcon, selected);
			AndroidUiHelper.updateVisibility(holder.divider, shouldShowDivider(sortMode));
		}

		private boolean shouldShowDivider(@NonNull LocalSortMode mode) {
			return mode == NAME_DESCENDING || mode == COUNTRY_NAME_DESCENDING || mode == DATE_DESCENDING;
		}

		@Override
		public int getItemCount() {
			return sortModes.length;
		}
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putSerializable(SORT_MODE_KEY, sortMode);
	}

	public static void showInstance(@NonNull FragmentManager manager, @NonNull Fragment target, @NonNull LocalItemType type) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			Bundle bundle = new Bundle();
			bundle.putSerializable(ITEM_TYPE_KEY, type);

			SortMapsBottomSheet fragment = new SortMapsBottomSheet();
			fragment.setArguments(bundle);
			fragment.setTargetFragment(target, 0);
			fragment.show(manager, TAG);
		}
	}

	public interface MapsSortModeListener {
		void setMapsSortMode(@NonNull LocalSortMode sortMode);
	}
}

