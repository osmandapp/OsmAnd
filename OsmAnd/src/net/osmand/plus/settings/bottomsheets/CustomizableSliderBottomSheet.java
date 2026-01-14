package net.osmand.plus.settings.bottomsheets;

import static net.osmand.plus.base.dialog.data.DialogExtra.SUBTITLE;
import static net.osmand.plus.base.dialog.data.DialogExtra.TITLE;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.R;
import net.osmand.plus.base.bottomsheetmenu.SimpleBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.LongDescriptionItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.card.base.headed.HeadedContentCard;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.controllers.ICustomizableSliderDialogController;
import net.osmand.plus.utils.AndroidUtils;

public class CustomizableSliderBottomSheet extends CustomizableBottomSheet {

	public static final String TAG = CustomizableSliderBottomSheet.class.getSimpleName();

	private ICustomizableSliderDialogController controller;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		controller = (ICustomizableSliderDialogController) manager.findController(processId);
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		if (displayData == null) return;

		String title = (String) displayData.getExtra(TITLE);
		if (title != null) {
			TitleItem titleItem = new TitleItem(title);
			items.add(titleItem);
		}

		String description = (String) displayData.getExtra(SUBTITLE);
		if (description != null) {
			LongDescriptionItem descriptionItem = new LongDescriptionItem(description);
			items.add(descriptionItem);
		}

		HeadedContentCard contentCard = new HeadedContentCard(requireActivity(), controller, usedOnMap);
		items.add(new SimpleBottomSheetItem.Builder().setCustomView(contentCard.build()).create());
	}

	@Override
	protected int getDismissButtonTextId() {
		return R.string.shared_string_cancel;
	}

	@Override
	protected int getRightBottomButtonTextId() {
		return R.string.shared_string_apply;
	}

	@Override
	protected void onRightBottomButtonClick() {
		controller.onApplyChanges();
		dismiss();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		controller.onDestroy(getActivity());
	}

	public static void showInstance(@NonNull FragmentManager manager,
	                                @NonNull ApplicationMode appMode,
	                                @NonNull String processId) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			CustomizableSliderBottomSheet fragment = new CustomizableSliderBottomSheet();
			fragment.setProcessId(processId);
			fragment.setAppMode(appMode);
			fragment.show(manager, TAG);
		}
	}
}
