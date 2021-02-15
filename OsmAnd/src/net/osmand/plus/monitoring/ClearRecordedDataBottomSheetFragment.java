package net.osmand.plus.monitoring;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithDescription;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.DividerSpaceItem;
import net.osmand.plus.monitoring.TripRecordingActiveBottomSheet.ItemType;
import net.osmand.plus.widgets.TextViewEx;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

public class ClearRecordedDataBottomSheetFragment extends MenuBottomSheetDialogFragment {

	public static final String TAG = ClearRecordedDataBottomSheetFragment.class.getSimpleName();

	private OsmandApplication app;

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		app = requiredMyApplication();
		LayoutInflater inflater = UiUtilities.getInflater(app, nightMode);
		int verticalBig = getResources().getDimensionPixelSize(R.dimen.dialog_content_margin);
		int verticalSmall = getResources().getDimensionPixelSize(R.dimen.content_padding_small);

		items.add(new BottomSheetItemWithDescription.Builder()
				.setDescription(app.getString(R.string.clear_recorded_data_warning))
				.setDescriptionColorId(!nightMode ? R.color.text_color_primary_light : R.color.text_color_primary_dark)
				.setDescriptionMaxLines(2)
				.setTitle(app.getString(R.string.clear_recorded_data))
				.setLayoutId(R.layout.bottom_sheet_item_title_with_description)
				.create());

		items.add(new DividerSpaceItem(app, verticalBig));

		items.add(new BaseBottomSheetItem.Builder()
				.setCustomView(setupBtn(inflater, ItemType.CLEAR_DATA))
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						app.getSavingTrackHelper().clearRecordedData(true);
						dismiss();
					}
				})
				.create());

		items.add(new DividerSpaceItem(app, verticalBig));

		items.add(new BaseBottomSheetItem.Builder()
				.setCustomView(setupBtn(inflater, ItemType.CANCEL))
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						dismiss();
					}
				})
				.create());

		items.add(new DividerSpaceItem(app, verticalSmall));
	}

	private View setupBtn(LayoutInflater inflater, ItemType type) {
		View button = inflater.inflate(R.layout.bottom_sheet_button_with_icon, null);
		button.setTag(type);
		Context context = button.getContext();
		LinearLayout container = button.findViewById(R.id.button_container);
		container.setClickable(false);
		container.setFocusable(false);

		FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
		int horizontal = context.getResources().getDimensionPixelSize(R.dimen.content_padding);
		params.setMargins(horizontal, 0, horizontal, 0);
		button.setLayoutParams(params);

		if (type.getTitleId() != null) {
			UiUtilities.setupDialogButton(nightMode, button, type.getEffect(), type.getTitleId());
		}

		TextViewEx title = button.findViewById(R.id.button_text);
		int margin = context.getResources().getDimensionPixelSize(R.dimen.context_menu_padding_margin_medium);
		UiUtilities.setMargins(title, 0, margin, 0, margin);

		int colorRes;
		if (type.getEffect() == UiUtilities.DialogButtonType.SECONDARY_HARMFUL) {
			colorRes = R.color.color_osm_edit_delete;
		} else {
			colorRes = nightMode ? R.color.dlg_btn_secondary_text_dark : R.color.dlg_btn_secondary_text_light;
		}
		AppCompatImageView icon = button.findViewById(R.id.icon);
		if (type.getIconId() != null) {
			icon.setImageDrawable(getIcon(type.getIconId(), colorRes));
		}

		return button;
	}

	@Override
	public void onResume() {
		super.onResume();
		Fragment target = getTargetFragment();
		if (target instanceof TripRecordingActiveBottomSheet) {
			((TripRecordingActiveBottomSheet) target).hide();
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		Fragment target = getTargetFragment();
		if (target instanceof TripRecordingActiveBottomSheet) {
			((TripRecordingActiveBottomSheet) target).show();
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