package net.osmand.plus.mapcontextmenu.other;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.SimpleBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.utils.AndroidUtils;

public class ShareMenuFragment extends MenuBottomSheetDialogFragment {

	public static final String TAG = "ShareMenuFragment";

	private ShareMenu menu;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (savedInstanceState != null && getActivity() instanceof MapActivity) {
			menu = ShareMenu.restoreMenu(savedInstanceState, (MapActivity) getActivity());
		}
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		items.add(new TitleItem(getString(R.string.share_menu_location)));

		View.OnClickListener itemOnClickListener = v -> {
			dismiss();
			menu.share((ShareItem) v.getTag());
		};

		for (ShareItem shareItem : menu.getItems()) {
			BaseBottomSheetItem item = new SimpleBottomSheetItem.Builder()
					.setIcon(getContentIcon(shareItem.getIconId()))
					.setTitle(getString(shareItem.getTitleId()))
					.setLayoutId(R.layout.bottom_sheet_item_simple)
					.setOnClickListener(itemOnClickListener)
					.setTag(shareItem)
					.create();
			items.add(item);
		}
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		menu.saveMenu(outState);
	}

	public static void showInstance(@NonNull FragmentManager manager, @NonNull ShareMenu menu) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			ShareMenuFragment fragment = new ShareMenuFragment();
			fragment.menu = menu;
			fragment.setUsedOnMap(true);
			fragment.show(manager, TAG);
		}
	}
}
