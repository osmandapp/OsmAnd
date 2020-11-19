package net.osmand.plus.openplacereviews;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemButton;
import net.osmand.plus.base.bottomsheetmenu.SimpleBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.DividerItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.DividerSpaceItem;

public class AddPhotosBottomSheetDialogFragment extends MenuBottomSheetDialogFragment {

	public static final String TAG = AddPhotosBottomSheetDialogFragment.class.getSimpleName();

	@Override
	public void createMenuItems(Bundle savedInstanceState) {

		View mainView = View.inflate(UiUtilities.getThemedContext(getMyApplication(), nightMode),
				R.layout.opr_add_photo, null);
		items.add(new SimpleBottomSheetItem.Builder()
				.setCustomView(mainView)
				.create());

		DividerItem divider = new DividerItem(getContext());
		int contextPadding = getResources().getDimensionPixelSize(R.dimen.content_padding);
		int contextPaddingSmall = getResources().getDimensionPixelSize(R.dimen.content_padding_small);
		divider.setMargins(contextPadding, contextPadding, contextPadding, contextPaddingSmall);
		items.add(divider);

		items.add(new BottomSheetItemButton.Builder()
				.setTitle(getString(R.string.add_to_opr))
				.setLayoutId(R.layout.bottom_sheet_button)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						FragmentActivity activity = getActivity();
						if (activity != null) {
							OprStartFragment.showInstance(activity.getSupportFragmentManager());
						}
						dismiss();
					}
				})
				.create());

		items.add(new DividerSpaceItem(getContext(), contextPaddingSmall));

		items.add(new BottomSheetItemButton.Builder()
				.setButtonType(UiUtilities.DialogButtonType.SECONDARY)
				.setTitle(getString(R.string.add_to_mapillary))
				.setLayoutId(R.layout.bottom_sheet_button)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						Activity activity = getActivity();
						if (activity instanceof MapActivity) {
						}
						dismiss();
					}
				})
				.create());

		items.add(new DividerSpaceItem(getContext(), contextPaddingSmall));
	}

	@Override
	protected int getDismissButtonTextId() {
		return R.string.shared_string_cancel;
	}

	public static void showInstance(@NonNull FragmentManager fragmentManager) {
		if (!fragmentManager.isStateSaved()) {
			AddPhotosBottomSheetDialogFragment fragment = new AddPhotosBottomSheetDialogFragment();
			fragment.show(fragmentManager, TAG);
		}
	}
}