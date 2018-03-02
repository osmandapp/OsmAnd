package net.osmand.plus.mapcontextmenu.other;

import android.os.Bundle;
import android.view.View;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.SimpleBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.mapcontextmenu.other.ShareMenu.ShareItem;

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

		View.OnClickListener itemOnClickListener = new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dismiss();
				menu.share((ShareItem) v.getTag());
			}
		};

		for (ShareItem shareItem : menu.getItems()) {
			BaseBottomSheetItem item = new SimpleBottomSheetItem.Builder()
					.setIcon(getContentIcon(shareItem.getIconResourceId()))
					.setTitle(getString(shareItem.getTitleResourceId()))
					.setLayoutId(R.layout.bottom_sheet_item_simple)
					.setOnClickListener(itemOnClickListener)
					.setTag(shareItem)
					.create();
			items.add(item);
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		menu.saveMenu(outState);
	}

	public static void showInstance(ShareMenu menu) {
		ShareMenuFragment fragment = new ShareMenuFragment();
		fragment.menu = menu;
		fragment.setUsedOnMap(true);
		fragment.show(menu.getMapActivity().getSupportFragmentManager(), ShareMenuFragment.TAG);
	}
}
