package net.osmand.plus.plugins.audionotes;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.R;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.SimpleBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.utils.AndroidUtils;

public class SortByMenuBottomSheetDialogFragment extends MenuBottomSheetDialogFragment {

	public static final String TAG = SortByMenuBottomSheetDialogFragment.class.getSimpleName();

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		items.add(new TitleItem(getString(R.string.shared_string_sort)));

		BaseBottomSheetItem byTypeItem = new SimpleBottomSheetItem.Builder()
				.setIcon(getContentIcon(R.drawable.ic_grouped_by_type))
				.setTitle(getString(R.string.by_type))
				.setLayoutId(R.layout.bottom_sheet_item_simple)
				.setOnClickListener(v -> selectSortByMode(NotesSortByMode.BY_TYPE))
				.create();
		items.add(byTypeItem);

		BaseBottomSheetItem byDateItem = new SimpleBottomSheetItem.Builder()
				.setIcon(getContentIcon(R.drawable.ic_action_sort_by_date))
				.setTitle(getString(R.string.by_date))
				.setLayoutId(R.layout.bottom_sheet_item_simple)
				.setOnClickListener(v -> selectSortByMode(NotesSortByMode.BY_DATE))
				.create();
		items.add(byDateItem);
	}

	private void selectSortByMode(NotesSortByMode mode) {
		AudioVideoNotesPlugin plugin = PluginsHelper.getPlugin(AudioVideoNotesPlugin.class);
		if (plugin != null && plugin.NOTES_SORT_BY_MODE.get() != mode) {
			plugin.NOTES_SORT_BY_MODE.set(mode);

			Fragment target = getTargetFragment();
			if (target instanceof NotesFragment) {
				((NotesFragment) target).recreateAdapterData();
			}
		}
		dismiss();
	}

	public static void showInstance(@NonNull FragmentManager manager, @NonNull Fragment target) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			SortByMenuBottomSheetDialogFragment fragment = new SortByMenuBottomSheetDialogFragment();
			fragment.setUsedOnMap(false);
			fragment.setTargetFragment(target, 0);
			fragment.show(manager, TAG);
		}
	}
}
