package net.osmand.plus.quickaction;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.R;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithDescription;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.widgets.dialogbutton.DialogButtonType;

public class ConfirmationBottomSheet extends MenuBottomSheetDialogFragment {

	private static final String TITLE_KEY = "title";
	private static final String MESSAGE_KEY = "message";
	private static final String RIGHT_BUTTON_TITLE_KEY = "right_button_title";

	private String title;
	private CharSequence message;
	private int rightButtonTitle;

	public static final String TAG = ConfirmationBottomSheet.class.getSimpleName();

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
		if (savedInstanceState != null) {
			title = savedInstanceState.getString(TITLE_KEY);
			message = savedInstanceState.getCharSequence(MESSAGE_KEY);
			rightButtonTitle = savedInstanceState.getInt(RIGHT_BUTTON_TITLE_KEY);
		}
		View view = super.onCreateView(inflater, parent, savedInstanceState);
		rightButton.setButtonType(DialogButtonType.SECONDARY);
		rightButton.setTitleId(rightButtonTitle);
		TextView tvRightButton = rightButton.findViewById(R.id.button_text);
		int colorDelete = getColor(R.color.color_osm_edit_delete);
		tvRightButton.setTextColor(colorDelete);
		return view;
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		items.add(new TitleItem(title));
		items.add(new BottomSheetItemWithDescription.Builder()
				.setDescription(message)
				.setLayoutId(R.layout.bottom_sheet_item_primary_descr)
				.create());
	}

	@Override
	protected void onRightBottomButtonClick() {
		if (getTargetFragment() instanceof OnConfirmButtonClickListener listener) {
			listener.onConfirmButtonClick();
		}
		dismiss();
	}

	@Override
	protected int getRightBottomButtonTextId() {
		return rightButtonTitle;
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString(TITLE_KEY, title);
		outState.putCharSequence(MESSAGE_KEY, message);
		outState.putInt(RIGHT_BUTTON_TITLE_KEY, rightButtonTitle);
	}

	public static void showInstance(@NonNull FragmentManager manager, @NonNull Fragment target,
	                                @NonNull String title, @NonNull CharSequence message,
	                                int rightButtonTitle, boolean usedOnMap) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			ConfirmationBottomSheet fragment = new ConfirmationBottomSheet();
			fragment.title = title;
			fragment.message = message;
			fragment.rightButtonTitle = rightButtonTitle;
			fragment.setTargetFragment(target, 0);
			fragment.setUsedOnMap(usedOnMap);
			fragment.show(manager, TAG);
		}
	}

	public interface OnConfirmButtonClickListener {
		void onConfirmButtonClick();
	}
}
