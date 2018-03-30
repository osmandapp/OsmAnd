package net.osmand.plus.mapmarkers;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.ColorRes;
import android.support.annotation.Nullable;
import android.view.View;

import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithCompoundButton;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithDescription;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.SubtitleDividerItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.SubtitleItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.mapmarkers.CoordinateInputFormats.CoordinateInputFormatDef;

public class CoordinateInputBottomSheetDialogFragment extends MenuBottomSheetDialogFragment {

	public final static String TAG = "CoordinateInputBottomSheetDialogFragment";

	private CoordinateInputFormatChangeListener listener;

	public void setListener(CoordinateInputFormatChangeListener listener) {
		this.listener = listener;
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		final Context context = getContext();
		final OsmandSettings settings = getMyApplication().getSettings();

		items.add(new TitleItem(getString(R.string.shared_string_options)));
		boolean useOsmandKeyboard = settings.COORDS_INPUT_USE_OSMAND_KEYBOARD.get();

		BaseBottomSheetItem useSystemKeyboardItem = new BottomSheetItemWithCompoundButton.Builder()
				.setChecked(!useOsmandKeyboard)
				.setIcon(getContentIcon(R.drawable.ic_action_keyboard))
				.setTitle(getString(R.string.use_system_keyboard))
				.setLayoutId(R.layout.bottom_sheet_item_with_switch)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						if (listener != null) {
							listener.onKeyboardChanged();
						}
						dismiss();
					}
				})
				.create();
		items.add(useSystemKeyboardItem);

		boolean useTwoDigitsLogtitude = settings.COORDS_INPUT_TWO_DIGITS_LONGTITUDE.get();
		BaseBottomSheetItem twoDigitsLongtitudeItem = new BottomSheetItemWithCompoundButton.Builder()
				.setChecked(useTwoDigitsLogtitude)
				.setIcon(getContentIcon(R.drawable.ic_action_next_field_stroke))
				.setTitle(getString(R.string.use_two_digits_longitude))
				.setLayoutId(R.layout.bottom_sheet_item_with_switch)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						if (listener != null) {
							OsmandSettings.CommonPreference<Boolean> pref = settings.COORDS_INPUT_TWO_DIGITS_LONGTITUDE;
							pref.set(!pref.get());
							listener.onTwoDigitsLongtitudeChanged();
						}
						dismiss();
					}
				})
				.create();
		items.add(twoDigitsLongtitudeItem);

		if (!AndroidUiHelper.isOrientationPortrait(getActivity())) {
			boolean rightHand = settings.COORDS_INPUT_USE_RIGHT_SIDE.get();

			BaseBottomSheetItem showNumberPadItem = new BottomSheetItemWithDescription.Builder()
					.setDescription(getString(rightHand ? R.string.shared_string_right : R.string.shared_string_left))
					.setDescriptionColorId(getActiveColorId())
					.setIcon(getContentIcon(rightHand
							? R.drawable.ic_action_show_keypad_right
							: R.drawable.ic_action_show_keypad_left))
					.setTitle(getString(R.string.show_number_pad))
					.setLayoutId(R.layout.bottom_sheet_item_with_right_descr)
					.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							if (listener != null) {
								OsmandSettings.CommonPreference<Boolean> pref = settings.COORDS_INPUT_USE_RIGHT_SIDE;
								pref.set(!pref.get());
								listener.onHandChanged();
							}
							dismiss();
						}
					})
					.create();
			items.add(showNumberPadItem);
		}

		items.add(new SubtitleDividerItem(context));

		items.add(new SubtitleItem(getString(R.string.coordinates_format)));

		int selectedFormat = settings.COORDS_INPUT_FORMAT.get();
		Drawable formatIcon = getContentIcon(R.drawable.ic_action_coordinates_latitude);
		View.OnClickListener formatsOnClickListener = new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				int format = (int) v.getTag();
				settings.COORDS_INPUT_FORMAT.set(format);
				if (listener != null) {
					listener.onFormatChanged();
				}
				dismiss();
			}
		};

		for (@CoordinateInputFormatDef int format : CoordinateInputFormats.VALUES) {
			boolean selectedItem = format == selectedFormat;

			BaseBottomSheetItem formatItem = new BottomSheetItemWithCompoundButton.Builder()
					.setChecked(selectedItem)
					.setButtonTintList(selectedItem
							? ColorStateList.valueOf(getResolvedColor(getActiveColorId()))
							: null)
					.setIcon(selectedItem ? getActiveIcon(R.drawable.ic_action_coordinates_latitude) : formatIcon)
					.setTitle(CoordinateInputFormats.formatToHumanString(context, format))
					.setTitleColorId(selectedItem ? getActiveColorId() : BaseBottomSheetItem.INVALID_ID)
					.setLayoutId(R.layout.bottom_sheet_item_with_radio_btn)
					.setOnClickListener(formatsOnClickListener)
					.setTag(format)
					.create();
			items.add(formatItem);
		}
	}

	@Override
	protected int getDismissButtonTextId() {
		return R.string.shared_string_close;
	}

	@ColorRes
	private int getActiveColorId() {
		return nightMode ? R.color.osmand_orange : R.color.color_myloc_distance;
	}

	interface CoordinateInputFormatChangeListener {

		void onTwoDigitsLongtitudeChanged();

		void onKeyboardChanged();

		void onHandChanged();

		void onFormatChanged();
	}
}
