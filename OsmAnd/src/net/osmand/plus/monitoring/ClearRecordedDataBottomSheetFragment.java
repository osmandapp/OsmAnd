package net.osmand.plus.monitoring;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithDescription;
import net.osmand.plus.widgets.TextViewEx;

import androidx.annotation.DimenRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

public class ClearRecordedDataBottomSheetFragment extends MenuBottomSheetDialogFragment implements View.OnClickListener {

	public static final String TAG = ClearRecordedDataBottomSheetFragment.class.getSimpleName();

	private OsmandApplication app;

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		app = requiredMyApplication();

		items.add(new BottomSheetItemWithDescription.Builder()
				.setDescription(app.getString(R.string.clear_recorded_data_warning))
				.setDescriptionColorId(!nightMode ? R.color.text_color_primary_light : R.color.text_color_primary_dark)
				.setDescriptionMaxLines(2)
				.setTitle(app.getString(R.string.clear_recorded_data))
				.setLayoutId(R.layout.bottom_sheet_item_title_with_description)
				.create());

		LayoutInflater inflater = UiUtilities.getInflater(getContext(), nightMode);

		items.add(new BaseBottomSheetItem.Builder()
				.setCustomView(setupBtn(inflater, ButtonType.CLEAR))
				.setOnClickListener(this)
				.create());

		items.add(new BaseBottomSheetItem.Builder()
				.setCustomView(setupBtn(inflater, ButtonType.CANCEL))
				.setOnClickListener(this)
				.create());

	}

	private View setupBtn(LayoutInflater inflater, ButtonType type) {
		View button = inflater.inflate(R.layout.bottom_sheet_item_button_with_icon, null);
		button.setTag(type);
		Context context = button.getContext();

		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
		int horizontal = context.getResources().getDimensionPixelSize(R.dimen.content_padding);
		int top = context.getResources().getDimensionPixelSize(type.topMarginRes);
		int bottom = context.getResources().getDimensionPixelSize(R.dimen.content_padding_small);
		params.setMargins(horizontal, top, horizontal, bottom);
		button.setLayoutParams(params);

		UiUtilities.setupDialogButton(nightMode, button, type.effect, type.titleId);

		TextViewEx title = button.findViewById(R.id.button_text);
		int margin = context.getResources().getDimensionPixelSize(R.dimen.context_menu_padding_margin_medium);
		UiUtilities.setMargins(title, 0, margin, 0, margin);

		int colorRes;
		if (type.effect == UiUtilities.DialogButtonType.SECONDARY_HARMFUL) {
			colorRes = R.color.color_osm_edit_delete;
		} else {
			colorRes = nightMode ? R.color.dlg_btn_secondary_text_dark : R.color.dlg_btn_secondary_text_light;
		}
		AppCompatImageView icon = button.findViewById(R.id.icon);
		icon.setImageDrawable(getIcon(type.iconRes, colorRes));

		return button;
	}

	@Override
	public void onClick(View v) {
		Object o = v.getTag();
		if (!(o instanceof ButtonType)) {
			return;
		}

		ButtonType tag = (ButtonType) o;
		if (tag == ButtonType.CLEAR) {
			app.getSavingTrackHelper().clearRecordedData(true);
		}
		dismiss();
	}

	@Override
	public void onResume() {
		super.onResume();
		// Replace later with tTripRecordingActiveBottomSheet.hide()
		Fragment target = getTargetFragment();
		if (target instanceof TripRecordingActiveBottomSheet) {
			Dialog dialog = ((TripRecordingActiveBottomSheet) target).getDialog();
			if (dialog != null) {
				dialog.hide();
			}
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		// Replace later with tTripRecordingActiveBottomSheet.show()
		Fragment target = getTargetFragment();
		if (target instanceof TripRecordingActiveBottomSheet) {
			Dialog dialog = ((TripRecordingActiveBottomSheet) target).getDialog();
			if (dialog != null) {
				dialog.show();
			}
		}
	}

	enum ButtonType {
		CLEAR(R.string.clear_recorded_data, R.drawable.ic_action_delete_dark, R.dimen.dialog_content_margin, UiUtilities.DialogButtonType.SECONDARY_HARMFUL),
		CANCEL(R.string.shared_string_cancel, R.drawable.ic_action_close, R.dimen.content_padding_small, UiUtilities.DialogButtonType.SECONDARY);

		@StringRes
		private final int titleId;
		@DrawableRes
		private final int iconRes;
		@DimenRes
		private final int topMarginRes;
		private final UiUtilities.DialogButtonType effect;

		ButtonType(int titleId, int iconRes, int topMarginRes, UiUtilities.DialogButtonType effect) {
			this.titleId = titleId;
			this.iconRes = iconRes;
			this.topMarginRes = topMarginRes;
			this.effect = effect;
		}
	}

	@Override
	protected boolean hideButtonsContainer() {
		return true;
	}

	public static void showInstance(@NonNull FragmentManager fragmentManager, @NonNull Fragment target) {
		if (!fragmentManager.isStateSaved()) {
			ClearRecordedDataBottomSheetFragment fragment = new ClearRecordedDataBottomSheetFragment();
			fragment.setTargetFragment(target, 0);
			fragment.show(fragmentManager, TAG);
		}
	}
}