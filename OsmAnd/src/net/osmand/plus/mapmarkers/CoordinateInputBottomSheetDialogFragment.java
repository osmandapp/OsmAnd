package net.osmand.plus.mapmarkers;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.R;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithCompoundButton;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithDescription;
import net.osmand.plus.base.bottomsheetmenu.SimpleBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.DividerHalfItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.SubtitleDividerItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.SubtitleItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.mapmarkers.CoordinateInputFormats.Format;
import net.osmand.plus.utils.AndroidUtils;

public class CoordinateInputBottomSheetDialogFragment extends MenuBottomSheetDialogFragment {

	public static final String TAG = CoordinateInputBottomSheetDialogFragment.class.getSimpleName();

	private CoordinateInputFormatChangeListener listener;

	public void setListener(CoordinateInputFormatChangeListener listener) {
		this.listener = listener;
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		Context context = getThemedContext();

		items.add(new TitleItem(getString(R.string.shared_string_options)));
		BaseBottomSheetItem editItem = new SimpleBottomSheetItem.Builder()
				.setIcon(getContentIcon(R.drawable.ic_action_save_to_file))
				.setTitle(getString(R.string.coord_input_save_as_track))
				.setLayoutId(R.layout.bottom_sheet_item_simple)
				.setOnClickListener(v -> {
					if (listener != null) {
						listener.saveAsTrack();
					}
					dismiss();
				})
				.create();
		items.add(editItem);

		items.add(new DividerHalfItem(context));
		
		boolean useOsmandKeyboard = settings.COORDS_INPUT_USE_OSMAND_KEYBOARD.get();

		BaseBottomSheetItem useSystemKeyboardItem = new BottomSheetItemWithCompoundButton.Builder()
				.setChecked(!useOsmandKeyboard)
				.setIcon(getContentIcon(R.drawable.ic_action_keyboard))
				.setTitle(getString(R.string.use_system_keyboard))
				.setLayoutId(R.layout.bottom_sheet_item_with_switch)
				.setOnClickListener(v -> {
					if (listener != null) {
						listener.onKeyboardChanged();
					}
					dismiss();
				})
				.create();
		items.add(useSystemKeyboardItem);

		boolean useTwoDigitsLogtitude = settings.COORDS_INPUT_TWO_DIGITS_LONGTITUDE.get();
		BaseBottomSheetItem twoDigitsLongtitudeItem = new BottomSheetItemWithCompoundButton.Builder()
				.setChecked(useTwoDigitsLogtitude)
				.setIcon(getContentIcon(R.drawable.ic_action_next_field_stroke))
				.setTitle(getString(R.string.use_two_digits_longitude))
				.setLayoutId(R.layout.bottom_sheet_item_with_switch)
				.setOnClickListener(v -> {
					if (listener != null) {
						CommonPreference<Boolean> pref = settings.COORDS_INPUT_TWO_DIGITS_LONGTITUDE;
						pref.set(!pref.get());
						listener.onInputSettingsChanged();
					}
					dismiss();
				})
				.create();
		items.add(twoDigitsLongtitudeItem);

		if (!AndroidUiHelper.isOrientationPortrait(requireActivity())) {
			boolean rightHand = settings.COORDS_INPUT_USE_RIGHT_SIDE.get();

			BaseBottomSheetItem showNumberPadItem = new BottomSheetItemWithDescription.Builder()
					.setDescription(getString(rightHand ? R.string.shared_string_right : R.string.shared_string_left))
					.setDescriptionColorId(getActiveColorId())
					.setIcon(getContentIcon(rightHand
							? R.drawable.ic_action_show_keypad_right
							: R.drawable.ic_action_show_keypad_left))
					.setTitle(getString(R.string.show_number_pad))
					.setLayoutId(R.layout.bottom_sheet_item_with_right_descr)
					.setOnClickListener(v -> {
						if (listener != null) {
							CommonPreference<Boolean> pref = settings.COORDS_INPUT_USE_RIGHT_SIDE;
							pref.set(!pref.get());
							listener.onHandChanged();
						}
						dismiss();
					})
					.create();
			items.add(showNumberPadItem);
		}

		items.add(new SubtitleDividerItem(context));

		items.add(new SubtitleItem(getString(R.string.coordinates_format)));

		Format selectedFormat = settings.COORDS_INPUT_FORMAT.get();
		Drawable formatIcon = getContentIcon(R.drawable.ic_action_coordinates_latitude);
		View.OnClickListener formatsOnClickListener = v -> {
			Format format = (Format) v.getTag();
			settings.COORDS_INPUT_FORMAT.set(format);
			if (listener != null) {
				listener.onInputSettingsChanged();
			}
			dismiss();
		};

		for (Format format : Format.values()) {
			boolean selectedItem = format == selectedFormat;

			BaseBottomSheetItem formatItem = new BottomSheetItemWithCompoundButton.Builder()
					.setChecked(selectedItem)
					.setButtonTintList(selectedItem
							? ColorStateList.valueOf(getColor(getActiveColorId()))
							: null)
					.setIcon(selectedItem ? getActiveIcon(R.drawable.ic_action_coordinates_latitude) : formatIcon)
					.setTitle(format.toHumanString(context))
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

	public static void showInstance(@NonNull FragmentManager childFragmentManager,
	                                @NonNull CoordinateInputFormatChangeListener listener) {
		if (AndroidUtils.isFragmentCanBeAdded(childFragmentManager, TAG)) {
			CoordinateInputBottomSheetDialogFragment fragment = new CoordinateInputBottomSheetDialogFragment();
			fragment.setUsedOnMap(false);
			fragment.setListener(listener);
			fragment.show(childFragmentManager, TAG);
		}
	}

	public interface CoordinateInputFormatChangeListener {

		void onKeyboardChanged();

		void onHandChanged();

		void onInputSettingsChanged();
		
		void saveAsTrack();
	}
}
